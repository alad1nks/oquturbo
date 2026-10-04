package com.alad1nks.oquturbo.core.data.model

/** A comparison of known journal entries in an inclusive UTC calendar-day window. */
sealed interface ProgressComparison {
    val firstEpochDay: Long
    val lastEpochDay: Long

    data class NoRecentSessions(
        override val firstEpochDay: Long,
        override val lastEpochDay: Long,
    ) : ProgressComparison

    data class InsufficientData(
        override val firstEpochDay: Long,
        override val lastEpochDay: Long,
        val series: GameSeriesKey,
        val availableCount: Int,
    ) : ProgressComparison

    /** availableCount may exceed the ten attempts used in the two five-attempt medians. */
    data class Compared(
        override val firstEpochDay: Long,
        override val lastEpochDay: Long,
        val series: GameSeriesKey,
        val availableCount: Int,
        val previousMedian: Int,
        val currentMedian: Int,
        val absoluteChange: Int,
    ) : ProgressComparison
}
