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
