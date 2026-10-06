package com.alad1nks.oquturbo.core.data.reminders

import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ReminderCapability { Supported, Unsupported, IosPending }

enum class ReminderAuthorization { Allowed, Requestable, Blocked }

sealed interface ReminderPending {
    data object Unknown : ReminderPending

    data class Known(val schedule: ReminderSchedule?) : ReminderPending
}

data class ReminderNativeState(
    val authorization: ReminderAuthorization,
    val limited: Boolean = false,
    val pending: ReminderPending = ReminderPending.Unknown,
)

enum class ReminderPhase {
    Loading,
    Off,
    NeedsTime,
    Saving,
    RequestingPermission,
    Scheduled,
    SystemBlocked,
    ReadError,
    ScheduleError,
    Unconfirmed,
    CancellationIncomplete,
    Unsupported,
}

data class ReminderState(
    val capability: ReminderCapability = ReminderCapability.Supported,
    val phase: ReminderPhase = ReminderPhase.Loading,
    val desired: Boolean? = null,
    val schedule: ReminderSchedule? = null,
    val timeText: String? = null,
    val native: ReminderNativeState? = null,
    val oldPendingTimeText: String? = null,
    val settingsOpenFailed: Boolean = false,
) {
    val busy: Boolean get() =
        phase in
            setOf(
                ReminderPhase.Loading,
                ReminderPhase.Saving,
                ReminderPhase.RequestingPermission,
            )
}

data class ReminderPickerLabels(val title: String, val helper: String, val confirm: String, val cancel: String)

interface ReminderController {
    val state: StateFlow<ReminderState>

    fun refresh()

    fun chooseTime(labels: ReminderPickerLabels, enable: Boolean)

    fun setEnabled(enabled: Boolean)

    fun allowPermission()

    fun openSystemSettings()

    fun retry()
}

class UnavailableReminderController(
    capability: ReminderCapability = ReminderCapability.Unsupported,
) : ReminderController {
    override val state: StateFlow<ReminderState> =
        MutableStateFlow(ReminderState(capability, ReminderPhase.Unsupported))

    override fun refresh() = Unit

    override fun chooseTime(labels: ReminderPickerLabels, enable: Boolean) = Unit

    override fun setEnabled(enabled: Boolean) = Unit

    override fun allowPermission() = Unit

    override fun openSystemSettings() = Unit

    override fun retry() = Unit
}

interface ReminderPlatform {
    val capability: ReminderCapability

    fun systemLanguage(): String

    fun formatTime(minutesOfDay: Int): String

    suspend fun inspect(): ReminderNativeState

    suspend fun replace(schedule: ReminderSchedule)

    suspend fun cancel()

    suspend fun requestPermission()

    suspend fun chooseTime(initialMinutes: Int?, labels: ReminderPickerLabels): Int?

    suspend fun openSystemSettings()
}
