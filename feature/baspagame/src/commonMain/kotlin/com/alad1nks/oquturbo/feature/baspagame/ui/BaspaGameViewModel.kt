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
    private var categoryIndex = content.categories.indices.random()
    private var letterIndex = content.letters.indices.random()
    private var wordLengthIndex = content.wordLengths.indices.random()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    record = repository.getRecord(mode.name).first() ?: 0,
                    categoryName = content.categories[categoryIndex].name,
                    categoryId = content.categories[categoryIndex].id,
                    letter = content.letters[letterIndex],
                    wordLength = content.wordLengths[wordLengthIndex],
                )
            }
            showNextStimulus()
        }
    }

    fun tap() {
        if (_uiState.value.phase != BaspaGameUiState.Phase.Playing) return
        if (_uiState.value.shouldTap) success() else mistake()
    }

    fun togglePause() {
        when (_uiState.value.phase) {
            BaspaGameUiState.Phase.Initial -> {
                _uiState.update { it.copy(phase = BaspaGameUiState.Phase.Playing) }
                startTimer()
            }
            BaspaGameUiState.Phase.Playing -> {
                timerJob?.cancel()
                _uiState.update { it.copy(phase = BaspaGameUiState.Phase.Paused) }
            }
            BaspaGameUiState.Phase.Paused -> {
                _uiState.update { it.copy(phase = BaspaGameUiState.Phase.Playing) }
                if (_uiState.value.stimulus.isEmpty()) {
                    scheduleNextRound()
                } else {
                    startTimer()
                }
            }
            BaspaGameUiState.Phase.Mistake -> Unit
        }
    }

    fun restart() {
        seenWords.clear()
        categoryIndex = content.categories.indices.random()
        letterIndex = content.letters.indices.random()
        wordLengthIndex = content.wordLengths.indices.random()
        _uiState.update {
            it.copy(
                score = 0,
                categoryName = content.categories[categoryIndex].name,
                categoryId = content.categories[categoryIndex].id,
                letter = content.letters[letterIndex],
                wordLength = content.wordLengths[wordLengthIndex],
                intervalMillis = INITIAL_INTERVAL_MILLIS,
                phase = BaspaGameUiState.Phase.Playing,
            )
        }
        scheduleNextRound()
    }

    private fun showNextStimulus() {
        val stimulus = createStimulus()
        _uiState.update {
            it.copy(stimulus = stimulus.text, shouldTap = stimulus.shouldTap, accent = stimulus.accent)
        }
        if (_uiState.value.phase == BaspaGameUiState.Phase.Playing) startTimer()
    }

    private fun scheduleNextRound(delayMillis: Long = WORDS_GAP_MILLIS) {
        timerJob?.cancel()
        _uiState.update { it.copy(stimulus = "") }
        timerJob =
            viewModelScope.launch {
                delay(delayMillis.milliseconds)
                if (_uiState.value.phase == BaspaGameUiState.Phase.Playing) {
                    showNextStimulus()
                }
            }
    }

    private fun startTimer() {
        timerJob =
            viewModelScope.launch {
                delay(_uiState.value.intervalMillis.milliseconds)
                if (_uiState.value.shouldTap) mistake() else scheduleNextRound()
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
        if (newScore % CHALLENGE_CHANGE_SCORE == 0 && mode in challengeChangingModes) {
            when (mode) {
                BaspaGameMode.Categories -> changeCategory()
                BaspaGameMode.Letter -> changeLetter()
                BaspaGameMode.WordLength -> changeWordLength()
                else -> Unit
            }
            scheduleNextRound(CHALLENGE_CHANGE_GAP_MILLIS)
        } else {
            scheduleNextRound()
        }
    }

    private fun mistake() {
        timerJob?.cancel()
        _uiState.update { it.copy(phase = BaspaGameUiState.Phase.Mistake) }
    }

    private fun createStimulus(): Stimulus {
        return when (mode) {
            BaspaGameMode.Categories -> categoryWord()
            BaspaGameMode.Letter -> letterWord()
            BaspaGameMode.WordLength -> wordLengthWord()
            BaspaGameMode.TextColor -> {
                val shouldTap = listOf(true, false).random()
                Stimulus(
                    text = content.allWords.random(),
                    shouldTap = shouldTap,
                    accent = if (shouldTap) BaspaGameUiState.Accent.Target else BaspaGameUiState.Accent.Other,
                )
            }
            BaspaGameMode.TrueFalse -> content.statements.random().toStimulus()
            BaspaGameMode.Math -> content.equations.random().toStimulus()
            BaspaGameMode.SpeedReading -> speedReadingStimulus()
        }
    }

    private fun categoryWord(): Stimulus {
        val shouldTap = listOf(true, false).random()
        val currentCategory = content.categories[categoryIndex]
        val otherWords = content.categories.filterIndexed { index, _ -> index != categoryIndex }.flatMap { it.words }
        return Stimulus(
            text = if (shouldTap) currentCategory.words.random() else otherWords.random(),
            shouldTap = shouldTap,
        )
    }

    private fun letterWord(): Stimulus {
        val letter = content.letters[letterIndex]
        return stimulusForRule { word -> letter in word }
    }

    private fun wordLengthWord(): Stimulus {
        val length = content.wordLengths[wordLengthIndex]
        return stimulusForRule { word -> word.length == length }
    }

    private fun stimulusForRule(matches: (String) -> Boolean): Stimulus {
        val shouldTap = listOf(true, false).random()
        val words = content.allWords.filter { matches(it) == shouldTap }
        return Stimulus(
            text = words.random(),
            shouldTap = shouldTap,
        )
    }

    private fun changeCategory() {
        val previousIndex = categoryIndex
        categoryIndex = content.categories.indices.filterNot { it == previousIndex }.random()
        _uiState.update {
            it.copy(
                categoryName = content.categories[categoryIndex].name,
                categoryId = content.categories[categoryIndex].id,
            )
        }
    }

    private fun changeLetter() {
        val previousIndex = letterIndex
        letterIndex = content.letters.indices.filterNot { it == previousIndex }.random()
        _uiState.update { it.copy(letter = content.letters[letterIndex]) }
    }

    private fun changeWordLength() {
        val previousIndex = wordLengthIndex
        wordLengthIndex = content.wordLengths.indices.filterNot { it == previousIndex }.random()
        _uiState.update { it.copy(wordLength = content.wordLengths[wordLengthIndex]) }
    }

    private fun speedReadingStimulus(): Stimulus {
        val allWords = content.allWords
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
        const val WORDS_GAP_MILLIS = 300L
        const val CHALLENGE_CHANGE_GAP_MILLIS = 1_000L
        const val CHALLENGE_CHANGE_SCORE = 10
        val challengeChangingModes =
            setOf(BaspaGameMode.Categories, BaspaGameMode.Letter, BaspaGameMode.WordLength)
    }
}
