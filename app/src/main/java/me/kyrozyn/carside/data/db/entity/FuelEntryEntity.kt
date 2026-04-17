package me.kyrozyn.carside.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filledAtEpochMs: Long,
    val odometerKm: Double,
    val liters: Double,
    val totalCostIdr: Long,
    val isFullTank: Boolean,
    val notes: String?,
)
