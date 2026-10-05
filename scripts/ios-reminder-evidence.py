"""Read-only validation of OquTurbo app-process native observations (never the XCTest runner's center)."""
import importlib.util
import datetime as dt
from zoneinfo import ZoneInfo
import json
from pathlib import Path

_spec = importlib.util.spec_from_file_location("android_reminder_wire", Path(__file__).with_name("qa-android-reminders.py"))
_wire = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_wire)
OWNED = "oquturbo_practice_reminder"
MUTABLE = {"reminders_enabled", "reminders_schedule_v1", "daily_training_v1"}


def snapshot(data):
    values = _wire.preferences(data)
    enabled = False
    if "reminders_enabled" in values:
        fields = dict((key, value) for key, _, value in _wire.protobuf_fields(bytes.fromhex(values["reminders_enabled"])))
        if set(fields) != {1} or fields[1] not in (0, 1):
            raise ValueError("Unreadable desired preference")
        enabled = bool(fields[1])
    schedule = None
    if "reminders_schedule_v1" in values:
        schedule = json.loads(_wire.preference_string(values, "reminders_schedule_v1"))
    return {"values": values, "enabled": enabled, "schedule": schedule}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def events_since(raw, boundary):
    """Journal append boundary, deliberately independent of adjustable wall-clock timestamps."""
    require(not boundary or boundary.endswith(b"\n"), "Incomplete phase boundary record")
    require(raw.startswith(boundary), "App diagnostic journal was replaced/truncated within the phase")
    appended = raw[len(boundary):]
    require(not appended or appended.endswith(b"\n"), "Incomplete native event record")
    return [json.loads(line) for line in appended.splitlines()]


def validate_summary(summary):
    require(summary.get("passedTests") == 1 and summary.get("failedTests") == 0 and
            summary.get("skippedTests") == 0, "Expected exactly one actual passed XCTest, no skipped/failed tests")


def validate_bootstrap(events, snapshots):
    require(events and snapshots, "Missing actual bootstrap journal/preferences")
    latest = snapshots[-1]
    require({"game_sessions_v1", "daily_training_v1"}.issubset(latest["values"]), "First-launch repositories are not persisted")
    require(all(not item["enabled"] and item["schedule"] is None for item in snapshots), "Bootstrap changed reminder intent")
    require(not any(e["event"] in {"accepted", "request", "response", "home-consumed", "picker-host", "foreground-delivery"} for e in events),
            "Bootstrap interacted with reminders")
    launches = [e for e in events if e["event"] == "runtime-created"]
    require(len(launches) == 1 and launches[0].get("launch"), "Unknown bootstrap runtime identity")
    require(all(e.get("launch") == launches[0]["launch"] for e in events), "Mixed bootstrap process identities")
    auth = [e for e in events if e["event"] == "authorization"]
    require(auth and all(e.get("status") == "0" for e in auth), "Bootstrap permission is not fresh/unrequested")
    for name in ("pending", "delivered"):
        records = [e for e in events if e["event"] == name]
        require(records and all(e.get("count") == "0" for e in records), "Bootstrap has owned native work: " + name)
    return {"status": "SETUP_ONLY_PASS", "launch": launches[0]["launch"], "snapshot": latest}


def mutable_keys(phase):
    return MUTABLE | ({"language"} if phase == "locales" else set())


