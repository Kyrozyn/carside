package me.kyrozyn.carside.ui.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale
import me.kyrozyn.carside.core.BluetoothMusicHelper

object InstalledAppsLoader {

    private data class CarSystemAppDef(
        val packageName: String,
        val activityName: String?,
        val label: String,
    )

    private val KNOWN_CAR_SYSTEM_APPS = listOf(
        // Bluetooth & Bluetooth Music (Head Unit Cina / Audiobank)
        CarSystemAppDef("com.ts.bt", "com.ts.bt.BtMusicActivity", "BT Music"),
        CarSystemAppDef("com.ts.MainUI", "com.ts.bt.BtMusicActivity", "BT Music"),
        CarSystemAppDef("com.ts.bt", null, "Bluetooth"),
        CarSystemAppDef("com.autochips.bluetooth", "com.autochips.bluetooth.BtMusicActivity", "BT Music"),
        CarSystemAppDef("com.autochips.bluetooth", "com.autochips.bluetooth.BluetoothActivity", "Bluetooth"),
        CarSystemAppDef("com.autochips.bluetooth", null, "Bluetooth"),
        CarSystemAppDef("com.android.ecar.bmusic", null, "BT Music"),
        CarSystemAppDef("com.allwinnertech.btmusic", null, "BT Music"),
        CarSystemAppDef("com.softwinner.bluetooth", "com.softwinner.bluetooth.BtMusic", "BT Music"),
        CarSystemAppDef("com.softwinner.bluetooth", null, "Bluetooth"),
        CarSystemAppDef("com.syu.bt", "com.syu.bt.BtAct", "BT Music"),
        CarSystemAppDef("com.syu.bt", "com.syu.app.PhoneActivity", "Bluetooth"),
        CarSystemAppDef("com.syu.bt", null, "Bluetooth"),
        CarSystemAppDef("com.microntek.btmusic", null, "BT Music"),
        CarSystemAppDef("com.microntek.bluetooth", null, "Bluetooth"),
        CarSystemAppDef("com.bt.btmusic", null, "BT Music"),
        CarSystemAppDef("com.car.btmusic", null, "BT Music"),

        // Radio FM bawaan Head Unit
        CarSystemAppDef("com.ts.radio", null, "Radio FM"),
        CarSystemAppDef("com.autochips.radio", null, "Radio FM"),
        CarSystemAppDef("com.syu.radio", null, "Radio FM"),
        CarSystemAppDef("com.microntek.radio", null, "Radio FM"),
        CarSystemAppDef("com.softwinner.radio", null, "Radio FM"),

        // Equalizer / DSP bawaan Head Unit
        CarSystemAppDef("com.ts.eq", null, "Equalizer"),
        CarSystemAppDef("com.autochips.sound", null, "Equalizer DSP"),
        CarSystemAppDef("com.syu.eq", null, "Equalizer"),

        // Pengaturan Mobil / Car Settings
        CarSystemAppDef("com.ts.carsetting", null, "Car Settings"),
        CarSystemAppDef("com.autochips.settings", null, "Car Settings"),
        CarSystemAppDef("com.syu.settings", null, "Car Settings"),
    )

    fun loadLaunchableApps(packageManager: PackageManager, context: Context? = null): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        @Suppress("DEPRECATION")
        val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }

        val apps = mutableListOf<LaunchableApp>()
        val seenTargetIds = mutableSetOf<String>()

        for (ri in resolves) {
            val pkg = ri.activityInfo.packageName
            val cls = ri.activityInfo.name
            val label = ri.loadLabel(packageManager).toString()
            val targetId = pkg
            if (seenTargetIds.add(targetId)) {
                apps.add(
                    LaunchableApp(
                        packageName = pkg,
                        activityName = cls,
                        label = label,
                        isCarSystemApp = false,
                    ),
                )
            }
        }

        // Tambahkan aplikasi sistem head unit yang mungkin tidak memiliki CATEGORY_LAUNCHER
        for (carApp in KNOWN_CAR_SYSTEM_APPS) {
            val isInstalled = runCatching {
                packageManager.getPackageInfo(carApp.packageName, 0)
            }.isSuccess

            if (isInstalled) {
                var actualActivity = carApp.activityName
                if (!carApp.activityName.isNullOrBlank()) {
                    val compExists = runCatching {
                        packageManager.getActivityInfo(
                            ComponentName(carApp.packageName, carApp.activityName),
                            0,
                        )
                    }.isSuccess
                    if (!compExists) {
                        actualActivity = null
                    }
                }

                val targetId = if (!actualActivity.isNullOrBlank()) {
                    "${carApp.packageName}/$actualActivity"
                } else {
                    carApp.packageName
                }

                if (seenTargetIds.add(targetId)) {
                    val appLabel = runCatching {
                        val appInfo = packageManager.getApplicationInfo(carApp.packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(carApp.label)

                    val finalLabel = if (appLabel.isNotBlank() && appLabel != carApp.packageName) {
                        if (carApp.label == "BT Music" && !appLabel.contains("music", ignoreCase = true)) {
                            "$appLabel (Musik)"
                        } else {
                            appLabel
                        }
                    } else {
                        carApp.label
                    }

                    apps.add(
                        LaunchableApp(
                            packageName = carApp.packageName,
                            activityName = actualActivity,
                            label = finalLabel,
                            isCarSystemApp = true,
                        ),
                    )
                }
            }
        }

        // Jika context tersedia, cari juga bluetooth music dinamis yang terpasang
        if (context != null) {
            val dynamicBt = BluetoothMusicHelper.findAllBluetoothApps(context)
            for (bt in dynamicBt) {
                if (seenTargetIds.add(bt.targetId)) {
                    apps.add(
                        LaunchableApp(
                            packageName = bt.packageName,
                            activityName = bt.activityName,
                            label = bt.label,
                            isCarSystemApp = true,
                        ),
                    )
                }
            }
        }

        return apps.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}
