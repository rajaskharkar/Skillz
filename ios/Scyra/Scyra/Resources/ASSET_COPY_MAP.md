# iOS Asset Copy Map

This file documents Scyra visual assets prepared for local iOS use. iOS must use local copies under `ios/Scyra/` and must never reference Android resource paths at build time or runtime.

> **Manual copy required:** `scyraTurtle.png` is intentionally not committed by this Codex task. After these text changes are applied, manually copy `android/app/src/main/res/drawable/scyra_turtle.png` to `ios/Scyra/Scyra/Assets.xcassets/scyraTurtle.imageset/scyraTurtle.png`. Until that PNG exists, the image set is intentionally incomplete and the app should not be considered ready to build or merge.

| iOS Name | Type | iOS Location | Android Source | Android Usage Evidence | Notes |
|---|---|---|---|---|---|
| scyraTurtle | Image asset | `Assets.xcassets/scyraTurtle.imageset/scyraTurtle.png` | `android/app/src/main/res/drawable/scyra_turtle.png` | `SkillzTopAppBar.kt` uses `painterResource(id = R.drawable.scyra_turtle)` for the Shell top-bar action. | Prepared for direct PNG copy. The previous SVG/base64 wrapper was removed because it can render invisibly in iOS. |

## Fonts

The Caveat semibold font is intentionally not committed in this PR because Codex cannot safely update binary font files. Typography is fallback-safe: `ScyraTypography.appTitleResolved` uses the custom font only if a future manual step adds and registers it; until then, the app title uses `ScyraTypography.appTitleFallback`.

| iOS Name | Type | Future iOS Location | Android Source | Android Usage Evidence | Notes |
|---|---|---|---|---|---|
| caveatsb | Font | `ios/Scyra/Scyra/Resources/Fonts/caveatsb.ttf` | `android/app/src/main/res/font/caveatsb.ttf` | `SkillzTopAppBar.kt` uses `FontFamily(Font(R.font.caveatsb))` at `30.sp` for the Scyra title. | Do not reference Android paths from iOS runtime/build code. A future manual/developer task can add this font to Xcode and register it with `UIAppFonts`; until that happens, the system fallback is expected. |

## Assets intentionally skipped

- Android launcher/adaptive icon resources were not copied because this task only prepares the Shell turtle top-bar action asset and does not redesign the iOS app icon.
- Shell room objects, Trinkets, Coral, Plants, Discoveries, removed economy assets, and feature-specific creature/room assets were not copied because they are outside this task's scope.
