package com.alad1nks.oquturbo.feature.dualfocus.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DualFocusBackCapabilityTest {
    @Test
    fun absentBackCapabilityReturnsNoAction() {
        assertNull(dualFocusBackAction(onBackClick = null, onAbandon = {}))
    }

    @Test
    fun presentBackCapabilityAbandonsBeforeNavigating() {
        val events = mutableListOf<String>()
        val action =
            dualFocusBackAction(
                onBackClick = { events += "back" },
                onAbandon = { events += "abandon" },
            )

        action?.invoke()

        assertEquals(listOf("abandon", "back"), events)
    }
}
