package com.alad1nks.oquturbo.shared.reminders

import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderCalendarTest {
    @Test
    fun exactMinuteAndPastMinuteChooseTomorrowAndMidnightDoesNotCatchUp() {
        val now = Instant.parse("2026-10-04T12:30:00Z").toEpochMilli()
        assertEquals(
            Instant.parse("2026-10-05T12:30:00Z").toEpochMilli(),
            nextReminderTime(750, now, TimeZone.getTimeZone("UTC")),
        )
        assertEquals(
            Instant.parse("2026-10-05T00:00:00Z").toEpochMilli(),
            nextReminderTime(0, now, TimeZone.getTimeZone("UTC")),
        )
        assertEquals(
            Instant.parse("2026-10-04T23:59:00Z").toEpochMilli(),
            nextReminderTime(1439, now, TimeZone.getTimeZone("UTC")),
        )
    }

    @Test
    fun localCalendarDayAcrossDstIsNotTwentyFourHourRepeatAndTravelUsesNewZone() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val spring = Instant.parse("2026-03-07T14:00:00Z").toEpochMilli()
        val fall = Instant.parse("2026-10-31T13:00:00Z").toEpochMilli()
        assertEquals(23 * 3_600_000L, nextReminderTime(540, spring, zone) - spring)
        assertEquals(25 * 3_600_000L, nextReminderTime(540, fall, zone) - fall)
        val now = Instant.parse("2026-10-04T01:00:00Z").toEpochMilli()
        for (id in listOf("Asia/Almaty", "Europe/Berlin", "America/New_York")) {
            val target = nextReminderTime(540, now, TimeZone.getTimeZone(id))
            assertTrue(target > now)
            assertEquals(9, Instant.ofEpochMilli(target).atZone(ZoneId.of(id)).hour)
        }
    }

    @Test
    fun nativeGapAndFoldResolutionAlwaysProducesFutureOccurrence() {
        for (now in listOf("2026-03-08T06:00:00Z", "2026-11-01T05:45:00Z", "2026-11-01T06:45:00Z")) {
            val epoch = Instant.parse(now).toEpochMilli()
            for (minute in listOf(90, 150)) {
                assertTrue(nextReminderTime(minute, epoch, TimeZone.getTimeZone("America/New_York")) > epoch)
            }
        }
    }
}
