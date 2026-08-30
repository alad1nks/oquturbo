@file:Suppress("ktlint:standard:max-line-length")

package com.alad1nks.oquturbo.feature.dualfocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusCard
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusFailure
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusGame
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusLane
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusPhase
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusResult
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusShape
import com.alad1nks.oquturbo.feature.dualfocus.model.DualFocusState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

internal fun dualFocusBackAction(onBackClick: (() -> Unit)?, onAbandon: () -> Unit): (() -> Unit)? =
    onBackClick?.let { callback ->
        {
            onAbandon()
            callback()
        }
    }

@Composable
internal fun DualFocusRoute(viewModel: DualFocusViewModel, onBackClick: (() -> Unit)?) {
    val state by viewModel.uiState.collectAsState()
    DualFocusScreen(state, viewModel::start, viewModel::tap, dualFocusBackAction(onBackClick, viewModel::abandon))
}

@Composable
internal fun DualFocusScreen(
    state: DualFocusUiState,
    onStartClick: () -> Unit,
    onCardClick: (DualFocusLane, Long) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().appBackground()) {
        Column(
            Modifier.align(
                Alignment.TopCenter,
            ).widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(start = 16.dp, top = 104.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (state.game.phase) {
                DualFocusPhase.Ready -> ReadyContent(state, onStartClick)
                DualFocusPhase.Active -> ActiveContent(state, onCardClick)
                DualFocusPhase.Result -> ResultContent(state, onStartClick, onBackClick)
            }
        }
        GameHeader(
            scoreLabel = stringResource(AppResource.String.dual_focus_score),
            score = state.game.score.toString(),
            recordLabel = stringResource(AppResource.String.dual_focus_record),
            record =
                if (state.isRecordLoading) {
                    stringResource(
                        AppResource.String.dual_focus_loading_record_placeholder,
                    )
                } else {
                    state.record.toString()
                },
            leadingContent =
                onBackClick?.let {
                    callback ->
                    {
                        AppBackButton(
                            onClick = callback,
                            contentDescription = stringResource(AppResource.String.dual_focus_back),
                        )
                    }
                },
            modifier =
                Modifier.align(
                    Alignment.TopCenter,
                ).widthIn(
                    max = 760.dp,
                ).fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun ReadyContent(state: DualFocusUiState, onStartClick: () -> Unit) {
    Text(
        stringResource(AppResource.String.dual_focus_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(AppResource.String.dual_focus_instructions),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TargetBadge(
            DualFocusLane.One,
            state.game.targets[DualFocusLane.One] ?: DualFocusShape.Circle,
            Modifier.weight(1f),
        )
        TargetBadge(
            DualFocusLane.Two,
            state.game.targets[DualFocusLane.Two] ?: DualFocusShape.Triangle,
            Modifier.weight(1f),
        )
    }
    Text(
        if (state.isRecordLoading) {
            stringResource(
                AppResource.String.dual_focus_loading_record,
            )
        } else {
            stringResource(AppResource.String.dual_focus_record_value, state.record)
        },
    )
    Button(onStartClick, enabled = !state.isRecordLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(Icons.Default.PlayArrow, null)
        Text(stringResource(AppResource.String.dual_focus_start))
    }
}

@Composable
private fun ActiveContent(state: DualFocusUiState, onCardClick: (DualFocusLane, Long) -> Unit) {
    val game = state.game
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DualFocusLane.entries.forEach { lane ->
            LanePanel(
                lane,
                game.targets.getValue(lane),
                game.cards[lane],
                game.nowMillis,
                state.correctFeedbackLane == lane,
                onCardClick,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LanePanel(
    lane: DualFocusLane,
    target: DualFocusShape,
    card: DualFocusCard?,
    nowMillis: Long,
    showCorrectFeedback: Boolean,
    onCardClick: (DualFocusLane, Long) -> Unit,
    modifier: Modifier,
) {
    val accent = if (lane == DualFocusLane.One) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, accent),
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TargetBadge(lane, target, Modifier.fillMaxWidth())
            if (card == null) {
                Box(Modifier.fillMaxWidth().aspectRatio(0.82f), contentAlignment = Alignment.Center) {
                    if (showCorrectFeedback) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text(stringResource(AppResource.String.dual_focus_correct), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            stringResource(AppResource.String.dual_focus_waiting),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                val description =
                    stringResource(
                        AppResource.String.dual_focus_card_accessibility,
                        laneLabel(lane),
                        shapeLabel(card.shownShape),
                        shapeLabel(target),
                        stringResource(
                            if (card.isTarget) AppResource.String.dual_focus_match else AppResource.String.dual_focus_non_match,
                        ),
                    )
                Surface(
                    Modifier.fillMaxWidth().aspectRatio(
                        0.82f,
                    ).heightIn(min = 48.dp).semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = description
                    }
                        .clickable { onCardClick(lane, card.id) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) { ShapeGlyph(card.shownShape, accent, Modifier.size(76.dp)) }
                }
                LinearProgressIndicator(
                    progress = {
                        ((card.expiresAtMillis - nowMillis).toFloat() / (card.expiresAtMillis - card.appearedAtMillis)).coerceIn(
                            0f,
                            1f,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun TargetBadge(lane: DualFocusLane, shape: DualFocusShape, modifier: Modifier) {
    val accent = if (lane == DualFocusLane.One) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(modifier, shape = MaterialTheme.shapes.large, color = accent.copy(alpha = 0.12f)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(AppResource.String.dual_focus_lane_target, laneLabel(lane)),
                style = MaterialTheme.typography.labelMedium,
            )
            ShapeGlyph(shape, accent, Modifier.size(44.dp))
            Text(shapeLabel(shape), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultContent(state: DualFocusUiState, onReplayClick: () -> Unit, onBackClick: (() -> Unit)?) {
    val result = state.game.result ?: return
    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
    Text(
        stringResource(
            if (result.failure == DualFocusFailure.WrongTap) AppResource.String.dual_focus_wrong_title else AppResource.String.dual_focus_missed_title,
        ),
        modifier =
            Modifier.semantics {
                heading()
            },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(
            AppResource.String.dual_focus_failure_detail,
            laneLabel(
                result.lane,
            ),
            shapeLabel(result.targetShape),
            result.shownShape?.let {
                shapeLabel(it)
            } ?: stringResource(AppResource.String.dual_focus_missed_shape),
        ),
        textAlign = TextAlign.Center,
    )
    if (state.isNewRecord) {
        Text(
            stringResource(AppResource.String.dual_focus_new_record),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
    GameResultCard(
        primaryText = stringResource(AppResource.String.dual_focus_score_value, state.game.score),
        secondaryText =
            stringResource(
                AppResource.String.dual_focus_result_details,
                state.previousRecord,
                state.game.correctAnswers,
                state.durationMillis,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onReplayClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(Icons.Default.Replay, null)
        Text(stringResource(AppResource.String.dual_focus_replay))
    }
    if (onBackClick != null) {
        OutlinedButton(onBackClick, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(AppResource.String.dual_focus_back_to_games))
        }
    }
}

@Composable
private fun laneLabel(
    lane: DualFocusLane,
) = stringResource(
    if (lane == DualFocusLane.One) AppResource.String.dual_focus_lane_one else AppResource.String.dual_focus_lane_two,
)

@Composable
private fun shapeLabel(shape: DualFocusShape) =
    stringResource(
        when (shape) {
            DualFocusShape.Circle -> AppResource.String.dual_focus_shape_circle
            DualFocusShape.Square -> AppResource.String.dual_focus_shape_square
            DualFocusShape.Triangle -> AppResource.String.dual_focus_shape_triangle
            DualFocusShape.Diamond -> AppResource.String.dual_focus_shape_diamond
        },
    )

@Composable
private fun ShapeGlyph(shape: DualFocusShape, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.08f)
        when (shape) {
            DualFocusShape.Circle -> {
                drawCircle(color.copy(alpha = 0.28f))
                drawCircle(color, style = stroke)
            }
            DualFocusShape.Square -> {
                val inset = size.minDimension * 0.12f
                drawRect(
                    color.copy(alpha = 0.28f),
                    Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 2),
                )
                drawRect(
                    color,
                    Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 2),
                    style = stroke,
                )
            }
            DualFocusShape.Triangle ->
                polygon(
                    listOf(
                        Offset(size.width / 2, size.height * 0.08f),
                        Offset(size.width * 0.9f, size.height * 0.88f),
                        Offset(size.width * 0.1f, size.height * 0.88f),
                    ),
                    color,
                    stroke,
                )
            DualFocusShape.Diamond ->
                polygon(
                    listOf(
                        Offset(size.width / 2, size.height * 0.05f),
                        Offset(size.width * 0.95f, size.height / 2),
                        Offset(size.width / 2, size.height * 0.95f),
                        Offset(size.width * 0.05f, size.height / 2),
                    ),
                    color,
                    stroke,
                )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.polygon(points: List<Offset>, color: Color, stroke: Stroke) {
    val path =
        Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
    drawPath(path, color.copy(alpha = 0.28f))
    drawPath(path, color, style = stroke)
}

private fun previewState(failure: DualFocusFailure? = null, score: Int = 3): DualFocusUiState {
    val targets = mapOf(DualFocusLane.One to DualFocusShape.Circle, DualFocusLane.Two to DualFocusShape.Triangle)
    val windowMillis = DualFocusGame.timingFor(score).windowMillis
    val laneOneAppearedAtMillis = if (score >= 15) 500L else 0L
    val cards =
        mapOf(
            DualFocusLane.One to
                DualFocusCard(
                    1,
                    DualFocusLane.One,
                    DualFocusShape.Circle,
                    DualFocusShape.Circle,
                    laneOneAppearedAtMillis,
                    laneOneAppearedAtMillis + windowMillis,
                ),
            DualFocusLane.Two to
                DualFocusCard(
                    2,
                    DualFocusLane.Two,
                    DualFocusShape.Diamond,
                    DualFocusShape.Triangle,
                    700,
                    700 + windowMillis,
                ),
        )
    val result =
        failure?.let {
            DualFocusResult(
                it,
                DualFocusLane.Two,
                DualFocusShape.Triangle,
                if (it == DualFocusFailure.WrongTap) DualFocusShape.Diamond else null,
            )
        }
    return DualFocusUiState(
        DualFocusState(
            if (failure == null) DualFocusPhase.Active else DualFocusPhase.Result,
            score,
            score,
            targets,
            if (failure == null) cards else emptyMap(),
            result = result,
            nowMillis = 800,
        ),
        record = 5,
        previousRecord = 5,
        durationMillis = 4300,
        isRecordLoading = false,
        isNewRecord = score > 5,
    )
}

@Preview(name = "Dual Focus — loading", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun DualFocusLoadingPreview() {
    OquTurboTheme { DualFocusScreen(DualFocusUiState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Dual Focus — ready", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun DualFocusReadyPreview() {
    OquTurboTheme { DualFocusScreen(DualFocusUiState(record = 4, isRecordLoading = false), {}, { _, _ -> }, null) }
}

@Preview(name = "Dual Focus — early active", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun DualFocusEarlyActivePreview() {
    OquTurboTheme { DualFocusScreen(previewState(), {}, { _, _ -> }, null) }
}

@Preview(name = "Dual Focus — overlap fast", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun DualFocusOverlapFastPreview() {
    OquTurboTheme { DualFocusScreen(previewState(score = 15), {}, { _, _ -> }, null) }
}

@Preview(name = "Dual Focus — correct feedback", widthDp = 390, heightDp = 844)
@ScreenshotPreview
@Composable
private fun DualFocusCorrectFeedbackPreview() {
    OquTurboTheme {
        DualFocusScreen(
            previewState().copy(
                correctFeedbackLane = DualFocusLane.One,
                game = previewState().game.copy(cards = previewState().game.cards - DualFocusLane.One),
            ),
            {
            },
            { _, _ -> },
            null,
        )
    }
}

@Preview(name = "Dual Focus — wrong", widthDp = 390, heightDp = 1000)
@ScreenshotPreview
@Composable
private fun DualFocusWrongPreview() {
    OquTurboTheme { DualFocusScreen(previewState(DualFocusFailure.WrongTap), {}, { _, _ -> }, {}) }
}

@Preview(name = "Dual Focus — missed", widthDp = 390, heightDp = 1000)
@ScreenshotPreview
@Composable
private fun DualFocusMissedPreview() {
    OquTurboTheme { DualFocusScreen(previewState(DualFocusFailure.MissedTarget), {}, { _, _ -> }, {}) }
}

@Preview(name = "Dual Focus — new record compact", widthDp = 320, heightDp = 1000)
@ScreenshotPreview
@Composable
private fun DualFocusNewRecordCompactPreview() {
    OquTurboTheme { DualFocusScreen(previewState(DualFocusFailure.WrongTap, 6), {}, { _, _ -> }, {}) }
}

@Preview(name = "Dual Focus — ready Russian", widthDp = 390, heightDp = 844, locale = "ru")
@ScreenshotPreview
@Composable
private fun DualFocusReadyRussianPreview() {
    OquTurboTheme { DualFocusScreen(DualFocusUiState(record = 4, isRecordLoading = false), {}, { _, _ -> }, null) }
}

@Preview(name = "Dual Focus — result Kazakh", widthDp = 320, heightDp = 1000, locale = "kk")
@ScreenshotPreview
@Composable
private fun DualFocusResultKazakhPreview() {
    OquTurboTheme { DualFocusScreen(previewState(DualFocusFailure.MissedTarget), {}, { _, _ -> }, {}) }
}
