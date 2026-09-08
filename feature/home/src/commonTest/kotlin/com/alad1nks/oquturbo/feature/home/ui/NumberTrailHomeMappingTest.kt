package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberTrailHomeMappingTest {
    @Test
    fun numberTrailRecentRecordUsesTheExpectedGameModeResourcesAndIcon() {
        assertEquals(HomeUiState.Game.NumberTrail, GameId.NumberTrail.toHomeGame())
        assertEquals(HomeUiState.Mode.Ascending, GameModeId.NumberTrailAscending.toHomeMode())
        assertEquals(
            AppResource.String.number_trail_title,
            HomeUiState.Game.NumberTrail.titleResource(),
        )
        assertEquals(
            AppResource.String.number_trail_mode,
            HomeUiState.Mode.Ascending.titleResource(),
        )
        assertEquals(Icons.Filled.FormatListNumbered, HomeUiState.Game.NumberTrail.icon())
    }
}
