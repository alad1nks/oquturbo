# AGENTS.md

Durable repository rules for coding agents. Read [PRODUCT.md](PRODUCT.md) before making product decisions and
[docs/agents/WORKFLOW.md](docs/agents/WORKFLOW.md) when running the role pipeline.

## Project map

- `app/oquturbo/shared`: OquTurbo root, navigation graph, and DI assembly; sibling `app/*` products also consume
  shared games.
- `feature/{home,games,stats,profile}`: OquTurbo top-level experiences.
- `feature/*game` and `feature/*menu`: portable games, modes, screens, ViewModels, and routes.
- `core/data`: activity, record, progression, training, profile, and settings repositories.
- `core/designsystem`, `core/ui`: theme/primitives and shared compound UI; `resources`: shared localized resources.
- `core/storage/{common,datastore,web}`: contracts and platform persistence. `screenshot-tests`: JVM visual tests.

`settings.gradle.kts`, module build files, and `gradle/libs.versions.toml` are authoritative for modules, targets,
dependencies, and versions. See [docs/agents/ARCHITECTURE.md](docs/agents/ARCHITECTURE.md) for extension points.

## Engineering rules

- Preserve `platform launcher -> app:<product>:shared -> feature -> core`. Put portable work in `commonMain` and
  platform APIs in the matching source set, using existing `expect`/`actual` patterns.
- Use type-safe Navigation Compose routes in each feature's `navigation` package. Routes obtain Koin ViewModels and
  pass state/callbacks to leaf UI; leaf composables do not receive `NavController`.
- Put Koin definitions in module `di` packages and product assembly in `GetCommonModules.kt` /
  `GetPlatformModules.kt`. Verify every OquTurbo and standalone consumer of shared behavior.
- Before adding an abstraction or component, locate the closest analogous implementation. Use Number Sprint and
  Memory Grid plus [ARCHITECTURE.md](docs/agents/ARCHITECTURE.md) as canonical game examples.
- Reuse `OquTurboTheme`, `Modifier.appBackground()`, and components in `core/ui`. Keep product roots and previews
  themed. Do not replace the visual language unless explicitly requested.
- Never hardcode user-facing text. Add matching entries to `resources/.../values/`, `values-ru/`, and `values-kk/`,
  expose them through the manually maintained `AppResource` facade, and use Compose Resources.
- Use the activity journal for recent history/charts and `observeTotals()` for all-time aggregates. Do not change
  storage keys/formats, route arguments, IDs, versions, targets, signing, or credentials without a separate request.

## Verification

Use the checked-in wrapper and the narrowest relevant tasks. Verified commands and scope are in
[QUALITY_GATES.md](docs/agents/QUALITY_GATES.md).

```shell
./gradlew ktlintCheck
./gradlew test
./gradlew :app:oquturbo:shared:compileKotlinJvm :app:oquturbo:shared:compileKotlinWasmJs
./gradlew :screenshot-tests:verifyRoborazziJvm
```

Behavioral changes require focused unit tests for testable logic and deterministic screenshot coverage for material
shared-UI states. Empty test source sets are compilation coverage, not behavioral evidence. Device-only acceptance
criteria must be reported as unverified unless actually exercised.

## Scope and Git

- Start from `main` on a short kebab-case task branch and open a concise PR back to `main`.
- Keep one bounded concern per PR. Avoid unrelated cleanup, speculative architecture, and new dependencies without
  a documented need. Preserve compatibility and unrelated work.
- Before finishing, inspect `git status`, `git diff`, and `git diff --check`; report failures honestly.
