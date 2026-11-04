package me.kyrozyn.carside.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import me.kyrozyn.carside.data.settings.AppSettingsRepository

/**
 * Representasi aplikasi Bluetooth / BT Music pada Head Unit Android.
 */
data class CarBluetoothApp(
    val packageName: String,
    val activityName: String? = null,
    val label: String,
) {
    val targetId: String
        get() = if (!activityName.isNullOrBlank()) "$packageName/$activityName" else packageName
}

/**
 * Helper untuk mendeteksi dan membuka aplikasi Bluetooth Music pada Head Unit Android (Audiobank, TS7/TS10, XYAuto, Allwinner, FYT, dll.).
 *
 * Pada head unit Android buatan Cina (seperti merek Audiobank), modul Bluetooth terhubung
 * secara fisik ke IC multiplexer/DSP audio yang dikontrol langsung oleh MCU (Microcontroller Unit).
 * Audio Bluetooth dari HP TIDAK AKAN keluar ke speaker mobil sebelum aplikasi Bluetooth Music
 * bawaan head unit dibuka/diaktifkan.
 */
object BluetoothMusicHelper {

    private data class KnownTarget(
        val packageName: String,
        val activityName: String?,
        val defaultLabel: String,
    )

    /**
     * Daftar package & activity Bluetooth Music yang umum digunakan oleh head unit Android (termasuk Audiobank).
     */
    private val KNOWN_TARGETS = listOf(
        // TopWay / TS Series (TS7, TS10, TS18, Junsun, Audiobank Allwinner/Topway builds)
        KnownTarget("com.ts.bt", "com.ts.bt.BtMusicActivity", "BT Music"),
        KnownTarget("com.ts.MainUI", "com.ts.bt.BtMusicActivity", "BT Music"),
        KnownTarget("com.ts.bt", null, "Bluetooth"),

        // AutoChips / XYAuto / MediaTek (AC8227L, YT9216, Audiobank MTK builds)
        KnownTarget("com.autochips.bluetooth", "com.autochips.bluetooth.BtMusicActivity", "Bluetooth Music"),
        KnownTarget("com.autochips.bluetooth", "com.autochips.bluetooth.BluetoothActivity", "Bluetooth"),
        KnownTarget("com.autochips.bluetooth", null, "Bluetooth"),
        KnownTarget("com.xyauto.btmusic", null, "BT Music"),
        KnownTarget("com.mediatek.bluetooth", null, "Bluetooth"),

        // Allwinner / ECar / Softwinner (T3, T3L, T5, T7, T8 - umum di Audiobank seri TV6911/TV6912/Phoenix)
        KnownTarget("com.android.ecar.bmusic", null, "BT Music"),
        KnownTarget("com.allwinnertech.btmusic", null, "BT Music"),
        KnownTarget("com.softwinner.bluetooth", "com.softwinner.bluetooth.BtMusic", "BT Music"),
        KnownTarget("com.softwinner.bluetooth", null, "Bluetooth"),
        KnownTarget("com.car.btmusic", null, "BT Music"),

        // FYT / Syu (Joying, Mekede, Navifly, Teyes)
        KnownTarget("com.syu.bt", "com.syu.bt.BtAct", "Bluetooth Music"),
        KnownTarget("com.syu.bt", "com.syu.app.PhoneActivity", "Bluetooth"),
        KnownTarget("com.syu.bt", null, "Bluetooth"),
        KnownTarget("com.syu.music", null, "Music"),

        // Microntek / Rockchip (PX3, PX5, PX6, PX30)
        KnownTarget("com.microntek.btmusic", null, "BT Music"),
        KnownTarget("com.microntek.bluetooth", null, "Bluetooth"),

        // Generic Chinese Head Unit variants
        KnownTarget("com.bt.btmusic", null, "BT Music"),
        KnownTarget("com.tw.bt", null, "BT Music"),
        KnownTarget("com.mcar.btmusic", null, "BT Music"),
        KnownTarget("com.yecon.bluetooth", null, "Bluetooth"),
        KnownTarget("com.forfan.carlauncher", null, "Car Music"),

        // AOSP / Android System Bluetooth
        KnownTarget("com.android.bluetooth", null, "Bluetooth"),
    )

