import UIKit
import UserNotifications
import AppOquTurbo

final class ReminderAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // Install before launch completes: a notification response can precede Compose graph creation.
        UNUserNotificationCenter.current().delegate = self
        IosReminderBridge.shared.bootstrap()
        return true
    }

    func applicationSignificantTimeChange(_ application: UIApplication) {
        IosReminderBridge.shared.significantTimeChanged()
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let identifier = notification.request.identifier
        let delivered = notification.date.timeIntervalSince1970
        // Preserve the app's previous no-foreground-presentation default for unrelated identifiers too.
        completionHandler([])
        DispatchQueue.main.async {
            IosReminderBridge.shared.observedForegroundDelivery(requestIdentifier: identifier,
                                                               deliveredAtEpochSeconds: delivered)
        }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let identifier = response.notification.request.identifier
        let action = response.actionIdentifier
        let delivered = response.notification.date.timeIntervalSince1970
        DispatchQueue.main.async {
            IosReminderBridge.shared.receivedResponse(requestIdentifier: identifier, actionIdentifier: action,
                                                     deliveredAtEpochSeconds: delivered)
            // Mailbox is queued; never hold the system callback while waiting for the navigation graph.
            completionHandler()
        }
    }
}
