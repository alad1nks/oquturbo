package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderPhase
import com.alad1nks.oquturbo.core.data.reminders.ReminderState
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class ReminderSettingsUiTest {
    @Test fun unknownDesiredIsNotOffAndBusyKeepsBackAndOtherSettingsAvailable() =
        localized("en") {
            runDesktopComposeUiTest(width = 390, height = 800) {
                val state = mutableStateOf(ReminderState())
                var back = 0
                setContent {
                    OquTurboTheme {
                        ProfileSettingsScreen(
                            ProfileSettingsUiState(),
                            {},
                            {},
                            {},
                            {},
                            {},
                            { back++ },
                            state.value,
                        )
                    }
                }
                onNode(hasScrollAction()).performScrollToNode(hasText("Checking the setting…"))
                onNodeWithContentDescription("Reminders").assertDoesNotExist()
                for (phase in listOf(ReminderPhase.Saving, ReminderPhase.RequestingPermission)) {
                    runOnIdle { state.value = reminderPreviewState(phase, true) }
                    onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Reminders"))
                    onNodeWithContentDescription("Reminders").assertIsOn().assertIsNotEnabled()
                    onNode(hasScrollAction()).performScrollToNode(hasText("Privacy policy"))
                    onNodeWithText("Privacy policy").assertIsDisplayed()
                }
                onAllNodes(hasClickAction()).onFirst().performClick()
                assertEquals(1, back)
            }
        }

    @Test fun legacyAndCancellationIncompleteExposeDesiredSeparatelyFromActualAvailability() =
        localized("en") {
            runDesktopComposeUiTest(width = 390, height = 1600) {
                val state = mutableStateOf(reminderPreviewState(ReminderPhase.NeedsTime, true, null))
                var enabledIntent: Boolean? = null
                var retry = 0
                setContent {
                    OquTurboTheme {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            ReminderSettingsCard(
                                state.value,
                                {},
                                { _, enable -> enabledIntent = enable },
                                {},
                                {},
                                { retry++ },
                            )
                        }
                    }
                }
                onNodeWithContentDescription("Reminders").assertIsOn()
                onNodeWithText("Choose time and enable").performClick()
                assertEquals(true, enabledIntent)
                onNodeWithText("Scheduled").assertDoesNotExist()
                runOnIdle { state.value = reminderPreviewState(ReminderPhase.CancellationIncomplete, false) }
                onNodeWithContentDescription("Reminders").assertIsOff().assertIsNotEnabled()
                onNodeWithText("Turning off is not complete.").assertExists()
                onNodeWithText("Retry turning off").performClick()
                assertEquals(1, retry)
                runOnIdle { state.value = ReminderState(ReminderCapability.Unsupported, ReminderPhase.Unsupported) }
                onNodeWithContentDescription("Reminders").assertDoesNotExist()
                onNodeWithText("Choose time and enable").assertDoesNotExist()
            }
        }

    @Test fun narrowRussianAndKazakhRecoveryActionsWrapWithoutOverflow() {
        for ((locale, labels) in listOf(
            "ru" to listOf("Повторить", "Выбрать новое время", "Выключить напоминания"),
            "kk" to listOf("Қайталау", "Жаңа уақыт таңдау", "Еске салғыштарды өшіру"),
        )) localized(locale) {
            runDesktopComposeUiTest(width = 320, height = 640) {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            ProfileSettingsScreen(ProfileSettingsUiState(), {
                            }, {}, {}, {}, {}, {}, reminderPreviewState(ReminderPhase.ReadError, null, null))
                        }
                    }
                }
                for (label in labels) {
                    onNode(hasScrollAction()).performScrollToNode(hasText(label))
                    val node = onNodeWithText(label).assertIsDisplayed()
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(1, layouts.size)
                    assertFalse(layouts.single().hasVisualOverflow, label)
                }
            }
        }
    }

    private fun localized(code: String, block: () -> Unit) {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(code))
            block()
        } finally {
            Locale.setDefault(old)
        }
    }
}
