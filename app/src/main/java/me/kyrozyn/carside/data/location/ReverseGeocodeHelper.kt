package me.kyrozyn.carside.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Mengubah koordinat menjadi alamat/nama jalan (reverse geocoding).
 * Membutuhkan layanan Geocoder di perangkat (biasanya perlu jaringan).
 */
suspend fun reverseGeocodeStreetDescription(
    context: Context,
    latitude: Double,
    longitude: Double,
): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) {
                            cont.resume(addresses.firstOrNull()?.toReadableAddress())
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                },
            )
        }
    } else {
        try {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.toReadableAddress()
        } catch (_: Exception) {
            null
        }
    }
}

private fun Address.toReadableAddress(): String? {
    getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    return buildString {
        val street = when {
            !thoroughfare.isNullOrBlank() -> thoroughfare
            !featureName.isNullOrBlank() && featureName != subLocality -> featureName
            else -> null
        }
        if (!street.isNullOrBlank()) {
            append(street)
            if (!subThoroughfare.isNullOrBlank()) append(" $subThoroughfare")
        }
        if (!locality.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(locality)
        }
        if (!subAdminArea.isNullOrBlank() && subAdminArea != locality) {
            if (isNotEmpty()) append(", ")
            append(subAdminArea)
        }
        if (!countryName.isNullOrBlank()) {
            if (isNotEmpty()) append(", ")
            append(countryName)
        }
    }.trim().takeIf { it.isNotBlank() }
}
