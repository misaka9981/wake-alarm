package com.misaka9981.alarm.core

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class DismissalChartTest {

    @Test
    fun noDismissalsGiveNoBars() {
        val bars = emptyList<DismissalRecord>().chartBars(DismissalMetric.RingingSeconds)

        assertEquals(emptyList(), bars)
    }

    @Test
    fun ringingSecondsAreScaledAgainstTheLongestRing() {
        val bars = listOf(
            record(day = 21, ringingSeconds = 30, wrongAnswers = 0),
            record(day = 22, ringingSeconds = 10, wrongAnswers = 0),
        ).chartBars(DismissalMetric.RingingSeconds)

        assertEquals(listOf(30L, 10L), bars.map { it.value })
        assertEquals(listOf(1f, 10f / 30f), bars.map { it.fraction })
    }

    @Test
    fun wrongAnswersCanBePlottedInstead() {
        val bars = listOf(
            record(day = 21, ringingSeconds = 30, wrongAnswers = 1),
            record(day = 22, ringingSeconds = 10, wrongAnswers = 4),
        ).chartBars(DismissalMetric.WrongAnswers)

        assertEquals(listOf(1L, 4L), bars.map { it.value })
        assertEquals(listOf(10f / 40f, 1f), bars.map { it.fraction })
    }

    @Test
    fun daysAreKeptInTheOrderGiven() {
        val bars = listOf(
            record(day = 22, ringingSeconds = 5, wrongAnswers = 0),
            record(day = 21, ringingSeconds = 5, wrongAnswers = 0),
        ).chartBars(DismissalMetric.RingingSeconds)

        assertEquals(
            listOf(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 21)),
            bars.map { it.day },
        )
    }

    @Test
    fun allZeroFiguresGiveZeroHeightBars() {
        val bars = listOf(
            record(day = 21, ringingSeconds = 0, wrongAnswers = 0),
            record(day = 22, ringingSeconds = 0, wrongAnswers = 3),
        ).chartBars(DismissalMetric.RingingSeconds)

        assertEquals(listOf(0f, 0f), bars.map { it.fraction })
    }

    private fun record(day: Int, ringingSeconds: Int, wrongAnswers: Int): DismissalRecord =
        DismissalRecord(
            day = LocalDate.of(2026, 9, day),
            dismissedAt = LocalTime.of(7, 0),
            ringingDuration = ringingSeconds.seconds,
            wrongAnswers = wrongAnswers,
        )
}
