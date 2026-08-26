@file:Suppress("ktlint:standard:max-line-length")

package com.alad1nks.oquturbo.feature.memorygrid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameHeader
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
    Box(modifier.fillMaxSize().appBackground()) {
        Column(
            modifier =
                Modifier.align(Alignment.Center).widthIn(max = 560.dp).fillMaxWidth()
                    .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = phaseHint(state.phase),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MemoryGridBoard(state, mode, onCellClick)
            if (state.phase == MemoryGridPhase.Ready || state.phase == MemoryGridPhase.GameOver) {
                ResultPanel(state, mode, onStartClick)
            }
        }
        GameHeader(
            scoreLabel = scoreLabel,
            score = state.score.toString(),
            recordLabel = stringResource(AppResource.String.memory_grid_length),
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
                    val error = state.phase == MemoryGridPhase.GameOver && state.expectedCellAfterMistake == index
                    val enabled = state.phase == MemoryGridPhase.AwaitingInput
                    val description =
                        stringResource(AppResource.String.memory_grid_cell_description, row + 1, column + 1)
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
                                error -> MaterialTheme.colorScheme.errorContainer
                                active -> MaterialTheme.colorScheme.primary
                                index in state.input -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        border =
                            BorderStroke(
                                if (active || error) 3.dp else 1.dp,
                                if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    ) {
                        if (error) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
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
                    } else {
                        stringResource(AppResource.String.memory_grid_result, state.score)
                    },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (state.phase == MemoryGridPhase.Ready) mode.ruleResource else AppResource.String.memory_grid_game_over,
                ),
            )
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

private val MemoryGridGameMode.ruleResource
    get() =
        when (this) {
            MemoryGridGameMode.Route -> AppResource.String.memory_grid_rule
            MemoryGridGameMode.Reverse -> AppResource.String.memory_grid_reverse_rule
            MemoryGridGameMode.Flash -> AppResource.String.memory_grid_flash_rule
        }

@Preview(widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun MemoryGridPreview() {
    OquTurboTheme { MemoryGridScreen(MemoryGridState(), MemoryGridGameMode.Route, {}, {}, {}) }
}
