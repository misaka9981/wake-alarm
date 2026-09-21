package com.misaka9981.alarm.core

/**
 * The private password that unlocks the Escape Hatch.
 *
 * The owner sets it during onboarding (ticket 10). A blank password could never
 * be a deliberate secret and would make the Escape Hatch trivially reachable, so
 * it cannot be constructed.
 */
@JvmInline
value class EscapeHatchPassword(val value: String) {
    init {
        require(value.isNotBlank()) { "an Escape Hatch password must not be blank" }
    }
}

/**
 * The Escape Hatch: a deliberately effortful last resort that force-silences an
 * Alarm without completing the Dismiss Challenge or reaching the Physical
 * Anchor. It exists for emergencies and defects, never as a normal path out.
 *
 * This class holds only the password check; the hidden long-press and the
 * password entry are platform gestures and stay on the Android side. The Escape
 * Hatch is unavailable until a password is set ([none]), so a missing
 * configuration fails closed. See `CONTEXT.md` and ADR-0002.
 */
class EscapeHatch private constructor(private val password: EscapeHatchPassword?) {

    /**
     * Whether [submitted] is the private password. When no password has been
     * configured nothing unlocks.
     */
    fun unlocks(submitted: String): Boolean = password != null && submitted == password.value

    companion object {
        /** No password is set yet; nothing unlocks. */
        val none: EscapeHatch = EscapeHatch(null)

        /** An Escape Hatch unlocked by [password], or unavailable when it is null. */
        fun of(password: EscapeHatchPassword?): EscapeHatch = EscapeHatch(password)
    }
}
