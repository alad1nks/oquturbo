package com.alad1nks.oquturbo.feature.baspagame.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BaspaGameRoute(viewModel: BaspaGameViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    BaspaGameScreen(uiState, onBackClick, viewModel::tap, viewModel::restart)
}

@Composable
private fun BaspaGameScreen(
    uiState: BaspaGameUiState,
    onBackClick: () -> Unit,
    onTap: () -> Unit,
    onRestart: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable(onClick = if (uiState.phase == BaspaGameUiState.Phase.Mistake) onRestart else onTap)
                .systemBarsPadding()
                .padding(24.dp),
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        ScoreHeader(uiState, Modifier.align(Alignment.TopCenter))
        RuleCard(uiState.mode, Modifier.align(Alignment.TopCenter).padding(top = 140.dp))

        Text(
            text = uiState.stimulus,
            modifier = Modifier.align(Alignment.Center),
            color = stimulusColor(uiState),
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(AppResource.String.baspa_game_tap_hint),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Card(shape = RoundedCornerShape(24.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(AppResource.String.baspa_game_speed, uiState.intervalMillis / 1000.0))
                }
            }
        }

        if (uiState.phase == BaspaGameUiState.Phase.Mistake) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Text(
                        text = stringResource(AppResource.String.baspa_game_try_again),
                        modifier = Modifier.padding(32.dp),
                        fontSize = 40.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreHeader(uiState: BaspaGameUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(stringResource(AppResource.String.baspa_game_score, uiState.score), fontSize = 32.sp)
        Text(
            stringResource(AppResource.String.baspa_game_record, uiState.record),
            modifier = Modifier.padding(start = 40.dp),
        )
    }
}

@Composable
private fun RuleCard(mode: BaspaGameMode, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Text(
            text = stringResource(mode.ruleResource()),
            modifier = Modifier.padding(28.dp),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}

private fun BaspaGameMode.ruleResource(): StringResource =
    when (this) {
        BaspaGameMode.Categories -> AppResource.String.baspa_game_rule_categories
        BaspaGameMode.Letter -> AppResource.String.baspa_game_rule_letter
        BaspaGameMode.WordLength -> AppResource.String.baspa_game_rule_length
        BaspaGameMode.TextColor -> AppResource.String.baspa_game_rule_color
        BaspaGameMode.TrueFalse -> AppResource.String.baspa_game_rule_true
        BaspaGameMode.Math -> AppResource.String.baspa_game_rule_math
        BaspaGameMode.SpeedReading -> AppResource.String.baspa_game_rule_seen
    }

@Composable
private fun stimulusColor(uiState: BaspaGameUiState): Color =
    when (uiState.accent) {
        BaspaGameUiState.Accent.Target -> Color.Red
        BaspaGameUiState.Accent.Other -> Color.Blue
        BaspaGameUiState.Accent.Default -> MaterialTheme.colorScheme.onBackground
    }

@Preview
@Composable
private fun BaspaGameScreenPreview() {
    BaspaGameScreen(BaspaGameUiState(BaspaGameMode.Categories, "КОШКА", score = 24, record = 57), {}, {}, {})
}
