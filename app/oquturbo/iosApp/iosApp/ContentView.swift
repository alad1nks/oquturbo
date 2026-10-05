import UIKit
import SwiftUI
import AppOquTurbo

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    static func dismantleUIViewController(_ uiViewController: UIViewController, coordinator: ()) {
        IosReminderBridge.shared.detachHost(controller: uiViewController)
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
