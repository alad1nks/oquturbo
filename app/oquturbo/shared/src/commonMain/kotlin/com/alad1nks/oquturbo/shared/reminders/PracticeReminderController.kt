package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.reminders.ReminderAuthorization
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderController
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import com.alad1nks.oquturbo.core.data.reminders.ReminderPhase
import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import com.alad1nks.oquturbo.core.data.reminders.ReminderPlatform
import com.alad1nks.oquturbo.core.data.reminders.ReminderState
import com.alad1nks.oquturbo.core.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** OquTurbo owns this instance. Preferences and OS operations are intentionally not a transaction. */
internal class PracticeReminderController(
    private val settings: SettingsRepository,
    private val platform: ReminderPlatform,
    private val scope: CoroutineScope,
    private val content: (String) -> ReminderContent,
) : ReminderController {
    private val mutableState = MutableStateFlow(ReminderState(capability = platform.capability))
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var action: Job? = null
    private var refreshJob: Job? = null
    private var disableQueued = false
    private var generation = 0L
    private var failedOff = false

    // Retained only in this controller: an explicit Retry repeats the same requested payload.
    private var failedWrite: (suspend () -> Unit)? = null

    override fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch { mutex.withLock { reconcile() } }
    }

    override fun chooseTime(labels: ReminderPickerLabels, enable: Boolean) =
        act { token ->
            val (previous, originalSnapshot) =
                mutex.withLock {
                    readState(ReminderPhase.Saving)
                    state.value to settings.readReminderScheduleSnapshot()
                }
            // Native UI can remain open while this Activity is stopped. Never lock the receiver behind it.
            val minutes = platform.chooseTime(previous.schedule?.minutesOfDay, labels)
            val saved =
                mutex.withLock {
                    if (token != generation) return@withLock false
                    val desired = settings.readRemindersDesired()
                    val currentSnapshot = settings.readReminderScheduleSnapshot()
                    if (minutes == null || desired != previous.desired ||
                        currentSnapshot != originalSnapshot
                    ) {
                        reconcile()
                        return@withLock false
                    }
                    val schedule = ReminderSchedule(minutes, resolvedContent())
                    saveThenRead { settings.saveReminderSchedule(schedule, enable) }
                }
            if (saved && enable) requestFromGesture(token, requireRequestable = false)
        }

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            act { token ->
                val saved = mutex.withLock { saveThenRead { settings.enableSavedReminder() } }
                if (saved) requestFromGesture(token, requireRequestable = false)
            }
        } else {
            if (disableQueued || platform.capability != ReminderCapability.Supported) return
            disableQueued = true
            generation++ // Invalidates an outstanding picker/permission callback before it can write.
            scope.launch {
                try {
                    mutex.withLock { disable() }
                } finally {
                    disableQueued = false
                }
            }
        }
    }

    override fun allowPermission() = act { token -> requestFromGesture(token, requireRequestable = true) }

    private suspend fun requestFromGesture(token: Long, requireRequestable: Boolean) {
        val request =
            mutex.withLock {
                if (token != generation || !settings.readRemindersDesired() ||
                    settings.readReminderSchedule() == null
                ) {
                    return@withLock false
                }
                val authorization = platform.inspect().authorization
                val allowed =
                    if (requireRequestable) {
                        authorization == ReminderAuthorization.Requestable
                    } else {
                        authorization != ReminderAuthorization.Allowed
                    }
                if (allowed) mutableState.value = state.value.copy(phase = ReminderPhase.RequestingPermission)
                allowed
            }
        if (request) {
            platform.requestPermission()
            mutex.withLock {
                // No captured preference is written here, even after Off or a changed native permission.
                reconcile()
            }
        }
    }

    override fun openSystemSettings() {
        try {
            platform.openSystemSettings()
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(settingsOpenFailed = true)
        }
    }

    override fun retry() {
        if (state.value.phase == ReminderPhase.CancellationIncomplete) {
            setEnabled(false)
        } else {
            val write = failedWrite
            if (write == null) refresh() else act { mutex.withLock { saveThenRead(write) } }
        }
    }

    private fun act(block: suspend (Long) -> Unit) {
        if (action?.isActive == true || disableQueued || platform.capability != ReminderCapability.Supported) return
        val token = generation
        action =
            scope.launch {
                try {
                    block(token)
                } catch (cancelled: CancellationException) {
                    if (token == generation) mutableState.value = ReminderState(platform.capability)
                    throw cancelled
                } catch (_: Exception) {
                    mutex.withLock { if (token == generation) readState(ReminderPhase.ScheduleError) }
                }
            }
    }

    /** Caller owns the mutex; no native dialog is awaited while it is held. */
    private suspend fun saveThenRead(write: suspend () -> Unit): Boolean {
        mutableState.value = mutableState.value.copy(phase = ReminderPhase.Saving)
        failedWrite = write
        try {
            write()
            failedWrite = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The write may already be committed; preserve the operation and reread, never compensate.
            reconcile()
            return false
        }
        reconcile()
        return true
    }

    private suspend fun disable() {
        failedOff = true
        failedWrite = null
        mutableState.value = mutableState.value.copy(phase = ReminderPhase.Saving)
        var cancelAccepted = false
        try {
            try {
                settings.setRemindersEnabled(false)
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (
                _: Exception,
            ) {
                // Always attempt native cancellation even if saving Off failed.
            }
            try {
                platform.cancel()
                cancelAccepted = true
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (_: Exception) {
                // Inspect and expose incomplete cancellation below.
            }
            readState(ReminderPhase.CancellationIncomplete)
            mutableState.value = state.value.copy(phase = ReminderPhase.CancellationIncomplete)
            if (state.value.desired == false && cancelAccepted) {
                failedOff = false
                mutableState.value = state.value.copy(phase = ReminderPhase.Off)
            }
        } catch (cancelled: CancellationException) {
            mutableState.value = ReminderState(platform.capability)
            throw cancelled
        }
    }

    /** Also used by a bounded receiver operation; the caller supplies its real platform lifetime. */
    suspend fun reconcileNow() = mutex.withLock { reconcile() }

    /** Dispatch and cancellation share the same lock, so a completed Off cannot be followed by an old post. */
    suspend fun dispatch(block: suspend (ReminderSchedule) -> Unit) =
        mutex.withLock {
            try {
                if (!failedOff && settings.readRemindersDesired()) {
                    val schedule = settings.readReminderSchedule()
                    if (schedule != null && platform.inspect().authorization == ReminderAuthorization.Allowed) {
                        block(
                            schedule,
                        )
                    }
                }
                reconcile(refreshContent = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                bestEffortCancel()
                readState(ReminderPhase.ScheduleError)
            }
        }

    private suspend fun reconcile(refreshContent: Boolean = true) {
        if (platform.capability != ReminderCapability.Supported) {
            mutableState.value = ReminderState(platform.capability, ReminderPhase.Unsupported)
            return
        }
        if (failedOff) {
            // A refresh must not erase a failed user operation or resurrect its cancelled request.
            bestEffortCancel()
            readState(ReminderPhase.CancellationIncomplete)
            mutableState.value = state.value.copy(phase = ReminderPhase.CancellationIncomplete)
            return
        }
        mutableState.value = mutableState.value.copy(phase = ReminderPhase.Loading, settingsOpenFailed = false)
        if (!readState(ReminderPhase.Loading)) {
            bestEffortCancel()
            return
        }
        val desired = state.value.desired == true
        var schedule = state.value.schedule
        var cancellationAccepted = false
        try {
            // Cleanup must not depend on locale reads or content writes succeeding.
            if (!desired || schedule == null) {
                platform.cancel()
                cancellationAccepted = true
            }
            if (schedule != null && refreshContent) {
                val updated = schedule.copy(content = resolvedContent())
                if (updated != schedule) settings.saveReminderSchedule(updated)
                schedule = updated
                mutableState.value =
                    state.value.copy(
                        schedule = schedule,
                        timeText = platform.formatTime(schedule.minutesOfDay),
                    )
            }
            if (!desired || schedule == null) {
                mutableState.value =
                    state.value.copy(
                        phase = if (desired) ReminderPhase.NeedsTime else ReminderPhase.Off,
                    )
                return
            }
            val native = platform.inspect()
            if (native.authorization != ReminderAuthorization.Allowed) {
                platform.cancel()
                mutableState.value = state.value.copy(phase = ReminderPhase.SystemBlocked, native = native)
                return
            }
            platform.replace(schedule)
            val accepted = platform.inspect()
            if (accepted.authorization != ReminderAuthorization.Allowed) {
                platform.cancel()
                mutableState.value = state.value.copy(phase = ReminderPhase.SystemBlocked, native = accepted)
            } else {
                val pending = accepted.pending
                check(pending !is ReminderPending.Known || pending.schedule == schedule) { "Request not confirmed" }
                mutableState.value = state.value.copy(phase = ReminderPhase.Scheduled, native = accepted)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            readState(
                if (!desired && !cancellationAccepted) {
                    ReminderPhase.CancellationIncomplete
                } else {
                    ReminderPhase.ScheduleError
                },
            )
        } finally {
            // Reconciliation exposes durable truth but cannot confirm a different requested write.
            if (failedWrite != null && state.value.phase != ReminderPhase.ReadError &&
                state.value.phase != ReminderPhase.CancellationIncomplete
            ) {
                mutableState.value = state.value.copy(phase = ReminderPhase.Unconfirmed)
            }
        }
    }

    private suspend fun resolvedContent(): ReminderContent {
        val language = settings.getLanguage().first().code ?: platform.systemLanguage()
        return content(language.takeIf { it in setOf("en", "ru", "kk") } ?: "en")
    }

    private suspend fun bestEffortCancel() {
        try {
            platform.cancel()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
    }

    private suspend fun readState(phase: ReminderPhase): Boolean {
        var valid = true
        val desired =
            try {
                settings.readRemindersDesired()
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (_: Exception) {
                valid = false
                null
            }
        val schedule =
            try {
                settings.readReminderSchedule()
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (_: Exception) {
                valid = false
                null
            }
        val native =
            try {
                platform.inspect()
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        val old = (native?.pending as? ReminderPending.Known)?.schedule
        mutableState.value =
            ReminderState(
                platform.capability,
                if (valid) phase else ReminderPhase.ReadError,
                desired,
                schedule,
                schedule?.let { platform.formatTime(it.minutesOfDay) },
                native,
                old?.let { platform.formatTime(it.minutesOfDay) },
            )
        return valid
    }
}
