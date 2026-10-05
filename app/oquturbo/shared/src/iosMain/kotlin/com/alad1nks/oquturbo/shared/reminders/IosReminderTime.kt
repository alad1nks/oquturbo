@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT

/** A time-only UI coordinate system, independent of today's zone/date and its DST gaps. */
internal fun iosReminderTimeCalendar(): NSCalendar =
    NSCalendar(NSCalendarIdentifierGregorian).apply {
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    }

internal fun iosReminderTimeDate(minutes: Int): NSDate {
    require(minutes in 0..1439)
    return NSDate.dateWithTimeIntervalSince1970(minutes * 60.0)
}

internal fun iosReminderTimeMinutes(date: NSDate): Int {
    val fields = iosReminderTimeCalendar().components(NSCalendarUnitHour or NSCalendarUnitMinute, date)
    return (fields.hour * 60 + fields.minute).toInt()
}

internal fun iosReminderTimeFormatter(): NSDateFormatter =
    NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        calendar = iosReminderTimeCalendar()
        timeZone = calendar.timeZone
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }
