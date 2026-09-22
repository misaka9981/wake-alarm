package com.misaka9981.alarm.core

import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingTest {
    private val time = AlarmTime(6, 30)
    private val code = AnchorCode("anchor-code")
    private val anchor = PhysicalAnchor(code = code, label = "Kitchen")

    @Test
    fun firstAlarmRingsEveryDayAndIsEnabled() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)

        assertEquals(AlarmId("first"), alarm.id)
        assertEquals(time, alarm.time)
        assertEquals(DayOfWeek.entries.toSet(), alarm.repeatDays)
        assertTrue(alarm.enabled)
        assertFalse(alarm.silentMode)
        assertEquals(Alarm.DEFAULT_DIFFICULTY, alarm.defaultDifficulty)
    }

    @Test
    fun nothingIsSetYetSoAllThreeStepsRemain() {
        val onboarding = Onboarding.of(emptyList(), AnchorCatalog.empty, null)

        assertFalse(onboarding.isComplete)
        assertNull(onboarding.firstAlarm)
        assertEquals(
            listOf(
                OnboardingStep.WakeTime,
                OnboardingStep.PhysicalAnchor,
                OnboardingStep.EscapeHatchPassword,
            ),
            onboarding.missingSteps,
        )
    }

    @Test
    fun afterTheWakeTimeTheAnchorAndPasswordRemain() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)

        val onboarding = Onboarding.of(listOf(alarm), AnchorCatalog.empty, null)

        assertFalse(onboarding.isComplete)
        assertEquals(
            listOf(OnboardingStep.PhysicalAnchor, OnboardingStep.EscapeHatchPassword),
            onboarding.missingSteps,
        )
    }

    @Test
    fun afterTheAnchorOnlyThePasswordRemains() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)
        val anchors = AnchorCatalog.empty.set(anchor).bind(alarm.id, code)

        val onboarding = Onboarding.of(listOf(alarm), anchors, null)

        assertEquals(listOf(OnboardingStep.EscapeHatchPassword), onboarding.missingSteps)
    }

    @Test
    fun allThreeTogetherCompleteOnboarding() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)
        val anchors = AnchorCatalog.empty.set(anchor).bind(alarm.id, code)

        val onboarding =
            Onboarding.of(listOf(alarm), anchors, EscapeHatchPassword("secret"))

        assertTrue(onboarding.isComplete)
        assertEquals(emptyList(), onboarding.missingSteps)
    }

    @Test
    fun anAnchorBoundToAnotherAlarmDoesNotCount() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)
        val other = alarm.copy(id = AlarmId("zzz"))
        val anchors = AnchorCatalog.empty.set(anchor).bind(other.id, code)

        val onboarding = Onboarding.of(listOf(alarm, other), anchors, null)

        assertFalse(onboarding.anchorBound)
    }

    @Test
    fun theEarliestAlarmIsTheOneSetupShows() {
        val late = Onboarding.firstAlarm(AlarmId("late"), AlarmTime(9, 0))
        val early = Onboarding.firstAlarm(AlarmId("early"), AlarmTime(5, 0))

        val onboarding = Onboarding.of(listOf(late, early), AnchorCatalog.empty, null)

        assertEquals(AlarmId("early"), onboarding.firstAlarm?.id)
    }

    @Test
    fun aDisabledFirstAlarmStillCountsAsSetUp() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time).copy(enabled = false)
        val anchors = AnchorCatalog.empty.set(anchor).bind(alarm.id, code)

        val onboarding =
            Onboarding.of(listOf(alarm), anchors, EscapeHatchPassword("secret"))

        assertTrue(onboarding.isComplete)
    }
}
