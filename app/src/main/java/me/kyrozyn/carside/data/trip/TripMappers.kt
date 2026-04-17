package me.kyrozyn.carside.data.trip

import me.kyrozyn.carside.data.db.entity.TripEntity
import me.kyrozyn.carside.data.db.entity.TripPointEntity
import me.kyrozyn.carside.domain.model.Trip
import me.kyrozyn.carside.domain.model.TripPoint

internal fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    maxSpeedMps = maxSpeedMps,
    avgSpeedMps = avgSpeedMps,
    startLat = startLat,
    startLon = startLon,
    endLat = endLat,
    endLon = endLon,
)

internal fun TripPointEntity.toDomain(): TripPoint = TripPoint(
    id = id,
    tripId = tripId,
    epochMs = epochMs,
    latitude = lat,
    longitude = lon,
    speedMps = speedMps,
)
