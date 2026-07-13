# OquTurbo

OquTurbo is a Kotlin Multiplatform and Compose Multiplatform monorepo for short memory, attention, reaction,
and reading exercises. It contains one training hub and three standalone games that reuse the same feature modules.

Shared UI and game logic target Android, iOS, Desktop (JVM), and Web through Kotlin/JS and Kotlin/Wasm. User-facing
content is available in English, Russian, and Kazakh.

## Products

| Product ID | Display name | Description |
| --- | --- | --- |
| `oquturbo` | OquTurbo | Training hub combining Number Sprint, Wide Eye, and Don't Tap. |
| `sansprint` | Number Sprint | Number-memory game with Classic, Binary String, and Custom modes. |
| `kenkoz` | Wide Eye | Attention and peripheral-vision training with four modes. |
| `baspa` | Don't Tap | Attention, reaction, and reading game with seven modes. |

OquTurbo provides Home, Games, Stats, and Profile top-level navigation. The standalone products expose their game
menus directly while reusing the same feature modules as the hub.

Each product has Android, Desktop, shared, and Web Gradle modules under [`app/`](./app), as well as an Xcode project
under `app/<product>/iosApp`.

## Supported platforms

- Android 7.0+ (`minSdk 24`, `targetSdk 37`)
- iOS devices and Apple Silicon simulators (`iosArm64` and `iosSimulatorArm64`)
- Desktop/JVM, with DMG, MSI, and DEB packaging configured
- Web through Kotlin/JS and Kotlin/Wasm browser targets

## Tech stack

- Kotlin Multiplatform and Compose Multiplatform
- Material 3 and Compose Resources
- Navigation Compose with `@Serializable` type-safe routes
- Koin for dependency injection
- Lifecycle ViewModel, Coroutines, and `StateFlow`
- DataStore on Android, iOS, and Desktop; browser `localStorage` on Web
- ktlint for code style checks

Dependency versions are defined in the [version catalog](./gradle/libs.versions.toml).

## Project structure

The repository currently contains 32 Gradle modules:

```text
app/
  <product>/
    androidApp/       Android launcher
    desktopApp/       Desktop/JVM launcher and packaging
    shared/           Product navigation graph, DI assembly, and platform entry points
    webApp/           JS and Wasm browser launchers
    iosApp/           Xcode wrapper

feature/
  main/               Root theme, Scaffold, NavHost, and Koin startup
  home/               OquTurbo Home tab
  games/              OquTurbo game catalog
  remembernumber/     Number Sprint game
  remembernumbermenu/ Number Sprint menu
  kenkozgame/         Wide Eye game
  kenkozgamemenu/     Wide Eye menu
  baspagame/          Don't Tap game
  baspagamemenu/      Don't Tap menu

core/
  data/               Repositories
  designsystem/       Basic reusable UI primitives
  ui/                 Compound reusable UI components
  storage/common/     Storage contracts and common implementation
  storage/datastore/  Android, iOS, and JVM persistence
  storage/web/        JS and Wasm persistence

resources/            Shared strings, arrays, and localized content
```

The main dependency direction is:

```text
platform launcher -> app:<product>:shared -> feature -> core:data -> core:storage:common
                                                    -> core:ui / core:designsystem / resources
```

Product `shared` modules select `core:storage:datastore` or `core:storage:web` for the active platform. Feature
modules own their routes, UI, ViewModels, state, and DI definitions; product apps assemble them into navigation graphs.

## Prerequisites

- JetBrains Runtime or JDK 21. The Gradle daemon toolchain is pinned to JetBrains JDK 21.
- Android Studio with Android SDK 37 for Android builds.
- macOS and Xcode for building or running iOS applications.
- A modern browser for the Kotlin/Wasm development server.

Use the checked-in Gradle wrapper for all commands. On Windows, replace `./gradlew` with `gradlew.bat`.

In the commands below, replace `<product>` with one of:

```text
oquturbo | sansprint | kenkoz | baspa
```

## Build and run

### Android

Build a debug APK:

```shell
./gradlew :app:<product>:androidApp:assembleDebug
```

You can run the application from an Android Studio run configuration after selecting the corresponding
`androidApp` module.

### Desktop

Run the Desktop/JVM application:

```shell
./gradlew :app:<product>:desktopApp:run
```

Build a platform package on the corresponding operating system:

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

Open `app/<product>/iosApp/iosApp.xcodeproj` in Xcode, configure signing if necessary, and run the application from
Xcode. The Xcode build phase builds and embeds the appropriate Kotlin framework from the product's `shared` module.

## Checks

Run the repository-wide formatting and verification tasks:

```shell
./gradlew ktlintCheck
./gradlew check
```

Useful product-specific checks include:

```shell
./gradlew :app:<product>:androidApp:test
./gradlew :app:<product>:desktopApp:test
./gradlew :app:<product>:shared:allTests
./gradlew :app:<product>:shared:iosSimulatorArm64Test
```

The repository currently has no test source files, so these tasks primarily provide compilation and configuration
coverage.

## Localization and resources

Shared strings and game content live in
[`resources/src/commonMain/composeResources`](./resources/src/commonMain/composeResources):

- `values/` — English/default resources
- `values-ru/` — Russian resources
- `values-kk/` — Kazakh resources

Portable UI reads them through `AppResource` and Compose `stringResource`. Product-specific Android launcher
resources live in `app/<product>/androidApp/src/main/res`; Web HTML and CSS live in
`app/<product>/webApp/src/webMain/resources`.

## CI/CD

GitHub Actions workflows are located in [`.github/workflows`](./.github/workflows):

- Every pull request runs the common ktlint check.
- OquTurbo PR checks always run because the hub consumes all shared games and infrastructure.
- SanSprint, KenKoz, and Baspa PR checks run only when their app, feature, shared core, resources, or build
  configuration changes.
- Product PR checks build Android, Desktop/JVM, JS, and Wasm, and invoke the configured iOS framework task.
- Pushes to `main` build and publish a signed Android APK. OquTurbo always runs; the standalone products use the
  same path filters as their PR checks.
- A push to `release/<product>/*` builds and publishes a signed Android App Bundle.

Local Android release builds expect `release-key.jks` in the repository root and the following environment variables:

```text
SIGNING_STORE_PASSWORD
SIGNING_KEY_ALIAS
SIGNING_KEY_PASSWORD
```

Then build an APK or App Bundle with:

```shell
./gradlew :app:<product>:androidApp:assembleRelease
./gradlew :app:<product>:androidApp:bundleRelease
```

## Contributing

- Put portable code in `commonMain`; use the existing `expect`/`actual` pattern for platform behavior.
- Keep feature routes and navigation extensions in each feature's `navigation` package.
- Update English, Russian, and Kazakh resources together when adding user-facing text.
- Run `./gradlew ktlintCheck` and the narrowest build tasks covering the affected modules.
- Compile common code on more than one target when practical.

## Further reading

- [Kotlin Multiplatform documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
