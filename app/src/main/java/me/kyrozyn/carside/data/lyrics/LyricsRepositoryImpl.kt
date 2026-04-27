package me.kyrozyn.carside.data.lyrics

import me.kyrozyn.carside.domain.model.LyricsState
import me.kyrozyn.carside.domain.repository.LyricsRepository

class LyricsRepositoryImpl : LyricsRepository {

    private val api = LrcLibApi()

    // LRU-style cap: remove oldest entries when limit exceeded
    private val cache = object : LinkedHashMap<String, LyricsState>(CACHE_CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsState>?): Boolean =
            size > CACHE_CAPACITY
    }

    override suspend fun getLyrics(title: String, artist: String, durationSec: Long): LyricsState {
        val key = buildKey(title, artist)
        cache[key]?.let { return it }
        val result = api.fetchLyrics(title, artist, durationSec)
        cache[key] = result
        return result
    }

    private fun buildKey(title: String, artist: String): String =
        "${title.lowercase().trim()}|${artist.lowercase().trim()}"

    companion object {
        private const val CACHE_CAPACITY = 30
    }
}
