package me.kyrozyn.carside.domain.repository

import me.kyrozyn.carside.domain.model.GeoLocation
import me.kyrozyn.carside.domain.model.WeatherSummary

interface WeatherRepository {
    suspend fun fetchCurrent(location: GeoLocation): Result<WeatherSummary>
}
