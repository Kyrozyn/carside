package me.kyrozyn.carside.domain.model

/**
 * Sumber aplikasi navigasi aktif yang dibaca Carside dari notifikasi sistem.
 */
enum class NavSource(val packageName: String, val displayName: String) {
    GoogleMaps("com.google.android.apps.maps", "Google Maps"),
    Waze("com.waze", "Waze"),
    Unknown("", "Navigasi");

    companion object {
        fun fromPackage(packageName: String?): NavSource = when (packageName) {
            GoogleMaps.packageName -> GoogleMaps
            Waze.packageName -> Waze
            else -> Unknown
        }
    }
}

/**
 * Snapshot state navigasi yang diturunkan dari notifikasi persistent Google Maps / Waze.
 *
 * Catatan: hanya teks + ikon manuver yang tersedia dari notifikasi; rute/polyline tidak ada.
 */
data class NavigationState(
    val active: Boolean,
    val sourceApp: NavSource,
    val maneuverText: String?,
    val roadText: String?,
    val distanceText: String?,
    val etaText: String?,
    val iconUri: String?,
) {
    companion object {
        val Idle = NavigationState(
            active = false,
            sourceApp = NavSource.Unknown,
            maneuverText = null,
            roadText = null,
            distanceText = null,
            etaText = null,
            iconUri = null,
        )
    }
}
