import SwiftUI
import AppOquTurbo

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(ReminderAppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    if scenePhase == .active { IosReminderBridge.shared.sceneBecameActive() }
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { IosReminderBridge.shared.sceneBecameActive() }
                }
        }
    }
}
