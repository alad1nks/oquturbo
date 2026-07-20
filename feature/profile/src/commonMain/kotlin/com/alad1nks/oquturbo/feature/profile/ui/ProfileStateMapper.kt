package com.alad1nks.oquturbo.feature.profile.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.DailyTrainingProgress
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameRecord
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.PlayerProgress
import com.alad1nks.oquturbo.core.data.model.ProfilePreferences
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal fun createProfileUiState(
    progress: PlayerProgress,
    records: List<GameRecord>,
    preferences: ProfilePreferences,
    sessions: List<GameSession>,
    todayTraining: DailyTrainingPlan?,
    trainingProgress: DailyTrainingProgress,
): ProfileUiState {
    val memoryModes = records.filter { it.game == GameId.NumberSprint }.map { it.mode }.distinct().size
    val wideEyeModes = records.filter { it.game == GameId.WideEye }.map { it.mode }.distinct().size
    val titles =
        listOf(
            ProfileUiState.ProfileTitle(TitleId.Starter, isUnlocked = true),
            ProfileUiState.ProfileTitle(TitleId.AttentionMaster, isUnlocked = progress.level >= ATTENTION_TITLE_LEVEL),
            ProfileUiState.ProfileTitle(TitleId.QuickLook, isUnlocked = wideEyeModes >= WIDE_EYE_TITLE_MODES),
        )
    val selectedTitle =
        enumValueOrNull<TitleId>(preferences.selectedTitle)
            ?.takeIf { selected -> titles.any { it.id == selected && it.isUnlocked } }
            ?: TitleId.Starter
    val recentProgress = recentProgress(progress, sessions.lastOrNull())
    val isTodayTrainingCompleted = todayTraining?.isCompleted == true

    return ProfileUiState(
        displayName = preferences.displayName,
        level = progress.level,
        currentLevelXp = progress.currentLevelXp,
        nextLevelXp = progress.xpPerLevel,
        completedTrainings = trainingProgress.totalCompletedTrainings,
        hasGameActivity = sessions.isNotEmpty() || progress.totalCorrectAnswers > 0 || records.isNotEmpty(),
        currentStreakDays = 0,
        bestStreakDays = 0,
        achievements =
            achievements(
                correctAnswers = progress.totalCorrectAnswers,
                memoryModes = memoryModes,
                completedTrainings = trainingProgress.totalCompletedTrainings,
                isTodayTrainingCompleted = isTodayTrainingCompleted,
                recentProgress = recentProgress,
            ),
        titles = titles,
        selectedTitle = selectedTitle,
        personalization = personalization(progress.level, preferences),
        recentUnlocks = recentProgress.unlocks,
        showNewRankBanner = recentProgress.rankChanged,
    )
}

private fun achievements(
    correctAnswers: Int,
    memoryModes: Int,
    completedTrainings: Int,
    isTodayTrainingCompleted: Boolean,
    recentProgress: RecentProgress,
): List<ProfileUiState.Achievement> =
    listOf(
        progressAchievement(
            AchievementId.FirstTraining,
            current = completedTrainings.coerceAtLeast(if (isTodayTrainingCompleted) 1 else 0),
            target = 1,
        ),
        progressAchievement(AchievementId.SevenDayStreak, current = 0, target = 7),
        progressAchievement(AchievementId.MemoryMaster, current = memoryModes, target = MEMORY_MASTER_MODES),
        progressAchievement(
            AchievementId.CorrectAnswers,
            current = correctAnswers,
            target = CORRECT_ANSWERS_TARGET,
            earnedDate = recentProgress.correctAnswersEarnedDate,
        ),
        ProfileUiState.Achievement(
            id = AchievementId.Hidden,
            status = AchievementStatus.Hidden,
        ),
    )

private fun progressAchievement(
    id: AchievementId,
    current: Int,
    target: Int,
    earnedDate: EarnedDate? = null,
) = ProfileUiState.Achievement(
    id = id,
    status = if (current >= target) AchievementStatus.Earned else AchievementStatus.InProgress,
    currentProgress = current.coerceAtMost(target),
    targetProgress = target,
    earnedDate = earnedDate,
)

