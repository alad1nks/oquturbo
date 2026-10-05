"""Pure harness checks; none is Android runtime evidence."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import copy
import os
import tempfile
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('reminders_qa', Path(__file__).with_name('qa-android-reminders.py'))
qa = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qa)


def string_field(number, value):
    assert len(value) < 128
    return bytes([(number << 3) | 2, len(value)]) + value


class EvidenceParserTest(unittest.TestCase):
    def test_preferences_preserve_unknown_typed_values_and_decode_snapshot(self):
        key = b'reminders_schedule_v1'
        value = string_field(5, b'{"version":1}')
        encoded = string_field(1, string_field(1, key) + string_field(2, value))
        decoded = qa.preferences(encoded)
        self.assertEqual({'reminders_schedule_v1': value.hex()}, decoded)
        self.assertEqual('{"version":1}', qa.preference_string(decoded, 'reminders_schedule_v1'))

    def test_truncated_invalid_and_duplicate_data_fail_closed(self):
        for data in [b'\x0a\x05x', b'\x80', b'\x00', b'\x0f']:
            with self.assertRaises(ValueError): list(qa.protobuf_fields(data))
        entry = string_field(1, string_field(1, b'key') + string_field(2, b'\x08\x01'))
        with self.assertRaises(ValueError): qa.preferences(entry + entry)

    def test_pending_inventory_excludes_history_registered_channel_and_other_package(self):
        header = ' RTC_WAKEUP #0: Alarm{123 type 0 origWhen 123 com.alad1nks.oquturbo}\n'
        alarm = header + ' tag=*walarm*:com.alad1nks.oquturbo.PRACTICE_REMINDER\n window=+1h\n'
        self.assertEqual(1, len(qa.pending_alarms(alarm)))
        self.assertEqual([], qa.pending_alarms('Recent history: ' + qa.ACTION))
        self.assertEqual([], qa.pending_alarms(alarm.replace('com.alad1nks.oquturbo}', 'another.package}')))
        self.assertEqual(2, len(qa.pending_alarms(alarm + alarm.replace('#0', '#1'))))


class BackgroundProcessTest(unittest.TestCase):
    def run_stop(self, pids, due=None, device_times=None, command_error=False):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            clock = [0.0]
            driver.deadline = 1000
            calls = []
            observed = iter(pids)
            dates = iter(device_times or [])

            def adb(*args, **kwargs):
                calls.append(args)
                self.assertEqual(("shell", "am", "kill", qa.PACKAGE), args)

            def text(*args, **kwargs):
                self.assertNotIn("check", kwargs)  # errors cannot become false absence
                if args == ("shell", "date", "+%s"):
                    return str(next(dates))
                self.assertEqual(("shell", qa.PIDOF_COMMAND), args)
                if command_error:
                    raise RuntimeError("device disconnected")
                value = next(observed)
                return qa.PROCESS_ABSENT if value == "" else value

            def sleep(seconds):
                clock[0] += seconds

            driver.adb, driver.text = adb, text
            with patch.object(qa.time, "monotonic", side_effect=lambda: clock[0]), patch.object(qa.time, "sleep", side_effect=sleep):
                driver.stop_background_process("test", before_due=due)
            return calls, (Path(output) / "test-process-stop.jsonl").read_text(), clock[0]

    def test_remote_pidof_protocol_distinguishes_absent_present_and_errors(self):
        with tempfile.TemporaryDirectory() as directory:
            fake = Path(directory) / "pidof"
            for body, code, output in [
                ("exit 1", 0, qa.PROCESS_ABSENT),
                ("echo '2407 2408'", 0, "2407 2408"),
                ("echo 'permission denied' >&2; exit 1", 1, ""),
                ("exit 127", 1, ""),
                ("exit 0", 1, ""),
            ]:
                fake.write_text("#!/bin/sh\n" + body + "\n")
                fake.chmod(0o755)
                result = qa.subprocess.run(["/bin/sh", "-c", qa.PIDOF_COMMAND],
                                           env={**os.environ, "PATH": directory}, capture_output=True, text=True)
                self.assertEqual(code, result.returncode, body)
                self.assertEqual(output, result.stdout.strip(), body)

    def test_home_transition_noop_retries_until_actual_absence_before_due(self):
        # Actual run37270962350 had only68ms between HOME and pidof; ordinary kill can be a no-op.
        calls, journal, elapsed = self.run_stop(["2407", "2407", ""], due=1791181320,
                                              device_times=[1791181232, 1791181232, 1791181233, 1791181233, 1791181234, 1791181234])
        self.assertEqual(3, len(calls))
        self.assertEqual(2, elapsed)
        self.assertEqual(["2407", "2407", ""], [qa.json.loads(line)["pids"] for line in journal.splitlines()])

    def test_persistent_process_fails_at_bounded_deadline(self):
        with self.assertRaisesRegex(TimeoutError, "within 30s"):
            self.run_stop(["2407"] * 31)

    def test_due_reached_before_or_after_kill_and_transport_failure_never_pass(self):
        for times, pids in [([100], []), ([99, 100], [""])]:
            with self.assertRaisesRegex(AssertionError, "missed the chosen due"):
                self.run_stop(pids, due=100, device_times=times)
        with self.assertRaisesRegex(RuntimeError, "device disconnected"):
            self.run_stop([], command_error=True)
        with self.assertRaisesRegex(AssertionError, "Unknown process inventory"):
            self.run_stop(["garbled output"])


class PermissionDismissalTest(unittest.TestCase):
    def test_back_waits_for_actual_prompt_then_requires_return_to_settings(self):
        prompt = ET.parse(Path(__file__).with_name("fixtures") / "reminders-api33-notification-permission.xml").getroot()
        settings = ET.fromstring(f'<hierarchy><node text="Settings" package="{qa.PACKAGE}"/></hierarchy>')
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            frames = iter([settings, prompt, prompt, settings])
            timeline = []
            def snapshot(label):
                timeline.append(label)
                return next(frames)
            driver.snapshot = snapshot
            driver.adb = lambda *args, **kwargs: timeline.append(args)
            with patch.object(qa.time, "sleep"):
                driver.dismiss_notification_prompt()
            self.assertEqual([
                "await-notification-prompt", "await-notification-prompt",
                ("shell", "input", "keyevent", "KEYCODE_BACK"),
                "notification-prompt-dismissed", "notification-prompt-dismissed",
            ], timeline)

    def test_missing_ambiguous_prompt_or_wrong_return_never_claims_dismissal(self):
        prompt = ET.parse(Path(__file__).with_name("fixtures") / "reminders-api33-notification-permission.xml").getroot()
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            calls = []
            driver.adb = lambda *args, **kwargs: calls.append(args)
            driver.snapshot = lambda label: ET.fromstring('<hierarchy/>')
            with patch.object(qa.time, "sleep"), self.assertRaisesRegex(AssertionError, "Back not sent"):
                driver.dismiss_notification_prompt()
            self.assertEqual([], calls)
            duplicate = copy.deepcopy(prompt)
            duplicate.append(copy.deepcopy(driver.find(duplicate, resource="com.android.permissioncontroller:id/permission_allow_button")))
            driver.snapshot = lambda label: duplicate
            with self.assertRaisesRegex(AssertionError, "Ambiguous"):
                driver.dismiss_notification_prompt()
            self.assertEqual([], calls)
            driver.snapshot = lambda label: prompt
            with patch.object(qa.time, "sleep"), self.assertRaisesRegex(AssertionError, "return to Settings"):
                driver.dismiss_notification_prompt()
            self.assertEqual([("shell", "input", "keyevent", "KEYCODE_BACK")], calls)


class NativeNotificationSwitchTest(unittest.TestCase):
    def fixture(self, enabled):
        name = "reminders-api33-settings-notifications-" + ("on" if enabled else "off") + ".xml"
        return ET.parse(Path(__file__).with_name("fixtures") / name).getroot()

    def test_actual_master_off_on_ignores_new_badge_switch_and_verifies_selected_state(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            for enabled in (True, False):
                frames = iter([self.fixture(not enabled), self.fixture(enabled)])
                calls = []
                driver.snapshot = lambda label: next(frames)
                driver.adb = lambda *args, **kwargs: calls.append(args)
                with patch.object(qa.time, "sleep"):
                    driver.toggle_system(enabled)
                self.assertEqual([("shell", "input", "tap", "912", "996")], calls)
            frames = iter([self.fixture(False), self.fixture(False)])
            driver.snapshot = lambda label: next(frames)
            with patch.object(qa.time, "sleep"), self.assertRaisesRegex(AssertionError, "did not reach requested"):
                driver.toggle_system(True)

    def test_main_bar_and_its_switch_must_be_unique_and_from_correct_settings_page(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            root = self.fixture(True)
            bar = driver.find(root, resource="com.android.settings:id/main_switch_bar")
            root.append(copy.deepcopy(bar))
            with self.assertRaisesRegex(AssertionError, "Ambiguous"):
                driver.notification_switch(root)
            root = self.fixture(True)
            bar = driver.find(root, resource="com.android.settings:id/main_switch_bar")
            bar.append(copy.deepcopy(driver.find(bar, resource="android:id/switch_widget")))
            with self.assertRaisesRegex(AssertionError, "Ambiguous"):
                driver.notification_switch(root)
            with self.assertRaisesRegex(AssertionError, "Wrong app/channel"):
                driver.notification_switch(self.fixture(True), channel=True)
            root = self.fixture(True)
            driver.find(root, resource="com.android.settings:id/main_switch_bar").set("package", "unrelated")
            with self.assertRaisesRegex(AssertionError, "Missing native"):
                driver.notification_switch(root)

    def test_source_backed_channel_main_bar_is_distinct_from_app_master(self):
        # Synthetic channel variant of actual app fixture: AOSP13 BlockPreferenceController
        # uses the same MainSwitchPreference with "Show notifications" for a channel.
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            frames = [self.fixture(False), self.fixture(True)]
            for root in frames:
                bar = driver.find(root, resource="com.android.settings:id/main_switch_bar")
                driver.find(bar, resource="com.android.settings:id/switch_text").set("text", "Show notifications")
            iterator = iter(frames)
            driver.snapshot = lambda label: next(iterator)
            calls = []
            driver.adb = lambda *args, **kwargs: calls.append(args)
            with patch.object(qa.time, "sleep"):
                driver.toggle_system(True, channel=True)
            self.assertEqual([("shell", "input", "tap", "912", "996")], calls)
            with self.assertRaisesRegex(AssertionError, "Wrong app/channel"):
                driver.notification_switch(frames[-1], channel=False)


class EditedScheduleTest(unittest.TestCase):
    DUE, MINUTE = 1791210180, 863

    def run_wait(self, samples, read_error=False, times=None):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            clock = [0.0]
            driver.deadline = 1000
            samples = iter(samples)
            current = [None]
            seen = []
            dates = iter(times) if times is not None else None

            def prefs(label, **kwargs):
                if read_error:
                    raise RuntimeError("device offline")
                current[0] = next(samples)
                seen.append(current[0])
                driver.step += 1
                minute, _, _, enabled = current[0]
                payload = qa.json.dumps({"version": 1, "minutesOfDay": minute}).encode()
                return {"reminders_enabled": enabled, "reminders_schedule_v1": string_field(5, payload).hex()}

            def text(*args, **kwargs):
                self.assertNotIn("check", kwargs)
                self.assertGreater(kwargs["timeout"], 0)
                if args == ("shell", "date", "+%s"):
                    return str(next(dates) if dates is not None else self.DUE - 60)
                minute, event, alarm, _ = current[0]
                if args[0] == "exec-out":
                    return f"1791210065681 scheduled target={event} minutes={self.MINUTE}"
                self.assertEqual(("shell", "dumpsys", "alarm"), args)
                captured = (Path(__file__).with_name("fixtures") / "reminders-api33-owned-alarm.txt").read_text()
                return captured.replace("origWhen 1791296400000", f"origWhen {alarm}")

            driver.prefs, driver.text = prefs, text
            def sleep(seconds):
                clock[0] += seconds
            with patch.object(qa.time, "monotonic", side_effect=lambda: clock[0]), patch.object(qa.time, "sleep", side_effect=sleep):
                driver.wait_edited_schedule(self.DUE, self.MINUTE)
            return seen, clock[0], list(Path(output).glob("*-edited-*.txt"))

    def test_actual_old_snapshot_does_not_pass_until_write_event_and_alarm_all_agree(self):
        # Run37321566592: chosen-76=863/due1791210180, 077 prefs still860,
        # 076 OS alarm origWhen1791296400000; later082 prefs and dispatch confirm863.
        old = (860, 1791296400000, 1791296400000, "0801")
        persisted_only = (863, 1791296400000, 1791296400000, "0801")
        event_only = (863, self.DUE * 1000, 1791296400000, "0801")
        ready = (863, self.DUE * 1000, self.DUE * 1000, "0801")
        seen, elapsed, artifacts = self.run_wait([old, persisted_only, event_only, ready])
        self.assertEqual([old, persisted_only, event_only, ready], seen)
        self.assertEqual(3, elapsed)
        self.assertEqual(8, len(artifacts))

    def test_stale_or_disabled_state_times_out_and_read_errors_do_not_pass(self):
        for state in [(860, self.DUE * 1000, self.DUE * 1000, "0801"),
                      (863, self.DUE * 1000, self.DUE * 1000, "0800")]:
            with self.assertRaisesRegex(TimeoutError, "within 30s"):
                self.run_wait([state] * 31)
        with self.assertRaisesRegex(RuntimeError, "device offline"):
            self.run_wait([], read_error=True)

    def test_real_api33_epoch_header_and_next_schedule_agree_not_formatted_detail(self):
        dump = (Path(__file__).with_name("fixtures") / "reminders-api33-owned-alarm.txt").read_text()
        alarm = qa.pending_alarms(dump)[0]
        self.assertEqual(1791296400000, qa.alarm_epoch(alarm))
        self.assertIn("origWhen=2026-10-06 14:20:00.000", alarm)
        for invalid in [alarm.replace("origWhen 1791296400000", "origWhen=1791296400000"),
                        alarm.replace("com.alad1nks.oquturbo}", "another.package}")]:
            with self.assertRaisesRegex(AssertionError, "Unrecognized"):
                qa.alarm_epoch(invalid)
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            driver.read_diagnostics = lambda: "scheduled target=1791296400000 minutes=860"
            def text(*args):
                if args == ("shell", "date", "+%s"): return "1791210000"
                if args == ("shell", "getprop", "persist.sys.timezone"): return "UTC"
                self.assertEqual(("shell", "dumpsys", "alarm"), args)
                return dump
            driver.text = text
            self.assertEqual((1791296400000, 860), driver.next_schedule())
            driver.read_diagnostics = lambda: "scheduled target=1791296460000 minutes=861"
            with self.assertRaisesRegex(AssertionError, "does not match"):
                driver.next_schedule()

    def test_due_reached_before_or_during_observation_fails(self):
        ready = (863, self.DUE * 1000, self.DUE * 1000, "0801")
        for times in [[self.DUE], [self.DUE - 1, self.DUE]]:
            with self.assertRaisesRegex(AssertionError, "missed the chosen due"):
                self.run_wait([ready], times=times)

    def test_picker_waits_only_for_enabled_edit_not_initial_permission_or_cancel(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            driver.tap = lambda **kwargs: None
            driver.text = lambda *args: str(self.DUE - 150)
            driver.snapshot = lambda label: ET.fromstring('<hierarchy><node resource-id="android:id/input_hour"/></hierarchy>')
            driver.adb = lambda *args: None
            calls = []
            driver.wait_edited_schedule = lambda *args: calls.append(args)
            driver.future_picker(first=True)
            driver.future_picker(cancel=True)
            self.assertEqual([], calls)
            due = driver.future_picker()
            self.assertEqual([(due, self.MINUTE)], calls)


class CompactPickerTest(unittest.TestCase):
    def picker(self, minute):
        # Synthetic KK/compact variant of public native clock IDs, observed in the API28
        # en-picker-cancel tree; this does not claim API33/KK native rendering passed.
        root = ET.Element("hierarchy")
        values = {"timePicker": "", "message": "Құрылғының ағымдағы жергілікті уақыты бойынша.",
                  "button1": "УАҚЫТТЫ САҚТАУ", "button2": "БАС ТАРТУ",
                  "hours": str(minute // 60 % 12 or 12), "minutes": f"{minute % 60:02d}",
                  "am_label": "AM", "pm_label": "PM"}
        for key, value in values.items():
            ET.SubElement(root, "node", {"resource-id": "android:id/" + key, "text": value,
                         "bounds": "[10,20][300,80]", "package": qa.PACKAGE, "enabled": "true", "clickable": "true",
                         "checked": str((key == "am_label" and minute < 720) or (key == "pm_label" and minute >= 720)).lower()})
        return root

    def test_card_and_native_period_preserve_midnight_noon_and_nonzero_minutes(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            for value, minute in [("12:00 AM", 0), ("12:00 PM", 720), ("2:23\u202fPM", 863), ("2:30 AM", 150)]:
                self.assertEqual(minute, qa.twelve_hour_minutes(value))
                driver.compact_picker_check(self.picker(minute), minute)
            for value in ["00:00 AM", "14:23", "2:60 PM", "2:23", "2:23 PM extra"]:
                with self.assertRaises(AssertionError): qa.twelve_hour_minutes(value)

    def test_wrong_period_clipped_or_missing_localized_actions_fail_closed(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            variants = [("pm_label", "checked", "false"), ("button1", "text", "УАҚЫТТЫ…"),
                        ("button2", "bounds", "[10,610][300,670]"), ("button2", "clickable", "false"),
                        ("message", "package", "other.package"), ("hours", "text", "3")]
            for key, attribute, value in variants:
                root = self.picker(863)
                driver.find(root, resource="android:id/" + key).set(attribute, value)
                with self.subTest(key=key, attribute=attribute), self.assertRaises(AssertionError):
                    driver.compact_picker_check(root, 863)
            root = self.picker(863)
            root.append(copy.deepcopy(driver.find(root, resource="android:id/button1")))
            with self.assertRaisesRegex(AssertionError, "Ambiguous"):
                driver.compact_picker_check(root, 863)

    def test_static_privacy_content_requires_one_back_then_existing_settings_reopen(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            state, calls = ["Settings"], []
            description = "Құпиялық саясаты туралы ақпарат осында қолжетімді болады"
            def snapshot(label):
                root = ET.Element("hierarchy")
                if state[0] == "Settings":
                    for caption in ("Баптаулар", "Құпиялық саясаты", description):
                        ET.SubElement(root, "node", {"text": caption, "package": qa.PACKAGE,
                                      "enabled": "true", "bounds": "[10,20][310,90]"})
                elif state[0] == "Profile":
                    ET.SubElement(root, "node", {"content-desc": "Баптаулар"})
                return root
            def tap(**kwargs):
                calls.append(kwargs)
                if kwargs.get("text") == description:
                    self.assertEqual("Settings", state[0])  # Static content remains in Settings.
                else:
                    self.assertEqual({"description": "Баптаулар"}, kwargs)
                    self.assertEqual("Profile", state[0])
                    state[0] = "Settings"
            def back():
                calls.append("Back")
                state[0] = "Profile" if state[0] == "Settings" else "Home"
            driver.tap, driver.back, driver.snapshot = tap, back, snapshot
            driver.compact_privacy_and_back()
            self.assertEqual([{"text": description, "scroll": True}, "Back", {"description": "Баптаулар"}], calls)
            self.assertEqual("Settings", state[0])
            calls.clear(); state[0] = "Profile"
            driver.reopen_kazakh_settings(snapshot("restore"))
            self.assertEqual([{"description": "Баптаулар"}], calls)
            calls.clear(); state[0] = "Unknown"
            with self.assertRaisesRegex(AssertionError, "actual Settings or Profile"):
                driver.reopen_kazakh_settings(snapshot("restore"))
            self.assertEqual([], calls)

    def test_exact_override_or_reset_restoration_attempts_all_even_after_error(self):
        original = {"size": "Physical size: 1080x1920\nOverride size: 800x1200",
                    "density": "Physical density: 420", "font_scale": "1.0", "time_12_24": "null"}
        self.assertEqual("800x1200", qa.display_override(original["size"], "size"))
        self.assertEqual("reset", qa.display_override(original["density"], "density"))
        with self.assertRaises(AssertionError): qa.display_override("device offline", "size")
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            for fail_size in (False, True):
                calls = []
                def adb(*args):
                    calls.append(args)
                    if fail_size and args == ("shell", "wm", "size", "800x1200"):
                        raise RuntimeError("size restore unavailable")
                driver.adb = adb
                driver.text = lambda *args: original[args[-1]]
                errors = driver.restore_compact_settings(original)
                self.assertEqual(1 if fail_size else 0, len(errors))
                self.assertEqual([("shell", "wm", "size", "800x1200"), ("shell", "wm", "density", "reset"),
                                  ("shell", "settings", "put", "system", "font_scale", "1.0"),
                                  ("shell", "settings", "delete", "system", "time_12_24")], calls)


class AlarmHelpTest(unittest.TestCase):
    # Exact required lines from API33 AlarmManagerService.onHelp. Status -1 is returned
    # by BasicShellCommandHandler.handleDefaultCommands for help and arrives as255.
    help_output = b"Alarm manager service (alarm) commands:\n  help\n    Print this help text.\n  set-time TIME\n  set-timezone TZ\n"

    def test_api33_help_exit255_preserves_raw_evidence_and_exact_content(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            for code in (255, 0):
                response = qa.subprocess.CompletedProcess([], code, self.help_output, b"")
                with patch.object(qa.subprocess, "run", return_value=response) as command:
                    self.assertEqual(self.help_output.decode().strip(), driver.text("shell", "cmd", "alarm", "help", alarm_help=True))
                self.assertEqual(["adb", "-s", "owned-test-serial", "shell", "cmd", "alarm", "help"], command.call_args.args[0])
                saved = qa.json.loads((Path(output) / "alarm-help.json").read_text())
                self.assertEqual({"code": code, "stdout": self.help_output.decode(), "stderr": ""}, saved)

    def test_missing_help_or_transport_or_unknown_status_fails_closed(self):
        cases = [(1, self.help_output, b""), (255, self.help_output, b"error: device offline"),
                 (255, b"", b""), (255, b"Unknown command: help", b""),
                 (255, self.help_output.replace(b"set-time TIME", b"set-time unavailable"), b"")]
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            for code, stdout, stderr in cases:
                response = qa.subprocess.CompletedProcess([], code, stdout, stderr)
                with patch.object(qa.subprocess, "run", return_value=response), self.assertRaisesRegex(RuntimeError, "Unverified alarm help"):
                    driver.text("shell", "cmd", "alarm", "help", alarm_help=True)
                self.assertEqual(code, qa.json.loads((Path(output) / "alarm-help.json").read_text())["code"])

    def test_clock_mutations_do_not_inherit_help_exit_allowance(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            response = qa.subprocess.CompletedProcess([], 255, self.help_output, b"")
            for arguments in [("shell", "cmd", "alarm", "set-time", "123"),
                              ("shell", "cmd", "alarm", "set-timezone", "UTC")]:
                with patch.object(qa.subprocess, "run", return_value=response), self.assertRaises(RuntimeError):
                    driver.adb(*arguments)
                with patch.object(qa.subprocess, "run", return_value=response), self.assertRaisesRegex(AssertionError, "limited to"):
                    driver.adb(*arguments, alarm_help=True)


class EvidenceRetentionTest(unittest.TestCase):
    def test_duplicate_control_fails_instead_of_tapping_arbitrary_match(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            root = ET.fromstring('<hierarchy><node text="Allow"/><node text="Allow"/></hierarchy>')
            with self.assertRaisesRegex(AssertionError, "Ambiguous"):
                driver.find(root, text="Allow")
            self.assertIsNone(driver.find(root, text="Missing"))

    def test_captured_native_picker_uses_unique_resource_ids_despite_uppercase_captions(self):
        root = ET.parse(Path(__file__).with_name("fixtures") / "reminders-api33-native-time-picker.xml").getroot()
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            self.assertIsNone(driver.find(root, text="Cancel"))
            self.assertIsNone(driver.find(root, text="Save and enable"))
            calls = []
            driver.snapshot = lambda label: root
            driver.adb = lambda *args, **kwargs: calls.append(args)
            with patch.object(qa.time, "sleep"):
                driver.tap(resource="android:id/button2", native_time_picker=True)
                driver.tap(resource="android:id/button1", native_time_picker=True)
            self.assertEqual([
                ("shell", "input", "tap", "463", "1438"),
                ("shell", "input", "tap", "755", "1438"),
            ], calls)

    def test_native_dialog_button_is_not_used_without_our_picker_or_when_ambiguous(self):
        fixture = ET.parse(Path(__file__).with_name("fixtures") / "reminders-api33-native-time-picker.xml").getroot()
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            calls = []
            driver.adb = lambda *args, **kwargs: calls.append(args)
            root = copy.deepcopy(fixture)
            picker = driver.find(root, resource="android:id/timePicker")
            picker.set("resource-id", "unrelated:id/dialog")
            driver.snapshot = lambda label: root
            with patch.object(qa.time, "sleep"), self.assertRaisesRegex(AssertionError, "Control unavailable"):
                driver.tap(resource="android:id/button2", native_time_picker=True)
            root = copy.deepcopy(fixture)
            button = driver.find(root, resource="android:id/button2")
            root.append(copy.deepcopy(button))
            with patch.object(qa.time, "sleep"), self.assertRaisesRegex(AssertionError, "Ambiguous"):
                driver.tap(resource="android:id/button2", native_time_picker=True)
            self.assertEqual([], calls)

    def test_repeated_episode_labels_keep_both_native_captures(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            driver.adb = lambda *args, **kwargs: b'first'
            driver.native("delivered")
            driver.adb = lambda *args, **kwargs: b'second'
            driver.native("delivered")
            alarms = sorted(Path(output).glob('*-delivered-alarm.txt'))
            self.assertEqual([b'first', b'second'], [p.read_bytes() for p in alarms])
            self.assertEqual(2, len(list(Path(output).glob('*-delivered-events.txt'))))


if __name__ == '__main__':
    unittest.main()
