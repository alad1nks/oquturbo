package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alad1nks.oquturbo.core.data.practice.practiceDate
import com.alad1nks.oquturbo.core.ui.component.AppCard
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun currentPracticeValue(state: ProfilePracticeState): String =
    when (state) {
        ProfilePracticeState.Loading -> stringResource(AppResource.String.practice_loading_short)
        ProfilePracticeState.Error -> stringResource(AppResource.String.practice_unavailable_short)
        is ProfilePracticeState.Ready ->
            when {
                state.rhythm.currentStreakIsExact ->
                    pluralStringResource(
                        AppResource.Plural.profile_days_format,
                        state.rhythm.currentStreakDays,
                        state.rhythm.currentStreakDays,
                    )
                state.rhythm.currentStreakDays > 0 ->
                    pluralStringResource(
                        AppResource.Plural.practice_at_least_days,
                        state.rhythm.currentStreakDays,
                        state.rhythm.currentStreakDays,
                    )
                else -> stringResource(AppResource.String.practice_insufficient_history)
            }
    }

@Composable
internal fun bestPracticeValue(state: ProfilePracticeState): String =
    when (state) {
        ProfilePracticeState.Loading -> stringResource(AppResource.String.practice_loading_short)
        ProfilePracticeState.Error -> stringResource(AppResource.String.practice_unavailable_short)
        is ProfilePracticeState.Ready ->
            pluralStringResource(
                AppResource.Plural.profile_days_format,
                state.rhythm.bestRecordedStreakDays,
                state.rhythm.bestRecordedStreakDays,
            )
    }

@Composable
internal fun PracticeProfileContext(state: ProfilePracticeState, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            ProfilePracticeState.Loading ->
                PracticeProfileText(
                    stringResource(AppResource.String.practice_history_loading),
                )
            ProfilePracticeState.Error -> {
                PracticeProfileText(stringResource(AppResource.String.practice_history_error))
                PracticeHistoryRetry(onRetry)
            }
            is ProfilePracticeState.Ready -> {
                if (state.rhythm.bestRecordedStreakDays == 0) {
                    PracticeProfileText(stringResource(AppResource.String.practice_no_confirmed_streak))
                }
                val date = practiceDate(state.rhythm.trackingStartedEpochDay)
                PracticeProfileText(
                    stringResource(
                        AppResource.String.practice_profile_scope,
                        stringResource(
                            AppResource.String.practice_date_format,
                            date.yearText,
                            date.monthText,
                            date.dayText,
                        ),
                    ),
                )
                if (state.rhythm.hasFutureFacts) {
                    PracticeProfileText(
                        stringResource(AppResource.String.practice_future_dates),
                    )
                }
            }
        }
        if (state !is ProfilePracticeState.Ready) {
            PracticeProfileText(stringResource(AppResource.String.practice_achievements_partial))
        }
    }
}

@Composable
internal fun SevenDayAchievementCard(
    achievement: ProfileUiState.Achievement,
    history: ProfilePracticeState,
    onRetry: () -> Unit,
    showFutureNote: Boolean,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier.fillMaxWidth(), compact = true) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val narrow = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.2f
            val icon: @Composable () -> Unit = {
                Surface(
                    Modifier.size(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AchievementId.SevenDayStreak.icon(), null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            val content: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(AchievementId.SevenDayStreak.titleResource()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    AchievementStatusLabel(achievement.status)
                    PracticeProfileText(stringResource(AchievementId.SevenDayStreak.conditionResource()))
                    when (achievement.status) {
                        AchievementStatus.Loading ->
                            PracticeProfileText(
                                stringResource(AppResource.String.practice_history_loading),
                            )
                        AchievementStatus.Unavailable -> {
                            PracticeProfileText(stringResource(AppResource.String.practice_history_error))
                            PracticeHistoryRetry(onRetry)
                        }
                        AchievementStatus.Earned -> {
                            PracticeProfileText(stringResource(AppResource.String.practice_earned_evidence))
                            PracticeProfileText(stringResource(AppResource.String.practice_saved_scope))
                        }
                        AchievementStatus.InProgress -> {
                            PracticeProfileText(stringResource(AppResource.String.practice_best_progress))
                            LinearProgressIndicator(
                                progress = { achievement.currentProgress.coerceIn(0, 7) / 7f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                            )
                            Text(
                                stringResource(
                                    AppResource.String.profile_progress_format,
                                    achievement.currentProgress,
                                    7,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            PracticeProfileText(stringResource(AppResource.String.practice_saved_scope))
                        }
                        AchievementStatus.Hidden -> Unit
                    }
                    if (showFutureNote && (history as? ProfilePracticeState.Ready)?.rhythm?.hasFutureFacts == true) {
                        PracticeProfileText(stringResource(AppResource.String.practice_future_dates))
                    }
                }
            }
            if (narrow) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    icon()
                    content()
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    icon()
                    Box(Modifier.weight(1f)) { content() }
                }
            }
        }
    }
}

@Composable
private fun PracticeProfileText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PracticeHistoryRetry(onRetry: () -> Unit) {
    TextButton(
        onClick = onRetry,
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(AppResource.String.practice_history_retry), textAlign = TextAlign.Center)
    }
}