@OptIn(ExperimentalTime::class)
private fun recentProgress(
    progress: PlayerProgress,
    latestSession: GameSession?,
): RecentProgress {
    latestSession ?: return RecentProgress()
    val previousXp = (progress.totalXp - latestSession.correctAnswers).coerceAtLeast(0)
    val previousLevel = previousXp / progress.xpPerLevel.coerceAtLeast(1) + 1
    val currentRank = rankNumber(progress.level)
    val previousRank = rankNumber(previousLevel)
    val rankChanged = currentRank > previousRank
    val correctAnswersAchievementEarned =
        progress.totalCorrectAnswers >= CORRECT_ANSWERS_TARGET &&
            progress.totalCorrectAnswers - latestSession.correctAnswers < CORRECT_ANSWERS_TARGET
    val correctAnswersEarnedDate =
        if (correctAnswersAchievementEarned) {
            when (Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY - latestSession.completedEpochDay) {
                0L -> EarnedDate.Today
                1L -> EarnedDate.Yesterday
                else -> null
            }
        } else {
            null
        }
    val unlocks =
        buildList {
            if (rankChanged) add(ProfileUiState.RecentUnlock.Rank(currentRank))
            if (progress.level > previousLevel) add(ProfileUiState.RecentUnlock.Level(progress.level))
            if (correctAnswersAchievementEarned) {
                add(ProfileUiState.RecentUnlock.Achievement(AchievementId.CorrectAnswers))
            }
            if (previousLevel < ATTENTION_TITLE_LEVEL && progress.level >= ATTENTION_TITLE_LEVEL) {
                add(ProfileUiState.RecentUnlock.Title(TitleId.AttentionMaster))
            }
            if (previousLevel < FOCUS_AVATAR_LEVEL && progress.level >= FOCUS_AVATAR_LEVEL) {
                add(ProfileUiState.RecentUnlock.Personalization(PersonalizationId.FocusAvatar))
            }
            if (previousLevel < LIGHTNING_AVATAR_LEVEL && progress.level >= LIGHTNING_AVATAR_LEVEL) {
                add(ProfileUiState.RecentUnlock.Personalization(PersonalizationId.LightningAvatar))
            }
            if (previousLevel < TWILIGHT_BACKGROUND_LEVEL && progress.level >= TWILIGHT_BACKGROUND_LEVEL) {
                add(ProfileUiState.RecentUnlock.Personalization(PersonalizationId.TwilightBackground))
            }
        }.take(MAX_RECENT_UNLOCKS)
    return RecentProgress(
        unlocks = unlocks,
        rankChanged = rankChanged,
        correctAnswersEarnedDate = correctAnswersEarnedDate,
    )
}

private fun rankNumber(level: Int): Int =
    ((level.coerceAtLeast(1) - 1) / ProfileUiState.LEVELS_PER_RANK + 1)
        .coerceAtMost(ProfileUiState.defaultRanks.size)

private data class RecentProgress(
    val unlocks: List<ProfileUiState.RecentUnlock> = emptyList(),
    val rankChanged: Boolean = false,
    val correctAnswersEarnedDate: EarnedDate? = null,
)

