package com.misaka9981.alarm.core

import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmCatalogTest {
    private val weekdayMorning = Alarm(
        id = AlarmId("weekday"),
        time = AlarmTime(6, 30),
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
        enabled = true,
    )
    private val weekend = Alarm(
        id = AlarmId("weekend"),
        time = AlarmTime(9, 0),
        repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        enabled = true,
    )

    @Test
    fun startsEmpty() {
        assertEquals(emptyList(), AlarmCatalog.of(emptyList()).alarms)
    }

    @Test
    fun createAddsAnAlarm() {
        val catalog = AlarmCatalog.of(emptyList()).add(weekdayMorning)

        assertEquals(listOf(weekdayMorning), catalog.alarms)
    }

    @Test
    fun multipleAlarmsCoexist() {
        val catalog = AlarmCatalog.of(emptyList()).add(weekdayMorning).add(weekend)

        assertEquals(2, catalog.alarms.size)
        assertEquals(setOf(weekdayMorning, weekend), catalog.alarms.toSet())
    }

    @Test
    fun editReplacesTheAlarmWithTheSameId() {
        val edited = weekdayMorning.copy(time = AlarmTime(7, 15), enabled = false)

        val catalog = AlarmCatalog.of(listOf(weekdayMorning)).update(edited)

        assertEquals(listOf(edited), catalog.alarms)
    }

    @Test
    fun deleteRemovesTheAlarm() {
        val catalog = AlarmCatalog.of(listOf(weekdayMorning, weekend)).delete(weekdayMorning.id)

        assertEquals(listOf(weekend), catalog.alarms)
        assertNull(catalog.find(weekdayMorning.id))
    }

    @Test
    fun deleteOfAnUnknownIdChangesNothing() {
        val catalog = AlarmCatalog.of(listOf(weekdayMorning)).delete(AlarmId("missing"))

        assertEquals(listOf(weekdayMorning), catalog.alarms)
    }

    @Test
    fun disablingAndEnablingAnAlarmKeepsIt() {
        val disabled = AlarmCatalog.of(listOf(weekdayMorning)).setEnabled(weekdayMorning.id, false)
        assertEquals(false, disabled.find(weekdayMorning.id)?.enabled)
        assertEquals(1, disabled.alarms.size)

        val enabled = disabled.setEnabled(weekdayMorning.id, true)
        assertEquals(true, enabled.find(weekdayMorning.id)?.enabled)
    }

    @Test
    fun alarmsAreOrderedByTime() {
        val late = weekend.copy(time = AlarmTime(23, 0))
        val early = weekend.copy(id = AlarmId("early"), time = AlarmTime(5, 0))

        val catalog = AlarmCatalog.of(listOf(late, weekdayMorning, early))

        assertEquals(listOf(early, weekdayMorning, late), catalog.alarms)
    }

    @Test
    fun theOnlyEnabledAlarmIsReportedAsTheLastOne() {
        val catalog = AlarmCatalog.of(listOf(weekdayMorning, weekend.copy(enabled = false)))

        assertEquals(true, catalog.isOnlyEnabled(weekdayMorning.id))
        assertEquals(false, catalog.isOnlyEnabled(weekend.id))
    }

    @Test
    fun withSeveralEnabledAlarmsNoneIsTheLastEnabledOne() {
        val catalog = AlarmCatalog.of(listOf(weekdayMorning, weekend))

        assertEquals(false, catalog.isOnlyEnabled(weekdayMorning.id))
        assertEquals(false, catalog.isOnlyEnabled(weekend.id))
    }

    @Test
    fun withNoEnabledAlarmsNoneIsTheLastEnabledOne() {
        val catalog = AlarmCatalog.of(
            listOf(weekdayMorning.copy(enabled = false), weekend.copy(enabled = false)),
        )

        assertEquals(false, catalog.isOnlyEnabled(weekdayMorning.id))
    }
}
