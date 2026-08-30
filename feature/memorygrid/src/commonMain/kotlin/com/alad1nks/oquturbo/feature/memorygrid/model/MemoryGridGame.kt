package com.alad1nks.oquturbo.feature.memorygrid.model

/**
 * Platform-independent prototype of the Memory Grid game rules.
 *
 * A UI or ViewModel drives presentation by calling [advancePresentation] on a timer and renders [state]. The
 * engine itself does not own a coroutine or depend on a platform clock, which keeps every transition deterministic.
 */
class MemoryGridGame(
    private val sequenceGenerator: MemoryGridSequenceGenerator = RandomMemoryGridSequenceGenerator(),
    val mode: MemoryGridGameMode = MemoryGridGameMode.Route,
) {
    var state: MemoryGridState = MemoryGridState()
        private set

    fun start() {
        state = createRound(sequenceLength = INITIAL_SEQUENCE_LENGTH, score = 0, correctCellCount = 0)
    }

    fun advancePresentation() {
        val current = state
        if (current.phase != MemoryGridPhase.ShowingSequence) return

        state =
            if (mode != MemoryGridGameMode.Flash && current.presentationIndex < current.sequence.lastIndex) {
                current.copy(presentationIndex = current.presentationIndex + 1)
            } else {
                current.copy(
                    phase = MemoryGridPhase.AwaitingInput,
                    presentationIndex = NO_CELL_INDEX,
                )
            }
    }

    fun selectCell(cellIndex: Int) {
        val current = state
        if (current.phase != MemoryGridPhase.AwaitingInput) return
        require(cellIndex in 0 until current.cellCount) {
            "Memory Grid cell index $cellIndex is outside a ${current.gridSize}x${current.gridSize} grid"
        }

        val expectedSequence =
            when (mode) {
                MemoryGridGameMode.Route -> current.sequence
                MemoryGridGameMode.Reverse -> current.sequence.reversed()
                MemoryGridGameMode.Flash -> current.sequence
            }
        val expectedCell = expectedSequence[current.input.size]
        val isCorrect =
            if (mode == MemoryGridGameMode.Flash) {
                cellIndex in current.sequence && cellIndex !in current.input
            } else {
                cellIndex == expectedCell
            }
        if (!isCorrect) {
            val expectedCells =
                if (mode == MemoryGridGameMode.Flash) {
                    current.sequence.filterNot { it in current.input }.toSet()
                } else {
                    setOf(expectedCell)
                }
            state =
                current.copy(
                    phase = MemoryGridPhase.GameOver,
                    mistakeIndex = current.input.size,
                    failedSelectedCell = cellIndex,
                    expectedCellsAfterMistake = expectedCells,
                )
            return
        }

        val updatedInput = current.input + cellIndex
        state =
            if (updatedInput.size == current.sequence.size) {
                current.copy(
                    phase = MemoryGridPhase.RoundSuccess,
                    input = updatedInput,
                    score = if (mode == MemoryGridGameMode.Flash) current.score + 1 else current.sequence.size,
                    correctCellCount = current.correctCellCount + 1,
                )
            } else {
                current.copy(
                    input = updatedInput,
                    correctCellCount = current.correctCellCount + 1,
                )
            }
    }

    fun continueAfterSuccess() {
        val current = state
        if (current.phase != MemoryGridPhase.RoundSuccess) return

        state =
            createRound(
                sequenceLength =
                    if (mode == MemoryGridGameMode.Flash) {
                        (current.sequence.size + 1).coerceAtMost(FLASH_CELL_COUNT)
                    } else {
                        current.sequence.size + 1
                    },
                score = current.score,
                correctCellCount = current.correctCellCount,
            )
    }

    private fun createRound(
        sequenceLength: Int,
        score: Int,
        correctCellCount: Int,
    ): MemoryGridState {
        val gridSize = if (mode == MemoryGridGameMode.Flash) 4 else gridSizeFor(sequenceLength)
        val allowRepeatedCells = mode != MemoryGridGameMode.Flash && sequenceLength >= REPEATED_CELLS_SEQUENCE_LENGTH
        val sequence =
            sequenceGenerator.generate(
                cellCount = gridSize * gridSize,
                length = sequenceLength,
                allowRepeatedCells = allowRepeatedCells,
            )
        require(sequence.size == sequenceLength) {
            "Memory Grid generator returned ${sequence.size} cells instead of $sequenceLength"
        }
        require(sequence.all { it in 0 until gridSize * gridSize }) {
            "Memory Grid generator returned a cell outside the grid"
        }
        require(allowRepeatedCells || sequence.distinct().size == sequence.size) {
            "Memory Grid generator returned repeated cells before repeats are enabled"
        }

        return MemoryGridState(
            phase = MemoryGridPhase.ShowingSequence,
            gridSize = gridSize,
            sequence = sequence,
            presentationIndex = 0,
            score = score,
            correctCellCount = correctCellCount,
            cellPresentationMillis = presentationMillisFor(sequenceLength),
        )
    }

    companion object {
        const val INITIAL_SEQUENCE_LENGTH = 3
        const val REPEATED_CELLS_SEQUENCE_LENGTH = 7
        const val INITIAL_PRESENTATION_MILLIS = 700L
        const val MIN_PRESENTATION_MILLIS = 350L
        const val PRESENTATION_STEP_MILLIS = 35L
        const val NO_CELL_INDEX = -1
        const val FLASH_CELL_COUNT = 16

        fun gridSizeFor(sequenceLength: Int): Int {
            require(sequenceLength >= INITIAL_SEQUENCE_LENGTH) { "Memory Grid sequence must contain at least 3 cells" }
            return when {
                sequenceLength <= 6 -> 3
                sequenceLength <= 10 -> 4
                else -> 5
            }
        }

        fun presentationMillisFor(sequenceLength: Int): Long {
            require(sequenceLength >= INITIAL_SEQUENCE_LENGTH) { "Memory Grid sequence must contain at least 3 cells" }
            val reduction = (sequenceLength - INITIAL_SEQUENCE_LENGTH) * PRESENTATION_STEP_MILLIS
            return (INITIAL_PRESENTATION_MILLIS - reduction).coerceAtLeast(MIN_PRESENTATION_MILLIS)
        }
    }
}

enum class MemoryGridPhase {
    Ready,
    ShowingSequence,
    AwaitingInput,
    RoundSuccess,
    GameOver,
}

data class MemoryGridState(
    val phase: MemoryGridPhase = MemoryGridPhase.Ready,
    val gridSize: Int = 3,
    val sequence: List<Int> = emptyList(),
    val presentationIndex: Int = MemoryGridGame.NO_CELL_INDEX,
    val input: List<Int> = emptyList(),
    val score: Int = 0,
    val correctCellCount: Int = 0,
    val cellPresentationMillis: Long = MemoryGridGame.INITIAL_PRESENTATION_MILLIS,
    val mistakeIndex: Int? = null,
    val failedSelectedCell: Int? = null,
    val expectedCellsAfterMistake: Set<Int> = emptySet(),
    val record: Int = 0,
    val isNewRecord: Boolean = false,
) {
    val cellCount: Int
        get() = gridSize * gridSize

    val highlightedCell: Int?
        get() = presentationIndex.takeIf { phase == MemoryGridPhase.ShowingSequence }?.let(sequence::get)

    fun highlightedCells(mode: MemoryGridGameMode): Set<Int> =
        when {
            phase != MemoryGridPhase.ShowingSequence -> emptySet()
            mode == MemoryGridGameMode.Flash -> sequence.toSet()
            else -> setOfNotNull(highlightedCell)
        }
}
