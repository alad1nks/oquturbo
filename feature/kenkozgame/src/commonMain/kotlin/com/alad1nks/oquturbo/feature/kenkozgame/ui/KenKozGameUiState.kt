package com.alad1nks.oquturbo.feature.kenkozgame.ui

import com.alad1nks.oquturbo.feature.kenkozgame.model.KenKozGameMode

internal data class KenKozGameUiState(
    val mode: KenKozGameMode,
    val score: Int = 0,
    val record: Int = 0,
    val phase: Phase = Phase.Initial,
    val items: List<String> = emptyList(),
    val answers: List<String> = emptyList(),
    val correctAnswer: String = "",
    val questionDirection: Direction? = null,
) {
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
