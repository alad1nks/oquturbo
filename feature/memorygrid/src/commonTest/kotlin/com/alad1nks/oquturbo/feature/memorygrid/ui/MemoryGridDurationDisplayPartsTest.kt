package com.alad1nks.oquturbo.feature.memorygrid.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryGridDurationDisplayPartsTest {
    @Test
    fun floorsSecondsAndKeepsLongMinutesWithoutOverflow() {
        listOf(-1L, 0L, 999L).forEach {
            assertEquals(MemoryGridDurationDisplayParts.LessThanOneSecond, memoryGridDurationDisplayParts(it))
        }
        mapOf(1_000L to 1L, 59_999L to 59L).forEach { (millis, seconds) ->
            assertEquals(MemoryGridDurationDisplayParts.Seconds(seconds), memoryGridDurationDisplayParts(millis))
        }
        mapOf(
            60_000L to MemoryGridDurationDisplayParts.MinutesSeconds(1, null),
            61_999L to MemoryGridDurationDisplayParts.MinutesSeconds(1, 1),
            725_300L to MemoryGridDurationDisplayParts.MinutesSeconds(12, 5),
            Long.MAX_VALUE to MemoryGridDurationDisplayParts.MinutesSeconds(153_722_867_280_912, 55),
        ).forEach { (millis, parts) -> assertEquals(parts, memoryGridDurationDisplayParts(millis)) }
    }

    @Test
    fun pluralSelectionKeepsActualLastDigitsForLargeValues() {
        listOf(1L, 2L, 5L, 11L, 21L, Int.MAX_VALUE.toLong()).forEach {
            assertEquals(it.toInt(), it.memoryGridPluralQuantity())
        }
        assertEquals(121, 3_000_000_021L.memoryGridPluralQuantity())
        assertEquals(112, 153_722_867_280_912L.memoryGridPluralQuantity())
    }
}
