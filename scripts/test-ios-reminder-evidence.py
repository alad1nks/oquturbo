import copy
import json
import importlib.util
from pathlib import Path
import unittest
import tempfile
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("evidence", Path(__file__).with_name("ios-reminder-evidence.py"))
evidence = importlib.util.module_from_spec(spec)
spec.loader.exec_module(evidence)


class EvidenceTest(unittest.TestCase):
    def fixture(self, phase="cold"):
        schedule = {"version": 1, "minutesOfDay": 150, "content": {"languageCode": "kk", "title": "Жаттығу", "body": "Уақыт"}}
        snapshots = [{"enabled": True, "schedule": schedule, "values": {"unchanged": "0102"}}]
        launch = "cold-new" if phase == "cold" else "existing"
        events = [
            {"event": "accepted", "time": "100", "launch": "existing"},
            {"event": "pending", "time": "100", "count": "1", "launch": "existing"},
            {"event": "request", "time": "100", "launch": "existing", "id": evidence.OWNED, "calendar": "true",
             "repeats": "true", "timezone": "null", "next": "200", "hour": "2", "minute": "30", "language": "kk",
             "title": "Жаттығу", "body": "Уақыт"},
            {"event": "response", "time": "202", "launch": launch, "id": evidence.OWNED,
             "action": "com.apple.UNNotificationDefaultActionIdentifier", "delivered": "200", "eventId": "1"},
            {"event": "home-consumed", "time": "203", "launch": launch, "eventId": "1", "destination": "Home"},
        ]
        return events, snapshots, {"unchanged": "0102"}

    def test_cold_and_warm_require_actual_different_or_same_process(self):
        for phase in ("cold", "warm"):
            events, snapshots, baseline = self.fixture(phase)
            self.assertEqual("PASS", evidence.validate_phase(phase, events, snapshots, baseline)["status"])
            with self.assertRaises(ValueError):
                evidence.validate_phase("warm" if phase == "cold" else "cold", events, snapshots, baseline)

    def test_no_actual_card_or_no_actual_home_is_not_evidence(self):
        for removed in ("response", "home-consumed"):
            events, snapshots, baseline = self.fixture()
            with self.assertRaises(ValueError):
                evidence.validate_phase("cold", [e for e in events if e["event"] != removed], snapshots, baseline)

    def test_content_shape_and_timing_must_match_native_and_durable(self):
        for key, value in (("timezone", "UTC"), ("minute", "31"), ("body", "Old content"), ("next", "1"), ("repeats", "false")):
            events, snapshots, baseline = self.fixture()
            events[2][key] = value
            with self.assertRaises(ValueError):
                evidence.validate_phase("cold", events, snapshots, baseline)

    def test_duplicate_consumption_or_pending_is_not_allowed(self):
        events, snapshots, baseline = self.fixture()
        with self.assertRaises(ValueError):
            evidence.validate_phase("cold", events + [events[-1]], snapshots, baseline)
        events[1]["count"] = "2"
        with self.assertRaises(ValueError):
            evidence.validate_phase("cold", events, snapshots, baseline)

    def test_non_reminder_preferences_cannot_change(self):
        events, snapshots, baseline = self.fixture()
        snapshots[-1]["values"]["progress"] = "abcd"
        with self.assertRaises(ValueError):
            evidence.validate_phase("cold", events, snapshots, baseline)

    def test_no_banner_without_real_foreground_callback_is_not_pass(self):
        events, snapshots, baseline = self.fixture("warm")
        events = events[:3]
        with self.assertRaises(ValueError):
            evidence.validate_phase("foreground", events, snapshots, baseline)
        events.append({"event": "foreground-delivery", "time": "200", "launch": "existing", "presentation": "none", "delivered": "200"})
        self.assertEqual("PASS", evidence.validate_phase("foreground", events, snapshots, baseline)["status"])

    def test_off_requires_retained_time_and_both_real_empty_inventories(self):
        events, snapshots, baseline = self.fixture()
        events = events[:3] + [
            {"event": "delivered-record", "id": evidence.OWNED, "time": "200"},
            {"event": "cancelled", "time": "210"},
            {"event": "pending", "time": "211", "count": "0"},
            {"event": "delivered", "time": "211", "count": "0"},
        ]
        snapshots.append(copy.deepcopy(snapshots[-1]))
        snapshots[-1]["enabled"] = False
        self.assertEqual("PASS", evidence.validate_phase("off", events, snapshots, baseline)["status"])
        events[-1]["count"] = "1"
        with self.assertRaises(ValueError):
            evidence.validate_phase("off", events, snapshots, baseline)
        events[-1]["count"] = "0"
        snapshots[-1]["schedule"] = None
        with self.assertRaises(ValueError):
            evidence.validate_phase("off", events, snapshots, baseline)

    def test_clock_probe_cannot_pass_without_actual_app_zone_and_clock_change(self):
        events, snapshots, baseline = self.fixture()
        events = events[:3]
        snapshots.append(copy.deepcopy(snapshots[-1]))
        snapshots[-1]["enabled"] = False
        with self.assertRaises(ValueError):
            evidence.validate_phase("dst", events, snapshots, baseline)
        events[2]["time"] = "1772949605"
        events[2]["next"] = "1772953200"
        events.append({"event": "system-clock", "time": "1772949605", "zone": "America/New_York"})
        evidence.validate_phase("dst", events, snapshots, baseline)
        events[-1]["zone"] = "UTC"
        with self.assertRaises(ValueError):
            evidence.validate_phase("dst", events, snapshots, baseline)

    def test_all_app_languages_require_native_pending_refresh(self):
        events, snapshots, baseline = self.fixture()
        events = events[:3]
        with self.assertRaises(ValueError):
            evidence.validate_phase("locales", events, snapshots, baseline)
        for code in ("ru", "en"):
            item = copy.deepcopy(snapshots[-1])
            item["schedule"]["content"]["languageCode"] = code
            snapshots.append(item)
            request = copy.deepcopy(events[2])
            request["language"] = code
            events.append(request)
        evidence.validate_phase("locales", events, snapshots, baseline)

    def test_failed_clock_phase_restores_clock_zone_and_network_time(self):
        runner_spec = importlib.util.spec_from_file_location("runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec)
        runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            runner = module.Runner(Path(directory) / "evidence", clock_probe=True)
            def command(*args, **kwargs):
                if args[-1] == "-gettimezone": return "Time Zone: UTC" if not changed else "Time Zone: Asia/Almaty"
                if args[-1] == "-getusingnetworktime": return "Network Time: On"
                if "-settimezone" in args: changed.append(True)
                return ""
            changed = []
            runner.run = command
            runner.phase = lambda *args: (_ for _ in ()).throw(RuntimeError("XCTest failed"))
            restored = []
            def restore(args, **kwargs):
                restored.append(args)
                return type("Result", (), {"returncode": 0, "stdout": "", "stderr": ""})()
            with patch.dict(module.os.environ, {"CI": "true"}), patch.object(module.subprocess, "run", restore):
                with self.assertRaisesRegex(RuntimeError, "XCTest failed"):
                    runner.clock_phases()
            self.assertEqual(3, len(restored))
            self.assertEqual(["sudo", "-n", "date", "-u"], restored[0][:4])
            self.assertEqual(["sudo", "-n", "systemsetup", "-settimezone", "UTC"], restored[1])
            self.assertEqual("on", restored[2][-1])

    def test_backward_clock_phase_uses_append_boundary_not_prior_future_timestamps(self):
        old = {"event": "system-clock", "time": "1791150000", "zone": "Asia/Almaty"}
        new = {"event": "system-clock", "time": "1772949605", "zone": "America/New_York"}
        boundary = (json.dumps(old) + "\n").encode()
        raw = boundary + (json.dumps(new) + "\n").encode()
        self.assertEqual([new], evidence.events_since(raw, boundary))
        self.assertEqual([], evidence.events_since(boundary, boundary))
        with self.assertRaises(ValueError):
            evidence.events_since((json.dumps(new) + "\n").encode(), boundary)
        with self.assertRaises(ValueError):
            evidence.events_since(raw[:-1], boundary)

    def test_travel_rejects_future_utc_time_that_is_wrong_in_actual_local_zone(self):
        events, snapshots, baseline = self.fixture()
        events = events[:3]
        # 2026-10-05 12:00Z, 17:00 in Almaty; next local02:30 is October05 21:30Z.
        now = 1791201600
        events[2]["time"] = str(now)
        events[2]["next"] = str(evidence.next_local_minute(now, 150, "UTC"))
        events.append({"event": "system-clock", "time": str(now), "zone": "Asia/Almaty"})
        snapshots.append(copy.deepcopy(snapshots[-1]))
        snapshots[-1]["enabled"] = False
        with self.assertRaisesRegex(ValueError, "next local 02:30"):
            evidence.validate_phase("travel", events, snapshots, baseline)
        events[2]["next"] = str(evidence.next_local_minute(now, 150, "Asia/Almaty"))
        evidence.validate_phase("travel", events, snapshots, baseline)

    def test_sampling_follows_actual_container_migration_and_never_injects_marker(self):
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec)
        runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            runner = module.Runner(root / "evidence")
            runner.udid = "owned-simulator"
            old, current = root / "old/Documents", root / "current/Documents"
            old.mkdir(parents=True); current.mkdir(parents=True)
            runner.documents = old
            (old / "oquturbo.preferences_pb").write_bytes(b"stale")
            (current / "oquturbo.preferences_pb").write_bytes(b"actual")
            (current / "reminder-diagnostics.jsonl").write_text('{"event":"picker-failed"}\n')
            paths = iter([str(current.parent), ""])
            def simctl(*args, **kwargs):
                self.assertEqual(("get_app_container", runner.udid, module.PACKAGE, "data"), args)
                return next(paths)
            runner.simctl = simctl
            snapshots = []
            with patch.object(module.evidence, "snapshot", side_effect=lambda data: {"raw": data.decode()}):
                runner.sample(runner.output, snapshots)
                runner.sample(runner.output, snapshots)
            self.assertEqual([{"raw": "actual"}], snapshots)
            self.assertEqual('{"event":"picker-failed"}\n', (runner.output / "app-native.jsonl").read_text())
            self.assertFalse((current / "reminder-diagnostics-enabled").exists())
            inventory = [json.loads(line) for line in (runner.output / "container-inventory.jsonl").read_text().splitlines()]
            self.assertEqual([True, False], [row["available"] for row in inventory])
            self.assertFalse(inventory[0]["probeEnabled"])

    def test_observer_timeout_discards_stale_container_and_resamples_actual_identity(self):
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec); runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            runner = module.Runner(root / "evidence"); runner.udid = "owned"
            stale, actual = root / "stale/Documents", root / "actual/Documents"
            stale.mkdir(parents=True); actual.mkdir(parents=True)
            runner.documents = stale
            (stale / "oquturbo.preferences_pb").write_bytes(b"do not read")
            (actual / "oquturbo.preferences_pb").write_bytes(b"current")
            failure = module.subprocess.TimeoutExpired("get_app_container", 10, output=str(stale.parent).encode())
            with patch.object(runner, "simctl", side_effect=[failure, str(actual.parent)]) as command, \
                    patch.object(module.evidence, "snapshot", side_effect=lambda data: {"raw": data.decode()}):
                snapshots = []
                self.assertFalse(runner.sample(runner.output, snapshots))
                self.assertIsNone(runner.documents)
                self.assertEqual([], snapshots)
                self.assertTrue(runner.sample(runner.output, snapshots))
                self.assertEqual([{"raw": "current"}], snapshots)
                self.assertEqual(2, command.call_count)
            inventory = [json.loads(row) for row in (runner.output / "container-inventory.jsonl").read_text().splitlines()]
            self.assertFalse(inventory[0]["available"])
            self.assertIn("TimeoutExpired", inventory[0]["error"])
            self.assertEqual("", inventory[0]["container"])
            self.assertTrue(inventory[1]["available"])

    def test_unavailable_observation_remains_bounded_and_never_accepts_cached_baseline(self):
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec); runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            runner = module.Runner(Path(directory) / "evidence")
            clock = [0.0]; runner.deadline = 100
            def sleep(seconds): clock[0] += seconds
            with patch.object(module.time, "monotonic", side_effect=lambda: clock[0]), \
                    patch.object(module.time, "sleep", side_effect=sleep), \
                    patch.object(runner, "simctl", side_effect=RuntimeError("simulator unavailable")) as command:
                with self.assertRaisesRegex(TimeoutError, "No current container"):
                    runner.await_sample(runner.output, [], 5)
                self.assertEqual(5, clock[0])
                self.assertEqual(3, command.call_count)
                self.assertIsNone(runner.documents)
                with self.assertRaisesRegex(TimeoutError, "deadline"):
                    runner.sample(runner.output, [], 5)
                self.assertEqual(3, command.call_count)

    def test_live_xctest_survives_sampler_timeout_but_still_requires_validated_native_evidence(self):
        from unittest.mock import Mock
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec); runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            runner = module.Runner(root / "evidence"); runner.udid = "owned"
            documents = root / "actual/Documents"; documents.mkdir(parents=True)
            (documents / "reminder-diagnostics.jsonl").write_text("")
            replies = iter([str(documents.parent), module.subprocess.TimeoutExpired("get_app_container", 10),
                            str(documents.parent), str(documents.parent)])
            def simctl(*args, **kwargs):
                if args[0] != "get_app_container": return ""
                reply = next(replies)
                if isinstance(reply, Exception): raise reply
                return reply
            runner.simctl = simctl
            runner.run = lambda *args, **kwargs: json.dumps({"passedTests": 1, "failedTests": 0, "skippedTests": 0})
            process = Mock(returncode=0); process.poll.side_effect = [None, None, 0]
            with patch.object(module.subprocess, "Popen", return_value=process) as start, \
                    patch.object(module.time, "sleep"), \
                    patch.object(module.evidence, "validate_phase", side_effect=ValueError("missing actual native delivery")) as validate:
                with self.assertRaisesRegex(ValueError, "missing actual native delivery"):
                    runner.phase("cold", "testColdDeliveryAndNativeOptIn")
                self.assertEqual(1, start.call_count)
                process.terminate.assert_not_called(); process.kill.assert_not_called()
                validate.assert_called_once()
            rows = [json.loads(row) for row in (runner.output / "cold/container-inventory.jsonl").read_text().splitlines()]
            self.assertEqual([True, False, True, True], [row["available"] for row in rows])
            self.assertEqual(0, json.loads((runner.output / "cold/phase.json").read_text())["code"])

    def test_command_timeout_retains_exit_unknown_and_partial_output_without_success(self):
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec); runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            runner = module.Runner(Path(directory) / "evidence")
            failure = module.subprocess.TimeoutExpired(["xcrun"], 10, output=b"partial", stderr=b"busy")
            with patch.object(module.subprocess, "run", side_effect=failure), self.assertRaises(module.subprocess.TimeoutExpired):
                runner.run("xcrun", timeout=10)
            record = json.loads((runner.output / "commands.jsonl").read_text())
            self.assertIsNone(record["code"])
            self.assertEqual("TimeoutExpired", record["error"])
            self.assertEqual("partial", record["stdout"])
            self.assertEqual("busy", record["stderr"])

    def test_failure_diagnostics_export_only_owned_process_log_predicate_and_recent_app_crash(self):
        import os
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec); runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); runner = module.Runner(root / "evidence"); runner.udid = "owned-simulator"
            reports = root / "reports"; reports.mkdir()
            owned = reports / "OquTurbo-new.ips"
            owned.write_text(json.dumps({"app_name": "OquTurbo"}) + "\n" + json.dumps({"bundleID": module.PACKAGE}))
            other = reports / "OquTurbo-unrelated.ips"
            other.write_text(json.dumps({"app_name": "AnotherApp"}) + "\n" + module.PACKAGE)
            old = reports / "OquTurbo-old.ips"; old.write_bytes(owned.read_bytes()); os.utime(old, (1, 1))
            calls = []
            def command(args, **kwargs):
                calls.append(args)
                self.assertEqual(["xcrun", "simctl", "spawn", "owned-simulator"], args[:4])
                self.assertLessEqual(kwargs["timeout"], 15)
                if args[4] == "launchctl":
                    text = "12 0 unrelated.private.service\n34 0 UIKitApplication:" + module.PACKAGE
                else:
                    self.assertEqual(["log", "show", "--last", "10m", "--style", "json", "--predicate"], args[4:-1])
                    self.assertEqual(f'process == "OquTurbo" OR eventMessage CONTAINS "{module.PACKAGE}"', args[-1])
                    text = "[]"
                return type("Result", (), {"returncode": 0, "stdout": text, "stderr": ""})()
            with patch.object(module.subprocess, "run", side_effect=command):
                runner.failure_diagnostics(runner.output, 2, reports)
            self.assertEqual(2, len(calls))
            self.assertNotIn("unrelated", (runner.output / "processes.txt").read_text())
            self.assertEqual(owned.read_bytes(), (runner.output / owned.name).read_bytes())
            self.assertFalse((runner.output / old.name).exists())
            self.assertFalse((runner.output / other.name).exists())
            self.assertEqual([owned.name], json.loads((runner.output / "failure-diagnostics.json").read_text())["appCrashes"])

    def test_diagnostics_timeout_respects_remaining_budget_and_records_unknown_not_success(self):
        runner_spec = importlib.util.spec_from_file_location("ios_runner", Path(__file__).with_name("qa-ios-reminders.py"))
        module = importlib.util.module_from_spec(runner_spec); runner_spec.loader.exec_module(module)
        with tempfile.TemporaryDirectory() as directory:
            runner = module.Runner(Path(directory) / "evidence"); runner.udid = "owned"
            clock = [0.0]; runner.deadline = 7
            def command(args, **kwargs):
                self.assertEqual(7, kwargs["timeout"])
                clock[0] = 7
                raise module.subprocess.TimeoutExpired(args, 7)
            with patch.object(module.time, "monotonic", side_effect=lambda: clock[0]), \
                    patch.object(module.subprocess, "run", side_effect=command) as run:
                runner.failure_diagnostics(runner.output, 0, Path(directory) / "no-reports")
            self.assertEqual(1, run.call_count)
            result = json.loads((runner.output / "failure-diagnostics.json").read_text())
            self.assertEqual([], result["appCrashes"])
            self.assertTrue(all(item["code"] is None for item in result["commands"]))
            self.assertIn("TimeoutExpired", result["commands"][0]["error"])
            self.assertIn("budget exhausted", result["commands"][1]["error"])

    def test_empty_skipped_or_unknown_xctest_schema_is_not_pass(self):
        evidence.validate_summary({"passedTests": 1, "failedTests": 0, "skippedTests": 0})
        for summary in ({}, {"passedTests": 0, "failedTests": 0, "skippedTests": 0},
                        {"passedTests": 1, "failedTests": 0, "skippedTests": 1}):
            with self.assertRaises(ValueError):
                evidence.validate_summary(summary)


if __name__ == "__main__":
    unittest.main()
