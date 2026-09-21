package com.misaka9981.alarm.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Computes when an Alarm next fires.
 *
 * The scheduling adapter arms the platform alarm with the instant this returns,
 * and re-arms every Alarm after a reboot, an app update, or power loss — so the
 * decision of *when* lives here and is tested, while *how* to arm the platform
 * alarm stays a thin adapter. A disabled Alarm has no next trigger.
 */
object AlarmSchedule {
    /**
     * The next instant [alarm] fires strictly after [after], or `null` when the
     * Alarm is disabled.
     *
     * [after] is exclusive so an Alarm that has just fired is armed for its next
     * occurrence rather than the same instant again.
     */
    fun nextTrigger(alarm: Alarm, after: ZonedDateTime): ZonedDateTime? {
        if (!alarm.enabled) return null

        val time = LocalTime.of(alarm.time.hour, alarm.time.minute)
        var date: LocalDate = after.toLocalDate()
        // An Alarm always repeats on at least one day, so a match is found within
        // seven days; the eighth iteration is only a guard for impossible data.
        repeat(DAYS_IN_A_WEEK + 1) {
            if (alarm.repeatDays.contains(date.dayOfWeek)) {
                val candidate = ZonedDateTime.of(date, time, after.zone)
                if (candidate.isAfter(after)) return candidate
            }
            date = date.plusDays(1)
        }
        return null
    }

    private const val DAYS_IN_A_WEEK = 7
}
