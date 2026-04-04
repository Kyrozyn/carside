package me.kyrozyn.carside.data.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.MediaController as SessionMediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.kyrozyn.carside.domain.model.TrackInfo
import me.kyrozyn.carside.domain.repository.MediaController as DomainMediaController
import me.kyrozyn.carside.domain.repository.MediaPlaybackState
import me.kyrozyn.carside.media.CarsideNotificationListenerService
import me.kyrozyn.carside.media.MediaNotificationHints
import me.kyrozyn.carside.media.MediaSessionBridge

class SystemMediaSessionRepository(
    context: Context,
) : DomainMediaController {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent =
        ComponentName(appContext, CarsideNotificationListenerService::class.java)
    private val pm = appContext.packageManager

    private val _playbackState = MutableStateFlow(idleState())
    override val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()

    private var bound: SessionMediaController? = null

    private val sessionCallback = object : SessionMediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = pushState()
        override fun onPlaybackStateChanged(state: PlaybackState?) = pushState()
        override fun onQueueChanged(queue: List<MediaSession.QueueItem>?) = pushState()
        override fun onExtrasChanged(extras: Bundle?) = pushState()
        override fun onSessionEvent(event: String, extras: Bundle?) = pushState()
        override fun onSessionDestroyed() {
            mainHandler.post {
                detach()
                refreshActiveSessions()
            }
        }
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            mainHandler.post { attachToBestController(controllers) }
        }

    init {
        MediaSessionBridge.onNotificationListenerConnected = {
            mainHandler.post { refreshActiveSessions() }
        }
        MediaSessionBridge.onMediaHintUpdated = {
            mainHandler.post { if (bound != null) pushState() }
        }
        try {
            sessionManager.addOnActiveSessionsChangedListener(activeSessionsListener, listenerComponent)
        } catch (_: SecurityException) {
            _playbackState.value = idleState(noAccess = true)
        }
        mainHandler.post { refreshActiveSessions() }
    }

    override fun refreshActiveSessions() {
        val controllers = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            _playbackState.value = idleState(noAccess = true)
            return
        }
        attachToBestController(controllers)
    }

    override fun play() {
        bound?.transportControls?.play()
    }

    override fun pause() {
        bound?.transportControls?.pause()
    }

    override fun skipNext() {
        bound?.transportControls?.skipToNext()
    }

    override fun skipPrevious() {
        bound?.transportControls?.skipToPrevious()
    }

    override fun seekTo(positionMs: Long) {
        bound?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    private fun detach() {
        bound?.unregisterCallback(sessionCallback)
        bound = null
    }

    private fun attachToBestController(controllers: List<SessionMediaController>?) {
        val list = controllers.orEmpty()
        if (list.isEmpty()) {
            detach()
            _playbackState.value = idleState(noAccess = false)
            return
        }

        val best = pickController(list) ?: run {
            detach()
            _playbackState.value = idleState(noAccess = false)
            return
        }

        if (bound?.sessionToken != best.sessionToken) {
            detach()
            bound = best
            best.registerCallback(sessionCallback, mainHandler)
        }
        pushState()
    }

    private fun pickController(list: List<SessionMediaController>): SessionMediaController? {
        if (list.isEmpty()) return null
        fun stateOf(c: SessionMediaController) = c.playbackState?.state ?: PlaybackState.STATE_NONE

        fun hasUsableTrackInfo(c: SessionMediaController): Boolean {
            val m = c.metadata
            if (m != null) {
                if (!m.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()) return true
                if (!m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).isNullOrBlank()) return true
                val desc = runCatching { m.description }.getOrNull()
                if (!desc?.title.isNullOrBlank()) return true
                val scr = scrapedMetadataStrings(m)
                if (!scrapedTitle(scr).isNullOrBlank() || !scrapedArtist(scr).isNullOrBlank()) return true
            }
            val q = c.queue
            if (!q.isNullOrEmpty()) {
                val d = q.firstOrNull()?.description
                if (!d?.title.isNullOrBlank()) return true
            }
            val hint = MediaNotificationHints.get(c.packageName)
            if (!hint?.title.isNullOrBlank()) return true
            if (!hint?.artUri.isNullOrBlank()) return true
            return false
        }

        fun pickFrom(sub: List<SessionMediaController>): SessionMediaController? {
            if (sub.isEmpty()) return null
            val rich = sub.filter { hasUsableTrackInfo(it) }
            return rich.firstOrNull() ?: sub.firstOrNull()
        }

        pickFrom(list.filter { stateOf(it) == PlaybackState.STATE_PLAYING })?.let { return it }
        pickFrom(list.filter { stateOf(it) == PlaybackState.STATE_BUFFERING })?.let { return it }
        pickFrom(list.filter { stateOf(it) == PlaybackState.STATE_PAUSED })?.let { return it }
        return pickFrom(list) ?: list.firstOrNull()
    }

    private fun pushState() {
        val mc = bound
        if (mc == null) {
            _playbackState.value = idleState(noAccess = false)
            return
        }

        val meta = mc.metadata
        val ps = mc.playbackState
        val actions = ps?.actions ?: 0L

        val resolved = resolveTrack(meta, mc, ps)

        val pkg = mc.packageName
        val appLabel = appLabelForPackage(pkg)

        val st = ps?.state ?: PlaybackState.STATE_NONE
        val isPlaying = st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING

        // Banyak aplikasi mengirim actions=0 — tetap izinkan play/pause jika sesi ada.
        val canPlay = actions == 0L || actions and PlaybackState.ACTION_PLAY != 0L
        val canPause = actions == 0L || actions and PlaybackState.ACTION_PAUSE != 0L
        val canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        val canSkipPrev = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
        val canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L

        val durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 } ?: 0L
        val rawPosition = ps?.position ?: 0L
        val anchor = ps?.lastPositionUpdateTime?.takeIf { it > 0L } ?: SystemClock.elapsedRealtime()

        _playbackState.value = MediaPlaybackState(
            track = resolved,
            isPlaying = isPlaying,
            sourceLabel = appLabel,
            sourcePackageName = pkg,
            hasActiveSession = true,
            canPlay = canPlay,
            canPause = canPause,
            canSkipNext = canSkipNext,
            canSkipPrevious = canSkipPrev,
            canSeek = canSeek,
            positionMs = rawPosition.coerceAtLeast(0L),
            durationMs = durationMs,
            positionAnchorElapsedRealtimeMs = anchor,
        )
    }

    private fun appLabelForPackage(packageName: String): String {
        if (packageName.isBlank()) return "Media"
        return try {
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun resolveTrack(
        meta: MediaMetadata?,
        mc: SessionMediaController,
        ps: PlaybackState?,
    ): TrackInfo {
        val fromQueue = descriptionFromQueue(mc, ps)
        val fromMetaDesc = meta?.let { runCatching { it.description }.getOrNull() }
        val extras = ps?.extras
        val controllerExtras = runCatching { mc.extras }.getOrNull()
        val scraped = scrapedMetadataStrings(meta)
        val notifHint = MediaNotificationHints.get(mc.packageName)

        val title = firstNonBlank(
            fromQueue?.title?.toString(),
            fromMetaDesc?.title?.toString(),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_TITLE),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            extras?.getString(MediaMetadata.METADATA_KEY_TITLE),
            extras?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            controllerExtras?.getString(MediaMetadata.METADATA_KEY_TITLE),
            controllerExtras?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            scrapedTitle(scraped),
            fromQueue?.description?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            fromMetaDesc?.description?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                meta?.stringOrText(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            } else {
                null
            },
            bundleTitleGuess(controllerExtras),
            bundleTitleGuess(extras),
            notifHint?.title,
        ) ?: "Tanpa judul"

        val artist = firstNonBlank(
            fromQueue?.subtitle?.toString(),
            fromMetaDesc?.subtitle?.toString(),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_ARTIST),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_AUTHOR),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_WRITER),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_COMPOSER),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                meta?.stringOrText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            } else {
                null
            },
            extras?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            extras?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            controllerExtras?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            controllerExtras?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            scrapedArtist(scraped),
            meta?.stringOrText(MediaMetadata.METADATA_KEY_ALBUM),
            bundleArtistGuess(controllerExtras),
            bundleArtistGuess(extras),
            notifHint?.subtitle,
        ).orEmpty()

        val notifArt = MediaNotificationHints.get(mc.packageName)?.artUri
        val artUri = artworkFromDescription(fromQueue)
            ?: artworkFromDescription(fromMetaDesc)
            ?: notifArt
            ?: artworkUriFromMetadata(meta)
            ?: bitmapFromMetadataBundle(meta)?.let { cacheBitmapArt(it) }
            ?: artworkUriFromExtras(extras)
            ?: artworkUriFromExtras(controllerExtras)
            ?: anyArtUriStringInBundle(controllerExtras)
            ?: anyArtUriStringInBundle(extras)
            ?: scrapedArtUri(scraped)

        return TrackInfo(title = title, artist = artist, albumArtUri = artUri)
    }

    private fun descriptionFromQueue(mc: SessionMediaController, ps: PlaybackState?): MediaDescription? {
        val queue = mc.queue ?: return null
        if (queue.isEmpty()) return null
        val activeId = ps?.activeQueueItemId ?: return queue.firstOrNull()?.description
        if (activeId < 0L) {
            return queue.firstOrNull()?.description
        }
        return queue.firstOrNull { it.queueId == activeId }?.description
            ?: queue.firstOrNull()?.description
    }

    private fun MediaMetadata.stringOrText(key: String): String? {
        getString(key)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return getText(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    /** Beberapa pemutar (mis. Spotify) menyimpan string di bundle internal yang tidak terbaca lewat getter standar. */
    private fun scrapedMetadataStrings(meta: MediaMetadata?): Map<String, String> {
        if (meta == null) return emptyMap()
        return try {
            val field = MediaMetadata::class.java.getDeclaredField("mBundle")
            field.isAccessible = true
            val bundle = field.get(meta) as? Bundle ?: return emptyMap()
            val out = linkedMapOf<String, String>()
            for (key in bundle.keySet()) {
                when (val v = bundle.get(key)) {
                    is String -> if (v.isNotBlank()) out[key] = v.trim()
                    is CharSequence -> v.toString().trim().takeIf { it.isNotEmpty() }?.let { out[key] = it }
                }
            }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun scrapedTitle(scraped: Map<String, String>): String? {
        val ordered = listOf(
            MediaMetadata.METADATA_KEY_TITLE,
            MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
        )
        for (k in ordered) {
            scraped[k]?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return scraped.entries
            .filter { (k, v) ->
                v.isNotBlank() && k.contains("media.metadata", ignoreCase = true) &&
                    (k.endsWith(".TITLE") || k.endsWith(".DISPLAY_TITLE"))
            }
            .maxByOrNull { it.value.length }
            ?.value
    }

    private fun scrapedArtist(scraped: Map<String, String>): String? {
        val ordered = listOf(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_AUTHOR,
            MediaMetadata.METADATA_KEY_WRITER,
            MediaMetadata.METADATA_KEY_COMPOSER,
        )
        for (k in ordered) {
            scraped[k]?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return scraped.entries
            .filter { (k, v) ->
                v.isNotBlank() && k.contains("media.metadata", ignoreCase = true) &&
                    (k.endsWith(".ARTIST") || k.endsWith(".ALBUM_ARTIST"))
            }
            .maxByOrNull { it.value.length }
            ?.value
    }

    private fun bundleTitleGuess(b: Bundle?): String? {
        if (b == null) return null
        val keys = b.keySet()
        for (k in keys) {
            if (!k.contains("title", ignoreCase = true)) continue
            if (k.contains("subtitle", ignoreCase = true)) continue
            when (val v = b.get(k)) {
                is String -> if (v.isNotBlank()) return v.trim()
                is CharSequence -> v.toString().trim().takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        return null
    }

    private fun bundleArtistGuess(b: Bundle?): String? {
        if (b == null) return null
        for (k in b.keySet()) {
            if (!k.contains("artist", ignoreCase = true)) continue
            when (val v = b.get(k)) {
                is String -> if (v.isNotBlank()) return v.trim()
                is CharSequence -> v.toString().trim().takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        return null
    }

    private fun artworkFromDescription(desc: MediaDescription?): String? {
        if (desc == null) return null
        desc.iconUri?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        desc.iconBitmap?.let { return cacheBitmapArt(it) }
        return null
    }

    private fun artworkUriFromExtras(extras: android.os.Bundle?): String? {
        if (extras == null) return null
        extras.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.takeIf { it.isNotBlank() }?.let { return it }
        extras.getString(MediaMetadata.METADATA_KEY_ART_URI)?.takeIf { it.isNotBlank() }?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            extras.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun artworkUriFromMetadata(meta: MediaMetadata?): String? {
        if (meta == null) return null
        meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.takeIf { it.isNotBlank() }?.let { return it }
        meta.getString(MediaMetadata.METADATA_KEY_ART_URI)?.takeIf { it.isNotBlank() }?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        val bmp = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            } else {
                null
            }
        return bmp?.let { cacheBitmapArt(it) }
    }

    private fun bitmapFromMetadataBundle(meta: MediaMetadata?): Bitmap? {
        if (meta == null) return null
        return try {
            val field = MediaMetadata::class.java.getDeclaredField("mBundle")
            field.isAccessible = true
            val bundle = field.get(meta) as? Bundle ?: return null
            for (key in bundle.keySet()) {
                when (val v = bundle.get(key)) {
                    is Bitmap -> return v
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun scrapedArtUri(scraped: Map<String, String>): String? {
        val ordered = buildList {
            add(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            add(MediaMetadata.METADATA_KEY_ART_URI)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            }
        }
        for (k in ordered) {
            scraped[k]?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        }
        return scraped.entries
            .filter { (k, v) ->
                v.isNotBlank() && k.contains("media.metadata", ignoreCase = true) &&
                    (k.endsWith(".ALBUM_ART_URI") || k.endsWith(".ART_URI") ||
                        k.endsWith(".DISPLAY_ICON_URI"))
            }
            .maxByOrNull { it.value.length }
            ?.value
            ?.trim()
    }

    /** URI gambar di bundle extras (Spotify / OEM kadang pakai kunci non-standar). */
    private fun anyArtUriStringInBundle(b: Bundle?): String? {
        if (b == null) return null
        for (key in b.keySet()) {
            val keyOk = key.contains("URI", ignoreCase = true) ||
                key.contains("ART", ignoreCase = true) ||
                key.contains("ICON", ignoreCase = true) ||
                key.contains("IMAGE", ignoreCase = true)
            if (!keyOk) continue
            when (val v = b.get(key)) {
                is String -> {
                    val t = v.trim()
                    if (t.startsWith("http://", ignoreCase = true) ||
                        t.startsWith("https://", ignoreCase = true) ||
                        t.startsWith("content://", ignoreCase = true) ||
                        t.startsWith("file://", ignoreCase = true)
                    ) {
                        return t
                    }
                }
                is CharSequence -> {
                    val t = v.toString().trim()
                    if (t.startsWith("http://", ignoreCase = true) ||
                        t.startsWith("https://", ignoreCase = true) ||
                        t.startsWith("content://", ignoreCase = true) ||
                        t.startsWith("file://", ignoreCase = true)
                    ) {
                        return t
                    }
                }
            }
        }
        return null
    }

    private fun cacheBitmapArt(bitmap: Bitmap): String? {
        return try {
            val file = File(appContext.cacheDir, "carside_now_playing_art_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.toURI().toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun idleState(noAccess: Boolean = false): MediaPlaybackState {
        val hint = if (noAccess) {
            "Aktifkan akses notifikasi untuk Carside"
        } else {
            "Putar musik/podcast di aplikasi lain"
        }
        return MediaPlaybackState(
            track = TrackInfo(
                title = "Tidak ada pemutaran",
                artist = hint,
                albumArtUri = null,
            ),
            isPlaying = false,
            sourceLabel = "—",
            sourcePackageName = "",
            hasActiveSession = false,
            canPlay = false,
            canPause = false,
            canSkipNext = false,
            canSkipPrevious = false,
            canSeek = false,
            positionMs = 0L,
            durationMs = 0L,
            positionAnchorElapsedRealtimeMs = SystemClock.elapsedRealtime(),
        )
    }
}
