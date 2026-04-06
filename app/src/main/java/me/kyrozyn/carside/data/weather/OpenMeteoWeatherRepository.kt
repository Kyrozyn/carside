package me.kyrozyn.carside.data.weather

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kyrozyn.carside.domain.model.GeoLocation
import me.kyrozyn.carside.domain.model.WeatherHourlyPoint
import me.kyrozyn.carside.domain.model.WeatherSummary
import me.kyrozyn.carside.domain.repository.WeatherRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OpenMeteoWeatherRepository(
    private val httpClient: OkHttpClient = defaultClient(),
) : WeatherRepository {

    override suspend fun fetchCurrent(location: GeoLocation): Result<WeatherSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=${location.latitude}" +
                        "&longitude=${location.longitude}" +
                        "&current=temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m" +
                        "&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m" +
                        "&timezone=auto"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CarsideHeadUnit/1.0 (https://github.com/)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val current = root.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m")
                    val code = current.getInt("weather_code")
                    val windSpeed = current.optDouble("wind_speed_10m", Double.NaN)
                    val humidity = if (current.has("relative_humidity_2m")) {
                        current.optInt("relative_humidity_2m")
                    } else {
                        -1
                    }
                    val forecast = parseHourlyForecast(root).take(24)
                    WeatherSummary(
                        temperatureCelsius = temp,
                        weatherCode = code,
                        description = wmoDescription(code),
                        fetchedAt = Instant.now(),
                        windSpeedKmh = windSpeed.takeUnless { it.isNaN() },
                        humidityPercent = humidity.takeIf { it >= 0 },
                        timezone = root.optString("timezone").ifBlank { null },
                        elevationMeters = root.optDouble("elevation", Double.NaN).takeUnless { it.isNaN() },
                        forecastHourly = forecast,
                    )
                }
            }
        }

    private fun parseHourlyForecast(root: JSONObject): List<WeatherHourlyPoint> {
        val hourly = root.optJSONObject("hourly") ?: return emptyList()
        val times = hourly.optJSONArray("time") ?: return emptyList()
        val temps = hourly.optJSONArray("temperature_2m") ?: return emptyList()
        val humidities = hourly.optJSONArray("relative_humidity_2m") ?: return emptyList()
        val winds = hourly.optJSONArray("wind_speed_10m") ?: return emptyList()
        val size = minOf(times.length(), temps.length(), humidities.length(), winds.length())
        return List(size) { i ->
            WeatherHourlyPoint(
                timeIso = times.optString(i),
                temperatureCelsius = temps.optDouble(i),
                humidityPercent = humidities.optInt(i),
                windSpeedKmh = winds.optDouble(i),
            )
        }
    }

    private fun wmoDescription(code: Int): String = when (code) {
        0 -> "Cerah"
        1, 2, 3 -> "Berawan"
        45, 48 -> "Kabut"
        51, 53, 55 -> "Gerimis"
        56, 57 -> "Hujan membeku"
        61, 63, 65 -> "Hujan"
        66, 67 -> "Hujan beku"
        71, 73, 75 -> "Salju"
        77 -> "Butiran salju"
        80, 81, 82 -> "Hujan lebat"
        85, 86 -> "Salju lebat"
        95 -> "Badai"
        96, 99 -> "Badai + hujan es"
        else -> "Cuaca"
    }

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder().build()
    }
}
