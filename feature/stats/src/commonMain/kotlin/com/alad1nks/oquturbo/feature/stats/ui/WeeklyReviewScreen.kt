package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.data.practice.PracticeRhythm
import com.alad1nks.oquturbo.core.data.practice.practiceDate
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.feature.stats.data.toStatsGame
import com.alad1nks.oquturbo.feature.stats.data.toStatsMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WeeklyReviewRouteContent(
    viewModel: WeeklyReviewViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onModeStatisticsClick: (GameSeriesKey) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshReview()
        onPauseOrDispose {}
    }
    WeeklyReviewScreen(
        state,
        onBackClick,
        onHomeClick,
        onModeStatisticsClick,
        viewModel::retryPractice,
        viewModel::retryTraining,
        viewModel::retrySessions,
    )
}

@Composable
internal fun WeeklyReviewScreen(
    state: WeeklyReviewUiState,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onModeStatisticsClick: (GameSeriesKey) -> Unit,
    onRetryPractice: () -> Unit,
    onRetryTraining: () -> Unit,
    onRetrySessions: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val title = stringResource(AppResource.String.weekly_review_title)
    StatsDetailLayout(title, onBackClick, modifier, listState) {
        item(key = "weekly-header") {
            PageHeader(title, subtitle = weeklyRange(state.todayEpochDay - 6, state.todayEpochDay))
        }
        item(key = "weekly-regularity") {
            WeeklyCard {
                WeeklyHeading(AppResource.String.weekly_regularity)
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    WeeklyHistoryBlock(state.practice, practice = true, onRetryPractice)
                    WeeklyHistoryBlock(state.training, practice = false, onRetryTraining)
                }
                WeeklyText(AppResource.String.weekly_today_open)
                if ((state.practice as? WeeklySource.Ready)?.value?.hasFutureFacts == true ||
                    (state.training as? WeeklySource.Ready)?.value?.hasFutureFacts == true
                ) {
                    WeeklyText(AppResource.String.practice_future_dates)
                }
            }
        }
        item(key = "weekly-comparison") {
            WeeklyCard {
                WeeklyHeading(AppResource.String.weekly_result_title)
                WeeklyText(AppResource.String.home_personal_result_window)
                WeeklyText(weeklyRange(state.todayEpochDay - 27, state.todayEpochDay))
                WeeklyText(AppResource.String.weekly_result_explanation)
                WeeklySourceBody(state.comparison) {
                    when (val source = state.comparison) {
                        WeeklySource.Loading -> WeeklyLoading(AppResource.String.weekly_sessions_loading)
                        WeeklySource.Error -> {
                            WeeklyText(AppResource.String.weekly_sessions_error)
                            WeeklyRetry(AppResource.String.weekly_result_title, onRetrySessions)
                        }
                        is WeeklySource.Ready -> WeeklyComparison(source.value, onModeStatisticsClick)
                    }
                }
            }
        }
        item(key = "weekly-next") {
            WeeklyCard {
                WeeklyHeading(AppResource.String.weekly_next_title)
                Text(stringResource(AppResource.String.weekly_next_goal), style = MaterialTheme.typography.titleMedium)
                WeeklyText(AppResource.String.weekly_next_hint)
                Button(
                    onClick = onHomeClick,
                    modifier = weeklyActionModifier(),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        stringResource(AppResource.String.weekly_home),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(OquTurboLayout.cardInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun WeeklyHistoryBlock(state: WeeklySource<PracticeRhythm>, practice: Boolean, onRetry: () -> Unit) {
    val label = if (practice) AppResource.String.weekly_practice_days else AppResource.String.weekly_training_days
    WeeklySourceBody(state) {
        Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (state is WeeklySource.Ready) {
                Text(
                    stringResource(
                        if (state.value.hasUnknownDays) {
                            AppResource.String.practice_rhythm_count_partial
                        } else {
                            AppResource.String.weekly_days_count
                        },
                        state.value.completedDaysInWindow,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        when (state) {
            WeeklySource.Loading ->
                WeeklyLoading(
                    if (practice) {
                        AppResource.String.practice_history_loading
                    } else {
                        AppResource.String.weekly_training_loading
                    },
                )
            WeeklySource.Error -> {
                WeeklyText(
                    if (practice) {
                        AppResource.String.practice_history_error
                    } else {
                        AppResource.String.weekly_training_error
                    },
                )
                WeeklyRetry(label, onRetry)
            }
            is WeeklySource.Ready -> {
                if (practice) {
                    WeeklyText(AppResource.String.practice_rhythm_goal)
                    if (state.value.weeklyGoalReached) WeeklyText(AppResource.String.practice_rhythm_reached)
                }
                WeeklyText(
                    if (practice) {
                        AppResource.String.practice_definition
                    } else {
                        AppResource.String.weekly_training_definition
                    },
                )
                WeeklyText(
                    stringResource(
                        AppResource.String.practice_tracking_start,
                        weeklyDate(state.value.trackingStartedEpochDay),
                    ),
                )
            }
        }
    }
}

@Composable
private fun WeeklyComparison(comparison: ProgressComparison, onModeStatistics: (GameSeriesKey) -> Unit) {
    when (comparison) {
        is ProgressComparison.NoRecentSessions -> WeeklyText(AppResource.String.home_personal_result_empty)
        is ProgressComparison.InsufficientData -> {
            WeeklySeries(comparison.series)
            Text(
                stringResource(AppResource.String.home_personal_result_count, comparison.availableCount),
                style = MaterialTheme.typography.titleMedium,
            )
            WeeklyText(AppResource.String.home_personal_result_insufficient_hint)
            WeeklyModeAction { onModeStatistics(comparison.series) }
        }
        is ProgressComparison.Compared -> {
            WeeklySeries(comparison.series)
            Text(
                stringResource(AppResource.String.home_personal_result_median),
                style = MaterialTheme.typography.titleSmall,
            )
            WeeklyValue(AppResource.String.home_personal_result_previous, comparison.previousMedian.toString())
            WeeklyValue(AppResource.String.home_personal_result_current, comparison.currentMedian.toString())
            WeeklyValue(AppResource.String.home_personal_result_change, comparison.absoluteChange.signedWeeklyChange())
            WeeklyModeAction { onModeStatistics(comparison.series) }
        }
    }
}

@Composable
private fun WeeklySeries(series: GameSeriesKey) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(series.game.toStatsGame().titleResource()), style = MaterialTheme.typography.titleMedium)
        WeeklyText(series.mode.toStatsMode().titleResource())
        when (val metadata = series.weeklyResultMetadata()) {
            WeeklyResultMetadata.None -> Unit
            is WeeklyResultMetadata.Language -> WeeklyText(metadata.resource)
            is WeeklyResultMetadata.Custom -> {
                WeeklyText(
                    stringResource(
                        AppResource.String.remember_number_menu_item_custom_dialog_length,
                    ) + ": " + metadata.length,
                )
                WeeklyText(
                    stringResource(
                        AppResource.String.remember_number_menu_item_custom_dialog_available_digits,
                    ) + ": " + metadata.digits,
                )
            }
            WeeklyResultMetadata.UnknownSettings -> WeeklyText(AppResource.String.home_personal_result_unknown_settings)
            WeeklyResultMetadata.UnknownLanguage -> WeeklyText(AppResource.String.home_personal_result_unknown_language)
            WeeklyResultMetadata.UnknownVariant -> WeeklyText(AppResource.String.home_personal_result_unknown_variant)
        }
    }
}

@Composable
private fun WeeklyValue(label: StringResource, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WeeklyText(label)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun WeeklyLoading(label: StringResource) {
    CircularProgressIndicator(Modifier.size(32.dp))
    WeeklyText(label)
}

@Composable
private fun WeeklyHeading(label: StringResource) {
    Text(
        stringResource(label),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun WeeklyText(label: StringResource) = WeeklyText(stringResource(label))

@Composable
private fun WeeklyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WeeklyRetry(source: StringResource, onRetry: () -> Unit) {
    val description = stringResource(AppResource.String.weekly_retry_source, stringResource(source))
    TextButton(
        onClick = onRetry,
        modifier = weeklyActionModifier().semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(AppResource.String.practice_history_retry),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeeklyModeAction(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = weeklyActionModifier(),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(AppResource.String.home_personal_result_statistics),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private fun weeklyActionModifier() = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp)

@Composable
internal fun weeklyDate(day: Long): String =
    practiceDate(day).let {
        stringResource(AppResource.String.practice_date_format, it.yearText, it.monthText, it.dayText)
    }

@Composable
private fun weeklyRange(start: Long, end: Long): String =
    stringResource(AppResource.String.practice_date_range, weeklyDate(start), weeklyDate(end))

@Composable
private fun <T> WeeklySourceBody(
    state: WeeklySource<T>,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        var height by rememberSaveable { mutableIntStateOf(0) }
        var width by rememberSaveable { mutableIntStateOf(0) }
        var scale by rememberSaveable { mutableFloatStateOf(0f) }
        var fontScale by rememberSaveable { mutableFloatStateOf(0f) }
        val reserve =
            state == WeeklySource.Loading && width == constraints.maxWidth &&
                scale == density.density && fontScale == density.fontScale
        Column(
            Modifier.fillMaxWidth().heightIn(min = if (reserve) with(density) { height.toDp() } else 0.dp)
                .onSizeChanged {
                    if (state is WeeklySource.Ready) {
                        height = it.height
                        width = it.width
                        scale = density.density
                        fontScale = density.fontScale
                    }
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}
