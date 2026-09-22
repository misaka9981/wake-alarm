package com.misaka9981.alarm.core

import java.time.DayOfWeek
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SignallingTest {
    private val alarm = Alarm(
        id = AlarmId("a"),
        time = AlarmTime(6, 30),
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        enabled = true,
    )

    @Test
    fun anAlarmWithoutSilentModeRingsAndVibrates() {
        val signalling = Signalling.of(alarm)

        assertEquals(Signalling.SoundAndVibration, signalling)
        assertEquals(true, signalling.playsSound)
    }

    @Test
    fun aSilentModeAlarmVibratesWithNoSound() {
        val signalling = Signalling.of(alarm.copy(silentMode = true))

        assertEquals(Signalling.VibrationOnly, signalling)
        assertEquals(false, signalling.playsSound)
    }

    @Test
    fun silentModeDefaultsOffWhenReadAsAFlag() {
        assertEquals(Signalling.SoundAndVibration, Signalling.of(silentMode = false))
        assertEquals(Signalling.VibrationOnly, Signalling.of(silentMode = true))
    }

    @Test
    fun silentModeDoesNotChangeWhenTheAlarmFires() {
        val now = ZonedDateTime.parse("2024-06-03T05:00:00+01:00[Europe/London]")

        val ringing = AlarmSchedule.nextTrigger(alarm, now)
        val silent = AlarmSchedule.nextTrigger(alarm.copy(silentMode = true), now)

        assertEquals(ringing, silent)
    }
}
