@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IosReminderTimeTest {
    @Test
    fun gapDayCannotNormalizeSavedTimeOrAnUntouchedPickerDraft() {
        val local =
            NSCalendar(NSCalendarIdentifierGregorian).apply {
                timeZone = requireNotNull(NSTimeZone.timeZoneWithName("America/New_York"))
            }
        // 2026-03-08 01:00 EST, immediately before the missing 02:00 hour.
        val gapDay = NSDate.dateWithTimeIntervalSince1970(1772949600.0)
        val oldDate = requireNotNull(local.dateBySettingHour(2, 30, 0, gapDay, 0u))
        val old = local.components(NSCalendarUnitHour or NSCalendarUnitMinute, oldDate)
        assertNotEquals(150L, old.hour * 60 + old.minute)
        val formatter =
            iosReminderTimeFormatter().apply {
                locale = NSLocale("en_US_POSIX")
                dateFormat = "HH:mm"
            }
        for ((minutes, expected) in listOf(0 to "00:00", 150 to "02:30", 1439 to "23:59")) {
            val draft = iosReminderTimeDate(minutes)
            assertEquals(minutes, iosReminderTimeMinutes(draft))
            assertEquals(expected, formatter.stringFromDate(draft))
        }
        formatter.dateFormat = "h:mm a"
        assertEquals("2:30 AM", formatter.stringFromDate(iosReminderTimeDate(150)))
    }
}
