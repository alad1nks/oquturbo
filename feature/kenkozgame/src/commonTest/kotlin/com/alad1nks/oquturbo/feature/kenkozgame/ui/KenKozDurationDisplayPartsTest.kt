package com.alad1nks.oquturbo.feature.kenkozgame.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class KenKozDurationDisplayPartsTest {
    @Test
    fun floorsSecondsAndKeepsLongMinutesWithoutOverflow() {
        listOf(-1L, 0L, 999L).forEach {
            assertEquals(KenKozDurationDisplayParts.LessThanOneSecond, kenKozDurationDisplayParts(it))
        }
        mapOf(1_000L to 1L, 1_999L to 1L, 59_999L to 59L).forEach { (millis, seconds) ->
            assertEquals(
                KenKozDurationDisplayParts.Seconds(seconds),
                kenKozDurationDisplayParts(millis),
            )
        }
        mapOf(
            60_000L to KenKozDurationDisplayParts.MinutesSeconds(1, 0),
            61_999L to KenKozDurationDisplayParts.MinutesSeconds(1, 1),
            65_999L to KenKozDurationDisplayParts.MinutesSeconds(1, 5),
            120_000L to KenKozDurationDisplayParts.MinutesSeconds(2, 0),
            725_300L to KenKozDurationDisplayParts.MinutesSeconds(12, 5),
            Long.MAX_VALUE to KenKozDurationDisplayParts.MinutesSeconds(153_722_867_280_912, 55),
        ).forEach { (millis, parts) -> assertEquals(parts, kenKozDurationDisplayParts(millis)) }
    }

    @Test
    fun pluralSelectionKeepsActualLastDigitsForLargeValues() {
        listOf(1L, 2L, 5L, 11L, 21L, Int.MAX_VALUE.toLong()).forEach {
            assertEquals(it.toInt(), it.kenKozPluralQuantity())
        }
        assertEquals(121, 3_000_000_021L.kenKozPluralQuantity())
        assertEquals(112, 153_722_867_280_912L.kenKozPluralQuantity())
    }
}
