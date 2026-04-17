package me.kyrozyn.carside.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.kyrozyn.carside.data.db.entity.TripPointEntity

@Dao
interface TripPointDao {
    @Query("SELECT * FROM trip_points WHERE tripId = :tripId ORDER BY epochMs ASC")
    fun observePointsForTrip(tripId: Long): Flow<List<TripPointEntity>>
}
