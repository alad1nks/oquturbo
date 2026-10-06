# Android reminder runtime acceptance

This harness is source-reviewed automation, **not native evidence until it runs**. The local API33 software
emulator failed before app installation. Do not substitute a software retry, API28, mock alarm, injected
receiver/notification, pending token or JVM test for the API33 delivery/permission gates.

The additive `PR Android reminder runtime` workflow builds the production debug APK and owns one fresh
API33 AVD on `/dev/kvm`. It requires acceleration (`-accel on`), has a ten-minute boot deadline and checks
pre-app stability. It preserves failures/partial evidence and cleans up only its process/serial. No existing
PR/release/publication workflow is changed. Workflow publication/execution is a separate parent/user decision.

The runner delegates to `qa-android-reminders.py` (stdlib only, no app instrumentation dependency). Every tap
uses a current UI XML node's bounds. Missing or ambiguous controls fail the run; the code does not silently
substitute a deep link for product navigation, permission grant, or notification tap. Native settings intents
open real OS notification controls. Selectors still need verification on the actual approved API33 image.

Four timed episodes:

1. A: fresh Off, native picker Cancel, explicit Confirm and real permission Allow; one inexact owned alarm;
   normal background process kill; actual delivered record/shade card; cold card tap → actual Home once.
2. B: **explicit `pm clear` fixture boundary**, after archiving A. A new preference baseline is recorded.
   Real picker/permission dismissal, explicit Allow then Deny, system Settings recovery, app and channel
   block/recovery, language/time replacement, one alarm, actual delivery and warm same-PID card tap → Home.
3. C: remain resumed until real receiver dispatch; assert no owned post/card and a future replacement alarm.
   This does not claim an audible test merely because the runner has no audio.
4. D: capture timezone/manual-time hook results separately, verify retained HH:mm and a future OS alarm.
   Restore ordinary UTC clock **before** choosing a new near-future time through the actual picker. Reboot,
   verify restored alarm before app launch; wait for actual delivery; leave card untapped; normal launch and
   Off remove the owned alarm/card and retain time. Missing the chosen occurrence during reboot is an honest
   blocked timing case, not a reason to change the scheduler or wait for tomorrow within a one-hour budget.

Each due time is approximately now+3 minutes, chosen through the native UI; observation ends at T+65 minutes
or the global305-minute harness budget. The workflow is330 minutes. Charging/power/DND/channel/permission,
clock, pending alarm dump, actual notification records, PNG/XML, process/receiver/Home events and APK hash
are preserved. `results.json` starts every episode NOT_RUN and marks PASS only after its assertions; exceptions
leave later cases NOT_RUN and return nonzero. A human separates environment BLOCKED from a validated product
FAIL using the artifacts; no `continue-on-error`, skipped-runtime green or promised delivery latency.

The API33 alarm oracle requires the owned RTC_WAKEUP record, non-repeating interval, known window format,
zero special flags and no `exactAllowReason`. Installed package evidence must exclude both exact-alarm
permissions. A zero derived window alone is not an exact-API indicator: Android13 computes a zero heuristic
window for `AlarmManager.set` with less than ten seconds of futurity, whereas caller-requested exact alarms
receive `FLAG_STANDALONE` before that calculation. See
[Android13 AlarmManagerService](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-13.0.0_r1/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java)
(`maxTriggerTime`, `setImpl`, and the binder `set` method). The near-due alarm fixture is the owned block from
run37357914200 `062-alarm-inventory.txt`; permission test fixtures are synthetic. These checks preserve the
no-exact-API/access requirement without changing production scheduling or native observation deadlines.

Read-only debug diagnostics require both a debuggable app and an app-private opt-in marker. They observe
production adapter/receiver/Home operations, never schedule/post/navigate/grant or change product preferences;
diagnostic I/O cannot fail product operations. Host-side protobuf decoding compares every unrelated stored
value byte-for-byte within each fixture. Allowed changing keys are the two reminder keys, explicitly changed
language, and the existing daily plan (Home can legitimately generate a new UTC-day plan). Activity journal,
training progress/receipts, records, profile and weekly focus remain invariant. Raw preferences and decoded
maps are retained. This does not seed completed games or claim past-practice suppression behavior: neutral
production scheduling never reads gameplay state, independently covered by controller tests/source review.

Local static checks:

```shell
python3 -B -m unittest discover -s scripts -p 'test_qa_android_reminders.py'
actionlint .github/workflows/pr-reminders-android-runtime.yml
```

The wire/inventory parser tests are harness correctness checks, not OS behavior. Native12/24h variation,
RU/KK picker accessibility, TalkBack, API24/25 guards, other OEMs and iOS remain distinct runtime evidence;
this bounded four-episode English/API33 job must not claim those checks.
