package com.alad1nks.oquturbo.feature.wordflow.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
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
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPhase
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPrompt
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowRound
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowState
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import kotlin.test.Test
import kotlin.test.assertEquals

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
