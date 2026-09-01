package com.alad1nks.oquturbo.feature.memorygrid.ui

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGame
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridPhase
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridSequenceGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryGridViewModelTest {
    @Test
    fun confirmedRecordShowsAcknowledgementAndPreservesSessionPayload() =
        runTest {
            withTestMain {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val viewModel = createViewModel(repository = repository)
                runCurrent()

                completeOneRoundThenFail(viewModel)
                runCurrent()

                val session = repository.observeSessions().first().single()
                assertEquals(GameId.MemoryGrid, session.game)
                assertEquals(GameModeId.MemoryGridRoute, session.mode)
                assertEquals(3, session.score)
                assertEquals(3, session.correctAnswers)
                assertTrue(session.isNewRecord)
                assertTrue(viewModel.uiState.value.isNewRecord)
                assertEquals(1, storage.gameSessionWriteCount)
            }
        }

    @Test
    fun repositoryRejectedRecordDoesNotShowAcknowledgement() =
        runTest {
            withTestMain {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val viewModel = createViewModel(repository = repository)
                runCurrent()
                repository.recordCompletedSession(
                    game = GameId.MemoryGrid,
                    mode = GameModeId.MemoryGridRoute,
                    score = 10,
                    durationMillis = 1,
                    isNewRecord = true,
                )

                completeOneRoundThenFail(viewModel)
                runCurrent()

                val sessions = repository.observeSessions().first()
                assertEquals(2, sessions.size)
                assertFalse(sessions.last().isNewRecord)
                assertFalse(viewModel.uiState.value.isNewRecord)
            }
        }

    @Test
    fun acknowledgementRemainsAbsentWhilePersistenceIsPending() =
        runTest {
            withTestMain {
                val gate = CompletableDeferred<Unit>()
                val storage = RecordingStorage(gate)
                val viewModel = createViewModel(repository = GameActivityRepository(storage))
                runCurrent()

                completeOneRoundThenFail(viewModel)
                runCurrent()

                assertTrue(storage.sessionWriteStarted.isCompleted)
                assertEquals(MemoryGridPhase.GameOver, viewModel.uiState.value.phase)
                assertFalse(viewModel.uiState.value.isNewRecord)
                assertEquals(0, storage.gameSessionWriteCount)

                gate.complete(Unit)
                runCurrent()
                assertTrue(viewModel.uiState.value.isNewRecord)
            }
        }

    @Test
    fun persistenceFailureKeepsGameOverUsableAndRetryStartsCleanAttempt() =
        runTest {
            withTestMain {
                val storage =
                    RecordingStorage(
                        sessionWriteFailure = IllegalStateException("write failed"),
                    )
                val viewModel = createViewModel(repository = GameActivityRepository(storage))
                runCurrent()

                completeOneRoundThenFail(viewModel)
                runCurrent()

                assertTrue(storage.sessionWriteStarted.isCompleted)
                assertEquals(0, storage.gameSessionWriteCount)
                assertEquals(MemoryGridPhase.GameOver, viewModel.uiState.value.phase)
                assertFalse(viewModel.uiState.value.isNewRecord)

                viewModel.start()

                val replayState = viewModel.uiState.value
                assertEquals(MemoryGridPhase.ShowingSequence, replayState.phase)
                assertEquals(0, replayState.score)
                assertTrue(replayState.input.isEmpty())
                assertFalse(replayState.isNewRecord)
                advanceTimeBy(2_100)
                runCurrent()
                viewModel.selectCell(8)
                runCurrent()
                assertEquals(MemoryGridPhase.GameOver, viewModel.uiState.value.phase)
                assertFalse(viewModel.uiState.value.isNewRecord)
            }
        }

    @Test
    fun retrySynchronouslyClearsAcknowledgement() =
        runTest {
            withTestMain {
                val viewModel = createViewModel(repository = GameActivityRepository(RecordingStorage()))
                runCurrent()
                completeOneRoundThenFail(viewModel)
                runCurrent()
                assertTrue(viewModel.uiState.value.isNewRecord)

                viewModel.start()

                assertFalse(viewModel.uiState.value.isNewRecord)
                assertEquals(MemoryGridPhase.ShowingSequence, viewModel.uiState.value.phase)
                advanceTimeBy(2_100)
                runCurrent()
                viewModel.selectCell(8)
                runCurrent()
            }
        }

    @Test
    fun lateCompletionCannotMutateReplayOrLaterGameOver() =
        runTest {
            withTestMain {
                val gate = CompletableDeferred<Unit>()
                val storage = RecordingStorage(gate)
                val repository = GameActivityRepository(storage)
                val viewModel = createViewModel(repository = repository)
                runCurrent()

                completeOneRoundThenFail(viewModel)
                runCurrent()
                assertFalse(viewModel.uiState.value.isNewRecord)

                viewModel.start()
                assertFalse(viewModel.uiState.value.isNewRecord)
                completeCurrentAttemptOneRoundThenFail(viewModel)
                runCurrent()
                assertFalse(viewModel.uiState.value.isNewRecord)

                gate.complete(Unit)
                runCurrent()

                val sessions = repository.observeSessions().first()
                assertEquals(2, sessions.size)
                assertTrue(sessions.first().isNewRecord)
                assertFalse(sessions.last().isNewRecord)
                assertEquals(MemoryGridPhase.GameOver, viewModel.uiState.value.phase)
                assertFalse(viewModel.uiState.value.isNewRecord)
            }
        }

    @Test
    fun repeatedTerminalInputWritesSessionExactlyOnce() =
        runTest {
            withTestMain {
                val storage = RecordingStorage()
                val viewModel = createViewModel(repository = GameActivityRepository(storage))
                runCurrent()

                completeOneRoundThenFail(viewModel)
                viewModel.selectCell(7)
                viewModel.selectCell(6)
                runCurrent()

                assertEquals(1, storage.gameSessionWriteCount)
            }
        }

    @Test
    fun allModesKeepTheirSeriesAndScoreSemantics() =
        runTest {
            withTestMain {
                val expectations =
                    listOf(
                        Triple(MemoryGridGameMode.Route, GameModeId.MemoryGridRoute, 3),
                        Triple(MemoryGridGameMode.Reverse, GameModeId.MemoryGridReverse, 3),
                        Triple(MemoryGridGameMode.Flash, GameModeId.MemoryGridFlash, 1),
                    )

                expectations.forEach { (mode, expectedMode, expectedScore) ->
                    val storage = RecordingStorage()
                    val repository = GameActivityRepository(storage)
                    val viewModel = createViewModel(mode = mode, repository = repository)
                    runCurrent()

                    completeOneRoundThenFail(viewModel, mode)
                    runCurrent()

                    val session = repository.observeSessions().first().single()
                    assertEquals(expectedMode, session.mode)
                    assertEquals(expectedScore, session.score)
                    assertEquals(3, session.correctAnswers)
                }
            }
        }

    private suspend fun TestScope.withTestMain(block: suspend TestScope.() -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun TestScope.completeOneRoundThenFail(
        viewModel: MemoryGridViewModel,
        mode: MemoryGridGameMode = MemoryGridGameMode.Route,
    ) {
        viewModel.start()
        completeCurrentAttemptOneRoundThenFail(viewModel, mode)
    }

    private fun TestScope.completeCurrentAttemptOneRoundThenFail(
        viewModel: MemoryGridViewModel,
        mode: MemoryGridGameMode = MemoryGridGameMode.Route,
    ) {
        advanceTimeBy(2_100)
        runCurrent()
        val firstRoundInput =
            when (mode) {
                MemoryGridGameMode.Route,
                MemoryGridGameMode.Flash,
                -> listOf(0, 1, 2)
                MemoryGridGameMode.Reverse -> listOf(2, 1, 0)
            }
        firstRoundInput.forEach(viewModel::selectCell)
        assertEquals(MemoryGridPhase.RoundSuccess, viewModel.uiState.value.phase)

        advanceTimeBy(500)
        runCurrent()
        advanceTimeBy(if (mode == MemoryGridGameMode.Flash) 665 else 2_660)
        runCurrent()
        assertEquals(MemoryGridPhase.AwaitingInput, viewModel.uiState.value.phase)
        viewModel.selectCell(8)
        assertEquals(MemoryGridPhase.GameOver, viewModel.uiState.value.phase)
    }

    private fun createViewModel(
        mode: MemoryGridGameMode = MemoryGridGameMode.Route,
        repository: GameActivityRepository,
    ): MemoryGridViewModel {
        val generator =
            MemoryGridSequenceGenerator { _, length, _ ->
                (0 until length).toList()
            }
        return MemoryGridViewModel(
            mode = mode,
            activityRepository = repository,
            game = MemoryGridGame(sequenceGenerator = generator, mode = mode),
        )
    }

    private class RecordingStorage(
        private val sessionWriteGate: CompletableDeferred<Unit>? = null,
        private val sessionWriteFailure: Exception? = null,
    ) : Storage {
        private val darkTheme = MutableStateFlow<Boolean?>(null)
        private val languageCode = MutableStateFlow<String?>(null)
        private val soundEnabled = MutableStateFlow<Boolean?>(null)
        private val vibrationEnabled = MutableStateFlow<Boolean?>(null)
        private val remindersEnabled = MutableStateFlow<Boolean?>(null)
        private val gameSessionsJson = MutableStateFlow<String?>(null)
        private val dailyTrainingJson = MutableStateFlow<String?>(null)
        private val dailyTrainingProgressJson = MutableStateFlow<String?>(null)
        private val profilePreferencesJson = MutableStateFlow<String?>(null)
        private val baspaRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val kenKozRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val rememberNumberRecords = mutableMapOf<Pair<Int, String>, MutableStateFlow<Int?>>()
        var gameSessionWriteCount = 0
            private set
        val sessionWriteStarted = CompletableDeferred<Unit>()

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
            baspaRecords.getOrPut(mode) { MutableStateFlow(null) }

        override fun getKenKozGameRecord(mode: String): Flow<Int?> =
            kenKozRecords.getOrPut(mode) { MutableStateFlow(null) }

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
            sessionWriteStarted.complete(Unit)
            sessionWriteFailure?.let { throw it }
            sessionWriteGate?.await()
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
