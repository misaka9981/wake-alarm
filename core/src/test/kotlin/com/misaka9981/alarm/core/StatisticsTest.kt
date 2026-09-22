package com.misaka9981.alarm.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class StatisticsTest {
    private val zone: ZoneId = ZoneOffset.UTC

    @Test
    fun noRecordedFiringsGivesAnEmptyStatistics() {
        val statistics = WakeStatistics.of(emptyList(), zone)

        assertEquals(WakeStatistics(), statistics)
    }

    @Test
    fun aSingleDismissedFiringShowsAStreakOfOneAndItsDismissal() {
        val statistics = WakeStatistics.of(
            listOf(dismissed("2026-09-22T06:00:00Z", duration = 5.minutes, wrongAnswers = 2)),
            zone,
        )

        assertEquals(1, statistics.currentStreak)
        assertEquals(
            listOf(
                DismissalRecord(
                    day = LocalDate.of(2026, 9, 22),
                    dismissedAt = LocalTime.of(6, 5),
                    ringingDuration = 5.minutes,
                    wrongAnswers = 2,
                ),
            ),
            statistics.recentDismissals,
        )
        assertEquals(LocalTime.of(6, 5), statistics.averageWakeTime)
    }

    @Test
    fun consecutiveDismissedDaysExtendTheStreak() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-20T06:00:00Z"),
                dismissed("2026-09-21T06:00:00Z"),
                dismissed("2026-09-22T06:00:00Z"),
            ),
            zone,
        )

        assertEquals(3, statistics.currentStreak)
    }

    @Test
    fun aCapExpiredFiringBreaksTheStreakForThatDay() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-20T06:00:00Z"),
                dismissed("2026-09-21T06:00:00Z"),
                entry("2026-09-22T06:00:00Z", outcome = FiringOutcome.CapExpired),
            ),
            zone,
        )

        assertEquals(0, statistics.currentStreak)
    }

    @Test
    fun anEscapeHatchUseBreaksTheStreak() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-21T06:00:00Z"),
                entry("2026-09-22T06:00:00Z", outcome = FiringOutcome.EscapeHatch),
            ),
            zone,
        )

        assertEquals(0, statistics.currentStreak)
    }

    @Test
    fun aDismissalAndACapExpiryOnTheSameDayStillBreakTheStreak() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-20T06:00:00Z"),
                dismissed("2026-09-21T06:00:00Z"),
                dismissed("2026-09-21T07:00:00Z"),
                entry("2026-09-21T08:00:00Z", outcome = FiringOutcome.CapExpired),
            ),
            zone,
        )

        assertEquals(0, statistics.currentStreak)
    }

    @Test
    fun theDayAfterABreakStartsANewStreak() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-20T06:00:00Z"),
                entry("2026-09-21T06:00:00Z", outcome = FiringOutcome.EscapeHatch),
                dismissed("2026-09-22T06:00:00Z"),
            ),
            zone,
        )

        assertEquals(1, statistics.currentStreak)
    }

    @Test
    fun aGapBetweenDismissalsRestartsTheStreak() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-20T06:00:00Z"),
                dismissed("2026-09-21T06:00:00Z"),
                dismissed("2026-09-23T06:00:00Z"),
            ),
            zone,
        )

        assertEquals(1, statistics.currentStreak)
    }

    @Test
    fun averageWakeTimeAveragesTheDismissalTimes() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-21T06:00:00Z", duration = 10.minutes),
                dismissed("2026-09-22T07:00:00Z", duration = 20.minutes),
            ),
            zone,
        )

        // 06:10 and 07:20 average to 06:45.
        assertEquals(LocalTime.of(6, 45), statistics.averageWakeTime)
    }

    @Test
    fun averageWakeTimeIgnoresFiringsThatWereNotDismissed() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-22T06:00:00Z", duration = 10.minutes),
                entry("2026-09-22T23:00:00Z", outcome = FiringOutcome.CapExpired),
            ),
            zone,
        )

        assertEquals(LocalTime.of(6, 10), statistics.averageWakeTime)
    }

    @Test
    fun recentDismissalsAreNewestFirstAndExcludeFiringsThatWereNotDismissed() {
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-21T06:00:00Z", duration = 10.minutes, wrongAnswers = 1),
                entry("2026-09-22T05:00:00Z", outcome = FiringOutcome.EscapeHatch),
                dismissed("2026-09-22T07:00:00Z", duration = 20.minutes, wrongAnswers = 3),
            ),
            zone,
        )

        assertEquals(
            listOf(
                DismissalRecord(
                    day = LocalDate.of(2026, 9, 22),
                    dismissedAt = LocalTime.of(7, 20),
                    ringingDuration = 20.minutes,
                    wrongAnswers = 3,
                ),
                DismissalRecord(
                    day = LocalDate.of(2026, 9, 21),
                    dismissedAt = LocalTime.of(6, 10),
                    ringingDuration = 10.minutes,
                    wrongAnswers = 1,
                ),
            ),
            statistics.recentDismissals,
        )
    }

    @Test
    fun recentDismissalsKeepOnlyTheMostRecent() {
        val entries = (1..9).map { day ->
            dismissed("2026-09-%02dT06:00:00Z".format(day), duration = day.minutes, wrongAnswers = day)
        }

        val statistics = WakeStatistics.of(entries, zone)

        // Days 1..9 recorded; the most recent 7 are days 3..9, newest first, so the
        // oldest the view still shows is day 3.
        assertEquals(WakeStatistics.RECENT_DISMISSALS, statistics.recentDismissals.size)
        assertEquals(9, statistics.recentDismissals.first().wrongAnswers)
        assertEquals(3, statistics.recentDismissals.last().wrongAnswers)
    }

    @Test
    fun firingsAreGroupedIntoDaysInTheGivenZone() {
        // Both instants are late on 23 September in a +02:00 zone, so they are one
        // day as far as the Streak is concerned.
        val statistics = WakeStatistics.of(
            listOf(
                dismissed("2026-09-22T23:30:00Z"),
                dismissed("2026-09-23T20:00:00Z"),
            ),
            ZoneOffset.ofHours(2),
        )

        assertEquals(1, statistics.currentStreak)
        assertEquals(2, statistics.recentDismissals.size)
    }

    private fun dismissed(
        firedAt: String,
        duration: Duration = Duration.ZERO,
        wrongAnswers: Int = 0,
    ): DiagnosticEntry = entry(
        firedAt = firedAt,
        duration = duration,
        wrongAnswers = wrongAnswers,
        outcome = FiringOutcome.Dismissed,
    )

    private fun entry(
        firedAt: String,
        duration: Duration = Duration.ZERO,
        wrongAnswers: Int = 0,
        outcome: FiringOutcome,
    ): DiagnosticEntry = DiagnosticEntry(
        alarmId = AlarmId("alarm-1"),
        scheduledTime = null,
        firedAt = Instant.parse(firedAt),
        missingRequirements = emptySet(),
        signallingVolume = 100,
        challengeDuration = duration,
        wrongAnswers = wrongAnswers,
        outcome = outcome,
    )
}
