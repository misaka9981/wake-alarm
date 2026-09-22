package com.misaka9981.alarm.core

import kotlin.time.Duration

/** The wrong answer the owner just submitted, shown back as immediate feedback. */
data class WrongAnswer(val submitted: String)

/**
 * The events a [DismissalSession] reacts to.
 *
 * There is deliberately no Snooze or skip event: an Alarm cannot be postponed,
 * and only a correct answer can end a session as dismissed. See `CONTEXT.md` and
 * ADR-0002.
 */
sealed interface DismissalEvent {
    /** The owner typed [answer] and submitted it. */
    data class AnswerSubmitted(val answer: String) : DismissalEvent

    /** Time has advanced to [elapsed] since the session started. */
    data class Tick(val elapsed: Duration) : DismissalEvent
}

/**
 * What the Dismiss Challenge screen renders. The screen reports this state; it
 * makes no decisions of its own.
 */
sealed interface DismissalState {
    /** The session is still running and the [challenge] must be answered. */
    data class Ongoing(
        val challenge: Challenge,
        val difficulty: Int,
        val elapsed: Duration,
        val wrongAnswers: Int,
        val feedback: WrongAnswer?,
    ) : DismissalState

    /** The Dismiss Challenge was completed; the Alarm may be silenced. */
    data object Dismissed : DismissalState
}

/**
 * The Dismiss Challenge session state machine, pure and Android-free.
 *
 * A correct answer transitions to [DismissalState.Dismissed]. A wrong answer
 * gives feedback and raises the difficulty of the next challenge; a [Tick] that
 * crosses an [EscalationPolicy] step raises difficulty independently of wrong
 * answers. The machine never exposes a way to skip or postpone, so no Snooze
 * path exists. See `CONTEXT.md`, ADR-0002, and ADR-0004.
 */
class DismissalSession private constructor(
    private val generator: ChallengeGenerator,
    private val policy: EscalationPolicy,
    private val baseDifficulty: Int,
    initialState: DismissalState.Ongoing,
) {
    var state: DismissalState = initialState
        private set

    /**
     * Wrong answers submitted so far, retained even after dismissal, so a firing
     * that has ended can still report how many the owner got wrong to the
     * Diagnostic Log.
     */
    var wrongAnswers: Int = 0
        private set

    /** Applies [event] and returns the resulting state. */
    fun onEvent(event: DismissalEvent): DismissalState {
        state = reduce(state, event)
        return state
    }

    private fun reduce(current: DismissalState, event: DismissalEvent): DismissalState {
        if (current !is DismissalState.Ongoing) return current
        return when (event) {
            is DismissalEvent.AnswerSubmitted -> onAnswer(current, event.answer)
            is DismissalEvent.Tick -> onTick(current, event.elapsed)
        }
    }

    private fun onAnswer(current: DismissalState.Ongoing, answer: String): DismissalState =
        if (generator.validate(current.challenge, answer)) {
            DismissalState.Dismissed
        } else {
            val nextWrongAnswers = current.wrongAnswers + 1
            wrongAnswers = nextWrongAnswers
            val difficulty = policy.difficulty(baseDifficulty, current.elapsed, nextWrongAnswers)
            current.copy(
                challenge = generator.generate(difficulty),
                difficulty = difficulty,
                wrongAnswers = nextWrongAnswers,
                feedback = WrongAnswer(answer),
            )
        }

    private fun onTick(current: DismissalState.Ongoing, elapsed: Duration): DismissalState {
        if (elapsed <= current.elapsed) return current

        val difficulty = policy.difficulty(baseDifficulty, elapsed, current.wrongAnswers)
        return if (difficulty > current.difficulty) {
            // Stalling does not help: crossing an escalation step replaces the
            // challenge with a harder one and clears any stale feedback.
            current.copy(
                challenge = generator.generate(difficulty),
                difficulty = difficulty,
                elapsed = elapsed,
                feedback = null,
            )
        } else {
            current.copy(elapsed = elapsed)
        }
    }

    companion object {
        /** Starts a session whose first challenge sits at [baseDifficulty]. */
        fun start(
            generator: ChallengeGenerator,
            policy: EscalationPolicy,
            baseDifficulty: Int = 1,
        ): DismissalSession {
            val difficulty = policy.difficulty(baseDifficulty, Duration.ZERO, wrongAnswers = 0)
            return DismissalSession(
                generator = generator,
                policy = policy,
                baseDifficulty = baseDifficulty,
                initialState = DismissalState.Ongoing(
                    challenge = generator.generate(difficulty),
                    difficulty = difficulty,
                    elapsed = Duration.ZERO,
                    wrongAnswers = 0,
                    feedback = null,
                ),
            )
        }
    }
}
