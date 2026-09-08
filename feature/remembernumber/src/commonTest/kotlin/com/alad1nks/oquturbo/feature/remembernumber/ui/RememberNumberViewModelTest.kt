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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

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

    @Test
    fun durationIsImmediateFrozenAndPersistedAcrossModesAndTraining() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                listOf(
                    Triple(4, "0123456789", GameModeId.NumberSprintClassic),
                    Triple(4, "01", GameModeId.NumberSprintBinary),
                    Triple(10, "0123456789", GameModeId.NumberSprintCustom),
                ).forEach { (length, digits, mode) ->
                    listOf<Int?>(null, 1).forEach { required ->
                        listOf(0, 1).forEach { score ->
                            val storage = TestStorage().apply { blockWrites = true }
                            val clock = kotlin.time.TestTimeSource()
                            val vm = createViewModel(storage, clock, length, digits, required)
                            runCurrent()
                            assertIs<RememberNumberUiState.Initial>(vm.uiState.value)
                            vm.start()
                            repeat(score) {
                                val answer = assertIs<RememberNumberUiState.Reading>(vm.uiState.value).text
                                advanceTimeBy(1_700)
                                runCurrent()
                                vm.writeText(answer)
                                runCurrent()
                            }
                            advanceTimeBy(1_700)
                            runCurrent()
                            assertIs<RememberNumberUiState.Writing>(vm.uiState.value)
                            clock += 65_999.milliseconds
                            vm.writeText("x".repeat(length))
                            runCurrent()
                            val pending = assertIs<RememberNumberUiState.Mistake>(vm.uiState.value)
                            assertEquals(65_999L, pending.completedDurationMillis)
                            assertEquals(required == null, pending.isTrainingResultReady)
                            var continues = 0
                            vm.continueTraining { continues++ }
                            assertEquals(0, continues)
                            clock += 100_000.milliseconds
                            storage.writeGate.complete(Unit)
                            runCurrent()
                            val result = assertIs<RememberNumberUiState.Mistake>(vm.uiState.value)
                            assertEquals(pending.completedDurationMillis, result.completedDurationMillis)
                            assertTrue(result.isTrainingResultReady)
                            val session = GameActivityRepository(storage).observeSessions().first().single()
                            assertEquals(result.completedDurationMillis, session.durationMillis)
                            assertEquals(score, session.score)
                            assertEquals(score, session.correctAnswers)
                            assertEquals(mode, session.mode)
                            assertEquals(if (length == 10) "length:10;digits:0123456789" else null, session.variantId)
                            vm.continueTraining { continues++ }
                            vm.continueTraining { continues++ }
                            assertEquals(if (required != null && score >= required) 1 else 0, continues)
                            vm.start()
                            assertIs<RememberNumberUiState.Reading>(vm.uiState.value)
                            advanceTimeBy(1_700)
                            runCurrent()
                            clock += 999.milliseconds
                            vm.writeText("x".repeat(length))
                            runCurrent()
                            assertEquals(
                                999L,
                                assertIs<RememberNumberUiState.Mistake>(vm.uiState.value).completedDurationMillis,
                            )
                            assertEquals(2, GameActivityRepository(storage).observeSessions().first().size)
                            vm.viewModelScope.cancel()
                        }
                    }
                }
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun slowRecordReadDoesNotDelayResultOrOverwriteLaterDuration() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = TestStorage()
                val clock = kotlin.time.TestTimeSource()
                val vm = createViewModel(storage, clock)
                runCurrent()
                storage.blockRecordReads = true
                finishAttempt(vm, 0, beforeFailure = { clock += 999.milliseconds })
                assertEquals(999L, assertIs<RememberNumberUiState.Mistake>(vm.uiState.value).completedDurationMillis)
                assertFalse(storage.writeStarted.isCompleted)
                finishAttempt(vm, 1, beforeFailure = { clock += 65_999.milliseconds })
                assertEquals(65_999L, assertIs<RememberNumberUiState.Mistake>(vm.uiState.value).completedDurationMillis)
                storage.recordReadGate.complete(Unit)
                runCurrent()
                val result = assertIs<RememberNumberUiState.Mistake>(vm.uiState.value)
                assertEquals(65_999L, result.completedDurationMillis)
                assertEquals(1, result.score)
                assertEquals(
                    listOf(999L, 65_999L),
                    GameActivityRepository(storage).observeSessions().first().map { it.durationMillis },
                )
                vm.viewModelScope.cancel()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun incompleteAttemptDoesNotRecordSession() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = TestStorage()
                val vm = createViewModel(storage)
                runCurrent()
                vm.start()
                advanceTimeBy(1_700)
                runCurrent()
                vm.viewModelScope.cancel()
                assertTrue(GameActivityRepository(storage).observeSessions().first().isEmpty())
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
        val clock = kotlin.time.TestTimeSource()
        val viewModel = createViewModel(storage, clock)
        runCurrent()

        finishAttempt(viewModel, score = firstScore)
        val firstDuration = assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value).completedDurationMillis
        runCurrent()
        assertTrue(storage.writeStarted.isCompleted)
        assertFalse(assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value).isNewRecord)

        finishAttempt(viewModel, score = laterScore, beforeFailure = { clock += 5_000.milliseconds })
        runCurrent()
        val laterPending = assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value)
        assertEquals(laterScore, laterPending.score)
        assertEquals(0L, firstDuration)
        assertEquals(5_000L, laterPending.completedDurationMillis)
        assertFalse(laterPending.isNewRecord)

        storage.writeGate.complete(Unit)
        runCurrent()

        val sessions = repository.observeSessions().first()
        assertEquals(2, sessions.size)
        assertEquals(firstScore > 0, sessions.first().isNewRecord)
        assertEquals(expectedLaterNewRecord, sessions.last().isNewRecord)
        val finalState = assertIs<RememberNumberUiState.Mistake>(viewModel.uiState.value)
        assertEquals(laterScore, finalState.score)
        assertEquals(laterPending.completedDurationMillis, finalState.completedDurationMillis)
        assertEquals(listOf(0L, 5_000L), sessions.map { it.durationMillis })
        assertEquals(expectedLaterNewRecord, finalState.isNewRecord)
        viewModel.viewModelScope.cancel()
    }

    private fun createViewModel(
        storage: Storage,
        timeSource: TimeSource = TimeSource.Monotonic,
        maxLength: Int = 1,
        availableDigits: String = "0",
        trainingRequiredScore: Int? = null,
    ) =
        RememberNumberViewModel(
            maxLength = maxLength,
            availableDigits = availableDigits,
            trainingEntryId = trainingRequiredScore?.let { "duration-test" },
            trainingRequiredScore = trainingRequiredScore,
            rememberNumberRepository = RememberNumberRepository(storage),
            gameActivityRepository = GameActivityRepository(storage),
            dailyTrainingRepository = DailyTrainingRepository(storage),
            timeSource = timeSource,
        )

    private fun TestScope.finishAttempt(
        viewModel: RememberNumberViewModel,
        score: Int,
        beforeFailure: () -> Unit = {},
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
        beforeFailure()
        viewModel.writeText("1")
        runCurrent()
    }

    private class TestStorage : Storage {
        private val nullableBoolean = MutableStateFlow<Boolean?>(null)
        private val nullableString = MutableStateFlow<String?>(null)
        private val gameSessions = MutableStateFlow<String?>(null)
        private val rememberRecord = MutableStateFlow<Int?>(null)
        var blockWrites = false
        var blockRecordReads = false
        val recordReadGate = CompletableDeferred<Unit>()
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

        override fun getRememberNumberRecord(maxLength: Int, availableDigits: String): Flow<Int?> =
            flow {
                if (blockRecordReads) recordReadGate.await()
                emitAll(rememberRecord)
            }

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
