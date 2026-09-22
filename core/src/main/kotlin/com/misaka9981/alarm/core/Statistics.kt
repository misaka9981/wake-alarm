package com.misaka9981.alarm.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration

/**
 * One morning's dismissal, as the Statistics view shows it.
 *
 * [day] is the local day the Alarm fired on, which is the day the Streak counts.
 * [dismissedAt] is the clock time the owner actually cleared the Alarm — the fire
 * instant plus how long it signalled — so "am I waking earlier?" is answerable.
 * [ringingDuration] is how long that took, so "am I getting faster?" is
 * answerable too. [wrongAnswers] is how many answers were wrong on the way.
 */
data class DismissalRecord(
    val day: LocalDate,
    val dismissedAt: LocalTime,
    val ringingDuration: Duration,
    val wrongAnswers: Int,
) {
    init {
        require(!ringingDuration.isNegative()) {
            "ringingDuration must not be negative, was $ringingDuration"
        }
        require(wrongAnswers >= 0) { "wrongAnswers must not be negative, was $wrongAnswers" }
    }
}

/**
 * The morning statistics, derived from the recorded Diagnostic Log.
 *
 * Every figure is computed from recorded history — the [DiagnosticEntry] values
 * the Diagnostic Log persisted — rather than from transient in-memory state, so
 * it survives the app closing and is the same in the morning and the afternoon
 * (spec: Statistics; `CONTEXT.md`).
 *
 * [currentStreak] is the [Streak] folded over the days the log actually records:
 * a day counts only when every firing on it was dismissed by Challenge and
 * Anchor, and a cap expiry or an Escape Hatch use breaks the day outright.
 * Because it is folded from history, a day with no recorded firing is not
 * evidence of a dismissal: the next dismissed day restarts the Streak at one.
 *
 * [recentDismissals] is the most recent [RECENT_DISMISSALS] dismissed firings,
 * newest first; firings that were not dismissed have no dismissal time and are
 * excluded. [averageWakeTime] is the mean clock time of every recorded dismissal,
 * or `null` before any Alarm has been dismissed.
 *
 * See `CONTEXT.md` (Streak, Diagnostic Log) and the spec's "Statistics".
 */
data class WakeStatistics(
    val currentStreak: Int = 0,
    val recentDismissals: List<DismissalRecord> = emptyList(),
    val averageWakeTime: LocalTime? = null,
) {
    init {
        require(currentStreak >= 0) { "currentStreak must not be negative, was $currentStreak" }
    }

    companion object {
        /** How many recent dismissed firings the Statistics view shows. */
        const val RECENT_DISMISSALS: Int = 7

        /** No recorded history yet: no Streak, nothing recent, no average. */
        val empty: WakeStatistics = WakeStatistics()

        /**
         * Derives the statistics from the recorded [entries], reading local days
         * and clock times in [zone]. Entries are ordered by [DiagnosticEntry.firedAt]
         * first, so an out-of-order log still folds chronologically.
         */
        fun of(entries: List<DiagnosticEntry>, zone: ZoneId): WakeStatistics {
            val ordered = entries.sortedBy { it.firedAt }
            val dismissalRecords = ordered
                .filter { it.outcome == FiringOutcome.Dismissed }
                .map { it.toDismissalRecord(zone) }
            return WakeStatistics(
                currentStreak = streakOver(ordered, zone).count,
                recentDismissals = dismissalRecords.takeLast(RECENT_DISMISSALS).asReversed(),
                averageWakeTime = averageOf(dismissalRecords.map { it.dismissedAt }),
            )
        }

        /**
         * Folds one [DayOutcome] per recorded day in chronological order. A day is
         * dismissed only when every firing on it was; an Escape Hatch use or a cap
         * expiry on the day breaks it, however many other Alarms were dismissed.
         */
        private fun streakOver(entries: List<DiagnosticEntry>, zone: ZoneId): Streak =
            entries
                .groupBy { it.firedAt.atZone(zone).toLocalDate() }
                .toSortedMap()
                .entries
                .fold(Streak()) { streak, (day, firings) ->
                    streak.record(day, dayOutcome(firings))
                }

        private fun dayOutcome(firings: List<DiagnosticEntry>): DayOutcome = when {
            firings.any { it.outcome == FiringOutcome.EscapeHatch } -> DayOutcome.EscapeHatch
            firings.any { it.outcome == FiringOutcome.CapExpired } -> DayOutcome.CapExpired
            else -> DayOutcome.Dismissed
        }

        private fun DiagnosticEntry.toDismissalRecord(zone: ZoneId): DismissalRecord =
            DismissalRecord(
                day = firedAt.atZone(zone).toLocalDate(),
                dismissedAt = firedAt
                    .plusMillis(challengeDuration.inWholeMilliseconds)
                    .atZone(zone)
                    .toLocalTime()
                    .truncatedTo(ChronoUnit.SECONDS),
                ringingDuration = challengeDuration,
                wrongAnswers = wrongAnswers,
            )

        /**
         * The mean clock time of [times], or `null` when there are none. A wake
         * alarm fires in the small hours, so a plain mean of the time of day is
         * enough; times are compared at whole-second resolution, which is all the
         * view shows.
         */
        private fun averageOf(times: List<LocalTime>): LocalTime? {
            if (times.isEmpty()) return null
            val meanSecondOfDay = times.sumOf { it.toSecondOfDay().toLong() } / times.size
            return LocalTime.ofSecondOfDay(meanSecondOfDay)
        }
    }
}
