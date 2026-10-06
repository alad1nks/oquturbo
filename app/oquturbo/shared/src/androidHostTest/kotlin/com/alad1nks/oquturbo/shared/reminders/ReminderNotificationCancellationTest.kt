package com.alad1nks.oquturbo.shared.reminders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderNotificationCancellationTest {
    private val owned = 6001 to "oquturbo_practice_reminder"

    @Test
    fun delayedServiceRemovalIsObservedAfterOneCancellation() =
        runTest {
            var cancellations = 0
            var reads = 0
            cancelReminderNotification(
                cancel = { cancellations++ },
                activeNotifications = { if (++reads < 4) listOf(owned) else emptyList() },
            )
            assertEquals(1, cancellations)
            assertEquals(4, reads)
            assertEquals(150, testScheduler.currentTime)
        }

    @Test
    fun unrelatedIdsAndTagsDoNotPreventCompletion() =
        runTest {
            var cancellations = 0
            cancelReminderNotification(
                cancel = { cancellations++ },
                activeNotifications = { listOf(6002 to owned.second, 6001 to "other", 6001 to null) },
            )
            assertEquals(1, cancellations)
            assertEquals(0, testScheduler.currentTime)
        }

    @Test
    fun persistentOwnedNotificationFailsWithinBoundWithoutAnotherCancellation() =
        runTest {
            var cancellations = 0
            assertFailsWith<IllegalStateException> {
                cancelReminderNotification({ cancellations++ }, { listOf(owned) })
            }
            assertEquals(1, cancellations)
            assertEquals(2_000, testScheduler.currentTime)
        }

    @Test
    fun slowNativeReadCannotReportSuccessAfterObservationBudget() =
        runTest {
            var now = 0L
            var cancellations = 0
            assertFailsWith<IllegalStateException> {
                cancelReminderNotification(
                    cancel = { cancellations++ },
                    activeNotifications = {
                        now = 2_000_000_000L
                        emptyList()
                    },
                    monotonicNanos = { now },
                )
            }
            assertEquals(1, cancellations)
        }

    @Test
    fun nativeMutationAndInspectionFailuresPropagate() =
        runTest {
            val failure = SecurityException("native failure")
            var reads = 0
            assertEquals(
                failure.message,
                assertFailsWith<SecurityException> {
                    cancelReminderNotification(
                        { throw failure },
                        {
                            reads++
                            emptyList()
                        },
                    )
                }.message,
            )
            assertEquals(0, reads)
            var cancellations = 0
            assertEquals(
                failure.message,
                assertFailsWith<SecurityException> {
                    cancelReminderNotification({ cancellations++ }, { throw failure })
                }.message,
            )
            assertEquals(1, cancellations)
        }

    @Test
    fun callerCancellationStopsObservationWithoutReportingSuccess() =
        runTest {
            var cancellations = 0
            var reads = 0
            var completed = false
            val job =
                launch {
                    cancelReminderNotification(
                        { cancellations++ },
                        {
                            reads++
                            listOf(owned)
                        },
                    )
                    completed = true
                }
            runCurrent()
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertFalse(completed)
            assertEquals(1, cancellations)
            assertEquals(1, reads)
            assertEquals(0, testScheduler.currentTime)
        }
}
