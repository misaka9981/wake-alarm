package com.misaka9981.alarm.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArithmeticChallengeGeneratorTest {
    private val generator = ArithmeticChallengeGenerator(Random(1))

    @Test
    fun generatedChallengeAlwaysValidatesItsStatedAnswer() {
        for (difficulty in 1..12) {
            val seeded = ArithmeticChallengeGenerator(Random(difficulty.toLong()))
            repeat(250) {
                val challenge = seeded.generate(difficulty)
                assertTrue(
                    seeded.validate(challenge, challenge.answer.toString()),
                    "the stated answer must validate for $challenge",
                )
            }
        }
    }

    @Test
    fun aWrongAnswerDoesNotValidate() {
        val challenge = generator.generate(1)

        assertFalse(generator.validate(challenge, (challenge.answer + 1).toString()))
    }

    @Test
    fun aNonNumericAnswerDoesNotValidate() {
        val challenge = generator.generate(1)

        assertFalse(generator.validate(challenge, ""))
        assertFalse(generator.validate(challenge, "not a number"))
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        val challenge = generator.generate(1)

        assertTrue(generator.validate(challenge, "  ${challenge.answer}  "))
    }

    @Test
    fun everyChallengeIsMultiStep() {
        for (difficulty in 1..12) {
            val challenge = ArithmeticChallengeGenerator(Random(difficulty.toLong())).generate(difficulty)

            assertTrue(
                operatorCount(challenge.problem) >= 2,
                "a Dismiss Challenge must be multi-step: ${challenge.problem}",
            )
        }
    }

    @Test
    fun higherDifficultyAddsMoreOperations() {
        val easy = ArithmeticChallengeGenerator(Random(7)).generate(1)
        val hard = ArithmeticChallengeGenerator(Random(7)).generate(9)

        assertTrue(operatorCount(hard.problem) > operatorCount(easy.problem))
    }

    @Test
    fun difficultyIsCarriedOnTheGeneratedChallenge() {
        assertEquals(4, ArithmeticChallengeGenerator(Random(3)).generate(4).difficulty)
    }

    @Test
    fun generationIsDeterministicForASeed() {
        val first = ArithmeticChallengeGenerator(Random(42)).generate(3)
        val second = ArithmeticChallengeGenerator(Random(42)).generate(3)

        assertEquals(first, second)
    }

    @Test
    fun difficultyMustBeAtLeastOne() {
        assertFailsWith<IllegalArgumentException> { generator.generate(0) }
    }

    private fun operatorCount(problem: String): Int =
        problem.count { it == '+' || it == '-' }
}
