package com.misaka9981.alarm.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The Alarm sound rising to maximum volume.
 *
 * Maps the elapsed ringing time to a fraction of the alarm stream's volume. It
 * starts at [startFraction] so the Alarm is immediately audible — an alarm that
 * faded in from silence could be slept through — and rises monotonically to
 * maximum at [rampUp], never falling. The Android adapter applies the fraction to
 * the platform player; the curve itself is pure and tested here.
 */
class VolumeRamp(
    private val startFraction: Float = DEFAULT_START_FRACTION,
    private val rampUp: Duration = DEFAULT_RAMP_UP,
) {
    init {
        require(startFraction > 0f && startFraction < 1f) {
            "startFraction must be between 0 and 1 exclusive, was $startFraction"
        }
        require(rampUp > Duration.ZERO) { "rampUp must be positive, was $rampUp" }
    }

    /** The volume fraction at [elapsed] since the Alarm started signalling. */
    fun fractionAt(elapsed: Duration): Float {
        val clamped = elapsed.coerceAtLeast(Duration.ZERO)
        if (clamped >= rampUp) return 1f
        val progress = clamped.inWholeMilliseconds.toDouble() / rampUp.inWholeMilliseconds
        return (startFraction + (1f - startFraction) * progress).toFloat()
    }

    companion object {
        const val DEFAULT_START_FRACTION: Float = 0.2f
        val DEFAULT_RAMP_UP: Duration = 30.seconds
    }
}
