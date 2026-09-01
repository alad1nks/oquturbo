package com.alad1nks.oquturbo.feature.memorygrid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGame
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

internal class MemoryGridViewModel(
    val mode: MemoryGridGameMode,
    private val activityRepository: GameActivityRepository,
    private val game: MemoryGridGame = MemoryGridGame(mode = mode),
) : ViewModel() {
    private val _uiState = MutableStateFlow(game.state)
    val uiState = _uiState.asStateFlow()
    private var presentationJob: Job? = null
    private var startedAt = TimeSource.Monotonic.markNow()
    private var record = 0
    private var sessionRecorded = false
    private var completedWithNewRecord = false
    private var currentAttemptId = 0L

    init {
        viewModelScope.launch {
            record =
                activityRepository.observeRecords().first()
                    .filter { it.game == GameId.MemoryGrid && it.mode == mode.activityMode }
                    .maxOfOrNull { it.score } ?: 0
            publish()
        }
    }

    fun start() {
        presentationJob?.cancel()
        currentAttemptId++
        startedAt = TimeSource.Monotonic.markNow()
        sessionRecorded = false
        completedWithNewRecord = false
        game.start()
        publish()
        schedulePresentation()
    }

    fun selectCell(cellIndex: Int) {
        if (game.state.phase != MemoryGridPhase.AwaitingInput) return
        game.selectCell(cellIndex)
        publish()
        if (game.state.phase == MemoryGridPhase.GameOver) recordSession()
        if (game.state.phase == MemoryGridPhase.RoundSuccess) {
            presentationJob?.cancel()
            presentationJob =
                viewModelScope.launch {
                    delay(ROUND_SUCCESS_MILLIS)
                    game.continueAfterSuccess()
                    publish()
                    schedulePresentation()
                }
        }
    }

    private fun schedulePresentation() {
        presentationJob?.cancel()
        presentationJob =
            viewModelScope.launch {
                while (game.state.phase == MemoryGridPhase.ShowingSequence) {
                    delay(game.state.cellPresentationMillis)
                    game.advancePresentation()
                    publish()
                }
            }
    }

    private fun publish() {
        _uiState.value =
            game.state.copy(
                record = maxOf(record, game.state.score),
                isNewRecord = completedWithNewRecord,
            )
    }

    private fun recordSession() {
        if (sessionRecorded) return
        sessionRecorded = true
        val finishedState = game.state
        val isNewRecord = finishedState.score > record
        val completedAttemptId = currentAttemptId
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                withContext(NonCancellable) {
                    val recordedSession =
                        activityRepository.recordCompletedSession(
                            game = GameId.MemoryGrid,
                            mode = mode.activityMode,
                            score = finishedState.score,
                            correctAnswers = finishedState.correctCellCount,
                            durationMillis = startedAt.elapsedNow().inWholeMilliseconds,
                            isNewRecord = isNewRecord,
                        )
                    if (completedAttemptId == currentAttemptId && game.state.phase == MemoryGridPhase.GameOver) {
                        completedWithNewRecord = recordedSession.isNewRecord
                        if (recordedSession.isNewRecord) record = recordedSession.score
                        publish()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A failed activity write must not make the completed game unusable.
                // The acknowledgement remains false until a repository-confirmed record exists.
            }
        }
    }

    override fun onCleared() {
        presentationJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val ROUND_SUCCESS_MILLIS = 500L
    }
}

private val MemoryGridGameMode.activityMode: GameModeId
    get() =
        when (this) {
            MemoryGridGameMode.Route -> GameModeId.MemoryGridRoute
            MemoryGridGameMode.Reverse -> GameModeId.MemoryGridReverse
            MemoryGridGameMode.Flash -> GameModeId.MemoryGridFlash
        }
