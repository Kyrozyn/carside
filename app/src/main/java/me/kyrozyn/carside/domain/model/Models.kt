package me.kyrozyn.carside.domain.model

import java.time.Instant

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
)

data class TrackInfo(
    val title: String,
    val artist: String,
    val albumArtUri: String? = null,
)

data class WeatherSummary(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val description: String,
    val fetchedAt: Instant,
)

data class CompassHeading(
    val degrees: Float,
    val cardinal: String,
)
