package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun RememberNumberRoute(
    viewModel: RememberNumberViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusEvent by viewModel.focusEvent.collectAsState()

    RememberNumberScreen(
        uiState = uiState,
        focusEvent = focusEvent,
        maxLength = viewModel.maxLength,
        writeText = viewModel::writeText,
        onStartClick = viewModel::start,
        modifier = modifier,
    )
}

@Composable
internal fun RememberNumberScreen(
    uiState: RememberNumberUiState,
    focusEvent: Unit?,
    maxLength: Int,
    writeText: (String) -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val blurRadius by animateDpAsState(
        targetValue =
            when (uiState) {
                is RememberNumberUiState.Initial,
                is RememberNumberUiState.Mistake,
                -> 8.dp
                is RememberNumberUiState.Reading,
                is RememberNumberUiState.Writing,
                -> 0.dp
            },
        animationSpec = tween(durationMillis = 700),
    )

    LaunchedEffect(focusEvent) {
        if (focusEvent != null) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .blur(blurRadius),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Score: ${uiState.score}")

            Spacer(modifier = Modifier.weight(1f))

            RememberNumberTextField(
                value = uiState.text,
                onValueChange = writeText,
                maxLength = maxLength,
                backgroundColor =
                    when (uiState) {
                        is RememberNumberUiState.Initial,
                        is RememberNumberUiState.Reading,
                        -> { index -> Color.Transparent }
                        is RememberNumberUiState.Writing -> { index ->
                            if (index < uiState.text.length) {
                                Color.LightGray.copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            }
                        }
                        is RememberNumberUiState.Mistake -> { index ->
                            if (uiState.text[index] == uiState.correctText[index]) {
                                Color.Green.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        }
                    },
                borderColor = { index ->
                    if (index == uiState.text.length && uiState is RememberNumberUiState.Writing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Gray
                    }
                },
                showFocusedPlaceholder = uiState is RememberNumberUiState.Writing,
                modifier =
                    Modifier
                        .focusRequester(focusRequester)
                        .focusProperties {
                            canFocus = uiState is RememberNumberUiState.Writing
                        }.padding(horizontal = 32.dp)
                        .widthIn(max = 600.dp),
            )

            Spacer(modifier = Modifier.weight(2f))
        }

        AnimatedVisibility(
            visible = uiState is RememberNumberUiState.Mistake || uiState is RememberNumberUiState.Initial,
            enter =
                fadeIn(
                    animationSpec = tween(durationMillis = 700),
                ),
            exit =
                fadeOut(
                    animationSpec = tween(durationMillis = 700),
                ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(onClick = onStartClick),
            ) {
                if (uiState is RememberNumberUiState.Initial) {
                    Text(
                        text = "Start the game!",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )
                }

                if (uiState is RememberNumberUiState.Mistake) {
                    RememberNumberMistakeScore(
                        score = uiState.score,
                        record = uiState.record,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .systemBarsPadding()
                                .padding(top = 64.dp),
                    )

                    Text(
                        text = "Try Again?",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RememberNumberMistakeScore(
    score: Int,
    record: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Score: $score",
            fontSize = 48.sp,
            textAlign = TextAlign.Center,
            lineHeight = 48.sp,
        )

        Text(
            text = "Record: $record",
            fontSize = 48.sp,
            textAlign = TextAlign.Center,
            lineHeight = 48.sp,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun RememberNumberScreenInitialPreview() {
    MaterialTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Initial(),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun RememberNumberScreenReadingPreview() {
    MaterialTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Reading(text = "1334", score = 4),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun RememberNumberScreenWritingPreview() {
    MaterialTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Writing(text = "12", score = 4),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun RememberNumberScreenMistakePreview() {
    MaterialTheme {
        RememberNumberScreen(
            uiState = RememberNumberUiState.Mistake(text = "1234", score = 4, correctText = "1334", record = 7),
            focusEvent = null,
            maxLength = 4,
            writeText = {},
            onStartClick = {},
        )
    }
}
