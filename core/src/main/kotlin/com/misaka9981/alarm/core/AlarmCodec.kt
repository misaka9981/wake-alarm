package com.misaka9981.alarm.core

import java.time.DateTimeException
import java.time.DayOfWeek

/** Raised when persisted Alarm data cannot be read back. */
class AlarmFormatException(message: String) : IllegalArgumentException(message)

/**
 * Encodes the Alarm configuration to a single string and back, with no
 * dependency on any storage or Android API.
 *
 * This is the part of persistence that is decidable, so it is the part kept in
 * `core` and tested; the adapter only stores the string it produces. Versioning
 * the header lets a future format change detect old data instead of silently
 * misreading it.
 */
object AlarmCodec {
    const val VERSION: Int = 1

    private const val HEADER = "wake-alarm-alarms"
    private const val FIELD_SEPARATOR = '|'
    private const val DAY_SEPARATOR = ','
    private const val ENABLED = "1"
    private const val DISABLED = "0"

    fun encode(alarms: List<Alarm>): String = buildString {
        append(HEADER).append(' ').append(VERSION)
        alarms.forEach { alarm ->
            append('\n')
            append(alarm.id.value).append(FIELD_SEPARATOR)
            append(alarm.time.hour).append(FIELD_SEPARATOR)
            append(alarm.time.minute).append(FIELD_SEPARATOR)
            append(if (alarm.enabled) ENABLED else DISABLED).append(FIELD_SEPARATOR)
            append(
                alarm.repeatDays
                    .sortedBy { it.value }
                    .joinToString(DAY_SEPARATOR.toString()) { it.value.toString() },
            )
        }
    }

    fun decode(text: String): List<Alarm> = try {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            emptyList()
        } else {
            requireVersion(lines.first())
            lines.drop(1).map(::decodeAlarm)
        }
    } catch (failure: AlarmFormatException) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw AlarmFormatException("malformed Alarm data: ${failure.message}")
    }

    private fun requireVersion(header: String) {
        val parts = header.trim().split(' ')
        val version = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != 2 || parts[0] != HEADER || version != VERSION) {
            throw AlarmFormatException("unrecognised Alarm data header: \"$header\"")
        }
    }

    private fun decodeAlarm(line: String): Alarm {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != 5) {
            throw AlarmFormatException("expected 5 fields, got ${fields.size}: \"$line\"")
        }
        val id = AlarmId(fields[0])
        val hour = fields[1].toIntOrNull()
            ?: throw AlarmFormatException("hour is not a number: \"${fields[1]}\"")
        val minute = fields[2].toIntOrNull()
            ?: throw AlarmFormatException("minute is not a number: \"${fields[2]}\"")
        val enabled = when (fields[3]) {
            ENABLED -> true
            DISABLED -> false
            else -> throw AlarmFormatException("enabled flag is not 0 or 1: \"${fields[3]}\"")
        }
        val repeatDays = decodeRepeatDays(fields[4], line)
        return Alarm(
            id = id,
            time = AlarmTime(hour, minute),
            repeatDays = repeatDays,
            enabled = enabled,
        )
    }

    private fun decodeRepeatDays(field: String, line: String): Set<DayOfWeek> {
        if (field.isBlank()) {
            throw AlarmFormatException("Alarm has no repeat days: \"$line\"")
        }
        return field.split(DAY_SEPARATOR).map { token ->
            val day = token.toIntOrNull()
                ?: throw AlarmFormatException("repeat day is not a number: \"$token\"")
            try {
                DayOfWeek.of(day)
            } catch (failure: DateTimeException) {
                throw AlarmFormatException("repeat day is out of range: \"$token\"")
            }
        }.toSet()
    }
}
