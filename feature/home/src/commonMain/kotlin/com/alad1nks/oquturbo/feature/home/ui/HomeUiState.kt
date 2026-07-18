package com.alad1nks.oquturbo.feature.home.ui

import com.alad1nks.oquturbo.core.data.model.DailyTrainingEntry

internal data class HomeUiState(
    val overallLevel: Int = 1,
    val rankNumber: Int = 1,
    val levelProgress: Float = 0f,
    val dailyTraining: DailyTraining? = null,
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

    data class DailyTraining(
        val items: List<TrainingItem>,
    ) {
        val isCompleted: Boolean
            get() = items.isNotEmpty() && items.all(TrainingItem::isCompleted)

        val nextItem: TrainingItem?
            get() = items.firstOrNull { !it.isCompleted }
    }

    data class TrainingItem(
        val entry: DailyTrainingEntry,
        val game: Game,
        val mode: Mode,
        val requiredScore: Int,
        val isCompleted: Boolean,
    )
}
