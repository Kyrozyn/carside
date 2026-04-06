package me.kyrozyn.carside.ui.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

object InstalledAppsLoader {

    fun loadLaunchableApps(packageManager: PackageManager): List<LaunchableApp> {
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

        return resolves
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                val label = ri.loadLabel(packageManager).toString()
                LaunchableApp(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}
