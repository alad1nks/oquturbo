#!/usr/bin/env python3
"""Bounded real-UI reminder acceptance. Never injects an alarm/notification or grants permission.

Requires an already booted, isolated API33 KVM AVD and the reviewed debug APK.
A timed failure leaves later episodes NOT_RUN and exits nonzero, with partial evidence.
"""
import argparse
import datetime as dt
import hashlib
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from zoneinfo import ZoneInfo
from pathlib import Path

PACKAGE = "com.alad1nks.oquturbo"
PROCESS_ABSENT = "OQUTURBO_PROCESS_ABSENT"
PIDOF_COMMAND = f"""reminder_pids=$(pidof {PACKAGE} 2>&1); reminder_status=$?
if [ "$reminder_status" -eq 1 ] && [ -z "$reminder_pids" ]; then
    printf '%s\\n' '{PROCESS_ABSENT}'
elif [ "$reminder_status" -eq 0 ] && [ -n "$reminder_pids" ]; then
    printf '%s\\n' "$reminder_pids"
else
    printf '%s\\n' "$reminder_pids" >&2
    exit 1
fi"""
CHANNEL = "oquturbo_practice_reminder"
ACTION = PACKAGE + ".PRACTICE_REMINDER"
PREFS = "files/oquturbo.preferences_pb"
MUTABLE_KEYS = {"reminders_enabled", "reminders_schedule_v1", "language", "daily_training_v1"}


def protobuf_fields(data):
    """Read-only wire parser: preserve unknown preference values byte-for-byte."""
    index = 0
    def varint():
        nonlocal index
        value = 0
        for shift in range(0, 70, 7):
            if index >= len(data): raise ValueError("Truncated varint")
            byte = data[index]; index += 1
            value |= (byte & 127) << shift
            if not byte & 128: return value
        raise ValueError("Oversized varint")
    while index < len(data):
        tag = varint(); number, wire = tag >> 3, tag & 7
        if not number: raise ValueError("Invalid field")
        if wire == 0: value = varint()
        elif wire in (1, 2, 5):
            size = varint() if wire == 2 else (8 if wire == 1 else 4)
            if index + size > len(data): raise ValueError("Truncated field")
            value = data[index:index + size]; index += size
        else: raise ValueError("Unsupported wire type")
        yield number, wire, value


def preferences(data):
    result = {}
    for field, wire, entry in protobuf_fields(data):
        if field != 1 or wire != 2: raise ValueError("Unexpected preferences envelope")
        values = {f: v for f, _, v in protobuf_fields(entry)}
        key = values[1].decode("utf-8")
        if key in result: raise ValueError("Duplicate preference key")
        result[key] = values[2].hex()
    return result


def preference_string(snapshot, key):
    fields = {field: value for field, _, value in protobuf_fields(bytes.fromhex(snapshot[key]))}
    return fields[5].decode("utf-8")


def pending_alarms(dump):
    headers = list(re.finditer(r"(?m)^\s*(?:RTC_WAKEUP|RTC|ELAPSED_WAKEUP|ELAPSED) #\d+: Alarm\{[^\n]*", dump))
    found = []
    for i, header in enumerate(headers):
        end = headers[i + 1].start() if i + 1 < len(headers) else header.end() + 1200
        block = dump[header.start():min(end, header.end() + 1200)]
        if PACKAGE in header.group() and "tag=*walarm*:" + ACTION in block:
            found.append(block)
    return found


def alarm_epoch(alarm):
    # API33 header stores epoch after a space; the detail origWhen= is a formatted date.
    header = alarm.lstrip().splitlines()[0]
    match = re.fullmatch(r"RTC_WAKEUP #\d+: Alarm\{[^\n]*\borigWhen (\d+)\b[^\n]* " + re.escape(PACKAGE) + r"\}", header)
    assert match, "Unrecognized owned RTC alarm header"
    return int(match[1])


def assert_inexact_alarm(alarm):
    alarm_epoch(alarm)  # Require the owned RTC_WAKEUP header, not history or another package.
    details = re.findall(r"(?m)^\s*type=RTC_WAKEUP origWhen=[^\n]+$", alarm)
    assert len(details) == 1, "Missing or ambiguous owned alarm details"
    detail = details[0]
    window = re.search(r"\bwindow=(0|\+(?:\d+(?:ms|d|h|m|s))+) ", detail)
    flags = re.search(r"\bflags=0x([0-9a-fA-F]+)$", detail)
    assert window and flags and " repeatInterval=0 " in detail, "Unrecognized owned alarm details"
    # Android13 AlarmManagerService marks caller-requested exact alarms FLAG_STANDALONE
    # before setImpl derives the heuristic window. AlarmManager.set near due (<10s)
    # can therefore have window=0 without exact access/reason or any special flags.
    assert int(flags[1], 16) == 0 and "exactAllowReason=" not in detail, "Exact or special alarm unexpectedly present"


def assert_no_exact_alarm_permissions(dump):
    packages = re.findall(r"(?m)^  Package \[([^\]]+)\] .*:$", dump)
    assert packages == [PACKAGE] and "    requested permissions:\n" in dump, "Unrecognized package permission inventory"
    assert not re.search(r"android\.permission\.(?:SCHEDULE_EXACT_ALARM|USE_EXACT_ALARM)\b", dump), "Exact alarm permission unexpectedly present"


def display_override(output, kind):
    matches = re.findall(r"^Override " + kind + r": (.+)$", output, re.M)
    assert len(matches) <= 1, "Ambiguous display override"
    assert re.search(r"^Physical " + kind + r": .+$", output, re.M), "Missing physical display inventory"
    return matches[0] if matches else "reset"


