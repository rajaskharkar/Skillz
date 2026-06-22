# iOS Asset Copy Map

This file documents Scyra visual assets prepared for local iOS use. iOS must use local copies under `ios/Scyra/` and must never reference Android resource paths at build time or runtime.

> **Manual copy required:** `scyraTurtle.png` is intentionally not committed by this Codex task. After these text changes are applied, manually copy `android/app/src/main/res/drawable/scyra_turtle.png` to `ios/Scyra/Scyra/Assets.xcassets/scyraTurtle.imageset/scyraTurtle.png`. Until that PNG exists, the image set is intentionally incomplete and the app should not be considered ready to build or merge.

| iOS Name | Type | iOS Location | Android Source | Android Usage Evidence | Notes |
|---|---|---|---|---|---|
| scyraTurtle | Image asset | `Assets.xcassets/scyraTurtle.imageset/scyraTurtle.png` | `android/app/src/main/res/drawable/scyra_turtle.png` | `SkillzTopAppBar.kt` uses `painterResource(id = R.drawable.scyra_turtle)` for the Shell top-bar action. | Prepared for direct PNG copy. The previous SVG/base64 wrapper was removed because it can render invisibly in iOS. |

## Fonts

No font files were copied for this task. Android uses `caveatsb.ttf` for the Android top-bar title and some score-header accent text, but the current iOS direction in `docs/04_DESIGN_SYSTEM.md` and `docs/09_IOS_PARITY_BACKLOG.md` is system iOS typography and explicitly not copying Caveat for iOS at this stage.

## Assets intentionally skipped

- Android launcher/adaptive icon resources were not copied because this task only prepares the Shell turtle top-bar action asset and does not redesign the iOS app icon.
- Shell room objects, Trinkets, Coral, Plants, Discoveries, removed economy assets, and feature-specific creature/room assets were not copied because they are outside this task's scope.
