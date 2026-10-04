package com.alad1nks.oquturbo.feature.home.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class HomeViewModelTest {
    @Test
    fun freshPartialCompletedAndReloadUseRepositoryProgressWithoutDuplicateAwards() =
        exercise {
            runCurrent()
            val plan = repository.ensureTodayTraining()
            assertEquals(0, vm.uiState.value.dailyTraining!!.completedCount)
            assertEquals(plan.entries.size, vm.uiState.value.dailyTraining!!.totalCount)
            var launched: DailyTrainingEntry? = null
            vm.startTraining { launched = it }
            vm.startTraining { error("Duplicate navigation") }
            runCurrent()
            assertEquals(plan.nextEntry, launched)
            vm.startTraining { error("Repeated click after callback") }
            assertEquals(1, storage.writes)
            repository.completeEntry(plan.entries.first().id, plan.entries.first().requiredScore)
            vm.refreshDailyTraining()
            runCurrent()
            assertEquals(1, vm.uiState.value.dailyTraining!!.completedCount)
            val reloaded = DailyTrainingRepository(storage, clock).ensureTodayTraining()
            assertEquals(plan.entries[1], reloaded.nextEntry)
            vm.startTraining { launched = it }
            runCurrent()
            assertEquals(plan.entries[1], launched)
            reloaded.entries.drop(1).forEach { repository.completeEntry(it.id, it.requiredScore) }
            vm.refreshDailyTraining()
            runCurrent()
            assertTrue(vm.uiState.value.dailyTraining!!.isCompleted)
            val totals = activity.observeTotals().first()
            val progress = repository.observeProgress().first()
            repeat(3) {
                vm.refreshDailyTraining()
                runCurrent()
            }
            assertEquals(totals, activity.observeTotals().first())
            assertEquals(progress, repository.observeProgress().first())
            assertEquals(1, progress.totalCompletedTrainings)
            vm.startTraining { error("Completed plans cannot launch") }
            assertEquals(3, vm.uiState.value.dailyTraining!!.completedCount)
            val shortPlan = plan.copy(entries = plan.entries.take(2)).toHomeDailyTraining()
            assertEquals(2, shortPlan.totalCount)
            assertEquals(0, shortPlan.completedCount)
            assertFalse(plan.copy(entries = emptyList()).toHomeDailyTraining().isCompleted)
        }

    @Test
    fun failedLoadAndFailedRetryRecoverWithoutLaunching() =
        exercise(initialFailure = true) {
            runCurrent()
            assertNull(vm.uiState.value.dailyTraining)
            vm.startTraining { error("Loading cannot launch") }
            advanceTimeBy(701)
            runCurrent()
            assertTrue(vm.uiState.value.trainingLoadFailed)
            vm.retryDailyTraining()
            vm.retryDailyTraining()
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
            advanceTimeBy(701)
            runCurrent()
            assertTrue(vm.uiState.value.trainingLoadFailed)
            storage.failReads = false
            vm.retryDailyTraining()
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
            assertNotNull(vm.uiState.value.dailyTraining)
            assertEquals(1, storage.writes)
        }

    @Test
    fun launchFailureShowsRecoveryAndCancellationDoesNotBecomeAnError() =
        exercise {
            runCurrent()
            storage.failReads = true
            vm.startTraining { error("Failed launch") }
            runCurrent()
            assertTrue(vm.uiState.value.isStartingTraining)
            advanceTimeBy(701)
            runCurrent()
            assertTrue(vm.uiState.value.trainingLoadFailed)
            assertNull(vm.uiState.value.dailyTraining)
            storage.failReads = false
            vm.retryDailyTraining()
            runCurrent()
            storage.readGate = CompletableDeferred()
            vm.startTraining { error("Cancelled launch") }
            runCurrent()
            vm.viewModelScope.cancel()
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
        }

    @Test
    fun dayRolloverHidesOldCompletionDuringDelayedRefreshAndResumeRepairsIt() =
        exercise {
            runCurrent()
            repository.ensureTodayTraining().entries.forEach { repository.completeEntry(it.id, it.requiredScore) }
            runCurrent()
            assertTrue(vm.uiState.value.dailyTraining!!.isCompleted)
            storage.readGate = CompletableDeferred()
            clock.millis += DAY
            advanceTimeBy(60_001)
            runCurrent()
            assertNull(vm.uiState.value.dailyTraining)
            vm.startTraining { error("Old-day launch") }
            storage.readGate!!.complete(Unit)
            storage.readGate = null
            runCurrent()
            assertEquals(0, vm.uiState.value.dailyTraining!!.completedCount)
            clock.millis += DAY
            vm.refreshDailyTraining()
            runCurrent()
            assertEquals(0, vm.uiState.value.dailyTraining!!.completedCount)
            assertEquals(clock.millis / DAY, repository.ensureTodayTraining().epochDay)
        }

    @Test
    fun midnightDuringLaunchDoesNotNavigateToDifferentDaysEntry() =
        exercise {
            runCurrent()
            storage.readGate = CompletableDeferred()
            vm.startTraining { error("Day changed during launch") }
            runCurrent()
            clock.millis += DAY
            storage.readGate!!.complete(Unit)
            storage.readGate = null
            runCurrent()
            assertFalse(vm.uiState.value.isStartingTraining)
            assertEquals(0, vm.uiState.value.dailyTraining!!.completedCount)
        }

    @Test
    fun ordinaryActivitySessionDoesNotCompleteTraining() =
        exercise {
            runCurrent()
            val entry = repository.ensureTodayTraining().nextEntry!!
            activity.recordCompletedSession(
                entry.game,
                entry.mode,
                score = 100,
                durationMillis = 500,
                isNewRecord = true,
            )
            vm.refreshDailyTraining()
            runCurrent()
            assertEquals(0, vm.uiState.value.dailyTraining!!.completedCount)
            assertEquals(0, repository.observeProgress().first().totalCompletedTrainings)
        }

    @Test
    fun sharedStorageFailureStillShowsRetryAndRecovers() =
        exercise(sharedFailure = true) {
            runCurrent()
            advanceTimeBy(701)
            runCurrent()
            assertTrue(vm.uiState.value.trainingLoadFailed)
            assertNull(vm.uiState.value.dailyTraining)
            storage.failAllReads = false
            vm.retryDailyTraining()
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
            assertNotNull(vm.uiState.value.dailyTraining)
            advanceTimeBy(5_001)
            runCurrent()
            assertEquals(0f, vm.uiState.value.levelProgress)
        }

    @Test
    fun sharedStorageFailureAfterSuccessPreservesKnownActivityAndResumesUpdates() =
        exercise {
            runCurrent()
            val entry = repository.ensureTodayTraining().nextEntry!!
            activity.recordCompletedSession(
                entry.game,
                entry.mode,
                score = 100,
                durationMillis = 500,
                isNewRecord = true,
            )
            runCurrent()
            val known = vm.uiState.value
            assertEquals(0.2f, known.levelProgress)
            assertEquals(100, known.recentRecords.single().score)
            storage.failAllReads = true
            vm.refreshDailyTraining()
            runCurrent()
            advanceTimeBy(701)
            runCurrent()
            assertTrue(vm.uiState.value.trainingLoadFailed)
            assertEquals(known.levelProgress, vm.uiState.value.levelProgress)
            assertEquals(known.recentRecords, vm.uiState.value.recentRecords)
            storage.failAllReads = false
            vm.retryDailyTraining()
            activity.recordCompletedSession(
                entry.game,
                entry.mode,
                score = 200,
                durationMillis = 500,
                isNewRecord = true,
            )
            advanceTimeBy(5_001)
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
            assertEquals(0.6f, vm.uiState.value.levelProgress)
            assertEquals(200, vm.uiState.value.recentRecords.first().score)
            storage.failAllReads = true
            runCurrent()
            vm.viewModelScope.cancel()
            runCurrent()
        }

    @Test
    fun returningAfterSubscriptionTimeoutRetainsActivityDuringStorageOutage() =
        exercise {
            runCurrent()
            val entry = repository.ensureTodayTraining().nextEntry!!
            activity.recordCompletedSession(
                entry.game,
                entry.mode,
                score = 100,
                durationMillis = 500,
                isNewRecord = true,
            )
            runCurrent()
            val known = vm.uiState.value
            stopObserving()
            advanceTimeBy(5_001)
            runCurrent()
            storage.failAllReads = true
            observe()
            vm.refreshDailyTraining()
            runCurrent()
            advanceTimeBy(701)
            runCurrent()
            assertTrue(vm.uiState.value.trainingLoadFailed)
            assertEquals(known.levelProgress, vm.uiState.value.levelProgress)
            assertEquals(known.recentRecords, vm.uiState.value.recentRecords)
            storage.failAllReads = false
            vm.retryDailyTraining()
            advanceTimeBy(5_001)
            runCurrent()
            assertFalse(vm.uiState.value.trainingLoadFailed)
            assertEquals(known.levelProgress, vm.uiState.value.levelProgress)
            assertEquals(known.recentRecords, vm.uiState.value.recentRecords)
        }

    private fun exercise(
        initialFailure: Boolean = false,
        sharedFailure: Boolean = false,
        block: suspend Fixture.() -> Unit,
    ) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = Fixture(this, initialFailure, sharedFailure)
            try {
                fixture.observe()
                fixture.block()
            } finally {
                fixture.vm.viewModelScope.cancel()
                backgroundScope.cancel()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private class Fixture(private val scope: TestScope, initialFailure: Boolean, sharedFailure: Boolean) {
        private var collector: Job? = null

        fun observe() {
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
        }

        fun stopObserving() {
            collector?.cancel()
        }

        fun runCurrent() = scope.runCurrent()

        fun advanceTimeBy(millis: Long) = scope.advanceTimeBy(millis)

        val storage =
            TestStorage().apply {
                failReads = initialFailure
                failAllReads = sharedFailure
            }
        val clock = TestClock()
        val repository = DailyTrainingRepository(storage, clock)
        val activity = GameActivityRepository(storage)
        val vm = HomeViewModel(activity, repository, clock)
    }

    private class TestClock : Clock {
        var millis = 20_000 * DAY

        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private companion object {
        const val DAY = 86_400_000L
    }

    private class TestStorage : Storage {
        private val darkTheme = MutableStateFlow<Boolean?>(null)
        private val languageCode = MutableStateFlow<String?>(null)
        private val soundEnabled = MutableStateFlow<Boolean?>(null)
        private val vibrationEnabled = MutableStateFlow<Boolean?>(null)
        private val remindersEnabled = MutableStateFlow<Boolean?>(null)
        private val gameSessionsJson = MutableStateFlow<String?>(null)
        val dailyTrainingJson = MutableStateFlow<String?>(null)
        private val dailyTrainingProgressJson = MutableStateFlow<String?>(null)
        private val profilePreferencesJson = MutableStateFlow<String?>(null)
        private val baspaRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val kenKozRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val rememberNumberRecords = mutableMapOf<Pair<Int, String>, MutableStateFlow<Int?>>()

        override fun getDarkTheme(): Flow<Boolean?> = darkTheme

        override fun getLanguageCode(): Flow<String?> = languageCode

        override fun getSoundEnabled(): Flow<Boolean?> = soundEnabled

        override fun getVibrationEnabled(): Flow<Boolean?> = vibrationEnabled

        override fun getRemindersEnabled(): Flow<Boolean?> = remindersEnabled

        private val allReadFailure = MutableStateFlow(false)
        var failAllReads: Boolean
            get() = allReadFailure.value
            set(value) {
                allReadFailure.value = value
            }

        override fun getGameSessionsJson(): Flow<String?> =
            combine(allReadFailure, gameSessionsJson) { failed, value ->
                check(!failed) { "Shared storage unavailable" }
                value
            }

        var failReads = false
        var readGate: CompletableDeferred<Unit>? = null
        var writes = 0

        override fun getDailyTrainingJson(): Flow<String?> =
            flow {
                readGate?.await()
                check(!failReads && !failAllReads) { "Storage unavailable" }
                emitAll(dailyTrainingJson)
            }

        override fun getDailyTrainingProgressJson(): Flow<String?> = dailyTrainingProgressJson

        override fun getProfilePreferencesJson(): Flow<String?> = profilePreferencesJson

        override fun getBaspaGameRecord(mode: String): Flow<Int?> =
            baspaRecords.getOrPut(
                mode,
            ) { MutableStateFlow(null) }

        override fun getKenKozGameRecord(mode: String): Flow<Int?> =
            kenKozRecords.getOrPut(
                mode,
            ) { MutableStateFlow(null) }

        override fun getRememberNumberRecord(maxLength: Int, availableDigits: String): Flow<Int?> =
            rememberNumberRecords.getOrPut(maxLength to availableDigits) { MutableStateFlow(null) }

        override suspend fun setDarkTheme(value: Boolean) {
            darkTheme.value = value
        }

        override suspend fun setLanguageCode(value: String) {
            languageCode.value = value
        }

        override suspend fun setSoundEnabled(value: Boolean) {
            soundEnabled.value = value
        }

        override suspend fun setVibrationEnabled(value: Boolean) {
            vibrationEnabled.value = value
        }

        override suspend fun setRemindersEnabled(value: Boolean) {
            remindersEnabled.value = value
        }

        override suspend fun setGameSessionsJson(value: String) {
            gameSessionsJson.value = value
        }

        override suspend fun setDailyTrainingJson(value: String) {
            writes++
            dailyTrainingJson.value = value
        }

        override suspend fun setDailyTrainingProgressJson(value: String) {
            dailyTrainingProgressJson.value = value
        }

        override suspend fun setProfilePreferencesJson(value: String) {
            profilePreferencesJson.value = value
        }

        override suspend fun setBaspaGameRecord(mode: String, record: Int) {
            baspaRecords.getOrPut(mode) { MutableStateFlow(null) }.value = record
        }

        override suspend fun setKenKozGameRecord(mode: String, record: Int) {
            kenKozRecords.getOrPut(mode) { MutableStateFlow(null) }.value = record
        }

        override suspend fun setRememberNumberRecord(maxLength: Int, availableDigits: String, record: Int) {
            rememberNumberRecords.getOrPut(maxLength to availableDigits) { MutableStateFlow(null) }.value = record
        }
    }
}
