package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.StateFlow
import me.kyrozyn.carside.domain.model.TrackInfo

interface MediaController {
    val playbackState: StateFlow<MediaPlaybackState>

    fun play()
    fun pause()
    fun skipNext()
    fun skipPrevious()

    fun seekTo(positionMs: Long)

    /** Scan ulang sesi media aktif (panggil setelah user mengaktifkan Notification Listener, atau di onResume). */
    fun refreshActiveSessions()
}

data class MediaPlaybackState(
    val track: TrackInfo,
    val isPlaying: Boolean,
    /** Nama aplikasi pemutar (mis. Spotify, YouTube Music). */
    val sourceLabel: String,
    /** Package pemutar aktif — untuk ikon aplikasi. */
    val sourcePackageName: String,
    val hasActiveSession: Boolean,
    val canPlay: Boolean,
    val canPause: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
    val canSeek: Boolean,
    /** Posisi terakhir dari [PlaybackState] (ms). */
    val positionMs: Long,
    /** Durasi trek dari metadata (ms), 0 jika tidak diketahui. */
    val durationMs: Long,
    /** [android.os.SystemClock.elapsedRealtime] saat [positionMs] diperbarui. */
    val positionAnchorElapsedRealtimeMs: Long,
)
