package com.alad1nks.oquturbo.core.data.practice

import com.alad1nks.oquturbo.core.data.model.DayCompletionStatus
import com.alad1nks.oquturbo.core.data.model.DayHistory

data class PracticeDay(val epochDay: Long, val status: DayCompletionStatus)

data class PracticeRhythm(
    val todayEpochDay: Long,
    val trackingStartedEpochDay: Long,
    val days: List<PracticeDay>,
    val completedDaysInWindow: Int,
    val hasUnknownDays: Boolean,
    val currentStreakDays: Int,
    val currentStreakIsExact: Boolean,
    val bestRecordedStreakDays: Int,
    val hasFutureFacts: Boolean,
) {
    val weeklyGoalReached: Boolean get() = completedDaysInWindow >= 4
    val sevenDayStreakEarned: Boolean get() = bestRecordedStreakDays >= 7
}

/** All calculations use one explicit UTC day; neither journal retention nor wall-clock reads are involved. */
fun calculatePracticeRhythm(history: DayHistory, todayEpochDay: Long): PracticeRhythm {
    val days = (todayEpochDay - 6..todayEpochDay).map { PracticeDay(it, history.statusOn(it, todayEpochDay)) }
    val today = days.last().status
    val yesterday = days[5].status
    val anchor =
        when {
            today == DayCompletionStatus.Completed -> todayEpochDay
            yesterday == DayCompletionStatus.Completed -> todayEpochDay - 1
            else -> null
        }
    var current = 0
    var boundary = anchor
    while (boundary != null && history.statusOn(boundary, todayEpochDay) == DayCompletionStatus.Completed) {
        current++
        boundary--
    }
    val exact =
        if (boundary == null) {
            today == DayCompletionStatus.NoCompletionRecorded && yesterday == DayCompletionStatus.NoCompletionRecorded
        } else {
            today != DayCompletionStatus.Unknown && history.statusOn(
                boundary,
                todayEpochDay,
            ) == DayCompletionStatus.NoCompletionRecorded
        }
    val recorded = history.completedEpochDays.sorted()
    var best = 0
    var run = 0
    var previous: Long? = null
    for (day in recorded) {
        run = if (previous?.let { day - it == 1L } == true) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return PracticeRhythm(
        todayEpochDay = todayEpochDay,
        trackingStartedEpochDay = history.trackingStartedEpochDay,
        days = days,
        completedDaysInWindow = days.count { it.status == DayCompletionStatus.Completed },
        hasUnknownDays = days.any { it.status == DayCompletionStatus.Unknown },
        currentStreakDays = current,
        currentStreakIsExact = exact,
        bestRecordedStreakDays = best,
        hasFutureFacts = recorded.any { it > todayEpochDay },
    )
}

/** Gregorian civil date without converting epoch days to Int or multiplying them into milliseconds. */
data class PracticeDate(val year: Long, val month: Int, val day: Int) {
    val yearText: String get() =
        if (year < 0) {
            "-" +
                (-year).toString().padStart(
                    4,
                    '0',
                )
        } else {
            year.toString().padStart(4, '0')
        }
    val monthText: String get() = month.toString().padStart(2, '0')
    val dayText: String get() = day.toString().padStart(2, '0')
}

fun practiceDate(epochDay: Long): PracticeDate {
    // Same civil-calendar arithmetic used by Stats, extended to include year and month.
    val shifted = epochDay + 719_468L
    val era = if (shifted >= 0) shifted / 146_097L else (shifted - 146_096L) / 146_097L
    val dayOfEra = shifted - era * 146_097L
    val yearOfEra = (dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime = (5L * dayOfYear + 2L) / 153L
    val day = (dayOfYear - (153L * monthPrime + 2L) / 5L + 1L).toInt()
    val month = (monthPrime + if (monthPrime < 10) 3 else -9).toInt()
    val year = yearOfEra + era * 400L + if (month <= 2) 1 else 0
    return PracticeDate(year, month, day)
}
