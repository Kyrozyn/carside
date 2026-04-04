package me.kyrozyn.carside.media

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Judul/teks + sampul dari notifikasi media.
 * Spotify sering pakai [Icon] ber-URI (CDN / content) — [loadDrawable] bisa gagal tanpa
 * [Context.createPackageContext] untuk paket pemutar.
 */
object MediaNotificationHints {

    data class Hint(
        val title: String?,
        val subtitle: String?,
        val postTime: Long,
        /** `file:` hasil cache, atau `https://` / `content:` untuk Coil. */
        val artUri: String?,
    )

    private val lock = Any()
    private val hints = mutableMapOf<String, Hint>()

    fun get(packageName: String): Hint? = synchronized(lock) { hints[packageName] }

    fun record(service: NotificationListenerService, sbn: StatusBarNotification) {
        if (!isMediaLike(sbn.notification)) return
        val ctx = service.applicationContext
        val hint = hintFrom(ctx, sbn) ?: return
        synchronized(lock) {
            val cur = hints[sbn.packageName]
            if (cur != null && cur.postTime > sbn.postTime) return
            val merged = if (cur != null && sbn.postTime == cur.postTime) {
                hint.copy(
                    artUri = hint.artUri ?: cur.artUri,
                    title = hint.title ?: cur.title,
                    subtitle = hint.subtitle ?: cur.subtitle,
                )
            } else {
                hint
            }
            hints[sbn.packageName] = merged
        }
    }

    fun reconcilePackage(service: NotificationListenerService, packageName: String) {
        try {
            val active = service.activeNotifications ?: return
            val best = active
                .filter { it.packageName == packageName && isMediaLike(it.notification) }
                .maxByOrNull { it.postTime }
            val ctx = service.applicationContext
            synchronized(lock) {
                if (best == null) {
                    hints.remove(packageName)
                } else {
                    hintFrom(ctx, best)?.let { hints[packageName] = it }
                }
            }
        } catch (_: SecurityException) {
            synchronized(lock) { hints.remove(packageName) }
        }
    }

    fun reconcileAll(service: NotificationListenerService) {
        try {
            val active = service.activeNotifications ?: return
            val byPackage = active
                .filter { isMediaLike(it.notification) }
                .groupBy { it.packageName }
                .mapValues { (_, list) -> list.maxBy { it.postTime } }
            val ctx = service.applicationContext
            synchronized(lock) {
                byPackage.forEach { (pkg, sbn) ->
                    hintFrom(ctx, sbn)?.let { hints[pkg] = it }
                }
            }
        } catch (_: SecurityException) {
            // ignore
        }
    }

    private fun hintFrom(context: Context, sbn: StatusBarNotification): Hint? {
        val n = sbn.notification
        val ex = n.extras
        val pkg = sbn.packageName
        val bigFirstLine = ex?.let { e ->
            charSeq(e, Notification.EXTRA_BIG_TEXT)
                ?.lineSequence()
                ?.map { it.trim() }
                ?.firstOrNull { it.isNotEmpty() }
        }
        val title = firstNonBlank(
            ex?.let { charSeq(it, Notification.EXTRA_TITLE) },
            bigFirstLine,
        )
        val subtitle = ex?.let { e ->
            firstNonBlank(
                charSeq(e, Notification.EXTRA_TEXT),
                charSeq(e, Notification.EXTRA_SUB_TEXT),
                charSeq(e, Notification.EXTRA_INFO_TEXT),
                charSeq(e, Notification.EXTRA_SUMMARY_TEXT),
            )
        }
        val effectiveTitle = title ?: subtitle
        val effectiveSubtitle = if (title != null) subtitle else null
        val artUri = extractNotificationArtwork(context, pkg, n, ex)

        if (effectiveTitle == null && effectiveSubtitle == null && artUri == null) return null
        return Hint(
            title = effectiveTitle,
            subtitle = effectiveSubtitle,
            postTime = sbn.postTime,
            artUri = artUri,
        )
    }

