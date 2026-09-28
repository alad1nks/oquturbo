package com.alad1nks.oquturbo.feature.ruleswitch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameHeaderActionButton
import com.alad1nks.oquturbo.core.ui.component.GameResultCard
import com.alad1nks.oquturbo.core.ui.component.GameScoreBadge
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchBoard
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchFailure
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchGame
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchPhase
import com.alad1nks.oquturbo.feature.ruleswitch.model.RuleSwitchState
import com.alad1nks.oquturbo.feature.ruleswitch.model.SwitchAnswer
import com.alad1nks.oquturbo.feature.ruleswitch.model.SwitchRule
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun RuleSwitchRoute(viewModel: RuleSwitchViewModel, onBackClick: (() -> Unit)?) {
    val state by viewModel.uiState.collectAsState()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer =
            LifecycleEventObserver { _, _ ->
                viewModel.setForeground(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            }
        owner.lifecycle.addObserver(observer)
        viewModel.setForeground(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            owner.lifecycle.removeObserver(observer)
            viewModel.setForeground(false)
        }
    }
    val unfinished = state.game.phase in listOf(RuleSwitchPhase.Active, RuleSwitchPhase.Correct, RuleSwitchPhase.Paused)
    val blocked = state.game.phase == RuleSwitchPhase.Result && state.saveStatus != RuleSwitchSaveStatus.Saved
    val exit: () -> Unit = { exitRuleSwitch(viewModel, onBackClick) }
    BackHandler(enabled = unfinished || blocked) { exit() }
    RuleSwitchScreen(
        state,
        viewModel::start,
        viewModel::selectAnswer,
        onBackClick?.let { exit },
        onPauseClick = viewModel::pause,
        onResumeClick = viewModel::resume,
        onReloadClick = viewModel::loadRecord,
        onRetrySaveClick = viewModel::retrySave,
        onExitClick = exit,
    )
}

// A callback retained from an earlier composition must ask the live attempt before navigating.
internal fun exitRuleSwitch(viewModel: RuleSwitchViewModel, onBackClick: (() -> Unit)?) {
    if (viewModel.abandon()) onBackClick?.invoke()
}

