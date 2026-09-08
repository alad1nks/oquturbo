# Number Trail

Find each visible number in ascending order before the board deadline. This standalone product launches the same
portable `feature/numbertrail` Ready screen used by OquTurbo, without a root Back button.

## Build and run

Use JDK 21 and the checked-in wrapper from the repository root (Android additionally needs SDK 37).

```shell
./gradlew :app:numbertrail:androidApp:assembleDebug
./gradlew :app:numbertrail:desktopApp:run
./gradlew :app:numbertrail:webApp:jsBrowserDevelopmentRun
./gradlew :app:numbertrail:webApp:wasmJsBrowserDevelopmentRun
```

Open `iosApp/iosApp.xcodeproj` on macOS/Xcode for the iOS wrapper and `AppNumberTrail` framework.
The application identity is `com.alad1nks.numbertrail`; the new app uses the sibling initial version 1.0.0.

## Storage

Android app-private storage and the iOS application container isolate settings, records and activity. Desktop uses
`~/.numbertrail/numbertrail.preferences_pb`; browsers use the `numbertrail` storage namespace. These products do not
synchronize records with OquTurbo or sibling apps. Unfinished attempts are abandoned on process recreation.

## Reproducible launcher assets

`branding/GenerateIcons.java` defines a font-free numbered 2×2 tile drawing and generates the SVG master and all raster
exports using Java2D. Run `java app/numbertrail/branding/GenerateIcons.java` from the repository root. No image-generation
service, font, or new application dependency is used. `branding/icon-master.svg` and `icon-master-1024.png` show the full
master. Android adaptive/legacy/monochrome, Play Store, iOS, desktop and browser exports share that drawing. Numerals
are vector strokes; monochrome exports cut them out so they survive monochrome tinting. Inspect regenerated assets
at small size and under adaptive masks before accepting changes.
