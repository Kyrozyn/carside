package me.kyrozyn.carside.media

/**
 * NotificationListener terhubung → refresh daftar [android.media.session.MediaSession] aktif.
 */
object MediaSessionBridge {
    var onNotificationListenerConnected: (() -> Unit)? = null

    /** Hint notifikasi (judul/sampul) berubah — refresh UI media. */
    var onMediaHintUpdated: (() -> Unit)? = null

    /** Hint notifikasi navigasi (Google Maps / Waze) berubah — refresh panel navigasi. */
    var onNavigationHintUpdated: (() -> Unit)? = null
}
