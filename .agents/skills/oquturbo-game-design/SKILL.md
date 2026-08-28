---
name: oquturbo-game-design
description: Turn an approved OquTurbo feature or game specification into an implementation-ready UX, interaction-state, and game-mechanics design using existing visual patterns. Use after product scope is approved and before production implementation.
---

# Design an OquTurbo experience

1. Read the approved product specification, `PRODUCT.md`, `AGENTS.md`, and `docs/agents/ARCHITECTURE.md`.
2. Inspect comparable screens/games and inventory reusable `core/ui` and design-system components, resource patterns,
   layouts, and screenshot previews. Preserve the current visual language.
3. Resolve implementation-level UX ambiguity while preserving product behavior. Escalate any material product choice
   as an unresolved decision rather than guessing.
4. Specify hierarchy, components, exact interactions/transitions, navigation/back behavior, copy intent, responsive
   and localization considerations, accessibility-relevant behavior, and acceptance mapping.
5. Cover normal screens' loading, empty, error, success, and disabled states when applicable.
6. For games cover entry/tutorial (if needed), ready, active play, correct/incorrect feedback, pause/interruption when
   applicable, result/game over, retry, record presentation, and mode/difficulty selection.
7. Use screenshots, Appshot, image generation, or previews only when useful and available; the textual specification
   must remain sufficient without them. Do not modify production application code unless the coordinator explicitly
   authorizes it.

Output: comparable-pattern evidence; state/transition table; screen/component specification; copy/localization list;
responsive/accessibility notes; unresolved decisions; and implementation/validation notes.
