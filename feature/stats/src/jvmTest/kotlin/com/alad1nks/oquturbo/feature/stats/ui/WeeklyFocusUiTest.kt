package com.alad1nks.oquturbo.feature.stats.ui

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
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.data.model.WeeklyFocus
import com.alad1nks.oquturbo.core.data.model.WeeklyFocusSelection
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class WeeklyFocusUiTest {
    @Test
    fun offScheduledActiveExpiredHaveDistinctActionsAndAbsoluteProvisionalOrSavedDates() =
        localized("en") {
            runDesktopComposeUiTest(width = 390, height = 1800) {
                val state = mutableStateOf<WeeklyFocusUiState>(WeeklyFocusUiState.Ready(WeeklyFocus()))
                val today = mutableStateOf(20_089L)
                var select = 0
                var disable = 0
                setContent {
                    OquTurboTheme {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            WeeklyFocusSection(state.value, today.value, { select++ }, { disable++ }, {}, {})
                        }
                    }
                }
                onNodeWithText("Proposed dates: 2025-01-02 – 2025-01-08 (UTC)").assertExists()
                onNodeWithText("Select for 7 days").performScrollTo().performClick()
                assertEquals(1, select)
                runOnIdle { state.value = WeeklyFocusUiState.Ready(WeeklyFocus(WeeklyFocusSelection(20_090, 20_097))) }
                onNodeWithText("Focus scheduled").assertExists()
                onNodeWithText("2025-01-02 – 2025-01-08 (UTC)").assertExists()
                onNodeWithText("Select for 7 days").assertDoesNotExist()
                onNodeWithText("Cancel focus").performClick()
                runOnIdle { today.value = 20_090 }
                onNodeWithText("Focus active").assertExists()
                onNodeWithText("Turn off focus").performClick()
                assertEquals(2, disable)
                runOnIdle { today.value = 20_097 }
                onNodeWithText("Focus ended").assertExists()
                onNodeWithText("Finished period: 2025-01-02 – 2025-01-08 (UTC)").assertExists()
                onNodeWithText("Proposed dates: 2025-01-10 – 2025-01-16 (UTC)").assertExists()
                onNodeWithText("Select again for 7 days").performClick()
                assertEquals(2, select)
            }
        }

    @Test
    fun errorAndUnconfirmedRecoveryMeanDifferentActionsAndBusyStatesKeepHomeEnabled() =
        localized("en") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                val state = mutableStateOf(previewWeeklyReview().copy(focus = WeeklyFocusUiState.Error))
                var retry = 0
                var reset = 0
                var home = 0
                setContent {
                    OquTurboTheme {
                        WeeklyReviewScreen(
                            state.value,
                            {},
                            { home++ },
                            {},
                            {},
                            {},
                            {},
                            onRetryFocus = { retry++ },
                            onResetFocus = { reset++ },
                        )
                    }
                }
                onNode(hasScrollAction()).performScrollToNode(hasText("Retry loading focus"))
                onNodeWithText("Retry loading focus").performClick()
                onNode(hasScrollAction()).performScrollToNode(hasText("Reset focus"))
                onNodeWithText(
                    "Reset only turns off the focus setting. Games, history and the existing plan stay unchanged.",
                ).assertExists()
                onNodeWithText("Reset focus").performClick()
                assertEquals(1, retry)
                assertEquals(1, reset)
                runOnIdle { state.value = state.value.copy(focus = WeeklyFocusUiState.Unconfirmed) }
                onNode(hasScrollAction()).performScrollToNode(hasText("Check setting"))
                onNodeWithText("Check setting").performClick()
                assertEquals(2, retry)
                for (busy in listOf(
                    WeeklyFocusUiState.Loading,
                    WeeklyFocusUiState.Saving,
                    WeeklyFocusUiState.Checking,
                )) {
                    runOnIdle { state.value = state.value.copy(focus = busy) }
                    onNodeWithText("Select for 7 days").assertDoesNotExist()
                    onNodeWithText("Reset focus").assertDoesNotExist()
                    onNodeWithText("Cancel focus").assertDoesNotExist()
                    onNode(hasScrollAction()).performScrollToNode(hasText("Home"))
                    onNodeWithText("Home").assertIsDisplayed().performClick()
                }
                assertEquals(3, home)
            }
        }

    @Test
    fun russianLargeTextActionsWrapWithoutOverflowAndResetExplanationComesFirst() =
        localized("ru") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            WeeklyReviewScreen(
                                previewWeeklyReview().copy(focus = WeeklyFocusUiState.Error),
                                {},
                                {},
                                {},
                                {},
                                {},
                                {},
                            )
                        }
                    }
                }
                for (label in listOf("Повторить загрузку фокуса", "Сбросить фокус", "На главную")) {
                    onNode(hasScrollAction()).performScrollToNode(hasText(label))
                    val node = onNodeWithText(label).assertIsDisplayed()
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(1, layouts.size)
                    assertFalse(layouts.single().hasVisualOverflow, label)
                }
            }
        }

    @Test
    fun maximumStoredFocusRangeUsesLongDatesAndLastActiveDayInsteadOfExclusiveEnd() =
        localized("en") {
            runDesktopComposeUiTest(width = 390, height = 1900) {
                setContent {
                    OquTurboTheme {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            WeeklyFocusSection(
                                WeeklyFocusUiState.Ready(
                                    WeeklyFocus(WeeklyFocusSelection(Long.MAX_VALUE - 7, Long.MAX_VALUE)),
                                ),
                                100,
                                {},
                                {},
                                {},
                                {},
                            )
                        }
                    }
                }
                onNodeWithText("25252734927768524-07-20 – 25252734927768524-07-26 (UTC)").assertExists()
                onNodeWithText("25252734927768524-07-27", substring = true).assertDoesNotExist()
            }
        }

    private fun localized(locale: String, block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(locale))
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
