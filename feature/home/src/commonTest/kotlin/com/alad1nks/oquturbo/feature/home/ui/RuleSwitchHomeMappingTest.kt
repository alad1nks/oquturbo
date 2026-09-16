package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleSwitchHomeMappingTest {
    @Test
    fun ruleSwitchRecentRecordUsesTheExpectedGameModeResourcesAndIcon() {
        assertEquals(HomeUiState.Game.RuleSwitch, GameId.RuleSwitch.toHomeGame())
        assertEquals(HomeUiState.Mode.Switch, GameModeId.RuleSwitchSwitch.toHomeMode())
        assertEquals(
            AppResource.String.rule_switch_title,
            HomeUiState.Game.RuleSwitch.titleResource(),
        )
        assertEquals(
            AppResource.String.rule_switch_mode,
            HomeUiState.Mode.Switch.titleResource(),
        )
        assertEquals(Icons.Filled.SwapHoriz, HomeUiState.Game.RuleSwitch.icon())
    }
}
