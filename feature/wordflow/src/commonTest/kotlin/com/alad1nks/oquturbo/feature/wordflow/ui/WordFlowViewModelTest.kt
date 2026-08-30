package com.alad1nks.oquturbo.feature.wordflow.ui

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowContent
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowFailure
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPhase
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPrompt
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
class WordFlowViewModelTest {
    @Test
    fun startWaitsForLocaleRecordAndReplayRetainsIt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                repository.recordCompletedSession(
                    GameId.WordFlow,
                    GameModeId.WordFlowContext,
                    variantId = "ru",
                    score = 4,
                    durationMillis = 100,
                    isNewRecord = true,
                )
                val viewModel = WordFlowViewModel("ru-RU", content(), repository)
                viewModel.start()
                assertEquals(WordFlowPhase.Ready, viewModel.uiState.value.game.phase)
                runCurrent()
                assertFalse(viewModel.uiState.value.isRecordLoading)
                assertEquals(4, viewModel.uiState.value.record)

                viewModel.start()
                val wrong = viewModel.uiState.value.game.round!!.wrongAnswer()
                viewModel.selectAnswer(wrong)
                runCurrent()
                viewModel.start()

                assertEquals(WordFlowPhase.Active, viewModel.uiState.value.game.phase)
                assertEquals(4, viewModel.uiState.value.record)
                assertFalse(viewModel.uiState.value.isNewRecord)
                viewModel.abandon()
                runCurrent()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun wrongZeroAndDuplicateInputPersistExactlyOnceWithLocaleSeries() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val viewModel = WordFlowViewModel("kk-KZ", content(), repository)
                runCurrent()
                viewModel.start()
                val wrong = viewModel.uiState.value.game.round!!.wrongAnswer()
                viewModel.selectAnswer(wrong)
                viewModel.selectAnswer(wrong)
                runCurrent()

                assertEquals(1, storage.gameSessionWriteCount)
                val session = repository.observeSessions().first().single()
                assertEquals(GameId.WordFlow, session.game)
                assertEquals(GameModeId.WordFlowContext, session.mode)
                assertEquals("kk", session.variantId)
                assertEquals(0, session.score)
                assertEquals(0, session.correctAnswers)
                assertFalse(session.isNewRecord)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun correctFeedbackDisablesInputAndTimeoutRecordsUpdatedScore() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val viewModel = WordFlowViewModel("en", content(), repository)
                runCurrent()
                viewModel.start()
                val correct = viewModel.uiState.value.game.round!!.prompt.correctAnswer
                viewModel.selectAnswer(correct)
                viewModel.selectAnswer(correct)
                assertEquals(1, viewModel.uiState.value.game.score)
                assertEquals(WordFlowPhase.CorrectFeedback, viewModel.uiState.value.game.phase)
                advanceTimeBy(500)
                runCurrent()
                assertEquals(WordFlowPhase.Active, viewModel.uiState.value.game.phase)

                viewModel.advanceTimerBy(10_000)
                runCurrent()
                assertEquals(WordFlowFailure.Timeout, viewModel.uiState.value.game.failure)
                assertEquals(1, storage.gameSessionWriteCount)
                val session = repository.observeSessions().first().single()
                assertEquals(1, session.score)
                assertEquals(1, session.correctAnswers)
                assertTrue(session.isNewRecord)
                assertEquals(1, repository.observeProgress().first().totalCorrectAnswers)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun replayAndAbandonDuringCorrectFeedbackCancelPendingAdvance() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val viewModel = WordFlowViewModel("en", content(), GameActivityRepository(storage))
                runCurrent()
                viewModel.start()
                viewModel.selectAnswer(viewModel.uiState.value.game.round!!.prompt.correctAnswer)
                assertEquals(WordFlowPhase.CorrectFeedback, viewModel.uiState.value.game.phase)

                viewModel.start()
                val replayPromptId = viewModel.uiState.value.game.round!!.prompt.id
                advanceTimeBy(500)
                runCurrent()
                assertEquals(WordFlowPhase.Active, viewModel.uiState.value.game.phase)
                assertEquals(0, viewModel.uiState.value.game.score)
                assertEquals(0, viewModel.uiState.value.game.correctAnswers)
                assertEquals(replayPromptId, viewModel.uiState.value.game.round!!.prompt.id)

