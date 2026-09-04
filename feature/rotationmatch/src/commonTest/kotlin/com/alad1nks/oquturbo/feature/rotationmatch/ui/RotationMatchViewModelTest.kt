package com.alad1nks.oquturbo.feature.rotationmatch.ui

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchAnswer
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchFailure
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchGame
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchPhase
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchRandom
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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class RotationMatchViewModelTest {
    @Test
    fun startWaitsForRecordAndEmptyRecordLoadsAsZero() =
        runViewModelTest { viewModel, _, _ ->
            viewModel.start()
            assertEquals(RotationMatchPhase.Ready, viewModel.uiState.value.game.phase)

            runCurrent()
            assertFalse(viewModel.uiState.value.isRecordLoading)
            assertEquals(0, viewModel.uiState.value.record)
            viewModel.start()

            assertEquals(RotationMatchPhase.Active, viewModel.uiState.value.game.phase)
        }

    @Test
    fun correctFeedbackLastsExactly350MillisAndReplayRestartsEasy() =
        runViewModelTest { viewModel, _, _ ->
            runCurrent()
            viewModel.start()
            viewModel.selectCurrentAnswer(viewModel.uiState.value.game.round!!.correctAnswer)
            assertEquals(RotationMatchPhase.CorrectFeedback, viewModel.uiState.value.game.phase)
            assertEquals(1, viewModel.uiState.value.game.score)

            advanceTimeBy(349)
            runCurrent()
            assertEquals(RotationMatchPhase.CorrectFeedback, viewModel.uiState.value.game.phase)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(RotationMatchPhase.Active, viewModel.uiState.value.game.phase)

            viewModel.start()
            assertEquals(0, viewModel.uiState.value.game.score)
            assertEquals(10_000L, viewModel.uiState.value.game.round!!.remainingTimeMillis)
        }

    @Test
    fun staleAnswerFromPreviousRoundIsIgnoredBeforeClockReconciliation() =
        runViewModelTest { viewModel, _, timeSource ->
            runCurrent()
            viewModel.start()
            val staleRound = requireNotNull(viewModel.uiState.value.game.round)
            viewModel.selectAnswer(staleRound.id, staleRound.correctAnswer)
            advanceTimeBy(350)
            runCurrent()

            val currentRound = requireNotNull(viewModel.uiState.value.game.round)
            assertNotEquals(staleRound.id, currentRound.id)
            val stateBeforeStaleAnswer = viewModel.uiState.value
            timeSource += 500.milliseconds

            viewModel.selectAnswer(staleRound.id, currentRound.correctAnswer)

            assertEquals(stateBeforeStaleAnswer, viewModel.uiState.value)
        }

    @Test
    fun staleAnswerFromPreviousAttemptIsIgnoredAfterReplay() =
        runViewModelTest { viewModel, _, timeSource ->
            runCurrent()
            viewModel.start()
            val staleRound = requireNotNull(viewModel.uiState.value.game.round)
            viewModel.start()

            val replayRound = requireNotNull(viewModel.uiState.value.game.round)
            assertNotEquals(staleRound.id, replayRound.id)
            val stateBeforeStaleAnswer = viewModel.uiState.value
            timeSource += 500.milliseconds

            viewModel.selectAnswer(staleRound.id, replayRound.correctAnswer)

            assertEquals(stateBeforeStaleAnswer, viewModel.uiState.value)
        }

    @Test
    fun wrongZeroPersistsExactlyOnceWithoutRecordAndDuplicateIsIgnored() =
        runViewModelTest { viewModel, storage, _ ->
            runCurrent()
            viewModel.start()
            val round = viewModel.uiState.value.game.round!!
            val wrong = round.correctAnswer.opposite()
            viewModel.selectCurrentAnswer(wrong)
            viewModel.selectCurrentAnswer(wrong)
            runCurrent()

            assertEquals(1, storage.gameSessionWriteCount)
            val session = GameActivityRepository(storage).observeSessions().first().single()
            assertEquals(GameId.RotationMatch, session.game)
            assertEquals(GameModeId.RotationMatchRotation, session.mode)
            assertNull(session.variantId)
            assertEquals(0, session.score)
            assertEquals(0, session.correctAnswers)
            assertFalse(session.isNewRecord)
            assertTrue(
                GameActivityRepository(storage).observeRecords().first().none { it.game == GameId.RotationMatch },
            )
        }

    @Test
    fun deadlineBoundaryMakesTimeoutWinAndDoesNotFabricateAnswer() =
        runViewModelTest { viewModel, storage, timeSource ->
            runCurrent()
            viewModel.start()
            val correct = viewModel.uiState.value.game.round!!.correctAnswer
            timeSource += 10_000.milliseconds

            viewModel.selectCurrentAnswer(correct)
            runCurrent()

            assertEquals(RotationMatchFailure.Timeout, viewModel.uiState.value.game.failure)
            assertNull(viewModel.uiState.value.game.selectedAnswer)
            assertEquals(0, GameActivityRepository(storage).observeSessions().first().single().score)
        }

    @Test
    fun durationIncludesFeedbackAndTerminalWriteContainsExactScore() =
        runViewModelTest { viewModel, storage, timeSource ->
            runCurrent()
            viewModel.start()
            timeSource += 100.milliseconds
            viewModel.selectCurrentAnswer(viewModel.uiState.value.game.round!!.correctAnswer)
            timeSource += 350.milliseconds
            advanceTimeBy(350)
            runCurrent()
            timeSource += 200.milliseconds
            val wrong = viewModel.uiState.value.game.round!!.correctAnswer.opposite()
            viewModel.selectCurrentAnswer(wrong)
            runCurrent()

            val repository = GameActivityRepository(storage)
            val session = repository.observeSessions().first().single()
            assertEquals(650L, session.durationMillis)
            assertEquals(1, session.score)
            assertEquals(1, session.correctAnswers)
            assertEquals(650L, viewModel.uiState.value.completedDurationMillis)
            assertTrue(viewModel.uiState.value.isNewRecord)
            val totals = repository.observeTotals().first()
            assertEquals(1L, totals.sessionCount)
            assertEquals(1L, totals.correctAnswers)
            assertEquals(650L, totals.durationMillis)
            val progress = repository.observeProgress().first()
            assertEquals(1, progress.totalCorrectAnswers)
            assertEquals(1, progress.totalXp)
        }

    @Test
    fun previousRecordIsCapturedAtStartAndRepositoryRevalidatesNewRecord() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                repository.recordCompletedSession(
                    GameId.RotationMatch,
                    GameModeId.RotationMatchRotation,
                    score = 2,
                    durationMillis = 10,
                    isNewRecord = true,
                )
                val viewModel = newViewModel(repository, TestTimeSource())
                runCurrent()
                viewModel.start()
                assertEquals(2, viewModel.uiState.value.previousRecord)
                repository.recordCompletedSession(
                    GameId.RotationMatch,
                    GameModeId.RotationMatchRotation,
                    score = 9,
                    durationMillis = 10,
                    isNewRecord = true,
                )
                runCurrent()
                val wrong = viewModel.uiState.value.game.round!!.correctAnswer.opposite()
                viewModel.selectCurrentAnswer(wrong)
                runCurrent()

                assertEquals(2, viewModel.uiState.value.previousRecord)
                assertEquals(9, viewModel.uiState.value.record)
                assertFalse(viewModel.uiState.value.isNewRecord)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun abandonAndReplayInvalidateFeedbackAndTimerWork() =
        runViewModelTest { viewModel, storage, _ ->
            runCurrent()
            viewModel.start()
            viewModel.selectCurrentAnswer(viewModel.uiState.value.game.round!!.correctAnswer)
            viewModel.start()
            val replayRound = viewModel.uiState.value.game.round
            advanceTimeBy(350)
            runCurrent()
            assertEquals(replayRound, viewModel.uiState.value.game.round)
            assertEquals(0, viewModel.uiState.value.game.score)

            viewModel.selectCurrentAnswer(viewModel.uiState.value.game.round!!.correctAnswer)
            viewModel.abandon()
            advanceTimeBy(350)
            runCurrent()
            assertEquals(RotationMatchPhase.CorrectFeedback, viewModel.uiState.value.game.phase)
            assertEquals(0, storage.gameSessionWriteCount)
        }

    @Test
    fun timeoutRecordsUpdatedScoreOnlyOnce() =
        runViewModelTest { viewModel, storage, _ ->
            runCurrent()
            viewModel.start()
            viewModel.selectCurrentAnswer(viewModel.uiState.value.game.round!!.correctAnswer)
            advanceTimeBy(350)
            runCurrent()
            viewModel.advanceTimerBy(10_000)
            viewModel.advanceTimerBy(10_000)
            runCurrent()

            assertEquals(1, storage.gameSessionWriteCount)
            val session = GameActivityRepository(storage).observeSessions().first().single()
            assertEquals(1, session.score)
            assertEquals(1, session.correctAnswers)
        }

    @Test
    fun hubBackWrapsAbandonWhileStandaloneHasNoAction() {
        var abandoned = false
        var backed = false
        val action = rotationMatchBackAction({ backed = true }) { abandoned = true }
        action!!()
        assertTrue(abandoned)
        assertTrue(backed)
        assertNull(rotationMatchBackAction(null) { abandoned = true })
    }

    @Test
    fun durationFormattingUsesApprovedBoundaries() {
        assertEquals(
            RotationMatchDurationDisplayParts.LessThanOneSecond,
            rotationMatchDurationDisplayParts(999),
        )
        assertEquals(RotationMatchDurationDisplayParts.Seconds(1), rotationMatchDurationDisplayParts(1_000))
        assertEquals(
            RotationMatchDurationDisplayParts.MinutesSeconds(1, null),
            rotationMatchDurationDisplayParts(60_000),
        )
        assertEquals(
            RotationMatchDurationDisplayParts.MinutesSeconds(2, 5),
            rotationMatchDurationDisplayParts(125_000),
        )
    }

    private fun runViewModelTest(
        block: suspend kotlinx.coroutines.test.TestScope.(
            RotationMatchViewModel,
            RecordingStorage,
            TestTimeSource,
        ) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val storage = RecordingStorage()
        val timeSource = TestTimeSource()
        val viewModel = newViewModel(GameActivityRepository(storage), timeSource)
        try {
            block(viewModel, storage, timeSource)
        } finally {
            viewModel.abandon()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private fun newViewModel(
        repository: GameActivityRepository,
        timeSource: TestTimeSource,
    ) = RotationMatchViewModel(
        activityRepository = repository,
        game = RotationMatchGame(SeededRandom()),
        timeSource = timeSource,
    )

    private fun RotationMatchAnswer.opposite(): RotationMatchAnswer =
        if (this == RotationMatchAnswer.Match) RotationMatchAnswer.Different else RotationMatchAnswer.Match

    private fun RotationMatchViewModel.selectCurrentAnswer(answer: RotationMatchAnswer) {
        selectAnswer(requireNotNull(uiState.value.game.round).id, answer)
    }

    private class SeededRandom : RotationMatchRandom {
        private val random = Random(7)

        override fun nextInt(until: Int): Int = random.nextInt(until)

        override fun <T> shuffle(values: List<T>): List<T> = values.shuffled(random)
    }

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
        private val baspaRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val kenKozRecords = mutableMapOf<String, MutableStateFlow<Int?>>()
        private val rememberNumberRecords = mutableMapOf<Pair<Int, String>, MutableStateFlow<Int?>>()
        var gameSessionWriteCount = 0

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
