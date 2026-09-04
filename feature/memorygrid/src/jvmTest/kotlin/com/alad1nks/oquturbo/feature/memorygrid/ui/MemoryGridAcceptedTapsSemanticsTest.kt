package com.alad1nks.oquturbo.feature.memorygrid.ui

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridGameMode
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridPhase
import com.alad1nks.oquturbo.feature.memorygrid.model.MemoryGridState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MemoryGridAcceptedTapsSemanticsTest {
    @Test
    fun awaitingInputAnnouncesAcceptedTapCountAsPoliteLiveRegion() =
        runComposeUiTest {
            setMemoryGridContent(
                MemoryGridState(
                    phase = MemoryGridPhase.AwaitingInput,
                    gridSize = 4,
                    sequence = listOf(0, 1, 0, 2, 3, 4, 5),
                    input = listOf(0, 1, 0),
                ),
            )

            onNodeWithContentDescription("Accepted taps: 3").assertExists()
            onAllNodes(acceptedTapsLiveRegionMatcher).assertCountEquals(1)
        }

    @Test
    fun otherPhasesOmitAcceptedTapCount() =
        runComposeUiTest {
            setMemoryGridContent(
                MemoryGridState(
                    phase = MemoryGridPhase.ShowingSequence,
                    sequence = listOf(0, 1, 2),
                    presentationIndex = 0,
                    input = listOf(0, 1),
                ),
            )

            onNodeWithContentDescription("Accepted taps: 2").assertDoesNotExist()
            onAllNodes(acceptedTapsLiveRegionMatcher).assertCountEquals(0)
        }

    private fun ComposeUiTest.setMemoryGridContent(state: MemoryGridState) {
        setContent {
            OquTurboTheme {
                MemoryGridScreen(
                    state = state,
                    mode = MemoryGridGameMode.Route,
                    onStartClick = {},
                    onCellClick = {},
                    onBackClick = {},
                )
            }
        }
    }

    private companion object {
        val acceptedTapsLiveRegionMatcher =
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            )
    }
}
