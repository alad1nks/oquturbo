package com.alad1nks.oquturbo.feature.baspagame.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.BaspaGameRepository
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameContent
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.feature.baspagame.model.Category
import com.alad1nks.oquturbo.feature.baspagame.model.GameColor
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
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class BaspaGameViewModelTest {
    @Test
    fun incorrectTapWinsOverLateTimeoutAndRecordsOrdinarySessionOnce() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val activityRepository = GameActivityRepository(storage)
                val viewModel = createViewModel(storage = storage, stimulusShouldMatch = false)
                runCurrent()
                startAndShowStimulus(viewModel)

                viewModel.tap()
                viewModel.onStimulusTimeout()
                runCurrent()

                assertEquals(BaspaMistakeReason.IncorrectTap, viewModel.uiState.value.mistakeReason)
                assertEquals(BaspaGameUiState.Phase.Mistake, viewModel.uiState.value.phase)
                assertEquals(1, storage.gameSessionWriteCount)
                val session = activityRepository.observeSessions().first().single()
                assertEquals(GameId.DontTap, session.game)
                assertEquals(GameModeId.DontTapMath, session.mode)
                assertEquals(0, session.score)
                assertEquals(0, session.correctAnswers)
                assertFalse(session.isNewRecord)
                assertEquals(0, storage.dailyTrainingWriteCount)
                assertEquals(0, storage.baspaRecordWriteCount)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun timeoutWinsOverLateTapAndPreservesTrainingPersistence() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val trainingEntry = storage.installTrainingPlan()
                val activityRepository = GameActivityRepository(storage)
                val viewModel =
                    createViewModel(
                        storage = storage,
                        stimulusShouldMatch = true,
                        trainingEntry = trainingEntry,
                    )
                runCurrent()
                startAndShowStimulus(viewModel)

                viewModel.tap()
                advanceTimeBy(STIMULUS_GAP_MILLIS)
                runCurrent()
                assertEquals(1, viewModel.uiState.value.score)
                assertTrue(viewModel.uiState.value.shouldTap)

                viewModel.onStimulusTimeout()
                viewModel.tap()
                runCurrent()

                val state = viewModel.uiState.value
                assertEquals(BaspaMistakeReason.MissedMatch, state.mistakeReason)
                assertEquals(BaspaGameUiState.Phase.Mistake, state.phase)
                assertTrue(state.isTrainingCompletionReady)
                assertEquals(GameId.NumberSprint, assertNotNull(state.trainingNextEntry).game)
                assertEquals(1, storage.gameSessionWriteCount)
                assertEquals(1, storage.dailyTrainingWriteCount)
                assertEquals(1, storage.baspaRecordWriteCount)
                assertEquals(1, storage.baspaRecords.getValue(BaspaGameMode.Math.name).value)
                val session = activityRepository.observeSessions().first().single()
                assertEquals(1, session.score)
                assertEquals(1, session.correctAnswers)
                assertTrue(session.isNewRecord)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun textColorStimulusRetainsLocalizedDisplayedColorNameForFeedback() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val viewModel =
                    createViewModel(
                        storage = storage,
                        stimulusShouldMatch = true,
                        mode = BaspaGameMode.TextColor,
                    )
                runCurrent()
                startAndShowStimulus(viewModel)

                val state = viewModel.uiState.value
                val expectedColorName =
                    testContent(stimulusShouldMatch = true)
                        .colors
                        .single { it.id == state.stimulusColorId }
                        .name
                assertEquals(expectedColorName, state.stimulusColorName)
                if (state.shouldTap) {
                    viewModel.onStimulusTimeout()
                } else {
                    viewModel.tap()
                }
                runCurrent()
            } finally {
                Dispatchers.resetMain()
            }
        }

    private suspend fun TestScope.startAndShowStimulus(viewModel: BaspaGameViewModel) {
        viewModel.togglePause()
        advanceTimeBy(STIMULUS_GAP_MILLIS)
        runCurrent()
        assertEquals(BaspaGameUiState.Phase.Playing, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.stimulus.isNotEmpty())
    }

    private fun createViewModel(
        storage: RecordingStorage,
        stimulusShouldMatch: Boolean,
        trainingEntry: DailyTrainingEntry? = null,
        mode: BaspaGameMode = BaspaGameMode.Math,
    ) =
        BaspaGameViewModel(
            mode = mode,
            content = testContent(stimulusShouldMatch),
            trainingEntryId = trainingEntry?.id,
            trainingRequiredScore = trainingEntry?.requiredScore,
            repository = BaspaGameRepository(storage),
            gameActivityRepository = GameActivityRepository(storage),
            dailyTrainingRepository = DailyTrainingRepository(storage),
        )

    private fun testContent(stimulusShouldMatch: Boolean) =
        BaspaGameContent(
            categories =
                listOf(
                    Category(id = "animals", name = "animals", words = listOf("CAT")),
                    Category(id = "vehicles", name = "vehicles", words = listOf("CAR")),
                ),
            letters = listOf("A"),
            wordLengths = listOf(3),
            colors =
                listOf(
                    GameColor(id = "red", name = "red", word = "RED"),
                    GameColor(id = "blue", name = "blue", word = "BLUE"),
                ),
            allWords = listOf("CAT", "CAR"),
            statements = listOf("The sky is blue" to true),
            equations = listOf("1 + 1 = 2" to stimulusShouldMatch),
        )

    private class RecordingStorage : Storage {
        private val darkTheme = MutableStateFlow<Boolean?>(null)
        private val languageCode = MutableStateFlow<String?>(null)
        private val soundEnabled = MutableStateFlow<Boolean?>(null)
        private val vibrationEnabled = MutableStateFlow<Boolean?>(null)
        private val remindersEnabled = MutableStateFlow<Boolean?>(null)
        private val gameSessionsJson = MutableStateFlow<String?>(null)
        private val dailyTrainingJson = MutableStateFlow<String?>(null)
        private val dailyTrainingProgressJson = MutableStateFlow<String?>(null)
        private val profilePreferencesJson = MutableStateFlow<String?>(null)
        val baspaRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val kenKozRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val rememberNumberRecords = mutableMapOf<Pair<Int, String>, MutableStateFlow<Int?>>()
        var gameSessionWriteCount = 0
            private set
        var dailyTrainingWriteCount = 0
            private set
        var baspaRecordWriteCount = 0
            private set

        fun installTrainingPlan(): DailyTrainingEntry {
            val epochDay = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY
            val dontTapEntry = trainingEntry(epochDay, GameId.DontTap, GameModeId.DontTapMath, requiredScore = 1)
            dailyTrainingJson.value =
                Json.encodeToString(
                    DailyTrainingPlan(
                        epochDay = epochDay,
                        entries =
                            listOf(
                                dontTapEntry,
                                trainingEntry(
                                    epochDay,
                                    GameId.NumberSprint,
                                    GameModeId.NumberSprintClassic,
                                    requiredScore = 5,
                                ),
                                trainingEntry(
                                    epochDay,
                                    GameId.WideEye,
                                    GameModeId.WideEyeCharacters,
                                    requiredScore = 5,
                                ),
                            ),
                    ),
                )
            return dontTapEntry
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
            gameSessionWriteCount++
            gameSessionsJson.value = value
        }

        override suspend fun setDailyTrainingJson(value: String) {
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
            baspaRecordWriteCount++
            baspaRecords.getOrPut(mode) { MutableStateFlow(null) }.value = record
        }

        override suspend fun setKenKozGameRecord(
            mode: String,
            record: Int,
        ) {
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
        ) =
            DailyTrainingEntry(
                id = "$epochDay:${game.name}:${mode.name}",
                game = game,
                mode = mode,
                requiredScore = requiredScore,
            )

        private companion object {
            const val MILLIS_PER_DAY = 86_400_000L
        }
    }

    private companion object {
        const val STIMULUS_GAP_MILLIS = 300L
    }
}
