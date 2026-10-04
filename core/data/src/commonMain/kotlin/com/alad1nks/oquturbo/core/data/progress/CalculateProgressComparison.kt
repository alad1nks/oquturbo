package com.alad1nks.oquturbo.core.data.progress

import com.alad1nks.oquturbo.core.data.model.GameSeriesKey
import com.alad1nks.oquturbo.core.data.model.GameSession
import com.alad1nks.oquturbo.core.data.model.ProgressComparison

/**
 * Compares the latest exact series within 28 UTC calendar days, including [todayEpochDay].
 * Orders by timestamp, then original input position: a later input wins equal timestamps.
 * Uses only the last ten known attempts, without reconstructing truncated journal history.
 */
fun calculateProgressComparison(sessions: List<GameSession>, todayEpochDay: Long): ProgressComparison {
    val firstDay = todayEpochDay - COMPARISON_WINDOW_DAYS + 1
    val ordered =
        sessions.withIndex()
            .filter { it.value.completedEpochDay in firstDay..todayEpochDay }
            .sortedWith(compareBy<IndexedValue<GameSession>> { it.value.completedAtEpochMillis }.thenBy { it.index })
            .map { it.value }
    val series =
        ordered.lastOrNull()?.exactSeriesKey()
            ?: return ProgressComparison.NoRecentSessions(firstDay, todayEpochDay)
    val selected = ordered.filter { it.exactSeriesKey() == series }
    if (selected.size < COMPARISON_GROUP_SIZE * 2) {
        return ProgressComparison.InsufficientData(firstDay, todayEpochDay, series, selected.size)
    }
    val scores = selected.takeLast(COMPARISON_GROUP_SIZE * 2).map(GameSession::score)
    val previousMedian = scores.take(COMPARISON_GROUP_SIZE).sorted()[COMPARISON_GROUP_SIZE / 2]
    val currentMedian = scores.drop(COMPARISON_GROUP_SIZE).sorted()[COMPARISON_GROUP_SIZE / 2]
    return ProgressComparison.Compared(
        firstEpochDay = firstDay,
        lastEpochDay = todayEpochDay,
        series = series,
        availableCount = selected.size,
        previousMedian = previousMedian,
        currentMedian = currentMedian,
        absoluteChange = currentMedian - previousMedian,
    )
}

private fun GameSession.exactSeriesKey() = GameSeriesKey(game, mode, variantId)

private const val COMPARISON_WINDOW_DAYS = 28L
private const val COMPARISON_GROUP_SIZE = 5
