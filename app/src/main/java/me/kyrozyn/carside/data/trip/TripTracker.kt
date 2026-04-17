package me.kyrozyn.carside.data.trip

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import me.kyrozyn.carside.core.geo.haversineMeters
import me.kyrozyn.carside.data.db.dao.TripDao
import me.kyrozyn.carside.data.db.entity.TripEntity
import me.kyrozyn.carside.data.db.entity.TripPointEntity
import me.kyrozyn.carside.domain.model.GeoLocation
import me.kyrozyn.carside.domain.model.TripTrackerPhase
import me.kyrozyn.carside.domain.model.TripTrackerState
import me.kyrozyn.carside.domain.repository.LocationProvider

/**
 * Deteksi trip otomatis: mulai setelah kecepatan ≥ ambang ~10 detik; berakhir setelah diam ~15 menit (macet).
 */
class TripTracker(
    private val locationProvider: LocationProvider,
    private val tripDao: TripDao,
    private val scope: CoroutineScope,
) {
    private val _activeTripState = MutableStateFlow(
        TripTrackerState(
            phase = TripTrackerPhase.Idle,
            currentDistanceMeters = 0.0,
            currentDurationSeconds = 0L,
            maxSpeedMps = null,
        ),
    )
    val activeTripState: StateFlow<TripTrackerState> = _activeTripState.asStateFlow()

    private var collectionJob: Job? = null
    private var trackingEnabled: Boolean = false

    private var phase: TripTrackerPhase = TripTrackerPhase.Idle
    private var armingStartMs: Long = 0L
    private var cooldownStartMs: Long = 0L
    private var tripStartMs: Long = 0L
    private var tripStartLat: Double = 0.0
    private var tripStartLon: Double = 0.0
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var distanceMeters: Double = 0.0
    private var maxSpeedMps: Float = 0f
    private val routePoints: MutableList<RoutePoint> = mutableListOf()

    private data class RoutePoint(
        val epochMs: Long,
        val lat: Double,
        val lon: Double,
        val speedMps: Float?,
    )

    fun setTrackingEnabled(enabled: Boolean) {
        trackingEnabled = enabled
        if (enabled) {
            startCollecting()
        } else {
            collectionJob?.cancel()
            collectionJob = null
            scope.launch {
                when (phase) {
                    TripTrackerPhase.Active, TripTrackerPhase.Cooldown -> stopActiveTrip()
                    else -> resetIdle()
                }
            }
        }
    }

    private fun startCollecting() {
        if (collectionJob?.isActive == true) return
        collectionJob = scope.launch {
            locationProvider.locationUpdates
                .catch { /* ignore */ }
                .collect { loc ->
                    if (loc != null) onLocation(loc)
                }
        }
    }

    private suspend fun resetIdle() {
        phase = TripTrackerPhase.Idle
        armingStartMs = 0L
        cooldownStartMs = 0L
        tripStartMs = 0L
        lastLat = null
        lastLon = null
        distanceMeters = 0.0
        maxSpeedMps = 0f
        routePoints.clear()
        emitState()
    }

    private suspend fun onLocation(loc: GeoLocation) {
        if (!trackingEnabled) return
        val acc = loc.accuracyMeters
        if (acc != null && acc > MAX_ACCURACY_METERS) return

        val now = System.currentTimeMillis()
        val speed = loc.speedMps ?: 0f
        val fastEnough = speed >= MIN_START_SPEED_MPS

        when (phase) {
            TripTrackerPhase.Idle -> {
                if (fastEnough) {
                    phase = TripTrackerPhase.Arming
                    armingStartMs = now
                }
                emitState()
            }
            TripTrackerPhase.Arming -> {
                if (!fastEnough) {
                    phase = TripTrackerPhase.Idle
                    emitState()
                    return
                }
                if (now - armingStartMs >= START_HOLD_MS) {
                    beginTrip(now, loc)
                    phase = TripTrackerPhase.Active
                    addPointIfNeeded(now, loc, speed)
                }
                emitState()
            }
            TripTrackerPhase.Active -> {
                if (!fastEnough) {
                    phase = TripTrackerPhase.Cooldown
                    cooldownStartMs = now
                } else {
                    addPointIfNeeded(now, loc, speed)
                }
                emitState()
            }
            TripTrackerPhase.Cooldown -> {
                if (fastEnough) {
                    phase = TripTrackerPhase.Active
                    addPointIfNeeded(now, loc, speed)
                } else if (now - cooldownStartMs >= STOP_HOLD_MS) {
                    endTrip(now, loc)
                    phase = TripTrackerPhase.Idle
                    clearTripBuffers()
                }
                emitState()
            }
        }
    }

    private fun beginTrip(now: Long, loc: GeoLocation) {
        tripStartMs = now
        tripStartLat = loc.latitude
        tripStartLon = loc.longitude
        lastLat = loc.latitude
        lastLon = loc.longitude
        distanceMeters = 0.0
        maxSpeedMps = loc.speedMps ?: 0f
        routePoints.clear()
        routePoints.add(
            RoutePoint(
                epochMs = now,
                lat = loc.latitude,
                lon = loc.longitude,
                speedMps = loc.speedMps,
            ),
        )
    }

    private fun addPointIfNeeded(now: Long, loc: GeoLocation, speed: Float) {
        val sp = loc.speedMps ?: speed
        if (sp > maxSpeedMps) maxSpeedMps = sp

        val prevLat = lastLat ?: return
        val prevLon = lastLon ?: return
        val d = haversineMeters(prevLat, prevLon, loc.latitude, loc.longitude)
        if (d >= MIN_POINT_DISTANCE_METERS) {
            distanceMeters += d
            lastLat = loc.latitude
            lastLon = loc.longitude
            routePoints.add(
                RoutePoint(
                    epochMs = now,
                    lat = loc.latitude,
                    lon = loc.longitude,
                    speedMps = loc.speedMps,
                ),
            )
        }
    }

    private suspend fun endTrip(now: Long, loc: GeoLocation) {
        if (tripStartMs == 0L) return
        val endLat = loc.latitude
        val endLon = loc.longitude
        val durationSec = ((now - tripStartMs) / 1000L).coerceAtLeast(1L)
        val avgSpeed = (distanceMeters / durationSec.toDouble()).toFloat()
        val trip = TripEntity(
            startedAtEpochMs = tripStartMs,
            endedAtEpochMs = now,
            distanceMeters = distanceMeters,
            durationSeconds = durationSec,
            maxSpeedMps = maxSpeedMps.takeIf { it > 0f },
            avgSpeedMps = avgSpeed.takeIf { it > 0f },
            startLat = tripStartLat,
            startLon = tripStartLon,
            endLat = endLat,
            endLon = endLon,
        )
        val points = routePoints.map {
            TripPointEntity(
                tripId = 0L,
                epochMs = it.epochMs,
                lat = it.lat,
                lon = it.lon,
                speedMps = it.speedMps,
            )
        }
        tripDao.insertTripWithPoints(trip, points)
    }

    private fun clearTripBuffers() {
        tripStartMs = 0L
        lastLat = null
        lastLon = null
        distanceMeters = 0.0
        maxSpeedMps = 0f
        routePoints.clear()
    }

    private fun emitState() {
        val now = System.currentTimeMillis()
        val durationSec = if (tripStartMs > 0L && (phase == TripTrackerPhase.Active || phase == TripTrackerPhase.Cooldown)) {
            (now - tripStartMs) / 1000L
        } else {
            0L
        }
        _activeTripState.value = TripTrackerState(
            phase = phase,
            currentDistanceMeters = distanceMeters,
            currentDurationSeconds = durationSec,
            maxSpeedMps = maxSpeedMps.takeIf { it > 0f },
        )
    }

    suspend fun stopActiveTrip() {
        if (phase != TripTrackerPhase.Active && phase != TripTrackerPhase.Cooldown) return
        val now = System.currentTimeMillis()
        val endLat = lastLat ?: tripStartLat
        val endLon = lastLon ?: tripStartLon
        endTrip(now, GeoLocation(latitude = endLat, longitude = endLon))
        phase = TripTrackerPhase.Idle
        clearTripBuffers()
        emitState()
    }

    private companion object {
        const val MIN_START_SPEED_MPS: Float = 1.4f
        const val START_HOLD_MS: Long = 10_000L
        /** Berhenti / pelan lama (mis. macet) sebelum trip dianggap selesai. */
        const val STOP_HOLD_MS: Long = 900_000L
        const val MIN_POINT_DISTANCE_METERS: Double = 5.0
        const val MAX_ACCURACY_METERS: Float = 50f
    }
}