    private fun extractNotificationArtwork(
        context: Context,
        notificationPackage: String,
        n: Notification,
        ex: Bundle?,
    ): String? {
        foreignContext(context, notificationPackage).let { fctx ->
            notificationLargeIconAsBitmap(fctx, n)?.let { return cacheArt(context, it) }
            largeIconUriString(n)?.let { u ->
                materializeArtUri(context, notificationPackage, u)?.let { return it }
            }
        }

        if (ex != null) {
            largestBitmapFromExtras(ex)?.let { return cacheArt(context, it) }

            val picture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ex.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                ex.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            }
            picture?.let { return cacheArt(context, it) }

            @Suppress("DEPRECATION")
            val legacyIcon = ex.get(Notification.EXTRA_LARGE_ICON)
            if (legacyIcon is Bitmap) {
                return cacheArt(context, legacyIcon)
            }
        }
        return null
    }

    private fun foreignContext(context: Context, packageName: String): Context {
        return runCatching {
            context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED)
        }.getOrElse { context }
    }

    private fun largeIconUriString(n: Notification): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return runCatching {
            val icon = Notification::class.java
                .getMethod("getLargeIcon")
                .invoke(n) as? Icon ?: return null
            when (icon.type) {
                Icon.TYPE_URI -> icon.uri
                6 -> icon.uri // Icon.TYPE_URI_ADAPTIVE (API 30), tidak tersedia di semua stub
                else -> null
            }
        }.getOrNull()
    }

    private fun notificationLargeIconAsBitmap(context: Context, n: Notification): Bitmap? {
        @Suppress("DEPRECATION")
        (n.largeIcon as? Bitmap)?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return runCatching {
                val icon = Notification::class.java
                    .getMethod("getLargeIcon")
                    .invoke(n) as? Icon ?: return@runCatching null
                icon.loadDrawable(context)?.let { drawableToBitmap(it) }
            }.getOrNull()
        }
        return null
    }

    /** Simpan ke cache jika perlu agar Coil tidak bergantung pada resolver asing. */
    private fun materializeArtUri(context: Context, notificationPackage: String, uri: Uri): String? {
        val s = uri.toString()
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme == "http" || scheme == "https") return s
        if (scheme != "content" && scheme != "file") return s

        return runCatching {
            val stream = openStreamForUri(context, notificationPackage, uri) ?: return@runCatching null
            stream.use { input ->
                val file = File(context.cacheDir, "carside_notif_art_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { output -> input.copyTo(output) }
                file.toURI().toString()
            }
        }.getOrNull()
    }

    private fun openStreamForUri(context: Context, notificationPackage: String, uri: Uri) =
        foreignContext(context, notificationPackage).contentResolver.openInputStream(uri)
            ?: context.contentResolver.openInputStream(uri)

    private fun largestBitmapFromExtras(ex: Bundle): Bitmap? {
        var best: Bitmap? = null
        var bestArea = 0
        for (key in ex.keySet()) {
            when (val v = ex.get(key)) {
                is Bitmap -> {
                    if (v.isRecycled) continue
                    val area = max(1, v.width) * max(1, v.height)
                    if (area > bestArea) {
                        bestArea = area
                        best = v
                    }
                }
            }
        }
        return best
    }

    private fun drawableToBitmap(d: Drawable): Bitmap? = runCatching {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: 512
        val h = d.intrinsicHeight.takeIf { it > 0 } ?: 512
        d.toBitmap(w, h)
    }.getOrNull()

    private fun cacheArt(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.cacheDir, "carside_notif_art_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.toURI().toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun charSeq(ex: Bundle, key: String): String? =
        ex.getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private fun isMediaLike(n: Notification): Boolean {
        if (n.category == Notification.CATEGORY_TRANSPORT) return true
        val ex = n.extras ?: return false
        if (ex.containsKey(Notification.EXTRA_MEDIA_SESSION)) return true
        if (ex.containsKey("android.compactActions")) return true
        val tmpl = ex.getCharSequence("android.template")?.toString().orEmpty()
        if (tmpl.contains("MediaStyle", ignoreCase = true)) return true
        return false
    }
}
