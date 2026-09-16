package com.alad1nks.oquturbo.feature.ruleswitch.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuleSwitchGameTest {
    @Test
    fun everyAllowedDigitHasExactlyTheExpectedClassificationForBothRules() {
        for (digit in RuleSwitchGame.digits) {
            val parity = RuleSwitchBoard(1, digit, SwitchRule.Parity, 6000)
            assertEquals(listOf(SwitchAnswer.Even, SwitchAnswer.Odd), parity.options)
            assertEquals(if (digit in listOf(2, 4, 6, 8)) SwitchAnswer.Even else SwitchAnswer.Odd, parity.correctAnswer)
            val magnitude = parity.copy(rule = SwitchRule.Magnitude)
            assertEquals(listOf(SwitchAnswer.Below, SwitchAnswer.Above), magnitude.options)
            assertEquals(if (digit in 1..4) SwitchAnswer.Below else SwitchAnswer.Above, magnitude.correctAnswer)
        }
        assertEquals(listOf(1, 2, 3, 4, 6, 7, 8, 9), RuleSwitchGame.digits)
    }

    @Test
    fun pauseAtExpiryCannotRescueAnAttemptAndTerminalInputCannotChangeResult() {
        val game = RuleSwitchGame(Random(2))
        game.start()
        val board = game.state.board!!
        game.pause(6000)
        val result = game.state
        assertEquals(RuleSwitchFailure.Timeout, result.failure)
        game.answer(board.id, board.correctAnswer)
        game.resume()
        game.elapse(300)
        assertEquals(result, game.state)
        game.start()
        game.answer(board.id, board.correctAnswer)
        assertEquals(0, game.state.score)
        assertEquals(SwitchRule.Parity, game.state.board!!.rule)
    }

    @Test
    fun seededFieldsAndOptionsMeetEveryStageIncludingLongCappedRuns() {
        repeat(40) { seed ->
            val game = RuleSwitchGame(Random(seed))
            game.start()
            repeat(40) { score ->
                val board = game.state.board!!
                assertTrue(board.digit in RuleSwitchGame.digits)
                assertEquals(if (score % 2 == 0) SwitchRule.Parity else SwitchRule.Magnitude, board.rule)
                assertEquals(2, board.options.distinct().size)
                assertEquals(1, board.options.count { it == board.correctAnswer })
                assertEquals(
                    when {
                        score < 5 -> 6000L
                        score < 10 -> 5000L
                        else -> 4000L
                    },
                    board.totalTimeMillis,
                )
                game.answer(board.id, board.correctAnswer)
                game.elapse(600)
            }
        }
    }

    @Test
    fun correctAnswerLocksRoundAndRejectsDuplicateInvalidAndStaleEvents() {
        val game = RuleSwitchGame(Random(4))
        game.start()
        val board = game.state.board!!
        game.answer(board.id, SwitchAnswer.Above)
        assertEquals(0, game.state.score)
        game.answer(board.id + 1, board.correctAnswer)
        assertEquals(0, game.state.score)
        game.answer(board.id, board.correctAnswer, 120)
        game.answer(board.id, board.correctAnswer)
        assertEquals(1, game.state.score)
        assertEquals(RuleSwitchPhase.Correct, game.state.phase)
        assertEquals(120, game.state.activeDurationMillis)
        game.elapse(599)
        assertEquals(board.id, game.state.board!!.id)
        game.elapse(1)
        assertNotEquals(board.id, game.state.board!!.id)
        game.answer(board.id, board.correctAnswer)
        assertEquals(1, game.state.score)
    }

    @Test
    fun timeoutAtDeadlineWinsAndWrongValueIsTruthful() {
        val game = RuleSwitchGame(Random(1))
        game.start()
        var board = game.state.board!!
        game.answer(board.id, board.correctAnswer, 6_000)
        assertEquals(RuleSwitchFailure.Timeout, game.state.failure)
        assertNull(game.state.selectedAnswer)
        assertEquals(0, game.state.score)
        assertEquals(6_000, game.state.activeDurationMillis)
        game.start()
        board = game.state.board!!
        val wrong = board.options.first { it != board.correctAnswer }
        game.answer(board.id, wrong, 90)
        assertEquals(RuleSwitchFailure.Wrong, game.state.failure)
        assertEquals(wrong, game.state.selectedAnswer)
        assertEquals(90, game.state.activeDurationMillis)
    }

    @Test
    fun pauseRetainsActiveAndFeedbackIntervalsWithoutOffscreenGeneration() {
        val game = RuleSwitchGame(Random(2))
        game.start()
        val board = game.state.board!!
        game.pause(-4)
        game.elapse(Long.MAX_VALUE)
        game.resume()
        assertEquals(6_000, game.state.board!!.remainingTimeMillis)
        game.pause(200)
        game.elapse(50_000)
        game.resume()
        assertEquals(5_800, game.state.board!!.remainingTimeMillis)
        game.answer(board.id, board.correctAnswer)
        game.pause(-10)
        game.resume()
        assertEquals(600, game.state.feedbackRemainingMillis)
        game.pause(200)
        val paused = game.state
        game.elapse(30_000)
        assertEquals(paused, game.state)
        game.resume()
        game.elapse(399)
        assertEquals(RuleSwitchPhase.Correct, game.state.phase)
        game.pause(1000)
        assertEquals(board.id, game.state.board!!.id)
        game.resume()
        game.elapse(1)
        assertEquals(RuleSwitchPhase.Active, game.state.phase)
        assertEquals(200, game.state.activeDurationMillis)
    }

    @Test
    fun seededRunsAreReproducibleAndRestartClearsTerminalState() {
        val first = RuleSwitchGame(Random(10))
        val second = RuleSwitchGame(Random(10))
        first.start()
        second.start()
        assertEquals(first.state, second.state)
        first.elapse(Long.MAX_VALUE)
        assertEquals(6_000, first.state.activeDurationMillis)
        first.start()
        assertEquals(0, first.state.score)
        assertNull(first.state.failure)
        assertNull(first.state.selectedAnswer)
        assertEquals(0, first.state.activeDurationMillis)
    }
}
