package me.kyrozyn.carside.domain.repository

import me.kyrozyn.carside.domain.model.LyricsState

interface LyricsRepository {
    suspend fun getLyrics(title: String, artist: String, durationSec: Long): LyricsState
}