    /**
     * Mendeteksi aplikasi Bluetooth Music terbaik yang terpasang di perangkat.
     */
    fun detect(
        context: Context,
        settingsRepository: AppSettingsRepository? = null,
    ): CarBluetoothApp? {
        val pm = context.packageManager

        // 1. Cek apakah pengguna sudah memilih aplikasi custom di pengaturan
        val customPkg = settingsRepository?.customBluetoothMusicPackage?.value
        val customAct = settingsRepository?.customBluetoothMusicActivity?.value
        if (!customPkg.isNullOrBlank()) {
            val label = runCatching {
                val appInfo = pm.getApplicationInfo(customPkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault("Bluetooth Music")
            return CarBluetoothApp(packageName = customPkg, activityName = customAct, label = label)
        }

        // 2. Cek daftar package & activity bawaan head unit yang dikenal
        for (target in KNOWN_TARGETS) {
            val isPkgInstalled = runCatching {
                pm.getPackageInfo(target.packageName, 0)
            }.isSuccess

            if (isPkgInstalled) {
                val label = runCatching {
                    val appInfo = pm.getApplicationInfo(target.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }.getOrDefault(target.defaultLabel)

                if (!target.activityName.isNullOrBlank()) {
                    val componentExists = runCatching {
                        val cn = ComponentName(target.packageName, target.activityName)
                        pm.getActivityInfo(cn, 0)
                    }.isSuccess
                    if (componentExists) {
                        return CarBluetoothApp(
                            packageName = target.packageName,
                            activityName = target.activityName,
                            label = label.ifBlank { target.defaultLabel },
                        )
                    }
                }

                // Cek apakah paket ini memiliki launch intent atau sub-activity musik
                val launchIntent = pm.getLaunchIntentForPackage(target.packageName)
                val targetActivity = target.activityName ?: launchIntent?.component?.className
                return CarBluetoothApp(
                    packageName = target.packageName,
                    activityName = targetActivity,
                    label = label.ifBlank { target.defaultLabel },
                )
            }
        }

        // 3. Scan dinamis pada semua aplikasi yang terpasang (mencari keyword btmusic/bluetooth)
        val dynamicApp = scanForBluetoothMusicActivities(context).firstOrNull()
        if (dynamicApp != null) {
            return dynamicApp
        }

        return null
    }

    /**
     * Mengembalikan semua aplikasi yang berpotensi merupakan aplikasi Bluetooth / BT Music pada head unit.
     */
    fun findAllBluetoothApps(context: Context): List<CarBluetoothApp> {
        val pm = context.packageManager
        val results = mutableListOf<CarBluetoothApp>()
        val seenTargets = mutableSetOf<String>()

        // 1. Periksa dari known targets
        for (target in KNOWN_TARGETS) {
            val isInstalled = runCatching { pm.getPackageInfo(target.packageName, 0) }.isSuccess
            if (isInstalled) {
                val label = runCatching {
                    val appInfo = pm.getApplicationInfo(target.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }.getOrDefault(target.defaultLabel)

                val targetId = if (!target.activityName.isNullOrBlank()) "${target.packageName}/${target.activityName}" else target.packageName
                if (seenTargets.add(targetId)) {
                    results.add(
                        CarBluetoothApp(
                            packageName = target.packageName,
                            activityName = target.activityName,
                            label = label.ifBlank { target.defaultLabel },
                        ),
                    )
                }
            }
        }

        // 2. Periksa dari scan dinamis
        val dynamic = scanForBluetoothMusicActivities(context)
        for (app in dynamic) {
            if (seenTargets.add(app.targetId)) {
                results.add(app)
            }
        }

        return results
    }

    /**
     * Membuka aplikasi Bluetooth Music. Jika berhasil return true.
     */
    fun launch(
        context: Context,
        settingsRepository: AppSettingsRepository? = null,
    ): Boolean {
        val app = detect(context, settingsRepository)

        // Broadcast intent umum radio/BT music head unit Cina (membantu switch MCU audio)
        sendCarBluetoothBroadcasts(context)

        if (app != null) {
            val success = launchTarget(context, app.targetId)
            if (success) return true
        }

        // Fallback: buka pengaturan Bluetooth sistem
        return try {
            val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Membuka targetId berupa "packageName" atau "packageName/activityName".
     */
    fun launchTarget(context: Context, targetId: String): Boolean {
        val pm = context.packageManager

        // Jika targetId mengandung activity eksplisit
        if (targetId.contains("/")) {
            val parts = targetId.split("/", limit = 2)
            val pkg = parts[0]
            val cls = parts[1]
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(pkg, cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Abaikan dan coba fallback di bawah
            }
        }

        val pkg = targetId.substringBefore("/")

        // Coba launch intent standar
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(launchIntent)
                return true
            } catch (_: Exception) {
                // Abaikan
            }
        }

        // Coba cari activity MAIN apapun di dalam package tersebut
        try {
            val queryIntent = Intent(Intent.ACTION_MAIN).setPackage(pkg)
            val resolves = pm.queryIntentActivities(queryIntent, 0)
            val activity = resolves.firstOrNull()?.activityInfo
            if (activity != null) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(activity.packageName, activity.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(intent)
                return true
            }
        } catch (_: Exception) {
            // Abaikan
        }

        return false
    }

    /**
     * Mengirim broadcast sinyal BT Music yang sering didengarkan oleh MCU/Service Head Unit Android Cina.
     */
    private fun sendCarBluetoothBroadcasts(context: Context) {
        val actions = listOf(
            "android.intent.action.BT_MUSIC",
            "com.ts.bt.music",
            "com.syu.bt.action.MUSIC",
            "com.microntek.music.play",
        )
        for (action in actions) {
            try {
                context.sendBroadcast(Intent(action))
            } catch (_: Exception) {}
        }
    }

    @Suppress("DEPRECATION")
    private fun scanForBluetoothMusicActivities(context: Context): List<CarBluetoothApp> {
        val pm = context.packageManager
        val queryIntent = Intent(Intent.ACTION_MAIN)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        } else {
            PackageManager.MATCH_ALL
        }

        val resolves = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(queryIntent, flags)
            } else {
                pm.queryIntentActivities(queryIntent, flags)
            }
        }.getOrDefault(emptyList())

        val matched = mutableListOf<CarBluetoothApp>()
        for (ri in resolves) {
            val activityInfo = ri.activityInfo ?: continue
            val pkg = activityInfo.packageName
            val cls = activityInfo.name
            val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault("")

            val lowerPkg = pkg.lowercase()
            val lowerCls = cls.lowercase()
            val lowerLabel = label.lowercase()

            val isBtMusic = lowerCls.contains("btmusic") ||
                lowerCls.contains("bt_music") ||
                lowerCls.contains("a2dp") ||
                lowerCls.contains("bluetoothmusic") ||
                lowerLabel.contains("bt music") ||
                lowerLabel.contains("bluetooth music") ||
                lowerLabel.contains("musik bluetooth") ||
                lowerLabel.contains("a2dp") ||
                (lowerPkg.contains("bt") && lowerCls.contains("music"))

            if (isBtMusic) {
                matched.add(
                    CarBluetoothApp(
                        packageName = pkg,
                        activityName = cls,
                        label = label.ifBlank { "BT Music" },
                    ),
                )
            }
        }
        return matched
    }
}
