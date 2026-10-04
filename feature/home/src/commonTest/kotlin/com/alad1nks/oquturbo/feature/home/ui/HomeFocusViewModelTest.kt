package com.alad1nks.oquturbo.feature.home.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusPhase
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class HomeFocusViewModelTest {
    @Test
    fun storedPlanIsUsableWhileIndependentFocusReadIsDelayedThenFails() =
        exercise {
            val plan = training.ensureTodayTraining()
            val bytes = preferences.values[PLAN]!!.value
            preferences.focusRead = CompletableDeferred()
            runCurrent()
            assertEquals(HomeFocusState.Loading, vm.uiState.value.focus)
            assertEquals(plan.toHomeDailyTraining(), vm.uiState.value.dailyTraining)
            preferences.failFocusRead = true
            preferences.focusRead!!.complete(Unit)
            runCurrent()
            assertEquals(HomeFocusState.Error, vm.uiState.value.focus)
            assertFalse(vm.uiState.value.trainingLoadFailed)
            var launched = false
            vm.startTraining {
                assertEquals(plan.nextEntry, it)
                launched = true
            }
            runCurrent()
            assertTrue(launched)
            assertEquals(bytes, preferences.values[PLAN]!!.value)
        }

    @Test
    fun missingPlanWithCorruptFocusShowsTrainingErrorAndExplicitResetAllowsNormalRetry() =
        exercise {
            preferences.seed(FOCUS, """{"version":2,"selection":null}""")
            runCurrent()
            assertEquals(HomeFocusState.Error, vm.uiState.value.focus)
            assertTrue(vm.uiState.value.trainingLoadFailed)
            assertEquals(null, vm.uiState.value.dailyTraining)
            assertEquals(null, preferences.values[PLAN]?.value)
            assertIs<PracticeRhythmState.Ready>(vm.uiState.value.practiceRhythm)
            assertIs<PersonalResultState.Loaded>(vm.uiState.value.personalResult)
            training.resetWeeklyFocus()
            vm.refreshHome()
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
            assertEquals(3, vm.uiState.value.dailyTraining!!.totalCount)
            assertEquals(
                WeeklyFocusPhase.Off,
                assertIs<HomeFocusState.Ready>(vm.uiState.value.focus).let {
                    it.focus.phaseOn(it.todayEpochDay)
                },
            )
        }

    @Test
    fun selectingAndDisablingSettingNeverReordersExistingPartialPlanOrGivesCredit() =
        exercise {
            var plan = training.ensureTodayTraining()
            plan = training.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE)
            runCurrent()
            val bytes = preferences.values[PLAN]!!.value
            training.selectWeeklyFocus()
            runCurrent()
            val ready = assertIs<HomeFocusState.Ready>(vm.uiState.value.focus)
            assertEquals(WeeklyFocusPhase.Scheduled, ready.focus.phaseOn(ready.todayEpochDay))
            assertEquals(plan.toHomeDailyTraining(), vm.uiState.value.dailyTraining)
            assertEquals(bytes, preferences.values[PLAN]!!.value)
            training.disableWeeklyFocus()
            runCurrent()
            assertEquals(bytes, preferences.values[PLAN]!!.value)
            assertEquals(1, vm.uiState.value.dailyTraining!!.completedCount)
            assertEquals(1, vm.uiState.value.overallLevel)
            assertTrue(vm.uiState.value.recentRecords.isEmpty())
        }

    @Test
    fun midnightAndFreshSubscriptionNeverReuseStaleFocusState() =
        exercise {
            training.selectWeeklyFocus()
            runCurrent()
            for ((day, phase) in listOf(
                101L to WeeklyFocusPhase.Active,
                108L to WeeklyFocusPhase.Expired,
                99L to WeeklyFocusPhase.Scheduled,
            )) {
                clock.day = day
                scope.advanceTimeBy(60_001)
                runCurrent()
                val ready = assertIs<HomeFocusState.Ready>(vm.uiState.value.focus)
                assertEquals(day, ready.todayEpochDay)
                assertEquals(phase, ready.focus.phaseOn(day))
            }
            collector.cancel()
            scope.advanceTimeBy(5_001)
            runCurrent()
            preferences.focusRead = CompletableDeferred()
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
            vm.refreshHome()
            runCurrent()
            assertEquals(HomeFocusState.Loading, vm.uiState.value.focus)
            preferences.focusRead!!.complete(Unit)
            runCurrent()
            assertIs<HomeFocusState.Ready>(vm.uiState.value.focus)
            assertEquals(1, preferences.focusWrites)
        }

    private fun exercise(block: suspend Fixture.() -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = Fixture(this)
            try {
                fixture.block()
            } finally {
                fixture.vm.viewModelScope.cancel()
                backgroundScope.cancel()
                fixture.app.close()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private class Fixture(val scope: TestScope) {
        val preferences = Preferences()
        val app = koinApplication { modules(StorageCommonModule, module { single<AppPreferences> { preferences } }) }
        val storage = app.koin.get<Storage>()
        val clock = TestClock()
        val training = DailyTrainingRepository(storage, clock)
        val vm = HomeViewModel(GameActivityRepository(storage, clock), training, clock)
        var collector = scope.backgroundScope.launch { vm.uiState.collect {} }

        fun runCurrent() = scope.runCurrent()
    }

    private class TestClock(var day: Long = 100) : Clock {
        override fun now() = Instant.fromEpochMilliseconds(day * 86_400_000)
    }

    private class Preferences : AppPreferences {
        val values = mutableMapOf<String, MutableStateFlow<String?>>()
        var focusRead: CompletableDeferred<Unit>? = null
        var failFocusRead = false
        var focusWrites = 0

        fun seed(key: String, value: String) {
            values.getOrPut(key) { MutableStateFlow(null) }.value = value
        }

        override fun getString(key: String): Flow<String?> =
            flow {
                if (key == FOCUS) {
                    focusRead?.await()
                    check(!failFocusRead) { "focus read failed" }
                }
                emitAll(values.getOrPut(key) { MutableStateFlow(null) })
            }

        override suspend fun setString(key: String, value: String) {
            if (key == FOCUS)focusWrites++
            seed(key, value)
        }

        override fun getInt(key: String): Flow<Int?> = flowOf(null)

        override fun getBoolean(key: String): Flow<Boolean?> = flowOf(null)

        override suspend fun setInt(key: String, value: Int) = Unit

        override suspend fun setBoolean(key: String, value: Boolean) = Unit
    }

    private companion object {
        const val FOCUS = "weekly_focus_v1"
        const val PLAN = "daily_training_v1"
    }
}
