package com.misaka9981.alarm.firing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.misaka9981.alarm.core.AlarmId
import com.misaka9981.alarm.core.AlarmTime
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import com.misaka9981.alarm.schedule.AlarmScheduler
import com.misaka9981.alarm.ui.AlarmFiringActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Thin Android adapter that reacts to a platform alarm firing.
 *
 * It arms the Alarm's next occurrence (so a repeating Alarm keeps repeating),
 * starts [AlarmService] to ring, and asks the platform to bring
 * [AlarmFiringActivity] over the lock screen through the service's full-screen
 * intent. The dismissal decision is not made here: it belongs to
 * [com.misaka9981.alarm.core.FiringSession].
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarm = DataStoreAlarmRepository(appContext)
                    .load()
                    .firstOrNull { it.id == AlarmId(alarmId) }

                if (alarm != null && alarm.enabled) {
                    AlarmScheduler(appContext).schedule(alarm)
                    AlarmService.start(appContext, alarmId, labelFor(alarm.time), alarm.silentMode)
                    launchFiringActivity(appContext, alarmId, intent.getLongExtra(EXTRA_SCHEDULED_AT, -1L))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun launchFiringActivity(context: Context, alarmId: String, scheduledAtMillis: Long) {
        val activityIntent = AlarmFiringActivity.intent(context, alarmId, scheduledAtMillis)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(activityIntent)
        } catch (_: Exception) {
            // Background activity starts can be refused; the full-screen-intent
            // notification from AlarmService is the sanctioned path and remains.
        }
    }

    private fun labelFor(time: AlarmTime): String =
        "%02d:%02d".format(time.hour, time.minute)

    companion object {
        const val ACTION_FIRE = "com.misaka9981.alarm.action.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"

        /**
         * The epoch millis the Alarm was armed for, carried through to the firing
         * screen so the Diagnostic Log can record the scheduled time. `-1` when
         * unknown, such as a development firing.
         */
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
    }
}
