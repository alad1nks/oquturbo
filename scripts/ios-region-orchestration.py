"""One locale episode with host-owned real Settings transitions and fail-closed readbacks."""
import json
from contextlib import closing
import importlib.util
import datetime as dt
import plistlib
import re
import sqlite3
import sys
import subprocess
import time
import uuid
from pathlib import Path


_generated_spec = importlib.util.spec_from_file_location("ios_generated", Path(__file__).with_name("ios-generated-test-run.py"))
generated = importlib.util.module_from_spec(_generated_spec)
_generated_spec.loader.exec_module(generated)


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


RUNNER_BUNDLE = "com.alad1nks.oquturboUITests.xctrunner"
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
                if date:
                    creation = dt.datetime.strptime(date[1], "%Y-%m-%d %H:%M:%S %z").timestamp()
                    # Creation has second precision: [C,C+1) must intersect causal [kill issue, log].
                    if creation <= when and creation + 1 > issue_times[0] and issue_times[0] <= when <= issue_times[1]:
                        matching.append(when)
    if len(matching) != 1:
        raise ValueError("Missing exact same-device post-kill automatic relaunch diagnostic")


def unrelated_usage_tracking_crash(header, payload, udid):
    # Two native runs show the simulator's Apple UsageTrackingAgent trapping in
    # CloudKit during region restoration. This is a separate system process,
    # not the app, runner, SpringBoard, or notification service. Preserve its
    # report, but let the existing independent restoration readback decide
    # whether the region transaction succeeded.
    if not udid or header.get("app_name") != "UsageTrackingAgent" or header.get("is_first_party") != 1:
        return False
    expected = {"procName": "UsageTrackingAgent", "codeSigningID": "com.apple.UsageTrackingAgent",
                "parentProc": "launchd_sim", "responsibleProc": "SimulatorTrampoline",
                "coalitionName": "com.apple.CoreSimulator.SimDevice." + udid,
                "procPath": "/Volumes/VOLUME/*/UsageTracking.framework/UsageTrackingAgent"}
    if any(payload.get(key) != value for key, value in expected.items()):
        return False
    exception = payload.get("exception", {})
    if exception.get("type") != "EXC_BREAKPOINT" or exception.get("signal") != "SIGTRAP":
        return False
    thread_index = payload["faultingThread"]
    if type(thread_index) is not int or not 0 <= thread_index < len(payload["threads"]):
        return False
    thread = payload["threads"][thread_index]
    frame = thread["frames"][0]
    image_index = frame["imageIndex"]
    if type(image_index) is not int or not 0 <= image_index < len(payload["usedImages"]):
        return False
    image = payload["usedImages"][image_index]
    return thread.get("triggered") is True and image.get("CFBundleIdentifier") == "com.apple.cloudkit.CloudKit" and \
        image.get("path") == "/Volumes/VOLUME/*/CloudKit.framework/CloudKit"


def scope_crashes(paths, created, udid=None):
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
            elif unrelated_usage_tracking_crash(header, payload, udid):
                record["classification"] = "UNRELATED_USAGE_TRACKING_CLOUDKIT"
                record["codeSigningID"] = payload["codeSigningID"]
                record["coalitionName"] = payload["coalitionName"]
        except (ValueError, KeyError, TypeError, IndexError, AttributeError, OSError) as error:
            record["parseError"] = repr(error)
        if record["classification"] not in {"PREEXISTING_UNRELATED", "UNRELATED_USAGE_TRACKING_CLOUDKIT"}:
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


