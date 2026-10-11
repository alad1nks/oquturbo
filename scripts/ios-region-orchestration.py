"""One locale episode with host-owned real Settings transitions and fail-closed readbacks."""
import json
import plistlib
import re
import sqlite3
import subprocess
import time
import uuid
from pathlib import Path


def validate_observation(request, observation, states):
    if any(observation.get(key) != value for key, value in request.items()):
        raise ValueError("Stale or mismatched region operation")
    if observation.get("state") not in states:
        raise ValueError("Missing required region observation")
    observed = observation.get("observed")
    if not isinstance(observed, (int, float)) or not request["created"] <= observed <= request["created"] + 720:
        raise ValueError("Invalid region observation timestamp")
    return observation


def validate_baseline(request, observation, saved_minutes):
    validate_observation(request, observation, {"baseline"})
    wheels = observation.get("wheels")
    if not isinstance(wheels, list) or len(wheels) != 3 or wheels[2] not in ("AM", "PM"):
        raise ValueError("Missing actual English twelve-hour baseline")
    values = [re.findall(r"\d+", value) for value in wheels[:2]]
    if any(len(v) != 1 for v in values):
        raise ValueError("Ambiguous baseline wheel")
    hour, minute = (int(v[0]) for v in values)
    if not 1 <= hour <= 12 or not 0 <= minute <= 59:
        raise ValueError("Invalid baseline time")
    expected = (hour % 12 + (12 if wheels[2] == "PM" else 0)) * 60 + minute
    if observation.get("minutes") != expected or saved_minutes != expected:
        raise ValueError("Observed baseline differs from saved reminder time")
    if observation.get("originalLanguage") != "en-US" or not observation.get("originalRegion") or observation["originalRegion"] == "Germany":
        raise ValueError("Invalid observed original system region/language")
    return observation


def classify_interruption(request, observation, issues, build_issues, diagnostic_text, crash_files):
    validate_observation(request, observation, {"before-confirmation", "before-region-choice"})
    if issues != ["Test crashed with signal kill."] or build_issues or crash_files:
        raise ValueError("Unrecognized or additional setup failure")
    if observation["state"] == "before-confirmation":
        if observation.get("confirmation") != "Keep English (US)":
            raise ValueError("Unknown confirmation")
        tap = 'Tap "Keep English (US)" Button'
    else:
        if request["step"] != "restore":
            raise ValueError("Forward transition never accepts a marker before confirmation")
        tap = 'Tap "' + request["target"] + '" StaticText'
    marker = "region-" + observation["state"] + "-" + request["operation"]
    marker_position = diagnostic_text.find(marker)
    # Detailed XCTest diagnostics prove completed dispatch, unlike the wrapper's pre-tap activity line.
    completed = re.search(r"didFinishActivity:[^\n]*" + re.escape(tap), diagnostic_text)
    death = re.search(r"got death notice for pid \d+ \(exit code: \(null\), signal: 9\)", diagnostic_text)
    runner = re.search(r"OquTurboUITests-Runner\[(\d+):", diagnostic_text)
    if marker_position < 0 or not completed or completed.start() <= marker_position or not death or death.start() <= completed.start() or not runner:
        raise ValueError("Missing actual scoped completed tap and runner death diagnostics")
    if "got death notice for pid " + runner[1] + " (exit code: (null), signal: 9)" not in diagnostic_text[completed.end():]:
        raise ValueError("SIGKILL does not identify the test runner")
    if re.search(r"Assertion Failure|XCTAssert[^\n]*failed|Test timed out|test execution[^\n]*exceeded|Test Case [^\n]*failed", diagnostic_text[:completed.end()], re.I):
        raise ValueError("Failure precedes setup transition")
    return "SETUP_TRANSITION_INTERRUPTED"


