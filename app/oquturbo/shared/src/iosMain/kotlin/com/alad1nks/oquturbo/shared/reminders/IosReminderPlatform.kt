@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import com.alad1nks.oquturbo.core.data.model.ReminderContent
import com.alad1nks.oquturbo.core.data.model.ReminderSchedule
import com.alad1nks.oquturbo.core.data.reminders.ReminderAuthorization
import com.alad1nks.oquturbo.core.data.reminders.ReminderCapability
import com.alad1nks.oquturbo.core.data.reminders.ReminderNativeState
import com.alad1nks.oquturbo.core.data.reminders.ReminderPending
import com.alad1nks.oquturbo.core.data.reminders.ReminderPickerLabels
import com.alad1nks.oquturbo.core.data.reminders.ReminderPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSDateComponentUndefined
import platform.Foundation.NSDateComponents
import platform.Foundation.NSLocale
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UIKit.UIViewController
import platform.UIKit.presentationController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSettingEnabled
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val IOS_REMINDER_ID = "oquturbo_practice_reminder"
internal const val IOS_REMINDER_LANGUAGE = "oquturboReminderLanguage"

internal fun iosReminderRequest(schedule: ReminderSchedule): UNNotificationRequest {
    val content =
        UNMutableNotificationContent().apply {
            setTitle(schedule.content.title)
            setBody(schedule.content.body)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf(IOS_REMINDER_LANGUAGE to schedule.content.languageCode))
        }
    val components =
        NSDateComponents().apply {
            hour = (schedule.minutesOfDay / 60).toLong()
            minute = (schedule.minutesOfDay % 60).toLong()
            // No timezone, calendar, date or interval: the OS resolves floating local time and DST.
        }
    return UNNotificationRequest.requestWithIdentifier(
        IOS_REMINDER_ID,
        content,
        UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true),
    )
}

internal fun iosReminderPending(requests: List<UNNotificationRequest>): ReminderPending {
    val owned = requests.filter { it.identifier == IOS_REMINDER_ID }
    if (owned.isEmpty()) return ReminderPending.Known(null)
    if (owned.size != 1) return ReminderPending.Unknown
    val request = owned.single()
    val trigger = request.trigger as? UNCalendarNotificationTrigger ?: return ReminderPending.Unknown
    val date = trigger.dateComponents
    if (!trigger.repeats || date.timeZone != null || date.calendar != null ||
        date.hour !in 0L..23L || date.minute !in 0L..59L ||
        date.leapMonth ||
        (date.respondsToSelector(NSSelectorFromString("isRepeatedDay")) && date.repeatedDay) ||
        listOf(
            date.era,
            date.year,
            date.month,
            date.day,
            date.second,
            date.weekday,
            date.quarter,
            date.weekdayOrdinal,
            date.weekOfMonth,
            date.weekOfYear,
            date.yearForWeekOfYear,
            date.dayOfYear,
            date.nanosecond,
        ).any { it != NSDateComponentUndefined }
    ) {
        return ReminderPending.Unknown
    }
    val language = request.content.userInfo[IOS_REMINDER_LANGUAGE] as? String ?: return ReminderPending.Unknown
    return runCatching {
        ReminderPending.Known(
            ReminderSchedule(
                (date.hour * 60 + date.minute).toInt(),
                ReminderContent(language, request.content.title, request.content.body),
            ),
        )
    }.getOrElse { ReminderPending.Unknown }
}

/** iOS has a public inventory; the Android Unknown allowance must not turn it into false success. */
internal fun requireIosReminderAgreement(schedule: ReminderSchedule, pending: ReminderPending) {
    check(pending == ReminderPending.Known(schedule)) { "Native reminder does not match saved time/content" }
}

internal class IosReminderPlatform : ReminderPlatform {
    override val capability = ReminderCapability.Supported
    val center = UNUserNotificationCenter.currentNotificationCenter()
    private var host: UIViewController? = null
    private var picker: IosReminderPicker? = null

    fun bind(controller: UIViewController) {
        if (host !== controller) picker?.cancel()
        host = controller
    }

    fun unbind(controller: UIViewController) {
        if (host !== controller) return
        picker?.cancel()
        picker = null
        host = null
    }

    override fun systemLanguage(): String =
        (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore('-')?.substringBefore('_')
            ?.takeIf { it in setOf("en", "ru", "kk") } ?: "en"

    override fun formatTime(minutesOfDay: Int): String =
        iosReminderTimeFormatter().stringFromDate(
            iosReminderTimeDate(minutesOfDay),
        )

    private suspend fun settings(): UNNotificationSettings =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { result ->
                if (continuation.isActive) {
                    if (result == null) {
                        continuation.resumeWithException(IllegalStateException("No notification settings"))
                    } else {
                        continuation.resume(result)
                    }
                }
            }
        }

