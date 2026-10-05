@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSSelectorFromString
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosReminderRequestTest {
    @Test
    fun everyAdditionalCalendarConstraintIsUnknownAndCannotConfirmScheduling() {
        val schedule = ReminderSchedule(150, ReminderContent("en", "Title", "Body"))
        val original = iosReminderRequest(schedule)
        val restrictions: List<NSDateComponents.() -> Unit> =
            listOf(
                { era = 1 },
                { year = 2030 },
                { month = 3 },
                { day = 8 },
                { second = 0 },
                { weekday = 1 },
                { quarter = 1 },
                { weekdayOrdinal = 1 },
                { weekOfMonth = 1 },
                { weekOfYear = 1 },
                { yearForWeekOfYear = 2030 },
                { nanosecond = 0 },
                { dayOfYear = 1 },
                { leapMonth = true },
            )
        val supported =
            if (NSDateComponents().respondsToSelector(NSSelectorFromString("setRepeatedDay:"))) {
                restrictions + listOf<NSDateComponents.() -> Unit>({ repeatedDay = true })
            } else {
                restrictions
            }
        supported.forEach { restrict ->
            val components =
                NSDateComponents().apply {
                    hour = 2
                    minute = 30
                    restrict()
                }
            val request =
                UNNotificationRequest.requestWithIdentifier(
                    IOS_REMINDER_ID,
                    original.content,
                    UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true),
                )
            val pending = iosReminderPending(listOf(request))
            assertEquals(ReminderPending.Unknown, pending)
            assertFailsWith<IllegalStateException> { requireIosReminderAgreement(schedule, pending) }
        }
        requireIosReminderAgreement(schedule, iosReminderPending(listOf(original)))
        for (pending in listOf(
            ReminderPending.Unknown,
            ReminderPending.Known(null),
            ReminderPending.Known(schedule.copy(minutesOfDay = 151)),
            ReminderPending.Known(schedule.copy(content = schedule.content.copy(title = "Changed"))),
        )) {
            assertFailsWith<IllegalStateException> { requireIosReminderAgreement(schedule, pending) }
        }
    }

    @Test
    fun ownedRequestUsesNativeFloatingRepeatingCalendarAndPreservesContent() {
        for (minute in listOf(0, 1439)) {
            val schedule = ReminderSchedule(minute, ReminderContent("kk", "Қазақша", "Күнделікті жаттығу"))
            val request = iosReminderRequest(schedule)
            assertEquals(IOS_REMINDER_ID, request.identifier)
            val trigger = assertIs<UNCalendarNotificationTrigger>(request.trigger)
            assertTrue(trigger.repeats)
            assertNull(trigger.dateComponents.timeZone)
            assertNull(trigger.dateComponents.calendar)
            assertTrue(requireNotNull(trigger.nextTriggerDate()).timeIntervalSince1970 > NSDate().timeIntervalSince1970)
            assertEquals(ReminderPending.Known(schedule), iosReminderPending(listOf(request)))
        }
    }

    @Test
    fun unexpectedOwnedShapeIsUnknownAndForeignRequestIsNotAnOwnedSchedule() {
        val request = iosReminderRequest(ReminderSchedule(600, ReminderContent("en", "Title", "Body")))
        val foreign = UNNotificationRequest.requestWithIdentifier("unrelated", request.content, request.trigger)
        assertEquals(ReminderPending.Known(null), iosReminderPending(listOf(foreign)))
        val interval =
            UNNotificationRequest.requestWithIdentifier(
                IOS_REMINDER_ID,
                request.content,
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(60.0, repeats = true),
            )
        assertEquals(ReminderPending.Unknown, iosReminderPending(listOf(interval)))
        assertEquals(ReminderPending.Unknown, iosReminderPending(listOf(request, request)))
        val date =
            NSDateComponents().apply {
                year = 2030
                hour = 10
                minute = 0
            }
        val dated =
            UNNotificationRequest.requestWithIdentifier(
                IOS_REMINDER_ID,
                request.content,
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(date, repeats = true),
            )
        assertEquals(ReminderPending.Unknown, iosReminderPending(listOf(dated)))
    }
}
