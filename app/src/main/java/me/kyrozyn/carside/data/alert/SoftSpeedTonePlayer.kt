package me.kyrozyn.carside.data.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import me.kyrozyn.carside.R

/**
 * Memutar nada peringatan kecepatan dari `res/raw/autopilot_alert.mp3`.
 * Stream media (`USAGE_MEDIA`, `STREAM_MUSIC`) seperti pemutar radio (ExoPlayer), bukan volume notifikasi.
 */
class SoftSpeedTonePlayer(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    fun play() {
        mainHandler.post {
            runCatching {
                val mp = MediaPlayer()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mp.setAudioAttributes(mediaPlaybackAudioAttributes())
                } else {
                    @Suppress("DEPRECATION")
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                appContext.resources.openRawResourceFd(R.raw.autopilot_alert).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                mp.prepare()
                mp.setVolume(0.95f, 0.95f)
                mp.setOnCompletionListener { player ->
                    player.release()
                }
                mp.setOnErrorListener { player, _, _ ->
                    player.release()
                    true
                }
                mp.start()
            }
        }
    }

    private fun mediaPlaybackAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
}
