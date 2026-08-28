package com.alad1nks.oquturbo.feature.stats.ui

import com.alad1nks.oquturbo.feature.stats.model.ActivityStatus
import com.alad1nks.oquturbo.feature.stats.model.StatsDayActivity
import com.alad1nks.oquturbo.feature.stats.model.StatsPeriodSnapshot
import com.alad1nks.oquturbo.feature.stats.model.StatsUiState
import com.alad1nks.oquturbo.feature.stats.model.StatsWeekday
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatsActivityStatusTest {
    @Test
    fun activityStatusesUseTheirLegendResources() {
        assertEquals(
            AppResource.String.stats_activity_completed,
            ActivityStatus.DailyComplete.titleResource(),
        )
        assertEquals(
            AppResource.String.stats_activity_started,
            ActivityStatus.DailyPartial.titleResource(),
        )
        assertEquals(
            AppResource.String.stats_activity_games_only,
            ActivityStatus.GamesOnly.titleResource(),
        )
        assertEquals(
            AppResource.String.stats_activity_none,
            ActivityStatus.None.titleResource(),
        )
    }

    @Test
    fun selectedDayStatusIsAvailableOnlyWhenADayIsSelected() {
        val gamesOnlyDay =
            StatsDayActivity(
                id = 3,
                dayNumber = 4,
                weekday = StatsWeekday.Thursday,
                status = ActivityStatus.GamesOnly,
                games = 2,
                minutes = 5,
            )
        val state =
            StatsUiState(
                snapshot = StatsPeriodSnapshot.Empty.copy(activityDays = listOf(gamesOnlyDay)),
            )

        assertNull(state.selectedDay)
        assertEquals(
            AppResource.String.stats_activity_games_only,
            state.copy(selectedDayId = gamesOnlyDay.id).selectedDay?.status?.titleResource(),
        )
    }
}
