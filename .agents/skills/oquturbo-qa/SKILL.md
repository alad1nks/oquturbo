---
name: oquturbo-qa
description: Independently acceptance-test an OquTurbo feature from the user's perspective using repository automation and honest manual/environment evidence. Use after code review, after a QA fix, or to build a game-specific test matrix; return PASS, FAIL, or BLOCKED.
---

# Verify user behavior

1. Read the approved acceptance criteria/design and identify supported products, platforms, locales, modes, and state.
2. Derive a traceable matrix of happy paths, meaningful edge cases, regression/integration paths, and required setup.
3. Run relevant automated checks from `docs/agents/QUALITY_GATES.md`; preserve exact commands/results.
4. Exercise what the environment truly supports. For persistence, test first run, completed write, reload/restart, and
   existing-state compatibility when relevant. For games, test ready/start, correct/incorrect input, score/difficulty,
   end/result, retry, personal record (equal/lower/new), interruption, each affected mode, and training/non-training
   behavior as applicable.
5. Verify English/Russian/Kazakh keys and inspect relevant layouts when runnable. Verify catalog/navigation/DI,
   records, Stats, Home/Profile XP, and Daily Training only when the feature integrates with them.
6. Distinguish code inspection, automated execution, manual interaction, and untested behavior. Never infer PASS from
   compilation or report a check not run.

Output the matrix with evidence, defects with reproduction/expected/actual/impact, environment limitations, and one
final result: `PASS` only when all mandatory criteria were verified; `FAIL` for a reproduced failure; `BLOCKED` when
required evidence cannot be obtained. QA remains independent and does not silently fix production code.
