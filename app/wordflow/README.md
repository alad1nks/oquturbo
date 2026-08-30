# Word Flow launcher assets

`icon-master-1024.png` is the canonical approved source for every Word Flow launcher icon. Keep it at 1024 x 1024,
opaque, and within the adaptive-icon safe area. The approved design is a simplified open book containing exactly two
rounded context strokes separated by one rounded blank slot, using the existing navy, coral, and cream visual family.

The master was created with the built-in image generation tool using a precise-object-edit prompt: preserve the
centered open book and flat navy/coral/cream palette; reduce the page content to exactly two context strokes total and
one clearly separated rounded blank slot; keep it readable at 48 px and adaptive-safe; exclude all other page marks,
text, trophies, gradients, shadows, watermarks, transparency, and white outer corners. The generated source output was:

`/home/deploy/.codex/generated_images/01a05140-d0f6-7321-abaf-405a0df48e33/exec-156ac3cb-d339-4a86-a8e3-554bb571b768.png`

The canonical master derives these checked-in assets:

- Play Store: `androidApp/src/main/ic_launcher-playstore.png`
- iOS: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`
- Android adaptive foreground: `androidApp/src/main/res/mipmap-*/ic_launcher_foreground.png`
- Android legacy launcher: `androidApp/src/main/res/mipmap-*/ic_launcher.png`
- Android monochrome launcher: `androidApp/src/main/res/mipmap-*/ic_launcher_monochrome.png`

When the master changes, regenerate and visually inspect every derivative at its target size and under an adaptive
mask. The adaptive background color remains defined in `androidApp/src/main/res/values/ic_launcher_background.xml`.
