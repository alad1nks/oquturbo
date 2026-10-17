package com.alad1nks.oquturbo.feature.numbertrail.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberTrailDurationDisplayPartsTest {
    @Test
    fun floorsSecondsAndKeepsLongMinutesWithoutOverflow() {
        listOf(-1L, 0L, 999L).forEach {
            assertEquals(NumberTrailDurationDisplayParts.LessThanOneSecond, numberTrailDurationDisplayParts(it))
        }
        mapOf(1_000L to 1L, 1_999L to 1L, 59_999L to 59L).forEach { (millis, seconds) ->
            assertEquals(NumberTrailDurationDisplayParts.Seconds(seconds), numberTrailDurationDisplayParts(millis))
        }
        mapOf(
            60_000L to NumberTrailDurationDisplayParts.MinutesSeconds(1, null),
            65_300L to NumberTrailDurationDisplayParts.MinutesSeconds(1, 5),
            120_000L to NumberTrailDurationDisplayParts.MinutesSeconds(2, null),
            3_600_000L to NumberTrailDurationDisplayParts.MinutesSeconds(60, null),
            7_505_000L to NumberTrailDurationDisplayParts.MinutesSeconds(125, 5),
            725_300L to NumberTrailDurationDisplayParts.MinutesSeconds(12, 5),
            Long.MAX_VALUE to NumberTrailDurationDisplayParts.MinutesSeconds(153_722_867_280_912, 55),
        ).forEach { (millis, parts) -> assertEquals(parts, numberTrailDurationDisplayParts(millis)) }
    }

    @Test
    fun pluralSelectionKeepsActualLastDigitsForLargeValues() {
        listOf(1L, 2L, 5L, 11L, 21L, Int.MAX_VALUE.toLong()).forEach {
            assertEquals(it.toInt(), it.numberTrailPluralQuantity())
        }
        assertEquals(121, 3_000_000_021L.numberTrailPluralQuantity())
        assertEquals(112, 153_722_867_280_912L.numberTrailPluralQuantity())
    }
}
