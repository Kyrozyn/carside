package me.kyrozyn.carside.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.kyrozyn.carside.domain.model.GeoLocation
import me.kyrozyn.carside.domain.repository.LocationProvider

class FusedLocationDataSource(context: Context) : LocationProvider {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @SuppressLint("MissingPermission")
    override val locationUpdates: Flow<GeoLocation?> = callbackFlow {
        var prevLocation: Location? = null
        var prevTimeMs: Long = 0L

        fun emitLocation(loc: Location) {
            val nowMs = System.currentTimeMillis()
            val prev = prevLocation
            val dist = if (prev != null) prev.distanceTo(loc) else 0f
            val dtSec = if (prevTimeMs > 0L) ((nowMs - prevTimeMs) / 1000f).coerceAtLeast(0.1f) else 0f

            val computedBearing = when {
                loc.hasBearing() && loc.bearing != 0f -> (loc.bearing + 360f) % 360f
                prev != null && dist >= 1.5f -> (prev.bearingTo(loc) + 360f) % 360f
                loc.hasBearing() -> (loc.bearing + 360f) % 360f
                else -> null
            }

            val computedSpeed = when {
                loc.hasSpeed() && loc.speed > 0f -> loc.speed
                prev != null && dtSec in 0.2f..15f && dist >= 1.0f -> (dist / dtSec)
                loc.hasSpeed() -> loc.speed
                else -> null
            }

            if (prev == null || dist >= 0.5f) {
                prevLocation = Location(loc)
                prevTimeMs = nowMs
            }

            trySend(
                GeoLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    bearingDegrees = computedBearing,
                    speedMps = computedSpeed,
                    accuracyMeters = loc.accuracy.takeIf { loc.hasAccuracy() },
                ),
            )
        }

        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) emitLocation(loc)
        }

        runCatching {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { emitLocation(it) }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                emitLocation(loc)
            }
        }

        val gpsListener = LocationListener { loc ->
            emitLocation(loc)
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        runCatching {
            if (locationManager?.allProviders?.contains(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    gpsListener,
                    Looper.getMainLooper(),
                )
            }
        }

        awaitClose {
            client.removeLocationUpdates(callback)
            runCatching { locationManager?.removeUpdates(gpsListener) }
        }
    }.distinctUntilChanged { old, new ->
        old?.latitude == new?.latitude &&
            old?.longitude == new?.longitude &&
            old?.speedMps == new?.speedMps &&
            old?.bearingDegrees == new?.bearingDegrees
    }
}
