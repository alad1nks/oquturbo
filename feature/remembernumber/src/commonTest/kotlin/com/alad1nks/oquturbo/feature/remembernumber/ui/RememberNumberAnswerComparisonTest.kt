package com.alad1nks.oquturbo.feature.remembernumber.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RememberNumberAnswerComparisonTest {
    @Test
    fun preservesTenDigitValuesAndMarksOnlyFirstAndLastSubmittedDigitsAsMismatches() {
        val comparison =
            requireNotNull(
                RememberNumberUiState.Mistake(
                    text = "0123456789",
                    score = 8,
                    correctText = "9123456780",
                    record = 12,
                ).answerComparisonOrNull(),
            )

        assertEquals("0123456789", comparison.submitted.value)
        assertEquals("9123456780", comparison.correct.value)
        assertEquals("0 1 2 3 4 5 6 7 8 9", comparison.submitted.digitSpacedValue)
        assertEquals(
            listOf(0, 9),
            comparison.submitted.digits.indices.filterNot {
                comparison.submitted.digits[it].isMatching
            },
        )
        assertTrue(comparison.submitted.digits.slice(1..8).all { it.isMatching })
        assertTrue(comparison.correct.digits.all { it.isMatching })
    }

    @Test
    fun preservesShortAnswerOrderingAndIndependentLengths() {
        val comparison =
            requireNotNull(
                RememberNumberUiState.Mistake(
                    text = "07",
                    score = 1,
                    correctText = "709",
                    record = 3,
                ).answerComparisonOrNull(),
            )

        assertEquals(listOf('0', '7'), comparison.submitted.digits.map { it.value })
        assertEquals(listOf('7', '0', '9'), comparison.correct.digits.map { it.value })
        assertEquals(2, comparison.submitted.digits.size)
        assertEquals(3, comparison.correct.digits.size)
        assertFalse(comparison.submitted.digits[0].isMatching)
        assertFalse(comparison.submitted.digits[1].isMatching)
    }

    @Test
    fun retryStatesHaveNoMistakeAnswerPayload() {
        assertNull(RememberNumberUiState.Initial().answerComparisonOrNull())
        assertNull(RememberNumberUiState.Reading(text = "0123", score = 0).answerComparisonOrNull())
        assertNull(RememberNumberUiState.Writing(text = "", score = 0).answerComparisonOrNull())
    }
}
