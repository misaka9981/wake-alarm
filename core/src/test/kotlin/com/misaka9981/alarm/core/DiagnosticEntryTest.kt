package com.misaka9981.alarm.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class DiagnosticEntryTest {
    @Test
    fun rejectsASignallingVolumeOutsideThePercentageRange() {
        assertFailsWith<IllegalArgumentException> { entry(signallingVolume = -1) }
        assertFailsWith<IllegalArgumentException> { entry(signallingVolume = 101) }
    }

    @Test
    fun rejectsNegativeWrongAnswers() {
        assertFailsWith<IllegalArgumentException> { entry(wrongAnswers = -1) }
    }

    @Test
    fun rejectsANegativeChallengeDuration() {
        assertFailsWith<IllegalArgumentException> { entry(challengeDuration = (-1).seconds) }
    }

    private fun entry(
        signallingVolume: Int = 100,
        challengeDuration: kotlin.time.Duration = 30.seconds,
        wrongAnswers: Int = 0,
    ) = DiagnosticEntry(
        alarmId = AlarmId("alarm-1"),
        scheduledTime = Instant.parse("2026-09-22T06:30:00Z"),
        firedAt = Instant.parse("2026-09-22T06:30:04Z"),
        missingRequirements = emptySet(),
        signallingVolume = signallingVolume,
        challengeDuration = challengeDuration,
        wrongAnswers = wrongAnswers,
        outcome = FiringOutcome.Dismissed,
    )
}
