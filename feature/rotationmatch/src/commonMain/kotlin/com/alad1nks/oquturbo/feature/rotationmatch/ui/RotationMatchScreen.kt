package com.alad1nks.oquturbo.feature.rotationmatch.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameHeader
import com.alad1nks.oquturbo.core.ui.component.GameResultCard
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchAnswer
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchBoard
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchDifficulty
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchFailure
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchPhase
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchRound
import com.alad1nks.oquturbo.feature.rotationmatch.model.RotationMatchState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil

internal fun rotationMatchBackAction(
    onBackClick: (() -> Unit)?,
    onAbandon: () -> Unit,
): (() -> Unit)? =
    onBackClick?.let { callback ->
        {
            onAbandon()
            callback()
        }
    }

internal fun RotationMatchDifficulty.resource(): StringResource =
    when (this) {
        RotationMatchDifficulty.Easy -> AppResource.String.rotation_match_difficulty_easy
        RotationMatchDifficulty.Medium -> AppResource.String.rotation_match_difficulty_medium
        RotationMatchDifficulty.Hard -> AppResource.String.rotation_match_difficulty_hard
    }

@Composable
internal fun RotationMatchRoute(
    viewModel: RotationMatchViewModel,
    onBackClick: (() -> Unit)?,
) {
    val state by viewModel.uiState.collectAsState()
    RotationMatchScreen(
        state = state,
        onStartClick = viewModel::start,
        onAnswerClick = viewModel::selectAnswer,
        onBackClick = rotationMatchBackAction(onBackClick, viewModel::abandon),
    )
}

@Composable
internal fun RotationMatchScreen(
    state: RotationMatchUiState,
    onStartClick: () -> Unit,
    onAnswerClick: (Long, RotationMatchAnswer) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    BoxWithConstraints(modifier.fillMaxSize().appBackground()) {
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            modifier =
                Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).navigationBarsPadding()
                    .padding(
                        start = horizontalPadding,
                        top = 104.dp,
                        end = horizontalPadding,
                        bottom = 32.dp,
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (game.phase) {
                RotationMatchPhase.Ready -> ReadyContent(state, onStartClick)
                RotationMatchPhase.Active, RotationMatchPhase.CorrectFeedback ->
                    PlayingContent(state, onAnswerClick)
                RotationMatchPhase.Result -> ResultContent(state, onStartClick, onAnswerClick, onBackClick)
            }
        }
        GameHeader(
            scoreLabel = stringResource(AppResource.String.rotation_match_score),
            score = game.score.toString(),
            recordLabel = stringResource(AppResource.String.rotation_match_record),
            record =
                if (state.isRecordLoading) {
                    stringResource(AppResource.String.rotation_match_placeholder)
                } else {
                    state.record.toString()
                },
            leadingContent =
                onBackClick?.let { callback ->
                    {
                        AppBackButton(
                            onClick = callback,
                            contentDescription = stringResource(AppResource.String.rotation_match_back),
                        )
                    }
                },
            modifier =
                Modifier.align(Alignment.TopCenter).widthIn(max = 760.dp).fillMaxWidth()
                    .statusBarsPadding().padding(horizontal = horizontalPadding, vertical = 16.dp),
        )
    }
}

@Composable
private fun ReadyContent(
    state: RotationMatchUiState,
    onStartClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                stringResource(AppResource.String.rotation_match_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(AppResource.String.rotation_match_ready_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.rotation_match_instructions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                if (state.isRecordLoading) {
                    stringResource(AppResource.String.rotation_match_loading_record)
                } else {
                    stringResource(AppResource.String.rotation_match_record_value, state.record)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onStartClick,
                enabled = !state.isRecordLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(stringResource(AppResource.String.rotation_match_start))
            }
        }
    }
}

@Composable
private fun PlayingContent(
    state: RotationMatchUiState,
    onAnswerClick: (Long, RotationMatchAnswer) -> Unit,
) {
    val round = state.game.round ?: return
    TimerContent(round)
    PatternPair(round)
    AnswerControls(state, onAnswerClick)
}