def resumed_activity(dump):
    """API33 activity inventory, scoped to the actual unique top resumed record."""
    assert dump.startswith("ACTIVITY MANAGER ACTIVITIES"), "Unknown activity inventory"
    records = re.findall(r"(?m)^\s*topResumedActivity=(ActivityRecord\{[^\n]+)", dump)
    assert len(records) <= 1, "Ambiguous resumed activity"
    if not records: return None, None
    component = re.search(r" u0 ([\w.]+/[\w.]+)[ } ]", records[0])
    assert component, "Unknown resumed component"
    sections = re.split(r"(?m)^\s*\* Hist  #\d+: ", dump)[1:]
    matches = [section for section in sections if section.splitlines()[0] == records[0]]
    assert len(matches) == 1, "Resumed activity detail missing or ambiguous"
    block = matches[0]
    assert re.search(r"(?m)^\s*state=RESUMED\b", block), "Top record is not resumed"
    font = re.search(r"(?m)^\s*CurrentConfiguration=\{([0-9.]+) ", block)
    assert font, "Resumed activity configuration unavailable"
    return component[1], float(font[1])


def visible_bounds(node, width=320, height=640):
    assert node is not None and node.get("package") == PACKAGE, "Missing owned visible element"
    values = list(map(int, re.findall(r"-?\d+", node.get("bounds", ""))))
    assert len(values) == 4 and 0 <= values[0] < values[2] <= width and 0 <= values[1] < values[3] <= height, "Clipped or missing compact bounds"
    assert node.get("enabled") == "true", "Disabled compact element"
    return values


def twelve_hour_minutes(value):
    match = re.fullmatch(r"(1[0-2]|[1-9]):([0-5][0-9])\s*([AP]M)", value.strip())
    assert match, "Saved card does not expose a complete 12h time"
    return (int(match[1]) % 12 + (12 if match[3] == "PM" else 0)) * 60 + int(match[2])


