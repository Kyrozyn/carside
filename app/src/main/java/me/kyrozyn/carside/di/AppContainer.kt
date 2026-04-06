package me.kyrozyn.carside.di

import android.content.Context
import me.kyrozyn.carside.data.compass.SensorCompassProvider
import me.kyrozyn.carside.data.connectivity.NetworkStatusTracker
import me.kyrozyn.carside.data.location.FusedLocationDataSource
import me.kyrozyn.carside.data.map.NoOpMapSession
import me.kyrozyn.carside.data.media.SystemMediaSessionRepository
import me.kyrozyn.carside.data.settings.AppSettingsRepository
import me.kyrozyn.carside.data.weather.OpenMeteoWeatherRepository
import me.kyrozyn.carside.domain.repository.CompassProvider
import me.kyrozyn.carside.domain.repository.LocationProvider
import me.kyrozyn.carside.domain.repository.MapSession
import me.kyrozyn.carside.domain.repository.MediaController
import me.kyrozyn.carside.domain.repository.WeatherRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val locationProvider: LocationProvider = FusedLocationDataSource(appContext)
    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
    val networkStatusTracker = NetworkStatusTracker(appContext)
    val mediaController: MediaController = SystemMediaSessionRepository(appContext)
    val compassProvider: CompassProvider = SensorCompassProvider(appContext)
    val mapSession: MapSession = NoOpMapSession()
    val settingsRepository = AppSettingsRepository(appContext)
}
