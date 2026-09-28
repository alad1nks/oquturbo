package com.alad1nks.oquturbo.feature.wordflow.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPhase
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPrompt
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowRound
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowState
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WordFlowScreenSemanticsTest {
    @Test
    fun compactPauseConcealsChallengeAndResumeRestoresControlsWithScrolling() =
        runComposeUiTest {
            val state = mutableStateOf(activeState())
            var pauses = 0
            var resumes = 0
            var backs = 0
            var choices = 0
            setContent {
                OquTurboTheme {
                    WordFlowScreen(
                        state.value,
                        {},
                        { choices++ },
                        { backs++ },
                        modifier = Modifier.requiredSize(320.dp, 420.dp),
                        onPauseClick = {
                            pauses++
                            state.value = state.value.copy(game = state.value.game.copy(phase = WordFlowPhase.Paused))
                        },
                        onResumeClick = {
                            resumes++
                            state.value = state.value.copy(game = state.value.game.copy(phase = WordFlowPhase.Active))
                        },
                    )
                }
            }
            onNodeWithText("Pause").assertIsEnabled().performClick()
            assertEquals(1, pauses)
            onNodeWithText("Paused").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                .assertCountEquals(1)
            for (unmerged in listOf(false, true)) {
                for (text in listOf("book", "window", "spoon", "Time")) {
                    onNodeWithText(text, useUnmergedTree = unmerged).assertDoesNotExist()
                }
                onAllNodes(hasContentDescription("Sentence", substring = true), useUnmergedTree = unmerged)
                    .assertCountEquals(0)
                onAllNodes(hasContentDescription("seconds", substring = true), useUnmergedTree = unmerged)
                    .assertCountEquals(0)
                onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo), unmerged)
                    .assertCountEquals(0)
            }
            onNodeWithText("Score").assertExists()
            onNodeWithText("Record").assertExists()
            onNodeWithContentDescription("Back").performClick()
            assertEquals(1, backs)
            onNodeWithText("Resume").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            assertEquals(1, resumes)
            onNodeWithText("spoon").performScrollTo().assertIsDisplayed().performClick()
            assertEquals(1, choices)
            onNodeWithText("Pause").performScrollTo().assertIsDisplayed()
            onNodeWithText("Resume").assertDoesNotExist()
        }

    @Test
    fun standalonePausedOmitsBackAndNonActiveStatesOmitPause() =
        runComposeUiTest {
            val state = mutableStateOf(activeState())
            setContent { OquTurboTheme { WordFlowScreen(state.value, {}, {}, null) } }
            for (phase in listOf(
                WordFlowPhase.Ready,
                WordFlowPhase.CorrectFeedback,
                WordFlowPhase.Result,
                WordFlowPhase.Paused,
            )) {
                runOnIdle { state.value = state.value.copy(game = state.value.game.copy(phase = phase)) }
                onNodeWithText("Pause").assertDoesNotExist()
                onNodeWithContentDescription("Back").assertDoesNotExist()
            }
            onNodeWithText("Resume").assertIsEnabled()
        }

    @Test
    fun compactLargeTextLongScoresDoNotOverlapAndReadyActionRemainsReachable() =
        runComposeUiTest {
            var starts = 0
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                    OquTurboTheme {
                        WordFlowScreen(
                            state =
                                WordFlowUiState(
                                    game = WordFlowState(score = 2147483647),
                                    record = 2147483646,
                                    isRecordLoading = false,
                                ),
                            onStartClick = { starts++ },
                            onChoiceClick = {},
                            onBackClick = {},
                            modifier = Modifier.requiredSize(320.dp, 640.dp),
                        )
                    }
                }
            }
            for (value in listOf("2147483647", "2147483646")) {
                val layouts = mutableListOf<TextLayoutResult>()
                onNodeWithText(value).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(1, layouts.single().lineCount, "A numeric value must remain one readable number")
                assertTrue(
                    layouts.single().size.width <= onNodeWithText(value).fetchSemanticsNode().boundsInRoot.width,
                    "The complete number must fit without horizontal scrolling",
                )
            }
            val score = onNodeWithText("2147483647").fetchSemanticsNode().boundsInRoot
            val record = onNodeWithText("2147483646").fetchSemanticsNode().boundsInRoot
            assertTrue(score.bottom <= record.top, "Long record must flow below the score")
            val ready = onNodeWithText("Word Flow").fetchSemanticsNode().boundsInRoot
            assertTrue(record.bottom < ready.top, "The ready panel must clear the measured header")
            onNodeWithText("Start").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            assertEquals(1, starts)
        }

    @Test
    fun compactKazakhActiveKeepsAllThreeAnswersVisibleWithoutScrolling() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("kk"))
            runSkikoComposeUiTest(size = Size(320f, 844f)) {
                val prompt =
                    WordFlowPrompt(
                        "compact-kk",
                        WordFlowTier.Hard,
                        "Өлшемдер әртүрлі болғандықтан, зерттеушілер қорытындыны қосымша деректер %1\$s дейін алдын ала деп санады.",
                        "жиналғанға",
                        listOf("жоғалғанға", "ұмытылғанға"),
                    )
                setContent {
                    OquTurboTheme {
                        WordFlowScreen(
                            WordFlowUiState(
                                game =
                                    WordFlowState(
                                        phase = WordFlowPhase.Active,
                                        score = 20,
                                        correctAnswers = 20,
                                        round =
                                            WordFlowRound(
                                                prompt,
                                                listOf("жоғалғанға", "жиналғанға", "ұмытылғанға"),
                                                6_000,
                                                4_000,
                                            ),
                                    ),
                                record = 25,
                                locale = "kk",
                                isRecordLoading = false,
                            ),
                            {},
                            {},
                            {},
                        )
                    }
                }
                onNodeWithText("Ұпай").assertIsDisplayed()
                for (answer in listOf("жоғалғанға", "жиналғанға", "ұмытылғанға")) {
                    val node = onNodeWithText(answer).assertIsDisplayed()
                    assertTrue(node.fetchSemanticsNode().boundsInRoot.bottom <= 844f)
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun activeState(): WordFlowUiState {
        val prompt =
            WordFlowPrompt("test", WordFlowTier.Easy, "The reader closed the %1\$s.", "book", listOf("window", "spoon"))
        return WordFlowUiState(
            game =
                WordFlowState(
                    phase = WordFlowPhase.Active,
                    score = 3,
                    correctAnswers = 3,
                    round = WordFlowRound(prompt, listOf("window", "book", "spoon"), 10_000, 7_000),
                ),
            record = 5,
            isRecordLoading = false,
            completedDurationMillis = 1_000,
        )
    }
}
