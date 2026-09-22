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
 * misreading it: version 2 added the per-Alarm Silent Mode flag, and version 1
 * data still decodes with Silent Mode off so an existing configuration is not
 * lost on upgrade.
 */
object AlarmCodec {
    const val VERSION: Int = 2

    private const val LEGACY_VERSION = 1
    private const val FIELD_COUNT = 6
    private const val LEGACY_FIELD_COUNT = 5
    private const val HEADER = "wake-alarm-alarms"
    private const val FIELD_SEPARATOR = '|'
    private const val DAY_SEPARATOR = ','
    private const val SET = "1"
    private const val UNSET = "0"

    fun encode(alarms: List<Alarm>): String = buildString {
        append(HEADER).append(' ').append(VERSION)
        alarms.forEach { alarm ->
            append('\n')
            append(alarm.id.value).append(FIELD_SEPARATOR)
            append(alarm.time.hour).append(FIELD_SEPARATOR)
            append(alarm.time.minute).append(FIELD_SEPARATOR)
            append(if (alarm.enabled) SET else UNSET).append(FIELD_SEPARATOR)
            append(if (alarm.silentMode) SET else UNSET).append(FIELD_SEPARATOR)
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
            val version = requireVersion(lines.first())
            lines.drop(1).map { decodeAlarm(it, version) }
        }
    } catch (failure: AlarmFormatException) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw AlarmFormatException("malformed Alarm data: ${failure.message}")
    }

    private fun requireVersion(header: String): Int {
        val parts = header.trim().split(' ')
        val version = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != 2 || parts[0] != HEADER || version !in SUPPORTED_VERSIONS) {
            throw AlarmFormatException("unrecognised Alarm data header: \"$header\"")
        }
        return version!!
    }

    private fun decodeAlarm(line: String, version: Int): Alarm {
        val fields = line.split(FIELD_SEPARATOR)
        // Version 1 had no Silent Mode field; it is the only difference.
        val expectedFields = if (version == LEGACY_VERSION) LEGACY_FIELD_COUNT else FIELD_COUNT
        if (fields.size != expectedFields) {
            throw AlarmFormatException("expected $expectedFields fields, got ${fields.size}: \"$line\"")
        }
        val id = AlarmId(fields[0])
        val hour = fields[1].toIntOrNull()
            ?: throw AlarmFormatException("hour is not a number: \"${fields[1]}\"")
        val minute = fields[2].toIntOrNull()
            ?: throw AlarmFormatException("minute is not a number: \"${fields[2]}\"")
        val enabled = decodeFlag(fields[3], "enabled")
        val silentMode = if (version == LEGACY_VERSION) {
            false
        } else {
            decodeFlag(fields[4], "silent")
        }
        val repeatDays = decodeRepeatDays(fields.last(), line)
        return Alarm(
            id = id,
            time = AlarmTime(hour, minute),
            repeatDays = repeatDays,
            enabled = enabled,
            silentMode = silentMode,
        )
    }

    private fun decodeFlag(field: String, name: String): Boolean = when (field) {
        SET -> true
        UNSET -> false
        else -> throw AlarmFormatException("$name flag is not 0 or 1: \"$field\"")
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

    private val SUPPORTED_VERSIONS = setOf(LEGACY_VERSION, VERSION)
}
