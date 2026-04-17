package me.kyrozyn.carside.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.FuelEconomySummary
import me.kyrozyn.carside.domain.model.FuelEntry

class GarageViewModel(
    private val container: AppContainer,
) : ViewModel() {

    val trips = container.tripRepository.observeTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTripState = container.tripRepository.activeTripState

    val fuelEntries = container.fuelRepository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val economy = container.fuelRepository.observeEconomy()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FuelEconomySummary(
                latestKmPerLiter = null,
                rollingAvgKmPerLiter = null,
                totalSpentIdr = 0L,
                totalLiters = 0.0,
                totalDistanceKm = null,
                pricePerKmIdr = null,
            ),
        )

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            container.tripRepository.deleteTrip(tripId)
        }
    }

    fun stopActiveTrip() {
        viewModelScope.launch {
            container.tripRepository.stopActiveTrip()
        }
    }

    fun addFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            container.fuelRepository.insert(entry)
        }
    }

    fun updateFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            container.fuelRepository.update(entry)
        }
    }

    fun deleteFuelEntry(id: Long) {
        viewModelScope.launch {
            container.fuelRepository.delete(id)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(GarageViewModel::class.java))
                    return GarageViewModel(container) as T
                }
            }
    }
}
