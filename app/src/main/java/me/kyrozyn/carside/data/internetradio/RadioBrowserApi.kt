package me.kyrozyn.carside.data.internetradio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.domain.model.SortOrder
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class RadioBrowserApi(
    private val userAgent: String = DEFAULT_USER_AGENT,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchIndonesianStations(
        sort: SortOrder = SortOrder.POPULAR,
        limit: Int = 500,
    ): List<InternetStation> = withContext(Dispatchers.IO) {
        val order = when (sort) {
            SortOrder.POPULAR -> "clickcount"
            SortOrder.MOST_VOTED -> "votes"
            SortOrder.RECENT -> "lastchangetime"
        }

        var lastError: Throwable? = null
        for (host in MIRRORS) {
            val url = "https://$host/json/stations/search".toHttpUrl().newBuilder()
                .addQueryParameter("countrycode", "ID")
                .addQueryParameter("hidebroken", "true")
                .addQueryParameter("order", order)
                .addQueryParameter("reverse", "true")
                .addQueryParameter("limit", limit.toString())
                .build()
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .get()
                .build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        lastError = RuntimeException("HTTP ${resp.code} dari $host")
                        return@use
                    }
                    val body = resp.body?.string().orEmpty()
                    return@withContext parseStations(body)
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: RuntimeException("Gagal mengambil daftar stasiun")
    }

    suspend fun reportClick(uuid: String): Boolean = withContext(Dispatchers.IO) {
        if (uuid.isBlank()) return@withContext false
        for (host in MIRRORS) {
            val url = "https://$host/json/url/$uuid"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .post(FormBody.Builder().build())
                .build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return@withContext true
                }
            } catch (_: Throwable) {
                // try next mirror
            }
        }
        false
    }

    private fun parseStations(body: String): List<InternetStation> {
        val arr = runCatching { JSONArray(body) }.getOrElse { return emptyList() }
        val out = ArrayList<InternetStation>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val uuid = obj.optString("stationuuid").takeIf { it.isNotBlank() } ?: continue
            val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
            val urlResolved = obj.optString("url_resolved")
                .takeIf { it.isNotBlank() }
                ?: obj.optString("url").takeIf { it.isNotBlank() }
                ?: continue
            val favicon = obj.optString("favicon").takeIf { it.isNotBlank() }
            val country = obj.optString("country").takeIf { it.isNotBlank() }
            val codec = obj.optString("codec").takeIf { it.isNotBlank() }
            val bitrate = obj.optInt("bitrate", 0)
            val votes = obj.optInt("votes", 0)
            val clickCount = obj.optInt("clickcount", 0)
            val tagsRaw = obj.optString("tags").orEmpty()
            val tags = tagsRaw.split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            out.add(
                InternetStation(
                    uuid = uuid,
                    name = name,
                    streamUrl = urlResolved,
                    faviconUrl = favicon,
                    tags = tags,
                    country = country,
                    bitrate = bitrate,
                    codec = codec,
                    votes = votes,
                    clickCount = clickCount,
                ),
            )
        }
        return out
    }

    companion object {
        const val DEFAULT_USER_AGENT = "Carside/1.0"
        private val MIRRORS = listOf(
            "de1.api.radio-browser.info",
            "fi1.api.radio-browser.info",
            "all.api.radio-browser.info",
        )
    }
}
