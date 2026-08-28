---
name: oquturbo-product-planning
description: Create evidence-based OquTurbo product specifications and prioritized, bounded GitHub-issue-ready proposals without implementing them. Use for product discovery, idea evaluation, feature scoping, acceptance criteria, or defining a new game before design or development.
---

# Plan OquTurbo product work

1. Read `PRODUCT.md`, `AGENTS.md`, and relevant architecture reference.
2. Inspect the current product and representative analogous code. If GitHub context is available, inspect current/open
   work and avoid duplication; state when it is unavailable.
3. Frame a concrete user/product problem. Do not begin from “add a feature.” When discovery is open-ended, generate
   several candidates and compare user value, complexity, integration breadth, evidence, and risk.
4. Recommend one next task and bound it to one reviewable PR. Do not modify production code.
5. Identify affected systems (games/training/activity/records/XP/Stats/Profile/navigation/DI/storage/locales/products)
   and decisions requiring a human. Never fill unknowns with invented policy.
6. Produce the specification below and stop before design/implementation.

## Output contract

- **Problem**
- **User value**
- **Proposed behavior**
- **Scope**
- **Out of scope**
- **Acceptance criteria** (observable and testable)
- **Edge cases**
- **Dependencies / affected systems**
- **Risks and unresolved decisions**
- **Suggested validation**

For a new game, additionally define the cognitive/user goal, moment-to-moment loop, scoring, difficulty progression,
end condition, replay, record series semantics, Daily Training eligibility/behavior (or explicit exclusion), and
English/Russian/Kazakh localization needs. Separate mechanics approved now from balancing unknowns.
