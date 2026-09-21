package com.misaka9981.alarm.firing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.misaka9981.alarm.R
import com.misaka9981.alarm.ui.AlarmFiringActivity

/**
 * Thin Android adapter that builds the Alarm's notification.
 *
 * The notification carries the full-screen intent that brings the firing screen
 * over the lock screen, is ongoing so it cannot be swiped away, and bypasses Do
 * Not Disturb when the owner has granted notification policy access. The alarm
 * sound and vibration are produced by [AlarmPlayback], not by the channel.
 */
object AlarmNotification {
    const val CHANNEL_ID = "wake-alarm-firing"
    const val NOTIFICATION_ID = 1

    /** Creates the high-importance alarm channel once. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alarm_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
            // Bypassing Do Not Disturb needs notification policy access; without
            // it the platform rejects the request, which the reliability guide
            // surfaces as a missing requirement.
            if (manager.isNotificationPolicyAccessGranted) {
                setBypassDnd(true)
            }
        }
        manager.createNotificationChannel(channel)
    }

    /** The ongoing notification that launches the firing screen over the lock screen. */
    fun buildFiringNotification(context: Context, alarmId: String, alarmLabel: String): Notification {
        ensureChannel(context)
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            AlarmFiringActivity.intent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(alarmLabel)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()
    }

    /** Removes the notification when the Alarm is dismissed. */
    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}
