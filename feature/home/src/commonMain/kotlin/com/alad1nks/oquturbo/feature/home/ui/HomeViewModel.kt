package com.alad1nks.oquturbo.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.core.data.model.DailyTrainingPlan
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.DailyTrainingRepository
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

internal class HomeViewModel(
    activityRepository: GameActivityRepository,
    private val dailyTrainingRepository: DailyTrainingRepository,
) : ViewModel() {
    private var ensuredEpochDay: Long? = null
    private var isStartingTraining = false

    val uiState =
        combine(
            activityRepository.observeProgress(),
            activityRepository.observeSessions(),
            dailyTrainingRepository.observeTodayTraining(),
        ) { progress, sessions, dailyTraining ->
            HomeUiState(
                overallLevel = progress.level,
                rankNumber = ((progress.level - 1) / LEVELS_PER_RANK + 1).coerceAtMost(MAX_KNOWN_RANKS),
                levelProgress =
                    if (progress.xpPerLevel > 0) {
                        progress.currentLevelXp.toFloat() / progress.xpPerLevel
                    } else {
                        0f
                    },
                dailyTraining = dailyTraining?.toHomeDailyTraining(),
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

    init {
        viewModelScope.launch {
            while (true) {
                val currentDay = currentEpochDay()
                if (ensuredEpochDay != currentDay) {
                    try {
                        ensuredEpochDay = dailyTrainingRepository.ensureTodayTraining().epochDay
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        delay(HOME_STORAGE_RETRY_DELAY_MILLIS.milliseconds)
                    }
                    continue
                }
                delay(minOf(millisUntilNextUtcDay(), DAY_CHANGE_POLL_INTERVAL_MILLIS).milliseconds)
            }
        }
    }

    fun refreshDailyTraining() {
        viewModelScope.launch {
            try {
                ensuredEpochDay = dailyTrainingRepository.ensureTodayTraining().epochDay
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The periodic refresh loop retries when storage is available again.
            }
        }
    }

    fun startTraining(onStart: (DailyTrainingEntry) -> Unit) {
        if (isStartingTraining) return
        isStartingTraining = true
        viewModelScope.launch {
            try {
                dailyTrainingRepository.ensureTodayTraining().nextEntry?.let(onStart)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The background refresh loop retries and updates the card when storage is available again.
            } finally {
                isStartingTraining = false
            }
        }
    }

    private fun DailyTrainingPlan.toHomeDailyTraining(): HomeUiState.DailyTraining =
        HomeUiState.DailyTraining(
            items =
                entries.map { entry ->
                    HomeUiState.TrainingItem(
                        entry = entry,
                        game = entry.game.toHomeGame(),
                        mode = entry.mode.toHomeMode(),
                        requiredScore = entry.requiredScore,
                        isCompleted = entry.isCompleted,
                    )
                },
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
        const val DAY_CHANGE_POLL_INTERVAL_MILLIS = 60_000L
        const val HOME_STORAGE_RETRY_DELAY_MILLIS = 5_000L
        const val LEVELS_PER_RANK = 5
        const val MAX_RECENT_RECORDS = 3
        const val MAX_KNOWN_RANKS = 8
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MILLIS_PER_DAY = 86_400_000L
    }

    @OptIn(ExperimentalTime::class)
    private fun currentEpochDay(): Long = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY

    @OptIn(ExperimentalTime::class)
    private fun millisUntilNextUtcDay(): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return MILLIS_PER_DAY - now % MILLIS_PER_DAY + 1_000L
    }
}
