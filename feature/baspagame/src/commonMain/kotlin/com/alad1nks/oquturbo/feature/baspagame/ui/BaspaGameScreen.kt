package com.alad1nks.oquturbo.feature.baspagame.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.AppBackButton
import com.alad1nks.oquturbo.core.ui.component.GameResultCard
import com.alad1nks.oquturbo.core.ui.component.GameStateOverlay
import com.alad1nks.oquturbo.core.ui.component.appBackground
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

    Box(modifier = Modifier.fillMaxSize().appBackground()) {
        Box(
            modifier =
                Modifier
                    .blur(blurRadius)
                    .fillMaxSize()
                    .clickable(
                        enabled = uiState.phase == BaspaGameUiState.Phase.Playing && uiState.stimulus.isNotEmpty(),
                        onClick = onTap,
                    ).systemBarsPadding(),
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .padding(24.dp),
            ) {
                val compactLayout = maxHeight < 600.dp
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ScoreHeader(
                        uiState = uiState,
                        onPauseClick = onPauseClick,
                        showPauseButton = uiState.phase == BaspaGameUiState.Phase.Playing,
                    )
                    Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 20.dp))
                    RuleCard(uiState)
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.stimulus,
                            color = stimulusColor(uiState),
                            fontSize = if (uiState.mode.usesCompactStimulusText()) 40.sp else 56.sp,
                            lineHeight = if (uiState.mode.usesCompactStimulusText()) 48.sp else 56.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (compactLayout) {
                        CompactGameHint(uiState = uiState)
                    } else {
                        GameHint(uiState = uiState)
                    }
                }
            }
        }

        when (uiState.phase) {
            BaspaGameUiState.Phase.Initial -> {
                GameStateOverlay(
                    title = stringResource(AppResource.String.baspa_game_start),
                    icon = Icons.Filled.PlayArrow,
                    onClick = onPauseClick,
                )
                GameBackButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            BaspaGameUiState.Phase.Paused -> {
                GameStateOverlay(
                    title = stringResource(AppResource.String.baspa_game_continue),
                    icon = Icons.Filled.PlayArrow,
                    onClick = onPauseClick,
                )
                GameBackButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            BaspaGameUiState.Phase.Mistake -> {
                GameStateOverlay(
                    title = stringResource(AppResource.String.baspa_game_try_again),
                    icon = Icons.Filled.Replay,
                    onClick = onRestart,
                    extraContent = {
                        GameResultCard(
                            primaryText =
                                "${stringResource(AppResource.String.baspa_game_score_label)}: ${uiState.score}",
                            secondaryText =
                                "${stringResource(AppResource.String.baspa_game_record_label)}: ${uiState.record}",
                        )
                    },
                )
                GameBackButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            BaspaGameUiState.Phase.Playing -> Unit
        }
    }
}

@Composable
private fun GameHint(uiState: BaspaGameUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(AppResource.String.baspa_game_tap_hint),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        SpeedBadge(uiState = uiState)
    }
}

@Composable
private fun CompactGameHint(uiState: BaspaGameUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(AppResource.String.baspa_game_tap_hint),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        SpeedBadge(uiState = uiState, compact = true)
    }
}

@Composable
private fun SpeedBadge(
    uiState: BaspaGameUiState,
    compact: Boolean = false,
) {
    Card(shape = MaterialTheme.shapes.medium) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = if (compact) 12.dp else 20.dp,
                    vertical = if (compact) 8.dp else 14.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 18.dp else 24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(AppResource.String.baspa_game_speed, uiState.intervalMillis / 1000.0),
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GameBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBackButton(
        onClick = onClick,
        contentDescription = stringResource(AppResource.String.kenkoz_game_back),
        modifier = modifier.statusBarsPadding().padding(start = 8.dp, top = 8.dp),
    )
}