private fun personalization(
    level: Int,
    preferences: ProfilePreferences,
): List<ProfileUiState.PersonalizationItem> {
    val unlockedAvatars =
        setOfNotNull(
            PersonalizationId.DefaultAvatar,
            PersonalizationId.FocusAvatar.takeIf { level >= FOCUS_AVATAR_LEVEL },
            PersonalizationId.LightningAvatar.takeIf { level >= LIGHTNING_AVATAR_LEVEL },
        )
    val selectedAvatar =
        enumValueOrNull<PersonalizationId>(preferences.selectedAvatar)
            ?.takeIf(unlockedAvatars::contains)
            ?: PersonalizationId.DefaultAvatar
    val selectedFrame =
        enumValueOrNull<PersonalizationId>(preferences.selectedFrame)
            ?.takeIf { it == PersonalizationId.DefaultFrame }
            ?: PersonalizationId.DefaultFrame
    val unlockedBackgrounds =
        setOfNotNull(
            PersonalizationId.DefaultBackground,
            PersonalizationId.TwilightBackground.takeIf { level >= TWILIGHT_BACKGROUND_LEVEL },
        )
    val selectedBackground =
        enumValueOrNull<PersonalizationId>(preferences.selectedBackground)
            ?.takeIf(unlockedBackgrounds::contains)
            ?: PersonalizationId.DefaultBackground

    return listOf(
        personalizationItem(
            id = PersonalizationId.DefaultAvatar,
            category = PersonalizationCategory.Avatar,
            isUnlocked = true,
            isSelected = selectedAvatar == PersonalizationId.DefaultAvatar,
        ),
        personalizationItem(
            id = PersonalizationId.FocusAvatar,
            category = PersonalizationCategory.Avatar,
            isUnlocked = level >= FOCUS_AVATAR_LEVEL,
            requirement = ProfileUiState.UnlockRequirement.Level(FOCUS_AVATAR_LEVEL),
            isSelected = selectedAvatar == PersonalizationId.FocusAvatar,
        ),
        personalizationItem(
            id = PersonalizationId.LightningAvatar,
            category = PersonalizationCategory.Avatar,
            isUnlocked = level >= LIGHTNING_AVATAR_LEVEL,
            requirement = ProfileUiState.UnlockRequirement.Level(LIGHTNING_AVATAR_LEVEL),
            isSelected = selectedAvatar == PersonalizationId.LightningAvatar,
        ),
        personalizationItem(
            id = PersonalizationId.DefaultFrame,
            category = PersonalizationCategory.AvatarFrame,
            isUnlocked = true,
            isSelected = selectedFrame == PersonalizationId.DefaultFrame,
        ),
        personalizationItem(
            id = PersonalizationId.ExplorerFrame,
            category = PersonalizationCategory.AvatarFrame,
            isUnlocked = false,
            requirement = ProfileUiState.UnlockRequirement.Trainings(EXPLORER_FRAME_TRAININGS),
        ),
        personalizationItem(
            id = PersonalizationId.DefaultBackground,
            category = PersonalizationCategory.CardBackground,
            isUnlocked = true,
            isSelected = selectedBackground == PersonalizationId.DefaultBackground,
        ),
        personalizationItem(
            id = PersonalizationId.TwilightBackground,
            category = PersonalizationCategory.CardBackground,
            isUnlocked = level >= TWILIGHT_BACKGROUND_LEVEL,
            requirement = ProfileUiState.UnlockRequirement.Level(TWILIGHT_BACKGROUND_LEVEL),
            isSelected = selectedBackground == PersonalizationId.TwilightBackground,
        ),
    )
}

private fun personalizationItem(
    id: PersonalizationId,
    category: PersonalizationCategory,
    isUnlocked: Boolean,
    requirement: ProfileUiState.UnlockRequirement? = null,
    isSelected: Boolean = false,
) = ProfileUiState.PersonalizationItem(
    id = id,
    category = category,
    isUnlocked = isUnlocked,
    unlockRequirement = requirement.takeUnless { isUnlocked },
    isSelected = isSelected,
)

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }

private const val ATTENTION_TITLE_LEVEL = 10
private const val CORRECT_ANSWERS_TARGET = 1_000
private const val EXPLORER_FRAME_TRAININGS = 10
private const val FOCUS_AVATAR_LEVEL = 10
private const val LIGHTNING_AVATAR_LEVEL = 20
private const val MAX_RECENT_UNLOCKS = 3
private const val MEMORY_MASTER_MODES = 3
private const val MILLIS_PER_DAY = 86_400_000L
private const val TWILIGHT_BACKGROUND_LEVEL = 15
private const val WIDE_EYE_TITLE_MODES = 4
