package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatsRouteContent(
    viewModel: StatsViewModel,
    onGamesClick: () -> Unit,
    onGameClick: (StatsGame, StatsPeriod) -> Unit,
    onActivityClick: (StatsGame, StatsMode?, StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    StatsScreen(
        uiState = uiState,
        onPeriodSelected = viewModel::selectPeriod,
        onDaySelected = viewModel::selectDay,
        onGameSelected = viewModel::selectGame,
        onModeSelected = viewModel::selectMode,
        onGamesClick = onGamesClick,
        onGameClick = { game -> onGameClick(game, uiState.period) },
        onActivityClick = { game, mode -> onActivityClick(game, mode, uiState.period) },
        modifier = modifier,
    )
}

@Composable
internal fun StatsScreen(
    uiState: StatsUiState,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onDaySelected: (Int) -> Unit,
    onGameSelected: (StatsGame) -> Unit,
    onModeSelected: (StatsMode) -> Unit,
    onGamesClick: () -> Unit,
    onGameClick: (StatsGame) -> Unit,
    onActivityClick: (StatsGame, StatsMode?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().appBackground()) {
        LazyColumn(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                PageHeader(
                    title = stringResource(AppResource.String.oquturbo_navigation_stats),
                    subtitle = stringResource(AppResource.String.oquturbo_stats_subtitle),
                    modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                )
            }
            item {
                PeriodSelector(
                    selectedPeriod = uiState.period,
                    onPeriodSelected = onPeriodSelected,
                    contentPadding = PaddingValues(horizontal = StatsScreenHorizontalPadding),
                )
            }

            when {
                uiState.snapshot.isNewUser -> {
                    item {
                        NewUserEmptyState(
                            onGamesClick = onGamesClick,
                            modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                        )
                    }
                }
                !uiState.snapshot.hasActivity -> {
                    if (uiState.snapshot.activityDays.isNotEmpty()) {
                        item {
                            ActivitySection(
                                days = uiState.snapshot.activityDays,
                                selectedDay = uiState.selectedDay,
                                onDaySelected = onDaySelected,
                                modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                            )
                        }
                    }
                    item {
                        PeriodEmptyState(
                            modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                        )
                    }
                }
                else -> {
                    item {
                        SummarySection(
                            summary = uiState.snapshot.summary,
                            modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                        )
                    }
                    if (uiState.snapshot.activityDays.isNotEmpty()) {
                        item {
                            ActivitySection(
                                days = uiState.snapshot.activityDays,
                                selectedDay = uiState.selectedDay,
                                onDaySelected = onDaySelected,
                                modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                            )
                        }
                    }
                    item {
                        DynamicsSection(
                            trends = uiState.snapshot.trends,
                            selectedGame = uiState.selectedGame,
                            selectedMode = uiState.selectedMode,
                            selectedModeTrend = uiState.selectedModeTrend,
                            onGameSelected = onGameSelected,
                            onModeSelected = onModeSelected,
                            modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                        )
                    }
                    if (uiState.snapshot.skills.isNotEmpty()) {
                        item {
                            SkillsSection(
                                skills = uiState.snapshot.skills,
                                modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                            )
                        }
                    }
                    if (uiState.snapshot.games.isNotEmpty()) {
                        item {
                            GamesStatsSection(
                                games = uiState.snapshot.games,
                                onGameClick = onGameClick,
                                modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                            )
                        }
                    }
                    if (uiState.snapshot.recentActivity.isNotEmpty()) {
                        item {
                            RecentHistorySection(
                                activities = uiState.snapshot.recentActivity,
                                onActivityClick = onActivityClick,
                                modifier = Modifier.padding(horizontal = StatsScreenHorizontalPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val StatsScreenHorizontalPadding = 24.dp

@Composable
private fun NewUserEmptyState(
    onGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyStateCard(
        icon = Icons.Filled.SportsEsports,
        title = stringResource(AppResource.String.stats_empty_title),
        message = stringResource(AppResource.String.stats_empty_message),
        modifier = modifier,
    ) {
        Button(onClick = onGamesClick) {
            Text(stringResource(AppResource.String.stats_go_to_games))
        }
    }
}

@Composable
private fun PeriodEmptyState(modifier: Modifier = Modifier) {
    EmptyStateCard(
        icon = Icons.Filled.Equalizer,
        title = stringResource(AppResource.String.stats_period_empty_title),
        message = stringResource(AppResource.String.stats_period_empty_message),
        modifier = modifier,
    )
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.invoke()
        }
    }
}
