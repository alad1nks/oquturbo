# OquTurbo product constitution

## Purpose

OquTurbo is a multilingual collection of short exercises for memory, attention, reaction, visual perception, and
reading. It combines reusable standalone games in a training hub that makes practice, records, activity, and
progress visible without turning preview fixtures into product state.

## Current user loop

1. Home presents the current level, a generated daily-training plan, and recent personal records.
2. A user starts the next daily exercise or chooses a game and mode from Games.
3. A session escalates its challenge according to that game's rules and ends on its defined failure/result state.
4. Completion stores score, correct answers, duration, time, mode/variant, and whether a record was achieved.
5. Correct answers contribute one XP each. Every 500 XP advances a level; displayed ranks span five levels.
6. Home, Stats, and Profile derive progress from persisted activity. Stats presents recent and aggregate activity;
   Profile presents identity, progression, ranks, achievements, unlocks, and preferences.
7. Daily Training sequences a persisted daily plan across supported game modes, marks qualifying entries complete,
   and records completed-training progress. Memory Grid is currently excluded from that plan.

## Current product structure

- **Home:** progress overview, today's training sequence, and recent records.
- **Games:** static catalog of Number Sprint, Wide Eye, Don't Tap, and Memory Grid; each opens a mode/menu flow.
- **Stats:** period summaries, activity, trends, totals, recent history, and game/mode drill-downs.
- **Profile:** identity, XP/level/rank, achievements, titles, personalization, and persisted settings.
- **Game menus and sessions:** select a supported mode/configuration, play, receive answer feedback, see score/record,
  and retry or return. The first three games are also standalone products.

## Evidence-based game principles

- Keep sessions focused and replayable, with a clear ready/active/result lifecycle.
- Increase challenge inside the session (for example speed, sequence length, or task difficulty) using game-specific
  rules; do not impose one progression formula on every game.
- Give immediate correct/incorrect feedback and make the score visible.
- Compare records within the correct series. Modes are distinct; Number Sprint custom configurations preserve a
  variant identity rather than merging unlike attempts.
- Persist only completed sessions and defensible metrics. Never fabricate dates or sessions from legacy records.
- Integrate a new game explicitly across catalog, navigation, DI, activity IDs/mappings, Stats/Profile/Home, and
  optionally Daily Training rather than assuming registration is centralized.
- Provide English, Russian, and Kazakh text through shared resources. Keep gameplay usable across supported shared
  targets and deterministic UI states previewable where practical.

## Product principles

1. Start with a concrete cognitive or user benefit and a measurable behavior, not novelty alone.
2. Prefer meaningful depth and consistency in training/progression over unrelated feature accumulation.
3. Reuse established games, navigation, feedback, persistence, and visual patterns unless divergence creates clear
   user value.
4. Keep screens responsible for distinct user jobs; avoid duplicating catalog, activity, or preference behavior.
5. Preserve trust in progress: score, records, XP, statistics, and training completion must share explicit semantics.
6. Keep changes small enough to validate across reusable product consumers and supported locales/platforms.

## Non-goals without strong justification

- Long-form entertainment, social feeds, competitive multiplayer, monetization, or unrelated productivity tools.
- Progress mechanics that reward unverified activity or inflate XP/records.
- A second design system, navigation framework, DI container, persistence layer, or platform-specific fork of common
  behavior.
- New screens or mechanics solely to fill perceived product gaps without user evidence and acceptance criteria.
- Treating standalone sessions as daily-training completion when they were not launched as training entries.

## Unknowns / needs product decision

- The intended audience segments, accessibility targets beyond platform defaults, and validated outcome measures.
- Desired session-duration targets and balancing standards for each cognitive skill.
- Whether and when Memory Grid should join Daily Training, and what qualifying scores should be.
- Long-term XP/rank cap behavior, rewards, reset policy, and whether all correct answers should remain equally valued.
- Product analytics, privacy/retention policy, synchronization, accounts, and cross-device migration strategy.
- Release-level platform parity requirements and the product policy for old or malformed persisted data.
