package com.misaka9981.alarm.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.misaka9981.alarm.MainActivity
import com.misaka9981.alarm.core.Alarm
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AlarmSchedule
import com.misaka9981.alarm.firing.AlarmReceiver
import java.time.ZonedDateTime

/**
 * Thin Android adapter that arms the platform alarm for an [Alarm].
 *
 * The decision of *when* an Alarm next fires is [AlarmSchedule]'s, in `core`;
 * this class only turns that instant into a platform alarm and re-arms every
 * Alarm after a reboot, an app update, or power loss.
 *
 * Exact alarms are used when the platform allows them. When the exact-alarm
 * grant is missing the Alarm is still armed inexactly rather than lost, and the
 * reliability guide warns the owner that exactness is missing — the app degrades
 * gracefully instead of failing silently. See the spec's Further Notes.
 */
class AlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    /** Arms every enabled Alarm and cancels disabled ones. */
    fun reschedule(alarms: List<Alarm>) {
        alarms.forEach(::schedule)
    }

    /** Arms the next occurrence of [alarm], or cancels it when disabled. */
    fun schedule(alarm: Alarm) {
        val manager = alarmManager ?: return
        val trigger = AlarmSchedule.nextTrigger(alarm, ZonedDateTime.now())
        if (trigger == null) {
            cancel(alarm.id)
            return
        }

        val atMillis = trigger.toInstant().toEpochMilli()
        val alarmIntent = pendingIntent(alarm.id, PendingIntent.FLAG_UPDATE_CURRENT, atMillis) ?: return
        if (Build.VERSION.SDK_INT < EXACT_ALARM_MIN_SDK || manager.canScheduleExactAlarms()) {
            val showIntent = PendingIntent.getActivity(
                appContext,
                requestCode(alarm.id),
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent), alarmIntent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent)
        }
    }

    /** Cancels the armed alarm for [id], if any. */
    fun cancel(id: AlarmId) {
        val existing = pendingIntent(id, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager?.cancel(existing)
        existing.cancel()
    }

    private fun pendingIntent(id: AlarmId, flags: Int, scheduledAtMillis: Long = -1L): PendingIntent? {
        val intent = Intent(appContext, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, id.value)
            .putExtra(AlarmReceiver.EXTRA_SCHEDULED_AT, scheduledAtMillis)
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(id),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(id: AlarmId): Int = id.value.hashCode()

    private companion object {
        const val EXACT_ALARM_MIN_SDK = 31
    }
}
