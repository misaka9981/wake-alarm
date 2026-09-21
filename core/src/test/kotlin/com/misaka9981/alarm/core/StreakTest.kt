package com.misaka9981.alarm.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {
    private val day = LocalDate.of(2026, 9, 22)

    @Test
    fun aFirstDismissedDayStartsTheStreakAtOne() {
        val streak = Streak().record(day, DayOutcome.Dismissed)

        assertEquals(Streak(count = 1, lastDay = day), streak)
    }

    @Test
    fun consecutiveDismissedDaysExtendTheStreak() {
        val streak = Streak()
            .record(day, DayOutcome.Dismissed)
            .record(day.plusDays(1), DayOutcome.Dismissed)
            .record(day.plusDays(2), DayOutcome.Dismissed)

        assertEquals(Streak(count = 3, lastDay = day.plusDays(2)), streak)
    }

    @Test
    fun aCapExpiryBreaksTheStreak() {
        val streak = Streak()
            .record(day, DayOutcome.Dismissed)
            .record(day.plusDays(1), DayOutcome.Dismissed)
            .record(day.plusDays(2), DayOutcome.CapExpired)

        assertEquals(Streak(count = 0, lastDay = day.plusDays(2)), streak)
    }

    @Test
    fun aCapExpiryAfterAnEarlierDismissalOnTheSameDayStillBreaksIt() {
        val streak = Streak()
            .record(day.minusDays(1), DayOutcome.Dismissed)
            .record(day, DayOutcome.Dismissed)
            .record(day, DayOutcome.CapExpired)

        assertEquals(Streak(count = 0, lastDay = day), streak)
    }

    @Test
    fun completingTheChallengeAfterTheCapCannotBuyTheDayBack() {
        val streak = Streak()
            .record(day.minusDays(1), DayOutcome.Dismissed)
            .record(day, DayOutcome.CapExpired)
            .record(day, DayOutcome.Dismissed)

        assertEquals(Streak(count = 0, lastDay = day), streak)
    }

    @Test
    fun theDayAfterACapExpiryStartsANewStreakAtOne() {
        val streak = Streak()
            .record(day, DayOutcome.Dismissed)
            .record(day.plusDays(1), DayOutcome.CapExpired)
            .record(day.plusDays(2), DayOutcome.Dismissed)

        assertEquals(Streak(count = 1, lastDay = day.plusDays(2)), streak)
    }

    @Test
    fun aMissedDayRestartsTheStreak() {
        val streak = Streak()
            .record(day, DayOutcome.Dismissed)
            .record(day.plusDays(1), DayOutcome.Dismissed)
            .record(day.plusDays(3), DayOutcome.Dismissed)

        assertEquals(Streak(count = 1, lastDay = day.plusDays(3)), streak)
    }

    @Test
    fun dismissingAgainOnTheSameDayDoesNotDoubleCount() {
        val streak = Streak()
            .record(day, DayOutcome.Dismissed)
            .record(day, DayOutcome.Dismissed)

        assertEquals(Streak(count = 1, lastDay = day), streak)
    }
}
