package com.alad1nks.oquturbo.feature.rotationmatch.model

import kotlin.random.Random

enum class RotationMatchPhase { Ready, Active, CorrectFeedback, Result }

enum class RotationMatchFailure { Wrong, Timeout }

enum class RotationMatchAnswer { Match, Different }

enum class RotationMatchDifficulty { Easy, Medium, Hard }

data class RotationMatchBoard(
    val size: Int,
    val filledCells: Set<Int>,
) {
    init {
        require(size > 0) { "Rotation Match board size must be positive" }
        require(filledCells.isNotEmpty()) { "Rotation Match board must contain a filled cell" }
        require(filledCells.all { it in 0 until size * size }) {
            "Rotation Match board contains a cell outside its dimensions"
        }
    }

    fun rotateClockwise(quarterTurns: Int = 1): RotationMatchBoard {
        val turns = ((quarterTurns % 4) + 4) % 4
        var transformed = this
        repeat(turns) {
            transformed =
                RotationMatchBoard(
                    size = size,
                    filledCells =
                        transformed.filledCells.mapTo(mutableSetOf()) { index ->
                            val row = index / size
                            val column = index % size
                            column * size + (size - row - 1)
                        },
                )
        }
        return transformed
    }

    fun mirrorHorizontally(): RotationMatchBoard =
        RotationMatchBoard(
            size = size,
            filledCells =
                filledCells.mapTo(mutableSetOf()) { index ->
                    val row = index / size
                    val column = index % size
                    row * size + (size - column - 1)
                },
        )

    fun isOrthogonallyConnected(): Boolean {
        val visited = mutableSetOf<Int>()
        val pending = mutableListOf(filledCells.first())
        while (pending.isNotEmpty()) {
            val cell = pending.removeAt(pending.lastIndex)
            if (!visited.add(cell)) continue
            orthogonalNeighbors(cell).forEach { neighbor ->
                if (neighbor in filledCells && neighbor !in visited) {
                    pending.add(neighbor)
                }
            }
        }
        return visited.size == filledCells.size
    }

    fun isValidReference(): Boolean =
        isOrthogonallyConnected() &&
            (1..3).none { rotateClockwise(it) == this } &&
            (0..3).none { mirrorHorizontally().rotateClockwise(it) == this }

    private fun orthogonalNeighbors(index: Int): List<Int> {
        val row = index / size
        val column = index % size
        return buildList {
            if (row > 0) add(index - this@RotationMatchBoard.size)
            if (row < this@RotationMatchBoard.size - 1) add(index + this@RotationMatchBoard.size)
            if (column > 0) add(index - 1)
            if (column < this@RotationMatchBoard.size - 1) add(index + 1)
        }
    }
}

data class RotationMatchRound(
    val reference: RotationMatchBoard,
    val candidate: RotationMatchBoard,
    val correctAnswer: RotationMatchAnswer,
    val difficulty: RotationMatchDifficulty,
    val totalTimeMillis: Long,
    val remainingTimeMillis: Long = totalTimeMillis,
    val id: Long = 0L,
)

data class RotationMatchState(
    val phase: RotationMatchPhase = RotationMatchPhase.Ready,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val round: RotationMatchRound? = null,
    val selectedAnswer: RotationMatchAnswer? = null,
    val failure: RotationMatchFailure? = null,
)

interface RotationMatchRandom {
    fun nextInt(until: Int): Int

    fun <T> shuffle(values: List<T>): List<T>
}

object DefaultRotationMatchRandom : RotationMatchRandom {
    override fun nextInt(until: Int): Int = Random.nextInt(until)

    override fun <T> shuffle(values: List<T>): List<T> = values.shuffled()
}

