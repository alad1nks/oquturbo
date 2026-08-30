---
name: oquturbo-qa
description: Independently acceptance-test an OquTurbo feature from the user's perspective using repository automation and honest manual/environment evidence. Use after code review, after a QA fix, or to build a game-specific test matrix; return PASS, FAIL, or BLOCKED.
---

# Verify user behavior

1. Read the approved acceptance criteria/design and identify supported products, platforms, locales, modes, and state.
2. Derive a traceable matrix of happy paths, meaningful edge cases, regression/integration paths, and required setup.
3. Run relevant automated checks from `docs/agents/QUALITY_GATES.md`; preserve exact commands/results.
4. For Android acceptance, use `scripts/qa-android-emulator.sh`: provision and start the headless AVD in a
   long-running tool session, keep that session alive, and poll `status` in separate calls until `boot_completed=1`.
   Build and install the affected product, then capture screenshots and layout evidence.
   Inspect every captured PNG with the available image viewer. Use the script's `adb` passthrough for taps, text,
   back, rotation, restart, and other interactions, recapturing evidence after material states. Keep the emulator log
   when provisioning or boot fails; lack of KVM is a performance limitation, not a reason to skip the software-mode
   attempt. Do not download SDK components unless the task authorizes the normal tooling setup needed for QA.
5. Exercise what the environment truly supports. For persistence, test first run, completed write, reload/restart, and
   existing-state compatibility when relevant. For games, test ready/start, correct/incorrect input, score/difficulty,
   end/result, retry, personal record (equal/lower/new), interruption, each affected mode, and training/non-training
   behavior as applicable.
6. Verify English/Russian/Kazakh keys and inspect relevant layouts when runnable. Verify catalog/navigation/DI,
   records, Stats, Home/Profile XP, and Daily Training only when the feature integrates with them.
7. Distinguish code inspection, automated execution, manual interaction, and untested behavior. Never infer PASS from
   compilation or report a check not run.

Output the matrix with evidence, defects with reproduction/expected/actual/impact, environment limitations, and one
final result: `PASS` only when all mandatory criteria were verified; `FAIL` for a reproduced failure; `BLOCKED` when
required evidence cannot be obtained. QA remains independent and does not silently fix production code.
