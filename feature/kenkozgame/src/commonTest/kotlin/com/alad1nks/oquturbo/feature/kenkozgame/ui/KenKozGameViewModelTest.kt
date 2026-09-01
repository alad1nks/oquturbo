package com.alad1nks.oquturbo.feature.kenkozgame.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.KenKozGameRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class KenKozGameViewModelTest {
    @Test
    fun verifiedNewRecordIsPendingAndPreservesPayloadForEveryMode() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                modeMappings.forEach { (mode, expectedMode) ->
                    val activityWriteGate = CompletableDeferred<Unit>()
                    val storage = RecordingStorage(activityWriteGate = activityWriteGate)
                    val activityRepository = GameActivityRepository(storage)
                    val viewModel = createViewModel(mode = mode, storage = storage)
                    runCurrent()

                    val selectedAnswer = finishAttempt(viewModel, score = 1)
                    runCurrent()

                    val pendingState = viewModel.uiState.value
                    assertEquals(KenKozGameUiState.Phase.Mistake, pendingState.phase)
                    assertEquals(selectedAnswer, pendingState.selectedAnswer)
                    assertEquals(1, pendingState.score)
                    assertEquals(0, pendingState.record)
                    assertFalse(pendingState.isNewRecord)
                    assertTrue(storage.activityWriteStarted.isCompleted)
                    assertEquals(0, storage.gameSessionWriteCount)
                    assertEquals(0, storage.kenKozRecordWriteCount)

                    activityWriteGate.complete(Unit)
                    runCurrent()

                    val session = activityRepository.observeSessions().first().single()
                    assertEquals(GameId.WideEye, session.game)
                    assertEquals(expectedMode, session.mode)
                    assertNull(session.variantId)
                    assertEquals(1, session.score)
                    assertEquals(1, session.correctAnswers)
                    assertTrue(session.durationMillis >= 0)
                    assertTrue(session.isNewRecord)
                    assertEquals(1, storage.gameSessionWriteCount)
                    assertEquals(1, storage.kenKozRecordWriteCount)
                    assertEquals(1, storage.kenKozRecords.getValue(mode.name).value)
                    assertEquals(1, viewModel.uiState.value.record)
                    assertTrue(viewModel.uiState.value.isNewRecord)
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun zeroLowerTieAndRepositoryRejectedClaimsNeverShowNewRecord() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val cases =
                    listOf(
                        RecordCase(score = 0, legacyRecord = 0),
                        RecordCase(score = 1, legacyRecord = 2),
                        RecordCase(score = 1, legacyRecord = 1),
                        RecordCase(score = 1, legacyRecord = 0, activityRecord = 10),
                    )
                cases.forEach { case ->
                    val storage = RecordingStorage()
                    storage.installKenKozRecord(KenKozGameMode.Words, case.legacyRecord)
                    val activityRepository = GameActivityRepository(storage)
                    case.activityRecord?.let { existingRecord ->
                        activityRepository.recordCompletedSession(
                            game = GameId.WideEye,
                            mode = GameModeId.WideEyeWords,
                            score = existingRecord,
                            durationMillis = 1,
                            isNewRecord = true,
                        )
                    }
                    storage.resetWriteCounts()
                    val viewModel = createViewModel(mode = KenKozGameMode.Words, storage = storage)
                    runCurrent()

                    finishAttempt(viewModel, score = case.score)
                    runCurrent()

                    val session = activityRepository.observeSessions().first().last()
                    assertFalse(session.isNewRecord)
                    assertFalse(viewModel.uiState.value.isNewRecord)
                    assertEquals(case.legacyRecord, viewModel.uiState.value.record)
                    assertEquals(1, storage.gameSessionWriteCount)
                    assertEquals(0, storage.kenKozRecordWriteCount)
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun replaySynchronouslyClearsVerifiedNewRecord() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val viewModel = createViewModel(mode = KenKozGameMode.Characters, storage = storage)
                runCurrent()
                finishAttempt(viewModel, score = 1)
                runCurrent()
                assertTrue(viewModel.uiState.value.isNewRecord)

                viewModel.start()

                val replayState = viewModel.uiState.value
                assertFalse(replayState.isNewRecord)
                assertEquals(0, replayState.score)
                assertEquals(KenKozGameUiState.Phase.Showing, replayState.phase)
                finishCurrentRoundWithMistake(viewModel)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun staleEarlierActivityCompletionCannotMutateReplayResult() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val activityWriteGate = CompletableDeferred<Unit>()
                val storage = RecordingStorage(activityWriteGate = activityWriteGate)
                val activityRepository = GameActivityRepository(storage)
                val viewModel = createViewModel(mode = KenKozGameMode.WideLine, storage = storage)
                runCurrent()

                finishAttempt(viewModel, score = 2)
                runCurrent()
                assertFalse(viewModel.uiState.value.isNewRecord)

                val laterSelectedAnswer = finishAttempt(viewModel, score = 1)
                runCurrent()
                val laterPendingState = viewModel.uiState.value
                assertEquals(1, laterPendingState.score)
                assertEquals(0, laterPendingState.record)
                assertEquals(laterSelectedAnswer, laterPendingState.selectedAnswer)
                assertFalse(laterPendingState.isNewRecord)

                activityWriteGate.complete(Unit)
                runCurrent()

                val sessions = activityRepository.observeSessions().first()
                assertEquals(2, sessions.size)
                assertTrue(sessions.first().isNewRecord)
                assertFalse(sessions.last().isNewRecord)
                val finalState = viewModel.uiState.value
                assertEquals(KenKozGameUiState.Phase.Mistake, finalState.phase)
                assertEquals(1, finalState.score)
                assertEquals(0, finalState.record)
                assertEquals(laterSelectedAnswer, finalState.selectedAnswer)
                assertFalse(finalState.isNewRecord)
                assertFalse(finalState.isTrainingCompletionReady)
                assertNull(finalState.trainingNextEntry)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun staleTrainingCompletionCannotMutateReplayedActiveAttempt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val dailyWriteGate = CompletableDeferred<Unit>()
                val storage = RecordingStorage(dailyWriteGate = dailyWriteGate)
                val trainingEntry = storage.installTrainingPlan(KenKozGameMode.FindDifference)
                val viewModel =
                    createViewModel(
                        mode = KenKozGameMode.FindDifference,
                        storage = storage,
                        trainingEntry = trainingEntry,
                    )
                runCurrent()

                finishAttempt(viewModel, score = 1)
                runCurrent()
                assertTrue(storage.dailyWriteStarted.isCompleted)
                viewModel.start()
                val replayState = viewModel.uiState.value
                assertEquals(KenKozGameUiState.Phase.Showing, replayState.phase)
                assertFalse(replayState.isTrainingCompletionReady)
                assertNull(replayState.trainingNextEntry)

                dailyWriteGate.complete(Unit)
                runCurrent()

                val finalState = viewModel.uiState.value
                assertEquals(KenKozGameUiState.Phase.Showing, finalState.phase)
                assertFalse(finalState.isTrainingCompletionReady)
                assertNull(finalState.trainingNextEntry)
                finishCurrentRoundWithMistake(viewModel)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun activityFailureKeepsOrdinaryResultUsableWithoutAcknowledgement() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage(failActivityWrites = true)
                val viewModel = createViewModel(mode = KenKozGameMode.Characters, storage = storage)
                runCurrent()

                val selectedAnswer = finishAttempt(viewModel, score = 1)
                runCurrent()

                val resultState = viewModel.uiState.value
                assertEquals(KenKozGameUiState.Phase.Mistake, resultState.phase)
                assertEquals(selectedAnswer, resultState.selectedAnswer)
                assertEquals(1, resultState.score)
                assertEquals(0, resultState.record)
                assertFalse(resultState.isNewRecord)
                assertEquals(1, storage.gameSessionWriteAttemptCount)
                assertEquals(0, storage.gameSessionWriteCount)
                assertEquals(0, storage.kenKozRecordWriteCount)

                viewModel.start()
                assertEquals(KenKozGameUiState.Phase.Showing, viewModel.uiState.value.phase)
                assertFalse(viewModel.uiState.value.isNewRecord)
                finishCurrentRoundWithMistake(viewModel)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun dailyTrainingCompletionIsIndependentFromActivityFailure() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage(failActivityWrites = true)
                val trainingEntry = storage.installTrainingPlan(KenKozGameMode.Words)
                val viewModel =
                    createViewModel(
                        mode = KenKozGameMode.Words,
                        storage = storage,
                        trainingEntry = trainingEntry,
                    )
                runCurrent()

                finishAttempt(viewModel, score = trainingEntry.requiredScore)
                runCurrent()

                val resultState = viewModel.uiState.value
                assertEquals(KenKozGameUiState.Phase.Mistake, resultState.phase)
                assertFalse(resultState.isNewRecord)
                assertEquals(0, resultState.record)
                assertTrue(resultState.isTrainingCompletionReady)
                assertEquals(GameId.NumberSprint, assertNotNull(resultState.trainingNextEntry).game)
                assertEquals(1, storage.gameSessionWriteAttemptCount)
                assertEquals(0, storage.gameSessionWriteCount)
                assertEquals(1, storage.dailyTrainingWriteCount)
                assertEquals(0, storage.kenKozRecordWriteCount)

                var continuedEntry: DailyTrainingEntry? = null
                var continuationCount = 0
                viewModel.continueTraining {
                    continuationCount++
                    continuedEntry = it
                }
                viewModel.continueTraining { continuationCount++ }
                assertEquals(1, continuationCount)
                assertEquals(GameId.NumberSprint, assertNotNull(continuedEntry).game)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private suspend fun TestScope.finishAttempt(
        viewModel: KenKozGameViewModel,
        score: Int,
    ): String {
        viewModel.start()
        repeat(score) {
            advanceToAnswering(viewModel)
            viewModel.selectAnswer(viewModel.uiState.value.correctAnswer)
        }
        advanceToAnswering(viewModel)
        val state = viewModel.uiState.value
        val wrongAnswer = state.answers.first { it != state.correctAnswer }
        viewModel.selectAnswer(wrongAnswer)
        return wrongAnswer
    }

    private suspend fun TestScope.finishCurrentRoundWithMistake(viewModel: KenKozGameViewModel) {
        advanceToAnswering(viewModel)
        val state = viewModel.uiState.value
        viewModel.selectAnswer(state.answers.first { it != state.correctAnswer })
        runCurrent()
    }

    private suspend fun TestScope.advanceToAnswering(viewModel: KenKozGameViewModel) {
        advanceTimeBy(ROUND_DURATION_MILLIS)
        runCurrent()
        assertEquals(KenKozGameUiState.Phase.Answering, viewModel.uiState.value.phase)
    }

    private fun createViewModel(
        mode: KenKozGameMode,
        storage: RecordingStorage,
        trainingEntry: DailyTrainingEntry? = null,
    ) = KenKozGameViewModel(
        mode = mode,
        characters = listOf("A", "B", "C", "D"),
        words = listOf("alpha", "bravo", "charlie", "delta"),
        differencePairs = listOf("same" to "different"),
        trainingEntryId = trainingEntry?.id,
        trainingRequiredScore = trainingEntry?.requiredScore,
        kenKozGameRepository = KenKozGameRepository(storage),
        gameActivityRepository = GameActivityRepository(storage),
        dailyTrainingRepository = DailyTrainingRepository(storage),
    )

    private data class RecordCase(
        val score: Int,
        val legacyRecord: Int,
        val activityRecord: Int? = null,
    )

    private class RecordingStorage(
        private val activityWriteGate: CompletableDeferred<Unit>? = null,
        private val dailyWriteGate: CompletableDeferred<Unit>? = null,
        private val failActivityWrites: Boolean = false,
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
        val kenKozRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val rememberNumberRecords = mutableMapOf<Pair<Int, String>, MutableStateFlow<Int?>>()
        var gameSessionWriteAttemptCount = 0
            private set
        var gameSessionWriteCount = 0
            private set
        var dailyTrainingWriteCount = 0
            private set
        var kenKozRecordWriteCount = 0
            private set
        val activityWriteStarted = CompletableDeferred<Unit>()
        val dailyWriteStarted = CompletableDeferred<Unit>()

        fun installKenKozRecord(
            mode: KenKozGameMode,
            record: Int,
        ) {
            kenKozRecords.getOrPut(mode.name) { MutableStateFlow(null) }.value = record
        }

        fun installTrainingPlan(mode: KenKozGameMode): DailyTrainingEntry {
            val epochDay = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY
            val wideEyeEntry =
                trainingEntry(
                    epochDay = epochDay,
                    game = GameId.WideEye,
                    mode = mode.activityMode,
                    requiredScore = 1,
                )
            dailyTrainingJson.value =
                Json.encodeToString(
                    DailyTrainingPlan(
                        epochDay = epochDay,
                        entries =
                            listOf(
                                wideEyeEntry,
                                trainingEntry(
                                    epochDay,
                                    GameId.NumberSprint,
                                    GameModeId.NumberSprintClassic,
                                    requiredScore = 5,
                                ),
                                trainingEntry(
                                    epochDay,
                                    GameId.DontTap,
                                    GameModeId.DontTapMath,
                                    requiredScore = 8,
                                ),
                            ),
                    ),
                )
            return wideEyeEntry
        }

        fun resetWriteCounts() {
            gameSessionWriteAttemptCount = 0
            gameSessionWriteCount = 0
            dailyTrainingWriteCount = 0
            kenKozRecordWriteCount = 0
        }

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

        override fun getRememberNumberRecord(
            maxLength: Int,
            availableDigits: String,
        ): Flow<Int?> =
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
            activityWriteStarted.complete(Unit)
            gameSessionWriteAttemptCount++
            if (failActivityWrites) throw IllegalStateException("Activity write failed")
            activityWriteGate?.await()
            gameSessionWriteCount++
            gameSessionsJson.value = value
        }

        override suspend fun setDailyTrainingJson(value: String) {
            dailyWriteStarted.complete(Unit)
            dailyWriteGate?.await()
            dailyTrainingWriteCount++
            dailyTrainingJson.value = value
        }

        override suspend fun setDailyTrainingProgressJson(value: String) {
            dailyTrainingProgressJson.value = value
        }

        override suspend fun setProfilePreferencesJson(value: String) {
            profilePreferencesJson.value = value
        }

        override suspend fun setBaspaGameRecord(
            mode: String,
            record: Int,
        ) {
            baspaRecords.getOrPut(mode) { MutableStateFlow(null) }.value = record
        }

        override suspend fun setKenKozGameRecord(
            mode: String,
            record: Int,
        ) {
            kenKozRecordWriteCount++
            kenKozRecords.getOrPut(mode) { MutableStateFlow(null) }.value = record
        }

        override suspend fun setRememberNumberRecord(
            maxLength: Int,
            availableDigits: String,
            record: Int,
        ) {
            rememberNumberRecords
                .getOrPut(maxLength to availableDigits) { MutableStateFlow(null) }
                .value = record
        }

        private fun trainingEntry(
            epochDay: Long,
            game: GameId,
            mode: GameModeId,
            requiredScore: Int,
        ) = DailyTrainingEntry(
            id = epochDay.toString() + ":" + game.name + ":" + mode.name,
            game = game,
            mode = mode,
            requiredScore = requiredScore,
        )

        private companion object {
            const val MILLIS_PER_DAY = 86_400_000L
        }
    }

    private companion object {
        const val ROUND_DURATION_MILLIS = 2_000L
        val modeMappings =
            mapOf(
                KenKozGameMode.Characters to GameModeId.WideEyeCharacters,
                KenKozGameMode.Words to GameModeId.WideEyeWords,
                KenKozGameMode.FindDifference to GameModeId.WideEyeFindDifference,
                KenKozGameMode.WideLine to GameModeId.WideEyeWideLine,
            )

        val KenKozGameMode.activityMode: GameModeId
            get() = modeMappings.getValue(this)
    }
}
