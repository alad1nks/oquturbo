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
                   "created": 100, "target": "Germany"}
        observation = dict(request, observed=101, state="before-confirmation", confirmation="Keep English (US)")
        log = ('OquTurboUITests-Runner[123:456]\nregion-before-confirmation-fresh\n'
               'didFinishActivity: Tap "Keep English (US)" Button\n'
               'got death notice for pid 123 (exit code: (null), signal: 9)')
        return request, observation, ["Test crashed with signal kill."], [], log, []

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
                       lambda t: t.replace("didFinishActivity", "willStartActivity")):
            values = list(self.fixture()); values[4] = change(values[4])
            with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_forward_region_choice_does_not_replace_confirmation(self):
        values = list(self.fixture()); values[1]["state"] = "before-region-choice"
        with self.assertRaises(ValueError): region.classify_interruption(*values)

    def test_baseline_requires_observed_time_equal_to_saved(self):
        request = self.fixture()[0]
        observation = dict(request, state="baseline", observed=101, originalRegion="United States",
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
        with patch.object(region.subprocess, "Popen", Process), patch.object(region.time, "monotonic", lambda: clock[0]), \
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


if __name__ == "__main__": unittest.main()
