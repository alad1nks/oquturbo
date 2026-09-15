package com.alad1nks.oquturbo.feature.symbolcount.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountFailure
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SymbolCountScreenSemanticsTest {
    @Test
    fun compactStageFiveKeepsWholeFieldAndAllAnswersVisibleWithStandardTargets() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    OquTurboTheme {
                        SymbolCountScreen(
                            symbolCountPreviewState(SymbolCountPhase.Active, 5),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 640.dp),
                        )
                    }
                }
            }
            onNodeWithTag("field").assertIsDisplayed()
            for (n in listOf(2, 5, 6, 1)) {
                val node = onNodeWithTag("answer-$n").assertIsDisplayed().assertIsEnabled().fetchSemanticsNode()
                assertTrue(node.boundsInRoot.height >= 48)
                assertTrue(node.boundsInRoot.bottom <= 640)
            }
            onNodeWithContentDescription("Pause").assertIsDisplayed()
        }

    @Test
    fun pauseRemovesFieldTargetAndAnswersAndResumeRetainsChoices() =
        runComposeUiTest {
            val initial = symbolCountPreviewState(SymbolCountPhase.Active)
            val state = mutableStateOf(initial)
            var answered = 0
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(
                        state.value,
                        {},
                        { _, value -> answered = value },
                        null,
                        onPauseClick = {
                            state.value =
                                initial.copy(
                                    game = initial.game.copy(phase = SymbolCountPhase.Paused),
                                )
                        },
                        onResumeClick = { state.value = initial },
                    )
                }
            }
            onNodeWithTag("answer-3").performClick()
            assertEquals(3, answered)
            onNodeWithContentDescription("Pause").performClick()
            onNodeWithTag("field").assertDoesNotExist()
            onNodeWithTag("target").assertDoesNotExist()
            for (n in 1..4) onNodeWithTag("answer-$n").assertDoesNotExist()
            onNodeWithContentDescription("Back to Games").assertDoesNotExist()
            onNodeWithText("Resume").performClick()
            onNodeWithTag("answer-3").assertIsEnabled()
        }

    @Test
    fun correctFeedbackDisablesAllOptionsButPreservesPause() =
        runComposeUiTest {
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Correct), {}, { _, _ -> }, {})
                }
            }
            for (n in 1..4) onNodeWithTag("answer-$n").assertIsNotEnabled()
            onNodeWithText("Correct").assertExists()
            onNodeWithContentDescription("Pause").assertIsEnabled()
        }

    @Test
    fun savingAndLoadFailureHaveTruthfulSingleRecoveryActions() =
        runComposeUiTest {
            val state = mutableStateOf(SymbolCountUiState())
            var retries = 0
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(state.value, {}, { _, _ -> }, null, onRetrySaveClick = { retries++ })
                }
            }
            onNodeWithText("Start").assertIsNotEnabled()
            state.value =
                symbolCountPreviewState(
                    SymbolCountPhase.Result,
                    failure = SymbolCountFailure.Timeout,
                    save = SymbolCountSaveStatus.Pending,
                )
            onNodeWithText("Play again").assertIsNotEnabled()
            state.value = state.value.copy(saveStatus = SymbolCountSaveStatus.Failed)
            onNodeWithText("Retry saving").performClick()
            assertEquals(1, retries)
            onNodeWithText("New record!").assertDoesNotExist()
        }

    @Test
    fun terminalAndSaveTransitionsExposePoliteLocalizedAnnouncements() =
        runComposeUiTest {
            val state = mutableStateOf(symbolCountPreviewState(SymbolCountPhase.Active))
            setContent {
                OquTurboTheme {
                    SymbolCountScreen(state.value, {}, { _, _ -> }, null)
                }
            }
            onNodeWithTag("result-announcement").assertDoesNotExist()
            for (failure in listOf(SymbolCountFailure.Wrong, SymbolCountFailure.Timeout)) {
                state.value =
                    symbolCountPreviewState(
                        SymbolCountPhase.Result,
                        failure = failure,
                        save = SymbolCountSaveStatus.Pending,
                    )
                val announcement = onNodeWithTag("result-announcement")
                announcement.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                val title = if (failure == SymbolCountFailure.Timeout) "Time’s up" else "Wrong answer"
                assertTrue(announcement.fetchSemanticsNode().config[SemanticsProperties.Text].any { it.text == title })
                assertTrue(
                    announcement.fetchSemanticsNode().config[SemanticsProperties.Text].any {
                        it.text == "Saving result…"
                    },
                )
                state.value = state.value.copy(saveStatus = SymbolCountSaveStatus.Failed)
                val text = announcement.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
                assertTrue("Result not saved. Retry saving to keep your progress." in text)
                assertTrue("Saving result…" !in text)
            }
        }

    @Test
    fun enlargedTextKeepsAnswersReachableWithoutClipping() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    OquTurboTheme {
                        SymbolCountScreen(
                            symbolCountPreviewState(SymbolCountPhase.Active, 5),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 600.dp),
                        )
                    }
                }
            }
            onNodeWithTag("answer-1").performScrollTo().assertIsDisplayed().assertIsEnabled()
            onNodeWithContentDescription("Pause").performScrollTo().assertIsDisplayed()
        }
}
