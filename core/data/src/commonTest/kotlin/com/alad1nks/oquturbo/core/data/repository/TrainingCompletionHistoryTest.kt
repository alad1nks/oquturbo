package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.DailyTrainingProgress
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class TrainingCompletionHistoryTest {
    @Test
    fun qualificationsStaySequentialAndTrainingReceiptDoesNotCreatePractice() =
        runTest {
            val storage = HistoryTestStorage()
            val repository = DailyTrainingRepository(storage, HistoryClock())
            val plan = repository.ensureTodayTraining()
            assertEquals(plan, repository.completeEntry(plan.entries[1].id, Int.MAX_VALUE))
            assertEquals(plan, repository.completeEntry(plan.nextEntry!!.id, plan.nextEntry!!.requiredScore - 1))
            val completed = finish(repository, plan)
            assertTrue(completed.isCompleted)
            assertEquals(1, repository.observeProgress().first().totalCompletedTrainings)
            assertEquals(setOf(100L), repository.observeCompletionHistory().first().completedEpochDays)
            assertEquals(1, storage.progressWrites)
            assertEquals(completed, repository.completeEntry(plan.entries.first().id, 100))
            assertEquals(1, storage.progressWrites)
            assertNull(storage.activity.value)
        }

    @Test
    fun progressFailureDoesNotAwardAndRetryCommitsExactlyOnce() =
        runTest {
            val storage = HistoryTestStorage()
            val repository = DailyTrainingRepository(storage, HistoryClock())
            val partial = beforeLast(repository)
            storage.beforeProgressWrite = { error("progress unavailable") }
            val bytes = storage.plan.value
            assertFailsWith<IllegalStateException> { repository.completeEntry(partial.nextEntry!!.id, 100) }
            assertEquals(bytes, storage.plan.value)
            assertNull(storage.progress.value)
            storage.beforeProgressWrite = {}
            assertTrue(repository.completeEntry(partial.nextEntry!!.id, 100).isCompleted)
            assertEquals(1, storage.progressWrites)
            assertEquals(1, repository.observeProgress().first().totalCompletedTrainings)
        }

    @Test
    fun committedReceiptSurvivesPlanFailuresAndRepairsOnReopenWithoutDoubleAward() =
        runTest {
            for (viaEnsure in listOf(true, false)) {
                val storage = HistoryTestStorage()
                val clock = HistoryClock()
                val repository = DailyTrainingRepository(storage, clock)
                val partial = beforeLast(repository)
                storage.beforePlanWrite = { error("plan unavailable") }
                assertFailsWith<IllegalStateException> { repository.completeEntry(partial.nextEntry!!.id, 100) }
                val receipt = storage.progress.value
                assertEquals(1, repository.observeProgress().first().totalCompletedTrainings)
                assertEquals(setOf(100L), repository.observeCompletionHistory().first().completedEpochDays)
                val reopened = DailyTrainingRepository(storage, clock)
                assertFailsWith<IllegalStateException> { reopened.ensureTodayTraining() }
                assertEquals(receipt, storage.progress.value)
                storage.beforePlanWrite = {}
                val repaired =
                    if (viaEnsure) {
                        reopened.ensureTodayTraining()
                    } else {
                        reopened.completeEntry(
                            partial.nextEntry!!.id,
                            100,
                        )
                    }
                assertTrue(repaired.isCompleted)
                assertEquals(partial.entries.map { it.copy(isCompleted = true) }, repaired.entries)
                assertEquals(receipt, storage.progress.value)
                assertEquals(1, storage.progressWrites)
                assertNull(storage.activity.value)
            }
        }

    @Test
    fun yesterdayReceiptNeverCompletesNewPlanAndRollbackNeverReawardsAnEarlierRecordedDay() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repository = DailyTrainingRepository(storage, clock)
            finish(repository, repository.ensureTodayTraining())
            clock.day = 101
            val next = repository.ensureTodayTraining()
            assertEquals(101, next.epochDay)
            assertFalse(next.isCompleted)
            finish(repository, next)
            assertEquals(2, repository.observeProgress().first().totalCompletedTrainings)
            clock.day = 100
            val rollback = repository.ensureTodayTraining()
            assertFalse(rollback.isCompleted) // newly generated plan is not a reconciliation of a valid saved plan
            val repaired = repository.ensureTodayTraining()
            assertTrue(repaired.isCompleted) // valid same-day plan now has an existing receipt
            assertEquals(2, repository.observeProgress().first().totalCompletedTrainings)
            assertEquals(setOf(100L, 101L), repository.observeCompletionHistory().first().completedEpochDays)
        }

    @Test
    fun invalidPlanIsRecoveredNormallyAndArbitraryEntryCannotUseReceiptAsCompletionEvidence() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repository = DailyTrainingRepository(storage, clock)
            finish(repository, repository.ensureTodayTraining())
            storage.plan.value = "{\"epochDay\":100,\"entries\":[{\"id\":\"foreign\"}]}"
            val regenerated = repository.completeEntry("foreign", Int.MAX_VALUE)
            assertFalse(regenerated.isCompleted)
            assertEquals(3, regenerated.entries.size)
            assertEquals(1, repository.observeProgress().first().totalCompletedTrainings)
        }

    @Test
    fun legacyProgressKeepsFlatFieldsSeedsOnlyTodayAndCanRepairByLastDayReceipt() =
        runTest {
            for (lastDay in listOf(99L, 100L)) {
                val storage = HistoryTestStorage()
                val repository = DailyTrainingRepository(storage, HistoryClock())
                val partial = repository.ensureTodayTraining()
                storage.progress.value = "{\"version\":1,\"totalCompletedTrainings\":8," +
                    "\"lastCompletedEpochDay\":$lastDay}"
                val repaired = repository.ensureTodayTraining()
                assertEquals(lastDay == 100L, repaired.isCompleted)
                assertEquals(partial.entries.map { it.copy(isCompleted = lastDay == 100L) }, repaired.entries)
                val history = repository.observeCompletionHistory().first()
                assertEquals(if (lastDay == 100L) setOf(100L) else emptySet(), history.completedEpochDays)
                val oldReader = json.decodeFromString<DailyTrainingProgress>(storage.progress.value!!)
                assertEquals(8, oldReader.totalCompletedTrainings)
                assertEquals(lastDay, oldReader.lastCompletedEpochDay)
                assertEquals(1, oldReader.version)
                assertTrue(
                    json.parseToJsonElement(storage.progress.value!!).jsonObject.containsKey("totalCompletedTrainings"),
                )
                assertFalse(json.parseToJsonElement(storage.progress.value!!).jsonObject.containsKey("progress"))
            }
        }

    @Test
    fun oldTrainingWriterDropsMetadataAndNewTrackingCannotRecoverCoverage() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repository = DailyTrainingRepository(storage, clock)
            finish(repository, repository.ensureTodayTraining())
            val old = json.decodeFromString<DailyTrainingProgress>(storage.progress.value!!)
            storage.progress.value = json.encodeToString(old)
            clock.day = 102
            val reopened = DailyTrainingRepository(storage, clock)
            assertEquals(DayHistory(102, emptyList()), reopened.observeCompletionHistory().first())
            assertEquals(1, reopened.observeProgress().first().totalCompletedTrainings)
            assertEquals(100L, reopened.observeProgress().first().lastCompletedEpochDay)
        }

    @Test
    fun malformedWholeProgressKeepsLegacyObserverFallbackButNeverInitializesHistory() =
        runTest {
            val invalidBases =
                listOf(
                    "broken",
                    "[]",
                    "{\"version\":2,\"totalCompletedTrainings\":8}",
                    "{\"totalCompletedTrainings\":-1}",
                )
            for (bytes in invalidBases) {
                val storage =
                    HistoryTestStorage().apply {
                        progress.value = bytes
                        activity.value = bytes
                    }
                val repository = DailyTrainingRepository(storage, HistoryClock())
                assertEquals(0, repository.observeProgress().first().totalCompletedTrainings)
                assertFailsWith<Exception> { repository.observeCompletionHistory().first() }
                assertFailsWith<Exception> { repository.ensureTodayTraining() }
                if (bytes != "{\"totalCompletedTrainings\":-1}") {
                    assertFailsWith<Exception> {
                        GameActivityRepository(
                            storage,
                            HistoryClock(),
                        ).observeSessions().first()
                    }
                }
                assertEquals(0, storage.progressWrites + storage.activityWrites + storage.planWrites)
            }
        }

    @Test
    fun committedTrainingReceiptAcrossMidnightRemainsYesterdayAndNewPlanStartsNormally() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock(100)
            val repository = DailyTrainingRepository(storage, clock)
            val partial = beforeLast(repository)
            val gate = CompletableDeferred<Unit>()
            storage.beforeProgressWrite = { gate.await() }
            val completing = async { repository.completeEntry(partial.nextEntry!!.id, 100) }
            runCurrent()
            assertNull(storage.progress.value)
            clock.day = 101
            gate.complete(Unit)
            val resultingPlan = completing.await()
            assertEquals(101, resultingPlan.epochDay)
            assertFalse(resultingPlan.isCompleted)
            val history = repository.observeCompletionHistory().first()
            assertEquals(100, history.trackingStartedEpochDay)
            assertEquals(setOf(100L), history.completedEpochDays)
            assertEquals(1, repository.observeProgress().first().totalCompletedTrainings)
        }

    @Test
    fun trainingReadCrossingMidnightDoesNotCreditPreviousPlanToToday() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock(100)
            val repository = DailyTrainingRepository(storage, clock)
            val partial = beforeLast(repository)
            val gate = CompletableDeferred<Unit>()
            storage.beforeProgressRead = { gate.await() }
            val completing = async { repository.completeEntry(partial.nextEntry!!.id, 100) }
            runCurrent()
            clock.day = 101
            gate.complete(Unit)
            val result = completing.await()
            assertEquals(101, result.epochDay)
            assertFalse(result.isCompleted)
            assertNull(storage.progress.value)
        }

    @Test
    fun independentRepositoryOutcomesNeverSynthesizeOtherSourceFactsOrXp() =
        runTest {
            for (practiceSuccess in listOf(false, true)) for (trainingSuccess in listOf(false, true)) {
                val storage = HistoryTestStorage()
                val clock = HistoryClock()
                val activity = GameActivityRepository(storage, clock)
                val training = DailyTrainingRepository(storage, clock)
                val initialPractice = activity.observePracticeHistory().first()
                clock.day = 101
                val initialTraining = training.observeCompletionHistory().first()
                assertEquals(100, initialPractice.trackingStartedEpochDay)
                assertEquals(101, initialTraining.trackingStartedEpochDay)
                val partial = beforeLast(training)
                val gate = CompletableDeferred<Unit>()
                storage.beforeActivityWrite = {
                    gate.await()
                    if (!practiceSuccess) error("session failed")
                }
                storage.beforeProgressWrite = {
                    gate.await()
                    if (!trainingSuccess) error("receipt failed")
                }
                val session =
                    async {
                        runCatching {
                            activity.recordCompletedSession(
                                GameId.WideEye,
                                GameModeId.WideEyeWords,
                                score = 5,
                                durationMillis = 1,
                                isNewRecord = false,
                            )
                        }
                    }
                val receipt = async { runCatching { training.completeEntry(partial.nextEntry!!.id, 100) } }
                runCurrent()
                assertTrue(
                    decodeDayHistory(
                        json.parseToJsonElement(storage.activity.value!!).jsonObject["practiceHistory"],
                    )!!.completedEpochDays.isEmpty(),
                )
                // Reading stored JSON avoids waiting behind the in-flight receipt mutex.
                assertEquals(
                    initialTraining,
                    decodeDayHistory(json.parseToJsonElement(storage.progress.value!!).jsonObject["completionHistory"]),
                )
                gate.complete(Unit)
                assertEquals(practiceSuccess, session.await().isSuccess)
                assertEquals(trainingSuccess, receipt.await().isSuccess)
                assertEquals(
                    if (practiceSuccess) setOf(101L) else emptySet(),
                    activity.observePracticeHistory().first().completedEpochDays,
                )
                assertEquals(
                    if (trainingSuccess) setOf(101L) else emptySet(),
                    training.observeCompletionHistory().first().completedEpochDays,
                )
                assertEquals(if (practiceSuccess) 5 else 0, activity.observeProgress().first().totalXp)
                assertEquals(if (trainingSuccess) 1 else 0, training.observeProgress().first().totalCompletedTrainings)
            }
        }

    private suspend fun beforeLast(repository: DailyTrainingRepository): DailyTrainingPlan {
        var plan = repository.ensureTodayTraining()
        repeat(plan.entries.size - 1) { plan = repository.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE) }
        return plan
    }

    private suspend fun finish(repository: DailyTrainingRepository, initial: DailyTrainingPlan): DailyTrainingPlan {
        var plan = initial
        while (!plan.isCompleted) plan = repository.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE)
        return plan
    }

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}
