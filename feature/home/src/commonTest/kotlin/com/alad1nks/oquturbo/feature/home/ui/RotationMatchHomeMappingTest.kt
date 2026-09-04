package com.alad1nks.oquturbo.feature.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.resources.AppResource
import kotlin.test.Test
import kotlin.test.assertEquals

class RotationMatchHomeMappingTest {
    @Test
    fun rotationMatchRecentRecordUsesTheExpectedGameModeResourcesAndIcon() {
        assertEquals(HomeUiState.Game.RotationMatch, GameId.RotationMatch.toHomeGame())
        assertEquals(HomeUiState.Mode.Rotation, GameModeId.RotationMatchRotation.toHomeMode())
        assertEquals(
            AppResource.String.rotation_match_title,
            HomeUiState.Game.RotationMatch.titleResource(),
        )
        assertEquals(
            AppResource.String.rotation_match_mode,
            HomeUiState.Mode.Rotation.titleResource(),
        )
        assertEquals(Icons.Filled.RotateRight, HomeUiState.Game.RotationMatch.icon())
    }
}
