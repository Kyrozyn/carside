package me.kyrozyn.carside.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.ui.theme.AppThemePalette
import org.json.JSONArray
import org.json.JSONObject

class AppSettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _useGpsForwardMapBearing = MutableStateFlow(
        prefs.getBoolean(KEY_USE_GPS_FORWARD_MAP_BEARING, false),
    )

    val useGpsForwardMapBearing: StateFlow<Boolean> = _useGpsForwardMapBearing.asStateFlow()

    private val _speedWarningEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SPEED_WARNING_ENABLED, true),
    )
    val speedWarningEnabled: StateFlow<Boolean> = _speedWarningEnabled.asStateFlow()
    private val _shortcutPackages = MutableStateFlow(readShortcutPackages())
    val shortcutPackages: StateFlow<List<String?>> = _shortcutPackages.asStateFlow()

    private val _internetRadioFavorites = MutableStateFlow(readInternetRadioFavorites())
    val internetRadioFavorites: StateFlow<List<InternetStation>> = _internetRadioFavorites.asStateFlow()

    private val _selectedPalette = MutableStateFlow(
        prefs.getString(KEY_SELECTED_PALETTE, null)
            ?.let { runCatching { AppThemePalette.valueOf(it) }.getOrNull() }
            ?: AppThemePalette.DARK_BLUE,
    )
    val selectedPalette: StateFlow<AppThemePalette> = _selectedPalette.asStateFlow()

    private val _locationDotSize = MutableStateFlow(
        prefs.getFloat(KEY_LOCATION_DOT_SIZE, DEFAULT_LOCATION_DOT_SIZE)
    )
    val locationDotSize: StateFlow<Float> = _locationDotSize.asStateFlow()

    private val _locationRefreshIntervalMs = MutableStateFlow(
        prefs.getLong(KEY_LOCATION_REFRESH_INTERVAL_MS, DEFAULT_LOCATION_REFRESH_INTERVAL_MS)
    )
    val locationRefreshIntervalMs: StateFlow<Long> = _locationRefreshIntervalMs.asStateFlow()

    private val _customBluetoothMusicPackage = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_BT_MUSIC_PACKAGE, null)
    )
    val customBluetoothMusicPackage: StateFlow<String?> = _customBluetoothMusicPackage.asStateFlow()

    private val _customBluetoothMusicActivity = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_BT_MUSIC_ACTIVITY, null)
    )
    val customBluetoothMusicActivity: StateFlow<String?> = _customBluetoothMusicActivity.asStateFlow()

    private val _autoModeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_MODE_ENABLED, false),
    )
    val autoModeEnabled: StateFlow<Boolean> = _autoModeEnabled.asStateFlow()

    private val _autoMapEngine = MutableStateFlow(
        prefs.getString(KEY_AUTO_MAP_ENGINE, null)
            ?.let { runCatching { AutoMapEngine.valueOf(it) }.getOrNull() }
            ?: AutoMapEngine.MAPLIBRE,
    )
    val autoMapEngine: StateFlow<AutoMapEngine> = _autoMapEngine.asStateFlow()

    private val _googleMapsApiKey = MutableStateFlow(
        prefs.getString(KEY_GOOGLE_MAPS_API_KEY, null)?.ifBlank { null }
            ?: me.kyrozyn.carside.core.GoogleMapsHelper.getApiKey(context),
    )
    val googleMapsApiKey: StateFlow<String?> = _googleMapsApiKey.asStateFlow()

    fun setAutoModeEnabled(enabled: Boolean) {
        if (_autoModeEnabled.value == enabled) return
        _autoModeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_MODE_ENABLED, enabled).apply()
    }

    fun setAutoMapEngine(engine: AutoMapEngine) {
        if (_autoMapEngine.value == engine) return
        _autoMapEngine.value = engine
        prefs.edit().putString(KEY_AUTO_MAP_ENGINE, engine.name).apply()
    }

    fun setGoogleMapsApiKey(apiKey: String?) {
        val trimmed = apiKey?.trim()?.ifBlank { null }
        val effective = trimmed ?: me.kyrozyn.carside.core.GoogleMapsHelper.getApiKey(context)
        if (_googleMapsApiKey.value == effective) return
        _googleMapsApiKey.value = effective
        prefs.edit().putString(KEY_GOOGLE_MAPS_API_KEY, trimmed).apply()
        me.kyrozyn.carside.core.GoogleMapsHelper.applyApiKey(context, effective)
    }

    fun setCustomBluetoothMusic(packageName: String?, activityName: String?) {
        _customBluetoothMusicPackage.value = packageName
        _customBluetoothMusicActivity.value = activityName
        prefs.edit()
            .putString(KEY_CUSTOM_BT_MUSIC_PACKAGE, packageName)
            .putString(KEY_CUSTOM_BT_MUSIC_ACTIVITY, activityName)
            .apply()
    }

    fun setUseGpsForwardMapBearing(enabled: Boolean) {
        if (_useGpsForwardMapBearing.value == enabled) return
        _useGpsForwardMapBearing.value = enabled
        prefs.edit().putBoolean(KEY_USE_GPS_FORWARD_MAP_BEARING, enabled).apply()
    }

    fun setSpeedWarningEnabled(enabled: Boolean) {
        if (_speedWarningEnabled.value == enabled) return
        _speedWarningEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SPEED_WARNING_ENABLED, enabled).apply()
    }

    fun setSelectedPalette(palette: AppThemePalette) {
        if (_selectedPalette.value == palette) return
        _selectedPalette.value = palette
        prefs.edit().putString(KEY_SELECTED_PALETTE, palette.name).apply()
    }

    fun setLocationDotSize(size: Float) {
        if (_locationDotSize.value == size) return
        _locationDotSize.value = size
        prefs.edit().putFloat(KEY_LOCATION_DOT_SIZE, size).apply()
    }

    fun setLocationRefreshIntervalMs(intervalMs: Long) {
        if (_locationRefreshIntervalMs.value == intervalMs) return
        _locationRefreshIntervalMs.value = intervalMs
        prefs.edit().putLong(KEY_LOCATION_REFRESH_INTERVAL_MS, intervalMs).apply()
    }

    fun setShortcutPackage(slot: Int, packageName: String?) {
        if (slot !in 0 until SHORTCUT_SLOT_COUNT) return
        val current = _shortcutPackages.value.toMutableList()
        current[slot] = packageName
        _shortcutPackages.value = current
        prefs.edit()
            .putString(shortcutKey(slot), packageName)
            .apply()
    }

    fun addInternetRadioFavorite(station: InternetStation) {
        val current = _internetRadioFavorites.value
        if (current.any { it.uuid == station.uuid }) return
        val next = current + station
        _internetRadioFavorites.value = next
        persistInternetRadioFavorites(next)
    }

    fun removeInternetRadioFavorite(uuid: String) {
        val current = _internetRadioFavorites.value
        if (current.none { it.uuid == uuid }) return
        val next = current.filterNot { it.uuid == uuid }
        _internetRadioFavorites.value = next
        persistInternetRadioFavorites(next)
    }

    private fun readShortcutPackages(): List<String?> =
        List(SHORTCUT_SLOT_COUNT) { idx -> prefs.getString(shortcutKey(idx), null) }

    private fun shortcutKey(slot: Int): String = "${KEY_SHORTCUT_PREFIX}_$slot"

    private fun readInternetRadioFavorites(): List<InternetStation> {
        val raw = prefs.getString(KEY_INTERNET_RADIO_FAVORITES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val uuid = obj.optString("uuid").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val url = obj.optString("url").takeIf { it.isNotBlank() } ?: continue
                    val favicon = obj.optString("favicon").takeIf { it.isNotBlank() }
                    val codec = obj.optString("codec").takeIf { it.isNotBlank() }
                    val bitrate = obj.optInt("bitrate", 0)
                    val country = obj.optString("country").takeIf { it.isNotBlank() }
                    val votes = obj.optInt("votes", 0)
                    val clickCount = obj.optInt("clickCount", 0)
                    val tagsArr = obj.optJSONArray("tags")
                    val tags = if (tagsArr != null) {
                        buildList {
                            for (j in 0 until tagsArr.length()) {
                                val t = tagsArr.optString(j).takeIf { it.isNotBlank() }
                                if (t != null) add(t)
                            }
                        }
                    } else emptyList()
                    add(
                        InternetStation(
                            uuid = uuid,
                            name = name,
                            streamUrl = url,
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
            }
        }.getOrElse { emptyList() }
    }

    private fun persistInternetRadioFavorites(favorites: List<InternetStation>) {
        val arr = JSONArray()
        favorites.forEach { fav ->
            val obj = JSONObject()
                .put("uuid", fav.uuid)
                .put("name", fav.name)
                .put("url", fav.streamUrl)
                .put("favicon", fav.faviconUrl ?: "")
                .put("codec", fav.codec ?: "")
                .put("bitrate", fav.bitrate)
                .put("country", fav.country ?: "")
                .put("votes", fav.votes)
                .put("clickCount", fav.clickCount)
                .put("tags", JSONArray(fav.tags))
            arr.put(obj)
        }
        prefs.edit()
            .putString(KEY_INTERNET_RADIO_FAVORITES, arr.toString())
            .apply()
    }

    private companion object {
        private const val PREFS_NAME = "carside_settings"
        private const val KEY_USE_GPS_FORWARD_MAP_BEARING = "use_gps_forward_map_bearing"
        private const val KEY_SPEED_WARNING_ENABLED = "speed_warning_enabled"
        private const val KEY_SHORTCUT_PREFIX = "shortcut_package"
        private const val SHORTCUT_SLOT_COUNT = 4
        private const val KEY_INTERNET_RADIO_FAVORITES = "internet_radio_favorites"
        private const val KEY_SELECTED_PALETTE = "selected_palette"
        private const val KEY_LOCATION_DOT_SIZE = "location_dot_size"
        private const val DEFAULT_LOCATION_DOT_SIZE = 60f
        private const val KEY_LOCATION_REFRESH_INTERVAL_MS = "location_refresh_interval_ms"
        private const val DEFAULT_LOCATION_REFRESH_INTERVAL_MS = 60_000L
        private const val KEY_CUSTOM_BT_MUSIC_PACKAGE = "custom_bt_music_package"
        private const val KEY_CUSTOM_BT_MUSIC_ACTIVITY = "custom_bt_music_activity"
        private const val KEY_AUTO_MODE_ENABLED = "auto_mode_enabled"
        private const val KEY_AUTO_MAP_ENGINE = "auto_map_engine"
        private const val KEY_GOOGLE_MAPS_API_KEY = "google_maps_api_key"
    }
}
