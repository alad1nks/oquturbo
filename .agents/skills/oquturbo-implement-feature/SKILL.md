---
name: oquturbo-implement-feature
description: Implement an approved OquTurbo product and design specification with established Kotlin Multiplatform architecture, localization, tests, and verification. Use when scoped production-code implementation or validated follow-up fixes are assigned to the Developer.
---

# Implement an approved feature

1. Read `AGENTS.md`; read `PRODUCT.md` for product behavior; read the supplied product/design specifications.
2. Inspect the closest analogous implementations and all consumers of shared behavior. Check status/diff and preserve
   unrelated work.
3. Write a brief plan mapping acceptance criteria to files, tests, locales, targets, and integration points.
4. Implement only approved scope using existing navigation, Koin, StateFlow, storage, resources, theme, and shared UI
   patterns. Avoid dependencies, broad cleanup, and opportunistic refactors.
5. Add/update English, Russian, Kazakh and `AppResource` together. Add focused behavioral tests and deterministic
   screenshot coverage where applicable.
6. If an ambiguity materially affects behavior, stop that portion and request the decision; do not invent policy.
7. Run the narrow checks in `docs/agents/QUALITY_GATES.md`, investigate failures, and never hide or relabel them.
8. Inspect `git diff`, `git diff --check`, and status. Summarize acceptance coverage, files, commands/results,
   unverified behavior, and remaining risks.

The Developer owns production writes. When fixing review/QA feedback, change only reproduced or validated findings
and return the diff for independent re-review/retest.
