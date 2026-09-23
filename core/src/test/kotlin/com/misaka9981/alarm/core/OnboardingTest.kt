package com.misaka9981.alarm.core

import java.time.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingTest {
    private val time = AlarmTime(6, 30)

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
    fun nothingIsSetYetSoBothStepsRemain() {
        val onboarding = Onboarding.of(emptyList(), null)

        assertFalse(onboarding.isComplete)
        assertNull(onboarding.firstAlarm)
        assertEquals(
            listOf(
                OnboardingStep.WakeTime,
                OnboardingStep.EscapeHatchPassword,
            ),
            onboarding.missingSteps,
        )
    }

    @Test
    fun afterTheWakeTimeOnlyThePasswordRemains() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)

        val onboarding = Onboarding.of(listOf(alarm), null)

        assertFalse(onboarding.isComplete)
        assertEquals(listOf(OnboardingStep.EscapeHatchPassword), onboarding.missingSteps)
    }

    @Test
    fun wakeTimeAndPasswordCompleteOnboardingWithoutAnAnchor() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time)

        val onboarding = Onboarding.of(listOf(alarm), EscapeHatchPassword("secret"))

        assertTrue(onboarding.isComplete)
        assertEquals(emptyList(), onboarding.missingSteps)
    }

    @Test
    fun theEarliestAlarmIsTheOneSetupShows() {
        val late = Onboarding.firstAlarm(AlarmId("late"), AlarmTime(9, 0))
        val early = Onboarding.firstAlarm(AlarmId("early"), AlarmTime(5, 0))

        val onboarding = Onboarding.of(listOf(late, early), null)

        assertEquals(AlarmId("early"), onboarding.firstAlarm?.id)
    }

    @Test
    fun aDisabledFirstAlarmStillCountsAsSetUp() {
        val alarm = Onboarding.firstAlarm(AlarmId("first"), time).copy(enabled = false)

        val onboarding = Onboarding.of(listOf(alarm), EscapeHatchPassword("secret"))

        assertTrue(onboarding.isComplete)
    }
}
