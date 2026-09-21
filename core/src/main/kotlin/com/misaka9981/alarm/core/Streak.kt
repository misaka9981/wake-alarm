package com.misaka9981.alarm.core

import java.time.LocalDate

/**
 * How a single day ended, as far as the Streak is concerned.
 *
 * A day is [Dismissed] only when every Alarm that fired on it was cleared by the
 * Dismiss Challenge and the Physical Anchor. A day whose Alarm rang out to its
 * sound cap is [CapExpired]: stopping the noise is not the same as dismissing the
 * Alarm, so letting the cap expire has a cost.
 */
enum class DayOutcome {
    /** The day's Alarms were dismissed by Challenge and Anchor. */
    Dismissed,

    /** An Alarm's sound cap expired; the Alarm stayed uncleared. */
    CapExpired,
}

/**
 * The consecutive-day Streak.
 *
 * [count] is the number of consecutive days on which the owner dismissed every
 * Alarm without letting the sound cap expire, and [lastDay] is the most recent
 * day that outcome was recorded for. [record] applies one day's outcome and
 * returns the next Streak; a cap expiry breaks the Streak to zero even if an
 * earlier Alarm on the same day was dismissed, so completing the challenge after
 * the cap cannot buy the day back.
 *
 * See `CONTEXT.md` (Streak) and the spec's "Streak rules".
 */
data class Streak(
    val count: Int = 0,
    val lastDay: LocalDate? = null,
) {
    init {
        require(count >= 0) { "count must not be negative, was $count" }
    }

    /** Applies [outcome] for [day] and returns the resulting Streak. */
    fun record(day: LocalDate, outcome: DayOutcome): Streak = when (outcome) {
        // Stopping the noise is not dismissal: the cap breaks the day outright,
        // and no later dismissal on the same day can undo it (lastDay is the day,
        // so a subsequent Dismissed is treated as the same already-broken day).
        DayOutcome.CapExpired -> Streak(count = 0, lastDay = day)

        DayOutcome.Dismissed -> when (lastDay) {
            day -> this
            day.minusDays(1) -> Streak(count = count + 1, lastDay = day)
            else -> Streak(count = 1, lastDay = day)
        }
    }
}