def run_locale(runner, directory, snapshots, baseline, boundary, cutoff):
    # Runner.run and all nested simctl/export calls use this same existing phase cutoff.
    suite_deadline = runner.deadline
    observations = []
    outcomes = []
    primary = None
    cleanup_error = None
    restore_needed = False
    initial = None
    identity = json.loads((runner.output / "app-identity.json").read_text())
    products = runner.derived / "Build/Products/Debug-iphonesimulator"
    infos = list(products.glob("OquTurboUITests-Runner.app/Info.plist"))
    if len(infos) != 1:
        runner.deadline = suite_deadline
        raise ValueError("Expected one built XCTest runner identity")
    runner_bundle = plistlib.loads(infos[0].read_bytes())["CFBundleIdentifier"]
    runner.deadline = min(suite_deadline, cutoff)
    try:
        def budget():
            if time.monotonic() >= cutoff:
                raise TimeoutError("Single locale phase deadline exhausted")

        def runner_documents(location, purpose):
            # XCTest may reinstall/migrate its own sandbox between invocations too.
            while True:
                budget()
                raw, error = "", None
                try:
                    raw = runner.simctl("get_app_container", runner.udid, runner_bundle, "data",
                                        timeout=min(10, cutoff - time.monotonic()))
                except (subprocess.TimeoutExpired, RuntimeError) as failure:
                    error = repr(failure)
                budget()
                container = Path(raw) if raw else None
                available = container is not None and container.is_absolute() and container.is_dir()
                with (location / "runner-container.jsonl").open("a") as log:
                    log.write(json.dumps({"purpose": purpose, "container": raw, "available": available,
                                          "error": error, "time": time.time()}) + "\n")
                if available:
                    documents = container / "Documents"
                    documents.mkdir(exist_ok=True)
                    return documents
                time.sleep(min(2, max(0, cutoff - time.monotonic())))

        def current_preferences(location, purpose):
            # Use the established current-container sampler, never the last polling path/snapshot.
            while True:
                budget()
                runner.await_preferences(directory, snapshots, cutoff)
                budget()
                documents = runner.documents
                try:
                    raw = (documents / "oquturbo.preferences_pb").read_bytes()
                except FileNotFoundError:
                    continue  # A migration during read consumes this same cutoff.
                budget()
                with (location / "app-container.jsonl").open("a") as log:
                    log.write(json.dumps({"purpose": purpose, "container": str(documents.parent),
                                          "time": time.time()}) + "\n")
                return raw

        def sample():
            if runner.sample(directory, snapshots, cutoff):
                probe = runner.documents / "reminder-diagnostics.jsonl"
                before = runner.evidence.events_since(probe.read_bytes(), boundary)
                raw = (runner.documents / "oquturbo.preferences_pb").read_bytes()
                after = runner.evidence.events_since(probe.read_bytes(), boundary)
                value = runner.evidence.dismissal_observation(before, after, raw)
                if value is not None:
                    observations.append(value)
                    if value["pickerHosts"] in (0, 2):
                        value["file"] = f"dismissal-prefs-{len(observations):04d}.preferences_pb"
                        (directory / value["file"]).write_bytes(raw)
                    (directory / "dismissal-observations.json").write_text(json.dumps(observations, indent=2))

        def step(name, method, extra=None, transition=False):
            budget()
            if runner.process is not None:
                raise RuntimeError("Previous XCTest process has not been reaped")
            request = {"operation": str(uuid.uuid4()), "udid": runner.udid, "source": identity["source"],
                       "build": identity["sha256"], "step": name, "created": time.time()}
            request.update(extra or {})
            location = directory / name
            location.mkdir()
            outcome = {"step": name, "status": "FAILED_OR_INCOMPLETE", "code": None, "operation": request["operation"]}
            outcomes.append(outcome)
            (location / "request.json").write_text(json.dumps(request, indent=2))
            documents = runner_documents(location, "request")
            (documents / "region-observation.json").unlink(missing_ok=True)
            (documents / "region-operation.json").write_text(json.dumps(request))
            unchanged = name in {"accept", "readback"}
            before_preferences = current_preferences(location, "before") if unchanged else None
            if unchanged:
                (location / "before.preferences_pb").write_bytes(before_preferences)
            result = location / "runtime.xcresult"
            command = runner.xcode_args() + ["test-without-building", "-only-testing:OquTurboUITests/LocalReminderRuntimeTests/" + method,
                                           "-resultBundlePath", str(result)]
            with (location / "xcodebuild.log").open("w") as log:
                runner.process = subprocess.Popen(command, cwd=runner.root, stdout=log, stderr=subprocess.STDOUT)
                try:
                    while runner.process.poll() is None:
                        budget()
                        sample()
                        time.sleep(min(2, max(0, cutoff - time.monotonic())))
                    code = runner.process.returncode
                    outcome["code"] = code
                finally:
                    if runner.process.poll() is None:
                        runner.process.kill()
                        # Reaping an owned failed process cannot turn an expired episode into PASS.
                        runner.process.wait(timeout=1)
                    runner.process = None
            budget()
            observation_path = runner_documents(location, "observation") / "region-observation.json"
            observation = json.loads(observation_path.read_text()) if observation_path.exists() else {}
            (location / "observation.json").write_text(json.dumps(observation, indent=2))
            runner.run("xcrun", "xcresulttool", "export", "attachments", "--path", str(result), "--output-path", str(location / "attachments"))
            if code == 0:
                summary = json.loads(runner.run("xcrun", "xcresulttool", "get", "test-results", "summary", "--path", str(result)))
                (location / "summary.json").write_text(json.dumps(summary, indent=2))
                runner.evidence.validate_summary(summary)
                states = {"baseline"} if name == "initial" else {"accepted-24-hour"} if name == "accept" else {"restored"} if name == "readback" else {"transition-completed", "already-restored"}
                validate_observation(request, observation, states)
                status = "SUCCESS"
            elif transition:
                diagnostic = location / "diagnostics"
                runner.run("xcrun", "xcresulttool", "export", "diagnostics", "--path", str(result), "--output-path", str(diagnostic))
                database = result / "database.sqlite3"
                with sqlite3.connect("file:" + str(database) + "?mode=ro", uri=True) as connection:
                    issues = [v[0] for v in connection.execute("SELECT compactDescription FROM TestIssues")]
                    build_issues = list(connection.execute("SELECT message FROM BuildIssues"))
                sessions = list(diagnostic.rglob("Session-*.log"))
                texts = [p.read_text(errors="replace") for p in sessions]
                matching = [text for text in texts if "region-" + observation.get("state", "") + "-" + request["operation"] in text]
                if len(matching) != 1:
                    raise ValueError("Ambiguous setup diagnostic sessions")
                text = matching[0]
                crashes = list(diagnostic.rglob("*.ips")) + list(diagnostic.rglob("*.crash"))
                status = classify_interruption(request, observation, issues, build_issues, text, crashes)
            else:
                raise RuntimeError("Actual acceptance XCTest failed: " + name)
            if unchanged:
                after_preferences = current_preferences(location, "after")
                (location / "after.preferences_pb").write_bytes(after_preferences)
                if after_preferences != before_preferences:
                    raise ValueError("Hour-cycle Cancel/readback changed application preferences")
            budget()
            outcome["status"] = status
            (directory / "region-substeps.json").write_text(json.dumps(outcomes, indent=2))
            return request, observation

        try:
            request, initial = step("initial", "testLocalizedPendingAndLargeTextPicker")
            validate_baseline(request, initial, snapshots[0]["schedule"]["minutesOfDay"])
            # Validate the normally successful initial journey before any system mutation.
            runner.await_sample(directory, snapshots, cutoff)
            initial_events = runner.evidence.events_since((directory / "app-native.jsonl").read_bytes(), boundary)
            runner.evidence.validate_dismissal(initial_events, observations)
            runner.evidence.validate_phase("locales", initial_events, snapshots, baseline)
            common = {key: initial[key] for key in ("originalRegion", "originalLanguage", "minutes", "wheels")}
            restore_needed = True  # Set before dispatch of the first mutating helper.
            step("change", "testSystemRegionTransition", dict(common, target="Germany"), transition=True)
            step("accept", "testSystem24HourAcceptance", common)
        except Exception as error:
            primary = repr(error)
        finally:
            if restore_needed:
                try:
                    common = {key: initial[key] for key in ("originalRegion", "originalLanguage", "minutes", "wheels")}
                    step("restore", "testSystemRegionTransition", dict(common, target=initial["originalRegion"]), transition=True)
                    step("readback", "testSystemRegionRestored", common)
                except Exception as error:
                    cleanup_error = repr(error)
            (directory / "region-cleanup.json").write_text(json.dumps({"required": restore_needed,
                "primaryError": primary, "restorationError": cleanup_error,
                "restored": restore_needed and cleanup_error is None}, indent=2))
        if primary or cleanup_error:
            raise RuntimeError("Locale orchestration failed: " + str(primary) + "; restoration: " + str(cleanup_error))
        budget()
        runner.await_sample(directory, snapshots, cutoff)
        events = runner.evidence.events_since((directory / "app-native.jsonl").read_bytes(), boundary)
        (directory / "phase-native.json").write_text(json.dumps(events, indent=2))
        runner.evidence.validate_dismissal(events, observations)
        report = runner.evidence.validate_phase("locales", events, snapshots, baseline)
        # Region changes and Cancel may not change desired permission or the chosen minute.
        if any(s["enabled"] != snapshots[0]["enabled"] or s["schedule"]["minutesOfDay"] != initial["minutes"] for s in snapshots):
            raise ValueError("Region journey changed reminder preference")
        budget()
        (directory / "validation.json").write_text(json.dumps(report, indent=2))
        return report
    finally:
        (directory / "snapshots.json").write_text(json.dumps(snapshots, indent=2))
        (directory / "region-substeps.json").write_text(json.dumps(outcomes, indent=2))
        runner.deadline = suite_deadline
