@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSSelectorFromString
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIAccessibilityIdentificationProtocol
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIButton
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleBody
import platform.UIKit.UIFontTextStyleTitle2
import platform.UIKit.UILabel
import platform.UIKit.UILayoutConstraintAxisVertical
import platform.UIKit.UIModalPresentationPageSheet
import platform.UIKit.UIPresentationController
import platform.UIKit.UIScrollView
import platform.UIKit.UIStackView
import platform.UIKit.UIViewController
import platform.UIKit.systemBackgroundColor
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Native draft only. Every dismissal path resolves once without performing a preference write. */
internal class IosReminderPicker(
    private val initialMinutes: Int?,
    private val labels: ReminderPickerLabels,
    private var result: ((Int?) -> Unit)?,
) : UIViewController(nibName = null, bundle = null), UIAdaptivePresentationControllerDelegateProtocol {
    private val time = UIDatePicker()

    init {
        modalPresentationStyle = UIModalPresentationPageSheet
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackgroundColor
        (view as UIAccessibilityIdentificationProtocol).accessibilityIdentifier = "reminder-native-picker"
        val scroll = UIScrollView().apply { translatesAutoresizingMaskIntoConstraints = false }
        val stack =
            UIStackView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                axis = UILayoutConstraintAxisVertical
                spacing = 16.0
            }
        val title =
            UILabel().apply {
                text = labels.title
                font = UIFont.preferredFontForTextStyle(UIFontTextStyleTitle2!!)
                adjustsFontForContentSizeCategory = true
                numberOfLines = 0
            }
        val helper =
            UILabel().apply {
                text = labels.helper
                font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody!!)
                adjustsFontForContentSizeCategory = true
                numberOfLines = 0
            }
        time.datePickerMode = UIDatePickerMode.UIDatePickerModeTime
        time.preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
        time.minuteInterval = 1
        (time as UIAccessibilityIdentificationProtocol).accessibilityIdentifier = "reminder-time-wheel"
        val localNow = NSCalendar.currentCalendar.components(NSCalendarUnitHour or NSCalendarUnitMinute, NSDate())
        val draft = initialMinutes ?: (localNow.hour * 60 + localNow.minute).toInt()
        time.calendar = iosReminderTimeCalendar()
        time.timeZone = time.calendar.timeZone
        time.setDate(iosReminderTimeDate(draft), animated = false)

        val confirm = button(labels.confirm, "confirm", "reminder-time-confirm")
        val cancel = button(labels.cancel, "cancel", "reminder-time-cancel")
        view.addSubview(scroll)
        scroll.addSubview(stack)
        listOf(title, helper, time, confirm, cancel).forEach(stack::addArrangedSubview)
        NSLayoutConstraint.activateConstraints(
            listOf(
                scroll.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
                scroll.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor),
                scroll.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
                scroll.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
                stack.topAnchor.constraintEqualToAnchor(scroll.contentLayoutGuide.topAnchor, 24.0),
                stack.bottomAnchor.constraintEqualToAnchor(scroll.contentLayoutGuide.bottomAnchor, -24.0),
                stack.leadingAnchor.constraintEqualToAnchor(scroll.contentLayoutGuide.leadingAnchor, 16.0),
                stack.trailingAnchor.constraintEqualToAnchor(scroll.contentLayoutGuide.trailingAnchor, -16.0),
                stack.widthAnchor.constraintEqualToAnchor(
                    scroll.frameLayoutGuide.widthAnchor,
                    multiplier = 1.0,
                    constant = -32.0,
                ),
                confirm.heightAnchor.constraintGreaterThanOrEqualToConstant(44.0),
                cancel.heightAnchor.constraintGreaterThanOrEqualToConstant(44.0),
            ),
        )
    }

    private fun button(text: String, selector: String, identifier: String): UIButton =
        UIButton.buttonWithType(UIButtonTypeSystem).apply {
            setTitle(text, UIControlStateNormal)
            titleLabel?.numberOfLines = 0
            titleLabel?.adjustsFontForContentSizeCategory = true
            titleLabel?.font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody!!)
            (this as UIAccessibilityIdentificationProtocol).accessibilityIdentifier = identifier
            addTarget(this@IosReminderPicker, NSSelectorFromString(selector), UIControlEventTouchUpInside)
        }

    @ObjCAction
    fun confirm() {
        finish(iosReminderTimeMinutes(time.date))
    }

    @ObjCAction
    fun cancel() = finish(null)

    fun cancelOnMain() {
        dispatch_async(dispatch_get_main_queue()) { cancel() }
    }

    private fun finish(minutes: Int?) {
        val callback = result ?: return
        result = null
        callback(minutes)
        dismissViewControllerAnimated(true, completion = null)
    }

    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
        val callback = result ?: return
        result = null
        callback(null)
    }
}
