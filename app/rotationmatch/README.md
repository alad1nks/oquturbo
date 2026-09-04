# Rotation Match

Rotation Match launches the shared Rotation Match game directly on Android, Desktop/JVM, iOS, JavaScript, and
WebAssembly. Its platform storage is product-isolated; records and settings do not synchronize with OquTurbo.

## Launcher assets

`icon-master-1024.png` is the canonical textless launcher source. It depicts paired 3x3 filled-cell patterns and a
clockwise rotation arrow in the OquTurbo purple, coral, and cream palette. The important geometry stays inside common
adaptive-icon masks, and the image contains no lettering or watermark.

The master was created with the built-in image-generation tool using a `logo-brand` launcher prompt, then inspected
and resized through the repository's existing launcher pipeline. It supplies:

- Play Store: `androidApp/src/main/ic_launcher-playstore.png`
- iOS: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`
- Android adaptive foreground: `androidApp/src/main/res/mipmap-*/ic_launcher_foreground.png`
- Android legacy launcher: `androidApp/src/main/res/mipmap-*/ic_launcher.png`
- Android monochrome launcher: `androidApp/src/main/res/mipmap-*/ic_launcher_monochrome.png`

When changing the master, inspect each derivative at its target size and under an adaptive mask. The adaptive
background remains defined in `androidApp/src/main/res/values/ic_launcher_background.xml`.
