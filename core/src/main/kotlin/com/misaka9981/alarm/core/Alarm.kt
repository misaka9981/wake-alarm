package com.misaka9981.alarm.core

import java.time.DayOfWeek

/** Stable identity of an [Alarm], independent of its time or repeat days. */
@JvmInline
value class AlarmId(val value: String)

/**
 * The time of day an [Alarm] fires.
 *
 * A value that cannot describe a real clock time cannot be constructed, so the
 * rest of the app never has to defend against an impossible [Alarm].
 */
data class AlarmTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "hour must be in 0..23, was $hour" }
        require(minute in 0..59) { "minute must be in 0..59, was $minute" }
    }
}

/**
 * A scheduled event that wakes the owner at [time] on [repeatDays].
 *
 * An Alarm with no repeat days could never fire, so it cannot be constructed.
 * See `CONTEXT.md` for the canonical vocabulary.
 */
data class Alarm(
    val id: AlarmId,
    val time: AlarmTime,
    val repeatDays: Set<DayOfWeek>,
    val enabled: Boolean,
) {
    init {
        require(repeatDays.isNotEmpty()) { "an Alarm must repeat on at least one day" }
    }
}
