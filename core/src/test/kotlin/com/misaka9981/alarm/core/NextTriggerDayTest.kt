package com.misaka9981.alarm.core

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextTriggerDayTest {
    private val zone = ZoneId.of("Europe/London")

    private fun at(day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)

    private val weekdayMorning = Alarm(
        id = AlarmId("weekday-morning"),
        time = AlarmTime(7, 0),
        repeatDays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ),
        enabled = true,
    )

    @Test
    fun anAlarmLaterTodayIsToday() {
        // Tuesday 2026-09-22.
        val day = NextTriggerDay.of(weekdayMorning, at(day = 22, hour = 6, minute = 30))

        assertEquals(NextTriggerDay.Today, day)
    }

    @Test
    fun anAlarmOnTheFollowingDayIsTomorrow() {
        // Tuesday 2026-09-22 at 08:00, so the next weekday morning is Wednesday.
        val day = NextTriggerDay.of(weekdayMorning, at(day = 22, hour = 8, minute = 0))

        assertEquals(NextTriggerDay.Tomorrow, day)
    }

    @Test
    fun anAlarmFurtherAwayIsItsWeekday() {
        val mondayOnly = weekdayMorning.copy(repeatDays = setOf(DayOfWeek.MONDAY))

        // Tuesday 2026-09-22 at 08:00, so the next Monday is 2026-09-28.
        val day = NextTriggerDay.of(mondayOnly, at(day = 22, hour = 8, minute = 0))

        assertEquals(NextTriggerDay.Weekday(DayOfWeek.MONDAY), day)
    }

    @Test
    fun aWeeklyAlarmThatHasJustPassedIsItsOwnWeekdayNotToday() {
        val tuesdayOnly = weekdayMorning.copy(repeatDays = setOf(DayOfWeek.TUESDAY))

        // Tuesday 2026-09-22 at 08:00, so the next firing is a week away.
        val day = NextTriggerDay.of(tuesdayOnly, at(day = 22, hour = 8, minute = 0))

        assertEquals(NextTriggerDay.Weekday(DayOfWeek.TUESDAY), day)
    }

    @Test
    fun aDisabledAlarmHasNoNextTriggerDay() {
        val day = NextTriggerDay.of(weekdayMorning.copy(enabled = false), at(day = 22, hour = 0, minute = 0))

        assertNull(day)
    }
}
