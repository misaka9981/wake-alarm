package com.misaka9981.alarm.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FiringSessionTest {
    private val policy = EscalationPolicy(
        wrongAnswerPenalty = 1,
        elapsedStep = 30.seconds,
        maxDifficulty = 9,
    )

    private fun session(baseDifficulty: Int = 1): FiringSession =
        FiringSession.start(
            generator = ArithmeticChallengeGenerator(Random(7)),
            policy = policy,
            baseDifficulty = baseDifficulty,
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
}
