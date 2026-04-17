package me.kyrozyn.carside.domain.repository

import kotlinx.coroutines.flow.Flow
import me.kyrozyn.carside.domain.model.FuelEconomySummary
import me.kyrozyn.carside.domain.model.FuelEntry

interface FuelRepository {
    fun observeEntries(): Flow<List<FuelEntry>>

    fun observeEconomy(): Flow<FuelEconomySummary>

    suspend fun insert(entry: FuelEntry): Long

    suspend fun update(entry: FuelEntry)

    suspend fun delete(entryId: Long)
}
