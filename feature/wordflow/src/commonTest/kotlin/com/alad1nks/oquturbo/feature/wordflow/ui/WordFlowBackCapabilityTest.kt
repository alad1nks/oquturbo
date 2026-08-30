package com.alad1nks.oquturbo.feature.wordflow.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordFlowBackCapabilityTest {
    @Test
    fun absentBackCapabilityReturnsNoAction() {
        assertNull(wordFlowBackAction(onBackClick = null, onAbandon = {}))
    }

    @Test
    fun presentBackCapabilityAbandonsBeforeNavigating() {
        val events = mutableListOf<String>()
        val action =
            wordFlowBackAction(
                onBackClick = { events += "back" },
                onAbandon = { events += "abandon" },
            )

        action?.invoke()

        assertEquals(listOf("abandon", "back"), events)
    }
}
