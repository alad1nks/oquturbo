package com.alad1nks.oquturbo.feature.profile.ui

internal data class ProfileUiState(
    val displayName: String? = null,
    val level: Int = 1,
    val currentLevelXp: Int = 0,
    val nextLevelXp: Int = 500,
    val ranks: List<Rank> = defaultRanks,
    val completedTrainings: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val titles: List<ProfileTitle> = emptyList(),
    val selectedTitle: TitleId? = null,
    val personalization: List<PersonalizationItem> = emptyList(),
    val recentUnlocks: List<RecentUnlock> = emptyList(),
    val showNewRankBanner: Boolean = false,
) {
    val normalizedLevel: Int = level.coerceAtLeast(1)
    val rankIndex: Int = ((normalizedLevel - 1) / LEVELS_PER_RANK).coerceAtMost(ranks.lastIndex.coerceAtLeast(0))
    val currentRank: Rank? = ranks.getOrNull(rankIndex)
    val currentRankFirstLevel: Int = rankIndex * LEVELS_PER_RANK + 1
    val currentRankLastLevel: Int = currentRankFirstLevel + LEVELS_PER_RANK - 1
    val isBeyondKnownRankRange: Boolean = ranks.isNotEmpty() && normalizedLevel > currentRankLastLevel
    val nextRank: Rank? = ranks.getOrNull(rankIndex + 1)
    val levelsUntilNextRank: Int? = nextRank?.let { (currentRankLastLevel - normalizedLevel + 1).coerceAtLeast(1) }
    val earnedAchievementsCount: Int = achievements.count { it.status == AchievementStatus.Earned }
    val xpProgress: Float =
        if (nextLevelXp > 0) {
            currentLevelXp.coerceIn(0, nextLevelXp).toFloat() / nextLevelXp
        } else {
            0f
        }

    data class Rank(val number: Int)

    data class Achievement(
        val id: AchievementId,
        val status: AchievementStatus,
        val currentProgress: Int = 0,
        val targetProgress: Int = 0,
        val earnedDate: EarnedDate? = null,
    )

    data class ProfileTitle(
        val id: TitleId,
        val isUnlocked: Boolean,
    )

    data class PersonalizationItem(
        val id: PersonalizationId,
        val category: PersonalizationCategory,
        val isUnlocked: Boolean,
        val unlockRequirement: UnlockRequirement? = null,
        val isSelected: Boolean = false,
    )

    sealed interface UnlockRequirement {
        data class Level(val value: Int) : UnlockRequirement

        data class Trainings(val value: Int) : UnlockRequirement
    }

    sealed interface RecentUnlock {
        data class Level(val value: Int) : RecentUnlock

        data class Rank(val number: Int) : RecentUnlock

        data class Title(val id: TitleId) : RecentUnlock

        data class Achievement(val id: AchievementId) : RecentUnlock

        data class Personalization(val id: PersonalizationId) : RecentUnlock
    }

    companion object {
        const val LEVELS_PER_RANK = 5
        val defaultRanks = List(size = 8) { index -> Rank(number = index + 1) }
    }
}

internal enum class AchievementId {
    FirstTraining,
    SevenDayStreak,
    MemoryMaster,
    CorrectAnswers,
    Hidden,
}

internal enum class AchievementStatus {
    Earned,
    InProgress,
    Hidden,
}

internal enum class EarnedDate {
    Today,
    Yesterday,
}

internal enum class TitleId {
    Starter,
    AttentionMaster,
    QuickLook,
}

internal enum class PersonalizationCategory {
    Avatar,
    AvatarFrame,
    CardBackground,
}

internal enum class PersonalizationId {
    DefaultAvatar,
    FocusAvatar,
    LightningAvatar,
    DefaultFrame,
    ExplorerFrame,
    DefaultBackground,
    TwilightBackground,
}
