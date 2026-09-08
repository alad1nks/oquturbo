package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailBoard
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailFailure
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailPhase
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NumberTrailScreenSemanticsTest {
    @Test
    fun enlargedShortScreenCanScrollToFinalTileAndBackToPause() =
        runComposeUiTest {
            val game = active().game.copy(board = NumberTrailBoard(1, 4, (1..16).toList(), 30_000), score = 12345)
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                    OquTurboTheme {
                        NumberTrailScreen(
                            NumberTrailUiState(game = game, record = 23456, isRecordLoading = false),
                            {},
                            { _, _ -> },
                            {},
                            modifier = Modifier.requiredSize(320.dp, 600.dp),
                        )
                    }
                }
            }
            onNodeWithContentDescription("Number 16").performScrollTo().assertIsDisplayed().assertIsEnabled()
            onNodeWithContentDescription("Pause").performScrollTo().assertIsDisplayed().assertIsEnabled()
        }

    @Test
    fun pauseRemovesGridTargetAndTimerAndResumeRestoresSameNumbers() =
        runComposeUiTest {
            val state = mutableStateOf(active())
            var tapped = -1
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(
                        state.value,
                        {},
                        { _, index -> tapped = index },
                        null,
                        onPauseClick = {
                            state.value =
                                state.value.copy(
                                    game = state.value.game.copy(phase = NumberTrailPhase.Paused),
                                )
                        },
                        onResumeClick = { state.value = active() },
                    )
                }
            }
            onNodeWithContentDescription("Number 1").assertIsNotEnabled()
            onNodeWithContentDescription("Number 2").assertIsEnabled().performClick()
            assertEquals(2, tapped)
            onNodeWithContentDescription("Pause").performClick()
            for (n in 1..4) onNodeWithContentDescription("Number $n").assertDoesNotExist()
            onNodeWithText("Find 2").assertDoesNotExist()
            onNodeWithText("Time left: 12 s").assertDoesNotExist()
            onNodeWithContentDescription("Back").assertDoesNotExist()
            onNodeWithText("Resume").performClick()
            onNodeWithContentDescription("Number 2").assertIsEnabled()
        }

    @Test
    fun completedBoardDisablesEveryTileAndHasNoCountdown() =
        runComposeUiTest {
            val active = active()
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(
                        active.copy(
                            game =
                                active.game.copy(
                                    phase = NumberTrailPhase.BoardComplete,
                                    board = active.game.board!!.copy(target = 5),
                                ),
                        ),
                        {},
                        { _, _ -> },
                        {},
                    )
                }
            }
            for (n in 1..4) onNodeWithContentDescription("Number $n").assertIsNotEnabled()
            onNodeWithText("Board complete").assertExists()
            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithText("Time left: 12 s").assertDoesNotExist()
            onNodeWithContentDescription("Back").assertExists()
        }

    @Test
    fun loadingAndFailedRecordReadExposeUsableRecoveryWithoutDeadBack() =
        runComposeUiTest {
            val state = mutableStateOf(NumberTrailUiState())
            var reloads = 0
            setContent {
                OquTurboTheme {
                    NumberTrailScreen(
                        state.value,
                        {},
                        { _, _ -> },
                        null,
                        onReloadClick = { reloads++ },
                    )
                }
            }
            onNodeWithText("Start").assertIsNotEnabled()
            onNodeWithContentDescription("Back").assertDoesNotExist()
            state.value =
                active().copy(
                    game = active().game.copy(phase = NumberTrailPhase.Result, failure = NumberTrailFailure.Timeout),
                    recordLoadFailed = true,
                )
            onNodeWithText("Retry").assertIsEnabled().performClick()
            assertEquals(1, reloads)
            onNodeWithText("New record!").assertDoesNotExist()
        }

    private fun active() =
        NumberTrailUiState(
            game =
                NumberTrailState(
                    phase = NumberTrailPhase.Active,
                    board = NumberTrailBoard(7, 2, listOf(4, 1, 2, 3), 12_000, target = 2),
                    score = 1,
                ),
            isRecordLoading = false,
        )
}
