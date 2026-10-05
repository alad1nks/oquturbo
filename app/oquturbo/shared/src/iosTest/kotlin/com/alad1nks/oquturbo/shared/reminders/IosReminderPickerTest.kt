@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
import platform.Foundation.valueForKey
import platform.UIKit.UIButton
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIDatePicker
import platform.UIKit.UIView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosReminderPickerTest {
    @Test
    fun actualNativeViewLoadsAndCancelResolvesOnlyOnce() {
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
        cancel.sendActionsForControlEvents(UIControlEventTouchUpInside)
        assertEquals(listOf<Int?>(null), results)
        sheet.cancel()
        sheet.confirm()
        assertEquals(listOf<Int?>(null), results)
    }

    @Test
    fun nativeConfirmUsesTheUntouchedWallClockDraftExactlyOnce() {
        assertTrue(NSThread.isMainThread, "UIKit construction requires the main thread")
        val results = mutableListOf<Int?>()
        val sheet = IosReminderPicker(150, labels, results::add)
        sheet.loadViewIfNeeded()
        val confirm =
            descendants(sheet.view).filterIsInstance<UIButton>().single {
                identifier(it) == "reminder-time-confirm"
            }
        confirm.sendActionsForControlEvents(UIControlEventTouchUpInside)
        assertEquals(listOf<Int?>(150), results)
        sheet.confirm()
        sheet.cancel()
        assertEquals(listOf<Int?>(150), results)
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
