package com.alad1nks.oquturbo.feature.stats.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun StatsGameDetailScreen(
    uiState: StatsDetailUiState,
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
private fun StatsModeDetailScreen(
    uiState: StatsDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = uiState.modes.firstOrNull()
    StatsDetailLayout(
        title = mode?.let { stringResource(it.mode.titleResource()) }.orEmpty(),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        item {
            PageHeader(
                title = stringResource(uiState.game.titleResource()),
                subtitle = stringResource(AppResource.String.stats_mode_detail_subtitle),
            )
        }
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
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
                    modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
                    contentPadding = PaddingValues(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    content = content,
                )
            }
        }
    }
}
