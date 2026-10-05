@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.alad1nks.oquturbo.shared.reminders

import platform.Foundation.NSThread
import platform.UIKit.UIViewController
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Export only primitive/native SDK values; Swift owns no repository, controller or navigation graph. */
object IosReminderBridge {
    fun bootstrap() = onMain { IosReminderRuntime.application }

    fun sceneBecameActive() = onMain { IosReminderRuntime.refresh() }

    fun significantTimeChanged() = onMain { IosReminderRuntime.refresh() }

    fun ownedRequestIdentifier(): String = IOS_REMINDER_ID

    fun detachHost(controller: UIViewController) = onMain { IosReminderRuntime.platform.unbind(controller) }

    fun receivedResponse(requestIdentifier: String, actionIdentifier: String, deliveredAtEpochSeconds: Double) =
        onMain {
            if (requestIdentifier != IOS_REMINDER_ID || actionIdentifier != UNNotificationDefaultActionIdentifier) {
                return@onMain
            }
            val runtime = IosReminderRuntime
            val identity = "$requestIdentifier:$actionIdentifier:$deliveredAtEpochSeconds"
            if (!runtime.responses.add(identity)) return@onMain
            runtime.homeActions.offer()
            IosReminderDiagnostics.record(
                "response",
                "id" to requestIdentifier,
                "action" to actionIdentifier,
                "delivered" to deliveredAtEpochSeconds.toString(),
                "eventId" to runtime.homeActions.pending.value.toString(),
            )
        }

    fun observedForegroundDelivery(requestIdentifier: String, deliveredAtEpochSeconds: Double) =
        onMain {
            if (requestIdentifier == IOS_REMINDER_ID) {
                IosReminderDiagnostics.record(
                    "foreground-delivery",
                    "id" to requestIdentifier,
                    "delivered" to deliveredAtEpochSeconds.toString(),
                    "presentation" to "none",
                )
                IosReminderDiagnostics.capture(IosReminderRuntime.platform.center)
            }
        }

    private fun onMain(block: () -> Unit) {
        if (NSThread.isMainThread) block() else dispatch_async(dispatch_get_main_queue()) { block() }
    }
}
