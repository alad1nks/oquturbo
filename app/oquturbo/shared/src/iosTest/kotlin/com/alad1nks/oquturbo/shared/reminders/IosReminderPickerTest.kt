@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDateComponents
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone
import platform.Foundation.setDefaultTimeZone
import platform.Foundation.timeZoneWithName
import platform.Foundation.valueForKey
import platform.UIKit.UIApplication
import platform.UIKit.UIButton
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIDatePicker
import platform.UIKit.UIView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosReminderPickerTest {
    @Test
    fun actualNativeViewLoadsAndRegisteredCancelResolvesOnlyOnce() {
        assertTrue(NSThread.isMainThread, "UIKit construction requires the main thread")
        val results = mutableListOf<Int?>()
        val sheet = IosReminderPicker(150, labels, results::add)
        sheet.loadViewIfNeeded()
        assertEquals("reminder-native-picker", identifier(sheet.view))
        val views = descendants(sheet.view)
        val picker = views.filterIsInstance<UIDatePicker>().single()
        assertEquals("reminder-time-wheel", identifier(picker))
        assertEquals(150, iosReminderTimeMinutes(picker.date))
        val buttons = views.filterIsInstance<UIButton>()
        val cancel =
            buttons.single {
                identifier(it) == "reminder-time-cancel"
            }
        assertTrue(
            buttons.any {
                identifier(it) == "reminder-time-confirm"
            },
        )
        invokeRegisteredAction(cancel, sheet, "cancel")
        assertEquals(listOf<Int?>(null), results)
        sheet.cancel()
        sheet.confirm()
        assertEquals(listOf<Int?>(null), results)
    }

    @Test
    fun registeredNativeConfirmUsesTheUntouchedWallClockDraftExactlyOnce() {
        assertTrue(NSThread.isMainThread, "UIKit construction requires the main thread")
        val results = mutableListOf<Int?>()
        val sheet = IosReminderPicker(150, labels, results::add)
        sheet.loadViewIfNeeded()
        val confirm =
            descendants(sheet.view).filterIsInstance<UIButton>().single {
                identifier(it) == "reminder-time-confirm"
            }
        invokeRegisteredAction(confirm, sheet, "confirm")
        assertEquals(listOf<Int?>(150), results)
        sheet.confirm()
        sheet.cancel()
        assertEquals(listOf<Int?>(150), results)
    }

    @Test
    fun nonUtcDeviceKeepsDisplayedAndConfirmedTimeInTheSameCoordinateSystem() {
        assertTrue(NSThread.isMainThread, "UIKit construction requires the main thread")
        val originalZone = NSTimeZone.defaultTimeZone
        try {
            for (zone in listOf("Asia/Almaty", "America/New_York")) {
                NSTimeZone.setDefaultTimeZone(requireNotNull(NSTimeZone.timeZoneWithName(zone)))
                assertEquals(zone, NSTimeZone.defaultTimeZone.name)
                for (edit in listOf(false, true)) {
                    val results = mutableListOf<Int?>()
                    val sheet = IosReminderPicker(478, labels, results::add)
                    sheet.loadViewIfNeeded()
                    val views = descendants(sheet.view)
                    val picker = views.filterIsInstance<UIDatePicker>().single()
                    val displayed =
                        (picker.calendar.copy() as NSCalendar).apply {
                            timeZone = requireNotNull(picker.timeZone)
                        }
                    assertEquals(0L, displayed.timeZone.secondsFromGMTForDate(picker.date))
                    val initial = displayed.components(NSCalendarUnitHour or NSCalendarUnitMinute, picker.date)
                    assertEquals(478L, initial.hour * 60 + initial.minute)
                    if (edit) {
                        // Build the edited date from the ACTUAL displayed calendar/zone, not the UTC helper.
                        val fields =
                            NSDateComponents().apply {
                                year = 1970
                                month = 1
                                day = 1
                                hour = 2
                                minute = 30
                            }
                        picker.setDate(requireNotNull(displayed.dateFromComponents(fields)), animated = false)
                        val selected = displayed.components(NSCalendarUnitHour or NSCalendarUnitMinute, picker.date)
                        assertEquals(150L, selected.hour * 60 + selected.minute)
                    }
                    val confirm =
                        views.filterIsInstance<UIButton>().single {
                            identifier(it) == "reminder-time-confirm"
                        }
                    invokeRegisteredAction(confirm, sheet, "confirm")
                    assertEquals(listOf<Int?>(if (edit) 150 else 478), results)
                    sheet.cancel()
                    assertEquals(1, results.size)
                }
            }
        } finally {
            NSTimeZone.setDefaultTimeZone(originalZone)
            assertEquals(originalZone.name, NSTimeZone.defaultTimeZone.name)
        }
    }

    private fun invokeRegisteredAction(button: UIButton, sheet: IosReminderPicker, expected: String) {
        // The standalone Kotlin runner has no UIApplicationMain. UIControl.sendAction delegates
        // to that missing singleton; the actual app XCTest still tests real button taps.
        memScoped {
            val signature = requireNotNull(NSMethodSignature.signatureWithObjCTypes("@@:".cstr.ptr))
            val result = alloc<ObjCObjectVar<UIApplication?>>()
            NSInvocation.invocationWithMethodSignature(signature).apply {
                target = UIApplication
                selector = NSSelectorFromString("sharedApplication")
                invoke()
                getReturnValue(result.ptr)
            }
            assertNull(result.value, "Standalone binding test requires a host without UIApplicationMain")
        }
        assertEquals(setOf(sheet), button.allTargets)
        val actions = button.actionsForTarget(sheet, forControlEvent = UIControlEventTouchUpInside)
        assertEquals(listOf(expected), actions)
        val selector = NSSelectorFromString(actions!!.single() as String)
        assertTrue(sheet.respondsToSelector(selector))
        // Dispatch the registered zero-argument, void Objective-C action without a Kotlin fallback.
        memScoped {
            val signature = requireNotNull(NSMethodSignature.signatureWithObjCTypes("v@:".cstr.ptr))
            NSInvocation.invocationWithMethodSignature(signature).apply {
                target = sheet
                this.selector = selector
                invoke()
            }
        }
    }

    private fun identifier(view: UIView): Any? {
        assertTrue(view.respondsToSelector(NSSelectorFromString("accessibilityIdentifier")))
        assertTrue(view.respondsToSelector(NSSelectorFromString("setAccessibilityIdentifier:")))
        return view.valueForKey("accessibilityIdentifier")
    }

    private fun descendants(view: UIView): List<UIView> =
        listOf(view) + view.subviews.filterIsInstance<UIView>().flatMap(::descendants)

    private val labels = ReminderPickerLabels("Reminder time", "Choose a device time", "Save", "Cancel")
}
