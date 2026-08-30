# Quality gates

Apply only gates relevant to the diff, but every READY_TO_MERGE result must explain applicability and evidence.

## Command inventory

| Command | Verifies | Typical scope/runtime | Prerequisites and limits |
| --- | --- | --- | --- |
| `./gradlew ktlintCheck` | Repository Kotlin formatting; matches common PR CI | Repository-wide, lightweight/minutes | JDK/toolchain and cached or reachable Gradle dependencies |
| `./gradlew test` | JVM/Android unit-test tasks and compilation; matches OquTurbo PR CI | Broad, several minutes | Most source sets lack behavioral tests; a pass is not E2E evidence |
| `./gradlew :app:oquturbo:shared:compileKotlinJvm` | Shared graph/features compile for JVM | OquTurbo and dependencies, minutes | JDK 21 toolchain |
| `./gradlew :app:oquturbo:shared:compileKotlinWasmJs` | Shared graph/features compile for Wasm | OquTurbo and dependencies, minutes | Node/Yarn artifacts may be downloaded by Gradle |
| `./gradlew :app:oquturbo:androidApp:assembleDebug` | Debug Android app integration | OquTurbo Android, several minutes | Android SDK 37 |
| `scripts/qa-android-emulator.sh provision/start/status/build/install/capture` | Headless Android launch, deployment, interaction, screenshots, and layout | Local/Cloud QA, first provision downloads several GB | Linux x86_64; uses KVM when available and software emulation otherwise |
| `./gradlew :screenshot-tests:verifyRoborazziJvm` | Deterministic shared-UI screenshots | JVM visual suite, minutes | Linux CI uses `xvfb-run -a`; rendering can be host-sensitive |
| `./gradlew :feature:memorygrid:allTests` | Existing Memory Grid common logic tests across configured targets | Focused feature, minutes | Some native/browser targets may require host tooling |

CI additionally runs release Android assembly (requires signing secrets), iOS framework linking, `jsJar`,
`wasmJsJar`, and desktop `jar`. Do not use a release build locally as a mandatory gate when credentials are absent.

## Mandatory gates

1. **Build:** compile the narrowest affected module and every affected OquTurbo/standalone consumer. Compile more
   than one applicable target after `commonMain` changes when practical.
2. **Static analysis:** `./gradlew ktlintCheck`; `git diff --check`; no ignored failures.
3. **Automated tests:** run focused tests for changed behavior and `./gradlew test` when the change crosses modules.
   Add tests for deterministic rules, mappings, persistence semantics, and regressions where feasible.
4. **Product acceptance:** trace every acceptance criterion to code and test/manual evidence; do not let
   implementation redefine the specification.
5. **UX/design:** compare every designed state, reuse shared components/theme, and add deterministic screenshot
   coverage for a materially changed shared UI where suitable.
6. **Localization:** keep English, Russian, Kazakh, and `AppResource.kt` keys aligned; inspect truncation-sensitive
   states rather than checking key presence alone.
7. **Regression review:** verify navigation/back behavior, Koin assembly, storage compatibility, session/record/XP
   semantics, and every product consumer implicated by the diff.
8. **QA:** independently execute the acceptance matrix and label unexercised behavior explicitly.
9. **PR scope:** one bounded feature, no unrelated refactor/dependency/version/credential changes, clean diff.

## Device and Cloud boundaries

Android/iOS lifecycle, system back, IME, accessibility services, vibration/sound, process restart, and device-specific
layout require an actual emulator/device. On Linux x86_64, first attempt Android runtime validation with
`scripts/qa-android-emulator.sh`; inspect its captured PNG/layout artifacts and record the interactions performed.
Software emulation is supported when KVM is unavailable but can boot slowly. Full iOS app validation requires
macOS/Xcode; Linux can at most compile supported Kotlin artifacts and must not claim iOS runtime validation. Browser
interaction needs a launched browser;
Gradle web compilation alone is not interaction testing. Codex Cloud may also lack an Android SDK, display server,
signing secrets, GitHub write permissions, or warm dependency caches. Report these as BLOCKED/unverified per
criterion, not as passes. JVM Roborazzi provides useful shared rendering evidence but does not replace platform E2E.
