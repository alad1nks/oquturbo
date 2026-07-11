package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KenKozGameRoute(
    viewModel: KenKozGameViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    KenKozGameScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onStartClick = viewModel::start,
        onAnswerClick = viewModel::selectAnswer,
        modifier = modifier,
    )
}

@Composable
private fun KenKozGameScreen(
    uiState: KenKozGameUiState,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onAnswerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blurRadius by animateDpAsState(
        targetValue =
            when (uiState.phase) {
                KenKozGameUiState.Phase.Initial,
                KenKozGameUiState.Phase.Mistake,
                -> 8.dp
                KenKozGameUiState.Phase.Showing,
                KenKozGameUiState.Phase.Answering,
                -> 0.dp
            },
        animationSpec = tween(durationMillis = 700),
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .blur(blurRadius),
        ) {
            GameScore(score = uiState.score)

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.phase == KenKozGameUiState.Phase.Showing) {
                    GameItems(uiState)
                }
            }

            if (uiState.phase == KenKozGameUiState.Phase.Answering) {
                QuestionCard(
                    uiState = uiState,
                    onAnswerClick = onAnswerClick,
                )
            }
        }

        AnimatedVisibility(
            visible =
                uiState.phase == KenKozGameUiState.Phase.Initial ||
                    uiState.phase == KenKozGameUiState.Phase.Mistake,
            enter = fadeIn(animationSpec = tween(durationMillis = 700)),
            exit = fadeOut(animationSpec = tween(durationMillis = 700)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(onClick = onStartClick),
            ) {
                if (uiState.phase == KenKozGameUiState.Phase.Initial) {
                    Text(
                        text = stringResource(AppResource.String.kenkoz_game_start),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )
                }

                if (uiState.phase == KenKozGameUiState.Phase.Mistake) {
                    Text(
                        text = stringResource(AppResource.String.kenkoz_game_score_value, uiState.score),
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .systemBarsPadding()
                                .padding(top = 64.dp),
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )

                    Text(
                        text = stringResource(AppResource.String.kenkoz_game_try_again),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                    )
                }
            }
        }

        IconButton(
            onClick = onBackClick,
            modifier =
                Modifier
                    .padding(start = 4.dp, top = 8.dp)
                    .statusBarsPadding(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(AppResource.String.kenkoz_game_back),
            )
        }
    }
}

@Composable
private fun GameScore(score: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(AppResource.String.kenkoz_game_score),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = score.toString(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GameItems(uiState: KenKozGameUiState) {
    if (uiState.mode == KenKozGameMode.WideLine) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            uiState.items.forEach { GameItemText(it) }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).size(320.dp),
            contentAlignment = Alignment.Center,
        ) {
            GameItemText(uiState.items.getOrElse(0) { "" }, Modifier.align(Alignment.TopCenter))
            GameItemText(uiState.items.getOrElse(1) { "" }, Modifier.align(Alignment.CenterStart))
            GameItemText(uiState.items.getOrElse(2) { "" }, Modifier.align(Alignment.CenterEnd))
            GameItemText(uiState.items.getOrElse(3) { "" }, Modifier.align(Alignment.BottomCenter))
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun GameItemText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QuestionCard(
    uiState: KenKozGameUiState,
    onAnswerClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = questionText(uiState),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            uiState.answers.chunked(2).forEach { rowAnswers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowAnswers.forEach { answer ->
                        TextButton(
                            onClick = { onAnswerClick(answer) },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(18.dp),
                                    )
                                    .padding(vertical = 12.dp),
                        ) {
                            Text(
                                text = answerLabel(uiState.mode, answer),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun questionText(uiState: KenKozGameUiState): String {
    return when (uiState.mode) {
        KenKozGameMode.Characters ->
            stringResource(
                AppResource.String.kenkoz_game_question_character,
                directionLabel(requireNotNull(uiState.questionDirection)),
            )
        KenKozGameMode.Words ->
            stringResource(
                AppResource.String.kenkoz_game_question_word,
                directionLabel(requireNotNull(uiState.questionDirection)),
            )
        KenKozGameMode.FindDifference -> stringResource(AppResource.String.kenkoz_game_question_difference)
        KenKozGameMode.WideLine -> stringResource(AppResource.String.kenkoz_game_question_first_word)
    }
}

@Composable
private fun answerLabel(
    mode: KenKozGameMode,
    answer: String,
): String {
    return if (mode == KenKozGameMode.FindDifference) {
        directionLabel(KenKozGameUiState.Direction.valueOf(answer))
    } else {
        answer
    }
}

@Composable
private fun directionLabel(direction: KenKozGameUiState.Direction): String {
    return stringResource(
        when (direction) {
            KenKozGameUiState.Direction.Top -> AppResource.String.kenkoz_game_direction_top
            KenKozGameUiState.Direction.Left -> AppResource.String.kenkoz_game_direction_left
            KenKozGameUiState.Direction.Right -> AppResource.String.kenkoz_game_direction_right
            KenKozGameUiState.Direction.Bottom -> AppResource.String.kenkoz_game_direction_bottom
        },
    )
}

@Preview
@Composable
private fun KenKozGameScreenInitialPreview() {
    KenKozGameScreen(
        uiState = KenKozGameUiState(mode = KenKozGameMode.Words),
        onBackClick = {},
        onStartClick = {},
        onAnswerClick = {},
    )
}

@Preview
@Composable
private fun KenKozGameScreenAnsweringPreview() {
    KenKozGameScreen(
        uiState =
            KenKozGameUiState(
                mode = KenKozGameMode.Words,
                score = 27,
                phase = KenKozGameUiState.Phase.Answering,
                items = listOf("море", "свет", "дом", "лес"),
                answers = listOf("море", "дом", "свет", "лес"),
                correctAnswer = "дом",
                questionDirection = KenKozGameUiState.Direction.Right,
            ),
        onBackClick = {},
        onStartClick = {},
        onAnswerClick = {},
    )
}

@Preview
@Composable
private fun KenKozGameScreenMistakePreview() {
    KenKozGameScreen(
        uiState =
            KenKozGameUiState(
                mode = KenKozGameMode.Words,
                score = 4,
                phase = KenKozGameUiState.Phase.Mistake,
            ),
        onBackClick = {},
        onStartClick = {},
        onAnswerClick = {},
    )
}
