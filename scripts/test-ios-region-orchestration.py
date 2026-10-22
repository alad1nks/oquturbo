import copy
import importlib.util
import json
import plistlib
import shutil
import sqlite3
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("region", Path(__file__).with_name("ios-region-orchestration.py"))
region = importlib.util.module_from_spec(spec)
spec.loader.exec_module(region)


class RegionEvidenceTest(unittest.TestCase):
    def fixture(self):
        request = {"operation": "fresh", "udid": "device", "source": "source", "build": "build", "step": "change",
                   "created": 1791734700, "target": "Germany"}
        observation = dict(request, observed=1791734701, state="before-confirmation", confirmation="Keep English (US)")
        log = ("16:05:01.000 OquTurboUITests-Runner[123:456] Activity UUID added attachment with name 'region-before-confirmation-fresh'\n"
               '16:05:02.000 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> started activity <XCActivityRecord: 0xaaa> 2026-10-11 16:05:02 +0000: Tap "Keep English (US)" Button\n'
               '16:05:03.000 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> started activity <XCActivityRecord: 0xbbb> 2026-10-11 16:05:03 +0000: Synthesize event\n'
               '16:05:04.000 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> finished activity <XCActivityRecord: 0xbbb> 2026-10-11 16:05:03 +0000: Synthesize event (1s)\n'
               '16:05:05.000 xcodebuild[9:8] device got death notice for pid 123 (exit code: (null), signal: 9)')
        return request, observation, ["Test crashed with signal kill."], [], log, []

    def test_buffered_arrival_order_uses_actual_activity_timestamps(self):
        values = list(self.fixture()); values[4] = "\n".join(reversed(values[4].splitlines()))
        self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(*values))

    def test_dispatch_requires_same_runner_thread_context_and_activity(self):
        for old, new in (("0xbbb>", "0xfff>"), ("[123:456] <XCTContext: 0xabc> finished", "[123:999] <XCTContext: 0xabc> finished"),
                         ("<XCTContext: 0xabc> finished", "<XCTContext: 0xfff> finished"),
                         ("16:05:04.000", "16:05:06.000"), ("2026-10-11 16:05:02", "2026-10-12 16:05:02")):
            values = list(self.fixture()); values[4] = values[4].replace(old, new, 1)
            with self.subTest(old=old), self.assertRaises(ValueError): region.classify_interruption(*values)
        values = list(self.fixture()); values[4] += '\n16:05:03.500 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> started activity <XCActivityRecord: 0xccc> 2026-10-11 16:05:03 +0000: Tap "Other" Button'
        with self.assertRaises(ValueError): region.classify_interruption(*values)

    def relaunch_fixture(self):
        values = list(self.fixture()); bundle = region.RUNNER_BUNDLE
        issue = ('Failed to install or launch the test runner. (Underlying Error: Simulator device failed to launch ' + bundle +
                 '. The system shell (SpringBoard:42) probably crashed. (Underlying Error: The request to terminate "' + bundle +
                 '" failed. The system shell (SpringBoard:42) probably crashed.))')
        values[2].append(issue); values[3] = ['Simulator device failed to launch ' + bundle + '.']
        session = ('device\n16:05:07.500 xcodebuild[9:8] Test operation failure: Error Domain=XCTHTestOperationCoordinatorErrorDomain Code=4 "Failed to install or launch the test runner" '
                   'Error Domain=NSPOSIXErrorDomain Code=64 "Host is down" The system shell (SpringBoard:42) probably crashed. '
                   'NSLocalizedDescription=Simulator device failed to launch ' + bundle + '. DVTErrorCreationDateKey=2026-10-11 16:05:07 +0000')
        return values, [1791734706, 1791734708], [session]

    def test_exact_downstream_relaunch_failure_is_setup_interruption_only(self):
        values, times, sessions = self.relaunch_fixture()
        self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(*values, issue_times=times, sessions=sessions))

    def test_distinct_creation_and_logging_events_need_causal_order_not_proximity(self):
        for logged, secondary in (("16:05:08.002", 1791734709), ("16:05:30.000", 1791734731)):
            values, times, sessions = self.relaunch_fixture()
            times[1] = secondary
            sessions[0] = sessions[0].replace("16:05:07.500", logged)
            with self.subTest(logged=logged):
                self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(
                    *values, issue_times=times, sessions=sessions))

    def test_creation_second_can_overlap_causal_boundary_or_equal_log(self):
        values, times, sessions = self.relaunch_fixture()
        times[0] = 1791734706.5
        sessions[0] = sessions[0].replace("DVTErrorCreationDateKey=2026-10-11 16:05:07", "DVTErrorCreationDateKey=2026-10-11 16:05:06")
        self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(*values, issue_times=times, sessions=sessions))
        values, times, sessions = self.relaunch_fixture()
        sessions[0] = sessions[0].replace("16:05:07.500", "16:05:07.000")
        self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(*values, issue_times=times, sessions=sessions))

    def test_empty_endpoint_earlier_and_future_creation_are_rejected(self):
        for creation in ("16:05:06", "16:05:05", "16:05:08"):
            values, times, sessions = self.relaunch_fixture()
            times[0] = 1791734707  # C+1 == K for 06 is an empty half-open intersection.
            sessions[0] = sessions[0].replace("DVTErrorCreationDateKey=2026-10-11 16:05:07", "DVTErrorCreationDateKey=2026-10-11 " + creation)
            with self.subTest(creation=creation), self.assertRaises(ValueError):
                region.classify_interruption(*values, issue_times=times, sessions=sessions)

    def test_creation_metadata_requires_actual_full_utc_date(self):
        for value in ("2026-10-12 16:05:07 +0000", "2026-10-11 16:05:07 +0100", "missing", ""):
            values, times, sessions = self.relaunch_fixture()
            sessions[0] = sessions[0].replace("2026-10-11 16:05:07 +0000", value)
            with self.subTest(value=value), self.assertRaises(ValueError):
                region.classify_interruption(*values, issue_times=times, sessions=sessions)

    def test_relaunch_cannot_hide_extra_issue_build_error_or_wrong_order(self):
        for mutation in (lambda v: v[2].append("Assertion failed"), lambda v: v[3].append("Build failed"),
                         lambda v: v[2].reverse(), lambda v: v[2].__setitem__(1, v[2][1].replace(region.RUNNER_BUNDLE, "other.bundle"))):
            values, times, sessions = self.relaunch_fixture(); mutation(values)
            with self.assertRaises(ValueError): region.classify_interruption(*values, issue_times=times, sessions=sessions)
        for times in (None, [1791734704, 1791734708], [1791734708, 1791734706]):
            values, _, sessions = self.relaunch_fixture()
            with self.assertRaises(ValueError): region.classify_interruption(*values, issue_times=times, sessions=sessions)

    def test_relaunch_requires_same_device_host_pid_exact_error_and_date(self):
        for old, new in (("device", "other"), ("xcodebuild[9:", "xcodebuild[8:"), ("Code=64", "Code=60"),
                         ("SpringBoard:42", "SpringBoard:43"), ("2026-10-11", "2026-10-12"),
                         ("16:05:07.500", "16:05:04.500")):
            values, times, sessions = self.relaunch_fixture(); sessions[0] = sessions[0].replace(old, new)
            with self.subTest(old=old), self.assertRaises(ValueError): region.classify_interruption(*values, issue_times=times, sessions=sessions)
        values, times, sessions = self.relaunch_fixture()
        with self.assertRaises(ValueError): region.classify_interruption(*values, issue_times=times, sessions=sessions * 2)

    def test_restore_confirmation_is_exact_and_restore_only(self):
        values = list(self.fixture()); values[0].update(step="restore", target="United States")
        values[1].update(values[0]); values[1]["confirmation"] = "Change to United States"
        values[4] = values[4].replace("Keep English (US)", "Change to United States")
        self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(*values))
        values[0]["step"] = values[1]["step"] = "change"
        with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_crash_scope_requires_both_zoned_timestamps_before_operation(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "crash.ips"
            def classify(header="2026-10-11 15:48:54.00 +0000", capture="2026-10-11 15:48:53.6069 +0000", name="TypeToSiriWidgetExtension"):
                path.write_text(json.dumps({"timestamp": header}) + "\n" + json.dumps({"captureTime": capture, "procName": name}))
                return region.scope_crashes([path], 1791734700)
            records, current = classify()
            self.assertEqual("PREEXISTING_UNRELATED", records[0]["classification"]); self.assertEqual([], current)
            for kwargs in ({"header": "2026-10-11 16:05:00.00 +0000"}, {"capture": "2026-10-11 16:05:01.00 +0000"},
                           {"header": "2026-10-11 15:48:54"}, {"capture": "bad"}, {"name": "OquTurbo"}, {"name": "OquTurboUITests-Runner"}):
                records, current = classify(**kwargs)
                self.assertEqual([path], current)
            path.write_text("not a crash schema")
            self.assertEqual([path], region.scope_crashes([path], 1791734700)[1])

    def test_current_usage_tracking_crash_requires_owned_apple_cloudkit_identity(self):
        header = {"timestamp": "2026-10-11 16:05:00.00 +0000", "app_name": "UsageTrackingAgent", "is_first_party": 1}
        payload = {"captureTime": "2026-10-11 16:04:59.0000 +0000", "procName": "UsageTrackingAgent",
                   "codeSigningID": "com.apple.UsageTrackingAgent", "parentProc": "launchd_sim",
                   "responsibleProc": "SimulatorTrampoline", "coalitionName": "com.apple.CoreSimulator.SimDevice.owned",
                   "procPath": "/Volumes/VOLUME/*/UsageTracking.framework/UsageTrackingAgent",
                   "exception": {"type": "EXC_BREAKPOINT", "signal": "SIGTRAP"}, "faultingThread": 0,
                   "threads": [{"triggered": True, "frames": [{"imageIndex": 0}]}],
                   "usedImages": [{"CFBundleIdentifier": "com.apple.cloudkit.CloudKit",
                                   "path": "/Volumes/VOLUME/*/CloudKit.framework/CloudKit"}]}
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "usage.ips"
            def classify(h, p, udid="owned"):
                path.write_text(json.dumps(h) + "\n" + json.dumps(p))
                return region.scope_crashes([path], 1791734700, udid)
            records, current = classify(header, payload)
            self.assertEqual([], current)
            self.assertEqual("UNRELATED_USAGE_TRACKING_CLOUDKIT", records[0]["classification"])
            self.assertEqual(payload["codeSigningID"], records[0]["codeSigningID"])
            for field in header:
                changed = dict(header); changed.pop(field)
                with self.subTest(missing_header=field):
                    self.assertEqual([path], classify(changed, payload)[1])
            for field in payload:
                changed = dict(payload); changed.pop(field)
                with self.subTest(missing_payload=field):
                    self.assertEqual([path], classify(header, changed)[1])
            for changes in ({"procName": "OquTurbo"}, {"procName": "OquTurboUITests-Runner"},
                            {"procName": "SpringBoard"}, {"codeSigningID": "com.alad1nks.oquturbo"},
                            {"procPath": "/Applications/OquTurbo.app/OquTurbo"}, {"parentProc": "launchd"},
                            {"responsibleProc": "OquTurbo"}, {"coalitionName": "com.apple.CoreSimulator.SimDevice.other"},
                            {"exception": {"type": "EXC_BAD_ACCESS", "signal": "SIGSEGV"}},
                            {"faultingThread": -1}, {"faultingThread": 1}, {"faultingThread": False},
                            {"threads": [{"triggered": False, "frames": [{"imageIndex": 0}]}]},
                            {"threads": [{"triggered": True, "frames": [{"imageIndex": -1}]}]},
                            {"usedImages": [{"CFBundleIdentifier": "com.alad1nks.oquturbo"}]},
                            {"threads": []}, {"usedImages": []}):
                with self.subTest(changes=changes):
                    self.assertEqual([path], classify(header, dict(payload, **changes))[1])
            self.assertEqual([path], classify(dict(header, is_first_party=0), payload)[1])
            self.assertEqual([path], classify(header, payload, None)[1])

    def test_midnight_clock_is_scoped_to_actual_operation_window(self):
        created = 1791763199  # 2026-10-11 23:59:59 UTC.
        self.assertEqual(created + 2, region.scoped_clock("00:00:01.000", created))
        with self.assertRaises(ValueError): region.scoped_clock("23:59:58.000", created)

    def test_exact_transition_is_interrupted_never_pass(self):
        self.assertEqual("SETUP_TRANSITION_INTERRUPTED", region.classify_interruption(*self.fixture()))

    def test_marker_only_is_rejected(self):
        values = list(self.fixture()); values[4] = "region-before-confirmation-fresh"
        with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_stale_wrong_scope_and_operation_are_rejected(self):
        for key in ("operation", "udid", "source", "build", "target", "step"):
            values = list(self.fixture()); values[1] = dict(values[1], **{key: "wrong"})
            with self.subTest(key=key), self.assertRaises(ValueError): region.classify_interruption(*values)
        values = list(self.fixture()); values[1]["observed"] = 99
        with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_wrong_signal_unrelated_issue_and_missing_result_are_rejected(self):
        for issues in ([], ["Timed out"], ["Test crashed with signal kill.", "Assertion failed"], ["Signal abort"]):
            values = list(self.fixture()); values[2] = issues
            with self.subTest(issues=issues), self.assertRaises(ValueError): region.classify_interruption(*values)
        for index, value in ((3, ["Build failed"]), (5, ["OtherApp.ips"]), (4, "")):
            values = list(self.fixture()); values[index] = value
            with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_assertion_before_kill_and_other_process_kill_are_rejected(self):
        for change in (lambda t: "Assertion Failure\n" + t,
                       lambda t: t.replace("pid 123", "pid 456"),
                       lambda t: t.replace("signal: 9", "signal: 6"),
                       lambda t: t.replace("finished activity", "started activity")):
            values = list(self.fixture()); values[4] = change(values[4])
            with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_forward_region_choice_does_not_replace_confirmation(self):
        values = list(self.fixture()); values[1]["state"] = "before-region-choice"
        with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_baseline_requires_observed_time_equal_to_saved(self):
        request = self.fixture()[0]
        observation = dict(request, state="baseline", observed=request["created"] + 1, originalRegion="United States",
                           originalLanguage="en-US", wheels=["12", "05", "AM"], minutes=5)
        region.validate_baseline(request, observation, 5)
        for field, value in (("minutes", 725), ("wheels", ["12", "05", "PM"]),
                             ("originalRegion", "Germany"), ("originalLanguage", "en-GB")):
            with self.subTest(field=field), self.assertRaises(ValueError):
                region.validate_baseline(request, dict(observation, **{field: value}), 5)
        with self.assertRaises(ValueError): region.validate_baseline(request, observation, 6)


class LocaleOrchestrationTest(unittest.TestCase):
    def exercise(self, fail=None, malformed=None, advance=0, cutoff=1300, migration=None, verified_region=None, final_delay=0, export_failure=None):
        temporary = tempfile.TemporaryDirectory(); self.addCleanup(temporary.cleanup)
        root = Path(temporary.name); output = root / "output"; output.mkdir()
        derived = root / "DerivedData"; info = derived / "Build/Products/Debug-iphonesimulator/OquTurboUITests-Runner.app/Info.plist"
        info.parent.mkdir(parents=True); info.write_bytes(plistlib.dumps({"CFBundleIdentifier": "runner"}))
        (output / "app-identity.json").write_text(json.dumps({"source": "source", "sha256": "build"}))
        container = root / "runner"; container.mkdir(); docs = container / "Documents"; docs.mkdir()
        directory = output / "locales"; directory.mkdir(); (directory / "app-native.jsonl").write_bytes(b"")
        appdocs = root / "appdocs"; appdocs.mkdir(); (appdocs / "reminder-diagnostics.jsonl").write_bytes(b"")
        (appdocs / "oquturbo.preferences_pb").write_bytes(b"prefs")
        snapshot = {"enabled": True, "schedule": {"minutesOfDay": 65}, "values": {}}
        calls = []; commands = []; clock = [100]
        current = {"runner": container, "app": appdocs, "step": None, "number": 0}
        validation_count = [0]
        def validate_phase(*args):
            validation_count[0] += 1
            if validation_count[0] == 2: clock[0] += final_delay
            return {"status": "PASS"}
        evidence = SimpleNamespace(events_since=lambda *a: [], dismissal_observation=lambda *a: None,
            validate_summary=lambda s: None if s == {"success": True} else (_ for _ in ()).throw(ValueError("Bad summary")),
            validate_dismissal=lambda *a: calls.append("dismissal"),
            validate_phase=validate_phase)
        runner = SimpleNamespace(output=output, derived=derived, deadline=2000, udid="device", root=root,
            documents=appdocs, process=None, evidence=evidence, xcode_args=lambda: ["xcodebuild"],
            sample=lambda *a: False, await_sample=lambda *a: None)
        def simctl(*args, **kwargs):
            if migration == "deadline" and current["step"] == "change":
                clock[0] += 1
                return ""
            return str(current["runner"])
        runner.simctl = simctl
        def await_preferences(*args):
            if clock[0] >= cutoff: raise TimeoutError("Current preferences deadline")
            runner.documents = current["app"]
        runner.await_preferences = await_preferences
        def run(*args):
            commands.append(runner.deadline)
            if "diagnostics" in args:
                if current["step"] == export_failure: raise RuntimeError("Missing diagnostic export")
                target = Path(args[args.index("--output-path") + 1]); target.mkdir()
                (target / "Session-fixture.log").write_text("normally completed setup")
            if "summary" in args: return json.dumps({"success": True})
            return ""
        runner.run = run
        class Process:
            def __init__(self, args, **kwargs):
                active_docs = current["runner"] / "Documents"
                request = json.loads((active_docs / "region-operation.json").read_text()); name = request["step"]
                current["step"] = name
                calls.append(name); commands.append(runner.deadline); clock[0] += advance.get(name, 0) if isinstance(advance, dict) else advance
                states = {"initial": "baseline", "change": "transition-completed", "accept": "accepted-24-hour",
                          "restore": "transition-completed", "readback": "restored"}
                observation = dict(request, observed=request["created"], state=states[name])
                if name in {"accept", "readback"}:
                    observation.update(verifiedRegion=(verified_region or "Germany") if name == "accept" else "United States", verifiedLanguage="en-US")
                result = Path(args[args.index("-resultBundlePath") + 1]) if "-resultBundlePath" in args else directory / name / "runtime.xcresult"
                result.mkdir()
                with sqlite3.connect(result / "database.sqlite3") as db:
                    db.execute("CREATE TABLE TestIssues (compactDescription TEXT, timestamp REAL)")
                    db.execute("CREATE TABLE BuildIssues (message TEXT)")
                db.close()
                if name == "initial": observation.update(originalRegion="United States", originalLanguage="en-US", minutes=65, wheels=["1", "05", "AM"])
                if name == malformed: observation["operation"] = "stale"
                (active_docs / "region-observation.json").write_text(json.dumps(observation))
                if migration == "transient" and name in {"change", "restore"}:
                    runner.documents = None  # Last asynchronous poll saw a temporarily absent app.
                if migration == "app" and name == "accept":
                    replacement = root / "new-appdocs"
                    shutil.copytree(current["app"], replacement)
                    (replacement / "oquturbo.preferences_pb").write_bytes(b"changed-current-prefs")
                    current["app"] = replacement  # Cached runner.documents still points to old bytes.
                if migration == "runner":
                    current["number"] += 1
                    replacement = root / ("runner-" + str(current["number"]))
                    shutil.copytree(current["runner"], replacement)
                    # Old sandbox survives with a contradictory stale operation; never consume it.
                    (active_docs / "region-observation.json").write_text(json.dumps(dict(observation, operation="stale-old-sandbox")))
                    current["runner"] = replacement
                self.returncode = 65 if name in (fail or []) else 0
                self.unreaped = name == "change" and "unreaped" in (fail or [])
            def poll(self): return None if self.unreaped else self.returncode
            def kill(self): self.returncode = -9
            def wait(self, timeout):
                if self.unreaped: raise region.subprocess.TimeoutExpired("xcodebuild", timeout)
                return self.returncode
        binding = SimpleNamespace(state={"specification": "fixture"}, command=lambda *args: ["xcodebuild"])
        with patch.object(region.generated, "GeneratedTestRun", return_value=binding), \
                patch.object(region.subprocess, "Popen", Process), patch.object(region.time, "monotonic", lambda: clock[0]), \
                patch.object(region.time, "sleep", lambda duration: clock.__setitem__(0, clock[0] + duration)):
            error = None
            try: region.run_locale(runner, directory, [copy.deepcopy(snapshot)], {}, b"", cutoff)
            except Exception as caught: error = caught
        cleanup = json.loads((directory / "region-cleanup.json").read_text()) if (directory / "region-cleanup.json").exists() else {}
        outcomes = json.loads((directory / "region-substeps.json").read_text()) if (directory / "region-substeps.json").exists() else []
        self.assertEqual(2000, runner.deadline)
        self.assertTrue(all(value <= cutoff for value in commands))
        for path in directory.rglob("invocation-timing.json"):
            timing=json.loads(path.read_text())
            reserve = 60 if path.parent.name in {"change", "restore"} else 0
            self.assertEqual(timing["cutoffMonotonic"] - reserve - 1, timing["executionDeadlineMonotonic"])
        return error, calls, cleanup, outcomes

    def test_unavailable_last_poll_recovers_current_preferences_before_readback(self):
        error, calls, cleanup, _ = self.exercise(migration="transient")
        self.assertIsNone(error); self.assertTrue(cleanup["restored"])
        self.assertIn("accept", calls); self.assertIn("readback", calls)

    def test_migrated_current_app_bytes_cannot_be_replaced_by_cached_old_bytes(self):
        error, calls, cleanup, _ = self.exercise(migration="app")
        self.assertIsNotNone(error)
        self.assertIn("changed application preferences", cleanup["primaryError"])
        self.assertTrue(cleanup["restored"])

    def test_runner_migration_uses_current_request_and_current_observation(self):
        error, calls, cleanup, outcomes = self.exercise(migration="runner")
        self.assertIsNone(error); self.assertTrue(cleanup["restored"])
        self.assertEqual(5, len(outcomes))
        self.assertEqual(5, len({value["operation"] for value in outcomes}))

    def test_rediscovery_exhaustion_does_not_renew_budget_or_use_stale_runner(self):
        error, calls, cleanup, _ = self.exercise(migration="deadline")
        self.assertIsNotNone(error); self.assertNotIn("accept", calls)
        self.assertIn("deadline", cleanup["primaryError"])
        self.assertIn("deadline", cleanup["restorationError"])
        self.assertFalse(cleanup["restored"])

    def test_unreaped_child_prevents_next_invocation(self):
        error, calls, cleanup, _ = self.exercise(fail=["unreaped"])
        self.assertIsNotNone(error); self.assertNotIn("restore", calls)
        self.assertIn("not been reaped", cleanup["restorationError"])

    def test_missing_setup_export_blocks_acceptance_but_preserves_cleanup(self):
        error, calls, cleanup, _ = self.exercise(export_failure="change")
        self.assertIsNotNone(error); self.assertNotIn("accept",calls)
        self.assertTrue(cleanup["restored"]); self.assertIn("Missing diagnostic export",cleanup["primaryError"])

    def test_short_suite_refuses_mutation_before_reserve_is_spent(self):
        error, calls, cleanup, _ = self.exercise(cutoff=999)
        self.assertIsNotNone(error); self.assertNotIn("change", calls)
        self.assertFalse(cleanup["required"])

    def test_uncommitted_forward_state_fails_even_if_cleanup_succeeds(self):
        error, calls, cleanup, _ = self.exercise(verified_region="United States")
        self.assertIsNotNone(error); self.assertTrue(cleanup["restored"])
        self.assertIn("postcondition", cleanup["primaryError"])

    def test_final_validation_consumes_readback_stage_budget(self):
        error, calls, cleanup, _ = self.exercise(final_delay=241)
        self.assertIsNotNone(error); self.assertIn("deadline", str(error))
        self.assertIn("readback", calls)

    def test_all_normally_successful_substeps_preserve_single_phase(self):
        error, calls, cleanup, outcomes = self.exercise()
        self.assertIsNone(error)
        self.assertEqual(["initial", "change", "accept", "restore", "readback"], [c for c in calls if c != "dismissal"])
        self.assertTrue(cleanup["restored"])
        self.assertEqual(5, len(outcomes))

    def test_acceptance_crash_is_failure_even_after_successful_restore(self):
        error, calls, cleanup, _ = self.exercise(fail=["accept"])
        self.assertIsNotNone(error); self.assertIn("readback", calls)
        self.assertIn("Actual acceptance XCTest failed", cleanup["primaryError"])
        self.assertTrue(cleanup["restored"])

    def test_unsuccessful_or_stale_restoration_readback_blocks_phase(self):
        for kwargs in ({"fail": ["readback"]}, {"malformed": "readback"}):
            error, calls, cleanup, _ = self.exercise(**kwargs)
            self.assertIsNotNone(error); self.assertFalse(cleanup["restored"])
            self.assertIsNotNone(cleanup["restorationError"])

    def test_primary_and_restoration_failure_are_both_preserved(self):
        error, calls, cleanup, _ = self.exercise(fail=["accept", "readback"])
        self.assertIsNotNone(error)
        self.assertIsNotNone(cleanup["primaryError"]); self.assertIsNotNone(cleanup["restorationError"])
        self.assertEqual(1, calls.count("restore"))

    def test_no_mutation_when_initial_journey_or_baseline_fails(self):
        for kwargs in ({"fail": ["initial"]}, {"malformed": "initial"}):
            error, calls, cleanup, _ = self.exercise(**kwargs)
            self.assertIsNotNone(error); self.assertNotIn("change", calls); self.assertFalse(cleanup["required"])

    def test_exhausted_budget_is_never_renewed_for_readback(self):
        error, calls, cleanup, _ = self.exercise(advance={"restore": 241})
        self.assertIsNotNone(error); self.assertNotIn("readback", calls)
        self.assertFalse(cleanup["restored"]); self.assertIn("deadline", cleanup["restorationError"])

    def test_expired_before_initial_does_not_start_xctest(self):
        error, calls, cleanup, _ = self.exercise(cutoff=100)
        self.assertIsNotNone(error); self.assertNotIn("initial", calls)



class GeneratedTestRunTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(); self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name).resolve()
        self.products = self.root / "Products"; self.products.mkdir()
        self.package = region.generated.PACKAGE
        for relative, suffix, executable in (
                ("OquTurbo.app", "", "OquTurbo"),
                ("OquTurboUITests-Runner.app", ".UITests.xctrunner", "OquTurboUITests-Runner"),
                ("OquTurboUITests-Runner.app/PlugIns/OquTurboUITests.xctest", ".UITests", "OquTurboUITests")):
            folder = self.products / "Debug-iphonesimulator" / relative; folder.mkdir(parents=True, exist_ok=True)
            (folder / "Info.plist").write_bytes(plistlib.dumps({"CFBundleIdentifier": self.package + suffix, "CFBundleExecutable": executable}))
            (folder / executable).write_bytes(b"payload")
        (self.products / "Debug-iphonesimulator/OquTurbo.app/OquTurbo.debug.dylib").write_bytes(b"actual debug payload")
        self.target = {"BlueprintName": "OquTurboUITests", "BlueprintProviderName": "iosApp",
            "BlueprintProviderRelativePath": "iosApp.xcodeproj", "ProductModuleName": "OquTurboUITests",
            "IsUITestBundle": True, "IsXCTRunnerHostedTestBundle": True,
            "TestHostBundleIdentifier": self.package + ".UITests.xctrunner", "DiagnosticCollectionPolicy": 1,
            "TestLanguage": "", "TestRegion": "", "CommandLineArguments": [], "UITargetAppCommandLineArguments": [],
            "UITargetAppPath": "__TESTROOT__/Debug-iphonesimulator/OquTurbo.app",
            "TestHostPath": "__TESTROOT__/Debug-iphonesimulator/OquTurboUITests-Runner.app",
            "TestBundlePath": "__TESTHOST__/PlugIns/OquTurboUITests.xctest",
            "DependentProductPaths": ["__TESTROOT__/Debug-iphonesimulator/OquTurbo.app",
                "__TESTROOT__/Debug-iphonesimulator/OquTurboUITests-Runner.app",
                "__TESTROOT__/Debug-iphonesimulator/OquTurboUITests-Runner.app/PlugIns/OquTurboUITests.xctest"],
            "EnvironmentVariables": {"OS_ACTIVITY_DT_MODE": "YES", "PERFC_ENABLE_EXTENDED_DIAGNOSTIC_FORMAT": "1"},
            "TestingEnvironmentVariables": {}, "UITargetAppEnvironmentVariables": {}}
        self.path = self.products / "OquTurboRuntime.xctestrun"
        self.write()
        self.identity = {"source": "a" * 40, "sha256": region.generated.hashlib.sha256(b"payload").hexdigest()}

    def write(self):
        self.path.write_bytes(plistlib.dumps({"OquTurboUITests": self.target, "__xctestrun_metadata__": {
            "FormatVersion": 1, "ContainerInfo": {"ContainerName": "iosApp", "SchemeName": "OquTurboRuntime"}}}))

    def bind(self, cutoff=None):
        return region.generated.GeneratedTestRun(self.products, self.identity, "a" * 40, self.root / "evidence",
                                                cutoff if cutoff is not None else region.time.monotonic() + 30)

    def test_symlinked_temporary_ancestor_is_canonicalized_by_fixture(self):
        real = self.root / "real-temporary-root"; real.mkdir()
        alias = self.root / "temporary-root-alias"; alias.symlink_to(real, target_is_directory=True)
        nested = GeneratedTestRunTest("test_original_unmodified_spec_and_exact_command")
        self.addCleanup(nested.doCleanups)
        with patch.object(tempfile, "tempdir", str(alias)):
            nested.setUp()
        self.assertEqual(nested.root, nested.root.resolve())
        binding = nested.bind()
        self.assertEqual(binding.products, nested.products)
        # The production safety guard still rejects noncanonical/symlinked product inputs.
        aliased_products = alias / nested.root.name / "Products"
        with self.assertRaisesRegex(ValueError, "symlinked"):
            region.generated.GeneratedTestRun(aliased_products, nested.identity, "a" * 40,
                                             nested.root / "other-evidence", region.time.monotonic() + 30)

    def test_original_unmodified_spec_and_exact_command(self):
        before = self.path.read_bytes(); binding = self.bind()
        command = binding.command("device", "testSystem24HourAcceptance", self.root / "result")
        self.assertEqual(command, ["xcodebuild", "test-without-building", "-xctestrun", str(self.path),
            "-destination", "platform=iOS Simulator,id=device", "-parallel-testing-enabled", "NO",
            "-maximum-concurrent-test-simulator-destinations", "1", "CODE_SIGNING_ALLOWED=NO",
            "-only-testing:OquTurboUITests/LocalReminderRuntimeTests/testSystem24HourAcceptance", "-resultBundlePath", str(self.root / "result")])
        self.assertEqual(self.path.read_bytes(), before)
        self.assertEqual((self.root / "evidence/generated.xctestrun").read_bytes(), before)
        self.assertIn("Debug-iphonesimulator/OquTurbo.app/OquTurbo.debug.dylib", binding.state["payloads"])

    def test_missing_or_multiple_specs_fail_without_fallback(self):
        self.path.unlink()
        with self.assertRaises(ValueError): self.bind()
        self.write(); (self.products / "extra.xctestrun").write_bytes(self.path.read_bytes())
        with self.assertRaises(ValueError): self.bind()

    def test_changed_payload_and_spec_fail_before_invocation(self):
        binding = self.bind()
        payload = self.products / "Debug-iphonesimulator/OquTurbo.app/OquTurbo.debug.dylib"
        payload.write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "changed"): binding.command("device", "testSystem24HourAcceptance", self.root / "result")
        payload.write_bytes(b"actual debug payload")
        self.target["EnvironmentVariables"]["OS_ACTIVITY_DT_MODE"] = "NO"; self.write()
        with self.assertRaises(ValueError): binding.command("device", "testSystem24HourAcceptance", self.root / "result")

    def test_malformed_format_and_changed_valid_spec_fail(self):
        self.path.write_bytes(b"not a plist")
        with self.assertRaises(plistlib.InvalidFileException): self.bind()
        self.write()
        data = plistlib.loads(self.path.read_bytes()); data["__xctestrun_metadata__"]["FormatVersion"] = 2
        self.path.write_bytes(plistlib.dumps(data))
        with self.assertRaises(ValueError): self.bind()
        self.write(); binding = self.bind()
        self.target["EnvironmentVariables"]["EXTRA"] = "changed"; self.write()
        with self.assertRaisesRegex(ValueError, "changed"):
            binding.command("device", "testSystem24HourAcceptance", self.root / "result")

    def test_wrong_identity_or_source_fails(self):
        self.identity["source"] = "b" * 40
        with self.assertRaises(ValueError): self.bind()
        self.identity["source"] = "a" * 40; self.identity["sha256"] = "bad"
        with self.assertRaises(ValueError): self.bind()

    def test_wrong_paths_unknown_macros_and_overrides_fail(self):
        for key, value in [("TestHostPath", "__OTHER__/runner"), ("TestBundlePath", "__TESTHOST__/../escape"),
                           ("UITargetAppPath", "/tmp/other.app"), ("DiagnosticCollectionPolicy", 0),
                           ("TestLanguage", "de"), ("UITargetAppCommandLineArguments", ["-AppleLocale", "de_DE"])]:
            with self.subTest(key=key):
                original = self.target[key]; self.target[key] = value; self.write()
                with self.assertRaises(ValueError): self.bind()
                self.target[key] = original; self.write()

    def test_product_symlink_and_test_bundle_identity_fail(self):
        payload = self.products / "Debug-iphonesimulator/OquTurbo.app/OquTurbo.debug.dylib"
        payload.unlink(); payload.symlink_to(self.path)
        with self.assertRaises(ValueError): self.bind()
        payload.unlink(); payload.write_bytes(b"actual debug payload")
        info = self.products / "Debug-iphonesimulator/OquTurboUITests-Runner.app/PlugIns/OquTurboUITests.xctest/Info.plist"
        info.write_bytes(plistlib.dumps({"CFBundleIdentifier": "wrong", "CFBundleExecutable": "OquTurboUITests"}))
        with self.assertRaises(ValueError): self.bind()

    def test_same_deadline_is_enforced_on_recheck(self):
        binding = self.bind()
        with patch.object(region.generated.time, "monotonic", return_value=binding.cutoff):
            with self.assertRaises(TimeoutError): binding.command("device", "testSystem24HourAcceptance", self.root / "result")

    def test_reused_result_or_other_phase_rejected(self):
        binding = self.bind(); result = self.root / "result"; result.mkdir()
        with self.assertRaises(ValueError): binding.command("device", "testSystem24HourAcceptance", result)
        with self.assertRaises(ValueError): binding.command("device", "testColdDeliveryAndNativeOptIn", self.root / "fresh")



