package com.alad1nks.oquturbo.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class HomeViewModel(
    activityRepository: GameActivityRepository,
) : ViewModel() {
    val uiState =
        combine(
            activityRepository.observeProgress(),
            activityRepository.observeSessions(),
        ) { progress, sessions ->
            HomeUiState(
                overallLevel = progress.level,
                rankNumber = ((progress.level - 1) / LEVELS_PER_RANK + 1).coerceAtMost(MAX_KNOWN_RANKS),
                levelProgress =
                    if (progress.xpPerLevel > 0) {
                        progress.currentLevelXp.toFloat() / progress.xpPerLevel
                    } else {
                        0f
                    },
                recentRecords =
                    sessions
                        .asReversed()
                        .asSequence()
                        .filter { it.isNewRecord }
                        .take(MAX_RECENT_RECORDS)
                        .map { session ->
                            HomeUiState.RecentRecord(
                                game = session.game.toHomeGame(),
                                mode = session.mode.toHomeMode(),
                                variantId = session.variantId,
                                score = session.score,
                            )
                        }.toList(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState(),
        )

    private fun GameId.toHomeGame(): HomeUiState.Game =
        when (this) {
            GameId.NumberSprint -> HomeUiState.Game.NumberSprint
            GameId.WideEye -> HomeUiState.Game.WideEye
            GameId.DontTap -> HomeUiState.Game.DontTap
        }

    private fun GameModeId.toHomeMode(): HomeUiState.Mode =
        when (this) {
            GameModeId.NumberSprintClassic -> HomeUiState.Mode.Classic
            GameModeId.NumberSprintBinary -> HomeUiState.Mode.Binary
            GameModeId.NumberSprintCustom -> HomeUiState.Mode.Custom
            GameModeId.WideEyeCharacters -> HomeUiState.Mode.Characters
            GameModeId.WideEyeWords -> HomeUiState.Mode.Words
            GameModeId.WideEyeFindDifference -> HomeUiState.Mode.FindDifference
            GameModeId.WideEyeWideLine -> HomeUiState.Mode.WideLine
            GameModeId.DontTapCategories -> HomeUiState.Mode.Categories
            GameModeId.DontTapLetter -> HomeUiState.Mode.Letter
            GameModeId.DontTapWordLength -> HomeUiState.Mode.WordLength
            GameModeId.DontTapTextColor -> HomeUiState.Mode.TextColor
            GameModeId.DontTapTrueFalse -> HomeUiState.Mode.TrueFalse
            GameModeId.DontTapMath -> HomeUiState.Mode.Math
            GameModeId.DontTapSpeedReading -> HomeUiState.Mode.SpeedReading
        }

    private companion object {
        const val LEVELS_PER_RANK = 5
        const val MAX_RECENT_RECORDS = 3
        const val MAX_KNOWN_RANKS = 8
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
