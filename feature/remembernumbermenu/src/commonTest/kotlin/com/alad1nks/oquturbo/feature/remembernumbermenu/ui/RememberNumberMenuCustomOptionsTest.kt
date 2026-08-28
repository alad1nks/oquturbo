package com.alad1nks.oquturbo.feature.remembernumbermenu.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RememberNumberMenuCustomOptionsTest {
    @Test
    fun digitSelectionRequiresAtLeastTwoSelectedDigits() {
        assertFalse(List(10) { false }.isDigitSelectionValid())
        assertFalse(List(10) { it == 0 }.isDigitSelectionValid())
        assertTrue(List(10) { it < 2 }.isDigitSelectionValid())
        assertTrue(List(10) { true }.isDigitSelectionValid())
    }

    @Test
    fun digitSelectionValidityTracksSelectionTransitions() {
        val digitsAvailability = MutableList(10) { false }

        assertFalse(digitsAvailability.isDigitSelectionValid())

        digitsAvailability[0] = true
        assertFalse(digitsAvailability.isDigitSelectionValid())

        digitsAvailability[7] = true
        assertTrue(digitsAvailability.isDigitSelectionValid())

        digitsAvailability[0] = false
        assertFalse(digitsAvailability.isDigitSelectionValid())

        digitsAvailability[4] = true
        assertTrue(digitsAvailability.isDigitSelectionValid())
    }
}
