package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class KenKozGameViewModel(
    private val mode: KenKozGameMode,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KenKozGameUiState(mode = mode))
    val uiState = _uiState.asStateFlow()

    private var showingDurationMillis = INITIAL_SHOWING_DURATION_MILLIS
    private var roundJob: Job? = null

    fun start() {
        showingDurationMillis = INITIAL_SHOWING_DURATION_MILLIS
        _uiState.update { it.copy(score = 0) }
        startRound()
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value
        if (state.phase != KenKozGameUiState.Phase.Answering) return

        if (answer == state.correctAnswer) {
            showingDurationMillis = (showingDurationMillis * SPEED_FACTOR_PERCENT) / 100
            _uiState.update { it.copy(score = it.score + 1) }
            startRound()
        } else {
            roundJob?.cancel()
            _uiState.update { it.copy(phase = KenKozGameUiState.Phase.Mistake) }
        }
    }

    private fun startRound() {
        roundJob?.cancel()
        val round = createRound()
        _uiState.update {
            it.copy(
                phase = KenKozGameUiState.Phase.Showing,
                items = round.items,
                answers = round.answers,
                correctAnswer = round.correctAnswer,
                questionDirection = round.questionDirection,
            )
        }
        scheduleQuestion()
    }

    private fun scheduleQuestion() {
        roundJob?.cancel()
        roundJob =
            viewModelScope.launch {
                delay(showingDurationMillis.milliseconds)
                _uiState.update { it.copy(phase = KenKozGameUiState.Phase.Answering) }
            }
    }

    private fun createRound(): Round {
        return when (mode) {
            KenKozGameMode.Characters -> createPositionRound(CHARACTERS.shuffled().take(4))
            KenKozGameMode.Words -> createPositionRound(WORDS.shuffled().take(4))
            KenKozGameMode.FindDifference -> createFindDifferenceRound()
            KenKozGameMode.WideLine -> createWideLineRound()
        }
    }

    private fun createPositionRound(items: List<String>): Round {
        val direction = KenKozGameUiState.Direction.entries.random()
        val correctAnswer = items[direction.ordinal]
        return Round(
            items = items,
            answers = items.shuffled(),
            correctAnswer = correctAnswer,
            questionDirection = direction,
        )
    }

    private fun createFindDifferenceRound(): Round {
        val (regular, different) = DIFFERENCE_PAIRS.random()
        val direction = KenKozGameUiState.Direction.entries.random()
        val items = MutableList(4) { regular }.apply { this[direction.ordinal] = different }
        return Round(
            items = items,
            answers = KenKozGameUiState.Direction.entries.map { it.name },
            correctAnswer = direction.name,
            questionDirection = null,
        )
    }

    private fun createWideLineRound(): Round {
        val items = WORDS.shuffled().take(4)
        return Round(
            items = items,
            answers = items.shuffled(),
            correctAnswer = items.first(),
            questionDirection = null,
        )
    }

    private data class Round(
        val items: List<String>,
        val answers: List<String>,
        val correctAnswer: String,
        val questionDirection: KenKozGameUiState.Direction?,
    )

    private companion object {
        const val INITIAL_SHOWING_DURATION_MILLIS = 2_000L
        const val SPEED_FACTOR_PERCENT = 95L

        val CHARACTERS = ('А'..'Я').map(Char::toString) + ('0'..'9').map(Char::toString)
        val WORDS =
            listOf(
                "море",
                "свет",
                "дом",
                "лес",
                "лето",
                "ветер",
                "дорога",
                "город",
                "река",
                "поле",
                "небо",
                "книга",
            )
        val DIFFERENCE_PAIRS =
            listOf(
                "книга" to "кинза",
                "город" to "горох",
                "ветер" to "вечер",
                "слово" to "снова",
            )
    }
}
