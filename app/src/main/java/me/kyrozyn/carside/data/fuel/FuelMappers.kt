package me.kyrozyn.carside.data.fuel

import me.kyrozyn.carside.data.db.entity.FuelEntryEntity
import me.kyrozyn.carside.domain.model.FuelEntry

internal fun FuelEntryEntity.toDomain(): FuelEntry = FuelEntry(
    id = id,
    filledAtEpochMs = filledAtEpochMs,
    odometerKm = odometerKm,
    liters = liters,
    totalCostIdr = totalCostIdr,
    isFullTank = isFullTank,
    notes = notes,
)

internal fun FuelEntry.toEntity(): FuelEntryEntity = FuelEntryEntity(
    id = id,
    filledAtEpochMs = filledAtEpochMs,
    odometerKm = odometerKm,
    liters = liters,
    totalCostIdr = totalCostIdr,
    isFullTank = isFullTank,
    notes = notes,
)
