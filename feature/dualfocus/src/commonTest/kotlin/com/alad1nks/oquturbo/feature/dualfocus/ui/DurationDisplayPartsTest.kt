package com.alad1nks.oquturbo.feature.dualfocus.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationDisplayPartsTest {
    @Test
    fun negativeAndSubsecondDurationsUseLessThanOneSecond() {
        listOf(-1L, 0L, 999L).forEach { durationMillis ->
            assertEquals(DurationDisplayParts.LessThanOneSecond, durationDisplayParts(durationMillis))
        }
    }

    @Test
    fun secondDurationsTruncateSubsecondRemainders() {
        assertEquals(DurationDisplayParts.Seconds(1), durationDisplayParts(1_000))
        assertEquals(DurationDisplayParts.Seconds(2), durationDisplayParts(2_000))
        assertEquals(DurationDisplayParts.Seconds(59), durationDisplayParts(59_999))
    }

    @Test
    fun exactMinutesOmitZeroSeconds() {
        assertEquals(DurationDisplayParts.MinutesSeconds(1, null), durationDisplayParts(60_000))
        assertEquals(DurationDisplayParts.MinutesSeconds(2, null), durationDisplayParts(120_000))
    }

    @Test
    fun minuteDurationsRetainWholeSecondRemainder() {
        assertEquals(DurationDisplayParts.MinutesSeconds(1, 5), durationDisplayParts(65_300))
        assertEquals(DurationDisplayParts.MinutesSeconds(12, 5), durationDisplayParts(725_300))
    }
}
