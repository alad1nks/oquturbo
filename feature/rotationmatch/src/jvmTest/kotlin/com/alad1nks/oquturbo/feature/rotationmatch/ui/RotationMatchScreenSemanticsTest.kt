package com.alad1nks.oquturbo.feature.rotationmatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchAnswer
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchBoard
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchDifficulty
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchFailure
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchPhase
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchRound
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RotationMatchScreenSemanticsTest {
    @Test
    fun pauseAndResumeCallbacksReplacePuzzleSemanticsAndKeepHubBack() =
        runComposeUiTest {
            val state = mutableStateOf(activeState())
            var pauses = 0
            var resumes = 0
            var backs = 0
            setContent {
                OquTurboTheme {
                    RotationMatchScreen(
                        state = state.value,
                        onStartClick = {},
                        onAnswerClick = { _, _ -> },
                        onBackClick = { backs++ },
                        onPauseClick = {
                            pauses++
                            state.value =
                                state.value.copy(
                                    game = state.value.game.copy(phase = RotationMatchPhase.Paused),
                                )
                        },
                        onResumeClick = {
                            resumes++
                            state.value =
                                state.value.copy(
                                    game = state.value.game.copy(phase = RotationMatchPhase.Active),
                                )
                        },
                    )
                }
            }
            onNodeWithContentDescription("Pause").assertIsEnabled().performClick()
            assertEquals(1, pauses)
            onNodeWithText("Paused").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            onNodeWithText("The timer is stopped. Resume when you’re ready.").assertExists()
            onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                .assertCountEquals(1)
            onAllNodes(
                hasContentDescription("Reference", substring = true),
                useUnmergedTree = true,
            ).assertCountEquals(0)
            onAllNodes(
                hasContentDescription("Candidate", substring = true),
                useUnmergedTree = true,
            ).assertCountEquals(0)
            onNodeWithText("Reference").assertDoesNotExist()
            onNodeWithText("Candidate").assertDoesNotExist()
            onNodeWithText("Match").assertDoesNotExist()
            onNodeWithText("Different").assertDoesNotExist()
            onNodeWithText("Time").assertDoesNotExist()
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithContentDescription("Back").performClick()
            assertEquals(1, backs)
            onNodeWithText("Resume").assertIsEnabled().performClick()
            assertEquals(1, resumes)
            onNodeWithContentDescription("Reference, row 1, column 1, filled").assertExists()
            onNodeWithText("Match").assertIsEnabled()
            onNodeWithContentDescription("Pause").assertIsEnabled()
            onNodeWithText("Resume").assertDoesNotExist()
        }

    @Test
    fun pausedStandaloneHasResumeWithoutBack() =
        runComposeUiTest {
            setContent {
                val active = activeState()
                rotationMatchScreen(active.copy(game = active.game.copy(phase = RotationMatchPhase.Paused)))
            }
            onNodeWithText("Resume").assertIsEnabled()
            onNodeWithContentDescription("Back").assertDoesNotExist()
        }

    @Test
    fun loadingStandaloneDisablesStartAndOmitsBack() =
        runComposeUiTest {
            setContent { rotationMatchScreen(RotationMatchUiState(), onBackClick = null) }

            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithText("Start").assertIsNotEnabled()
            onNodeWithContentDescription("Back").assertDoesNotExist()
        }

    @Test
    fun readyHubEnablesStartAndExposesBack() =
        runComposeUiTest {
            setContent {
                rotationMatchScreen(
                    RotationMatchUiState(record = 4, isRecordLoading = false),
                    onBackClick = {},
                )
            }

            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithText("Start").assertIsEnabled()
            onNodeWithContentDescription("Back").assertExists()
        }

    @Test
    fun activeRoundExposesLocalizedBoardsCellsAndEqualChoices() =
        runComposeUiTest {
            setContent { rotationMatchScreen(activeState()) }

            onNodeWithContentDescription("Reference, 3 by 3 grid").assertExists()
            onNodeWithContentDescription("Candidate, 3 by 3 grid").assertExists()
            onNodeWithContentDescription("Reference, row 1, column 1, filled").assertExists()
            onNodeWithContentDescription("Candidate, row 1, column 1, empty").assertExists()
            onNodeWithText("Match").assertIsEnabled()
            onNodeWithText("Different").assertIsEnabled()
            onNodeWithText("The candidate is the same pattern after rotation.").assertDoesNotExist()
            onNodeWithText("The candidate is a reflected pattern, so it is different.").assertDoesNotExist()
        }

    @Test
    fun correctAndWrongFeedbackExposeExplicitDisabledStatuses() =
        runComposeUiTest {
            val round = testRound()
            setContent {
                rotationMatchScreen(
                    RotationMatchUiState(
                        game =
                            RotationMatchState(
                                phase = RotationMatchPhase.CorrectFeedback,
                                score = 1,
                                correctAnswers = 1,
                                round = round,
                                selectedAnswer = RotationMatchAnswer.Match,
                            ),
                        record = 4,
                        isRecordLoading = false,
                    ),
                )
            }

            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithContentDescription("Match. Correct").assertIsNotEnabled()
        }

    @Test
    fun wrongResultExplainsMatchFromCorrectAnswerAndLabelsAnswers() =
        runComposeUiTest {
            setContent {
                rotationMatchScreen(
                    resultState(
                        failure = RotationMatchFailure.Wrong,
                        selectedAnswer = RotationMatchAnswer.Different,
                    ),
                )
            }

            onNodeWithContentDescription("Different. Your answer").assertIsNotEnabled()
            onNodeWithContentDescription("Match. Correct answer").assertIsNotEnabled()
            onNodeWithText("The candidate is the same pattern after rotation.").assertExists()
            onNodeWithText("The candidate is a reflected pattern, so it is different.").assertDoesNotExist()
        }

    @Test
    fun wrongResultExplainsDifferentFromCorrectAnswer() =
        runComposeUiTest {
            setContent {
                rotationMatchScreen(
                    resultState(
                        failure = RotationMatchFailure.Wrong,
                        selectedAnswer = RotationMatchAnswer.Match,
                        correctAnswer = RotationMatchAnswer.Different,
                    ),
                )
            }

            onNodeWithContentDescription("Match. Your answer").assertIsNotEnabled()
            onNodeWithContentDescription("Different. Correct answer").assertIsNotEnabled()
            onNodeWithText("The candidate is a reflected pattern, so it is different.").assertExists()
            onNodeWithText("The candidate is the same pattern after rotation.").assertDoesNotExist()
        }

    @Test
    fun timeoutResultExplainsMatchWithoutFabricatingAUserAnswer() =
        runComposeUiTest {
            setContent {
                rotationMatchScreen(
                    resultState(
                        failure = RotationMatchFailure.Timeout,
                        selectedAnswer = null,
                    ),
                )
            }

            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithText("Your answer").assertDoesNotExist()
            onNodeWithContentDescription("Match. Correct answer").assertIsNotEnabled()
            onNodeWithText("The candidate is the same pattern after rotation.").assertExists()
        }

    @Test
    fun timeoutResultExplanationDependsOnlyOnDifferentCorrectAnswer() =
        runComposeUiTest {
            setContent {
                rotationMatchScreen(
                    resultState(
                        failure = RotationMatchFailure.Timeout,
                        selectedAnswer = null,
                        correctAnswer = RotationMatchAnswer.Different,
                    ),
                )
            }

            onNodeWithContentDescription("Pause").assertDoesNotExist()
            onNodeWithText("Your answer").assertDoesNotExist()
            onNodeWithContentDescription("Different. Correct answer").assertIsNotEnabled()
            onNodeWithText("The candidate is a reflected pattern, so it is different.").assertExists()
        }

    private fun activeState() =
        RotationMatchUiState(
            game = RotationMatchState(phase = RotationMatchPhase.Active, round = testRound()),
            record = 4,
            isRecordLoading = false,
        )

    private fun resultState(
        failure: RotationMatchFailure,
        selectedAnswer: RotationMatchAnswer?,
        correctAnswer: RotationMatchAnswer = RotationMatchAnswer.Match,
    ) = RotationMatchUiState(
        game =
            RotationMatchState(
                phase = RotationMatchPhase.Result,
                round =
                    testRound(
                        remainingTimeMillis = if (failure == RotationMatchFailure.Timeout) 0 else 4_000,
                        correctAnswer = correctAnswer,
                    ),
                selectedAnswer = selectedAnswer,
                failure = failure,
            ),
        record = 4,
        isRecordLoading = false,
        completedDurationMillis = 1_000,
    )

    private fun testRound(
        remainingTimeMillis: Long = 10_000,
        correctAnswer: RotationMatchAnswer = RotationMatchAnswer.Match,
    ) =
        RotationMatchRound(
            reference = RotationMatchBoard(3, setOf(0, 3, 6, 7)),
            candidate = RotationMatchBoard(3, setOf(2, 5, 7, 8)),
            correctAnswer = correctAnswer,
            difficulty = RotationMatchDifficulty.Easy,
            totalTimeMillis = 10_000,
            remainingTimeMillis = remainingTimeMillis,
            id = 7,
        )

    @Composable
    private fun rotationMatchScreen(
        state: RotationMatchUiState,
        onBackClick: (() -> Unit)? = null,
    ) {
        OquTurboTheme {
            RotationMatchScreen(
                state = state,
                onStartClick = {},
                onAnswerClick = { _, _ -> },
                onBackClick = onBackClick,
            )
        }
    }
}
