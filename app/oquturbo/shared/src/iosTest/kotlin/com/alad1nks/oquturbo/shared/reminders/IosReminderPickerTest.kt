@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
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
