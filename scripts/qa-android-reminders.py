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



class Driver:
    def __init__(self, serial, output):
        self.serial, self.output = serial, output
        self.output.mkdir(parents=True, exist_ok=True)
        self.step = 0
        self.deadline = time.monotonic() + 305 * 60
        self.baseline = None

    def adb(self, *args, timeout=40, check=True):
        if time.monotonic() >= self.deadline: raise TimeoutError("Whole native acceptance deadline exceeded")
        result = subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, timeout=timeout)
        with (self.output / "commands.log").open("a") as log:
            log.write(json.dumps({"time": time.time(), "argv": list(args), "code": result.returncode}) + "\n")
        if check and result.returncode:
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
        return target

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

    def prefs(self, label):
        self.step += 1
        label = f"{self.step:03d}-{label}"
        data = self.adb("exec-out", "run-as", PACKAGE, "cat", PREFS)
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
            assert "window=0 " not in alarms[0], "Exact alarm unexpectedly present"
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
        assert f"origWhen={target}" in alarms[0], "OS inventory does not match the accepted epoch"
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

    def toggle_system(self, enabled):
        root = self.snapshot("system-notification-control")
        nodes = [n for n in root.iter("node") if n.get("checkable") == "true" and n.get("enabled") == "true"]
        if len(nodes) != 1: raise AssertionError("Expected one unambiguous native notification switch")
        node = nodes[0]
        if (node.get("checked") == "true") != enabled:
            bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
            self.adb("shell", "input", "tap", str((bounds[0]+bounds[2])//2), str((bounds[1]+bounds[3])//2))
            time.sleep(1)
        root = self.snapshot("system-notification-changed")
        current = [n for n in root.iter("node") if n.get("checkable") == "true" and n.get("enabled") == "true"]
        assert len(current) == 1 and (current[0].get("checked") == "true") == enabled

    def verify_block_and_recover(self, channel=False):
        before = self.prefs("before-block")
        self.notification_settings(channel); self.toggle_system(False); self.back()
        self.tap(text="Notifications are blocked by the system.", scroll=True)
        self.assert_alarm(0)
        self.snapshot("no-automatic-permission-prompt")
        self.tap(text="Notification settings", scroll=True)
        if channel:
            # App settings opened by the product; use the actual channel row.
            self.tap(text="Practice reminders")
        self.toggle_system(True); self.back()
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

    def zone_and_time_hooks(self):
        help_text = self.text("shell", "cmd", "alarm", "help")
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

    def reboot(self):
        self.native("before-real-reboot")
        self.adb("reboot")
        self.adb("wait-for-device", timeout=180)
        end = time.monotonic() + 180
        while time.monotonic() < end:
            if self.text("shell", "getprop", "sys.boot_completed", check=False) == "1": break
            time.sleep(3)
        else: raise TimeoutError("Reboot did not complete")
        self.adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        self.adb("shell", "wm", "dismiss-keyguard")
        time.sleep(5)
        self.assert_alarm(1); self.native("restored-before-app-launch")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    driver = Driver(args.serial, args.output)
    result = {name: "NOT_RUN" for name in ["A_cold", "B_warm_permissions", "C_foreground", "D_reboot_cancel"]}
    args.output.joinpath("apk-sha256.txt").write_text(hashlib.sha256(args.apk.read_bytes()).hexdigest())
    try:
        assert driver.text("shell", "getprop", "ro.build.version.sdk") == "33"
        driver.adb("install", "-r", str(args.apk), timeout=120)
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
        driver.reboot()
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
