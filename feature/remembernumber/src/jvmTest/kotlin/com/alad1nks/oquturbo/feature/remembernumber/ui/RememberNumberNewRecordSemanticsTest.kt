package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.alad1nks.oquturbo.core.designsystem.theme.OquTurboTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RememberNumberNewRecordSemanticsTest {
    @Test
    fun newRecordIsConditionalPoliteLiveRegion() =
        runComposeUiTest {
            setContent {
                OquTurboTheme {
                    RememberNumberScreen(
                        uiState =
                            RememberNumberUiState.Mistake(
                                text = "1234",
                                score = 8,
                                correctText = "1334",
                                record = 8,
                                isNewRecord = true,
                            ),
                        focusEvent = null,
                        maxLength = 4,
                        record = 8,
                        writeText = {},
                        onStartClick = {},
                        onBackClick = {},
                    )
                }
            }

            onNodeWithText(NEW_RECORD_TEXT).assertExists()
            onAllNodes(newRecordLiveRegionMatcher).assertCountEquals(1)
        }

    @Test
    fun ordinaryResultOmitsNewRecordAndLiveRegion() =
        runComposeUiTest {
            setContent {
                OquTurboTheme {
                    RememberNumberScreen(
                        uiState =
                            RememberNumberUiState.Mistake(
                                text = "1234",
                                score = 4,
                                correctText = "1334",
                                record = 7,
                            ),
                        focusEvent = null,
                        maxLength = 4,
                        record = 7,
                        writeText = {},
                        onStartClick = {},
                        onBackClick = {},
                    )
                }
            }

            onNodeWithText(NEW_RECORD_TEXT).assertDoesNotExist()
            onAllNodes(newRecordLiveRegionMatcher).assertCountEquals(0)
        }

    private companion object {
        const val NEW_RECORD_TEXT = "New record!"
        val newRecordLiveRegionMatcher =
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            )
    }
}
