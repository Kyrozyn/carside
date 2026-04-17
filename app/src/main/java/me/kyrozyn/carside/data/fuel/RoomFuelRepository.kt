package me.kyrozyn.carside.data.fuel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.kyrozyn.carside.data.db.dao.FuelDao
import me.kyrozyn.carside.domain.model.FuelEconomySummary
import me.kyrozyn.carside.domain.model.FuelEntry
import me.kyrozyn.carside.domain.repository.FuelRepository

class RoomFuelRepository(
    private val fuelDao: FuelDao,
) : FuelRepository {

    override fun observeEntries(): Flow<List<FuelEntry>> =
        fuelDao.observeEntries().map { list -> list.map { it.toDomain() } }

    override fun observeEconomy(): Flow<FuelEconomySummary> =
        fuelDao.observeEntries().map { entities ->
            computeFuelEconomySummary(entities.map { it.toDomain() })
        }

    override suspend fun insert(entry: FuelEntry): Long {
        require(entry.id == 0L) { "insert requires id=0" }
        return fuelDao.insert(entry.toEntity())
    }

    override suspend fun update(entry: FuelEntry) {
        require(entry.id != 0L)
        fuelDao.update(entry.toEntity())
    }

    override suspend fun delete(entryId: Long) {
        fuelDao.deleteById(entryId)
    }
}