class Driver:
    def __init__(self, serial, output):
        self.serial, self.output = serial, output
        self.output.mkdir(parents=True, exist_ok=True)
        self.step = 0
        self.deadline = time.monotonic() + 305 * 60
        self.baseline = None

    def adb(self, *args, timeout=40, check=True, alarm_help=False):
        if alarm_help:
            assert check and args == ("shell", "cmd", "alarm", "help"), "Help status policy is limited to cmd alarm help"
        if time.monotonic() >= self.deadline: raise TimeoutError("Whole native acceptance deadline exceeded")
        result = subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, timeout=timeout)
        with (self.output / "commands.log").open("a") as log:
            log.write(json.dumps({"time": time.time(), "argv": list(args), "code": result.returncode}) + "\n")
        if alarm_help:
            stdout, stderr = result.stdout.decode(errors="replace"), result.stderr.decode(errors="replace")
            (self.output / "alarm-help.json").write_text(json.dumps({"code": result.returncode, "stdout": stdout, "stderr": stderr}, indent=2))
            lines = [line.strip() for line in stdout.splitlines()]
            # API33 BasicShellCommandHandler prints help then returns -1 (ADB reports255).
            if result.returncode not in (0, 255) or stderr.strip() or not lines or \
                    lines[0] != "Alarm manager service (alarm) commands:" or \
                    not {"set-time TIME", "set-timezone TZ"}.issubset(lines):
                raise RuntimeError(f"Unverified alarm help response (exit {result.returncode}); see alarm-help.json")
        elif check and result.returncode:
            raise RuntimeError(result.stderr.decode(errors="replace"))
        return result.stdout

    def text(self, *args, **kwargs):
        return self.adb(*args, **kwargs).decode(errors="replace").strip()

    def stop_background_process(self, label, before_due=None):
        # am kill only kills background processes; HOME/receiver completion is asynchronous.
        deadline = min(self.deadline, time.monotonic() + 30)
        while time.monotonic() < deadline:
            def remaining():
                seconds = deadline - time.monotonic()
                if seconds <= 0:
                    raise TimeoutError("Normal background kill did not remove the process within 30s")
                return min(5, seconds)

            def require_before_due():
                if before_due is not None:
                    now = int(self.text("shell", "date", "+%s", timeout=remaining()))
                    if now >= before_due:
                        raise AssertionError("Cold-process setup missed the chosen due time")

            require_before_due()
            self.adb("shell", "am", "kill", PACKAGE, timeout=remaining())
            # pidof exit1 means absent; transport/other command failures must not look like absence.
            observation = self.text("shell", PIDOF_COMMAND, timeout=remaining())
            assert observation == PROCESS_ABSENT or re.fullmatch(r"[0-9]+(?: [0-9]+)*", observation), "Unknown process inventory"
            pids = "" if observation == PROCESS_ABSENT else observation
            with (self.output / f"{label}-process-stop.jsonl").open("a") as log:
                log.write(json.dumps({"monotonic": time.monotonic(), "pids": pids, "before_due": before_due}) + "\n")
            require_before_due()
            if not pids:
                remaining()
                return
            time.sleep(min(1, remaining()))
        raise TimeoutError("Normal background kill did not remove the process within 30s")

    def snapshot(self, label):
        self.step += 1
        stem = self.output / f"{self.step:03d}-{label}"
        self.adb("shell", "uiautomator", "dump", "/sdcard/oquturbo-reminders.xml")
        xml = self.adb("exec-out", "cat", "/sdcard/oquturbo-reminders.xml")
        stem.with_suffix(".xml").write_bytes(xml)
        stem.with_suffix(".png").write_bytes(self.adb("exec-out", "screencap", "-p"))
        return ET.fromstring(xml)

    def find(self, root, text=None, resource=None, description=None):
        matches = [n for n in root.iter("node") if
                   (text is None or n.get("text") == text) and
                   (resource is None or n.get("resource-id") == resource) and
                   (description is None or n.get("content-desc") == description)]
        if len(matches) > 1:
            raise AssertionError(f"Ambiguous UI control: {text or resource or description}")
        return matches[0] if matches else None

    def tap(self, text=None, resource=None, description=None, scroll=False, native_time_picker=False):
        for attempt in range(9 if scroll else 3):
            root = self.snapshot("find-control")
            if native_time_picker:
                assert resource in {"android:id/button1", "android:id/button2"}
                picker = self.find(root, resource="android:id/timePicker")
                if picker is None or picker.get("class") != "android.widget.TimePicker" or picker.get("package") != PACKAGE:
                    time.sleep(1)
                    continue
            node = self.find(root, text, resource, description)
            if native_time_picker and node is not None:
                assert node.get("package") == PACKAGE and node.get("class") == "android.widget.Button"
                assert node.get("enabled") == "true" and node.get("clickable") == "true"
            if node is not None:
                coords = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                if len(coords) == 4 and coords[2] > coords[0] and coords[3] > coords[1]:
                    self.adb("shell", "input", "tap", str((coords[0]+coords[2])//2), str((coords[1]+coords[3])//2))
                    time.sleep(.7)
                    return
            if scroll:
                bounds = list(map(int, re.findall(r"\d+", next(root.iter("node")).get("bounds", ""))))
                if len(bounds) != 4: raise AssertionError("No viewport bounds")
                left, top, right, bottom = bounds
                self.adb("shell", "input", "swipe", str((left + right)//2), str(top + (bottom-top)*3//4),
                         str((left + right)//2), str(top + (bottom-top)//4), "400")
            else:
                time.sleep(1)
        raise AssertionError(f"Control unavailable: {text or resource or description}")

    def assert_text(self, text):
        assert self.find(self.snapshot("assert-state"), text=text) is not None, text

    def launch(self):
        self.adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
        time.sleep(2)

    def settings(self):
        self.tap(text="Profile")
        self.tap(description="Settings")

    def read_diagnostics(self):
        return self.text("exec-out", "run-as", PACKAGE, "cat", "files/reminder-diagnostics.log", check=False)

    def native(self, label):
        self.step += 1
        label = f"{self.step:03d}-{label}"
        for service in ["alarm", "notification", "package", "power", "deviceidle"]:
            args = ["shell", "dumpsys", service]
            if service == "notification": args += ["--noredact"]
            if service == "package": args += [PACKAGE]
            (self.output / f"{label}-{service}.txt").write_bytes(self.adb(*args))
        (self.output / f"{label}-events.txt").write_text(self.read_diagnostics())
        (self.output / f"{label}-clock.txt").write_text(self.text("shell", "date", "+%s %F %T %Z") + "\n" + self.text("shell", "getprop", "persist.sys.timezone"))

    def future_picker(self, first=False, cancel=False):
        self.tap(text="Choose time and enable" if first else "Change time", scroll=True)
        if cancel:
            self.tap(resource="android:id/button2", native_time_picker=True)
            return None
        now = int(self.text("shell", "date", "+%s"))
        target = ((now // 60) + 3) * 60
        # Baseline delivery runs in UTC, configured by the isolated runner. Travel is a separate case.
        chosen = dt.datetime.fromtimestamp(target, dt.timezone.utc)
        root = self.snapshot("native-picker")
        if self.find(root, resource="android:id/input_hour") is None:
            self.tap(resource="android:id/toggle_mode")
        for resource, value in [("android:id/input_hour", chosen.hour), ("android:id/input_minute", chosen.minute)]:
            self.tap(resource=resource)
            self.adb("shell", "input", "keyevent", "KEYCODE_MOVE_END", "KEYCODE_DEL", "KEYCODE_DEL")
            self.adb("shell", "input", "text", str(value))
        if int(self.text("shell", "date", "+%s")) >= target - 30:
            self.tap(resource="android:id/button2", native_time_picker=True)
            return self.future_picker(first=first)
        self.tap(resource="android:id/button1", native_time_picker=True)
        (self.output / f"chosen-{self.step}.json").write_text(json.dumps({"device_now": now, "due": target, "minutes": chosen.hour*60+chosen.minute}))
        if not first:
            self.wait_edited_schedule(target, chosen.hour * 60 + chosen.minute)
        return target

    def wait_edited_schedule(self, due, minute):
        # Existing enabled edits are asynchronous. Initial enable must first allow the OS prompt.
        deadline = min(self.deadline, time.monotonic() + 30)
        def remaining():
            seconds = deadline - time.monotonic()
            if seconds <= 0:
                raise TimeoutError("Edited reminder did not persist and replace its alarm within 30s")
            return seconds
        while time.monotonic() < deadline:
            assert int(self.text("shell", "date", "+%s", timeout=remaining())) < due, "Edit confirmation missed the chosen due"
            saved = self.prefs("await-edited-schedule", timeout=remaining())
            schedule = json.loads(preference_string(saved, "reminders_schedule_v1"))
            events = self.text("exec-out", "run-as", PACKAGE, "cat", "files/reminder-diagnostics.log", timeout=remaining())
            dump = self.text("shell", "dumpsys", "alarm", timeout=remaining())
            self.output.joinpath(f"{self.step:03d}-edited-events.txt").write_text(events)
            self.output.joinpath(f"{self.step:03d}-edited-alarm.txt").write_text(dump)
            alarms = pending_alarms(dump)
            assert len(alarms) <= 1, "Edited reminder left duplicate owned alarms"
            if alarms: assert_inexact_alarm(alarms[0])
            accepted = re.findall(r"scheduled target=(\d+) minutes=(\d+)", events)
            assert int(self.text("shell", "date", "+%s", timeout=remaining())) < due, "Edit confirmation missed the chosen due"
            if saved.get("reminders_enabled") == "0801" and schedule.get("version") == 1 and schedule.get("minutesOfDay") == minute and \
                    accepted and tuple(map(int, accepted[-1])) == (due * 1000, minute) and len(alarms) == 1 and \
                    alarm_epoch(alarms[0]) == due * 1000:
                return
            time.sleep(min(1, remaining()))
        raise TimeoutError("Edited reminder did not persist and replace its alarm within 30s")

    def wait_delivery(self, due, foreground=False):
        end = min(self.deadline, time.monotonic() + max(0, due-int(self.text("shell", "date", "+%s"))) + 65*60)
        before = self.read_diagnostics()
        while time.monotonic() < end:
            events = self.read_diagnostics()[len(before):]
            if foreground and re.search(r" dispatch foreground=true current=true sameDate=true", events):
                assert " posted " not in events, "Foreground receiver posted a notification"
                assert not self.owned_notification(), "Foreground dispatch left an owned notification"
                self.assert_alarm(1)
                self.native("foreground-dispatched")
                self.snapshot("foreground-no-banner")
                return
            record = self.text("shell", "dumpsys", "notification", "--noredact")
            if not foreground and self.owned_notification(record):
                posts = re.findall(r"(?m)^(\d+) posted id=6001$", events)
                assert posts and int(posts[-1]) >= due * 1000, "Delivered record is not this future occurrence"
                self.adb("shell", "cmd", "statusbar", "expand-notifications")
                self.assert_text("Practice in OquTurbo")
                self.native("delivered")
                return
            print(json.dumps({"waiting": "foreground dispatch" if foreground else "real local notification", "device_epoch": self.text("shell", "date", "+%s"), "due": due}), flush=True)
            time.sleep(30)
        raise TimeoutError("No verified production inexact delivery/dispatch within due+65min; inspect power/OS evidence")

    def tap_notification(self):
        before = self.read_diagnostics().count(" open-consumed ")
        self.tap(text="Practice in OquTurbo")
        deadline = time.monotonic()+30
        while time.monotonic() < deadline:
            events = self.read_diagnostics()
            if events.count(" open-consumed ") == before+1:
                assert "destination=Home" in events
                self.snapshot("notification-home")
                self.invariant("after-real-card-tap")
                time.sleep(2)
                assert self.read_diagnostics().count(" open-consumed ") == before + 1
                return
            time.sleep(1)
        raise AssertionError("Real notification tap was not consumed once by Home graph")

    def reset_fixture(self, label):
        if self.baseline is not None: self.invariant(label + "-before-explicit-fixture-reset")
        self.native(label + "-before-reset")
        self.adb("shell", "pm", "clear", PACKAGE)
        self.adb("shell", "run-as", PACKAGE, "mkdir", "-p", "files")
        self.adb("shell", "run-as", PACKAGE, "touch", "files/reminder-diagnostics-enabled")
        self.launch(); self.settings()
        self.baseline = self.prefs(label + "-new-baseline")

    def prefs(self, label, timeout=40):
        self.step += 1
        label = f"{self.step:03d}-{label}"
        data = self.adb("exec-out", "run-as", PACKAGE, "cat", PREFS, timeout=timeout)
        self.output.joinpath(label + ".preferences_pb").write_bytes(data)
        decoded = preferences(data)
        self.output.joinpath(label + "-preferences.json").write_text(json.dumps(decoded, indent=2))
        return decoded

    def invariant(self, label):
        actual = self.prefs(label)
        expected = {k: v for k, v in self.baseline.items() if k not in MUTABLE_KEYS}
        assert expected == {k: v for k, v in actual.items() if k not in MUTABLE_KEYS}, "Non-reminder persisted data changed"
        return actual

    def owned_notification(self, record=None):
        record = record if record is not None else self.text("shell", "dumpsys", "notification", "--noredact")
        # Only active NotificationRecord lines, not history or a registered channel.
        return re.search(r"NotificationRecord\([^\n]*pkg=" + re.escape(PACKAGE) +
                         r"\s[^\n]*id=6001\b[^\n]*tag=" + CHANNEL + r"\b", record) is not None

    def assert_alarm(self, count):
        dump = self.text("shell", "dumpsys", "alarm")
        alarms = pending_alarms(dump)
        self.output.joinpath(f"{self.step:03d}-alarm-inventory.txt").write_text(dump)
        assert len(alarms) == count, f"Owned pending alarms: {len(alarms)}, expected {count}"
        if count:
            assert_inexact_alarm(alarms[0])
        return alarms

    def next_schedule(self):
        events = re.findall(r"scheduled target=(\d+) minutes=(\d+)", self.read_diagnostics())
        assert events, "No accepted production schedule event"
        target, minute = map(int, events[-1])
        assert target > int(self.text("shell", "date", "+%s")) * 1000
        zone = ZoneInfo(self.text("shell", "getprop", "persist.sys.timezone"))
        local = dt.datetime.fromtimestamp(target / 1000, zone)
        assert local.hour * 60 + local.minute == minute
        alarms = self.assert_alarm(1)
        assert alarm_epoch(alarms[0]) == target, "OS inventory does not match the accepted epoch"
        return target, minute

    def back(self):
        self.adb("shell", "input", "keyevent", "KEYCODE_BACK"); time.sleep(1)

    def dismiss_notification_prompt(self):
        # Saving the picker and showing the OS prompt are asynchronous; Back must target the prompt.
        permission_package = "com.android.permissioncontroller"
        for _ in range(10):
            root = self.snapshot("await-notification-prompt")
            message = self.find(root, resource=permission_package + ":id/permission_message")
            buttons = [self.find(root, resource=permission_package + ":id/" + name)
                       for name in ("permission_allow_button", "permission_deny_button")]
            if message is not None and message.get("package") == permission_package and message.get("text") == "Allow OquTurbo to send you notifications?" and all(
                node is not None and node.get("package") == permission_package and
                node.get("enabled") == "true" and node.get("clickable") == "true" and
                len(coords := list(map(int, re.findall(r"\d+", node.get("bounds", ""))))) == 4 and
                coords[2] > coords[0] and coords[3] > coords[1] for node in buttons
            ):
                self.back()  # Exactly one real dismissal, only after the actual prompt is visible.
                break
            time.sleep(1)
        else:
            raise AssertionError("Actual notification permission prompt did not appear; Back not sent")
        for _ in range(10):
            root = self.snapshot("notification-prompt-dismissed")
            settings = self.find(root, text="Settings")
            if not any(node.get("package") == permission_package for node in root.iter("node")) and \
                    settings is not None and settings.get("package") == PACKAGE:
                return
            time.sleep(1)
        raise AssertionError("Permission dismissal did not return to Settings")

    def ensure_settings_top(self):
        # Reopen the real existing profile child; no synthetic product route.
        self.back(); self.tap(description="Settings")

    def notification_settings(self, channel=False):
        action = "android.settings.CHANNEL_NOTIFICATION_SETTINGS" if channel else "android.settings.APP_NOTIFICATION_SETTINGS"
        args = ["shell", "am", "start", "-W", "-a", action, "--es", "android.provider.extra.APP_PACKAGE", PACKAGE]
        if channel: args += ["--es", "android.provider.extra.CHANNEL_ID", CHANNEL]
        self.adb(*args); time.sleep(1)

    def notification_switch(self, root, channel=False):
        # API33 also exposes independent badge/channel switches after the master is enabled.
        bar = self.find(root, resource="com.android.settings:id/main_switch_bar")
        assert bar is not None and bar.get("package") == "com.android.settings", "Missing native notification main switch bar"
        label = self.find(bar, resource="com.android.settings:id/switch_text")
        expected = "Show notifications" if channel else "All OquTurbo notifications"
        assert label is not None and label.get("text") == expected, "Wrong app/channel notification control"
        node = self.find(bar, resource="android:id/switch_widget")
        assert node is not None and node.get("package") == "com.android.settings" and \
            node.get("class") == "android.widget.Switch" and node.get("checkable") == "true" and \
            node.get("enabled") == "true" and node.get("checked") in {"true", "false"}, "Unreadable native notification switch"
        return node

    def toggle_system(self, enabled, channel=False):
        node = self.notification_switch(self.snapshot("system-notification-control"), channel)
        if (node.get("checked") == "true") != enabled:
            bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
            assert len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1], "Notification switch has no visible bounds"
            self.adb("shell", "input", "tap", str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
            time.sleep(1)
        current = self.notification_switch(self.snapshot("system-notification-changed"), channel)
        assert (current.get("checked") == "true") == enabled, "Native notification switch did not reach requested state"

    def verify_block_and_recover(self, channel=False):
        before = self.prefs("before-block")
        self.notification_settings(channel); self.toggle_system(False, channel); self.back()
        self.tap(text="Notifications are blocked by the system.", scroll=True)
        self.assert_alarm(0)
        self.snapshot("no-automatic-permission-prompt")
        self.tap(text="Notification settings", scroll=True)
        if channel:
            # App settings opened by the product; use the actual channel row.
            self.tap(text="Practice reminders")
        self.toggle_system(True, channel); self.back()
        if channel: self.back()
        self.tap(text="Scheduled", scroll=True); self.assert_alarm(1)
        after = self.prefs("after-block-recovery")
        assert before["reminders_schedule_v1"] == after["reminders_schedule_v1"]

    def change_language_roundtrip(self):
        self.ensure_settings_top(); self.tap(text="Language"); self.tap(text="Русский")
        time.sleep(2)
        snapshot = self.prefs("russian-content-snapshot")
        content = json.loads(preference_string(snapshot, "reminders_schedule_v1"))["content"]
        assert content["languageCode"] == "ru"
        self.tap(text="Язык"); self.tap(text="English"); time.sleep(2)
        self.assert_alarm(1)

    def compact_picker_check(self, root, minute):
        picker = self.find(root, resource="android:id/timePicker")
        assert picker is not None and picker.get("package") == PACKAGE, "Native time picker unavailable"
        expected = {"android:id/message": "Құрылғының ағымдағы жергілікті уақыты бойынша.",
                    "android:id/button1": "УАҚЫТТЫ САҚТАУ", "android:id/button2": "БАС ТАРТУ"}
        for resource, caption in expected.items():
            node = self.find(root, resource=resource)
            visible_bounds(node)
            assert node.get("text") == caption, "Incomplete localized native label: " + resource
            if resource != "android:id/message":
                assert node.get("clickable") == "true", "Native action is not actionable"
        hour = self.find(root, resource="android:id/hours")
        minutes = self.find(root, resource="android:id/minutes")
        am = self.find(root, resource="android:id/am_label")
        pm = self.find(root, resource="android:id/pm_label")
        for node in (hour, minutes, am, pm): visible_bounds(node)
        assert am.get("text") == "AM" and pm.get("text") == "PM", "12h native period controls unavailable"
        assert am.get("checked") in {"true", "false"} and pm.get("checked") in {"true", "false"} and am.get("checked") != pm.get("checked"), "Ambiguous native AM/PM selection"
        actual = (int(hour.get("text")) % 12 + (12 if pm.get("checked") == "true" else 0)) * 60 + int(minutes.get("text"))
        assert 1 <= int(hour.get("text")) <= 12 and 0 <= int(minutes.get("text")) < 60 and actual == minute, "Native picker changed saved minutes"

    def wait_compact_activity(self, label, package=None, font_scale=None):
        deadline = min(self.deadline, time.monotonic() + 30)
        while time.monotonic() < deadline:
            dump = self.text("shell", "dumpsys", "activity", "activities", timeout=min(5, deadline-time.monotonic()))
            self.step += 1
            self.output.joinpath(f"{self.step:03d}-{label}-activity.txt").write_text(dump)
            component, font = resumed_activity(dump)
            if component is not None and (package is None or component.split("/")[0] == package) and \
                    (font_scale is None or font == font_scale):
                if time.monotonic() < deadline: return dump
            time.sleep(max(0, min(0.5, deadline-time.monotonic())))
        raise TimeoutError("N12 activity/configuration did not settle: " + label)

    def resume_compact_app(self):
        # input keyevent completion does not mean HOME has finished launching.
        self.adb("shell", "input", "keyevent", "KEYCODE_HOME")
        self.wait_compact_activity("N12-home-settled", "com.android.launcher3")
        self.launch()
        self.wait_compact_activity("N12-app-settled", PACKAGE, 1.5)

    def restore_absent_font_scale(self):
        # API33 ATMS persists a changed effective font scale. Settle default first,
        # then delete the key; a late queued default write must not count as absence.
        self.adb("shell", "settings", "put", "system", "font_scale", "1.0")
        self.wait_compact_activity("N12-default-font-settled", font_scale=1.0)
        deadline = min(self.deadline, time.monotonic() + 10)
        absent_since = None
        self.adb("shell", "settings", "delete", "system", "font_scale")
        while time.monotonic() < deadline:
            value = self.text("shell", "settings", "get", "system", "font_scale", timeout=min(5, deadline-time.monotonic()))
            now = time.monotonic()
            with self.output.joinpath("N12-font-restoration.jsonl").open("a") as log:
                log.write(json.dumps({"monotonic": now, "value": value}) + "\n")
            assert value in {"null", "1.0"}, "Unexpected font restoration readback"
            if value == "null":
                if absent_since is None: absent_since = now
                if now - absent_since >= 1 and now < deadline: return
            else:
                absent_since = None
                self.adb("shell", "settings", "delete", "system", "font_scale", timeout=min(5, max(0.001, deadline-now)))
            time.sleep(max(0, min(0.5, deadline-time.monotonic())))
        raise TimeoutError("Original absent font_scale was not restored")

    def restore_compact_settings(self, original):
        errors = []
        # Each independent setting is attempted even when another restoration fails.
        for kind in ("size", "density"):
            try:
                self.adb("shell", "wm", kind, display_override(original[kind], kind))
                assert self.text("shell", "wm", kind) == original[kind], "Display restoration mismatch"
            except Exception as error: errors.append(f"{kind}: {error}")
        for key in ("font_scale", "time_12_24"):
            try:
                value = original[key]
                if key == "font_scale" and value == "null":
                    self.restore_absent_font_scale()
                else:
                    command = ("delete", "system", key) if value == "null" else ("put", "system", key, value)
                    self.adb("shell", "settings", *command)
                assert self.text("shell", "settings", "get", "system", key) == value, "System setting restoration mismatch"
            except Exception as error: errors.append(f"{key}: {error}")
        return errors

    def reopen_kazakh_settings(self, root):
        if self.find(root, text="Баптаулар") is not None:
            self.back()  # Settings -> existing Profile; Privacy is only static Settings content.
        else:
            assert self.find(root, description="Баптаулар") is not None, "Expected actual Settings or Profile for restoration"
        self.tap(description="Баптаулар")
        self.assert_text("Баптаулар")

    def compact_privacy_and_back(self):
        # SettingsInfoRow has no navigation callback. Scroll to its real informational content.
        description = "Құпиялық саясаты туралы ақпарат осында қолжетімді болады"
        self.tap(text=description, scroll=True)
        root = self.snapshot("N12-ordinary-privacy-content")
        visible_bounds(self.find(root, text="Құпиялық саясаты"))
        visible_bounds(self.find(root, text=description))
        self.reopen_kazakh_settings(root)
        self.snapshot("N12-back-to-settings")

    def compact_kazakh_picker(self):
        original = {kind: self.text("shell", "wm", kind) for kind in ("size", "density")}
        original.update({key: self.text("shell", "settings", "get", "system", key) for key in ("font_scale", "time_12_24")})
        before = self.prefs("N12-before-configuration")
        assert preference_string(before, "language") == "en", "N12 expects the preceding restored English fixture"
        assert before.get("reminders_enabled") == "0800", "N12 starts only after verified D Off"
        minute = json.loads(preference_string(before, "reminders_schedule_v1"))["minutesOfDay"]
        self.output.joinpath("N12-original-settings.json").write_text(json.dumps(original, indent=2))
        language_changed = False
        try:
            self.ensure_settings_top(); self.tap(text="Language"); self.tap(text="Қазақ тілі")
            language_changed = True
            self.tap(text="Тіл")  # Real dialog verifies settled KK; dismiss without changing preference.
            self.back()
            self.adb("shell", "wm", "size", "320x640")
            self.adb("shell", "wm", "density", "160")
            self.adb("shell", "settings", "put", "system", "font_scale", "1.5")
            self.adb("shell", "settings", "put", "system", "time_12_24", "12")
            assert display_override(self.text("shell", "wm", "size"), "size") == "320x640"
            assert display_override(self.text("shell", "wm", "density"), "density") == "160"
            assert self.text("shell", "settings", "get", "system", "font_scale") == "1.5"
            assert self.text("shell", "settings", "get", "system", "time_12_24") == "12"
            # Ordinary background/resume applies native time-format/configuration; no process kill.
            self.resume_compact_app()
            self.assert_text("Баптаулар")  # Configuration recreation must preserve the real Settings route.
            self.output.joinpath("N12-configuration.txt").write_text(self.text("shell", "dumpsys", "activity", "activities"))
            self.tap(text="Өшірулі", scroll=True)
            root = self.snapshot("N12-kk-12h-saved-card")
            cards = [node for node in root.iter("node") if node.get("text", "").startswith("Таңдалған уақыт: ")]
            assert len(cards) == 1, "Saved time summary missing or ambiguous"
            visible_bounds(cards[0])
            assert twelve_hour_minutes(cards[0].get("text").split(": ", 1)[1]) == minute, "Saved card changed stored minutes"
            self.tap(text="Уақытты өзгерту", scroll=True)
            self.compact_picker_check(self.snapshot("N12-kk-12h-native-picker"), minute)
            cancel_before = self.adb("exec-out", "run-as", PACKAGE, "cat", PREFS)
            self.output.joinpath("N12-before-cancel.preferences_pb").write_bytes(cancel_before)
            self.tap(resource="android:id/button2", native_time_picker=True)
            self.tap(text="Өшірулі", scroll=True)
            cancel_after = self.adb("exec-out", "run-as", PACKAGE, "cat", PREFS)
            self.output.joinpath("N12-after-cancel.preferences_pb").write_bytes(cancel_after)
            assert cancel_after == cancel_before, "Native Cancel changed persisted bytes"
            self.assert_alarm(0)
            self.compact_privacy_and_back()
        except Exception as error:
            self.output.joinpath("N12-primary-error.json").write_text(json.dumps({"type": type(error).__name__, "error": str(error)}, indent=2))
            raise
        finally:
            errors = self.restore_compact_settings(original)
            if language_changed:
                try:
                    # Cleanup may follow a failed foreground transition; ordinary owned launch
                    # is permitted here, never as a notification-tap acceptance fallback.
                    self.launch()
                    self.wait_compact_activity("N12-cleanup-app-settled", PACKAGE)
                    root = self.snapshot("N12-before-language-restore")
                    if self.find(root, resource="android:id/timePicker") is not None:
                        self.tap(resource="android:id/button2", native_time_picker=True)
                    root = self.snapshot("N12-language-restore-route")
                    if self.find(root, text="English") is not None:
                        self.tap(text="English")  # Interrupted while the actual language dialog was open.
                    else:
                        self.reopen_kazakh_settings(root)  # Reset normal Settings scroll from the observed real route.
                        self.tap(text="Тіл"); self.tap(text="English")
                    self.tap(text="Language")
                    self.back()
                    restored = self.prefs("N12-restored")
                    assert preference_string(restored, "language") == preference_string(before, "language")
                    assert restored.get("reminders_enabled") == before.get("reminders_enabled")
                    assert json.loads(preference_string(restored, "reminders_schedule_v1"))["minutesOfDay"] == minute
                    assert {k: v for k, v in restored.items() if k not in MUTABLE_KEYS} == {k: v for k, v in before.items() if k not in MUTABLE_KEYS}
                except Exception as error: errors.append(f"language/data: {error}")
            self.output.joinpath("N12-restoration.json").write_text(json.dumps({"errors": errors}, indent=2))
            if errors: raise RuntimeError("N12 restoration failed: " + "; ".join(errors))

    def zone_and_time_hooks(self):
        help_text = self.text("shell", "cmd", "alarm", "help", alarm_help=True)
        assert "set-timezone" in help_text and "set-time" in help_text, "OS clock controls unavailable"
        self.adb("shell", "settings", "put", "global", "auto_time_zone", "0")
        self.adb("shell", "settings", "put", "global", "auto_time", "0")
        initial = self.prefs("before-clock-hooks")["reminders_schedule_v1"]
        for zone in ["Europe/Berlin", "UTC"]:
            self.adb("shell", "cmd", "alarm", "set-timezone", zone)
            assert self.text("shell", "getprop", "persist.sys.timezone") == zone
            time.sleep(2); self.next_schedule(); self.native("zone-" + zone.replace("/", "-"))
            assert self.prefs("zone-" + zone.replace("/", "-"))["reminders_schedule_v1"] == initial
        # Untimed hook only: a later real picker confirmation defines episode D's due time.
        real = int(time.time() * 1000)
        self.adb("shell", "cmd", "alarm", "set-time", str(real + 2 * 60 * 60 * 1000))
        assert abs(int(self.text("shell", "date", "+%s")) - (real//1000 + 7200)) < 10
        time.sleep(2); self.next_schedule(); self.native("manual-time-hook")
        assert not self.owned_notification(), "Clock hook produced a catch-up notification"
        self.adb("shell", "cmd", "alarm", "set-time", str(int(time.time()*1000)))
        self.adb("shell", "settings", "put", "global", "auto_time", "1")
        time.sleep(2)
        assert abs(int(self.text("shell", "date", "+%s")) - int(time.time())) < 10
        self.native("ordinary-clock-restored")

    def wait_reboot_restore(self, due, before_events, before_preferences):
        deadline = min(self.deadline, time.monotonic() + 30)
        minute = json.loads(preference_string(before_preferences, "reminders_schedule_v1"))["minutesOfDay"]
        assert before_preferences.get("reminders_enabled") == "0801", "Boot restoration requires saved enabled intent"
        def remaining():
            seconds = deadline - time.monotonic()
            if seconds <= 0:
                raise TimeoutError("No verified boot restoration within 30s after unlock")
            return seconds
        while time.monotonic() < deadline:
            assert int(self.text("shell", "date", "+%s", timeout=remaining())) < due, "Reboot missed the chosen occurrence"
            current = self.prefs("await-boot-restore", timeout=remaining())
            assert current == before_preferences, "Reboot reconciliation changed persisted preferences"
            events = self.text("exec-out", "run-as", PACKAGE, "cat", "files/reminder-diagnostics.log", timeout=remaining())
            dump = self.text("shell", "dumpsys", "alarm", timeout=remaining())
            self.output.joinpath(f"{self.step:03d}-boot-events.txt").write_text(events)
            self.output.joinpath(f"{self.step:03d}-boot-alarm.txt").write_text(dump)
            assert events.startswith(before_events), "Boot event journal lost its captured prefix"
            fresh = events[len(before_events):]
            assert "receiver-failed" not in fresh, "Boot receiver reported a failure"
            accepted = re.findall(r"(?m)^\d+ scheduled target=(\d+) minutes=(\d+)$", fresh)
            alarms = pending_alarms(dump)
            assert len(alarms) <= 1, "Boot restoration left duplicate owned alarms"
            if alarms: assert_inexact_alarm(alarms[0])
            assert int(self.text("shell", "date", "+%s", timeout=remaining())) < due, "Reboot missed the chosen occurrence"
            if accepted and tuple(map(int, accepted[-1])) == (due * 1000, minute) and \
                    len(alarms) == 1 and alarm_epoch(alarms[0]) == due * 1000:
                return
            time.sleep(min(1, remaining()))
        raise TimeoutError("No verified boot restoration within 30s after unlock")

    def reboot(self, due):
        self.native("before-real-reboot")
        before_preferences = self.prefs("before-real-reboot")
        before_events = self.text("exec-out", "run-as", PACKAGE, "cat", "files/reminder-diagnostics.log")
        self.output.joinpath("reboot-event-boundary.txt").write_text(before_events)
        self.adb("reboot")
        self.adb("wait-for-device", timeout=180)
        end = time.monotonic() + 180
        while time.monotonic() < end:
            if self.text("shell", "getprop", "sys.boot_completed", check=False) == "1": break
            time.sleep(3)
        else: raise TimeoutError("Reboot did not complete")
        self.adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        self.adb("shell", "wm", "dismiss-keyguard")
        self.wait_reboot_restore(due, before_events, before_preferences)
        self.native("restored-before-app-launch")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    driver = Driver(args.serial, args.output)
    result = {name: "NOT_RUN" for name in ["A_cold", "B_warm_permissions", "C_foreground", "D_reboot_cancel", "N12_compact_kk_12h"]}
    args.output.joinpath("apk-sha256.txt").write_text(hashlib.sha256(args.apk.read_bytes()).hexdigest())
    try:
        assert driver.text("shell", "getprop", "ro.build.version.sdk") == "33"
        driver.adb("install", "-r", str(args.apk), timeout=120)
        installed_package = driver.text("shell", "dumpsys", "package", PACKAGE)
        args.output.joinpath("installed-package.txt").write_text(installed_package)
        assert_no_exact_alarm_permissions(installed_package)
        driver.adb("shell", "settings", "put", "system", "time_12_24", "24")
        driver.adb("shell", "settings", "put", "system", "screen_off_timeout", "7200000")
        driver.adb("shell", "settings", "put", "global", "stay_on_while_plugged_in", "7")
        driver.adb("shell", "cmd", "alarm", "set-timezone", "UTC")
        assert driver.text("shell", "getprop", "persist.sys.timezone") == "UTC"
        driver.reset_fixture("A")
        driver.tap(text="Choose time and enable", scroll=True); driver.tap(resource="android:id/button2", native_time_picker=True)
        driver.assert_text("Off"); driver.assert_alarm(0)
        due = driver.future_picker(first=True)
        driver.tap(resource="com.android.permissioncontroller:id/permission_allow_button")
        driver.tap(text="Scheduled", scroll=True); driver.assert_alarm(1); driver.native("A-accepted")
        driver.adb("shell", "input", "keyevent", "KEYCODE_HOME")
        driver.stop_background_process("A-before-delivery", before_due=due)
        driver.wait_delivery(due)
        driver.stop_background_process("A-before-cold-tap")
        driver.tap_notification(); result["A_cold"] = "PASS"

        # New permission fixture, not continuity evidence for A. No permission is shell-granted.
        driver.reset_fixture("B")
        driver.future_picker(first=True)
        driver.dismiss_notification_prompt()  # Real permission dismissal; no preference rollback.
        driver.tap(text="Allow notifications", scroll=True)
        driver.tap(resource="com.android.permissioncontroller:id/permission_deny_button")
        driver.tap(text="Notifications are blocked by the system.", scroll=True); driver.assert_alarm(0)
        driver.tap(text="Notification settings", scroll=True)
        driver.toggle_system(True); driver.back()
        driver.tap(text="Scheduled", scroll=True); driver.assert_alarm(1)
        driver.verify_block_and_recover()
        driver.verify_block_and_recover(channel=True)
        driver.change_language_roundtrip()
        due = driver.future_picker()
        driver.assert_alarm(1); driver.invariant("B-updated")
        warm_pid = driver.text("shell", "pidof", PACKAGE)
        assert warm_pid
        driver.adb("shell", "input", "keyevent", "KEYCODE_HOME")
        driver.wait_delivery(due)
        assert driver.text("shell", "pidof", PACKAGE) == warm_pid, "Process was killed: not warm evidence"
        driver.tap_notification()
        assert driver.text("shell", "pidof", PACKAGE) == warm_pid
        result["B_warm_permissions"] = "PASS"

        driver.settings(); due = driver.future_picker()
        driver.tap(text="Scheduled", scroll=True)
        driver.wait_delivery(due, foreground=True)
        driver.invariant("C-foreground"); result["C_foreground"] = "PASS"

        driver.zone_and_time_hooks()
        # Clock/zone hook evidence is separate; only this final native picker chooses the timed occurrence.
        due = driver.future_picker()
        driver.adb("shell", "input", "keyevent", "KEYCODE_HOME")
        driver.reboot(due)
        assert driver.next_schedule()[0] == due * 1000, "Reboot missed the chosen occurrence; timed episode BLOCKED"
        driver.wait_delivery(due)
        driver.adb("shell", "cmd", "statusbar", "collapse")
        driver.launch(); driver.settings()
        before_off = driver.prefs("D-before-off")
        driver.ensure_settings_top()
        driver.tap(description="Reminders", scroll=True)
        driver.assert_text("Off"); driver.assert_alarm(0)
        assert not driver.owned_notification(), "Untapped delivered card survived Off"
        after_off = driver.invariant("D-after-off")
        assert before_off["reminders_schedule_v1"] == after_off["reminders_schedule_v1"]
        result["D_reboot_cancel"] = "PASS"
        driver.compact_kazakh_picker()
        result["N12_compact_kk_12h"] = "PASS"
    except Exception as error:
        result["error"] = f"{type(error).__name__}: {error}"
        try: driver.native("failure"); driver.snapshot("failure")
        except Exception as capture_error: result["capture_error"] = str(capture_error)
        raise
    finally:
        args.output.joinpath("results.json").write_text(json.dumps(result, indent=2)+"\n")
        args.output.joinpath("logcat.txt").write_bytes(driver.adb("logcat", "-d", "-v", "threadtime", check=False))


if __name__ == "__main__":
    main()
