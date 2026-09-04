package com.alad1nks.oquturbo.feature.rotationmatch.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RotationMatchGameTest {
    @Test
    fun rotationAndMirrorTransformsPreserveCellsAndHaveExpectedCoordinates() {
        val board = RotationMatchBoard(3, setOf(0, 3, 4, 7))

        assertEquals(setOf(1, 2, 3, 4), board.rotateClockwise().filledCells)
        assertEquals(setOf(2, 4, 5, 7), board.mirrorHorizontally().filledCells)
        assertEquals(board, board.rotateClockwise(4))
        assertEquals(board.filledCells.size, board.mirrorHorizontally().filledCells.size)
        assertTrue(board.isOrthogonallyConnected())
        assertTrue(board.isValidReference())
    }

    @Test
    fun generatedReferencesMeetEveryStructuralInvariantAtEveryDifficulty() {
        val game = RotationMatchGame(SeededRandom())
        game.start()
        repeat(14) {
            val round = game.state.round!!
            val expectedDifficulty = RotationMatchGame.difficultyFor(game.state.score)
            assertEquals(expectedDifficulty, round.difficulty)
            assertEquals(RotationMatchGame.gridSizeFor(expectedDifficulty), round.reference.size)
            assertEquals(RotationMatchGame.filledCountFor(expectedDifficulty), round.reference.filledCells.size)
            assertTrue(round.reference.isOrthogonallyConnected())
            assertTrue(round.reference.isValidReference())
            assertFalse((1..3).any { round.reference.rotateClockwise(it) == round.reference })
            assertFalse((0..3).any { round.reference.mirrorHorizontally().rotateClockwise(it) == round.reference })
            game.answer(round.correctAnswer)
            game.continueAfterCorrect()
        }
    }

    @Test
    fun repeatedDecksContainExactlyTwoOfEachOutcome() {
        val game = RotationMatchGame(SeededRandom())
        game.start()
        val outcomes = mutableListOf<RotationMatchAnswer>()
        repeat(12) {
            val answer = game.state.round!!.correctAnswer
            outcomes += answer
            game.answer(answer)
            game.continueAfterCorrect()
        }

        outcomes.chunked(4).forEach { deck ->
            assertEquals(2, deck.count { it == RotationMatchAnswer.Match })
            assertEquals(2, deck.count { it == RotationMatchAnswer.Different })
        }
    }

    @Test
    fun candidatesFollowTheirDeclaredTransformClass() {
        val game = RotationMatchGame(SeededRandom())
        game.start()
        repeat(8) {
            val round = game.state.round!!
            val rotations = (0..3).map(round.reference::rotateClockwise).toSet()
            if (round.correctAnswer == RotationMatchAnswer.Match) {
                assertTrue(round.candidate in rotations)
            } else {
                assertFalse(round.candidate in rotations)
                assertTrue(round.candidate in (0..3).map { round.reference.mirrorHorizontally().rotateClockwise(it) })
            }
            game.answer(round.correctAnswer)
            game.continueAfterCorrect()
        }
    }

    @Test
    fun thresholdsUseApprovedBoardsAndTimers() {
        assertEquals(RotationMatchDifficulty.Easy, RotationMatchGame.difficultyFor(0))
        assertEquals(RotationMatchDifficulty.Easy, RotationMatchGame.difficultyFor(4))
        assertEquals(RotationMatchDifficulty.Medium, RotationMatchGame.difficultyFor(5))
        assertEquals(RotationMatchDifficulty.Medium, RotationMatchGame.difficultyFor(9))
        assertEquals(RotationMatchDifficulty.Hard, RotationMatchGame.difficultyFor(10))
        assertEquals(10_000L, RotationMatchGame.timeFor(RotationMatchDifficulty.Easy))
        assertEquals(8_000L, RotationMatchGame.timeFor(RotationMatchDifficulty.Medium))
        assertEquals(6_000L, RotationMatchGame.timeFor(RotationMatchDifficulty.Hard))
    }

    @Test
    fun correctAnswerCountsOnceAndWaitsForFeedbackTransition() {
        val game = RotationMatchGame(SeededRandom())
        game.start()
        val frozenRound = game.state.round
        val answer = frozenRound!!.correctAnswer

        game.answer(answer)
        game.answer(answer)

        assertEquals(RotationMatchPhase.CorrectFeedback, game.state.phase)
        assertEquals(1, game.state.score)
        assertEquals(1, game.state.correctAnswers)
        assertEquals(frozenRound, game.state.round)
        game.continueAfterCorrect()
        assertEquals(RotationMatchPhase.Active, game.state.phase)
        assertEquals(1, game.state.score)
        assertNotEquals(frozenRound, game.state.round)
    }

    @Test
    fun wrongAndTimeoutFreezeRoundAndLateInputsAreIgnored() {
        val wrongGame = RotationMatchGame(SeededRandom())
        wrongGame.start()
        val wrongRound = wrongGame.state.round!!
        val wrong = wrongRound.correctAnswer.opposite()
        wrongGame.answer(wrong)
        wrongGame.answer(wrongRound.correctAnswer)
        assertEquals(RotationMatchFailure.Wrong, wrongGame.state.failure)
        assertEquals(wrong, wrongGame.state.selectedAnswer)
        assertEquals(wrongRound, wrongGame.state.round)
        assertEquals(0, wrongGame.state.score)

        val timeoutGame = RotationMatchGame(SeededRandom())
        timeoutGame.start()
        val timeoutRound = timeoutGame.state.round!!
        timeoutGame.elapse(timeoutRound.totalTimeMillis)
        timeoutGame.answer(timeoutRound.correctAnswer)
        assertEquals(RotationMatchFailure.Timeout, timeoutGame.state.failure)
        assertNull(timeoutGame.state.selectedAnswer)
        assertEquals(0L, timeoutGame.state.round!!.remainingTimeMillis)
        assertEquals(timeoutRound.reference, timeoutGame.state.round!!.reference)
    }

    @Test
    fun tapAtDeadlineTimesOutBeforeAnswerIsApplied() {
        val game = RotationMatchGame(SeededRandom())
        game.start()
        val round = game.state.round!!

        game.answer(round.correctAnswer, round.remainingTimeMillis)

        assertEquals(RotationMatchPhase.Result, game.state.phase)
        assertEquals(RotationMatchFailure.Timeout, game.state.failure)
        assertEquals(0, game.state.score)
        assertNull(game.state.selectedAnswer)
    }

    @Test
    fun replayResetsScoreDifficultyTimerAndDeck() {
        val game = RotationMatchGame(SeededRandom())
        game.start()
        repeat(6) {
            game.answer(game.state.round!!.correctAnswer)
            game.continueAfterCorrect()
        }
        assertEquals(RotationMatchDifficulty.Medium, game.state.round!!.difficulty)

        game.start()

        assertEquals(0, game.state.score)
        assertEquals(0, game.state.correctAnswers)
        assertEquals(RotationMatchDifficulty.Easy, game.state.round!!.difficulty)
        assertEquals(10_000L, game.state.round!!.remainingTimeMillis)
    }

    @Test
    fun invalidRandomAndShuffleFailFast() {
        assertFailsWith<IllegalArgumentException> {
            RotationMatchGame(
                object : RotationMatchRandom {
                    override fun nextInt(until: Int): Int = until

                    override fun <T> shuffle(values: List<T>): List<T> = values
                },
            ).start()
        }
        assertFailsWith<IllegalArgumentException> {
            RotationMatchGame(
                object : RotationMatchRandom {
                    private val delegate = SeededRandom()

                    override fun nextInt(until: Int): Int = delegate.nextInt(until)

                    override fun <T> shuffle(values: List<T>): List<T> = values.drop(1)
                },
            ).start()
        }
    }

    private fun RotationMatchAnswer.opposite(): RotationMatchAnswer =
        if (this == RotationMatchAnswer.Match) RotationMatchAnswer.Different else RotationMatchAnswer.Match

    private class SeededRandom : RotationMatchRandom {
        private val random = Random(7)

        override fun nextInt(until: Int): Int = random.nextInt(until)

        override fun <T> shuffle(values: List<T>): List<T> = values.shuffled(random)
    }
}
