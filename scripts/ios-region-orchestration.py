"""One locale episode with host-owned real Settings transitions and fail-closed readbacks."""
import json
import datetime as dt
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


def scoped_clock(clock, created):
    day = dt.datetime.fromtimestamp(created, dt.timezone.utc).date()
    candidates = [dt.datetime.fromisoformat(str(day + dt.timedelta(days=offset)) + "T" + clock + "+00:00").timestamp()
                  for offset in (-1, 0, 1)]
    matching = [value for value in candidates if created <= value <= created + 720]
    if len(matching) != 1:
        raise ValueError("Ambiguous or out-of-operation diagnostic time")
    return matching[0]


def completed_dispatch(request, observation, text, tap):
    # Runner event timestamps include the actual date/zone; host receipt order is buffered.
    pattern = re.compile(r'^(\d\d:\d\d:\d\d\.\d+) OquTurboUITests-Runner\[(\d+):(\d+)\] '
                         r'<XCTContext: (0x[0-9a-f]+)> (started|finished) activity '
                         r'<XCActivityRecord: (0x[0-9a-f]+)> (\d{4}-\d\d-\d\d) \d\d:\d\d:\d\d \+0000: (.+)$', re.M)
    activities = []
    for match in pattern.finditer(text):
        clock, pid, thread, context, action, activity, date, title = match.groups()
        stamp = dt.datetime.fromisoformat(date + "T" + clock + "+00:00").timestamp()
        activities.append((stamp, pid, thread, context, action, activity, title))
    starts = [a for a in activities if a[4] == "started" and a[6] == tap]
    if len(starts) != 1:
        raise ValueError("Ambiguous exact native tap activity")
    start = starts[0]
    stamp, pid, thread, context = start[:4]
    marker = "region-" + observation["state"] + "-" + request["operation"]
    marker_lines = re.findall(r'^(\d\d:\d\d:\d\d\.\d+) OquTurboUITests-Runner\[' + pid + ':' + thread +
                             r'\] Activity [^\n]*added attachment with name \x27' + re.escape(marker) + r'\x27', text, re.M)
    if len(marker_lines) != 1:
        raise ValueError("Missing fresh marker from the dispatching runner/thread")
    marker_time = scoped_clock(marker_lines[0], request["created"])
    if not request["created"] <= observation["observed"] <= marker_time <= stamp <= request["created"] + 720:
        raise ValueError("Marker and native dispatch timestamps disagree")
    deaths = re.findall(r'^(\d\d:\d\d:\d\d\.\d+) xcodebuild\[(\d+):\d+\][^\n]*' + re.escape(request["udid"]) +
                        r'[^\n]*got death notice for pid ' + pid + r' \(exit code: \(null\), signal: 9\)', text, re.M)
    if len(deaths) != 1:
        raise ValueError("Missing unique matching simulator/runner SIGKILL")
    death = scoped_clock(deaths[0][0], request["created"])
    syntheses = [a for a in activities if a[1:4] == (pid, thread, context) and a[4] == "started" and
                 a[6] == "Synthesize event" and stamp < a[0] < death]
    if len(syntheses) != 1:
        raise ValueError("Missing unique synthesis nested in the exact native tap")
    synthesis = syntheses[0]
    finishes = [a for a in activities if a[1:4] == (pid, thread, context) and a[4] == "finished" and
                a[5] == synthesis[5] and a[6].startswith("Synthesize event (") and synthesis[0] < a[0] < death]
    if len(finishes) != 1:
        raise ValueError("Native synthesis did not complete before SIGKILL")
    finish = finishes[0][0]
    other_gestures = [a for a in activities if a[1] == pid and a[4] == "started" and stamp < a[0] <= finish and
                      a[6].startswith(("Tap ", "Swipe ", "Press ", "Type "))]
    if other_gestures:
        raise ValueError("Intervening gesture makes dispatch provenance ambiguous")
    return death, deaths[0][1]


RUNNER_BUNDLE = "com.alad1nks.oquturbo.OquTurbo.UITests.xctrunner"
KILL_ISSUE = "Test crashed with signal kill."


