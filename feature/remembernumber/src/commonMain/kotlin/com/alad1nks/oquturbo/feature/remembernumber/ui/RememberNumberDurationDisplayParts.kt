package com.alad1nks.oquturbo.feature.remembernumber.ui

internal sealed interface RememberNumberDurationDisplayParts {
    data object LessThanOneSecond : RememberNumberDurationDisplayParts

    data class Seconds(val seconds: Long) : RememberNumberDurationDisplayParts

    data class MinutesSeconds(val minutes: Long, val seconds: Long) : RememberNumberDurationDisplayParts
}

internal fun rememberNumberDurationDisplayParts(durationMillis: Long): RememberNumberDurationDisplayParts {
    val wholeMillis = durationMillis.coerceAtLeast(0)
    if (wholeMillis < MILLIS_PER_SECOND) return RememberNumberDurationDisplayParts.LessThanOneSecond
    val totalSeconds = wholeMillis / MILLIS_PER_SECOND
    if (wholeMillis < MILLIS_PER_MINUTE) return RememberNumberDurationDisplayParts.Seconds(totalSeconds)
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return RememberNumberDurationDisplayParts.MinutesSeconds(
        minutes = wholeMillis / MILLIS_PER_MINUTE,
        seconds = seconds,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE

// Preserve integer plural categories for EN/RU/KK without truncating the displayed Long.
internal fun Long.rememberNumberPluralQuantity(): Int =
    if (this <= Int.MAX_VALUE) toInt() else (100 + this % 100).toInt()
