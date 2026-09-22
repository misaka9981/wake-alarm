package com.misaka9981.alarm.core

/**
 * Port for reading the morning statistics.
 *
 * `core` declares the contract and decides every figure ([WakeStatistics] is
 * derived from the recorded history by [WakeStatistics.of]); the Android adapter
 * only supplies the recorded Diagnostic Log and the device's time zone.
 * Persistence itself cannot be unit-tested in CI, which is why the deciding part
 * lives here and is tested. See the spec's "Repository ports" and "Statistics".
 */
interface StatisticsRepository {
    /** The statistics derived from the recorded Diagnostic Log. */
    suspend fun load(): WakeStatistics
}
