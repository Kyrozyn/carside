package me.kyrozyn.carside.data.settings

/**
 * Pilihan engine penampil peta untuk Mode Auto (Google Maps).
 */
enum class AutoMapEngine {
    /**
     * MapLibre Engine (Google Tiles).
     * Sangat stabil, anti-blank, cepat, tidak memerlukan Google Play Services.
     * Sangat direkomendasikan untuk Head Unit Android aftermarket.
     */
    MAPLIBRE,

    /**
     * Google Maps SDK Resmi (com.google.android.gms.maps).
     * Memerlukan Google Play Services dan Maps SDK for Android diaktifkan di Google Cloud.
     */
    GOOGLE_MAPS_OFFICIAL,
}
