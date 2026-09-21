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
 * Thin Android adapter that keeps the Alarm signalling while it is uncleared.
 *
 * A foreground service keeps the ringing sound and vibration alive and lets the
 * ongoing notification with its full-screen intent remain, so the Alarm does not
 * stop on its own. It makes no decision about dismissal — the firing screen ends
 * the service once [com.misaka9981.alarm.core.FiringSession] reports dismissed.
 */
class AlarmService : Service() {
    private var playback: AlarmPlayback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        /** Starts (or restarts) the ringing service for [alarmId]. */
        fun start(context: Context, alarmId: String, alarmLabel: String) {
            val intent = Intent(context, AlarmService::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stops ringing. */
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}
