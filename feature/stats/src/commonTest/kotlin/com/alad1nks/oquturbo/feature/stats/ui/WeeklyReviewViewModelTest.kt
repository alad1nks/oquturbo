package com.alad1nks.oquturbo.feature.stats.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.feature.stats.data.WeeklyReviewDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class WeeklyReviewViewModelTest {
    @Test
    fun delayedSourcesStayIndependentAndTrainingOnlyDoesNotCreditPractice() =
        exercise {
            source.practice.gate = CompletableDeferred()
            source.sessions.gate = CompletableDeferred()
            source.training.value = DayHistory(80, listOf(98, 99, 100))
            runCurrent()
            assertEquals(WeeklySource.Loading, vm.uiState.value.practice)
            assertEquals(WeeklySource.Loading, vm.uiState.value.comparison)
            assertEquals(3, vm.uiState.value.training.ready().completedDaysInWindow)
            source.practice.gate!!.complete(Unit)
            source.sessions.gate!!.complete(Unit)
            runCurrent()
            assertEquals(0, vm.uiState.value.practice.ready().completedDaysInWindow)
            assertIs<ProgressComparison.NoRecentSessions>(vm.uiState.value.comparison.ready())
            assertEquals(3, vm.uiState.value.training.ready().completedDaysInWindow)
        }

    @Test
    fun partialHistoriesHaveDifferentStartsAndNeverBorrowEachOthersDays() =
        exercise {
            source.practice.value = DayHistory(97, listOf(94, 97, 99))
            source.training.value = DayHistory(100, listOf(100))
            runCurrent()
            assertEquals(3, vm.uiState.value.practice.ready().completedDaysInWindow)
            assertTrue(vm.uiState.value.practice.ready().hasUnknownDays)
            assertEquals(97L, vm.uiState.value.practice.ready().trackingStartedEpochDay)
            assertEquals(1, vm.uiState.value.training.ready().completedDaysInWindow)
            assertTrue(vm.uiState.value.training.ready().hasUnknownDays)
            assertEquals(100L, vm.uiState.value.training.ready().trackingStartedEpochDay)
        }

    @Test
    fun practiceAndTrainingCountsUseDistinctSevenDayFactsIncludingPartialGoalReached() =
        exercise {
            for (count in listOf(0, 3, 4, 7)) {
                val days = (0 until count).map { 100L - it }
                source.practice.value = DayHistory(98, days + days + listOf(93L, 101L))
                source.training.value = DayHistory(80, emptyList())
                runCurrent()
                assertEquals(count, vm.uiState.value.practice.ready().completedDaysInWindow)
                assertEquals(count >= 4, vm.uiState.value.practice.ready().weeklyGoalReached)
                assertEquals(count < 7, vm.uiState.value.practice.ready().hasUnknownDays)
                assertEquals(0, vm.uiState.value.training.ready().completedDaysInWindow)
            }
            source.training.value = DayHistory(80, (94L..100L).toList() + listOf(94L, 101L))
            runCurrent()
            assertEquals(7, vm.uiState.value.training.ready().completedDaysInWindow)
            assertTrue(vm.uiState.value.training.ready().hasFutureFacts)
        }

    @Test
    fun comparisonUsesLatestExactSeriesAcross28DaysEvenWhenWeekHasNoPractice() =
        exercise {
            source.sessions.value = (0..9).map { session(index = it, day = 75) }
            runCurrent()
            assertEquals(0, vm.uiState.value.practice.ready().completedDaysInWindow)
            val compared = assertIs<ProgressComparison.Compared>(vm.uiState.value.comparison.ready())
            assertEquals(2, compared.previousMedian)
            assertEquals(4, compared.currentMedian)
            assertEquals(2, compared.absoluteChange)
            assertEquals(73L, compared.firstEpochDay)
            source.sessions.value += session(index = 20, day = 100).copy(variantId = "other-series")
            runCurrent()
            val latest = assertIs<ProgressComparison.InsufficientData>(vm.uiState.value.comparison.ready())
            assertEquals("other-series", latest.series.variantId)
            assertEquals(1, latest.availableCount)
        }

    @Test
    fun everySourceHasLocalErrorImmediateCoalescedRetryAndNoStaleReady() =
        exercise {
            runCurrent()
            for (index in 0..2) {
                val reader = source.readers[index]
                val state: () -> WeeklySource<*> =
                    when (index) {
                        0 -> {
                            { vm.uiState.value.practice }
                        }
                        1 -> {
                            { vm.uiState.value.training }
                        }
                        else -> {
                            { vm.uiState.value.comparison }
                        }
                    }
                val retry =
                    when (index) {
                        0 -> vm::retryPractice
                        1 -> vm::retryTraining
                        else -> vm::retrySessions
                    }
                reader.fail(IllegalStateException("read failure"))
                runCurrent()
                assertEquals(WeeklySource.Error, state())
                assertEquals(
                    2,
                    listOf(vm.uiState.value.practice, vm.uiState.value.training, vm.uiState.value.comparison)
                        .count { it is WeeklySource.Ready },
                )
                reader.gate = CompletableDeferred()
                reader.recover()
                retry()
                retry()
                assertEquals(WeeklySource.Loading, state())
                runCurrent()
                assertEquals(1, reader.active)
                assertEquals(1, reader.maximumActive)
                reader.gate!!.complete(Unit)
                reader.gate = null
                runCurrent()
                assertIs<WeeklySource.Ready<*>>(state())
            }
        }

    @Test
    fun midnightAndAsynchronousReadAlwaysPublishOneDayForAllProjections() =
        exercise {
            source.practice.value = DayHistory(80, listOf(94, 100, 101))
            source.training.value = DayHistory(80, listOf(94, 101))
            source.sessions.value = (0..9).map { session(it, day = 73) }
            runCurrent()
            assertTrue(vm.uiState.value.practice.ready().hasFutureFacts)
            assertTrue(vm.uiState.value.training.ready().hasFutureFacts)
            clock.day = 101
            source.training.value = DayHistory(80, listOf(94, 100, 101))
            runCurrent()
            val state = vm.uiState.value
            assertEquals(101L, state.todayEpochDay)
            assertEquals(101L, state.practice.ready().todayEpochDay)
            assertEquals(101L, state.training.ready().todayEpochDay)
            assertEquals(2, state.practice.ready().completedDaysInWindow)
            assertEquals(2, state.training.ready().completedDaysInWindow)
            assertIs<ProgressComparison.NoRecentSessions>(state.comparison.ready())
            clock.day = 102
            advanceTimeBy(1_001)
            runCurrent()
            assertEquals(102L, vm.uiState.value.todayEpochDay)
            assertEquals(102L, vm.uiState.value.practice.ready().todayEpochDay)
            assertEquals(102L, vm.uiState.value.training.ready().todayEpochDay)
        }

    @Test
    fun resumeRefreshHidesCachedValuesAndReprojectsEverySourceAfterClockRollback() =
        exercise {
            source.practice.value = DayHistory(80, listOf(98, 99, 100))
            source.training.value = DayHistory(80, listOf(100))
            source.sessions.value = (0..9).map { session(it, day = 100) }
            runCurrent()
            assertIs<ProgressComparison.Compared>(vm.uiState.value.comparison.ready())
            source.readers.forEach { it.gate = CompletableDeferred() }
            clock.day = 99
            vm.refreshReview()
            vm.refreshReview()
            assertEquals(WeeklyReviewUiState(99), vm.uiState.value)
            runCurrent()
            assertTrue(source.readers.all { it.active == 1 && it.maximumActive == 1 })
            source.readers.forEach {
                it.gate!!.complete(Unit)
                it.gate = null
            }
            runCurrent()
            val state = vm.uiState.value
            assertEquals(99L, state.todayEpochDay)
            assertEquals(2, state.practice.ready().completedDaysInWindow)
            assertEquals(0, state.training.ready().completedDaysInWindow)
            assertTrue(state.practice.ready().hasFutureFacts)
            assertTrue(state.training.ready().hasFutureFacts)
            assertIs<ProgressComparison.NoRecentSessions>(state.comparison.ready())
        }

    @Test
    fun stoppedSubscriptionAndResumeRequireFreshReadsWhileShortGapKeepsOnlyOneCollector() =
        exercise {
            runCurrent()
            collector.cancel()
            advanceTimeBy(4_000)
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
            runCurrent()
            advanceTimeBy(2_000)
            runCurrent()
            assertTrue(source.readers.all { it.active == 1 && it.maximumActive == 1 })
            collector.cancel()
            advanceTimeBy(5_001)
            runCurrent()
            assertTrue(source.readers.all { it.active == 0 })
            source.readers.forEach { it.gate = CompletableDeferred() }
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
            vm.refreshReview()
            vm.refreshReview()
            runCurrent()
            assertEquals(WeeklySource.Loading, vm.uiState.value.practice)
            assertEquals(WeeklySource.Loading, vm.uiState.value.training)
            assertEquals(WeeklySource.Loading, vm.uiState.value.comparison)
            source.readers.forEach {
                it.gate!!.complete(Unit)
                it.gate = null
            }
            runCurrent()
            assertTrue(source.readers.all { it.active == 1 && it.maximumActive == 1 })
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.comparison)
        }

    @Test
    fun cancelledReadDoesNotTurnIntoErrorOrEmptyResult() =
        exercise {
            source.sessions.gate = CompletableDeferred()
            runCurrent()
            source.sessions.gate!!.completeExceptionally(CancellationException("cancel source"))
            runCurrent()
            assertEquals(WeeklySource.Loading, vm.uiState.value.comparison)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.practice)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.training)
        }

    private fun exercise(block: suspend Fixture.() -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val fixture = Fixture(this)
            try {
                fixture.block()
            } finally {
                fixture.vm.viewModelScope.cancel()
                backgroundScope.cancel()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private class Fixture(val scope: TestScope) {
        val source = Sources()
        val clock = TestClock()
        val vm = WeeklyReviewViewModel(source, clock)
        var collector = scope.backgroundScope.launch { vm.uiState.collect {} }

        fun runCurrent() = scope.runCurrent()

        fun advanceTimeBy(time: Long) = scope.advanceTimeBy(time)
    }

    private class TestClock(var day: Long = 100) : Clock {
        override fun now() = Instant.fromEpochMilliseconds(day * DAY)
    }

    private class Sources : WeeklyReviewDataSource {
        val practice = Reader(DayHistory(80, emptyList()))
        val training = Reader(DayHistory(80, emptyList()))
        val sessions = Reader<List<GameSession>>(emptyList())
        val readers: List<Reader<*>> get() = listOf(practice, training, sessions)

        override fun observePractice() = practice.observe()

        override fun observeTraining() = training.observe()

        override fun observeSessions() = sessions.observe()
    }

    private class Reader<T>(initial: T) {
        data class Value<T>(val value: T, val failure: Throwable? = null)

        private val values = MutableStateFlow(Value(initial))
        var value: T
            get() = values.value.value
            set(value) {
                values.value = Value(value)
            }
        var gate: CompletableDeferred<Unit>? = null
        var active = 0
        var maximumActive = 0

        fun fail(error: Throwable) {
            values.value = values.value.copy(failure = error)
        }

        fun recover() {
            values.value = values.value.copy(failure = null)
        }

        fun observe(): Flow<T> =
            flow {
                active++
                maximumActive = maxOf(active, maximumActive)
                try {
                    values.collect { read ->
                        gate?.await()
                        read.failure?.let { throw it }
                        emit(read.value)
                    }
                } finally {
                    active--
                }
            }
    }

    private companion object {
        const val DAY = 86_400_000L

        fun session(index: Int, day: Long) =
            GameSession(
                game = GameId.NumberSprint,
                mode = GameModeId.NumberSprintClassic,
                variantId = null,
                score = if (index < 5) 2 else 4,
                correctAnswers = 0,
                durationMillis = 0,
                completedAtEpochMillis = day * DAY + index,
                completedEpochDay = day,
                isNewRecord = false,
            )
    }
}

private fun <T> WeeklySource<T>.ready(): T = assertIs<WeeklySource.Ready<T>>(this).value
