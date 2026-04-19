package me.kyrozyn.carside.di

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.kyrozyn.carside.data.compass.GpsCompassProvider
import me.kyrozyn.carside.data.connectivity.NetworkStatusTracker
import me.kyrozyn.carside.data.internetradio.InternetRadioControllerImpl
import me.kyrozyn.carside.data.offline.OfflineMapsRepository
import me.kyrozyn.carside.data.location.FusedLocationDataSource
import me.kyrozyn.carside.data.map.NoOpMapSession
import me.kyrozyn.carside.data.media.SystemMediaSessionRepository
import me.kyrozyn.carside.data.navigation.NavigationRepository
import me.kyrozyn.carside.data.radio.HardwareRadioController
import me.kyrozyn.carside.data.radio.NoOpRadioController
import me.kyrozyn.carside.data.db.CarsideDatabase
import me.kyrozyn.carside.data.fuel.RoomFuelRepository
import me.kyrozyn.carside.data.alert.SoftSpeedTonePlayer
import me.kyrozyn.carside.data.settings.AppSettingsRepository
import me.kyrozyn.carside.data.trip.RoomTripRepository
import me.kyrozyn.carside.data.trip.TripTracker
import me.kyrozyn.carside.data.weather.OpenMeteoWeatherRepository
import me.kyrozyn.carside.domain.repository.CompassProvider
import me.kyrozyn.carside.domain.repository.InternetRadioController
import me.kyrozyn.carside.domain.repository.LocationProvider
import me.kyrozyn.carside.domain.repository.FuelRepository
import me.kyrozyn.carside.domain.repository.MapSession
import me.kyrozyn.carside.domain.repository.MediaController
import me.kyrozyn.carside.domain.repository.RadioController
import me.kyrozyn.carside.domain.repository.TripRepository
import me.kyrozyn.carside.domain.repository.WeatherRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val locationProvider: LocationProvider = FusedLocationDataSource(appContext)
    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
    val networkStatusTracker = NetworkStatusTracker(appContext)
    val mediaController: MediaController = SystemMediaSessionRepository(appContext)
    val navigationRepository = NavigationRepository(appContext)
    val compassProvider: CompassProvider = GpsCompassProvider(locationProvider)
    val mapSession: MapSession = NoOpMapSession()
    val settingsRepository = AppSettingsRepository(appContext)
    val softSpeedTonePlayer = SoftSpeedTonePlayer(appContext)
    val offlineMapsRepository = OfflineMapsRepository(appContext)
    val radioController: RadioController = runCatching {
        HardwareRadioController(appContext, settingsRepository)
    }.getOrElse {
        NoOpRadioController(
            initialPresets = settingsRepository.radioPresets.value,
            reason = "Radio tidak tersedia: ${it.message}",
        )
    }
    val internetRadioController: InternetRadioController =
        InternetRadioControllerImpl(appContext, settingsRepository)

    private val database: CarsideDatabase = Room.databaseBuilder(
        appContext,
        CarsideDatabase::class.java,
        "carside.db",
    ).fallbackToDestructiveMigration(true).build()

    private val tripTrackerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tripTracker = TripTracker(locationProvider, database.tripDao(), tripTrackerScope)

    val tripRepository: TripRepository = RoomTripRepository(
        database.tripDao(),
        database.tripPointDao(),
        tripTracker,
    )

    val fuelRepository: FuelRepository = RoomFuelRepository(database.fuelDao())
}
