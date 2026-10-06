package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.core.ui.component.AppCardTone
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NewRankBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(AppResource.String.profile_new_rank_banner),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
internal fun ProfileSummarySection(
    uiState: ProfileUiState,
    onStatsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onRetryPracticeHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(AppResource.String.profile_progress_summary_title),
            style = MaterialTheme.typography.titleLarge,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minimum = 150.dp * LocalDensity.current.fontScale
            val columns =
                when {
                    maxWidth >= minimum * 4 + 30.dp -> 4
                    maxWidth >= minimum * 2 + 10.dp -> 2
                    else -> 1
                }
            val facts =
                listOf(
                    SummaryFactData(
                        value = uiState.completedTrainings.toString(),
                        label = { stringResource(AppResource.String.profile_completed_trainings) },
                        icon = Icons.Filled.School,
                        onClick = onStatsClick,
                    ),
                    SummaryFactData(
                        value = currentPracticeValue(uiState.practiceHistory),
                        words =
                            uiState.practiceHistory !is ProfilePracticeState.Ready ||
                                (
                                    !uiState.practiceHistory.rhythm.currentStreakIsExact &&
                                        uiState.currentStreakDays == 0
                                ),
                        label = { stringResource(AppResource.String.profile_current_streak) },
                        icon = Icons.Filled.LocalFireDepartment,
                        onClick = onStatsClick,
                    ),
                    SummaryFactData(
                        value = bestPracticeValue(uiState.practiceHistory),
                        words = uiState.practiceHistory !is ProfilePracticeState.Ready,
                        label = { stringResource(AppResource.String.profile_best_streak) },
                        icon = Icons.Filled.WorkspacePremium,
                        onClick = onStatsClick,
                    ),
                    SummaryFactData(
                        value =
                            if (uiState.practiceHistory is ProfilePracticeState.Ready) {
                                uiState.earnedAchievementsCount.toString()
                            } else {
                                stringResource(
                                    AppResource.String.practice_at_least_count,
                                    uiState.earnedAchievementsCount,
                                )
                            },
                        label = { stringResource(AppResource.String.profile_achievements_count) },
                        icon = Icons.Filled.EmojiEvents,
                        onClick = onAchievementsClick,
                    ),
                )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                facts.chunked(columns).forEach { rowFacts ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowFacts.forEach { fact ->
                            SummaryFact(
                                fact = fact,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowFacts.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        PracticeProfileContext(uiState.practiceHistory, onRetryPracticeHistory)
    }
}

private data class SummaryFactData(
    val value: String,
    val label: @Composable () -> String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val words: Boolean = false,
)

@Composable
private fun SummaryFact(
    fact: SummaryFactData,
    modifier: Modifier = Modifier,
) {
    AppCard(
        onClick = fact.onClick,
        modifier = modifier.fillMaxHeight(),
        compact = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = fact.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = fact.value,
                style = if (fact.words) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = fact.label(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
            )
        }
    }
}

@Composable
internal fun ProfileAchievementsSection(
    achievements: List<ProfileUiState.Achievement>,
    onAllAchievementsClick: () -> Unit,
    practiceHistory: ProfilePracticeState = ProfilePracticeState.Loading,
    onRetryPracticeHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = stringResource(AppResource.String.profile_achievements_title),
            action = stringResource(AppResource.String.profile_all_achievements),
            onActionClick = onAllAchievementsClick,
        )
        achievements.take(3).forEach { achievement ->
            AchievementCard(
                achievement = achievement,
                history = practiceHistory,
                onRetryPracticeHistory = onRetryPracticeHistory,
            )
        }
    }
}

