package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailGame
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailPhase
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal enum class NumberTrailSaveStatus { None, Pending, Saved, Failed }

internal data class NumberTrailUiState(
    val game: NumberTrailState = NumberTrailState(),
    val record: Int = 0,
    val isRecordLoading: Boolean = true,
    val recordLoadFailed: Boolean = false,
    val isNewRecord: Boolean = false,
    val saveStatus: NumberTrailSaveStatus = NumberTrailSaveStatus.None,
)

internal class NumberTrailViewModel(
    private val activityRepository: GameActivityRepository,
    private val game: NumberTrailGame = NumberTrailGame(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NumberTrailUiState())
    val uiState = mutableState.asStateFlow()
    private var attempt = 0L
    private var liveAttempt = false
    private var foreground = true
    private var timerGeneration = 0L
    private var mark: TimeMark? = null
    private var elapsedRemainder: Duration = Duration.ZERO
    private var timer: Job? = null
    private var recordJob: Job? = null

    init {
        loadRecord()
    }

    fun loadRecord() {
        recordJob?.cancel()
        mutableState.value = mutableState.value.copy(isRecordLoading = true, recordLoadFailed = false)
        recordJob =
            viewModelScope.launch {
                try {
                    activityRepository.observeRecords().collect { records ->
                        val record =
                            records.filter {
                                it.game == GameId.NumberTrail &&
                                    it.mode == GameModeId.NumberTrailAscending && it.variantId == null
                            }.maxOfOrNull { it.score } ?: 0
                        mutableState.value =
                            mutableState.value.copy(
                                record = record,
                                isRecordLoading = false,
                                recordLoadFailed = false,
                            )
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    mutableState.value = mutableState.value.copy(isRecordLoading = false, recordLoadFailed = true)
                }
            }
    }

    fun start() {
        if (!foreground || uiState.value.isRecordLoading || uiState.value.recordLoadFailed) return
        if (game.state.phase != NumberTrailPhase.Ready && game.state.phase != NumberTrailPhase.Result) return
        cancelTimer()
        attempt++
        liveAttempt = true
        game.start()
        elapsedRemainder = Duration.ZERO
        mutableState.value = mutableState.value.copy(isNewRecord = false, saveStatus = NumberTrailSaveStatus.None)
        mark = timeSource.markNow()
        publish()
        scheduleTimer()
    }

    fun selectAnswer(boardId: Long, index: Int) {
        if (!liveAttempt || !foreground || game.state.phase != NumberTrailPhase.Active ||
            game.state.board?.id != boardId
        ) {
            return
        }
        game.answer(boardId, index, elapsed())
        if (game.state.phase != NumberTrailPhase.Active) elapsedRemainder = Duration.ZERO
        afterEvent()
    }

    fun pause() {
        if (!liveAttempt) return
        if (game.state.phase != NumberTrailPhase.Active && game.state.phase != NumberTrailPhase.BoardComplete) return
        game.pause(elapsed())
        cancelTimer()
        mark = null
        afterEvent()
    }

    fun setForeground(active: Boolean) {
        foreground = active
        if (!active) pause()
    }

    fun resume() {
        if (!foreground || !liveAttempt || game.state.phase != NumberTrailPhase.Paused) return
        game.resume()
        mark = timeSource.markNow()
        publish()
        scheduleTimer()
    }

    fun abandon() {
        attempt++
        liveAttempt = false
        cancelTimer()
        mark = null
    }

    private fun elapsed(): Long {
        val total = (mark?.elapsedNow() ?: Duration.ZERO) + elapsedRemainder
        val millis = total.inWholeMilliseconds.coerceAtLeast(0)
        elapsedRemainder = total - millis.milliseconds
        mark = timeSource.markNow()
        return millis
    }

    private fun afterEvent() {
        if (game.state.phase == NumberTrailPhase.Result) {
            completeAttempt()
        } else {
            publish()
            if (game.state.phase == NumberTrailPhase.BoardComplete) scheduleTimer()
        }
    }

    private fun scheduleTimer() {
        cancelTimer()
        val callback = timerTickCallback()
        timer =
            viewModelScope.launch {
                while (liveAttempt && isTimedPhase()) {
                    delay(50)
                    callback()
                }
            }
    }

    internal fun timerTickCallback(): () -> Unit {
        val token = attempt
        val generation = timerGeneration
        return tick@{
            if (token != attempt || generation != timerGeneration || !liveAttempt || !foreground) return@tick
            if (!isTimedPhase()) return@tick
            val previousPhase = game.state.phase
            game.elapse(elapsed())
            if (game.state.phase != previousPhase) elapsedRemainder = Duration.ZERO
            if (game.state.phase == NumberTrailPhase.Result) completeAttempt() else publish()
        }
    }

    private fun completeAttempt() {
        if (!liveAttempt || game.state.phase != NumberTrailPhase.Result) return
        liveAttempt = false
        cancelTimer()
        mark = null
        val token = attempt
        val finished = game.state
        val claimed = finished.score > 0 && finished.score > uiState.value.record
        mutableState.value =
            mutableState.value.copy(
                game = finished,
                saveStatus = NumberTrailSaveStatus.Pending,
                isNewRecord = false,
            )
        // Enter the repository's non-cancellable write before a retry or disposal can cancel the ViewModel.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val session =
                    activityRepository.recordCompletedSession(
                        game = GameId.NumberTrail,
                        mode = GameModeId.NumberTrailAscending,
                        variantId = null,
                        score = finished.score,
                        correctAnswers = finished.correctAnswers,
                        durationMillis = finished.activeDurationMillis,
                        isNewRecord = claimed,
                    )
                if (token == attempt) {
                    mutableState.value =
                        mutableState.value.copy(
                            record = maxOf(uiState.value.record, session.score),
                            saveStatus = NumberTrailSaveStatus.Saved,
                            isNewRecord = session.isNewRecord,
                        )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (token == attempt) {
                    mutableState.value =
                        mutableState.value.copy(
                            saveStatus = NumberTrailSaveStatus.Failed,
                            isNewRecord = false,
                        )
                }
            }
        }
    }

    private fun isTimedPhase(): Boolean =
        game.state.phase == NumberTrailPhase.Active || game.state.phase == NumberTrailPhase.BoardComplete

    private fun publish() {
        mutableState.value = mutableState.value.copy(game = game.state)
    }

    private fun cancelTimer() {
        timerGeneration++
        timer?.cancel()
        timer = null
    }

    override fun onCleared() {
        abandon()
        super.onCleared()
    }
}
