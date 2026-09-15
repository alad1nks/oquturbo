# Symbol Count

Count the target shape in a mixed field and choose its number before time runs out. One wrong answer or timeout ends the run. This standalone product launches the same
portable `feature/symbolcount` Ready screen used by OquTurbo, without a root Back button.

## Build and run

Use JDK 21 and the checked-in wrapper from the repository root (Android additionally needs SDK 37).

```shell
./gradlew :app:symbolcount:androidApp:assembleDebug
./gradlew :app:symbolcount:desktopApp:run
./gradlew :app:symbolcount:webApp:jsBrowserDevelopmentRun
./gradlew :app:symbolcount:webApp:wasmJsBrowserDevelopmentRun
```

Open `iosApp/iosApp.xcodeproj` on macOS/Xcode for the iOS wrapper and `AppSymbolCount` framework.
The application identity is `com.alad1nks.symbolcount`; the new app uses the sibling initial version 1.0.0.

## Storage

Android app-private storage and the iOS application container isolate settings, records and activity. Desktop uses
`~/.symbolcount/symbolcount.preferences_pb`; browsers use the `symbolcount` storage namespace. These products do not
synchronize records with OquTurbo or sibling apps. Unfinished attempts are abandoned on process recreation.

## Reproducible launcher assets

`branding/GenerateIcons.java` defines four geometric shapes with a three-dot tally, and generates the SVG master and
platform raster exports using Java2D. Run `java app/symbolcount/branding/GenerateIcons.java` from the repository root.
Android adaptive/legacy/monochrome, iOS, desktop and browser assets share the drawing, without text or font dependencies.
