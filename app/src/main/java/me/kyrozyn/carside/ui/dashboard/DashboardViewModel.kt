package me.kyrozyn.carside.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.CompassHeading
import me.kyrozyn.carside.domain.model.GeoLocation
import me.kyrozyn.carside.domain.model.WeatherSummary
import me.kyrozyn.carside.domain.repository.MediaPlaybackState

sealed interface WeatherUi {
    data object Idle : WeatherUi
    data object Loading : WeatherUi
    data class Ready(val summary: WeatherSummary) : WeatherUi
    data class Error(val message: String) : WeatherUi
}

data class DashboardUiState(
    val media: MediaPlaybackState,
    val weather: WeatherUi,
    val clockText: String,
    val compass: CompassHeading,
    val browseOpen: Boolean,
    val locationPermissionGranted: Boolean,
    /** Lokasi terakhir dari Fused Location — untuk peta mengikuti posisi Anda. */
    val userLocation: GeoLocation?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _locationPermission = MutableStateFlow(false)
    private val _browseOpen = MutableStateFlow(false)
    private val _weather = MutableStateFlow<WeatherUi>(WeatherUi.Idle)
    private val _clock = MutableStateFlow(formatTime())
    private val _compass = MutableStateFlow(CompassHeading(0f, "N"))
    private val _isOnline = MutableStateFlow(false)
    private val _userLocation = MutableStateFlow<GeoLocation?>(null)
    private var weatherFetchJob: Job? = null

    private val effectiveLocation = _locationPermission.flatMapLatest { granted ->
        if (granted) container.locationProvider.locationUpdates else flowOf(null)
    }

    private val lastLocation = effectiveLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            container.mediaController.playbackState,
            _weather,
            _clock,
        ) { media, weather, clock ->
            Triple(media, weather, clock)
        },
        combine(_compass, _browseOpen, _locationPermission) { compass, browse, perm ->
            Triple(compass, browse, perm)
        },
        _userLocation,
    ) { mediaClock, compassBrowsePerm, userLoc ->
        DashboardUiState(
            media = mediaClock.first,
            weather = mediaClock.second,
            clockText = mediaClock.third,
            compass = compassBrowsePerm.first,
            browseOpen = compassBrowsePerm.second,
            locationPermissionGranted = compassBrowsePerm.third,
            userLocation = userLoc,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState(
            media = container.mediaController.playbackState.value,
            weather = WeatherUi.Idle,
            clockText = formatTime(),
            compass = CompassHeading(0f, "N"),
            browseOpen = false,
            locationPermissionGranted = false,
            userLocation = null,
        ),
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _clock.value = formatTime()
            }
        }

        viewModelScope.launch {
            container.compassProvider.heading.collect { _compass.value = it }
        }

        viewModelScope.launch {
            container.networkStatusTracker.isOnline.collect { _isOnline.value = it }
        }

        viewModelScope.launch {
            effectiveLocation.collect { _userLocation.value = it }
        }

        // Fetch cuaca sekali saat pertama kali lokasi + jaringan tersedia
        viewModelScope.launch {
            val (loc, _) = combine(
                effectiveLocation,
                container.networkStatusTracker.isOnline,
            ) { loc: GeoLocation?, online: Boolean -> loc to online }
                .first { (loc, online) -> loc != null && online }
            fetchWeather(loc!!)
        }

        // Refresh cuaca setiap 10 menit menggunakan lokasi terbaru
        viewModelScope.launch {
            while (isActive) {
                delay(10.minutes)
                val loc = lastLocation.value
                if (_isOnline.value && loc != null) {
                    fetchWeather(loc)
                }
            }
        }
    }

    private fun formatTime(): String = LocalTime.now().format(timeFormatter)

    private fun fetchWeather(location: GeoLocation) {
        weatherFetchJob?.cancel()
        weatherFetchJob = viewModelScope.launch {
            _weather.value = WeatherUi.Loading
            container.weatherRepository.fetchCurrent(location).fold(
                onSuccess = { summary -> _weather.value = WeatherUi.Ready(summary) },
                onFailure = { e ->
                    _weather.value = WeatherUi.Error(e.message ?: "Cuaca gagal dimuat")
                },
            )
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermission.value = granted
    }

    fun setBrowseOpen(open: Boolean) {
        _browseOpen.value = open
    }

    fun onPlayPause() {
        val playing = container.mediaController.playbackState.value.isPlaying
        if (playing) container.mediaController.pause() else container.mediaController.play()
    }

    fun onNext() = container.mediaController.skipNext()
    fun onPrevious() = container.mediaController.skipPrevious()

    fun onSeekTo(positionMs: Long) = container.mediaController.seekTo(positionMs)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(container) as T
                }
            }
    }
}
