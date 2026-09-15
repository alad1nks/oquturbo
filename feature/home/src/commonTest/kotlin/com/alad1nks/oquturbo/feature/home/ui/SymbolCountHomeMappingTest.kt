package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals

class SymbolCountHomeMappingTest {
    @Test
    fun symbolCountRecentRecordUsesTheExpectedGameModeResourcesAndIcon() {
        assertEquals(HomeUiState.Game.SymbolCount, GameId.SymbolCount.toHomeGame())
        assertEquals(HomeUiState.Mode.Count, GameModeId.SymbolCountCount.toHomeMode())
        assertEquals(
            AppResource.String.symbol_count_title,
            HomeUiState.Game.SymbolCount.titleResource(),
        )
        assertEquals(
            AppResource.String.symbol_count_mode,
            HomeUiState.Mode.Count.titleResource(),
        )
        assertEquals(Icons.Filled.FormatListNumbered, HomeUiState.Game.SymbolCount.icon())
    }
}
