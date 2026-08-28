---
name: oquturbo-code-review
description: Independently review an OquTurbo implementation against approved product/design specifications, repository rules, architecture, persistence, localization, tests, and regressions. Use after implementation or material fixes; report findings without silently editing production code.
---

# Review independently

Read the original product/design specifications, `PRODUCT.md`, `AGENTS.md`, architecture reference, diff, and nearby
existing implementation. Act as a reviewer, not the author. Do not modify production code.

Check acceptance completeness; UX states; type-safe navigation/back behavior; Koin assembly and every consumer;
StateFlow/coroutine/lifecycle correctness; game transitions; idempotence/concurrency; record, journal, totals, XP,
training, migration, and storage semantics; three-locale coverage; shared theme/components; edge cases; regression
risk; useful tests; unnecessary abstraction; unrelated changes; and actual verification evidence.

Prioritize correctness and user impact over preferences. Classify each finding **Blocking**, **Important**, or
**Minor** and include:

- file/symbol;
- concrete problem and triggering scenario;
- why it matters;
- suggested direction (not a stealth patch);
- how to verify the fix.

Separate questions and environment gaps from defects. If no material findings exist, explicitly return `APPROVED`,
summarize reviewed scope and residual risks, and do not invent nits to avoid approval.
