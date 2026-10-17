package com.alad1nks.oquturbo.feature.numbertrail.ui

internal sealed interface NumberTrailDurationDisplayParts {
    data object LessThanOneSecond : NumberTrailDurationDisplayParts

    data class Seconds(val seconds: Long) : NumberTrailDurationDisplayParts

    data class MinutesSeconds(val minutes: Long, val seconds: Long?) : NumberTrailDurationDisplayParts
}

internal fun numberTrailDurationDisplayParts(durationMillis: Long): NumberTrailDurationDisplayParts {
    val wholeMillis = durationMillis.coerceAtLeast(0)
    if (wholeMillis < MILLIS_PER_SECOND) return NumberTrailDurationDisplayParts.LessThanOneSecond
    val totalSeconds = wholeMillis / MILLIS_PER_SECOND
    if (wholeMillis < MILLIS_PER_MINUTE) return NumberTrailDurationDisplayParts.Seconds(totalSeconds)
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).takeIf { it != 0L }
    return NumberTrailDurationDisplayParts.MinutesSeconds(
        minutes = wholeMillis / MILLIS_PER_MINUTE,
        seconds = seconds,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE

// Preserve integer plural categories for EN/RU/KK without truncating the displayed Long.
internal fun Long.numberTrailPluralQuantity(): Int =
    if (this <= Int.MAX_VALUE) toInt() else (100 + this % 100).toInt()
