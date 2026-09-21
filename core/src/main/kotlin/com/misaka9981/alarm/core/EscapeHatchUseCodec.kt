package com.misaka9981.alarm.core

import java.time.Instant
import java.util.Base64

/** Raised when persisted Escape Hatch use data cannot be read back. */
class EscapeHatchFormatException(message: String) : IllegalArgumentException(message)

/**
 * Encodes recorded Escape Hatch uses to a single string and back, with no
 * dependency on any storage or Android API.
 *
 * This is the part of persistence that is decidable, so it is the part kept in
 * `core` and tested; the adapter only stores the string it produces. The Alarm
 * id is Base64-encoded because it is an arbitrary payload that may itself contain
 * the separator, and the header version lets a future format change detect old
 * data instead of silently misreading it.
 */
object EscapeHatchUseCodec {
    const val VERSION: Int = 1

    private const val HEADER = "wake-alarm-escape-hatch"
    private const val FIELD_SEPARATOR = '|'
    private const val FIELDS_PER_RECORD = 2

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    fun encode(uses: List<EscapeHatchUse>): String = buildString {
        append(HEADER).append(' ').append(VERSION)
        uses.forEach { use ->
            append('\n')
            append(encodeField(use.alarmId.value)).append(FIELD_SEPARATOR)
            append(use.usedAt.toString())
        }
    }

    fun decode(text: String): List<EscapeHatchUse> = try {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            emptyList()
        } else {
            requireVersion(lines.first())
            lines.drop(1).map(::decodeUse)
        }
    } catch (failure: EscapeHatchFormatException) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw EscapeHatchFormatException("malformed Escape Hatch use data: ${failure.message}")
    }

    private fun requireVersion(header: String) {
        val parts = header.trim().split(' ')
        val version = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != 2 || parts[0] != HEADER || version != VERSION) {
            throw EscapeHatchFormatException("unrecognised Escape Hatch use data header: \"$header\"")
        }
    }

    private fun decodeUse(line: String): EscapeHatchUse {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELDS_PER_RECORD) {
            throw EscapeHatchFormatException(
                "expected $FIELDS_PER_RECORD fields, got ${fields.size}: \"$line\"",
            )
        }
        val alarmId = AlarmId(decodeField(fields[0]))
        val usedAt = try {
            Instant.parse(fields[1])
        } catch (failure: RuntimeException) {
            throw EscapeHatchFormatException("use instant is not valid ISO-8601: \"${fields[1]}\"")
        }
        return EscapeHatchUse(alarmId = alarmId, usedAt = usedAt)
    }

    private fun encodeField(value: String): String = encoder.encodeToString(value.toByteArray())

    private fun decodeField(field: String): String = try {
        decoder.decode(field).toString(Charsets.UTF_8)
    } catch (failure: IllegalArgumentException) {
        throw EscapeHatchFormatException("field is not valid Base64: \"$field\"")
    }
}
