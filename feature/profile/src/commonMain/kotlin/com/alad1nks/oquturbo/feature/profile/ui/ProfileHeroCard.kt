package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileHeroCard(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onRanksClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBackground =
        uiState.personalization.firstOrNull {
            it.category == PersonalizationCategory.CardBackground && it.isSelected
        }?.id
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selectedBackground == PersonalizationId.TwilightBackground) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 480.dp
                ProfileIdentity(
                    uiState = uiState,
                    onEditProfileClick = onEditProfileClick,
                    centered = isCompact,
                )
            }

            ProfileLevelProgress(uiState = uiState)

            uiState.currentRank?.let { rank ->
                RankSummary(
                    uiState = uiState,
                    rank = rank,
                    onClick = onRanksClick,
                )
            }
        }
    }
}

@Composable
private fun ProfileIdentity(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    centered: Boolean,
) {
    if (centered) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileAvatar(
                uiState = uiState,
                onEditProfileClick = onEditProfileClick,
            )
            ProfileIdentityText(
                uiState = uiState,
                onEditProfileClick = onEditProfileClick,
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                uiState = uiState,
                onEditProfileClick = onEditProfileClick,
            )
            ProfileIdentityText(
                uiState = uiState,
                onEditProfileClick = onEditProfileClick,
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
) {
    val selectedFrame =
        uiState.personalization.firstOrNull {
            it.category == PersonalizationCategory.AvatarFrame && it.isSelected
        }?.id
    Box {
        Surface(
            modifier = Modifier.size(88.dp).clickable(onClick = onEditProfileClick),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            border =
                BorderStroke(
                    width = if (selectedFrame == PersonalizationId.ExplorerFrame) 3.dp else 1.dp,
                    color =
                        if (selectedFrame == PersonalizationId.ExplorerFrame) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector =
                        uiState.personalization
                            .firstOrNull { it.category == PersonalizationCategory.Avatar && it.isSelected }
                            ?.id
                            ?.icon() ?: Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).size(30.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary,
        ) {
            IconButton(onClick = onEditProfileClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(AppResource.String.profile_edit_profile),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun ProfileIdentityText(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = uiState.displayName ?: stringResource(AppResource.String.profile_player_name),
            modifier = Modifier.clickable(onClick = onEditProfileClick),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        uiState.selectedTitle?.let { title ->
            Text(
                text = stringResource(title.titleResource()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileLevelProgress(uiState: ProfileUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(AppResource.String.profile_overall_level_format, uiState.normalizedLevel),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    stringResource(
                        AppResource.String.profile_xp_format,
                        uiState.currentLevelXp,
                        uiState.nextLevelXp,
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { uiState.xpProgress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.small),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text =
                stringResource(
                    AppResource.String.profile_xp_to_next_format,
                    (uiState.nextLevelXp - uiState.currentLevelXp).coerceAtLeast(0),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RankSummary(
    uiState: ProfileUiState,
    rank: ProfileUiState.Rank,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.profile_rank_neutral_format, rank.number),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!uiState.isBeyondKnownRankRange) {
                    Text(
                        text =
                            stringResource(
                                AppResource.String.profile_rank_range_format,
                                uiState.currentRankFirstLevel,
                                uiState.currentRankLastLevel,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val nextRank = uiState.nextRank
                val levelsUntilNextRank = uiState.levelsUntilNextRank
                if (nextRank != null) {
                    Text(
                        text =
                            stringResource(
                                AppResource.String.profile_next_rank_format,
                                stringResource(AppResource.String.profile_rank_neutral_format, nextRank.number),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text =
                        if (nextRank != null && levelsUntilNextRank != null) {
                            pluralStringResource(
                                AppResource.Plural.profile_levels_to_next_rank_format,
                                levelsUntilNextRank,
                                levelsUntilNextRank,
                            )
                        } else {
                            stringResource(AppResource.String.profile_highest_rank)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
