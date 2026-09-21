package com.misaka9981.alarm.reliability

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.misaka9981.alarm.core.ReliabilityRequirement

/**
 * Thin Android adapter that opens the platform settings page where a missing
 * [ReliabilityRequirement] can be granted.
 *
 * Whether the requirement is needed and missing is decided in `core`; this class
 * only knows where to send the owner. A `null` result means the platform does not
 * gate that requirement on this version.
 */
object ReliabilityLauncher {
    private const val EXACT_ALARM_MIN_SDK = 31
    private const val FULL_SCREEN_INTENT_MIN_SDK = 34

    fun intentFor(context: Context, requirement: ReliabilityRequirement): Intent? = when (requirement) {
        ReliabilityRequirement.ExactAlarm ->
            if (Build.VERSION.SDK_INT >= EXACT_ALARM_MIN_SDK) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
            } else {
                null
            }

        ReliabilityRequirement.FullScreenIntent ->
            if (Build.VERSION.SDK_INT >= FULL_SCREEN_INTENT_MIN_SDK) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))
            } else {
                null
            }

        ReliabilityRequirement.BatteryOptimisation ->
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))

        ReliabilityRequirement.DoNotDisturbAccess ->
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
}
