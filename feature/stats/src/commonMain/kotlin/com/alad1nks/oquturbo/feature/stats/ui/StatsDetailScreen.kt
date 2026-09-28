package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboLayout
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.core.ui.component.AppTopBar
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.feature.stats.model.ModeTrend
import com.alad1nks.oquturbo.feature.stats.model.StatsDetailUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsGame
import com.alad1nks.oquturbo.feature.stats.model.StatsMode
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriod
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatsGameDetailRouteContent(
    viewModel: StatsGameDetailViewModel,
    onBackClick: () -> Unit,
    onModeClick: (StatsGame, StatsMode, StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    StatsGameDetailScreen(
        uiState = uiState,
        onPeriodSelected = viewModel::selectPeriod,
        onBackClick = onBackClick,
        onModeClick = onModeClick,
        modifier = modifier,
    )
}

@Composable
internal fun StatsModeDetailRouteContent(
    viewModel: StatsModeDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    StatsModeDetailScreen(
        uiState = uiState,
        onPeriodSelected = viewModel::selectPeriod,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun StatsGameDetailScreen(
    uiState: StatsDetailUiState,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onBackClick: () -> Unit,
    onModeClick: (StatsGame, StatsMode, StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsDetailLayout(
        title = stringResource(uiState.game.titleResource()),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        item {
            PageHeader(
                title = stringResource(AppResource.String.stats_mode_statistics),
                subtitle = stringResource(AppResource.String.stats_game_detail_subtitle),
            )
        }
        item { PeriodSelector(selectedPeriod = uiState.period, onPeriodSelected = onPeriodSelected) }
        if (uiState.modes.isEmpty()) {
            item { NotEnoughDynamics() }
        } else {
            uiState.modes.forEach { trend ->
                item {
                    ModeDetailCard(
                        trend = trend,
                        onClick = { onModeClick(uiState.game, trend.mode, uiState.period) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatsModeDetailScreen(
    uiState: StatsDetailUiState,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsDetailLayout(
        title = uiState.selectedMode?.let { stringResource(it.titleResource()) }.orEmpty(),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        item {
            PageHeader(
                title = stringResource(uiState.game.titleResource()),
                subtitle = stringResource(AppResource.String.stats_mode_detail_subtitle),
            )
        }
        item { PeriodSelector(selectedPeriod = uiState.period, onPeriodSelected = onPeriodSelected) }
        if (uiState.modes.isEmpty()) {
            item { NotEnoughDynamics() }
        } else {
            uiState.modes.forEach { trend ->
                item { ModeDetailCard(trend = trend) }
            }
        }
    }
}

@Composable
private fun ModeDetailCard(
    trend: ModeTrend,
    onClick: (() -> Unit)? = null,
) {
    val openDescription = stringResource(AppResource.String.stats_open_details)
    AppCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = trend.title(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text =
                            pluralStringResource(
                                AppResource.Plural.stats_games_count,
                                trend.gamesPlayed,
                                trend.gamesPlayed,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = openDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            ModeTrendContent(
                trend = trend,
                horizontalContentPadding = PaddingValues(horizontal = 18.dp),
            )
        }
    }
}

@Composable
private fun StatsDetailLayout(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().appBackground()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = title, onBackClick = onBackClick)
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = OquTurboLayout.pageMaxWidth).fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = OquTurboLayout.pageGutter,
                            top = 20.dp,
                            end = OquTurboLayout.pageGutter,
                            bottom = 32.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    content = content,
                )
            }
        }
    }
}
