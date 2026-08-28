---
name: oquturbo-feature-pipeline
description: Coordinate a complete OquTurbo idea-to-merge lifecycle across Product Manager, Designer, Developer, Reviewer, and QA roles with controlled handoffs and failure loops. Use to orchestrate one bounded feature; do not use this preparation task to invent or execute a real feature.
---

# Coordinate the feature pipeline

Read `docs/agents/WORKFLOW.md` and maintain a visible state and artifact ledger:

`IDEA → PRODUCT_READY → DESIGN_READY → IMPLEMENTED → REVIEW_PASSED → QA_PASSED → READY_TO_MERGE`

1. Invoke `oquturbo-product-planning`; freeze the approved issue-style specification.
2. Invoke `oquturbo-game-design`; require complete states and explicit unresolved decisions.
3. Invoke `oquturbo-implement-feature`; only this role writes production code.
4. Invoke `oquturbo-code-review` independently.
5. Invoke `oquturbo-qa` independently after approval.
6. Re-run applicable `QUALITY_GATES.md` checks and reconcile all evidence.

On findings: `REVIEW_FAILED → Developer fixes validated findings → Review again`. On reproduced QA defects:
`QA_FAILED → Developer fixes → QA again → Review again if the fix is material`. Reopen product/design only when a
discovery genuinely invalidates the stable specification; never let implementation redefine requirements.

Use project custom agents when available. Use subagents for independent read-heavy work where helpful, but never
allow concurrent writes to the same production code. Keep one logical feature on one branch/PR.

Final output: feature summary; product/design artifact status; implementation summary; review result; QA result;
exact verification commands/results; unresolved risks/limitations; transition ledger; and an explicit
`READY_TO_MERGE: YES|NO`.
