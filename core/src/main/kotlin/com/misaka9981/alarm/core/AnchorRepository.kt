package com.misaka9981.alarm.core

/**
 * Port for storing the owner's Physical Anchor configuration.
 *
 * `core` declares the contract; the Android adapter implements it with
 * persistent storage. Persistence itself cannot be unit-tested in CI, so the
 * part that decides what is written — [AnchorCodec] — lives here and is tested.
 */
interface AnchorRepository {
    suspend fun load(): AnchorCatalog
    suspend fun save(catalog: AnchorCatalog)
}
