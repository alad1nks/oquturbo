package com.alad1nks.oquturbo.feature.wordflow.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class WordFlowDurationDisplayPartsTest {
    @Test
    fun negativeAndSubsecondDurationsUseLessThanOneSecond() {
        listOf(-1L, 0L, 999L).forEach { durationMillis ->
            assertEquals(WordFlowDurationDisplayParts.LessThanOneSecond, wordFlowDurationDisplayParts(durationMillis))
        }
    }

    @Test
    fun secondDurationsTruncateSubsecondRemainders() {
        assertEquals(WordFlowDurationDisplayParts.Seconds(1), wordFlowDurationDisplayParts(1_000))
        assertEquals(WordFlowDurationDisplayParts.Seconds(59), wordFlowDurationDisplayParts(59_999))
    }

    @Test
    fun exactMinutesOmitZeroSeconds() {
        assertEquals(WordFlowDurationDisplayParts.MinutesSeconds(1, null), wordFlowDurationDisplayParts(60_000))
        assertEquals(WordFlowDurationDisplayParts.MinutesSeconds(2, null), wordFlowDurationDisplayParts(120_000))
    }

    @Test
    fun minuteDurationsRetainWholeSecondRemainder() {
        assertEquals(WordFlowDurationDisplayParts.MinutesSeconds(1, 5), wordFlowDurationDisplayParts(65_300))
        assertEquals(WordFlowDurationDisplayParts.MinutesSeconds(12, 5), wordFlowDurationDisplayParts(725_300))
    }

    @Test
    fun longDurationsKeepLongMinuteValues() {
        val durationMillis = Long.MAX_VALUE
        assertEquals(
            WordFlowDurationDisplayParts.MinutesSeconds(
                minutes = durationMillis / 60_000,
                seconds = (durationMillis / 1_000) % 60,
            ),
            wordFlowDurationDisplayParts(durationMillis),
        )
    }
}
