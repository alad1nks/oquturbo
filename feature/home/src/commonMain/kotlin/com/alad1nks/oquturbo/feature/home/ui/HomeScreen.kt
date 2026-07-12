package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeRoute(
    onStartTrainingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeScreen(
        uiState = HomeUiState(),
        onStartTrainingClick = onStartTrainingClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onStartTrainingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(AppResource.String.home_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LevelProgress(
                    level = uiState.overallLevel,
                    rank = uiState.rank,
                )
                TrainingCard(onStartTrainingClick = onStartTrainingClick)
                RecentRecords(
                    records = uiState.recentRecords,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LevelProgress(
    level: Int,
    rank: HomeUiState.Rank,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(AppResource.String.home_overall_level, level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(AppResource.String.home_rank, stringResource(rank.titleResource())),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { level.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun StartTrainingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Text(
            text = stringResource(AppResource.String.home_start_training),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun TrainingCard(onStartTrainingClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(AppResource.String.home_today_training),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TrainingItem(HomeUiState.Game.NumberSprint)
            TrainingItem(HomeUiState.Game.WideEye)
            TrainingItem(HomeUiState.Game.DontTap)

            StartTrainingButton(
                onClick = onStartTrainingClick,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().widthIn(max = 360.dp),
            )
        }
    }
}

@Composable
private fun RecentRecords(
    records: List<HomeUiState.RecentRecord>,
    modifier: Modifier = Modifier,
) {
    HomeSection(
        title = AppResource.String.home_recent_records,
        modifier = modifier,
    ) {
        records.forEach { record ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(record.game.titleResource()),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = record.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HomeSection(
    title: StringResource,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun TrainingItem(game: HomeUiState.Game) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = game.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(game.titleResource()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun HomeUiState.Game.titleResource(): StringResource =
    when (this) {
        HomeUiState.Game.NumberSprint -> AppResource.String.remember_number_title
        HomeUiState.Game.WideEye -> AppResource.String.kenkoz_title
        HomeUiState.Game.DontTap -> AppResource.String.baspa_title
    }

private fun HomeUiState.Rank.titleResource(): StringResource =
    when (this) {
        HomeUiState.Rank.Master -> AppResource.String.home_rank_master
    }

private fun HomeUiState.Game.icon(): ImageVector =
    when (this) {
        HomeUiState.Game.NumberSprint -> Icons.Filled.Bolt
        HomeUiState.Game.WideEye -> Icons.Filled.Visibility
        HomeUiState.Game.DontTap -> Icons.Filled.Block
    }

@Preview
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onStartTrainingClick = {},
        )
    }
}
