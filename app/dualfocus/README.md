# Dual Focus launcher assets

`icon-master-1024.png` is the canonical source for the Dual Focus launcher mark. It uses two centered vertical rounded
lanes with circle, triangle, square, and diamond glyphs in the OquTurbo purple, coral, and cream palette. The mark is
opaque, contains no text, trophy, timer, or watermark, and keeps its important geometry inside adaptive-icon masks.

The source was created with the built-in image generation tool using the approved `logo-brand` launcher prompt. The
selected generated source was:

`/home/deploy/.codex/generated_images/01a052d4-14b5-7781-82c4-8c1c01ca0bfe/exec-94a5c8fc-e48a-421d-89ba-0909a73a0497.png`

The canonical master supplies these checked-in assets:

- Play Store: `androidApp/src/main/ic_launcher-playstore.png`
- iOS: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`
- Android adaptive foreground: `androidApp/src/main/res/mipmap-*/ic_launcher_foreground.png`
- Android legacy launcher: `androidApp/src/main/res/mipmap-*/ic_launcher.png`
- Android monochrome launcher: `androidApp/src/main/res/mipmap-*/ic_launcher_monochrome.png`

When changing the master, inspect each derivative at its target size and under an adaptive mask. The adaptive
background remains defined in `androidApp/src/main/res/values/ic_launcher_background.xml`.
