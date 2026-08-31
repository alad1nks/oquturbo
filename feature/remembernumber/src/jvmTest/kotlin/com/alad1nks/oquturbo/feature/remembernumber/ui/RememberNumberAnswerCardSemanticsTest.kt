package com.alad1nks.oquturbo.feature.remembernumber.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RememberNumberAnswerCardSemanticsTest {
    @Test
    fun answerGroupsRemainStandaloneAndOrderedInsideClickableParent() =
        runComposeUiTest {
            var parentClicked = false
            val submittedDescription = "Your answer: 1 2"
            val correctDescription = "Correct answer: 1 3"
            val comparison =
                requireNotNull(
                    RememberNumberUiState.Mistake(
                        text = "12",
                        score = 1,
                        correctText = "13",
                        record = 2,
                    ).answerComparisonOrNull(),
                )

            setContent {
                MaterialTheme {
                    Box(
                        modifier = Modifier.clickable { parentClicked = true },
                    ) {
                        RememberNumberAnswerCard(
                            comparison = comparison,
                            submittedLabel = "Your answer",
                            submittedDescription = submittedDescription,
                            correctLabel = "Correct answer",
                            correctDescription = correctDescription,
                        )
                    }
                }
            }

            onAllNodes(
                hasContentDescription(submittedDescription)
                    .or(hasContentDescription(correctDescription)),
            ).assertCountEquals(2)

            val submittedNode = onNodeWithContentDescription(submittedDescription).fetchSemanticsNode()
            val correctNode = onNodeWithContentDescription(correctDescription).fetchSemanticsNode()
            val parentNode = onNode(hasClickAction()).fetchSemanticsNode()

            assertNotEquals(submittedNode.id, correctNode.id)
            assertNotEquals(parentNode.id, submittedNode.id)
            assertNotEquals(parentNode.id, correctNode.id)
            assertEquals(0f, submittedNode.config[SemanticsProperties.TraversalIndex])
            assertEquals(1f, correctNode.config[SemanticsProperties.TraversalIndex])

            onNode(hasClickAction()).performClick()
            assertTrue(parentClicked)
        }
}
