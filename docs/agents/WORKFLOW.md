# OquTurbo agent workflow

Use `.agents/skills/oquturbo-feature-pipeline` to coordinate the specialized roles. One logical, bounded feature maps
to one branch and one reviewable PR; specifications and evidence are PR artifacts or issue text, not unrelated
production commits.

## State machine and ownership

`IDEA → PRODUCT_READY → DESIGN_READY → IMPLEMENTED → REVIEW → QA → READY_TO_MERGE`

| State / owner | Required input | Expected output and permitted changes | Success / failure transition |
| --- | --- | --- | --- |
| IDEA / requester | Product observation or request | Problem seed; no code | PM accepts for analysis |
| PRODUCT_READY / Product Manager | Idea, PRODUCT.md, current product/GitHub context | Stable issue-style specification, alternatives, scope, acceptance criteria; docs only | Complete and decision-ready → DESIGN; ambiguity → IDEA |
| DESIGN_READY / Designer | Approved product specification | Reusable-component map and all interaction states; design artifacts only unless authorized | Implementable, unresolved decisions explicit → IMPLEMENTATION; product decision → PM |
| IMPLEMENTED / Developer | Frozen product/design specs | Scoped production code, locales, tests, command evidence | Acceptance implementation complete → REVIEW; material ambiguity → PM/Designer |
| REVIEW / Reviewer | Specs and implementation diff | Independent classified findings; no fixes | APPROVED → QA; REVIEW_FAILED → Developer fixes validated findings → REVIEW |
| QA / QA Engineer | Acceptance criteria, approved implementation | Test matrix, actual evidence, PASS/FAIL/BLOCKED | PASS → FINAL VERIFICATION; FAIL → Developer reproduces/fixes → QA (and REVIEW for material fixes) |
| READY_TO_MERGE / Orchestrator | Review approval, QA pass, quality gates | Consolidated summary, commands, risks, readiness decision | All mandatory gates satisfied; otherwise return to owner or remain blocked |

Keep the approved requirements stable. A newly discovered contradiction may reopen product/design, but the Developer
cannot silently change the goal. Parallelize independent read-only investigation only; never have agents edit the
same production area concurrently.

## Fictional handoff simulation

For a fictional “practice reminder explanation” improvement (not implemented here): the PM establishes that users
misunderstand a current reminder control, bounds the change to explanatory copy, and writes measurable locale and
navigation criteria. The Designer inspects Profile settings and shared text patterns, specifying hierarchy plus
default, disabled, and narrow/localized states. The Developer changes only approved resources/UI and adds an
appropriate deterministic preview/test. The Reviewer compares the diff with both specs and flags any hardcoded copy
or preference-semantic change. QA checks all three locales, persistence unaffected, the relevant screen states, and
automated gates. The orchestrator reports READY_TO_MERGE only with approval and evidence. This simulation confirms
that every handoff carries a stable spec, state inventory, affected systems, evidence, and unresolved decisions.

## Autonomy gaps

| Priority | Current limitation | Why it matters | Proposed future improvement |
| --- | --- | --- | --- |
| P0 | Behavioral tests cover mainly Memory Grid logic; repository/storage/ViewModel flows are sparsely tested | Autonomous changes can compile while breaking records, XP, training, or migration behavior | Add deterministic fake Storage and common tests for activity, training, and representative ViewModels |
| P0 | Cloud sessions do not guarantee Android emulator or macOS/Xcode | Platform lifecycle, restart, IME, back, accessibility, and iOS behavior cannot be proven | Add managed Android device CI and a macOS iOS simulator smoke workflow with stable fixtures |
| P1 | No end-to-end seeded user-state harness | Stats/Profile/Home integration and persistence transitions require brittle manual setup | Add test-only deterministic storage fixtures and cross-feature journey tests |
| P1 | Screenshot coverage is preview/JVM based and visual approval remains human-dependent | Platform rendering and subtle UX regressions can escape | Expand deterministic state coverage and add documented human baseline approval plus targeted device screenshots |
| P1 | CI release Android job depends on secrets and publishes artifacts as part of PR checks | Fork/Cloud verification may be blocked and build validation is coupled to publishing | Split unsigned build verification from credentialed publishing/release jobs |
| P1 | Game registration is distributed across enums, mappings, graph, DI, resources, and statistics | A new game can compile while missing an integration surface | Add focused mapping/registration contract tests before considering a registry refactor |
| P2 | Product outcomes, accessibility targets, and balance metrics are undocumented/unknown | Agents cannot validate whether a mechanic improves cognition or inclusion | Establish human-approved research, accessibility, telemetry/privacy, and balancing policy |
| P2 | GitHub issue/PR permissions and visual tooling vary by session | The pipeline may produce artifacts but cannot always publish or inspect them | Document capability detection and retain a manual issue/PR handoff fallback |

## Recommended pilot

Run a small enhancement to an existing game's result/feedback state—selected by the Product Manager from observed
usability evidence—through all five roles. Keep it to one existing module, three locales, a pure-logic or ViewModel
test, and one deterministic screenshot state. This exercises every handoff without the cross-cutting risk of adding
a game or changing storage/navigation contracts.