@Composable
internal fun AchievementCard(
    achievement: ProfileUiState.Achievement,
    modifier: Modifier = Modifier,
    history: ProfilePracticeState = ProfilePracticeState.Loading,
    onRetryPracticeHistory: () -> Unit = {},
    showFutureNote: Boolean = false,
) {
    if (achievement.id == AchievementId.SevenDayStreak) {
        SevenDayAchievementCard(achievement, history, onRetryPracticeHistory, showFutureNote, modifier)
        return
    }
    AppCard(
        modifier = modifier.fillMaxWidth(),
        compact = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                color =
                    if (achievement.status == AchievementStatus.Hidden) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector =
                            if (achievement.status == AchievementStatus.Hidden) {
                                Icons.Filled.Lock
                            } else {
                                achievement.id.icon()
                            },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = stringResource(achievement.id.titleResource()),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    AchievementStatusLabel(status = achievement.status)
                }
                Text(
                    text = stringResource(achievement.id.conditionResource()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (achievement.status == AchievementStatus.InProgress && achievement.targetProgress > 0) {
                    LinearProgressIndicator(
                        progress = {
                            achievement.currentProgress.coerceIn(0, achievement.targetProgress).toFloat() /
                                achievement.targetProgress
                        },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text =
                            stringResource(
                                AppResource.String.profile_progress_format,
                                achievement.currentProgress,
                                achievement.targetProgress,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                achievement.earnedDate?.let { date ->
                    Text(
                        text =
                            stringResource(
                                AppResource.String.profile_received_date_format,
                                stringResource(date.labelResource()),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AchievementStatusLabel(status: AchievementStatus) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color =
            if (status == AchievementStatus.Earned) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
    ) {
        Text(
            text = stringResource(status.labelResource()),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color =
                if (status == AchievementStatus.Earned) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
internal fun ProfileTitleSection(
    selectedTitle: TitleId?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        tone = AppCardTone.Secondary,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.profile_current_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text =
                        selectedTitle?.let { stringResource(it.titleResource()) }
                            ?: stringResource(AppResource.String.profile_no_titles),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(AppResource.String.profile_choose_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
internal fun ProfilePersonalizationSection(
    items: List<ProfileUiState.PersonalizationItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(AppResource.String.profile_personalization_title),
            style = MaterialTheme.typography.titleLarge,
        )
        AppCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(AppResource.String.profile_personalization_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items.filter { it.isUnlocked && it.isSelected }.take(3).forEach { item ->
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(
                                    imageVector = item.id.icon(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(item.category.titleResource()),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(AppResource.String.profile_personalization_open))
                }
            }
        }
    }
}

@Composable
internal fun ProfileRecentlyUnlockedSection(
    recentUnlocks: List<ProfileUiState.RecentUnlock>,
    modifier: Modifier = Modifier,
) {
    if (recentUnlocks.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(AppResource.String.profile_recently_unlocked),
            style = MaterialTheme.typography.titleLarge,
        )
        AppCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                recentUnlocks.take(3).forEach { unlock ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = recentUnlockText(unlock),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun recentUnlockText(unlock: ProfileUiState.RecentUnlock): String =
    when (unlock) {
        is ProfileUiState.RecentUnlock.Level ->
            stringResource(AppResource.String.profile_recent_level_format, unlock.value)

        is ProfileUiState.RecentUnlock.Rank -> {
            val rankNames = stringArrayResource(AppResource.Array.profile_rank_names)
            stringResource(
                AppResource.String.profile_recent_rank_format,
                rankNames.getOrNull(unlock.number - 1)
                    ?: stringResource(AppResource.String.profile_rank_neutral_format, unlock.number),
            )
        }

        is ProfileUiState.RecentUnlock.Title ->
            stringResource(
                AppResource.String.profile_recent_title_format,
                stringResource(unlock.id.titleResource()),
            )

        is ProfileUiState.RecentUnlock.Achievement ->
            stringResource(
                AppResource.String.profile_recent_achievement_format,
                stringResource(unlock.id.titleResource()),
            )

        is ProfileUiState.RecentUnlock.Personalization ->
            stringResource(
                AppResource.String.profile_recent_personalization_format,
                stringResource(unlock.id.titleResource()),
            )
    }

@Composable
internal fun NewUserHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    ) {
        Text(
            text = stringResource(AppResource.String.profile_start_training_hint),
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onActionClick) {
            Text(action)
        }
    }
}
