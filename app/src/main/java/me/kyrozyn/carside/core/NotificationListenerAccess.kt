package me.kyrozyn.carside.core

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import me.kyrozyn.carside.media.CarsideNotificationListenerService

fun Context.requestCarsideNotificationListenerRebind() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        NotificationListenerService.requestRebind(
            ComponentName(this, CarsideNotificationListenerService::class.java),
        )
    }
}

fun Context.isCarsideNotificationListenerEnabled(): Boolean {
    val expected = ComponentName(this, CarsideNotificationListenerService::class.java)
    val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        ?: return false
    val strictMatch = flat.split(":")
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { runCatching { ComponentName.unflattenFromString(it) }.getOrNull() }
        .any { it.packageName == expected.packageName && it.className == expected.className }
    if (strictMatch) return true
    val pkg = packageName
    val cls = CarsideNotificationListenerService::class.java.name
    return flat.contains(pkg, ignoreCase = true) &&
        (flat.contains(cls) || flat.contains("CarsideNotificationListenerService"))
}
