package com.misaka9981.alarm.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FiringSessionTest {
    private val policy = EscalationPolicy(
        wrongAnswerPenalty = 1,
        elapsedStep = 30.seconds,
        maxDifficulty = 9,
    )
    private val soundCap = SoundCap(10.minutes)

    private fun session(
        baseDifficulty: Int = 1,
        cap: SoundCap = soundCap,
        escapeHatch: EscapeHatch = EscapeHatch.of(EscapeHatchPassword("open-sesame")),
        anchorRequired: Boolean = true,
    ): FiringSession =
        FiringSession.start(
            generator = ArithmeticChallengeGenerator(Random(7)),
            policy = policy,
            baseDifficulty = baseDifficulty,
            soundCap = cap,
            escapeHatch = escapeHatch,
            anchorRequired = anchorRequired,
        )

    private fun FiringState.ringing(): FiringState.Ringing = this as FiringState.Ringing

    private fun FiringState.Ringing.challenge(): DismissalState.Ongoing =
        challenge as DismissalState.Ongoing

    @Test
    fun startsRingingWithAnOngoingChallengeAndNoAnchorReached() {
        val state = session().state.ringing()

        assertTrue(state.challenge is DismissalState.Ongoing)
        assertFalse(state.anchorReached)
        assertNull(state.anchorFeedback)
        assertFalse(state.capExpired)
    }

    @Test
    fun startsTheChallengeAtTheAlarmsDefaultDifficulty() {
        val state = session(baseDifficulty = 3).state.ringing().challenge()

        assertEquals(3, state.difficulty)
        assertEquals(3, state.challenge.difficulty)
    }

    @Test
    fun theDismissChallengeAloneNeverDismisses() {
        val session = session()
        val answer = session.state.ringing().challenge().challenge.answer

        val state = session.onEvent(FiringEvent.AnswerSubmitted(answer.toString())).ringing()

        assertTrue(state.challenge is DismissalState.Dismissed)
        assertFalse(state.anchorReached)
    }

    @Test
    fun whenNoAnchorIsBoundTheChallengeAloneDismisses() {
        val session = session(anchorRequired = false)
        val answer = session.state.ringing().challenge().challenge.answer

        val result = session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))

        assertEquals(FiringState.Dismissed, result)
    }

    @Test
    fun whenNoAnchorIsBoundAReachedAnchorDoesNotDismissByItself() {
        val session = session(anchorRequired = false)

        val state = session
            .onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))
            .ringing()

        assertTrue(state.anchorReached)
        assertTrue(state.challenge is DismissalState.Ongoing)
    }

    @Test
    fun thePhysicalAnchorAloneNeverDismisses() {
        val state = session()
            .onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))
            .ringing()

        assertTrue(state.anchorReached)
        assertTrue(state.challenge is DismissalState.Ongoing)
    }

    @Test
    fun solvingTheChallengeThenReachingTheAnchorDismisses() {
        val session = session()
        val answer = session.state.ringing().challenge().challenge.answer
        session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))

        val result = session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))

        assertEquals(FiringState.Dismissed, result)
    }

    @Test
    fun reachingTheAnchorThenSolvingTheChallengeDismisses() {
        val session = session()
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))
        val answer = session.state.ringing().challenge().challenge.answer

        val result = session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))

        assertEquals(FiringState.Dismissed, result)
    }

    @Test
    fun aScanOfTheWrongAnchorKeepsRingingAndReportsFeedback() {
        val state = session()
            .onEvent(FiringEvent.AnchorScanned(AnchorScanResult.NotReached("hallway")))
            .ringing()

        assertFalse(state.anchorReached)
        assertEquals(AnchorScanResult.NotReached("hallway"), state.anchorFeedback)
    }

    @Test
    fun onceTheAnchorIsReachedALaterWrongScanDoesNotUndoIt() {
        val session = session()
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.NotReached("hallway")))
        val answer = session.state.ringing().challenge().challenge.answer

        val result = session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))

        assertEquals(FiringState.Dismissed, result)
    }

    @Test
    fun aWrongAnswerNeverDismissesEvenWithTheAnchorReached() {
        val session = session()
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))

        repeat(20) { session.onEvent(FiringEvent.AnswerSubmitted("wrong-$it")) }

        assertTrue(session.state is FiringState.Ringing)
    }

    @Test
    fun ticksEscalateTheChallengeWhileRinging() {
        val before = session(baseDifficulty = 1).state.ringing().challenge().difficulty

        val after = session(baseDifficulty = 1)
            .onEvent(FiringEvent.Tick(300.seconds))
            .ringing()
            .challenge()

        assertTrue(after.difficulty > before)
    }

    @Test
    fun keepsSignallingBeforeTheCap() {
        val state = session()
            .onEvent(FiringEvent.Tick(9.minutes))
            .ringing()

        assertFalse(state.capExpired)
    }

    @Test
    fun theSoundCapStopsSignallingAtTheCap() {
        val state = session()
            .onEvent(FiringEvent.Tick(10.minutes))
            .ringing()

        assertTrue(state.capExpired)
    }

    @Test
    fun theAlarmRemainsRingingAndUnclearedAfterTheCap() {
        val state = session()
            .onEvent(FiringEvent.Tick(11.minutes))
            .ringing()

        assertTrue(state.capExpired)
        assertTrue(state.challenge is DismissalState.Ongoing)
        assertFalse(state.anchorReached)
    }

    @Test
    fun anExpiredCapIsNotUndoneByALaterBackwardsTick() {
        val session = session()
        session.onEvent(FiringEvent.Tick(1.hours))

        val state = session.onEvent(FiringEvent.Tick(1.seconds)).ringing()

        assertTrue(state.capExpired)
    }

    @Test
    fun solvingTheChallengeAndAnchorAfterTheCapStillDismisses() {
        val session = session()
        session.onEvent(FiringEvent.Tick(1.hours))
        val answer = session.state.ringing().challenge().challenge.answer
        session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))

        val result = session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))

        assertEquals(FiringState.Dismissed, result)
    }

    @Test
    fun aDismissedFiringSessionIgnoresFurtherEvents() {
        val session = session()
        val answer = session.state.ringing().challenge().challenge.answer
        session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))

        assertEquals(FiringState.Dismissed, session.onEvent(FiringEvent.AnswerSubmitted("anything")))
        assertEquals(
            FiringState.Dismissed,
            session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.NotReached("hallway"))),
        )
        assertEquals(FiringState.Dismissed, session.onEvent(FiringEvent.Tick(1.seconds)))
    }

    @Test
    fun theEscapeHatchRequiresTheLongPressBeforeThePassword() {
        val session = session()

        val state = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))

        assertTrue(state is FiringState.Ringing)
        assertFalse(state.ringing().escapeHatchRevealed)
    }

    @Test
    fun theLongPressRevealsTheEscapeHatchPasswordPrompt() {
        val state = session().onEvent(FiringEvent.EscapeHatchLongPressed).ringing()

        assertTrue(state.escapeHatchRevealed)
    }

    @Test
    fun theLongPressAndCorrectPasswordForceSilenceWithoutTheChallengeOrAnchor() {
        val session = session()
        session.onEvent(FiringEvent.EscapeHatchLongPressed)

        val result = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))

        assertEquals(FiringState.EscapeHatchUsed, result)
    }

    @Test
    fun theEscapeHatchIsAvailableEvenAfterTheSoundCapExpires() {
        val session = session()
        session.onEvent(FiringEvent.Tick(1.hours))
        session.onEvent(FiringEvent.EscapeHatchLongPressed)

        val result = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))

        assertEquals(FiringState.EscapeHatchUsed, result)
    }

    @Test
    fun aWrongPasswordDoesNotForceSilence() {
        val session = session()
        session.onEvent(FiringEvent.EscapeHatchLongPressed)

        val state = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("guess"))

        assertTrue(state is FiringState.Ringing)
        assertTrue(state.ringing().challenge is DismissalState.Ongoing)
        assertFalse(state.ringing().anchorReached)
    }

    @Test
    fun theEscapeHatchIsUnavailableWhenNoPasswordIsSet() {
        val session = session(escapeHatch = EscapeHatch.none)
        session.onEvent(FiringEvent.EscapeHatchLongPressed)

        val state = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))

        assertTrue(state is FiringState.Ringing)
    }

    @Test
    fun theEscapeHatchPromptCanBeCancelledAndRevealedAgain() {
        val session = session()
        val revealed = session.onEvent(FiringEvent.EscapeHatchLongPressed).ringing()
        assertTrue(revealed.escapeHatchRevealed)

        val cancelled = session.onEvent(FiringEvent.EscapeHatchCancelled).ringing()
        assertFalse(cancelled.escapeHatchRevealed)

        val ignored = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))
        assertTrue(ignored is FiringState.Ringing)
    }

    @Test
    fun anEscapeHatchUseIsTerminal() {
        val session = session()
        session.onEvent(FiringEvent.EscapeHatchLongPressed)
        session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))

        assertEquals(FiringState.EscapeHatchUsed, session.onEvent(FiringEvent.AnswerSubmitted("anything")))
        assertEquals(
            FiringState.EscapeHatchUsed,
            session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached)),
        )
        assertEquals(FiringState.EscapeHatchUsed, session.onEvent(FiringEvent.Tick(1.seconds)))
        assertEquals(FiringState.EscapeHatchUsed, session.onEvent(FiringEvent.EscapeHatchLongPressed))
    }

    @Test
    fun aDismissedFiringSessionCannotBeForceSilenced() {
        val session = session()
        val answer = session.state.ringing().challenge().challenge.answer
        session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))
        session.onEvent(FiringEvent.EscapeHatchLongPressed)

        val result = session.onEvent(FiringEvent.EscapeHatchPasswordSubmitted("open-sesame"))

        assertEquals(FiringState.Dismissed, result)
    }

    @Test
    fun theSummaryReportsRingingTimeAndWrongAnswers() {
        val session = session()
        session.onEvent(FiringEvent.Tick(45.seconds))
        session.onEvent(FiringEvent.AnswerSubmitted("definitely-not-the-answer"))
        session.onEvent(FiringEvent.AnswerSubmitted("still-not-the-answer"))

        val summary = session.summary()

        assertEquals(45.seconds, summary.ringingDuration)
        assertEquals(2, summary.wrongAnswers)
    }

    @Test
    fun theSummaryKeepsTheWrongAnswerCountAfterDismissal() {
        val session = session()
        session.onEvent(FiringEvent.AnswerSubmitted("wrong-1"))
        session.onEvent(FiringEvent.AnswerSubmitted("wrong-2"))
        val answer = session.state.ringing().challenge().challenge.answer
        session.onEvent(FiringEvent.AnswerSubmitted(answer.toString()))
        session.onEvent(FiringEvent.AnchorScanned(AnchorScanResult.Reached))
        assertEquals(FiringState.Dismissed, session.state)

        assertEquals(2, session.summary().wrongAnswers)
    }
}
