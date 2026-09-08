package com.alad1nks.oquturbo.feature.numbertrail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
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
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailBoard
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailFailure
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailPhase
import com.alad1nks.oquturbo.feature.numbertrail.model.NumberTrailState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil

@Composable
internal fun NumberTrailRoute(viewModel: NumberTrailViewModel, onBackClick: (() -> Unit)?) {
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
    NumberTrailScreen(
        state,
        viewModel::start,
        viewModel::selectAnswer,
        onBackClick?.let {
            {
                viewModel.abandon()
                it()
            }
        },
        onPauseClick = viewModel::pause,
        onResumeClick = viewModel::resume,
        onReloadClick = viewModel::loadRecord,
    )
}

@Composable
internal fun NumberTrailScreen(
    state: NumberTrailUiState,
    onStartClick: () -> Unit,
    onAnswerClick: (Long, Int) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onReloadClick: () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().appBackground()) {
        val horizontal = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                .statusBarsPadding().navigationBarsPadding().padding(horizontal = horizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NumberTrailHeader(state, onBackClick)
            when (state.game.phase) {
                NumberTrailPhase.Ready -> ReadyContent(state, onStartClick, onReloadClick)
                NumberTrailPhase.Active, NumberTrailPhase.BoardComplete ->
                    PlayingContent(
                        state,
                        onAnswerClick,
                        onPauseClick,
                    )
                NumberTrailPhase.Paused -> PausedContent(onResumeClick)
                NumberTrailPhase.Result -> ResultContent(state, onStartClick, onBackClick, onReloadClick)
            }
            Box(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun NumberTrailHeader(state: NumberTrailUiState, onBackClick: (() -> Unit)?) {
    val stacked = LocalDensity.current.fontScale > 1.2f || state.game.score >= 1000 || state.record >= 1000
    val best =
        if (state.isRecordLoading || state.recordLoadFailed) {
            stringResource(AppResource.String.number_trail_placeholder)
        } else {
            state.record.toString()
        }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            AppBackButton(
                onBackClick,
                contentDescription = stringResource(AppResource.String.number_trail_back),
            )
        }
        if (stacked) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameScoreBadge(
                    stringResource(AppResource.String.number_trail_score),
                    state.game.score.toString(),
                    Modifier.fillMaxWidth(),
                )
                GameScoreBadge(stringResource(AppResource.String.number_trail_record), best, Modifier.fillMaxWidth())
            }
        } else {
            GameScoreBadge(
                stringResource(AppResource.String.number_trail_score),
                state.game.score.toString(),
                Modifier.weight(1f),
            )
            GameScoreBadge(stringResource(AppResource.String.number_trail_record), best, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadyContent(
    state: NumberTrailUiState,
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
                stringResource(AppResource.String.number_trail_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(AppResource.String.number_trail_ready_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.number_trail_instructions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                if (state.recordLoadFailed) {
                    stringResource(AppResource.String.number_trail_record_unavailable)
                } else if (state.isRecordLoading) {
                    stringResource(AppResource.String.number_trail_loading_record)
                } else {
                    stringResource(AppResource.String.number_trail_record_value, state.record)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick =
                    if (state.recordLoadFailed) {
                        onReloadClick
                    } else {
                        onStartClick
                    },
                enabled = !state.isRecordLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(
                    stringResource(
                        if (state.recordLoadFailed) {
                            AppResource.String.number_trail_retry_load
                        } else {
                            AppResource.String.number_trail_start
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PausedContent(onResumeClick: () -> Unit) {
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
                stringResource(AppResource.String.number_trail_paused_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.number_trail_paused_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onResumeClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(AppResource.String.number_trail_resume))
            }
        }
    }
}

@Composable
private fun PlayingContent(state: NumberTrailUiState, onAnswerClick: (Long, Int) -> Unit, onPauseClick: () -> Unit) {
    val board = state.game.board ?: return
    val active = state.game.phase == NumberTrailPhase.Active
    if (active) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        AppResource.String.number_trail_time_remaining,
                        ceil(board.remainingTimeMillis / 1000.0).toInt(),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearProgressIndicator(progress = {
                    board.remainingTimeMillis.toFloat() / board.totalTimeMillis
                }, modifier = Modifier.fillMaxWidth())
            }
            GameHeaderActionButton(
                Icons.Filled.Pause,
                onPauseClick,
                contentDescription = stringResource(AppResource.String.number_trail_pause),
            )
        }
    }
    Text(
        stringResource(AppResource.String.number_trail_grid_size, board.size),
        style = MaterialTheme.typography.titleMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!active) Icon(Icons.Default.Check, null)
        Text(
            if (active) {
                stringResource(
                    AppResource.String.number_trail_target,
                    board.target,
                )
            } else {
                stringResource(AppResource.String.number_trail_board_complete)
            },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boardWidth = maxWidth.coerceIn(216.dp, 400.dp)
        val horizontalScroll = rememberScrollState()
        Box(if (maxWidth < 216.dp) Modifier.horizontalScroll(horizontalScroll) else Modifier) {
            Column(Modifier.width(boardWidth).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(board.size) { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(board.size) { column ->
                            val index = row * board.size + column
                            val number = board.numbers[index]
                            val completed = number < board.target
                            val description = stringResource(AppResource.String.number_trail_tile_description, number)
                            val completedDescription = stringResource(AppResource.String.number_trail_tile_completed)
                            Surface(
                                Modifier.weight(1f).fillMaxSize().semantics {
                                    role = Role.Button
                                    contentDescription = description
                                    if (completed) stateDescription = completedDescription
                                    if (!active || completed) disabled()
                                }.clickable(enabled = active && !completed) { onAnswerClick(board.id, index) },
                                shape = MaterialTheme.shapes.medium,
                                color =
                                    if (completed) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                border =
                                    BorderStroke(
                                        1.dp,
                                        if (completed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        number.toString(),
                                        style =
                                            if (board.size == 4) {
                                                MaterialTheme.typography.titleLarge
                                            } else {
                                                MaterialTheme.typography.headlineMedium
                                            },
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (completed) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            Modifier.align(Alignment.TopEnd).padding(3.dp).size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (!active) Text(stringResource(AppResource.String.number_trail_next_board), textAlign = TextAlign.Center)
}

@Composable
private fun ResultContent(
    state: NumberTrailUiState,
    onRetry: () -> Unit,
    onBackClick: (() -> Unit)?,
    onReloadClick: () -> Unit,
) {
    val game = state.game
    val target = game.board?.target ?: return
    val timeout = game.failure == NumberTrailFailure.Timeout
    Icon(if (timeout) Icons.Default.Timer else Icons.Default.Close, null, Modifier.size(40.dp))
    Text(
        stringResource(
            if (timeout) AppResource.String.number_trail_timeout_title else AppResource.String.number_trail_wrong_title,
        ),
        Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        if (timeout) {
            stringResource(AppResource.String.number_trail_timeout_detail, target)
        } else {
            stringResource(AppResource.String.number_trail_wrong_detail, requireNotNull(game.selectedNumber), target)
        },
        textAlign = TextAlign.Center,
    )
    GameResultCard(
        primaryText = stringResource(AppResource.String.number_trail_score) + ": " + game.score,
        secondaryText = stringResource(AppResource.String.number_trail_record_value, state.record),
        modifier = Modifier.fillMaxWidth(),
    )
    when (state.saveStatus) {
        NumberTrailSaveStatus.Pending ->
            Text(
                stringResource(AppResource.String.number_trail_saving_result),
                textAlign = TextAlign.Center,
            )
        NumberTrailSaveStatus.Failed ->
            Text(
                stringResource(AppResource.String.number_trail_save_failed),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        else ->
            if (state.isNewRecord) {
                Text(
                    stringResource(AppResource.String.number_trail_new_record),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
    }
    if (state.recordLoadFailed) {
        Text(
            stringResource(AppResource.String.number_trail_record_unavailable),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
    Button(
        if (state.recordLoadFailed) {
            onReloadClick
        } else {
            onRetry
        },
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = !state.isRecordLoading,
    ) {
        Icon(Icons.Default.Replay, null)
        Text(
            stringResource(
                if (state.recordLoadFailed) {
                    AppResource.String.number_trail_retry_load
                } else {
                    AppResource.String.number_trail_retry
                },
            ),
        )
    }
    if (onBackClick != null) {
        OutlinedButton(onBackClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(AppResource.String.number_trail_back))
        }
    }
}

private fun previewState(
    phase: NumberTrailPhase = NumberTrailPhase.Ready,
    size: Int = 2,
    target: Int = 1,
    failure: NumberTrailFailure? = null,
    save: NumberTrailSaveStatus = NumberTrailSaveStatus.None,
    newRecord: Boolean = false,
): NumberTrailUiState =
    NumberTrailUiState(
        game =
            NumberTrailState(
                phase = phase,
                board =
                    NumberTrailBoard(
                        1,
                        size,
                        (1..size * size).reversed().toList(),
                        when (size) {
                            2 -> 12_000
                            3 -> 20_000
                            else -> 30_000
                        },
                        when {
                            failure == NumberTrailFailure.Timeout -> 0
                            size == 2 -> 7_400
                            size == 3 -> 12_400
                            else -> 17_400
                        },
                        target,
                    ),
                score = target - 1,
                failure = failure,
                selectedNumber = if (failure == NumberTrailFailure.Wrong) 12 else null,
            ),
        record = if (newRecord) target - 1 else 7,
        isRecordLoading = false,
        isNewRecord = newRecord,
        saveStatus = save,
    )

@Preview(name = "Number Trail ReadyHub", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailReadyHubPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail LoadingStandalone", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailLoadingStandalonePreview() {
    OquTurboTheme { NumberTrailScreen(NumberTrailUiState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Number Trail Readyru", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailReadyruPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Readykk", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailReadykkPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Active2", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailActive2Preview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Active, 2, 1), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Active3", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailActive3Preview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Active, 3, 4), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Active4en", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailActive4enPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Active, 4, 10), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Active4ru", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailActive4ruPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Active, 4, 10), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Active4kk", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailActive4kkPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Active, 4, 10), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail BoardComplete", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailBoardCompletePreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.BoardComplete, 3, 10), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Pauseden", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailPausedenPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Paused, 4, 10), {}, { _, _ -> }, null) }
}

@Preview(name = "Number Trail Pausedru", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailPausedruPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Paused, 4, 10), {}, { _, _ -> }, null) }
}

@Preview(name = "Number Trail Pausedkk", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailPausedkkPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Paused, 4, 10), {}, { _, _ -> }, null) }
}

@Preview(name = "Number Trail PausedHub", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailPausedHubPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Paused), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail Wrong", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailWrongPreview() {
    OquTurboTheme {
        NumberTrailScreen(
            previewState(
                NumberTrailPhase.Result,
                4,
                10,
                NumberTrailFailure.Wrong,
                NumberTrailSaveStatus.Saved,
            ),
            {
            },
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Number Trail Timeout", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun NumberTrailTimeoutPreview() {
    OquTurboTheme {
        NumberTrailScreen(
            previewState(
                NumberTrailPhase.Result,
                failure = NumberTrailFailure.Timeout,
                save = NumberTrailSaveStatus.Saved,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Number Trail NewRecord", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailNewRecordPreview() {
    OquTurboTheme {
        NumberTrailScreen(
            previewState(
                NumberTrailPhase.Result,
                4,
                10,
                NumberTrailFailure.Wrong,
                NumberTrailSaveStatus.Saved,
                true,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Number Trail SavePending", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailSavePendingPreview() {
    OquTurboTheme {
        NumberTrailScreen(
            previewState(
                NumberTrailPhase.Result,
                4,
                10,
                NumberTrailFailure.Wrong,
                NumberTrailSaveStatus.Pending,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Number Trail SaveFailed", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun NumberTrailSaveFailedPreview() {
    OquTurboTheme {
        NumberTrailScreen(
            previewState(
                NumberTrailPhase.Result,
                4,
                10,
                NumberTrailFailure.Wrong,
                NumberTrailSaveStatus.Failed,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Number Trail large font short", widthDp = 320, heightDp = 600, fontScale = 1.5f, locale = "ru")
@ScreenshotPreview
@Composable
private fun NumberTrailLargeFontPreview() {
    OquTurboTheme { NumberTrailScreen(previewState(NumberTrailPhase.Active, 4, 10), {}, { _, _ -> }, {}) }
}

@Preview(name = "Number Trail large scores", widthDp = 320, heightDp = 844, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun NumberTrailLargeScoresPreview() {
    val state = previewState(NumberTrailPhase.Active, 4, 10)
    OquTurboTheme {
        NumberTrailScreen(state.copy(game = state.game.copy(score = 12345), record = 23456), {}, { _, _ -> }, {})
    }
}
