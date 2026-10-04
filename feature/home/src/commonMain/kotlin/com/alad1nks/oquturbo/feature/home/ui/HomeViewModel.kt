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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class HomeViewModel(
    activityRepository: GameActivityRepository,
    private val dailyTrainingRepository: DailyTrainingRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private data class TrainingState(
        val plan: DailyTrainingPlan? = null,
        val failed: Boolean = false,
        val starting: Boolean = false,
    )

    private val trainingState = MutableStateFlow(TrainingState())
    private var operationPending = false
    private var navigationPending = false
    private var lastActivityState = HomeUiState()

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
        }.retryWhen { error, _ ->
            if (error is CancellationException) return@retryWhen false
            delay(HOME_STORAGE_RETRY_DELAY_MILLIS.milliseconds)
            true
        }.onEach { activity ->
            lastActivityState = activity
        }.onStart {
            emit(lastActivityState)
        }.combine(trainingState) { activity, training ->
            activity.copy(
                dailyTraining = training.plan?.toHomeDailyTraining(),
                trainingLoadFailed = training.failed,
                isStartingTraining = training.starting,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState(),
        )

    init {
        viewModelScope.launch {
            dailyTrainingRepository.observeTodayTraining().collect { plan ->
                if (plan != null && plan.epochDay == currentEpochDay()) {
                    trainingState.value = TrainingState(plan = plan, starting = navigationPending)
                } else if (!trainingState.value.failed) {
                    trainingState.value = TrainingState()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                discardStalePlan()
                if (trainingState.value.plan == null) loadTraining(showLoading = false)
                val now = clock.now().toEpochMilliseconds()
                delay(
                    minOf(
                        if (trainingState.value.plan == null) {
                            HOME_STORAGE_RETRY_DELAY_MILLIS
                        } else {
                            DAY_CHANGE_POLL_INTERVAL_MILLIS
                        },
                        MILLIS_PER_DAY - now % MILLIS_PER_DAY,
                    ).milliseconds,
                )
            }
        }
    }

    fun refreshDailyTraining() {
        navigationPending = false
        discardStalePlan()
        loadTraining(showLoading = false)
    }

    fun retryDailyTraining() {
        loadTraining(showLoading = true)
    }

    private fun discardStalePlan() {
        if (trainingState.value.plan?.epochDay?.let { it != currentEpochDay() } == true) {
            trainingState.value = TrainingState()
        }
    }

    private fun loadTraining(showLoading: Boolean) {
        if (operationPending) return
        operationPending = true
        if (showLoading) trainingState.value = TrainingState()
        viewModelScope.launch {
            try {
                val plan = dailyTrainingRepository.ensureTodayTraining()
                trainingState.value = TrainingState(plan = plan.takeIf { it.epochDay == currentEpochDay() })
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                trainingState.value = TrainingState(failed = true)
            } finally {
                operationPending = false
            }
        }
    }

    fun startTraining(onStart: (DailyTrainingEntry) -> Unit) {
        if (operationPending || navigationPending) return
        val displayedPlan = trainingState.value.plan ?: return
        if (displayedPlan.nextEntry == null) return
        if (displayedPlan.epochDay != currentEpochDay()) {
            discardStalePlan()
            loadTraining(showLoading = true)
            return
        }
        operationPending = true
        navigationPending = true
        trainingState.value = trainingState.value.copy(starting = true)
        viewModelScope.launch {
            try {
                val plan = dailyTrainingRepository.ensureTodayTraining()
                val sameDay = plan.epochDay == displayedPlan.epochDay && plan.epochDay == currentEpochDay()
                val entry = plan.nextEntry.takeIf { sameDay }
                navigationPending = entry != null
                trainingState.value =
                    TrainingState(
                        plan = plan.takeIf { it.epochDay == currentEpochDay() },
                        starting = navigationPending,
                    )
                entry?.let(onStart)
            } catch (error: CancellationException) {
                navigationPending = false
                throw error
            } catch (_: Exception) {
                navigationPending = false
                trainingState.value = TrainingState(failed = true)
            } finally {
                operationPending = false
            }
        }
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

    private fun currentEpochDay(): Long = clock.now().toEpochMilliseconds() / MILLIS_PER_DAY
}

internal fun DailyTrainingPlan.toHomeDailyTraining(): HomeUiState.DailyTraining =
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

internal fun GameId.toHomeGame(): HomeUiState.Game =
    when (this) {
        GameId.NumberSprint -> HomeUiState.Game.NumberSprint
        GameId.WideEye -> HomeUiState.Game.WideEye
        GameId.DontTap -> HomeUiState.Game.DontTap
        GameId.MemoryGrid -> HomeUiState.Game.MemoryGrid
        GameId.WordFlow -> HomeUiState.Game.WordFlow
        GameId.DualFocus -> HomeUiState.Game.DualFocus
        GameId.RotationMatch -> HomeUiState.Game.RotationMatch
        GameId.NumberTrail -> HomeUiState.Game.NumberTrail
        GameId.SymbolCount -> HomeUiState.Game.SymbolCount
        GameId.RuleSwitch -> HomeUiState.Game.RuleSwitch
    }

internal fun GameModeId.toHomeMode(): HomeUiState.Mode =
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
        GameModeId.MemoryGridRoute -> HomeUiState.Mode.Route
        GameModeId.MemoryGridReverse -> HomeUiState.Mode.Reverse
        GameModeId.MemoryGridFlash -> HomeUiState.Mode.Flash
        GameModeId.WordFlowContext -> HomeUiState.Mode.Context
        GameModeId.DualFocusMatch -> HomeUiState.Mode.Match
        GameModeId.RotationMatchRotation -> HomeUiState.Mode.Rotation
        GameModeId.NumberTrailAscending -> HomeUiState.Mode.Ascending
        GameModeId.SymbolCountCount -> HomeUiState.Mode.Count
        GameModeId.RuleSwitchSwitch -> HomeUiState.Mode.Switch
    }
