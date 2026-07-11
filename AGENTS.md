# AGENTS.md

## Project

Kotlin Multiplatform/Compose Multiplatform repository with three products: `oquturbo`, `sansprint`, and `kenkoz`. Each has Android, iOS, Desktop (JVM), and Web entry points; Web is built for JS and Wasm. The three `iosApp` directories are Xcode projects, not Gradle modules.

## Modules and ownership

`settings.gradle.kts` declares 22 modules:

- `app:<product>:{androidApp,desktopApp,shared,webApp}` for each product. Platform launchers delegate to `shared`; `shared/App.kt` assembles the product navigation graph and DI modules.
- `feature:main`: Koin root, app theme, root `Scaffold`, and `NavHost`.
- `feature:remembernumber`, `feature:remembernumbermenu`: feature UI, ViewModel/state, navigation, and DI.
- `core:data`: repositories.
- `core:storage:common`: storage contracts and `StorageImpl`.
- `core:storage:datastore`: Android/iOS/JVM DataStore implementation.
- `core:storage:web`: JS/Wasm `localStorage` implementation.
- `core:designsystem`: basic UI primitives.
- `core:ui`: compound UI components.
- `resources`: shared Compose resources and strings.

`feature/kenkozgamemenu` is a directory only; it has no build file and is not an included module.

## Stack and existing patterns

Dependency versions are defined in `gradle/libs.versions.toml`; consult that file rather than copying versions here. The project uses Compose Multiplatform/Material 3, Koin, Navigation Compose with `@Serializable` routes, Lifecycle ViewModel, Coroutines/Flow, Compose Resources, and DataStore.

Follow the existing dependency direction:

```text
platform app -> app:<product>:shared -> feature -> core:data -> core:storage:common
                                                       ^
                              core:storage:datastore or core:storage:web
```

- ViewModels expose `StateFlow`. Route composables obtain them from Koin and pass state/callbacks to screen composables.
- Feature navigation stays in `feature/*/navigation`: a serializable route plus `navigateTo...` and `...Screen` extensions.
- Koin definitions live in each module's `di` package. Product module lists are assembled in `GetCommonModules.kt`; `expect`/`actual` `GetPlatformModules.kt` selects DataStore or Web storage.
- There is no separate domain/use-case module in the current repository.

## Source sets

- Put portable code in `commonMain`; keep platform APIs out of it.
- Product `shared` modules use `androidMain`, `iosMain`, `jvmMain`, and `webMain`. `webMain` serves both JS and Wasm.
- Shared/core/feature/resource modules generally target Android, `iosArm64`, `iosSimulatorArm64`, JVM, JS browser, and Wasm browser as declared in their build files.
- Exception: `core:storage:datastore` targets Android/iOS/JVM only; `core:storage:web` targets JS/Wasm only.
- Add platform behavior through the existing `expect`/`actual` pattern when a common API needs different implementations.
- iOS Kotlin entry points are `shared/src/iosMain/.../MainViewController.kt`; Swift/Xcode files live under `app/<product>/iosApp`.

## UI, navigation, and resources

- Shared Compose screens live in feature `commonMain`; basic primitives belong in `core:designsystem`, while compound reusable components belong in `core:ui`.
- Keep the current route/screen split: route composables connect ViewModels, while screen/component composables accept state and callbacks.
- Product graphs and start destinations are defined in `app/<product>/shared/App.kt`; leaf UI receives navigation callbacks rather than a `NavController`.
- Shared UI strings live in `resources/src/commonMain/composeResources/values*/strings.xml` for default, Russian, and Kazakh locales. Use `AppResource.String` with `stringResource` and update matching keys in all three files.
- Product names and launcher resources live in each `androidApp/src/main/res`; Web HTML/CSS lives in each `webApp/src/webMain/resources`.

## Commands

Use the checked-in wrapper and replace `<product>` with `oquturbo`, `sansprint`, or `kenkoz`. These tasks were confirmed from Gradle; the README documents the OquTurbo run commands.

```shell
./gradlew ktlintCheck
./gradlew check
./gradlew :app:<product>:androidApp:assembleDebug
./gradlew :app:<product>:androidApp:test
./gradlew :app:<product>:desktopApp:run
./gradlew :app:<product>:desktopApp:test
./gradlew :app:<product>:webApp:wasmJsBrowserDevelopmentRun
./gradlew :app:<product>:webApp:jsBrowserDevelopmentRun
./gradlew :app:<product>:shared:allTests
./gradlew :app:<product>:shared:iosSimulatorArm64Test
```

No test source files currently exist, so test tasks provide compilation/configuration coverage but run no project tests. The repository provides no CLI command for building the iOS app; README says to run it from the IDE or open `app/oquturbo/iosApp` in Xcode. The other two products also contain Xcode projects, but their workflow is not documented.

Android release tasks use `release-key.jks` and the environment variables `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, and `SIGNING_KEY_PASSWORD`.

## Style and verification

- `.editorconfig` requires UTF-8, LF, final newline, trimmed trailing whitespace, 120-character lines, official Kotlin style, and trailing commas. It also records the project's disabled ktlint rules.
- The root build applies ktlint to every subproject with `ignoreFailures=false`.
- After Kotlin or Gradle edits, run `./gradlew ktlintCheck` and the narrowest build/check task covering the changed module and target.
- After `commonMain` changes, compile affected consumers on more than one target when practical. Do not report iOS verification unless Apple tooling actually ran.
- For resource changes, check the three shared locale files. For DI/navigation changes, check every affected product's module list, graph, arguments, start destination, and back behavior.
- Preserve unrelated work and inspect `git diff`/`git status` before finishing.

## Known limitations

- `kenkoz/shared/App.kt` uses `startDestination = ""` and registers no destinations.
- Desktop DataStore writes all products to `${java.io.tmpdir}/oquturbo.preferences_pb`; products can share data and the file is temporary.
- `RememberNumberViewModel` calls `availableDigits.random()` without validating route arguments.
- `feature:main/MainScreen.kt` starts Koin inside Compose and ignores the root `Scaffold` content padding.
- `app/oquturbo/webApp` uses the leftover package `com.alad1nks.startkmp`.
- Each `shared/webpack.config.d/watch.js` documents a temporary workaround for KT-80582.
- `gradle.properties` currently has `android.useAndroidX=truedistributionBase=GRADLE_USER_HOME` on one line; do not silently fix it during unrelated work.

## Require a separate request

Do not change release signing/credentials, application or bundle IDs, package namespaces, version codes/names, platform targets, dependency/plugin/wrapper versions, Xcode project settings, storage keys/file format, or navigation route arguments as incidental cleanup. Do not remove the KT-80582 workaround without first confirming that the configured Kotlin version resolves it.
