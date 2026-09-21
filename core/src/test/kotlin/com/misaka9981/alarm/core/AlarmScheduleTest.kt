package com.misaka9981.alarm.core

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmScheduleTest {
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
    fun anAlarmLaterTodayFiresToday() {
        // Tuesday 2026-09-22.
        val next = AlarmSchedule.nextTrigger(weekdayMorning, at(day = 22, hour = 6, minute = 30))

        assertEquals(at(day = 22, hour = 7, minute = 0), next)
    }

    @Test
    fun aTimeThatHasPassedFiresOnTheNextRepeatDay() {
        // Tuesday 2026-09-22 at 08:00, so the next weekday morning is Wednesday.
        val next = AlarmSchedule.nextTrigger(weekdayMorning, at(day = 22, hour = 8, minute = 0))

        assertEquals(at(day = 23, hour = 7, minute = 0), next)
    }

    @Test
    fun daysTheAlarmDoesNotRepeatOnAreSkipped() {
        val mondayOnly = weekdayMorning.copy(repeatDays = setOf(DayOfWeek.MONDAY))

        // Tuesday 2026-09-22 at 08:00, so the next Monday is 2026-09-28.
        val next = AlarmSchedule.nextTrigger(mondayOnly, at(day = 22, hour = 8, minute = 0))

        assertEquals(at(day = 28, hour = 7, minute = 0), next)
    }

    @Test
    fun aDisabledAlarmNeverFires() {
        val next = AlarmSchedule.nextTrigger(weekdayMorning.copy(enabled = false), at(day = 22, hour = 0, minute = 0))

        assertNull(next)
    }

    @Test
    fun theNextTriggerIsStrictlyAfterTheReferenceInstant() {
        // Exactly the fire time must move on, so a just-fired Alarm is not re-armed
        // to the same instant.
        val next = AlarmSchedule.nextTrigger(weekdayMorning, at(day = 22, hour = 7, minute = 0))

        assertEquals(at(day = 23, hour = 7, minute = 0), next)
    }

    @Test
    fun everyDayAlarmFiresTomorrowWhenTodaysTimeHasPassed() {
        val everyDay = Alarm(
            id = AlarmId("every-day"),
            time = AlarmTime(6, 45),
            repeatDays = DayOfWeek.entries.toSet(),
            enabled = true,
        )

        val next = AlarmSchedule.nextTrigger(everyDay, at(day = 22, hour = 12, minute = 0))

        assertEquals(at(day = 23, hour = 6, minute = 45), next)
    }
}
