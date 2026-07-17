package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.feature.stats.model.ActivityStatus
import com.alad1nks.oquturbo.feature.stats.model.GameStatsRow
import com.alad1nks.oquturbo.feature.stats.model.GameTrend
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.RecentActivity
import com.alad1nks.oquturbo.feature.stats.model.RecentActivityType
import com.alad1nks.oquturbo.feature.stats.model.SkillInsight
import com.alad1nks.oquturbo.feature.stats.model.StatsDayActivity
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsSummary
import com.alad1nks.oquturbo.feature.stats.model.StatsTrend
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue

@Composable
internal fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(StatsPeriod.entries) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(stringResource(period.titleResource())) },
            )
        }
    }
}

@Composable
internal fun SummarySection(
    summary: StatsSummary,
    modifier: Modifier = Modifier,
) {
    StatsSection(
        title = stringResource(AppResource.String.stats_summary_title),
        modifier = modifier,
    ) {
        val metrics =
            listOf(
                SummaryMetric(
                    Icons.Filled.EventAvailable,
                    AppResource.Plural.stats_trainings_count,
                    summary.trainings,
                ),
                SummaryMetric(
                    Icons.Filled.SportsEsports,
                    AppResource.Plural.stats_games_count,
                    summary.games,
                ),
                SummaryMetric(
                    Icons.Filled.Timer,
                    AppResource.Plural.stats_minutes_count,
                    summary.minutes,
                ),
                SummaryMetric(
                    Icons.Filled.CheckCircle,
                    AppResource.Plural.stats_correct_answers_count,
                    summary.correctAnswers,
                ),
            )
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowMetrics.forEach { metric ->
                    SummaryMetricItem(metric = metric, modifier = Modifier.weight(1f))
                }
                if (rowMetrics.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    metric: SummaryMetric,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = pluralStringResource(metric.text, metric.value, metric.value),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun ActivitySection(
    days: List<StatsDayActivity>,
    selectedDay: StatsDayActivity?,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsSection(
        title = stringResource(AppResource.String.stats_activity_title),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = StatsCardContentPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.chunked(7).forEach { week ->
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = StatsCardContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items = week, key = { it.id }) { day ->
                        ActivityDay(
                            day = day,
                            selected = day.id == selectedDay?.id,
                            onClick = { onDaySelected(day.id) },
                        )
                    }
                }
            }
        }

        ActivityLegend(modifier = Modifier.padding(horizontal = StatsCardContentPadding))

        selectedDay?.let { day ->
            Surface(
                modifier =
                    Modifier
                        .padding(horizontal = StatsCardContentPadding)
                        .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                AppResource.String.stats_day_details_summary,
                                pluralStringResource(
                                    AppResource.Plural.stats_games_count,
                                    day.games,
                                    day.games,
                                ),
                                pluralStringResource(
                                    AppResource.Plural.stats_minutes_count,
                                    day.minutes,
                                    day.minutes,
                                ),
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text =
                            stringResource(
                                if (day.status == ActivityStatus.DailyComplete) {
                                    AppResource.String.stats_training_completed
                                } else {
                                    AppResource.String.stats_training_not_completed
                                },
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityDay(
    day: StatsDayActivity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekdayDescription = stringResource(day.weekday.titleResource())
    val statusDescription = stringResource(day.status.titleResource())
    val activityDescription =
        stringResource(
            AppResource.String.stats_day_details_summary,
            day.games,
            day.minutes,
        )
    val dayDescription =
        listOf(
            weekdayDescription,
            day.dayNumber.toString(),
            statusDescription,
            activityDescription,
        ).joinToString()
    val colors = day.status.colors()
    Surface(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = dayDescription },
        shape = MaterialTheme.shapes.small,
        color = colors.first,
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else colors.second,
            ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = weekdayDescription,
                style = MaterialTheme.typography.labelSmall,
                color = colors.third,
                maxLines = 1,
            )
            Text(
                text = day.dayNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = colors.third,
            )
        }
    }
}

@Composable
private fun ActivityStatus.colors(): Triple<Color, Color, Color> =
    when (this) {
        ActivityStatus.DailyComplete ->
            Triple(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
            )
        ActivityStatus.DailyPartial ->
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.onPrimaryContainer,
            )
        ActivityStatus.GamesOnly ->
            Triple(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.onSecondaryContainer,
            )
        ActivityStatus.None ->
            Triple(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }

@Composable
private fun ActivityLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ActivityStatus.entries.chunked(2).forEach { statuses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                statuses.forEach { status ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val colors = status.colors()
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = CircleShape,
                            color = colors.first,
                            border = BorderStroke(1.dp, colors.second),
                            content = {},
                        )
                        Text(
                            text = stringResource(status.titleResource()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DynamicsSection(
    trends: List<GameTrend>,
    selectedGame: StatsGame?,
    selectedMode: StatsMode?,
    selectedModeTrend: ModeTrend?,
    onGameSelected: (StatsGame) -> Unit,
    onModeSelected: (StatsMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsSection(
        title = stringResource(AppResource.String.stats_dynamics_title),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = StatsCardContentPadding),
    ) {
        Text(
            text = stringResource(AppResource.String.stats_game_label),
            modifier = Modifier.padding(horizontal = StatsCardContentPadding),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectorRow {
            items(items = trends, key = { it.game }) { trend ->
                FilterChip(
                    selected = selectedGame == trend.game,
                    onClick = { onGameSelected(trend.game) },
                    label = { Text(stringResource(trend.game.titleResource())) },
                    leadingIcon = {
                        Icon(
                            imageVector = trend.game.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        val modes = trends.firstOrNull { it.game == selectedGame }?.modes.orEmpty()
        if (modes.size > 1) {
            Text(
                text = stringResource(AppResource.String.stats_mode_label),
                modifier = Modifier.padding(horizontal = StatsCardContentPadding),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectorRow {
                items(items = modes, key = { it.mode }) { mode ->
                    FilterChip(
                        selected = selectedMode == mode.mode,
                        onClick = { onModeSelected(mode.mode) },
                        label = { Text(stringResource(mode.mode.titleResource())) },
                    )
                }
            }
        }

        if (selectedModeTrend == null || selectedModeTrend.scores.size < 3) {
            NotEnoughDynamics(modifier = Modifier.padding(horizontal = StatsCardContentPadding))
        } else {
            ModeTrendContent(
                trend = selectedModeTrend,
                horizontalContentPadding = PaddingValues(horizontal = StatsCardContentPadding),
            )
        }
    }
}

@Composable
internal fun ModeTrendContent(
    trend: ModeTrend,
    modifier: Modifier = Modifier,
    horizontalContentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (trend.hasNewRecord) {
            Surface(
                modifier = Modifier.padding(horizontalContentPadding),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(AppResource.String.stats_new_record_label),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        StatsLineChart(
            scores = trend.scores,
            modifier = Modifier.padding(horizontalContentPadding),
        )
        StatsScoreHistory(
            scores = trend.scores,
            contentPadding = horizontalContentPadding,
        )
        Row(
            modifier =
                Modifier
                    .padding(horizontalContentPadding)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrendMetric(
                label = stringResource(AppResource.String.stats_record),
                value = trend.record,
                modifier = Modifier.weight(1f),
            )
            TrendMetric(
                label = stringResource(AppResource.String.stats_last_result),
                value = trend.lastResult,
                modifier = Modifier.weight(1f),
            )
            TrendMetric(
                label = stringResource(AppResource.String.stats_average_result),
                value = trend.averageResult,
                modifier = Modifier.weight(1f),
            )
        }
        trend.comparisonPercent?.let { comparison ->
            val comparisonText =
                when {
                    comparison > 0 ->
                        stringResource(AppResource.String.stats_comparison_higher, comparison)
                    comparison < 0 ->
                        stringResource(AppResource.String.stats_comparison_lower, comparison.absoluteValue)
                    else -> stringResource(AppResource.String.stats_comparison_same)
                }
            Text(
                text = comparisonText,
                modifier = Modifier.padding(horizontalContentPadding),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TrendMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun NotEnoughDynamics(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(AppResource.String.stats_not_enough_dynamics_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(AppResource.String.stats_not_enough_dynamics_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SkillsSection(
    skills: List<SkillInsight>,
    modifier: Modifier = Modifier,
) {
    StatsSection(
        title = stringResource(AppResource.String.stats_skills_title),
        modifier = modifier,
    ) {
        skills.forEach { insight -> SkillRow(insight) }
    }
}

@Composable
private fun SkillRow(insight: SkillInsight) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(insight.skill.titleResource()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        pluralStringResource(
                            AppResource.Plural.stats_skill_training_count,
                            insight.trainings,
                            insight.trainings,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = insight.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (insight.trend != StatsTrend.NotEnoughData) {
                Text(
                    text = stringResource(insight.trend.titleResource()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SkillInsight.description(): String =
    averageChangePercent?.let { change ->
        if (change >= 0) {
            stringResource(AppResource.String.stats_skill_positive_change, change)
        } else {
            stringResource(AppResource.String.stats_skill_negative_change, change.absoluteValue)
        }
    } ?: stringResource(AppResource.String.stats_skill_not_enough)

@Composable
internal fun GamesStatsSection(
    games: List<GameStatsRow>,
    onGameClick: (StatsGame) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsSection(
        title = stringResource(AppResource.String.stats_by_games_title),
        modifier = modifier,
    ) {
        games.forEach { row -> GameStatsItem(row = row, onClick = { onGameClick(row.game) }) }
    }
}

@Composable
private fun GameStatsItem(
    row: GameStatsRow,
    onClick: () -> Unit,
) {
    val openDescription = stringResource(AppResource.String.stats_open_details)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = row.game.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(row.game.titleResource()),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            AppResource.String.stats_game_row_summary,
                            pluralStringResource(
                                AppResource.Plural.stats_games_count,
                                row.gamesPlayed,
                                row.gamesPlayed,
                            ),
                            pluralStringResource(
                                AppResource.Plural.stats_modes_count,
                                row.totalModes,
                                row.totalModes,
                            ),
                            pluralStringResource(
                                AppResource.Plural.stats_minutes_count,
                                row.minutes,
                                row.minutes,
                            ),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        pluralStringResource(
                            AppResource.Plural.stats_game_records_modes,
                            row.totalModes,
                            row.modesWithRecords,
                            row.totalModes,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = openDescription,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun RecentHistorySection(
    activities: List<RecentActivity>,
    onActivityClick: (StatsGame, StatsMode?) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsSection(
        title = stringResource(AppResource.String.stats_recent_history_title),
        modifier = modifier,
    ) {
        activities.forEach { activity ->
            RecentHistoryItem(
                activity = activity,
                onClick = activity.game?.let { game -> ({ onActivityClick(game, activity.mode) }) },
            )
        }
    }
}

@Composable
private fun RecentHistoryItem(
    activity: RecentActivity,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector =
                        if (activity.type == RecentActivityType.DailyTraining) {
                            Icons.Filled.EventAvailable
                        } else {
                            activity.game?.icon() ?: Icons.Filled.History
                        },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = activity.description(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = activity.timeDescription(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentActivity.description(): String {
    val gameTitle = game?.let { stringResource(it.titleResource()) }.orEmpty()
    val modeTitle = mode?.let { stringResource(it.titleResource()) }.orEmpty()
    return when (type) {
        RecentActivityType.NewRecord ->
            stringResource(AppResource.String.stats_history_new_record, gameTitle, modeTitle, score ?: 0)
        RecentActivityType.DailyTraining -> stringResource(AppResource.String.stats_history_daily_completed)
        RecentActivityType.GameResult ->
            stringResource(AppResource.String.stats_history_game_result, gameTitle, modeTitle, score ?: 0)
        RecentActivityType.PersonalBest ->
            stringResource(AppResource.String.stats_history_personal_best, gameTitle, modeTitle, score ?: 0)
    }
}

@Composable
private fun RecentActivity.timeDescription(): String =
    when (daysAgo) {
        0 -> stringResource(AppResource.String.stats_today)
        1 -> stringResource(AppResource.String.stats_yesterday)
        else -> pluralStringResource(AppResource.Plural.stats_days_ago, daysAgo, daysAgo)
    }

@Composable
internal fun StatsSection(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(StatsCardContentPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SelectorRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = StatsCardContentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private val StatsCardContentPadding = 18.dp

private data class SummaryMetric(
    val icon: ImageVector,
    val text: PluralStringResource,
    val value: Int,
)