class StagedSetupTest(unittest.TestCase):
    def fixture(self):
        request = {"operation": "fresh", "udid": "device", "source": "source", "build": "build", "step": "change",
                   "created": 1791734700, "target": "Germany", "originalRegion": "United States", "originalLanguage": "en-US"}
        observation = dict(request, observed=1791734702, state="before-confirmation", confirmation="Keep English (US)",
            confirmationSheet="Would you like to change the iPhone language to English (UK)?", runnerPID=123, fromRegion="United States")
        text = ('-only-testing:OquTurboUITests/LocalReminderRuntimeTests/testSystemRegionTransition\n'
                '16:05:01.000 xcodebuild[9:8] device: Launched app with identifier: ' + region.RUNNER_BUNDLE + ' (pid = 123)\n'
                "Added attachment named 'region-before-confirmation-fresh'\n"
                '16:05:05.000 xcodebuild[9:8] device got death notice for pid 123 (exit code: (null), signal: 9)')
        return [request, observation, [region.KILL_ISSUE], [], text, [], [1791734706], [text]]

    def test_known_interrupt_is_only_pending_readback(self):
        self.assertEqual("SETUP_INTERRUPTED_PENDING_READBACK", region.provisional_setup(*self.fixture()))

    def test_missing_launch_marker_only_wrong_pid_or_before_marker_kill_rejects(self):
        for old, new in [("Launched app", "Other app"), ("pid = 123", "pid = 999"), ("16:05:05.000", "16:05:01.500"),
                         ("device: Launched", "other: Launched"), ("region-before-confirmation-fresh", "region-before-confirmation-stale")]:
            values = self.fixture(); values[4] = values[4].replace(old,new); values[7]=[values[4]]
            with self.subTest(old=old), self.assertRaises(ValueError): region.provisional_setup(*values)

    def test_current_unknown_extra_issue_or_unrelated_build_error_rejects(self):
        for index, value in [(5,[Path("current.ips")]), (2,[region.KILL_ISSUE,"extra"]), (3,["unrelated build failure"])]:
            values=self.fixture(); values[index]=value
            with self.subTest(index=index), self.assertRaises(ValueError): region.provisional_setup(*values)
        values=self.fixture(); values[1]["source"]="wrong"
        with self.assertRaises(ValueError): region.provisional_setup(*values)

    def test_partial_started_tap_tail_is_pending_not_completed_dispatch(self):
        values=self.fixture()
        started='\n16:05:03.000 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> started activity <XCActivityRecord: 0xaaa> 2026-10-11 16:05:03 +0000: Tap "Keep English (US)" Button'
        values[4]+=started
        self.assertEqual("SETUP_INTERRUPTED_PENDING_READBACK",region.provisional_setup(*values))
        values[4]+='\n16:05:03.500 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> started activity <XCActivityRecord: 0xbbb> 2026-10-11 16:05:03 +0000: Synthesize event'
        self.assertEqual("SETUP_INTERRUPTED_PENDING_READBACK",region.provisional_setup(*values))
        values[4]+='\n16:05:04.000 OquTurboUITests-Runner[123:999] <XCTContext: 0xabc> finished activity <XCActivityRecord: 0xbbb> 2026-10-11 16:05:03 +0000: Synthesize event (0.5s)'
        with self.assertRaises(ValueError):region.provisional_setup(*values)

    def test_reused_activity_address_does_not_attach_an_older_completion(self):
        values = self.fixture()
        prefix = ' OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> '
        def event(clock, action, address, title):
            return '\n16:05:' + clock + prefix + action + ' activity <XCActivityRecord: ' + address + '> 2026-10-11 16:05:00 +0000: ' + title
        values[4] += event('01.100', 'started', '0xbbb', 'Check for interrupting elements')
        values[4] += event('01.200', 'finished', '0xbbb', 'Check for interrupting elements (0.1s)')
        values[4] += event('01.300', 'started', '0xbbb', 'Synthesize event')
        values[4] += event('01.400', 'finished', '0xbbb', 'Synthesize event (0.1s)')
        values[4] += event('03.000', 'started', '0xaaa', 'Tap "Keep English (US)" Button')
        values[4] += event('03.500', 'started', '0xbbb', 'Synthesize event')
        completion = event('04.000', 'finished', '0xbbb', 'Synthesize event (0.5s)')
        values[4] += completion
        self.assertEqual("SETUP_INTERRUPTED_PENDING_READBACK", region.provisional_setup(*values))
        for replacement in (completion + completion, completion.replace('123:456', '123:999'),
                            completion.replace('16:05:04.000', '16:05:06.000'),
                            completion.replace('Synthesize event (0.5s)', 'Unrelated activity (0.5s)')):
            changed = copy.deepcopy(values); changed[4] = changed[4][:-len(completion)] + replacement
            with self.subTest(replacement=replacement), self.assertRaises(ValueError):
                region.provisional_setup(*changed)

    def test_available_contradictory_dispatch_rejects(self):
        values=self.fixture()
        values[4]+='\n16:05:03.000 OquTurboUITests-Runner[123:456] <XCTContext: 0xabc> started activity <XCActivityRecord: 0xaaa> 2026-10-11 16:05:03 +0000: Tap "Other" Button'
        with self.assertRaises(ValueError): region.provisional_setup(*values)

    def test_pending_operation_needs_exact_normally_successful_actual_readback(self):
        request={"operation":"verify", "verifiesOperation":"setup", "originalRegion":"United States", "created":1}
        observation=dict(request, observed=2,state="accepted-24-hour",verifiedRegion="Germany",verifiedLanguage="en-US")
        outcomes=[{"step":"change","operation":"setup","status":"SETUP_INTERRUPTED_PENDING_READBACK"},
                  {"step":"accept","operation":"verify","status":"SUCCESS"}]
        for change in [{"verifiesOperation":"stale"},{"verifiedRegion":"United States"},{"state":"partial"}]:
            with self.subTest(change=change), self.assertRaises(ValueError):
                region.settle_setup(copy.deepcopy(outcomes),"change","accept",request,dict(observation,**change))
        failed=copy.deepcopy(outcomes);failed[1]["status"]="FAILED_OR_INCOMPLETE"
        with self.assertRaises(ValueError):region.settle_setup(failed,"change","accept",request,observation)
        region.settle_setup(outcomes,"change","accept",request,observation)
        self.assertEqual("verify",outcomes[0]["verifiedBy"])
        self.assertEqual("SETUP_INTERRUPTED_PENDING_READBACK",outcomes[0]["status"])

    def test_owned_reap_is_bounded_by_remaining_stage_time(self):
        calls=[]
        process=SimpleNamespace(poll=lambda:None,kill=lambda:calls.append("kill"),wait=lambda timeout:calls.append(timeout))
        with patch.object(region.time,"monotonic",return_value=10.5):
            region.reap_owned(process,11)
        self.assertEqual(["kill",.5],calls)
        with patch.object(region.time,"monotonic",return_value=11):
            region.reap_owned(process,11)
        self.assertEqual(0,calls[-1])

    def test_region_export_reserve_does_not_extend_xctest_execution(self):
        now = [100.0]
        with patch.object(region.time, "monotonic", side_effect=lambda: now[0]):
            budget = region.StageBudget(5000)
            deadline = budget.enter("change")
            self.assertEqual(339, budget.execution_deadline("change"))
            now[0] = 338  # XCTest completes near its original 240s cap.
            self.assertGreaterEqual(deadline - now[0], 60)
            now[0] = 390  # Required diagnostic export still fits in this stage.
            budget.finish("change")
            now[0] = deadline
            with self.assertRaises(TimeoutError):
                budget.finish("change")

    def test_acceptance_reserves_full_restoration_and_readback_budgets(self):
        with patch.object(region.time, "monotonic", return_value=100):
            budget = region.StageBudget(800)
            self.assertEqual(260, budget.enter("accept"))
            self.assertEqual(259, budget.execution_deadline("accept"))
            with self.assertRaises(TimeoutError):
                budget.enter("change")

    def test_stage_caps_are_single_use_without_transfer_and_preserve_cleanup(self):
        now=[100.0]
        with patch.object(region.time,"monotonic",side_effect=lambda:now[0]):
            budget=region.StageBudget(5000)
            self.assertEqual(400,budget.enter("initial"))
            now[0]=200;budget.finish("initial")
            self.assertEqual(500,budget.enter("change"))
            self.assertEqual(439,budget.execution_deadline("change"))
            now[0]=300;budget.finish("change")
            self.assertEqual(480,budget.enter("accept"))
            with self.assertRaises(ValueError):budget.enter("accept")
            now[0]=481
            with self.assertRaises(TimeoutError):budget.finish("accept")
            self.assertEqual(781,budget.enter("restore"))
            self.assertEqual(720,budget.execution_deadline("restore"))
            now[0]=600;budget.finish("restore")
            self.assertEqual(840,budget.enter("readback"))
            self.assertEqual(1420,budget.total)


if __name__ == "__main__": unittest.main()
