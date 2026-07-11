package com.alad1nks.oquturbo.feature.baspagame.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
    BaspaGameScreen(uiState, onBackClick, viewModel::togglePause, viewModel::tap, viewModel::restart)
}

@Composable
private fun BaspaGameScreen(
    uiState: BaspaGameUiState,
    onBackClick: () -> Unit,
    onPauseClick: () -> Unit,
    onTap: () -> Unit,
    onRestart: () -> Unit,
) {
    val blurRadius by animateDpAsState(
        targetValue = if (uiState.phase == BaspaGameUiState.Phase.Playing) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = 700),
    )
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .blur(blurRadius)
                    .fillMaxSize()
                    .clickable(onClick = if (uiState.phase == BaspaGameUiState.Phase.Mistake) onRestart else onTap)
                    .systemBarsPadding()
                    .padding(24.dp),
        ) {
            ScoreHeader(uiState, onPauseClick, Modifier.align(Alignment.TopCenter))
            RuleCard(uiState, Modifier.align(Alignment.TopCenter).padding(top = 140.dp))

            Text(
                text = uiState.stimulus,
                modifier = Modifier.align(Alignment.Center),
                color = stimulusColor(uiState),
                fontSize = 64.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Icon(
                    Icons.Outlined.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
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
        }

        if (uiState.phase == BaspaGameUiState.Phase.Mistake) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(onClick = onRestart),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Text(
                        text = stringResource(AppResource.String.baspa_game_try_again),
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                BackButton(onBackClick, Modifier.align(Alignment.TopStart))
            }
        }

        if (uiState.phase == BaspaGameUiState.Phase.Initial) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(onClick = onPauseClick),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Text(
                        text = stringResource(AppResource.String.baspa_game_start),
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                BackButton(onBackClick, Modifier.align(Alignment.TopStart))
            }
        }

        if (uiState.phase == BaspaGameUiState.Phase.Paused) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(onClick = onPauseClick),
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp).size(88.dp),
                    )
                }
                BackButton(onBackClick, Modifier.align(Alignment.TopStart))
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(start = 4.dp, top = 8.dp).statusBarsPadding(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun ScoreHeader(
    uiState: BaspaGameUiState,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier.align(Alignment.CenterStart),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            IconButton(onClick = onPauseClick, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(AppResource.String.baspa_game_score_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(uiState.score.toString(), fontSize = 56.sp, fontWeight = FontWeight.Black)
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(AppResource.String.baspa_game_record_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(uiState.record.toString(), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp).size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RuleCard(uiState: BaspaGameUiState, modifier: Modifier = Modifier) {
    val rule =
        when (uiState.mode) {
            BaspaGameMode.Categories -> stringResource(uiState.mode.ruleResource(), uiState.categoryName)
            BaspaGameMode.Letter -> stringResource(uiState.mode.ruleResource(), uiState.letter)
            BaspaGameMode.WordLength -> stringResource(uiState.mode.ruleResource(), uiState.wordLength)
            BaspaGameMode.TextColor -> stringResource(uiState.mode.ruleResource(), uiState.targetColorName)
            else -> stringResource(uiState.mode.ruleResource())
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    ruleIcon(uiState),
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp).size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = highlightedRule(rule),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun ruleIcon(uiState: BaspaGameUiState): ImageVector {
    if (uiState.mode == BaspaGameMode.Letter || uiState.mode == BaspaGameMode.WordLength) {
        return Icons.Filled.SortByAlpha
    }
    if (uiState.mode == BaspaGameMode.TextColor) return Icons.Filled.Palette
    return when (uiState.categoryId) {
        "fruits" -> Icons.Filled.Restaurant
        "vehicles" -> Icons.Filled.DirectionsCar
        "professions" -> Icons.Filled.Work
        "clothes" -> Icons.Filled.Checkroom
        else -> Icons.Filled.Pets
    }
}

@Composable
private fun highlightedRule(rule: String) =
    buildAnnotatedString {
        val splitIndex = rule.lastIndexOf(' ')
        if (splitIndex < 0) {
            withStyle(
                SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
            ) { append(rule) }
        } else {
            append(rule.substring(0, splitIndex + 1))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(rule.substring(splitIndex + 1))
            }
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
    when (uiState.stimulusColorId) {
        "red" -> Color.Red
        "blue" -> Color.Blue
        "green" -> Color.Green
        "yellow" -> Color.Yellow
        "purple" -> Color(0xFF8E24AA)
        else -> MaterialTheme.colorScheme.primary
    }

@Preview
@Composable
private fun BaspaGameScreenPreview() {
    Surface {
        BaspaGameScreen(
            BaspaGameUiState(BaspaGameMode.Categories, "КОШКА", score = 24, record = 57),
            {},
            {},
            {},
            {},
        )
    }
}
