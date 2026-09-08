package com.alad1nks.oquturbo.feature.remembernumber.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class RememberNumberDurationDisplayPartsTest {
    @Test
    fun floorsSecondsAndKeepsLongMinutesWithoutOverflow() {
        listOf(-1L, 0L, 999L).forEach {
            assertEquals(RememberNumberDurationDisplayParts.LessThanOneSecond, rememberNumberDurationDisplayParts(it))
        }
        mapOf(1_000L to 1L, 1_999L to 1L, 59_999L to 59L).forEach { (millis, seconds) ->
            assertEquals(
                RememberNumberDurationDisplayParts.Seconds(seconds),
                rememberNumberDurationDisplayParts(millis),
            )
        }
        mapOf(
            60_000L to RememberNumberDurationDisplayParts.MinutesSeconds(1, 0),
            65_999L to RememberNumberDurationDisplayParts.MinutesSeconds(1, 5),
            120_000L to RememberNumberDurationDisplayParts.MinutesSeconds(2, 0),
            725_300L to RememberNumberDurationDisplayParts.MinutesSeconds(12, 5),
            Long.MAX_VALUE to RememberNumberDurationDisplayParts.MinutesSeconds(153_722_867_280_912, 55),
        ).forEach { (millis, parts) -> assertEquals(parts, rememberNumberDurationDisplayParts(millis)) }
    }

    @Test
    fun pluralSelectionKeepsActualLastDigitsForLargeValues() {
        listOf(1L, 2L, 5L, 11L, 21L, Int.MAX_VALUE.toLong()).forEach {
            assertEquals(it.toInt(), it.rememberNumberPluralQuantity())
        }
        assertEquals(121, 3_000_000_021L.rememberNumberPluralQuantity())
        assertEquals(112, 153_722_867_280_912L.rememberNumberPluralQuantity())
    }
}
