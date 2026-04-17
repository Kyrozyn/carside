package me.kyrozyn.carside.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.kyrozyn.carside.data.db.entity.FuelEntryEntity

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries ORDER BY filledAtEpochMs ASC, id ASC")
    fun observeEntries(): Flow<List<FuelEntryEntity>>

    @Insert
    suspend fun insert(entity: FuelEntryEntity): Long

    @Update
    suspend fun update(entity: FuelEntryEntity)

    @Delete
    suspend fun delete(entity: FuelEntryEntity)

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun getById(id: Long): FuelEntryEntity?

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
