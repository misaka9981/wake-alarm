package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnchorCatalogTest {
    private val kitchen = PhysicalAnchor(code = AnchorCode("kitchen-qr"), label = "Kitchen")
    private val hallway = PhysicalAnchor(code = AnchorCode("hallway-qr"), label = "Hallway")
    private val morning = AlarmId("morning")
    private val evening = AlarmId("evening")

    @Test
    fun startsEmpty() {
        assertEquals(emptyList(), AnchorCatalog.empty.anchors)
        assertTrue(AnchorCatalog.empty.bindings.isEmpty())
    }

    @Test
    fun setAddsAnAnchor() {
        val catalog = AnchorCatalog.empty.set(kitchen)

        assertEquals(listOf(kitchen), catalog.anchors)
    }

    @Test
    fun anchorsWithDifferentCodesCoexist() {
        val catalog = AnchorCatalog.empty.set(kitchen).set(hallway)

        assertEquals(setOf(kitchen, hallway), catalog.anchors.toSet())
    }

    @Test
    fun setRelabelsTheAnchorThatCarriesTheSameCode() {
        val renamed = kitchen.copy(label = "New kitchen")

        val catalog = AnchorCatalog.empty.set(kitchen).set(renamed)

        assertEquals(listOf(renamed), catalog.anchors)
    }

    @Test
    fun anchorsAreOrderedByLabel() {
        val catalog = AnchorCatalog.empty.set(kitchen).set(hallway)

        assertEquals(listOf(hallway, kitchen), catalog.anchors)
    }

    @Test
    fun bindPointsAnAlarmAtAnAnchor() {
        val catalog = AnchorCatalog.empty.set(kitchen).bind(morning, kitchen.code)

        assertEquals(kitchen, catalog.anchorFor(morning))
    }

    @Test
    fun bindRequiresTheAnchorToHaveBeenSet() {
        assertFailsWith<IllegalArgumentException> {
            AnchorCatalog.empty.bind(morning, kitchen.code)
        }
    }

    @Test
    fun bindReplacesThePreviousAnchorForTheSameAlarm() {
        val catalog = AnchorCatalog.empty
            .set(kitchen)
            .set(hallway)
            .bind(morning, kitchen.code)
            .bind(morning, hallway.code)

        assertEquals(hallway, catalog.anchorFor(morning))
        assertEquals(mapOf(morning to hallway.code), catalog.bindings)
    }

    @Test
    fun oneAnchorCanBeBoundToSeveralAlarms() {
        val catalog = AnchorCatalog.empty
            .set(kitchen)
            .bind(morning, kitchen.code)
            .bind(evening, kitchen.code)

        assertEquals(kitchen, catalog.anchorFor(morning))
        assertEquals(kitchen, catalog.anchorFor(evening))
    }

    @Test
    fun unbindKeepsTheAnchor() {
        val catalog = AnchorCatalog.empty
            .set(kitchen)
            .bind(morning, kitchen.code)
            .unbind(morning)

        assertNull(catalog.anchorFor(morning))
        assertEquals(listOf(kitchen), catalog.anchors)
    }

    @Test
    fun anUnboundAlarmHasNoAnchor() {
        assertNull(AnchorCatalog.empty.set(kitchen).anchorFor(morning))
    }

    @Test
    fun deleteRemovesTheAnchorAndEveryBindingToIt() {
        val catalog = AnchorCatalog.empty
            .set(kitchen)
            .bind(morning, kitchen.code)
            .bind(evening, kitchen.code)
            .delete(kitchen.code)

        assertEquals(emptyList(), catalog.anchors)
        assertTrue(catalog.bindings.isEmpty())
        assertNull(catalog.anchorFor(morning))
    }

    @Test
    fun deleteOfAnUnknownCodeChangesNothing() {
        val catalog = AnchorCatalog.empty.set(kitchen).delete(hallway.code)

        assertEquals(listOf(kitchen), catalog.anchors)
    }

    @Test
    fun findByCodeFindsASetAnchor() {
        val catalog = AnchorCatalog.empty.set(kitchen)

        assertEquals(kitchen, catalog.findByCode(kitchen.code))
        assertNull(catalog.findByCode(hallway.code))
    }

    @Test
    fun twoAnchorsCannotShareACode() {
        val duplicate = PhysicalAnchor(code = kitchen.code, label = "Impostor")

        assertFailsWith<IllegalArgumentException> {
            AnchorCatalog.of(listOf(kitchen, duplicate))
        }
    }

    @Test
    fun aBindingToAnUnsetAnchorIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            AnchorCatalog.of(listOf(kitchen), mapOf(morning to hallway.code))
        }
    }
}
