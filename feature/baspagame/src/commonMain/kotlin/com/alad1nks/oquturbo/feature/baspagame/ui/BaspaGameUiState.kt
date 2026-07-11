package com.alad1nks.oquturbo.feature.baspagame.ui

import com.alad1nks.oquturbo.feature.baspagame.model.BaspaGameMode

internal data class BaspaGameUiState(
    val mode: BaspaGameMode,
    val stimulus: String = "",
    val shouldTap: Boolean = false,
    val score: Int = 0,
    val record: Int = 0,
    val intervalMillis: Long = 2_000L,
    val phase: Phase = Phase.Playing,
    val accent: Accent = Accent.Default,
) {
    enum class Phase { Playing, Mistake }

    enum class Accent { Default, Target, Other }
}
