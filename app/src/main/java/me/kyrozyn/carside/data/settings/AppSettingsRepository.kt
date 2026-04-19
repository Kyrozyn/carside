package me.kyrozyn.carside.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.domain.model.RadioBand
import me.kyrozyn.carside.domain.model.RadioPreset
import me.kyrozyn.carside.domain.model.RadioState
import me.kyrozyn.carside.ui.theme.AppThemePalette
import org.json.JSONArray
import org.json.JSONObject

class AppSettingsRepository(context: Context) {
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

    private val _radioPresets = MutableStateFlow(readRadioPresets())
    val radioPresets: StateFlow<List<RadioPreset>> = _radioPresets.asStateFlow()

    private val _internetRadioFavorites = MutableStateFlow(readInternetRadioFavorites())
    val internetRadioFavorites: StateFlow<List<InternetStation>> = _internetRadioFavorites.asStateFlow()

    private val _selectedPalette = MutableStateFlow(
        prefs.getString(KEY_SELECTED_PALETTE, null)
            ?.let { runCatching { AppThemePalette.valueOf(it) }.getOrNull() }
            ?: AppThemePalette.DARK_BLUE,
    )
    val selectedPalette: StateFlow<AppThemePalette> = _selectedPalette.asStateFlow()

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

    fun setShortcutPackage(slot: Int, packageName: String?) {
        if (slot !in 0 until SHORTCUT_SLOT_COUNT) return
        val current = _shortcutPackages.value.toMutableList()
        current[slot] = packageName
        _shortcutPackages.value = current
        prefs.edit()
            .putString(shortcutKey(slot), packageName)
            .apply()
    }

    fun setRadioPreset(preset: RadioPreset) {
        if (preset.slot !in 0 until RadioState.PRESET_SLOT_COUNT) return
        val current = _radioPresets.value.filter { it.slot != preset.slot } + preset
        val sorted = current.sortedBy { it.slot }
        _radioPresets.value = sorted
        prefs.edit()
            .putString(radioPresetKey(preset.slot), serializePreset(preset))
            .apply()
    }

    fun clearRadioPreset(slot: Int) {
        if (slot !in 0 until RadioState.PRESET_SLOT_COUNT) return
        _radioPresets.value = _radioPresets.value.filter { it.slot != slot }
        prefs.edit()
            .remove(radioPresetKey(slot))
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

    private fun readRadioPresets(): List<RadioPreset> =
        (0 until RadioState.PRESET_SLOT_COUNT).mapNotNull { slot ->
            val raw = prefs.getString(radioPresetKey(slot), null) ?: return@mapNotNull null
            deserializePreset(slot, raw)
        }

    private fun shortcutKey(slot: Int): String = "${KEY_SHORTCUT_PREFIX}_$slot"
    private fun radioPresetKey(slot: Int): String = "${KEY_RADIO_PRESET_PREFIX}_$slot"

    private fun serializePreset(preset: RadioPreset): String {
        val label = preset.label.orEmpty().replace("|", " ")
        return "${preset.band.name}|${preset.frequencyKhz}|$label"
    }

    private fun deserializePreset(slot: Int, raw: String): RadioPreset? {
        val parts = raw.split('|', limit = 3)
        if (parts.size < 2) return null
        val band = runCatching { RadioBand.valueOf(parts[0]) }.getOrNull() ?: return null
        val freq = parts[1].toIntOrNull() ?: return null
        val label = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
        return RadioPreset(slot = slot, frequencyKhz = freq, band = band, label = label)
    }

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
        private const val KEY_RADIO_PRESET_PREFIX = "radio_preset"
        private const val KEY_INTERNET_RADIO_FAVORITES = "internet_radio_favorites"
        private const val KEY_SELECTED_PALETTE = "selected_palette"
    }
}
