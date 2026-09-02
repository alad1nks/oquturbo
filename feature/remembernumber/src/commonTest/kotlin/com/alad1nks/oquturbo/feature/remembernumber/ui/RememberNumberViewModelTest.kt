package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.data.repository.RememberNumberRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RememberNumberViewModelTest {
    @Test
    fun acknowledgementUsesRepositoryReturnedTrueAndFalse() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val trueStorage = TestStorage()
                val trueViewModel = createViewModel(trueStorage)
                runCurrent()

                finishAttempt(trueViewModel, score = 1)
                runCurrent()

                val trueState = assertIs<RememberNumberUiState.Mistake>(trueViewModel.uiState.value)
                assertTrue(trueState.isNewRecord)
                assertEquals(1, trueState.record)
                val acceptedSession = GameActivityRepository(trueStorage).observeSessions().first().single()
                assertEquals(GameId.NumberSprint, acceptedSession.game)
                assertEquals(GameModeId.NumberSprintCustom, acceptedSession.mode)
                assertEquals("length:1;digits:0", acceptedSession.variantId)
                assertTrue(acceptedSession.isNewRecord)

                val falseStorage = TestStorage()
                val falseRepository = GameActivityRepository(falseStorage)
                falseRepository.recordCompletedSession(
                    game = GameId.NumberSprint,
                    mode = GameModeId.NumberSprintCustom,
                    variantId = "length:1;digits:0",
                    score = 5,
                    durationMillis = 1,
                    isNewRecord = true,
                )
                val falseViewModel = createViewModel(falseStorage)
                runCurrent()

                finishAttempt(falseViewModel, score = 1)
                runCurrent()

                val falseState = assertIs<RememberNumberUiState.Mistake>(falseViewModel.uiState.value)
                assertFalse(falseState.isNewRecord)
                assertEquals(1, falseState.record)
                assertFalse(falseRepository.observeSessions().first().last().isNewRecord)
                trueViewModel.viewModelScope.cancel()
                falseViewModel.viewModelScope.cancel()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun restartSynchronouslyClearsAcknowledgement() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel = createViewModel(TestStorage())
                runCurrent()
                finishAttempt(viewModel, score = 1)
                runCurrent()
                assertTrue(assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value).isNewRecord)

                viewModel.start()

                val restarted = assertIs<RememberNumberUiState.Reading>(viewModel.uiState.value)
                assertEquals(0, restarted.score)
                viewModel.viewModelScope.cancel()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun staleTrueAndFalseCompletionsCannotMutateLaterAttempt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                verifyStaleCompletion(firstScore = 1, laterScore = 0, expectedLaterNewRecord = false)
                verifyStaleCompletion(firstScore = 0, laterScore = 1, expectedLaterNewRecord = true)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private suspend fun TestScope.verifyStaleCompletion(
        firstScore: Int,
        laterScore: Int,
        expectedLaterNewRecord: Boolean,
    ) {
        val storage = TestStorage()
        storage.blockWrites = true
        val repository = GameActivityRepository(storage)
        val viewModel = createViewModel(storage)
        runCurrent()

        finishAttempt(viewModel, score = firstScore)
        runCurrent()
        assertTrue(storage.writeStarted.isCompleted)
        assertFalse(assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value).isNewRecord)

        finishAttempt(viewModel, score = laterScore)
        runCurrent()
        val laterPending = assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value)
        assertEquals(laterScore, laterPending.score)
        assertFalse(laterPending.isNewRecord)

        storage.writeGate.complete(Unit)
        runCurrent()

        val sessions = repository.observeSessions().first()
        assertEquals(2, sessions.size)
        assertEquals(firstScore > 0, sessions.first().isNewRecord)
        assertEquals(expectedLaterNewRecord, sessions.last().isNewRecord)
        val finalState = assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value)
        assertEquals(laterScore, finalState.score)
        assertEquals(expectedLaterNewRecord, finalState.isNewRecord)
        viewModel.viewModelScope.cancel()
    }

    private fun createViewModel(storage: Storage) =
        RememberNumberViewModel(
            maxLength = 1,
            availableDigits = "0",
            trainingEntryId = null,
            trainingRequiredScore = null,
            rememberNumberRepository = RememberNumberRepository(storage),
            gameActivityRepository = GameActivityRepository(storage),
            dailyTrainingRepository = DailyTrainingRepository(storage),
        )

    private fun TestScope.finishAttempt(
        viewModel: RememberNumberViewModel,
        score: Int,
    ) {
        viewModel.start()
        advanceTimeBy(1_700)
        runCurrent()
        repeat(score) {
            viewModel.writeText("0")
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
        }
        viewModel.writeText("1")
        runCurrent()
    }

    private class TestStorage : Storage {
        private val nullableBoolean = MutableStateFlow<Boolean?>(null)
        private val nullableString = MutableStateFlow<String?>(null)
        private val gameSessions = MutableStateFlow<String?>(null)
        private val rememberRecord = MutableStateFlow<Int?>(null)
        var blockWrites = false
        val writeStarted = CompletableDeferred<Unit>()
        val writeGate = CompletableDeferred<Unit>()

        override fun getDarkTheme(): Flow<Boolean?> = nullableBoolean

        override fun getLanguageCode(): Flow<String?> = nullableString

        override fun getSoundEnabled(): Flow<Boolean?> = nullableBoolean

        override fun getVibrationEnabled(): Flow<Boolean?> = nullableBoolean

        override fun getRemindersEnabled(): Flow<Boolean?> = nullableBoolean

        override fun getGameSessionsJson(): Flow<String?> = gameSessions

        override fun getDailyTrainingJson(): Flow<String?> = nullableString

        override fun getDailyTrainingProgressJson(): Flow<String?> = nullableString

        override fun getProfilePreferencesJson(): Flow<String?> = nullableString

        override fun getBaspaGameRecord(mode: String): Flow<Int?> = MutableStateFlow(null)

        override fun getKenKozGameRecord(mode: String): Flow<Int?> = MutableStateFlow(null)

        override fun getRememberNumberRecord(maxLength: Int, availableDigits: String): Flow<Int?> = rememberRecord

        override suspend fun setDarkTheme(value: Boolean) = Unit

        override suspend fun setLanguageCode(value: String) = Unit

        override suspend fun setSoundEnabled(value: Boolean) = Unit

        override suspend fun setVibrationEnabled(value: Boolean) = Unit

        override suspend fun setRemindersEnabled(value: Boolean) = Unit

        override suspend fun setGameSessionsJson(value: String) {
            writeStarted.complete(Unit)
            if (blockWrites) writeGate.await()
            gameSessions.value = value
        }

        override suspend fun setDailyTrainingJson(value: String) = Unit

        override suspend fun setDailyTrainingProgressJson(value: String) = Unit

        override suspend fun setProfilePreferencesJson(value: String) = Unit

        override suspend fun setBaspaGameRecord(mode: String, record: Int) = Unit

        override suspend fun setKenKozGameRecord(mode: String, record: Int) = Unit

        override suspend fun setRememberNumberRecord(maxLength: Int, availableDigits: String, record: Int) {
            rememberRecord.value = record
        }
    }
}
