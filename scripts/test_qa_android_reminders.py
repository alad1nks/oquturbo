"""Pure harness checks; none is Android runtime evidence."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import copy
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
