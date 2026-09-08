package com.alad1nks.oquturbo.feature.numbertrail.model

import kotlin.random.Random

enum class NumberTrailPhase { Ready, Active, BoardComplete, Paused, Result }

enum class NumberTrailFailure { Wrong, Timeout }

data class NumberTrailBoard(
    val id: Long,
    val size: Int,
    val numbers: List<Int>,
    val totalTimeMillis: Long,
    val remainingTimeMillis: Long = totalTimeMillis,
    val target: Int = 1,
)

data class NumberTrailState(
    val phase: NumberTrailPhase = NumberTrailPhase.Ready,
    val board: NumberTrailBoard? = null,
    val score: Int = 0,
    val completedBoards: Int = 0,
    val activeDurationMillis: Long = 0,
    val feedbackRemainingMillis: Long = 600,
    val returnPhase: NumberTrailPhase = NumberTrailPhase.Active,
    val failure: NumberTrailFailure? = null,
    val selectedNumber: Int? = null,
) {
    val correctAnswers: Int get() = score
}

class NumberTrailGame(private val random: Random = Random.Default) {
    var state = NumberTrailState()
        private set
    private var nextBoardId = 0L

    fun start() {
        state = NumberTrailState(phase = NumberTrailPhase.Active, board = createBoard(0))
    }

    fun answer(boardId: Long, index: Int, elapsedMillis: Long = 0) {
        val board = state.board ?: return
        if (state.phase != NumberTrailPhase.Active || board.id != boardId) return
        elapse(elapsedMillis)
        if (state.phase != NumberTrailPhase.Active || index !in board.numbers.indices) return
        val number = board.numbers[index]
        if (number < board.target) return
        if (number != board.target) {
            state =
                state.copy(
                    phase = NumberTrailPhase.Result,
                    failure = NumberTrailFailure.Wrong,
                    selectedNumber = number,
                )
            return
        }
        val complete = number == board.size * board.size
        state =
            state.copy(
                board = requireNotNull(state.board).copy(target = board.target + 1),
                score = state.score + 1,
                completedBoards = state.completedBoards + if (complete) 1 else 0,
                phase = if (complete) NumberTrailPhase.BoardComplete else NumberTrailPhase.Active,
                feedbackRemainingMillis = 600,
            )
    }

    fun elapse(millis: Long) {
        val board = state.board ?: return
        if (millis <= 0) return
        when (state.phase) {
            NumberTrailPhase.Active -> {
                val accepted = millis.coerceAtMost(board.remainingTimeMillis)
                val remaining = board.remainingTimeMillis - accepted
                state =
                    state.copy(
                        board = board.copy(remainingTimeMillis = remaining),
                        activeDurationMillis = state.activeDurationMillis + accepted,
                        phase = if (remaining == 0L) NumberTrailPhase.Result else NumberTrailPhase.Active,
                        failure = if (remaining == 0L) NumberTrailFailure.Timeout else null,
                    )
            }
            NumberTrailPhase.BoardComplete -> {
                val remaining = (state.feedbackRemainingMillis - millis).coerceAtLeast(0)
                state =
                    if (remaining == 0L) {
                        state.copy(phase = NumberTrailPhase.Active, board = createBoard(state.completedBoards))
                    } else {
                        state.copy(feedbackRemainingMillis = remaining)
                    }
            }
            else -> Unit
        }
    }

    fun pause(elapsedMillis: Long = 0) {
        if (state.phase != NumberTrailPhase.Active && state.phase != NumberTrailPhase.BoardComplete) return
        // During feedback, retain even zero unspent time until deliberate resume; never generate an invisible board.
        if (state.phase == NumberTrailPhase.BoardComplete) {
            state =
                state.copy(
                    feedbackRemainingMillis = (state.feedbackRemainingMillis - elapsedMillis).coerceAtLeast(0),
                )
        } else {
            elapse(elapsedMillis)
        }
        if (state.phase != NumberTrailPhase.Result) {
            state =
                state.copy(
                    returnPhase = state.phase,
                    phase = NumberTrailPhase.Paused,
                )
        }
    }

    fun resume() {
        if (state.phase == NumberTrailPhase.Paused) state = state.copy(phase = state.returnPhase)
    }

    private fun createBoard(completedBoards: Int): NumberTrailBoard {
        val size = sizeFor(completedBoards)
        return NumberTrailBoard(++nextBoardId, size, (1..size * size).shuffled(random), timeFor(completedBoards))
    }

    companion object {
        fun sizeFor(completedBoards: Int): Int =
            when {
                completedBoards < 3 -> 2
                completedBoards < 6 -> 3
                else -> 4
            }

        fun timeFor(completedBoards: Int): Long =
            when {
                completedBoards < 3 -> 12_000
                completedBoards < 6 -> 20_000
                else -> 30_000
            }
    }
}
