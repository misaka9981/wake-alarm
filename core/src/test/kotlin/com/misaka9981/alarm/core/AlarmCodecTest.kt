package com.misaka9981.alarm.core

import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AlarmCodecTest {
    private val alarms = listOf(
        Alarm(
            id = AlarmId("6f9a1c2e-0001"),
            time = AlarmTime(6, 30),
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            enabled = true,
        ),
        Alarm(
            id = AlarmId("6f9a1c2e-0002"),
            time = AlarmTime(9, 5),
            repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            enabled = false,
            silentMode = true,
        ),
    )

    @Test
    fun roundTripsEveryAlarm() {
        val decoded = AlarmCodec.decode(AlarmCodec.encode(alarms))

        assertEquals(alarms, decoded)
    }

    @Test
    fun roundTripsAnEmptyConfiguration() {
        assertEquals(emptyList(), AlarmCodec.decode(AlarmCodec.encode(emptyList())))
    }

    @Test
    fun rejectsUnknownHeader() {
        assertFailsWith<AlarmFormatException> { AlarmCodec.decode("not-alarm-data") }
    }

    @Test
    fun rejectsUnknownVersion() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 99")
        }
    }

    @Test
    fun rejectsAStructurallyBrokenRecord() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 1\nonly-one-field")
        }
    }

    @Test
    fun rejectsAnOutOfRangeTime() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 1\nid|24|00|1|1")
        }
    }

    @Test
    fun rejectsAnUnknownRepeatDay() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 1\nid|07|00|1|8")
        }
    }

    @Test
    fun rejectsAnAlarmWithNoRepeatDays() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 1\nid|07|00|1|")
        }
    }

    @Test
    fun readsLegacyDataWithSilentModeOff() {
        val decoded = AlarmCodec.decode("wake-alarm-alarms 1\nlegacy|07|00|1|1,2")

        assertEquals(1, decoded.size)
        assertEquals(false, decoded.single().silentMode)
        assertEquals(
            Alarm(
                id = AlarmId("legacy"),
                time = AlarmTime(7, 0),
                repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                enabled = true,
                silentMode = false,
            ),
            decoded.single(),
        )
    }

    @Test
    fun roundTripsSilentMode() {
        val silent = Alarm(
            id = AlarmId("silent"),
            time = AlarmTime(6, 0),
            repeatDays = setOf(DayOfWeek.MONDAY),
            enabled = true,
            silentMode = true,
        )

        assertEquals(listOf(silent), AlarmCodec.decode(AlarmCodec.encode(listOf(silent))))
    }

    @Test
    fun rejectsAMalformedSilentFlag() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 2\nid|07|00|1|2|1")
        }
    }

    @Test
    fun rejectsAV2RecordWithTheLegacyFieldCount() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 2\nid|07|00|1|1")
        }
    }

    @Test
    fun roundTripsDefaultDifficulty() {
        val hard = Alarm(
            id = AlarmId("hard"),
            time = AlarmTime(6, 0),
            repeatDays = setOf(DayOfWeek.MONDAY),
            enabled = true,
            defaultDifficulty = 4,
        )

        assertEquals(listOf(hard), AlarmCodec.decode(AlarmCodec.encode(listOf(hard))))
    }

    @Test
    fun readsVersion2DataWithTheGentlestDefaultDifficulty() {
        val decoded = AlarmCodec.decode("wake-alarm-alarms 2\nlegacy|07|00|1|1|1,2")

        assertEquals(1, decoded.single().defaultDifficulty)
        assertEquals(true, decoded.single().silentMode)
    }

    @Test
    fun rejectsAVersion3RecordWithTheVersion2FieldCount() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 3\nid|07|00|1|1|1")
        }
    }

    @Test
    fun rejectsAMalformedDefaultDifficulty() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 3\nid|07|00|1|0|x|1")
        }
    }

    @Test
    fun rejectsAnOutOfRangeDefaultDifficulty() {
        assertFailsWith<AlarmFormatException> {
            AlarmCodec.decode("wake-alarm-alarms 3\nid|07|00|1|0|11|1")
        }
    }
}
