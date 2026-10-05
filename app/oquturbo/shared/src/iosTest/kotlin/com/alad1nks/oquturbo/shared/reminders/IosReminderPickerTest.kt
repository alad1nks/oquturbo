@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import platform.Foundation.NSThread
import platform.UIKit.UIAccessibilityIdentificationProtocol
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
        val views = descendants(sheet.view)
        val picker = views.filterIsInstance<UIDatePicker>().single()
        assertEquals("reminder-time-wheel", (picker as UIAccessibilityIdentificationProtocol).accessibilityIdentifier)
        assertEquals(150, iosReminderTimeMinutes(picker.date))
        val buttons = views.filterIsInstance<UIButton>()
        val cancel =
            buttons.single {
                (it as UIAccessibilityIdentificationProtocol).accessibilityIdentifier == "reminder-time-cancel"
            }
        assertTrue(
            buttons.any {
                (it as UIAccessibilityIdentificationProtocol).accessibilityIdentifier == "reminder-time-confirm"
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
                (it as UIAccessibilityIdentificationProtocol).accessibilityIdentifier == "reminder-time-confirm"
            }
        confirm.sendActionsForControlEvents(UIControlEventTouchUpInside)
        assertEquals(listOf<Int?>(150), results)
        sheet.confirm()
        sheet.cancel()
        assertEquals(listOf<Int?>(150), results)
    }

    private fun descendants(view: UIView): List<UIView> =
        listOf(view) + view.subviews.filterIsInstance<UIView>().flatMap(::descendants)

    private val labels = ReminderPickerLabels("Reminder time", "Choose a device time", "Save", "Cancel")
}
