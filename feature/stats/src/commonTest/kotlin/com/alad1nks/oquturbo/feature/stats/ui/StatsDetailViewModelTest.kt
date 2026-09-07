package com.alad1nks.oquturbo.feature.stats.ui

import com.alad1nks.oquturbo.feature.stats.data.StatsDataSource
import com.alad1nks.oquturbo.feature.stats.model.GameTrend
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StatsDetailViewModelTest {
    @AfterTest
    fun resetDispatcher() = Dispatchers.resetMain()

    @Test
    fun bothDetailsInitializeFromEveryRoutePeriod() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            StatsPeriod.entries.forEach { period ->
                val source = ControlledStatsSource()
                val game = StatsGameDetailViewModel(StatsGame.NumberSprint, period, source)
                val mode = StatsModeDetailViewModel(StatsGame.NumberSprint, StatsMode.Custom, period, source)
                assertEquals(period, game.uiState.value.period)
                assertEquals(period, mode.uiState.value.period)
                assertEquals(StatsMode.Custom, mode.uiState.value.selectedMode)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { game.uiState.collect {} }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { mode.uiState.collect {} }
                runCurrent()
                assertEquals(listOf(period, period), source.requests)
            }
        }

    @Test
    fun gameSwitchesAtomicallyCancelsPendingRequestsAndIgnoresLateOldResults() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val source = ControlledStatsSource()
            val vm = StatsGameDetailViewModel(StatsGame.NumberSprint, StatsPeriod.SevenDays, source)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            runCurrent()
            val original = listOf(trend(StatsMode.Classic))
            source.emit(StatsPeriod.SevenDays, snapshot(StatsGame.NumberSprint, original))
            runCurrent()
            vm.selectPeriod(StatsPeriod.ThirtyDays)
            runCurrent()
            assertEquals(StatsPeriod.SevenDays, vm.uiState.value.period)
            assertEquals(original, vm.uiState.value.modes)
            vm.selectPeriod(StatsPeriod.SevenDays)
            runCurrent()
            source.emit(StatsPeriod.ThirtyDays, StatsPeriodSnapshot.Empty)
            runCurrent()
            assertEquals(original, vm.uiState.value.modes)
            vm.selectPeriod(StatsPeriod.ThirtyDays)
            runCurrent()
            vm.selectPeriod(StatsPeriod.AllTime)
            runCurrent()
            val latest = listOf(trend(StatsMode.Binary))
            source.emit(StatsPeriod.AllTime, snapshot(StatsGame.NumberSprint, latest))
            runCurrent()
            source.emit(StatsPeriod.SevenDays, StatsPeriodSnapshot.Empty)
            source.emit(StatsPeriod.ThirtyDays, StatsPeriodSnapshot.Empty)
            runCurrent()
            assertEquals(StatsPeriod.AllTime, vm.uiState.value.period)
            assertEquals(latest, vm.uiState.value.modes)
            val requests = source.requests.toList()
            vm.selectPeriod(StatsPeriod.AllTime)
            runCurrent()
            assertEquals(requests, source.requests)
            source.emit(StatsPeriod.AllTime, snapshot(StatsGame.WordFlow, listOf(trend(StatsMode.Context))))
            runCurrent()
            assertEquals(emptyList(), vm.uiState.value.modes)
            source.emit(StatsPeriod.AllTime, snapshot(StatsGame.NumberSprint, latest))
            runCurrent()
            assertEquals(latest, vm.uiState.value.modes)
        }

    @Test
    fun modeKeepsAllCustomAndLanguageVariantsAndSelectionIsLocal() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            listOf(
                Triple(StatsGame.NumberSprint, StatsMode.Custom, listOf("length:3;digits:012", "length:4;digits:345")),
                Triple(StatsGame.WordFlow, StatsMode.Context, listOf("en", "ru", "kk")),
            ).forEach { (game, mode, variants) ->
                val source = ControlledStatsSource()
                val parent = StatsGameDetailViewModel(game, StatsPeriod.SevenDays, source)
                val vm = StatsModeDetailViewModel(game, mode, StatsPeriod.SevenDays, source)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { parent.uiState.collect {} }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
                runCurrent()
                vm.selectPeriod(StatsPeriod.ThirtyDays)
                runCurrent()
                val expected = variants.map { trend(mode, it) }
                source.emit(StatsPeriod.ThirtyDays, snapshot(game, expected + trend(StatsMode.Binary)))
                runCurrent()
                assertEquals(expected, vm.uiState.value.modes)
                assertEquals(StatsPeriod.ThirtyDays, vm.uiState.value.period)
                assertEquals(StatsPeriod.SevenDays, parent.uiState.value.period)
                vm.selectPeriod(StatsPeriod.AllTime)
                runCurrent()
                assertEquals(expected, vm.uiState.value.modes)
                source.emit(StatsPeriod.AllTime, StatsPeriodSnapshot.Empty)
                runCurrent()
                source.emit(StatsPeriod.ThirtyDays, snapshot(game, expected))
                runCurrent()
                assertEquals(emptyList(), vm.uiState.value.modes)
                assertEquals(mode, vm.uiState.value.selectedMode)
                assertEquals(StatsPeriod.AllTime, vm.uiState.value.period)
                source.emit(StatsPeriod.AllTime, snapshot(game, expected))
                runCurrent()
                assertEquals(expected, vm.uiState.value.modes)
            }
        }

    private fun trend(mode: StatsMode, variant: String? = null) =
        ModeTrend(mode, variant, listOf(2, 4), 4, 4, 3, 2)

    private fun snapshot(game: StatsGame, modes: List<ModeTrend>) =
        StatsPeriodSnapshot.Empty.copy(
            trends = listOf(GameTrend(StatsGame.WideEye, listOf(trend(StatsMode.Characters))), GameTrend(game, modes)),
        )

    private class ControlledStatsSource : StatsDataSource {
        val requests = mutableListOf<StatsPeriod>()
        private val streams = StatsPeriod.entries.associateWith { MutableSharedFlow<StatsPeriodSnapshot>() }

        override fun observeSnapshot(period: StatsPeriod): MutableSharedFlow<StatsPeriodSnapshot> {
            requests += period
            return streams.getValue(period)
        }

        suspend fun emit(period: StatsPeriod, snapshot: StatsPeriodSnapshot) = streams.getValue(period).emit(snapshot)
    }
}
