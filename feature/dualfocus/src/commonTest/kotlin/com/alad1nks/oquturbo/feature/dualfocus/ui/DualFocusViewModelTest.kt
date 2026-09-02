package com.alad1nks.oquturbo.feature.dualfocus.ui

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusGame
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusLane
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusPhase
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusRandom
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class DualFocusViewModelTest {
    @Test
    fun wrongZeroAndDuplicateInputPersistExactlyOnceInMatchSeries() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val viewModel =
                    DualFocusViewModel(
                        repository,
                        DualFocusGame(SequenceRandom(0, 0, 1, 1)),
                    )
                runCurrent()
                viewModel.start()
                val card = viewModel.uiState.value.game.cards.getValue(DualFocusLane.One)

                viewModel.tap(DualFocusLane.One, card.id)
                viewModel.tap(DualFocusLane.One, card.id)
                runCurrent()

                assertEquals(1, storage.gameSessionWriteCount)
                val session = repository.observeSessions().first().single()
                assertEquals(GameId.DualFocus, session.game)
                assertEquals(GameModeId.DualFocusMatch, session.mode)
                assertNull(session.variantId)
                assertEquals(0, session.score)
                assertEquals(0, session.correctAnswers)
                assertFalse(session.isNewRecord)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun activeAbandonmentDoesNotWriteSession() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val viewModel =
                    DualFocusViewModel(
                        GameActivityRepository(storage),
                        DualFocusGame(SequenceRandom(0, 1, 0, 0)),
                    )
                runCurrent()

                viewModel.start()
                viewModel.abandon()
                runCurrent()

                assertEquals(0, storage.gameSessionWriteCount)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun overdueMissPersistsAuthoritativeExpiryDuration() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val viewModel =
                    DualFocusViewModel(
                        repository,
                        DualFocusGame(SequenceRandom(0, 0, 0, 0)),
                    )
                runCurrent()
                viewModel.start()

                viewModel.advanceTo(60_000)
                runCurrent()

                val session = repository.observeSessions().first().single()
                assertEquals(1_100L, session.durationMillis)
                assertEquals(1_100L, viewModel.uiState.value.durationMillis)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun pausedSessionDoesNotAdvanceMutateOrWriteAndCanBeAbandoned() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val timeSource = TestTimeSource()
                val viewModel =
                    DualFocusViewModel(
                        GameActivityRepository(storage),
                        DualFocusGame(SequenceRandom(0, 0, 0, 2, 1, 2, 1)),
                        timeSource,
                    )
                runCurrent()
                viewModel.start()
                timeSource += 800.milliseconds
                viewModel.pause()
                val paused = viewModel.uiState.value

                viewModel.pause()
                viewModel.advanceTo(60_000)
                timeSource += 60_000.milliseconds
                advanceTimeBy(60_000)
                runCurrent()

                assertEquals(DualFocusPhase.Paused, paused.game.phase)
                assertEquals(paused, viewModel.uiState.value)
                assertEquals(0, storage.gameSessionWriteCount)

                viewModel.abandon()
                runCurrent()
                assertEquals(0, storage.gameSessionWriteCount)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun correctFeedbackUsesOnlyItsRemainingActiveIntervalAfterResume() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val timeSource = TestTimeSource()
                val viewModel =
                    DualFocusViewModel(
                        GameActivityRepository(RecordingStorage()),
                        DualFocusGame(SequenceRandom(0, 0, 0, 0)),
                        timeSource,
                    )
                runCurrent()
                viewModel.start()
                val target = viewModel.uiState.value.game.cards.getValue(DualFocusLane.One)
                viewModel.tap(DualFocusLane.One, target.id)
                timeSource += 100.milliseconds
                viewModel.pause()

                timeSource += 10_000.milliseconds
                advanceTimeBy(10_000)
                runCurrent()
                assertEquals(DualFocusLane.One, viewModel.uiState.value.correctFeedbackLane)

                viewModel.resume()
                timeSource += 149.milliseconds
                advanceTimeBy(149)
                runCurrent()
                assertEquals(DualFocusLane.One, viewModel.uiState.value.correctFeedbackLane)
                timeSource += 1.milliseconds
                advanceTimeBy(1)
                runCurrent()
                assertNull(viewModel.uiState.value.correctFeedbackLane)
                viewModel.abandon()
                runCurrent()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun publicPauseCyclesPreserveExactEventRemaindersAndPersistOnlyActiveDurationOnce() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage()
                val repository = GameActivityRepository(storage)
                val timeSource = TestTimeSource()
                val viewModel =
                    DualFocusViewModel(
                        repository,
                        DualFocusGame(SequenceRandom(0, 0, 0, 2, 1, 2, 1, 0, 1)),
                        timeSource,
                    )
                runCurrent()
                viewModel.start()
                timeSource += 700.milliseconds
                advanceTimeBy(700)
                runCurrent()
                val originalCards = viewModel.uiState.value.game.cards
                assertEquals(setOf(1L, 2L), originalCards.values.map { it.id }.toSet())
                assertEquals(1_100L, originalCards.getValue(DualFocusLane.One).expiresAtMillis)
                assertEquals(1_800L, originalCards.getValue(DualFocusLane.Two).expiresAtMillis)

                viewModel.pause()
                val firstPause = viewModel.uiState.value
                viewModel.pause()
                timeSource += 30_000.milliseconds
                advanceTimeBy(30_000)
                runCurrent()
                assertEquals(firstPause, viewModel.uiState.value)
                viewModel.resume()
                viewModel.resume()

                timeSource += 399.milliseconds
                advanceTimeBy(399)
                runCurrent()
                assertEquals(setOf(1L, 2L), viewModel.uiState.value.game.cards.values.map { it.id }.toSet())
                timeSource += 1.milliseconds
                advanceTimeBy(1)
                runCurrent()
                assertEquals(setOf(2L), viewModel.uiState.value.game.cards.values.map { it.id }.toSet())

                timeSource += 100.milliseconds
                advanceTimeBy(100)
                runCurrent()
                viewModel.pause()
                val secondPause = viewModel.uiState.value
                viewModel.pause()
                timeSource += 45_000.milliseconds
                advanceTimeBy(45_000)
                runCurrent()
                assertEquals(secondPause, viewModel.uiState.value)
                viewModel.resume()

                timeSource += 199.milliseconds
                advanceTimeBy(199)
                runCurrent()
                assertEquals(setOf(2L), viewModel.uiState.value.game.cards.values.map { it.id }.toSet())
                timeSource += 1.milliseconds
                advanceTimeBy(1)
                runCurrent()
                val spawnedCards = viewModel.uiState.value.game.cards
                assertEquals(setOf(2L, 3L), spawnedCards.values.map { it.id }.toSet())
                assertEquals(1_400L, spawnedCards.getValue(DualFocusLane.One).appearedAtMillis)

                timeSource += 399.milliseconds
                advanceTimeBy(399)
                runCurrent()
                assertEquals(setOf(2L, 3L), viewModel.uiState.value.game.cards.values.map { it.id }.toSet())
                timeSource += 1.milliseconds
                advanceTimeBy(1)
                runCurrent()
                assertEquals(setOf(3L), viewModel.uiState.value.game.cards.values.map { it.id }.toSet())

                timeSource += 699.milliseconds
                advanceTimeBy(699)
                runCurrent()
                assertEquals(DualFocusPhase.Active, viewModel.uiState.value.game.phase)
                timeSource += 1.milliseconds
                advanceTimeBy(1)
                runCurrent()

                assertEquals(DualFocusPhase.Result, viewModel.uiState.value.game.phase)
                assertEquals(1, storage.gameSessionWriteCount)
                val session = repository.observeSessions().first().single()
                assertEquals(2_500, session.durationMillis)
                assertEquals(2_500, viewModel.uiState.value.durationMillis)
                assertEquals(0, session.score)
                assertEquals(0, session.correctAnswers)
                assertFalse(session.isNewRecord)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun delayedPriorCompletionCannotMutateAnActiveReplay() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val storage = RecordingStorage(blockNextGameSessionWrite = true)
                val repository = GameActivityRepository(storage)
                val timeSource = TestTimeSource()
                val viewModel =
                    DualFocusViewModel(
                        repository,
                        DualFocusGame(SequenceRandom(0, 0, 0, 0, 1, 1, 0, 0, 0)),
                        timeSource,
                    )
                runCurrent()
                viewModel.start()
                val target = viewModel.uiState.value.game.cards.getValue(DualFocusLane.One)
                viewModel.tap(DualFocusLane.One, target.id)
                timeSource += 700.milliseconds
                viewModel.advanceTo(700)
                val wrongCard = viewModel.uiState.value.game.cards.getValue(DualFocusLane.Two)
                viewModel.tap(DualFocusLane.Two, wrongCard.id)
                storage.gameSessionWriteStarted.await()

                viewModel.start()
                val replay = viewModel.uiState.value
                assertEquals(DualFocusPhase.Active, replay.game.phase)
                assertTrue(replay.game.cards.isNotEmpty())
                assertNull(replay.correctFeedbackLane)
                assertFalse(replay.isNewRecord)

                storage.releaseGameSessionWrite.complete(Unit)
                runCurrent()

                assertEquals(replay, viewModel.uiState.value)
                assertEquals(1, storage.gameSessionWriteCount)
                assertEquals(1, repository.observeSessions().first().size)
                viewModel.abandon()
                runCurrent()
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class SequenceRandom(vararg values: Int) : DualFocusRandom {
        private val values = values.toList()
        private var index = 0

        override fun nextInt(until: Int): Int = values[index++ % values.size] % until
    }

    private class RecordingStorage(
        private var blockNextGameSessionWrite: Boolean = false,
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
        val gameSessionWriteStarted = CompletableDeferred<Unit>()
        val releaseGameSessionWrite = CompletableDeferred<Unit>()

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
            if (blockNextGameSessionWrite) {
                blockNextGameSessionWrite = false
                gameSessionWriteStarted.complete(Unit)
                releaseGameSessionWrite.await()
            }
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