                viewModel.selectAnswer(viewModel.uiState.value.game.round!!.prompt.correctAnswer)
                viewModel.abandon()
                advanceTimeBy(500)
                runCurrent()
                assertEquals(WordFlowPhase.CorrectFeedback, viewModel.uiState.value.game.phase)
                assertEquals(1, viewModel.uiState.value.game.score)
                assertEquals(0, storage.gameSessionWriteCount)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun staleTimerTickAfterReplayCannotReduceNewRound() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val viewModel = WordFlowViewModel("en", content(), GameActivityRepository(storage))
                runCurrent()
                viewModel.start()
                runCurrent()
                advanceTimeBy(99)

                viewModel.start()
                runCurrent()
                val replayState = viewModel.uiState.value
                advanceTimeBy(1)
                runCurrent()

                assertEquals(replayState, viewModel.uiState.value)
                assertEquals(10_000L, viewModel.uiState.value.game.round!!.remainingTimeMillis)
                assertEquals(0, storage.gameSessionWriteCount)
                viewModel.abandon()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun suspendedOldPersistenceCompletesOnceWithoutMutatingReplay() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val sessionWriteGate = CompletableDeferred<Unit>()
                val storage = RecordingStorage(sessionWriteGate)
                val repository = GameActivityRepository(storage)
                val viewModel = WordFlowViewModel("en", content(), repository)
                runCurrent()
                viewModel.start()
                viewModel.selectAnswer(viewModel.uiState.value.game.round!!.prompt.correctAnswer)
                advanceTimeBy(500)
                runCurrent()
                viewModel.advanceTimerBy(10_000)
                runCurrent()
                assertTrue(storage.sessionWriteStarted.isCompleted)
                assertEquals(0, storage.gameSessionWriteCount)

                viewModel.start()
                runCurrent()
                val replayState = viewModel.uiState.value
                assertEquals(WordFlowPhase.Active, replayState.game.phase)
                assertEquals(0, replayState.game.score)
                assertEquals(1, replayState.record)
                assertFalse(replayState.isNewRecord)

                sessionWriteGate.complete(Unit)
                runCurrent()

                assertEquals(1, storage.gameSessionWriteCount)
                val session = repository.observeSessions().first().single()
                assertEquals(GameId.WordFlow, session.game)
                assertEquals(GameModeId.WordFlowContext, session.mode)
                assertEquals("en", session.variantId)
                assertEquals(1, session.score)
                assertEquals(1, session.correctAnswers)
                assertTrue(session.isNewRecord)
                assertEquals(replayState, viewModel.uiState.value)
                viewModel.abandon()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun abandonDoesNotPersistPartialAttempt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val viewModel = WordFlowViewModel("en", content(), GameActivityRepository(storage))
                runCurrent()
                viewModel.start()
                viewModel.abandon()
                viewModel.advanceTimerBy(10_000)
                runCurrent()
                assertEquals(0, storage.gameSessionWriteCount)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun tieDoesNotCreateRecord() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                repository.recordCompletedSession(
                    GameId.WordFlow,
                    GameModeId.WordFlowContext,
                    variantId = "en",
                    score = 1,
                    durationMillis = 100,
                    isNewRecord = true,
                )
                val writesBeforeAttempt = storage.gameSessionWriteCount
                val viewModel = WordFlowViewModel("en", content(), repository)
                runCurrent()
                viewModel.start()
                viewModel.selectAnswer(viewModel.uiState.value.game.round!!.prompt.correctAnswer)
                advanceTimeBy(500)
                runCurrent()
                viewModel.advanceTimerBy(10_000)
                runCurrent()

                assertFalse(viewModel.uiState.value.isNewRecord)
                assertEquals(1, viewModel.uiState.value.record)
                assertEquals(writesBeforeAttempt + 1, storage.gameSessionWriteCount)
                assertFalse(repository.observeSessions().first().last().isNewRecord)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun com.alad1nks.oquturbo.feature.wordflow.model.WordFlowRound.wrongAnswer(): String =
        choices.first { it != prompt.correctAnswer }

    private fun content() =
        WordFlowContent(
            WordFlowTier.entries.flatMap { tier ->
                (1..6).map { index ->
                    WordFlowPrompt(
                        id = "${tier.name}-$index",
                        tier = tier,
                        sentenceTemplate = "Answer %1\$s.",
                        correctAnswer = "yes-$index",
                        wrongAnswers = listOf("no-$index", "maybe-$index"),
                    )
                }
            },
        )

    private class RecordingStorage(
        private val sessionWriteGate: CompletableDeferred<Unit>? = null,
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
        val sessionWriteStarted = CompletableDeferred<Unit>()
        var gameSessionWriteCount = 0
            private set

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
            sessionWriteStarted.complete(Unit)
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
