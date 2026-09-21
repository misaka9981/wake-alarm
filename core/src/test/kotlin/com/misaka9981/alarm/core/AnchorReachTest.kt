package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class AnchorReachTest {
    private val kitchen = PhysicalAnchor(code = AnchorCode("kitchen-qr"), label = "Kitchen")
    private val hallway = PhysicalAnchor(code = AnchorCode("hallway-qr"), label = "Hallway")
    private val morning = AlarmId("morning")

    private class FakeScanner(private val payload: String?) : AnchorScanner {
        var scans = 0
            private set

        override suspend fun scan(): String? {
            scans++
            return payload
        }
    }

    @Test
    fun aScanOfTheBoundAnchorIsReached() {
        val result = AnchorVerifier(kitchen).verify(kitchen.code.value)

        assertEquals(AnchorScanResult.Reached, result)
    }

    @Test
    fun aScanOfAnythingElseIsNotReached() {
        val result = AnchorVerifier(kitchen).verify(hallway.code.value)

        assertEquals(AnchorScanResult.NotReached(hallway.code.value), result)
    }

    @Test
    fun aCancelledScanIsNotReached() {
        assertEquals(AnchorScanResult.NotReached(null), AnchorVerifier(kitchen).verify(null))
    }

    @Test
    fun anAlarmWithNoBoundAnchorCanNeverBeReached() {
        assertEquals(AnchorScanResult.NotReached(kitchen.code.value), AnchorVerifier(null).verify(kitchen.code.value))
    }

    @Test
    fun matchingIsExactAboutCaseAndWhitespace() {
        assertEquals(
            AnchorScanResult.NotReached(" kitchen-qr"),
            AnchorVerifier(kitchen).verify(" kitchen-qr"),
        )
        assertEquals(
            AnchorScanResult.NotReached("KITCHEN-QR"),
            AnchorVerifier(kitchen).verify("KITCHEN-QR"),
        )
    }

    @Test
    fun theGateReportsReachedThroughTheScannerPort() = runTest {
        val catalog = AnchorCatalog.empty.set(kitchen).bind(morning, kitchen.code)
        val scanner = FakeScanner(kitchen.code.value)

        val result = catalog.gateFor(morning, scanner).scan()

        assertEquals(AnchorScanResult.Reached, result)
        assertEquals(1, scanner.scans)
    }

    @Test
    fun theGateReportsNotReachedForTheWrongObject() = runTest {
        val catalog = AnchorCatalog.empty.set(kitchen).bind(morning, kitchen.code)
        val scanner = FakeScanner(hallway.code.value)

        val result = catalog.gateFor(morning, scanner).scan()

        assertEquals(AnchorScanResult.NotReached(hallway.code.value), result)
    }

    @Test
    fun theGateReportsNotReachedWhenTheOwnerCancels() = runTest {
        val catalog = AnchorCatalog.empty.set(kitchen).bind(morning, kitchen.code)

        val result = catalog.gateFor(morning, FakeScanner(null)).scan()

        assertEquals(AnchorScanResult.NotReached(null), result)
    }
}
