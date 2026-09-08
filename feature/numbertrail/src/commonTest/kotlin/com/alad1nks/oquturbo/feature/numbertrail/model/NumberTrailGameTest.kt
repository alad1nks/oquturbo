package com.alad1nks.oquturbo.feature.numbertrail.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumberTrailGameTest {
    @Test
    fun permutationsAndExactCompletedBoardBoundaries() {
        val game = NumberTrailGame(Random(23))
        game.start()
        repeat(20) { completed ->
            val board = requireNotNull(game.state.board)
            val expectedSize =
                when {
                    completed < 3 -> 2
                    completed < 6 -> 3
                    else -> 4
                }
            val expectedTime =
                when {
                    completed < 3 -> 12_000L
                    completed < 6 -> 20_000L
                    else -> 30_000L
                }
            assertEquals(expectedSize, board.size)
            assertEquals(expectedTime, board.remainingTimeMillis)
            assertEquals((1..expectedSize * expectedSize).toList(), board.numbers.sorted())
            for (number in 1..expectedSize * expectedSize) {
                game.answer(board.id, board.numbers.indexOf(number), 1)
                assertEquals(board.numbers, game.state.board!!.numbers)
            }
            assertEquals(NumberTrailPhase.BoardComplete, game.state.phase)
            assertEquals(completed + 1, game.state.completedBoards)
            val score = game.state.score
            game.answer(board.id, 0)
            assertEquals(score, game.state.score)
            game.elapse(599)
            assertEquals(NumberTrailPhase.BoardComplete, game.state.phase)
            game.elapse(1)
            assertEquals(NumberTrailPhase.Active, game.state.phase)
            assertEquals(1, game.state.board!!.target)
            assertNotEquals(board.id, game.state.board!!.id)
        }
        assertEquals(game.state.score.toLong(), game.state.activeDurationMillis)
    }

    @Test
    fun completedInvalidAndStaleTilesCannotScoreOrEndSession() {
        val game = NumberTrailGame(Random(1))
        game.start()
        val board = game.state.board!!
        game.answer(board.id, board.numbers.indexOf(1), 100)
        game.answer(board.id, board.numbers.indexOf(1), 100)
        game.answer(board.id, -1)
        game.answer(board.id, 4)
        assertEquals(1, game.state.score)
        assertEquals(11_800, game.state.board!!.remainingTimeMillis)
        assertEquals(NumberTrailPhase.Active, game.state.phase)
        game.answer(board.id, board.numbers.indexOf(3))
        assertEquals(NumberTrailFailure.Wrong, game.state.failure)
        assertEquals(3, game.state.selectedNumber)
        assertEquals(2, game.state.board!!.target)
        game.start()
        val fresh = game.state
        game.answer(board.id, 0, 99_000)
        assertEquals(fresh, game.state)
    }

    @Test
    fun deadlineWinsFinalTileAndPauseTies() {
        val game = NumberTrailGame(Random(4))
        game.start()
        val board = game.state.board!!
        for (number in 1..3) game.answer(board.id, board.numbers.indexOf(number))
        game.answer(board.id, board.numbers.indexOf(4), 12_000)
        assertEquals(3, game.state.score)
        assertEquals(NumberTrailFailure.Timeout, game.state.failure)
        assertNull(game.state.selectedNumber)
        assertEquals(12_000, game.state.activeDurationMillis)
        game.start()
        game.pause(12_000)
        assertEquals(NumberTrailPhase.Result, game.state.phase)
        assertEquals(NumberTrailFailure.Timeout, game.state.failure)
    }

    @Test
    fun pauseFreezesActiveAndFeedbackTimeWithoutGeneratingBoard() {
        val game = NumberTrailGame(Random(3))
        game.start()
        game.pause(300)
        val paused = game.state
        game.elapse(50_000)
        game.pause()
        assertEquals(paused, game.state)
        game.resume()
        val board = game.state.board!!
        assertEquals(11_700, board.remainingTimeMillis)
        for (number in 1..4) game.answer(board.id, board.numbers.indexOf(number))
        game.pause(200)
        assertEquals(400, game.state.feedbackRemainingMillis)
        game.elapse(50_000)
        game.resume()
        game.elapse(399)
        assertEquals(NumberTrailPhase.BoardComplete, game.state.phase)
        game.elapse(1)
        assertEquals(NumberTrailPhase.Active, game.state.phase)
        assertEquals(300, game.state.activeDurationMillis)
        assertTrue(game.state.board!!.id != board.id)
    }
}
