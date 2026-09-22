package com.misaka9981.alarm.core

import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AlarmTest {
    @Test
    fun alarmTimeAcceptsAnyClockTime() {
        assertEquals(0, AlarmTime(0, 0).hour)
        assertEquals(59, AlarmTime(23, 59).minute)
    }

    @Test
    fun alarmTimeRejectsImpossibleHour() {
        assertFailsWith<IllegalArgumentException> { AlarmTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { AlarmTime(-1, 0) }
    }

    @Test
    fun alarmTimeRejectsImpossibleMinute() {
        assertFailsWith<IllegalArgumentException> { AlarmTime(7, 60) }
        assertFailsWith<IllegalArgumentException> { AlarmTime(7, -1) }
    }

    @Test
    fun alarmMustRepeatOnAtLeastOneDay() {
        assertFailsWith<IllegalArgumentException> {
            Alarm(
                id = AlarmId("a"),
                time = AlarmTime(7, 0),
                repeatDays = emptySet(),
                enabled = true,
            )
        }
    }

    @Test
    fun alarmKeepsItsConfiguration() {
        val alarm = Alarm(
            id = AlarmId("a"),
            time = AlarmTime(6, 30),
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            enabled = false,
        )

        assertEquals(AlarmId("a"), alarm.id)
        assertEquals(AlarmTime(6, 30), alarm.time)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), alarm.repeatDays)
        assertEquals(false, alarm.enabled)
    }

    @Test
    fun silentModeIsOffByDefault() {
        val alarm = Alarm(
            id = AlarmId("a"),
            time = AlarmTime(6, 30),
            repeatDays = setOf(DayOfWeek.MONDAY),
            enabled = true,
        )

        assertEquals(false, alarm.silentMode)
    }

    @Test
    fun silentModeIsPerAlarm() {
        val ringing = Alarm(
            id = AlarmId("ringing"),
            time = AlarmTime(6, 30),
            repeatDays = setOf(DayOfWeek.MONDAY),
            enabled = true,
        )
        val silent = ringing.copy(silentMode = true)

        assertEquals(false, ringing.silentMode)
        assertEquals(true, silent.silentMode)
    }
}
