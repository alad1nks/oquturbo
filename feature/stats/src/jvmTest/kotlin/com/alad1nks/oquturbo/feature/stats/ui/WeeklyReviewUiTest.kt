package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class WeeklyReviewUiTest {
    @Test
    fun partialFirstDayUsesIndependentConfirmedCountsAndNoWeeklyGrowthClaim() =
        localized("en") {
            runDesktopComposeUiTest(width = 390, height = 2400) {
                val state =
                    previewWeeklyReview().copy(
                        practice = WeeklySource.Ready(calculatePracticeRhythm(DayHistory(20_089, emptyList()), 20_089)),
                        training =
                            WeeklySource.Ready(
                                calculatePracticeRhythm(DayHistory(20_000, listOf(20_089)), 20_089),
                            ),
                    )
                setContent { OquTurboTheme { WeeklyReviewScreen(state, {}, {}, {}, {}, {}, {}) } }
                onNodeWithText("Confirmed days: 0. Some history is unknown.", useUnmergedTree = true).assertExists()
                onNodeWithText("1 of 7 days", useUnmergedTree = true).assertExists()
                onNodeWithText("2024-12-26 – 2025-01-01 (UTC)").assertExists()
                onNodeWithText("2024-12-05 – 2025-01-01 (UTC)").assertExists()
                onNodeWithText("+2", useUnmergedTree = true).assertExists()
                onNodeWithText("Today is still in progress.").assertExists()
                onNodeWithText("Goal reached").assertDoesNotExist()
            }
        }

    @Test
    fun eachRetryHasSourceSemanticsAndNeverInvokesAnotherSourceOrHome() =
        localized("en") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                val state =
                    mutableStateOf(
                        WeeklyReviewUiState(20_089, WeeklySource.Error, WeeklySource.Error, WeeklySource.Error),
                    )
                val retry = IntArray(3)
                var homes = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            WeeklyReviewScreen(
                                state.value,
                                {},
                                { homes++ },
                                {},
                                {
                                    retry[0]++
                                    state.value = state.value.copy(practice = WeeklySource.Loading)
                                },
                                {
                                    retry[1]++
                                    state.value = state.value.copy(training = WeeklySource.Loading)
                                },
                                {
                                    retry[2]++
                                    state.value = state.value.copy(comparison = WeeklySource.Loading)
                                },
                            )
                        }
                    }
                }
                val labels = listOf("Practice days", "Days with full training", "Latest comparable result")
                labels.forEachIndexed { index, label ->
                    val description = "Retry loading: $label"
                    onNode(
                        hasScrollAction(),
                    ).performScrollToNode(androidx.compose.ui.test.hasContentDescription(description))
                    onNodeWithContentDescription(description).assertIsDisplayed().performClick()
                    assertEquals(List(3) { if (it <= index) 1 else 0 }, retry.toList())
                    onNodeWithContentDescription(description).assertDoesNotExist()
                }
                onNodeWithText("0 of 7 days", useUnmergedTree = true).assertDoesNotExist()
                onNodeWithText("Mode statistics").assertDoesNotExist()
                assertEquals(0, homes)
                onNode(hasScrollAction()).performScrollToNode(hasText("Home"))
                onNodeWithText("Home").assertIsDisplayed().performClick()
                assertEquals(1, homes)
            }
        }

    @Test
    fun savedReviewPositionSurvivesFreshLoadingWithoutOldNumbersOrModeAction() =
        localized("en") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                val ready = previewWeeklyReview()
                val state = mutableStateOf(ready)
                val visible = mutableStateOf(true)
                lateinit var list: LazyListState
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            val holder = rememberSaveableStateHolder()
                            if (visible.value) {
                                holder.SaveableStateProvider("review") {
                                    list = rememberLazyListState()
                                    WeeklyReviewScreen(state.value, {}, {}, {}, {}, {}, {}, listState = list)
                                }
                            }
                        }
                    }
                }
                onNode(hasScrollAction()).performScrollToNode(hasText("Mode statistics"))
                val before = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                val top = onNodeWithText("Mode statistics").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
                runOnIdle { visible.value = false }
                runOnIdle {
                    state.value = WeeklyReviewUiState(20_089)
                    visible.value = true
                }
                onNodeWithText("Mode statistics").assertDoesNotExist()
                onNodeWithText("+2", useUnmergedTree = true).assertDoesNotExist()
                assertEquals(before, runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset })
                runOnIdle { state.value = ready }
                assertEquals(before, runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset })
                assertEquals(
                    top,
                    onNodeWithText("Mode statistics").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top,
                    1f,
                )
            }
        }

    @Test
    fun russianCompactFooterWrapsFullyAndRemainsReachableWithAllSourcesUnavailable() =
        localized("ru") {
            runDesktopComposeUiTest(width = 320, height = 640) {
                var homes = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            WeeklyReviewScreen(
                                WeeklyReviewUiState(20_089, WeeklySource.Error, WeeklySource.Error, WeeklySource.Error),
                                {},
                                { homes++ },
                                {},
                                {},
                                {},
                                {},
                            )
                        }
                    }
                }
                onNode(hasScrollAction()).performScrollToNode(hasText("На главную"))
                val button = onNodeWithText("На главную").assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                button.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(1, layouts.size)
                assertFalse(layouts.single().hasVisualOverflow)
                button.performClick()
                assertEquals(1, homes)
            }
        }

    @Test
    fun savedLanguageAndCustomDigitsAreRenderedInsteadOfOpaqueVariantIdentifiers() =
        localized("en") {
            runDesktopComposeUiTest(width = 390, height = 2300) {
                val state =
                    mutableStateOf(
                        previewWeeklyReview().copy(
                            comparison =
                                WeeklySource.Ready(
                                    ProgressComparison.InsufficientData(
                                        20_062,
                                        20_089,
                                        GameSeriesKey(
                                            GameId.NumberSprint,
                                            GameModeId.NumberSprintCustom,
                                            "digits:013579;length:12",
                                        ),
                                        9,
                                    ),
                                ),
                        ),
                    )
                setContent { OquTurboTheme { WeeklyReviewScreen(state.value, {}, {}, {}, {}, {}, {}) } }
                onNode(hasScrollAction()).performScrollToNode(hasText("Mode statistics"))
                onNodeWithText("digits:013579;length:12").assertDoesNotExist()
                onNodeWithText("013579", substring = true).assertExists()
                runOnIdle {
                    state.value =
                        state.value.copy(
                            comparison =
                                WeeklySource.Ready(
                                    ProgressComparison.InsufficientData(
                                        20_062,
                                        20_089,
                                        GameSeriesKey(GameId.WordFlow, GameModeId.WordFlowContext, "ru"),
                                        1,
                                    ),
                                ),
                        )
                }
                onNodeWithText("Russian").assertExists()
            }
        }

    private fun localized(tag: String, block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(tag))
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
