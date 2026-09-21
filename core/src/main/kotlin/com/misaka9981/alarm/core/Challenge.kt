package com.misaka9981.alarm.core

/**
 * A Dismiss Challenge: the problem the owner must solve before an Alarm is
 * silenced. [answer] is the answer [problem] states, so a generated challenge is
 * always verifiable and no bug can make an Alarm impossible to dismiss.
 *
 * See `CONTEXT.md`: a Dismiss Challenge, never a task or puzzle.
 */
data class Challenge(
    val problem: String,
    val answer: Int,
    val difficulty: Int,
) {
    init {
        require(problem.isNotBlank()) { "a Challenge must state a problem" }
        require(difficulty >= 1) { "difficulty must be at least 1, was $difficulty" }
    }
}

/**
 * Produces a Dismiss Challenge at an integer difficulty and validates a typed
 * answer.
 *
 * The interface exists so the MVP's multi-step arithmetic problem can be
 * replaced or joined by other offline challenge types without touching
 * [DismissalSession]. See ADR-0004.
 */
interface ChallengeGenerator {
    /** Generates a challenge at [difficulty]. */
    fun generate(difficulty: Int): Challenge

    /** Whether [typedAnswer] is the correct answer for [challenge]. */
    fun validate(challenge: Challenge, typedAnswer: String): Boolean
}
