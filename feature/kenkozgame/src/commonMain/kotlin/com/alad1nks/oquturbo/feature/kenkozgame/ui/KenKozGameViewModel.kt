package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.KenKozGameRepository
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class KenKozGameViewModel(
    private val mode: KenKozGameMode,
    private val characters: List<String>,
    private val words: List<String>,
    private val differencePairs: List<Pair<String, String>>,
    private val trainingEntryId: String?,
    trainingRequiredScore: Int?,
    private val kenKozGameRepository: KenKozGameRepository,
    private val gameActivityRepository: GameActivityRepository,
    private val dailyTrainingRepository: DailyTrainingRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            KenKozGameUiState(
                mode = mode,
                trainingRequiredScore = trainingRequiredScore,
            ),
        )
    val uiState = _uiState.asStateFlow()

    private var showingDurationMillis = INITIAL_SHOWING_DURATION_MILLIS
    private var roundJob: Job? = null
    private var sessionStartMark: TimeMark? = null
    private var hasContinuedTraining = false

    init {
        require((trainingEntryId == null) == (trainingRequiredScore == null)) {
            "Wide Eye training id and required score must be provided together"
        }
        require(trainingEntryId == null || trainingEntryId.isNotBlank()) {
            "Wide Eye training id must not be blank"
        }
        require(trainingRequiredScore == null || trainingRequiredScore > 0) {
            "Wide Eye training required score must be positive"
        }
    }

    fun start() {
        showingDurationMillis = INITIAL_SHOWING_DURATION_MILLIS
        sessionStartMark = TimeSource.Monotonic.markNow()
        hasContinuedTraining = false
        _uiState.update {
            it.copy(
                score = 0,
                trainingNextEntry = null,
                isTrainingCompletionReady = false,
            )
        }
        startRound()
    }

    fun continueTraining(onContinue: (DailyTrainingEntry?) -> Unit) {
        val state = _uiState.value
        if (!state.isTrainingCompletionReady || hasContinuedTraining) return

        hasContinuedTraining = true
        _uiState.value = state.copy(isTrainingCompletionReady = false)
        onContinue(state.trainingNextEntry)
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
            updateRecord(state.score)
        }
    }

    private fun updateRecord(score: Int) {
        val startMark = sessionStartMark ?: return
        sessionStartMark = null
        val sessionDurationMillis = startMark.elapsedNow().inWholeMilliseconds
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                val storedRecord = kenKozGameRepository.getRecord(mode.name).first() ?: 0
                val currentRecord = maxOf(storedRecord, score)
                _uiState.update { it.copy(record = currentRecord) }
                gameActivityRepository.recordCompletedSession(
                    game = GameId.WideEye,
                    mode = mode.toGameModeId(),
                    variantId = null,
                    score = score,
                    correctAnswers = score,
                    durationMillis = sessionDurationMillis,
                    isNewRecord = currentRecord > storedRecord,
                )
                if (currentRecord > storedRecord) {
                    kenKozGameRepository.setRecord(mode.name, currentRecord)
                }
                trainingEntryId?.let { entryId ->
                    val plan = dailyTrainingRepository.completeEntry(entryId, score)
                    _uiState.update {
                        it.copy(
                            trainingNextEntry = plan.nextEntry,
                            isTrainingCompletionReady = true,
                        )
                    }
                }
            }
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
            KenKozGameMode.Characters -> createPositionRound(characters.shuffled().take(4))
            KenKozGameMode.Words -> createPositionRound(words.shuffled().take(4))
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
        val (regular, different) = differencePairs.random()
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
        val items = words.shuffled().take(4)
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

    private fun KenKozGameMode.toGameModeId(): GameModeId =
        when (this) {
            KenKozGameMode.Characters -> GameModeId.WideEyeCharacters
            KenKozGameMode.Words -> GameModeId.WideEyeWords
            KenKozGameMode.FindDifference -> GameModeId.WideEyeFindDifference
            KenKozGameMode.WideLine -> GameModeId.WideEyeWideLine
        }

    private companion object {
        const val INITIAL_SHOWING_DURATION_MILLIS = 2_000L
        const val SPEED_FACTOR_PERCENT = 95L
    }
}
