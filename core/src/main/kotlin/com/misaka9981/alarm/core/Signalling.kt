package com.misaka9981.alarm.core

/**
 * How an Alarm signals its owner.
 *
 * Silent Mode changes only this: a Silent Mode Alarm signals by vibration only,
 * with no sound. It never changes how hard the Alarm is to dismiss — [FiringSession],
 * [EscalationPolicy], and the Streak rules never see this value — so silencing the
 * sound cannot become a shortcut past the Dismiss Challenge.
 *
 * The [SoundCap] applies in both modes: with [VibrationOnly] the same cap ends the
 * vibration instead of the sound. See `CONTEXT.md` (Silent Mode).
 */
enum class Signalling(val playsSound: Boolean) {
    /** Sound rising to maximum volume plus vibration: how an Alarm signals by default. */
    SoundAndVibration(playsSound = true),

    /** Silent Mode: vibration only, with no sound. */
    VibrationOnly(playsSound = false),
    ;

    companion object {
        /** The signalling [alarm]'s Silent Mode calls for. */
        fun of(alarm: Alarm): Signalling = of(alarm.silentMode)

        /** The signalling a Silent Mode flag calls for; off by default. */
        fun of(silentMode: Boolean): Signalling =
            if (silentMode) VibrationOnly else SoundAndVibration
    }
}
