package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class KenKozGameScreenRetryTest {
    @Test
    fun retryHidesNewRecordWhileResultOverlayFadesOut() =
        runComposeUiTest {
            var uiState by
                mutableStateOf(
                    KenKozGameUiState(
                        mode = KenKozGameMode.Words,
                        score = 8,
                        record = 8,
                        isNewRecord = true,
                        phase = KenKozGameUiState.Phase.Mistake,
                        correctAnswer = "mountain",
                        selectedAnswer = "morning",
                    ),
                )

            setContent {
                OquTurboTheme {
                    KenKozGameScreen(
                        uiState = uiState,
                        onBackClick = {},
                        onStartClick = {
                            uiState =
                                uiState.startingSession().copy(
                                    phase = KenKozGameUiState.Phase.Showing,
                                    items = listOf("alpha", "bravo", "charlie", "delta"),
                                )
                        },
                        onTrainingContinueClick = {},
                        onAnswerClick = {},
                    )
                }
            }

            onNodeWithText(NEW_RECORD_TEXT).assertExists()
            onAllNodes(newRecordLiveRegionMatcher).assertCountEquals(1)

            mainClock.autoAdvance = false
            onNodeWithText(TRY_AGAIN_TEXT).performClick()
            mainClock.advanceTimeByFrame()
            waitForIdle()

            onNodeWithText(TRY_AGAIN_TEXT).assertExists()
            onNodeWithText(NEW_RECORD_TEXT).assertDoesNotExist()
            onAllNodes(newRecordLiveRegionMatcher).assertCountEquals(0)
        }

    private companion object {
        const val NEW_RECORD_TEXT = "New record!"
        const val TRY_AGAIN_TEXT = "Try Again?"
        val newRecordLiveRegionMatcher =
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            )
    }
}
