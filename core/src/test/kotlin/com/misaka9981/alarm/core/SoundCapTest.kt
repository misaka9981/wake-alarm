package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SoundCapTest {
    private val cap = SoundCap(10.minutes)

    @Test
    fun signalsRightUpToTheCap() {
        assertFalse(cap.hasExpired(9.minutes + 59.seconds))
    }

    @Test
    fun stopsExactlyAtTheCap() {
        assertTrue(cap.hasExpired(10.minutes))
    }

    @Test
    fun staysStoppedAfterTheCap() {
        assertTrue(cap.hasExpired(2.hours))
    }

    @Test
    fun aFreshlyStartedAlarmHasNotExpired() {
        assertFalse(cap.hasExpired(Duration.ZERO))
    }

    @Test
    fun aNegativeElapsedTimeHasNotExpired() {
        assertFalse(cap.hasExpired((-1).seconds))
    }

    @Test
    fun theDefaultCapIsUsable() {
        assertFalse(SoundCap().hasExpired(1.seconds))
        assertTrue(SoundCap().hasExpired(1.hours))
    }

    @Test
    fun aNonPositiveCapIsRejected() {
        assertFailsWith<IllegalArgumentException> { SoundCap(Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { SoundCap((-1).minutes) }
    }
}
