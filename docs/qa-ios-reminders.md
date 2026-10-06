# iOS reminder native acceptance

This harness builds and tests the real OquTurbo.app and its testing-only OquTurboUITests bundle. A framework link,
Linux Kotlin tests, metadata inspection, a successful UI test without the app's native probe, or an injected push
is not native reminder acceptance. Nothing here injects notifications, invokes receiver/delegate/navigation
callbacks, grants permissions, edits product preferences, or supplies an app-only timezone override.

Run only on a disposable macOS arm64 CI host with Xcode, iOS >=18.2 and an installed narrow iPhone SE3 simulator:

```sh
python3 scripts/qa-ios-reminders.py --output build/qa/reminders-ios --dedicated-runner-clock-probe
```

The new `pr-reminders-ios-runtime.yml` job uses macos-15, verifies arm64, runs actual native Kotlin tests and the
app/UI-test matrix. All 21 existing workflows remain byte-identical. Runner architecture is documented in
[GitHub's official inventory](https://docs.github.com/en/actions/reference/runners/github-hosted-runners).
Build retains the real Kotlin embed phases; it rejects the framework-build bypass environment flag.
The first real Xcode build must verify generated Swift bridge selectors and the existing framework search path.
No successful macOS compile or runtime result is claimed during Linux authoring.

The runner creates and owns one fresh simulator UDID. It verifies the actual built bundle ID, arm64 executable,
SHA and source identity. `build-for-testing` and individual `test-without-building -only-testing` invocations use
the same real shared scheme. One expected passed XCTest, no skipped/failed tests, is required in each xcresult.
Unknown CLI schemas or missing selectors/evidence fail; they never fall back to simulated success. Actual UI
selectors still need their first macOS execution. The app's small Debug-only diagnostic writer requires an
app-private Documents marker. The host creates that marker after installing the app. It survives cold launch
from the actual card, unlike launchEnvironment. The XCTest runner's notification center is never queried.

The ordered phases preserve screenshots, UI trees, xcresult, logs, actual app-native JSONL and every changed
preference snapshot:

1. Fresh Off and native picker Cancel, real first permission opt-in, real calendar delivery while process is
   absent, actual card tap to Home. Native response launch UUID must differ from the scheduling launch.
2. Real warm delivery from Settings/background; UUID must remain the same and actual Home consumption occurs
   once. Neither phase calls app.launch/activate after the card tap to manufacture navigation.
3. Actual foreground `willPresent`, no presentation; absence of a banner alone is insufficient.
4. Native large-text setting readback, narrow app bounds, app-selected RU/KK/EN, actual pending title/body agrees
   with each durable localized snapshot, native picker labels/actions remain accessible. Cancel preserves time.
5. Untapped real delivery, explicit Off, retained chosen time, actual empty owned pending AND delivered inventories.
6. Actual travel and DST/manual-clock controls, selected02:30, actual app system timezone/epoch and public
   `nextTriggerDate` readback. Each fixture disables owned work before the host restores clock settings.
7. Explicit separate permission fixture: after preserving phases1–6 evidence, shutdown/erase only the owned
   simulator, reboot/reinstall, real denial, real Settings grant/revoke and truthful state on foreground return.
   No persistence claim crosses that fixture reset.

Native probes observe system authorization, pending/calendar/content/timezone/next occurrence, delivered records,
actual response and foreground callbacks, process UUID, actual graph Home consumption and actual system clock.
The host checks byte-preservation of unrelated DataStore values. Only reminder preferences and the normal Home
plan may change; app language may additionally change in the explicit localization phase.

Clock controls are privileged and limited to the explicit disposable-CI mode (`CI=true`). The harness first
requires readable original timezone/network-time settings and passwordless sudo. It records real systemsetup
and date controls/readbacks, then requires the *app* to observe Asia/Almaty and the New York pre-gap date. It never
uses status-bar clocks or `TZ` launch arguments as substitutes. If the simulator does not inherit the changes,
controls are denied, or readback cannot be proven, the mandatory N5 gate is BLOCKED/red. In `finally`, original
clock plus monotonic elapsed time, timezone and network-time mode are restored; restoration errors are red.
The scheduler keeps hour/minute-only components; gap normalization is the actual OS policy, not a fabricated
expected notification time. This does not require another delivery episode or a24-hour wait.

Phase timeout12min, delivery bounds due+180seconds, whole harness100min and workflow120min. Partial evidence is
retained on all failures and the owned simulator is shut down. The runner does not affect other simulators.
Every mandatory automated phase must succeed for `MANDATORY_AUTOMATED_MATRIX_PASS`; unsupported required native
capabilities produce nonzero `FAILED_OR_BLOCKED`, not a skipped/pass or green subset. VoiceOver remains separately
unverified if not actually exercised. Review real captures for clipping/overlap, system12/24 preference and other
manual matrix criteria before declaring full N1–N13 acceptance. Repeat configuration plus one real delivery is
not evidence of tomorrow's delivery.

Local helper tests: `python3 scripts/test-ios-reminder-evidence.py`. These validate rejection of missing/mismatched
native evidence, process identity, clock/zone, locales, unrelated preference mutation and zero/skipped XCTest
results; they do not execute any native OS behavior. CLI operations follow installed command help and
[Apple's xcresulttool guidance](https://developer.apple.com/documentation/xcode-release-notes/xcode-16_3-release-notes).
