package com.alad1nks.oquturbo.core.data.repository

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class WeeklyFocusRepositoryTest {
    @Test
    fun observeSelectDuplicateDisableReselectAndExpiryOnlyWriteFocus() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repository = DailyTrainingRepository(storage, clock)
            assertEquals(WeeklyFocus(), repository.observeWeeklyFocus().first())
            assertEquals(0, storage.focusWrites)
            val selected = repository.selectWeeklyFocus()
            assertEquals(WeeklyFocus(WeeklyFocusSelection(101, 108)), selected)
            assertEquals(selected, repository.selectWeeklyFocus())
            clock.day = 104
            assertEquals(selected, repository.selectWeeklyFocus())
            assertEquals(1, storage.focusWrites)
            assertEquals(WeeklyFocus(), repository.disableWeeklyFocus())
            assertEquals(WeeklyFocus(WeeklyFocusSelection(105, 112)), repository.selectWeeklyFocus())
            clock.day = 112
            assertEquals(WeeklyFocus(WeeklyFocusSelection(113, 120)), repository.selectWeeklyFocus())
            assertEquals(0, storage.planWrites)
            assertEquals(0, storage.progressWrites)
            assertEquals(0, storage.activityWrites)
        }

    @Test
    fun activeTransformOnlyChangesNumberSprintAndNoFocusScheduledExpiredHaveBaselineParity() =
        runTest {
            for (day in listOf(-123L, 0, 1, 100, 101, 20_089)) {
                val clock = HistoryClock(day)
                val baseline = DailyTrainingRepository(HistoryTestStorage(), clock).ensureTodayTraining()
                for (setting in listOf(
                    WeeklyFocus(),
                    WeeklyFocus(WeeklyFocusSelection(day + 1, day + 8)),
                    WeeklyFocus(WeeklyFocusSelection(day - 7, day)),
                )) {
                    val storage = HistoryTestStorage().apply { focus.value = setting.encodeFocus() }
                    assertEquals(baseline, DailyTrainingRepository(storage, clock).ensureTodayTraining())
                }
                val storage =
                    HistoryTestStorage().apply {
                        focus.value =
                            WeeklyFocus(
                                WeeklyFocusSelection(day, day + 7),
                            ).encodeFocus()
                    }
                val plan = DailyTrainingRepository(storage, clock).ensureTodayTraining()
                val first = plan.entries.first()
                assertEquals(GameId.NumberSprint, first.game)
                assertEquals(GameModeId.NumberSprintClassic, first.mode)
                assertEquals("$day:NumberSprint:NumberSprintClassic", first.id)
                assertEquals(5, first.requiredScore)
                assertEquals(baseline.entries.filter { it.game != GameId.NumberSprint }, plan.entries.drop(1))
                assertEquals(
                    setOf(GameId.NumberSprint, GameId.WideEye, GameId.DontTap),
                    plan.entries.map { it.game }.toSet(),
                )
                assertEquals(3, plan.entries.size)
                assertTrue(plan.entries.all { !it.isCompleted })
            }
        }

    @Test
    fun savedUnstartedPartialCompletedPlansRemainAuthoritativeEvenWhenFocusReadFails() =
        runTest {
            for (completeCount in 0..3) {
                val storage = HistoryTestStorage()
                val clock = HistoryClock()
                val repo = DailyTrainingRepository(storage, clock)
                var plan = repo.ensureTodayTraining()
                repeat(completeCount) { plan = repo.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE) }
                val before = storage.plan.value
                repo.selectWeeklyFocus()
                assertEquals(before, storage.plan.value)
                repo.disableWeeklyFocus()
                repo.resetWeeklyFocus()
                assertEquals(before, storage.plan.value)
                storage.beforeFocusRead = { error("focus unavailable") }
                assertEquals(plan, repo.ensureTodayTraining())
                if (!plan.isCompleted) {
                    val completed = repo.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE)
                    assertEquals(
                        plan.entries.map { it.copy(isCompleted = false) },
                        completed.entries.map { it.copy(isCompleted = false) },
                    )
                    assertEquals(completeCount + 1, completed.entries.count { it.isCompleted })
                }
            }
        }

    @Test
    fun allCreatePathsHonorFocusAndStaleEntryCannotCompleteNextDayPlan() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repo = DailyTrainingRepository(storage, clock)
            val previous = repo.ensureTodayTraining()
            repo.selectWeeklyFocus()
            clock.day = 101
            val next = repo.completeEntry(previous.nextEntry!!.id, Int.MAX_VALUE)
            assertEquals(101L, next.epochDay)
            assertEquals(GameModeId.NumberSprintClassic, next.entries.first().mode)
            assertFalse(next.entries.any { it.isCompleted })
            val raw = storage.plan.value
            assertEquals(next, repo.ensureTodayTraining())
            assertEquals(raw, storage.plan.value)
            repo.disableWeeklyFocus()
            assertEquals(next, repo.ensureTodayTraining())
            clock.day = 102
            assertEquals(
                DailyTrainingRepository(HistoryTestStorage(), clock).ensureTodayTraining(),
                repo.ensureTodayTraining(),
            )
        }

    @Test
    fun corruptFocusDoesNotSilentlyCreateOrOverwriteButExplicitResetNeedsNoOldRead() =
        runTest {
            val storage = HistoryTestStorage().apply { focus.value = """{"version":2,"selection":null}""" }
            val repo = DailyTrainingRepository(storage, HistoryClock())
            assertFailsWith<IllegalStateException> { repo.ensureTodayTraining() }
            assertFailsWith<IllegalStateException> { repo.completeEntry("foreign", Int.MAX_VALUE) }
            assertFailsWith<IllegalStateException> { repo.selectWeeklyFocus() }
            assertFailsWith<IllegalStateException> { repo.disableWeeklyFocus() }
            assertEquals(0, storage.focusWrites)
            assertEquals(0, storage.planWrites)
            storage.beforeFocusRead = { error("read failure") }
            assertEquals(WeeklyFocus(), repo.resetWeeklyFocus())
            assertEquals(WeeklyFocus(), decodeWeeklyFocus(storage.focus.value))
            assertEquals(1, storage.focusWrites)
            assertEquals(0, storage.progressWrites)
            assertEquals(0, storage.activityWrites)
        }

    @Test
    fun dateIsSampledAfterReadAndOnePayloadSurvivesMidnightRetries() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repo = DailyTrainingRepository(storage, clock)
            val read = CompletableDeferred<Unit>()
            val write = CompletableDeferred<Unit>()
            storage.beforeFocusRead = { read.await() }
            val payloads = mutableListOf<String>()
            storage.beforeFocusWrite = { payload ->
                payloads.add(payload)
                write.await()
                if (payloads.size < 3) error("transient write")
            }
            val selecting = async { repo.selectWeeklyFocus() }
            runCurrent()
            clock.day = 101
            read.complete(Unit)
            runCurrent()
            assertEquals(1, payloads.size)
            clock.day = 102
            write.complete(Unit)
            assertEquals(WeeklyFocus(WeeklyFocusSelection(102, 109)), selecting.await())
            assertEquals(3, payloads.size)
            assertEquals(1, payloads.toSet().size)
        }

    @Test
    fun failedAndCancelledWritesExposePersistedTruthWithoutCompensatingErase() =
        runTest {
            for (committed in listOf(false, true)) {
                val storage = HistoryTestStorage()
                val repo = DailyTrainingRepository(storage, HistoryClock())
                if (committed) {
                    storage.afterFocusWrite = { error("commit response lost") }
                } else {
                    storage.beforeFocusWrite = { error("before commit") }
                }
                assertFailsWith<IllegalStateException> { repo.selectWeeklyFocus() }
                assertEquals(
                    if (committed) WeeklyFocus(WeeklyFocusSelection(101, 108)) else WeeklyFocus(),
                    DailyTrainingRepository(storage, HistoryClock()).observeWeeklyFocus().first(),
                )
                assertEquals(if (committed) 4 else 0, storage.focusWrites)
                assertEquals(0, storage.planWrites)
            }
            val storage = HistoryTestStorage()
            storage.afterFocusWrite = { throw CancellationException("cancel after commit") }
            val repo = DailyTrainingRepository(storage, HistoryClock())
            assertFailsWith<CancellationException> { repo.selectWeeklyFocus() }
            assertEquals(1, storage.focusWrites)
            assertEquals(WeeklyFocus(WeeklyFocusSelection(101, 108)), repo.observeWeeklyFocus().first())
        }

    @Test
    fun commonMutexSerializesSelectAndDisableWithEntryCompletionWithoutLosingReceipt() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repo = DailyTrainingRepository(storage, clock)
            var plan = repo.ensureTodayTraining()
            repeat(2) { plan = repo.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE) }
            val gate = CompletableDeferred<Unit>()
            storage.beforeFocusWrite = { gate.await() }
            val selecting = async { repo.selectWeeklyFocus() }
            runCurrent()
            val completing = async { repo.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE) }
            val disabling = async { repo.disableWeeklyFocus() }
            runCurrent()
            assertFalse(completing.isCompleted)
            assertFalse(disabling.isCompleted)
            gate.complete(Unit)
            selecting.await()
            assertTrue(completing.await().isCompleted)
            assertEquals(WeeklyFocus(), disabling.await())
            assertEquals(1, repo.observeProgress().first().totalCompletedTrainings)
            assertEquals(setOf(100L), repo.observeCompletionHistory().first().completedEpochDays)
            assertTrue(repo.ensureTodayTraining().isCompleted)
        }

    @Test
    fun cancellationBeforeWriteReleasesMutexAndConcurrentSelectionsChooseOnlyOneCycle() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repo = DailyTrainingRepository(storage, clock)
            val gate = CompletableDeferred<Unit>()
            storage.beforeFocusWrite = { gate.await() }
            val cancelled = launch { repo.selectWeeklyFocus() }
            runCurrent()
            cancelled.cancelAndJoin()
            assertNull(storage.focus.value)
            storage.beforeFocusWrite = {}
            val choices = List(5) { async { repo.selectWeeklyFocus() } }.map { it.await() }
            assertEquals(1, choices.toSet().size)
            assertEquals(1, storage.focusWrites)
        }

    @Test
    fun focusDoesNotBreakReceiptBeforePlanFailureReconciliation() =
        runTest {
            val storage = HistoryTestStorage()
            val clock = HistoryClock()
            val repo = DailyTrainingRepository(storage, clock)
            repo.selectWeeklyFocus()
            clock.day = 101
            var plan = repo.ensureTodayTraining()
            repeat(2) { plan = repo.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE) }
            storage.beforePlanWrite = { error("plan commit failure") }
            assertFailsWith<IllegalStateException> { repo.completeEntry(plan.nextEntry!!.id, Int.MAX_VALUE) }
            assertEquals(1, repo.observeProgress().first().totalCompletedTrainings)
            storage.beforePlanWrite = {}
            storage.focus.value = "malformed"
            assertTrue(repo.ensureTodayTraining().isCompleted)
            assertEquals(1, repo.observeProgress().first().totalCompletedTrainings)
            assertEquals(setOf(101L), repo.observeCompletionHistory().first().completedEpochDays)
        }
}