@Composable
internal fun RuleSwitchScreen(
    state: RuleSwitchUiState,
    onStartClick: () -> Unit,
    onAnswerClick: (Long, SwitchAnswer) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onReloadClick: () -> Unit = {},
    onRetrySaveClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().appBackground().statusBarsPadding().navigationBarsPadding()) {
        val fieldSide = (maxHeight - 390.dp).coerceIn(120.dp, 160.dp)
        val horizontal = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                .padding(horizontal = horizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val canLeave = state.game.phase != RuleSwitchPhase.Result || state.saveStatus == RuleSwitchSaveStatus.Saved
            RuleSwitchHeader(state, if (canLeave) onBackClick else null)
            when (state.game.phase) {
                RuleSwitchPhase.Ready -> ReadyContent(state, onStartClick, onReloadClick)
                RuleSwitchPhase.Active, RuleSwitchPhase.Correct ->
                    PlayingContent(
                        state,
                        onAnswerClick,
                        onPauseClick,
                        fieldSide,
                    )
                RuleSwitchPhase.Paused -> PausedContent(onResumeClick, state.isForeground)
                RuleSwitchPhase.Result ->
                    ResultContent(
                        state,
                        onStartClick,
                        onBackClick,
                        onReloadClick,
                        onRetrySaveClick,
                    )
            }
            if (state.game.phase in listOf(RuleSwitchPhase.Active, RuleSwitchPhase.Correct, RuleSwitchPhase.Paused)) {
                OutlinedButton(onExitClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(AppResource.String.rule_switch_exit))
                }
            }
            Box(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun RuleSwitchHeader(state: RuleSwitchUiState, onBackClick: (() -> Unit)?) {
    val stacked = LocalDensity.current.fontScale > 1.2f || state.game.score >= 1000 || state.record >= 1000
    val best =
        if (state.isRecordLoading || state.recordLoadFailed) {
            stringResource(AppResource.String.rule_switch_placeholder)
        } else {
            state.record.toString()
        }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBackClick != null) {
                AppBackButton(onBackClick, contentDescription = stringResource(AppResource.String.rule_switch_back))
            }
            Text(
                stringResource(AppResource.String.rule_switch_title),
                Modifier.weight(1f).semantics {
                    heading()
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (stacked) {
            GameScoreBadge(
                stringResource(AppResource.String.rule_switch_score),
                state.game.score.toString(),
                Modifier.fillMaxWidth(),
            )
            GameScoreBadge(stringResource(AppResource.String.rule_switch_record), best, Modifier.fillMaxWidth())
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameScoreBadge(
                    stringResource(AppResource.String.rule_switch_score),
                    state.game.score.toString(),
                    Modifier.weight(1f),
                )
                GameScoreBadge(stringResource(AppResource.String.rule_switch_record), best, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: RuleSwitchUiState,
    onStartClick: () -> Unit,
    onReloadClick: () -> Unit,
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
                stringResource(AppResource.String.rule_switch_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(AppResource.String.rule_switch_ready_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.rule_switch_instructions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            RuleSwitchGlyph(Modifier.size(56.dp))
            Text(
                if (state.recordLoadFailed) {
                    stringResource(AppResource.String.rule_switch_record_unavailable)
                } else if (state.isRecordLoading) {
                    stringResource(AppResource.String.rule_switch_loading_record)
                } else {
                    stringResource(AppResource.String.rule_switch_record_value, state.record)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (state.recordLoadFailed) {
                OutlinedButton(onReloadClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(AppResource.String.rule_switch_retry_load))
                }
            }
            Button(
                onClick = onStartClick,
                enabled = !state.isRecordLoading && !state.recordLoadFailed,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(stringResource(AppResource.String.rule_switch_start))
            }
        }
    }
}

@Composable
private fun PausedContent(onResumeClick: () -> Unit, foreground: Boolean) {
    Surface(
        modifier =
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                stringResource(AppResource.String.rule_switch_paused_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.rule_switch_paused_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onResumeClick,
                enabled = foreground,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(AppResource.String.rule_switch_resume))
            }
        }
    }
}

@Composable
private fun PlayingContent(
    state: RuleSwitchUiState,
    onAnswerClick: (Long, SwitchAnswer) -> Unit,
    onPauseClick: () -> Unit,
    fieldSide: Dp,
) {
    val board = state.game.board ?: return
    val active = state.game.phase == RuleSwitchPhase.Active
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    AppResource.String.rule_switch_time_remaining,
                    ceil(board.remainingTimeMillis / 1000.0).toInt(),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { board.remainingTimeMillis.toFloat() / board.totalTimeMillis },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GameHeaderActionButton(
            Icons.Filled.Pause,
            onPauseClick,
            contentDescription = stringResource(AppResource.String.rule_switch_pause),
        )
    }
    RulePrompt(board.rule)
    Surface(
        Modifier.fillMaxWidth().heightIn(min = fieldSide).testTag("digit"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(board.digit.toString(), style = MaterialTheme.typography.displayLarge)
        }
    }
    if (LocalDensity.current.fontScale > 1.2f) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            board.options.forEach {
                value ->
                AnswerButton(board, value, active, onAnswerClick, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            board.options.forEach { value -> AnswerButton(board, value, active, onAnswerClick, Modifier.weight(1f)) }
        }
    }
    Row(
        Modifier.heightIn(min = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!active) {
            Icon(Icons.Default.Check, null, Modifier.size(20.dp))
            Text(stringResource(AppResource.String.rule_switch_correct))
        }
    }
}

@Composable
private fun AnswerButton(
    board: RuleSwitchBoard,
    value: SwitchAnswer,
    active: Boolean,
    onAnswer: (Long, SwitchAnswer) -> Unit,
    modifier: Modifier,
) {
    Button(onClick = {
        onAnswer(board.id, value)
    }, enabled = active, modifier = modifier.heightIn(min = 56.dp).testTag("answer-$value")) {
        Text(answerName(value), textAlign = TextAlign.Center)
    }
}

@Composable
private fun answerName(answer: SwitchAnswer): String =
    stringResource(
        when (answer) {
            SwitchAnswer.Even -> AppResource.String.rule_switch_even
            SwitchAnswer.Odd -> AppResource.String.rule_switch_odd
            SwitchAnswer.Below -> AppResource.String.rule_switch_below
            SwitchAnswer.Above -> AppResource.String.rule_switch_above
        },
    )

@Composable
private fun RulePrompt(rule: SwitchRule) {
    Text(
        stringResource(
            if (rule == SwitchRule.Parity) {
                AppResource.String.rule_switch_parity
            } else {
                AppResource.String.rule_switch_magnitude
            },
        ),
        Modifier.testTag("rule"),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RuleSwitchGlyph(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val unit = size.minDimension / 100f
        val stroke = 6 * unit
        drawRect(
            color,
            Offset(13 * unit, 24 * unit),
            Size(18 * unit, 18 * unit),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawLine(color, Offset(35 * unit, 33 * unit), Offset(80 * unit, 33 * unit), stroke)
        drawLine(color, Offset(69 * unit, 22 * unit), Offset(80 * unit, 33 * unit), stroke)
        drawLine(color, Offset(69 * unit, 44 * unit), Offset(80 * unit, 33 * unit), stroke)
        drawCircle(
            color,
            9 * unit,
            Offset(78 * unit, 67 * unit),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawLine(color, Offset(20 * unit, 67 * unit), Offset(63 * unit, 67 * unit), stroke)
        drawLine(color, Offset(31 * unit, 56 * unit), Offset(20 * unit, 67 * unit), stroke)
        drawLine(color, Offset(31 * unit, 78 * unit), Offset(20 * unit, 67 * unit), stroke)
    }
}

@Composable
private fun ResultContent(
    state: RuleSwitchUiState,
    onRetry: () -> Unit,
    onBackClick: (() -> Unit)?,
    onReloadClick: () -> Unit,
    onRetrySaveClick: () -> Unit,
) {
    val game = state.game
    val board = game.board ?: return
    val target = board.correctAnswer
    val timeout = game.failure == RuleSwitchFailure.Timeout
    Column(
        Modifier.fillMaxWidth().testTag("result-announcement").semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(if (timeout) Icons.Default.Timer else Icons.Default.Close, null, Modifier.size(40.dp))
        Text(
            stringResource(
                if (timeout) {
                    AppResource.String.rule_switch_timeout_title
                } else {
                    AppResource.String.rule_switch_wrong_title
                },
            ),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        RulePrompt(board.rule)
        Text(stringResource(AppResource.String.rule_switch_digit, board.digit))
        if (!timeout) {
            Text(
                stringResource(AppResource.String.rule_switch_chosen, answerName(requireNotNull(game.selectedAnswer))),
                Modifier.testTag("chosen"),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(AppResource.String.rule_switch_correct_answer, answerName(target)),
            textAlign = TextAlign.Center,
        )
        GameResultCard(
            primaryText = stringResource(AppResource.String.rule_switch_score_value, game.score),
            secondaryText = stringResource(AppResource.String.rule_switch_record_value, state.record),
            modifier = Modifier.fillMaxWidth(),
        )
        when (state.saveStatus) {
            RuleSwitchSaveStatus.Pending ->
                Text(
                    stringResource(AppResource.String.rule_switch_saving_result),
                    textAlign = TextAlign.Center,
                )
            RuleSwitchSaveStatus.Failed ->
                Text(
                    stringResource(AppResource.String.rule_switch_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            else ->
                if (state.isNewRecord) {
                    Text(
                        stringResource(AppResource.String.rule_switch_new_record),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
        }
        if (state.recordLoadFailed) {
            Text(
                stringResource(AppResource.String.rule_switch_record_unavailable),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
    Button(
        if (state.saveStatus == RuleSwitchSaveStatus.Failed) {
            onRetrySaveClick
        } else if (state.recordLoadFailed) {
            onReloadClick
        } else {
            onRetry
        },
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = !state.isRecordLoading && state.saveStatus != RuleSwitchSaveStatus.Pending,
    ) {
        Icon(Icons.Default.Replay, null)
        Text(
            stringResource(
                if (state.saveStatus == RuleSwitchSaveStatus.Failed) {
                    AppResource.String.rule_switch_retry_save
                } else if (state.recordLoadFailed) {
                    AppResource.String.rule_switch_retry_load
                } else {
                    AppResource.String.rule_switch_retry
                },
            ),
        )
    }
    if (onBackClick != null && state.saveStatus == RuleSwitchSaveStatus.Saved) {
        OutlinedButton(onBackClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(AppResource.String.rule_switch_back))
        }
    }
}

internal fun ruleSwitchPreviewState(
    phase: RuleSwitchPhase = RuleSwitchPhase.Ready,
    size: Int = 3,
    failure: RuleSwitchFailure? = null,
    save: RuleSwitchSaveStatus = RuleSwitchSaveStatus.None,
): RuleSwitchUiState {
    val rule = if (size == 4) SwitchRule.Magnitude else SwitchRule.Parity
    return RuleSwitchUiState(
        game =
            RuleSwitchState(
                phase = phase,
                board =
                    RuleSwitchBoard(
                        1,
                        if (rule == SwitchRule.Parity) 8 else 3,
                        rule,
                        RuleSwitchGame.timeFor(if (size == 3) 0 else 5),
                    ),
                score =
                    if (size == 3) {
                        0
                    } else if (size == 4) {
                        5
                    } else {
                        10
                    },
                failure = failure,
                selectedAnswer =
                    if (failure == RuleSwitchFailure.Wrong) {
                        if (rule == SwitchRule.Parity) SwitchAnswer.Odd else SwitchAnswer.Above
                    } else {
                        null
                    },
            ),
        record = 7,
        isRecordLoading = false,
        saveStatus = save,
    )
}

@Preview(name = "Rule Switch ReadyHub", widthDp = 390, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchReadyHubPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch ReadyStandalone", widthDp = 390, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchReadyStandalonePreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Rule Switch ReadyRu", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchReadyRuPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch ReadyKk", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun RuleSwitchReadyKkPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Loading", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchLoadingPreview() {
    OquTurboTheme { RuleSwitchScreen(RuleSwitchUiState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Rule Switch LoadError", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchLoadErrorPreview() {
    OquTurboTheme {
        RuleSwitchScreen(RuleSwitchUiState(isRecordLoading = false, recordLoadFailed = true), {}, { _, _ -> }, null)
    }
}

@Preview(name = "Rule Switch Active3", widthDp = 390, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchActive3Preview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 3), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Active4", widthDp = 390, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchActive4Preview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 4), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Active5", widthDp = 390, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchActive5Preview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Compacten", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchCompactenPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Compactru", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchCompactruPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Compactkk", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun RuleSwitchCompactkkPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch Correct", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchCorrectPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Correct, 4), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch PausedRu", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchPausedRuPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Paused), {}, { _, _ -> }, {}) }
}

@Preview(name = "Rule Switch PausedKk", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun RuleSwitchPausedKkPreview() {
    OquTurboTheme {
        val state = ruleSwitchPreviewState(RuleSwitchPhase.Paused)
        RuleSwitchScreen(
            state.copy(game = state.game.copy(returnPhase = RuleSwitchPhase.Correct)),
            {},
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rule Switch Wrong", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchWrongPreview() {
    OquTurboTheme {
        RuleSwitchScreen(
            ruleSwitchPreviewState(
                RuleSwitchPhase.Result,
                4,
                RuleSwitchFailure.Wrong,
                RuleSwitchSaveStatus.Saved,
            ),
            {
            },
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Rule Switch Timeout", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun RuleSwitchTimeoutPreview() {
    OquTurboTheme {
        RuleSwitchScreen(
            ruleSwitchPreviewState(
                RuleSwitchPhase.Result,
                3,
                RuleSwitchFailure.Timeout,
                RuleSwitchSaveStatus.Saved,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rule Switch Pending", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchPendingPreview() {
    OquTurboTheme {
        RuleSwitchScreen(
            ruleSwitchPreviewState(
                RuleSwitchPhase.Result,
                4,
                RuleSwitchFailure.Wrong,
                RuleSwitchSaveStatus.Pending,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rule Switch SaveError", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchSaveErrorPreview() {
    OquTurboTheme {
        RuleSwitchScreen(
            ruleSwitchPreviewState(
                RuleSwitchPhase.Result,
                4,
                RuleSwitchFailure.Wrong,
                RuleSwitchSaveStatus.Failed,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rule Switch NewRecord", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchNewRecordPreview() {
    OquTurboTheme {
        RuleSwitchScreen(
            ruleSwitchPreviewState(
                RuleSwitchPhase.Result,
                5,
                RuleSwitchFailure.Wrong,
                RuleSwitchSaveStatus.Saved,
            ).copy(record = 10, isNewRecord = true),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Rule Switch dark", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchDarkPreview() {
    OquTurboTheme(darkTheme = true) {
        RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 5), {}, { _, _ -> }, {})
    }
}

@Preview(name = "Rule Switch large text", widthDp = 320, heightDp = 640, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun RuleSwitchLargeTextPreview() {
    val state = ruleSwitchPreviewState(RuleSwitchPhase.Active, 5)
    OquTurboTheme {
        RuleSwitchScreen(state.copy(game = state.game.copy(score = 12345), record = 23456), {}, { _, _ -> }, {})
    }
}

@Preview(name = "Rule Switch large result", widthDp = 320, heightDp = 640, locale = "kk", fontScale = 2f)
@ScreenshotPreview
@Composable
private fun RuleSwitchLargeResultPreview() {
    OquTurboTheme {
        RuleSwitchScreen(
            ruleSwitchPreviewState(
                RuleSwitchPhase.Result,
                5,
                RuleSwitchFailure.Wrong,
                RuleSwitchSaveStatus.Saved,
            ).copy(record = 10),
            {},
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Rule Switch Magnitude en", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun RuleSwitchMagnitudeenPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 4), {}, { _, _ -> }, null) }
}

@Preview(name = "Rule Switch Magnitude ru", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun RuleSwitchMagnituderuPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 4), {}, { _, _ -> }, null) }
}

@Preview(name = "Rule Switch Magnitude kk", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun RuleSwitchMagnitudekkPreview() {
    OquTurboTheme { RuleSwitchScreen(ruleSwitchPreviewState(RuleSwitchPhase.Active, 4), {}, { _, _ -> }, null) }
}
