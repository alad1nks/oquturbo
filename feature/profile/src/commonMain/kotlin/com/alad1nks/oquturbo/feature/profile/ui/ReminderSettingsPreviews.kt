package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.reminders.ReminderAuthorization
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderNativeState
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import com.alad1nks.oquturbo.core.data.reminders.ReminderPhase
import com.alad1nks.oquturbo.core.data.reminders.ReminderState
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.resources.reminderResourceContent

internal fun reminderPreviewState(phase: ReminderPhase, desired: Boolean? = true, minutes: Int? = 1110): ReminderState {
    val content = reminderResourceContent("en")
    return ReminderState(
        phase = phase,
        desired = desired,
        schedule =
            minutes?.let {
                ReminderSchedule(it, ReminderContent("en", content.title, content.body))
            },
        timeText =
            minutes?.let {
                "${it / 60}:${(it % 60).toString().padStart(2,'0')}"
            },
        native = ReminderNativeState(ReminderAuthorization.Allowed),
    )
}

@Composable
private fun ReminderPreview(state: ReminderState, dark: Boolean = false) {
    OquTurboTheme(darkTheme = dark) {
        Column(Modifier.fillMaxSize().appBackground().padding(24.dp)) {
            ReminderSettingsCard(state, {}, { _, _ -> }, {}, {}, {})
        }
    }
}

@Preview(name = "Reminders full Settings Off", widthDp = 390, heightDp = 1700, locale = "en")
@ScreenshotPreview
@Composable
private fun RemindersOffPreview() {
    OquTurboTheme {
        ProfileSettingsScreen(ProfileSettingsUiState(), {
        }, {}, {}, {}, {}, {}, reminderPreviewState(ReminderPhase.Off, false, null))
    }
}

@Preview(name = "Reminders legacy NeedsTime", widthDp = 320, heightDp = 1600, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersLegacyPreview() = ReminderPreview(reminderPreviewState(ReminderPhase.NeedsTime, true, null))

@Preview(name = "Reminders Off saved time", widthDp = 390, heightDp = 1000, locale = "en")
@ScreenshotPreview
@Composable
private fun RemindersSavedOffPreview() = ReminderPreview(reminderPreviewState(ReminderPhase.Off, false))

@Preview(name = "Reminders Scheduled limited", widthDp = 320, heightDp = 2000, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersScheduledPreview() =
    ReminderPreview(
        reminderPreviewState(
            ReminderPhase.Scheduled,
        ).copy(native = ReminderNativeState(ReminderAuthorization.Allowed, true)),
        true,
    )

@Preview(name = "Reminders Loading Saving Requesting", widthDp = 390, heightDp = 2600, locale = "en")
@ScreenshotPreview
@Composable
private fun RemindersBusyPreview() {
    OquTurboTheme {
        Column(
            Modifier.fillMaxSize().appBackground().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(ReminderPhase.Loading, ReminderPhase.Saving, ReminderPhase.RequestingPermission).forEach {
                ReminderSettingsCard(
                    reminderPreviewState(
                        it,
                        if (it == ReminderPhase.Loading)null else true,
                        if (it == ReminderPhase.Loading)null else 1110,
                    ),
                    {
                    },
                    { _, _ -> },
                    {},
                    {},
                    {},
                )
            }
        }
    }
}

@Preview(name = "Reminders SystemBlocked", widthDp = 320, heightDp = 1900, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersBlockedPreview() =
    ReminderPreview(
        reminderPreviewState(
            ReminderPhase.SystemBlocked,
        ).copy(native = ReminderNativeState(ReminderAuthorization.Requestable)),
    )

@Preview(name = "Reminders replace error old pending", widthDp = 320, heightDp = 2200, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersReplaceErrorPreview() =
    ReminderPreview(
        reminderPreviewState(ReminderPhase.ScheduleError, minutes = 1140).copy(
            native =
                ReminderNativeState(
                    ReminderAuthorization.Allowed,
                    pending = ReminderPending.Known(reminderPreviewState(ReminderPhase.Scheduled).schedule),
                ),
            oldPendingTimeText = "18:30",
        ),
    )

@Preview(name = "Reminders malformed read error", widthDp = 390, heightDp = 1400, locale = "en")
@ScreenshotPreview
@Composable
private fun RemindersReadErrorPreview() = ReminderPreview(reminderPreviewState(ReminderPhase.ReadError, null, null))

@Preview(name = "Reminders cancellation incomplete", widthDp = 320, heightDp = 1800, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersCancelErrorPreview() =
    ReminderPreview(
        reminderPreviewState(ReminderPhase.CancellationIncomplete, false),
        true,
    )

@Preview(name = "Reminders Unsupported", widthDp = 320, heightDp = 1400, locale = "kk", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersUnsupportedPreview() =
    ReminderPreview(
        ReminderState(ReminderCapability.Unsupported, ReminderPhase.Unsupported),
    )

@Preview(name = "Reminders iOS pending 6a", widthDp = 390, heightDp = 900, locale = "en")
@ScreenshotPreview
@Composable
private fun RemindersIosPendingPreview() =
    ReminderPreview(
        ReminderState(ReminderCapability.IosPending, ReminderPhase.Unsupported),
    )

@Preview(name = "Reminders scrolled recovery", widthDp = 320, heightDp = 640, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RemindersScrolledPreview() {
    OquTurboTheme {
        ProfileSettingsScreen(
            ProfileSettingsUiState(),
            {},
            {},
            {},
            {},
            {},
            {},
            reminderPreviewState(ReminderPhase.ReadError, true, null),
            listState = rememberLazyListState(4, 650),
        )
    }
}