class StageBudget:
    # Region changes restart SpringBoard and XCTest spends most of its 240s
    # execution budget recovering. Reserve separate time to export and validate
    # the resulting diagnostics, which are required before independent readback.
    EXPORT_RESERVE = 60
    CAPS = {"initial": 300, "change": 300, "accept": 180, "restore": 300, "readback": 240}
    CLEANUP_RESERVE = CAPS["restore"] + CAPS["readback"]
    MUTATION_RESERVE = CAPS["change"] + CAPS["accept"] + CLEANUP_RESERVE

    def __init__(self, suite, total=None):
        self.started = time.monotonic()
        self.total = min(suite, total if total is not None else self.started + sum(self.CAPS.values()))
        self.suite = suite
        self.records = {}
        self.active = None

    def enter(self, name):
        now = time.monotonic()
        if name in self.records:
            raise ValueError("Stage is single-use: " + name)
        if name == "change" and self.total - now < self.MUTATION_RESERVE:
            raise TimeoutError("Insufficient mutation/cleanup reserve")
        deadline = min(now + self.CAPS[name], self.total, self.suite)
        if name == "accept":
            deadline = min(deadline, self.total - self.CLEANUP_RESERVE, self.suite - self.CLEANUP_RESERVE)
        if now >= deadline:
            raise TimeoutError("Stage/transaction deadline exhausted: " + name)
        self.active = name
        self.records[name] = {"startedMonotonic": now, "deadlineMonotonic": deadline, "capSeconds": self.CAPS[name]}
        return deadline

    def execution_deadline(self, name):
        reserve = self.EXPORT_RESERVE if name in {"change", "restore"} else 0
        return self.records[name]["deadlineMonotonic"] - reserve - 1

    def finish(self, name):
        record = self.records[name]
        now = time.monotonic()
        record["finishedMonotonic"] = now
        if now >= record["deadlineMonotonic"]:
            raise TimeoutError("Stage deadline exhausted: " + name)


def reap_owned(process, deadline):
    if process.poll() is None:
        process.kill()
        process.wait(timeout=min(1, max(0, deadline - time.monotonic())))


