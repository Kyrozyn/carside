package me.kyrozyn.carside.domain.model

data class SyncedLine(val timestampMs: Long, val text: String)

sealed class LyricsState {
    data object Idle : LyricsState()
    data object Loading : LyricsState()
    data object NotFound : LyricsState()
    data class Found(
        val plainLyrics: String?,
        val syncedLines: List<SyncedLine>?,
    ) : LyricsState()
}
