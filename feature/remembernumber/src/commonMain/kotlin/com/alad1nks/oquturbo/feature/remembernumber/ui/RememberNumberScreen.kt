package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AnimatedGameStateOverlay
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameHeader
import com.alad1nks.oquturbo.core.ui.component.GameResultCard
import com.alad1nks.oquturbo.core.ui.component.GameStateOverlay
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RememberNumberRoute(
    viewModel: RememberNumberViewModel,
    onBackClick: () -> Unit,
    onTrainingContinue: (DailyTrainingEntry?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusEvent by viewModel.focusEvent.collectAsState()
    val record by viewModel.record.collectAsState()

    RememberNumberScreen(
        uiState = uiState,
        focusEvent = focusEvent,
        maxLength = viewModel.maxLength,
        record = record,
        writeText = viewModel::writeText,
        onStartClick = viewModel::start,
        onBackClick = onBackClick,
        trainingRequiredScore = viewModel.trainingRequiredScore,
        onTrainingContinueClick = { viewModel.continueTraining(onTrainingContinue) },
        modifier = modifier,
    )
}

@Composable
internal fun RememberNumberScreen(
    uiState: RememberNumberUiState,
    focusEvent: Unit?,
    maxLength: Int,
    record: Int = 0,
    writeText: (String) -> Unit,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    trainingRequiredScore: Int? = null,
    onTrainingContinueClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val scoreText = stringResource(AppResource.String.remember_number_game_score, uiState.score)
    val scoreValue = uiState.score.toString()
    val scoreLabel = scoreText.removeSuffix(scoreValue).trimEnd(' ', ':')
    val recordValue = record.toString()
    val recordLabel =
        stringResource(AppResource.String.remember_number_game_record, record)
            .removeSuffix(recordValue)
            .trimEnd(' ', ':')
    val backContentDescription = stringResource(AppResource.String.kenkoz_game_back)
    val overlayState =
        when (uiState) {
            is RememberNumberUiState.Initial,
            is RememberNumberUiState.Mistake,
            -> uiState
            is RememberNumberUiState.Reading,
            is RememberNumberUiState.Writing,
            -> null
        }

    LaunchedEffect(focusEvent) {
        if (focusEvent != null) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize().appBackground()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 72.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            RememberNumberTextField(
                value = uiState.text,
                onValueChange = writeText,
                maxLength = maxLength,
                backgroundColor =
                    when (uiState) {
                        is RememberNumberUiState.Initial,
                        is RememberNumberUiState.Reading,
                        -> { _ -> MaterialTheme.colorScheme.surfaceContainerHigh }
                        is RememberNumberUiState.Writing -> { index ->
                            if (index < uiState.text.length) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        }
                        is RememberNumberUiState.Mistake -> { index ->
                            if (uiState.text[index] == uiState.correctText[index]) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        }
                    },
                borderColor = { index ->
                    when {
                        uiState is RememberNumberUiState.Mistake &&
                            uiState.text[index] == uiState.correctText[index] ->
                            MaterialTheme.colorScheme.tertiary
                        uiState is RememberNumberUiState.Mistake -> MaterialTheme.colorScheme.error
                        index == uiState.text.length && uiState is RememberNumberUiState.Writing ->
                            MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                },
                showFocusedPlaceholder = uiState is RememberNumberUiState.Writing,
                modifier =
                    Modifier
                        .focusRequester(focusRequester)
                        .focusProperties {
                            canFocus = uiState is RememberNumberUiState.Writing
                        }.widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
            )
        }

        GameHeader(
            scoreLabel = scoreLabel,
            score = scoreValue,
            recordLabel = recordLabel,
            record = recordValue,
            leadingContent =
                if (overlayState != null) {
                    null
                } else {
                    {
                        AppBackButton(
                            onClick = onBackClick,
                            contentDescription = backContentDescription,
                        )
                    }
                },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        )

        AnimatedGameStateOverlay(
            state = overlayState,
            modifier = Modifier.fillMaxSize(),
        ) { displayedOverlayState ->
            val displayedMistake = displayedOverlayState as? RememberNumberUiState.Mistake
            val isTrainingResult = displayedMistake != null && trainingRequiredScore != null
            val isTrainingGoalReached =
                displayedMistake != null &&
                    trainingRequiredScore != null &&
                    displayedMistake.score >= trainingRequiredScore

            GameStateOverlay(
                title =
                    when {
                        displayedOverlayState is RememberNumberUiState.Initial ->
                            stringResource(AppResource.String.remember_number_game_start)
                        isTrainingGoalReached ->
                            stringResource(AppResource.String.home_training_goal_reached)
                        isTrainingResult ->
                            stringResource(
                                AppResource.String.home_training_goal_not_reached,
                                trainingRequiredScore,
                            )
                        else -> stringResource(AppResource.String.remember_number_game_try_again)
                    },
                supportingText =
                    if (isTrainingGoalReached) {
                        stringResource(AppResource.String.home_training_goal_reached_message)
                    } else {
                        null
                    },
                icon =
                    when {
                        displayedOverlayState is RememberNumberUiState.Initial -> Icons.Default.PlayArrow
                        isTrainingGoalReached -> Icons.AutoMirrored.Filled.ArrowForward
                        else -> Icons.Default.Replay
                    },
                onClick = if (isTrainingGoalReached) onTrainingContinueClick else onStartClick,
                enabled = !isTrainingResult || displayedMistake.isTrainingResultReady,
                modifier = Modifier.fillMaxSize(),
                extraContent = {
                    if (displayedMistake != null) {
                        GameResultCard(
                            primaryText =
                                stringResource(
                                    AppResource.String.remember_number_game_score,
                                    displayedMistake.score,
                                ),
                            secondaryText =
                                stringResource(
                                    AppResource.String.remember_number_game_record,
                                    displayedMistake.record,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }

        if (overlayState != null) {
            AppBackButton(
                onClick = onBackClick,
                contentDescription = backContentDescription,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 24.dp, top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RememberNumberScreenInitialPreview() {
    OquTurboTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Initial(),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RememberNumberScreenReadingPreview() {
    OquTurboTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Reading(text = "1334", score = 4),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RememberNumberScreenWritingPreview() {
    OquTurboTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Writing(text = "12", score = 4),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RememberNumberScreenMistakePreview() {
    OquTurboTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Mistake(text = "1234", score = 4, correctText = "1334", record = 7),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
            onBackClick = {},
        )
    }
}
