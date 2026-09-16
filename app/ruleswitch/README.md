# Rule Switch

Classify a digit as even or odd, then below or above 5. The rules alternate after each correct answer. One wrong answer or timeout ends the run. This standalone product launches the same
portable `feature/ruleswitch` Ready screen used by OquTurbo, without a root Back button.

## Build and run

Use JDK 21 and the checked-in wrapper from the repository root (Android additionally needs SDK 37).

```shell
./gradlew :app:ruleswitch:androidApp:assembleDebug
./gradlew :app:ruleswitch:desktopApp:run
./gradlew :app:ruleswitch:webApp:jsBrowserDevelopmentRun
./gradlew :app:ruleswitch:webApp:wasmJsBrowserDevelopmentRun
```

Open `iosApp/iosApp.xcodeproj` on macOS/Xcode for the iOS wrapper and `AppRuleSwitch` framework.
The application identity is `com.alad1nks.ruleswitch`; the new app uses the sibling initial version 1.0.0.

## Storage

Android app-private storage and the iOS application container isolate settings, records and activity. Desktop uses
`~/.ruleswitch/ruleswitch.preferences_pb`; browsers use the `ruleswitch` storage namespace. These products do not
synchronize records with OquTurbo or sibling apps. Unfinished attempts are abandoned on process recreation.

## Reproducible launcher assets

`branding/GenerateIcons.java` defines opposing arrows with an outlined square and circle, and generates the SVG master and
platform raster exports using Java2D. Run `java app/ruleswitch/branding/GenerateIcons.java` from the repository root.
Android adaptive/legacy/monochrome, iOS, desktop and browser assets share the drawing, without text or font dependencies.

## Platform validation

Shared rules and UI are portable; automated JVM, Android, JS and Wasm builds validate their targets.
Android and browser runtime evidence is recorded in the feature PR. iOS source/framework checks do not establish
iOS runtime behavior; device validation requires macOS/Xcode and is not performed on a Linux host.
