package com.alad1nks.oquturbo.feature.baspagame.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode

internal data class BaspaGameUiState(
    val mode: BaspaGameMode,
    val stimulus: String = "",
    val categoryName: String = "",
    val categoryId: String = "animals",
    val letter: String = "",
    val wordLength: Int = 0,
    val targetColorName: String = "",
    val stimulusColorId: String = "",
    val stimulusColorName: String = "",
    val shouldTap: Boolean = false,
    val score: Int = 0,
    val record: Int = 0,
    val isRecordLoaded: Boolean = false,
    val intervalMillis: Long = 2_000L,
    val phase: Phase = Phase.Initial,
    val mistakeReason: BaspaMistakeReason? = null,
    val trainingRequiredScore: Int? = null,
    val isTrainingCompletionReady: Boolean = false,
    val trainingNextEntry: DailyTrainingEntry? = null,
) {
    val isTraining: Boolean
        get() = trainingRequiredScore != null

    val isTrainingGoalReached: Boolean
        get() = trainingRequiredScore?.let { score >= it } == true

    enum class Phase { Initial, Playing, Paused, Mistake }
}

internal enum class BaspaMistakeReason {
    IncorrectTap,
    MissedMatch,
}

internal fun BaspaGameUiState.mistakeReasonOnTap(): BaspaMistakeReason? =
    BaspaMistakeReason.IncorrectTap.takeIf {
        phase == BaspaGameUiState.Phase.Playing && stimulus.isNotEmpty() && !shouldTap
    }

internal fun BaspaGameUiState.mistakeReasonOnTimeout(): BaspaMistakeReason? =
    BaspaMistakeReason.MissedMatch.takeIf { shouldTap }

internal fun BaspaGameUiState.withMistake(
    reason: BaspaMistakeReason,
    sessionInProgress: Boolean,
): BaspaGameUiState =
    if (phase == BaspaGameUiState.Phase.Playing && sessionInProgress) {
        copy(
            phase = BaspaGameUiState.Phase.Mistake,
            mistakeReason = reason,
        )
    } else {
        this
    }

internal fun BaspaGameUiState.restartingSession(): BaspaGameUiState =
    copy(
        score = 0,
        intervalMillis = 2_000L,
        phase = BaspaGameUiState.Phase.Playing,
        mistakeReason = null,
        isTrainingCompletionReady = false,
        trainingNextEntry = null,
    )
