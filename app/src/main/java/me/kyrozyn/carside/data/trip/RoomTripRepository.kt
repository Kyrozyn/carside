package me.kyrozyn.carside.data.trip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import me.kyrozyn.carside.data.db.dao.TripDao
import me.kyrozyn.carside.data.db.dao.TripPointDao
import me.kyrozyn.carside.domain.model.Trip
import me.kyrozyn.carside.domain.model.TripPoint
import me.kyrozyn.carside.domain.model.TripTrackerState
import me.kyrozyn.carside.domain.repository.TripRepository

class RoomTripRepository(
    private val tripDao: TripDao,
    private val tripPointDao: TripPointDao,
    private val tripTracker: TripTracker,
) : TripRepository {

    override val activeTripState: StateFlow<TripTrackerState> = tripTracker.activeTripState

    override fun observeTrips(): Flow<List<Trip>> =
        tripDao.observeTrips().map { list -> list.map { it.toDomain() } }

    override fun observeTrip(tripId: Long): Flow<Trip?> =
        tripDao.observeTrip(tripId).map { it?.toDomain() }

    override fun observePoints(tripId: Long): Flow<List<TripPoint>> =
        tripPointDao.observePointsForTrip(tripId).map { list -> list.map { it.toDomain() } }

    override suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteTrip(tripId)
    }

    override suspend fun stopActiveTrip() {
        tripTracker.stopActiveTrip()
    }

    override fun setTrackingEnabled(enabled: Boolean) {
        tripTracker.setTrackingEnabled(enabled)
    }
}
