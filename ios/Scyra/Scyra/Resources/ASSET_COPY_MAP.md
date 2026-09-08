# iOS Asset Copy Map

This file documents Scyra visual assets prepared for local iOS use. iOS must use local copies under `ios/Scyra/` and must never reference Android resource paths at build time or runtime.

| iOS Name | Type | iOS Location | Android Source | Android Usage Evidence | Notes |
|---|---|---|---|---|---|
| scyraTurtle | Image asset | `Assets.xcassets/scyraTurtle.imageset/scyraTurtle.png` | `android/app/src/main/res/drawable/scyra_turtle.png` | `SkillzTopAppBar.kt` uses `painterResource(id = R.drawable.scyra_turtle)` for the Shell top-bar action. | Copied byte-for-byte and packaged locally. |
| AppIcon-1024 | App icon | `Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` | Android launcher foreground/background resources | Android launcher manifest/resources. | Composited as a no-alpha 1024-point App Store icon using the canonical launcher artwork and light background. |
| material* | Template image sets | `Assets.xcassets/material*.imageset/` | AndroidX Compose Material icon vector sources used by Android screens | `Icons.Filled` and `Icons.Outlined` imports throughout the canonical Compose UI. | Converted to local template-rendered SVG image sets; `ScyraCanonicalIcon` and `ScyraCanonicalLabel` provide the SwiftUI selection layer. |

## Fonts

| iOS Name | Type | iOS Location | Android Source | Android Usage Evidence | Notes |
|---|---|---|---|---|---|
| caveatsb | Font | `Resources/Fonts/caveatsb.ttf` | `android/app/src/main/res/font/caveatsb.ttf` | `SkillzTopAppBar.kt` uses `FontFamily(Font(R.font.caveatsb))` at `30.sp` for the Scyra title. | Copied byte-for-byte, registered in `UIAppFonts`, and resolved by `ScyraTypography`. |

## Assets intentionally skipped

- Android has no additional raster creature or room art beyond the turtle and launcher resources. Those visuals are authored as Compose Canvas/path drawings, so remaining parity work must port the canonical geometry and animation into SwiftUI Canvas rather than substitute unrelated third-party artwork.
