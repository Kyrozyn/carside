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

enum class RadioBand { FM, AM }

data class RadioProgramInfo(
    val frequencyKhz: Int,
    val band: RadioBand,
    val stationName: String? = null,
    val radioText: String? = null,
    val programType: String? = null,
    val signalStrength: Int? = null,
)

data class RadioPreset(
    val slot: Int,
    val frequencyKhz: Int,
    val band: RadioBand,
    val label: String? = null,
)

data class RadioState(
    val available: Boolean,
    val band: RadioBand,
    val frequencyKhz: Int,
    val fmRangeKhz: IntRange,
    val amRangeKhz: IntRange,
    val fmStepKhz: Int,
    val amStepKhz: Int,
    val isTuning: Boolean,
    val isScanning: Boolean,
    val isMuted: Boolean,
    val program: RadioProgramInfo?,
    val presets: List<RadioPreset>,
    val errorMessage: String?,
) {
    val stepKhz: Int
        get() = if (band == RadioBand.FM) fmStepKhz else amStepKhz

    val currentRangeKhz: IntRange
        get() = if (band == RadioBand.FM) fmRangeKhz else amRangeKhz

    companion object {
        val DEFAULT_FM_RANGE_KHZ: IntRange = 87_500..108_000
        val DEFAULT_AM_RANGE_KHZ: IntRange = 530..1_710
        const val DEFAULT_FM_STEP_KHZ: Int = 100
        const val DEFAULT_AM_STEP_KHZ: Int = 9
        const val PRESET_SLOT_COUNT: Int = 6

        fun unavailable(presets: List<RadioPreset> = emptyList()): RadioState = RadioState(
            available = false,
            band = RadioBand.FM,
            frequencyKhz = DEFAULT_FM_RANGE_KHZ.first,
            fmRangeKhz = DEFAULT_FM_RANGE_KHZ,
            amRangeKhz = DEFAULT_AM_RANGE_KHZ,
            fmStepKhz = DEFAULT_FM_STEP_KHZ,
            amStepKhz = DEFAULT_AM_STEP_KHZ,
            isTuning = false,
            isScanning = false,
            isMuted = false,
            program = null,
            presets = presets,
            errorMessage = null,
        )
    }
}
