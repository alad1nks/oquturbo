package com.alad1nks.oquturbo.feature.memorygrid.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemoryGridGameTest {
    @Test
    fun sessionTotalIncludesCompletedAndPartialFinalRoundsInEveryMode() {
        MemoryGridGameMode.entries.forEach { mode ->
            val game = MemoryGridGame(RecordingGenerator(), mode).apply { start() }
            finishPresentation(game)
            val first = if (mode == MemoryGridGameMode.Reverse) game.state.sequence.reversed() else game.state.sequence
            first.forEach(game::selectCell)
            game.continueAfterSuccess()
            finishPresentation(game)
            val second = if (mode == MemoryGridGameMode.Reverse) game.state.sequence.reversed() else game.state.sequence
            second.take(2).forEach(game::selectCell)
            game.selectCell(8)

            assertEquals(MemoryGridPhase.GameOver, game.state.phase)
            assertEquals(5, game.state.correctCellCount)
            assertEquals(2, game.state.input.size)
            assertEquals(if (mode == MemoryGridGameMode.Flash) 1 else 3, game.state.score)

            game.start()
            assertEquals(0, game.state.correctCellCount)
            finishPresentation(game)
            game.selectCell(8)
            assertEquals(MemoryGridPhase.GameOver, game.state.phase)
            assertEquals(0, game.state.correctCellCount)
        }
    }

    @Test
    fun routeAndReverseCountEachAcceptedRepeatedCoordinate() {
        listOf(MemoryGridGameMode.Route, MemoryGridGameMode.Reverse).forEach { mode ->
            val generator =
                MemoryGridSequenceGenerator { _, length, repeated ->
                    if (repeated) List(length) { it % 2 } else List(length) { it }
                }
            val game = MemoryGridGame(generator, mode).apply { start() }
            repeat(4) {
                finishPresentation(game)
                val input =
                    if (mode == MemoryGridGameMode.Reverse) game.state.sequence.reversed() else game.state.sequence
                input.forEach(game::selectCell)
                game.continueAfterSuccess()
            }
            finishPresentation(game)
            listOf(0, 1, 0).forEach(game::selectCell)
            assertEquals(listOf(0, 1, 0), game.state.input)
            assertEquals(21, game.state.correctCellCount)
            game.selectCell(8)
            assertEquals(MemoryGridPhase.GameOver, game.state.phase)
            assertEquals(21, game.state.correctCellCount)
        }
    }

    @Test
    fun startCreatesFirstRoundAndRequestsUniqueCells() {
        val generator = RecordingGenerator()
        val game = MemoryGridGame(generator)

        game.start()

        assertEquals(MemoryGridPhase.ShowingSequence, game.state.phase)
        assertEquals(3, game.state.gridSize)
        assertEquals(listOf(0, 1, 2), game.state.sequence)
        assertEquals(0, game.state.highlightedCell)
        assertEquals(700L, game.state.cellPresentationMillis)
        assertEquals(
            GenerationRequest(
                cellCount = 9,
                length = 3,
                allowRepeatedCells = false,
            ),
            generator.requests.single(),
        )
    }

    @Test
    fun presentationMustFinishBeforeInputIsAccepted() {
        val game = MemoryGridGame(RecordingGenerator()).apply { start() }

        game.selectCell(0)
        assertEquals(emptyList(), game.state.input)

        finishPresentation(game)
        assertEquals(MemoryGridPhase.AwaitingInput, game.state.phase)
        assertNull(game.state.highlightedCell)
    }

    @Test
    fun correctInputCompletesRoundAndCarriesProgressIntoNextRound() {
        val generator = RecordingGenerator()
        val game = MemoryGridGame(generator).apply { start() }
        finishPresentation(game)

        game.state.sequence.forEach(game::selectCell)

        assertEquals(MemoryGridPhase.RoundSuccess, game.state.phase)
        assertEquals(3, game.state.score)
        assertEquals(3, game.state.correctCellCount)

        game.continueAfterSuccess()

        assertEquals(MemoryGridPhase.ShowingSequence, game.state.phase)
        assertEquals(4, game.state.sequence.size)
        assertEquals(3, game.state.score)
        assertEquals(3, game.state.correctCellCount)
        assertEquals(665L, game.state.cellPresentationMillis)
    }

    @Test
    fun reverseModeAcceptsCellsFromLastToFirst() {
        val game = MemoryGridGame(RecordingGenerator(), MemoryGridGameMode.Reverse).apply { start() }
        finishPresentation(game)

        game.state.sequence.reversed().forEach(game::selectCell)

        assertEquals(MemoryGridPhase.RoundSuccess, game.state.phase)
        assertEquals(3, game.state.score)
    }

    @Test
    fun flashModeShowsAllCellsAndAcceptsAnyOrder() {
        val game = MemoryGridGame(RecordingGenerator(), MemoryGridGameMode.Flash).apply { start() }

        assertEquals(4, game.state.gridSize)
        assertEquals(setOf(0, 1, 2), game.state.highlightedCells(MemoryGridGameMode.Flash))
        game.advancePresentation()
        assertEquals(MemoryGridPhase.AwaitingInput, game.state.phase)
        listOf(2, 0, 1).forEach(game::selectCell)

        assertEquals(MemoryGridPhase.RoundSuccess, game.state.phase)
        assertEquals(1, game.state.score)
        assertEquals(3, game.state.correctCellCount)
    }

    @Test
    fun routeMistakeEndsGameAndExposesWrongAndExpectedCells() {
        val game = MemoryGridGame(RecordingGenerator()).apply { start() }
        finishPresentation(game)

        game.selectCell(0)
        game.selectCell(8)

        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
        assertEquals(listOf(0), game.state.input)
        assertEquals(1, game.state.correctCellCount)
        assertEquals(1, game.state.mistakeIndex)
        assertEquals(8, game.state.failedSelectedCell)
        assertEquals(setOf(1), game.state.expectedCellsAfterMistake)

        game.selectCell(1)
        game.continueAfterSuccess()
        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
    }

    @Test
    fun reverseMistakeExposesCorrectNextCellInReverseOrder() {
        val game = MemoryGridGame(RecordingGenerator(), MemoryGridGameMode.Reverse).apply { start() }
        finishPresentation(game)

        game.selectCell(2)
        game.selectCell(8)

        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
        assertEquals(8, game.state.failedSelectedCell)
        assertEquals(setOf(1), game.state.expectedCellsAfterMistake)
    }

    @Test
    fun flashMistakeExposesEveryRemainingCorrectCell() {
        val game = MemoryGridGame(RecordingGenerator(), MemoryGridGameMode.Flash).apply { start() }
        finishPresentation(game)

        game.selectCell(1)
        game.selectCell(8)

        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
        assertEquals(listOf(1), game.state.input)
        assertEquals(8, game.state.failedSelectedCell)
        assertEquals(setOf(0, 2), game.state.expectedCellsAfterMistake)
    }

    @Test
    fun flashDuplicateTapKeepsAcceptedWrongAndRemainingRolesIndependent() {
        val game = MemoryGridGame(RecordingGenerator(), MemoryGridGameMode.Flash).apply { start() }
        finishPresentation(game)

        game.selectCell(1)
        game.selectCell(1)

        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
        assertEquals(listOf(1), game.state.input)
        assertEquals(1, game.state.correctCellCount)
        assertEquals(1, game.state.failedSelectedCell)
        assertEquals(setOf(0, 2), game.state.expectedCellsAfterMistake)
    }

    @Test
    fun retryStartsWithCleanMistakeFeedback() {
        val game = MemoryGridGame(RecordingGenerator()).apply { start() }
        finishPresentation(game)
        game.selectCell(8)

        game.start()

        assertEquals(MemoryGridPhase.ShowingSequence, game.state.phase)
        assertNull(game.state.mistakeIndex)
        assertNull(game.state.failedSelectedCell)
        assertEquals(emptySet(), game.state.expectedCellsAfterMistake)
        assertEquals(emptyList(), game.state.input)
    }

    @Test
    fun repeatedCoordinatesKeepAcceptedWrongAndExpectedRolesIndependent() {
        val generator =
            QueueGenerator(
                listOf(0, 1, 2),
                listOf(0, 1, 2, 3),
                listOf(0, 1, 2, 3, 4),
                listOf(0, 1, 2, 3, 4, 5),
                listOf(0, 1, 0, 2, 3, 4, 5),
            )
        val game = MemoryGridGame(generator).apply { start() }
        repeat(4) {
            finishPresentation(game)
            game.state.sequence.forEach(game::selectCell)
            game.continueAfterSuccess()
        }
        finishPresentation(game)

        game.selectCell(0)
        game.selectCell(1)
        game.selectCell(1)

        assertEquals(listOf(0, 1), game.state.input)
        assertEquals(1, game.state.failedSelectedCell)
        assertEquals(setOf(0), game.state.expectedCellsAfterMistake)
    }

    @Test
    fun gridAndRepeatRulesChangeOnlyBetweenRounds() {
        val generator = RecordingGenerator()
        val game = MemoryGridGame(generator).apply { start() }

        repeat(4) {
            finishPresentation(game)
            game.state.sequence.forEach(game::selectCell)
            game.continueAfterSuccess()
        }

        assertEquals(7, game.state.sequence.size)
        assertEquals(4, game.state.gridSize)
        assertEquals(
            GenerationRequest(cellCount = 16, length = 7, allowRepeatedCells = true),
            generator.requests.last(),
        )
    }

    @Test
    fun presentationSpeedHasLowerBound() {
        assertEquals(700L, MemoryGridGame.presentationMillisFor(3))
        assertEquals(350L, MemoryGridGame.presentationMillisFor(13))
        assertEquals(350L, MemoryGridGame.presentationMillisFor(100))
    }

    @Test
    fun invalidGeneratorOutputIsRejected() {
        val wrongLength = MemoryGridSequenceGenerator { _, _, _ -> listOf(0) }
        assertFailsWith<IllegalArgumentException> { MemoryGridGame(wrongLength).start() }

        val repeated = MemoryGridSequenceGenerator { _, length, _ -> List(length) { 0 } }
        assertFailsWith<IllegalArgumentException> { MemoryGridGame(repeated).start() }
    }

    private fun finishPresentation(game: MemoryGridGame) {
        repeat(game.state.sequence.size) { game.advancePresentation() }
    }

    private data class GenerationRequest(
        val cellCount: Int,
        val length: Int,
        val allowRepeatedCells: Boolean,
    )

    private class RecordingGenerator : MemoryGridSequenceGenerator {
        val requests = mutableListOf<GenerationRequest>()

        override fun generate(
            cellCount: Int,
            length: Int,
            allowRepeatedCells: Boolean,
        ): List<Int> {
            requests += GenerationRequest(cellCount, length, allowRepeatedCells)
            return List(length) { it % cellCount }
        }
    }

    private class QueueGenerator(vararg sequences: List<Int>) : MemoryGridSequenceGenerator {
        private val sequences = sequences.toMutableList()

        override fun generate(cellCount: Int, length: Int, allowRepeatedCells: Boolean): List<Int> =
            sequences.removeAt(0)
    }
}
