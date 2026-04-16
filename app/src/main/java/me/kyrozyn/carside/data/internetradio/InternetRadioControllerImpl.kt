package me.kyrozyn.carside.data.internetradio

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.kyrozyn.carside.data.settings.AppSettingsRepository
import me.kyrozyn.carside.domain.model.InternetRadioState
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.domain.model.NowPlaying
import me.kyrozyn.carside.domain.model.SortOrder
import me.kyrozyn.carside.domain.repository.InternetRadioController

class InternetRadioControllerImpl(
    private val appContext: Context,
    private val settings: AppSettingsRepository,
    private val api: RadioBrowserApi = RadioBrowserApi(),
) : InternetRadioController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(
        InternetRadioState(
            favorites = settings.internetRadioFavorites.value,
        ),
    )
    override val state: StateFlow<InternetRadioState> = _state.asStateFlow()

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(appContext).build().apply {
            addListener(playerListener)
        }
    }

    private var refreshJob: Job? = null
    private var preMuteVolume: Float = 1f

    init {
        scope.launch {
            settings.internetRadioFavorites.collect { favs ->
                _state.value = _state.value.copy(favorites = favs)
            }
        }
    }

    override fun refresh() {
        refreshJob?.cancel()
        val sort = _state.value.sort
        _state.value = _state.value.copy(loading = true, errorMessage = null)
        refreshJob = scope.launch {
            runCatching { api.fetchIndonesianStations(sort = sort) }
                .onSuccess { stations ->
                    val tags = stations.asSequence()
                        .flatMap { it.tags.asSequence() }
                        .map { it.lowercase() }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(24)
                        .map { it.key }
                    _state.value = _state.value.copy(
                        loading = false,
                        stations = stations,
                        availableTags = tags,
                        errorMessage = null,
                    )
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        loading = false,
                        errorMessage = err.message ?: "Gagal memuat stasiun",
                    )
                }
        }
    }

    override fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    override fun setTag(tag: String?) {
        _state.value = _state.value.copy(tagFilter = tag)
    }

    override fun setSort(sort: SortOrder) {
        if (_state.value.sort == sort) return
        _state.value = _state.value.copy(sort = sort)
        refresh()
    }

    override fun play(station: InternetStation) {
        ensureOnMain {
            val item = MediaItem.Builder()
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .build(),
                )
                .build()
            player.setMediaItem(item)
            player.prepare()
            player.playWhenReady = true
            _state.value = _state.value.copy(
                playing = station,
                nowPlaying = NowPlaying(
                    stationName = station.name,
                    title = null,
                    artworkUrl = station.faviconUrl,
                ),
                isBuffering = true,
                errorMessage = null,
            )
        }
        scope.launch(Dispatchers.IO) {
            runCatching { api.reportClick(station.uuid) }
        }
    }

    override fun stop() {
        ensureOnMain {
            player.stop()
            player.clearMediaItems()
            _state.value = _state.value.copy(
                playing = null,
                nowPlaying = null,
                isBuffering = false,
                isPlaying = false,
            )
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        ensureOnMain {
            player.volume = clamped
            _state.value = _state.value.copy(
                volume = clamped,
                isMuted = clamped <= 0f,
            )
        }
    }

    override fun toggleMute() {
        ensureOnMain {
            val current = _state.value
            if (current.isMuted) {
                val restored = if (preMuteVolume > 0f) preMuteVolume else 1f
                player.volume = restored
                _state.value = current.copy(volume = restored, isMuted = false)
            } else {
                preMuteVolume = current.volume.takeIf { it > 0f } ?: 1f
                player.volume = 0f
                _state.value = current.copy(volume = 0f, isMuted = true)
            }
        }
    }

    override fun addFavorite(station: InternetStation) {
        settings.addInternetRadioFavorite(station)
    }

    override fun removeFavorite(uuid: String) {
        settings.removeInternetRadioFavorite(uuid)
    }

    override fun release() {
        ensureOnMain {
            runCatching { player.release() }
        }
        scope.cancel()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val current = _state.value
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.value = current.copy(isBuffering = true)
                }
                Player.STATE_READY -> {
                    _state.value = current.copy(isBuffering = false)
                }
                Player.STATE_ENDED, Player.STATE_IDLE -> {
                    _state.value = current.copy(isBuffering = false)
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                errorMessage = error.message ?: "Playback error",
                isBuffering = false,
                isPlaying = false,
            )
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val current = _state.value
            val station = current.playing ?: return
            val icyTitle = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
            val stationFallback = station.name
            _state.value = current.copy(
                nowPlaying = NowPlaying(
                    stationName = stationFallback,
                    title = icyTitle?.takeIf { it != stationFallback },
                    artworkUrl = station.faviconUrl,
                ),
            )
        }
    }

    private inline fun ensureOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            scope.launch { block() }
        }
    }
}
