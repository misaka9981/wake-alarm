package com.misaka9981.alarm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReliabilityCheckTest {
    private val nothingGranted = ReliabilityGrants(
        exactAlarm = false,
        fullScreenIntent = false,
        batteryOptimisationExempt = false,
        doNotDisturbAccess = false,
    )

    private val everythingGranted = ReliabilityGrants(
        exactAlarm = true,
        fullScreenIntent = true,
        batteryOptimisationExempt = true,
        doNotDisturbAccess = true,
    )

    @Test
    fun belowApi31ExactAlarmsAreNotRequired() {
        val required = ReliabilityCheck(sdkInt = 30).required()

        assertFalse(ReliabilityRequirement.ExactAlarm in required)
    }

    @Test
    fun fromApi31ExactAlarmsAreRequired() {
        val required = ReliabilityCheck(sdkInt = 31).required()

        assertTrue(ReliabilityRequirement.ExactAlarm in required)
    }

    @Test
    fun fullScreenIntentIsOnlyRequiredFromApi34() {
        assertFalse(ReliabilityRequirement.FullScreenIntent in ReliabilityCheck(sdkInt = 33).required())
        assertTrue(ReliabilityRequirement.FullScreenIntent in ReliabilityCheck(sdkInt = 34).required())
    }

    @Test
    fun batteryOptimisationAndDoNotDisturbAccessAreAlwaysRequired() {
        val required = ReliabilityCheck(sdkInt = 26).required()

        assertTrue(ReliabilityRequirement.BatteryOptimisation in required)
        assertTrue(ReliabilityRequirement.DoNotDisturbAccess in required)
    }

    @Test
    fun missingReportsOnlyRequiredUngrantedRequirements() {
        val grants = everythingGranted.copy(exactAlarm = false, doNotDisturbAccess = false)

        val missing = ReliabilityCheck(sdkInt = 31).missing(grants)

        assertEquals(
            setOf(ReliabilityRequirement.ExactAlarm, ReliabilityRequirement.DoNotDisturbAccess),
            missing,
        )
    }

    @Test
    fun aRequirementThatDoesNotApplyIsNotReportedMissing() {
        val grants = everythingGranted.copy(fullScreenIntent = false)

        val missing = ReliabilityCheck(sdkInt = 33).missing(grants)

        assertTrue(missing.isEmpty())
    }

    @Test
    fun anAlarmIsReliableOnlyWhenNothingRequiredIsMissing() {
        val check = ReliabilityCheck(sdkInt = 34)

        assertFalse(check.isReliable(nothingGranted))
        assertTrue(check.isReliable(everythingGranted))
    }
}
