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
            if element.waitForExistence(timeout: 2) && element.isHittable {
                // Hittable can become true while the preceding swipe is still decelerating.
                // A moving visible target needs observation, not another swipe or a speculative tap.
                if scrolling && !waitUntilStill(element, deadline: ProcessInfo.processInfo.systemUptime + 3) { continue }
                if let name = evidenceName {
                    capture(name)
                    let state = XCTAttachment(string: "epoch=\(Date().timeIntervalSince1970) uptime=\(ProcessInfo.processInfo.systemUptime) " +
                        "enabled=\(element.isEnabled) hittable=\(element.isHittable) frame=\(element.frame) appState=\(app.state.rawValue)")
                    state.name = name + "-control-state"; state.lifetime = .keepAlways; test.add(state)
                }
                element.tap(); return
            }
            if scrolling { app.swipeUp() }
        }
        capture("missing-control")
        XCTFail("Required control is not hittable: \(element)")
    }

    // Observation only: never activate the retained-time action or change reminder state.
    func revealRetainedTimeAction() -> Bool {
        for attempt in 0...10 {
            let matches = app.buttons.matching(NSPredicate(format: "label == %@", "Change time"))
            let action = matches.element(boundBy: 0)
            let exists = action.waitForExistence(timeout: 2)
            if matches.count > 1 { return false }
            if exists && matches.count == 1 && action.isEnabled && action.isHittable {
                // Observe deceleration without another swipe when the control is already onscreen.
                let deadline = ProcessInfo.processInfo.systemUptime + 3
                if !waitUntilStill(action, deadline: deadline) { continue }
                let frame = action.frame
                if matches.count == 1 && action.isEnabled && action.isHittable &&
                    !frame.isEmpty && app.frame.contains(frame) &&
                    ProcessInfo.processInfo.systemUptime < deadline { return true }
            }
            if attempt < 10 { app.swipeUp() }
        }
        return false
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
            // Native traces show Compose still decelerating for about half a second
            // after the accessibility frame stops moving. A tap during that interval
            // only stops scrolling. Require a full second of stable UI observations.
            if now - stableSince >= 1 { return true }
            RunLoop.current.run(until: Date().addingTimeInterval(min(0.1, deadline - now)))
        }
        return false
    }

    func notificationPermissionSwitch(in settings: XCUIApplication) -> XCUIElement {
        func captureSettings(_ name: String) {
            let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            screenshot.name = name; screenshot.lifetime = .keepAlways; test.add(screenshot)
            let tree = XCTAttachment(string: settings.debugDescription)
            tree.name = name + "-tree"; tree.lifetime = .keepAlways; test.add(tree)
        }
        func tapSettingsRow(_ name: String) {
            let row = settings.staticTexts[name]
            for _ in 0..<10 {
                if row.waitForExistence(timeout: 2) && row.isHittable { row.tap(); return }
                settings.swipeUp()
            }
            captureSettings("missing-system-settings-" + name)
            XCTFail("Required system Settings row is not hittable: " + name)
        }

        let allow = settings.switches["Allow Notifications"]
        if !allow.waitForExistence(timeout: 5) {
            captureSettings("notification-settings-link-destination")
            // On the fresh simulator the public notification Settings URL opens
            // Settings' root. Follow the real UI to the app's permission page.
            // Any other unexpected destination remains a failure.
            XCTAssertTrue(settings.navigationBars["Settings"].exists)
            // This simulator exposes app permissions under Apps; it has no
            // Notifications category at the Settings root.
            tapSettingsRow("Apps")
            XCTAssertTrue(settings.navigationBars["Apps"].waitForExistence(timeout: 5))
            tapSettingsRow("OquTurbo")
            XCTAssertTrue(settings.navigationBars["OquTurbo"].waitForExistence(timeout: 5))
            captureSettings("owned-app-settings")
            tapSettingsRow("Notifications")
        }
        XCTAssertTrue(allow.waitForExistence(timeout: 10))
        XCTAssertTrue(settings.navigationBars["OquTurbo"].exists || settings.navigationBars["Notifications"].exists)
        XCTAssertTrue(allow.isHittable)
        captureSettings("owned-notification-permission")
        return allow
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

    // The clock fixture uses English system UI; read what UIKit actually exposes before confirming.
    func pickerMinutes(_ picker: XCUIElement) -> Int? {
        let wheels = picker.pickerWheels.allElementsBoundByIndex
        guard wheels.count == 2 || wheels.count == 3 else { return nil }
        let values = wheels.compactMap { $0.value as? String }
        guard values.count == wheels.count else { return nil }
        return englishClockMinutes(values.joined(separator: " "))
    }

    func reminderSummaryMinutes() -> Int? {
        let summaries = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "Chosen time:"))
        guard summaries.count == 1 else { return nil }
        return englishClockMinutes(summaries.element(boundBy: 0).label)
    }

    private func englishClockMinutes(_ text: String) -> Int? {
        let numbers = text.components(separatedBy: CharacterSet.decimalDigits.inverted).compactMap(Int.init)
        guard numbers.count == 2, (0...59).contains(numbers[1]) else { return nil }
        let am = text.contains("AM"), pm = text.contains("PM")
        if am || pm {
            guard am != pm, (1...12).contains(numbers[0]) else { return nil }
            return (numbers[0] % 12 + (pm ? 12 : 0)) * 60 + numbers[1]
        }
        guard (0...23).contains(numbers[0]) else { return nil }
        return numbers[0] * 60 + numbers[1]
    }

    func scrollTop() { for _ in 0..<10 { app.swipeDown() } }

    func language(_ row: String, option: String) {
        scrollTop()
        tap(app.staticTexts[row])
        tap(app.staticTexts[option])
    }

    func inspectLocalizedPicker(change: String, title: String, helper: String, confirm: String, cancel: String) {
        let action = app.buttons[change]
        func captureActionState(_ name: String) {
            let exists = action.exists
            let details = exists ? "frame=\(action.frame) enabled=\(action.isEnabled) hittable=\(action.isHittable)" : "exists=false"
            let state = XCTAttachment(string:
                "epoch=\(Date().timeIntervalSince1970) uptime=\(ProcessInfo.processInfo.systemUptime) " + details)
            state.name = name; state.lifetime = .keepAlways; test.add(state)
        }
        tap(action, scrolling: true, evidenceName: "localized-picker-before-tap")
        let titleDeadline = ProcessInfo.processInfo.systemUptime + 5
        capture("localized-picker-after-tap")
        captureActionState("localized-picker-after-tap-control")
        let remaining = titleDeadline - ProcessInfo.processInfo.systemUptime
        let titleExists = remaining > 0 && app.staticTexts[title].waitForExistence(timeout: remaining) &&
            ProcessInfo.processInfo.systemUptime < titleDeadline
        if !titleExists { capture("localized-picker-missing-title") }
        XCTAssertTrue(titleExists)
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

    // Host-owned Settings transitions may kill XCTest. Only these readback tests claim acceptance.
    func recordRegionBaseline(original: [String?]) throws {
        let control = try RegionRuntimeControl(test)
        let minutes = try control.minutes(original)
        try control.openRegionPage()
        let region = try control.readRegion()
        guard region != "Germany" else { throw RegionRuntimeControl.Failure.invalidState }
        try control.record("baseline", extra: ["originalRegion": region, "originalLanguage": "en-US",
                                               "minutes": minutes, "wheels": original.compactMap { $0 }])
    }

    func inspectSystem24HourTime() throws {
        let control = try RegionRuntimeControl(test)
        try control.openRegionPage()
        XCTAssertEqual(try control.readRegion(), "Germany")
        let minutes = try control.expectedMinutes()
        start(allowLanguageChanges: true)
        assertScheduled()
        let summaries = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "Chosen time:"))
        XCTAssertEqual(summaries.count, 1)
        let summary = summaries.element(boundBy: 0).label
        XCTAssertEqual(control.numbers(summary), [minutes / 60, minutes % 60])
        XCTAssertFalse(summary.contains("AM") || summary.contains("PM"))
        capture("system-24-hour-summary")
        tap(app.buttons["Change time"], scrolling: true)
        let picker = app.datePickers["reminder-time-wheel"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        XCTAssertEqual(picker.pickerWheels.count, 2)
        XCTAssertEqual(picker.pickerWheels.allElementsBoundByIndex.map { control.numbers($0.value as? String ?? "") },
                       [[minutes / 60], [minutes % 60]])
        capture("system-24-hour-unchanged-draft")
        let cancel = app.buttons["reminder-time-cancel"]
        XCTAssertEqual(cancel.label, "Cancel")
        for _ in 0..<5 { if pickerActionFullyVisible(cancel) { break }; scrollPickerContentUp() }
        XCTAssertTrue(pickerActionFullyVisible(cancel))
        cancel.tap()
        assertScheduled()
        try control.record("accepted-24-hour", extra: ["verifiedRegion": "Germany", "verifiedLanguage": "en-US"])
    }

    func inspectRestoredRegion() throws {
        let control = try RegionRuntimeControl(test)
        try control.openRegionPage()
        XCTAssertEqual(try control.readRegion(), try control.originalRegion())
        start(allowLanguageChanges: true)
        assertScheduled()
        tap(app.buttons["Change time"], scrolling: true)
        let picker = app.datePickers["reminder-time-wheel"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        let values = picker.pickerWheels.allElementsBoundByIndex.map { $0.value as? String }
        XCTAssertEqual(try control.minutes(values), try control.expectedMinutes())
        let cancel = app.buttons["reminder-time-cancel"]
        for _ in 0..<5 { if pickerActionFullyVisible(cancel) { break }; scrollPickerContentUp() }
        XCTAssertTrue(pickerActionFullyVisible(cancel))
        cancel.tap()
        assertScheduled()
        capture("system-hour-cycle-restored-app")
        try control.record("restored", extra: ["verifiedRegion": try control.originalRegion(), "verifiedLanguage": "en-US"])
    }

    func allowIfPrompted() {
        let allow = springboard.alerts.buttons["Allow"]
        if allow.waitForExistence(timeout: 5) { allow.tap() }
    }

    func assertScheduled() {
        var observations: [String] = []
        func attachObservations() {
            let trace = XCTAttachment(string: observations.joined(separator: "\n"))
            trace.name = "scheduled-navigation"
            trace.lifetime = .keepAlways
            test.add(trace)
        }
        for attempt in 0..<10 {
            let matches = app.staticTexts.matching(NSPredicate(format: "label == %@", "Scheduled"))
            let exists = matches.element(boundBy: 0).waitForExistence(timeout: 2)
            let count = matches.count
            let viewport = app.frame
            if count > 1 {
                let frames = matches.allElementsBoundByIndex.map { $0.frame }
                observations.append("attempt=\(attempt + 1) count=\(count) frames=\(frames) viewport=\(viewport) action=fail-ambiguous")
                break
            }
            let scheduled = matches.element(boundBy: 0)
            let frame = exists && count == 1 ? scheduled.frame : CGRect.zero
            let hittable = exists && count == 1 && scheduled.isHittable
            let usableFrame = !frame.isEmpty && !frame.isNull && !frame.isInfinite
            let usableViewport = !viewport.isEmpty && !viewport.isNull && !viewport.isInfinite
            let observation = "attempt=\(attempt + 1) count=\(count) frame=\(frame) viewport=\(viewport) hittable=\(hittable)"
            if hittable && usableFrame && usableViewport && viewport.contains(frame) {
                observations.append(observation + " action=accept-visible")
                attachObservations()
                capture("scheduled")
                return
            }
            if attempt == 9 {
                observations.append(observation + " action=fail-exhausted")
                break
            }
            // Move the observed status toward the viewport center; an unavailable frame retains upward search.
            let swipeDown = usableFrame && usableViewport && frame.midY < viewport.midY
            observations.append(observation + (swipeDown ? " action=swipe-down" : " action=swipe-up"))
            if swipeDown { app.swipeDown() } else { app.swipeUp() }
        }
        attachObservations()
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

/// This journal belongs only to the XCTest runner sandbox, never to OquTurbo or system preferences.
final class RegionRuntimeControl {
    enum Failure: Error { case invalidState }
    let test: XCTestCase
    let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
    let directory: URL
    let request: [String: Any]

    init(_ test: XCTestCase) throws {
        self.test = test
        let folder = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        directory = folder
        let data = try Data(contentsOf: folder.appendingPathComponent("region-operation.json"))
        guard let value = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let operation = value["operation"] as? String, UUID(uuidString: operation) != nil,
              let udid = value["udid"] as? String, UUID(uuidString: udid) != nil,
              ProcessInfo.processInfo.environment["SIMULATOR_UDID"] == udid,
              let source = value["source"] as? String, source.count == 40,
              let build = value["build"] as? String, build.count == 64,
              let created = value["created"] as? Double, abs(Date().timeIntervalSince1970 - created) < 720 else {
            throw Failure.invalidState
        }
        request = value
    }

    func record(_ state: String, extra: [String: Any] = [:]) throws {
        var value = request
        value["state"] = state
        value["observed"] = Date().timeIntervalSince1970
        value["runnerPID"] = Int(ProcessInfo.processInfo.processIdentifier)
        for (key, item) in extra { value[key] = item }
        let data = try JSONSerialization.data(withJSONObject: value, options: [.sortedKeys])
        try data.write(to: directory.appendingPathComponent("region-observation.json"), options: .atomic)
        let attachment = XCTAttachment(data: data, uniformTypeIdentifier: "public.json")
        attachment.name = "region-" + state + "-" + (request["operation"] as! String)
        attachment.lifetime = .keepAlways
        test.add(attachment)
    }

    func numbers(_ text: String) -> [Int] {
        text.components(separatedBy: CharacterSet.decimalDigits.inverted).compactMap(Int.init)
    }

    func minutes(_ original: [String?]) throws -> Int {
        guard original.count == 3, let h = original[0], let m = original[1], let period = original[2],
              numbers(h).count == 1, numbers(m).count == 1,
              let hour = numbers(h).first, let minute = numbers(m).first,
              (1...12).contains(hour), (0...59).contains(minute), ["AM", "PM"].contains(period) else {
            throw Failure.invalidState
        }
        return (hour % 12 + (period == "PM" ? 12 : 0)) * 60 + minute
    }

    func expectedMinutes() throws -> Int {
        guard let value = request["minutes"] as? Int, (0..<1440).contains(value) else { throw Failure.invalidState }
        return value
    }

    func originalRegion() throws -> String {
        guard let value = request["originalRegion"] as? String, !value.isEmpty, value != "Germany",
              request["originalLanguage"] as? String == "en-US" else { throw Failure.invalidState }
        return value
    }

    func capture(_ name: String) {
        let image = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        image.name = name; image.lifetime = .keepAlways; test.add(image)
        let tree = XCTAttachment(string: settings.debugDescription)
        tree.name = name + "-tree"; tree.lifetime = .keepAlways; test.add(tree)
    }

    func tapRow(_ name: String) throws {
        let row = settings.staticTexts[name]
        for _ in 0..<10 {
            if row.waitForExistence(timeout: 2) && row.isHittable { row.tap(); return }
            settings.swipeUp()
        }
        capture("system-region-missing-" + name)
        throw Failure.invalidState
    }

    func openRegionPage() throws {
        settings.launch()
        guard settings.wait(for: .runningForeground, timeout: 15) else { throw Failure.invalidState }
        // A failed transition can leave its real UI pending. Cancel it before any readback.
        let sheet = settings.sheets["Would you like to change the iPhone language to English (UK)?"]
        let selection = settings.navigationBars["Select Region"]
        let cleanupDeadline = ProcessInfo.processInfo.systemUptime + 5
        let restart = settings.sheets["Applying this setting will restart your iPhone."]
        for overlay in [sheet, restart, selection] where overlay.exists {
            let cancel = overlay.buttons["Cancel"]
            guard cancel.isHittable, ProcessInfo.processInfo.systemUptime < cleanupDeadline else {
                throw Failure.invalidState
            }
            cancel.tap()
            let gone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: overlay)
            guard XCTWaiter.wait(for: [gone], timeout: max(0, cleanupDeadline - ProcessInfo.processInfo.systemUptime)) == .completed,
                  ProcessInfo.processInfo.systemUptime <= cleanupDeadline else { throw Failure.invalidState }
        }
        guard ProcessInfo.processInfo.systemUptime <= cleanupDeadline,
              settings.sheets.count == 0, settings.alerts.count == 0, !selection.exists else {
            throw Failure.invalidState
        }
        for _ in 0..<5 {
            if settings.navigationBars["Language & Region"].exists { return }
            if settings.navigationBars["Settings"].exists { break }
            let back = settings.navigationBars.buttons.element(boundBy: 0)
            guard back.exists && back.isHittable else { throw Failure.invalidState }
            back.tap()
        }
        guard settings.navigationBars["Settings"].exists else { throw Failure.invalidState }
        try tapRow("General")
        try tapRow("Language & Region")
    }

    func readRegion() throws -> String {
        let cells = settings.cells.containing(.staticText, identifier: "Region")
        let committed = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            !self.settings.navigationBars["Select Region"].exists && self.settings.sheets.count == 0 &&
                self.settings.alerts.count == 0 && cells.count == 1 && cells.element(boundBy: 0).exists
        }, object: settings)
        guard XCTWaiter.wait(for: [committed], timeout: 5) == .completed,
              settings.staticTexts["en-US"].exists else { throw Failure.invalidState }
        let values = cells.element(boundBy: 0).staticTexts.allElementsBoundByIndex
            .map { $0.label }.filter { $0 != "Region" && !$0.isEmpty }
        guard values.count == 1 else { throw Failure.invalidState }
        capture("system-region-committed-" + values[0])
        return values[0]
    }

    func transition() throws {
        let original = try originalRegion()
        guard let target = request["target"] as? String,
              target == "Germany" || target == original else { throw Failure.invalidState }
        try openRegionPage()
        let current = try readRegion()
        if current == target {
            // Restoration after a precommit failure may already have its original value.
            guard request["step"] as? String == "restore" else { throw Failure.invalidState }
            try record("already-restored"); return
        }
        try record("before-region-choice", extra: ["fromRegion": current])
        try tapRow("Region")
        guard settings.searchFields.count == 1 else { throw Failure.invalidState }
        let search = settings.searchFields.element(boundBy: 0)
        guard search.isHittable else { throw Failure.invalidState }
        search.tap(); search.typeText(target)
        let result = settings.staticTexts[target]
        guard result.waitForExistence(timeout: 5), result.isHittable else { throw Failure.invalidState }
        result.tap()
        let restoringUS = request["step"] as? String == "restore" && target == "United States"
        let sheetLabel = restoringUS ? "Applying this setting will restart your iPhone." :
            "Would you like to change the iPhone language to English (UK)?"
        let confirmation = restoringUS ? "Change to United States" : "Keep English (US)"
        let sheets = settings.sheets.matching(NSPredicate(format: "label == %@", sheetLabel))
        let sheet = sheets.element(boundBy: 0)
        if sheet.waitForExistence(timeout: 2) {
            let buttons = sheet.buttons.matching(NSPredicate(format: "label == %@", confirmation))
            guard sheets.count == 1, settings.sheets.count == 1, settings.alerts.count == 0,
                  buttons.count == 1 else { throw Failure.invalidState }
            let keep = buttons.element(boundBy: 0)
            guard keep.isHittable else { throw Failure.invalidState }
            capture("system-region-confirmation")
            try record("before-confirmation", extra: ["fromRegion": current, "confirmation": confirmation, "confirmationSheet": sheetLabel])
            keep.tap()
        }
        capture("system-region-selected-" + target)
        guard try readRegion() == target else { throw Failure.invalidState }
        try record("transition-completed")
    }
}
