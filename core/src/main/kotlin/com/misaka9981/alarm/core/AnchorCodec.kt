package com.misaka9981.alarm.core

import java.util.Base64

/** Raised when persisted Physical Anchor data cannot be read back. */
class AnchorFormatException(message: String) : IllegalArgumentException(message)

/**
 * Encodes the Physical Anchor configuration to a single string and back, with
 * no dependency on any storage or Android API.
 *
 * This is the part of persistence that is decidable, so it is the part kept in
 * `core` and tested; the adapter only stores the string it produces. Every field
 * is Base64-encoded because an anchor's code is an arbitrary payload read from
 * the real world, which may itself contain the separator or a newline; versioning
 * the header lets a future format change detect old data instead of silently
 * misreading it.
 */
object AnchorCodec {
    const val VERSION: Int = 1

    private const val HEADER = "wake-alarm-anchors"
    private const val FIELD_SEPARATOR = '|'
    private const val ANCHOR = "anchor"
    private const val BINDING = "binding"
    private const val FIELDS_PER_RECORD = 3

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    fun encode(catalog: AnchorCatalog): String = buildString {
        append(HEADER).append(' ').append(VERSION)
        catalog.anchors.forEach { anchor ->
            append('\n')
            append(ANCHOR).append(FIELD_SEPARATOR)
            append(encodeField(anchor.code.value)).append(FIELD_SEPARATOR)
            append(encodeField(anchor.label))
        }
        catalog.bindings.entries.sortedBy { it.key.value }.forEach { (alarmId, code) ->
            append('\n')
            append(BINDING).append(FIELD_SEPARATOR)
            append(encodeField(alarmId.value)).append(FIELD_SEPARATOR)
            append(encodeField(code.value))
        }
    }

    fun decode(text: String): AnchorCatalog = try {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            AnchorCatalog.empty
        } else {
            requireVersion(lines.first())
            decodeRecords(lines.drop(1))
        }
    } catch (failure: AnchorFormatException) {
        throw failure
    } catch (failure: IllegalArgumentException) {
        throw AnchorFormatException("malformed Physical Anchor data: ${failure.message}")
    }

    private fun requireVersion(header: String) {
        val parts = header.trim().split(' ')
        val version = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != 2 || parts[0] != HEADER || version != VERSION) {
            throw AnchorFormatException("unrecognised Physical Anchor data header: \"$header\"")
        }
    }

    private fun decodeRecords(lines: List<String>): AnchorCatalog {
        val anchors = mutableListOf<PhysicalAnchor>()
        val bindings = mutableMapOf<AlarmId, AnchorCode>()
        lines.forEach { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != FIELDS_PER_RECORD) {
                throw AnchorFormatException(
                    "expected $FIELDS_PER_RECORD fields, got ${fields.size}: \"$line\"",
                )
            }
            when (fields[0]) {
                ANCHOR -> anchors += PhysicalAnchor(
                    code = AnchorCode(decodeField(fields[1])),
                    label = decodeField(fields[2]),
                )

                BINDING -> bindings[AlarmId(decodeField(fields[1]))] =
                    AnchorCode(decodeField(fields[2]))

                else -> throw AnchorFormatException("unknown record type: \"${fields[0]}\"")
            }
        }
        return AnchorCatalog.of(anchors, bindings)
    }

    private fun encodeField(value: String): String = encoder.encodeToString(value.toByteArray())

    private fun decodeField(field: String): String = try {
        decoder.decode(field).toString(Charsets.UTF_8)
    } catch (failure: IllegalArgumentException) {
        throw AnchorFormatException("field is not valid Base64: \"$field\"")
    }
}