@Composable
private fun ScoreHeader(
    uiState: BaspaGameUiState,
    onPauseClick: () -> Unit,
    showPauseButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (showPauseButton) {
            Surface(
                modifier = Modifier.align(Alignment.CenterStart),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                IconButton(onClick = onPauseClick, modifier = Modifier.size(52.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.align(Alignment.CenterStart).size(52.dp))
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(AppResource.String.baspa_game_score_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.score.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(AppResource.String.baspa_game_record_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.record.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(26.dp),
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
    val accent =
        when (uiState.mode) {
            BaspaGameMode.Categories -> stringResource(uiState.mode.ruleAccentResource(), uiState.categoryName)
            BaspaGameMode.Letter -> stringResource(uiState.mode.ruleAccentResource(), uiState.letter)
            BaspaGameMode.WordLength -> stringResource(uiState.mode.ruleAccentResource(), uiState.wordLength)
            BaspaGameMode.TextColor -> stringResource(uiState.mode.ruleAccentResource(), uiState.targetColorName)
            else -> stringResource(uiState.mode.ruleAccentResource())
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = ruleIcon(uiState),
                    contentDescription = null,
                    modifier = Modifier.padding(13.dp).size(26.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = highlightedRule(rule, accent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun ruleIcon(uiState: BaspaGameUiState): ImageVector =
    when (uiState.mode) {
        BaspaGameMode.Categories -> {
            when (uiState.categoryId) {
                "fruits" -> Icons.Filled.Restaurant
                "vehicles" -> Icons.Filled.DirectionsCar
                "professions" -> Icons.Filled.Work
                "clothes" -> Icons.Filled.Checkroom
                "nature" -> Icons.Filled.Forest
                "food" -> Icons.Filled.Restaurant
                "home" -> Icons.Filled.Home
                "sports" -> Icons.Filled.SportsSoccer
                "school" -> Icons.Filled.School
                "animals" -> Icons.Filled.Pets
                else -> Icons.Outlined.Category
            }
        }
        BaspaGameMode.Letter -> Icons.Filled.SortByAlpha
        BaspaGameMode.WordLength -> Icons.Filled.Straighten
        BaspaGameMode.TextColor -> Icons.Filled.Palette
        BaspaGameMode.TrueFalse -> Icons.AutoMirrored.Outlined.FactCheck
        BaspaGameMode.Math -> Icons.Outlined.Calculate
        BaspaGameMode.SpeedReading -> Icons.Outlined.Speed
    }

@Composable
private fun highlightedRule(rule: String, accent: String) =
    buildAnnotatedString {
        val accentStart = rule.lastIndexOf(accent)
        if (accentStart < 0) {
            append(rule)
        } else {
            append(rule.substring(0, accentStart))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(accent)
            }
            append(rule.substring(accentStart + accent.length))
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

private fun BaspaGameMode.ruleAccentResource(): StringResource =
    when (this) {
        BaspaGameMode.Categories -> AppResource.String.baspa_game_rule_categories_accent
        BaspaGameMode.Letter -> AppResource.String.baspa_game_rule_letter_accent
        BaspaGameMode.WordLength -> AppResource.String.baspa_game_rule_length_accent
        BaspaGameMode.TextColor -> AppResource.String.baspa_game_rule_color_accent
        BaspaGameMode.TrueFalse -> AppResource.String.baspa_game_rule_true_accent
        BaspaGameMode.Math -> AppResource.String.baspa_game_rule_math_accent
        BaspaGameMode.SpeedReading -> AppResource.String.baspa_game_rule_seen_accent
    }

private fun BaspaGameMode.usesCompactStimulusText(): Boolean =
    this == BaspaGameMode.TrueFalse || this == BaspaGameMode.Math

@Composable
private fun stimulusColor(uiState: BaspaGameUiState): Color =
    when (uiState.stimulusColorId) {
        "red" -> themeAwareColor(light = Color(0xFFB71C1C), dark = Color(0xFFFF8A80))
        "blue" -> themeAwareColor(light = Color(0xFF0D47A1), dark = Color(0xFF82B1FF))
        "green" -> themeAwareColor(light = Color(0xFF1B5E20), dark = Color(0xFF69F0AE))
        "yellow" -> themeAwareColor(light = Color(0xFF745900), dark = Color(0xFFFFD740))
        "purple" -> themeAwareColor(light = Color(0xFF6A1B9A), dark = Color(0xFFD1A7FF))
        else -> MaterialTheme.colorScheme.primary
    }

@Composable
private fun themeAwareColor(
    light: Color,
    dark: Color,
): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) dark else light

@Preview
@Composable
private fun BaspaGameScreenPreview() {
    OquTurboTheme {
        BaspaGameScreen(
            BaspaGameUiState(BaspaGameMode.Categories, "КОШКА", score = 24, record = 57),
            {},
            {},
            {},
            {},
        )
    }
}
