package com.alad1nks.oquturbo.feature.stats.ui

import androidx.lifecycle.ViewModel
import com.alad1nks.oquturbo.feature.stats.data.StatsDataSource
import com.alad1nks.oquturbo.feature.stats.model.StatsDetailUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StatsViewModel(
    private val dataSource: StatsDataSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow(createState(StatsPeriod.SevenDays))
    val uiState = _uiState.asStateFlow()

    fun selectPeriod(period: StatsPeriod) {
        if (period != _uiState.value.period) {
            _uiState.value = createState(period)
        }
    }

    fun selectDay(dayId: Int) {
        if (_uiState.value.snapshot.activityDays.any { it.id == dayId }) {
            _uiState.value = _uiState.value.copy(selectedDayId = dayId)
        }
    }

    fun selectGame(game: StatsGame) {
        val trend = _uiState.value.snapshot.trends.firstOrNull { it.game == game } ?: return
        _uiState.value =
            _uiState.value.copy(
                selectedGame = game,
                selectedMode = trend.modes.firstOrNull()?.mode,
            )
    }

    fun selectMode(mode: StatsMode) {
        if (_uiState.value.selectedGameTrend?.modes?.any { it.mode == mode } == true) {
            _uiState.value = _uiState.value.copy(selectedMode = mode)
        }
    }

    private fun createState(period: StatsPeriod): StatsUiState {
        val snapshot = dataSource.getSnapshot(period)
        val firstGame = snapshot.trends.firstOrNull()
        return StatsUiState(
            period = period,
            snapshot = snapshot,
            selectedDayId = snapshot.activityDays.lastOrNull()?.id,
            selectedGame = firstGame?.game,
            selectedMode = firstGame?.modes?.firstOrNull()?.mode,
        )
    }
}

internal class StatsGameDetailViewModel(
    game: StatsGame,
    period: StatsPeriod,
    dataSource: StatsDataSource,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            StatsDetailUiState(
                game = game,
                period = period,
                modes = dataSource.gameTrend(game, period)?.modes.orEmpty(),
            ),
        )
    val uiState = _uiState.asStateFlow()
}

internal class StatsModeDetailViewModel(
    game: StatsGame,
    mode: StatsMode,
    period: StatsPeriod,
    dataSource: StatsDataSource,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            StatsDetailUiState(
                game = game,
                period = period,
                modes = dataSource.gameTrend(game, period)?.modes?.filter { it.mode == mode }.orEmpty(),
                selectedMode = mode,
            ),
        )
    val uiState = _uiState.asStateFlow()
}

private fun StatsDataSource.gameTrend(
    game: StatsGame,
    period: StatsPeriod,
) = getSnapshot(period).trends.firstOrNull { it.game == game }
