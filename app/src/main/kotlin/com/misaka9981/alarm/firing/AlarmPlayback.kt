package com.misaka9981.alarm.firing

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.misaka9981.alarm.core.VolumeRamp
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thin Android adapter that makes the Alarm audible and felt.
 *
 * It owns the platform concerns — a looping [MediaPlayer] on the alarm stream, a
 * repeating [Vibrator] pattern, and a periodic tick — and applies the volume
 * curve that [VolumeRamp] decides in `core`. It does not decide when to stop:
 * the Alarm is dismissed only by the firing session, so the sound keeps going
 * until then. See the spec's "Firing reliably" stories.
 */
class AlarmPlayback(context: Context) {
    private val appContext = context.applicationContext
    private val ramp = VolumeRamp()
    private val startedAt = SystemClock.elapsedRealtime()
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? = vibratorFor(appContext)

    private var player: MediaPlayer? = null

    private val volumeTick = object : Runnable {
        override fun run() {
            applyRampedVolume()
            handler.postDelayed(this, VOLUME_TICK_MILLIS)
        }
    }

    /** Starts ringing and vibrating, with the volume already at its start fraction. */
    fun start() {
        raiseAlarmStreamToMaximum()
        player = createPlayer()?.also {
            applyRampedVolume()
            it.start()
        }
        startVibrating()
        handler.post(volumeTick)
    }

    /** Stops ringing and vibrating. */
    fun stop() {
        handler.removeCallbacks(volumeTick)
        player?.let { current ->
            try {
                current.stop()
            } catch (_: IllegalStateException) {
                // Already stopped; releasing below is what matters.
            }
            current.release()
        }
        player = null
        vibrator?.cancel()
    }

    private fun createPlayer(): MediaPlayer? {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return null
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(appContext, uri)
                isLooping = true
                prepare()
            }
        } catch (_: Exception) {
            // A missing or unreadable ringtone must not stop the Alarm from
            // vibrating and showing its full-screen intent.
            null
        }
    }

    private fun applyRampedVolume() {
        val fraction = ramp.fractionAt((SystemClock.elapsedRealtime() - startedAt).milliseconds)
        player?.setVolume(fraction, fraction)
    }

    private fun raiseAlarmStreamToMaximum() {
        val audioManager = appContext.getSystemService(AudioManager::class.java) ?: return
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0,
            )
        } catch (_: SecurityException) {
            // Volume could not be raised; the Alarm still rings at the current level.
        }
    }

    private fun startVibrating() {
        vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
    }

    private companion object {
        const val VOLUME_TICK_MILLIS = 500L
        val VIBRATION_PATTERN = longArrayOf(0, 800, 600)

        fun vibratorFor(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
    }
}
