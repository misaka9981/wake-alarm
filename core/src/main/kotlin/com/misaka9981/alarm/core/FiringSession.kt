package com.misaka9981.alarm.core

import kotlin.time.Duration

/**
 * The events a [FiringSession] reacts to while an Alarm is signalling.
 *
 * There is deliberately no Snooze or skip event: an Alarm cannot be postponed,
 * and it is dismissed once the Dismiss Challenge is solved and, when the Alarm
 * has a bound Physical Anchor, that Anchor is also reached. The Escape Hatch is
 * the one deliberate exception — it force-silences an Alarm without either, but
 * only after a hidden long-press and the correct private password. See
 * `CONTEXT.md` and ADR-0002/ADR-0003.
 */
sealed interface FiringEvent {
    /** The owner typed [answer] into the Dismiss Challenge and submitted it. */
    data class AnswerSubmitted(val answer: String) : FiringEvent

    /** Time has advanced to [elapsed] since the Alarm started firing. */
    data class Tick(val elapsed: Duration) : FiringEvent

    /** The owner scanned something; [result] says whether it was the bound anchor. */
    data class AnchorScanned(val result: AnchorScanResult) : FiringEvent

    /** The owner long-pressed the hidden Escape Hatch target, revealing its prompt. */
    data object EscapeHatchLongPressed : FiringEvent

    /** The owner typed [password] into the revealed Escape Hatch prompt. */
    data class EscapeHatchPasswordSubmitted(val password: String) : FiringEvent

    /** The owner backed out of the Escape Hatch prompt without unlocking it. */
    data object EscapeHatchCancelled : FiringEvent
}

/**
 * What the firing screen renders. The screen reports this state; it makes no
 * decisions of its own.
 */
sealed interface FiringState {
    /**
     * The Alarm is still firing. [challenge] is the Dismiss Challenge state
     * (still ongoing, or already solved while the anchor is still outstanding),
     * [anchorReached] is whether the bound Physical Anchor has been reached, and
     * [anchorFeedback] is the most recent scan result, if any.
     *
     * [capExpired] is whether the [SoundCap] has stopped the sound and vibration.
     * It is deliberately independent of [challenge]: after the cap the Alarm is
     * still firing and uncleared, and completing the challenge and anchor still
     * dismisses it. The screen renders this; it never decides it.
     *
     * [escapeHatchRevealed] is whether the hidden Escape Hatch prompt is showing.
     * It only becomes true after the long-press, so neither the password entry
     * alone nor a reflex can reach the Escape Hatch.
     */
    data class Ringing(
        val challenge: DismissalState,
        val anchorReached: Boolean,
        val anchorFeedback: AnchorScanResult?,
        val capExpired: Boolean = false,
        val escapeHatchRevealed: Boolean = false,
    ) : FiringState

    /** The Dismiss Challenge is solved and every required Anchor has been reached. */
    data object Dismissed : FiringState

    /**
     * The Escape Hatch force-silenced the Alarm without the Dismiss Challenge or
     * the Physical Anchor. It is a deliberate last resort for emergencies and
     * defects: the app records the use and the day's Streak is broken.
     */
    data object EscapeHatchUsed : FiringState
}

/**
 * The facts a finished Alarm firing reports to the Diagnostic Log.
 *
 * It is a value, not a decision: the recording adapter combines it with what it
 * observed on the platform (the schedule, the fire instant, the permissions, the
 * volume). Keeping it in `core` lets the recording path be tested without a
 * device.
 */
data class FiringSummary(
    /** How long the Alarm signalled before it stopped. */
    val ringingDuration: Duration,
    /** How many wrong answers the owner submitted during the firing. */
    val wrongAnswers: Int,
) {
    init {
        require(!ringingDuration.isNegative()) {
            "ringingDuration must not be negative, was $ringingDuration"
        }
        require(wrongAnswers >= 0) { "wrongAnswers must not be negative, was $wrongAnswers" }
    }
}

/**
 * The end-to-end Alarm firing state machine, pure and Android-free.
 *
 * It composes the Dismiss Challenge ([DismissalSession]) with the Physical
 * Anchor decision ([AnchorScanResult]). When [anchorRequired] the Alarm is
 * dismissed only when **both** are satisfied — neither the challenge alone nor
 * the anchor alone silences it. When the Alarm has no bound anchor the anchor is
 * not required, so the Dismiss Challenge alone dismisses it; the anchor is an
 * optional extra the owner may add to an Alarm. The challenge itself still
 * escalates with time and wrong answers through [EscalationPolicy]; that is why
 * the whole session, not just the compile-time event type, is the testing seam.
 *
 * It also applies the [SoundCap] to the elapsed ringing time. Once the cap has
 * expired, [FiringState.Ringing.capExpired] is true and the adapter stops the
 * sound and vibration, but the state stays [FiringState.Ringing] — the Alarm is
 * uncleared until the challenge is solved and, when [anchorRequired], the anchor
 * is reached.
 *
 * Finally it owns the Escape Hatch: only the hidden long-press followed by the
 * correct private password reaches [FiringState.EscapeHatchUsed], which
 * force-silences the Alarm without the challenge or the anchor. See `CONTEXT.md`,
 * ADR-0002, and ADR-0003.
 */
