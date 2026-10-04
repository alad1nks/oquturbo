package com.alad1nks.oquturbo.core.data.model

/** One optional Number Sprint Classic cycle; dates are UTC epoch days, with an exclusive end. */
data class WeeklyFocus(val selection: WeeklyFocusSelection? = null) {
    fun phaseOn(today: Long): WeeklyFocusPhase =
        selection?.let {
            when {
                today < it.startEpochDay -> WeeklyFocusPhase.Scheduled
                today < it.endExclusiveEpochDay -> WeeklyFocusPhase.Active
                else -> WeeklyFocusPhase.Expired
            }
        } ?: WeeklyFocusPhase.Off
}

data class WeeklyFocusSelection(val startEpochDay: Long, val endExclusiveEpochDay: Long) {
    init {
        require(startEpochDay <= Long.MAX_VALUE - 7 && endExclusiveEpochDay == startEpochDay + 7) {
            "Focus interval must contain exactly seven representable UTC days"
        }
    }

    val lastEpochDay: Long get() = endExclusiveEpochDay - 1

    companion object {
        fun after(today: Long): WeeklyFocusSelection {
            require(today <= Long.MAX_VALUE - 8) { "Focus interval overflows UTC epoch days" }
            return WeeklyFocusSelection(today + 1, today + 8)
        }
    }
}

enum class WeeklyFocusPhase { Off, Scheduled, Active, Expired }
