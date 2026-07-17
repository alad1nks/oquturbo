package com.alad1nks.oquturbo.feature.home.ui

internal data class HomeUiState(
    val overallLevel: Int = 1,
    val rankNumber: Int = 1,
    val levelProgress: Float = 0f,
    val recentRecords: List<RecentRecord> = emptyList(),
) {
    enum class Game {
        NumberSprint,
        WideEye,
        DontTap,
    }

    enum class Mode {
        Classic,
        Binary,
        Custom,
        Characters,
        Words,
        FindDifference,
        WideLine,
        Categories,
        Letter,
        WordLength,
        TextColor,
        TrueFalse,
        Math,
        SpeedReading,
    }

    data class RecentRecord(
        val game: Game,
        val mode: Mode,
        val variantId: String? = null,
        val score: Int,
    )
}