class FiringSession private constructor(
    private val challenge: DismissalSession,
    private val soundCap: SoundCap,
    private val escapeHatch: EscapeHatch,
    private val anchorRequired: Boolean,
    private var elapsed: Duration,
    initialState: FiringState.Ringing,
) {
    var state: FiringState = initialState
        private set

    /** Applies [event] and returns the resulting state. */
    fun onEvent(event: FiringEvent): FiringState {
        state = reduce(state, event)
        return state
    }

    /**
     * The facts to record in the Diagnostic Log: how long the Alarm signalled and
     * how many wrong answers the owner submitted, both retained after the firing
     * ends. [elapsed] is the ringing time, which only grows.
     */
    fun summary(): FiringSummary = FiringSummary(
        ringingDuration = elapsed,
        wrongAnswers = challenge.wrongAnswers,
    )

    private fun reduce(current: FiringState, event: FiringEvent): FiringState {
        if (current !is FiringState.Ringing) return current
        return when (event) {
            is FiringEvent.AnswerSubmitted ->
                settle(current.copy(challenge = challenge.onEvent(DismissalEvent.AnswerSubmitted(event.answer))))

            is FiringEvent.Tick -> {
                // Ringing time only grows; a backwards tick cannot resurrect sound
                // that the cap has already stopped.
                if (event.elapsed > elapsed) elapsed = event.elapsed
                settle(
                    current.copy(
                        challenge = challenge.onEvent(DismissalEvent.Tick(event.elapsed)),
                        capExpired = current.capExpired || soundCap.hasExpired(elapsed),
                    ),
                )
            }

            is FiringEvent.AnchorScanned -> settle(
                current.copy(
                    // Reaching the anchor is a fact about the physical world and
                    // cannot be undone by a later wrong scan.
                    anchorReached = current.anchorReached || event.result is AnchorScanResult.Reached,
                    anchorFeedback = event.result,
                ),
            )

            // The long-press only reveals the prompt; by itself it silences nothing.
            is FiringEvent.EscapeHatchLongPressed -> current.copy(escapeHatchRevealed = true)

            // Force-silencing requires both gestures: the prompt must already be
            // revealed by the long-press, and the password must be correct. A
            // wrong password leaves the Alarm ringing.
            is FiringEvent.EscapeHatchPasswordSubmitted ->
                if (current.escapeHatchRevealed && escapeHatch.unlocks(event.password)) {
                    FiringState.EscapeHatchUsed
                } else {
                    current
                }

            is FiringEvent.EscapeHatchCancelled -> current.copy(escapeHatchRevealed = false)
        }
    }

    /**
     * Dismisses once the Dismiss Challenge is solved and, when [anchorRequired],
     * the Physical Anchor has also been reached. An Alarm with no bound anchor
     * requires no scan, so its challenge alone clears it.
     */
    private fun settle(current: FiringState.Ringing): FiringState =
        if (current.challenge is DismissalState.Dismissed && (!anchorRequired || current.anchorReached)) {
            FiringState.Dismissed
        } else {
            current
        }

    companion object {
        /**
         * Starts firing with a fresh Dismiss Challenge at [baseDifficulty], the
         * anchor not yet reached, the sound cap not yet expired, and the Escape
         * Hatch prompt hidden. [escapeHatch] is the owner's private password; it
         * defaults to [EscapeHatch.none], so an Alarm without a configured
         * password cannot be force-silenced. [anchorRequired] is whether the
         * Alarm has a bound Physical Anchor; it defaults to `true`, preserving
         * the canonical behaviour, and an Alarm with no bound anchor starts with
         * it `false` so the Dismiss Challenge alone dismisses it.
         */
        fun start(
            generator: ChallengeGenerator,
            policy: EscalationPolicy,
            baseDifficulty: Int = 1,
            soundCap: SoundCap = SoundCap(),
            escapeHatch: EscapeHatch = EscapeHatch.none,
            anchorRequired: Boolean = true,
        ): FiringSession {
            val challenge = DismissalSession.start(generator, policy, baseDifficulty)
            return FiringSession(
                challenge = challenge,
                soundCap = soundCap,
                escapeHatch = escapeHatch,
                anchorRequired = anchorRequired,
                elapsed = Duration.ZERO,
                initialState = FiringState.Ringing(
                    challenge = challenge.state,
                    anchorReached = false,
                    anchorFeedback = null,
                    capExpired = false,
                    escapeHatchRevealed = false,
                ),
            )
        }
    }
}
