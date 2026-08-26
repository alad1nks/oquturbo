package com.alad1nks.oquturbo.feature.memorygrid.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemoryGridGameTest {
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
    fun mistakeEndsGameAndExposesExpectedCell() {
        val game = MemoryGridGame(RecordingGenerator()).apply { start() }
        finishPresentation(game)

        game.selectCell(0)
        game.selectCell(8)

        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
        assertEquals(listOf(0), game.state.input)
        assertEquals(1, game.state.correctCellCount)
        assertEquals(1, game.state.mistakeIndex)
        assertEquals(1, game.state.expectedCellAfterMistake)

        game.selectCell(1)
        game.continueAfterSuccess()
        assertEquals(MemoryGridPhase.GameOver, game.state.phase)
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
}
