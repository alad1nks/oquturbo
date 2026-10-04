package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.model.DayCompletionStatus
import com.alad1nks.oquturbo.core.data.practice.PracticeDay
import com.alad1nks.oquturbo.core.data.practice.PracticeRhythm
import com.alad1nks.oquturbo.core.data.practice.practiceDate
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PracticeRhythmCard(
    state: PracticeRhythmState,
    onRetry: () -> Unit,
    onWeeklyReview: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        var readyHeight by rememberSaveable { mutableIntStateOf(0) }
        var readyWidth by rememberSaveable { mutableIntStateOf(0) }
        var readyDensity by rememberSaveable { mutableFloatStateOf(0f) }
        var readyFontScale by rememberSaveable { mutableFloatStateOf(0f) }
        val reserve =
            state == PracticeRhythmState.Loading && readyWidth == constraints.maxWidth &&
                readyDensity == density.density && readyFontScale == density.fontScale
        AppCard(
            Modifier.fillMaxWidth().heightIn(min = if (reserve) with(density) { readyHeight.toDp() } else 0.dp)
                .onSizeChanged {
                    if (state is PracticeRhythmState.Ready) {
                        readyHeight = it.height
                        readyWidth = it.width
                        readyDensity = density.density
                        readyFontScale = density.fontScale
                    }
                },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(OquTurboLayout.cardInset),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(AppResource.String.practice_rhythm_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                when (state) {
                    PracticeRhythmState.Loading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).size(32.dp))
                        RhythmText(stringResource(AppResource.String.practice_history_loading))
                    }
                    PracticeRhythmState.Error -> {
                        RhythmText(stringResource(AppResource.String.practice_history_error))
                    }
                    is PracticeRhythmState.Ready -> ReadyRhythm(state.rhythm)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state == PracticeRhythmState.Error) {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                stringResource(AppResource.String.practice_history_retry),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    TextButton(
                        onClick = onWeeklyReview,
                        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            stringResource(AppResource.String.weekly_review_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyRhythm(rhythm: PracticeRhythm) {
    Text(stringResource(AppResource.String.practice_rhythm_goal), style = MaterialTheme.typography.titleMedium)
    RhythmText(
        stringResource(
            if (rhythm.hasUnknownDays) {
                AppResource.String.practice_rhythm_count_partial
            } else {
                AppResource.String.practice_rhythm_count
            },
            rhythm.completedDaysInWindow,
        ),
    )
    RhythmText(
        stringResource(
            AppResource.String.practice_date_range,
            historyDate(rhythm.days.first().epochDay),
            historyDate(rhythm.todayEpochDay),
        ),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val slot = 36.dp * LocalDensity.current.fontScale
        val columns =
            if (maxWidth >= slot * 7 + 4.dp * 6) {
                7
            } else {
                ((maxWidth + 4.dp) / (slot + 4.dp)).toInt().coerceIn(
                    1,
                    4,
                )
            }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rhythm.days.chunked(columns).forEach { days ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { day -> DayTile(day, day.epochDay == rhythm.todayEpochDay, Modifier.weight(1f)) }
                    repeat(columns - days.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
    Text(
        stringResource(
            AppResource.String.practice_today,
            historyDate(rhythm.todayEpochDay),
            stringResource(rhythm.days.last().status.dayStatusResource(today = true)),
        ),
        style = MaterialTheme.typography.bodyMedium,
        textDecoration = TextDecoration.Underline,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DayCompletionStatus.entries.forEach { status ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status.glyph(), Modifier.clearAndSetSemantics {})
                RhythmText(stringResource(status.dayStatusResource(false)), Modifier.weight(1f))
            }
        }
    }
    Text(
        stringResource(
            if (rhythm.weeklyGoalReached) {
                AppResource.String.practice_rhythm_reached
            } else {
                AppResource.String.practice_rhythm_continue
            },
        ),
        style = MaterialTheme.typography.titleMedium,
    )
    RhythmText(stringResource(AppResource.String.practice_definition))
    RhythmText(stringResource(AppResource.String.practice_tracking_start, historyDate(rhythm.trackingStartedEpochDay)))
    if (rhythm.hasFutureFacts) RhythmText(stringResource(AppResource.String.practice_future_dates))
}

@Composable
private fun DayTile(day: PracticeDay, today: Boolean, modifier: Modifier) {
    val semantics =
        stringResource(
            if (today) AppResource.String.practice_today_semantics else AppResource.String.practice_day_semantics,
            historyDate(day.epochDay),
            stringResource(day.status.dayStatusResource(today)),
        )
    Surface(
        modifier.clearAndSetSemantics { contentDescription = semantics },
        shape = MaterialTheme.shapes.medium,
        color =
            if (day.status == DayCompletionStatus.Completed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        border = BorderStroke(if (today) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                practiceDate(day.epochDay).day.toString(),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (today) TextDecoration.Underline else null,
            )
            Text(day.status.glyph(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RhythmText(value: String, modifier: Modifier = Modifier) {
    Text(
        value,
        modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun historyDate(day: Long): String =
    practiceDate(day).let {
        stringResource(AppResource.String.practice_date_format, it.yearText, it.monthText, it.dayText)
    }

private fun DayCompletionStatus.glyph(): String =
    when (this) {
        DayCompletionStatus.Completed -> "✓"
        DayCompletionStatus.NoCompletionRecorded -> "−"
        DayCompletionStatus.Unknown -> "?"
    }

private fun DayCompletionStatus.dayStatusResource(today: Boolean): StringResource =
    when (this) {
        DayCompletionStatus.Completed -> AppResource.String.practice_day_complete
        DayCompletionStatus.NoCompletionRecorded ->
            if (today) {
                AppResource.String.practice_day_today_none
            } else {
                AppResource.String.practice_day_none
            }
        DayCompletionStatus.Unknown -> AppResource.String.practice_day_unknown
    }
