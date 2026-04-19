package me.kyrozyn.carside.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Wajib diaktifkan pengguna di Pengaturan → Notifikasi → Akses notifikasi aplikasi,
 * agar [android.media.session.MediaSessionManager.getActiveSessions] mengembalikan sesi pemutaran.
 */
class CarsideNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaNotificationHints.reconcileAll(this)
        NavigationNotificationHints.reconcileAll(this)
        MediaSessionBridge.onNotificationListenerConnected?.invoke()
        MediaSessionBridge.onMediaHintUpdated?.invoke()
        MediaSessionBridge.onNavigationHintUpdated?.invoke()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        MediaNotificationHints.record(this, sbn)
        NavigationNotificationHints.record(this, sbn)
        MediaSessionBridge.onMediaHintUpdated?.invoke()
        MediaSessionBridge.onNavigationHintUpdated?.invoke()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        MediaNotificationHints.reconcilePackage(this, sbn.packageName)
        NavigationNotificationHints.reconcilePackage(this, sbn.packageName)
        MediaSessionBridge.onMediaHintUpdated?.invoke()
        MediaSessionBridge.onNavigationHintUpdated?.invoke()
    }
}
