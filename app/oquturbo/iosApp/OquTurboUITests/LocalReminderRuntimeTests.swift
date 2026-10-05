import XCTest

final class LocalReminderRuntimeTests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

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
        ui.assertHomeAfterCard(ui.notification(until: due))
    }

    func testWarmDeliveryFromSettings() {
        let ui = ReminderRuntimeEvidence(self)
        ui.start()
        let due = ui.selectFutureTime()
        ui.assertScheduled()
        XCUIDevice.shared.press(.home)
        XCTAssertTrue(ui.app.state == .runningBackground || ui.app.state == .runningBackgroundSuspended)
        let card = ui.notification(until: due)
        XCTAssertTrue(ui.app.state == .runningBackground || ui.app.state == .runningBackgroundSuspended)
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
        _ = ui.notification(until: due) // Deliberately do not tap/remove this notification.
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
