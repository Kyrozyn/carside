package me.kyrozyn.carside

import android.annotation.SuppressLint
import android.app.Application
import coil.Coil
import coil.ImageLoader
import me.kyrozyn.carside.di.AppContainer
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineManager
import org.osmdroid.config.Configuration

class CarsideApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        me.kyrozyn.carside.core.AppLogger.i("App", "Carside Application dimulai")
        MapLibre.getInstance(this)
        OfflineManager.getInstance(this).setOfflineMapboxTileCountLimit(200_000L)
        initOsmdroid()
        initCoil()
        container = AppContainer(this)
        me.kyrozyn.carside.core.GoogleMapsHelper.init(this)
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            me.kyrozyn.carside.core.AppLogger.e("Crash", "Uncaught Exception pada thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initCoil() {
        val http = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 Carside/1.0",
                        )
                        .build(),
                )
            }
            .build()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(http)
                .build(),
        )
    }

    @SuppressLint("DiscouragedApi")
    private fun initOsmdroid() {
        val prefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
        @Suppress("DEPRECATION")
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        Configuration.getInstance().userAgentValue =
            "$packageName/$versionName (Carside; OpenStreetMap — https://www.openstreetmap.org/copyright)"
    }
}
