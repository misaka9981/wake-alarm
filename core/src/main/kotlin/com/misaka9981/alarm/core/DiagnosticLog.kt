package com.misaka9981.alarm.core

import java.time.Instant
import kotlin.time.Duration

/**
 * How one Alarm firing ended, as the Diagnostic Log records it.
 *
 * A firing is [Dismissed] only when the Dismiss Challenge was solved **and** the
 * Physical Anchor was reached. A [CapExpired] firing stopped signalling on its
 * own but stayed uncleared. An [EscapeHatch] firing was force-silenced without
 * either. `core` decides this; the adapter only records it.
 */
enum class FiringOutcome {
    /** The Dismiss Challenge was solved and the Physical Anchor was reached. */
    Dismissed,

    /** The sound cap expired; signalling stopped but the Alarm stayed uncleared. */
    CapExpired,

    /** The Escape Hatch force-silenced the Alarm without the challenge or anchor. */
    EscapeHatch,
}

/**
 * One Alarm firing, as it actually happened, for the Diagnostic Log.
 *
 * Cloud-built sideloaded builds have no device logging, so this record is how a
 * missed or misbehaving Alarm is explained (ADR-0001, `CONTEXT.md`). Every field
 * is a fact observed while the Alarm fired; nothing here is a decision.
 *
 * [scheduledTime] is the instant the Alarm was armed for, when known — it is
 * `null` for a development firing that bypasses the schedule. [firedAt] is when
 * the Alarm actually began firing, so the pair exposes a late fire.
 * [missingRequirements] is the permission state that can make an Alarm
 * unreliable. [signallingVolume] is the alarm stream volume as a percentage of
 * its maximum at fire, so silence is explainable. [challengeDuration] is how long
 * the Alarm signalled before it stopped, [wrongAnswers] is how many answers the
 * owner got wrong, and [outcome] is how it ended.
 */
data class DiagnosticEntry(
    val alarmId: AlarmId,
    val scheduledTime: Instant?,
    val firedAt: Instant,
    val missingRequirements: Set<ReliabilityRequirement>,
    val signallingVolume: Int,
    val challengeDuration: Duration,
    val wrongAnswers: Int,
    val outcome: FiringOutcome,
) {
    init {
        require(signallingVolume in 0..100) {
            "signallingVolume must be a percentage in 0..100, was $signallingVolume"
        }
        require(!challengeDuration.isNegative()) {
            "challengeDuration must not be negative, was $challengeDuration"
        }
        require(wrongAnswers >= 0) { "wrongAnswers must not be negative, was $wrongAnswers" }
    }

    /** Whether the Escape Hatch force-silenced this Alarm. */
    val escapeHatchUsed: Boolean get() = outcome == FiringOutcome.EscapeHatch
}

/**
 * Port for recording and reading the Diagnostic Log.
 *
 * `core` declares the contract; the Android adapter persists it. Persistence
 * itself cannot be unit-tested in CI, so the part that decides what is written —
 * [DiagnosticLogCodec], including the bound on history size — lives here and is
 * tested. Recording is what makes a firing survive the app closing, and the bound
 * is what stops the log growing without limit.
 */
interface DiagnosticLog {
    /** Appends [entry], keeping only the most recent [DiagnosticLogCodec.MAX_ENTRIES]. */
    suspend fun record(entry: DiagnosticEntry)

    /**
     * The recorded firings, oldest first, at most [DiagnosticLogCodec.MAX_ENTRIES]
     * of them.
     */
    suspend fun load(): List<DiagnosticEntry>
}
