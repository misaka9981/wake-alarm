package com.misaka9981.alarm.core

import java.time.DayOfWeek

/** The steps of the first-run setup, in the order the owner meets them. */
enum class OnboardingStep {
    /** Set the wake time, which creates the first Alarm. */
    WakeTime,

    /** Set the private Escape Hatch password. */
    EscapeHatchPassword,
}

/**
 * How far the owner's first-run setup has got, decided from what is actually
 * configured rather than from a separate flag.
 *
 * The app is usable for a full night's sleep exactly when an Alarm exists and
 * the Escape Hatch password is set. A Physical Anchor is optional: the owner may
 * bind one to an Alarm later from settings, and an Alarm without one is
 * dismissed by its Dismiss Challenge alone. Requiring it here would force every
 * Alarm to carry an anchor, which is not what the owner wants. The first-run
 * flow renders [missingSteps]; once [isComplete] that flow is gone and the Alarm
 * list is shown. Because this reads the same configuration the rest of the app
 * uses, finishing setup cannot drift from what the dismissal flow will actually
 * require.
 *
 * See `CONTEXT.md`: an Alarm and an Escape Hatch — never a Snooze.
 */
class Onboarding private constructor(
    /** The earliest Alarm, or `null` before the wake time has been set. */
    val firstAlarm: Alarm?,
    /** Whether the Escape Hatch password has been set. */
    val passwordSet: Boolean,
) {

    /** Whether the app is ready to wake the owner for a full night's sleep. */
    val isComplete: Boolean = firstAlarm != null && passwordSet

    /** The steps still to do, in the order to do them; empty once complete. */
    val missingSteps: List<OnboardingStep> = buildList {
        if (firstAlarm == null) add(OnboardingStep.WakeTime)
        if (!passwordSet) add(OnboardingStep.EscapeHatchPassword)
    }

    companion object {
        /**
         * The Alarm the first-run flow creates from the wake time [time].
         *
         * It is enabled and repeats every day, so it fires on the very next day
         * whichever day that is, and it rings rather than signalling silently. An
         * Alarm created here therefore needs no further tuning before the owner
         * goes to sleep.
         */
        fun firstAlarm(id: AlarmId, time: AlarmTime): Alarm = Alarm(
            id = id,
            time = time,
            repeatDays = DayOfWeek.entries.toSet(),
            enabled = true,
        )

        /**
         * Decides how far setup has got, given what is configured.
         *
         * [firstAlarm] is the earliest Alarm — the one the flow creates and the
         * one settings changes the wake time of. The escape-hatch [password] must
         * be present. A Physical Anchor is optional and is not consulted here.
         */
        fun of(
            alarms: List<Alarm>,
            password: EscapeHatchPassword?,
        ): Onboarding {
            val first = AlarmCatalog.of(alarms).alarms.firstOrNull()
            return Onboarding(
                firstAlarm = first,
                passwordSet = password != null,
            )
        }
    }
}
