package com.alad1nks.oquturbo.feature.profile.ui

internal object ProfileDemoData {
    val newUser =
        ProfileUiState(
            achievements =
                listOf(
                    achievement(AchievementId.FirstTraining, progress = 0, target = 1),
                    hiddenAchievement(),
                ),
            titles = listOf(ProfileUiState.ProfileTitle(TitleId.Starter, isUnlocked = true)),
            selectedTitle = TitleId.Starter,
            personalization = starterPersonalization(),
        )

    val midRank =
        returningUser(
            level = 17,
            currentXp = 340,
            nextXp = 500,
        )

    val nearNextRank =
        returningUser(
            level = 20,
            currentXp = 480,
            nextXp = 500,
        )

    val newRank =
        returningUser(
            level = 21,
            currentXp = 45,
            nextXp = 550,
        ).copy(
            showNewRankBanner = true,
            recentUnlocks =
                listOf(
                    ProfileUiState.RecentUnlock.Rank(number = 5),
                    ProfileUiState.RecentUnlock.Level(value = 21),
                ),
        )

    val mixedAchievements =
        midRank.copy(
            achievements = mixedAchievementItems(),
        )

    val noExtraTitles =
        midRank.copy(
            titles = listOf(ProfileUiState.ProfileTitle(TitleId.Starter, isUnlocked = true)),
            selectedTitle = TitleId.Starter,
        )

    val customized =
        midRank.copy(
            displayName = "Alexandra Turboplayer",
            personalization = customizedPersonalization(),
            recentUnlocks =
                listOf(
                    ProfileUiState.RecentUnlock.Personalization(PersonalizationId.ExplorerFrame),
                    ProfileUiState.RecentUnlock.Title(TitleId.QuickLook),
                    ProfileUiState.RecentUnlock.Achievement(AchievementId.SevenDayStreak),
                ),
        )

    val appDemo =
        returningUser(
            level = 37,
            currentXp = 340,
            nextXp = 500,
        ).copy(
            ranks = ProfileUiState.defaultRanks,
            achievements = mixedAchievementItems(),
            personalization = customizedPersonalization(),
        )

    val returningUser =
        midRank.copy(
            achievements = mixedAchievementItems(),
            personalization = customizedPersonalization(),
        )

    private fun returningUser(
        level: Int,
        currentXp: Int,
        nextXp: Int,
    ) =
        ProfileUiState(
            displayName = "User",
            level = level,
            currentLevelXp = currentXp,
            nextLevelXp = nextXp,
            completedTrainings = 26,
            currentStreakDays = 8,
            bestStreakDays = 14,
            achievements = mixedAchievementItems(),
            titles =
                listOf(
                    ProfileUiState.ProfileTitle(TitleId.Starter, isUnlocked = true),
                    ProfileUiState.ProfileTitle(TitleId.AttentionMaster, isUnlocked = true),
                    ProfileUiState.ProfileTitle(TitleId.QuickLook, isUnlocked = false),
                ),
            selectedTitle = TitleId.AttentionMaster,
            personalization = starterPersonalization(),
            recentUnlocks =
                listOf(
                    ProfileUiState.RecentUnlock.Level(value = level),
                    ProfileUiState.RecentUnlock.Achievement(AchievementId.SevenDayStreak),
                ),
        )

    private fun mixedAchievementItems() =
        listOf(
            ProfileUiState.Achievement(
                id = AchievementId.FirstTraining,
                status = AchievementStatus.Earned,
                currentProgress = 1,
                targetProgress = 1,
                earnedDate = EarnedDate.Yesterday,
            ),
            achievement(AchievementId.CorrectAnswers, progress = 720, target = 1000),
            achievement(AchievementId.MemoryMaster, progress = 2, target = 3),
            hiddenAchievement(),
            ProfileUiState.Achievement(
                id = AchievementId.SevenDayStreak,
                status = AchievementStatus.Earned,
                currentProgress = 7,
                targetProgress = 7,
                earnedDate = EarnedDate.Today,
            ),
        )

    private fun achievement(
        id: AchievementId,
        progress: Int,
        target: Int,
    ) =
        ProfileUiState.Achievement(
            id = id,
            status = AchievementStatus.InProgress,
            currentProgress = progress,
            targetProgress = target,
        )

    private fun hiddenAchievement() =
        ProfileUiState.Achievement(
            id = AchievementId.Hidden,
            status = AchievementStatus.Hidden,
        )

    private fun starterPersonalization() =
        listOf(
            ProfileUiState.PersonalizationItem(
                id = PersonalizationId.DefaultAvatar,
                category = PersonalizationCategory.Avatar,
                isUnlocked = true,
                isSelected = true,
            ),
            ProfileUiState.PersonalizationItem(
                id = PersonalizationId.FocusAvatar,
                category = PersonalizationCategory.Avatar,
                isUnlocked = false,
                unlockRequirement = ProfileUiState.UnlockRequirement.Level(value = 10),
            ),
            ProfileUiState.PersonalizationItem(
                id = PersonalizationId.DefaultFrame,
                category = PersonalizationCategory.AvatarFrame,
                isUnlocked = true,
                isSelected = true,
            ),
            ProfileUiState.PersonalizationItem(
                id = PersonalizationId.ExplorerFrame,
                category = PersonalizationCategory.AvatarFrame,
                isUnlocked = false,
                unlockRequirement = ProfileUiState.UnlockRequirement.Trainings(value = 10),
            ),
            ProfileUiState.PersonalizationItem(
                id = PersonalizationId.DefaultBackground,
                category = PersonalizationCategory.CardBackground,
                isUnlocked = true,
                isSelected = true,
            ),
        )

    private fun customizedPersonalization() =
        starterPersonalization().map { item ->
            if (item.id == PersonalizationId.ExplorerFrame) {
                item.copy(
                    isUnlocked = true,
                    unlockRequirement = null,
                )
            } else {
                item
            }
        } +
            listOf(
                ProfileUiState.PersonalizationItem(
                    id = PersonalizationId.LightningAvatar,
                    category = PersonalizationCategory.Avatar,
                    isUnlocked = true,
                ),
                ProfileUiState.PersonalizationItem(
                    id = PersonalizationId.TwilightBackground,
                    category = PersonalizationCategory.CardBackground,
                    isUnlocked = true,
                ),
            )
}
