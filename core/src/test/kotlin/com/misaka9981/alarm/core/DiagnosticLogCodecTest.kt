package com.misaka9981.alarm.core

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class DiagnosticLogCodecTest {
    private val dismissed = DiagnosticEntry(
        alarmId = AlarmId("a1b2c3"),
        scheduledTime = Instant.parse("2026-09-22T06:30:00Z"),
        firedAt = Instant.parse("2026-09-22T06:30:04Z"),
        missingRequirements = setOf(
            ReliabilityRequirement.DoNotDisturbAccess,
            ReliabilityRequirement.BatteryOptimisation,
        ),
        signallingVolume = 100,
        challengeDuration = 90.seconds,
        wrongAnswers = 2,
        outcome = FiringOutcome.Dismissed,
    )

    private val escaped = DiagnosticEntry(
        alarmId = AlarmId("alarm|with|separators"),
        scheduledTime = null,
        firedAt = Instant.parse("2026-09-23T07:00:00Z"),
        missingRequirements = emptySet(),
        signallingVolume = 0,
        challengeDuration = 5.seconds,
        wrongAnswers = 0,
        outcome = FiringOutcome.EscapeHatch,
    )

    @Test
    fun encodeThenDecodeRoundTripsEveryEntry() {
        val entries = listOf(dismissed, escaped)

        assertEquals(entries, DiagnosticLogCodec.decode(DiagnosticLogCodec.encode(entries)))
    }

    @Test
    fun anEmptyLogRoundTrips() {
        assertEquals(emptyList(), DiagnosticLogCodec.decode(DiagnosticLogCodec.encode(emptyList())))
    }

    @Test
    fun aMissingScheduledTimeRoundTripsAsNull() {
        val decoded = DiagnosticLogCodec.decode(DiagnosticLogCodec.encode(listOf(escaped))).single()

        assertNull(decoded.scheduledTime)
    }

    @Test
    fun theEscapeHatchUseIsDerivedFromTheOutcome() {
        assertEquals(true, escaped.escapeHatchUsed)
        assertEquals(false, dismissed.escapeHatchUsed)
    }

    @Test
    fun encodingKeepsOnlyTheMostRecentEntries() {
        val entries = (1..DiagnosticLogCodec.MAX_ENTRIES + 3).map { index ->
            entryFor(index)
        }

        val decoded = DiagnosticLogCodec.decode(DiagnosticLogCodec.encode(entries))

        assertEquals(DiagnosticLogCodec.MAX_ENTRIES, decoded.size)
        // The three oldest firings fall off the front.
        assertEquals(AlarmId("alarm-4"), decoded.first().alarmId)
        assertEquals(AlarmId("alarm-${DiagnosticLogCodec.MAX_ENTRIES + 3}"), decoded.last().alarmId)
    }

    @Test
    fun decodingCapsAnOversizedHistoryToo() {
        val entries = (1..DiagnosticLogCodec.MAX_ENTRIES + 3).map { index ->
            entryFor(index)
        }
        // Bypass encode's own cap by joining two encoded batches.
        val oversized = DiagnosticLogCodec.encode(entries).lines() +
            DiagnosticLogCodec.encode(entries).lines().drop(1)
        val text = oversized.joinToString("\n")

        val decoded = DiagnosticLogCodec.decode(text)

        assertEquals(DiagnosticLogCodec.MAX_ENTRIES, decoded.size)
    }

    @Test
    fun aMalformedHeaderIsRejected() {
        assertFailsWith<DiagnosticFormatException> {
            DiagnosticLogCodec.decode("not-wake-alarm-diagnostic-log 1")
        }
    }

    @Test
    fun aMalformedRecordIsRejected() {
        val encoded = DiagnosticLogCodec.encode(listOf(dismissed))
        val corrupted = encoded.replace("|", "|broken|")

        assertFailsWith<DiagnosticFormatException> { DiagnosticLogCodec.decode(corrupted) }
    }

    @Test
    fun anUnknownRequirementIsRejected() {
        val encoded = DiagnosticLogCodec.encode(listOf(dismissed))
        val corrupted = encoded.replace("DoNotDisturbAccess", "NotARequirement")

        assertFailsWith<DiagnosticFormatException> { DiagnosticLogCodec.decode(corrupted) }
    }

    private fun entryFor(index: Int): DiagnosticEntry = DiagnosticEntry(
        alarmId = AlarmId("alarm-$index"),
        scheduledTime = Instant.parse("2026-09-22T06:30:00Z"),
        firedAt = Instant.parse("2026-09-22T06:30:00Z").plusSeconds(index.toLong()),
        missingRequirements = emptySet(),
        signallingVolume = 100,
        challengeDuration = index.seconds,
        wrongAnswers = index,
        outcome = FiringOutcome.Dismissed,
    )
}
