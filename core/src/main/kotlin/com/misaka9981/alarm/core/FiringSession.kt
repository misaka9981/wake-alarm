package com.misaka9981.alarm.core

import kotlin.time.Duration

/**
 * The events a [FiringSession] reacts to while an Alarm is signalling.
 *
 * There is deliberately no Snooze or skip event: an Alarm cannot be postponed,
 * and it is dismissed only once the Dismiss Challenge is solved and the Physical
 * Anchor is reached. See `CONTEXT.md` and ADR-0002/ADR-0003.
 */
sealed interface FiringEvent {
    /** The owner typed [answer] into the Dismiss Challenge and submitted it. */
    data class AnswerSubmitted(val answer: String) : FiringEvent

    /** Time has advanced to [elapsed] since the Alarm started firing. */
    data class Tick(val elapsed: Duration) : FiringEvent

    /** The owner scanned something; [result] says whether it was the bound anchor. */
    data class AnchorScanned(val result: AnchorScanResult) : FiringEvent
}

/**
 * What the firing screen renders. The screen reports this state; it makes no
 * decisions of its own.
 */
sealed interface FiringState {
    /**
     * The Alarm is still signalling. [challenge] is the Dismiss Challenge state
     * (still ongoing, or already solved while the anchor is still outstanding),
     * [anchorReached] is whether the bound Physical Anchor has been reached, and
     * [anchorFeedback] is the most recent scan result, if any.
     */
    data class Ringing(
        val challenge: DismissalState,
        val anchorReached: Boolean,
        val anchorFeedback: AnchorScanResult?,
    ) : FiringState

    /** Both the Dismiss Challenge and the Physical Anchor are satisfied. */
    data object Dismissed : FiringState
}

/**
 * The end-to-end Alarm firing state machine, pure and Android-free.
 *
 * It composes the Dismiss Challenge ([DismissalSession]) with the Physical
 * Anchor decision ([AnchorScanResult]) and only reports [FiringState.Dismissed]
 * when **both** are satisfied — neither the challenge alone nor the anchor alone
 * silences the Alarm. The challenge itself still escalates with time and wrong
 * answers through [EscalationPolicy]; that is why the whole session, not just the
 * compile-time event type, is the testing seam. See `CONTEXT.md`, ADR-0002, and
 * ADR-0003.
 */
class FiringSession private constructor(
    private val challenge: DismissalSession,
    initialState: FiringState.Ringing,
) {
    var state: FiringState = initialState
        private set

    /** Applies [event] and returns the resulting state. */
    fun onEvent(event: FiringEvent): FiringState {
        state = reduce(state, event)
        return state
    }

    private fun reduce(current: FiringState, event: FiringEvent): FiringState {
        if (current !is FiringState.Ringing) return current
        return when (event) {
            is FiringEvent.AnswerSubmitted ->
                settle(current.copy(challenge = challenge.onEvent(DismissalEvent.AnswerSubmitted(event.answer))))

            is FiringEvent.Tick ->
                settle(current.copy(challenge = challenge.onEvent(DismissalEvent.Tick(event.elapsed))))

            is FiringEvent.AnchorScanned -> settle(
                current.copy(
                    // Reaching the anchor is a fact about the physical world and
                    // cannot be undone by a later wrong scan.
                    anchorReached = current.anchorReached || event.result is AnchorScanResult.Reached,
                    anchorFeedback = event.result,
                ),
            )
        }
    }

    /** Dismisses only once both the Dismiss Challenge and the Physical Anchor are done. */
    private fun settle(current: FiringState.Ringing): FiringState =
        if (current.challenge is DismissalState.Dismissed && current.anchorReached) {
            FiringState.Dismissed
        } else {
            current
        }

    companion object {
        /** Starts firing with a fresh Dismiss Challenge at [baseDifficulty] and the anchor not yet reached. */
        fun start(
            generator: ChallengeGenerator,
            policy: EscalationPolicy,
            baseDifficulty: Int = 1,
        ): FiringSession {
            val challenge = DismissalSession.start(generator, policy, baseDifficulty)
            return FiringSession(
                challenge = challenge,
                initialState = FiringState.Ringing(
                    challenge = challenge.state,
                    anchorReached = false,
                    anchorFeedback = null,
                ),
            )
        }
    }
}
