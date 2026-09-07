package com.alad1nks.oquturbo.feature.memorygrid.ui

internal sealed interface MemoryGridDurationDisplayParts {
    data object LessThanOneSecond : MemoryGridDurationDisplayParts

    data class Seconds(val seconds: Long) : MemoryGridDurationDisplayParts

    data class MinutesSeconds(val minutes: Long, val seconds: Long?) : MemoryGridDurationDisplayParts
}

internal fun memoryGridDurationDisplayParts(durationMillis: Long): MemoryGridDurationDisplayParts {
    val wholeMillis = durationMillis.coerceAtLeast(0)
    if (wholeMillis < MILLIS_PER_SECOND) return MemoryGridDurationDisplayParts.LessThanOneSecond
    val totalSeconds = wholeMillis / MILLIS_PER_SECOND
    if (wholeMillis < MILLIS_PER_MINUTE) return MemoryGridDurationDisplayParts.Seconds(totalSeconds)
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).takeIf { it != 0L }
    return MemoryGridDurationDisplayParts.MinutesSeconds(
        minutes = wholeMillis / MILLIS_PER_MINUTE,
        seconds = seconds,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE

// Preserve integer plural categories for EN/RU/KK without truncating the displayed Long.
internal fun Long.memoryGridPluralQuantity(): Int =
    if (this <= Int.MAX_VALUE) toInt() else (100 + this % 100).toInt()
