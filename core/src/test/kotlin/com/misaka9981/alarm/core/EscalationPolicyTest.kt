package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class EscalationPolicyTest {
    private val policy = EscalationPolicy(
        wrongAnswerPenalty = 1,
        elapsedStep = 30.seconds,
        maxDifficulty = 8,
    )

    @Test
    fun aFreshSessionSitsAtTheBaseDifficulty() {
        assertEquals(3, policy.difficulty(base = 3, elapsed = Duration.ZERO, wrongAnswers = 0))
    }

    @Test
    fun wrongAnswersRaiseDifficultyIndependentlyOfElapsedTime() {
        val none = policy.difficulty(base = 2, elapsed = Duration.ZERO, wrongAnswers = 0)
        val two = policy.difficulty(base = 2, elapsed = Duration.ZERO, wrongAnswers = 2)

        assertEquals(none + 2, two)
    }

    @Test
    fun elapsedTimeRaisesDifficultyIndependentlyOfWrongAnswers() {
        val start = policy.difficulty(base = 2, elapsed = Duration.ZERO, wrongAnswers = 0)
        val later = policy.difficulty(base = 2, elapsed = 90.seconds, wrongAnswers = 0)

        assertEquals(start + 3, later)
    }

    @Test
    fun wrongAnswersAndElapsedTimeCompound() {
        assertEquals(5, policy.difficulty(base = 1, elapsed = 60.seconds, wrongAnswers = 2))
    }

    @Test
    fun aPartialStepDoesNotRaiseDifficulty() {
        assertEquals(1, policy.difficulty(base = 1, elapsed = 29.seconds, wrongAnswers = 0))
    }

    @Test
    fun difficultyIsCappedAtTheMaximum() {
        assertEquals(8, policy.difficulty(base = 1, elapsed = 10.hours, wrongAnswers = 50))
    }

    @Test
    fun theBaseDifficultyIsNeverBelowOne() {
        assertEquals(1, policy.difficulty(base = 0, elapsed = Duration.ZERO, wrongAnswers = 0))
    }
}
