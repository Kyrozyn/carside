package me.kyrozyn.carside.ui.offlinemaps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.kyrozyn.carside.data.offline.INDONESIAN_CITY_PRESETS
import me.kyrozyn.carside.data.offline.OfflineMapsRepository
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.ActiveOfflineDownload
import me.kyrozyn.carside.domain.model.CityPreset
import me.kyrozyn.carside.domain.model.OfflineMapBounds
import me.kyrozyn.carside.domain.model.OfflineRegionInfo

private const val AVG_BYTES_PER_TILE = 15_000L

data class OfflineMapsUiState(
    val cityPresets: List<CityPreset> = INDONESIAN_CITY_PRESETS,
    val selectedCityId: String = INDONESIAN_CITY_PRESETS.first().id,
    /** Zoom range slider (0f–17f); disimpan sebagai float untuk RangeSlider. */
    val zoomMin: Float = 10f,
    val zoomMax: Float = 15f,
    val estimatedTiles: Long = 0L,
    val estimatedSizeBytes: Long = 0L,
    val lastError: String? = null,
)

class OfflineMapsViewModel(
    private val repository: OfflineMapsRepository,
) : ViewModel() {

    val regions: StateFlow<List<OfflineRegionInfo>> = repository.regions
    val activeDownload: StateFlow<ActiveOfflineDownload?> = repository.activeDownload

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.refreshRegions()
            recomputeEstimate()
        }
    }

    fun clearLastError() {
        _uiState.update { it.copy(lastError = null) }
    }

    fun setSelectedCityId(id: String) {
        if (_uiState.value.cityPresets.none { it.id == id }) return
        _uiState.update { it.copy(selectedCityId = id) }
        recomputeEstimate()
    }

    fun setZoomRange(minZ: Float, maxZ: Float) {
        var lo = min(minZ, maxZ)
        var hi = max(minZ, maxZ)
        lo = lo.coerceIn(0f, 17f)
        hi = hi.coerceIn(0f, 17f)
        if (lo > hi) lo = hi
        _uiState.update { it.copy(zoomMin = lo, zoomMax = hi) }
        recomputeEstimate()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshRegions()
        }
    }

    /** @return true jika unduhan berhasil diantrikan (bisa navigasi kembali). */
    fun startDownload(): Boolean {
        val s = _uiState.value
        val city = s.cityPresets.find { it.id == s.selectedCityId } ?: return false
        val minZ = floor(s.zoomMin.toDouble()).toInt().coerceIn(0, 17)
        val maxZ = floor(s.zoomMax.toDouble()).toInt().coerceIn(0, 17)
        if (minZ > maxZ) {
            _uiState.update { it.copy(lastError = "Zoom min tidak boleh lebih besar dari max.") }
            return false
        }
        val displayName = "${city.displayName} (${city.province})"
        val started = repository.enqueueBackgroundDownload(
            displayName = displayName,
            cityId = city.id,
            bounds = city.bounds,
            minZoom = minZ,
            maxZoom = maxZ,
        )
        if (!started) {
            _uiState.update { it.copy(lastError = "Unduhan lain sedang berjalan. Tunggu selesai atau jeda di daftar.") }
            return false
        }
        _uiState.update { it.copy(lastError = null) }
        return true
    }

    fun pauseRegion(id: Long) {
        viewModelScope.launch {
            repository.pauseRegion(id)
        }
    }

    fun resumeRegion(id: Long) {
        viewModelScope.launch {
            repository.resumeRegion(id)
        }
    }

    fun deleteRegion(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteRegion(id) }
                .onFailure { e ->
                    _uiState.update { it.copy(lastError = e.message ?: e.toString()) }
                }
        }
    }

    private fun recomputeEstimate() {
        val s = _uiState.value
        val city = s.cityPresets.find { it.id == s.selectedCityId } ?: return
        val minZ = floor(s.zoomMin.toDouble()).toInt().coerceIn(0, 17)
        val maxZ = floor(s.zoomMax.toDouble()).toInt().coerceIn(0, 17)
        val tiles = estimateTiles(city.bounds, min(minZ, maxZ), max(minZ, maxZ))
        val bytes = tiles * AVG_BYTES_PER_TILE
        _uiState.update { it.copy(estimatedTiles = tiles, estimatedSizeBytes = bytes) }
    }

    companion object {
        fun estimateTiles(bounds: OfflineMapBounds, minZ: Int, maxZ: Int): Long {
            if (minZ > maxZ) return 0L
            var total = 0L
            for (z in minZ..maxZ) {
                val n = 1L shl z
                val xWest = lonToTileX(bounds.west, z)
                val xEast = lonToTileX(bounds.east, z)
                val xMin = min(xWest, xEast)
                val xMax = max(xWest, xEast)
                val yNorth = latToTileY(bounds.north, z)
                val ySouth = latToTileY(bounds.south, z)
                val yMin = min(yNorth, ySouth)
                val yMax = max(yNorth, ySouth)
                total += (xMax - xMin + 1) * (yMax - yMin + 1)
            }
            return total
        }

        private fun lonToTileX(lon: Double, z: Int): Long {
            val n = 1L shl z
            return floor((lon + 180.0) / 360.0 * n).toLong().coerceIn(0L, n - 1)
        }

        private fun latToTileY(latDeg: Double, z: Int): Long {
            val latRad = Math.toRadians(latDeg)
            val n = 1L shl z
            val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / (2 * Math.PI)) * n
            return floor(y).toLong().coerceIn(0L, n - 1)
        }

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(OfflineMapsViewModel::class.java))
                    return OfflineMapsViewModel(container.offlineMapsRepository) as T
                }
            }
    }
}
