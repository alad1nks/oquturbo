package com.alad1nks.oquturbo.feature.rotationmatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchAnswer
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchGame
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchPhase
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchState
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

internal data class RotationMatchUiState(
    val game: RotationMatchState = RotationMatchState(),
    val record: Int = 0,
    val previousRecord: Int = 0,
    val isRecordLoading: Boolean = true,
    val isNewRecord: Boolean = false,
    val completedDurationMillis: Long? = null,
)

internal class RotationMatchViewModel(
    private val activityRepository: GameActivityRepository,
    private val game: RotationMatchGame = RotationMatchGame(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RotationMatchUiState())
    val uiState = _uiState.asStateFlow()

    private var record = 0
    private var previousRecord = 0
    private var attemptToken = 0L
    private var attemptStartedAt: TimeMark? = null
    private var roundTimerMark: TimeMark? = null
    private var timerJob: Job? = null
    private var feedbackJob: Job? = null
    private val recordedAttempts = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            activityRepository.observeRecords().collect { records ->
                record =
                    records.filter {
                        it.game == GameId.RotationMatch &&
                            it.mode == GameModeId.RotationMatchRotation &&
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
        game.start()
        attemptStartedAt = timeSource.markNow()
        roundTimerMark = timeSource.markNow()
        publish(isNewRecord = false, completedDurationMillis = null)
        scheduleTimer(attemptToken, requireNotNull(game.state.round).id)
    }

    fun selectAnswer(
        roundId: Long,
        answer: RotationMatchAnswer,
    ) {
        val displayedRound = game.state.round
        if (game.state.phase != RotationMatchPhase.Active || displayedRound?.id != roundId) return
        val token = attemptToken
        val elapsed = roundTimerMark?.elapsedNow()?.inWholeMilliseconds?.coerceAtLeast(0) ?: return
        roundTimerMark = timeSource.markNow()
        game.answer(answer, elapsed)
        if (game.state.phase == RotationMatchPhase.Active) {
            publish()
            return
        }
        timerJob?.cancel()
        timerJob = null
        roundTimerMark = null
        when (game.state.phase) {
            RotationMatchPhase.CorrectFeedback -> {
                publish()
                feedbackJob =
                    viewModelScope.launch {
                        delay(CORRECT_FEEDBACK_MILLIS)
                        if (
                            token != attemptToken ||
                            game.state.phase != RotationMatchPhase.CorrectFeedback ||
                            game.state.round?.id != roundId
                        ) {
                            return@launch
                        }
                        game.continueAfterCorrect()
                        roundTimerMark = timeSource.markNow()
                        publish()
                        scheduleTimer(token, requireNotNull(game.state.round).id)
                    }
            }
            RotationMatchPhase.Result -> completeAttempt(token)
            else -> publish()
        }
    }

    fun abandon() {
        attemptToken++
        cancelJobs()
        attemptStartedAt = null
        roundTimerMark = null
    }

    internal fun advanceTimerBy(millis: Long) {
        val token = attemptToken
        if (game.state.phase != RotationMatchPhase.Active) return
        game.elapse(millis)
        if (game.state.phase == RotationMatchPhase.Result) {
            roundTimerMark = null
            completeAttempt(token)
        } else {
            publish()
        }
    }

    private fun scheduleTimer(
        token: Long,
        roundId: Long,
    ) {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (
                    token == attemptToken &&
                    game.state.phase == RotationMatchPhase.Active &&
                    game.state.round?.id == roundId
                ) {
                    delay(TIMER_TICK_MILLIS)
                    if (
                        token != attemptToken ||
                        game.state.phase != RotationMatchPhase.Active ||
                        game.state.round?.id != roundId
                    ) {
                        return@launch
                    }
                    val mark = roundTimerMark ?: return@launch
                    val elapsed = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
                    if (elapsed > 0) {
                        roundTimerMark = timeSource.markNow()
                        advanceTimerBy(elapsed)
                    }
                }
            }
    }

    private fun completeAttempt(token: Long) {
        if (token != attemptToken || !recordedAttempts.add(token) || game.state.phase != RotationMatchPhase.Result) {
            return
        }
        val finished = game.state
        cancelJobs()
        roundTimerMark = null
        val duration = attemptStartedAt?.elapsedNow()?.inWholeMilliseconds?.coerceAtLeast(0) ?: 0
        attemptStartedAt = null
        val claimedNewRecord = finished.score > 0 && finished.score > record
        if (claimedNewRecord) record = finished.score
        publish(isNewRecord = false, completedDurationMillis = duration)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val session =
                withContext(NonCancellable) {
                    activityRepository.recordCompletedSession(
                        game = GameId.RotationMatch,
                        mode = GameModeId.RotationMatchRotation,
                        variantId = null,
                        score = finished.score,
                        correctAnswers = finished.correctAnswers,
                        durationMillis = duration,
                        isNewRecord = claimedNewRecord,
                    )
                }
            if (token != attemptToken) return@launch
            record = maxOf(record, session.score)
            publish(isNewRecord = session.isNewRecord, completedDurationMillis = duration)
        }
    }

    private fun publish(
        isRecordLoading: Boolean = _uiState.value.isRecordLoading,
        isNewRecord: Boolean = _uiState.value.isNewRecord,
        completedDurationMillis: Long? = _uiState.value.completedDurationMillis,
    ) {
        _uiState.value =
            RotationMatchUiState(
                game = game.state,
                record = record,
                previousRecord = previousRecord,
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
        const val CORRECT_FEEDBACK_MILLIS = 350L
    }
}
