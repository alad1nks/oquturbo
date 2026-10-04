package com.alad1nks.oquturbo.feature.stats.ui

import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusPhase
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.feature.stats.data.WeeklyReviewDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class WeeklyFocusViewModelTest {
    @Test
    fun focusLoadingAndFailureNeverHideThreeAvailableReviewSources() =
        exercise {
            source.readGate = CompletableDeferred()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Loading, vm.uiState.value.focus)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.practice)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.training)
            assertIs<WeeklySource.Ready<*>>(vm.uiState.value.comparison)
            source.failReads = true
            source.readGate!!.complete(Unit)
            runCurrent()
            assertEquals(WeeklyFocusUiState.Error, vm.uiState.value.focus)
            source.failReads = false
            vm.retryFocus()
            vm.retryFocus()
            assertEquals(WeeklyFocusUiState.Loading, vm.uiState.value.focus)
            runCurrent()
            assertEquals(WeeklyFocusUiState.Ready(WeeklyFocus()), vm.uiState.value.focus)
            assertEquals(0, source.writes)
            assertEquals(1, source.maximumReaders)
        }

    @Test
    fun duplicateSelectWaitsForAcknowledgementEvenIfCommitAlreadyEmitted() =
        exercise {
            runCurrent()
            source.afterCommit = CompletableDeferred()
            vm.selectFocus()
            vm.selectFocus()
            assertEquals(WeeklyFocusUiState.Saving, vm.uiState.value.focus)
            runCurrent()
            assertEquals(selected, source.stored.value)
            assertEquals(WeeklyFocusUiState.Saving, vm.uiState.value.focus)
            assertEquals(1, source.writes)
            vm.refreshReview()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Saving, vm.uiState.value.focus)
            source.afterCommit!!.complete(Unit)
            runCurrent()
            assertEquals(WeeklyFocusUiState.Ready(selected), vm.uiState.value.focus)
            vm.selectFocus()
            runCurrent()
            assertEquals(1, source.writes)
        }

    @Test
    fun beforeAndAfterCommitFailuresCheckTruthWithoutRetryingMutation() =
        exercise {
            runCurrent()
            for (committed in listOf(false, true)) {
                source.failBefore = !committed
                source.failAfter = committed
                source.readGate = CompletableDeferred()
                vm.selectFocus()
                runCurrent()
                assertEquals(WeeklyFocusUiState.Checking, vm.uiState.value.focus)
                vm.selectFocus()
                vm.disableFocus()
                vm.resetFocus()
                assertEquals(if (committed)1 else 0, source.writes)
                source.readGate!!.complete(Unit)
                runCurrent()
                assertEquals(
                    WeeklyFocusUiState.Ready(if (committed)selected else WeeklyFocus()),
                    vm.uiState.value.focus,
                )
                assertEquals(if (committed)1 else 0, source.writes)
            }
        }

    @Test
    fun failedConfirmationAllowsExplicitCheckAndResetOnlyAfterAcknowledgedWrite() =
        exercise {
            runCurrent()
            source.failAfter = true
            source.failReads = true
            vm.selectFocus()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Unconfirmed, vm.uiState.value.focus)
            assertEquals(1, source.writes)
            source.failReads = false
            vm.retryFocus()
            vm.retryFocus()
            assertEquals(WeeklyFocusUiState.Checking, vm.uiState.value.focus)
            runCurrent()
            assertEquals(WeeklyFocusUiState.Ready(selected), vm.uiState.value.focus)
            source.failAfter = false
            source.failReads = true
            vm.refreshReview()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Error, vm.uiState.value.focus)
            source.failReads = false
            source.afterCommit = CompletableDeferred()
            vm.resetFocus()
            vm.resetFocus()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Saving, vm.uiState.value.focus)
            assertEquals(WeeklyFocus(), source.stored.value)
            source.afterCommit!!.complete(Unit)
            runCurrent()
            assertEquals(WeeklyFocusUiState.Ready(WeeklyFocus()), vm.uiState.value.focus)
            assertEquals(2, source.writes)
        }

    @Test
    fun disableDoesNotClaimOffWhilePendingAndResumeReadsPersistedTruthAfterCancellation() =
        exercise {
            source.stored.value = selected
            runCurrent()
            source.afterCommit = CompletableDeferred()
            vm.disableFocus()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Saving, vm.uiState.value.focus)
            source.afterCommit!!.completeExceptionally(CancellationException("screen closed after commit"))
            runCurrent()
            assertEquals(WeeklyFocusUiState.Loading, vm.uiState.value.focus)
            collector.cancel()
            scope.advanceTimeBy(5_001)
            runCurrent()
            collector = scope.backgroundScope.launch { vm.uiState.collect {} }
            vm.refreshReview()
            runCurrent()
            assertEquals(WeeklyFocusUiState.Ready(WeeklyFocus()), vm.uiState.value.focus)
            assertEquals(1, source.writes)
        }

    @Test
    fun utcStatusChangesWithoutEmissionsAndNoExpiryWriteOrRenewalOccurs() =
        exercise {
            source.stored.value = selected
            runCurrent()
            for ((day, phase) in listOf(
                100L to WeeklyFocusPhase.Scheduled,
                101L to WeeklyFocusPhase.Active,
                108L to WeeklyFocusPhase.Expired,
                103L to WeeklyFocusPhase.Active,
                99L to WeeklyFocusPhase.Scheduled,
            )) {
                clock.day = day
                scope.advanceTimeBy(1_001)
                runCurrent()
                val state = vm.uiState.value
                assertEquals(day, state.todayEpochDay)
                assertEquals(phase, assertIs<WeeklyFocusUiState.Ready>(state.focus).focus.phaseOn(state.todayEpochDay))
                assertEquals(
                    day,
                    assertIs<WeeklySource.Ready<*>>(state.practice).let {
                        (it.value as com.alad1nks.oquturbo.core.data.practice.PracticeRhythm).todayEpochDay
                    },
                )
            }
            assertEquals(0, source.writes)
            assertTrue(source.maximumReaders <= 1)
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
        val source = Source()
        val clock = TestClock()
        val vm = WeeklyReviewViewModel(source, clock)
        var collector = scope.backgroundScope.launch { vm.uiState.collect {} }

        fun runCurrent() = scope.runCurrent()
    }

    private class TestClock(var day: Long = 100) : Clock {
        override fun now() = Instant.fromEpochMilliseconds(day * 86_400_000L)
    }

    private class Source : WeeklyReviewDataSource {
        val stored = MutableStateFlow(WeeklyFocus())
        var readGate: CompletableDeferred<Unit>? = null
        var afterCommit: CompletableDeferred<Unit>? = null
        var failReads = false
        var failBefore = false
        var failAfter = false
        var writes = 0
        var readers = 0
        var maximumReaders = 0

        override fun observeFocus() =
            flow {
                readers++
                maximumReaders = maxOf(maximumReaders, readers)
                try {
                    readGate?.await()
                    check(!failReads) { "focus read failure" }
                    stored.collect { emit(it) }
                } finally {
                    readers--
                }
            }

        private suspend fun write(value: WeeklyFocus): WeeklyFocus {
            check(!failBefore) { "before commit" }
            writes++
            stored.value = value
            afterCommit?.await()
            check(!failAfter) { "acknowledgement lost" }
            return value
        }

        override suspend fun selectFocus() = write(selected)

        override suspend fun disableFocus() = write(WeeklyFocus())

        override suspend fun resetFocus() = write(WeeklyFocus())

        override fun observePractice() = flowOf(DayHistory(80, listOf(98, 99, 100)))

        override fun observeTraining() = flowOf(DayHistory(80, listOf(99)))

        override fun observeSessions() = flowOf(emptyList<GameSession>())
    }

    private companion object {
        val selected = WeeklyFocus(WeeklyFocusSelection(101, 108))
    }
}
