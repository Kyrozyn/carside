package me.kyrozyn.carside.domain.model

import java.time.Instant

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    /** Bearing/arah gerak dari GPS (course over ground), derajat 0..360 jika tersedia. */
    val bearingDegrees: Float? = null,
    /** Kecepatan dari GPS dalam meter/detik jika tersedia. */
    val speedMps: Float? = null,
    /** Akurasi horizontal GPS (meter); null jika tidak tersedia. */
    val accuracyMeters: Float? = null,
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
    val windSpeedKmh: Double? = null,
    val humidityPercent: Int? = null,
    val timezone: String? = null,
    val elevationMeters: Double? = null,
    val forecastHourly: List<WeatherHourlyPoint> = emptyList(),
)

data class WeatherHourlyPoint(
    val timeIso: String,
    val temperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
)

data class CompassHeading(
    val degrees: Float,
    val cardinal: String,
)
