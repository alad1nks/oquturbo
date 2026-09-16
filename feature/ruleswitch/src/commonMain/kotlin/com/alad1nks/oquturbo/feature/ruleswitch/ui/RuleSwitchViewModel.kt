package com.alad1nks.oquturbo.feature.ruleswitch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchGame
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchPhase
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchState
import com.alad1nks.oquturbo.feature.ruleswitch.model.SwitchAnswer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal enum class RuleSwitchSaveStatus { None, Pending, Saved, Failed }

internal data class RuleSwitchUiState(
    val game: RuleSwitchState = RuleSwitchState(),
    val record: Int = 0,
    val isForeground: Boolean = true,
    val isRecordLoading: Boolean = true,
    val recordLoadFailed: Boolean = false,
    val isNewRecord: Boolean = false,
    val saveStatus: RuleSwitchSaveStatus = RuleSwitchSaveStatus.None,
)

@OptIn(ExperimentalTime::class)
internal class RuleSwitchViewModel(
    private val activityRepository: GameActivityRepository,
    private val game: RuleSwitchGame = RuleSwitchGame(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RuleSwitchUiState())
    val uiState = mutableState.asStateFlow()
    private var completedAtEpochMillis = 0L
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
                                it.game == GameId.RuleSwitch &&
                                    it.mode == GameModeId.RuleSwitchSwitch && it.variantId == null
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
        if (game.state.phase != RuleSwitchPhase.Ready && game.state.phase != RuleSwitchPhase.Result) return
        if (game.state.phase == RuleSwitchPhase.Result && uiState.value.saveStatus != RuleSwitchSaveStatus.Saved) {
            return
        }
        cancelTimer()
        attempt++
        liveAttempt = true
        game.start()
        elapsedRemainder = Duration.ZERO
        mutableState.value = mutableState.value.copy(isNewRecord = false, saveStatus = RuleSwitchSaveStatus.None)
        mark = timeSource.markNow()
        publish()
        scheduleTimer()
    }

    fun selectAnswer(boardId: Long, value: SwitchAnswer) {
        if (!liveAttempt || !foreground || game.state.phase != RuleSwitchPhase.Active ||
            (game.state.board?.id != boardId || value !in game.state.board!!.options)
        ) {
            return
        }
        game.answer(boardId, value, elapsed())
        if (game.state.phase != RuleSwitchPhase.Active) elapsedRemainder = Duration.ZERO
        afterEvent()
    }

    fun pause() {
        if (!liveAttempt) return
        if (game.state.phase != RuleSwitchPhase.Active && game.state.phase != RuleSwitchPhase.Correct) return
        game.pause(elapsed())
        cancelTimer()
        mark = null
        afterEvent()
    }

    fun setForeground(active: Boolean) {
        foreground = active
        mutableState.value = mutableState.value.copy(isForeground = active)
        if (!active) pause()
    }

    fun resume() {
        if (!foreground || !liveAttempt || game.state.phase != RuleSwitchPhase.Paused) return
        game.resume()
        mark = timeSource.markNow()
        publish()
        scheduleTimer()
    }

    fun abandon() {
        if (game.state.phase == RuleSwitchPhase.Result && uiState.value.saveStatus != RuleSwitchSaveStatus.Saved) return
        game.abandon()
        publish()
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
        if (game.state.phase == RuleSwitchPhase.Result) {
            completeAttempt()
        } else {
            publish()
            if (game.state.phase == RuleSwitchPhase.Correct) scheduleTimer()
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
            if (game.state.phase == RuleSwitchPhase.Result) {
                completeAttempt()
            } else {
                publish()
                if (game.state.phase != previousPhase) scheduleTimer()
            }
        }
    }

    private fun completeAttempt() {
        if (!liveAttempt || game.state.phase != RuleSwitchPhase.Result) return
        liveAttempt = false
        cancelTimer()
        mark = null
        completedAtEpochMillis = Clock.System.now().toEpochMilliseconds()
        saveCompletion()
    }

    fun retrySave() {
        if (game.state.phase != RuleSwitchPhase.Result || uiState.value.saveStatus != RuleSwitchSaveStatus.Failed) {
            return
        }
        saveCompletion()
    }

    private fun saveCompletion() {
        val token = attempt
        val finished = game.state
        val claimed = finished.score > 0 && finished.score > uiState.value.record
        mutableState.value =
            mutableState.value.copy(
                game = finished,
                saveStatus = RuleSwitchSaveStatus.Pending,
                isNewRecord = false,
            )
        // Enter the repository's non-cancellable write before a retry or disposal can cancel the ViewModel.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val session =
                    activityRepository.recordCompletedSession(
                        game = GameId.RuleSwitch,
                        mode = GameModeId.RuleSwitchSwitch,
                        variantId = null,
                        score = finished.score,
                        correctAnswers = finished.correctAnswers,
                        durationMillis = finished.activeDurationMillis,
                        isNewRecord = claimed,
                        completedAtEpochMillis = completedAtEpochMillis,
                    )
                if (token == attempt) {
                    mutableState.value =
                        mutableState.value.copy(
                            record = maxOf(uiState.value.record, session.score),
                            saveStatus = RuleSwitchSaveStatus.Saved,
                            isNewRecord = session.isNewRecord,
                        )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (token == attempt) {
                    mutableState.value =
                        mutableState.value.copy(
                            saveStatus = RuleSwitchSaveStatus.Failed,
                            isNewRecord = false,
                        )
                }
            }
        }
    }

    private fun isTimedPhase(): Boolean =
        game.state.phase == RuleSwitchPhase.Active || game.state.phase == RuleSwitchPhase.Correct

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
