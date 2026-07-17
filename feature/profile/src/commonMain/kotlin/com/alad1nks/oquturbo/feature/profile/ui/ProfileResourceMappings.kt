package com.alad1nks.oquturbo.feature.profile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.alad1nks.oquturbo.resources.AppResource
import org.jetbrains.compose.resources.StringResource

internal fun AchievementId.titleResource(): StringResource =
    when (this) {
        AchievementId.FirstTraining -> AppResource.String.profile_achievement_first_training_title
        AchievementId.SevenDayStreak -> AppResource.String.profile_achievement_seven_days_title
        AchievementId.MemoryMaster -> AppResource.String.profile_achievement_memory_master_title
        AchievementId.CorrectAnswers -> AppResource.String.profile_achievement_correct_answers_title
        AchievementId.Hidden -> AppResource.String.profile_achievement_hidden_title
    }

internal fun AchievementId.conditionResource(): StringResource =
    when (this) {
        AchievementId.FirstTraining -> AppResource.String.profile_achievement_first_training_condition
        AchievementId.SevenDayStreak -> AppResource.String.profile_achievement_seven_days_condition
        AchievementId.MemoryMaster -> AppResource.String.profile_achievement_memory_master_condition
        AchievementId.CorrectAnswers -> AppResource.String.profile_achievement_correct_answers_condition
        AchievementId.Hidden -> AppResource.String.profile_achievement_hidden_condition
    }

internal fun AchievementId.icon(): ImageVector =
    when (this) {
        AchievementId.FirstTraining -> Icons.Filled.Bolt
        AchievementId.SevenDayStreak -> Icons.Filled.AutoAwesome
        AchievementId.MemoryMaster -> Icons.Filled.Psychology
        AchievementId.CorrectAnswers -> Icons.Filled.EmojiEvents
        AchievementId.Hidden -> Icons.AutoMirrored.Filled.Help
    }

internal fun AchievementStatus.labelResource(): StringResource =
    when (this) {
        AchievementStatus.Earned -> AppResource.String.profile_achievement_earned
        AchievementStatus.InProgress -> AppResource.String.profile_achievement_in_progress
        AchievementStatus.Hidden -> AppResource.String.profile_achievement_hidden
    }

internal fun EarnedDate.labelResource(): StringResource =
    when (this) {
        EarnedDate.Today -> AppResource.String.profile_date_today
        EarnedDate.Yesterday -> AppResource.String.profile_date_yesterday
    }

internal fun TitleId.titleResource(): StringResource =
    when (this) {
        TitleId.Starter -> AppResource.String.profile_title_starter
        TitleId.AttentionMaster -> AppResource.String.profile_title_attention_master
        TitleId.QuickLook -> AppResource.String.profile_title_quick_look
    }

internal fun PersonalizationCategory.titleResource(): StringResource =
    when (this) {
        PersonalizationCategory.Avatar -> AppResource.String.profile_avatar
        PersonalizationCategory.AvatarFrame -> AppResource.String.profile_avatar_frame
        PersonalizationCategory.CardBackground -> AppResource.String.profile_card_background
    }

internal fun PersonalizationId.titleResource(): StringResource =
    when (this) {
        PersonalizationId.DefaultAvatar -> AppResource.String.profile_avatar_default
        PersonalizationId.FocusAvatar -> AppResource.String.profile_avatar_focus
        PersonalizationId.LightningAvatar -> AppResource.String.profile_avatar_lightning
        PersonalizationId.DefaultFrame -> AppResource.String.profile_frame_default
        PersonalizationId.ExplorerFrame -> AppResource.String.profile_frame_explorer
        PersonalizationId.DefaultBackground -> AppResource.String.profile_background_default
        PersonalizationId.TwilightBackground -> AppResource.String.profile_background_twilight
    }

internal fun PersonalizationId.icon(): ImageVector =
    when (this) {
        PersonalizationId.DefaultAvatar -> Icons.Filled.Person
        PersonalizationId.FocusAvatar -> Icons.Filled.Visibility
        PersonalizationId.LightningAvatar -> Icons.Filled.Bolt
        PersonalizationId.DefaultFrame -> Icons.Filled.EmojiEvents
        PersonalizationId.ExplorerFrame -> Icons.Filled.AutoAwesome
        PersonalizationId.DefaultBackground -> Icons.Filled.Person
        PersonalizationId.TwilightBackground -> Icons.Filled.AutoAwesome
    }
