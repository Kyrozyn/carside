package me.kyrozyn.carside.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kyrozyn.carside.domain.model.LyricsState
import me.kyrozyn.carside.domain.model.SyncedLine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class LrcLibApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        durationSec: Long,
    ): LyricsState = withContext(Dispatchers.IO) {
        val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use LyricsState.NotFound
                val body = response.body?.string().orEmpty()
                parseResponse(body, durationSec)
            }
        } catch (_: Exception) {
            LyricsState.NotFound
        }
    }

    private fun parseResponse(body: String, durationSec: Long): LyricsState {
        val array = runCatching { JSONArray(body) }.getOrElse { return LyricsState.NotFound }
        if (array.length() == 0) return LyricsState.NotFound

        val best = pickBestMatch(array, durationSec) ?: return LyricsState.NotFound

        val instrumental = best.optBoolean("instrumental", false)
        if (instrumental) return LyricsState.NotFound

        val syncedRaw = best.optString("syncedLyrics").takeIf { it.isNotBlank() }
        val plainRaw = best.optString("plainLyrics").takeIf { it.isNotBlank() }

        val syncedLines = syncedRaw?.let { parseLrc(it) }

        return LyricsState.Found(
            plainLyrics = plainRaw,
            syncedLines = syncedLines,
        )
    }

    private fun pickBestMatch(array: JSONArray, durationSec: Long): JSONObject? {
        var best: JSONObject? = null
        var bestDiff = Long.MAX_VALUE

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val dur = obj.optDouble("duration", -1.0).toLong()
            val diff = abs(dur - durationSec)
            if (diff <= 2 && diff < bestDiff) {
                best = obj
                bestDiff = diff
            }
        }

        // Fall back to first result if no duration match within ±2s
        return best ?: array.optJSONObject(0)
    }

    private fun parseLrc(raw: String): List<SyncedLine>? {
        val lines = mutableListOf<SyncedLine>()
        for (line in raw.lines()) {
            val match = LRC_PATTERN.find(line) ?: continue
            val mm = match.groupValues[1].toLongOrNull() ?: continue
            val ss = match.groupValues[2].toLongOrNull() ?: continue
            val centStr = match.groupValues[3]
            val centMs = when (centStr.length) {
                2 -> (centStr.toLongOrNull() ?: 0L) * 10L
                3 -> centStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val timestampMs = mm * 60_000L + ss * 1_000L + centMs
            val text = match.groupValues[4].trim()
            lines.add(SyncedLine(timestampMs, text))
        }
        if (lines.isEmpty()) return null
        return lines.sortedBy { it.timestampMs }
    }

    companion object {
        private const val USER_AGENT = "Carside/1.0"
        private val LRC_PATTERN = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})](.*)$""")
    }
}
