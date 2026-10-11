#!/usr/bin/env python3
"""One disposable Settings transition: diagnostic capability investigation, never acceptance."""
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
import plistlib
import sqlite3
import subprocess
import time
import uuid
from pathlib import Path

_spec = importlib.util.spec_from_file_location("native_runner", Path(__file__).with_name("qa-ios-reminders.py"))
native = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(native)


def save(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def inventory(source, destination, deadline):
    """Readable candidate inventory is NOT proof of authoritative source completeness."""
    result = {"source": str(source), "started": time.time(), "readable": False,
              "authority": "UNPROVEN", "reports": [], "errors": []}
    destination.mkdir(parents=True, exist_ok=True)
    try:
        if not source.is_dir() or source.is_symlink():
            raise ValueError("Missing directory or symlink source")
        def failed(error):
            raise error
        count = 0
        for base, directories, files in os.walk(source, onerror=failed, followlinks=False):
            if any((Path(base) / name).is_symlink() for name in directories):
                raise ValueError("Symlink directory prevents complete enumeration")
            for name in files:
                if time.monotonic() >= deadline:
                    raise TimeoutError("Single probe deadline exhausted during inventory")
                count += 1
                if count > 10000:
                    raise ValueError("Inventory truncated at explicit file bound")
                path = Path(base) / name
                if path.suffix not in {".ips", ".crash"}:
                    continue
                if path.is_symlink() or path.stat().st_size > 32 * 1024 * 1024:
                    raise ValueError("Unsupported report link/size")
                raw = path.read_bytes()
                digest = hashlib.sha256(raw).hexdigest()
                (destination / (digest + path.suffix)).write_bytes(raw)
                record = {"source": str(path), "sha256": digest, "bytes": len(raw), "timeStatus": "UNKNOWN"}
                try:
                    parts = raw.decode().splitlines()
                    header, body = json.loads(parts[0]), json.loads("\n".join(parts[1:]))
                    recorded = dt.datetime.strptime(header["timestamp"], "%Y-%m-%d %H:%M:%S.%f %z").timestamp()
                    captured = dt.datetime.strptime(body["captureTime"], "%Y-%m-%d %H:%M:%S.%f %z").timestamp()
                    record.update(recorded=recorded, captured=captured, process=body["procName"], timeStatus="PARSED")
                except (ValueError, KeyError, UnicodeError, TypeError):
                    pass  # Preserved UNKNOWN reports must never be silently excluded.
                result["reports"].append(record)
        result["readable"] = True
    except (OSError, ValueError, TimeoutError) as error:
        result["errors"].append(repr(error))
    result["finished"] = time.time()
    save(destination / "inventory.json", result)
    return result


def compare(candidate, reference, created):
    hashes = {r["sha256"] for source in candidate for r in source["reports"]}
    reports = reference["reports"]
    missing = [r for r in reports if r["sha256"] not in hashes]
    current = [r for r in reports if r["timeStatus"] != "PARSED" or max(r["recorded"], r["captured"]) >= created]
    return {"missingReferenceReports": missing, "currentOrUnknownReferenceReports": current,
            "currentDetectionDemonstrated": bool(current) and all(r["sha256"] in hashes for r in current),
            "sourceCompleteness": "UNPROVEN", "status": "INCONCLUSIVE",
            "reason": "Hash agreement, especially an empty inventory, does not prove authoritative completeness"}


class ProbeRunner(native.Runner):
    def run(self, *args, **kwargs):
        result = super().run(*args, **kwargs)
        if args == ("xcodebuild", "-version") and result.splitlines()[0] != "Xcode 26.3":
            raise ValueError("Probe requires the same Xcode26.3 family")
        if "build-for-testing" in args:
            # Start once at actual build completion, before identity/install/postbuild observations.
            self.deadline = min(self.deadline, time.monotonic() + 720)
        return result

    def simctl(self, *args, **kwargs):
        if args and args[0] == "create":
            if args[3] != "com.apple.CoreSimulator.SimRuntime.iOS-18-5":
                raise ValueError("Probe requires the measured iOS18.5 runtime family")
            args = (args[0], "OquTurbo-diagnostic-capability-only", *args[2:])
        return super().simctl(*args, **kwargs)


class Probe:
    def __init__(self, output):
        self.runner = ProbeRunner(output)
        self.root = self.runner.output
        self.report = {"status": "INCONCLUSIVE", "acceptance": "NOT_RUN", "operations": [], "cleanup": {}}
        self.original = None
        self.mutation = False

    def budget(self):
        if time.monotonic() >= self.runner.deadline:
            raise TimeoutError("Single 12-minute postbuild probe deadline exhausted")

    def runner_documents(self):
        self.budget()
        value = self.runner.simctl("get_app_container", self.runner.udid, native.region.RUNNER_BUNDLE, "data", timeout=10)
        folder = Path(value) / "Documents"
        if not Path(value).is_absolute() or not Path(value).is_dir():
            raise ValueError("Missing current runner sandbox")
        folder.mkdir(exist_ok=True)
        return folder

    def step(self, name, method, fields=None, flag=False):
        self.budget()
        folder = self.root / name
        folder.mkdir()
        request = dict(self.identity, operation=str(uuid.uuid4()), udid=self.runner.udid,
                       created=time.time(), step=name, purpose="diagnostic-capability-only")
        request.update(fields or {})
        save(folder / "request.json", request)
        documents = self.runner_documents()
        (documents / "region-observation.json").unlink(missing_ok=True)
        save(documents / "region-operation.json", request)
        args = self.runner.xcode_args() + ["test-without-building", "-only-testing:OquTurboUITests/LocalReminderRuntimeTests/" + method,
                                           "-resultBundlePath", str(folder / "runtime.xcresult")]
        if flag:
            args += ["-collect-test-diagnostics", "never"]
        operation = {"name": name, "args": args, "started": time.time(), "startMonotonic": time.monotonic()}
        self.report["operations"].append(operation)
        with (folder / "xcodebuild.log").open("w") as log:
            process = subprocess.Popen(args, cwd=self.runner.root, stdout=log, stderr=subprocess.STDOUT)
            self.runner.process = process
            try:
                operation["code"] = process.wait(timeout=max(0.001, self.runner.deadline - time.monotonic()))
            finally:
                if process.poll() is None:
                    process.kill()
                    process.wait(timeout=1)
                self.runner.process = None
                operation.update(finished=time.time(), endMonotonic=time.monotonic(), reaped=True)
                save(folder / "operation.json", operation)
        self.budget()
        path = self.runner_documents() / "region-observation.json"
        observation = json.loads(path.read_text()) if path.exists() else {}
        save(folder / "observation.json", observation)
        if name in {"baseline", "readback"}:
            summary = json.loads(self.runner.run("xcrun", "xcresulttool", "get", "test-results", "summary", "--path", str(folder / "runtime.xcresult")))
            save(folder / "summary.json", summary)
            native.evidence.validate_summary(summary)
            if operation["code"] != 0:
                raise ValueError("Settings readback XCTest unsuccessful")
            native.region.validate_observation(request, observation, {"probe-region"})
        return request, observation

    def full_reference(self, name):
        # Only accept installed help advertising all three scope/output options; never diagnose all devices.
        folder = self.root / name
        folder.mkdir()
        capture = {"started": time.time(), "startMonotonic": time.monotonic(), "udid": self.runner.udid}
        try:
            self.runner.run("xcrun", "simctl", "diagnose", "--udid=" + self.runner.udid,
                            "--output=" + str(folder), "--no-archive", timeout=720)
        finally:
            capture.update(finished=time.time(), endMonotonic=time.monotonic())
            save(folder / "capture-window.json", capture)
        return inventory(folder, self.root / (name + "-inventory"), self.runner.deadline)

    def candidates(self, name):
        return [inventory(source, self.root / name / str(i), self.runner.deadline) for i, source in enumerate(self.sources)]

    def investigate(self):
        self.runner.prepare()  # Existing45m build,100m global cap; no product acceptance episodes.
        self.report["deadlineMonotonic"] = self.runner.deadline
        self.identity = {k: v for k, v in json.loads((self.root / "app-identity.json").read_text()).items() if k in {"source", "sha256"}}
        self.identity["build"] = self.identity.pop("sha256")
        help_text = self.runner.run("xcodebuild", "-help", check=False)
        # xcodebuild may put its help on stderr; preserve complete commands.jsonl and use that entry.
        last = json.loads((self.root / "commands.jsonl").read_text().splitlines()[-1])
        help_text += last.get("stderr", "")
        if "-collect-test-diagnostics" not in help_text:
            raise ValueError("Installed Xcode does not advertise requested flag")
        help_text = self.runner.simctl("help", "diagnose", check=False)
        last = json.loads((self.root / "commands.jsonl").read_text().splitlines()[-1])
        help_text += last.get("stderr", "")
        if not all(option in help_text for option in ("--udid", "--output", "--no-archive")):
            raise ValueError("Installed diagnose cannot provide required scoped unarchived reference")
        self.runner.run("xcrun", "xcresulttool", "help", "export", "diagnostics")
        runner_app = self.runner.derived / "Build/Products/Debug-iphonesimulator/OquTurboUITests-Runner.app"
        info = plistlib.loads((runner_app / "Info.plist").read_bytes())
        if info["CFBundleIdentifier"] != native.region.RUNNER_BUNDLE:
            raise ValueError("Built runner identity mismatch")
        binary = runner_app / info["CFBundleExecutable"]
        save(self.root / "runner-identity.json", {"bundle": info["CFBundleIdentifier"], "sha256": hashlib.sha256(binary.read_bytes()).hexdigest()})
        self.runner.simctl("install", self.runner.udid, str(runner_app))
        _, baseline = self.step("baseline", "testDiagnosticRegionReadback")
        if baseline.get("observedLanguage") != "en-US" or baseline.get("observedRegion") != "United States":
            raise ValueError("Probe requires observed United States/en-US fixture")
        self.original = {"originalRegion": baseline["observedRegion"], "originalLanguage": baseline["observedLanguage"]}
        home = self.runner.simctl("getenv", self.runner.udid, "HOME")
        if not Path(home).is_absolute() or self.runner.udid not in home:
            raise ValueError("Simulator HOME is not scoped to the owned device")
        self.sources = [Path.home() / "Library/Logs/DiagnosticReports", Path(home) / "Library/Logs/CrashReporter"]
        save(self.root / "candidate-sources.json", {"sources": [str(p) for p in self.sources], "authority": "UNPROVEN",
              "note": "Native candidates derived from actual host/device homes; completeness requires reference metadata, not path convention"})
        self.report["referenceBefore"] = self.full_reference("reference-before")
        self.report["candidateBefore"] = self.candidates("candidate-before")
        if not any(s["readable"] for s in self.report["candidateBefore"]):
            raise ValueError("No readable native candidate source; preflight inconclusive")
        self.mutation = True
        request, observation = self.step("change", "testSystemRegionTransition", dict(self.original, target="Germany"), flag=True)
        self.report["candidateAfter"] = self.candidates("candidate-after")
        result = self.root / "change/runtime.xcresult"
        self.runner.run("xcrun", "xcresulttool", "export", "diagnostics", "--path", str(result),
                        "--output-path", str(self.root / "candidate-diagnostics"), check=False)
        sessions = list((self.root / "candidate-diagnostics").rglob("Session-*.log"))
        database = result / "database.sqlite3"
        self.report["candidateSessions"] = [{"path": str(p), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()} for p in sessions]
        if database.is_file():
            with sqlite3.connect("file:" + str(database) + "?mode=ro", uri=True) as db:
                rows = list(db.execute("SELECT compactDescription,timestamp FROM TestIssues ORDER BY timestamp"))
                self.report["candidateIssues"] = rows
                self.report["candidateBuildIssues"] = list(db.execute("SELECT message FROM BuildIssues"))
        self.report["referenceAfter"] = self.full_reference("reference-after")
        self.report["candidateRecheck"] = self.candidates("candidate-recheck")
        self.report["comparison"] = compare(self.report["candidateRecheck"], self.report["referenceAfter"], request["created"])
        original_hashes = {r["sha256"] for source in self.report["candidateAfter"] for r in source["reports"]}
        self.report["comparison"]["materializedAfterCandidate"] = [
            report for source in self.report["candidateRecheck"] for report in source["reports"]
            if report["sha256"] not in original_hashes]
        self.report["comparison"]["candidateCaptureWindows"] = [
            {key: source[key] for key in ("source", "started", "finished", "readable", "errors")}
            for source in self.report["candidateAfter"]]
        self.report["observation"] = observation
        # Replay unchanged strict issue/tap provenance, but never promote this to completeness or acceptance.
        try:
            texts = [p.read_text(errors="replace") for p in sessions]
            marker = "region-" + observation.get("state", "") + "-" + request["operation"]
            matching = [text for text in texts if marker in text]
            if len(matching) != 1 or "candidateIssues" not in self.report:
                raise ValueError("Missing unique Session/SQLite classifier evidence")
            crash_paths = list((self.root / "candidate-recheck").rglob("*.ips")) + list((self.root / "candidate-recheck").rglob("*.crash"))
            scoped, current = native.region.scope_crashes(crash_paths, request["created"])
            self.report["candidateCrashScope"] = scoped
            self.report["classifierReplay"] = native.region.classify_interruption(
                request, observation, [v[0] for v in self.report["candidateIssues"]],
                [v[0] for v in self.report["candidateBuildIssues"]], matching[0], current,
                issue_times=[v[1] + 978307200 for v in self.report["candidateIssues"]], sessions=texts)
        except Exception as error:
            self.report["classifierReplay"] = repr(error)
        if not sessions or not database.is_file():
            self.report.update(status="REJECT_FLAG_PATH", reason="Required Session or SQLite evidence is absent")
        else:
            self.report["reason"] = "Candidate artifacts present; authoritative source completeness still requires independent proof"

    def close(self):
        try:
            if self.mutation and time.monotonic() < self.runner.deadline:
                self.step("restore", "testSystemRegionTransition", dict(self.original, target=self.original["originalRegion"]))
                self.step("readback", "testDiagnosticRegionReadback", {"expectedRegion": self.original["originalRegion"]})
                self.report["cleanup"]["uiReadback"] = "SUCCESS"
            else:
                self.report["cleanup"]["uiReadback"] = "NOT_RUN"
        except Exception as error:
            self.report["cleanup"]["uiReadback"] = repr(error)
        finally:
            # Resource disposal only; never extends probe evidence/acceptance time or touches other simulators.
            if self.runner.udid:
                for action in ["shutdown", "delete"]:
                    try:
                        value = subprocess.run(["xcrun", "simctl", action, self.runner.udid], capture_output=True, text=True, timeout=40)
                        self.report["cleanup"][action] = {"code": value.returncode, "stderr": value.stderr}
                    except Exception as error:
                        self.report["cleanup"][action] = repr(error)
            save(self.root / "probe-report.json", self.report)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    probe = Probe(args.output)
    try:
        probe.investigate()
    except Exception as error:
        probe.report["error"] = repr(error)
    finally:
        probe.close()
    print(json.dumps({"diagnosticCapability": probe.report["status"], "acceptance": "NOT_RUN"}))
    raise SystemExit(2)  # This investigative scaffold cannot establish equivalence/acceptance automatically.


if __name__ == "__main__":
    main()
