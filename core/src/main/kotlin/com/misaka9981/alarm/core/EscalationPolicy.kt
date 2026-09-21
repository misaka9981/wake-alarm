package com.misaka9981.alarm.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The Escalation policy: how hard the Dismiss Challenge becomes.
 *
 * Difficulty rises with every wrong answer and, independently, with the time the
 * session has been running — so both careless guessing and stalling make the
 * Alarm harder to silence. It is a pure function, so the rules are tested
 * directly and the UI never decides difficulty itself.
 *
 * See `CONTEXT.md` (Escalation) and ADR-0002.
 */
class EscalationPolicy(
    private val wrongAnswerPenalty: Int = 1,
    private val elapsedStep: Duration = 60.seconds,
    private val maxDifficulty: Int = 10,
) {
    init {
        require(wrongAnswerPenalty >= 0) {
            "wrongAnswerPenalty must not be negative, was $wrongAnswerPenalty"
        }
        require(elapsedStep > Duration.ZERO) { "elapsedStep must be positive, was $elapsedStep" }
        require(maxDifficulty >= 1) { "maxDifficulty must be at least 1, was $maxDifficulty" }
    }

    /**
     * The difficulty fed into the [ChallengeGenerator]: [base] raised by one step
     * per [wrongAnswers] and per whole [elapsedStep] of [elapsed], capped at
     * [maxDifficulty].
     */
    fun difficulty(base: Int, elapsed: Duration, wrongAnswers: Int): Int {
        require(wrongAnswers >= 0) { "wrongAnswers must not be negative, was $wrongAnswers" }

        val elapsedSteps = elapsed.coerceAtLeast(Duration.ZERO).inWholeMilliseconds /
            elapsedStep.inWholeMilliseconds
        return (base.coerceAtLeast(1) + wrongAnswers * wrongAnswerPenalty + elapsedSteps.toInt())
            .coerceAtMost(maxDifficulty)
    }
}