@Composable
private fun TimerContent(round: RotationMatchRound) {
    val seconds = ceil(round.remainingTimeMillis / 1000.0).toInt()
    val timerDescription = stringResource(AppResource.String.rotation_match_timer_accessibility, seconds)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(AppResource.String.rotation_match_time), fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(AppResource.String.rotation_match_seconds, seconds),
                modifier = Modifier.semantics { contentDescription = timerDescription },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        LinearProgressIndicator(
            progress = { round.remainingTimeMillis.toFloat() / round.totalTimeMillis },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PatternPair(round: RotationMatchRound) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PatternPanel(
            label = stringResource(AppResource.String.rotation_match_reference),
            board = round.reference,
            modifier = Modifier.weight(1f),
        )
        PatternPanel(
            label = stringResource(AppResource.String.rotation_match_candidate),
            board = round.candidate,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PatternPanel(
    label: String,
    board: RotationMatchBoard,
    modifier: Modifier = Modifier,
) {
    val boardDescription =
        stringResource(
            AppResource.String.rotation_match_board_accessibility,
            label,
            board.size,
        )
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Surface(
            modifier =
                Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                    contentDescription = boardDescription
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(board.size) { row ->
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(board.size) { column ->
                            val index = row * board.size + column
                            val isFilled = index in board.filledCells
                            PatternCell(
                                description =
                                    stringResource(
                                        AppResource.String.rotation_match_cell_accessibility,
                                        label,
                                        row + 1,
                                        column + 1,
                                        stringResource(
                                            if (isFilled) {
                                                AppResource.String.rotation_match_filled
                                            } else {
                                                AppResource.String.rotation_match_empty
                                            },
                                        ),
                                    ),
                                filled = isFilled,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternCell(
    description: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = RoundedCornerShape(6.dp),
        color =
            if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        border =
            if (filled) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            },
    ) {}
}

@Composable
private fun AnswerControls(
    state: RotationMatchUiState,
    onAnswerClick: (Long, RotationMatchAnswer) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AnswerControl(
            state = state,
            answer = RotationMatchAnswer.Match,
            onClick = onAnswerClick,
            modifier = Modifier.weight(1f),
        )
        AnswerControl(
            state = state,
            answer = RotationMatchAnswer.Different,
            onClick = onAnswerClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnswerControl(
    state: RotationMatchUiState,
    answer: RotationMatchAnswer,
    onClick: (Long, RotationMatchAnswer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    val round = game.round ?: return
    val enabled = game.phase == RotationMatchPhase.Active
    val isCorrect = answer == round.correctAnswer
    val isSelectedWrong = game.failure == RotationMatchFailure.Wrong && answer == game.selectedAnswer
    val showCorrect = isCorrect && game.phase != RotationMatchPhase.Active
    val label = stringResource(answer.resource())
    val status =
        when {
            isSelectedWrong -> stringResource(AppResource.String.rotation_match_your_answer)
            showCorrect && game.phase == RotationMatchPhase.CorrectFeedback ->
                stringResource(AppResource.String.rotation_match_correct)
            showCorrect -> stringResource(AppResource.String.rotation_match_correct_answer)
            else -> null
        }
    val answerDescription =
        status?.let { stringResource(AppResource.String.rotation_match_answer_accessibility, label, it) }
    Surface(
        modifier =
            modifier.heightIn(min = 56.dp).semantics {
                role = Role.Button
                if (status != null) {
                    stateDescription = status
                    contentDescription = requireNotNull(answerDescription)
                }
                if (!enabled) disabled()
            }.clickable(enabled = enabled) { onClick(round.id, answer) },
        shape = RoundedCornerShape(16.dp),
        color =
            when {
                isSelectedWrong -> MaterialTheme.colorScheme.errorContainer
                showCorrect -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        border =
            BorderStroke(
                if (isSelectedWrong || showCorrect) 2.dp else 1.dp,
                when {
                    isSelectedWrong -> MaterialTheme.colorScheme.error
                    showCorrect -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCorrect) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            if (isSelectedWrong) Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                if (status != null) {
                    Text(
                        status,
                        modifier =
                            if (game.phase == RotationMatchPhase.CorrectFeedback) {
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            } else {
                                Modifier
                            },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun RotationMatchAnswer.resource(): StringResource =
    when (this) {
        RotationMatchAnswer.Match -> AppResource.String.rotation_match_match
        RotationMatchAnswer.Different -> AppResource.String.rotation_match_different
    }

@Composable
private fun ResultContent(
    state: RotationMatchUiState,
    onReplayClick: () -> Unit,
    onAnswerClick: (Long, RotationMatchAnswer) -> Unit,
    onBackClick: (() -> Unit)?,
) {
    val game = state.game
    val round = game.round ?: return
    val duration = state.completedDurationMillis ?: return
    val timeout = game.failure == RotationMatchFailure.Timeout
    Icon(
        if (timeout) Icons.Default.Timer else Icons.Default.Close,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = if (timeout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Text(
        stringResource(
            if (timeout) {
                AppResource.String.rotation_match_timeout_title
            } else {
                AppResource.String.rotation_match_wrong_title
            },
        ),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(
            if (timeout) {
                AppResource.String.rotation_match_timeout_message
            } else {
                AppResource.String.rotation_match_wrong_message
            },
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    PatternPair(round)
    AnswerControls(state, onAnswerClick)
    if (state.isNewRecord) {
        Text(
            stringResource(AppResource.String.rotation_match_new_record),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
    GameResultCard(
        primaryText = stringResource(AppResource.String.rotation_match_score_value, game.score),
        secondaryText =
            stringResource(
                AppResource.String.rotation_match_result_details,
                state.previousRecord,
                game.correctAnswers,
                stringResource(round.difficulty.resource()),
                rotationMatchDurationText(duration),
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onReplayClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(Icons.Default.Replay, contentDescription = null)
        Text(stringResource(AppResource.String.rotation_match_play_again))
    }
    if (onBackClick != null) {
        OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(AppResource.String.rotation_match_back_to_games))
        }
    }
}

@Composable
private fun rotationMatchDurationText(durationMillis: Long): String =
    when (val parts = rotationMatchDurationDisplayParts(durationMillis)) {
        RotationMatchDurationDisplayParts.LessThanOneSecond ->
            stringResource(AppResource.String.rotation_match_duration_less_than_one_second)
        is RotationMatchDurationDisplayParts.Seconds ->
            pluralStringResource(
                AppResource.Plural.rotation_match_duration_seconds,
                parts.seconds.pluralQuantity(),
                parts.seconds,
            )
        is RotationMatchDurationDisplayParts.MinutesSeconds -> {
            val minutes =
                pluralStringResource(
                    AppResource.Plural.rotation_match_duration_minutes,
                    parts.minutes.pluralQuantity(),
                    parts.minutes,
                )
            val seconds = parts.seconds ?: return minutes
            stringResource(
                AppResource.String.rotation_match_duration_minutes_seconds,
                minutes,
                pluralStringResource(
                    AppResource.Plural.rotation_match_duration_seconds,
                    seconds.pluralQuantity(),
                    seconds,
                ),
            )
        }
    }

internal sealed interface RotationMatchDurationDisplayParts {
    data object LessThanOneSecond : RotationMatchDurationDisplayParts

    data class Seconds(val seconds: Long) : RotationMatchDurationDisplayParts

    data class MinutesSeconds(
        val minutes: Long,
        val seconds: Long?,
    ) : RotationMatchDurationDisplayParts
}

internal fun rotationMatchDurationDisplayParts(durationMillis: Long): RotationMatchDurationDisplayParts {
    val seconds = durationMillis.coerceAtLeast(0) / 1_000
    if (seconds == 0L) return RotationMatchDurationDisplayParts.LessThanOneSecond
    if (seconds < 60L) return RotationMatchDurationDisplayParts.Seconds(seconds)
    return RotationMatchDurationDisplayParts.MinutesSeconds(
        minutes = seconds / 60,
        seconds = (seconds % 60).takeIf { it > 0 },
    )
}

private fun Long.pluralQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private val previewReference = RotationMatchBoard(3, setOf(0, 1, 2, 3))
private val previewRound =
    RotationMatchRound(
        reference = previewReference,
        candidate = previewReference.rotateClockwise(),
        correctAnswer = RotationMatchAnswer.Match,
        difficulty = RotationMatchDifficulty.Easy,
        totalTimeMillis = 10_000,
        remainingTimeMillis = 7_200,
    )

private fun previewState(
    phase: RotationMatchPhase,
    difficulty: RotationMatchDifficulty = RotationMatchDifficulty.Easy,
    failure: RotationMatchFailure? = null,
    answer: RotationMatchAnswer? = null,
    score: Int = 0,
    isNewRecord: Boolean = false,
    loading: Boolean = false,
    durationMillis: Long? = null,
): RotationMatchUiState {
    val size =
        when (difficulty) {
            RotationMatchDifficulty.Easy -> 3
            RotationMatchDifficulty.Medium -> 4
            RotationMatchDifficulty.Hard -> 5
        }
    val filled =
        when (difficulty) {
            RotationMatchDifficulty.Easy -> setOf(0, 1, 2, 3)
            RotationMatchDifficulty.Medium -> setOf(1, 5, 6, 9, 10, 13)
            RotationMatchDifficulty.Hard -> setOf(1, 6, 7, 8, 11, 12, 16, 21)
        }
    val reference = RotationMatchBoard(size, filled)
    val correct =
        if (failure == RotationMatchFailure.Wrong) {
            RotationMatchAnswer.Different
        } else {
            RotationMatchAnswer.Match
        }
    val candidate =
        if (correct == RotationMatchAnswer.Match) {
            reference.rotateClockwise()
        } else {
            reference.mirrorHorizontally().rotateClockwise()
        }
    val total =
        when (difficulty) {
            RotationMatchDifficulty.Easy -> 10_000L
            RotationMatchDifficulty.Medium -> 8_000L
            RotationMatchDifficulty.Hard -> 6_000L
        }
    return RotationMatchUiState(
        game =
            RotationMatchState(
                phase = phase,
                score = score,
                correctAnswers = score,
                round =
                    RotationMatchRound(
                        reference,
                        candidate,
                        correct,
                        difficulty,
                        total,
                        if (failure == RotationMatchFailure.Timeout) 0 else total / 2,
                    ),
                selectedAnswer = answer,
                failure = failure,
            ),
        record = 7,
        previousRecord = 4,
        isRecordLoading = loading,
        isNewRecord = isNewRecord,
        completedDurationMillis = durationMillis,
    )
}

@Preview(name = "Rotation Match — loading standalone", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun RotationMatchLoadingStandalonePreview() {
    OquTurboTheme { RotationMatchScreen(RotationMatchUiState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Rotation Match — ready English hub", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun RotationMatchReadyEnglishHubPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            RotationMatchUiState(record = 7, isRecordLoading = false),
            {},
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Rotation Match — ready Russian hub", widthDp = 390, heightDp = 900, locale = "ru")
@ScreenshotPreview
@Composable
private fun RotationMatchReadyRussianHubPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            RotationMatchUiState(record = 7, isRecordLoading = false),
            {},
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Rotation Match — active Easy", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun RotationMatchActiveEasyPreview() {
    OquTurboTheme { RotationMatchScreen(previewState(RotationMatchPhase.Active), {}, { _, _ -> }, null) }
}

@Preview(name = "Rotation Match — active Medium", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun RotationMatchActiveMediumPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(RotationMatchPhase.Active, RotationMatchDifficulty.Medium, score = 6),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rotation Match — active Hard", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun RotationMatchActiveHardPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(RotationMatchPhase.Active, RotationMatchDifficulty.Hard, score = 11),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rotation Match — correct feedback", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun RotationMatchCorrectFeedbackPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(RotationMatchPhase.CorrectFeedback, answer = RotationMatchAnswer.Match, score = 3),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rotation Match — wrong", widthDp = 390, heightDp = 1100)
@ScreenshotPreview
@Composable
private fun RotationMatchWrongPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(
                RotationMatchPhase.Result,
                failure = RotationMatchFailure.Wrong,
                answer = RotationMatchAnswer.Match,
                score = 3,
                durationMillis = 12_300,
            ),
            {},
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Rotation Match — timeout", widthDp = 390, heightDp = 1100)
@ScreenshotPreview
@Composable
private fun RotationMatchTimeoutPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(
                RotationMatchPhase.Result,
                difficulty = RotationMatchDifficulty.Medium,
                failure = RotationMatchFailure.Timeout,
                score = 7,
                durationMillis = 65_300,
            ),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rotation Match — new record", widthDp = 390, heightDp = 1160)
@ScreenshotPreview
@Composable
private fun RotationMatchNewRecordPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(
                RotationMatchPhase.Result,
                failure = RotationMatchFailure.Wrong,
                answer = RotationMatchAnswer.Match,
                score = 8,
                isNewRecord = true,
                durationMillis = 125_000,
            ),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rotation Match — hard active compact Kazakh", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun RotationMatchHardActiveCompactKazakhPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(RotationMatchPhase.Active, RotationMatchDifficulty.Hard, score = 11),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rotation Match — long result compact Kazakh", widthDp = 320, heightDp = 1300, locale = "kk")
@ScreenshotPreview
@Composable
private fun RotationMatchLongResultCompactKazakhPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(
                RotationMatchPhase.Result,
                RotationMatchDifficulty.Hard,
                RotationMatchFailure.Wrong,
                RotationMatchAnswer.Match,
                score = 12,
                durationMillis = 725_300,
            ),
            {},
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Rotation Match — result Russian", widthDp = 390, heightDp = 1200, locale = "ru")
@ScreenshotPreview
@Composable
private fun RotationMatchResultRussianPreview() {
    OquTurboTheme {
        RotationMatchScreen(
            previewState(
                RotationMatchPhase.Result,
                failure = RotationMatchFailure.Timeout,
                score = 5,
                durationMillis = 62_000,
            ),
            {},
            { _, _ -> },
            {},
        )
    }
}
