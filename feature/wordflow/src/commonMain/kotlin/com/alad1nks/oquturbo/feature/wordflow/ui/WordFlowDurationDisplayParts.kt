package com.alad1nks.oquturbo.feature.wordflow.ui

internal sealed interface WordFlowDurationDisplayParts {
    data object LessThanOneSecond : WordFlowDurationDisplayParts

    data class Seconds(val seconds: Long) : WordFlowDurationDisplayParts

    data class MinutesSeconds(val minutes: Long, val seconds: Long?) : WordFlowDurationDisplayParts
}

internal fun wordFlowDurationDisplayParts(durationMillis: Long): WordFlowDurationDisplayParts {
    val wholeMillis = durationMillis.coerceAtLeast(0)
    if (wholeMillis < MILLIS_PER_SECOND) return WordFlowDurationDisplayParts.LessThanOneSecond
    val totalSeconds = wholeMillis / MILLIS_PER_SECOND
    if (wholeMillis < MILLIS_PER_MINUTE) return WordFlowDurationDisplayParts.Seconds(totalSeconds)
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).takeIf { it != 0L }
    return WordFlowDurationDisplayParts.MinutesSeconds(
        minutes = wholeMillis / MILLIS_PER_MINUTE,
        seconds = seconds,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE
