#!/usr/bin/env python3
"""Offline native-result materialization; never builds, boots or runs a test."""
import argparse
import hashlib
import importlib.util
import json
import shutil
import sqlite3
import subprocess
import time
from pathlib import Path

RUN = 38169491833
SOURCE = "2648f9bfc7f7cfa3f209dcbe256e289dce5336b5"
UDID = "9E29BF7A-9CCA-4F7B-B330-20851AA434CC"
BUILD = "4bd85c4aba11938a0e6116949c731c42e6d74660e30943d4d6ee92a195309689"
_spec = importlib.util.spec_from_file_location("region", Path(__file__).with_name("ios-region-orchestration.py"))
region = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(region)


def save(path, data):
    path.write_text(json.dumps(data, indent=2) + "\n")


def budget(deadline):
    if time.monotonic() >= deadline:
        raise TimeoutError("Archive processing deadline exhausted")


def manifest(root, deadline):
    rows = {}
    for path in sorted(root.rglob("*")):
        budget(deadline)
        if path.is_symlink():
            raise ValueError("Symlink in archived input")
        if path.is_file():
            digest = hashlib.sha256()
            with path.open("rb") as stream:
                while chunk := stream.read(1024 * 1024):
                    budget(deadline)
                    digest.update(chunk)
            rows[str(path.relative_to(root))] = digest.hexdigest()
    if not rows:
        raise ValueError("Empty archived input")
    return rows


def identities(root, metadata):
    if metadata.get("id") != RUN or metadata.get("head_sha") != SOURCE or metadata.get("status") != "completed":
        raise ValueError("Wrong immutable source run")
    identity = json.loads((root / "app-identity.json").read_text())
    if identity.get("source") != SOURCE or identity.get("sha256") != BUILD:
        raise ValueError("Wrong archived product identity")
    for name in ("change", "restore"):
        request = json.loads((root / name / "request.json").read_text())
        observation = json.loads((root / name / "observation.json").read_text())
        if any(request.get(k) != v for k, v in {"source": SOURCE, "build": BUILD, "udid": UDID,
                                                "step": name, "purpose": "diagnostic-capability-only"}.items()):
            raise ValueError("Wrong archived operation identity")
        region.validate_observation(request, observation, {"before-confirmation", "before-region-choice"})


def issues(path):
    if not path.is_file():
        raise ValueError("Supported extraction did not materialize database")
    with sqlite3.connect("file:" + str(path) + "?mode=ro", uri=True) as db:
        # Missing schema is an error, never an empty issue list.
        return (list(db.execute("SELECT compactDescription,timestamp FROM TestIssues ORDER BY timestamp")),
                list(db.execute("SELECT message FROM BuildIssues")))


def replay(root, result, diagnostics, crash_paths):
    request = json.loads((root / "request.json").read_text())
    observation = json.loads((root / "observation.json").read_text())
    test_issues, build_issues = issues(result / "database.sqlite3")
    paths = sorted(diagnostics.rglob("Session-*.log"))
    texts = [p.read_text() for p in paths]
    marker = "region-" + observation["state"] + "-" + request["operation"]
    matching = [text for text in texts if marker in text]
    data = {"testIssues": test_issues, "buildIssues": build_issues,
            "sessions": [str(p) for p in paths], "classifier": "REJECTED"}
    scoped, current = region.scope_crashes(crash_paths, request["created"])
    data["crashScope"] = scoped
    try:
        if len(matching) != 1:
            raise ValueError("Missing unique exact-operation Session")
        data["classifier"] = region.classify_interruption(
            request, observation, [row[0] for row in test_issues], [row[0] for row in build_issues],
            matching[0], current, issue_times=[row[1] + 978307200 for row in test_issues], sessions=texts)
    except Exception as error:
        data["classifierError"] = repr(error)
    return data


