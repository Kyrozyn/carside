package me.kyrozyn.carside.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _useGpsForwardMapBearing = MutableStateFlow(
        prefs.getBoolean(KEY_USE_GPS_FORWARD_MAP_BEARING, false),
    )

    val useGpsForwardMapBearing: StateFlow<Boolean> = _useGpsForwardMapBearing.asStateFlow()

    fun setUseGpsForwardMapBearing(enabled: Boolean) {
        if (_useGpsForwardMapBearing.value == enabled) return
        _useGpsForwardMapBearing.value = enabled
        prefs.edit().putBoolean(KEY_USE_GPS_FORWARD_MAP_BEARING, enabled).apply()
    }

    private companion object {
        private const val PREFS_NAME = "carside_settings"
        private const val KEY_USE_GPS_FORWARD_MAP_BEARING = "use_gps_forward_map_bearing"
    }
}
