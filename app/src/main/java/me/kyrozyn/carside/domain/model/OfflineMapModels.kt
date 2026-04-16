package me.kyrozyn.carside.domain.model

/**
 * Geographic bounds for offline tile pyramid (WGS84).
 * [north] and [south] are latitudes; [east] and [west] are longitudes.
 */
data class OfflineMapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
)

enum class OfflineRegionState {
    /** Download paused or not started; may still have partial cache. */
    IDLE,
    DOWNLOADING,
    COMPLETE,
    ERROR,
}

data class OfflineRegionInfo(
    val id: Long,
    val name: String,
    val minZoom: Int,
    val maxZoom: Int,
    val bounds: OfflineMapBounds,
    /** Bytes downloaded (tiles + other resources) when known. */
    val sizeBytes: Long,
    val tilesCompleted: Long,
    val tilesRequired: Long,
    val state: OfflineRegionState,
    val errorMessage: String? = null,
)

data class CityPreset(
    val id: String,
    val displayName: String,
    val province: String,
    val bounds: OfflineMapBounds,
)

data class OfflineDownloadProgress(
    val regionId: Long,
    val completedResourceCount: Long,
    val requiredResourceCount: Long,
    val completedResourceSize: Long,
    val isComplete: Boolean,
    val downloadState: Int,
    val errorMessage: String? = null,
)

/** Unduhan aktif di background (label + progres terakhir dari MapLibre). */
data class ActiveOfflineDownload(
    val label: String,
    val progress: OfflineDownloadProgress,
)
