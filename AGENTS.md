# AGENTS.md

Operational rules for AI coding agents. Human-facing project documentation belongs in `README.md`.

## Git workflow

- Start each task from `main` by creating a separate branch with a short, descriptive kebab-case name that includes
  the task intent, for example `add-logout-button` or `fix-crash-on-start`.
- Open pull requests back into `main` with a concise human-readable title that summarizes the change, for example
  `Add logout button` or `Fix crash on start`.

## Sources of truth

- Treat `settings.gradle.kts`, module `build.gradle.kts` files, and `gradle/libs.versions.toml` as the sources of
  truth for modules, targets, dependencies, and versions. Do not copy fixed counts or versions into this file.
- A shared feature can be consumed by OquTurbo and a standalone product. Find and verify every consumer before
  changing shared navigation, DI, storage, or behavior.

## Architecture

- Preserve the dependency direction: `platform launcher -> app:<product>:shared -> feature -> core`.
  Platform `shared` source sets select `core:storage:datastore` for Android/iOS/JVM and `core:storage:web` for
  JS/Wasm.
- Put portable implementation in `commonMain`. Keep platform APIs in the corresponding platform source set and
  use the existing `expect`/`actual` pattern when common code needs platform behavior.
- Keep feature routes and navigation extensions in `feature/*/navigation`. When a feature has a ViewModel, its
  route obtains it from Koin and passes state and callbacks to screen composables. Leaf UI must not receive a
  `NavController`.
- Product graphs and start destinations belong in `app/<product>/shared/App.kt`. Koin definitions belong in each
  module's `di` package; product module lists belong in `GetCommonModules.kt` and `GetPlatformModules.kt`.
- Do not introduce a new architectural layer as incidental refactoring.
- Home, Stats, and Profile derive runtime state from persisted game sessions and profile preferences. Games remains
  a static product catalog; Stats/Profile demo fixtures are for previews only. Daily training is not implemented,
  so do not infer completed trainings or streaks from standalone game sessions.
- `GameActivityRepository` keeps a bounded session journal and separate cumulative aggregates. Use the journal for
  recent charts and history, and `observeTotals()` for all-time counts, duration, and per-series averages.

## UI and resources

- Keep `OquTurboTheme`, color schemes, typography, shapes, and basic design primitives in `core:designsystem`.
  Put compound reusable UI in `core:ui`, with one focused component per file instead of a catch-all
  `AppComponents.kt`.
- Apply `OquTurboTheme` at every app root and in every `@Preview`; do not wrap previews in a bare `MaterialTheme`
  or define product theme colors inside feature modules.
- Persist language selection through `SettingsRepository` and apply it only at the shared app root through
  `LocalAppLocale`. Keep its platform behavior in the existing Android, iOS, JVM, JS, and Wasm source sets; Web
  launchers must install the `navigator.languages` override before loading the application bundle.
- Use `Modifier.appBackground()` for the shared background. Do not add a wrapper composable whose only purpose is
  drawing it. Keep the root `Scaffold` and shared top bars transparent so the background extends under the status
  bar.
- Use shared `AppTopBar` and `AppBackButton` for back navigation. The Back control starts `24.dp` from the screen
  edge. Game-menu top bars use enter-always scrolling.
- Horizontal scrolling content fills the parent width and uses `contentPadding` for side spacing. Sibling cards in
  the same `Row` must match the tallest item.
- Do not hardcode user-facing text. Update matching keys in `values/`, `values-ru/`, and `values-kk/`, expose them
  through the manually maintained `AppResource.String`, `Array`, or `Plural` facade, and read them with Compose
  Resources. Do not edit generated resource or build output.

## Verification

- Use the checked-in `./gradlew`. After Kotlin or Gradle changes, run `./gradlew ktlintCheck` and the narrowest
  compile or build task covering every affected module and product consumer.
- After `commonMain` changes, compile more than one applicable target when practical. Do not report iOS
  verification unless Apple tooling actually ran.
- For navigation or DI changes, verify every affected product's graph, module list, arguments, start destination,
  and back behavior. For resource changes, verify all three locale files and the `AppResource` facade.
- Test source sets are currently empty. Do not describe a passing test task as behavioral test coverage.
- Preserve unrelated work. Inspect `git status`, `git diff`, and `git diff --check` before finishing.

## Change boundaries

A separate user request is required before changing signing or credentials, application or bundle IDs, package
namespaces, version codes or names, platform targets, dependency/plugin/wrapper versions, Xcode project settings,
storage keys or formats, or navigation route arguments. Do not remove the KT-80582 webpack workaround without first
confirming that the configured Kotlin version resolves it.
