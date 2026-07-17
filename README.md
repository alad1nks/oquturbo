# OquTurbo

OquTurbo is a Kotlin Multiplatform and Compose Multiplatform monorepo for short memory, attention, reaction,
visual-perception, and reading exercises. It contains the OquTurbo training hub and three standalone games that
reuse the same feature modules.

Shared UI and game logic run on Android, iOS, Desktop/JVM, and the browser through Kotlin/JS and Kotlin/Wasm.
User-facing content is available in English, Russian, and Kazakh.

## Products

| Product ID | Display name | Description |
| --- | --- | --- |
| `oquturbo` | OquTurbo | Hub with Home, Games, Stats, and Profile tabs plus all three games. |
| `sansprint` | Number Sprint | Number-memory game with Classic, Binary String, and Custom modes. |
| `kenkoz` | Wide Eye | Attention and peripheral-vision game with four modes. |
| `baspa` | Don't Tap | Attention, reaction, and reading game with seven modes. |

The standalone products open their own game menus directly. OquTurbo adds a shared catalog and top-level
navigation around the same Number Sprint, Wide Eye, and Don't Tap implementations.

## Current implementation status

The playable games and per-mode records are connected to persistent storage. The OquTurbo progression screens are
implemented visually, but several of their data sources are still placeholders:

- Home uses a static level, rank, daily-training card, and recent records. Starting the daily training currently
  opens Games; there is no training orchestrator yet.
- Games uses a static catalog of available and upcoming games.
- Stats uses `DemoStatsDataSource`, including its periods, activity, charts, skills, history, and detail screens.
- Profile uses an in-memory `ProfileDemoStore`. Identity, XP, ranks, achievements, titles, and personalization are
  not yet derived from completed games and are lost when the process stops.
- Home and Profile do not yet share one progression source.
- Dark theme is persisted. The language row currently reflects the system default; sound, vibration, and reminder
  switches are local UI state only.

This separation is intentional: demo and static UI state is isolated from the repositories that store real game
records, so the screens can later be connected to a product data model without changing their visual components.

## Supported platforms

- Android 7.0+ (`minSdk 24`, `compileSdk 37`, `targetSdk 37`)
- iOS devices and Apple Silicon simulators (`iosArm64` and `iosSimulatorArm64`)
- Desktop/JVM, with DMG, MSI, and DEB packaging configured
- Web through Kotlin/JS and Kotlin/Wasm browser targets

Every product has Android, Desktop, shared, and Web Gradle modules. Every product also has an Xcode project under
`app/<product>/iosApp`; Xcode projects are wrappers rather than Gradle modules.

## Tech stack

- Kotlin Multiplatform and Compose Multiplatform
- Material 3 and a shared `OquTurboTheme`
- Compose Resources for strings, plurals, and game content
- Navigation Compose with `@Serializable` type-safe routes
- Koin for dependency injection
- Lifecycle ViewModel, Coroutines, and `StateFlow`
- DataStore on Android, iOS, and Desktop; browser `localStorage` on Web
- ktlint for code style checks

Dependency and plugin versions are defined in the [version catalog](./gradle/libs.versions.toml).

## Project architecture

The current module layout is declared in [`settings.gradle.kts`](./settings.gradle.kts):

```text
app/
  <product>/
    androidApp/       Android launcher
    desktopApp/       Desktop/JVM launcher and native packaging
    shared/           Product navigation graph, DI assembly, and platform entry points
    webApp/           JS and Wasm browser launchers
    iosApp/           Xcode wrapper (not a Gradle module)

feature/
  main/               Koin application, shared theme application, root Scaffold, and NavHost
  home/               OquTurbo Home tab
  games/              OquTurbo game catalog
  stats/              OquTurbo activity, result trends, and game/mode details
  profile/            OquTurbo progression, identity, achievements, and settings
  remembernumber/     Number Sprint game
  remembernumbermenu/ Number Sprint menu
  kenkozgame/         Wide Eye game
  kenkozgamemenu/      Wide Eye menu
  baspagame/          Don't Tap game
  baspagamemenu/      Don't Tap menu

core/
  data/               Repositories for settings and game records
  designsystem/       OquTurboTheme, color schemes, typography, and shapes
  ui/                 Shared background, app bars, headers, and compound game components
  storage/common/     Storage contracts and common implementation
  storage/datastore/  Android, iOS, and JVM DataStore backend
  storage/web/        JS and Wasm localStorage backend

resources/            Shared English, Russian, and Kazakh Compose resources
```

