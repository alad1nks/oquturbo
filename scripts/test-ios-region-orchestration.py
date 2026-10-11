import copy
import importlib.util
import json
import plistlib
import shutil
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
    def exercise(self, fail=None, malformed=None, advance=0, cutoff=200, migration=None):
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
        evidence = SimpleNamespace(events_since=lambda *a: [], dismissal_observation=lambda *a: None,
            validate_summary=lambda s: None if s == {"success": True} else (_ for _ in ()).throw(ValueError("Bad summary")),
            validate_dismissal=lambda *a: calls.append("dismissal"),
            validate_phase=lambda *a: {"status": "PASS"})
        runner = SimpleNamespace(output=output, derived=derived, deadline=1000, udid="device", root=root,
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
            if "summary" in args: return json.dumps({"success": True})
            return ""
        runner.run = run
        class Process:
            def __init__(self, args, **kwargs):
                active_docs = current["runner"] / "Documents"
                request = json.loads((active_docs / "region-operation.json").read_text()); name = request["step"]
                current["step"] = name
                calls.append(name); commands.append(runner.deadline); clock[0] += advance
                states = {"initial": "baseline", "change": "transition-completed", "accept": "accepted-24-hour",
                          "restore": "transition-completed", "readback": "restored"}
                observation = dict(request, observed=request["created"], state=states[name])
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
            def poll(self): return self.returncode
            def kill(self): self.returncode = -9
            def wait(self, timeout): return self.returncode
        binding = SimpleNamespace(state={"specification": "fixture"}, command=lambda *args: ["xcodebuild"])
        with patch.object(region.generated, "GeneratedTestRun", return_value=binding), \
                patch.object(region.subprocess, "Popen", Process), patch.object(region.time, "monotonic", lambda: clock[0]), \
                patch.object(region.time, "sleep", lambda duration: clock.__setitem__(0, clock[0] + duration)):
            error = None
            try: region.run_locale(runner, directory, [copy.deepcopy(snapshot)], {}, b"", cutoff)
            except Exception as caught: error = caught
        cleanup = json.loads((directory / "region-cleanup.json").read_text()) if (directory / "region-cleanup.json").exists() else {}
        outcomes = json.loads((directory / "region-substeps.json").read_text())
        self.assertEqual(1000, runner.deadline)
        self.assertTrue(all(value == cutoff for value in commands))
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
        error, calls, cleanup, _ = self.exercise(migration="deadline", cutoff=105)
        self.assertIsNotNone(error); self.assertNotIn("accept", calls)
        self.assertIn("deadline", cleanup["primaryError"])
        self.assertIn("deadline", cleanup["restorationError"])
        self.assertFalse(cleanup["restored"])

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
        error, calls, cleanup, _ = self.exercise(advance=1, cutoff=104)
        self.assertIsNotNone(error); self.assertNotIn("readback", calls)
        self.assertFalse(cleanup["restored"]); self.assertIn("deadline", cleanup["restorationError"])

    def test_expired_before_initial_does_not_start_xctest(self):
        error, calls, cleanup, _ = self.exercise(cutoff=100)
        self.assertIsNotNone(error); self.assertNotIn("initial", calls)



class GeneratedTestRunTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(); self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
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


if __name__ == "__main__": unittest.main()
