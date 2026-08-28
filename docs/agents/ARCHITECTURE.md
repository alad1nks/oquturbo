# Architecture reference for agents

## Module map and platforms

`settings.gradle.kts` is the module inventory. The monorepo contains OquTurbo plus standalone `sansprint`, `kenkoz`,
and `baspa` products. Each has Android, Desktop/JVM, shared, and JS/Wasm web Gradle modules and an iOS Xcode wrapper.
Common Kotlin supports Android, iOS Arm64/simulator Arm64, JVM, JS, and Wasm.

The dependency direction is launcher → product `shared` → feature → core/resources. `app/oquturbo/shared/App.kt`
owns the OquTurbo graph and top-level Home/Games/Stats/Profile destinations. `GetCommonModules.kt` assembles Koin
modules; platform actuals choose DataStore for Android/iOS/JVM and browser local storage for web.

## Feature and state pattern

Features normally contain `navigation`, `ui`, optional `model`, and optional `di` packages. Serializable type-safe
routes and NavGraphBuilder extensions live in `navigation`. A route resolves its lifecycle ViewModel with Koin,
collects `StateFlow`, and passes immutable state and callbacks into a screen. Navigation remains in route/product
code, not leaf UI. Stateless catalogs/menus need no artificial ViewModel or domain layer.

`feature/home/ui/HomeViewModel.kt` is a representative non-game stateful screen. Stats uses
`RepositoryStatsDataSource`; Profile combines stored preferences with activity-derived progression. Preview-only
fixtures are isolated under Stats `demo` and `ProfileDemoData.kt`.

## Games and registration

Representative established implementations:

- **Number Sprint:** `feature/remembernumber` + `feature/remembernumbermenu`. Its ViewModel models Initial, Reading,
  Writing, and Mistake states; mode/configuration is expressed by digit set and length. It uses a legacy
  `RememberNumberRepository` record and records normalized activity, including a variant ID for custom settings.
- **Memory Grid:** `feature/memorygrid` + `feature/memorygridmenu`. Pure `MemoryGridGame` logic drives Ready,
  ShowingSequence, AwaitingInput, RoundSuccess, and GameOver; `MemoryGridViewModel` owns timing and activity writes.
  `MemoryGridGameTest.kt` demonstrates focused common tests.
- **Wide Eye / Don't Tap:** `feature/kenkozgame` and `feature/baspagame` demonstrate enum-based modes, localized
  content, Koin ViewModels, records, telemetry, and Daily Training integration. Their menu modules are separate and
  are also consumed by standalone apps.

There is no single game plugin registry. Adding a game normally requires auditing:

1. game/menu modules and their Gradle dependencies;
2. `GameId`, `GameModeId`, session/record mappings, and Stats/Profile/Home resource mappings;
3. `TrainingGame`, `GamesUiState`, catalog artwork/text, and navigation handling;
4. `app/oquturbo/shared/App.kt` graph and `GetCommonModules.kt`;
5. Daily Training candidate generation and route conversion only when explicitly specified;
6. all standalone product graphs/module lists if the feature is shared;
7. all three locale files and `resources/.../AppResource.kt`.

## Persistence, scores, and progression

`core/storage/common/Storage.kt` is the contract. `core/storage/datastore` implements Preferences DataStore;
`core/storage/web` implements browser localStorage. Repositories in `core/data` isolate storage from features.

`GameActivityRepository` records completed sessions in a versioned payload. It retains the latest 1,000 journal
entries plus separate cumulative totals, preventing old activity from disappearing from all-time statistics. A
series key is game + mode + optional variant. A record is the maximum positive score per series, and a claimed new
record is rechecked against stored data. Legacy per-game repositories remain part of compatibility paths.

Progress is derived from cumulative correct answers: one correct answer equals one XP, 500 XP equals a level, and
Home/Profile map five levels to each displayed rank. This is repository behavior, not a general balancing mandate.

## Daily Training

`DailyTrainingRepository` persists a versioned plan for the current UTC epoch day and cumulative training progress.
Home ensures a plan exists and launches its next incomplete entry. Number Sprint, Wide Eye, and Don't Tap training
routes carry an entry ID and required score; qualifying results complete the entry and navigate to the next entry or
`DailyTrainingCompleteScreen`. Memory Grid is explicitly rejected by the current training navigation. Ordinary
game sessions still feed activity/XP, but do not complete training entries.

## UI and localization

`core/designsystem` owns `OquTurboTheme`, color schemes, typography, and shapes. `core/ui/component` contains focused
shared components including app bars, background, game header, score badge, result card, menu item, and state
overlays. Product roots and previews apply the theme. Shared UI uses `Modifier.appBackground()` and transparent
root chrome.

Compose resources live in `resources/src/commonMain/composeResources/values` (English), `values-ru`, and
`values-kk`. The manually maintained `AppResource.kt` facade exposes strings, arrays, and plurals. Language selection
is stored by `SettingsRepository` and applied once through `feature/main`'s platform `LocalAppLocale` actuals.

## Tests and extension evidence

- Common behavioral tests currently exist under `feature/memorygrid/src/commonTest`.
- `screenshot-tests` uses Roborazzi on JVM and includes deterministic `@ScreenshotPreview` states for games and
  Home/Games/Stats/Profile. It is visual regression evidence, not device end-to-end coverage.
- Most other test source sets are empty; Gradle `test` is mainly compilation/configuration coverage there.
- Root ktlint configuration is in `build.gradle.kts`; CI commands are documented in `QUALITY_GATES.md`.

For a normal feature, begin with the closest feature's route/ViewModel/screen/DI structure. For a game, prefer the
separation shown by Memory Grid (pure rules + lifecycle ViewModel) while retaining compatibility patterns required
by an existing shared game. Do not introduce a new layer merely to make examples uniform.
