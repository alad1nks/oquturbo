package com.alad1nks.oquturbo.feature.wordflow.navigation

import com.alad1nks.oquturbo.feature.wordflow.model.ANSWER_SLOT
import com.alad1nks.oquturbo.feature.wordflow.model.WordFlowTier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class WordFlowResourceLoadingTest {
    @Test
    fun waitsForAllResourcesBeforePublishingContent() =
        runTest {
            val finalReadStarted = CompletableDeferred<Unit>()
            val finishFinalRead = CompletableDeferred<Unit>()
            val arrays = validArrays()
            val content =
                async {
                    loadLocalizedWordFlowContent {
                        if (arrays.size == 1) {
                            finalReadStarted.complete(Unit)
                            finishFinalRead.await()
                        }
                        arrays.removeAt(0)
                    }
                }

            finalReadStarted.await()
            assertFalse(content.isCompleted)
            finishFinalRead.complete(Unit)

            val prompts = content.await().prompts
            assertEquals(18, prompts.size)
            assertEquals(
                WordFlowTier.entries.flatMap { tier -> (1..6).map { "${tier.name.lowercase()}-$it" } },
                prompts.map { it.id },
            )
            assertEquals(listOf("correct", "wrong-a", "wrong-b"), prompts.first().answers)
        }

    @Test
    fun rejectsMalformedArraysAfterReadCompletes() =
        runTest {
            val arrays = validArrays().apply { this[0] = emptyList() }

            val failure =
                assertFailsWith<IllegalArgumentException> {
                    loadLocalizedWordFlowContent { arrays.removeAt(0) }
                }

            assertEquals("Word Flow Easy prompt arrays must contain six aligned entries", failure.message)
        }

    private fun validArrays(): MutableList<List<String>> =
        WordFlowTier.entries.flatMap {
            listOf(
                List(6) { "Choose $ANSWER_SLOT." },
                List(6) { "correct" },
                List(6) { "wrong-a" },
                List(6) { "wrong-b" },
            )
        }.toMutableList()
}