def next_local_minute(now, minutes, zone):
    local = dt.datetime.fromtimestamp(now, ZoneInfo(zone))
    candidate = local.replace(hour=minutes // 60, minute=minutes % 60, second=0, microsecond=0)
    if candidate.timestamp() <= now:
        candidate += dt.timedelta(days=1)
    return candidate.timestamp()


def validate_phase(phase, events, snapshots, baseline):
    require(events and snapshots, "Missing app-process native evidence or preferences")
    for item in snapshots:
        require({k: v for k, v in item["values"].items() if k not in mutable_keys(phase)} == baseline,
                "Unrelated product preferences changed")
    accepted = [e for e in events if e["event"] == "accepted"]
    require(accepted, "No actual native add agreement")
    schedules = [s["schedule"] for s in snapshots if s["enabled"] and s["schedule"]]
    require(schedules, "No durable enabled schedule observed")
    requests = []
    for event in events:
        if event["event"] != "request":
            continue
        require(event.get("id") == OWNED and event.get("calendar") == "true" and
                event.get("repeats") == "true" and event.get("timezone") == "null", "Not a floating daily owned request")
        require(float(event["next"]) > float(event["time"]), "Native next occurrence is not future")
        matching = [s for s in schedules if s["minutesOfDay"] == int(event["hour"]) * 60 + int(event["minute"]) and
                    s["content"] == {"languageCode": event["language"], "title": event["title"], "body": event["body"]}]
        require(matching, "Pending native time/content does not agree with durable snapshot")
        requests.append(event)
    require(requests, "No actual pending calendar inventory")
    require(all(e.get("count") in ("0", "1") for e in events if e["event"] == "pending"), "Invalid owned pending count")

    if phase in ("cold", "warm"):
        responses = [e for e in events if e["event"] == "response"]
        require(len(responses) == 1, "Expected exactly one actual card response")
        response = responses[0]
        require(response.get("id") == OWNED and response.get("action") == "com.apple.UNNotificationDefaultActionIdentifier",
                "Not an owned default notification response")
        home = [e for e in events if e["event"] == "home-consumed" and e.get("eventId") == response.get("eventId")]
        require(len(home) == 1 and home[0].get("destination") == "Home", "No exactly-once actual Home consumption")
        require(home[0]["launch"] == response["launch"], "Response and graph are not the same app process")
        candidates = [r for r in requests if float(r["time"]) < float(response["delivered"]) and
                      abs(float(r["next"]) - float(response["delivered"])) <= 180]
        require(candidates, "Response has no matching real calendar due occurrence")
        request = candidates[-1]
        require((request["launch"] == response["launch"]) == (phase == "warm"), "Cold/warm launch identity mismatch")
    elif phase == "foreground":
        foreground = [e for e in events if e["event"] == "foreground-delivery" and e.get("presentation") == "none"]
        require(foreground, "No actual willPresent callback; absence of a banner alone is insufficient")
        require(any(r["launch"] == f["launch"] and float(r["time"]) < float(f["delivered"]) and
                    abs(float(r["next"]) - float(f["delivered"])) <= 180 for r in requests for f in foreground),
                "Foreground callback has no matching native occurrence")
        require(not any(e["event"] == "response" for e in events), "Unexpected foreground notification tap")
    elif phase == "off":
        delivered = [e for e in events if e["event"] == "delivered-record" and e.get("id") == OWNED]
        cancelled = [e for e in events if e["event"] == "cancelled"]
        require(delivered and cancelled, "Missing untapped delivered notification/cancellation evidence")
        require(not snapshots[-1]["enabled"] and snapshots[-1]["schedule"] == schedules[-1], "Off lost desired state or saved time")
        cutoff = float(cancelled[-1]["time"])
        for name in ("pending", "delivered"):
            after = [e for e in events if e["event"] == name and float(e["time"]) >= cutoff]
            require(after and after[-1]["count"] == "0", "Owned native work remains after Off")
    elif phase == "locales":
        languages = {e.get("language") for e in requests}
        require({"en", "ru", "kk"} <= languages, "No actual pending content refresh for all three app languages")
        require(snapshots[-1]["schedule"]["content"]["languageCode"] == "en", "Language fixture did not restore English")
    elif phase in ("travel", "dst"):
        expected_zone = "Asia/Almaty" if phase == "travel" else "America/New_York"
        clocks = [e for e in events if e["event"] == "system-clock"]
        require(clocks and all(e.get("zone") == expected_zone for e in clocks), "Actual app system timezone did not change")
        matching = [e for e in requests if int(e["hour"]) * 60 + int(e["minute"]) == 150]
        require(matching, "No actual native calendar 02:30 after verified clock/zone change")
        require(any(min(float(c["time"]) for c in clocks) <= float(r["time"]) <=
                    max(float(c["time"]) for c in clocks) + 5 for r in matching),
                "Native request inventory was not captured with verified app system clock")
        if phase == "travel":
            for request in matching:
                expected = next_local_minute(float(request["time"]), 150, expected_zone)
                require(abs(float(request["next"]) - expected) < 1,
                        "Native next date does not resolve to the next local 02:30 in the changed timezone")
        if phase == "dst":
            require(all(1772949600 <= float(e["time"]) < 1772953200 for e in clocks), "App did not observe controlled pre-gap wall clock")
            # No single invented gap policy: retain actual native civil resolution for independent QA.
            # The next date must either match the daily wall time or resolve on the actual missing-hour day.
            for request in matching:
                resolved = dt.datetime.fromtimestamp(float(request["next"]), ZoneInfo(expected_zone))
                exact_wall_time = resolved.hour == 2 and resolved.minute == 30
                gap_resolution = resolved.date() == dt.date(2026, 3, 8) and resolved.hour == 3
                require(exact_wall_time or gap_resolution,
                        "Native next date is neither the selected local time nor the real DST-gap resolution")
        require(not snapshots[-1]["enabled"], "Clock fixture must disable owned work before restoring host time")
    elif phase == "permission":
        statuses = [e.get("status") for e in events if e["event"] == "authorization"]
        require("1" in statuses and "2" in statuses and statuses[-1] == "1", "Missing actual denied/granted/revoked settings")
        denied = statuses.index("1")
        require("2" in statuses[denied + 1:], "Grant did not follow denial")
        require(snapshots[-1]["enabled"], "System block changed desired preference")
        pending = [e for e in events if e["event"] == "pending"]
        require(pending and pending[-1]["count"] == "0", "Revoke did not cancel owned pending work")
    else:
        raise ValueError("Unknown phase")
    return {"phase": phase, "status": "PASS", "requests": len(requests), "observations": len(events)}
