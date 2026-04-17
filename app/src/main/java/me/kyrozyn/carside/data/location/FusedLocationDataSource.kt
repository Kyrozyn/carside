package me.kyrozyn.carside.data.location

import android.annotation.SuppressLint
import android.content.Context
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

    @SuppressLint("MissingPermission")
    override val locationUpdates: Flow<GeoLocation?> = callbackFlow {
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                trySend(
                    GeoLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        bearingDegrees = loc.bearing.takeIf { loc.hasBearing() },
                        speedMps = loc.speed.takeIf { loc.hasSpeed() },
                        accuracyMeters = loc.accuracy.takeIf { loc.hasAccuracy() },
                    ),
                )
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    GeoLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        bearingDegrees = loc.bearing.takeIf { loc.hasBearing() },
                        speedMps = loc.speed.takeIf { loc.hasSpeed() },
                        accuracyMeters = loc.accuracy.takeIf { loc.hasAccuracy() },
                    ),
                )
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }.distinctUntilChanged { old, new ->
        old?.latitude == new?.latitude && old?.longitude == new?.longitude
    }
}
