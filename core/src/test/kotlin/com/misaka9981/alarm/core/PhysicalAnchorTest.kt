package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PhysicalAnchorTest {
    @Test
    fun anAnchorCarriesItsCodeAndLabel() {
        val anchor = PhysicalAnchor(code = AnchorCode("https://example.test/secret"), label = "Kitchen")

        assertEquals("https://example.test/secret", anchor.code.value)
        assertEquals("Kitchen", anchor.label)
    }

    @Test
    fun aBlankCodeIsImpossible() {
        assertFailsWith<IllegalArgumentException> { AnchorCode("") }
        assertFailsWith<IllegalArgumentException> { AnchorCode("   ") }
    }

    @Test
    fun aBlankLabelIsImpossible() {
        assertFailsWith<IllegalArgumentException> {
            PhysicalAnchor(code = AnchorCode("code"), label = " ")
        }
    }

    @Test
    fun aLabelMustFitOnOneLine() {
        assertFailsWith<IllegalArgumentException> {
            PhysicalAnchor(code = AnchorCode("code"), label = "two\nlines")
        }
    }
}
