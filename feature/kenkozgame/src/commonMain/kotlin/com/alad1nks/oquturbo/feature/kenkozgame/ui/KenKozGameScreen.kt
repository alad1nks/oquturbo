package com.alad1nks.oquturbo.feature.kenkozgame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameResultCard
import com.alad1nks.oquturbo.core.ui.component.GameScoreBadge
import com.alad1nks.oquturbo.core.ui.component.GameStateOverlay
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KenKozGameRoute(
    viewModel: KenKozGameViewModel,
    onBackClick: () -> Unit,
    onTrainingContinue: (DailyTrainingEntry?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    KenKozGameScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onStartClick = viewModel::start,
        onTrainingContinueClick = { viewModel.continueTraining(onTrainingContinue) },
        onAnswerClick = viewModel::selectAnswer,
        modifier = modifier,
    )
}

@Composable
private fun KenKozGameScreen(
    uiState: KenKozGameUiState,
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    onTrainingContinueClick: () -> Unit,
    onAnswerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showStateOverlay =
        uiState.phase == KenKozGameUiState.Phase.Initial ||
            uiState.phase == KenKozGameUiState.Phase.Mistake
    val blurRadius by animateDpAsState(
        targetValue = if (showStateOverlay) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 700),
    )
    val backContentDescription = stringResource(AppResource.String.kenkoz_game_back)
    val isTrainingAttempt = uiState.trainingRequiredScore != null
    val isTrainingResult = isTrainingAttempt && uiState.phase == KenKozGameUiState.Phase.Mistake
    val isTrainingGoalReached =
        isTrainingResult && uiState.score >= requireNotNull(uiState.trainingRequiredScore)
    val isTrainingGoalNotReached =
        isTrainingResult && !isTrainingGoalReached

    Box(modifier = modifier.fillMaxSize().appBackground()) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .blur(blurRadius),
        ) {
            GameHud(score = uiState.score)

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (uiState.phase) {
                    KenKozGameUiState.Phase.Showing -> GameStage(uiState = uiState)
                    KenKozGameUiState.Phase.Answering ->
                        QuestionCard(
                            uiState = uiState,
                            onAnswerClick = onAnswerClick,
                        )
                    KenKozGameUiState.Phase.Initial,
                    KenKozGameUiState.Phase.Mistake,
                    -> Unit
                }
            }
        }

        AnimatedVisibility(
            visible = showStateOverlay,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(durationMillis = 700)),
            exit = fadeOut(animationSpec = tween(durationMillis = 700)),
        ) {
            GameStateOverlay(
                title =
                    when {
                        uiState.phase == KenKozGameUiState.Phase.Initial ->
                            stringResource(AppResource.String.kenkoz_game_start)
                        isTrainingGoalReached ->
                            stringResource(AppResource.String.home_training_goal_reached)
                        isTrainingGoalNotReached ->
                            stringResource(
                                AppResource.String.home_training_goal_not_reached,
                                requireNotNull(uiState.trainingRequiredScore),
                            )
                        else -> stringResource(AppResource.String.kenkoz_game_try_again)
                    },
                supportingText =
                    when {
                        uiState.phase == KenKozGameUiState.Phase.Initial ->
                            stringResource(AppResource.String.kenkoz_game_menu_subtitle)
                        isTrainingGoalReached ->
                            stringResource(AppResource.String.home_training_goal_reached_message)
                        else -> null
                    },
                icon =
                    when {
                        uiState.phase == KenKozGameUiState.Phase.Initial -> Icons.Filled.PlayArrow
                        isTrainingGoalReached -> Icons.AutoMirrored.Filled.ArrowForward
                        else -> Icons.Filled.Replay
                    },
                onClick = if (isTrainingGoalReached) onTrainingContinueClick else onStartClick,
                enabled = !isTrainingResult || uiState.isTrainingCompletionReady,
                extraContent = {
                    if (uiState.phase == KenKozGameUiState.Phase.Mistake) {
                        GameResultCard(
                            primaryText = stringResource(AppResource.String.kenkoz_game_score_value, uiState.score),
                            secondaryText = stringResource(AppResource.String.kenkoz_game_record, uiState.record),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }

        AppBackButton(
            onClick = onBackClick,
            contentDescription = backContentDescription,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(
                        start = 24.dp,
                        top = if (showStateOverlay) 8.dp else 16.dp,
                    ),
        )
    }
}

@Composable
private fun GameHud(score: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.size(48.dp))
        GameScoreBadge(
            label = stringResource(AppResource.String.kenkoz_game_score),
            value = score.toString(),
        )
    }
}

@Composable
private fun GameStage(uiState: KenKozGameUiState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val stageWidth = minOf(maxWidth, 640.dp)
        val stageHeight =
            if (uiState.mode == KenKozGameMode.WideLine) {
                minOf(maxHeight, 240.dp)
            } else {
                minOf(maxWidth, maxHeight, 380.dp)
            }

        Surface(
            modifier =
                if (uiState.mode == KenKozGameMode.WideLine) {
                    Modifier.size(width = stageWidth, height = stageHeight)
                } else {
                    Modifier.size(stageHeight)
                },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            if (uiState.mode == KenKozGameMode.WideLine) {
                WideLineItems(items = uiState.items)
            } else {
                RadialItems(items = uiState.items)
            }
        }
    }
}

@Composable
private fun WideLineItems(items: List<String>) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            GameItemText(
                text = item,
                modifier = Modifier.weight(1f),
                compact = true,
            )
        }
    }
}

@Composable
private fun RadialItems(items: List<String>) {
    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        GameItemText(items.getOrElse(0) { "" }, Modifier.align(Alignment.TopCenter))
        GameItemText(items.getOrElse(1) { "" }, Modifier.align(Alignment.CenterStart))
        GameItemText(items.getOrElse(2) { "" }, Modifier.align(Alignment.CenterEnd))
        GameItemText(items.getOrElse(3) { "" }, Modifier.align(Alignment.BottomCenter))

        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun GameItemText(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
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
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        FilledTonalButton(
                            onClick = { onAnswerClick(answer) },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = answerLabel(uiState.mode, answer),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (rowAnswers.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
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
    OquTurboTheme {
        KenKozGameScreen(
            uiState = KenKozGameUiState(mode = KenKozGameMode.Words),
            onBackClick = {},
            onStartClick = {},
            onTrainingContinueClick = {},
            onAnswerClick = {},
        )
    }
}

@Preview
@Composable
private fun KenKozGameScreenAnsweringPreview() {
    OquTurboTheme {
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
            onTrainingContinueClick = {},
            onAnswerClick = {},
        )
    }
}

@Preview
@Composable
private fun KenKozGameScreenMistakePreview() {
    OquTurboTheme {
        KenKozGameScreen(
            uiState =
                KenKozGameUiState(
                    mode = KenKozGameMode.Words,
                    score = 4,
                    record = 7,
                    phase = KenKozGameUiState.Phase.Mistake,
                ),
            onBackClick = {},
            onStartClick = {},
            onTrainingContinueClick = {},
            onAnswerClick = {},
        )
    }
}