def provisional_setup(request, observation, issues, build_issues, text, crashes, issue_times, sessions):
    """A permitted interrupted setup is pending actual independent state verification, never a proven tap."""
    validate_observation(request, observation, {"before-confirmation", "before-region-choice"})
    if crashes:
        raise ValueError("Current or unknown crash evidence during setup")
    pid = observation.get("runnerPID")
    if type(pid) is not int or pid <= 0:
        raise ValueError("Missing operation-bound runner PID")
    restoring = request["step"] == "restore"
    expected = "Change to United States" if restoring else "Keep English (US)"
    sheet = "Applying this setting will restart your iPhone." if restoring else "Would you like to change the iPhone language to English (UK)?"
    if observation["state"] == "before-confirmation":
        if observation.get("confirmation") != expected or observation.get("confirmationSheet") != sheet:
            raise ValueError("Missing exact scoped setup intent")
    elif not restoring:
        raise ValueError("Forward setup requires exact confirmation intent")
    if observation.get("fromRegion") != ("Germany" if restoring else request.get("originalRegion")):
        raise ValueError("Unexpected setup source state")
    if request.get("target") != (request.get("originalRegion") if restoring else "Germany"):
        raise ValueError("Unexpected setup target state")
    marker = "region-" + observation["state"] + "-" + request["operation"]
    if marker not in text or '-only-testing:OquTurboUITests/LocalReminderRuntimeTests/testSystemRegionTransition' not in text:
        raise ValueError("Missing exact operation/test session")
    launch = re.findall(r'^(\d\d:\d\d:\d\d\.\d+) xcodebuild\[(\d+):\d+\] ' + re.escape(request["udid"]) +
                        r': Launched app with identifier: ' + re.escape(RUNNER_BUNDLE) + r' \(pid = (\d+)\)$', text, re.M)
    if len(launch) != 1 or launch[0][2] != str(pid):
        raise ValueError("Missing unique same-device runner launch")
    launched = scoped_clock(launch[0][0], request["created"])
    host = launch[0][1]
    deaths = re.findall(r'^(\d\d:\d\d:\d\d\.\d+) xcodebuild\[' + host + r':\d+\][^\n]*' + re.escape(request["udid"]) +
                        r'[^\n]*got death notice for pid (\d+) \(exit code: \(null\), signal: (\d+)\)', text, re.M)
    if len(deaths) != 1 or deaths[0][1:] != (str(pid), "9"):
        raise ValueError("Missing unique matching runner SIGKILL")
    death = scoped_clock(deaths[0][0], request["created"])
    if not launched <= observation["observed"] < death or not issue_times or not death < issue_times[0] <= request["created"] + 240:
        raise ValueError("Setup launch/intent/death/issue order disagrees")
    validate_setup_issues(request, issues, build_issues, issue_times, sessions, death, host)
    if re.search(r"Assertion Failure|XCTAssert[^\n]*failed|Test timed out|test execution[^\n]*exceeded|Test Case [^\n]*failed", "\n".join(sessions), re.I):
        raise ValueError("Unexpected failure in setup diagnostics")
    # Incomplete debug records are not contradictions and never prove dispatch completion.
    tap = 'Tap "' + (request["target"] if observation["state"] == "before-region-choice" else expected) + '" ' + ("StaticText" if observation["state"] == "before-region-choice" else "Button")
    if observation["state"] == "before-region-choice":
        completed_dispatch(request, observation, text, tap)
    else:
        pattern = re.compile(r'^(\d\d:\d\d:\d\d\.\d+) OquTurboUITests-Runner\[(\d+):(\d+)\] '
                             r'<XCTContext: (0x[0-9a-f]+)> (started|finished) activity '
                             r'<XCActivityRecord: (0x[0-9a-f]+)> (\d{4}-\d\d-\d\d) \d\d:\d\d:\d\d \+0000: (.+)$', re.M)
        activities = []
        for match in pattern.finditer(text):
            clock, actor, thread, context, action, activity, date, title = match.groups()
            when = dt.datetime.fromisoformat(date + "T" + clock + "+00:00").timestamp()
            activities.append((when, actor, thread, context, action, activity, title))
        starts = [a for a in activities if a[4] == "started" and a[6] == tap]
        if len(starts) > 1:
            raise ValueError("Ambiguous observed setup taps")
        if starts:
            start = starts[0]
            if start[1] != str(pid) or not observation["observed"] <= start[0] < death:
                raise ValueError("Contradictory setup tap identity/time")
            syntheses = [a for a in activities if a[4] == "started" and a[6] == "Synthesize event" and start[0] < a[0] < death]
            if len(syntheses) > 1 or any(a[1:4] != start[1:4] for a in syntheses):
                raise ValueError("Ambiguous observed setup synthesis")
            if syntheses:
                synthesis = syntheses[0]
                # XCActivityRecord is an object address, not a session-wide ID.
                # XCTest may reuse it after an earlier activity has finished.
                finishes = [a for a in activities if a[4] == "finished" and a[5] == synthesis[5] and a[0] >= synthesis[0]]
                if len(finishes) > 1 or any(a[1:4] != start[1:4] or not synthesis[0] < a[0] < death or
                                           not a[6].startswith("Synthesize event (") for a in finishes):
                    raise ValueError("Contradictory observed synthesis completion")
    # An available conflicting post-intent gesture is contradictory, even if the expected activity is missing.
    for line in text.splitlines():
        match = re.match(r'^(\d\d:\d\d:\d\d\.\d+) OquTurboUITests-Runner\[(\d+):\d+\].*> started activity .*: ((?:Tap|Swipe|Press|Type) .+)$', line)
        if match and scoped_clock(match[1], request["created"]) >= observation["observed"] and (match[2] != str(pid) or match[3] != tap):
            raise ValueError("Contradictory setup gesture evidence")
    return "SETUP_INTERRUPTED_PENDING_READBACK"


def settle_setup(outcomes, setup, verification, request, observation):
    pending = next((row for row in outcomes if row["step"] == setup), None)
    if pending is None or pending["status"] not in {"SUCCESS", "SETUP_INTERRUPTED_PENDING_READBACK"}:
        raise ValueError("No eligible setup operation to verify")
    if request.get("verifiesOperation") != pending["operation"] or observation.get("verifiesOperation") != pending["operation"]:
        raise ValueError("Readback does not verify the exact setup operation")
    actual = next((row for row in outcomes if row["operation"] == request["operation"]), None)
    if actual is None or actual["step"] != verification or actual["status"] != "SUCCESS":
        raise ValueError("Verification did not finish normally")
    validate_observation(request, observation, {"accepted-24-hour" if setup == "change" else "restored"})
    expected = "Germany" if setup == "change" else request["originalRegion"]
    if observation.get("verifiedRegion") != expected or observation.get("verifiedLanguage") != "en-US":
        raise ValueError("Setup postcondition not independently observed")
    pending["verifiedBy"] = request["operation"]
    pending["verificationStep"] = verification
    pending["postcondition"] = "VERIFIED_BY_NORMAL_READBACK"


