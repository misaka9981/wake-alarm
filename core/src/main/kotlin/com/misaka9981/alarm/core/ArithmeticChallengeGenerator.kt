package com.misaka9981.alarm.core

import kotlin.random.Random

/**
 * The MVP [ChallengeGenerator]: a multi-step arithmetic problem answered by
 * typing.
 *
 * Generated entirely on the device with no network, so it works at the moment
 * the Alarm rings whatever the connectivity, at a difficulty the app controls
 * precisely. Higher difficulty means more operations and larger operands. The
 * running value is never driven below [MIN_RUNNING_VALUE], so every stated
 * answer is a plain non-negative integer. See ADR-0004.
 */
class ArithmeticChallengeGenerator(
    private val random: Random = Random.Default,
) : ChallengeGenerator {

    override fun generate(difficulty: Int): Challenge {
        require(difficulty >= 1) { "difficulty must be at least 1, was $difficulty" }

        val magnitude = magnitudeFor(difficulty)
        var value = 1 + random.nextInt(magnitude)
        val problem = StringBuilder(value.toString())

        repeat(operationsFor(difficulty)) {
            val operand = 1 + random.nextInt(magnitude)
            if (random.nextBoolean() && value - operand >= MIN_RUNNING_VALUE) {
                value -= operand
                problem.append(" - ").append(operand)
            } else {
                value += operand
                problem.append(" + ").append(operand)
            }
        }

        return Challenge(problem = problem.toString(), answer = value, difficulty = difficulty)
    }

    override fun validate(challenge: Challenge, typedAnswer: String): Boolean =
        typedAnswer.trim().toIntOrNull() == challenge.answer

    private fun operationsFor(difficulty: Int): Int =
        (MIN_OPERATIONS + (difficulty - 1) / 2).coerceAtMost(MAX_OPERATIONS)

    private fun magnitudeFor(difficulty: Int): Int =
        (4 + difficulty * 6).coerceAtMost(MAX_MAGNITUDE)

    private companion object {
        const val MIN_RUNNING_VALUE = 1
        const val MIN_OPERATIONS = 2
        const val MAX_OPERATIONS = 8
        const val MAX_MAGNITUDE = 60
    }
}