class Archive:
    def __init__(self, source, output):
        self.source, self.output = source, output
        self.deadline = time.monotonic() + 720
        output.mkdir(parents=True, exist_ok=False)
        self.report = {"acceptance": "NOT_RUN", "status": "REJECTED", "sourceRun": RUN,
                       "sourceCompleteness": "UNPROVEN", "currentDetection": "UNPROVEN", "steps": {}}

    def command(self, arguments, label):
        budget(self.deadline)
        record = {"args": arguments, "started": time.time()}
        try:
            value = subprocess.run(arguments, capture_output=True, text=True,
                                   timeout=max(.001, self.deadline - time.monotonic()))
            record.update(code=value.returncode, stdout=value.stdout, stderr=value.stderr)
            if value.returncode:
                raise RuntimeError("Archive extraction command failed: " + label)
            return value.stdout
        except subprocess.TimeoutExpired as error:
            record.update(timeout=True, stdout=(error.stdout or b"").decode(errors="replace"),
                          stderr=(error.stderr or b"").decode(errors="replace"))
            raise
        finally:
            record["finished"] = time.time()
            save(self.output / (label + ".json"), record)

    def extract(self, name):
        folder = self.output / name
        folder.mkdir()
        original = self.source / name / "runtime.xcresult"
        before = manifest(original, self.deadline)
        save(folder / "original-sha256.json", before)
        target = folder / "runtime.xcresult"
        def copy_bounded(source, destination):
            with open(source, "rb") as reader, open(destination, "wb") as writer:
                while chunk := reader.read(1024 * 1024):
                    budget(self.deadline)
                    writer.write(chunk)
            return destination
        shutil.copytree(original, target, copy_function=copy_bounded)
        if manifest(target, self.deadline) != before:
            raise ValueError("Result copy differs from original")
        for suffix in ("request.json", "observation.json", "operation.json"):
            shutil.copy2(self.source / name / suffix, folder / suffix)
        row = self.report["steps"][name] = {"errors": []}
        operations = [(["get", "test-results", "summary", "--path", str(target)], "summary")]
        operations += [(["export", kind, "--path", str(target), "--output-path", str(folder / kind)], kind)
                       for kind in ("attachments", "diagnostics")]
        for args, label in operations:
            try:
                self.command(["xcrun", "xcresulttool", *args], name + "-" + label)
            except Exception as error:
                row["errors"].append(repr(error))
        try:
            # Include every exported report. Candidate inventory is additional evidence, not completeness.
            crash_paths = list((folder / "diagnostics").rglob("*.ips")) + list((folder / "diagnostics").rglob("*.crash"))
            if name == "change":
                inventory = self.source / "candidate-recheck"
                if not inventory.is_dir():
                    raise ValueError("Missing archived candidate crash inventory")
                crash_paths += list(inventory.rglob("*.ips")) + list(inventory.rglob("*.crash"))
            row.update(replay(folder, target, folder / "diagnostics", crash_paths))
        except Exception as error:
            row["errors"].append(repr(error))
        row["originalUnchanged"] = manifest(original, self.deadline) == before
        save(folder / "materialized-sha256.json", manifest(target, self.deadline))
        save(folder / "export-sha256.json", manifest(folder / "diagnostics", self.deadline))

    def run(self, metadata):
        try:
            identities(self.source, metadata)
            save(self.output / "source-run.json", metadata)
            evidence = self.output / "input-evidence"
            evidence.mkdir()
            for name in ("app-identity.json", "probe-report.json", "candidate-sources.json"):
                shutil.copy2(self.source / name, evidence / name)
            inventory = self.source / "candidate-recheck"
            save(evidence / "candidate-input-sha256.json", manifest(inventory, self.deadline))
            version = self.command(["xcodebuild", "-version"], "toolchain")
            if version.strip() != "Xcode 26.3\nBuild version 17C529":
                raise ValueError("Source Xcode toolchain mismatch")
            for name in ("change", "restore"):
                try:
                    self.extract(name)
                except Exception as error:
                    self.report["steps"].setdefault(name, {})["error"] = repr(error)
            rows = self.report["steps"]
            if all(rows.get(n, {}).get("classifier") == "SETUP_TRANSITION_INTERRUPTED" and
                   rows[n].get("originalUnchanged") and not rows[n].get("errors") and not rows[n].get("error")
                   for n in ("change", "restore")):
                self.report["status"] = "INPUTS_RECOVERED_NOT_EQUIVALENCE"
        except Exception as error:
            self.report["error"] = repr(error)
        finally:
            save(self.output / "archive-report.json", self.report)
        return self.report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--run-metadata", type=Path, required=True)
    args = parser.parse_args()
    report = Archive(args.input, args.output).run(json.loads(args.run_metadata.read_text()))
    print(json.dumps(report))
    # Diagnostic completion is never acceptance PASS; rejection is a failing job.
    raise SystemExit(0 if report["status"] == "INPUTS_RECOVERED_NOT_EQUIVALENCE" else 2)


if __name__ == "__main__":
    main()