def run_locale(runner, directory, snapshots, baseline, boundary, cutoff, stages=None):
    # Runner.run and all nested simctl/export calls use this same existing phase cutoff.
    suite_deadline = runner.deadline
    stages = stages or StageBudget(suite_deadline, cutoff)
    if "initial" not in stages.records:
        cutoff = stages.enter("initial")
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
        test_run = generated.GeneratedTestRun(products.parent, identity, runner.run("git", "rev-parse", "HEAD"),
                                              directory / "generated-test-run", runner.deadline)

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
            nonlocal cutoff
            if name != "initial":
                cutoff = stages.enter(name)
                runner.deadline = cutoff
                test_run.cutoff = cutoff
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
            binding_started = time.monotonic()
            command = test_run.command(runner.udid, method, result)
            timing = {"bindingStartedMonotonic": binding_started, "command": command, "specification": test_run.state["specification"], "cutoffMonotonic": cutoff,
                      "startedMonotonic": time.monotonic(), "samples": []}
            stage_cutoff = cutoff
            cutoff = stages.execution_deadline(name)  # Preserve export and owned-process reap reserves.
            runner.deadline = cutoff
            timing["executionDeadlineMonotonic"] = cutoff
            budget()
            with (location / "xcodebuild.log").open("w") as log:
                runner.process = subprocess.Popen(command, cwd=runner.root, stdout=log, stderr=subprocess.STDOUT)
                try:
                    while runner.process.poll() is None:
                        budget()
                        sample_started = time.monotonic()
                        try:
                            sample()
                        finally:
                            timing["samples"].append({"startedMonotonic": sample_started, "finishedMonotonic": time.monotonic()})
                        time.sleep(min(2, max(0, cutoff - time.monotonic())))
                    timing["processCompletionObservedMonotonic"] = time.monotonic()
                    budget()
                    code = runner.process.returncode
                    outcome["code"] = code
                finally:
                    cutoff = stage_cutoff
                    runner.deadline = cutoff
                    # Failure to reap leaves the pointer intact; another invocation cannot start.
                    reap_owned(runner.process, cutoff)
                    runner.process = None
                    timing["finishedMonotonic"] = time.monotonic()
                    timing["completionMeaning"] = "Host poll observation, not exact OS exit time"
                    (location / "invocation-timing.json").write_text(json.dumps(timing, indent=2))
            budget()
            observation_path = runner_documents(location, "observation") / "region-observation.json"
            observation = json.loads(observation_path.read_text()) if observation_path.exists() else {}
            (location / "observation.json").write_text(json.dumps(observation, indent=2))
            runner.run("xcrun", "xcresulttool", "export", "attachments", "--path", str(result), "--output-path", str(location / "attachments"))
            if code == 0:
                summary = json.loads(runner.run("xcrun", "xcresulttool", "get", "test-results", "summary", "--path", str(result)))
                (location / "summary.json").write_text(json.dumps(summary, indent=2))
                runner.evidence.validate_summary(summary)
                with closing(sqlite3.connect("file:" + str(result / "database.sqlite3") + "?mode=ro", uri=True)) as connection:
                    if list(connection.execute("SELECT compactDescription FROM TestIssues")) or list(connection.execute("SELECT message FROM BuildIssues")):
                        raise ValueError("Normally successful XCTest contains test/build issues")
                states = {"baseline"} if name == "initial" else {"accepted-24-hour"} if name == "accept" else {"restored"} if name == "readback" else {"transition-completed", "already-restored"}
                validate_observation(request, observation, states)
                status = "SUCCESS"
            elif not transition:
                raise RuntimeError("Actual acceptance XCTest failed: " + name)
            if transition:
                diagnostic = location / "diagnostics"
                runner.run("xcrun", "xcresulttool", "export", "diagnostics", "--path", str(result), "--output-path", str(diagnostic))
                database = result / "database.sqlite3"
                with closing(sqlite3.connect("file:" + str(database) + "?mode=ro", uri=True)) as connection:
                    rows = list(connection.execute("SELECT compactDescription, timestamp FROM TestIssues ORDER BY timestamp"))
                    issues = [v[0] for v in rows]
                    issue_times = [v[1] + 978307200 for v in rows]
                    build_issues = [v[0] for v in connection.execute("SELECT message FROM BuildIssues")]
                sessions = list(diagnostic.rglob("Session-*.log"))
                if not sessions:
                    raise ValueError("Missing exported setup diagnostic sessions")
                texts = [p.read_text(errors="replace") for p in sessions]
                matching = [text for text in texts if "region-" + observation.get("state", "") + "-" + request["operation"] in text]
                if code != 0 and len(matching) != 1:
                    raise ValueError("Ambiguous setup diagnostic sessions")
                text = matching[0] if matching else ""
                crashes = list(diagnostic.rglob("*.ips")) + list(diagnostic.rglob("*.crash"))
                crash_records, current_crashes = scope_crashes(crashes, request["created"], request["udid"])
                (location / "crash-scope.json").write_text(json.dumps(crash_records, indent=2))
                if current_crashes:
                    raise ValueError("Current or unknown crash evidence during setup")
                if code == 0:
                    if issues or build_issues:
                        raise ValueError("Normally completed setup contains issues")
                elif code == 65:
                    status = provisional_setup(request, observation, issues, build_issues, text, current_crashes,
                                               issue_times, texts)
                else:
                    raise ValueError("Unknown interrupted setup exit code")
            if unchanged:
                after_preferences = current_preferences(location, "after")
                (location / "after.preferences_pb").write_bytes(after_preferences)
                if after_preferences != before_preferences:
                    raise ValueError("Hour-cycle Cancel/readback changed application preferences")
            budget()
            outcome["status"] = status
            try:
                if name in {"accept", "readback"}:
                    settle_setup(outcomes, "change" if name == "accept" else "restore", name, request, observation)
                (directory / "region-substeps.json").write_text(json.dumps(outcomes, indent=2))
                if name in {"change", "accept", "restore"}:
                    stages.finish(name)
            except Exception:
                outcome["status"] = "FAILED_OR_INCOMPLETE"
                raise
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
            stages.finish("initial")
            if stages.total - time.monotonic() < stages.MUTATION_RESERVE:
                raise TimeoutError("Insufficient mutation/cleanup reserve")
            restore_needed = True  # Set before dispatch of the first mutating helper.
            step("change", "testSystemRegionTransition", dict(common, target="Germany"), transition=True)
            step("accept", "testSystem24HourAcceptance", dict(common, verifiesOperation=outcomes[-1]["operation"]))
        except Exception as error:
            primary = repr(error)
        finally:
            if restore_needed:
                try:
                    common = {key: initial[key] for key in ("originalRegion", "originalLanguage", "minutes", "wheels")}
                    step("restore", "testSystemRegionTransition", dict(common, target=initial["originalRegion"]), transition=True)
                    step("readback", "testSystemRegionRestored", dict(common, verifiesOperation=outcomes[-1]["operation"]))
                    if primary:
                        stages.finish("readback")
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
        if any(row.get("status") == "SETUP_INTERRUPTED_PENDING_READBACK" and not row.get("verifiedBy") for row in outcomes):
            raise ValueError("Unresolved provisional setup")
        (directory / "validation.json").write_text(json.dumps(report, indent=2))
        stages.finish("readback")
        return report
    finally:
        (directory / "stage-budgets.json").write_text(json.dumps({"transactionStartedMonotonic": stages.started,
            "transactionDeadlineMonotonic": stages.total, "stages": stages.records}, indent=2))
        (directory / "snapshots.json").write_text(json.dumps(snapshots, indent=2))
        (directory / "region-substeps.json").write_text(json.dumps(outcomes, indent=2))
        runner.deadline = suite_deadline
        if sys.exc_info()[0] is None and time.monotonic() >= min(cutoff, stages.total):
            raise TimeoutError("Final locale evidence exceeded stage/transaction deadline")
