package com.alad1nks.oquturbo.feature.stats.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.feature.stats.data.RepositoryWeeklyReviewDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Real repositories and StorageImpl; only the platform preferences backend is in memory. */
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class WeeklyReviewRepositoryTest {
    @Test
    fun firstOpenInitializesOnlyAbsentMetadataOnceWithoutCreatingTrainingPlanOrActivity() =
        exercise {
            runCurrent()
            assertEquals(setOf(ACTIVITY, TRAINING), preferences.writes.keys)
            assertTrue(preferences.writes.values.all { it == 1 })
            assertNull(preferences.strings[PLAN]?.value)
            val state = vm.uiState.value
            assertEquals(100L, state.practice.readyValue().trackingStartedEpochDay)
            assertEquals(100L, state.training.readyValue().trackingStartedEpochDay)
            assertTrue(state.practice.readyValue().hasUnknownDays)
            assertEquals(0, state.practice.readyValue().completedDaysInWindow)
            assertEquals(0, state.training.readyValue().completedDaysInWindow)
            assertIs<ProgressComparison.NoRecentSessions>(state.comparison.readyValue())
            val bytes = preferences.snapshot()
            repeat(2) {
                vm.refreshReview()
                runCurrent()
            }
            assertEquals(bytes, preferences.snapshot())
            assertTrue(preferences.writes.values.all { it == 1 })
        }

    @Test
    fun durableTrainingReceiptDoesNotCreatePracticeAndZeroSessionDoesNotCompleteTraining() =
        exercise {
            preferences.seed(ACTIVITY, history("practiceHistory", 80, ""))
            preferences.seed(TRAINING, history("completionHistory", 90, "98,99"))
            runCurrent()
            assertEquals(0, vm.uiState.value.practice.readyValue().completedDaysInWindow)
            assertEquals(2, vm.uiState.value.training.readyValue().completedDaysInWindow)
            assertTrue(preferences.writes.isEmpty())
            val trainingBytes = preferences.strings[TRAINING]!!.value
            repeat(2) {
                activity.recordCompletedSession(
                    GameId.WordFlow,
                    GameModeId.WordFlowContext,
                    "ru",
                    score = 0,
                    durationMillis = 0,
                    isNewRecord = false,
                )
                runCurrent()
                assertEquals(1, vm.uiState.value.practice.readyValue().completedDaysInWindow)
                assertEquals(2, vm.uiState.value.training.readyValue().completedDaysInWindow)
                assertEquals(
                    it + 1,
                    assertIs<ProgressComparison.InsufficientData>(
                        vm.uiState.value.comparison.readyValue(),
                    ).availableCount,
                )
            }
            assertEquals(trainingBytes, preferences.strings[TRAINING]!!.value)
            assertNull(preferences.strings[PLAN]?.value)
        }

    @Test
    fun realCompletionReceiptRefreshesOpenReviewWithoutAddingPracticeAndReadRefreshIsIdempotent() =
        exercise {
            runCurrent()
            val plan = training.ensureTodayTraining()
            plan.entries.forEach { entry ->
                training.completeEntry(entry.id, entry.requiredScore)
            }
            runCurrent()
            assertEquals(1, vm.uiState.value.training.readyValue().completedDaysInWindow)
            assertEquals(0, vm.uiState.value.practice.readyValue().completedDaysInWindow)
            assertIs<ProgressComparison.NoRecentSessions>(vm.uiState.value.comparison.readyValue())
            val bytes = preferences.snapshot()
            val writes = preferences.writes.toMap()
            vm.refreshReview()
            runCurrent()
            assertEquals(bytes, preferences.snapshot())
            assertEquals(writes, preferences.writes)
            assertEquals(1, vm.uiState.value.training.readyValue().completedDaysInWindow)
        }

    @Test
    fun malformedPracticeMetadataDoesNotHideValidJournalAndTrainingRemainsIndependent() =
        exercise {
            preferences.seed(
                ACTIVITY,
                """{"practiceHistory":null,
                "sessions":[{"game":"NumberSprint",
                "mode":"NumberSprintBinary",
                "score":0,
                "correctAnswers":0,
                "durationMillis":0,
                "completedAtEpochMillis":8640000000,
                "completedEpochDay":100,
                "isNewRecord":false}]}""",
            )
            preferences.seed(TRAINING, history("completionHistory", 80, "100"))
            val bytes = preferences.snapshot()
            runCurrent()
            assertEquals(WeeklySource.Error, vm.uiState.value.practice)
            assertEquals(1, vm.uiState.value.training.readyValue().completedDaysInWindow)
            assertEquals(
                1,
                assertIs<ProgressComparison.InsufficientData>(vm.uiState.value.comparison.readyValue()).availableCount,
            )
            vm.retryPractice()
            runCurrent()
            assertEquals(WeeklySource.Error, vm.uiState.value.practice)
            assertEquals(bytes, preferences.snapshot())
            assertTrue(preferences.writes.isEmpty())
        }

    @Test
    fun failedTrainingInitializationIsUnavailableAndRetryWritesNoReceiptOrPlan() =
        exercise {
            preferences.failWriteKey = TRAINING
            runCurrent()
            assertEquals(WeeklySource.Error, vm.uiState.value.training)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.practice)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.comparison)
            assertNull(preferences.strings[TRAINING]?.value)
            preferences.failWriteKey = null
            vm.retryTraining()
            vm.retryTraining()
            assertEquals(WeeklySource.Loading, vm.uiState.value.training)
            runCurrent()
            assertEquals(0, vm.uiState.value.training.readyValue().completedDaysInWindow)
            assertEquals(1, preferences.writes[TRAINING])
            assertNull(preferences.strings[PLAN]?.value)
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
                fixture.application.close()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private class Fixture(val scope: TestScope) {
        val preferences = MemoryPreferences()
        val application =
            koinApplication { modules(StorageCommonModule, module { single<AppPreferences> { preferences } }) }
        val storage = application.koin.get<Storage>()
        val clock =
            object : Clock {
                override fun now() = Instant.fromEpochMilliseconds(100L * 86_400_000)
            }
        val activity = GameActivityRepository(storage, clock)
        val training = DailyTrainingRepository(storage, clock)
        val vm =
            WeeklyReviewViewModel(
                RepositoryWeeklyReviewDataSource(activity, training),
                clock,
            )
        val collector = scope.backgroundScope.launch { vm.uiState.collect {} }

        fun runCurrent() = scope.runCurrent()
    }

    private class MemoryPreferences : AppPreferences {
        val strings = mutableMapOf<String, MutableStateFlow<String?>>()
        val writes = mutableMapOf<String, Int>()
        var failWriteKey: String? = null

        fun seed(key: String, value: String) {
            strings.getOrPut(key) { MutableStateFlow(null) }.value = value
        }

        fun snapshot() = strings.mapValues { it.value.value }

        override fun getString(key: String): Flow<String?> = strings.getOrPut(key) { MutableStateFlow(null) }

        override suspend fun setString(key: String, value: String) {
            check(key != failWriteKey) { "Initialization write failed" }
            writes[key] = (writes[key] ?: 0) + 1
            seed(key, value)
        }

        override fun getInt(key: String): Flow<Int?> = flowOf(null)

        override fun getBoolean(key: String): Flow<Boolean?> = flowOf(null)

        override suspend fun setInt(key: String, value: Int) {
            error("Review cannot write integer preferences")
        }

        override suspend fun setBoolean(key: String, value: Boolean) {
            error("Review cannot write boolean preferences")
        }
    }

    private companion object {
        const val ACTIVITY = "game_sessions_v1"
        const val TRAINING = "daily_training_progress_v1"
        const val PLAN = "daily_training_v1"

        fun history(key: String, start: Long, days: String) = """{"$key":{"version":1,
                "trackingStartedEpochDay":$start,
                "completedEpochDays":[$days]}}"""
    }
}

private fun <T> WeeklySource<T>.readyValue(): T = assertIs<WeeklySource.Ready<T>>(this).value
