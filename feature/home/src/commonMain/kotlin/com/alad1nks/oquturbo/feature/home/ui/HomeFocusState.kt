package com.alad1nks.oquturbo.feature.home.ui

import com.alad1nks.oquturbo.core.data.model.WeeklyFocus

internal sealed interface HomeFocusState {
    data object Loading : HomeFocusState

    data object Error : HomeFocusState

    data class Ready(val focus: WeeklyFocus, val todayEpochDay: Long) : HomeFocusState
}
