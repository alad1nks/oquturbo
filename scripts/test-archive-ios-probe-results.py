#!/usr/bin/env python3
import importlib.util
import json
import sqlite3
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("archive", Path(__file__).with_name("archive-ios-probe-results.py"))
archive = importlib.util.module_from_spec(spec)
spec.loader.exec_module(archive)


class ArchiveTests(unittest.TestCase):
    def test_missing_database_is_not_empty_issues(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                archive.issues(Path(tmp) / "missing")

    def test_missing_schema_is_not_empty_issues(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "database.sqlite3"
            sqlite3.connect(path).close()
            with self.assertRaises(sqlite3.OperationalError):
                archive.issues(path)

    def test_complete_issue_inventory_retains_extra_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "database.sqlite3"
            with sqlite3.connect(path) as db:
                db.execute("CREATE TABLE TestIssues (compactDescription TEXT,timestamp REAL)")
                db.execute("CREATE TABLE BuildIssues (message TEXT)")
                db.executemany("INSERT INTO TestIssues VALUES (?,?)", [("kill", 2), ("assertion", 3)])
                db.execute("INSERT INTO BuildIssues VALUES ('additional failure')")
            self.assertEqual(archive.issues(path), ([("kill", 2.), ("assertion", 3.)], [("additional failure",)]))

    def test_wrong_run_rejected_before_loading_input(self):
        with self.assertRaisesRegex(ValueError, "immutable"):
            archive.identities(Path("/missing"), {"id": archive.RUN, "head_sha": "wrong", "status": "completed"})

    def test_manifest_deadline_and_symlink_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "payload").write_text("native bytes")
            with self.assertRaises(TimeoutError):
                archive.manifest(root, time.monotonic() - 1)
            (root / "link").symlink_to(root / "payload")
            with self.assertRaisesRegex(ValueError, "Symlink"):
                archive.manifest(root, time.monotonic() + 10)

    def test_command_error_is_preserved(self):
        import subprocess
        with tempfile.TemporaryDirectory() as tmp:
            harness = archive.Archive(Path(tmp), Path(tmp) / "out")
            with patch.object(archive.subprocess, "run", return_value=subprocess.CompletedProcess([], 1, "partial", "failure")):
                with self.assertRaises(RuntimeError):
                    harness.command(["xcrun", "xcresulttool"], "failed")
            record = json.loads((harness.output / "failed.json").read_text())
            self.assertEqual((record["code"], record["stdout"], record["stderr"]), (1, "partial", "failure"))

    def test_timeout_keeps_partial_output_and_does_not_restart(self):
        import subprocess
        with tempfile.TemporaryDirectory() as tmp:
            harness = archive.Archive(Path(tmp), Path(tmp) / "out")
            with patch.object(archive.subprocess, "run", side_effect=subprocess.TimeoutExpired([], 1, b"partial", b"error")) as call:
                with self.assertRaises(subprocess.TimeoutExpired):
                    harness.command(["xcrun", "xcresulttool"], "timeout")
            self.assertEqual(call.call_count, 1)
            self.assertTrue(json.loads((harness.output / "timeout.json").read_text())["timeout"])

    def test_failures_never_produce_acceptance_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            harness = archive.Archive(Path(tmp), Path(tmp) / "out")
            result = harness.run({})
            self.assertEqual(result["status"], "REJECTED")
            self.assertEqual(result["acceptance"], "NOT_RUN")
            self.assertEqual(result["sourceCompleteness"], "UNPROVEN")

    def test_exact_dispatch_rejection_is_retained_with_all_issues(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "request.json").write_text(json.dumps({"operation": "one", "created": 1}))
            (root / "observation.json").write_text(json.dumps({"state": "before-confirmation"}))
            (root / "Session-a.log").write_text("region-before-confirmation-one")
            with patch.object(archive, "issues", return_value=([("signal", 2)], [("launch",)])), \
                 patch.object(archive.region, "scope_crashes", return_value=([], [])), \
                 patch.object(archive.region, "classify_interruption", side_effect=ValueError("missing dispatch")) as classify:
                row = archive.replay(root, root, root, [])
            self.assertEqual(row["classifier"], "REJECTED")
            self.assertIn("missing dispatch", row["classifierError"])
            self.assertEqual(row["buildIssues"], [("launch",)])
            self.assertEqual(classify.call_args.kwargs["sessions"], ["region-before-confirmation-one"])

    def test_both_results_receive_identical_supported_extraction(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for name in ("change", "restore"):
                folder = root / name
                (folder / "runtime.xcresult").mkdir(parents=True)
                (folder / "runtime.xcresult/data").write_text("native bytes")
                for leaf in ("request.json", "observation.json", "operation.json"):
                    (folder / leaf).write_text("{}")
            (root / "candidate-recheck").mkdir()
            harness = archive.Archive(root, root / "out")
            commands = []
            def command(args, label):
                commands.append((label, args[:4]))
                if "--output-path" in args:
                    output = Path(args[args.index("--output-path") + 1])
                    output.mkdir()
                    (output / "evidence").write_text("export")
            with patch.object(harness, "command", side_effect=command), \
                 patch.object(archive, "replay", return_value={"classifier": "REJECTED"}):
                harness.extract("change")
                harness.extract("restore")
            self.assertEqual([args for _, args in commands[:3]], [args for _, args in commands[3:]])
            self.assertTrue(all(row["originalUnchanged"] for row in harness.report["steps"].values()))

    def test_workflow_does_not_run_original_probe(self):
        workflow = (Path(__file__).parent.parent / ".github/workflows/archive-ios-probe-results.yml").read_text()
        self.assertNotIn("qa-ios-diagnostic-capability.py", workflow)
        self.assertNotIn("simctl", workflow)
        self.assertNotIn("gradlew", workflow)
        self.assertIn("run-id: 38169491833", workflow)


if __name__ == "__main__":
    unittest.main()
