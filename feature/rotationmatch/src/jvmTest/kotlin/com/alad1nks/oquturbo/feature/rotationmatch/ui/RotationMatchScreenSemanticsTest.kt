package com.alad1nks.oquturbo.feature.rotationmatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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

@OptIn(ExperimentalTestApi::class)
class RotationMatchScreenSemanticsTest {
    @Test
    fun loadingStandaloneDisablesStartAndOmitsBack() =
        runComposeUiTest {
            setContent { rotationMatchScreen(RotationMatchUiState(), onBackClick = null) }

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

            onNodeWithContentDescription("Match. Correct").assertIsNotEnabled()
        }

    @Test
    fun wrongResultLabelsUserAndCorrectAnswers() =
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
        }

    @Test
    fun timeoutResultDoesNotFabricateAUserAnswer() =
        runComposeUiTest {
            setContent {
                rotationMatchScreen(
                    resultState(
                        failure = RotationMatchFailure.Timeout,
                        selectedAnswer = null,
                    ),
                )
            }

            onNodeWithText("Your answer").assertDoesNotExist()
            onNodeWithContentDescription("Match. Correct answer").assertIsNotEnabled()
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
    ) = RotationMatchUiState(
        game =
            RotationMatchState(
                phase = RotationMatchPhase.Result,
                round = testRound(remainingTimeMillis = if (failure == RotationMatchFailure.Timeout) 0 else 4_000),
                selectedAnswer = selectedAnswer,
                failure = failure,
            ),
        record = 4,
        isRecordLoading = false,
        completedDurationMillis = 1_000,
    )

    private fun testRound(remainingTimeMillis: Long = 10_000) =
        RotationMatchRound(
            reference = RotationMatchBoard(3, setOf(0, 3, 6, 7)),
            candidate = RotationMatchBoard(3, setOf(2, 5, 7, 8)),
            correctAnswer = RotationMatchAnswer.Match,
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
