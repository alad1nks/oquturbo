#!/usr/bin/env python3
"""Host-only negative evidence tests; not proof of native source completeness."""
import importlib.util
import json
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location("capability", Path(__file__).with_name("qa-ios-diagnostic-capability.py"))
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


class InventoryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.source = self.root / "source"
        self.source.mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def collect(self):
        return probe.inventory(self.source, self.root / "copied", time.monotonic() + 10)

    def test_deadline_starts_at_build_completion_and_cannot_be_renewed(self):
        runner = probe.ProbeRunner.__new__(probe.ProbeRunner)
        runner.deadline = 5000
        with patch.object(probe.native.Runner, "run", return_value=""), patch.object(probe.time, "monotonic", return_value=100):
            runner.run("xcodebuild", "build-for-testing")
        self.assertEqual(820, runner.deadline)
        with patch.object(probe.native.Runner, "run", return_value=""), patch.object(probe.time, "monotonic", return_value=200):
            runner.run("xcodebuild", "build-for-testing")
        self.assertEqual(820, runner.deadline)

    def test_timeout_kills_and_reaps_only_owned_process_preserving_partial_result(self):
        instance = probe.Probe.__new__(probe.Probe)
        instance.root = self.root
        instance.identity = {"source": "a" * 40, "build": "b" * 64}
        instance.report = {"operations": []}
        instance.runner = Mock(deadline=time.monotonic() + 10, udid="owned", root=self.root)
        instance.runner.xcode_args.return_value = ["xcodebuild"]
        instance.runner_documents = Mock(return_value=self.source)
        process = Mock()
        process.poll.return_value = None
        process.wait.side_effect = [probe.subprocess.TimeoutExpired("xcodebuild", 10), -9]
        with patch.object(probe.subprocess, "Popen", return_value=process):
            with self.assertRaises(probe.subprocess.TimeoutExpired):
                instance.step("change", "testSystemRegionTransition", flag=True)
        process.kill.assert_called_once()
        self.assertEqual(2, process.wait.call_count)
        self.assertIsNone(instance.runner.process)
        self.assertTrue(json.loads((self.root / "change/operation.json").read_text())["reaped"])

    def test_empty_readable_inventory_is_never_equivalence(self):
        data = self.collect()
        self.assertTrue(data["readable"])
        self.assertEqual("UNPROVEN", data["authority"])
        result = probe.compare([data], data, time.time())
        self.assertEqual("INCONCLUSIVE", result["status"])
        self.assertFalse(result["currentDetectionDemonstrated"])

    def test_missing_directory_is_error(self):
        self.source.rmdir()
        data = self.collect()
        self.assertFalse(data["readable"])
        self.assertTrue(data["errors"])

    def test_enumeration_permission_error_is_not_empty_success(self):
        def denied(*args, **kwargs):
            kwargs["onerror"](PermissionError("denied"))
        with patch.object(probe.os, "walk", denied):
            data = self.collect()
        self.assertFalse(data["readable"])
        self.assertIn("denied", data["errors"][0])

    def test_deadline_interrupts_inventory(self):
        (self.source / "report.ips").write_text("partial")
        data = probe.inventory(self.source, self.root / "copied", time.monotonic() - 1)
        self.assertFalse(data["readable"])
        self.assertTrue(data["errors"])

    def test_symlink_source_is_rejected(self):
        link = self.root / "alias"
        link.symlink_to(self.source, target_is_directory=True)
        data = probe.inventory(link, self.root / "copied", time.monotonic() + 10)
        self.assertFalse(data["readable"])

    def test_malformed_report_preserved_as_unknown_and_missing_reference_blocks(self):
        raw = b"partial report"
        (self.source / "unknown.ips").write_bytes(raw)
        data = self.collect()
        self.assertEqual("UNKNOWN", data["reports"][0]["timeStatus"])
        self.assertEqual(raw, next((self.root / "copied").glob("*.ips")).read_bytes())
        result = probe.compare([], data, time.time())
        self.assertEqual(1, len(result["missingReferenceReports"]))
        self.assertFalse(result["currentDetectionDemonstrated"])

    def test_current_usage_tracking_shape_is_detected_even_if_hashes_match(self):
        header = {"timestamp": "2026-10-11 17:17:20.00 +0000"}
        body = {"captureTime": "2026-10-11 17:17:09.6803 +0000", "procName": "UsageTrackingAgent"}
        (self.source / "current.ips").write_text(json.dumps(header) + "\n" + json.dumps(body))
        data = self.collect()
        result = probe.compare([data], data, data["reports"][0]["captured"] - 1)
        self.assertTrue(result["currentDetectionDemonstrated"])
        self.assertEqual("INCONCLUSIVE", result["status"])
        self.assertEqual("UNPROVEN", result["sourceCompleteness"])

    def test_historical_reports_do_not_prove_current_detection(self):
        report = {"sha256": "abc", "timeStatus": "PARSED", "recorded": 1, "captured": 1}
        data = {"reports": [report]}
        result = probe.compare([data], data, 2)
        self.assertFalse(result["currentDetectionDemonstrated"])
        self.assertEqual("INCONCLUSIVE", result["status"])


if __name__ == "__main__":
    unittest.main()
