package com.alad1nks.oquturbo.feature.symbolcount.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SymbolCountGameTest {
    @Test
    fun failuresRetainExactBoardAndReviewTimeCannotChangeCompletedMetrics() {
        for (timeout in listOf(false, true)) {
            for (score in listOf(0, 5, 10)) {
                val game = SymbolCountGame(Random(score))
                game.start()
                repeat(score) {
                    val board = requireNotNull(game.state.board)
                    game.answer(board.id, board.actualCount)
                    game.elapse(600)
                }
                val before = requireNotNull(game.state.board)
                if (timeout) {
                    game.elapse(before.remainingTimeMillis)
                } else {
                    game.answer(before.id, before.options.first { it != before.actualCount }, 90)
                }
                val failed = requireNotNull(game.state.board)
                assertEquals(before.id, failed.id)
                assertEquals(before.size, failed.size)
                assertEquals(before.shapes, failed.shapes)
                assertEquals(before.target, failed.target)
                assertEquals(before.actualCount, failed.actualCount)
                val terminal = game.state
                game.elapse(Long.MAX_VALUE)
                game.answer(before.id, before.actualCount)
                assertEquals(terminal, game.state)
                game.start()
                assertNotEquals(before.id, game.state.board!!.id)
                assertEquals(SymbolCountPhase.Active, game.state.phase)
            }
        }
    }

    @Test
    fun seededFieldsAndOptionsMeetEveryStageIncludingLongCappedRuns() {
        repeat(40) { seed ->
            val game = SymbolCountGame(Random(seed))
            game.start()
            repeat(40) { score ->
                val board = game.state.board!!
                val size =
                    when {
                        score < 5 -> 3
                        score < 10 -> 4
                        else -> 5
                    }
                val max =
                    when {
                        score < 5 -> 4
                        score < 10 -> 6
                        else -> 8
                    }
                val time =
                    when {
                        score < 5 -> 15_000L
                        score < 10 -> 18_000L
                        else -> 20_000L
                    }
                assertEquals(size, board.size)
                assertEquals(size * size, board.shapes.size)
                assertTrue(board.actualCount in 1..max)
                assertEquals(4, board.options.distinct().size)
                assertEquals(1, board.options.count { it == board.actualCount })
                assertTrue(board.options.all { it in 1..max })
                assertEquals(time, board.totalTimeMillis)
                game.answer(board.id, board.actualCount)
                game.elapse(600)
            }
        }
    }

    @Test
    fun correctAnswerLocksRoundAndRejectsDuplicateInvalidAndStaleEvents() {
        val game = SymbolCountGame(Random(4))
        game.start()
        val board = game.state.board!!
        game.answer(board.id, 99)
        assertEquals(0, game.state.score)
        game.answer(board.id + 1, board.actualCount)
        assertEquals(0, game.state.score)
        game.answer(board.id, board.actualCount, 120)
        game.answer(board.id, board.actualCount)
        assertEquals(1, game.state.score)
        assertEquals(SymbolCountPhase.Correct, game.state.phase)
        assertEquals(120, game.state.activeDurationMillis)
        game.elapse(599)
        assertEquals(board.id, game.state.board!!.id)
        game.elapse(1)
        assertNotEquals(board.id, game.state.board!!.id)
        game.answer(board.id, board.actualCount)
        assertEquals(1, game.state.score)
    }

    @Test
    fun timeoutAtDeadlineWinsAndWrongValueIsTruthful() {
        val game = SymbolCountGame(Random(1))
        game.start()
        var board = game.state.board!!
        game.answer(board.id, board.actualCount, 15_000)
        assertEquals(SymbolCountFailure.Timeout, game.state.failure)
        assertNull(game.state.selectedNumber)
        assertEquals(0, game.state.score)
        assertEquals(15_000, game.state.activeDurationMillis)
        game.start()
        board = game.state.board!!
        val wrong = board.options.first { it != board.actualCount }
        game.answer(board.id, wrong, 90)
        assertEquals(SymbolCountFailure.Wrong, game.state.failure)
        assertEquals(wrong, game.state.selectedNumber)
        assertEquals(90, game.state.activeDurationMillis)
    }

    @Test
    fun pauseRetainsActiveAndFeedbackIntervalsWithoutOffscreenGeneration() {
        val game = SymbolCountGame(Random(2))
        game.start()
        val board = game.state.board!!
        game.pause(-4)
        game.elapse(Long.MAX_VALUE)
        game.resume()
        assertEquals(15_000, game.state.board!!.remainingTimeMillis)
        game.pause(200)
        game.elapse(50_000)
        game.resume()
        assertEquals(14_800, game.state.board!!.remainingTimeMillis)
        game.answer(board.id, board.actualCount)
        game.pause(-10)
        game.resume()
        assertEquals(600, game.state.feedbackRemainingMillis)
        game.pause(200)
        val paused = game.state
        game.elapse(30_000)
        assertEquals(paused, game.state)
        game.resume()
        game.elapse(399)
        assertEquals(SymbolCountPhase.Correct, game.state.phase)
        game.pause(1000)
        assertEquals(board.id, game.state.board!!.id)
        game.resume()
        game.elapse(1)
        assertEquals(SymbolCountPhase.Active, game.state.phase)
        assertEquals(200, game.state.activeDurationMillis)
    }

    @Test
    fun seededRunsAreReproducibleAndRestartClearsTerminalState() {
        val first = SymbolCountGame(Random(10))
        val second = SymbolCountGame(Random(10))
        first.start()
        second.start()
        assertEquals(first.state, second.state)
        first.elapse(Long.MAX_VALUE)
        assertEquals(15_000, first.state.activeDurationMillis)
        first.start()
        assertEquals(0, first.state.score)
        assertNull(first.state.failure)
        assertNull(first.state.selectedNumber)
        assertEquals(0, first.state.activeDurationMillis)
    }
}
