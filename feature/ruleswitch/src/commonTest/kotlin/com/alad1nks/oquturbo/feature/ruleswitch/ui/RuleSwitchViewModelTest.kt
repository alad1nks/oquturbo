package com.alad1nks.oquturbo.feature.ruleswitch.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchFailure
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchGame
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchPhase
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
class RuleSwitchViewModelTest {
    @Test
    fun staleFeedbackAndPriorAttemptTimerCallbacksDoNotSettleNewRound() =
        exercise { vm, _, clock ->
            runCurrent()
            vm.start()
            vm.correct()
            val staleFeedback = vm.timerTickCallback()
            clock += 600.milliseconds
            staleFeedback()
            val fresh = vm.uiState.value.game
            clock += 100.milliseconds
            staleFeedback()
            assertEquals(fresh, vm.uiState.value.game)
            vm.timerTickCallback()()
            assertEquals(5900, vm.uiState.value.game.board!!.remainingTimeMillis)
            vm.wrong()
            runCurrent()
            val oldAttempt = vm.timerTickCallback()
            vm.start()
            val replay = vm.uiState.value.game
            clock += 9000.milliseconds
            oldAttempt()
            assertEquals(replay, vm.uiState.value.game)
            vm.timerTickCallback()()
            assertEquals(RuleSwitchFailure.Timeout, vm.uiState.value.game.failure)
        }

