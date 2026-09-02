@file:Suppress("ktlint:standard:max-line-length")

package com.alad1nks.oquturbo.feature.wordflow.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowFailure
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPhase
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowPrompt
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowRound
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowState
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil

internal fun wordFlowBackAction(
    onBackClick: (() -> Unit)?,
    onAbandon: () -> Unit,
): (() -> Unit)? =
    onBackClick?.let { callback ->
        {
            onAbandon()
            callback()
        }
    }

internal fun WordFlowState.difficultyResource(): StringResource? =
    when (tier) {
        WordFlowTier.Easy -> AppResource.String.word_flow_difficulty_easy
        WordFlowTier.Medium -> AppResource.String.word_flow_difficulty_medium
        WordFlowTier.Hard -> AppResource.String.word_flow_difficulty_hard
        null -> null
    }

@Composable
internal fun WordFlowRoute(
    viewModel: WordFlowViewModel,
    onBackClick: (() -> Unit)?,
) {
    val state by viewModel.uiState.collectAsState()
    WordFlowScreen(
        state = state,
        onStartClick = viewModel::start,
        onChoiceClick = viewModel::selectAnswer,
        onBackClick = wordFlowBackAction(onBackClick, viewModel::abandon),
    )
}

@Composable
internal fun WordFlowScreen(
    state: WordFlowUiState,
    onStartClick: () -> Unit,
    onChoiceClick: (String) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    Box(modifier.fillMaxSize().appBackground()) {
        Column(
            modifier =
                Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).navigationBarsPadding()
                    .padding(start = 24.dp, top = 104.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (game.phase) {
                WordFlowPhase.Ready -> ReadyContent(state, onStartClick)
                WordFlowPhase.Active, WordFlowPhase.CorrectFeedback -> PlayingContent(state, onChoiceClick)
                WordFlowPhase.Result -> ResultContent(state, onStartClick, onChoiceClick, onBackClick)
            }
        }
        GameHeader(
            scoreLabel = stringResource(AppResource.String.word_flow_score),
            score = game.score.toString(),
            recordLabel = stringResource(AppResource.String.word_flow_record),
            record =
                if (state.isRecordLoading) {
                    stringResource(
                        AppResource.String.word_flow_loading_record,
                    )
                } else {
                    state.record.toString()
                },
            leadingContent =
                onBackClick?.let { callback ->
                    {
                        AppBackButton(
                            onClick = callback,
                            contentDescription = stringResource(AppResource.String.word_flow_back),
                        )
                    }
                },
            modifier =
                Modifier.align(Alignment.TopCenter).widthIn(max = 760.dp).fillMaxWidth()
                    .statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun ReadyContent(state: WordFlowUiState, onStartClick: () -> Unit) {
    Surface(
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
                stringResource(AppResource.String.word_flow_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(AppResource.String.word_flow_ready_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.word_flow_instructions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                if (state.isRecordLoading) {
                    stringResource(AppResource.String.word_flow_loading_record)
                } else {
                    stringResource(AppResource.String.word_flow_record_value, state.record)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onStartClick,
                enabled = !state.isRecordLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(stringResource(AppResource.String.word_flow_start))
            }
        }
    }
}

@Composable
private fun PlayingContent(state: WordFlowUiState, onChoiceClick: (String) -> Unit) {
    val round = state.game.round ?: return
    TimerRow(round)
    val sentence = round.prompt.sentenceWithBlank(stringResource(AppResource.String.word_flow_blank))
    SentenceCard(
        sentence = sentence,
        accessibilityDescription = stringResource(AppResource.String.word_flow_sentence_accessibility, sentence),
    )
    ChoiceList(state, onChoiceClick)
}

@Composable
private fun TimerRow(round: WordFlowRound) {
    val seconds = ceil(round.remainingTimeMillis / 1000.0).toInt()
    val timerDescription = stringResource(AppResource.String.word_flow_timer_accessibility, seconds)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(AppResource.String.word_flow_time), fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(AppResource.String.word_flow_seconds, seconds),
                modifier =
                    Modifier.semantics {
                        contentDescription = timerDescription
                    },
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
private fun SentenceCard(
    sentence: String,
    label: String? = null,
    accessibilityDescription: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (label != null) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                sentence,
                modifier =
                    Modifier.semantics {
                        heading()
                        if (accessibilityDescription != null) contentDescription = accessibilityDescription
                    },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChoiceList(state: WordFlowUiState, onChoiceClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.game.round?.choices.orEmpty().forEach { choice -> Choice(state, choice, onChoiceClick) }
    }
}

@Composable
private fun Choice(state: WordFlowUiState, choice: String, onChoiceClick: (String) -> Unit) {
    val game = state.game
    val round = game.round ?: return
    val enabled = game.phase == WordFlowPhase.Active
    val isCorrect = choice == round.prompt.correctAnswer
    val isSelectedWrong = game.failure == WordFlowFailure.Wrong && choice == game.selectedAnswer
    val showCorrect =
        isCorrect &&
            (game.phase == WordFlowPhase.CorrectFeedback || game.phase == WordFlowPhase.Result)
    val feedback =
        when {
            isSelectedWrong -> stringResource(AppResource.String.word_flow_your_answer)
            showCorrect && game.phase == WordFlowPhase.CorrectFeedback ->
                stringResource(
                    AppResource.String.word_flow_correct,
                )
            showCorrect -> stringResource(AppResource.String.word_flow_correct_answer)
            else -> null
        }
    val choiceDescription =
        if (feedback == null) {
            choice
        } else {
            stringResource(
                AppResource.String.word_flow_choice_accessibility,
                choice,
                feedback,
            )
        }
    Surface(
        modifier =
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = choiceDescription
                    if (!enabled) disabled()
                }.clickable(enabled = enabled) { onChoiceClick(choice) },
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
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCorrect) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            if (isSelectedWrong) Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f)) {
                Text(choice, style = MaterialTheme.typography.titleMedium)
                if (feedback != null) {
                    Text(
                        feedback,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: WordFlowUiState,
    onReplayClick: () -> Unit,
    onChoiceClick: (String) -> Unit,
    onBackClick: (() -> Unit)?,
) {
    val game = state.game
    val prompt = game.round?.prompt ?: return
    val difficultyResource = game.difficultyResource() ?: return
    val completedDurationMillis = state.completedDurationMillis ?: return
    val timeout = game.failure == WordFlowFailure.Timeout
    Icon(
        if (timeout) Icons.Default.Timer else Icons.Default.Close,
        contentDescription = null,
        tint = if (timeout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Text(
        stringResource(
            if (timeout) AppResource.String.word_flow_timeout_title else AppResource.String.word_flow_wrong_title,
        ),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(
            if (timeout) AppResource.String.word_flow_timeout_message else AppResource.String.word_flow_wrong_message,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    ChoiceList(state, onChoiceClick)
    SentenceCard(
        sentence = prompt.sentence(prompt.correctAnswer),
        label = stringResource(AppResource.String.word_flow_completed_sentence),
    )
    if (state.isNewRecord) {
        Text(
            stringResource(AppResource.String.word_flow_new_record),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
    GameResultCard(
        primaryText = stringResource(AppResource.String.word_flow_score_value, game.score),
        secondaryText =
            stringResource(
                AppResource.String.word_flow_result_details,
                state.record,
                stringResource(difficultyResource),
                wordFlowDurationText(completedDurationMillis),
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onReplayClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(Icons.Default.Replay, contentDescription = null)
        Text(stringResource(AppResource.String.word_flow_replay))
    }
    if (onBackClick != null) {
        OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(AppResource.String.word_flow_back))
        }
    }
}

@Composable
private fun wordFlowDurationText(durationMillis: Long): String =
    when (val parts = wordFlowDurationDisplayParts(durationMillis)) {
        WordFlowDurationDisplayParts.LessThanOneSecond ->
            stringResource(AppResource.String.word_flow_duration_less_than_one_second)
        is WordFlowDurationDisplayParts.Seconds ->
            pluralStringResource(
                AppResource.Plural.word_flow_duration_seconds,
                parts.seconds.pluralQuantity(),
                parts.seconds,
            )
        is WordFlowDurationDisplayParts.MinutesSeconds -> {
            val minutes =
                pluralStringResource(
                    AppResource.Plural.word_flow_duration_minutes,
                    parts.minutes.pluralQuantity(),
                    parts.minutes,
                )
            val seconds = parts.seconds ?: return minutes
            stringResource(
                AppResource.String.word_flow_duration_minutes_seconds,
                minutes,
                pluralStringResource(
                    AppResource.Plural.word_flow_duration_seconds,
                    seconds.pluralQuantity(),
                    seconds,
                ),
            )
        }
    }

private fun Long.pluralQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private val previewPrompt =
    WordFlowPrompt(
        id = "preview",
        tier = WordFlowTier.Easy,
        sentenceTemplate = "The reader placed a bookmark before closing the %1\$s.",
        correctAnswer = "book",
        wrongAnswers = listOf("window", "spoon"),
    )

private val compactKazakhPrompt =
    previewPrompt.copy(
        id = "preview-kk",
        tier = WordFlowTier.Hard,
        sentenceTemplate = "Өлшемдер әртүрлі болғандықтан, зерттеушілер қорытындыны қосымша деректер %1\$s дейін алдын ала деп санады.",
        correctAnswer = "жиналғанға",
        wrongAnswers = listOf("жоғалғанға", "ұмытылғанға"),
    )

private fun previewState(
    phase: WordFlowPhase,
    failure: WordFlowFailure? = null,
    selected: String? = null,
    score: Int = 3,
    isNewRecord: Boolean = false,
    tier: WordFlowTier = WordFlowTier.Easy,
    completedDurationMillis: Long? = null,
) = WordFlowUiState(
    game =
        WordFlowState(
            phase = phase,
            score = score,
            correctAnswers = score,
            round =
                WordFlowRound(
                    previewPrompt.copy(tier = tier),
                    listOf("window", "book", "spoon"),
                    10_000,
                    7_000,
                ),
            selectedAnswer = selected,
            failure = failure,
        ),
    record = maxOf(2, score),
    isRecordLoading = false,
    isNewRecord = isNewRecord,
    completedDurationMillis = completedDurationMillis,
)

private fun compactKazakhState(): WordFlowUiState =
    previewState(WordFlowPhase.Result, WordFlowFailure.Timeout).let { state ->
        state.copy(
            game =
                state.game.copy(
                    round = WordFlowRound(compactKazakhPrompt, compactKazakhPrompt.answers, 6_000, 0),
                ),
            locale = "kk",
            completedDurationMillis = 725_300L,
        )
    }

@Preview(name = "Word Flow — loading", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun WordFlowLoadingPreview() {
    OquTurboTheme { WordFlowScreen(WordFlowUiState(), {}, {}, null) }
}

@Preview(name = "Word Flow — ready", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun WordFlowReadyPreview() {
    OquTurboTheme { WordFlowScreen(WordFlowUiState(record = 4, isRecordLoading = false), {}, {}, null) }
}

@Preview(name = "Word Flow — hub ready with Back", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun WordFlowHubReadyWithBackPreview() {
    OquTurboTheme { WordFlowScreen(WordFlowUiState(record = 4, isRecordLoading = false), {}, {}, {}) }
}

@Preview(name = "Word Flow — active", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun WordFlowActivePreview() {
    OquTurboTheme { WordFlowScreen(previewState(WordFlowPhase.Active), {}, {}, null) }
}

@Preview(name = "Word Flow — correct feedback", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun WordFlowCorrectFeedbackPreview() {
    OquTurboTheme {
        WordFlowScreen(previewState(WordFlowPhase.CorrectFeedback, selected = "book"), {}, {}, null)
    }
}

@Preview(name = "Word Flow — wrong", widthDp = 390, heightDp = 1100)
@ScreenshotPreview
@Composable
private fun WordFlowWrongPreview() {
    OquTurboTheme {
        WordFlowScreen(
            previewState(
                WordFlowPhase.Result,
                WordFlowFailure.Wrong,
                "window",
                completedDurationMillis = 15_300L,
            ),
            {},
            {},
            null,
        )
    }
}

@Preview(name = "Word Flow — timeout", widthDp = 390, heightDp = 1100)
@ScreenshotPreview
@Composable
private fun WordFlowTimeoutPreview() {
    OquTurboTheme {
        WordFlowScreen(
            previewState(
                phase = WordFlowPhase.Result,
                failure = WordFlowFailure.Timeout,
                tier = WordFlowTier.Medium,
                completedDurationMillis = 60_000L,
            ),
            {},
            {},
            null,
        )
    }
}

@Preview(name = "Word Flow — result viewport", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun WordFlowResultViewportPreview() {
    OquTurboTheme {
        WordFlowScreen(
            previewState(
                phase = WordFlowPhase.Result,
                failure = WordFlowFailure.Timeout,
                tier = WordFlowTier.Medium,
                completedDurationMillis = 65_300L,
            ),
            {},
            {},
            {},
        )
    }
}

@Preview(name = "Word Flow — new record", widthDp = 390, heightDp = 1160)
@ScreenshotPreview
@Composable
private fun WordFlowNewRecordPreview() {
    OquTurboTheme {
        WordFlowScreen(
            previewState(
                phase = WordFlowPhase.Result,
                failure = WordFlowFailure.Wrong,
                selected = "window",
                score = 8,
                isNewRecord = true,
                tier = WordFlowTier.Medium,
                completedDurationMillis = 999L,
            ),
            {},
            {},
            null,
        )
    }
}

@Preview(name = "Word Flow — compact Kazakh", widthDp = 320, heightDp = 1320, locale = "kk")
@ScreenshotPreview
@Composable
private fun WordFlowCompactKazakhPreview() {
    OquTurboTheme { WordFlowScreen(compactKazakhState(), {}, {}, {}) }
}
