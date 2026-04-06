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
    private val _shortcutPackages = MutableStateFlow(readShortcutPackages())
    val shortcutPackages: StateFlow<List<String?>> = _shortcutPackages.asStateFlow()

    fun setUseGpsForwardMapBearing(enabled: Boolean) {
        if (_useGpsForwardMapBearing.value == enabled) return
        _useGpsForwardMapBearing.value = enabled
        prefs.edit().putBoolean(KEY_USE_GPS_FORWARD_MAP_BEARING, enabled).apply()
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

    private fun readShortcutPackages(): List<String?> =
        List(SHORTCUT_SLOT_COUNT) { idx -> prefs.getString(shortcutKey(idx), null) }

    private fun shortcutKey(slot: Int): String = "${KEY_SHORTCUT_PREFIX}_$slot"

    private companion object {
        private const val PREFS_NAME = "carside_settings"
        private const val KEY_USE_GPS_FORWARD_MAP_BEARING = "use_gps_forward_map_bearing"
        private const val KEY_SHORTCUT_PREFIX = "shortcut_package"
        private const val SHORTCUT_SLOT_COUNT = 4
    }
}