    @Test
    fun exclusiveDeadlineIncludesFractionalIntervalsAndPausedRemainder() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            clock += 5_999.5.milliseconds
            vm.setForeground(false)
            val paused = vm.uiState.value.game
            clock += 50_000.milliseconds
            vm.setForeground(true)
            assertEquals(paused, vm.uiState.value.game)
            vm.resume()
            clock += 0.5.milliseconds
            vm.correct()
            runCurrent()
            assertEquals(RuleSwitchFailure.Timeout, vm.uiState.value.game.failure)
            assertEquals(6_000, GameActivityRepository(storage).observeSessions().first().single().durationMillis)
            assertEquals(0, vm.uiState.value.game.score)
        }

    @Test
    fun fractionalTickIntervalsCannotExtendTheDeadline() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            repeat(119) {
                clock += 50.1.milliseconds
                vm.timerTickCallback()()
            }
            clock += 38.1.milliseconds
            vm.correct()
            runCurrent()
            assertEquals(RuleSwitchFailure.Timeout, vm.uiState.value.game.failure)
            assertEquals(6_000, GameActivityRepository(storage).observeSessions().first().single().durationMillis)
        }

    @Test
    fun initialLoadAndRepeatedStartAreGuarded() =
        exercise { vm, _, _ ->
            vm.start()
            assertEquals(RuleSwitchPhase.Ready, vm.uiState.value.game.phase)
            runCurrent()
            vm.start()
            val initial = vm.uiState.value.game
            vm.start()
            assertEquals(initial, vm.uiState.value.game)
        }

    @Test
    fun zeroFailureWritesOneSessionAndNoRecordXpOrTrainingChange() =
        exercise { vm, storage, _ ->
            val daily = DailyTrainingRepository(storage)
            val plan = daily.ensureTodayTraining()
            val before = storage.getDailyTrainingJson().first()
            runCurrent()
            vm.start()
            vm.wrong()
            vm.wrong()
            vm.retrySave()
            runCurrent()
            val repo = GameActivityRepository(storage)
            val session = repo.observeSessions().first().single()
            assertEquals(GameId.RuleSwitch, session.game)
            assertEquals(GameModeId.RuleSwitchSwitch, session.mode)
            assertNull(session.variantId)
            assertEquals(0, session.score)
            assertFalse(session.isNewRecord)
            assertEquals(1, storage.gameSessionWriteCount)
            assertEquals(0, repo.observeProgress().first().totalXp)
            assertTrue(repo.observeRecords().first().isEmpty())
            assertEquals(before, storage.getDailyTrainingJson().first())
            assertEquals(plan, daily.ensureTodayTraining())
        }

    @Test
    fun feedbackPauseAndStaleCallbacksNeverGenerateOffscreenOrCountPausedTime() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            val old = vm.uiState.value.game.board!!
            clock += 100.milliseconds
            vm.correct()
            vm.correct()
            assertEquals(1, vm.uiState.value.game.score)
            clock += 200.milliseconds
            val stale = vm.timerTickCallback()
            vm.setForeground(false)
            assertEquals(400, vm.uiState.value.game.feedbackRemainingMillis)
            clock += 50_000.milliseconds
            stale()
            vm.setForeground(true)
            assertEquals(RuleSwitchPhase.Paused, vm.uiState.value.game.phase)
            vm.resume()
            clock += 399.milliseconds
            vm.timerTickCallback()()
            assertEquals(RuleSwitchPhase.Correct, vm.uiState.value.game.phase)
            clock += 1.milliseconds
            vm.timerTickCallback()()
            assertEquals(RuleSwitchPhase.Active, vm.uiState.value.game.phase)
            vm.selectAnswer(old.id, old.correctAnswer)
            assertEquals(1, vm.uiState.value.game.score)
            clock += 6_000.milliseconds
            vm.correct()
            runCurrent()
            val session = GameActivityRepository(storage).observeSessions().first().single()
            assertEquals(1, session.score)
            assertEquals(6_100, session.durationMillis)
        }

    @Test
    fun abandonAndProcessRestartDoNotRecordUnfinishedRun() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            vm.correct()
            val stale = vm.timerTickCallback()
            val abandonedBoard = vm.uiState.value.game.board!!
            vm.abandon()
            clock += 30_000.milliseconds
            stale()
            vm.selectAnswer(abandonedBoard.id, abandonedBoard.options.first { it != abandonedBoard.correctAnswer })
            runCurrent()
            assertEquals(0, storage.gameSessionWriteCount)
            val restarted = RuleSwitchViewModel(GameActivityRepository(storage))
            assertEquals(RuleSwitchPhase.Ready, restarted.uiState.value.game.phase)
            restarted.viewModelScope.cancel()
        }

    @Test
    fun pendingSaveBlocksReplayAndRetryThenRetainsBestOnTie() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            vm.correct()
            clock += 600.milliseconds
            vm.timerTickCallback()()
            val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
            storage.writeGate = gate
            vm.wrong()
            assertEquals(RuleSwitchSaveStatus.Pending, vm.uiState.value.saveStatus)
            val terminal = vm.uiState.value.game
            vm.start()
            vm.retrySave()
            assertEquals(terminal, vm.uiState.value.game)
            assertFalse(vm.uiState.value.isNewRecord)
            gate.complete(Unit)
            runCurrent()
            assertEquals(RuleSwitchSaveStatus.Saved, vm.uiState.value.saveStatus)
            assertTrue(vm.uiState.value.isNewRecord)
            assertEquals(1, vm.uiState.value.record)
            vm.retrySave()
            vm.start()
            assertEquals(0, vm.uiState.value.game.score)
            vm.correct()
            clock += 600.milliseconds
            vm.timerTickCallback()()
            vm.wrong()
            runCurrent()
            assertFalse(vm.uiState.value.isNewRecord)
            assertEquals(2, storage.gameSessionWriteCount)
            assertEquals(2, GameActivityRepository(storage).observeTotals().first().correctAnswers)
        }

    @Test
    fun failedSaveRetriesSameCompletionOnceAndRecordLoadRecovers() =
        exercise { vm, storage, clock ->
            runCurrent()
            vm.start()
            vm.correct()
            clock += 600.milliseconds
            vm.timerTickCallback()()
            storage.failWrites = true
            vm.wrong()
            runCurrent()
            val terminal = vm.uiState.value.game
            assertEquals(RuleSwitchSaveStatus.Failed, vm.uiState.value.saveStatus)
            assertFalse(vm.uiState.value.isNewRecord)
            vm.start()
            assertEquals(terminal, vm.uiState.value.game)
            storage.failWrites = false
            val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
            storage.writeGate = gate
            vm.retrySave()
            vm.retrySave()
            assertEquals(RuleSwitchSaveStatus.Pending, vm.uiState.value.saveStatus)
            gate.complete(Unit)
            runCurrent()
            vm.retrySave()
            runCurrent()
            assertEquals(1, storage.gameSessionWriteCount)
            assertEquals(RuleSwitchSaveStatus.Saved, vm.uiState.value.saveStatus)
            assertTrue(vm.uiState.value.isNewRecord)
            val persisted = storage.gameSessionsJson.value
            storage.gameSessionsJson.value = "broken"
            runCurrent()
            assertTrue(vm.uiState.value.recordLoadFailed)
            storage.gameSessionsJson.value = persisted
            vm.loadRecord()
            runCurrent()
            assertFalse(vm.uiState.value.recordLoadFailed)
            assertEquals(1, vm.uiState.value.record)
        }

    @Test
    fun repositorySupportsExplicitCompletionTimeAndPreservesDefaultClock() =
        exercise { _, storage, _ ->
            val repo = GameActivityRepository(storage)
            val explicit =
                repo.recordCompletedSession(
                    GameId.RuleSwitch,
                    GameModeId.RuleSwitchSwitch,
                    score = 2,
                    correctAnswers = 2,
                    durationMillis = 300,
                    isNewRecord = true,
                    completedAtEpochMillis = 123456789L,
                )
            assertEquals(123456789L, explicit.completedAtEpochMillis)
            assertEquals(1, explicit.completedEpochDay)
            val current =
                repo.recordCompletedSession(
                    GameId.RuleSwitch,
                    GameModeId.RuleSwitchSwitch,
                    score = 2,
                    correctAnswers = 2,
                    durationMillis = 300,
                    isNewRecord = true,
                )
            assertTrue(current.completedAtEpochMillis > explicit.completedAtEpochMillis)
            assertFalse(current.isNewRecord)
        }

    private fun RuleSwitchViewModel.correct() {
        val board = uiState.value.game.board!!
        selectAnswer(board.id, board.correctAnswer)
    }

    private fun RuleSwitchViewModel.wrong() {
        val board = uiState.value.game.board!!
        selectAnswer(board.id, board.options.first { it != board.correctAnswer })
    }

    private fun exercise(
        block: suspend kotlinx.coroutines.test.TestScope.(
            RuleSwitchViewModel,
            RecordingStorage,
            TestTimeSource,
        ) -> Unit,
    ) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val storage = RecordingStorage()
            val clock = TestTimeSource()
            val vm = RuleSwitchViewModel(GameActivityRepository(storage), RuleSwitchGame(Random(7)), clock)
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
