package com.misaka9981.alarm.core

import java.time.Instant

/**
 * One recorded use of the Escape Hatch.
 *
 * It is recorded so the owner can see how often they resort to it (Diagnostic
 * Log, ticket 08) and so the day's Streak can be broken and shown (Statistics,
 * ticket 09). [usedAt] is the instant the Alarm was force-silenced.
 */
data class EscapeHatchUse(
    val alarmId: AlarmId,
    val usedAt: Instant,
)

/**
 * Port for recording Escape Hatch uses.
 *
 * `core` declares the contract; the Android adapter persists it. Persistence
 * itself cannot be unit-tested in CI, so the part that decides what is written —
 * [EscapeHatchUseCodec] — lives here and is tested. Recording every use is what
 * makes "each use is recorded" and "each use breaks the day's Streak" hold
 * beyond the moment the Alarm is silenced.
 */
interface EscapeHatchLog {
    /** Appends one use, keeping every earlier use. */
    suspend fun record(use: EscapeHatchUse)

    /** Every recorded use, oldest first. */
    suspend fun load(): List<EscapeHatchUse>
}
