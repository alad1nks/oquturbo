package com.alad1nks.oquturbo.feature.dualfocus.ui

internal sealed interface DurationDisplayParts {
    data object LessThanOneSecond : DurationDisplayParts

    data class Seconds(val seconds: Long) : DurationDisplayParts

    data class MinutesSeconds(val minutes: Long, val seconds: Long?) : DurationDisplayParts
}

internal fun durationDisplayParts(durationMillis: Long): DurationDisplayParts {
    val wholeMillis = durationMillis.coerceAtLeast(0)
    if (wholeMillis < MILLIS_PER_SECOND) return DurationDisplayParts.LessThanOneSecond

    val totalSeconds = wholeMillis / MILLIS_PER_SECOND
    if (wholeMillis < MILLIS_PER_MINUTE) return DurationDisplayParts.Seconds(totalSeconds)

    val seconds = (totalSeconds % SECONDS_PER_MINUTE).takeIf { it != 0L }
    return DurationDisplayParts.MinutesSeconds(
        minutes = wholeMillis / MILLIS_PER_MINUTE,
        seconds = seconds,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE
