package com.alad1nks.oquturbo.feature.games.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.core.ui.component.appBackground
import com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview
import com.alad1nks.oquturbo.feature.games.model.TrainingGame
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GamesRoute(
    onGameClick: (TrainingGame) -> Unit,
    modifier: Modifier = Modifier,
) {
    GamesScreen(
        uiState = GamesUiState(),
        onGameClick = onGameClick,
        modifier = modifier,
    )
}

@Composable
private fun GamesScreen(
    uiState: GamesUiState,
    onGameClick: (TrainingGame) -> Unit,
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
        ) {
            item {
                GamesHeader(
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
                )
            }
            item { Spacer(modifier = Modifier.height(28.dp)) }
            item {
                Text(
                    text = stringResource(AppResource.String.games_skills_title),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                SkillsRow()
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            uiState.games.forEach { summary ->
                item {
                    ActiveGameCard(
                        summary = summary,
                        onClick = { onGameClick(summary.game) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                }
            }
            if (uiState.upcomingGames.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(22.dp)) }
                item {
                    Text(
                        text = stringResource(AppResource.String.games_coming_soon_title),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                uiState.upcomingGames.forEach { game ->
                    item {
                        UpcomingGameCard(
                            game = game,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp),
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
            item {
                MotivationBanner(modifier = Modifier.padding(horizontal = 24.dp))
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GamesHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(AppResource.String.oquturbo_navigation_games),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(AppResource.String.games_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SkillsRow(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            listOf(
                GamesUiState.Skill.Memory,
                GamesUiState.Skill.Attention,
                GamesUiState.Skill.Reaction,
                GamesUiState.Skill.Reading,
            ),
        ) { skill ->
            SkillChip(skill)
        }
    }
}

@Composable
private fun SkillChip(
    skill: GamesUiState.Skill,
    modifier: Modifier = Modifier,
) {
    val color = skill.color()
    Surface(
        modifier = modifier.height(46.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = skill.icon(),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(skill.titleResource()),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActiveGameCard(
    summary: GamesUiState.GameSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameArtwork(game = summary.game)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(summary.game.titleResource()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(summary.game.descriptionResource()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                SkillTags(skills = summary.skills)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = stringResource(AppResource.String.games_modes_count, summary.modesCount),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillTags(skills: List<GamesUiState.Skill>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEach { skill ->
            Text(
                text = "• ${stringResource(skill.titleResource())}",
                style = MaterialTheme.typography.labelMedium,
                color = skill.color(),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GameArtwork(game: TrainingGame) {
    Box(
        modifier =
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(game.artworkBackground()),
        contentAlignment = Alignment.Center,
    ) {
        when (game) {
            TrainingGame.NumberSprint -> NumberGridArtwork()
            TrainingGame.WideEye ->
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            TrainingGame.DontTap -> DontTapArtwork()
            TrainingGame.MemoryGrid -> MemoryGridArtwork()
            TrainingGame.WordFlow -> WordFlowArtwork()
            TrainingGame.DualFocus ->
                Icon(
                    imageVector = Icons.Filled.ViewColumn,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            TrainingGame.RotationMatch -> RotationMatchArtwork()
        }
    }
}

@Composable
private fun RotationMatchArtwork() {
    Row(
        modifier = Modifier.size(64.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniRotationPattern(rotated = false)
        Icon(
            imageVector = Icons.Filled.RotateRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        MiniRotationPattern(rotated = true)
    }
}

@Composable
private fun MiniRotationPattern(rotated: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    val filled = index in if (rotated) setOf(2, 5, 7, 8) else setOf(0, 1, 2, 3)
                    Box(
                        Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WordFlowArtwork() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(
                    14.dp,
                ).height(
                    4.dp,
                ).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSecondaryContainer),
            )
            Box(
                Modifier.width(20.dp).height(12.dp).clip(RoundedCornerShape(4.dp))
                    .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
            )
            Box(
                Modifier.width(
                    14.dp,
                ).height(
                    4.dp,
                ).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSecondaryContainer),
            )
        }
    }
}

@Composable
private fun MemoryGridArtwork() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { column ->
                    Box(
                        Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(
                            if (row == column) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberGridArtwork() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberTile(1)
            NumberTile(2)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberTile(3)
            NumberTile(4)
        }
    }
}

@Composable
private fun NumberTile(number: Int) {
    Box(
        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun DontTapArtwork() {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = null,
            modifier = Modifier.size(58.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Icon(
            imageVector = Icons.Filled.TouchApp,
            contentDescription = null,
            modifier = Modifier.padding(start = 18.dp, top = 20.dp).size(38.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun UpcomingGameCard(
    game: GamesUiState.UpcomingGame,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(game.titleResource()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = stringResource(game.descriptionResource()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            ) {
                Text(
                    text = stringResource(AppResource.String.games_soon),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun MotivationBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.games_motivation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(AppResource.String.games_motivation_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun TrainingGame.titleResource(): StringResource =
    when (this) {
        TrainingGame.NumberSprint -> AppResource.String.remember_number_title
        TrainingGame.WideEye -> AppResource.String.kenkoz_title
        TrainingGame.DontTap -> AppResource.String.baspa_title
        TrainingGame.MemoryGrid -> AppResource.String.memory_grid_title
        TrainingGame.WordFlow -> AppResource.String.word_flow_title
        TrainingGame.DualFocus -> AppResource.String.dual_focus_title
        TrainingGame.RotationMatch -> AppResource.String.rotation_match_title
    }

private fun TrainingGame.descriptionResource(): StringResource =
    when (this) {
        TrainingGame.NumberSprint -> AppResource.String.games_number_sprint_description
        TrainingGame.WideEye -> AppResource.String.games_wide_eye_description
        TrainingGame.DontTap -> AppResource.String.games_dont_tap_description
        TrainingGame.MemoryGrid -> AppResource.String.games_memory_grid_description
        TrainingGame.WordFlow -> AppResource.String.games_word_flow_description
        TrainingGame.DualFocus -> AppResource.String.games_dual_focus_description
        TrainingGame.RotationMatch -> AppResource.String.games_rotation_match_description
    }

@Composable
private fun TrainingGame.artworkBackground(): Color =
    when (this) {
        TrainingGame.NumberSprint -> MaterialTheme.colorScheme.primaryContainer
        TrainingGame.WideEye -> MaterialTheme.colorScheme.secondaryContainer
        TrainingGame.DontTap -> MaterialTheme.colorScheme.tertiaryContainer
        TrainingGame.MemoryGrid -> MaterialTheme.colorScheme.primaryContainer
        TrainingGame.WordFlow -> MaterialTheme.colorScheme.secondaryContainer
        TrainingGame.DualFocus -> MaterialTheme.colorScheme.primaryContainer
        TrainingGame.RotationMatch -> MaterialTheme.colorScheme.primaryContainer
    }

private fun GamesUiState.Skill.titleResource(): StringResource =
    when (this) {
        GamesUiState.Skill.Memory -> AppResource.String.games_skill_memory
        GamesUiState.Skill.Attention -> AppResource.String.games_skill_attention
        GamesUiState.Skill.Reaction -> AppResource.String.games_skill_reaction
        GamesUiState.Skill.Reading -> AppResource.String.games_skill_reading
        GamesUiState.Skill.Vision -> AppResource.String.games_skill_vision
    }

private fun GamesUiState.Skill.icon(): ImageVector =
    when (this) {
        GamesUiState.Skill.Memory -> Icons.Filled.Psychology
        GamesUiState.Skill.Attention -> Icons.Filled.Visibility
        GamesUiState.Skill.Reaction -> Icons.Filled.Bolt
        GamesUiState.Skill.Reading -> Icons.Filled.AutoStories
        GamesUiState.Skill.Vision -> Icons.Filled.Visibility
    }

@Composable
private fun GamesUiState.Skill.color(): Color =
    when (this) {
        GamesUiState.Skill.Memory -> MaterialTheme.colorScheme.primary
        GamesUiState.Skill.Attention -> MaterialTheme.colorScheme.secondary
        GamesUiState.Skill.Reaction -> MaterialTheme.colorScheme.tertiary
        GamesUiState.Skill.Reading -> MaterialTheme.colorScheme.onSecondaryContainer
        GamesUiState.Skill.Vision -> MaterialTheme.colorScheme.onTertiaryContainer
    }

private fun GamesUiState.UpcomingGame.titleResource(): StringResource =
    when (this) {
        GamesUiState.UpcomingGame.DualFocus -> AppResource.String.games_dual_focus_title
    }

private fun GamesUiState.UpcomingGame.descriptionResource(): StringResource =
    when (this) {
        GamesUiState.UpcomingGame.DualFocus -> AppResource.String.games_dual_focus_description
    }

@Preview(
    name = "Games — English",
    widthDp = 390,
    heightDp = 1400,
    locale = "en",
)
@ScreenshotPreview
@Composable
private fun GamesScreenPreview() {
    GamesScreenPreviewContent()
}

@Preview(
    name = "Games — Russian",
    widthDp = 390,
    heightDp = 1400,
    locale = "ru",
)
@ScreenshotPreview
@Composable
private fun GamesScreenRussianPreview() {
    GamesScreenPreviewContent()
}

@Preview(
    name = "Games — Kazakh",
    widthDp = 390,
    heightDp = 1400,
    locale = "kk",
)
@ScreenshotPreview
@Composable
private fun GamesScreenKazakhPreview() {
    GamesScreenPreviewContent()
}

@Preview(
    name = "Games — compact Kazakh",
    widthDp = 320,
    heightDp = 1200,
    locale = "kk",
)
@ScreenshotPreview
@Composable
private fun GamesScreenCompactKazakhPreview() {
    GamesScreenPreviewContent()
}

@Composable
private fun GamesScreenPreviewContent() {
    OquTurboTheme {
        GamesScreen(
            uiState = GamesUiState(),
            onGameClick = {},
        )
    }
}
