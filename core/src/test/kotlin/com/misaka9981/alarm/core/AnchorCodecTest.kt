package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AnchorCodecTest {
    private val kitchen = PhysicalAnchor(code = AnchorCode("kitchen-qr"), label = "Kitchen")
    private val hallway = PhysicalAnchor(code = AnchorCode("hallway-qr"), label = "Hallway")
    private val morning = AlarmId("morning")
    private val evening = AlarmId("evening")

    private fun catalog() = AnchorCatalog.of(
        anchors = listOf(kitchen, hallway),
        bindings = mapOf(morning to kitchen.code, evening to hallway.code),
    )

    @Test
    fun roundTripsAnchorsAndTheirBindings() {
        val decoded = AnchorCodec.decode(AnchorCodec.encode(catalog()))

        assertEquals(catalog().anchors, decoded.anchors)
        assertEquals(catalog().bindings, decoded.bindings)
    }

    @Test
    fun roundTripsAnEmptyCatalog() {
        val decoded = AnchorCodec.decode(AnchorCodec.encode(AnchorCatalog.empty))

        assertEquals(emptyList(), decoded.anchors)
        assertTrue(decoded.bindings.isEmpty())
    }

    @Test
    fun roundTripsACodeContainingTheSeparatorAndNewlines() {
        val awkward = PhysicalAnchor(
            code = AnchorCode("line one|line two\nline three"),
            label = "Awkward",
        )

        val decoded = AnchorCodec.decode(AnchorCodec.encode(AnchorCatalog.empty.set(awkward)))

        assertEquals(listOf(awkward), decoded.anchors)
    }

    @Test
    fun roundTripsALabelContainingTheSeparator() {
        val labelled = PhysicalAnchor(code = AnchorCode("code"), label = "By the | door")

        val decoded = AnchorCodec.decode(AnchorCodec.encode(AnchorCatalog.empty.set(labelled)))

        assertEquals(listOf(labelled), decoded.anchors)
    }

    @Test
    fun rejectsUnknownHeader() {
        assertFailsWith<AnchorFormatException> { AnchorCodec.decode("not-anchor-data") }
    }

    @Test
    fun rejectsUnknownVersion() {
        assertFailsWith<AnchorFormatException> {
            AnchorCodec.decode("wake-alarm-anchors 99")
        }
    }

    @Test
    fun rejectsAStructurallyBrokenRecord() {
        assertFailsWith<AnchorFormatException> {
            AnchorCodec.decode("wake-alarm-anchors 1\nanchor|only-one-field")
        }
    }

    @Test
    fun rejectsAnUnknownRecordType() {
        assertFailsWith<AnchorFormatException> {
            AnchorCodec.decode("wake-alarm-anchors 1\nnonsense|Zm9v|YmFy")
        }
    }

    @Test
    fun rejectsAFieldThatIsNotBase64() {
        assertFailsWith<AnchorFormatException> {
            AnchorCodec.decode("wake-alarm-anchors 1\nanchor|not base64!|YmFy")
        }
    }

    @Test
    fun rejectsADanglingBinding() {
        val encoded = AnchorCodec.encode(AnchorCatalog.empty.set(kitchen))
        val withDangling = encoded + "\nbinding|" + encode("morning") + "|" + encode("hallway-qr")

        assertFailsWith<AnchorFormatException> { AnchorCodec.decode(withDangling) }
    }

    private fun encode(value: String): String =
        java.util.Base64.getEncoder().encodeToString(value.toByteArray())
}
