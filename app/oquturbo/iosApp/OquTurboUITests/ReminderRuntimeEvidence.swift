import XCTest

/// UI/system evidence only. The host separately validates OquTurbo's own native diagnostic log and preferences.
final class ReminderRuntimeEvidence {
    let test: XCTestCase
    let app = XCUIApplication(bundleIdentifier: "com.alad1nks.oquturbo.OquTurbo")
    let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    init(_ test: XCTestCase) { self.test = test }

    func capture(_ name: String) {
        let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        image.name = name; image.lifetime = .keepAlways; test.add(image)
        let tree = XCTAttachment(string: app.debugDescription + "\nSPRINGBOARD\n" + springboard.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; test.add(tree)
        let state = XCTAttachment(string: "appState=\(app.state.rawValue) springboardState=\(springboard.state.rawValue)")
        state.name = name + "-process-state"; state.lifetime = .keepAlways; test.add(state)
    }

    func start() {
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        tap(app.buttons["Profile"])
        tap(app.buttons["Settings"])
        capture("settings")
    }

    func tap(_ element: XCUIElement, scrolling: Bool = false, evidenceName: String? = nil) {
        for _ in 0..<(scrolling ? 10 : 1) {
            if element.waitForExistence(timeout: 2) && element.isHittable {
                if let name = evidenceName {
                    capture(name)
                    let state = XCTAttachment(string: "enabled=\(element.isEnabled) frame=\(element.frame) appState=\(app.state.rawValue)")
                    state.name = name + "-control-state"; state.lifetime = .keepAlways; test.add(state)
                }
                element.tap(); return
            }
            if scrolling { app.swipeUp() }
        }
        capture("missing-control")
        XCTFail("Required control is not hittable: \(element)")
    }

    func selectFutureTime(first: Bool = false) -> Date {
        tap(app.buttons[first ? "Choose time and enable" : "Change time"], scrolling: true,
            evidenceName: "before-native-picker-open")
        let picker = app.datePickers["reminder-time-wheel"]
        let appeared = picker.waitForExistence(timeout: 5)
        if !appeared { capture("native-picker-missing-after-tap") }
        XCTAssertTrue(appeared)
        let target = Calendar.current.date(byAdding: .minute, value: 3, to: Date())!
        let fields = Calendar.current.dateComponents([.hour, .minute], from: target)
        setWheels(picker, minutes: fields.hour! * 60 + fields.minute!)
        let due = Calendar.current.date(bySettingHour: fields.hour!, minute: fields.minute!, second: 0, of: target)!
        XCTAssertGreaterThan(due.timeIntervalSinceNow, 30, "Picker interaction consumed the future time; rerun safely")
        capture("native-picker-confirm")
        tap(app.buttons["reminder-time-confirm"])
        return due
    }

    func setWheels(_ picker: XCUIElement, minutes: Int) {
        let wheels = picker.pickerWheels
        XCTAssertTrue(wheels.count == 2 || wheels.count == 3, "Unknown native picker layout")
        guard wheels.count == 2 || wheels.count == 3 else { return }
        if wheels.count == 3 {
            wheels.element(boundBy: 0).adjust(toPickerWheelValue: String(((minutes / 60) + 11) % 12 + 1))
            wheels.element(boundBy: 1).adjust(toPickerWheelValue: String(format: "%02d", (minutes % 60)))
            wheels.element(boundBy: 2).adjust(toPickerWheelValue: (minutes / 60) < 12 ? "AM" : "PM")
        } else {
            wheels.element(boundBy: 0).adjust(toPickerWheelValue: String(format: "%02d", (minutes / 60)))
            wheels.element(boundBy: 1).adjust(toPickerWheelValue: String(format: "%02d", (minutes % 60)))
        }
    }

    func scrollTop() { for _ in 0..<10 { app.swipeDown() } }

    func language(_ row: String, option: String) {
        scrollTop()
        tap(app.staticTexts[row])
        tap(app.staticTexts[option])
    }

    func inspectLocalizedPicker(change: String, title: String, helper: String, confirm: String, cancel: String) {
        tap(app.buttons[change], scrolling: true)
        XCTAssertTrue(app.staticTexts[title].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts[helper].exists)
        capture("localized-picker-top-" + title)
        let button = app.buttons["reminder-time-confirm"]
        XCTAssertEqual(button.label, confirm)
        // Actual scrollable sheet must expose both complete actions at the configured large text size.
        for _ in 0..<5 { if button.isHittable { break }; app.swipeUp() }
        XCTAssertTrue(button.isHittable)
        let dismiss = app.buttons["reminder-time-cancel"]
        XCTAssertEqual(dismiss.label, cancel)
        for _ in 0..<5 { if dismiss.isHittable { break }; app.swipeUp() }
        XCTAssertTrue(dismiss.isHittable)
        capture("localized-picker-actions-" + title)
        dismiss.tap()
        capture("localized-picker-cancelled-" + title)
    }

    func allowIfPrompted() {
        let allow = springboard.alerts.buttons["Allow"]
        if allow.waitForExistence(timeout: 5) { allow.tap() }
    }

    func assertScheduled() {
        let scheduled = app.staticTexts["Scheduled"]
        for _ in 0..<10 {
            if scheduled.waitForExistence(timeout: 2) && scheduled.isHittable { capture("scheduled"); return }
            app.swipeUp()
        }
        capture("not-scheduled"); XCTFail("No actual Scheduled state")
    }

    func notification(until due: Date) -> XCUIElement? {
        XCUIDevice.shared.press(.home)
        // Actual SpringBoard tree exposes an actionable platter button around the title/body.
        // A static title's computed hit point did not open the app in the captured cold run.
        let cards = springboard.buttons.matching(NSPredicate(
            format: "identifier == %@ AND label CONTAINS %@ AND label CONTAINS %@",
            "ShortLook.Platter.Content.Seamless", "Practice in OquTurbo",
            "Open OquTurbo to practice when it suits you."))
        let deadline = due.addingTimeInterval(180)
        while Date() < deadline {
            if cards.count > 1 {
                capture("ambiguous-owned-notification")
                XCTFail("Multiple matching owned notification buttons")
                return nil
            }
            if cards.count == 1 {
                let card = cards.element(boundBy: 0)
                if card.isHittable { capture("real-card"); return card }
            }
            RunLoop.current.run(until: min(deadline, Date().addingTimeInterval(5)))
            // Open the real Notification Center; do not launch the application to substitute for its card.
            let top = springboard.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            top.press(forDuration: 0.1, thenDragTo: springboard.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        }
        capture("delivery-timeout"); XCTFail("No actual local notification by due+180s")
        return nil
    }

    func assertHomeAfterCard(_ card: XCUIElement) {
        XCTAssertEqual(card.elementType, .button)
        XCTAssertTrue(card.isHittable)
        // One card tap; some observed SpringBoard states expose a separate native Open action.
        // Both gestures share the existing foreground budget, without a launch/activate fallback.
        let deadline = ProcessInfo.processInfo.systemUptime + 30
        var gestures: [String] = []
        func recordGesture(_ name: String) {
            gestures.append("\(ProcessInfo.processInfo.systemUptime): \(name)")
            let ledger = XCTAttachment(string: gestures.joined(separator: "\n"))
            ledger.name = "notification-gesture-ledger-\(gestures.count)"
            ledger.lifetime = .keepAlways; test.add(ledger)
        }
        card.tap()
        recordGesture("owned-card-tap")
        capture("immediately-after-real-card-tap")
        var usedNativeOpen = false
        while app.state != .runningForeground && ProcessInfo.processInfo.systemUptime < deadline {
            if !usedNativeOpen {
                let cells = springboard.buttons.matching(NSPredicate(
                    format: "identifier == %@ AND label CONTAINS %@ AND label CONTAINS %@",
                    "ListCell", "Practice in OquTurbo", "Open OquTurbo to practice when it suits you."))
                guard cells.count <= 1 else {
                    capture("ambiguous-owned-open-cell")
                    XCTFail("Multiple owned notification cells for native Open")
                    return
                }
                if cells.count == 1 {
                    let actions = cells.element(boundBy: 0).buttons.matching(NSPredicate(
                        format: "identifier == %@ AND label == %@", "swipe-action-button-identifier", "Open"))
                    guard actions.count <= 1 else {
                        capture("ambiguous-owned-open-action")
                        XCTFail("Multiple native Open actions in owned notification cell")
                        return
                    }
                    if actions.count == 1 && actions.element(boundBy: 0).isHittable {
                        capture("before-owned-native-open")
                        // Recheck after evidence capture: do not tap a stale/disappeared action.
                        if app.state != .runningForeground && ProcessInfo.processInfo.systemUptime < deadline &&
                            cells.count == 1 && actions.count == 1 && actions.element(boundBy: 0).isHittable {
                            // XCTest queries above can block; recheck the budget immediately before dispatch.
                            guard ProcessInfo.processInfo.systemUptime < deadline else { break }
                            usedNativeOpen = true
                            actions.element(boundBy: 0).tap()
                            recordGesture("owned-native-open-tap")
                            capture("after-owned-native-open")
                        }
                    }
                }
            }
            let remaining = deadline - ProcessInfo.processInfo.systemUptime
            if remaining > 0 { _ = app.wait(for: .runningForeground, timeout: min(0.5, remaining)) }
        }
        let opened = app.state == .runningForeground && ProcessInfo.processInfo.systemUptime <= deadline
        if !opened { capture("real-card-did-not-open-app") }
        XCTAssertTrue(opened)
        // The host still requires one real default response and matching cold/warm launch identity.
        XCTAssertTrue(app.staticTexts["Today’s training"].waitForExistence(timeout: 15))
        capture("home-after-real-card")
    }
}
