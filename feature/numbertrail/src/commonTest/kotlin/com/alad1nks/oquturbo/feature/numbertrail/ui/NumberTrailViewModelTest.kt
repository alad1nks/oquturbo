package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailFailure
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailGame
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class NumberTrailViewModelTest {
    @Test
    fun fractionalTickIntervalsCannotExtendTheExclusiveDeadline() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            repeat(239) {
                clock += 50.1.milliseconds
                vm.timerTickCallback()()
            }
            clock += 26.1.milliseconds
            vm.answer(1)
            runCurrent()
            assertEquals(NumberTrailFailure.Timeout, vm.uiState.value.game.failure)
            assertEquals(0, vm.uiState.value.game.score)
            assertEquals(12_000, GameActivityRepository(storage).observeSessions().first().single().durationMillis)
        }

    @Test
    fun fractionalActiveTimeSurvivesPauseAndRetainedRouteReentry() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            clock += 11_999.5.milliseconds
            vm.setForeground(false)
            val paused = vm.uiState.value.game
            clock += 50_000.milliseconds
            vm.setForeground(true)
            assertEquals(paused, vm.uiState.value.game)
            vm.resume()
            assertEquals(NumberTrailPhase.Active, vm.uiState.value.game.phase)
            clock += 0.5.milliseconds
            vm.answer(1)
            runCurrent()
            assertEquals(NumberTrailFailure.Timeout, vm.uiState.value.game.failure)
            assertEquals(0, vm.uiState.value.game.score)
            assertEquals(1, storage.gameSessionWriteCount)
        }

    @Test
    fun recordLoadingAndStartAreGuarded() =
        exercise { vm, _, _ ->
            vm.start()
            assertEquals(NumberTrailPhase.Ready, vm.uiState.value.game.phase)
            runCurrent()
            vm.start()
            val started = vm.uiState.value.game
            vm.start()
            assertEquals(started, vm.uiState.value.game)
        }

    @Test
    fun zeroMistakeWritesOnceWithIdentityAndNoTrainingMutation() =
        exercise { vm, storage, _ ->
            val daily = DailyTrainingRepository(storage)
            val plan = daily.ensureTodayTraining()
            val before = storage.getDailyTrainingJson().first()
            runCurrent()
            vm.start()
            vm.answer(2)
            vm.answer(2)
            runCurrent()
            val repository = GameActivityRepository(storage)
            val session = repository.observeSessions().first().single()
            assertEquals(GameId.NumberTrail, session.game)
            assertEquals(GameModeId.NumberTrailAscending, session.mode)
            assertNull(session.variantId)
            assertEquals(0, session.score)
            assertFalse(session.isNewRecord)
            assertEquals(1, storage.gameSessionWriteCount)
            assertEquals(0, repository.observeTotals().first().correctAnswers)
            assertTrue(repository.observeRecords().first().isEmpty())
            assertEquals(before, storage.getDailyTrainingJson().first())
            assertEquals(plan, daily.ensureTodayTraining())
        }

    @Test
    fun activeDurationExcludesPauseAndBoardFeedbackAndTimeoutWinsInputTie() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            clock += 100.milliseconds
            vm.answer(1)
            vm.pause()
            val paused = vm.uiState.value.game
            clock += 30_000.milliseconds
            vm.answer(2)
            vm.timerTickCallback()()
            assertEquals(paused, vm.uiState.value.game)
            vm.setForeground(false)
            vm.resume()
            assertEquals(paused, vm.uiState.value.game)
            vm.setForeground(true)
            vm.resume()
            for (n in 2..4) vm.answer(n)
            clock += 200.milliseconds
            vm.setForeground(false)
            assertEquals(NumberTrailPhase.Paused, vm.uiState.value.game.phase)
            assertEquals(400, vm.uiState.value.game.feedbackRemainingMillis)
            clock += 20_000.milliseconds
            vm.setForeground(true)
            assertEquals(NumberTrailPhase.Paused, vm.uiState.value.game.phase)
            vm.resume()
            clock += 399.milliseconds
            vm.timerTickCallback()()
            assertEquals(NumberTrailPhase.BoardComplete, vm.uiState.value.game.phase)
            clock += 1.milliseconds
            vm.timerTickCallback()()
            assertEquals(NumberTrailPhase.Active, vm.uiState.value.game.phase)
            clock += 12_000.milliseconds
            vm.answer(1)
            runCurrent()
            val session = GameActivityRepository(storage).observeSessions().first().single()
            assertEquals(4, session.score)
            assertEquals(12_100, session.durationMillis)
            assertEquals(NumberTrailFailure.Timeout, vm.uiState.value.game.failure)
        }

    @Test
    fun staleCallbacksAndAbandonmentCannotAdvanceOrPersist() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            val old = vm.uiState.value.game.board!!
            val tick = vm.timerTickCallback()
            vm.pause()
            clock += 50_000.milliseconds
            tick()
            assertEquals(NumberTrailPhase.Paused, vm.uiState.value.game.phase)
            vm.resume()
            vm.answer(2)
            runCurrent()
            vm.start()
            val fresh = vm.uiState.value
            tick()
            vm.selectAnswer(old.id, 0)
            assertEquals(fresh, vm.uiState.value)
            vm.abandon()
            clock += 30_000.milliseconds
            vm.timerTickCallback()()
            vm.answer(2)
            runCurrent()
            assertEquals(1, storage.gameSessionWriteCount)
        }

    @Test
    fun pendingWriteCompletesOnceWithoutChangingRetryAndStrictBestIsPreserved() =
        exercise { vm, storage, _ ->
            runCurrent()
            vm.start()
            vm.answer(1)
            val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
            storage.writeGate = gate
            vm.answer(3)
            assertEquals(NumberTrailSaveStatus.Pending, vm.uiState.value.saveStatus)
            assertEquals(0, vm.uiState.value.record)
            assertFalse(vm.uiState.value.isNewRecord)
            vm.start()
            val board = vm.uiState.value.game.board
            gate.complete(Unit)
            runCurrent()
            assertEquals(board, vm.uiState.value.game.board)
            assertEquals(NumberTrailSaveStatus.None, vm.uiState.value.saveStatus)
            assertEquals(1, vm.uiState.value.record)
            vm.answer(1)
            vm.answer(3)
            runCurrent()
            assertEquals(2, storage.gameSessionWriteCount)
            assertFalse(vm.uiState.value.isNewRecord)
            assertEquals(2, GameActivityRepository(storage).observeTotals().first().correctAnswers)
            vm.start()
            vm.answer(1)
            vm.answer(2)
            vm.answer(4)
            runCurrent()
            assertTrue(vm.uiState.value.isNewRecord)
            assertEquals(2, vm.uiState.value.record)
        }

    @Test
    fun failedWriteAndRecordStreamFailureAreHonestAndRecoverable() =
        exercise { vm, storage, _ ->
            runCurrent()
            vm.start()
            vm.answer(1)
            storage.failWrites = true
            vm.answer(3)
            runCurrent()
            assertEquals(NumberTrailSaveStatus.Failed, vm.uiState.value.saveStatus)
            assertEquals(0, vm.uiState.value.record)
            assertFalse(vm.uiState.value.isNewRecord)
            storage.gameSessionsJson.value = "broken"
            runCurrent()
            assertTrue(vm.uiState.value.recordLoadFailed)
            vm.start()
            assertEquals(NumberTrailPhase.Result, vm.uiState.value.game.phase)
            storage.gameSessionsJson.value = null
            storage.failWrites = false
            vm.loadRecord()
            runCurrent()
            assertFalse(vm.uiState.value.recordLoadFailed)
            vm.start()
            vm.answer(1)
            vm.answer(3)
            runCurrent()
            assertEquals(NumberTrailSaveStatus.Saved, vm.uiState.value.saveStatus)
            assertTrue(vm.uiState.value.isNewRecord)
        }

    private fun NumberTrailViewModel.answer(number: Int) {
        val board = uiState.value.game.board!!
        selectAnswer(board.id, board.numbers.indexOf(number))
    }

    private fun exercise(
        block: suspend kotlinx.coroutines.test.TestScope.(
            NumberTrailViewModel,
            RecordingStorage,
            TestTimeSource,
        ) -> Unit,
    ) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val storage = RecordingStorage()
            val clock = TestTimeSource()
            val vm = NumberTrailViewModel(GameActivityRepository(storage), NumberTrailGame(Random(7)), clock)
            try {
                block(vm, storage, clock)
            } finally {
                vm.abandon()
                vm.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    internal class RecordingStorage : Storage {
        private val darkTheme = MutableStateFlow<Boolean?>(null)
        private val languageCode = MutableStateFlow<String?>(null)
        private val soundEnabled = MutableStateFlow<Boolean?>(null)
        private val vibrationEnabled = MutableStateFlow<Boolean?>(null)
        private val remindersEnabled = MutableStateFlow<Boolean?>(null)
        val gameSessionsJson = MutableStateFlow<String?>(null)
        private val dailyTrainingJson = MutableStateFlow<String?>(null)
        private val dailyTrainingProgressJson = MutableStateFlow<String?>(null)
        private val profilePreferencesJson = MutableStateFlow<String?>(null)
        private val baspaRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val kenKozRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val rememberNumberRecords = mutableMapOf<Pair<Int, String>, MutableStateFlow<Int?>>()
        var gameSessionWriteCount = 0
        var failWrites = false
        var writeGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

        override fun getDarkTheme(): Flow<Boolean?> = darkTheme

        override fun getLanguageCode(): Flow<String?> = languageCode

        override fun getSoundEnabled(): Flow<Boolean?> = soundEnabled

        override fun getVibrationEnabled(): Flow<Boolean?> = vibrationEnabled

        override fun getRemindersEnabled(): Flow<Boolean?> = remindersEnabled

        override fun getGameSessionsJson(): Flow<String?> = gameSessionsJson

        override fun getDailyTrainingJson(): Flow<String?> = dailyTrainingJson

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
            writeGate?.await()
            check(!failWrites) { "write failed" }
            gameSessionWriteCount++
            gameSessionsJson.value = value
        }

        override suspend fun setDailyTrainingJson(value: String) {
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
