package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class VolumeRampTest {
    private val ramp = VolumeRamp(startFraction = 0.2f, rampUp = 30.seconds)

    @Test
    fun startsAtTheStartFractionSoTheAlarmIsImmediatelyAudible() {
        assertEquals(0.2f, ramp.fractionAt(kotlin.time.Duration.ZERO))
    }

    @Test
    fun risesMonotonically() {
        var previous = ramp.fractionAt(kotlin.time.Duration.ZERO)
        for (second in 1..30) {
            val current = ramp.fractionAt(second.seconds)
            assertTrue(current >= previous, "volume fell at ${second}s: $previous -> $current")
            previous = current
        }
    }

    @Test
    fun reachesMaximumAtTheRampUpTime() {
        assertEquals(1f, ramp.fractionAt(30.seconds))
    }

    @Test
    fun staysAtMaximumAfterwards() {
        assertEquals(1f, ramp.fractionAt(120.seconds))
    }

    @Test
    fun aNegativeElapsedTimeIsTreatedAsTheStart() {
        assertEquals(0.2f, ramp.fractionAt((-5).seconds))
    }

    @Test
    fun aDefaultRampIsUsable() {
        assertTrue(VolumeRamp().fractionAt(60.seconds) == 1f)
    }
}
