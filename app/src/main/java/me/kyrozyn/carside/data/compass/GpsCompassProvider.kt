package me.kyrozyn.carside.data.compass

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.kyrozyn.carside.domain.model.CompassHeading
import me.kyrozyn.carside.domain.repository.CompassProvider
import me.kyrozyn.carside.domain.repository.LocationProvider

/**
 * Compass berbasis GPS bearing (course over ground).
 * Saat bergerak (ada bearing dari GPS), heading diambil dari GPS.
 * Saat diam / GPS tidak punya bearing, heading menggunakan nilai GPS terakhir.
 */
class GpsCompassProvider(private val locationProvider: LocationProvider) : CompassProvider {

    private var lastKnownDegrees: Float = 0f

    override val heading: Flow<CompassHeading> = locationProvider.locationUpdates
        .map { loc ->
            val bearing = loc?.bearingDegrees
            if (bearing != null) {
                lastKnownDegrees = normalize(bearing)
            }
            val deg = lastKnownDegrees
            CompassHeading(degrees = deg, cardinal = toCardinal(deg))
        }

    private fun normalize(deg: Float): Float {
        val mod = deg % 360f
        return if (mod < 0f) mod + 360f else mod
    }

    private fun toCardinal(deg: Float): String {
        val d = ((deg % 360f) + 360f) % 360f
        return when {
            d < 22.5f || d >= 337.5f -> "Utara"
            d < 67.5f  -> "Timur Laut"
            d < 112.5f -> "Timur"
            d < 157.5f -> "Tenggara"
            d < 202.5f -> "Selatan"
            d < 247.5f -> "Barat Daya"
            d < 292.5f -> "Barat"
            else       -> "Barat Laut"
        }
    }
}
