package me.kyrozyn.carside.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maxSpeedMps: Float?,
    val avgSpeedMps: Float?,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
)
