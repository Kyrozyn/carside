package me.kyrozyn.carside.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import me.kyrozyn.carside.core.geo.haversineMeters
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
import me.kyrozyn.carside.domain.model.LyricsState
import me.kyrozyn.carside.domain.model.NavigationState
import me.kyrozyn.carside.domain.model.TripTrackerPhase
import me.kyrozyn.carside.domain.model.TripTrackerState
import me.kyrozyn.carside.domain.model.WeatherSummary
import me.kyrozyn.carside.domain.repository.MediaPlaybackState

sealed interface WeatherUi {
    data object Idle : WeatherUi
    data object Loading : WeatherUi
    data class Ready(val summary: WeatherSummary) : WeatherUi
    data class Error(val message: String) : WeatherUi
}

enum class MapRotationMode {
    COMPASS,
    FORWARD_GPS,
}

data class DashboardUiState(
    val media: MediaPlaybackState,
    val weather: WeatherUi,
    val clockText: String,
    val compass: CompassHeading,
    val mapRotationMode: MapRotationMode,
    val mapBearingDegrees: Float,
    val shortcutPackages: List<String?>,
    val shortcutPickerSlot: Int?,
    val weatherDetailsOpen: Boolean,
    val browseOpen: Boolean,
    val locationPermissionGranted: Boolean,
    /** Lokasi terakhir dari Fused Location — untuk peta mengikuti posisi Anda. */
    val userLocation: GeoLocation?,
    /** Status trip otomatis (GPS). */
    val activeTrip: TripTrackerState,
    /** Snapshot navigasi dari notifikasi Google Maps / Waze. */
    val navigation: NavigationState,
    /** State lirik lagu yang sedang diputar. */
    val lyricsState: LyricsState,
    /** Apakah layar lirik fullscreen terbuka. */
    val lyricsFullscreenOpen: Boolean,
    /** Mode Auto (Google Maps bergaya Android Auto). */
    val autoModeEnabled: Boolean = false,
    /** Engine peta yang digunakan untuk Mode Auto (MapLibre vs Google Maps Resmi SDK). */
    val autoMapEngine: me.kyrozyn.carside.data.settings.AutoMapEngine = me.kyrozyn.carside.data.settings.AutoMapEngine.MAPLIBRE,
    /** Google Maps Platform API Key (untuk Maps SDK, Directions, dan Places). */
    val googleMapsApiKey: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _locationPermission = MutableStateFlow(
        ContextCompat.checkSelfPermission(
            container.appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                container.appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
    )
    private val _browseOpen = MutableStateFlow(false)
    private val _weather = MutableStateFlow<WeatherUi>(WeatherUi.Idle)
    private val _clock = MutableStateFlow(formatTime())
    private val _compass = MutableStateFlow(CompassHeading(0f, "N"))
    private val _isOnline = MutableStateFlow(false)
    private val _userLocation = MutableStateFlow<GeoLocation?>(null)
    private val _shortcutPickerSlot = MutableStateFlow<Int?>(null)
    private val _weatherDetailsOpen = MutableStateFlow(false)
    private var weatherFetchJob: Job? = null
    private var lastWeatherFetchLocation: GeoLocation? = null
    private var lastWeatherFetchAtMs: Long = 0L
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    private var lyricsFetchJob: Job? = null
    private val _lyricsFullscreenOpen = MutableStateFlow(false)

    private val effectiveLocation = _locationPermission.flatMapLatest { granted ->
        if (granted) container.locationProvider.locationUpdates else flowOf(null)
    }

    private val lastLocation = effectiveLocation
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mapRotationMode = container.settingsRepository.useGpsForwardMapBearing
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val shortcutPackages = container.settingsRepository.shortcutPackages
        .stateIn(viewModelScope, SharingStarted.Eagerly, List(4) { null })
    private val autoModeEnabled = container.settingsRepository.autoModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val autoMapEngine = container.settingsRepository.autoMapEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, me.kyrozyn.carside.data.settings.AutoMapEngine.MAPLIBRE)
    private val googleMapsApiKey = container.settingsRepository.googleMapsApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            container.mediaController.playbackState,
            _weather,
            _clock,
            container.navigationRepository.state,
        ) { media, weather, clock, nav ->
            Quadruple(media, weather, clock, nav)
        },
        combine(
            shortcutPackages,
            _shortcutPickerSlot,
            _weatherDetailsOpen,
            _lyricsState,
            _lyricsFullscreenOpen,
        ) { shortcuts, pickerSlot, weatherOpen, lyrics, lyricsFs ->
            Quintuple(shortcuts, pickerSlot, weatherOpen, lyrics, lyricsFs)
        },
        combine(_compass, _browseOpen, _locationPermission, mapRotationMode) { compass, browse, perm, useGps ->
            Quadruple(compass, browse, perm, useGps)
        },
        combine(_userLocation, container.tripRepository.activeTripState) { userLoc, activeTrip ->
            Pair(userLoc, activeTrip)
        },
        combine(autoModeEnabled, googleMapsApiKey, autoMapEngine) { autoEnabled, apiKey, engine ->
            Triple(autoEnabled, apiKey, engine)
        },
    ) { mediaClock, shortcutsState, compassBrowsePermMode, userLocActiveTrip, autoSettings ->
        val compass = compassBrowsePermMode.first
        val userLoc = userLocActiveTrip.first
        val activeTrip = userLocActiveTrip.second
        val mode = if (compassBrowsePermMode.fourth) MapRotationMode.FORWARD_GPS else MapRotationMode.COMPASS
        val mapBearing = when (mode) {
            MapRotationMode.COMPASS -> compass.degrees
            MapRotationMode.FORWARD_GPS -> gpsForwardBearingOrNull(userLoc) ?: compass.degrees
        }
        DashboardUiState(
            media = mediaClock.first,
            weather = mediaClock.second,
            clockText = mediaClock.third,
            compass = compass,
            mapRotationMode = mode,
            mapBearingDegrees = normalizeBearing(mapBearing),
            shortcutPackages = shortcutsState.first,
            shortcutPickerSlot = shortcutsState.second,
            weatherDetailsOpen = shortcutsState.third,
            browseOpen = compassBrowsePermMode.second,
            locationPermissionGranted = compassBrowsePermMode.third,
            userLocation = userLoc,
            activeTrip = activeTrip,
            navigation = mediaClock.fourth,
            lyricsState = shortcutsState.fourth,
            lyricsFullscreenOpen = shortcutsState.fifth,
            autoModeEnabled = autoSettings.first,
            googleMapsApiKey = autoSettings.second,
            autoMapEngine = autoSettings.third,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState(
            media = container.mediaController.playbackState.value,
            weather = WeatherUi.Idle,
            clockText = formatTime(),
            compass = CompassHeading(0f, "N"),
            mapRotationMode = MapRotationMode.COMPASS,
            mapBearingDegrees = 0f,
            shortcutPackages = List(4) { null },
            shortcutPickerSlot = null,
            weatherDetailsOpen = false,
            browseOpen = false,
            locationPermissionGranted = false,
            userLocation = null,
            activeTrip = TripTrackerState(
                phase = TripTrackerPhase.Idle,
                currentDistanceMeters = 0.0,
                currentDurationSeconds = 0L,
                maxSpeedMps = null,
            ),
            navigation = NavigationState.Idle,
            lyricsState = LyricsState.Idle,
            lyricsFullscreenOpen = false,
            autoModeEnabled = container.settingsRepository.autoModeEnabled.value,
            autoMapEngine = container.settingsRepository.autoMapEngine.value,
            googleMapsApiKey = container.settingsRepository.googleMapsApiKey.value,
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

        // Fetch lyrics when track changes, with debounce to avoid thrashing.
        viewModelScope.launch {
            var lastKey = ""
            container.mediaController.playbackState.collect { playback ->
                val title = playback.track.title
                val artist = playback.track.artist
                val key = "$title|$artist"
                if (key == lastKey || title.isBlank()) return@collect
                lastKey = key
                lyricsFetchJob?.cancel()
                lyricsFetchJob = launch {
                    _lyricsState.value = LyricsState.Loading
                    delay(LYRICS_DEBOUNCE_MS)
                    val durationSec = playback.durationMs / 1_000L
                    _lyricsState.value = container.lyricsRepository.getLyrics(title, artist, durationSec)
                }
            }
        }

        // Fetch cuaca saat lokasi berubah signifikan atau interval refresh terlewati.
        viewModelScope.launch {
            combine(
                effectiveLocation,
                container.networkStatusTracker.isOnline,
            ) { loc: GeoLocation?, online: Boolean -> loc to online }
                .collect { (loc, online) ->
                    if (!online || loc == null) return@collect
                    if (shouldFetchWeatherForLocation(loc, System.currentTimeMillis())) {
                        fetchWeather(loc)
                    }
                }
        }

        // Backup refresh: tetap update berkala meski kendaraan tidak bergerak jauh.
        viewModelScope.launch {
            while (isActive) {
                delay(10.minutes)
                val loc = lastLocation.value
                if (_isOnline.value && loc != null && shouldFetchWeatherForLocation(loc, System.currentTimeMillis())) {
                    fetchWeather(loc)
                }
            }
        }

        viewModelScope.launch {
            combine(
                effectiveLocation,
                container.settingsRepository.speedWarningEnabled,
            ) { loc: GeoLocation?, enabled: Boolean -> loc to enabled }
                .collect { (loc, enabled) ->
                    if (!enabled) {
                        if (loc?.speedMps != null) {
                            speedWarningWasAbove =
                                loc.speedMps!! * MPS_TO_KMH > SPEED_WARNING_THRESHOLD_KMH
                        }
                        return@collect
                    }
                    if (loc == null || loc.speedMps == null) return@collect
                    val speedKmh = loc.speedMps!! * MPS_TO_KMH
                    val now = System.currentTimeMillis()
                    val above = speedKmh > SPEED_WARNING_THRESHOLD_KMH
                    val crossedUp = above && !speedWarningWasAbove
                    speedWarningWasAbove = above
                    if (!crossedUp) return@collect
                    if (now - lastSpeedWarningAtMs < SPEED_WARNING_COOLDOWN_MS) return@collect
                    lastSpeedWarningAtMs = now
                    container.softSpeedTonePlayer.play()
                }
        }
    }

    private var speedWarningWasAbove = false
    private var lastSpeedWarningAtMs = 0L

    private fun formatTime(): String = LocalTime.now().format(timeFormatter)

    private fun fetchWeather(location: GeoLocation) {
        lastWeatherFetchLocation = location
        lastWeatherFetchAtMs = System.currentTimeMillis()
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
        container.tripRepository.setTrackingEnabled(granted)
    }

    fun setBrowseOpen(open: Boolean) {
        _browseOpen.value = open
    }

    fun setUseGpsForwardMapBearing(enabled: Boolean) {
        container.settingsRepository.setUseGpsForwardMapBearing(enabled)
    }

    fun openShortcutPicker(slot: Int) {
        if (slot !in 0..3) return
        _shortcutPickerSlot.value = slot
    }

    fun closeShortcutPicker() {
        _shortcutPickerSlot.value = null
    }

    fun setShortcutPackage(slot: Int, packageName: String?) {
        container.settingsRepository.setShortcutPackage(slot, packageName)
        _shortcutPickerSlot.value = null
    }

    fun openWeatherDetails() {
        _weatherDetailsOpen.value = true
    }

    fun closeWeatherDetails() {
        _weatherDetailsOpen.value = false
    }

    fun openLyricsFullscreen() {
        _lyricsFullscreenOpen.value = true
    }

    fun closeLyricsFullscreen() {
        _lyricsFullscreenOpen.value = false
    }

    fun resetOverlays() {
        _browseOpen.value = false
        _shortcutPickerSlot.value = null
        _weatherDetailsOpen.value = false
        _lyricsFullscreenOpen.value = false
    }

    fun onPlayPause() {
        val playing = container.mediaController.playbackState.value.isPlaying
        if (playing) container.mediaController.pause() else container.mediaController.play()
    }

    fun onNext() = container.mediaController.skipNext()
    fun onPrevious() = container.mediaController.skipPrevious()

    fun onSeekTo(positionMs: Long) = container.mediaController.seekTo(positionMs)

    fun stopActiveTrip() {
        viewModelScope.launch {
            container.tripRepository.stopActiveTrip()
        }
    }

    companion object {
        private const val WEATHER_REFRESH_DISTANCE_METERS = 1_000.0
        private const val WEATHER_REFRESH_INTERVAL_MS = 10 * 60 * 1_000L

        private const val MPS_TO_KMH = 3.6f
        private const val SPEED_WARNING_THRESHOLD_KMH = 90f
        private const val SPEED_WARNING_COOLDOWN_MS = 10 * 60 * 1_000L

        private const val LYRICS_DEBOUNCE_MS = 600L

        /** Saat di bawah ini, arah GPS cenderung noise saat kendaraan diam/pelan. */
        private const val GPS_BEARING_MIN_SPEED_MPS = 2.0f

        private fun gpsForwardBearingOrNull(location: GeoLocation?): Float? {
            location ?: return null
            val speed = location.speedMps ?: return null
            val bearing = location.bearingDegrees ?: return null
            if (speed < GPS_BEARING_MIN_SPEED_MPS) return null
            return bearing
        }

        private fun normalizeBearing(degrees: Float): Float {
            val mod = degrees % 360f
            return if (mod < 0f) mod + 360f else mod
        }

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
                    return DashboardViewModel(container) as T
                }
            }
    }

    fun setAutoMapEngine(engine: me.kyrozyn.carside.data.settings.AutoMapEngine) {
        container.settingsRepository.setAutoMapEngine(engine)
    }

    private fun shouldFetchWeatherForLocation(location: GeoLocation, nowMs: Long): Boolean {
        val lastLoc = lastWeatherFetchLocation
        if (lastLoc == null) return true
        val elapsedMs = nowMs - lastWeatherFetchAtMs
        if (elapsedMs >= WEATHER_REFRESH_INTERVAL_MS) return true
        val distance = haversineMeters(
            lastLoc.latitude,
            lastLoc.longitude,
            location.latitude,
            location.longitude,
        )
        return distance >= WEATHER_REFRESH_DISTANCE_METERS
    }

}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

