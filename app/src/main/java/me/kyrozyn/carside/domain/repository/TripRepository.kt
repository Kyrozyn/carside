package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.kyrozyn.carside.domain.model.Trip
import me.kyrozyn.carside.domain.model.TripPoint
import me.kyrozyn.carside.domain.model.TripTrackerState

interface TripRepository {
    val activeTripState: StateFlow<TripTrackerState>

    fun observeTrips(): Flow<List<Trip>>

    fun observeTrip(tripId: Long): Flow<Trip?>

    fun observePoints(tripId: Long): Flow<List<TripPoint>>

    suspend fun deleteTrip(tripId: Long)

    /** Hentikan trip aktif segera (override auto-stop). */
    suspend fun stopActiveTrip()

    fun setTrackingEnabled(enabled: Boolean)
}
