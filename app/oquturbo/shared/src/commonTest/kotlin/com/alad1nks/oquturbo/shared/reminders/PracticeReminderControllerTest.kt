package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.reminders.ReminderAuthorization
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderNativeState
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import com.alad1nks.oquturbo.core.data.reminders.ReminderPhase
import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import com.alad1nks.oquturbo.core.data.reminders.ReminderPlatform
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PracticeReminderControllerTest {
    private val labels = ReminderPickerLabels("Title", "Local time", "Save", "Cancel")

    private fun scenario(block: suspend Fixture.() -> Unit) =
        runTest {
            val f = Fixture(this)
            try {
                f.block()
            } finally {
                backgroundScope.cancel()
                f.app.close()
            }
        }

    @Test fun offLegacyAndCancelPickerNeverScheduleOrPromptAndUnsupportedPreservesPrefs() =
        scenario {
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertEquals(0, platform.replaces)
            assertEquals(0, platform.prompts)
            assertTrue(prefs.writes.isEmpty())
            prefs.enabled.value = true
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.NeedsTime, controller.state.value.phase)
            platform.chosen = null
            controller.chooseTime(labels, true)
            tick()
            assertEquals(ReminderPhase.NeedsTime, controller.state.value.phase)
            assertTrue(prefs.writes.isEmpty())
            platform.capability = ReminderCapability.Unsupported
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.Unsupported, controller.state.value.phase)
            assertTrue(prefs.writes.isEmpty())
        }

    @Test fun cancelledUnchangedDraftPreservesScheduledOffAndNeedsTimeWithoutEffects() =
        scenario {
            val phases =
                listOf(ReminderPhase.Scheduled, ReminderPhase.Off, ReminderPhase.NeedsTime, ReminderPhase.ScheduleError)
            for (phase in phases) {
                seed()
                if (phase == ReminderPhase.Off) repo.setRemindersEnabled(false)
                if (phase == ReminderPhase.NeedsTime) prefs.values["reminders_schedule_v1"]!!.value = null
                platform.failReplace = phase == ReminderPhase.ScheduleError
                controller.refresh()
                tick()
                assertEquals(phase, controller.state.value.phase)
                val before = controller.state.value
                val writes = prefs.writes.toList()
                val replacements = platform.replaces
                val cancellations = platform.cancels
                val pending = platform.pending
                controller.chooseTime(labels, phase == ReminderPhase.NeedsTime)
                tick()
                assertEquals(before.phase, controller.state.value.phase)
                assertEquals(before.desired, controller.state.value.desired)
                assertEquals(before.schedule, controller.state.value.schedule)
                assertEquals(ReminderPending.Known(pending), controller.state.value.native!!.pending)
                assertEquals(writes, prefs.writes)
                assertEquals(replacements, platform.replaces)
                assertEquals(cancellations, platform.cancels)
                assertEquals(pending, platform.pending)
                assertEquals(0, platform.prompts)
                if (phase == ReminderPhase.ScheduleError) {
                    platform.failReplace = false
                    controller.retry()
                    tick()
                    assertEquals(ReminderPhase.Scheduled, controller.state.value.phase)
                }
            }
        }

    @Test fun cancelledDraftDoesNotOverwriteConcurrentRefreshOrLateOff() =
        scenario {
            seed()
            controller.refresh()
            tick()
            val picker = CompletableDeferred<Int?>()
            platform.picker = { picker.await() }
            controller.chooseTime(labels, false)
            tick()
            platform.authorization = ReminderAuthorization.Blocked
            controller.refresh()
            tick()
            val refreshed = controller.state.value
            val cancellations = platform.cancels
            picker.complete(null)
            tick()
            assertEquals(ReminderPhase.SystemBlocked, refreshed.phase)
            assertEquals(refreshed, controller.state.value)
            assertEquals(cancellations, platform.cancels)
            val latePicker = CompletableDeferred<Int?>()
            platform.picker = { latePicker.await() }
            controller.chooseTime(labels, false)
            tick()
            controller.setEnabled(false)
            tick()
            val off = controller.state.value
            val writes = prefs.writes.toList()
            val afterOffCancels = platform.cancels
            latePicker.complete(null)
            tick()
            assertEquals(ReminderPhase.Off, off.phase)
            assertEquals(off, controller.state.value)
            assertEquals(writes, prefs.writes)
            assertEquals(afterOffCancels, platform.cancels)
        }

    @Test fun cancelledDraftKeepsFailedOffRecoveryAndReconcilesExternalIntentChange() =
        scenario {
            seed()
            controller.refresh()
            tick()
            prefs.beforeBoolean = { error("off write") }
            controller.setEnabled(false)
            tick()
            val failed = controller.state.value
            val cancels = platform.cancels
            controller.chooseTime(labels, false)
            tick()
            assertEquals(ReminderPhase.CancellationIncomplete, failed.phase)
            assertEquals(failed, controller.state.value)
            assertEquals(cancels, platform.cancels)
            prefs.beforeBoolean = {}
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            seed()
            controller.refresh()
            tick()
            val picker = CompletableDeferred<Int?>()
            platform.picker = { picker.await() }
            controller.chooseTime(labels, false)
            tick()
            repo.setRemindersEnabled(false)
            val writes = prefs.writes.toList()
            picker.complete(null)
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertNull(platform.pending)
            assertEquals(writes, prefs.writes)
        }

    @Test fun confirmedSchedulePrecedesDesiredAndGesturePermissionThenOneAcceptedRequest() =
        scenario {
            platform.authorization = ReminderAuthorization.Requestable
            platform.chosen = 1100
            platform.onPrompt = {
                assertTrue(prefs.enabled.value == true)
                assertNotNull(prefs.values["reminders_schedule_v1"]?.value)
                platform.authorization = ReminderAuthorization.Allowed
            }
            controller.chooseTime(labels, true)
            controller.chooseTime(labels, true)
            tick()
            assertEquals(listOf("reminders_schedule_v1", "reminders_enabled"), prefs.writes)
            assertEquals(1, platform.prompts)
            assertEquals(1, platform.replaces)
            assertEquals(ReminderPhase.Scheduled, controller.state.value.phase)
            assertEquals(1100, platform.pending!!.minutesOfDay)
        }

    @Test fun failedBooleanRetainsTimeButDoesNotEnableAndLostAckRereadsCommittedTruth() =
        scenario {
            platform.chosen = 50
            prefs.beforeBoolean = { error("before desire") }
            controller.chooseTime(labels, true)
            tick()
            assertEquals(ReminderPhase.Unconfirmed, controller.state.value.phase)
            assertFalse(controller.state.value.desired!!)
            assertEquals(0, platform.replaces)
            assertEquals(50, repo.readReminderSchedule()!!.minutesOfDay)
            prefs.beforeBoolean = {}
            prefs.afterBoolean = { error("after desire") }
            controller.setEnabled(true)
            tick()
            assertEquals(ReminderPhase.Unconfirmed, controller.state.value.phase)
            assertEquals(1, platform.replaces)
            assertTrue(repo.readRemindersDesired())
            assertEquals(1, prefs.writes.count { it == "reminders_schedule_v1" })
        }

    @Test fun disablingAttemptsCancelDespiteFailedSaveAndRetainsIncompleteTruthUntilRetry() =
        scenario {
            seed()
            prefs.beforeBoolean = { error("cannot write off") }
            controller.setEnabled(false)
            tick()
            assertEquals(1, platform.cancels)
            assertTrue(controller.state.value.desired == true)
            assertEquals(ReminderPhase.CancellationIncomplete, controller.state.value.phase)
            prefs.beforeBoolean = {}
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertFalse(repo.readRemindersDesired())
            assertEquals(900, repo.readReminderSchedule()!!.minutesOfDay)
        }

    @Test fun corruptScheduleNeverBecomesOffAndExplicitOffWorksWithoutDecodingIt() =
        scenario {
            prefs.enabled.value = true
            prefs.values.getOrPut("reminders_schedule_v1") { MutableStateFlow(null) }.value = "null"
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.ReadError, controller.state.value.phase)
            assertEquals(0, platform.replaces)
            platform.failCancel = true
            controller.setEnabled(false)
            tick()
            assertEquals(ReminderPhase.CancellationIncomplete, controller.state.value.phase)
            assertFalse(controller.state.value.desired!!)
            platform.failCancel = false
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertEquals("null", prefs.values["reminders_schedule_v1"]!!.value)
        }

    @Test fun saveCommitThenUnreadableNeverClaimsScheduledOrCompensates() =
        scenario {
            platform.chosen = 60
            prefs.afterString = {
                prefs.failRead = true
                error("lost ack")
            }
            controller.chooseTime(labels, true)
            tick()
            assertEquals(ReminderPhase.ReadError, controller.state.value.phase)
            assertNull(controller.state.value.schedule)
            assertEquals(0, platform.replaces)
            assertEquals(1, prefs.writes.size)
            prefs.failRead = false
            prefs.afterString = {}
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Scheduled, controller.state.value.phase)
            assertEquals(60, controller.state.value.schedule!!.minutesOfDay)
            assertEquals(0, platform.prompts)
        }

    @Test fun deniedRefreshRetryAndEditingNeverPromptButExplicitAllowDoes() =
        scenario {
            seed()
            platform.authorization = ReminderAuthorization.Requestable
            controller.refresh()
            tick()
            controller.retry()
            tick()
            assertEquals(ReminderPhase.SystemBlocked, controller.state.value.phase)
            assertEquals(0, platform.prompts)
            platform.chosen = 910
            controller.chooseTime(labels, false)
            tick()
            assertEquals(0, platform.prompts)
            controller.allowPermission()
            tick()
            assertEquals(1, platform.prompts)
            assertEquals(ReminderPhase.SystemBlocked, controller.state.value.phase)
        }

    @Test fun acceptedOffAfterPendingPermissionWinsAndRepeatedDisableCoalesces() =
        scenario {
            seed()
            platform.authorization = ReminderAuthorization.Requestable
            val gate = CompletableDeferred<Unit>()
            platform.onPrompt = {
                gate.await()
                platform.authorization = ReminderAuthorization.Allowed
            }
            controller.refresh()
            tick()
            controller.allowPermission()
            tick()
            assertEquals(ReminderPhase.RequestingPermission, controller.state.value.phase)
            controller.setEnabled(false)
            controller.setEnabled(false)
            tick()
            assertFalse(repo.readRemindersDesired())
            assertNull(platform.pending)
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            gate.complete(Unit)
            tick()
            assertFalse(repo.readRemindersDesired())
            assertNull(platform.pending)
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertEquals(1, prefs.writes.count { it == "reminders_enabled" } - 1)
        }

    @Test fun replacedRequestFailureReportsOldPendingAndLocaleRefreshKeepsTimeWithoutPrompt() =
        scenario {
            seed()
            controller.refresh()
            tick()
            val old = platform.pending
            platform.failReplace = true
            platform.chosen = 1000
            controller.chooseTime(labels, false)
            tick()
            assertEquals(ReminderPhase.ScheduleError, controller.state.value.phase)
            assertEquals(old, (controller.state.value.native!!.pending as ReminderPending.Known).schedule)
            assertEquals(1000, controller.state.value.schedule!!.minutesOfDay)
            platform.failReplace = false
            prefs.values.getOrPut("language") { MutableStateFlow(null) }.value = "ru"
            controller.refresh()
            tick()
            assertEquals("ru", platform.pending!!.content.languageCode)
            assertEquals(1000, platform.pending!!.minutesOfDay)
            assertEquals(0, platform.prompts)
        }

    @Test fun revocationDuringReplaceNeverReportsScheduledAndDispatchHasNoSideEffectsWhenOff() =
        scenario {
            seed()
            platform.afterReplace = { platform.authorization = ReminderAuthorization.Blocked }
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.SystemBlocked, controller.state.value.phase)
            assertNull(platform.pending)
            repo.setRemindersEnabled(false)
            var posts = 0
            controller.dispatch { posts++ }
            assertEquals(0, posts)
            assertTrue(prefs.writes.all { it in setOf("reminders_enabled", "reminders_schedule_v1") })
        }

    @Test fun cancelledPostcommitSaveIsRereadByNextControllerWithoutCompensation() =
        scenario {
            val owner = SupervisorJob(scope.backgroundScope.coroutineContext[Job])
            val operationScope = CoroutineScope(scope.backgroundScope.coroutineContext + owner)
            val first =
                PracticeReminderController(
                    repo,
                    platform,
                    operationScope,
                ) { ReminderContent(it, "Title $it", "Body $it") }
            val gate = CompletableDeferred<Unit>()
            prefs.afterString = { gate.await() }
            platform.chosen = 123
            first.chooseTime(labels, true)
            tick()
            owner.cancel()
            tick()
            prefs.afterString = {}
            controller.refresh()
            tick()
            assertEquals(123, controller.state.value.schedule!!.minutesOfDay)
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertEquals(1, prefs.writes.size)
        }

    @Test fun offCancellationDoesNotDependOnLocaleReadContentResolutionOrSnapshotWrite() =
        scenario {
            seed()
            repo.setRemindersEnabled(false)
            for (failure in 0..2) {
                platform.pending = repo.readReminderSchedule()
                prefs.failLanguage = failure == 0
                failContent = failure == 1
                prefs.values.getOrPut("language") { MutableStateFlow(null) }.value = "ru"
                prefs.beforeString = { if (failure == 2) error("snapshot save") }
                val before = platform.cancels
                controller.refresh()
                tick()
                assertEquals(before + 1, platform.cancels)
                assertNull(platform.pending)
                assertFalse(controller.state.value.desired!!)
                assertEquals(ReminderPhase.ScheduleError, controller.state.value.phase)
            }
        }

    @Test fun failedNewTimeStaysVisibleAcrossRefreshAndExplicitRetryUsesFixedPayloadWithoutPrompt() =
        scenario {
            seed()
            controller.refresh()
            tick()
            prefs.beforeString = { error("before commit") }
            platform.chosen = 1000
            controller.chooseTime(labels, false)
            tick()
            assertEquals(ReminderPhase.Unconfirmed, controller.state.value.phase)
            assertEquals(900, controller.state.value.schedule!!.minutesOfDay)
            assertEquals(900, platform.pending!!.minutesOfDay)
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.Unconfirmed, controller.state.value.phase)
            prefs.beforeString = {}
            platform.chosen = 1200
            controller.retry()
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Scheduled, controller.state.value.phase)
            assertEquals(1000, repo.readReminderSchedule()!!.minutesOfDay)
            assertEquals(0, platform.prompts)
        }

    @Test fun retryBeforeFirstWriteAndBetweenKeysPreservesEnableIntentWithoutPermissionPrompt() =
        scenario {
            platform.authorization = ReminderAuthorization.Requestable
            platform.chosen = 72
            prefs.beforeString = { error("before schedule") }
            controller.chooseTime(labels, true)
            tick()
            assertEquals(ReminderPhase.Unconfirmed, controller.state.value.phase)
            assertNull(controller.state.value.schedule)
            assertFalse(controller.state.value.desired!!)
            prefs.beforeString = {}
            prefs.beforeBoolean = { error("before enabled") }
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Unconfirmed, controller.state.value.phase)
            assertEquals(72, controller.state.value.schedule!!.minutesOfDay)
            assertFalse(controller.state.value.desired!!)
            prefs.beforeBoolean = {}
            controller.retry()
            tick()
            assertEquals(ReminderPhase.SystemBlocked, controller.state.value.phase)
            assertTrue(controller.state.value.desired!!)
            assertEquals(0, platform.prompts)
        }

    @Test fun suspendedPickerDoesNotBlockDispatchOrOffAndItsLateConfirmationCannotEnable() =
        scenario {
            seed()
            controller.refresh()
            tick()
            val picker = CompletableDeferred<Int?>()
            platform.picker = { picker.await() }
            controller.chooseTime(labels, true)
            tick()
            var posts = 0
            val dispatch = scope.backgroundScope.launch { controller.dispatch { posts++ } }
            tick()
            assertTrue(dispatch.isCompleted)
            assertEquals(1, posts)
            assertEquals(900, platform.pending!!.minutesOfDay)
            controller.setEnabled(false)
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertFalse(repo.readRemindersDesired())
            assertNull(platform.pending)
            val writes = prefs.writes.toList()
            picker.complete(1000)
            tick()
            assertEquals(writes, prefs.writes)
            assertFalse(repo.readRemindersDesired())
            assertEquals(900, repo.readReminderSchedule()!!.minutesOfDay)
            assertEquals(0, platform.prompts)
        }

    @Test fun suspendedPickerRejectsAnExternallyChangedDesiredPreference() =
        scenario {
            seed()
            controller.refresh()
            tick()
            val picker = CompletableDeferred<Int?>()
            platform.picker = { picker.await() }
            controller.chooseTime(labels, true)
            tick()
            repo.setRemindersEnabled(false)
            picker.complete(1000)
            tick()
            assertFalse(repo.readRemindersDesired())
            assertEquals(900, repo.readReminderSchedule()!!.minutesOfDay)
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
        }

    @Test
    fun explicitPickerRepairsReadableMalformedScheduleButCancelAndOffNeverDo() =
        scenario {
            for (raw in listOf("null", "{\"version\":99}")) {
                prefs.enabled.value = true
                prefs.values.getOrPut("reminders_schedule_v1") { MutableStateFlow(null) }.value = raw
                controller.refresh()
                tick()
                platform.chosen = null
                val before = prefs.writes.toList()
                controller.chooseTime(labels, false)
                tick()
                assertEquals(before, prefs.writes)
                assertEquals(raw, repo.readReminderScheduleSnapshot())
                platform.chosen = 300
                controller.chooseTime(labels, false)
                tick()
                assertEquals(300, repo.readReminderSchedule()!!.minutesOfDay)
                assertTrue(repo.readRemindersDesired())
                assertEquals(ReminderPhase.Scheduled, controller.state.value.phase)
                assertEquals(listOf("reminders_schedule_v1"), prefs.writes.drop(before.size))
            }
            prefs.values["reminders_schedule_v1"]!!.value = "null"
            val picker = CompletableDeferred<Int?>()
            platform.picker = { picker.await() }
            controller.chooseTime(labels, false)
            tick()
            controller.setEnabled(false)
            tick()
            picker.complete(600)
            tick()
            assertFalse(repo.readRemindersDesired())
            assertEquals("null", repo.readReminderScheduleSnapshot())
        }

    @Test
    fun unavailableStorageCannotBeReplacedByPickerConfirmation() =
        scenario {
            seed()
            controller.refresh()
            tick()
            val picker = CompletableDeferred<Int?>()
            platform.picker = { picker.await() }
            controller.chooseTime(labels, false)
            tick()
            val before = prefs.writes.toList()
            prefs.failRead = true
            picker.complete(1000)
            tick()
            assertEquals(before, prefs.writes)
            assertEquals(ReminderPhase.ReadError, controller.state.value.phase)
            assertNull(controller.state.value.schedule)
        }

    @Test
    fun failedOffSurvivesRefreshAndDispatchUntilExplicitRetryWithoutHidingDurableTrue() =
        scenario {
            seed()
            controller.refresh()
            tick()
            prefs.beforeBoolean = { error("off write failed") }
            controller.setEnabled(false)
            tick()
            val writes = prefs.writes.toList()
            val replaces = platform.replaces
            controller.refresh()
            tick()
            assertEquals(ReminderPhase.CancellationIncomplete, controller.state.value.phase)
            assertTrue(controller.state.value.desired!!)
            assertEquals(writes, prefs.writes)
            assertEquals(replaces, platform.replaces)
            assertNull(platform.pending)
            var posts = 0
            controller.dispatch { posts++ }
            assertEquals(0, posts)
            assertEquals(ReminderPhase.CancellationIncomplete, controller.state.value.phase)
            prefs.beforeBoolean = {}
            controller.retry()
            tick()
            assertEquals(ReminderPhase.Off, controller.state.value.phase)
            assertFalse(repo.readRemindersDesired())
            assertEquals(0, platform.prompts)
        }

    @Test
    fun settingsResultWaitsForActualCallbackAndFailureDoesNotChangePreferencesOrSchedule() =
        scenario {
            seed()
            controller.refresh()
            tick()
            val before = prefs.writes.toList()
            val pending = platform.pending
            val result = CompletableDeferred<Unit>()
            platform.openSettings = { result.await() }
            controller.openSystemSettings()
            tick()
            assertFalse(controller.state.value.settingsOpenFailed)
            result.completeExceptionally(IllegalStateException("open returned false"))
            tick()
            assertTrue(controller.state.value.settingsOpenFailed)
            assertEquals(before, prefs.writes)
            assertEquals(pending, platform.pending)
            platform.openSettings = {}
            controller.openSystemSettings()
            tick()
            assertFalse(controller.state.value.settingsOpenFailed)
            assertEquals(0, platform.prompts)
        }

    @Test
    fun cancelledSettingsOpenDoesNotBecomeSuccessOrOverwriteExistingFailure() =
        scenario {
            platform.openSettings = { error("not opened") }
            controller.openSystemSettings()
            tick()
            assertTrue(controller.state.value.settingsOpenFailed)
            platform.openSettings = { throw kotlinx.coroutines.CancellationException("host cancelled") }
            controller.openSystemSettings()
            tick()
            assertTrue(controller.state.value.settingsOpenFailed)
            assertTrue(prefs.writes.isEmpty())
        }

    private class Fixture(val scope: TestScope) {
        val prefs = Preferences()
        val app = koinApplication { modules(StorageCommonModule, module { single<AppPreferences> { prefs } }) }
        val repo = SettingsRepository(app.koin.get<Storage>())
        val platform = Platform()
        var failContent = false
        val controller =
            PracticeReminderController(repo, platform, scope.backgroundScope) {
                if (failContent) error("resources")
                ReminderContent(it, "Title $it", "Body $it")
            }

        suspend fun seed() {
            repo.saveReminderSchedule(ReminderSchedule(900, ReminderContent("en", "Title en", "Body en")), true)
        }

        fun tick() = scope.runCurrent()
    }

    private class Platform : ReminderPlatform {
        override var capability = ReminderCapability.Supported
        var authorization = ReminderAuthorization.Allowed
        var chosen: Int? = null
        var pending: ReminderSchedule? = null
        var prompts = 0
        var replaces = 0
        var cancels = 0
        var failCancel = false
        var failReplace = false
        var picker: (suspend () -> Int?)? = null
        var onPrompt: suspend() -> Unit = {}
        var afterReplace: () -> Unit = {}

        override fun systemLanguage() = "en"

        override fun formatTime(minutesOfDay: Int) = "${minutesOfDay / 60}:${minutesOfDay % 60}"

        override suspend fun inspect() = ReminderNativeState(authorization, pending = ReminderPending.Known(pending))

        override suspend fun replace(schedule: ReminderSchedule) {
            if (failReplace)error("replace")
            replaces++
            pending = schedule
            afterReplace()
        }

        override suspend fun cancel() {
            cancels++
            if (failCancel)error("cancel")
            pending = null
        }

        override suspend fun requestPermission() {
            prompts++
            onPrompt()
        }

        override suspend fun chooseTime(initialMinutes: Int?, labels: ReminderPickerLabels) = picker?.invoke() ?: chosen

        var openSettings: suspend () -> Unit = {}

        override suspend fun openSystemSettings() = openSettings()
    }

    private class Preferences : AppPreferences {
        val values = mutableMapOf<String, MutableStateFlow<String?>>()
        val enabled = MutableStateFlow<Boolean?>(null)
        val writes = mutableListOf<String>()
        var failLanguage = false
        var beforeString: () -> Unit = {}
        var beforeBoolean: () -> Unit = {}
        var afterBoolean: () -> Unit = {}
        var afterString: suspend() -> Unit = {}
        var failRead = false

        override fun getString(key: String): Flow<String?> =
            flow {
                if (failRead || (failLanguage && key == "language"))error("read")
                emitAll(values.getOrPut(key) { MutableStateFlow(null) })
            }

        override fun getBoolean(key: String): Flow<Boolean?> = if (key == "reminders_enabled") enabled else flowOf(null)

        override fun getInt(key: String): Flow<Int?> = flowOf(null)

        override suspend fun setString(key: String, value: String) {
            beforeString()
            writes += key
            values.getOrPut(key) { MutableStateFlow(null) }.value = value
            afterString()
        }

        override suspend fun setBoolean(key: String, value: Boolean) {
            beforeBoolean()
            writes += key
            enabled.value = value
            afterBoolean()
        }

        override suspend fun setInt(key: String, value: Int) = error("No game write")
    }
}
