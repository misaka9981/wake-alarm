package com.misaka9981.alarm.core

/**
 * Port for storing the owner's private Escape Hatch password.
 *
 * `core` declares the contract; the Android adapter persists it. The password is
 * only ever compared, never displayed, so the adapter stores it as-is and `core`
 * validates it through [EscapeHatchPassword].
 */
interface EscapeHatchRepository {
    /** The configured password, or `null` if the owner has not set one. */
    suspend fun load(): EscapeHatchPassword?

    /** Stores [password], replacing any earlier one. */
    suspend fun save(password: EscapeHatchPassword)
}
