package com.misaka9981.alarm.core

/**
 * Port for storing the owner's Alarm configuration.
 *
 * `core` declares the contract; the Android adapter implements it with
 * persistent storage. Persistence itself cannot be unit-tested in CI, so the
 * part that decides what is written — [AlarmCodec] — lives here and is tested.
 */
interface AlarmRepository {
    suspend fun load(): List<Alarm>
    suspend fun save(alarms: List<Alarm>)
}
