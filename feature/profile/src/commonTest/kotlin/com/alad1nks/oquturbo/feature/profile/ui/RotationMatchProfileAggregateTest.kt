package com.alad1nks.oquturbo.feature.profile.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingProgress
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.PlayerProgress
import com.alad1nks.oquturbo.core.data.model.ProfilePreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotationMatchProfileAggregateTest {
    @Test
    fun rotationMatchAggregateProgressReachesProfileWithoutGameSpecificAchievement() {
        val correctAnswers = 12
        val state =
            createProfileUiState(
                progress =
                    PlayerProgress(
                        totalCorrectAnswers = correctAnswers,
                        totalXp = correctAnswers,
                        level = 1,
                        currentLevelXp = correctAnswers,
                    ),
                records = emptyList(),
                preferences = ProfilePreferences(),
                sessions =
                    listOf(
                        GameSession(
                            game = GameId.RotationMatch,
                            mode = GameModeId.RotationMatchRotation,
                            score = correctAnswers,
                            correctAnswers = correctAnswers,
                            durationMillis = 12_000,
                            completedAtEpochMillis = 1,
                            completedEpochDay = 0,
                            isNewRecord = true,
                        ),
                    ),
                todayTraining = null,
                trainingProgress = DailyTrainingProgress(),
            )

        assertTrue(state.hasGameActivity)
        assertEquals(1, state.level)
        assertEquals(correctAnswers, state.currentLevelXp)
        assertEquals(
            correctAnswers,
            state.achievements.single { it.id == AchievementId.CorrectAnswers }.currentProgress,
        )
        assertEquals(
            0,
            state.achievements.single { it.id == AchievementId.MemoryMaster }.currentProgress,
        )
    }
}
