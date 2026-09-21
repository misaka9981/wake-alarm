package com.misaka9981.alarm.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EscapeHatchUseCodecTest {
    private val first = EscapeHatchUse(
        alarmId = AlarmId("a1b2c3"),
        usedAt = Instant.parse("2026-09-22T06:31:00Z"),
    )
    private val second = EscapeHatchUse(
        alarmId = AlarmId("alarm|with|separators"),
        usedAt = Instant.parse("2026-09-23T07:00:00Z"),
    )

    @Test
    fun encodeThenDecodeRoundTripsEveryUse() {
        val uses = listOf(first, second)

        assertEquals(uses, EscapeHatchUseCodec.decode(EscapeHatchUseCodec.encode(uses)))
    }

    @Test
    fun anEmptyLogRoundTrips() {
        assertEquals(emptyList(), EscapeHatchUseCodec.decode(EscapeHatchUseCodec.encode(emptyList())))
    }

    @Test
    fun aMalformedHeaderIsRejected() {
        assertFailsWith<EscapeHatchFormatException> { EscapeHatchUseCodec.decode("not-wake-alarm 1") }
    }

    @Test
    fun aMalformedRecordIsRejected() {
        val encoded = EscapeHatchUseCodec.encode(listOf(first))
        val corrupted = encoded.replace("|", "|broken|")

        assertFailsWith<EscapeHatchFormatException> { EscapeHatchUseCodec.decode(corrupted) }
    }
}
