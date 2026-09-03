package com.alad1nks.oquturbo.feature.wordflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowContent
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowGame
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPhase
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowState
import com.alad1nks.oquturbo.feature.wordflow.model.normalizeWordFlowLocale
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal data class WordFlowUiState(
    val game: WordFlowState = WordFlowState(),
    val locale: String = "en",
    val record: Int = 0,
    val isRecordLoading: Boolean = true,
    val isNewRecord: Boolean = false,
    val completedDurationMillis: Long? = null,
)

internal class WordFlowViewModel(
    locale: String,
    content: WordFlowContent,
    private val activityRepository: GameActivityRepository,
    private val game: WordFlowGame = WordFlowGame(content),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    private val locale = normalizeWordFlowLocale(locale)
    private val _uiState = MutableStateFlow(WordFlowUiState(locale = this.locale))
    val uiState = _uiState.asStateFlow()
    private var record = 0
    private var attemptToken = 0L
    private var attemptStartedAt: TimeMark? = null
    private var activeAttemptToken: Long? = null
    private var lastTimerMark: TimeMark? = null
    private var timerJob: Job? = null
    private var feedbackJob: Job? = null
    private val recordedAttempts = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            activityRepository.observeRecords().collect { records ->
                record =
                    records.filter {
                        it.game == GameId.WordFlow &&
                            it.mode == GameModeId.WordFlowContext &&
                            it.variantId == this@WordFlowViewModel.locale
                    }.maxOfOrNull { it.score } ?: 0
                publish(isRecordLoading = false)
            }
        }
    }

    fun start() {
        if (_uiState.value.isRecordLoading) return
        attemptToken++
        cancelJobs()
        game.start()
        attemptStartedAt = timeSource.markNow()
        activeAttemptToken = attemptToken
        publish(isNewRecord = false, completedDurationMillis = null)
        scheduleTimer(attemptToken)
    }

    fun selectAnswer(answer: String) {
        if (game.state.phase != WordFlowPhase.Active) return
        val token = attemptToken
        if (activeAttemptToken != token) return
        game.selectAnswer(answer)
        if (game.state.phase == WordFlowPhase.Active) return
        timerJob?.cancel()
        when (game.state.phase) {
            WordFlowPhase.CorrectFeedback -> {
                publish()
                feedbackJob =
                    viewModelScope.launch {
                        delay(CORRECT_FEEDBACK_MILLIS)
                        if (token != attemptToken || game.state.phase != WordFlowPhase.CorrectFeedback) return@launch
                        game.continueAfterCorrect()
                        publish()
                        scheduleTimer(token)
                    }
            }
            WordFlowPhase.Result -> completeAttempt(token)
            else -> publish()
        }
    }

    fun abandon() {
        attemptToken++
        activeAttemptToken = null
        attemptStartedAt = null
        cancelJobs()
        publish(isNewRecord = false, completedDurationMillis = null)
    }

    internal fun advanceTimerBy(millis: Long) {
        val token = attemptToken
        if (activeAttemptToken != token || game.state.phase != WordFlowPhase.Active) return
        game.elapse(millis)
        if (game.state.phase == WordFlowPhase.Result) {
            completeAttempt(token)
        } else {
            publish()
        }
    }

    private fun scheduleTimer(token: Long) {
        timerJob?.cancel()
        lastTimerMark = timeSource.markNow()
        timerJob =
            viewModelScope.launch {
                while (token == attemptToken && game.state.phase == WordFlowPhase.Active) {
                    delay(TIMER_TICK_MILLIS)
                    if (token != attemptToken || game.state.phase != WordFlowPhase.Active) return@launch
                    val mark = lastTimerMark ?: return@launch
                    val elapsed = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1)
                    lastTimerMark = timeSource.markNow()
                    advanceTimerBy(elapsed)
                    if (game.state.phase == WordFlowPhase.Result) {
                        return@launch
                    }
                }
            }
    }

    private fun completeAttempt(token: Long) {
        if (token != attemptToken || !recordedAttempts.add(token)) return
        val finished = game.state
        if (finished.phase != WordFlowPhase.Result) return
        activeAttemptToken = null
        timerJob?.cancel()
        feedbackJob?.cancel()
        val duration = attemptStartedAt?.elapsedNow()?.inWholeMilliseconds?.coerceAtLeast(0) ?: 0
        val isNewRecord = finished.score > 0 && finished.score > record
        if (isNewRecord) record = finished.score
        publish(isNewRecord = false, completedDurationMillis = duration)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val recordedSession =
                withContext(NonCancellable) {
                    activityRepository.recordCompletedSession(
                        game = GameId.WordFlow,
                        mode = GameModeId.WordFlowContext,
                        variantId = locale,
                        score = finished.score,
                        correctAnswers = finished.correctAnswers,
                        durationMillis = duration,
                        isNewRecord = isNewRecord,
                    )
                }
            if (token != attemptToken) return@launch
            record = maxOf(record, recordedSession.score)
            publish(isNewRecord = recordedSession.isNewRecord, completedDurationMillis = duration)
        }
    }

    private fun publish(
        isRecordLoading: Boolean = _uiState.value.isRecordLoading,
        isNewRecord: Boolean = _uiState.value.isNewRecord,
        completedDurationMillis: Long? = _uiState.value.completedDurationMillis,
    ) {
        _uiState.value =
            WordFlowUiState(
                game = game.state,
                locale = locale,
                record = record,
                isRecordLoading = isRecordLoading,
                isNewRecord = isNewRecord,
                completedDurationMillis = completedDurationMillis,
            )
    }

    private fun cancelJobs() {
        timerJob?.cancel()
        feedbackJob?.cancel()
        timerJob = null
        feedbackJob = null
    }

    override fun onCleared() {
        cancelJobs()
        super.onCleared()
    }

    private companion object {
        const val TIMER_TICK_MILLIS = 100L
        const val CORRECT_FEEDBACK_MILLIS = 500L
    }
}