def validate_setup_issues(request, issues, build_issues, issue_times, sessions, death, host_pid):
    if issues == [KILL_ISSUE] and not build_issues:
        return
    # Only the observed automatic relaunch of this same runner after its SIGKILL is recognized.
    if len(issues) != 2 or issues[0] != KILL_ISSUE or build_issues != ["Simulator device failed to launch " + RUNNER_BUNDLE + "."]:
        raise ValueError("Unrecognized or additional setup failure")
    prefix = 'Failed to install or launch the test runner. (Underlying Error: Simulator device failed to launch ' + RUNNER_BUNDLE
    exact = re.fullmatch(re.escape(prefix) + r'\. The system shell \(SpringBoard:(\d+)\) probably crashed\. '
                         r'\(Underlying Error: The request to terminate "' + re.escape(RUNNER_BUNDLE) +
                         r'" failed\. The system shell \(SpringBoard:\1\) probably crashed\.\)\)', issues[1])
    if not exact or not issue_times or len(issue_times) != 2 or not death < issue_times[0] < issue_times[1] <= request["created"] + 720:
        raise ValueError("Additional issue is not the exact downstream runner relaunch failure")
    shell = "The system shell (SpringBoard:" + exact[1] + ") probably crashed."
    matching = []
    for text in sessions:
        if request["udid"] not in text:
            continue
        for line in text.splitlines():
            if 'Test operation failure: Error Domain=XCTHTestOperationCoordinatorErrorDomain Code=4 "Failed to install or launch the test runner"' not in line:
                continue
            clock = re.match(r'^(\d\d:\d\d:\d\d\.\d+) xcodebuild\[' + host_pid + r':\d+\]', line)
            if clock and 'Error Domain=NSPOSIXErrorDomain Code=64 "Host is down"' in line and shell in line and \
                    'NSLocalizedDescription=Simulator device failed to launch ' + RUNNER_BUNDLE + '.' in line:
                when = scoped_clock(clock[1], request["created"])
                date = re.search(r"DVTErrorCreationDateKey=(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d \+0000)", line)
                if date and 0 <= when - dt.datetime.strptime(date[1], "%Y-%m-%d %H:%M:%S %z").timestamp() < 1 and \
                        issue_times[0] <= when <= issue_times[1]:
                    matching.append(when)
    if len(matching) != 1:
        raise ValueError("Missing exact same-device post-kill automatic relaunch diagnostic")


def scope_crashes(paths, created):
    records, current = [], []
    for path in paths:
        record = {"file": str(path), "classification": "CURRENT_OR_UNKNOWN"}
        try:
            lines = path.read_text().splitlines()
            header, payload = json.loads(lines[0]), json.loads("\n".join(lines[1:]))
            recorded = dt.datetime.strptime(header["timestamp"], "%Y-%m-%d %H:%M:%S.%f %z")
            captured = dt.datetime.strptime(payload["captureTime"], "%Y-%m-%d %H:%M:%S.%f %z")
            if recorded.tzinfo is None or captured.tzinfo is None:
                raise ValueError("Missing crash timezone")
            if not isinstance(payload["procName"], str):
                raise ValueError("Unknown crash process")
            record.update(timestamp=header["timestamp"], captureTime=payload["captureTime"], process=payload["procName"])
            if recorded.timestamp() < created and captured.timestamp() < created and \
                    not payload["procName"].startswith("OquTurbo"):
                record["classification"] = "PREEXISTING_UNRELATED"
        except (ValueError, KeyError, TypeError, IndexError, OSError) as error:
            record["parseError"] = repr(error)
        if record["classification"] != "PREEXISTING_UNRELATED":
            current.append(path)
        records.append(record)
    return records, current


def classify_interruption(request, observation, issues, build_issues, diagnostic_text, crash_files,
                          issue_times=None, sessions=()):
    validate_observation(request, observation, {"before-confirmation", "before-region-choice"})
    if crash_files:
        raise ValueError("Current or unknown crash evidence during setup")
    if observation["state"] == "before-confirmation":
        restoring = request["step"] == "restore" and request["target"] == "United States"
        confirmation = "Change to United States" if restoring else "Keep English (US)"
        if observation.get("confirmation") != confirmation:
            raise ValueError("Unknown confirmation")
        tap = 'Tap "' + confirmation + '" Button'
    else:
        if request["step"] != "restore":
            raise ValueError("Forward transition never accepts a marker before confirmation")
        tap = 'Tap "' + request["target"] + '" StaticText'
    death, host_pid = completed_dispatch(request, observation, diagnostic_text, tap)
    validate_setup_issues(request, issues, build_issues, issue_times, sessions, death, host_pid)
    if re.search(r"Assertion Failure|XCTAssert[^\n]*failed|Test timed out|test execution[^\n]*exceeded|Test Case [^\n]*failed", diagnostic_text, re.I):
        raise ValueError("Unexpected failure in setup diagnostics")
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
                    rows = list(connection.execute("SELECT compactDescription, timestamp FROM TestIssues ORDER BY timestamp"))
                    issues = [v[0] for v in rows]
                    issue_times = [v[1] + 978307200 for v in rows]
                    build_issues = [v[0] for v in connection.execute("SELECT message FROM BuildIssues")]
                sessions = list(diagnostic.rglob("Session-*.log"))
                texts = [p.read_text(errors="replace") for p in sessions]
                matching = [text for text in texts if "region-" + observation.get("state", "") + "-" + request["operation"] in text]
                if len(matching) != 1:
                    raise ValueError("Ambiguous setup diagnostic sessions")
                text = matching[0]
                crashes = list(diagnostic.rglob("*.ips")) + list(diagnostic.rglob("*.crash"))
                crash_records, current_crashes = scope_crashes(crashes, request["created"])
                (location / "crash-scope.json").write_text(json.dumps(crash_records, indent=2))
                status = classify_interruption(request, observation, issues, build_issues, text, current_crashes,
                                               issue_times=issue_times, sessions=texts)
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
