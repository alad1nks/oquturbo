package com.alad1nks.oquturbo.feature.symbolcount.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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
import com.alad1nks.oquturbo.feature.symbolcount.model.CountShape
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountBoard
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountFailure
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountGame
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountPhase
import com.alad1nks.oquturbo.feature.symbolcount.model.SymbolCountState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil

@Composable
internal fun SymbolCountRoute(viewModel: SymbolCountViewModel, onBackClick: (() -> Unit)?) {
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
    SymbolCountScreen(
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
        onRetrySaveClick = viewModel::retrySave,
    )
}

@Composable
internal fun SymbolCountScreen(
    state: SymbolCountUiState,
    onStartClick: () -> Unit,
    onAnswerClick: (Long, Int) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onReloadClick: () -> Unit = {},
    onRetrySaveClick: () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().appBackground().statusBarsPadding().navigationBarsPadding()) {
        val fieldSide = (maxHeight - 390.dp).coerceIn(180.dp, 320.dp).coerceAtMost(maxWidth - 32.dp)
        val horizontal = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                .padding(horizontal = horizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SymbolCountHeader(state, onBackClick)
            when (state.game.phase) {
                SymbolCountPhase.Ready -> ReadyContent(state, onStartClick, onReloadClick)
                SymbolCountPhase.Active, SymbolCountPhase.Correct ->
                    PlayingContent(
                        state,
                        onAnswerClick,
                        onPauseClick,
                        fieldSide,
                    )
                SymbolCountPhase.Paused -> PausedContent(onResumeClick)
                SymbolCountPhase.Result ->
                    ResultContent(
                        state,
                        onStartClick,
                        onBackClick,
                        onReloadClick,
                        onRetrySaveClick,
                    )
            }
            Box(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun SymbolCountHeader(state: SymbolCountUiState, onBackClick: (() -> Unit)?) {
    val stacked = LocalDensity.current.fontScale > 1.2f || state.game.score >= 1000 || state.record >= 1000
    val best =
        if (state.isRecordLoading || state.recordLoadFailed) {
            stringResource(AppResource.String.symbol_count_placeholder)
        } else {
            state.record.toString()
        }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            AppBackButton(
                onBackClick,
                contentDescription = stringResource(AppResource.String.symbol_count_back),
            )
        }
        if (stacked) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameScoreBadge(
                    stringResource(AppResource.String.symbol_count_score),
                    state.game.score.toString(),
                    Modifier.fillMaxWidth(),
                )
                GameScoreBadge(stringResource(AppResource.String.symbol_count_record), best, Modifier.fillMaxWidth())
            }
        } else {
            GameScoreBadge(
                stringResource(AppResource.String.symbol_count_score),
                state.game.score.toString(),
                Modifier.weight(1f),
            )
            GameScoreBadge(stringResource(AppResource.String.symbol_count_record), best, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadyContent(
    state: SymbolCountUiState,
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
                stringResource(AppResource.String.symbol_count_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(AppResource.String.symbol_count_ready_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.symbol_count_instructions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CountShape.entries.forEach { SymbolGlyph(it, Modifier.size(32.dp)) }
            }
            Text(
                if (state.recordLoadFailed) {
                    stringResource(AppResource.String.symbol_count_record_unavailable)
                } else if (state.isRecordLoading) {
                    stringResource(AppResource.String.symbol_count_loading_record)
                } else {
                    stringResource(AppResource.String.symbol_count_record_value, state.record)
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
                            AppResource.String.symbol_count_retry_load
                        } else {
                            AppResource.String.symbol_count_start
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
                stringResource(AppResource.String.symbol_count_paused_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(AppResource.String.symbol_count_paused_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onResumeClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(AppResource.String.symbol_count_resume))
            }
        }
    }
}

@Composable
private fun PlayingContent(
    state: SymbolCountUiState,
    onAnswerClick: (Long, Int) -> Unit,
    onPauseClick: () -> Unit,
    fieldSide: Dp,
) {
    val board = state.game.board ?: return
    val active = state.game.phase == SymbolCountPhase.Active
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    AppResource.String.symbol_count_time_remaining,
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
            contentDescription = stringResource(AppResource.String.symbol_count_pause),
        )
    }
    TargetPrompt(board.target)
    SymbolField(board, fieldSide)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        board.options.chunked(2).forEach { options ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { value ->
                    val description = stringResource(AppResource.String.symbol_count_answer_description, value)
                    Button(
                        onClick = { onAnswerClick(board.id, value) },
                        enabled = active,
                        modifier =
                            Modifier.weight(1f).heightIn(min = 56.dp).testTag("answer-$value")
                                .semantics { contentDescription = description },
                    ) {
                        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
    Row(
        Modifier.heightIn(min = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!active) {
            Icon(Icons.Default.Check, null, Modifier.size(20.dp))
            Text(stringResource(AppResource.String.symbol_count_correct))
        }
    }
}

@Composable
private fun shapeName(shape: CountShape): String =
    stringResource(
        when (shape) {
            CountShape.Circle -> AppResource.String.symbol_count_circle
            CountShape.Square -> AppResource.String.symbol_count_square
            CountShape.Triangle -> AppResource.String.symbol_count_triangle
            CountShape.Diamond -> AppResource.String.symbol_count_diamond
        },
    )

@Composable
private fun TargetPrompt(shape: CountShape) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SymbolGlyph(shape, Modifier.size(32.dp))
        Text(
            stringResource(AppResource.String.symbol_count_target, shapeName(shape)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("target"),
        )
    }
}

@Composable
private fun SymbolField(board: SymbolCountBoard, side: Dp) {
    val description = stringResource(AppResource.String.symbol_count_field_description, board.size)
    Surface(
        Modifier.size(side).testTag("field").semantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxSize()) {
            repeat(board.size) { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(board.size) { column ->
                        val shape = board.shapes[row * board.size + column]
                        val label =
                            stringResource(
                                AppResource.String.symbol_count_cell_description,
                                row + 1,
                                column + 1,
                                shapeName(shape),
                            )
                        SymbolGlyph(shape, Modifier.weight(1f).fillMaxSize().semantics { contentDescription = label })
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolGlyph(shape: CountShape, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val edge = size.minDimension * 0.64f
        val left = (size.width - edge) / 2
        val top = (size.height - edge) / 2
        when (shape) {
            CountShape.Circle -> drawCircle(color, edge / 2)
            CountShape.Square -> drawRect(color, Offset(left, top), Size(edge, edge))
            CountShape.Triangle, CountShape.Diamond -> {
                val path =
                    Path().apply {
                        moveTo(size.width / 2, top)
                        if (shape == CountShape.Triangle) {
                            lineTo(left + edge, top + edge)
                            lineTo(left, top + edge)
                        } else {
                            lineTo(left + edge, size.height / 2)
                            lineTo(size.width / 2, top + edge)
                            lineTo(left, size.height / 2)
                        }
                        close()
                    }
                drawPath(path, color)
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: SymbolCountUiState,
    onRetry: () -> Unit,
    onBackClick: (() -> Unit)?,
    onReloadClick: () -> Unit,
    onRetrySaveClick: () -> Unit,
) {
    val game = state.game
    val board = game.board ?: return
    val target = board.actualCount
    val timeout = game.failure == SymbolCountFailure.Timeout
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
                    AppResource.String.symbol_count_timeout_title
                } else {
                    AppResource.String.symbol_count_wrong_title
                },
            ),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        TargetPrompt(board.target)
        Text(
            if (timeout) {
                stringResource(AppResource.String.symbol_count_timeout_detail, target)
            } else {
                stringResource(
                    AppResource.String.symbol_count_wrong_detail,
                    requireNotNull(game.selectedNumber),
                    target,
                )
            },
            textAlign = TextAlign.Center,
        )
        GameResultCard(
            primaryText = stringResource(AppResource.String.symbol_count_score) + ": " + game.score,
            secondaryText = stringResource(AppResource.String.symbol_count_record_value, state.record),
            modifier = Modifier.fillMaxWidth(),
        )
        when (state.saveStatus) {
            SymbolCountSaveStatus.Pending ->
                Text(
                    stringResource(AppResource.String.symbol_count_saving_result),
                    textAlign = TextAlign.Center,
                )
            SymbolCountSaveStatus.Failed ->
                Text(
                    stringResource(AppResource.String.symbol_count_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            else ->
                if (state.isNewRecord) {
                    Text(
                        stringResource(AppResource.String.symbol_count_new_record),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
        }
        if (state.recordLoadFailed) {
            Text(
                stringResource(AppResource.String.symbol_count_record_unavailable),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
    Button(
        if (state.saveStatus == SymbolCountSaveStatus.Failed) {
            onRetrySaveClick
        } else if (state.recordLoadFailed) {
            onReloadClick
        } else {
            onRetry
        },
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = !state.isRecordLoading && state.saveStatus != SymbolCountSaveStatus.Pending,
    ) {
        Icon(Icons.Default.Replay, null)
        Text(
            stringResource(
                if (state.saveStatus == SymbolCountSaveStatus.Failed) {
                    AppResource.String.symbol_count_retry_save
                } else if (state.recordLoadFailed) {
                    AppResource.String.symbol_count_retry_load
                } else {
                    AppResource.String.symbol_count_retry
                },
            ),
        )
    }
    if (onBackClick != null) {
        OutlinedButton(onBackClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(AppResource.String.symbol_count_back))
        }
    }
}

internal fun symbolCountPreviewState(
    phase: SymbolCountPhase = SymbolCountPhase.Ready,
    size: Int = 3,
    failure: SymbolCountFailure? = null,
    save: SymbolCountSaveStatus = SymbolCountSaveStatus.None,
): SymbolCountUiState {
    val count = if (size == 3) 3 else 5
    val shapes =
        List(size * size) { index ->
            if (index < count) CountShape.Circle else CountShape.entries[1 + index % 3]
        }
    return SymbolCountUiState(
        game =
            SymbolCountState(
                phase = phase,
                board =
                    SymbolCountBoard(
                        1,
                        size,
                        shapes,
                        CountShape.Circle,
                        if (size == 3) listOf(1, 3, 4, 2) else listOf(2, 5, 6, 1),
                        SymbolCountGame.timeFor(
                            if (size == 3) {
                                0
                            } else if (size == 4) {
                                5
                            } else {
                                10
                            },
                        ),
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
                selectedNumber = if (failure == SymbolCountFailure.Wrong) 2 else null,
            ),
        record = 7,
        isRecordLoading = false,
        saveStatus = save,
    )
}

@Preview(name = "Symbol Count ReadyHub", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountReadyHubPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count ReadyStandalone", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountReadyStandalonePreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Symbol Count ReadyRu", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun SymbolCountReadyRuPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count ReadyKk", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun SymbolCountReadyKkPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Loading", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountLoadingPreview() {
    OquTurboTheme { SymbolCountScreen(SymbolCountUiState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Symbol Count LoadError", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun SymbolCountLoadErrorPreview() {
    OquTurboTheme {
        SymbolCountScreen(SymbolCountUiState(isRecordLoading = false, recordLoadFailed = true), {}, { _, _ -> }, null)
    }
}

@Preview(name = "Symbol Count Active3", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountActive3Preview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 3), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Active4", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountActive4Preview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 4), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Active5", widthDp = 390, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountActive5Preview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Compacten", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountCompactenPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Compactru", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun SymbolCountCompactruPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Compactkk", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun SymbolCountCompactkkPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 5), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count Correct", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountCorrectPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Correct, 4), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count PausedRu", widthDp = 320, heightDp = 640, locale = "ru")
@ScreenshotPreview
@Composable
private fun SymbolCountPausedRuPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Paused), {}, { _, _ -> }, {}) }
}

@Preview(name = "Symbol Count PausedKk", widthDp = 320, heightDp = 640, locale = "kk")
@ScreenshotPreview
@Composable
private fun SymbolCountPausedKkPreview() {
    OquTurboTheme { SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Paused), {}, { _, _ -> }, null) }
}

@Preview(name = "Symbol Count Wrong", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun SymbolCountWrongPreview() {
    OquTurboTheme {
        SymbolCountScreen(
            symbolCountPreviewState(
                SymbolCountPhase.Result,
                4,
                SymbolCountFailure.Wrong,
                SymbolCountSaveStatus.Saved,
            ),
            {
            },
            { _, _ -> },
            {},
        )
    }
}

@Preview(name = "Symbol Count Timeout", widthDp = 320, heightDp = 844, locale = "kk")
@ScreenshotPreview
@Composable
private fun SymbolCountTimeoutPreview() {
    OquTurboTheme {
        SymbolCountScreen(
            symbolCountPreviewState(
                SymbolCountPhase.Result,
                3,
                SymbolCountFailure.Timeout,
                SymbolCountSaveStatus.Saved,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Symbol Count Pending", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountPendingPreview() {
    OquTurboTheme {
        SymbolCountScreen(
            symbolCountPreviewState(
                SymbolCountPhase.Result,
                4,
                SymbolCountFailure.Wrong,
                SymbolCountSaveStatus.Pending,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Symbol Count SaveError", widthDp = 320, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun SymbolCountSaveErrorPreview() {
    OquTurboTheme {
        SymbolCountScreen(
            symbolCountPreviewState(
                SymbolCountPhase.Result,
                4,
                SymbolCountFailure.Wrong,
                SymbolCountSaveStatus.Failed,
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Symbol Count NewRecord", widthDp = 320, heightDp = 844, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountNewRecordPreview() {
    OquTurboTheme {
        SymbolCountScreen(
            symbolCountPreviewState(
                SymbolCountPhase.Result,
                5,
                SymbolCountFailure.Wrong,
                SymbolCountSaveStatus.Saved,
            ).copy(record = 10, isNewRecord = true),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Symbol Count dark", widthDp = 320, heightDp = 640, locale = "en")
@ScreenshotPreview
@Composable
private fun SymbolCountDarkPreview() {
    OquTurboTheme(darkTheme = true) {
        SymbolCountScreen(symbolCountPreviewState(SymbolCountPhase.Active, 5), {}, { _, _ -> }, {})
    }
}

@Preview(name = "Symbol Count large text", widthDp = 320, heightDp = 640, locale = "ru", fontScale = 1.5f)
@ScreenshotPreview
@Composable
private fun SymbolCountLargeTextPreview() {
    val state = symbolCountPreviewState(SymbolCountPhase.Active, 5)
    OquTurboTheme {
        SymbolCountScreen(state.copy(game = state.game.copy(score = 12345), record = 23456), {}, { _, _ -> }, {})
    }
}

@Preview(name = "Symbol Count large result", widthDp = 320, heightDp = 844, locale = "kk", fontScale = 2f)
@ScreenshotPreview
@Composable
private fun SymbolCountLargeResultPreview() {
    OquTurboTheme {
        SymbolCountScreen(
            symbolCountPreviewState(
                SymbolCountPhase.Result,
                5,
                SymbolCountFailure.Wrong,
                SymbolCountSaveStatus.Saved,
            ).copy(record = 10),
            {},
            { _, _ -> },
            {},
        )
    }
}
