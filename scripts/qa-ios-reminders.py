#!/usr/bin/env python3
"""Actual OquTurbo.app/XCTest runner. macOS arm64 only; no injected notification, grant, clock, or navigation."""
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
import platform
import plistlib
import re
import shutil
import subprocess
import time
from pathlib import Path

_spec = importlib.util.spec_from_file_location("ios_evidence", Path(__file__).with_name("ios-reminder-evidence.py"))
evidence = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(evidence)
PACKAGE = "com.alad1nks.oquturbo.OquTurbo"
PHASES = {
    "cold": "testColdDeliveryAndNativeOptIn",
    "warm": "testWarmDeliveryFromSettings",
    "foreground": "testForegroundDispatch",
    "locales": "testLocalizedPendingAndLargeTextPicker",
    "off": "testUntappedDeliveryThenOffRetainsTime",
    "permission": "testDeniedPermissionAndSettingsRecovery",
}


class Runner:
    def __init__(self, output, clock_probe=False):
        self.output = output.resolve()
        self.output.mkdir(parents=True, exist_ok=False)
        self.clock_probe = clock_probe
        self.udid = None
        self.process = None
        self.deadline = time.monotonic() + 100 * 60
        self.root = Path(__file__).resolve().parents[1]
        self.project = self.root / "app/oquturbo/iosApp/iosApp.xcodeproj"
        self.derived = self.output / "DerivedData"

    def run(self, *args, timeout=120, check=True):
        remaining = self.deadline - time.monotonic()
        if remaining <= 0:
            raise TimeoutError("Whole native run deadline exceeded")
        result = subprocess.run(args, cwd=self.root, capture_output=True, text=True,
                                timeout=min(timeout, remaining))
        with (self.output / "commands.jsonl").open("a") as log:
            log.write(json.dumps({"time": time.time(), "args": list(args), "code": result.returncode,
                                  "stdout": result.stdout, "stderr": result.stderr}) + "\n")
        if check and result.returncode:
            raise RuntimeError(f"Command failed ({result.returncode}): {args}")
        return result.stdout.strip()

    def simctl(self, *args, **kwargs):
        return self.run("xcrun", "simctl", *args, **kwargs)

    def xcode_args(self):
        return ["xcodebuild", "-project", str(self.project), "-scheme", "OquTurboRuntime", "-configuration", "Debug",
                "-destination", f"platform=iOS Simulator,id={self.udid}", "-derivedDataPath", str(self.derived),
                "-parallel-testing-enabled", "NO", "-maximum-concurrent-test-simulator-destinations", "1",
                "CODE_SIGNING_ALLOWED=NO"]

    def prepare(self):
        if platform.system() != "Darwin" or platform.machine() != "arm64":
            raise RuntimeError("Requires an actual macOS arm64 host, not a framework/metadata surrogate")
        if os.environ.get("OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED") == "YES":
            raise RuntimeError("Kotlin framework build must not be bypassed")
        self.run("xcodebuild", "-version")
        self.run("xcrun", "xcresulttool", "help", "get", "test-results", "summary")
        self.run("xcrun", "xcresulttool", "help", "export", "attachments")
        inventory = json.loads(self.simctl("list", "--json"))
        (self.output / "simulator-inventory.json").write_text(json.dumps(inventory, indent=2))
        runtimes = [r for r in inventory["runtimes"] if r.get("isAvailable") and
                    r["identifier"].startswith("com.apple.CoreSimulator.SimRuntime.iOS-") and
                    tuple(map(int, r["version"].split("."))) >= (18, 2)]
        if not runtimes:
            raise RuntimeError("No installed compatible iOS runtime")
        runtime = min(runtimes, key=lambda r: tuple(map(int, r["version"].split("."))))
        # Prefer an installed known-compatible iPhone's type; do not guess support from a newer model name.
        devices = inventory["devices"].get(runtime["identifier"], [])
        supported = next((d for d in devices if d.get("isAvailable") and "iPhone-SE-3rd-generation" in d.get("deviceTypeIdentifier", "")), None)
        if supported is None:
            raise RuntimeError("No installed narrow iPhone SE (3rd generation) type for selected runtime")
        self.udid = self.simctl("create", "OquTurbo-reminder-acceptance", supported["deviceTypeIdentifier"], runtime["identifier"])
        if not re.fullmatch(r"[0-9A-Fa-f-]{36}", self.udid):
            raise RuntimeError("Invalid owned simulator identity")
        (self.output / "owned-simulator.json").write_text(json.dumps({"udid": self.udid, "runtime": runtime}))
        self.simctl("boot", self.udid)
        self.simctl("bootstatus", self.udid, "-b", timeout=240)
        self.run("xcodebuild", "-project", str(self.project), "-scheme", "OquTurboRuntime", "-showBuildSettings",
                 "-destination", f"platform=iOS Simulator,id={self.udid}")
        # Both existing Kotlin embed phases execute; errors are not replaced by a placeholder framework.
        self.run(*self.xcode_args(), "build-for-testing", "-resultBundlePath", str(self.output / "build.xcresult"), timeout=2700)
        self.app = self.derived / "Build/Products/Debug-iphonesimulator/OquTurbo.app"
        with (self.app / "Info.plist").open("rb") as stream:
            info = plistlib.load(stream)
        if info.get("CFBundleIdentifier") != PACKAGE:
            raise RuntimeError("Built application bundle identity mismatch")
        binary = self.app / info["CFBundleExecutable"]
        if "arm64" not in self.run("lipo", "-archs", str(binary)).split():
            raise RuntimeError("Actual app binary is not arm64")
        (self.output / "app-identity.json").write_text(json.dumps({"bundle": PACKAGE, "executable": str(binary),
            "sha256": hashlib.sha256(binary.read_bytes()).hexdigest(), "source": self.run("git", "rev-parse", "HEAD")}))
        self.install_fixture()

    def install_fixture(self):
        self.simctl("install", self.udid, str(self.app))
        container = Path(self.simctl("get_app_container", self.udid, PACKAGE, "data")).resolve()
        if not container.is_dir() or not container.is_absolute():
            raise RuntimeError("No actual installed application container")
        self.documents = container / "Documents"
        self.documents.mkdir(exist_ok=True)
        # Debug-only read-only probe switch; survives a real SpringBoard cold launch without launchEnvironment.
        (self.documents / "reminder-diagnostics-enabled").touch(exist_ok=False)

    def sample(self, directory, snapshots):
        # XCTest may reinstall the app and migrate its data to a new container UUID.
        # Resolve the installed app again; never recreate a probe marker or substitute stale data.
        raw = self.simctl("get_app_container", self.udid, PACKAGE, "data", check=False, timeout=10)
        container = Path(raw) if raw else None
        available = container is not None and container.is_absolute() and container.is_dir()
        documents = container / "Documents" if available else None
        with (directory / "container-inventory.jsonl").open("a") as log:
            log.write(json.dumps({"time": time.time(), "container": raw, "available": available,
                                  "probeEnabled": available and (documents / "reminder-diagnostics-enabled").exists()}) + "\n")
        if not available:
            return
        self.documents = documents
        preferences = self.documents / "oquturbo.preferences_pb"
        if preferences.exists():
            data = preferences.read_bytes()
            current = evidence.snapshot(data)
            if not snapshots or current != snapshots[-1]:
                snapshots.append(current)
                (directory / f"prefs-{len(snapshots):04d}.preferences_pb").write_bytes(data)
        probe = self.documents / "reminder-diagnostics.jsonl"
        if probe.exists():
            shutil.copyfile(probe, directory / "app-native.jsonl")

    def phase(self, name, method):
        directory = self.output / name
        directory.mkdir()
        snapshots = []
        self.sample(directory, snapshots)
        baseline = {k: v for k, v in (snapshots[-1]["values"] if snapshots else {}).items() if k not in evidence.mutable_keys(name)}
        probe = self.documents / "reminder-diagnostics.jsonl"
        boundary = probe.read_bytes() if probe.exists() else b""
        # A phase owns the journal suffix, not timestamps that intentionally move during DST probes.
        evidence.events_since(boundary, boundary)
        (directory / "event-boundary.json").write_text(json.dumps({"bytes": len(boundary),
            "prefixSha256": hashlib.sha256(boundary).hexdigest()}))
        started = time.time()
        result = directory / "runtime.xcresult"
        command = self.xcode_args() + ["test-without-building", "-only-testing:OquTurboUITests/LocalReminderRuntimeTests/" + method,
                                      "-resultBundlePath", str(result)]
        with (directory / "xcodebuild.log").open("w") as log:
            self.process = subprocess.Popen(command, cwd=self.root, stdout=log, stderr=subprocess.STDOUT)
            cutoff = min(self.deadline, time.monotonic() + 12 * 60)
            while self.process.poll() is None:
                if time.monotonic() >= cutoff:
                    self.process.terminate()
                    raise TimeoutError("Bounded XCTest episode timed out")
                self.sample(directory, snapshots)
                time.sleep(2)
            code = self.process.returncode
            self.process = None
        # Preserve partial native evidence even when the app/test bundle fails.
        self.sample(directory, snapshots)
        (directory / "snapshots.json").write_text(json.dumps(snapshots, indent=2))
        (directory / "phase.json").write_text(json.dumps({"start": started, "end": time.time(), "method": method, "code": code}))
        self.simctl("io", self.udid, "screenshot", str(directory / "final.png"), check=False)
        if result.exists():
            self.run("xcrun", "xcresulttool", "export", "attachments", "--path", str(result), "--output-path", str(directory / "attachments"))
        if code != 0:
            raise RuntimeError(f"Actual XCTest failed in {name}")
        summary = json.loads(self.run("xcrun", "xcresulttool", "get", "test-results", "summary", "--path", str(result)))
        (directory / "summary.json").write_text(json.dumps(summary, indent=2))
        evidence.validate_summary(summary)
        events = evidence.events_since((directory / "app-native.jsonl").read_bytes(), boundary)
        (directory / "phase-native.json").write_text(json.dumps(events, indent=2))
        report = evidence.validate_phase(name, events, snapshots, baseline)
        (directory / "validation.json").write_text(json.dumps(report, indent=2))
        return report

    def clock_phases(self):
        if not self.clock_probe:
            raise RuntimeError("BLOCKED N5: dedicated runner clock/zone controls not explicitly enabled")
        # A disposable CI host only: never change a developer's ordinary Mac clock without this explicit mode.
        if os.environ.get("CI") != "true":
            raise RuntimeError("BLOCKED N5: clock controls require a disposable CI host")
        self.run("sudo", "-n", "-v")
        zone = self.run("sudo", "-n", "systemsetup", "-gettimezone").removeprefix("Time Zone: ").strip()
        network = self.run("sudo", "-n", "systemsetup", "-getusingnetworktime")
        if not zone or network not in ("Network Time: On", "Network Time: Off"):
            raise RuntimeError("BLOCKED N5: unreadable original system controls")
        original = time.time()
        monotonic = time.monotonic()
        reports = []
        try:
            self.run("sudo", "-n", "systemsetup", "-setusingnetworktime", "off")
            for phase, new_zone in (("travel", "Asia/Almaty"), ("dst", "America/New_York")):
                self.run("sudo", "-n", "systemsetup", "-settimezone", new_zone)
                actual = self.run("sudo", "-n", "systemsetup", "-gettimezone")
                if actual != "Time Zone: " + new_zone:
                    raise RuntimeError("BLOCKED N5: system timezone change not verified")
                if phase == "dst":
                    self.run("sudo", "-n", "date", "-u", "030806002026.00")
                    epoch = float(self.run("date", "+%s"))
                    if not 1772949600 <= epoch < 1772949660:
                        raise RuntimeError("BLOCKED N5: system clock change not verified")
                reports.append(self.phase(phase, "testCalendarControlProbe"))
        finally:
            # Restore even if an assertion, capability probe or XCTest fails. Use monotonic elapsed time.
            restored = dt.datetime.fromtimestamp(original + time.monotonic() - monotonic, dt.timezone.utc)
            results = []
            for command in (["sudo", "-n", "date", "-u", restored.strftime("%m%d%H%M%Y.%S")],
                            ["sudo", "-n", "systemsetup", "-settimezone", zone],
                            ["sudo", "-n", "systemsetup", "-setusingnetworktime", "on" if network.endswith("On") else "off"]):
                try:
                    result = subprocess.run(command, capture_output=True, text=True, timeout=40)
                    results.append({"command": command, "code": result.returncode, "stdout": result.stdout, "stderr": result.stderr})
                except Exception as error:
                    # A failed clock restore must not prevent attempts to restore zone and network time too.
                    results.append({"command": command, "code": -1, "error": str(error)})
            (self.output / "clock-restoration.json").write_text(json.dumps(results, indent=2))
            if any(r["code"] != 0 for r in results):
                raise RuntimeError("System clock/zone restoration failed; inspect clock-restoration.json")
        return reports

    def close(self):
        if self.process and self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=10)
        if self.udid:
            subprocess.run(["xcrun", "simctl", "shutdown", self.udid], capture_output=True, timeout=40, check=False)
            # Keep owned simulator data on failure for inspection; never shutdown/delete unrelated UDIDs.


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--dedicated-runner-clock-probe", action="store_true")
    args = parser.parse_args()
    runner = Runner(args.output, args.dedicated_runner_clock_probe)
    report = {"status": "NOT_RUN", "phases": [], "unverified": ["VoiceOver navigation"]}
    try:
        runner.prepare()
        for name, method in PHASES.items():
            if name == "permission":
                # Explicit separate permission fixture. No persistence claim crosses this boundary.
                runner.simctl("shutdown", runner.udid)
                runner.simctl("erase", runner.udid)
                runner.simctl("boot", runner.udid)
                runner.simctl("bootstatus", runner.udid, "-b", timeout=240)
                runner.install_fixture()
                (runner.output / "permission-reset.json").write_text(json.dumps({"time": time.time(), "scope": runner.udid, "operation": "explicit owned simulator erase after prior evidence preserved"}))
            if name == "locales":
                runner.simctl("ui", runner.udid, "content_size", "accessibility-extra-extra-large")
                readback = runner.simctl("ui", runner.udid, "content_size")
                if "accessibility-extra-extra-large" not in readback:
                    raise RuntimeError("BLOCKED N12: actual large text setting not verified")
            report["phases"].append(runner.phase(name, method))
            if name == "locales":
                runner.simctl("ui", runner.udid, "content_size", "large")
            if name == "off":
                report["phases"].extend(runner.clock_phases())
        report["status"] = "MANDATORY_AUTOMATED_MATRIX_PASS"
    except Exception as error:
        report["status"] = "FAILED_OR_BLOCKED"
        report["error"] = str(error)
        raise
    finally:
        (runner.output / "report.json").write_text(json.dumps(report, indent=2))
        runner.close()


if __name__ == "__main__":
    main()
