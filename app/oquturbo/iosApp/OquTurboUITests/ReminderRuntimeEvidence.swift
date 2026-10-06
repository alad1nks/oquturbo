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

    func start(allowLanguageChanges: Bool = false) {
        // Argument-domain locale overrides would mask the app's own language preference changes.
        app.launchArguments = allowLanguageChanges ? [] : ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 30))
        tap(app.buttons["Profile"])
        tap(app.buttons["Settings"])
        capture("settings")
    }

    func tap(_ element: XCUIElement, scrolling: Bool = false, evidenceName: String? = nil) {
        for _ in 0..<(scrolling ? 10 : 1) {
            let deadline = ProcessInfo.processInfo.systemUptime + 2
            if element.waitForExistence(timeout: 2) && element.isHittable {
                // Hittable can become true while the preceding swipe is still decelerating.
                // A moving visible target needs observation, not another swipe or a speculative tap.
                if scrolling && !waitUntilStill(element, deadline: deadline) { continue }
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

    private func waitUntilStill(_ element: XCUIElement, deadline: TimeInterval) -> Bool {
        var lastFrame: CGRect?
        var stableSince = ProcessInfo.processInfo.systemUptime
        var observations: [String] = []
        defer {
            let trace = XCTAttachment(string: observations.joined(separator: "\n"))
            trace.name = "tap-frame-readiness"; trace.lifetime = .keepAlways; test.add(trace)
        }
        while ProcessInfo.processInfo.systemUptime < deadline {
            let frame = element.frame
            let usable = element.isEnabled && element.isHittable
            let now = ProcessInfo.processInfo.systemUptime
            observations.append("\(now): frame=\(frame) usable=\(usable) deadline=\(deadline)")
            // XCTest queries can block. Never treat a late result as readiness within this budget.
            guard now < deadline else { return false }
            if !usable || frame.isEmpty { return false }
            if frame != lastFrame { stableSince = now }
            lastFrame = frame
            if now - stableSince >= 0.2 { return true }
            RunLoop.current.run(until: Date().addingTimeInterval(min(0.1, deadline - now)))
        }
        return false
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
        for _ in 0..<5 { if pickerActionFullyVisible(button) { break }; scrollPickerContentUp() }
        XCTAssertTrue(pickerActionFullyVisible(button))
        let dismiss = app.buttons["reminder-time-cancel"]
        XCTAssertEqual(dismiss.label, cancel)
        for _ in 0..<5 { if pickerActionFullyVisible(dismiss) { break }; scrollPickerContentUp() }
        XCTAssertTrue(pickerActionFullyVisible(dismiss))
        XCTAssertTrue(pickerActionFullyVisible(button))
        capture("localized-picker-actions-" + title)
        dismiss.tap()
        capture("localized-picker-cancelled-" + title)
    }

    private func pickerActionFullyVisible(_ button: XCUIElement) -> Bool {
        let scrolls = app.otherElements["reminder-native-picker"].scrollViews
        guard scrolls.count == 1 && button.isHittable else { return false }
        let viewport = scrolls.element(boundBy: 0).frame.intersection(app.frame)
        guard !button.frame.isEmpty && viewport.contains(button.frame) else { return false }
        return button.staticTexts.allElementsBoundByIndex.allSatisfy {
            !$0.frame.isEmpty && viewport.contains($0.frame)
        }
    }

    private func scrollPickerContentUp() {
        let sheet = app.otherElements["reminder-native-picker"]
        let scrolls = sheet.scrollViews
        guard scrolls.count == 1 else { XCTFail("Expected one native picker content scroll view"); return }
        let scroll = scrolls.element(boundBy: 0)
        let picker = sheet.datePickers["reminder-time-wheel"]
        let viewport = scroll.frame.intersection(app.frame)
        let wheel = picker.frame
        // The actual sheet has a content gutter to the left of the picker. Center swipes edit its minute wheel.
        guard !viewport.isEmpty && wheel.minX > viewport.minX && wheel.minX < viewport.maxX else {
            capture("native-picker-scroll-gutter-missing")
            XCTFail("No visible outer scroll gutter beside the native picker"); return
        }
        let x = (viewport.minX + wheel.minX) / 2
        let origin = app.coordinate(withNormalizedOffset: .zero)
        let start = origin.withOffset(CGVector(dx: x - app.frame.minX, dy: viewport.minY + viewport.height * 0.75 - app.frame.minY))
        let end = origin.withOffset(CGVector(dx: x - app.frame.minX, dy: viewport.minY + viewport.height * 0.25 - app.frame.minY))
        start.press(forDuration: 0.1, thenDragTo: end)
    }

    // Original N12: one actual alternate system hour cycle, without saving a new reminder time.
    func inspectSystem24HourTime(original: [String?]) {
        func numbers(_ text: String) -> [Int] {
            text.components(separatedBy: CharacterSet.decimalDigits.inverted).compactMap(Int.init)
        }
        guard original.count == 3, let hourText = original[0], let minuteText = original[1],
              let period = original[2], numbers(hourText).count == 1, numbers(minuteText).count == 1,
              let hour = numbers(hourText).first, let minute = numbers(minuteText).first,
              (1...12).contains(hour), (0...59).contains(minute), ["AM", "PM"].contains(period) else {
            XCTFail("Expected an observed English 12-hour baseline"); return
        }
        let expectedHour = hour % 12 + (period == "PM" ? 12 : 0)
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        func captureSettings(_ name: String) {
            let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            image.name = name; image.lifetime = .keepAlways; test.add(image)
            let tree = XCTAttachment(string: settings.debugDescription)
            tree.name = name + "-tree"; tree.lifetime = .keepAlways; test.add(tree)
        }
        func tapSettingsRow(_ label: String) {
            let row = settings.staticTexts[label]
            for _ in 0..<10 {
                if row.waitForExistence(timeout: 2) && row.isHittable { row.tap(); return }
                settings.swipeUp()
            }
            captureSettings("system-hour-cycle-missing-" + label)
            XCTFail("Required real Settings row is not hittable: " + label)
        }
        settings.launch()
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 15))
        // Settings may resume the notification page visited earlier in this same simulator.
        for _ in 0..<5 {
            if settings.navigationBars["Settings"].exists { break }
            let back = settings.navigationBars.buttons.element(boundBy: 0)
            guard back.exists && back.isHittable else {
                captureSettings("system-hour-cycle-missing-back"); XCTFail("No Settings back navigation"); return
            }
            back.tap()
        }
        XCTAssertTrue(settings.navigationBars["Settings"].exists)
        tapSettingsRow("General")
        // This simulator exposes Language & Region, but no Date & Time page.
        tapSettingsRow("Language & Region")
        let regionCells = settings.cells.containing(.staticText, identifier: "Region")
        func readRegion() -> String? {
            guard regionCells.element(boundBy: 0).waitForExistence(timeout: 5), regionCells.count == 1 else { return nil }
            let values = regionCells.element(boundBy: 0).staticTexts.allElementsBoundByIndex
                .map { $0.label }.filter { $0 != "Region" && !$0.isEmpty }
            return values.count == 1 ? values[0] : nil
        }
        func selectRegion(_ region: String) {
            tapSettingsRow("Region")
            captureSettings("system-region-selection")
            guard settings.searchFields.count == 1 else { XCTFail("Expected one real region search field"); return }
            let search = settings.searchFields.element(boundBy: 0)
            XCTAssertTrue(search.isHittable)
            search.tap(); search.typeText(region)
            let result = settings.staticTexts[region]
            XCTAssertTrue(result.waitForExistence(timeout: 5) && result.isHittable)
            result.tap()
            if settings.alerts.element(boundBy: 0).waitForExistence(timeout: 2) {
                captureSettings("system-region-confirmation")
                let confirm = settings.alerts.buttons["Change to " + region]
                XCTAssertTrue(confirm.exists && confirm.isHittable)
                confirm.tap()
            }
            captureSettings("system-region-selected-" + region)
            XCTAssertEqual(readRegion(), region)
        }
        captureSettings("system-region-original")
        guard let originalRegion = readRegion(), originalRegion != "Germany" else {
            XCTFail("Expected an observed original region distinct from the documented 24-hour region"); return
        }
        // Register before the first region mutation, including its native confirmation.
        test.addTeardownBlock {
            settings.activate()
            // A failure may leave the country picker open; return through actual back navigation.
            for _ in 0..<5 {
                if settings.navigationBars["Language & Region"].exists { break }
                let back = settings.navigationBars.buttons.element(boundBy: 0)
                guard back.exists && back.isHittable else {
                    captureSettings("system-region-restoration-navigation-missing")
                    XCTFail("Cannot return to original system region"); return
                }
                back.tap()
            }
            guard let current = readRegion() else {
                captureSettings("system-region-restoration-value-missing")
                XCTFail("Cannot read system region for restoration"); return
            }
            if current != originalRegion { selectRegion(originalRegion) }
            captureSettings("system-region-teardown-restored")
            XCTAssertEqual(readRegion(), originalRegion)
        }
        selectRegion("Germany")
        app.activate()
        assertScheduled()
        let summaries = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "Chosen time:"))
        XCTAssertEqual(summaries.count, 1)
        let summary = summaries.element(boundBy: 0).label
        XCTAssertEqual(numbers(summary), [expectedHour, minute])
        XCTAssertFalse(summary.contains("AM") || summary.contains("PM"))
        capture("system-24-hour-summary")
        tap(app.buttons["Change time"], scrolling: true)
        let picker = app.datePickers["reminder-time-wheel"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        XCTAssertEqual(picker.pickerWheels.count, 2)
        let values = picker.pickerWheels.allElementsBoundByIndex.map { numbers($0.value as? String ?? "") }
        XCTAssertEqual(values, [[expectedHour], [minute]])
        capture("system-24-hour-unchanged-draft")
        let cancel = app.buttons["reminder-time-cancel"]
        XCTAssertEqual(cancel.label, "Cancel")
        for _ in 0..<5 { if pickerActionFullyVisible(cancel) { break }; scrollPickerContentUp() }
        XCTAssertTrue(pickerActionFullyVisible(cancel))
        cancel.tap()
        assertScheduled()
        settings.activate()
        selectRegion(originalRegion)
        captureSettings("system-region-restored")
        XCTAssertEqual(readRegion(), originalRegion)
        app.activate()
        assertScheduled()
        capture("system-hour-cycle-restored-app")
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
