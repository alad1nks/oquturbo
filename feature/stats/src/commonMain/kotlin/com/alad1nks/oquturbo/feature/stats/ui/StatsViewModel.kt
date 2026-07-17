package com.alad1nks.oquturbo.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.feature.stats.data.StatsDataSource
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsDetailUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
internal class StatsViewModel(
    private val dataSource: StatsDataSource,
) : ViewModel() {
    private val selection = MutableStateFlow(StatsSelection())

    val uiState =
        selection
            .flatMapLatest { selection ->
                dataSource.observeSnapshot(selection.period).map { snapshot ->
                    createState(selection, snapshot)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StatsUiState(),
            )

    fun selectPeriod(period: StatsPeriod) {
        if (period != selection.value.period) {
            selection.value = StatsSelection(period = period)
        }
    }

    fun selectDay(dayId: Int) {
        if (uiState.value.snapshot.activityDays.any { it.id == dayId }) {
            selection.update { it.copy(selectedDayId = dayId) }
        }
    }

    fun selectGame(game: StatsGame) {
        val trend = uiState.value.snapshot.trends.firstOrNull { it.game == game } ?: return
        selection.update {
            it.copy(
                selectedGame = game,
                selectedMode = trend.modes.firstOrNull()?.mode,
                selectedVariantId = trend.modes.firstOrNull()?.variantId,
            )
        }
    }

    fun selectMode(trend: ModeTrend) {
        if (uiState.value.selectedGameTrend?.modes?.any { it.sameSeries(trend) } == true) {
            selection.update {
                it.copy(
                    selectedMode = trend.mode,
                    selectedVariantId = trend.variantId,
                )
            }
        }
    }

    private fun createState(
        selection: StatsSelection,
        snapshot: StatsPeriodSnapshot,
    ): StatsUiState {
        val selectedGameTrend =
            snapshot.trends.firstOrNull { it.game == selection.selectedGame }
                ?: snapshot.trends.firstOrNull()
        val selectedTrend =
            selectedGameTrend?.modes?.firstOrNull {
                it.mode == selection.selectedMode && it.variantId == selection.selectedVariantId
            } ?: selectedGameTrend?.modes?.firstOrNull()
        return StatsUiState(
            period = selection.period,
            snapshot = snapshot,
            selectedDayId =
                selection.selectedDayId?.takeIf { id -> snapshot.activityDays.any { it.id == id } }
                    ?: snapshot.activityDays.lastOrNull()?.id,
            selectedGame = selectedGameTrend?.game,
            selectedMode = selectedTrend?.mode,
            selectedVariantId = selectedTrend?.variantId,
        )
    }
}

internal class StatsGameDetailViewModel(
    game: StatsGame,
    period: StatsPeriod,
    dataSource: StatsDataSource,
) : ViewModel() {
    val uiState =
        dataSource
            .observeSnapshot(period)
            .map { snapshot ->
                StatsDetailUiState(
                    game = game,
                    period = period,
                    modes = snapshot.gameTrend(game)?.modes.orEmpty(),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StatsDetailUiState(game = game, period = period, modes = emptyList()),
            )
}

internal class StatsModeDetailViewModel(
    game: StatsGame,
    mode: StatsMode,
    period: StatsPeriod,
    dataSource: StatsDataSource,
) : ViewModel() {
    val uiState =
        dataSource
            .observeSnapshot(period)
            .map { snapshot ->
                StatsDetailUiState(
                    game = game,
                    period = period,
                    modes = snapshot.gameTrend(game)?.modes?.filter { it.mode == mode }.orEmpty(),
                    selectedMode = mode,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    StatsDetailUiState(
                        game = game,
                        period = period,
                        modes = emptyList(),
                        selectedMode = mode,
                    ),
            )
}

private data class StatsSelection(
    val period: StatsPeriod = StatsPeriod.SevenDays,
    val selectedDayId: Int? = null,
    val selectedGame: StatsGame? = null,
    val selectedMode: StatsMode? = null,
    val selectedVariantId: String? = null,
)

private fun ModeTrend.sameSeries(other: ModeTrend): Boolean =
    mode == other.mode && variantId == other.variantId

private fun StatsPeriodSnapshot.gameTrend(game: StatsGame) =
    trends.firstOrNull { it.game == game }
