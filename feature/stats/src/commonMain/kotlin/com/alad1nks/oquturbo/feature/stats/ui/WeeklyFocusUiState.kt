package com.alad1nks.oquturbo.feature.stats.ui

import com.alad1nks.oquturbo.core.data.model.WeeklyFocus

internal sealed interface WeeklyFocusUiState {
    data object Loading : WeeklyFocusUiState

    data object Saving : WeeklyFocusUiState

    data object Checking : WeeklyFocusUiState

    data object Error : WeeklyFocusUiState

    data object Unconfirmed : WeeklyFocusUiState

    data class Ready(val focus: WeeklyFocus) : WeeklyFocusUiState
}
