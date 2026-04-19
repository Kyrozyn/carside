package me.kyrozyn.carside.media

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Menarik isi notifikasi navigasi Google Maps / Waze (instruksi manuver, jarak, ETA, ikon).
 *
 * Google Maps (com.google.android.apps.maps) menjalankan notifikasi navigasi sebagai foreground
 * service yang dipicu terus-menerus selama rute aktif. Layout-nya pakai [Notification.EXTRA_TITLE]
 * untuk instruksi manuver ("Belok kanan"), [Notification.EXTRA_TEXT] untuk nama jalan/tujuan, dan
 * [Notification.EXTRA_SUB_TEXT] untuk ETA/sisa jarak. Waze (com.waze) mirip tapi kadang menukar
 * title/text — kita pakai heuristik longgar dan biarkan UI menampilkan apa adanya bila parsing
 * gagal.
 */
object NavigationNotificationHints {

    const val PKG_GMAPS = "com.google.android.apps.maps"
    const val PKG_WAZE = "com.waze"

    private val SUPPORTED_PACKAGES = setOf(PKG_GMAPS, PKG_WAZE)

    /** Regex longgar untuk memisah potongan "450 m" / "5,4 km" / "1.2 mi" / "300 ft". */
    private val DISTANCE_REGEX = Regex(
        "\\d+[.,]?\\d*\\s?(km|mi|m|ft)\\b",
        RegexOption.IGNORE_CASE,
    )

    data class NavHint(
        val packageName: String,
        val maneuverText: String?,
        val roadText: String?,
        val distanceText: String?,
        val etaText: String?,
        val iconUri: String?,
        val postTime: Long,
        val isOngoing: Boolean,
    )

    private val lock = Any()
    private val hints = mutableMapOf<String, NavHint>()

    /** Hint terbaru dari paket navigasi manapun yang sedang ongoing. */
    fun get(): NavHint? = synchronized(lock) {
        hints.values
            .filter { it.isOngoing }
            .maxByOrNull { it.postTime }
    }

    fun get(packageName: String): NavHint? = synchronized(lock) { hints[packageName] }

    fun record(service: NotificationListenerService, sbn: StatusBarNotification) {
        if (!isNavigationLike(sbn)) return
        val ctx = service.applicationContext
        val hint = hintFrom(ctx, sbn) ?: return
        synchronized(lock) {
            val cur = hints[sbn.packageName]
            if (cur != null && cur.postTime > sbn.postTime) return
            hints[sbn.packageName] = hint
        }
    }

    fun reconcilePackage(service: NotificationListenerService, packageName: String) {
        try {
            val active = service.activeNotifications ?: return
            val best = active
                .filter { it.packageName == packageName && isNavigationLike(it) }
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
                .filter { isNavigationLike(it) }
                .groupBy { it.packageName }
                .mapValues { (_, list) -> list.maxBy { it.postTime } }
            val ctx = service.applicationContext
            synchronized(lock) {
                hints.keys.toList().forEach { pkg ->
                    if (pkg !in byPackage.keys) hints.remove(pkg)
                }
                byPackage.forEach { (pkg, sbn) ->
                    hintFrom(ctx, sbn)?.let { hints[pkg] = it }
                }
            }
        } catch (_: SecurityException) {
            // ignore
        }
    }

    private fun hintFrom(context: Context, sbn: StatusBarNotification): NavHint? {
        val n = sbn.notification
        val ex = n.extras ?: return null
        val pkg = sbn.packageName

        val rawTitle = charSeq(ex, Notification.EXTRA_TITLE)
        val rawText = charSeq(ex, Notification.EXTRA_TEXT)
        val rawBig = charSeq(ex, Notification.EXTRA_BIG_TEXT)
        val rawSub = charSeq(ex, Notification.EXTRA_SUB_TEXT)
        val rawInfo = charSeq(ex, Notification.EXTRA_INFO_TEXT)
        val rawSummary = charSeq(ex, Notification.EXTRA_SUMMARY_TEXT)

        val maneuverText = rawTitle
        val bodyLine = firstNonBlank(rawText, rawBig)
        val (distanceFromBody, roadText) = splitDistanceAndText(bodyLine)
        val etaCandidate = firstNonBlank(rawSub, rawInfo, rawSummary)

        val distanceText = distanceFromBody ?: extractDistance(etaCandidate)
        val etaText = etaCandidate

        val iconUri = extractManeuverIcon(context, pkg, n)

        if (maneuverText == null && roadText == null && distanceText == null &&
            etaText == null && iconUri == null
        ) {
            return null
        }

        return NavHint(
            packageName = pkg,
            maneuverText = maneuverText,
            roadText = roadText,
            distanceText = distanceText,
            etaText = etaText,
            iconUri = iconUri,
            postTime = sbn.postTime,
            isOngoing = sbn.isOngoing,
        )
    }

    /** Pisahkan potongan jarak ("450 m") dari nama jalan dalam satu string notifikasi. */
    private fun splitDistanceAndText(text: String?): Pair<String?, String?> {
        if (text.isNullOrBlank()) return null to null
        val match = DISTANCE_REGEX.find(text)
        if (match == null) return null to text.trim()
        val distance = match.value.trim()
        val remainder = (text.substring(0, match.range.first) + " " +
            text.substring(match.range.last + 1))
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('-', '·', '•', ',', ' ')
            .trim()
            .takeIf { it.isNotEmpty() }
        return distance to remainder
    }

    private fun extractDistance(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return DISTANCE_REGEX.find(text)?.value?.trim()
    }

    private fun extractManeuverIcon(
        context: Context,
        notificationPackage: String,
        n: Notification,
    ): String? {
        val fctx = foreignContext(context, notificationPackage)

        notificationLargeIconAsBitmap(fctx, n)?.let { return cacheIcon(context, it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val icon = n.smallIcon
                icon?.loadDrawable(fctx)?.let { drawable ->
                    drawableToBitmap(drawable)?.let { return cacheIcon(context, it) }
                }
            }
        }

        return null
    }

    private fun foreignContext(context: Context, packageName: String): Context {
        return runCatching {
            context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED)
        }.getOrElse { context }
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

    private fun drawableToBitmap(d: Drawable): Bitmap? = runCatching {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: 192
        val h = d.intrinsicHeight.takeIf { it > 0 } ?: 192
        d.toBitmap(max(w, 64), max(h, 64))
    }.getOrNull()

    private fun cacheIcon(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(
                context.cacheDir,
                "carside_nav_icon_${System.currentTimeMillis()}.png",
            )
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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

    private fun isNavigationLike(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in SUPPORTED_PACKAGES && sbn.isOngoing) return true
        val n = sbn.notification
        if (n.category == Notification.CATEGORY_NAVIGATION && sbn.isOngoing) return true
        return false
    }
}
