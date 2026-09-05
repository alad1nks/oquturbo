@file:Suppress("ktlint:standard:max-line-length")

package com.alad1nks.oquturbo.feature.memorygrid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameHeader
import com.alad1nks.oquturbo.core.ui.component.GameScoreBadge
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridPhase
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MemoryGridRoute(
    viewModel: MemoryGridViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    MemoryGridScreen(state, viewModel.mode, viewModel::start, viewModel::selectCell, onBackClick)
}

@Composable
internal fun MemoryGridScreen(
    state: MemoryGridState,
    mode: MemoryGridGameMode,
    onStartClick: () -> Unit,
    onCellClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreLabel = stringResource(AppResource.String.memory_grid_score)
    val backDescription = stringResource(AppResource.String.memory_grid_back)
    val gameOver = state.phase == MemoryGridPhase.GameOver
    val contentModifier =
        if (gameOver) {
            Modifier.verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
    Box(modifier.fillMaxSize().appBackground()) {
        Column(
            modifier =
                contentModifier.align(Alignment.Center).widthIn(max = 560.dp).fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = if (gameOver) 88.dp else 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (gameOver) 20.dp else 24.dp),
        ) {
            Text(
                text = phaseHint(state.phase),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.phase == MemoryGridPhase.AwaitingInput) {
                val acceptedTapsDescription =
                    stringResource(
                        AppResource.String.memory_grid_accepted_taps_count,
                        state.input.size,
                    )
                GameScoreBadge(
                    label = stringResource(AppResource.String.memory_grid_accepted_taps),
                    value = state.input.size.toString(),
                    modifier =
                        Modifier.clearAndSetSemantics {
                            contentDescription = acceptedTapsDescription
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            }
            MemoryGridBoard(state, mode, onCellClick)
            if (state.phase == MemoryGridPhase.GameOver) {
                MistakeLegend(mode)
            }
            if (state.phase == MemoryGridPhase.Ready || state.phase == MemoryGridPhase.GameOver) {
                ResultPanel(state, mode, onStartClick)
            }
        }
        GameHeader(
            scoreLabel = scoreLabel,
            score = state.score.toString(),
            recordLabel =
                stringResource(
                    if (mode == MemoryGridGameMode.Flash) {
                        AppResource.String.memory_grid_rounds
                    } else {
                        AppResource.String.memory_grid_length
                    },
                ),
            record = state.record.toString(),
            leadingContent = {
                AppBackButton(
                    onClick = onBackClick,
                    contentDescription = backDescription,
                )
            },
            modifier =
                Modifier.align(Alignment.TopCenter).widthIn(max = 760.dp).fillMaxWidth()
                    .statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun MemoryGridBoard(state: MemoryGridState, mode: MemoryGridGameMode, onCellClick: (Int) -> Unit) {
    val highlightedCells = state.highlightedCells(mode)
    Column(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(state.gridSize) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.gridSize) { column ->
                    val index = row * state.gridSize + column
                    val active = index in highlightedCells
                    val accepted = state.phase == MemoryGridPhase.GameOver && index in state.input
                    val wrong = state.phase == MemoryGridPhase.GameOver && state.failedSelectedCell == index
                    val expected = state.phase == MemoryGridPhase.GameOver && index in state.expectedCellsAfterMistake
                    val enabled = state.phase == MemoryGridPhase.AwaitingInput
                    val positionDescription =
                        stringResource(AppResource.String.memory_grid_cell_description, row + 1, column + 1)
                    val roleDescriptions =
                        buildList {
                            if (accepted) add(stringResource(AppResource.String.memory_grid_cell_accepted))
                            if (wrong) add(stringResource(AppResource.String.memory_grid_cell_wrong))
                            if (expected) {
                                add(
                                    stringResource(
                                        if (mode == MemoryGridGameMode.Flash) {
                                            AppResource.String.memory_grid_cell_remaining
                                        } else {
                                            AppResource.String.memory_grid_cell_expected
                                        },
                                    ),
                                )
                            }
                        }
                    val description =
                        if (roleDescriptions.isEmpty()) {
                            positionDescription
                        } else {
                            stringResource(
                                AppResource.String.memory_grid_cell_feedback_description,
                                positionDescription,
                                roleDescriptions.joinToString(),
                            )
                        }
                    Surface(
                        modifier =
                            Modifier.weight(1f).fillMaxSize().sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .scale(if (active) 0.9f else 1f)
                                .semantics {
                                    contentDescription = description
                                    if (!enabled) disabled()
                                }
                                .clickable(enabled = enabled) { onCellClick(index) },
                        shape = RoundedCornerShape(14.dp),
                        color =
                            when {
                                wrong -> MaterialTheme.colorScheme.errorContainer
                                expected -> MaterialTheme.colorScheme.tertiaryContainer
                                active -> MaterialTheme.colorScheme.primary
                                accepted -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        border =
                            BorderStroke(
                                if (active || wrong || expected) 3.dp else 1.dp,
                                when {
                                    wrong -> MaterialTheme.colorScheme.error
                                    expected -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    ) {
                        Box {
                            if (accepted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                )
                            }
                            if (wrong) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            } else if (expected) {
                                Box(
                                    modifier =
                                        Modifier.align(Alignment.Center).sizeIn(minWidth = 28.dp, minHeight = 28.dp)
                                            .border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeLegend(mode: MemoryGridGameMode) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(AppResource.String.memory_grid_legend_wrong, LegendRole.Wrong)
        LegendItem(
            if (mode == MemoryGridGameMode.Flash) {
                AppResource.String.memory_grid_legend_remaining
            } else {
                AppResource.String.memory_grid_legend_expected
            },
            LegendRole.Expected,
        )
        LegendItem(AppResource.String.memory_grid_legend_accepted, LegendRole.Accepted)
    }
}

@Composable
private fun LegendItem(labelResource: org.jetbrains.compose.resources.StringResource, role: LegendRole) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp),
            shape = RoundedCornerShape(6.dp),
            color =
                when (role) {
                    LegendRole.Accepted -> MaterialTheme.colorScheme.primaryContainer
                    LegendRole.Wrong -> MaterialTheme.colorScheme.errorContainer
                    LegendRole.Expected -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            border =
                BorderStroke(
                    if (role == LegendRole.Expected) 3.dp else 1.dp,
                    when (role) {
                        LegendRole.Accepted -> MaterialTheme.colorScheme.primary
                        LegendRole.Wrong -> MaterialTheme.colorScheme.error
                        LegendRole.Expected -> MaterialTheme.colorScheme.tertiary
                    },
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (role) {
                    LegendRole.Accepted ->
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    LegendRole.Wrong ->
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    LegendRole.Expected ->
                        Box(
                            Modifier.sizeIn(minWidth = 12.dp, minHeight = 12.dp)
                                .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        )
                }
            }
        }
        Text(stringResource(labelResource), style = MaterialTheme.typography.labelMedium)
    }
}

private enum class LegendRole { Accepted, Wrong, Expected }

@Composable
private fun ResultPanel(state: MemoryGridState, mode: MemoryGridGameMode, onStartClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text =
                    if (state.phase == MemoryGridPhase.Ready) {
                        stringResource(AppResource.String.memory_grid_ready_title)
                    } else if (mode == MemoryGridGameMode.Flash) {
                        stringResource(AppResource.String.memory_grid_flash_result, state.score)
                    } else {
                        stringResource(AppResource.String.memory_grid_result, state.score)
                    },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (state.phase == MemoryGridPhase.GameOver) {
                Text(
                    text = stringResource(AppResource.String.memory_grid_session_correct_taps, state.correctCellCount),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                stringResource(
                    if (state.phase == MemoryGridPhase.Ready) {
                        mode.ruleResource
                    } else if (mode == MemoryGridGameMode.Flash) {
                        AppResource.String.memory_grid_game_over_flash
                    } else {
                        AppResource.String.memory_grid_game_over
                    },
                ),
            )
            if (state.isNewRecord) {
                Text(
                    text = stringResource(AppResource.String.memory_grid_new_record),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Button(onClick = onStartClick) {
                Icon(if (state.phase == MemoryGridPhase.Ready) Icons.Default.PlayArrow else Icons.Default.Replay, null)
                Text(
                    stringResource(
                        if (state.phase == MemoryGridPhase.Ready) AppResource.String.memory_grid_start else AppResource.String.memory_grid_retry,
                    ),
                )
            }
        }
    }
}

@Composable
private fun phaseHint(phase: MemoryGridPhase): String =
    stringResource(
        when (phase) {
            MemoryGridPhase.Ready -> AppResource.String.memory_grid_hint_ready
            MemoryGridPhase.ShowingSequence -> AppResource.String.memory_grid_hint_watch
            MemoryGridPhase.AwaitingInput -> AppResource.String.memory_grid_hint_repeat
            MemoryGridPhase.RoundSuccess -> AppResource.String.memory_grid_hint_success
            MemoryGridPhase.GameOver -> AppResource.String.memory_grid_hint_mistake
        },
    )

@Preview(name = "Memory Grid — session total Russian Reverse", widthDp = 390, heightDp = 1000, locale = "ru")
@ScreenshotPreview
@Composable
private fun MemoryGridSessionTotalReversePreview() {
    OquTurboTheme {
        MemoryGridScreen(
            state =
                MemoryGridState(
                    phase = MemoryGridPhase.GameOver,
                    sequence = listOf(0, 1, 2, 3),
                    input = listOf(3, 2),
                    score = 3,
                    correctCellCount = 5,
                    failedSelectedCell = 8,
                    expectedCellsAfterMistake = setOf(1),
                ),
            mode = MemoryGridGameMode.Reverse,
            onStartClick = {},
            onCellClick = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Memory Grid — session total compact Kazakh Flash", widthDp = 320, heightDp = 1100, locale = "kk")
@ScreenshotPreview
@Composable
private fun MemoryGridSessionTotalFlashPreview() {
    OquTurboTheme {
        MemoryGridScreen(
            state =
                MemoryGridState(
                    phase = MemoryGridPhase.GameOver,
                    gridSize = 4,
                    sequence = listOf(0, 1, 2, 3),
                    input = listOf(0, 1),
                    score = 1,
                    correctCellCount = 5,
                    failedSelectedCell = 1,
                    expectedCellsAfterMistake = setOf(2, 3),
                ),
            mode = MemoryGridGameMode.Flash,
            onStartClick = {},
            onCellClick = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Memory Grid — large session total compact", widthDp = 320, heightDp = 1000)
@ScreenshotPreview
@Composable
private fun MemoryGridLargeSessionTotalPreview() {
    OquTurboTheme {
        MemoryGridScreen(
            state = MemoryGridState(phase = MemoryGridPhase.GameOver, correctCellCount = Int.MAX_VALUE),
            mode = MemoryGridGameMode.Route,
            onStartClick = {},
            onCellClick = {},
            onBackClick = {},
        )
    }
}

private val MemoryGridGameMode.ruleResource
    get() =
        when (this) {
            MemoryGridGameMode.Route -> AppResource.String.memory_grid_rule
            MemoryGridGameMode.Reverse -> AppResource.String.memory_grid_reverse_rule
            MemoryGridGameMode.Flash -> AppResource.String.memory_grid_flash_rule
        }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MemoryGridPreview() {
    OquTurboTheme { MemoryGridScreen(MemoryGridState(), MemoryGridGameMode.Route, {}, {}, {}) }
}

@Preview(name = "Memory Grid — accepted taps", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun MemoryGridAcceptedTapsPreview() {
    OquTurboTheme {
        MemoryGridScreen(
            state =
                MemoryGridState(
                    phase = MemoryGridPhase.AwaitingInput,
                    gridSize = 4,
                    sequence = listOf(0, 1, 0, 2, 3, 4, 5),
                    input = listOf(0, 1, 0),
                    score = 6,
                    record = 8,
                ),
            mode = MemoryGridGameMode.Route,
            onStartClick = {},
            onCellClick = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Memory Grid — layered mistake", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun MemoryGridLayeredMistakePreview() {
    OquTurboTheme {
        MemoryGridScreen(
            state =
                MemoryGridState(
                    phase = MemoryGridPhase.GameOver,
                    gridSize = 3,
                    sequence = listOf(0, 1, 2, 4),
                    input = listOf(0, 1),
                    score = 3,
                    correctCellCount = 5,
                    failedSelectedCell = 1,
                    expectedCellsAfterMistake = setOf(2),
                    record = 5,
                ),
            mode = MemoryGridGameMode.Route,
            onStartClick = {},
            onCellClick = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Memory Grid — new record", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun MemoryGridNewRecordPreview() {
    OquTurboTheme {
        MemoryGridScreen(
            state =
                MemoryGridState(
                    phase = MemoryGridPhase.GameOver,
                    gridSize = 3,
                    sequence = listOf(0, 1, 2, 4),
                    input = listOf(0, 1),
                    score = 3,
                    correctCellCount = 5,
                    failedSelectedCell = 1,
                    expectedCellsAfterMistake = setOf(2),
                    record = 3,
                    isNewRecord = true,
                ),
            mode = MemoryGridGameMode.Route,
            onStartClick = {},
            onCellClick = {},
            onBackClick = {},
        )
    }
}
