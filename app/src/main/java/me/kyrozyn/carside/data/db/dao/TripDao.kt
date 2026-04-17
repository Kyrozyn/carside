package me.kyrozyn.carside.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.kyrozyn.carside.data.db.entity.TripEntity
import me.kyrozyn.carside.data.db.entity.TripPointEntity

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startedAtEpochMs DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeTrip(id: Long): Flow<TripEntity?>

    @Insert
    suspend fun insertTrip(entity: TripEntity): Long

    @Insert
    suspend fun insertPoints(points: List<TripPointEntity>)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTrip(id: Long)

    @Transaction
    suspend fun insertTripWithPoints(trip: TripEntity, points: List<TripPointEntity>): Long {
        val tripId = insertTrip(trip)
        if (points.isNotEmpty()) {
            insertPoints(points.map { it.copy(tripId = tripId) })
        }
        return tripId
    }
}
