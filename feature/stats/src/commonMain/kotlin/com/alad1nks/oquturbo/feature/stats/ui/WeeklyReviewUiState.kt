package com.alad1nks.oquturbo.feature.stats.ui

import com.alad1nks.oquturbo.core.data.model.ProgressComparison
import com.alad1nks.oquturbo.core.data.practice.PracticeRhythm

internal sealed interface WeeklySource<out T> {
    data object Loading : WeeklySource<Nothing>

    data object Error : WeeklySource<Nothing>

    data class Ready<T>(val value: T) : WeeklySource<T>
}

internal data class WeeklyReviewUiState(
    val todayEpochDay: Long,
    val practice: WeeklySource<PracticeRhythm> = WeeklySource.Loading,
    val training: WeeklySource<PracticeRhythm> = WeeklySource.Loading,
    val comparison: WeeklySource<ProgressComparison> = WeeklySource.Loading,
    val focus: WeeklyFocusUiState = WeeklyFocusUiState.Loading,
)
