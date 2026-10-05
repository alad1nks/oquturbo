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
    }

    func start() {
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        tap(app.buttons["Profile"])
        tap(app.buttons["Settings"])
        capture("settings")
    }

    func tap(_ element: XCUIElement, scrolling: Bool = false) {
        for _ in 0..<(scrolling ? 10 : 1) {
            if element.waitForExistence(timeout: 2) && element.isHittable { element.tap(); return }
            if scrolling { app.swipeUp() }
        }
        capture("missing-control")
        XCTFail("Required control is not hittable: \(element)")
    }

    func selectFutureTime(first: Bool = false) -> Date {
        tap(app.buttons[first ? "Choose time and enable" : "Change time"], scrolling: true)
        let picker = app.datePickers["reminder-time-wheel"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
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

    func notification(until due: Date) -> XCUIElement {
        XCUIDevice.shared.press(.home)
        let card = springboard.staticTexts["Practice in OquTurbo"].firstMatch
        let deadline = due.addingTimeInterval(180)
        while Date() < deadline {
            if card.waitForExistence(timeout: 5) && card.isHittable { capture("real-card"); return card }
            // Open the real Notification Center; do not launch the application to substitute for its card.
            let top = springboard.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01))
            top.press(forDuration: 0.1, thenDragTo: springboard.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75)))
        }
        capture("delivery-timeout"); XCTFail("No actual local notification by due+180s")
        return card
    }

    func assertHomeAfterCard(_ card: XCUIElement) {
        XCTAssertTrue(card.isHittable); card.tap()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        // Do not app.launch/activate here: the real response must open it.
        XCTAssertTrue(app.staticTexts["Today’s training"].waitForExistence(timeout: 15))
        capture("home-after-real-card")
    }
}
