package com.alad1nks.oquturbo.feature.ruleswitch.model

import kotlin.random.Random

enum class RuleSwitchPhase { Ready, Active, Correct, Paused, Result }

enum class RuleSwitchFailure { Wrong, Timeout }

enum class SwitchRule { Parity, Magnitude }

enum class SwitchAnswer { Even, Odd, Below, Above }

data class RuleSwitchBoard(
    val id: Long,
    val digit: Int,
    val rule: SwitchRule,
    val totalTimeMillis: Long,
    val remainingTimeMillis: Long = totalTimeMillis,
) {
    val options: List<SwitchAnswer> get() =
        when (rule) {
            SwitchRule.Parity -> listOf(SwitchAnswer.Even, SwitchAnswer.Odd)
            SwitchRule.Magnitude -> listOf(SwitchAnswer.Below, SwitchAnswer.Above)
        }
    val correctAnswer: SwitchAnswer get() =
        when (rule) {
            SwitchRule.Parity -> if (digit % 2 == 0) SwitchAnswer.Even else SwitchAnswer.Odd
            SwitchRule.Magnitude -> if (digit < 5) SwitchAnswer.Below else SwitchAnswer.Above
        }
}

data class RuleSwitchState(
    val phase: RuleSwitchPhase = RuleSwitchPhase.Ready,
    val board: RuleSwitchBoard? = null,
    val score: Int = 0,
    val activeDurationMillis: Long = 0,
    val feedbackRemainingMillis: Long = 600,
    val returnPhase: RuleSwitchPhase = RuleSwitchPhase.Active,
    val failure: RuleSwitchFailure? = null,
    val selectedAnswer: SwitchAnswer? = null,
) {
    val correctAnswers: Int get() = score
}

class RuleSwitchGame(private val random: Random = Random.Default) {
    var state = RuleSwitchState()
        private set
    private var nextBoardId = 0L

    fun start() {
        state = RuleSwitchState(phase = RuleSwitchPhase.Active, board = createBoard(0))
    }

    fun answer(boardId: Long, value: SwitchAnswer, elapsedMillis: Long = 0) {
        val board = state.board ?: return
        if (state.phase != RuleSwitchPhase.Active || board.id != boardId || value !in board.options) return
        elapse(elapsedMillis)
        if (state.phase != RuleSwitchPhase.Active) return
        state =
            if (value == board.correctAnswer) {
                state.copy(
                    phase = RuleSwitchPhase.Correct,
                    score = state.score + 1,
                    selectedAnswer = value,
                    feedbackRemainingMillis = 600,
                )
            } else {
                state.copy(phase = RuleSwitchPhase.Result, failure = RuleSwitchFailure.Wrong, selectedAnswer = value)
            }
    }

    fun elapse(millis: Long) {
        val board = state.board ?: return
        if (millis <= 0) return
        when (state.phase) {
            RuleSwitchPhase.Active -> {
                val accepted = millis.coerceAtMost(board.remainingTimeMillis)
                val remaining = board.remainingTimeMillis - accepted
                state =
                    state.copy(
                        board = board.copy(remainingTimeMillis = remaining),
                        activeDurationMillis = state.activeDurationMillis + accepted,
                        phase = if (remaining == 0L) RuleSwitchPhase.Result else RuleSwitchPhase.Active,
                        failure = if (remaining == 0L) RuleSwitchFailure.Timeout else null,
                    )
            }
            RuleSwitchPhase.Correct -> {
                val remaining = (state.feedbackRemainingMillis - millis).coerceAtLeast(0)
                state =
                    if (remaining == 0L) {
                        state.copy(
                            phase = RuleSwitchPhase.Active,
                            board = createBoard(state.score),
                            selectedAnswer = null,
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
        if (state.phase != RuleSwitchPhase.Active && state.phase != RuleSwitchPhase.Correct) return
        if (state.phase == RuleSwitchPhase.Correct) {
            state =
                state.copy(
                    feedbackRemainingMillis =
                        (state.feedbackRemainingMillis - elapsedMillis.coerceAtLeast(0)).coerceAtLeast(0),
                )
        } else {
            elapse(elapsedMillis)
        }
        if (state.phase != RuleSwitchPhase.Result) {
            state = state.copy(returnPhase = state.phase, phase = RuleSwitchPhase.Paused)
        }
    }

    fun resume() {
        if (state.phase == RuleSwitchPhase.Paused) state = state.copy(phase = state.returnPhase)
    }

    fun abandon() {
        state = RuleSwitchState()
    }

    private fun createBoard(score: Int): RuleSwitchBoard =
        RuleSwitchBoard(
            id = ++nextBoardId,
            digit = digits.random(random),
            rule = if (score % 2 == 0) SwitchRule.Parity else SwitchRule.Magnitude,
            totalTimeMillis = timeFor(score),
        )

    companion object {
        val digits = listOf(1, 2, 3, 4, 6, 7, 8, 9)

        fun timeFor(score: Int): Long =
            when {
                score < 5 -> 6_000
                score < 10 -> 5_000
                else -> 4_000
            }
    }
}
