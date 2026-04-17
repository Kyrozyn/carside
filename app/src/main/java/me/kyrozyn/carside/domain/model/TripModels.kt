package me.kyrozyn.carside.domain.model

data class Trip(
    val id: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maxSpeedMps: Float?,
    val avgSpeedMps: Float?,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
)

data class TripPoint(
    val id: Long,
    val tripId: Long,
    val epochMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float?,
)

data class FuelEntry(
    val id: Long,
    val filledAtEpochMs: Long,
    val odometerKm: Double,
    val liters: Double,
    val totalCostIdr: Long,
    val isFullTank: Boolean,
    val notes: String?,
)

data class FuelEconomySummary(
    /** km/L dari segmen full-to-full terakhir yang valid. */
    val latestKmPerLiter: Double?,
    /** Rata-rata km/L dari semua segmen full-to-full. */
    val rollingAvgKmPerLiter: Double?,
    val totalSpentIdr: Long,
    val totalLiters: Double,
    /** Selisih odometer terakhir dan pertama (km). */
    val totalDistanceKm: Double?,
    /** Rp per km berdasarkan total pengeluaran / jarak odometer. */
    val pricePerKmIdr: Double?,
)

enum class TripTrackerPhase {
    Idle,
    /** Kecepatan di atas ambang; menunggu 10 detik berturut-turut. */
    Arming,
    /** Trip aktif (bergerak). */
    Active,
    /** Kecepatan rendah; menunggu 15 menit (antisipasi macet) sebelum mengakhiri trip. */
    Cooldown,
}

data class TripTrackerState(
    val phase: TripTrackerPhase,
    /** Jarak kumulatif trip saat ini (meter). */
    val currentDistanceMeters: Double,
    /** Durasi trip saat ini (detik). */
    val currentDurationSeconds: Long,
    val maxSpeedMps: Float?,
)
