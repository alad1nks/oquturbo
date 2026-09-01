package com.alad1nks.oquturbo.feature.kenkozgame.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry
import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode

internal data class KenKozGameUiState(
    val mode: KenKozGameMode,
    val trainingRequiredScore: Int? = null,
    val trainingNextEntry: DailyTrainingEntry? = null,
    val isTrainingCompletionReady: Boolean = false,
    val score: Int = 0,
    val record: Int = 0,
    val isNewRecord: Boolean = false,
    val phase: Phase = Phase.Initial,
    val items: List<String> = emptyList(),
    val answers: List<String> = emptyList(),
    val correctAnswer: String = "",
    val selectedAnswer: String? = null,
    val questionDirection: Direction? = null,
    val wideLineWordIndex: Int? = null,
) {
    fun startingSession(): KenKozGameUiState =
        copy(
            score = 0,
            isNewRecord = false,
            trainingNextEntry = null,
            isTrainingCompletionReady = false,
            selectedAnswer = null,
        )

    fun withMistake(answer: String): KenKozGameUiState =
        copy(
            phase = Phase.Mistake,
            selectedAnswer = answer,
        )

    enum class Phase {
        Initial,
        Showing,
        Answering,
        Mistake,
    }

    enum class Direction {
        Top,
        Left,
        Right,
        Bottom,
    }
}
