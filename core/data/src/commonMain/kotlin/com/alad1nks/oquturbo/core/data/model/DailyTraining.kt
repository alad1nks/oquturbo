package com.alad1nks.oquturbo.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyTrainingPlan(
    val version: Int = CURRENT_VERSION,
    val epochDay: Long,
    val entries: List<DailyTrainingEntry>,
) {
    val isCompleted: Boolean
        get() = entries.isNotEmpty() && entries.all(DailyTrainingEntry::isCompleted)

    val nextEntry: DailyTrainingEntry?
        get() = entries.firstOrNull { !it.isCompleted }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class DailyTrainingEntry(
    val id: String,
    val game: GameId,
    val mode: GameModeId,
    val requiredScore: Int,
    val isCompleted: Boolean = false,
)

@Serializable
data class DailyTrainingProgress(
    val version: Int = CURRENT_VERSION,
    val totalCompletedTrainings: Int = 0,
    val lastCompletedEpochDay: Long? = null,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