    private suspend fun pending(): List<UNNotificationRequest> =
        suspendCancellableCoroutine { continuation ->
            center.getPendingNotificationRequestsWithCompletionHandler { result ->
                if (continuation.isActive) {
                    if (result == null || result.any { it !is UNNotificationRequest }) {
                        continuation.resumeWithException(IllegalStateException("Unreadable pending requests"))
                    } else {
                        continuation.resume(result.filterIsInstance<UNNotificationRequest>())
                    }
                }
            }
        }

    private suspend fun delivered(): List<UNNotification> =
        suspendCancellableCoroutine { continuation ->
            center.getDeliveredNotificationsWithCompletionHandler { result ->
                if (continuation.isActive) {
                    if (result == null || result.any { it !is UNNotification }) {
                        continuation.resumeWithException(IllegalStateException("Unreadable delivered notifications"))
                    } else {
                        continuation.resume(result.filterIsInstance<UNNotification>())
                    }
                }
            }
        }

    override suspend fun inspect(): ReminderNativeState {
        val native = settings()
        val authorization =
            when (native.authorizationStatus) {
                UNAuthorizationStatusNotDetermined -> ReminderAuthorization.Requestable
                UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral ->
                    ReminderAuthorization.Allowed
                else -> ReminderAuthorization.Blocked
            }
        return ReminderNativeState(
            authorization,
            native.authorizationStatus != UNAuthorizationStatusAuthorized ||
                native.alertSetting != UNNotificationSettingEnabled ||
                native.soundSetting != UNNotificationSettingEnabled,
            iosReminderPending(pending()),
        )
    }

    override suspend fun replace(schedule: ReminderSchedule) {
        check(inspect().authorization == ReminderAuthorization.Allowed)
        val request = iosReminderRequest(schedule)
        suspendCancellableCoroutine<Unit> { continuation ->
            center.addNotificationRequest(request) { error ->
                if (continuation.isActive) {
                    if (error != null) {
                        continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        }
        requireIosReminderAgreement(schedule, iosReminderPending(pending()))
        IosReminderDiagnostics.record("accepted", "minutes" to schedule.minutesOfDay.toString())
        IosReminderDiagnostics.capture(center)
    }

    override suspend fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(IOS_REMINDER_ID))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(IOS_REMINDER_ID))
        check(pending().none { it.identifier == IOS_REMINDER_ID }) { "Pending reminder remains" }
        check(delivered().none { it.request.identifier == IOS_REMINDER_ID }) { "Delivered reminder remains" }
        IosReminderDiagnostics.record("cancelled")
        IosReminderDiagnostics.capture(center)
    }

    override suspend fun requestPermission() {
        suspendCancellableCoroutine<Unit> { continuation ->
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
            ) { _, error ->
                if (continuation.isActive) {
                    if (error != null) {
                        continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        }
        IosReminderDiagnostics.capture(center)
    }

    override suspend fun chooseTime(initialMinutes: Int?, labels: ReminderPickerLabels): Int? =
        withContext(Dispatchers.Main.immediate) {
            var stage = "host"
            try {
                val presenter = requireNotNull(host) { "No picker host" }
                IosReminderDiagnostics.record(
                    "picker-host",
                    "attached" to (presenter.view.window != null).toString(),
                    "presenting" to (presenter.presentedViewController != null).toString(),
                )
                check(
                    presenter.view.window != null && presenter.presentedViewController == null,
                ) { "Picker host unavailable" }
                suspendCancellableCoroutine { continuation ->
                    stage = "construction"
                    val sheet =
                        IosReminderPicker(initialMinutes, labels) { result ->
                            picker = null
                            if (continuation.isActive) continuation.resume(result)
                        }
                    picker = sheet
                    continuation.invokeOnCancellation { sheet.cancelOnMain() }
                    stage = "presentation"
                    presenter.presentViewController(sheet, animated = true, completion = null)
                    stage = "delegate"
                    sheet.presentationController?.delegate = sheet
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                IosReminderDiagnostics.record(
                    "picker-failed",
                    "stage" to stage,
                    "type" to error::class.simpleName.orEmpty(),
                    "message" to error.message.orEmpty(),
                )
                throw error
            }
        }

    override suspend fun openSystemSettings() =
        withContext(Dispatchers.Main.immediate) {
            val url = requireNotNull(NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString))
            suspendCancellableCoroutine<Unit> { continuation ->
                UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>()) { opened ->
                    if (continuation.isActive) {
                        if (opened) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Notification settings did not open"),
                            )
                        }
                    }
                }
            }
        }
}
