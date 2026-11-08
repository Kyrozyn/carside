package me.kyrozyn.carside.core

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kyrozyn.carside.domain.model.GeoLocation
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AutoPlaceResult(
    val name: String,
    val address: String,
    val latLng: LatLng,
)

data class AutoRouteStep(
    val instruction: String,
    val distanceText: String,
    val maneuver: String?,
    val latLng: LatLng,
)

data class AutoRouteResult(
    val points: List<LatLng>,
    val distanceText: String,
    val distanceMeters: Long,
    val durationText: String,
    val durationSeconds: Long,
    val startAddress: String,
    val endAddress: String,
    val summary: String = "",
    val steps: List<AutoRouteStep>,
)

object GoogleMapsHelper {

    private val httpClient = OkHttpClient.Builder().build()

    @Volatile
    private var cachedApiKey: String? = null

    /**
     * Membaca API Key dari AndroidManifest (ditambahkan saat build / GitHub Actions).
     */
    fun getApiKey(context: Context): String? {
        val cached = cachedApiKey
        if (!cached.isNullOrBlank()) return cached
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA,
                )
            }
            val key = appInfo.metaData?.getString("com.google.android.geo.API_KEY")?.trim()?.ifEmpty { null }
            cachedApiKey = key
            key
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Inisialisasi awal GoogleMapsHelper pada saat aplikasi dimulai.
     */
    fun init(context: Context) {
        val key = getApiKey(context)
        if (!key.isNullOrBlank()) {
            AppLogger.i("GoogleMaps", "API Key AndroidManifest terdeteksi: ${key.take(6)}...")
        } else {
            AppLogger.i("GoogleMaps", "API Key AndroidManifest belum terisi (kosong).")
        }
    }

    /**
     * Menyimpan API Key ke memory jika diberikan secara dinamis.
     */
    fun applyApiKey(context: Context, apiKey: String?) {
        if (!apiKey.isNullOrBlank()) {
            cachedApiKey = apiKey.trim()
        }
    }

    /**
     * Mencari tempat/alamat menggunakan Google Places Text Search API.
     * Menggunakan API Key dari AndroidManifest atau parameter, lalu fallback ke Android Geocoder jika kosong.
     */
    suspend fun searchPlaces(
        context: Context,
        query: String,
        location: GeoLocation?,
        apiKey: String? = null,
    ): List<AutoPlaceResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val effectiveKey = apiKey?.ifBlank { null } ?: getApiKey(context)
        AppLogger.i("PlacesSearch", "Mencari alamat: '$query' (API Key: ${if (effectiveKey.isNullOrBlank()) "Kosong" else "Ada"})")

        if (!effectiveKey.isNullOrBlank()) {
            val fromApi = searchPlacesWithGoogleApi(query, location, effectiveKey)
            if (fromApi.isNotEmpty()) return@withContext fromApi
        }

        // Fallback: Geocoder bawaan Android
        searchWithGeocoderFallback(context, query)
    }

    private fun searchPlacesWithGoogleApi(
        query: String,
        location: GeoLocation?,
        apiKey: String,
    ): List<AutoPlaceResult> {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val urlBuilder = StringBuilder(
            "https://maps.googleapis.com/maps/api/place/textsearch/json?query=$encodedQuery&language=id&key=$apiKey",
        )
        if (location != null) {
            urlBuilder.append("&location=${location.latitude},${location.longitude}&radius=50000")
        }

        val request = Request.Builder().url(urlBuilder.toString()).build()
        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                AppLogger.w("PlacesSearch", "Google Places API gagal: HTTP ${response.code} $body")
                return emptyList()
            }
            val json = JSONObject(body)
            val status = json.optString("status", "")
            if (status != "OK" && status != "ZERO_RESULTS") {
                AppLogger.e("PlacesSearch", "Google Places API status=$status, pesan: ${json.optString("error_message")}")
            }
            val resultsArr = json.optJSONArray("results") ?: return emptyList()

            val list = mutableListOf<AutoPlaceResult>()
            for (i in 0 until resultsArr.length()) {
                val item = resultsArr.optJSONObject(i) ?: continue
                val name = item.optString("name", "Tempat")
                val formattedAddress = item.optString("formatted_address", "")
                val geometry = item.optJSONObject("geometry")
                val locObj = geometry?.optJSONObject("location")
                if (locObj != null) {
                    val lat = locObj.optDouble("lat", 0.0)
                    val lng = locObj.optDouble("lng", 0.0)
                    if (lat != 0.0 && lng != 0.0) {
                        list.add(
                            AutoPlaceResult(
                                name = name,
                                address = formattedAddress.ifBlank { name },
                                latLng = LatLng(lat, lng),
                            ),
                        )
                    }
                }
            }
            AppLogger.i("PlacesSearch", "Ditemukan ${list.size} tempat via Google Places API")
            list
        } catch (e: Exception) {
            AppLogger.e("PlacesSearch", "Exception Google Places API: ${e.message}", e)
            emptyList()
        }
    }

    private fun searchWithGeocoderFallback(context: Context, query: String): List<AutoPlaceResult> {
        return try {
            AppLogger.i("PlacesSearch", "Mencari via Android Geocoder bawaan (offline/tanpa key)...")
            val geocoder = Geocoder(context, Locale("id", "ID"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 8) ?: emptyList()
            val list = addresses.mapNotNull { addr ->
                val title = addr.featureName ?: addr.thoroughfare ?: query
                val fullAddress = (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
                AutoPlaceResult(
                    name = title,
                    address = fullAddress.ifBlank { title },
                    latLng = LatLng(addr.latitude, addr.longitude),
                )
            }
            AppLogger.i("PlacesSearch", "Geocoder selesai: ${list.size} hasil")
            list
        } catch (e: Exception) {
            AppLogger.e("PlacesSearch", "Exception Geocoder: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Mengambil seluruh kemungkinan rute perjalanan menggunakan Google Directions API (alternatives=true).
     */
    suspend fun getRoutes(
        origin: LatLng,
        destination: LatLng,
        apiKey: String? = null,
    ): List<AutoRouteResult> = withContext(Dispatchers.IO) {
        val effectiveKey = apiKey?.ifBlank { null } ?: cachedApiKey
        if (effectiveKey.isNullOrBlank()) {
            AppLogger.w("Directions", "Tidak dapat meminta rute: Google Maps API Key kosong!")
            return@withContext emptyList()
        }

        AppLogger.i("Directions", "Meminta rute navigasi dari (${origin.latitude}, ${origin.longitude}) ke (${destination.latitude}, ${destination.longitude})")

        val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&mode=driving" +
            "&alternatives=true" +
            "&language=id" +
            "&key=$effectiveKey"

        val request = Request.Builder().url(url).build()
        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                AppLogger.e("Directions", "Directions API gagal: HTTP ${response.code} $body")
                return@withContext emptyList()
            }
            val json = JSONObject(body)
            val status = json.optString("status", "")
            if (status != "OK") {
                AppLogger.e("Directions", "Directions API status=$status, pesan: ${json.optString("error_message")}")
            }

            val routesArr = json.optJSONArray("routes")
            if (routesArr == null || routesArr.length() == 0) return@withContext emptyList()

            val routeResults = mutableListOf<AutoRouteResult>()

            for (i in 0 until routesArr.length()) {
                val route = routesArr.getJSONObject(i)
                val summary = route.optString("summary", "")
                val overviewPoly = route.optJSONObject("overview_polyline")?.optString("points") ?: ""
                val decodedPoints = decodePolyline(overviewPoly)

                val legs = route.optJSONArray("legs")
                if (legs == null || legs.length() == 0) continue
                val leg = legs.getJSONObject(0)

                val distanceText = leg.optJSONObject("distance")?.optString("text", "—") ?: "—"
                val distanceMeters = leg.optJSONObject("distance")?.optLong("value", 0L) ?: 0L
                val durationText = leg.optJSONObject("duration")?.optString("text", "—") ?: "—"
                val durationSeconds = leg.optJSONObject("duration")?.optLong("value", 0L) ?: 0L
                val startAddr = leg.optString("start_address", "")
                val endAddr = leg.optString("end_address", "")

                val stepsList = mutableListOf<AutoRouteStep>()
                val stepsArr = leg.optJSONArray("steps")
                if (stepsArr != null) {
                    for (s in 0 until stepsArr.length()) {
                        val stepObj = stepsArr.optJSONObject(s) ?: continue
                        val rawInstruction = stepObj.optString("html_instructions", "")
                        val cleanInstruction = rawInstruction
                            .replace("<[^>]*>".toRegex(), " ")
                            .replace("&nbsp;", " ")
                            .trim()
                        val stepDist = stepObj.optJSONObject("distance")?.optString("text", "") ?: ""
                        val maneuver = stepObj.optString("maneuver").takeIf { it.isNotBlank() }
                        val startLoc = stepObj.optJSONObject("start_location")
                        val stepLatLng = if (startLoc != null) {
                            LatLng(startLoc.optDouble("lat", 0.0), startLoc.optDouble("lng", 0.0))
                        } else LatLng(0.0, 0.0)

                        stepsList.add(
                            AutoRouteStep(
                                instruction = cleanInstruction,
                                distanceText = stepDist,
                                maneuver = maneuver,
                                latLng = stepLatLng,
                            ),
                        )
                    }
                }

                routeResults.add(
                    AutoRouteResult(
                        points = decodedPoints,
                        distanceText = distanceText,
                        distanceMeters = distanceMeters,
                        durationText = durationText,
                        durationSeconds = durationSeconds,
                        startAddress = startAddr,
                        endAddress = endAddr,
                        summary = summary,
                        steps = stepsList,
                    ),
                )
            }
            AppLogger.i("Directions", "Berhasil mendapatkan ${routeResults.size} rute.")
            routeResults
        } catch (e: Exception) {
            AppLogger.e("Directions", "Exception Directions API: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Mengambil rute perjalanan utama menggunakan Google Directions API.
     */
    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        apiKey: String?,
    ): AutoRouteResult? {
        return getRoutes(origin, destination, apiKey).firstOrNull()
    }

    /**
     * Algoritma dekode Google Polyline format (5-bit encoded).
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}
