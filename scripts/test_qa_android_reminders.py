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


class BootRestoreTest(unittest.TestCase):
    DUE = 1791214500
    # Exact last114-before-real-reboot event from run37330307301; alarm was sampled
    # at15:33:03.004, before receiver process startup03.799/application init04.553.
    BEFORE = "1791214350265 scheduled target=1791214500000 minutes=935"
    # Hypothetical subsequent callback for deterministic polling tests; not captured native evidence.
    FRESH = BEFORE + "\n1791214384804 scheduled target=1791214500000 minutes=935"

    def run_wait(self, samples, dates=None, changed=False, offline=False):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            driver.deadline = 1000
            clock, current, observed = [0.0], [None], []
            samples = iter(samples); dates = iter(dates) if dates else None
            saved = {"reminders_enabled": "0801", "reminders_schedule_v1": string_field(
                5, b'{"version":1,"minutesOfDay":935}').hex(), "unrelated": "0102"}
            def prefs(label, **kwargs):
                driver.step += 1
                current[0] = next(samples)
                observed.append(current[0])
                return {**saved, "unrelated": "changed"} if changed else saved
            def text(*args, **kwargs):
                self.assertNotIn("check", kwargs)
                self.assertGreater(kwargs["timeout"], 0)
                if offline: raise RuntimeError("adb offline")
                if args == ("shell", "date", "+%s"):
                    return str(next(dates) if dates else self.DUE - 116)
                if args == ("exec-out", "run-as", qa.PACKAGE, "cat", "files/reminder-diagnostics.log"):
                    return current[0][0]
                self.assertEqual(("shell", "dumpsys", "alarm"), args)
                if not current[0][1]: return ""
                captured = (Path(__file__).with_name("fixtures") / "reminders-api33-owned-alarm.txt").read_text()
                return captured.replace("origWhen 1791296400000", f"origWhen {self.DUE * 1000}")
            def mutate(*args, **kwargs): self.fail("Observation must not launch an app or inject a receiver")
            def sleep(seconds): clock[0] += seconds
            driver.prefs, driver.text, driver.adb = prefs, text, mutate
            with patch.object(qa.time, "monotonic", side_effect=lambda: clock[0]), patch.object(qa.time, "sleep", side_effect=sleep):
                driver.wait_reboot_restore(self.DUE, self.BEFORE, saved)
            return observed, clock[0], len(list(Path(output).glob("*-boot-*.txt")))

    def test_empty_inventory_before_receiver_start_then_fresh_event_and_alarm(self):
        sequence = [(self.BEFORE, False), (self.BEFORE, False), (self.FRESH, False), (self.FRESH, True)]
        seen, elapsed, artifacts = self.run_wait(sequence)
        self.assertEqual(sequence, seen)
        self.assertEqual(3, elapsed)
        self.assertEqual(8, artifacts)

    def test_stale_preboot_event_or_missing_alarm_never_passes_and_wait_is_bounded(self):
        for sample in [(self.BEFORE, True), (self.FRESH, False)]:
            with self.assertRaisesRegex(TimeoutError, "within 30s"):
                self.run_wait([sample] * 31)

    def test_changed_prefs_lost_journal_receiver_failure_and_transport_fail_closed(self):
        with self.assertRaisesRegex(AssertionError, "persisted preferences"):
            self.run_wait([(self.FRESH, True)], changed=True)
        with self.assertRaisesRegex(AssertionError, "captured prefix"):
            self.run_wait([("replacement journal", True)])
        with self.assertRaisesRegex(AssertionError, "receiver reported"):
            self.run_wait([(self.BEFORE + "\n1791214384000 receiver-failed TimeoutCancellationException", False)])
        with self.assertRaisesRegex(RuntimeError, "adb offline"):
            self.run_wait([], offline=True)

    def test_due_guard_before_and_after_reads_prevents_claiming_missed_restoration(self):
        for dates in [[self.DUE], [self.DUE - 1, self.DUE]]:
            with self.assertRaisesRegex(AssertionError, "missed the chosen occurrence"):
                self.run_wait([(self.FRESH, True)], dates=dates)


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

