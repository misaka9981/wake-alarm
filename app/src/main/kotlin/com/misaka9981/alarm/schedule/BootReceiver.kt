package com.misaka9981.alarm.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.misaka9981.alarm.data.DataStoreAlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Thin Android adapter that re-arms every Alarm after the device restarts or the
 * app is updated.
 *
 * Platform alarms do not survive a reboot or power loss, so the owner would miss
 * an Alarm without this. Which Alarms exist lives in `core`; this receiver only
 * loads them and asks [AlarmScheduler] to arm each one.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarms = DataStoreAlarmRepository(context).load()
                AlarmScheduler(context).reschedule(alarms)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
