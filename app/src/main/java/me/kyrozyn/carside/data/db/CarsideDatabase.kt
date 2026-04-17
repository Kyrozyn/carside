package me.kyrozyn.carside.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.kyrozyn.carside.data.db.dao.FuelDao
import me.kyrozyn.carside.data.db.dao.TripDao
import me.kyrozyn.carside.data.db.dao.TripPointDao
import me.kyrozyn.carside.data.db.entity.FuelEntryEntity
import me.kyrozyn.carside.data.db.entity.TripEntity
import me.kyrozyn.carside.data.db.entity.TripPointEntity

@Database(
    entities = [TripEntity::class, TripPointEntity::class, FuelEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CarsideDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripPointDao(): TripPointDao
    abstract fun fuelDao(): FuelDao
}
