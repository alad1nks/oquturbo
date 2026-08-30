package com.alad1nks.oquturbo.feature.dualfocus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusGame
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusLane
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusPhase
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusState
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

internal data class DualFocusUiState(
    val game: DualFocusState = DualFocusState(),
    val record: Int = 0,
    val previousRecord: Int = 0,
    val durationMillis: Long = 0,
    val isRecordLoading: Boolean = true,
    val isNewRecord: Boolean = false,
    val correctFeedbackLane: DualFocusLane? = null,
)

internal class DualFocusViewModel(
    private val activityRepository: GameActivityRepository,
    private val game: DualFocusGame = DualFocusGame(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DualFocusUiState())
    val uiState = _uiState.asStateFlow()
    private var record = 0
    private var previousRecord = 0
    private var attemptToken = 0L
    private var attemptStartedAt: TimeMark? = null
    private var timerJob: Job? = null
    private var feedbackJob: Job? = null
    private val recordedAttempts = mutableSetOf<Long>()

    init {
        game.prepare()
        publish()
        viewModelScope.launch {
            activityRepository.observeRecords().collect { records ->
                record =
                    records.filter {
                        it.game == GameId.DualFocus &&
                            it.mode == GameModeId.DualFocusMatch &&
                            it.variantId == null
                    }.maxOfOrNull { it.score } ?: 0
                publish(isRecordLoading = false)
            }
        }
    }

    fun start() {
        if (_uiState.value.isRecordLoading) return
        attemptToken++
        cancelJobs()
        previousRecord = record
        attemptStartedAt = TimeSource.Monotonic.markNow()
        game.start(nowMillis = 0)
        publish(isNewRecord = false, durationMillis = 0, correctFeedbackLane = null)
        scheduleTimer(attemptToken)
    }

    fun tap(
        lane: DualFocusLane,
        cardId: Long,
    ) {
        if (game.state.phase != DualFocusPhase.Active) return
        val card = game.state.cards[lane] ?: return
        if (card.id != cardId) return
        val scoreBefore = game.state.score
        val token = attemptToken
        game.tap(lane, cardId, elapsedMillis())
        val correctLane = lane.takeIf { game.state.score > scoreBefore }
        publish(correctFeedbackLane = correctLane)
        if (correctLane != null) scheduleFeedbackClear(token, correctLane)
        if (game.state.phase == DualFocusPhase.Result) completeAttempt(token)
    }

    fun abandon() {
        attemptToken++
        cancelJobs()
        attemptStartedAt = null
    }

    internal fun advanceTo(millis: Long) {
        val token = attemptToken
        if (game.state.phase != DualFocusPhase.Active) return
        game.advanceTo(millis)
        publish()
        if (game.state.phase == DualFocusPhase.Result) completeAttempt(token)
    }

    private fun scheduleTimer(token: Long) {
        timerJob =
            viewModelScope.launch {
                while (token == attemptToken && game.state.phase == DualFocusPhase.Active) {
                    delay(TIMER_TICK_MILLIS)
                    if (token != attemptToken || game.state.phase != DualFocusPhase.Active) return@launch
                    advanceTo(elapsedMillis())
                }
            }
    }

    private fun scheduleFeedbackClear(
        token: Long,
        lane: DualFocusLane,
    ) {
        feedbackJob?.cancel()
        feedbackJob =
            viewModelScope.launch {
                delay(CORRECT_FEEDBACK_MILLIS)
                if (token == attemptToken && _uiState.value.correctFeedbackLane == lane) {
                    publish(correctFeedbackLane = null)
                }
            }
    }

    private fun completeAttempt(token: Long) {
        if (token != attemptToken ||
            !recordedAttempts.add(token) ||
            game.state.phase != DualFocusPhase.Result
        ) {
            return
        }
        cancelJobs()
        val finished = game.state
        val duration = finished.nowMillis
        val claimedNewRecord = finished.score > 0 && finished.score > record
        if (claimedNewRecord) record = finished.score
        publish(isNewRecord = false, durationMillis = duration, correctFeedbackLane = null)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val session =
                withContext(NonCancellable) {
                    activityRepository.recordCompletedSession(
                        game = GameId.DualFocus,
                        mode = GameModeId.DualFocusMatch,
                        variantId = null,
                        score = finished.score,
                        correctAnswers = finished.correctAnswers,
                        durationMillis = duration,
                        isNewRecord = claimedNewRecord,
                    )
                }
            if (token != attemptToken) return@launch
            record = maxOf(record, session.score)
            publish(isNewRecord = session.isNewRecord, durationMillis = duration)
        }
    }

    private fun elapsedMillis(): Long =
        attemptStartedAt?.elapsedNow()?.inWholeMilliseconds?.coerceAtLeast(0) ?: 0

    private fun publish(
        isRecordLoading: Boolean = _uiState.value.isRecordLoading,
        isNewRecord: Boolean = _uiState.value.isNewRecord,
        durationMillis: Long = _uiState.value.durationMillis,
        correctFeedbackLane: DualFocusLane? = _uiState.value.correctFeedbackLane,
    ) {
        _uiState.value =
            DualFocusUiState(
                game = game.state,
                record = record,
                previousRecord = previousRecord,
                durationMillis = durationMillis,
                isRecordLoading = isRecordLoading,
                isNewRecord = isNewRecord,
                correctFeedbackLane = correctFeedbackLane,
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
        const val TIMER_TICK_MILLIS = 50L
        const val CORRECT_FEEDBACK_MILLIS = 250L
    }
}