class RotationMatchGame(
    private val random: RotationMatchRandom = DefaultRotationMatchRandom,
) {
    var state: RotationMatchState = RotationMatchState()
        private set

    private val answerDeck = mutableListOf<RotationMatchAnswer>()
    private var nextRoundId = 0L

    fun start() {
        answerDeck.clear()
        state = RotationMatchState(phase = RotationMatchPhase.Active, round = createRound(score = 0))
    }

    fun answer(
        answer: RotationMatchAnswer,
        elapsedMillis: Long = 0,
    ) {
        if (state.phase != RotationMatchPhase.Active) return
        elapse(elapsedMillis)
        val current = state
        val round = current.round ?: return
        if (current.phase != RotationMatchPhase.Active) return
        state =
            if (answer == round.correctAnswer) {
                current.copy(
                    phase = RotationMatchPhase.CorrectFeedback,
                    score = current.score + 1,
                    correctAnswers = current.correctAnswers + 1,
                    selectedAnswer = answer,
                )
            } else {
                current.copy(
                    phase = RotationMatchPhase.Result,
                    selectedAnswer = answer,
                    failure = RotationMatchFailure.Wrong,
                )
            }
    }

    fun continueAfterCorrect() {
        val current = state
        if (current.phase != RotationMatchPhase.CorrectFeedback) return
        state =
            current.copy(
                phase = RotationMatchPhase.Active,
                round = createRound(current.score),
                selectedAnswer = null,
                failure = null,
            )
    }

    fun elapse(millis: Long) {
        val current = state
        val round = current.round ?: return
        if (current.phase != RotationMatchPhase.Active || millis <= 0) return
        val remaining = (round.remainingTimeMillis - millis).coerceAtLeast(0)
        state =
            if (remaining == 0L) {
                current.copy(
                    phase = RotationMatchPhase.Result,
                    round = round.copy(remainingTimeMillis = 0),
                    selectedAnswer = null,
                    failure = RotationMatchFailure.Timeout,
                )
            } else {
                current.copy(round = round.copy(remainingTimeMillis = remaining))
            }
    }

    private fun createRound(score: Int): RotationMatchRound {
        val difficulty = difficultyFor(score)
        val board = generateReferenceBoard(difficulty)
        val answer = nextAnswer()
        val quarterTurns = checkedRandomIndex(3) + 1
        val candidate =
            when (answer) {
                RotationMatchAnswer.Match -> board.rotateClockwise(quarterTurns)
                RotationMatchAnswer.Different -> board.mirrorHorizontally().rotateClockwise(quarterTurns)
            }
        return RotationMatchRound(
            reference = board,
            candidate = candidate,
            correctAnswer = answer,
            difficulty = difficulty,
            totalTimeMillis = timeFor(difficulty),
            id = ++nextRoundId,
        )
    }

    private fun generateReferenceBoard(difficulty: RotationMatchDifficulty): RotationMatchBoard {
        val size = gridSizeFor(difficulty)
        val filledCount = filledCountFor(difficulty)
        repeat(MAX_GENERATION_ATTEMPTS) {
            val filled = mutableSetOf(checkedRandomIndex(size * size))
            while (filled.size < filledCount) {
                val frontier =
                    filled
                        .flatMap { index -> orthogonalNeighbors(index, size) }
                        .filterNot(filled::contains)
                        .distinct()
                check(frontier.isNotEmpty()) { "Rotation Match generator has no connected frontier" }
                filled += frontier[checkedRandomIndex(frontier.size)]
            }
            val board = RotationMatchBoard(size, filled)
            if (board.isValidReference()) return board
        }
        error("Unable to generate an asymmetric Rotation Match pattern")
    }

    private fun nextAnswer(): RotationMatchAnswer {
        if (answerDeck.isEmpty()) {
            val source =
                listOf(
                    RotationMatchAnswer.Match,
                    RotationMatchAnswer.Match,
                    RotationMatchAnswer.Different,
                    RotationMatchAnswer.Different,
                )
            val shuffled = random.shuffle(source)
            require(
                shuffled.size == source.size && shuffled.groupingBy {
                    it
                }.eachCount() == source.groupingBy { it }.eachCount(),
            ) {
                "Rotation Match shuffler changed the balanced answer deck"
            }
            answerDeck += shuffled
        }
        return answerDeck.removeAt(0)
    }

    private fun checkedRandomIndex(until: Int): Int {
        require(until > 0) { "Rotation Match random range must be positive" }
        return random.nextInt(until).also { value ->
            require(value in 0 until until) { "Rotation Match random returned $value outside 0 until $until" }
        }
    }

    private fun orthogonalNeighbors(
        index: Int,
        boardSize: Int,
    ): List<Int> {
        val row = index / boardSize
        val column = index % boardSize
        return buildList {
            if (row > 0) add(index - boardSize)
            if (row < boardSize - 1) add(index + boardSize)
            if (column > 0) add(index - 1)
            if (column < boardSize - 1) add(index + 1)
        }
    }

    companion object {
        const val EASY_TIME_MILLIS = 10_000L
        const val MEDIUM_TIME_MILLIS = 8_000L
        const val HARD_TIME_MILLIS = 6_000L
        private const val MAX_GENERATION_ATTEMPTS = 10_000

        fun difficultyFor(score: Int): RotationMatchDifficulty =
            when {
                score < 5 -> RotationMatchDifficulty.Easy
                score < 10 -> RotationMatchDifficulty.Medium
                else -> RotationMatchDifficulty.Hard
            }

        fun gridSizeFor(difficulty: RotationMatchDifficulty): Int =
            when (difficulty) {
                RotationMatchDifficulty.Easy -> 3
                RotationMatchDifficulty.Medium -> 4
                RotationMatchDifficulty.Hard -> 5
            }

        fun filledCountFor(difficulty: RotationMatchDifficulty): Int =
            when (difficulty) {
                RotationMatchDifficulty.Easy -> 4
                RotationMatchDifficulty.Medium -> 6
                RotationMatchDifficulty.Hard -> 8
            }

        fun timeFor(difficulty: RotationMatchDifficulty): Long =
            when (difficulty) {
                RotationMatchDifficulty.Easy -> EASY_TIME_MILLIS
                RotationMatchDifficulty.Medium -> MEDIUM_TIME_MILLIS
                RotationMatchDifficulty.Hard -> HARD_TIME_MILLIS
            }
    }
}
