# iOS Asset Copy Map

This file documents the Scyra visual assets copied into the iOS project for local iOS use. iOS must use these local copies rather than Android resource paths at build time or runtime.

| iOS Name | Type | iOS Location | Android Source | Android Usage Evidence | Notes |
|---|---|---|---|---|---|
| scyraTurtle | Image asset | `Assets.xcassets/scyraTurtle.imageset` | `android/app/src/main/res/drawable/scyra_turtle.png` | `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/SkillzTopAppBar.kt` uses `painterResource(id = R.drawable.scyra_turtle)` for the Shell top-bar action. | Owner-created turtle copied into the iOS-local asset catalog. Android source is a single 1024×1024 PNG. To keep the PR text-only while preserving the original turtle pixels, the PNG is embedded as a base64 data image inside a local SVG wrapper and used as one universal asset rather than density-specific slots. |

## Fonts

No font files were copied for this task. Android uses `caveatsb.ttf` for the Android top-bar title and some score-header accent text, but the current iOS direction in `docs/04_DESIGN_SYSTEM.md` and `docs/09_IOS_PARITY_BACKLOG.md` is system iOS typography and explicitly not copying Caveat for iOS at this stage.

## Assets intentionally skipped

- Android launcher/adaptive icon resources were not copied because this task only wires the Shell turtle top-bar action and does not redesign the iOS app icon.
- Shell room objects, Trinkets, Coral, Plants, Discoveries, removed economy assets, and feature-specific creature/room assets were not copied because they are outside this task's scope.
