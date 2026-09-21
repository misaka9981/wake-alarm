package com.misaka9981.alarm.core

/**
 * A platform capability an Alarm's reliability depends on.
 *
 * The alarm clock is useless if a guarantee it relies on is silently missing, so
 * each of these is surfaced to the owner for granting and warned about when
 * absent. See the spec's "Firing reliably" stories and ADR-0001.
 */
enum class ReliabilityRequirement {
    /** Exact alarms, gated on Android 12 (API 31) and above. */
    ExactAlarm,

    /** Full-screen intents, revocable from Android 14 (API 34). */
    FullScreenIntent,

    /** Exemption from battery optimisation, which can defer or drop an Alarm. */
    BatteryOptimisation,

    /** Notification policy access, needed for the Alarm to bypass Do Not Disturb. */
    DoNotDisturbAccess,
}

/**
 * What the platform currently grants, as the Android adapter observes it.
 *
 * Every field is what the adapter read from the platform; this type carries no
 * Android dependency, so the decision can be tested.
 */
data class ReliabilityGrants(
    val exactAlarm: Boolean,
    val fullScreenIntent: Boolean,
    val batteryOptimisationExempt: Boolean,
    val doNotDisturbAccess: Boolean,
) {
    /** Whether this device grants [requirement]. */
    fun isGranted(requirement: ReliabilityRequirement): Boolean = when (requirement) {
        ReliabilityRequirement.ExactAlarm -> exactAlarm
        ReliabilityRequirement.FullScreenIntent -> fullScreenIntent
        ReliabilityRequirement.BatteryOptimisation -> batteryOptimisationExempt
        ReliabilityRequirement.DoNotDisturbAccess -> doNotDisturbAccess
    }
}

/**
 * Decides which reliability requirements apply at a given Android API level and
 * which of those are missing, so the owner can be guided to grant them and warned
 * when an Alarm may not fire.
 *
 * Requirements are API-level dependent because the platform does not gate them
 * uniformly: exact alarms need no grant before API 31, and full-screen intents
 * are only revocable from API 34. This is the decidable part; reading the real
 * platform state and opening the right settings page are the adapter's job.
 */
class ReliabilityCheck(private val sdkInt: Int) {
    init {
        require(sdkInt >= 1) { "sdkInt must be positive, was $sdkInt" }
    }

    /** The requirements that apply on this device. */
    fun required(): Set<ReliabilityRequirement> = buildSet {
        if (sdkInt >= EXACT_ALARM_MIN_SDK) add(ReliabilityRequirement.ExactAlarm)
        if (sdkInt >= FULL_SCREEN_INTENT_MIN_SDK) add(ReliabilityRequirement.FullScreenIntent)
        add(ReliabilityRequirement.BatteryOptimisation)
        add(ReliabilityRequirement.DoNotDisturbAccess)
    }

    /** The applicable requirements that [grants] does not satisfy. */
    fun missing(grants: ReliabilityGrants): Set<ReliabilityRequirement> =
        required().filterTo(linkedSetOf()) { requirement -> !grants.isGranted(requirement) }

    /** Whether every applicable requirement is satisfied, so an Alarm can be relied on. */
    fun isReliable(grants: ReliabilityGrants): Boolean = missing(grants).isEmpty()

    private companion object {
        const val EXACT_ALARM_MIN_SDK = 31
        const val FULL_SCREEN_INTENT_MIN_SDK = 34
    }
}
