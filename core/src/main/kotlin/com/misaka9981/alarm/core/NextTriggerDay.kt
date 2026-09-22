package com.misaka9981.alarm.core

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * How an Alarm's next firing relates to the current day, for the relative label
 * shown on each Alarm row ("today", "tomorrow", or a weekday name).
 *
 * The words the owner reads are rendered in the Android layer from resources;
 * deciding *which* of those three cases applies — and, for the weekday case,
 * which day — is pure logic and lives here. It builds on
 * [AlarmSchedule.nextTrigger], so the label can never disagree with when the
 * Alarm is actually armed.
 */
sealed interface NextTriggerDay {
    /** The Alarm fires later today. */
    data object Today : NextTriggerDay

    /** The Alarm fires tomorrow. */
    data object Tomorrow : NextTriggerDay

    /** The Alarm fires on a later [day] of the week. */
    data class Weekday(val day: DayOfWeek) : NextTriggerDay

    companion object {
        /**
         * The relative day [alarm] next fires on, strictly after [after], or
         * `null` when the Alarm is disabled and therefore not armed.
         */
        fun of(alarm: Alarm, after: ZonedDateTime): NextTriggerDay? {
            val trigger = AlarmSchedule.nextTrigger(alarm, after) ?: return null
            return when (ChronoUnit.DAYS.between(after.toLocalDate(), trigger.toLocalDate())) {
                0L -> Today
                1L -> Tomorrow
                else -> Weekday(trigger.dayOfWeek)
            }
        }
    }
}
