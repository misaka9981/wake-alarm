package com.misaka9981.alarm.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The fixed cap on an Alarm's continuous signalling.
 *
 * After [duration] of continuous signalling the Alarm stops sounding and
 * vibrating on its own, so a defect or an unanswerable Dismiss Challenge cannot
 * trap the owner or disturb others forever. Expiring the cap does **not** dismiss
 * the Alarm: it stays in the ongoing, uncleared state until the Dismiss Challenge
 * is solved and the Physical Anchor is reached, and the day's Streak is broken.
 * See `CONTEXT.md` and the spec's "Sound cap, Streak, and the Escape Hatch".
 *
 * In Silent Mode the same cap ends the vibration instead of the sound. The cap
 * never changes how hard the Alarm is to dismiss.
 */
class SoundCap(private val duration: Duration = DEFAULT_DURATION) {
    init {
        require(duration > Duration.ZERO) { "duration must be positive, was $duration" }
    }

    /**
     * Whether continuous signalling that began at elapsed zero has reached the
     * cap at [elapsed]. Once true it stays true, because [elapsed] only grows.
     */
    fun hasExpired(elapsed: Duration): Boolean = elapsed >= duration

    companion object {
        /**
         * Ten minutes: long enough to wake the owner and complete the Dismiss
         * Challenge, short enough that a defect does not ring forever.
         */
        val DEFAULT_DURATION: Duration = 10.minutes
    }
}
