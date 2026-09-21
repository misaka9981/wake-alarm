package com.misaka9981.alarm.firing

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Thin Android adapter that keeps the Alarm's notification alive while it is
 * uncleared.
 *
 * A foreground service keeps the ringing sound and vibration alive and lets the
 * ongoing notification with its full-screen intent remain. When the sound cap
 * expires the sound and vibration stop, but the service stays foreground so the
 * notification persists — the Alarm is not cleared by the cap. It makes no
 * decision about dismissal: the firing screen ends the service once
 * [com.misaka9981.alarm.core.FiringSession] reports dismissed.
 */
class AlarmService : Service() {
    private var playback: AlarmPlayback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SIGNALLING) {
            // Cap expired: stop the noise but keep the foreground notification,
            // because the Alarm is still uncleared.
            playback?.stop()
            playback = null
            return START_STICKY
        }

        val alarmId = intent?.getStringExtra(EXTRA_ALARM_ID).orEmpty()
        val label = intent?.getStringExtra(EXTRA_ALARM_LABEL) ?: getString(android.R.string.untitled)

        val notification = AlarmNotification.buildFiringNotification(this, alarmId, label)
        ServiceCompat.startForeground(
            this,
            AlarmNotification.NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )

        if (playback == null) {
            playback = AlarmPlayback(this).also { it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        playback?.stop()
        playback = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        private const val ACTION_STOP_SIGNALLING = "com.misaka9981.alarm.action.STOP_SIGNALLING"

        /** Starts (or restarts) the ringing service for [alarmId]. */
        fun start(context: Context, alarmId: String, alarmLabel: String) {
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Stops the sound and vibration when the sound cap expires, while leaving
         * the service running so the ongoing notification (and the full-screen
         * intent that reaches the firing screen) persists. Idempotent.
         */
        fun stopSignalling(context: Context) {
            context.startService(
                Intent(context, AlarmService::class.java).setAction(ACTION_STOP_SIGNALLING),
            )
        }

        /** Stops ringing and removes the notification when the Alarm is dismissed. */
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
