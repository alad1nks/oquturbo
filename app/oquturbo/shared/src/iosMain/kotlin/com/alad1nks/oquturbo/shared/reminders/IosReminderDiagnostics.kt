@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSTimeZone
import platform.Foundation.NSUUID
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** App-process observations only. Never part of product scheduling, preferences or success conditions. */
@OptIn(ExperimentalNativeApi::class)
internal object IosReminderDiagnostics {
    private val launchId = NSUUID().UUIDString
    private val directory get() = NSHomeDirectory() + "/Documents"

    private fun enabled() =
        Platform.isDebugBinary &&
            NSFileManager.defaultManager.fileExistsAtPath("$directory/reminder-diagnostics-enabled")

    fun record(event: String, vararg values: Pair<String, String>) {
        if (!enabled()) return
        val fields =
            listOf(
                "time" to NSDate().timeIntervalSince1970.toString(),
                "launch" to launchId,
                "event" to event,
            ) + values
        dispatch_async(dispatch_get_main_queue()) {
            runCatching {
                val file = fopen("$directory/reminder-diagnostics.jsonl", "a") ?: return@runCatching
                try {
                    fputs(
                        fields.joinToString(",", "{", "}\n") { (key, value) -> "${quote(key)}:${quote(value)}" },
                        file,
                    )
                } finally {
                    fclose(file)
                }
            }
        }
    }

    fun capture(center: UNUserNotificationCenter) {
        if (!enabled()) return
        val capture = NSUUID().UUIDString
        record("system-clock", "capture" to capture, "zone" to NSTimeZone.localTimeZone.name)
        center.getNotificationSettingsWithCompletionHandler { settings ->
            record(
                "authorization",
                "capture" to capture,
                "status" to settings?.authorizationStatus.toString(),
                "alert" to settings?.alertSetting.toString(),
                "sound" to settings?.soundSetting.toString(),
            )
        }
        center.getPendingNotificationRequestsWithCompletionHandler { all ->
            val owned = all?.filterIsInstance<UNNotificationRequest>()?.filter { it.identifier == IOS_REMINDER_ID }
            record("pending", "capture" to capture, "count" to owned?.size.toString())
            owned?.forEach { request ->
                val trigger = request.trigger as? UNCalendarNotificationTrigger
                record(
                    "request",
                    "capture" to capture,
                    "id" to request.identifier,
                    "calendar" to (trigger != null).toString(),
                    "repeats" to trigger?.repeats.toString(),
                    "hour" to trigger?.dateComponents?.hour.toString(),
                    "minute" to trigger?.dateComponents?.minute.toString(),
                    "timezone" to trigger?.dateComponents?.timeZone?.name.toString(),
                    "next" to trigger?.nextTriggerDate()?.timeIntervalSince1970.toString(),
                    "language" to request.content.userInfo[IOS_REMINDER_LANGUAGE].toString(),
                    "title" to request.content.title,
                    "body" to request.content.body,
                )
            }
        }
        center.getDeliveredNotificationsWithCompletionHandler { all ->
            val owned = all?.filterIsInstance<UNNotification>()?.filter { it.request.identifier == IOS_REMINDER_ID }
            record("delivered", "capture" to capture, "count" to owned?.size.toString())
            owned?.forEach {
                record(
                    "delivered-record",
                    "capture" to capture,
                    "id" to it.request.identifier,
                    "date" to it.date.timeIntervalSince1970.toString(),
                )
            }
        }
    }

    private fun quote(value: String): String =
        buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else ->
                        if (char.code < 32) {
                            append(
                                "\\u" + char.code.toString(16).padStart(4, '0'),
                            )
                        } else {
                            append(char)
                        }
                }
            }
            append('"')
        }
}
