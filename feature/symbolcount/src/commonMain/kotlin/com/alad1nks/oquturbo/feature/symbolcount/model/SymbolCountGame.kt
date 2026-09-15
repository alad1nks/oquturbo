package com.alad1nks.oquturbo.feature.symbolcount.model

import kotlin.random.Random

enum class SymbolCountPhase { Ready, Active, Correct, Paused, Result }

enum class SymbolCountFailure { Wrong, Timeout }

enum class CountShape { Circle, Square, Triangle, Diamond }

data class SymbolCountBoard(
    val id: Long,
    val size: Int,
    val shapes: List<CountShape>,
    val target: CountShape,
    val options: List<Int>,
    val totalTimeMillis: Long,
    val remainingTimeMillis: Long = totalTimeMillis,
) {
    val actualCount: Int get() = shapes.count { it == target }
}

data class SymbolCountState(
    val phase: SymbolCountPhase = SymbolCountPhase.Ready,
    val board: SymbolCountBoard? = null,
    val score: Int = 0,
    val activeDurationMillis: Long = 0,
    val feedbackRemainingMillis: Long = 600,
    val returnPhase: SymbolCountPhase = SymbolCountPhase.Active,
    val failure: SymbolCountFailure? = null,
    val selectedNumber: Int? = null,
) {
    val correctAnswers: Int get() = score
}

class SymbolCountGame(private val random: Random = Random.Default) {
    var state = SymbolCountState()
        private set
    private var nextBoardId = 0L

    fun start() {
        state = SymbolCountState(phase = SymbolCountPhase.Active, board = createBoard(0))
    }

    fun answer(boardId: Long, value: Int, elapsedMillis: Long = 0) {
        val board = state.board ?: return
        if (state.phase != SymbolCountPhase.Active || board.id != boardId || value !in board.options) return
        elapse(elapsedMillis)
        if (state.phase != SymbolCountPhase.Active) return
        state =
            if (value == board.actualCount) {
                state.copy(
                    phase = SymbolCountPhase.Correct,
                    score = state.score + 1,
                    selectedNumber = value,
                    feedbackRemainingMillis = 600,
                )
            } else {
                state.copy(phase = SymbolCountPhase.Result, failure = SymbolCountFailure.Wrong, selectedNumber = value)
            }
    }

    fun elapse(millis: Long) {
        val board = state.board ?: return
        if (millis <= 0) return
        when (state.phase) {
            SymbolCountPhase.Active -> {
                val accepted = millis.coerceAtMost(board.remainingTimeMillis)
                val remaining = board.remainingTimeMillis - accepted
                state =
                    state.copy(
                        board = board.copy(remainingTimeMillis = remaining),
                        activeDurationMillis = state.activeDurationMillis + accepted,
                        phase = if (remaining == 0L) SymbolCountPhase.Result else SymbolCountPhase.Active,
                        failure = if (remaining == 0L) SymbolCountFailure.Timeout else null,
                    )
            }
            SymbolCountPhase.Correct -> {
                val remaining = (state.feedbackRemainingMillis - millis).coerceAtLeast(0)
                state =
                    if (remaining == 0L) {
                        state.copy(
                            phase = SymbolCountPhase.Active,
                            board = createBoard(state.score),
                            selectedNumber = null,
                            feedbackRemainingMillis = 600,
                        )
                    } else {
                        state.copy(feedbackRemainingMillis = remaining)
                    }
            }
            else -> Unit
        }
    }

    fun pause(elapsedMillis: Long = 0) {
        if (state.phase != SymbolCountPhase.Active && state.phase != SymbolCountPhase.Correct) return
        if (state.phase == SymbolCountPhase.Correct) {
            state =
                state.copy(
                    feedbackRemainingMillis =
                        (state.feedbackRemainingMillis - elapsedMillis.coerceAtLeast(0)).coerceAtLeast(0),
                )
        } else {
            elapse(elapsedMillis)
        }
        if (state.phase != SymbolCountPhase.Result) {
            state = state.copy(returnPhase = state.phase, phase = SymbolCountPhase.Paused)
        }
    }

    fun resume() {
        if (state.phase == SymbolCountPhase.Paused) state = state.copy(phase = state.returnPhase)
    }

    private fun createBoard(score: Int): SymbolCountBoard {
        val size = sizeFor(score)
        val maximum = maximumFor(score)
        val target = CountShape.entries.random(random)
        val count = random.nextInt(1, maximum + 1)
        val distractors = CountShape.entries.filter { it != target }
        val shapes =
            (
                List(
                    count,
                ) { target } + List(size * size - count) { distractors.random(random) }
            ).shuffled(random)
        val options = (listOf(count) + (1..maximum).filter { it != count }.shuffled(random).take(3)).shuffled(random)
        return SymbolCountBoard(++nextBoardId, size, shapes, target, options, timeFor(score))
    }

    companion object {
        fun sizeFor(score: Int): Int =
            when {
                score < 5 -> 3
                score < 10 -> 4
                else -> 5
            }

        fun maximumFor(score: Int): Int =
            when {
                score < 5 -> 4
                score < 10 -> 6
                else -> 8
            }

        fun timeFor(score: Int): Long =
            when {
                score < 5 -> 15_000
                score < 10 -> 18_000
                else -> 20_000
            }
    }
}
