package com.alad1nks.oquturbo.feature.kenkozgame.ui

internal sealed interface KenKozDurationDisplayParts {
    data object LessThanOneSecond : KenKozDurationDisplayParts

    data class Seconds(val seconds: Long) : KenKozDurationDisplayParts

    data class MinutesSeconds(val minutes: Long, val seconds: Long) : KenKozDurationDisplayParts
}

internal fun kenKozDurationDisplayParts(durationMillis: Long): KenKozDurationDisplayParts {
    val wholeMillis = durationMillis.coerceAtLeast(0)
    if (wholeMillis < MILLIS_PER_SECOND) return KenKozDurationDisplayParts.LessThanOneSecond
    val totalSeconds = wholeMillis / MILLIS_PER_SECOND
    if (wholeMillis < MILLIS_PER_MINUTE) return KenKozDurationDisplayParts.Seconds(totalSeconds)
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return KenKozDurationDisplayParts.MinutesSeconds(
        minutes = wholeMillis / MILLIS_PER_MINUTE,
        seconds = seconds,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE

// Preserve integer plural categories for EN/RU/KK without truncating the displayed Long.
internal fun Long.kenKozPluralQuantity(): Int =
    if (this <= Int.MAX_VALUE) toInt() else (100 + this % 100).toInt()
