package com.misaka9981.alarm.core

import java.time.Instant
import java.util.Base64
import kotlin.time.Duration.Companion.milliseconds

/** Raised when persisted Diagnostic Log data cannot be read back. */
class DiagnosticFormatException(message: String) : IllegalArgumentException(message)

/**
 * Encodes the Diagnostic Log to a single string and back, with no dependency on
 * any storage or Android API.
 *
 * This is the part of persistence that is decidable, so it is the part kept in
 * `core` and tested; the adapter only stores the string it produces. The Alarm id
 * is Base64-encoded because it is an arbitrary payload that may itself contain
 * the separator, the header version lets a future format change detect old data
 * instead of silently misreading it, and [MAX_ENTRIES] bounds the history so the
 * log can never grow without limit however many Alarms fire.
 */
object DiagnosticLogCodec {
    const val VERSION: Int = 1

    /** The most recent firings the Diagnostic Log keeps. */
    const val MAX_ENTRIES: Int = 50

    private const val HEADER = "wake-alarm-diagnostic-log"
    private const val FIELD_SEPARATOR = '|'
    private const val FIELDS_PER_RECORD = 8
    private const val REQUIREMENT_SEPARATOR = ','

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /** Encodes the most recent [MAX_ENTRIES] of [entries], oldest first. */
    fun encode(entries: List<DiagnosticEntry>): String = buildString {
        append(HEADER).append(' ').append(VERSION)
        entries.takeLast(MAX_ENTRIES).forEach { entry ->
            append('\n')
            append(encodeField(entry.alarmId.value)).append(FIELD_SEPARATOR)
            append(entry.scheduledTime?.toString().orEmpty()).append(FIELD_SEPARATOR)
            append(entry.firedAt).append(FIELD_SEPARATOR)
            append(encodeRequirements(entry.missingRequirements)).append(FIELD_SEPARATOR)
            append(entry.signallingVolume).append(FIELD_SEPARATOR)
            append(entry.challengeDuration.inWholeMilliseconds).append(FIELD_SEPARATOR)
            append(entry.wrongAnswers).append(FIELD_SEPARATOR)
            append(entry.outcome.name)
        }
    }

    /** Decodes [text], keeping only the most recent [MAX_ENTRIES] records. */
    fun decode(text: String): List<DiagnosticEntry> = try {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            emptyList()
        } else {
            requireVersion(lines.first())
            lines.drop(1).map(::decodeEntry).takeLast(MAX_ENTRIES)
        }
    } catch (failure: DiagnosticFormatException) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw DiagnosticFormatException("malformed Diagnostic Log data: ${failure.message}")
    }

    private fun requireVersion(header: String) {
        val parts = header.trim().split(' ')
        val version = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != 2 || parts[0] != HEADER || version != VERSION) {
            throw DiagnosticFormatException("unrecognised Diagnostic Log header: \"$header\"")
        }
    }

    private fun decodeEntry(line: String): DiagnosticEntry {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELDS_PER_RECORD) {
            throw DiagnosticFormatException(
                "expected $FIELDS_PER_RECORD fields, got ${fields.size}: \"$line\"",
            )
        }
        return try {
            DiagnosticEntry(
                alarmId = AlarmId(decodeField(fields[0])),
                scheduledTime = fields[1].ifBlank { null }?.let(::parseInstant),
                firedAt = parseInstant(fields[2]),
                missingRequirements = decodeRequirements(fields[3]),
                signallingVolume = fields[4].toIntOrNull()
                    ?: throw DiagnosticFormatException("signalling volume is not a number: \"${fields[4]}\""),
                challengeDuration = (
                    fields[5].toLongOrNull()
                        ?: throw DiagnosticFormatException("challenge duration is not a number: \"${fields[5]}\"")
                    ).milliseconds,
                wrongAnswers = fields[6].toIntOrNull()
                    ?: throw DiagnosticFormatException("wrong-answer count is not a number: \"${fields[6]}\""),
                outcome = parseOutcome(fields[7]),
            )
        } catch (failure: DiagnosticFormatException) {
            throw failure
        } catch (failure: IllegalArgumentException) {
            throw DiagnosticFormatException("malformed Diagnostic Log record: \"$line\"")
        }
    }

    private fun encodeRequirements(requirements: Set<ReliabilityRequirement>): String =
        requirements.sortedBy { it.name }.joinToString(REQUIREMENT_SEPARATOR.toString()) { it.name }

    private fun decodeRequirements(field: String): Set<ReliabilityRequirement> {
        if (field.isBlank()) return emptySet()
        return field.split(REQUIREMENT_SEPARATOR).mapTo(linkedSetOf()) { name ->
            ReliabilityRequirement.entries.firstOrNull { it.name == name }
                ?: throw DiagnosticFormatException("unknown reliability requirement: \"$name\"")
        }
    }

    private fun parseOutcome(field: String): FiringOutcome =
        FiringOutcome.entries.firstOrNull { it.name == field }
            ?: throw DiagnosticFormatException("unknown firing outcome: \"$field\"")

    private fun parseInstant(field: String): Instant = try {
        Instant.parse(field)
    } catch (failure: RuntimeException) {
        throw DiagnosticFormatException("instant is not valid ISO-8601: \"$field\"")
    }

    private fun encodeField(value: String): String = encoder.encodeToString(value.toByteArray())

    private fun decodeField(field: String): String = try {
        decoder.decode(field).toString(Charsets.UTF_8)
    } catch (failure: IllegalArgumentException) {
        throw DiagnosticFormatException("field is not valid Base64: \"$field\"")
    }
}
