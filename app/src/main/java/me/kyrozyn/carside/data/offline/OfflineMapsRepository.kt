package me.kyrozyn.carside.data.offline

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_LIBERTY_STYLE_URL
import me.kyrozyn.carside.domain.model.ActiveOfflineDownload
import me.kyrozyn.carside.domain.model.OfflineDownloadProgress
import me.kyrozyn.carside.domain.model.OfflineMapBounds
import me.kyrozyn.carside.domain.model.OfflineRegionInfo
import me.kyrozyn.carside.domain.model.OfflineRegionState
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps MapLibre [OfflineManager] for OpenFreeMap Liberty style offline packs.
 */
class OfflineMapsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val offlineManager: OfflineManager = OfflineManager.getInstance(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Scope aplikasi: unduhan tidak dibatalkan saat layar offline ditutup. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var backgroundDownloadJob: Job? = null

    private val _regions = MutableStateFlow<List<OfflineRegionInfo>>(emptyList())
    val regions: StateFlow<List<OfflineRegionInfo>> = _regions.asStateFlow()

    private val _activeDownload = MutableStateFlow<ActiveOfflineDownload?>(null)
    val activeDownload: StateFlow<ActiveOfflineDownload?> = _activeDownload.asStateFlow()

    suspend fun refreshRegions() {
        val list = withContext(Dispatchers.Main) { listOfflineRegionsInternal() }
        val mapped = list.mapNotNull { region ->
            runCatching { mapRegionToInfo(region) }.getOrNull()
        }
        _regions.value = mapped.sortedBy { it.name }
    }

    /**
     * Memulai unduhan di background. Mengembalikan `false` jika unduhan lain masih berjalan.
     * Koleksi tidak terikat ke ViewModel sehingga user bisa kembali ke dashboard.
     */
    fun enqueueBackgroundDownload(
        displayName: String,
        cityId: String?,
        bounds: OfflineMapBounds,
        minZoom: Int,
        maxZoom: Int,
    ): Boolean {
        if (backgroundDownloadJob?.isActive == true) {
            return false
        }
        val label = displayName
        backgroundDownloadJob = scope.launch {
            try {
                downloadRegion(
                    displayName = displayName,
                    cityId = cityId,
                    bounds = bounds,
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                ).collect { progress ->
                    _activeDownload.value = ActiveOfflineDownload(label, progress)
                }
            } finally {
                _activeDownload.value = null
                backgroundDownloadJob = null
                scope.launch { refreshRegions() }
            }
        }
        return true
    }

    fun downloadRegion(
        displayName: String,
        cityId: String?,
        bounds: OfflineMapBounds,
        minZoom: Int,
        maxZoom: Int,
    ): Flow<OfflineDownloadProgress> = callbackFlow {
        val metadata = buildMetadataJson(displayName, cityId, minZoom, maxZoom)
        val latLngBounds = bounds.toLatLngBounds()
        val pixelRatio = appContext.resources.displayMetrics.density.coerceAtLeast(1f)
        val definition = OfflineTilePyramidRegionDefinition(
            OPEN_FREE_MAP_LIBERTY_STYLE_URL,
            latLngBounds,
            minZoom.toDouble(),
            maxZoom.toDouble(),
            pixelRatio,
            false,
        )

        var activeRegion: OfflineRegion? = null

        fun startObservingAndDownload(region: OfflineRegion) {
            region.setDeliverInactiveMessages(true)
            region.setObserver(
                object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        trySend(status.toProgress(region.id))
                        if (status.isComplete) {
                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            region.setObserver(null)
                            close()
                        }
                    }

                    override fun onError(error: OfflineRegionError) {
                        val msg = error.reason + (error.message.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
                        trySend(
                            OfflineDownloadProgress(
                                regionId = region.id,
                                completedResourceCount = 0,
                                requiredResourceCount = 0,
                                completedResourceSize = 0,
                                isComplete = false,
                                downloadState = OfflineRegion.STATE_INACTIVE,
                                errorMessage = msg,
                            ),
                        )
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        trySend(
                            OfflineDownloadProgress(
                                regionId = region.id,
                                completedResourceCount = 0,
                                requiredResourceCount = 0,
                                completedResourceSize = 0,
                                isComplete = false,
                                downloadState = OfflineRegion.STATE_INACTIVE,
                                errorMessage = "Batas jumlah tile tercapai ($limit). Kurangi zoom atau area.",
                            ),
                        )
                    }
                },
            )
            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        }

        val createCallback = object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                activeRegion = offlineRegion
                startObservingAndDownload(offlineRegion)
            }

            override fun onError(error: String) {
                trySend(
                    OfflineDownloadProgress(
                        regionId = -1L,
                        completedResourceCount = 0,
                        requiredResourceCount = 0,
                        completedResourceSize = 0,
                        isComplete = false,
                        downloadState = OfflineRegion.STATE_INACTIVE,
                        errorMessage = error,
                    ),
                )
                close()
            }
        }

        mainHandler.post {
            offlineManager.createOfflineRegion(definition, metadata.toByteArray(Charsets.UTF_8), createCallback)
        }

        awaitClose {
            val r = activeRegion
            if (r != null) {
                mainHandler.post {
                    runCatching {
                        r.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        r.setObserver(null)
                    }
                }
            }
        }
    }

    suspend fun pauseRegion(regionId: Long) = withContext(Dispatchers.Main) {
        val region = findRegionById(regionId) ?: return@withContext
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        refreshRegions()
    }

    suspend fun resumeRegion(regionId: Long) = withContext(Dispatchers.Main) {
        val region = findRegionById(regionId) ?: return@withContext
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        refreshRegions()
    }

    suspend fun deleteRegion(regionId: Long) = withContext(Dispatchers.Main) {
        val region = findRegionById(regionId) ?: return@withContext
        suspendCancellableCoroutine { cont ->
            region.delete(
                object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(error: String) {
                        if (cont.isActive) cont.resumeWithException(IllegalStateException(error))
                    }
                },
            )
        }
        refreshRegions()
    }

    private suspend fun listOfflineRegionsInternal(): Array<OfflineRegion> = suspendCancellableCoroutine { cont ->
        offlineManager.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    if (cont.isActive) cont.resume(offlineRegions ?: emptyArray())
                }

                override fun onError(error: String) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException(error))
                }
            },
        )
    }

    private suspend fun findRegionById(id: Long): OfflineRegion? {
        val all = withContext(Dispatchers.Main) { listOfflineRegionsInternal() }
        return all.firstOrNull { it.id == id }
    }

    private suspend fun mapRegionToInfo(region: OfflineRegion): OfflineRegionInfo {
        val status = withContext(Dispatchers.Main) { region.awaitStatus() }
        val meta = parseMetadata(region.metadata)
        val def = region.definition as? OfflineTilePyramidRegionDefinition
        val bounds = def?.bounds?.toOfflineMapBounds()
            ?: OfflineMapBounds(north = 0.0, south = 0.0, east = 0.0, west = 0.0)
        val minZ = meta?.minZoom ?: def?.minZoom?.toInt() ?: 0
        val maxZ = meta?.maxZoom ?: def?.maxZoom?.toInt() ?: 0
        val name = meta?.name ?: "Area ${region.id}"
        val state = when {
            status.isComplete -> OfflineRegionState.COMPLETE
            status.downloadState == OfflineRegion.STATE_ACTIVE -> OfflineRegionState.DOWNLOADING
            else -> OfflineRegionState.IDLE
        }
        return OfflineRegionInfo(
            id = region.id,
            name = name,
            minZoom = minZ,
            maxZoom = maxZ,
            bounds = bounds,
            sizeBytes = status.completedResourceSize,
            tilesCompleted = status.completedTileCount,
            tilesRequired = status.requiredResourceCount,
            state = state,
            errorMessage = null,
        )
    }

    private suspend fun OfflineRegion.awaitStatus(): OfflineRegionStatus = suspendCancellableCoroutine { cont ->
        getStatus(
            object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    if (status != null && cont.isActive) cont.resume(status)
                }

                override fun onError(error: String?) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException(error ?: "Status error"))
                }
            },
        )
    }

    private fun OfflineRegionStatus.toProgress(regionId: Long): OfflineDownloadProgress =
        OfflineDownloadProgress(
            regionId = regionId,
            completedResourceCount = completedResourceCount,
            requiredResourceCount = requiredResourceCount,
            completedResourceSize = completedResourceSize,
            isComplete = isComplete,
            downloadState = downloadState,
        )

    private fun OfflineMapBounds.toLatLngBounds(): LatLngBounds =
        LatLngBounds.Builder()
            .include(LatLng(north, west))
            .include(LatLng(south, east))
            .build()

    private fun LatLngBounds.toOfflineMapBounds(): OfflineMapBounds =
        OfflineMapBounds(
            north = latitudeNorth,
            south = latitudeSouth,
            east = longitudeEast,
            west = longitudeWest,
        )

    private fun buildMetadataJson(name: String, cityId: String?, minZoom: Int, maxZoom: Int): String {
        val o = JSONObject()
        o.put("name", name)
        o.put("minZoom", minZoom)
        o.put("maxZoom", maxZoom)
        if (cityId != null) o.put("cityId", cityId)
        return o.toString()
    }

    private fun parseMetadata(bytes: ByteArray?): Metadata? {
        if (bytes == null || bytes.isEmpty()) return null
        return runCatching {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            Metadata(
                name = json.optString("name").takeIf { it.isNotBlank() },
                minZoom = if (json.has("minZoom")) json.getInt("minZoom") else null,
                maxZoom = if (json.has("maxZoom")) json.getInt("maxZoom") else null,
                cityId = json.optString("cityId").takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }

    private data class Metadata(
        val name: String?,
        val minZoom: Int?,
        val maxZoom: Int?,
        val cityId: String?,
    )
}
