package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.PageHeader
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel,
    onStartTrainingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        onStartTrainingClick = onStartTrainingClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onStartTrainingClick: () -> Unit,
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
            contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                PageHeader(title = stringResource(AppResource.String.home_title))
            }
            item {
                LevelProgress(
                    level = uiState.overallLevel,
                    rankNumber = uiState.rankNumber,
                    progress = uiState.levelProgress,
                )
            }
            item {
                TrainingCard(onStartTrainingClick = onStartTrainingClick)
            }
            item {
                RecentRecords(records = uiState.recentRecords)
            }
        }
    }
}

@Composable
private fun LevelProgress(
    level: Int,
    rankNumber: Int,
    progress: Float,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(AppResource.String.home_overall_level, level),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text =
                            stringResource(
                                AppResource.String.home_rank,
                                stringResource(AppResource.String.profile_rank_neutral_format, rankNumber),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun TrainingCard(onStartTrainingClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(AppResource.String.home_today_training),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TrainingItem(HomeUiState.Game.NumberSprint)
            TrainingItem(HomeUiState.Game.WideEye)
            TrainingItem(HomeUiState.Game.DontTap)
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onStartTrainingClick,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
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
    }
}

@Composable
private fun RecentRecords(records: List<HomeUiState.RecentRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(AppResource.String.home_recent_records),
            style = MaterialTheme.typography.titleLarge,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            if (records.isEmpty()) {
                Text(
                    text = stringResource(AppResource.String.home_no_recent_records),
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    records.forEach { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = record.game.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(21.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stringResource(record.game.titleResource()),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = record.modeTitle(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = record.score.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingItem(game: HomeUiState.Game) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = game.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = stringResource(game.titleResource()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun HomeUiState.Game.titleResource(): StringResource =
    when (this) {
        HomeUiState.Game.NumberSprint -> AppResource.String.remember_number_title
        HomeUiState.Game.WideEye -> AppResource.String.kenkoz_title
        HomeUiState.Game.DontTap -> AppResource.String.baspa_title
    }

private fun HomeUiState.Mode.titleResource(): StringResource =
    when (this) {
        HomeUiState.Mode.Classic -> AppResource.String.remember_number_menu_item_classic_title
        HomeUiState.Mode.Binary -> AppResource.String.remember_number_menu_item_binary_title
        HomeUiState.Mode.Custom -> AppResource.String.remember_number_menu_item_custom_title
        HomeUiState.Mode.Characters -> AppResource.String.kenkoz_game_menu_item_characters_title
        HomeUiState.Mode.Words -> AppResource.String.kenkoz_game_menu_item_words_title
        HomeUiState.Mode.FindDifference -> AppResource.String.kenkoz_game_menu_item_find_difference_title
        HomeUiState.Mode.WideLine -> AppResource.String.kenkoz_game_menu_item_wide_line_title
        HomeUiState.Mode.Categories -> AppResource.String.baspa_game_menu_categories_title
        HomeUiState.Mode.Letter -> AppResource.String.baspa_game_menu_letter_title
        HomeUiState.Mode.WordLength -> AppResource.String.baspa_game_menu_word_length_title
        HomeUiState.Mode.TextColor -> AppResource.String.baspa_game_menu_text_color_title
        HomeUiState.Mode.TrueFalse -> AppResource.String.baspa_game_menu_true_false_title
        HomeUiState.Mode.Math -> AppResource.String.baspa_game_menu_math_title
        HomeUiState.Mode.SpeedReading -> AppResource.String.baspa_game_menu_speed_reading_title
    }

@Composable
private fun HomeUiState.RecentRecord.modeTitle(): String {
    val title = stringResource(mode.titleResource())
    val settings = variantId?.toCustomSettings() ?: return title
    return buildString {
        append(title)
        append(" · ")
        append(stringResource(AppResource.String.remember_number_menu_item_custom_dialog_length))
        append(": ")
        append(settings.length)
        append(" · ")
        append(stringResource(AppResource.String.remember_number_menu_item_custom_dialog_available_digits))
        append(": ")
        append(settings.digits)
    }
}

private fun String.toCustomSettings(): CustomSettings? {
    val values =
        split(';').associate { part ->
            val separator = part.indexOf(':')
            if (separator < 1) return null
            part.substring(0, separator) to part.substring(separator + 1)
        }
    return CustomSettings(
        length = values["length"]?.toIntOrNull() ?: return null,
        digits = values["digits"]?.takeIf(String::isNotBlank) ?: return null,
    )
}

private data class CustomSettings(val length: Int, val digits: String)

private fun HomeUiState.Game.icon(): ImageVector =
    when (this) {
        HomeUiState.Game.NumberSprint -> Icons.Filled.Bolt
        HomeUiState.Game.WideEye -> Icons.Filled.Visibility
        HomeUiState.Game.DontTap -> Icons.Filled.Block
    }

@Preview
@Composable
private fun HomeScreenPreview() {
    OquTurboTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onStartTrainingClick = {},
        )
    }
}
