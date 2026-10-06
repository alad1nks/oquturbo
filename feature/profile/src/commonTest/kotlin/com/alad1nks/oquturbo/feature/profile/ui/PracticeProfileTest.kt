package com.alad1nks.oquturbo.feature.profile.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.ProfileRepository
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class PracticeProfileTest {
    @Test
    fun mapperAvailabilityEarnedCountAndNoInventedEarnedDateStayConsistent() {
        val base =
            ProfileUiState(
                achievements =
                    listOf(
                        ProfileUiState.Achievement(AchievementId.FirstTraining, AchievementStatus.Earned),
                    ),
            )
        for (state in listOf(ProfilePracticeState.Loading, ProfilePracticeState.Error)) {
            val mapped = base.withPracticeHistory(state)
            assertEquals(1, mapped.earnedAchievementsCount)
            assertEquals(
                if (state == ProfilePracticeState.Loading) AchievementStatus.Loading else AchievementStatus.Unavailable,
                mapped.achievements.last().status,
            )
            assertNull(mapped.achievements.last().earnedDate)
        }
        val earned =
            base.withPracticeHistory(
                ProfilePracticeState.Ready(calculatePracticeRhythm(DayHistory(80, (101L..107L).toList()), 100)),
            )
        assertEquals(2, earned.earnedAchievementsCount)
        assertEquals(0, earned.currentStreakDays)
        assertEquals(7, earned.bestStreakDays)
        assertEquals(AchievementStatus.Earned, earned.achievements.last().status)
        assertNull(earned.achievements.last().earnedDate)
        assertTrue(earned.recentUnlocks.isEmpty())
    }

    @Test
    fun savedZeroSessionUpdatesCurrentAndRepeatedDayDoesNotDuplicateProgress() =
        exercise {
            runCurrent()
            assertEquals(
                ProfilePracticeState.Ready(calculatePracticeRhythm(DayHistory(100, emptyList()), 100)),
                vm.uiState.value.practiceHistory,
            )
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
                assertEquals(1, vm.uiState.value.currentStreakDays)
                assertEquals(1, vm.uiState.value.bestStreakDays)
                assertEquals(
                    1,
                    vm.uiState.value.achievements.single { it.id == AchievementId.SevenDayStreak }.currentProgress,
                )
            }
            assertEquals(0, vm.uiState.value.currentLevelXp)
            assertEquals(0, vm.uiState.value.completedTrainings)
        }

    @Test
    fun historyFailurePreservesIdentityXpAndOtherAchievementsAndRetryCoalesces() =
        exercise {
            runCurrent()
            preferences.setString("game_sessions_v1", """{"totalCorrectAnswers":1000,"practiceHistory":null}""")
            profile.updateIdentity("Ada", PersonalizationId.DefaultAvatar.name)
            runCurrent()
            assertEquals(ProfilePracticeState.Error, vm.uiState.value.practiceHistory)
            assertEquals(3, vm.uiState.value.level)
            assertEquals("Ada", vm.uiState.value.displayName)
            assertEquals(1, vm.uiState.value.earnedAchievementsCount)
            preferences.gate = CompletableDeferred()
            vm.retryPracticeHistory()
            vm.retryPracticeHistory()
            assertEquals(ProfilePracticeState.Loading, vm.uiState.value.practiceHistory)
            runCurrent()
            assertEquals(4, preferences.activeActivityReaders) // progress, records, sessions, history
            preferences.setString("game_sessions_v1", validHistory)
            preferences.gate!!.complete(Unit)
            preferences.gate = null
            runCurrent()
            assertIs<ProfilePracticeState.Ready>(vm.uiState.value.practiceHistory)
            assertEquals(2, vm.uiState.value.earnedAchievementsCount)
            assertNull(vm.uiState.value.achievements.single { it.id == AchievementId.SevenDayStreak }.earnedDate)
        }

    @Test
    fun midnightAndResumeUseFreshHistoryWithoutResettingConfirmedBest() =
        exercise {
            runCurrent()
            preferences.setString("game_sessions_v1", validHistory)
            runCurrent()
            assertEquals(7, vm.uiState.value.currentStreakDays)
            clock.day = 102
            advanceTimeBy(1_001)
            runCurrent()
            assertEquals(0, vm.uiState.value.currentStreakDays)
            assertEquals(7, vm.uiState.value.bestStreakDays)
            collector.cancel()
            advanceTimeBy(5_001)
            runCurrent()
            assertEquals(ProfilePracticeState.Loading, vm.uiState.value.practiceHistory)
            preferences.gate = CompletableDeferred()
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
            vm.refreshProfile()
            runCurrent()
            assertEquals(ProfilePracticeState.Loading, vm.uiState.value.practiceHistory)
            assertEquals(3, vm.uiState.value.level)
            preferences.gate!!.complete(Unit)
            preferences.gate = null
            runCurrent()
            assertEquals(0, vm.uiState.value.currentStreakDays)
            assertEquals(7, vm.uiState.value.bestStreakDays)
            assertIs<ProfilePracticeState.Ready>(vm.uiState.value.practiceHistory)
        }

    @Test
    fun delayedInitialReadDoesNotInventZeroAndCancellationDoesNotBecomeError() =
        exercise {
            preferences.gate = CompletableDeferred()
            runCurrent()
            assertEquals(ProfilePracticeState.Loading, vm.uiState.value.practiceHistory)
            assertEquals(
                AchievementStatus.Loading,
                vm.uiState.value.achievements.single {
                    it.id == AchievementId.SevenDayStreak
                }.status,
            )
            vm.viewModelScope.cancel()
            runCurrent()
            assertEquals(ProfilePracticeState.Loading, vm.uiState.value.practiceHistory)
            assertEquals(0, preferences.activeActivityReaders)
            assertEquals(0, preferences.activityWrites)
        }

    @Test
    fun failedInitializationRemainsUnavailableUntilSuccessfulWriteAndRetryDoesNotDuplicateIt() =
        exercise {
            preferences.failWrites = true
            runCurrent()
            assertEquals(ProfilePracticeState.Error, vm.uiState.value.practiceHistory)
            assertEquals(0, preferences.activityWrites)
            preferences.failWrites = false
            vm.retryPracticeHistory()
            vm.retryPracticeHistory()
            assertEquals(ProfilePracticeState.Loading, vm.uiState.value.practiceHistory)
            runCurrent()
            val ready = assertIs<ProfilePracticeState.Ready>(vm.uiState.value.practiceHistory)
            assertEquals(100L, ready.rhythm.trackingStartedEpochDay)
            assertEquals(0, ready.rhythm.completedDaysInWindow)
            assertEquals(1, preferences.activityWrites)
            vm.refreshProfile()
            runCurrent()
            assertEquals(1, preferences.activityWrites)
        }

    @Test
    fun sharedReadFailureKeepsLastKnownProfileAndRecoversBothSources() =
        exercise {
            runCurrent()
            preferences.setString("game_sessions_v1", validHistory)
            profile.updateIdentity("Ada", PersonalizationId.DefaultAvatar.name)
            runCurrent()
            assertEquals(3, vm.uiState.value.level)
            preferences.failReads.value = true
            runCurrent()
            assertEquals(ProfilePracticeState.Error, vm.uiState.value.practiceHistory)
            assertEquals(3, vm.uiState.value.level)
            assertEquals("Ada", vm.uiState.value.displayName)
            collector.cancel()
            advanceTimeBy(5_001)
            runCurrent()
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
            runCurrent()
            assertEquals(ProfilePracticeState.Error, vm.uiState.value.practiceHistory)
            assertEquals(3, vm.uiState.value.level)
            assertEquals("Ada", vm.uiState.value.displayName)
            preferences.failReads.value = false
            vm.retryPracticeHistory()
            advanceTimeBy(1_001)
            runCurrent()
            assertIs<ProfilePracticeState.Ready>(vm.uiState.value.practiceHistory)
            assertEquals(7, vm.uiState.value.bestStreakDays)
            assertEquals(4, preferences.activeActivityReaders)
        }

    private fun exercise(block: suspend Fixture.() -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = Fixture(this)
            try {
                fixture.block()
            } finally {
                fixture.vm.viewModelScope.cancel()
                fixture.application.close()
                backgroundScope.cancel()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private class Fixture(val scope: TestScope) {
        val preferences = MemoryPreferences()
        val application =
            koinApplication { modules(StorageCommonModule, module { single<AppPreferences> { preferences } }) }
        val storage = application.koin.get<Storage>()
        val clock = TestClock()
        val activity = GameActivityRepository(storage, clock)
        val profile = ProfileRepository(storage)
        val vm =
            ProfileViewModel(
                activity,
                DailyTrainingRepository(storage, clock),
                profile,
                SettingsRepository(storage),
                clock,
            )
        var collector = scope.backgroundScope.launch { vm.uiState.collect {} }

        fun runCurrent() = scope.runCurrent()

        fun advanceTimeBy(time: Long) = scope.advanceTimeBy(time)
    }

    private class TestClock(var day: Long = 100) : Clock {
        override fun now() = Instant.fromEpochMilliseconds(day * 86_400_000L)
    }

    private class MemoryPreferences : AppPreferences {
        val strings = mutableMapOf<String, MutableStateFlow<String?>>()
        var gate: CompletableDeferred<Unit>? = null
        var activeActivityReaders = 0
        var activityWrites = 0
        var failWrites = false
        val failReads = MutableStateFlow(false)

        override fun getString(key: String): Flow<String?> =
            flow {
                if (key == "game_sessions_v1") activeActivityReaders++
                try {
                    if (key == "game_sessions_v1") gate?.await()
                    emitAll(
                        combine(strings.getOrPut(key) { MutableStateFlow(null) }, failReads) { value, failed ->
                            check(key != "game_sessions_v1" || !failed) { "Shared activity read failure" }
                            value
                        },
                    )
                } finally {
                    if (key == "game_sessions_v1") activeActivityReaders--
                }
            }

        override suspend fun setString(key: String, value: String) {
            if (key == "game_sessions_v1") {
                check(!failWrites) { "History initialization write failure" }
                activityWrites++
            }
            strings.getOrPut(key) { MutableStateFlow(null) }.value = value
        }

        override fun getInt(key: String): Flow<Int?> = flowOf(null)

        override fun getBoolean(key: String): Flow<Boolean?> = flowOf(null)

        override suspend fun setInt(key: String, value: Int) = Unit

        override suspend fun setBoolean(key: String, value: Boolean) = Unit
    }

    private companion object {
        val validHistory = """{"totalCorrectAnswers":1000,"practiceHistory":{"version":1,
            "trackingStartedEpochDay":80,"completedEpochDays":[94,95,96,97,98,99,100]}}"""
    }
}
