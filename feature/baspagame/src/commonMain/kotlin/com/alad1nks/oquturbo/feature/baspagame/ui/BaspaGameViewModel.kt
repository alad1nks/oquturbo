package com.alad1nks.oquturbo.feature.baspagame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.repository.BaspaGameRepository
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class BaspaGameViewModel(
    private val mode: BaspaGameMode,
    private val content: BaspaGameContent,
    private val repository: BaspaGameRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BaspaGameUiState(mode = mode))
    val uiState = _uiState.asStateFlow()
    private val seenWords = mutableListOf<String>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(record = repository.getRecord(mode.name).first() ?: 0) }
            nextRound()
        }
    }

    fun tap() {
        if (_uiState.value.phase != BaspaGameUiState.Phase.Playing) return
        if (_uiState.value.shouldTap) success() else mistake()
    }

    fun restart() {
        seenWords.clear()
        _uiState.update {
            it.copy(score = 0, intervalMillis = INITIAL_INTERVAL_MILLIS, phase = BaspaGameUiState.Phase.Playing)
        }
        nextRound()
    }

    private fun nextRound() {
        timerJob?.cancel()
        val stimulus = createStimulus()
        _uiState.update {
            it.copy(stimulus = stimulus.text, shouldTap = stimulus.shouldTap, accent = stimulus.accent)
        }
        timerJob =
            viewModelScope.launch {
                delay(_uiState.value.intervalMillis.milliseconds)
                if (_uiState.value.shouldTap) mistake() else nextRound()
            }
    }

    private fun success() {
        timerJob?.cancel()
        val newScore = _uiState.value.score + 1
        val oldRecord = _uiState.value.record
        val newRecord = maxOf(oldRecord, newScore)
        _uiState.update {
            it.copy(
                score = newScore,
                record = newRecord,
                intervalMillis = (it.intervalMillis * SPEED_FACTOR_PERCENT) / 100,
            )
        }
        if (newRecord > oldRecord) {
            viewModelScope.launch { repository.setRecord(mode.name, newRecord) }
        }
        nextRound()
    }

    private fun mistake() {
        timerJob?.cancel()
        _uiState.update { it.copy(phase = BaspaGameUiState.Phase.Mistake) }
    }

    private fun createStimulus(): Stimulus {
        return when (mode) {
            BaspaGameMode.Categories,
            BaspaGameMode.Letter,
            BaspaGameMode.WordLength,
            -> matchingOrOtherWord()
            BaspaGameMode.TextColor -> {
                val shouldTap = listOf(true, false).random()
                Stimulus(
                    text = (content.matchingWords + content.otherWords).random(),
                    shouldTap = shouldTap,
                    accent = if (shouldTap) BaspaGameUiState.Accent.Target else BaspaGameUiState.Accent.Other,
                )
            }
            BaspaGameMode.TrueFalse -> content.statements.random().toStimulus()
            BaspaGameMode.Math -> content.equations.random().toStimulus()
            BaspaGameMode.SpeedReading -> speedReadingStimulus()
        }
    }

    private fun matchingOrOtherWord(): Stimulus {
        val shouldTap = listOf(true, false).random()
        return Stimulus(
            text = if (shouldTap) content.matchingWords.random() else content.otherWords.random(),
            shouldTap = shouldTap,
        )
    }

    private fun speedReadingStimulus(): Stimulus {
        val allWords = content.matchingWords + content.otherWords
        val repeat = seenWords.isNotEmpty() && listOf(true, false).random()
        val word =
            if (repeat) {
                seenWords.random()
            } else {
                allWords.filterNot(
                    seenWords::contains,
                ).ifEmpty { allWords }.random()
            }
        if (!repeat) seenWords += word
        return Stimulus(word, repeat)
    }

    private fun Pair<String, Boolean>.toStimulus() = Stimulus(first, second)

    private data class Stimulus(
        val text: String,
        val shouldTap: Boolean,
        val accent: BaspaGameUiState.Accent = BaspaGameUiState.Accent.Default,
    )

    private companion object {
        const val INITIAL_INTERVAL_MILLIS = 2_000L
        const val SPEED_FACTOR_PERCENT = 95L
    }
}
