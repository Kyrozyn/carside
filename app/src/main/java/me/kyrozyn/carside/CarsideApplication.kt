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
        MapLibre.getInstance(this)
        OfflineManager.getInstance(this).setOfflineMapboxTileCountLimit(200_000L)
        initOsmdroid()
        initCoil()
        container = AppContainer(this)
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
