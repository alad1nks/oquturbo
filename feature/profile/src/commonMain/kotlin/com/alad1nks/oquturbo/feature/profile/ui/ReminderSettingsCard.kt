package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.reminders.ReminderAuthorization
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import com.alad1nks.oquturbo.core.data.reminders.ReminderPhase
import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import com.alad1nks.oquturbo.core.data.reminders.ReminderState
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReminderSettingsCard(
    state: ReminderState,
    onEnabled: (Boolean) -> Unit,
    onChooseTime: (ReminderPickerLabels, Boolean) -> Unit,
    onAllow: () -> Unit,
    onSystemSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    val title = stringResource(AppResource.String.profile_settings_reminders)
    val status =
        stringResource(
            when (state.phase) {
                ReminderPhase.Loading -> AppResource.String.reminder_checking
                ReminderPhase.Off -> AppResource.String.reminder_off
                ReminderPhase.NeedsTime -> AppResource.String.reminder_needs_time
                ReminderPhase.Saving -> AppResource.String.reminder_saving
                ReminderPhase.RequestingPermission -> AppResource.String.reminder_requesting
                ReminderPhase.Scheduled ->
                    if (state.native?.limited == true) {
                        AppResource.String.reminder_limited
                    } else {
                        AppResource.String.reminder_scheduled
                    }
                ReminderPhase.SystemBlocked ->
                    if (state.native?.authorization == ReminderAuthorization.Requestable) {
                        AppResource.String.reminder_permission_needed
                    } else {
                        AppResource.String.reminder_blocked
                    }
                ReminderPhase.ReadError -> AppResource.String.reminder_read_error
                ReminderPhase.ScheduleError -> AppResource.String.reminder_schedule_error
                ReminderPhase.Unconfirmed -> AppResource.String.reminder_save_unconfirmed
                ReminderPhase.CancellationIncomplete -> AppResource.String.reminder_cancel_incomplete
                ReminderPhase.Unsupported ->
                    if (state.capability == ReminderCapability.IosPending) {
                        AppResource.String.reminder_ios_pending6a
                    } else {
                        AppResource.String.reminder_unsupported
                    }
            },
        )
    val picker =
        ReminderPickerLabels(
            stringResource(AppResource.String.reminder_picker_title),
            stringResource(AppResource.String.reminder_picker_local),
            stringResource(AppResource.String.reminder_save_time),
            stringResource(AppResource.String.reminder_cancel),
        )
    val saveEnable = stringResource(AppResource.String.reminder_save_enable)

    fun choose(enable: Boolean) = onChooseTime(if (enable) picker.copy(confirm = saveEnable) else picker, enable)
    val enabled = !state.busy && state.phase != ReminderPhase.CancellationIncomplete
    AppCard(Modifier.fillMaxWidth(), compact = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            if (state.desired != null && state.capability == ReminderCapability.Supported) {
                Switch(
                    state.desired == true,
                    { value ->
                        if (value && state.schedule == null) choose(true) else onEnabled(value)
                    },
                    enabled = enabled,
                    modifier =
                        Modifier.semantics {
                            contentDescription = title
                            stateDescription = status
                        },
                )
            }
            ReminderText(AppResource.String.reminder_experimental)
            ReminderText(AppResource.String.reminder_purpose)
            state.timeText?.let {
                Text(
                    stringResource(AppResource.String.reminder_time_label, it),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (state.busy) CircularProgressIndicator(Modifier.size(32.dp))
            Text(status, style = MaterialTheme.typography.titleMedium)
            when (state.phase) {
                ReminderPhase.Off -> {
                    if (state.schedule == null) {
                        ReminderAction(AppResource.String.reminder_choose_enable) { choose(true) }
                    } else {
                        ReminderAction(AppResource.String.reminder_change_time) { choose(false) }
                    }
                }
                ReminderPhase.NeedsTime -> {
                    ReminderText(AppResource.String.reminder_not_scheduled)
                    ReminderAction(AppResource.String.reminder_choose_enable) { choose(true) }
                }
                ReminderPhase.Scheduled -> {
                    state.timeText?.let { ReminderText(stringResource(AppResource.String.reminder_around_time, it)) }
                    ReminderText(AppResource.String.reminder_accepted)
                    if (state.native?.limited == true) {
                        ReminderText(AppResource.String.reminder_quiet_hint)
                        ReminderAction(AppResource.String.reminder_system_settings, onSystemSettings)
                    }
                    ReminderAction(AppResource.String.reminder_change_time) { choose(false) }
                }
                ReminderPhase.SystemBlocked -> {
                    if (state.native?.authorization == ReminderAuthorization.Requestable) {
                        ReminderAction(
                            AppResource.String.reminder_allow,
                            onAllow,
                        )
                    }
                    ReminderAction(AppResource.String.reminder_system_settings, onSystemSettings)
                    ReminderAction(AppResource.String.reminder_change_time) { choose(false) }
                }
                ReminderPhase.ScheduleError -> {
                    when (val pending = state.native?.pending) {
                        is ReminderPending.Known ->
                            if (pending.schedule == null) {
                                ReminderText(AppResource.String.reminder_not_scheduled)
                            } else {
                                state.oldPendingTimeText?.let {
                                    ReminderText(
                                        stringResource(AppResource.String.reminder_old_pending, it),
                                    )
                                }
                            }
                        else -> ReminderText(AppResource.String.reminder_pending_unknown)
                    }
                    ReminderAction(AppResource.String.reminder_retry, onRetry)
                    ReminderAction(AppResource.String.reminder_change_time) { choose(false) }
                    ReminderAction(AppResource.String.reminder_turn_off) { onEnabled(false) }
                }
                ReminderPhase.ReadError, ReminderPhase.Unconfirmed -> {
                    ReminderAction(AppResource.String.reminder_retry, onRetry)
                    ReminderAction(AppResource.String.reminder_replace_time) { choose(state.desired != false) }
                    ReminderAction(AppResource.String.reminder_turn_off) { onEnabled(false) }
                }
                ReminderPhase.CancellationIncomplete ->
                    ReminderAction(
                        AppResource.String.reminder_retry_cancel,
                        onRetry,
                    )
                else -> Unit
            }
            if (state.settingsOpenFailed) ReminderText(AppResource.String.reminder_open_settings_error)
            ReminderText(AppResource.String.reminder_os_caveat)
            if (state.capability == ReminderCapability.Supported) {
                ReminderText(
                    AppResource.String.reminder_after_practice,
                )
            }
        }
    }
}

@Composable
private fun ReminderText(resource: StringResource) = ReminderText(stringResource(resource))

@Composable
private fun ReminderText(
    value: String,
) {
    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ReminderAction(label: StringResource, onClick: () -> Unit) {
    TextButton(
        onClick,
        Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(label),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
