package com.alad1nks.oquturbo.feature.memorygrid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGame
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MemoryGridViewModel(
    val mode: MemoryGridGameMode,
    private val game: MemoryGridGame = MemoryGridGame(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(game.state)
    val uiState = _uiState.asStateFlow()
    private var presentationJob: Job? = null

    fun start() {
        presentationJob?.cancel()
        game.start()
        publish()
        schedulePresentation()
    }

    fun selectCell(cellIndex: Int) {
        if (game.state.phase != MemoryGridPhase.AwaitingInput) return
        game.selectCell(cellIndex)
        publish()
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
        _uiState.value = game.state
    }

    override fun onCleared() {
        presentationJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val ROUND_SUCCESS_MILLIS = 500L
    }
}