The main dependency flow is:

```text
platform launcher -> app:<product>:shared -> feature
feature -> core:ui / core:designsystem / resources
stateful feature -> core:data -> core:storage:common
platform source set -> core:storage:datastore or core:storage:web -> core:storage:common
```

Each product's `shared/App.kt` defines its start destination and navigation graph. `GetCommonModules.kt` assembles
common Koin modules, while `expect`/`actual` `GetPlatformModules.kt` selects the persistence backend. Feature modules
own their UI and navigation routes. Stateful games, Stats, and Profile use Koin-provided ViewModels and `StateFlow`;
simple catalogs and menus can remain stateless. There is currently no separate domain or use-case layer.

Portable code lives in `commonMain`. Platform entry points and APIs stay in their Android, iOS, JVM, or Web source
sets; shared platform differences use the existing `expect`/`actual` pattern.

## Persistence

The common storage API currently persists dark-theme selection and game records:

- Number Sprint records are keyed by number length and the available digit set.
- Wide Eye and Don't Tap records are keyed by game mode.
- Android writes a Preferences DataStore file in the app's files directory.
- iOS writes the same DataStore file in the app's documents directory.
- Desktop writes `${java.io.tmpdir}/oquturbo.preferences_pb`.
- JS and Wasm use browser `localStorage`.

