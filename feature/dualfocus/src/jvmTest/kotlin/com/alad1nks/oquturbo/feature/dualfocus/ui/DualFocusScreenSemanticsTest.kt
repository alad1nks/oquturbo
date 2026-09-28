package com.alad1nks.oquturbo.feature.dualfocus.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DualFocusScreenSemanticsTest {
    @Test
    fun compactLargeTextReadyScrollStaysBelowFixedHeaderAndStartRemainsClickable() =
        runSkikoComposeUiTest(size = Size(320f, 640f), density = Density(1f, fontScale = 1.5f)) {
            var starts = 0
            setContent {
                OquTurboTheme {
                    DualFocusScreen(
                        state = DualFocusUiState(record = 5, isRecordLoading = false),
                        onStartClick = { starts++ },
                        onCardClick = { _, _ -> },
                        onBackClick = {},
                    )
                }
            }
            val headerValues = setOf("Score", "Record", "0", "5")
            val headerBefore = headerValues.associateWith { onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
            val headerBottom = headerBefore.values.maxOf { it.bottom }
            onNodeWithText("Start").performScrollTo().assertIsDisplayed()
            val visibleContent =
                onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().filter { node ->
                    node.config[SemanticsProperties.Text].none { it.text in headerValues } &&
                        node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f
                }
            assertTrue(visibleContent.isNotEmpty())
            visibleContent.forEach { node ->
                assertTrue(
                    node.boundsInRoot.top >= headerBottom,
                    "Scrolled content ${node.config[SemanticsProperties.Text]} overlaps the fixed header: ${node.boundsInRoot}",
                )
            }
            headerBefore.forEach { (value, bounds) ->
                assertEquals(
                    bounds,
                    onNodeWithText(value).fetchSemanticsNode().boundsInRoot,
                    "Header must remain fixed",
                )
            }
            onNodeWithText("Start").performClick()
            assertEquals(1, starts)
        }
}
