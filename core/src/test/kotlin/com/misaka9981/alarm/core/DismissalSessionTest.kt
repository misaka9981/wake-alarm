package com.misaka9981.alarm.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class DismissalSessionTest {
    private val policy = EscalationPolicy(
        wrongAnswerPenalty = 1,
        elapsedStep = 30.seconds,
        maxDifficulty = 9,
    )

    private fun session(baseDifficulty: Int = 1): DismissalSession =
        DismissalSession.start(
            generator = ArithmeticChallengeGenerator(Random(5)),
            policy = policy,
            baseDifficulty = baseDifficulty,
        )

    private fun DismissalState.ongoing(): DismissalState.Ongoing = this as DismissalState.Ongoing

    @Test
    fun startsOngoingWithAChallengeAtTheBaseDifficulty() {
        val state = session(baseDifficulty = 2).state.ongoing()

        assertEquals(2, state.difficulty)
        assertEquals(2, state.challenge.difficulty)
        assertEquals(0, state.wrongAnswers)
        assertNull(state.feedback)
        assertEquals(Duration.ZERO, state.elapsed)
    }

    @Test
    fun aCorrectAnswerDismissesTheSession() {
        val session = session()
        val answer = session.state.ongoing().challenge.answer

        val result = session.onEvent(DismissalEvent.AnswerSubmitted(answer.toString()))

        assertEquals(DismissalState.Dismissed, result)
    }

    @Test
    fun aWrongAnswerKeepsTheSessionOngoingAndGivesImmediateFeedback() {
        val result = session().onEvent(DismissalEvent.AnswerSubmitted("nonsense")).ongoing()

        assertEquals(1, result.wrongAnswers)
        assertEquals(WrongAnswer("nonsense"), result.feedback)
    }

    @Test
    fun aWrongAnswerRaisesTheDifficultyOfTheNextChallenge() {
        val session = session(baseDifficulty = 1)
        val before = session.state.ongoing().difficulty

        val after = session.onEvent(DismissalEvent.AnswerSubmitted("nope")).ongoing()

        assertTrue(after.difficulty > before)
        assertEquals(after.difficulty, after.challenge.difficulty)
    }

    @Test
    fun elapsedTimeRaisesTheDifficultyWithoutAnyWrongAnswer() {
        val after = session(baseDifficulty = 1)
            .onEvent(DismissalEvent.Tick(90.seconds))
            .ongoing()

        assertEquals(4, after.difficulty)
        assertEquals(4, after.challenge.difficulty)
    }

    @Test
    fun aTickThatDoesNotCrossAStepKeepsTheSameChallenge() {
        val session = session(baseDifficulty = 1)
        val before = session.state.ongoing().challenge

        val after = session.onEvent(DismissalEvent.Tick(10.seconds)).ongoing()

        assertEquals(before, after.challenge)
        assertEquals(1, after.difficulty)
        assertEquals(10.seconds, after.elapsed)
    }

    @Test
    fun stallingMakesTheChallengeHarderThenAnsweringCorrectlyStillDismisses() {
        val session = session(baseDifficulty = 1)
        val startDifficulty = session.state.ongoing().difficulty

        val stalled = session.onEvent(DismissalEvent.Tick(300.seconds)).ongoing()
        assertTrue(stalled.difficulty > startDifficulty)

        val result = session.onEvent(DismissalEvent.AnswerSubmitted(stalled.challenge.answer.toString()))

        assertEquals(DismissalState.Dismissed, result)
    }

    @Test
    fun repeatedWrongAnswersCompoundTheDifficulty() {
        val session = session(baseDifficulty = 1)
        repeat(3) { session.onEvent(DismissalEvent.AnswerSubmitted("wrong")) }

        val state = session.state.ongoing()

        assertEquals(3, state.wrongAnswers)
        assertEquals(4, state.difficulty)
    }

    @Test
    fun aDismissedSessionIgnoresFurtherEvents() {
        val session = session()
        val answer = session.state.ongoing().challenge.answer
        session.onEvent(DismissalEvent.AnswerSubmitted(answer.toString()))

        assertEquals(DismissalState.Dismissed, session.onEvent(DismissalEvent.AnswerSubmitted("anything")))
        assertEquals(DismissalState.Dismissed, session.onEvent(DismissalEvent.Tick(1.hours)))
    }

    @Test
    fun aWrongAnswerNeverDismissesThereIsNoSkipOrSnooze() {
        val session = session()
        repeat(20) { session.onEvent(DismissalEvent.AnswerSubmitted("wrong-$it")) }

        assertTrue(session.state is DismissalState.Ongoing)
    }
}
