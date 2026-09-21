package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EscapeHatchTest {
    private val password = EscapeHatchPassword("open-sesame")

    @Test
    fun theCorrectPasswordUnlocksTheEscapeHatch() {
        assertTrue(EscapeHatch.of(password).unlocks("open-sesame"))
    }

    @Test
    fun aWrongPasswordDoesNotUnlockTheEscapeHatch() {
        assertFalse(EscapeHatch.of(password).unlocks("sesame"))
        assertFalse(EscapeHatch.of(password).unlocks(""))
    }

    @Test
    fun withoutAPasswordNothingUnlocksTheEscapeHatch() {
        assertFalse(EscapeHatch.none.unlocks("open-sesame"))
        assertFalse(EscapeHatch.none.unlocks(""))
    }

    @Test
    fun aBlankPasswordCannotBeConfigured() {
        assertFailsWith<IllegalArgumentException> { EscapeHatchPassword("") }
        assertFailsWith<IllegalArgumentException> { EscapeHatchPassword("   ") }
    }
}