Stats analytics and Profile metaprogression are not persisted yet; see
[Current implementation status](#current-implementation-status).

## Prerequisites

- Android Studio and Android SDK 37 for Android development
- JetBrains Runtime or JDK 21; the Gradle daemon toolchain is pinned to JetBrains JDK 21 and can be provisioned
  through the configured Foojay resolver
- macOS and Xcode for running an iOS application
- A modern browser for the JS or Wasm development server

Use the checked-in Gradle wrapper for every command. On Windows, replace `./gradlew` with `gradlew.bat`.

In the examples below, replace `<product>` with one of:

```text
oquturbo | sansprint | kenkoz | baspa
```

## Build and run

### Android

Build a debug APK:

```shell
./gradlew :app:<product>:androidApp:assembleDebug
```

Install it on a connected device or emulator:

```shell
./gradlew :app:<product>:androidApp:installDebug
```

You can also select the corresponding `androidApp` run configuration in Android Studio.

### Desktop

Run the Desktop/JVM application:

```shell
./gradlew :app:<product>:desktopApp:run
```

Build a native package on the matching host operating system:

```shell
./gradlew :app:<product>:desktopApp:packageDmg
./gradlew :app:<product>:desktopApp:packageMsi
./gradlew :app:<product>:desktopApp:packageDeb
```

### Web

Run the Wasm application for modern browsers:

```shell
./gradlew :app:<product>:webApp:wasmJsBrowserDevelopmentRun
```

Run the JavaScript application:

```shell
./gradlew :app:<product>:webApp:jsBrowserDevelopmentRun
```

Build production browser distributions:

```shell
./gradlew :app:<product>:webApp:wasmJsBrowserDistribution
./gradlew :app:<product>:webApp:jsBrowserDistribution
```

### iOS

Open `app/<product>/iosApp/iosApp.xcodeproj` in Xcode, configure signing if necessary, choose a device or Apple
Silicon simulator, and run the application. The Xcode build phase builds and embeds the Kotlin framework from the
product's `shared` module.

Gradle can compile or link the shared iOS framework, but building and signing the complete iOS application remains
an Xcode workflow.

## Checks and test status

Run repository-wide style and verification tasks with:

```shell
./gradlew ktlintCheck
./gradlew check
```

Useful product checks include:

```shell
./gradlew :app:<product>:androidApp:test
./gradlew :app:<product>:desktopApp:test
./gradlew :app:<product>:shared:compileKotlinJvm
./gradlew :app:<product>:shared:compileKotlinWasmJs
./gradlew :app:<product>:shared:allTests
./gradlew :app:<product>:shared:iosSimulatorArm64Test
```

The repository currently has no test source files. Test tasks therefore provide compilation and configuration
coverage, not behavioral regression coverage. iOS simulator tasks require compatible Apple tooling.

## Localization and resources

Shared strings, plurals, arrays, and game content live in
[`resources/src/commonMain/composeResources`](./resources/src/commonMain/composeResources):

- `values/` — English/default resources
- `values-ru/` — Russian resources
- `values-kk/` — Kazakh resources

[`AppResource.kt`](./resources/src/commonMain/kotlin/com/alad1nks/oquturbo/resources/AppResource.kt) is a manually
maintained facade over generated Compose resources. A new resource must be added to all three locale files and
exposed through `AppResource.String`, `AppResource.Plural`, or `AppResource.Array` before feature code can use it.

Product-specific Android launcher resources live in `app/<product>/androidApp/src/main/res`. Browser HTML and CSS
live in `app/<product>/webApp/src/webMain/resources`.

## CI/CD and Android releases

GitHub Actions workflows are located in [`.github/workflows`](./.github/workflows):

- Every pull request runs the common ktlint check.
- OquTurbo product checks run for every pull request; standalone product checks use path filters.
- Product pull-request workflows build Android and JVM artifacts, build JS and Wasm jars, and invoke the configured
  iOS framework task.
- Pushes to `main` build signed Android APKs. OquTurbo always runs; standalone products use path filters.
- Pushes to `release/<product>/*` build signed Android App Bundles.
- Release artifacts are uploaded to GitHub Actions and then committed to `alad1nks/alad1nks.github.io` by the
  workflow's post-build job.

Local release builds expect `release-key.jks` in the repository root and these environment variables:

```text
SIGNING_STORE_PASSWORD
SIGNING_KEY_ALIAS
SIGNING_KEY_PASSWORD
```

Do not commit the keystore. Build an APK or App Bundle with:

```shell
./gradlew :app:<product>:androidApp:assembleRelease
./gradlew :app:<product>:androidApp:bundleRelease
```

## Known technical limitations

- Every Desktop product uses the same temporary DataStore path,
  `${java.io.tmpdir}/oquturbo.preferences_pb`; Desktop products can share records and the data is not durable.
- Number Sprint assumes that route arguments contain at least one available digit. An empty set reaches
  `availableDigits.random()` and fails.
- The OquTurbo Web entry point still uses the legacy package `com.alad1nks.startkmp`.
- Each product keeps a temporary `shared/webpack.config.d/watch.js` workaround for KT-80582.

## Contributing

- Use `settings.gradle.kts`, module build files, and the version catalog as the current sources of truth.
- Put portable code in `commonMain`; use platform source sets and the existing `expect`/`actual` pattern for
  platform behavior.
- Keep feature routes and navigation extensions in each feature's `navigation` package. Product graphs belong in
  `app/<product>/shared/App.kt`.
- Update English, Russian, and Kazakh resources together.
- Follow [`.editorconfig`](./.editorconfig), run `./gradlew ktlintCheck`, and run the narrowest builds covering every
  affected product and target.
- Instructions specific to AI coding agents are intentionally kept in [`AGENTS.md`](./AGENTS.md).

## Further reading

- [Kotlin Multiplatform documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
