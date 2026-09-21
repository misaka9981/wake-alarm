package com.misaka9981.alarm.reliability

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.misaka9981.alarm.core.ReliabilityGrants

/**
 * Thin Android adapter that reads the platform's current reliability grants into
 * the pure `core` model.
 *
 * It makes no decision about what is required or missing — [com.misaka9981.alarm.core.ReliabilityCheck]
 * does that in `core`; this class only observes the platform.
 */
object AndroidReliabilityGrants {
    private const val EXACT_ALARM_MIN_SDK = 31
    private const val FULL_SCREEN_INTENT_MIN_SDK = 34

    fun read(context: Context): ReliabilityGrants {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)

        return ReliabilityGrants(
            exactAlarm = Build.VERSION.SDK_INT < EXACT_ALARM_MIN_SDK ||
                alarmManager?.canScheduleExactAlarms() == true,
            fullScreenIntent = Build.VERSION.SDK_INT < FULL_SCREEN_INTENT_MIN_SDK ||
                notificationManager?.canUseFullScreenIntent() == true,
            batteryOptimisationExempt = powerManager
                ?.isIgnoringBatteryOptimizations(context.packageName) == true,
            doNotDisturbAccess = notificationManager?.isNotificationPolicyAccessGranted == true,
        )
    }
}
