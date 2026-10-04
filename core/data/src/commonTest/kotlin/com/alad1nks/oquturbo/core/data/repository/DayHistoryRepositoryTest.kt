package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.DayCompletionStatus
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameActivityTotals
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameRecord
import com.alad1nks.oquturbo.core.data.model.GameSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class DayHistoryRepositoryTest {
    @Test
    fun queriesRespectPartialStartSparseFactsFutureAndImmutableSnapshots() {
        val input = mutableSetOf(99L, 102L, 105L)
        val history = DayHistory(100, input)
        input.add(100)
        assertEquals(DayCompletionStatus.Completed, history.statusOn(99, 104))
        assertEquals(DayCompletionStatus.Unknown, history.statusOn(100, 200))
        assertEquals(DayCompletionStatus.Unknown, history.statusOn(98, 104))
        assertEquals(DayCompletionStatus.NoCompletionRecorded, history.statusOn(101, 104))
        assertEquals(DayCompletionStatus.NoCompletionRecorded, history.statusOn(104, 104))
        assertEquals(DayCompletionStatus.Unknown, history.statusOn(105, 104))
        assertEquals(DayCompletionStatus.Unknown, DayHistory(105, emptyList()).statusOn(103, 103))
        val exported = history.completedEpochDays
        (exported as? MutableSet<Long>)?.add(100)
        assertEquals(setOf(99L, 102L, 105L), history.completedEpochDays)
    }

    @Test
    fun ordinaryObserversAreReadOnlyAndBlankHistoryStartsOnlyAfterExplicitSubscription() =
        runTest {
            for (blank in listOf(null, "", "  ")) {
                val storage =
                    HistoryTestStorage().apply {
                        activity.value = blank
                        progress.value = blank
                    }
                val clock = HistoryClock()
                val activity = GameActivityRepository(storage, clock)
                val training = DailyTrainingRepository(storage, clock)
                assertTrue(activity.observeSessions().first().isEmpty())
                assertTrue(activity.observeRecords().first().isEmpty())
                assertEquals(0, activity.observeProgress().first().totalXp)
                assertEquals(0L, activity.observeTotals().first().sessionCount)
                assertEquals(0, training.observeProgress().first().totalCompletedTrainings)
                assertEquals(0, storage.activityWrites + storage.progressWrites)
                val practice = activity.observePracticeHistory().first()
                val completion = training.observeCompletionHistory().first()
                assertEquals(DayHistory(100, emptyList()), practice)
                assertEquals(practice, completion)
                assertEquals(DayCompletionStatus.Unknown, practice.statusOn(100, 101))
                assertEquals(DayCompletionStatus.NoCompletionRecorded, practice.statusOn(101, 101))
                clock.day = 102
                assertEquals(practice, activity.observePracticeHistory().first())
                assertEquals(completion, training.observeCompletionHistory().first())
                assertEquals(1, storage.activityWrites)
                assertEquals(1, storage.progressWrites)
                assertNull(storage.plan.value)
            }
        }

    @Test
    fun everySavedGameIncludingZeroCountsOncePerDayWithoutChangingSessionOrXpRules() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val activity = GameActivityRepository(storage, clock)
            save(activity, 100, 0)
            activity.recordCompletedSession(GameId.WordFlow, GameModeId.WordFlowContext, "ru", 3, 2, 5, false)
            save(activity, 101, 4)
            assertEquals(setOf(100L, 101L), activity.observePracticeHistory().first().completedEpochDays)
            assertEquals(3, storage.activityWrites)
            assertEquals(3, activity.observeSessions().first().size)
            assertEquals(6, activity.observeProgress().first().totalXp)
            assertEquals(3L, activity.observeTotals().first().sessionCount)
            assertNull(storage.progress.value)
        }

    @Test
    fun eventAndHistoryHaveOneCommitAndNoPrematureFlowPublication() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val activity = GameActivityRepository(storage, clock)
            val snapshots = mutableListOf<DayHistory>()
            backgroundScope.launch { activity.observePracticeHistory().collect { snapshots.add(it) } }
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            storage.beforeActivityWrite = { gate.await() }
            val saving = async { save(activity, 100, 7) }
            runCurrent()
            assertEquals(listOf(DayHistory(100, emptyList())), snapshots)
            assertTrue(activity.observeSessions().first().isEmpty())
            assertEquals(0, activity.observeProgress().first().totalXp)
            gate.complete(Unit)
            saving.await()
            runCurrent()
            assertEquals(setOf(100L), snapshots.last().completedEpochDays)
            assertEquals(2, storage.activityWrites) // one initialization, one combined event write
            val reopened = GameActivityRepository(storage, clock)
            assertEquals(7, reopened.observeProgress().first().totalXp)
            assertEquals(setOf(100L), reopened.observePracticeHistory().first().completedEpochDays)
            storage.beforeActivityWrite = { error("disk full") }
            assertFailsWith<IllegalStateException> { save(activity, 101, 8) }
            assertEquals(2, storage.activityWrites)
            assertEquals(1, reopened.observeSessions().first().size)
            assertEquals(setOf(100L), reopened.observePracticeHistory().first().completedEpochDays)
        }

    @Test
    fun firstEventWritesOnlyOnceAndKeepsAcceptedEarlierDateWithoutBackdatingCoverage() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock(100)
            val repository = GameActivityRepository(storage, clock)
            save(repository, 99, 5)
            assertEquals(1, storage.activityWrites)
            val history = repository.observePracticeHistory().first()
            assertEquals(100, history.trackingStartedEpochDay)
            assertEquals(DayCompletionStatus.Completed, history.statusOn(99, 101))
            assertEquals(DayCompletionStatus.Unknown, history.statusOn(98, 101))
            assertEquals(DayCompletionStatus.Unknown, history.statusOn(100, 101))
        }

    @Test
    fun legacyUpgradePreservesRichBaseButSeedsOnlyTheSampledDay() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repository = GameActivityRepository(storage, clock)
            save(repository, 99, 7)
            save(repository, 100, 8)
            val original = json.parseToJsonElement(storage.activity.value!!).jsonObject
            storage.activity.value = JsonObject(original - "practiceHistory").toString()
            val before = repository.observeTotals().first()
            val history = repository.observePracticeHistory().first()
            assertEquals(setOf(100L), history.completedEpochDays)
            assertEquals(before, repository.observeTotals().first())
            val after = json.parseToJsonElement(storage.activity.value!!).jsonObject
            assertEquals(original - "practiceHistory", after - "practiceHistory")
            assertEquals(15, repository.observeProgress().first().totalXp)
        }

    @Test
    fun initializationSamplesAfterSuspendedReadAndPreservesSampleAcrossSuspendedWrite() =
        runTest {
            for (training in listOf(false, true)) {
                val storage = HistoryTestStorage()
                val clock = HistoryClock(100)
                val readGate = CompletableDeferred<Unit>()
                val writeGate = CompletableDeferred<Unit>()
                if (training) {
                    storage.beforeProgressRead = { readGate.await() }
                    storage.beforeProgressWrite = { writeGate.await() }
                } else {
                    storage.beforeActivityRead = { readGate.await() }
                    storage.beforeActivityWrite = { writeGate.await() }
                }
                val reading =
                    async {
                        if (training) {
                            DailyTrainingRepository(storage, clock).observeCompletionHistory().first()
                        } else {
                            GameActivityRepository(storage, clock).observePracticeHistory().first()
                        }
                    }
                runCurrent()
                clock.day = 101
                readGate.complete(Unit)
                runCurrent()
                assertFalse(reading.isCompleted)
                assertNull(if (training) storage.progress.value else storage.activity.value)
                clock.day = 102
                writeGate.complete(Unit)
                val history = reading.await()
                assertEquals(101, history.trackingStartedEpochDay)
                assertEquals(DayCompletionStatus.Unknown, history.statusOn(101, 102))
                assertEquals(1, storage.activityWrites + storage.progressWrites)
            }
        }

    @Test
    fun concurrentSubscriptionsAndFirstEventDoNotMoveStartOrLoseFacts() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repository = GameActivityRepository(storage, clock)
            val gate = CompletableDeferred<Unit>()
            storage.beforeActivityWrite = { gate.await() }
            val first = async { repository.observePracticeHistory().first() }
            runCurrent()
            val second = async { repository.observePracticeHistory().first() }
            val event = async { save(repository, 100, 1) }
            runCurrent()
            clock.day = 101
            gate.complete(Unit)
            first.await()
            second.await()
            event.await()
            val result = repository.observePracticeHistory().first()
            assertEquals(100, result.trackingStartedEpochDay)
            assertEquals(setOf(100L), result.completedEpochDays)
            assertEquals(2, storage.activityWrites)
            assertEquals(1, repository.observeProgress().first().totalXp)
        }

    @Test
    fun readFailureWriteFailureAndCancellationNeverFabricateReadyHistory() =
        runTest {
            for (training in listOf(false, true)) {
                val storage = HistoryTestStorage()
                val clock = HistoryClock()
                val repository = GameActivityRepository(storage, clock)
                val trainingRepository = DailyTrainingRepository(storage, clock)

                suspend fun history() =
                    if (training) {
                        trainingRepository.observeCompletionHistory().first()
                    } else {
                        repository.observePracticeHistory().first()
                    }
                if (training) {
                    storage.beforeProgressRead = { error("unavailable") }
                } else {
                    storage.beforeActivityRead = { error("unavailable") }
                }
                assertFailsWith<IllegalStateException> { history() }
                storage.beforeProgressRead = {}
                storage.beforeActivityRead = {}
                if (training) {
                    storage.beforeProgressWrite = { error("disk full") }
                } else {
                    storage.beforeActivityWrite = { error("disk full") }
                }
                assertFailsWith<IllegalStateException> { history() }
                assertEquals(0, storage.activityWrites + storage.progressWrites)
                val gate = CompletableDeferred<Unit>()
                if (training) {
                    storage.beforeProgressWrite = { gate.await() }
                } else {
                    storage.beforeActivityWrite = { gate.await() }
                }
                val pending = launch { history() }
                runCurrent()
                pending.cancelAndJoin()
                assertNull(if (training) storage.progress.value else storage.activity.value)
                storage.beforeActivityWrite = {}
                storage.beforeProgressWrite = {}
                clock.day = 102
                assertEquals(102, history().trackingStartedEpochDay)
            }
        }

    @Test
    fun malformedPresentMetadataBlocksWritesAndHistoryWithoutZeroingOldAggregates() =
        runTest {
            val invalid =
                listOf(
                    "null",
                    "[]",
                    "3",
                    "{}",
                    "{\"version\":2,\"trackingStartedEpochDay\":100,\"completedEpochDays\":[]}",
                    "{\"version\":1,\"trackingStartedEpochDay\":\"100\",\"completedEpochDays\":[]}",
                    "{\"version\":1,\"trackingStartedEpochDay\":100,\"completedEpochDays\":[100,null]}",
                    "{\"version\":1,\"trackingStartedEpochDay\":100,\"completedEpochDays\":[100,1.5]}",
                    "{\"version\":1,\"trackingStartedEpochDay\":100,\"completedEpochDays\":[9223372036854775808]}",
                )
            for (metadata in invalid) {
                val storage =
                    HistoryTestStorage().apply {
                        activity.value = "{\"version\":1,\"totalCorrectAnswers\":42,\"practiceHistory\":$metadata}"
                        progress.value = "{\"version\":1,\"totalCompletedTrainings\":8," +
                            "\"lastCompletedEpochDay\":99,\"completionHistory\":$metadata}"
                    }
                val clock = HistoryClock()
                val activity = GameActivityRepository(storage, clock)
                val training = DailyTrainingRepository(storage, clock)
                assertEquals(42, activity.observeProgress().first().totalXp)
                assertEquals(8, training.observeProgress().first().totalCompletedTrainings)
                assertFailsWith<IllegalStateException> { activity.observePracticeHistory().first() }
                assertFailsWith<IllegalStateException> { training.observeCompletionHistory().first() }
                assertFailsWith<IllegalStateException> { save(activity, 100, 1) }
                assertFailsWith<IllegalStateException> { training.ensureTodayTraining() }
                assertEquals(0, storage.activityWrites + storage.progressWrites + storage.planWrites)
            }
        }

    @Test
    fun validUnsortedMetadataCanonicalizesOnWriteWithoutDroppingDates() =
        runTest {
            val storage =
                HistoryTestStorage().apply {
                    activity.value = "{\"practiceHistory\":{\"version\":1,\"trackingStartedEpochDay\":105," +
                        "\"completedEpochDays\":[105,99,105,102]}}"
                }
            val repository = GameActivityRepository(storage, HistoryClock(100))
            assertEquals(setOf(99L, 102L, 105L), repository.observePracticeHistory().first().completedEpochDays)
            save(repository, 100, 1)
            assertTrue(storage.activity.value!!.contains("\"completedEpochDays\":[99,100,102,105]"))
            assertEquals(DayCompletionStatus.Unknown, repository.observePracticeHistory().first().statusOn(105, 100))
        }

    @Test
    fun retentionKeepsAllDaysAndEarlySevenDayEvidenceAfterJournalTruncation() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock(100)
            val repository = GameActivityRepository(storage, clock)
            repeat(1001) { save(repository, 100L + it, 1) }
            save(repository, 1102, 1) // leave day1101 without a saved completion
            val reopened = GameActivityRepository(storage, clock)
            assertEquals(1000, reopened.observeSessions().first().size)
            assertFalse(reopened.observeSessions().first().any { it.completedEpochDay == 100L })
            val history = reopened.observePracticeHistory().first()
            assertEquals(1002, history.completedEpochDays.size)
            assertTrue((100L..106L).all { it in history.completedEpochDays })
            assertEquals(DayCompletionStatus.NoCompletionRecorded, history.statusOn(1101, 1102))
            assertEquals(1002L, reopened.observeTotals().first().sessionCount)
            assertEquals(1002, reopened.observeProgress().first().totalXp)
        }

    @Test
    fun oldReaderAndWriterRoundTripIsCompatibleButCannotPreserveUnknownHistory() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock(100)
            val repository = GameActivityRepository(storage, clock)
            save(repository, 100, 3)
            val old = json.decodeFromString<OldActivity>(storage.activity.value!!)
            assertEquals(1, old.version)
            assertEquals(3, old.totalCorrectAnswers)
            assertEquals(100, old.sessions.single().completedEpochDay)
            storage.activity.value = json.encodeToString(old)
            clock.day = 102
            val restarted = GameActivityRepository(storage, clock).observePracticeHistory().first()
            assertEquals(102, restarted.trackingStartedEpochDay)
            assertTrue(restarted.completedEpochDays.isEmpty())
            assertEquals(DayCompletionStatus.Unknown, restarted.statusOn(101, 102))
        }

    @Test
    fun acceptedEventKeepsItsDayThroughReadMidnightAndCancellationOfItsSingleDurableWrite() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock(100)
            val repository = GameActivityRepository(storage, clock)
            val readGate = CompletableDeferred<Unit>()
            val writeGate = CompletableDeferred<Unit>()
            storage.beforeActivityRead = { readGate.await() }
            storage.beforeActivityWrite = { writeGate.await() }
            val saving =
                launch {
                    repository.recordCompletedSession(
                        GameId.NumberSprint,
                        GameModeId.NumberSprintBinary,
                        score = 5,
                        durationMillis = 1,
                        isNewRecord = false,
                    )
                }
            runCurrent()
            clock.day = 101
            readGate.complete(Unit)
            runCurrent()
            saving.cancel()
            runCurrent()
            assertFalse(saving.isCompleted)
            assertNull(storage.activity.value)
            clock.day = 102
            writeGate.complete(Unit)
            saving.join()
            val history = repository.observePracticeHistory().first()
            assertEquals(101, history.trackingStartedEpochDay)
            assertEquals(setOf(100L), history.completedEpochDays)
            assertEquals(100, repository.observeSessions().first().single().completedEpochDay)
            assertEquals(5, repository.observeProgress().first().totalXp)
            assertEquals(1, storage.activityWrites)
        }

    @Test
    fun recordsOnlyLegacyDataDoesNotInventPracticeDates() =
        runTest {
            val storage =
                HistoryTestStorage().apply {
                    activity.value = """{"version":1,"totalCorrectAnswers":42,
                "records":[{"game":"NumberSprint","mode":"NumberSprintBinary","score":88}]}"""
                }
            val repository = GameActivityRepository(storage, HistoryClock())
            assertEquals(DayHistory(100, emptyList()), repository.observePracticeHistory().first())
            assertEquals(42, repository.observeProgress().first().totalXp)
            assertEquals(88, repository.observeRecords().first().single().score)
            assertTrue(repository.observeSessions().first().isEmpty())
        }

    @Serializable
    private data class OldActivity(
        val version: Int = 1,
        val totalCorrectAnswers: Long = 0,
        val sessions: List<GameSession> = emptyList(),
        val records: List<GameRecord> = emptyList(),
        val totals: GameActivityTotals? = null,
    )

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private suspend fun save(repository: GameActivityRepository, day: Long, score: Int) =
        repository.recordCompletedSession(
            GameId.NumberSprint,
            GameModeId.NumberSprintBinary,
            score = score,
            durationMillis = 1,
            isNewRecord = false,
            completedAtEpochMillis = day * DAY_MILLIS,
        )
}
