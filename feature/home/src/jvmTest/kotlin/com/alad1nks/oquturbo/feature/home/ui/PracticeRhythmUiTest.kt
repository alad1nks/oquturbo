package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.data.model.DayHistory
import com.alad1nks.oquturbo.core.data.practice.calculatePracticeRhythm
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PracticeRhythmUiTest {
    @Test
    fun calendarSemanticsDistinguishUnknownCompletedAndTodayWithoutColor() =
        inEnglish {
            runDesktopComposeUiTest(width = 390, height = 1200) {
                setContent {
                    OquTurboTheme {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            PracticeRhythmCard(
                                PracticeRhythmState.Ready(calculatePracticeRhythm(DayHistory(-2, listOf(-1)), 0)),
                                onRetry = {},
                            )
                        }
                    }
                }
                onNodeWithContentDescription("1969-12-26 (UTC): No data").assertExists()
                onNodeWithContentDescription("1969-12-31 (UTC): Practice saved").assertExists()
                onNodeWithContentDescription("Today, 1970-01-01 (UTC): No saved completions yet").assertExists()
                onNodeWithText("Confirmed days: 1. Some history is unknown.").assertExists()
                onNodeWithText("Goal reached").assertDoesNotExist()
            }
        }

    @Test
    fun unavailableAndLoadingDoNotPretendToBeReadyUnknownAndRetryIsReachable() =
        inEnglish {
            runDesktopComposeUiTest(width = 320, height = 640) {
                val state = mutableStateOf<PracticeRhythmState>(PracticeRhythmState.Error)
                var retries = 0
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                PracticeRhythmCard(state.value) {
                                    retries++
                                    state.value = PracticeRhythmState.Loading
                                }
                            }
                        }
                    }
                }
                onNodeWithText("Practice history is unavailable.").assertExists()
                onNodeWithText("Practice: 0 of 7 days").assertDoesNotExist()
                onNodeWithText("Retry").performScrollTo().assertIsDisplayed().performClick()
                assertEquals(1, retries)
                onNodeWithText("Loading practice history…").assertExists()
                onNodeWithText("Retry").assertDoesNotExist()
                onNodeWithContentDescription("Today, 1970-01-01 (UTC): No data").assertDoesNotExist()
                runOnIdle {
                    state.value = PracticeRhythmState.Ready(calculatePracticeRhythm(DayHistory(0, emptyList()), 0))
                }
                onNodeWithContentDescription("Today, 1970-01-01 (UTC): No data").assertExists()
                onNodeWithText("Confirmed days: 0. Some history is unknown.").assertExists()
                onNodeWithText("Loading practice history…").assertDoesNotExist()
            }
        }

    @Test
    fun homeRhythmReadyLoadingReadyPreservesScrollWithoutShowingOldProgress() =
        inEnglish {
            runDesktopComposeUiTest(width = 320, height = 640) {
                val ready =
                    HomeUiState(
                        dailyTraining = previewDailyTraining(false),
                        practiceRhythm =
                            PracticeRhythmState.Ready(
                                calculatePracticeRhythm(DayHistory(0, listOf(2, 4)), 6),
                            ),
                    )
                val state = mutableStateOf(ready)
                lateinit var list: LazyListState
                lateinit var scope: CoroutineScope
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                        OquTurboTheme {
                            list = rememberLazyListState()
                            scope = rememberCoroutineScope()
                            HomeScreen(state.value, onStartTrainingClick = {}, listState = list)
                        }
                    }
                }
                runOnIdle { scope.launch { list.scrollToItem(4) } }
                val scopeText = "Tracking began 1970-01-01 (UTC). The first day may be incomplete."
                onNodeWithText(scopeText).performScrollTo().assertIsDisplayed()
                val before = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                val top = onNodeWithText(scopeText).fetchSemanticsNode().boundsInRoot.top
                runOnIdle { state.value = ready.copy(practiceRhythm = PracticeRhythmState.Loading) }
                onNodeWithText("Confirmed days: 2. Some history is unknown.").assertDoesNotExist()
                onNodeWithText(scopeText).assertDoesNotExist()
                assertEquals(before, runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset })
                runOnIdle { state.value = ready }
                onNodeWithText(scopeText).assertIsDisplayed()
                assertEquals(before, runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset })
                assertEquals(top, onNodeWithText(scopeText).fetchSemanticsNode().boundsInRoot.top, 1f)
            }
        }

    private fun inEnglish(block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