class CompactTransitionTest(unittest.TestCase):
    fixture = Path(__file__).with_name("fixtures") / "reminders-api33-compact-launcher-activity.txt"

    def inventory(self, package="com.android.launcher3", font="1.5"):
        # Captured failure inventory; variants model later observed transitions, not native proof.
        dump = self.fixture.read_text()
        if package == qa.PACKAGE:
            dump = dump.replace("com.android.launcher3", qa.PACKAGE + ".fixture")
            dump = dump.replace(qa.PACKAGE + ".fixture/.uioverrides.QuickstepLauncher", qa.PACKAGE + "/.MainActivity")
        return dump.replace("CurrentConfiguration={1.5 ", "CurrentConfiguration={" + font + " ")

    def test_captured_launcher_is_not_mistaken_for_stopped_background_app(self):
        self.assertEqual(("com.android.launcher3/.uioverrides.QuickstepLauncher", 1.5), qa.resumed_activity(self.fixture.read_text()))
        self.assertEqual((qa.PACKAGE + "/.MainActivity", 1.0), qa.resumed_activity(self.inventory(qa.PACKAGE, "1.0")))
        for dump in ["error: device offline", self.inventory().replace("state=RESUMED", "state=STOPPED"),
                     self.inventory().replace("topResumedActivity=", "topResumedActivity=ActivityRecord{unknown}\n topResumedActivity=")]:
            with self.assertRaises(AssertionError): qa.resumed_activity(dump)

    def test_home_must_really_resume_before_app_launch_then_app_before_ui(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            events, now = [], [0.0]
            driver.deadline = 100
            observations = iter([self.inventory(qa.PACKAGE), self.inventory(), self.inventory(), self.inventory(qa.PACKAGE)])
            def text(*args, **kwargs):
                dump = next(observations)
                events.append(qa.resumed_activity(dump)[0].split("/")[0])
                return dump
            driver.text = text
            driver.adb = lambda *args, **kwargs: events.append("HOME")
            def launch():
                self.assertEqual("com.android.launcher3", events[-1])
                events.append("launch")
            driver.launch = launch
            with patch.object(qa.time, "monotonic", side_effect=lambda: now[0]), \
                    patch.object(qa.time, "sleep", side_effect=lambda seconds: now.__setitem__(0, now[0]+seconds)):
                driver.resume_compact_app()
            self.assertEqual(["HOME", qa.PACKAGE, "com.android.launcher3", "launch", "com.android.launcher3", qa.PACKAGE], events)
            self.assertEqual(4, len(list(Path(output).glob("*-activity.txt"))))

    def test_launcher_timeout_or_transport_failure_never_launches_app(self):
        for offline in (False, True):
            with tempfile.TemporaryDirectory() as output:
                driver = qa.Driver("owned-test-serial", Path(output))
                now, launches = [0.0], []
                driver.deadline = 100
                driver.adb = lambda *args, **kwargs: None
                def text(*args, **kwargs):
                    if offline: raise RuntimeError("device offline")
                    return self.inventory(qa.PACKAGE)
                driver.text, driver.launch = text, lambda: launches.append(True)
                with patch.object(qa.time, "monotonic", side_effect=lambda: now[0]), \
                        patch.object(qa.time, "sleep", side_effect=lambda seconds: now.__setitem__(0, now[0]+seconds)), \
                        self.assertRaises(RuntimeError if offline else TimeoutError):
                    driver.resume_compact_app()
                self.assertEqual([], launches)

    def test_absent_font_waits_effective_default_and_redeletes_late_os_write(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            now, calls = [0.0], []
            driver.deadline = 100
            configs = iter([self.inventory(), self.inventory(font="1.0")])
            values = iter(["null", "1.0", "null", "null", "null"])
            def text(*args, **kwargs):
                if args[1] == "dumpsys":
                    dump = next(configs); calls.append(qa.resumed_activity(dump)[1]); return dump
                return next(values)
            driver.text = text
            driver.adb = lambda *args, **kwargs: calls.append(args)
            with patch.object(qa.time, "monotonic", side_effect=lambda: now[0]), \
                    patch.object(qa.time, "sleep", side_effect=lambda seconds: now.__setitem__(0, now[0]+seconds)):
                driver.restore_absent_font_scale()
            self.assertEqual([("shell", "settings", "put", "system", "font_scale", "1.0"), 1.5, 1.0,
                              ("shell", "settings", "delete", "system", "font_scale"),
                              ("shell", "settings", "delete", "system", "font_scale")], calls)
            evidence = [qa.json.loads(line)["value"] for line in Path(output, "N12-font-restoration.jsonl").read_text().splitlines()]
            self.assertEqual(["null", "1.0", "null", "null", "null"], evidence)

    def test_absent_font_never_accepts_persistent_default_unknown_or_transport_error(self):
        for value in ("1.0", "1.5", "device offline"):
            with self.subTest(value=value), tempfile.TemporaryDirectory() as output:
                driver = qa.Driver("owned-test-serial", Path(output))
                now = [0.0]; driver.deadline = 100
                driver.adb = lambda *args, **kwargs: None
                driver.wait_compact_activity = lambda *args, **kwargs: None
                def text(*args, **kwargs):
                    if value == "device offline": raise RuntimeError(value)
                    return value
                driver.text = text
                with patch.object(qa.time, "monotonic", side_effect=lambda: now[0]), \
                        patch.object(qa.time, "sleep", side_effect=lambda seconds: now.__setitem__(0, now[0]+seconds)), \
                        self.assertRaises((TimeoutError, AssertionError, RuntimeError)):
                    driver.restore_absent_font_scale()

    def test_primary_failure_is_retained_separately_when_cleanup_also_fails(self):
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            driver.text = lambda *args, **kwargs: "null"
            driver.prefs = lambda label: {"language": string_field(5, b"en").hex(),
                "reminders_enabled": "0800", "reminders_schedule_v1": string_field(5, b'{"minutesOfDay":998}').hex()}
            def fail(): raise AssertionError("actual Settings unavailable")
            driver.ensure_settings_top = fail
            driver.restore_compact_settings = lambda original: ["font_scale: restore unavailable"]
            with self.assertRaisesRegex(RuntimeError, "N12 restoration failed"):
                driver.compact_kazakh_picker()
            self.assertEqual({"type": "AssertionError", "error": "actual Settings unavailable"},
                             qa.json.loads(Path(output, "N12-primary-error.json").read_text()))
            self.assertEqual({"errors": ["font_scale: restore unavailable"]},
                             qa.json.loads(Path(output, "N12-restoration.json").read_text()))

    def test_absent_font_failure_does_not_prevent_time_format_restore(self):
        original = {"size": "Physical size: 1080x1920", "density": "Physical density: 420", "font_scale": "null", "time_12_24": "24"}
        with tempfile.TemporaryDirectory() as output:
            driver = qa.Driver("owned-test-serial", Path(output))
            calls = []
            driver.adb = lambda *args, **kwargs: calls.append(args)
            driver.text = lambda *args, **kwargs: original[args[-1]]
            def fail(): raise TimeoutError("OS did not restore absence")
            driver.restore_absent_font_scale = fail
            self.assertEqual(["font_scale: OS did not restore absence"], driver.restore_compact_settings(original))
            self.assertEqual(("shell", "settings", "put", "system", "time_12_24", "24"), calls[-1])


if __name__ == '__main__':
    unittest.main()
