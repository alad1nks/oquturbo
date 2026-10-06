import XCTest

final class LocalReminderRuntimeTests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    // Setup only: exercise ordinary first launch before the host freezes product preferences.
    // No picker, reminder enable, permission request or delivery belongs to this method.
    func testInitializeFreshFixture() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        let off = ui.app.staticTexts["Off"]
        for _ in 0..<10 {
            if off.waitForExistence(timeout: 2) && off.isHittable { break }
            ui.app.swipeUp()
        }
        XCTAssertTrue(off.exists && off.isHittable)
        ui.capture("bootstrap-settings-off")
        ui.app.terminate()
        XCTAssertTrue(ui.app.wait(for: .notRunning, timeout: 10))
        ui.capture("bootstrap-terminated")
    }

    func testColdDeliveryAndNativeOptIn() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        ui.tap(ui.app.buttons["Choose time and enable"], scrolling: true)
        ui.tap(ui.app.buttons["reminder-time-cancel"])
        XCTAssertTrue(ui.app.staticTexts["Off"].exists)
        let due = ui.selectFutureTime(first: true)
        let allow = ui.springboard.alerts.buttons["Allow"]
        XCTAssertTrue(allow.waitForExistence(timeout: 10), "Fresh fixture must show a real permission prompt")
        allow.tap(); ui.assertScheduled()
        ui.app.terminate()
        XCTAssertTrue(ui.app.wait(for: .notRunning, timeout: 10))
        XCTAssertGreaterThan(due.timeIntervalSinceNow, 0)
        guard let card = ui.notification(until: due) else { return }
        ui.assertHomeAfterCard(card)
    }

    func testWarmDeliveryFromSettings() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        let due = ui.selectFutureTime()
        ui.assertScheduled()
        let backgroundDeadline = ProcessInfo.processInfo.systemUptime + min(10, due.timeIntervalSinceNow)
        XCUIDevice.shared.press(.home)
        var backgroundStates: [String] = []
        var backgroundObserved = false
        while ProcessInfo.processInfo.systemUptime < backgroundDeadline && Date() < due {
            // Read once: the app can move from background to suspended between two state queries.
            let state = ui.app.state
            backgroundStates.append("\(ProcessInfo.processInfo.systemUptime): state=\(state.rawValue)")
            if state == .notRunning || state == .unknown { break }
            if state == .runningBackground || state == .runningBackgroundSuspended {
                backgroundObserved = ProcessInfo.processInfo.systemUptime < backgroundDeadline && Date() < due
                break
            }
            let remaining = min(backgroundDeadline - ProcessInfo.processInfo.systemUptime, due.timeIntervalSinceNow)
            if remaining > 0 { RunLoop.current.run(until: Date().addingTimeInterval(min(0.1, remaining))) }
        }
        let transition = XCTAttachment(string: backgroundStates.joined(separator: "\n"))
        transition.name = "warm-background-transition"; transition.lifetime = .keepAlways; add(transition)
        guard backgroundObserved else {
            ui.capture("warm-did-not-background-before-due")
            XCTFail("App did not remain running and reach background within 10s and before the chosen due time")
            return
        }
        guard let card = ui.notification(until: due) else { return }
        let stateAtCard = ui.app.state
        XCTAssertTrue(stateAtCard == .runningBackground || stateAtCard == .runningBackgroundSuspended)
        ui.assertHomeAfterCard(card)
    }

    func testForegroundDispatch() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        let due = ui.selectFutureTime()
        ui.assertScheduled()
        // Absence alone is not a pass: host validation also requires the actual willPresent callback.
        while Date() < due.addingTimeInterval(90) {
            XCTAssertEqual(ui.app.state, .runningForeground)
            XCTAssertFalse(ui.springboard.staticTexts["Practice in OquTurbo"].exists)
            RunLoop.current.run(until: Date().addingTimeInterval(5))
        }
        ui.capture("foreground-no-presentation")
    }

    func testUntappedDeliveryThenOffRetainsTime() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        let due = ui.selectFutureTime()
        ui.assertScheduled()
        guard ui.notification(until: due) != nil else { return } // Deliberately leave the actual card untapped.
        ui.app.activate()
        // Existing Settings should resume; return to its top before locating the named switch.
        for _ in 0..<10 { ui.app.swipeDown() }
        let toggle = ui.app.switches["Reminders"]
        ui.tap(toggle, scrolling: true)
        XCTAssertTrue(ui.app.staticTexts["Off"].waitForExistence(timeout: 5))
        XCTAssertTrue(ui.app.buttons["Change time"].exists)
        ui.capture("off-retains-time")
        // Host requires actual pending AND delivered owned inventories to be empty after cancellation.
    }

    func testDeniedPermissionAndSettingsRecovery() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        _ = ui.selectFutureTime(first: true)
        let deny = ui.springboard.alerts.buttons["Don’t Allow"]
        XCTAssertTrue(deny.waitForExistence(timeout: 10)); deny.tap()
        ui.tap(ui.app.buttons["Notification settings"], scrolling: true)
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        XCTAssertTrue(settings.wait(for: .runningForeground, timeout: 15))
        let allow = settings.switches["Allow Notifications"]
        XCTAssertTrue(allow.waitForExistence(timeout: 10)); allow.tap()
        ui.app.activate(); ui.assertScheduled()
        ui.tap(ui.app.buttons["Change time"], scrolling: true)
        ui.tap(ui.app.buttons["reminder-time-cancel"])
        // Revoke through actual system Settings and require truthful blocked state on return.
        settings.activate(); XCTAssertTrue(allow.waitForExistence(timeout: 10)); allow.tap()
        ui.app.activate()
        ui.tap(ui.app.staticTexts["Notifications are blocked by the system."], scrolling: true)
        ui.capture("revoked-without-reprompt")
    }

    func testLocalizedPendingAndLargeTextPicker() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        XCTAssertLessThanOrEqual(ui.app.frame.width, 390, "Use a genuinely narrow supported iPhone")
        // Keep this interval free of preference changes; the host brackets raw bytes and native events.
        ui.assertScheduled()
        RunLoop.current.run(until: Date().addingTimeInterval(3))
        ui.tap(ui.app.buttons["Change time"], scrolling: true)
        let picker = ui.app.datePickers["reminder-time-wheel"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        let original = picker.pickerWheels.allElementsBoundByIndex.map { $0.value as? String }
        XCTAssertFalse(original.isEmpty)
        ui.capture("dismissal-original-draft")
        ui.setWheels(picker, minutes: 150)
        if picker.pickerWheels.allElementsBoundByIndex.map({ $0.value as? String }) == original {
            ui.setWheels(picker, minutes: 151)
        }
        XCTAssertNotEqual(picker.pickerWheels.allElementsBoundByIndex.map { $0.value as? String }, original)
        ui.capture("dismissal-edited-draft")
        let sheet = ui.app.otherElements["reminder-native-picker"]
        XCTAssertTrue(sheet.exists)
        // Drag the sheet's top edge, outside the wheels and scrollable content.
        sheet.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.01)).press(
            forDuration: 0.1, thenDragTo: ui.app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.95)))
        let gone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: picker)
        XCTAssertEqual(XCTWaiter.wait(for: [gone], timeout: 5), .completed)
        ui.capture("dismissal-sheet-closed")
        // Reopening proves the real dismissal callback released the pending picker operation.
        ui.tap(ui.app.buttons["Change time"], scrolling: true)
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        XCTAssertEqual(picker.pickerWheels.allElementsBoundByIndex.map { $0.value as? String }, original)
        ui.capture("dismissal-original-draft-restored")
        RunLoop.current.run(until: Date().addingTimeInterval(3))
        ui.tap(ui.app.buttons["reminder-time-cancel"])
        // Ordinary foreground refresh supplies a separate actual native inventory observation.
        XCUIDevice.shared.press(.home)
        ui.app.activate()
        ui.assertScheduled()
        RunLoop.current.run(until: Date().addingTimeInterval(3))
        ui.capture("dismissal-after-foreground-inventory")
        // A real picker-host event closes host byte sampling before any language preference write.
        ui.tap(ui.app.buttons["Change time"], scrolling: true)
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        ui.tap(ui.app.buttons["reminder-time-cancel"])
        ui.language("Language", option: "Русский")
        ui.inspectLocalizedPicker(change: "Изменить время", title: "Время напоминания",
                                  helper: "По текущему времени устройства.", confirm: "Сохранить время", cancel: "Отмена")
        RunLoop.current.run(until: Date().addingTimeInterval(3))
        ui.language("Язык", option: "Қазақ тілі")
        ui.inspectLocalizedPicker(change: "Уақытты өзгерту", title: "Еске салу уақыты",
                                  helper: "Құрылғының ағымдағы жергілікті уақыты бойынша.",
                                  confirm: "Уақытты сақтау", cancel: "Бас тарту")
        RunLoop.current.run(until: Date().addingTimeInterval(3))
        ui.language("Тіл", option: "English")
        ui.assertScheduled()
        ui.inspectLocalizedPicker(change: "Change time", title: "Reminder time",
                                  helper: "Uses the device’s current local time.", confirm: "Save time", cancel: "Cancel")
    }

    func testCalendarControlProbe() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        // This phase follows verified host/system clock controls. The app receives no clock override.
        ui.tap(ui.app.switches["Reminders"], scrolling: true)
        ui.assertScheduled()
        ui.tap(ui.app.buttons["Change time"], scrolling: true)
        let picker = ui.app.datePickers["reminder-time-wheel"]
        XCTAssertTrue(picker.waitForExistence(timeout: 5))
        ui.setWheels(picker, minutes: 150)
        ui.tap(ui.app.buttons["reminder-time-confirm"])
        ui.assertScheduled()
        RunLoop.current.run(until: Date().addingTimeInterval(3))
        ui.capture("real-calendar-next-date")
        // Remove the real request before the host restores its original wall clock.
        ui.scrollTop()
        ui.tap(ui.app.switches["Reminders"], scrolling: true)
        XCTAssertTrue(ui.app.staticTexts["Off"].waitForExistence(timeout: 5))
    }
}
