# Asset, Resource, and Copy Audit

## 1. Purpose

This document audits Android resources and source-backed visuals/copy so future iOS work can decide what must be copied into `ios/` later, what must be recreated natively, what should be redrawn with SwiftUI/Canvas, what should be localized, what needs licensing verification, what is Android-specific, and what is legacy enough to skip unless product confirms it.

The audit is scoped to visual parity, navigation labels, reward copy, The Shell, The Blue, The Chest, Focus Room, Health/Movement, notification copy, accessibility copy, localization, and App Store asset planning. It is documentation only: it does not copy, export, rename, delete, or modify Android resources; it does not create iOS code, folders, asset catalogs, fonts, translations, or an Xcode project.

## 2. Source Material Inspected

### Prior reference docs

- `docs/00_REPO_BOUNDARIES.md`
- `docs/01_ANDROID_ARCHITECTURE.md`
- `docs/02_SCYRA_PRODUCT_SPEC.md`
- `docs/03_NAVIGATION_AND_SCREEN_MAP.md`
- `docs/04_DESIGN_SYSTEM.md`
- `docs/05_DATA_MODEL_MAP.md`
- `docs/06_REWARD_AND_ECONOMY_SPEC.md`

### Android resource directories and files

- `android/app/src/main/res/values/` (`colors.xml`, `strings.xml`, `themes.xml`, `ic_launcher_background.xml`)
- `android/app/src/main/res/values-es/strings.xml`
- `android/app/src/main/res/values-hi/strings.xml`
- `android/app/src/main/res/values-mr/strings.xml`
- No additional `values-*` folders were observed beyond Spanish, Hindi, and Marathi.
- `android/app/src/main/res/font/caveatsb.ttf`
- `android/app/src/main/res/drawable/` (`scyra_turtle.png`, splash drawables, launcher foreground/background, notification icon)
- `android/app/src/main/res/mipmap-*` launcher icon PNGs and adaptive icon XML.
- `android/app/src/main/res/xml/backup_rules.xml`
- `android/app/src/main/res/xml/data_extraction_rules.xml`
- `android/app/src/main/res/animator/scyra_turtle_drift.xml`
- `android/app/src/main/res/raw/`: not present.
- `android/app/src/main/ic_launcher-playstore.png`

### Android source-backed visual/copy files

- Theme/design: `android/app/src/main/java/com/kingkharnivore/skillz/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- Root chrome: `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/SkillzTopAppBar.kt`
- Shell chrome and procedural visuals: `ui/screen/shell/ShellTopBar.kt`, `ui/screen/shell/icons/ShellObjectIcon.kt`, `ui/screen/shell/icons/ShellPearlMiniIcon.kt`, `ui/screen/shell/icons/draw/ShellDrawings.kt`, `ui/screen/shell/icons/draw/TurtleShellInteriorBackground.kt`
- The Blue procedural rendering: files under `ui/screen/shell/rooms/blue/`, especially drawing, creature, tray, zone, and detail-sheet files.
- Catalog/economy sources: `ShellContentCatalog`, `CreatureCatalog`, `CreatureEconomy`, `StillwaterCatalog`, and reward/economy files named in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- Reward reveal UI/copy: files under `ui/screen/flow/reward/` including `RewardRevealMapper.kt`, `RewardRevealDeck.kt`, `SessionRewardContent.kt`, `SoftSessionRewardContent.kt`, and `ArcSummaryContent.kt`.
- Focus Room UI/copy: files under `ui/screen/shell/rooms/focus/` and `FocusExerciseVoiceGuide.kt`.
- Health/settings copy: Health settings UI components and repositories documented in `docs/03_NAVIGATION_AND_SCREEN_MAP.md` through `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- Notification resources/factories: `ic_scyra_notification.xml`, notification strings, foreground Flow notification code, and notification permission gate files documented in prior docs.

If future code moves these files, treat this document as a point-in-time audit and re-run the resource inventory before implementing iOS.

## 3. Repo Boundary Rules for Assets

- Android assets/resources must remain under `android/`.
- Future iOS assets/resources must live under `ios/`.
- iOS must not reference Android resource paths at build/runtime.
- Shared root-level runtime asset folders are disallowed unless an explicit architecture decision is made later.
- `docs/` may reference Android resource paths for planning only.
- If both Android and iOS need the same font/image/audio/string, iOS must receive its own local copy or native equivalent inside `ios/`.

Bad examples:

- iOS build phase copies `../android/app/src/main/res/font/caveatsb.ttf`.
- iOS references `../android/app/src/main/res/drawable/scyra_turtle.png`.
- Root `shared/assets/` becomes required by both apps.

Good future examples, not created by this task:

- `ios/Scyra/Resources/Fonts/` only for future non-Caveat fonts if needed
- `ios/Scyra/Assets.xcassets/scyra_turtle.imageset`
- `ios/Scyra/Resources/Localizable.xcstrings`

## 4. Resource Directory Inventory

| Path | Resource type | Important files | Purpose | Portable? | iOS destination recommendation | Action | Notes |
| ---- | ------------- | --------------- | ------- | --------- | ------------------------------ | ------ | ----- |
| `android/app/src/main/res/values/` | XML values | `colors.xml`, `strings.xml`, `themes.xml`, `ic_launcher_background.xml` | Base colors, copy, theme/splash configuration, launcher background value | Concepts portable; XML format Android-only | `ios/Scyra/Resources/Localizable.xcstrings`, design tokens, launch settings | localize / recreate | Do not import Android XML into iOS. |
| `android/app/src/main/res/values-es/` | Localized strings | `strings.xml` | Spanish copy | Portable as translation content if licensed/approved | iOS String Catalog Spanish locale | localize later | Key count matched base in inspection; translation quality still TODO. |
| `android/app/src/main/res/values-hi/` | Localized strings | `strings.xml` | Hindi copy | Portable as translation content if approved | iOS String Catalog Hindi locale | localize later | Devanagari layout and brand font fallback risk. |
| `android/app/src/main/res/values-mr/` | Localized strings | `strings.xml` | Marathi copy | Portable as translation content if approved | iOS String Catalog Marathi locale | localize later | Devanagari layout and long-string risk. |
| `android/app/src/main/res/font/` | Android font asset | `caveatsb.ttf` | Android title font only | Not needed for iOS | None for Caveat | skip for iOS | iOS should use regular/system typography and not copy Caveat. |
| `android/app/src/main/res/drawable/` | PNG/vector/XML drawables | `scyra_turtle.png`, `ic_scyra_splash_base.xml`, `ic_scyra_splash_animated.xml`, `ic_scyra_notification.xml`, launcher foreground/background XML | Brand turtle, splash, notification icon, launcher layers | Mixed | `Assets.xcassets`, native iOS launch assets, notification assets | copy/recreate/redraw/verify | Android vector/adaptive formats are not directly AppIcon-ready. |
| `android/app/src/main/res/mipmap-*` | Launcher icons | `ic_launcher.png`, `ic_launcher_foreground.png`, `ic_launcher_round.png`, adaptive icon XML in anydpi | Android launcher icons | Concept portable; files not directly sufficient for iOS | `Assets.xcassets/AppIcon.appiconset` | recreate as iOS AppIcon | iOS has different sizes, masks, and transparency rules. |
| `android/app/src/main/res/xml/` | Android platform XML | `backup_rules.xml`, `data_extraction_rules.xml` | Backup/data extraction policy placeholders | Concept portable; XML Android-only | iOS backup/privacy decisions | skip / recreate concept | Current files are template-like and need real policy review. |
| `android/app/src/main/res/animator/` | Android animator XML | `scyra_turtle_drift.xml` | Animated splash/turtle drift | Android-only | SwiftUI/Core Animation equivalent if needed | recreate | Do not copy XML animator into iOS. |
| `android/app/src/main/res/raw/` | Raw/audio | Not present | No bundled audio/soundscape assets observed | N/A | `Resources/Audio/` only if future assets exist | verify later | Focus Room uses TTS behavior, not bundled audio. |
| `android/app/src/main/ic_launcher-playstore.png` | Store image | Play Store icon PNG | Google Play listing/store icon | Reuse only if source/license and App Store needs permit | App Store marketing asset source folder later | verify/recreate | App Store icon requirements differ from Play Store asset. |

## 5. Strings and Copy Inventory

The base string file is `android/app/src/main/res/values/strings.xml`; localized equivalents are present in `values-es`, `values-hi`, and `values-mr`. Inspection found 1,820 string/plural names in each of the four string files, but this task did not edit or generate translations.

| Copy domain | Representative keys/prefixes | Android file/folder | User-facing purpose | Localization status | iOS destination recommendation | Layout risk | Legacy terminology risk / TODO |
| ----------- | ----------------------------- | ------------------- | ------------------- | ------------------- | ------------------------------ | ----------- | ------------------------------ |
| App name / flavor names | `app_name`, flavor app names from Gradle, home app-name copy | `values/strings.xml`, `android/app/build.gradle.kts` | App label and Scyra/Aera identity | Localized strings exist; flavor labels require verification | String Catalog plus build-setting display names | Low/medium | Aera score-hidden behavior is separate product flavor; do not conflate with Scyra. |
| Top app bar / navigation labels | Story, Paths/Horizon, Shell turtle, Notepad, Help labels and a11y copy | `strings.xml`, `SkillzTopAppBar.kt` | Root navigation and icon-only button labels | Localized | String Catalog and VoiceOver labels | Medium for compact top bar | Preserve exact icon order from navigation/design docs. |
| Flow screen copy | `flow_screen_*`, `flow_card_*`, Flow form labels | `strings.xml`, Flow screen files | Flow creation/active/pause/complete UX | Localized | Feature-specific String Catalog groups | Medium | Soft Flow copy mentions Beam as excluded; Beam is legacy/TODO. |
| Flow reward copy | `session_reward_*`, `reward_card_*`, `reward_chip_*`, Movement reward strings | `strings.xml`, `ui/screen/flow/reward/` | Reward reveal cards, chips, details | Localized | Reward feature strings | High in card deck | Beam reward strings remain; hidden/legacy cards should not be ported unless confirmed. |
| Pulse copy | `pulse_*`, `pulse_card_*`, `story_fab_record_pulse` | `strings.xml`, Pulse UI files | Thought/idea capture and edit | Localized | Pulse feature strings | Medium | No copy should treat Pulse creation as a rewarded Flow; verify any “reward” language before iOS. |
| Story / Chronicle copy | `story_*`, `journey_*`, `atlas_*`, `beam_card_*` | `strings.xml`, Story UI files | History, filters, details, older Atlas/Beam planning copy | Localized | Story/Chronicle strings | High due legacy volume | Beam/Atlas naming may be legacy; product confirmation needed before iOS exposure. |
| Paths / Arc copy | `paths_*`, `plan_arc_*`, `arc_*`, `suggested_route_*` | `strings.xml`, Paths/Arc UI | Plans, Arcs, suggested routes | Localized | Paths/Arc strings | Medium/high for cards | Distinguish Arc from Beam. |
| Shell root copy | `shell_*`, Shell top bar labels | `strings.xml`, Shell source files | Shell hub, room previews, Pearls | Localized | Shell String Catalog group | Medium | `shell_kind_object` currently displays “Creature”; internal object naming remains legacy. |
| The Blue copy | `the_blue_*`, `beyond_blue_*` | `strings.xml`, The Blue room files | Blue room scene, depth, release, Beyond Blue | Localized | The Blue feature strings | High for sheets/trays | “Discover” copy exists for Beyond Blue; do not imply Discovery Journal without confirmation. |
| The Chest copy | `shell_chest_*`, `the_blue_*chest*`, `shell_*chest*` | `strings.xml`, Chest/inventory source files | Creature inventory | Localized | Chest strings | Medium/high for grid labels | Product UI should say “The Chest.” Key names may still say `shell_chest`; no need to copy internal naming. |
| Badges copy | badge-related keys, `shell_objective_badges_*`, Mastery/Tier labels from catalog/code | `strings.xml`, badge source files | Countable badge display | Localized | Badges strings | Medium | Creature Mastery copy should use “Mastery” even if Android uses tier terms elsewhere. |
| Stillwater copy | `stillwater_*`, `shell_stillwater_*` | `strings.xml`, `Stillwater.kt`, room files | Soft Flow drops/draws | Localized | Stillwater strings | Medium/high | Stillwater creatures are separate from regular Flow drops. |
| Idea Grove copy | `idea_grove_*` and Pulse/Grove status copy | `strings.xml`, Idea Grove source files | Pulse-derived idea garden | Localized | Idea Grove strings | Medium | No direct Pulse reward copy should be added. |
| The Lookout copy | `lookout_*`, `shell_objective_badge_*` | `strings.xml`, Lookout source files | Objectives, completion, claim/reward copy | Localized | Lookout strings | High for objective cards | Lookout rewards are Pearl/badge claims, not Scyra Score. |
| Voyage Hall copy | `voyage_*` | `strings.xml`, Voyage source files | Stats/history | Localized | Voyage strings | Medium | Computed stats copy; no separate persisted asset. |
| Focus Room copy | `focus_room_*`, exercise copy in model/source | `strings.xml`, Focus room source files | Calm exercises, player controls, TTS state | Localized | Focus Room strings | Medium/high for player controls | No rewards/stats from Focus Room exercises. |
| Health / Movement copy | `movement_bonus_*`, Health settings and permission text | `strings.xml`, Health UI files | Movement bonus, Health Connect, permissions/errors | Localized | Health/Movement strings | High for permission screens | iOS must rewrite platform-specific HealthKit copy. |
| Notification copy | channel strings, foreground Flow notification strings, reward notification/inlay copy | `strings.xml`, notification factories | Android notifications and Shell inlay | Localized | iOS notification copy | Medium | Persistent foreground notification has no direct iOS equivalent. |
| Permissions copy | notification permission gate, Health permission rationale | `strings.xml`, `NotificationPermissionGate.kt`, Health UI | Permission explanation and CTA text | Localized | iOS-native permission education strings | Medium | Android permission names should not be copied verbatim. |
| Errors / empty states | `*_empty_*`, `*_error_*`, `*_loading` | `strings.xml`, screen files | Loading, empty, error copy | Localized | Feature strings | High in localized languages | Check button/card overflow in Hindi/Marathi. |
| Accessibility strings | `*_a11y`, content descriptions in source | `strings.xml`, Compose files | Screen reader labels | Localized where string-backed | VoiceOver labels and hints | High | Icon-only and custom-drawn creature interactions need explicit labels. |

Legacy terms found in resource or source-backed copy include Beam, object, room object, Coral, Plants, ShellFind/internal find terminology, discoveries/discover language, trinkets, and object reward event types. Android copy currently uses “The Chest” in user-facing Chest strings, while many internal keys use `shell_chest_*`; future iOS should use product-facing “The Chest,” not internal key names.

## 6. Localization Audit

| Locale folder | Language | Coverage notes | Layout risks | iOS recommendation | TODO |
| ------------- | -------- | -------------- | ------------ | ------------------ | ---- |
| `values/` | Base English | 1,820 string/plural names inspected. | English is shortest in many controls; still verify reward/Lookout sheets. | Source language in `Localizable.xcstrings`. | Confirm final product terminology before copying to iOS. |
| `values-es/` | Spanish | 1,820 names inspected; count matches base. | Spanish strings may expand buttons/chips and top bar labels. | Add Spanish locale to String Catalog after translation review. | Verify exact key parity and translation quality. |
| `values-hi/` | Hindi | 1,820 names inspected; count matches base. | Devanagari glyphs, line height, card/chip overflow, Caveat fallback. | Add Hindi locale with system font fallback, not Caveat for body text. | Verify glyph coverage and line wrapping. |
| `values-mr/` | Marathi | 1,820 names inspected; count matches base. | Devanagari glyphs and long-string overflow in sheets/cards. | Add Marathi locale with robust dynamic type testing. | Verify translation quality and key parity. |
| Other `values-*` | Not present | No RTL locale folders observed. | RTL mirroring not proven. | Add later only with full RTL testing. | TODO: verify if future product needs RTL. |

Android has an app language setting through preferences/app locale handling documented in the data-model and navigation docs. iOS should prefer String Catalogs (`Localizable.xcstrings`) for typed locale management and screenshots/testing, with `Localizable.strings` only if the future iOS project intentionally chooses the older format. This task does not generate or correct translations.

## 7. Font Audit

| Font | File path | Product purpose | Android usage | Glyph/language concerns | Licensing/source concern | iOS destination | Registration/fallback | Action |
| ---- | --------- | --------------- | ------------- | ----------------------- | ------------------------ | --------------- | --------------------- | ------ |
| Caveat SemiBold | `android/app/src/main/res/font/caveatsb.ttf` | Android-only Scyra title/brand accent | `Type.kt` defines `CaveatSemiBold`; `SkillzTopAppBar.kt` applies it to the Android Scyra title. | Not an iOS dependency; use regular/system iOS typography and verify Hindi/Marathi layout with system fallback. | No iOS license blocker because iOS should not copy Caveat. | No iOS font destination for Caveat. | Not applicable for Caveat. | Skip for iOS; never reference Android font path. |

Do not expose, embed, or copy the font binary in documentation or this task. Future iOS implementation should use regular/system typography; no Caveat copy is needed.

## 8. Image and Drawable Audit

| Asset | Android path | Type | Used by | Product meaning | iOS action | Notes/TODO |
| ----- | ------------ | ---- | ------- | --------------- | ---------- | ---------- |
| Scyra turtle | `android/app/src/main/res/drawable/scyra_turtle.png` | PNG | Top bar Shell icon, splash/brand surfaces where referenced | Shell turtle/logo identity | export/copy local iOS asset later | Product-owner-created and usable; still export proper iOS sizes and do not reference Android path. |
| Splash base | `android/app/src/main/res/drawable/ic_scyra_splash_base.xml` | Vector/XML drawable | Splash animated drawable | Branded launch/splash turtle | recreate or redraw natively | Android vector XML not directly an iOS launch asset. |
| Splash animated | `android/app/src/main/res/drawable/ic_scyra_splash_animated.xml` | Animated/vector XML | `Theme.Skillz.Splash` | Animated Scyra launch | recreate with iOS launch screen + optional post-launch animation | iOS launch screen is constrained; animation likely belongs after launch. |
| Turtle drift animator | `android/app/src/main/res/animator/scyra_turtle_drift.xml` | Animator XML | Splash animation | Gentle turtle motion | recreate with SwiftUI/Core Animation if needed | Android animator cannot be copied. |
| Notification icon | `android/app/src/main/res/drawable/ic_scyra_notification.xml` | Vector/XML | Android notification | Monochrome notification mark | recreate for iOS notification/brand needs | iOS notification icon behavior differs. |
| Launcher foreground | `android/app/src/main/res/drawable/ic_launcher_foreground.xml` and mipmap foreground PNGs | Vector/XML + PNG | Android adaptive launcher | App icon foreground | recreate as iOS AppIcon | AppIcon needs complete iOS size set. |
| Launcher background | `android/app/src/main/res/drawable/ic_launcher_background.xml`, `values/ic_launcher_background.xml` | XML drawable/value | Android launcher background | Icon background color/shape | recreate as iOS AppIcon layer/source | Do not copy adaptive icon XML as-is. |
| Launcher PNGs | `android/app/src/main/res/mipmap-*/ic_launcher*.png` | PNG | Android launcher | Installed app icon | recreate as iOS AppIcon | Verify if these are generated from source artwork. |
| Adaptive icon XML | `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Android adaptive icon XML | Android 8+ launcher | Adaptive icon composition | skip/recreate concept | Android-only format. |
| Play Store icon | `android/app/src/main/ic_launcher-playstore.png` | PNG | Google Play listing | Store marketing icon | verify/recreate for App Store | App Store marketing/icon requirements differ. |
| Room/creature images | No separate room/creature image files observed in `drawable/` | N/A | The Blue/Shell visuals are mostly source-backed | Procedural ocean/creature visuals | redraw natively | Use SwiftUI Canvas/custom views or exported art later. |

## 9. Launcher, Splash, and Store Asset Audit

- Launcher icons live in `android/app/src/main/res/mipmap-*` with density-specific PNGs plus adaptive icon XML under `mipmap-anydpi-v26`. iOS needs a dedicated `AppIcon.appiconset`; Android density PNGs are not a complete iOS app icon set.
- Launcher foreground/background drawables live in `android/app/src/main/res/drawable/` and `values/ic_launcher_background.xml`. These should inform the iOS icon source artwork but should not be copied into the Xcode project as Android XML.
- Splash resources are defined by `Theme.Skillz.Splash` in `themes.xml`, which references `@drawable/ic_scyra_splash_animated`, a splash background color, and a 1400 ms animation duration. iOS should use a static Launch Screen plus optional immediate SwiftUI animation if parity is required.
- The store asset `android/app/src/main/ic_launcher-playstore.png` exists for Google Play. Treat it as provenance/reference material; future App Store assets need independent sizing, safe-area, and licensing review.
- App labels are resource/flavor-driven. Future iOS targets/schemes must choose Scyra vs Aera naming intentionally and not rely on Android Gradle flavor resources.

## 10. XML Resource Audit

| XML path | Purpose | Android-only or portable concept | iOS equivalent | Action |
| -------- | ------- | -------------------------------- | -------------- | ------ |
| `android/app/src/main/res/xml/backup_rules.xml` | Android full-backup include/exclude policy placeholder. | Android-only file; backup policy concept portable. | iCloud backup / local data backup settings and privacy decisions. | skip file, recreate concept after product/privacy review. |
| `android/app/src/main/res/xml/data_extraction_rules.xml` | Android 12+ cloud/device-transfer data extraction placeholder. | Android-only file; data portability concept portable. | iOS backup/restore/import policy. | skip file, recreate concept later. |
| `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive launcher icon composition. | Android-only. | iOS AppIcon source composition. | recreate. |
| `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Round adaptive launcher icon composition. | Android-only. | No direct iOS round icon file. | skip/recreate AppIcon. |
| Health Connect XML | Not observed in `res/xml/`. | TODO: verify if future Android adds it. | HealthKit permission education copy, not XML. | verify later. |
| File provider XML | Not observed in `res/xml/`. | Android-only if added later. | iOS document provider/share extensions only if needed. | verify later. |

Most Android XML platform resources should not be copied to iOS. Their policy concepts may still matter for backup, transfer, privacy, and launch icon design.

## 11. Audio / Raw Resource Audit

`android/app/src/main/res/raw/` is not present in the inspected resource tree. No bundled soundscapes, water ambience, Focus Room audio files, or other raw/audio assets were observed.

Focus Room behavior is source-backed and uses TTS/voice-guide logic rather than bundled audio files. Soundscapes are a product idea/open question in prior docs, but there is no current Android raw asset set to migrate. If future iOS implements soundscapes, audio files must be licensed, reviewed, and placed locally under `ios/`; this task intentionally introduces no audio assets.

## 12. Theme and Color Resource Audit

| Resource/source | Value | Android usage | Product meaning | iOS token recommendation | Risk/TODO |
| --------------- | ----- | ------------- | --------------- | ------------------------ | --------- |
| `colors.xml/purple_200` | `#FFBB86FC` | Template Material color | Legacy/template | Do not make primary token | Verify if unused. |
| `colors.xml/purple_500` | `#FF6200EE` | Template Material color | Legacy/template | Do not make primary token | Verify if unused. |
| `colors.xml/purple_700` | `#FF3700B3` | Template Material color | Legacy/template | Do not make primary token | Verify if unused. |
| `colors.xml/teal_200` | `#FF03DAC5` | Template Material color | Legacy/template | Do not make Scyra token | Current Android Material may still use teal in places; verify drift. |
| `colors.xml/teal_700` | `#FF018786` | Template Material color | Legacy/template | Do not make Scyra token | Verify if unused. |
| `colors.xml/black`, `white` | `#FF000000`, `#FFFFFFFF` | System colors | Neutral | `ScyraColor.black/white` only if needed | Low. |
| `Color.kt/RavenclawBlue` | `0xFF2F4F6F` | Android source/flavor reference | Prior/secondary/reference blue | `ScyraColor.referenceBlue` only if needed | Not the iOS primary direction. |
| `Color.kt/SlytherinButNiceTeal` | `0xFF3F8F8B` | Current Material primary, splash, and teal direction | Main Scyra iOS teal | `ScyraColor.primaryTeal` | Use as primary iOS visual direction despite stale Android name. |
| `Color.kt/GryffindorOffWhite` | `0xFFF2EBDD` | Background/surface tone | Warm parchment/off-white | `ScyraColor.backgroundWarm` | Stale name; rename only in iOS tokens. |
| `Color.kt/AntiqueGold`, `Bronze`, `RoyalAmethyst` | Hex values in source | Accents/rewards/badges | Reward/accent tones | `ScyraColor.rewardGold`, `badgeBronze`, `amethyst` | Verify exact values in source before coding tokens. |
| `themes.xml/windowSplashScreenBackground` | `#3F8F8B` | Splash background | Calm teal launch field | `ScyraColor.launchBackground` | Use teal launch direction for iOS unless future product direction changes. |
| `ic_scyra_notification.xml` | Vector fill values in drawable | Android notification mark | Brand notification icon | iOS notification asset or system handling | iOS notification asset rules differ. |
| Gradle `PRIMARY_COLOR` Scyra | `0xFF2F4F6F` | BuildConfig for Scyra/default | Prior/reference Scyra blue | `ScyraColor.referenceBlue` if needed | Not the iOS primary direction; use teal. |
| Gradle `PRIMARY_COLOR` Aera | `0xFF3F8F8B` | Aera flavor | Aera calm teal | `AeraColor.primary` only if Aera supported | Aera support is an open product decision. |

Treat teal (`0xFF3F8F8B` / Android teal direction) as Scyra’s main iOS color. Treat `0xFF2F4F6F` as a prior/secondary/reference blue where still present in Android source. Do not carry stale Hogwarts-style Android source names into product-facing iOS token names.

## 13. Source-Backed Visual Asset Audit

| Android source | Visual produced | Data/catalog dependency | iOS strategy | Complexity/risk | Phase priority |
| -------------- | --------------- | ----------------------- | ------------ | --------------- | ------------ |
| `ui/screen/shell/icons/draw/ShellDrawings.kt` | Shell object/creature icon drawings | Shell catalog item kind/status | Redraw with SwiftUI Canvas or create vector assets | Medium/high | Phase 3 for visible Shell icons. |
| `ui/screen/shell/icons/draw/TurtleShellInteriorBackground.kt` | Shell room/interior background | Shell room state | SwiftUI Canvas/background component | Medium | Phase 3 for Shell feel. |
| `ui/screen/shell/icons/ShellPearlMiniIcon.kt` | Pearl mini icon | Pearl balance/reward rows | SwiftUI vector/Canvas icon | Low/medium | Foundation/full parity. |
| `ui/screen/shell/icons/ShellObjectIcon.kt` | Object/creature tile icon wrapper | Shell find/object catalog | Prefer creature-only tile wrapper for iOS | Medium | Phase 3 for Chest/Blue. |
| `ui/screen/shell/rooms/blue/draw/DrawTheBlue.kt` | Ocean scene/background and creature rendering | The Blue zone/depth and creature presence models | SwiftUI Canvas/custom drawing or exported art pipeline | High | Phase 3 when The Blue is implemented. |
| `ui/screen/shell/rooms/blue/creatures/Life.kt` | Procedural creature life/drawing models | Creature definitions and levels | SwiftUI Canvas equivalents or asset sprites | High | Phase 3/full parity depending drawing strategy. |
| `ui/screen/shell/rooms/blue/TheBlueRenderedCreature.kt` | Rendered creature placement/interaction model | User creatures and depth/zone state | Native data-driven scene nodes | High | Phase 3 for The Blue parity. |
| Reward reveal files under `ui/screen/flow/reward/` | Reward deck/cards/chips/icons | Reward UI model, badges, creatures, movement | Custom SwiftUI reward card deck | Medium/high | Foundation/full parity. |
| Focus Room source files | Exercise player visuals, timers, voice state | Exercise model/TTS state | SwiftUI calm card/player components | Medium | Phase 2 unless Focus Room Foundation/full parity. |
| Stillwater room/source files | Vessel/draw visuals | Stillwater ledger/catalog | SwiftUI room/vessel visuals | Medium/high | Phase 2. |
| Lookout/Voyage/Idea Grove source files | Objective cards, stats cards, Pulse grove cards | Sessions/objectives/pulses | SwiftUI cards and charts/stat rows | Medium | Phase 4/full parity per room sequencing. |

Procedural Compose visuals should not be referenced by iOS at build/runtime. Future iOS should redraw natively, create equivalent vector assets, or explicitly simplify after product approval.

## 14. Creature and Catalog Visual/Copy Audit

| Creature/source | Android ID/name | Source file | Visual source | Copy source | iOS action | Notes/TODO |
| --------------- | --------------- | ----------- | ------------- | ----------- | ---------- | ---------- |
| Regular Flow creature | `FOCUS_MINNOW` / Minnow | `ShellContentCatalog`, reward docs | Procedural Shell/The Blue drawing | Catalog display name / reward copy | Redraw natively; localize name if product approves | Duration threshold documented in reward spec; verify current catalog before implementation. |
| Regular Flow creature | `FOCUS_SEAHORSE` / Seahorse | `ShellContentCatalog` | Procedural | Catalog/copy | Redraw natively | Regular Flow creature. |
| Regular Flow creature | `FOCUS_MANTA` / Manta | `ShellContentCatalog` | Procedural | Catalog/copy | Redraw natively | Regular Flow creature. |
| Regular Flow creature | `FOCUS_WHALE` / Whale | `ShellContentCatalog` | Procedural | Catalog/copy | Redraw natively | Regular Flow creature. |
| Stillwater shallow creatures | shrimp, crab, clam, snail, limpet, barnacle, cowrie, horseshoe | `StillwaterCatalog` | Procedural/catalog-driven | Catalog/copy | Redraw natively; keep Stillwater-exclusive if confirmed | Do not mix with regular Flow drops unless reward spec changes. |
| Stillwater reef creatures | goby, wrasse, blenny, lionfish, anemone, cuttlefish, moray, nautilus | `StillwaterCatalog` | Procedural/catalog-driven | Catalog/copy | Redraw natively | Verify display names/localization. |
| Stillwater open-water creatures | mahi, wahoo, bonito, barracuda, amberjack, grouper, marlin, sailfish | `StillwaterCatalog` | Procedural/catalog-driven | Catalog/copy | Redraw natively | Verify rarity/depth display. |
| Stillwater deep creatures | fangtooth, viperfish, hatchetfish, gulper, grenadier, oarfish, blackdragon, coelacanth | `StillwaterCatalog` | Procedural/catalog-driven | Catalog/copy | Redraw natively | High visual complexity. |
| Beyond Blue examples | dolphin, ocean sunfish, starfish examples from tests/catalog | `CreatureCatalog`, `ShellContentCatalog`, Beyond Blue source/tests | Procedural/catalog-driven | Beyond Blue copy | Redraw natively; verify list | TODO: audit full Beyond Blue catalog before implementation. |
| Legacy non-creature catalog items | objects/trinkets/discoveries/Coral/Plants | Shell catalog/reward event code and strings | Procedural icons/copy | `shell_object_*`, `shell_category_*` | Skip iOS Scyra parity unless product confirms | The Chest should remain creature-only. |

Creature visuals appear source/procedural rather than image-backed. Creature Mastery copy should use “Mastery” such as “Minnow Mastery” even if Android source uses other tier labels; verify final badge catalog before iOS implementation.

## 15. Reward Reveal Resource/Copy Audit

| Reward reveal area | Android source | Current behavior | iOS copy/asset recommendation | Legacy risk / TODO |
| ------------------ | -------------- | ---------------- | ----------------------------- | ------------------ |
| Score/Scyra Points | `session_reward_*`, `reward_chip_*`, `RewardRevealMapper.kt` | Regular Flow reward cards/chips show score breakdown. | Custom reward card deck with Scyra tone. | Aera score-hidden behavior must be separate. |
| Timed bonuses | `session_reward_*`, score/reward mapper | Bonus rows/cards appear in reward detail. | Localized labels and compact chips. | Verify exact formulas from reward spec. |
| Surge | `session_reward_*`, Surge runtime/reward fields | Surge points appear when applicable. | Surge-specific chip/card copy. | Surge exact formula must match reward spec. |
| Arc | `ArcSummaryContent.kt`, Arc summary strings | Arc summary reward card/content. | Dedicated Arc summary surface, not alert. | Arc multiplier source-of-truth is high-risk. |
| Movement | movement bonus/reward strings, Health refresh code | Movement points can update after completion. | Movement card capable of pending/updated state. | Delayed Health refresh can change Pearls. |
| Pearls | Pearl strings/icons, Shell reward model | Regular Flow can unify Scyra Points/Pearls; delayed deltas possible. | Pearl icon/token in card deck. | Avoid duplicate Pearl amount card when unified. |
| Creature card | Shell reward/event UI, creature catalog | Creature grants displayed as Shell/Blue/Chest rewards. | Creature card using native icon/Canvas. | Do not show legacy objects/trinkets as Chest items. |
| Badges | Badge reward strings and grouped cards | Badge grants grouped in reward reveal/inlay. | Badge card/list rows. | Mastery count behavior is a product gap. |
| Stillwater | `SoftSessionRewardContent.kt`, Stillwater strings | Soft Flow shows Stillwater drops, not Score/Pearls. | Gentle Stillwater reward surface. | Stillwater creatures are separate/exclusive if confirmed. |
| Shell bridge | Reward reveal Shell bridge/open Shell action | Sends user toward Shell/The Chest/The Blue. | Custom CTA card/button. | Keep celebratory but calm tone. |
| Legacy hidden categories | Reward reveal mapper/tests, reward event types | Legacy objects/trinkets/discoveries may be hidden/grouped away. | Do not create iOS cards unless confirmed. | Explicit product confirmation required. |
| Empty/no reward | Reward UI model/content | Soft/no reward cases show explanatory copy. | Preserve calm explanation, not error state. | TODO: verify all edge strings. |

Reward reveal should not become a generic iOS alert. It needs a custom Scyra card deck/surface with localized copy and native icons/procedural creature visuals.

## 16. Shell Room Asset/Copy Audit

| Shell room | Copy source | Visual source | Empty/CTA/dialog copy | Accessibility copy | iOS action | Legacy terms to avoid |
| ---------- | ----------- | ------------- | --------------------- | ------------------ | ---------- | --------------------- |
| Shell root / Heart | `shell_*`, room source files | Shell backgrounds/icons in source | Room previews, Pearl basin, shortcuts | Shell icon/top bar labels | Redraw/recreate room hub; localize strings | Internal ShellFind/object terms. |
| The Blue | `the_blue_*`, `beyond_blue_*` | `rooms/blue/` procedural scene/creatures | Empty ocean, creature detail, release, Beyond Blue | Creature/depth/zone labels TODO verify | SwiftUI Canvas/scene; localize copy | Discovery Journal implication; non-creature items. |
| The Chest | `shell_chest_*`, Chest/inventory source | Creature tiles/source icons | Empty inventory, stack detail, release/growth | Stack count/level labels | Creature-only grid; localize | “Shell Chest” as product UI; objects/trinkets/released creatures. |
| Badges | badge strings/source | Badge rows/tiles from source | Empty/new/viewed states | Badge labels/counts | Recreate badge tiles and count semantics | Non-countable Mastery ambiguity. |
| Stillwater | `stillwater_*`, `shell_stillwater_*` | Stillwater room/vessel source | Draw/drop/empty/CTA copy | TODO verify | Recreate vessel/draw visuals; localize | Treat as Soft Flow-specific; no Score/Pearls. |
| Idea Grove | `idea_grove_*`, Pulse source | Grove cards/source visuals | Empty Pulse-derived idea states | TODO verify | Recreate cards; localize | Pulse rewards or Flow equivalence. |
| The Lookout | `lookout_*`, objective source | Objective cards/source visuals | Set/claim/remove dialogs | Objective a11y strings | Recreate objective cards; localize | Score rewards; Lookout should be Pearls/badges only unless Android says otherwise. |
| Voyage Hall | `voyage_*`, stats source | Stats cards/source visuals | Empty/stat period copy | TODO verify | Recreate stats cards/charts | Persisting derived stats unnecessarily. |
| Focus Room | `focus_room_*`, exercise model/source | Calm cards/player/source visuals | Start/pause/resume/end/completion/TTS copy | Control labels TODO verify | Recreate calm player; localize; TTS native | Rewards/stats or auto-start behavior. |
| Shell notifications/inlay | Shell notification mapper strings/source | Inlay card/icons | Mark viewed/open room copy | Notification labels | Recreate in-app notification surface | Legacy discovery/object event types. |

## 17. Notification and Permission Resource Audit

| Area | Android source | Android-specific behavior | iOS equivalent | Copy/assets needed | TODO |
| ---- | -------------- | ------------------------- | -------------- | ------------------ | ---- |
| Notification icon | `res/drawable/ic_scyra_notification.xml` | Monochrome/vector Android notification icon | iOS app notification presentation uses app icon and notification content; custom attachments only if needed | Brand notification copy; maybe no custom icon | Verify iOS notification design. |
| Notification channels | notification factory/source and string resources | Android channel IDs/names/descriptions | iOS notification categories/thread identifiers if needed | Channel-like user-facing category copy | Android channel concepts should not be copied verbatim. |
| Flow foreground notification | Flow service/notification source | Persistent foreground service notification for active Flow | iOS local notification/live activity/background limits; no direct equivalent | Active Flow reminder copy | iOS timer restoration must be timestamp-based. |
| Reward notification | Shell reward event/inlay sources | Reward events may appear in notification/inlay | iOS local notification or in-app inlay | Reward summary copy | Avoid duplicate-award impression. |
| Health permission copy | Health settings UI/resources | Health Connect install/update/permission states | HealthKit authorization education | Platform-specific HealthKit copy | Rewrite Android-specific Health Connect wording. |
| Notification permission gate | `NotificationPermissionGate.kt`, strings | Android 13+ notification permission education | iOS notification authorization prompt education | Native iOS permission copy | Do not copy Android permission labels. |
| Shell notification inlay | Shell notification mapper/tests/source | In-app viewed/new reward event surface | SwiftUI in-app inlay/list | Mark viewed/open room copy | Legacy event types need filtering. |
| Deep link/action labels | Flow notification/deep link source | `skillz://flow` and Android pending intents | iOS URL scheme/universal link later | Action labels if implemented | TODO: verify iOS deep link needs. |

## 18. Backup, Data Extraction, and Privacy Resource Audit

`backup_rules.xml` and `data_extraction_rules.xml` are Android platform resources for backup and device-transfer policy. In the inspected files, they appear template-like with comments and no substantive active include/exclude policy. They do not become iOS resources.

Future iOS work should separately decide whether Room-equivalent data, settings, notepad text, Health-derived snapshots, and economy ledgers are backed up to iCloud or excluded. This audit is not a legal/privacy policy; it only flags that backup/data extraction concepts exist and need engineering/product review before iOS persistence ships.

## 19. Accessibility Copy Audit

| Area | Android string/source | Purpose | iOS VoiceOver recommendation | Risk/TODO |
| ---- | --------------------- | ------- | ---------------------------- | --------- |
| Top app bar icons | `SkillzTopAppBar.kt`, `*_a11y` strings | Icon-only Story/Paths/Shell/Notepad/Help actions | Explicit labels: Story, Paths/Horizon, The Shell, Notepad, Help | Verify exact localized labels. |
| Shell turtle | `scyra_turtle.png`, top bar content description | Brand/Shell navigation icon | “Open The Shell” or product-approved label | Do not rely on image name. |
| Flow fields/controls | `flow_screen_*`, source content descriptions | Text fields, timer, pause/resume/complete | Labels, hints, values, timer announcements | Dynamic timer announcements need care. |
| Story cards | `story_*`, card source | Flow/Pulse/Arc history cards | Combine title, Journey, duration, score/reward summary | Long card labels may be verbose. |
| Pulse cards | `pulse_*`, Pulse source | Idea/thought cards | Distinguish Pulse from Flow in labels | Avoid reward language. |
| The Blue creatures/zones/depth | `the_blue_*`, procedural source | Interactive creature/depth scene | Each creature must have species, level, status, action label | High risk for custom-drawn visuals. |
| Chest stacks | `shell_chest_*`, Chest source | Inventory grid stack labels | Species, level, count, selected state, available actions | Count and level badges must be read. |
| Badges | badge strings/source | Badge groups, count, new/viewed | Badge name, count, new/viewed state | Mastery count semantics need labels. |
| Focus Room controls | `focus_room_*`, player source | Start/pause/resume/restart/end/voice | Clear control labels and state changes | TTS state should not conflict with VoiceOver. |
| Health/settings controls | Health strings/source | Toggles, permission status, movement settings | State-aware labels and hints | Platform-specific HealthKit wording needed. |
| Notification permission | Notification permission strings/source | Permission education/gate | Native permission education labels | Android-specific copy rewrite required. |
| Custom procedural icons | Shell/Blue/reward drawing source | Non-resource visual interactions | Accessibility labels independent of drawing code | TODO: audit all clickable Canvas elements. |

## 20. Legacy Terminology and Do-Not-Port Copy

| Term/concept | Resource key/source | Current Android text or meaning | Why questionable | iOS decision | TODO |
| ------------ | ------------------ | ------------------------------- | ---------------- | ------------ | ---- |
| Beam | `story_fab_schedule_beam`, `schedule_beam_*`, `beam_*`, reward Beam strings | Scheduling/time-window bonus concept with copy and reward chips. | Current product docs emphasize Flow/Pulse/Arc/Surge; Beam may be legacy or separate. | Do not port into iOS Scyra parity unless product confirms. | Decide Beam scope separately. |
| ShellFind | Entity/catalog/internal source naming | Umbrella persistence/catalog term for creatures/finds. | Product direction wants creature-focused language. | Use `Creature` domain names in iOS where possible. | Preserve only for data import compatibility if needed. |
| Shell Chest | Internal key prefix `shell_chest_*`; UI text generally “The Chest.” | Internal naming for inventory. | Product-facing name should be “The Chest.” | Use “The Chest” in iOS UI. | Avoid copying internal key names into UI. |
| Discovery / Discoveries | Beyond Blue discover copy, discovery entities/events in source/docs | Discovery-like reward/journal concepts. | Discovery Journal should not be ported without confirmation. | Skip iOS Scyra parity except copy like “discover life” if product approves. | Product decision needed. |
| Trinkets | Reward event/source references | Legacy reward category. | The Chest should be creature-only. | Do not port unless confirmed. | Audit catalog if data import is planned. |
| Objects / room objects | `shell_object_*`, `shell_placed_object_a11y`, reward event object types | Room object/invite/placement copy. | Product direction says no trinkets/objects in iOS Scyra parity; The Chest creature-only. | Skip or rename only after product confirmation. | Decide if Focus Room displayed creatures replace objects. |
| Coral | `shell_category_coral`, `shell_slot_coral_bed`, Glow Coral strings | Coral/object category and item. | Non-creature inventory conflicts with creature-only Chest. | Skip unless Shell room decoration returns by product decision. | Product confirmation needed. |
| Plants | `shell_category_plants` | Room category. | Legacy/non-creature category. | Skip Foundation/full parity. | Product confirmation needed. |
| Pulse reward implication | `pulseBonusPoints` field in reward breakdown; no direct copy proof | Reward field exists in persistence; Pulse copy should remain idea/thought oriented. | Pulses are not rewarded sessions. | Do not add Pulse reward copy/cards. | Verify any future copy additions. |
| Chest non-creatures | object/trinket/coral/plants copy and reward events | Old Shell objects may be invite/display items. | The Chest should contain creatures only. | Do not show non-creatures in iOS Chest. | Data compatibility decision later. |

## 21. iOS Asset Destination Recommendations

Recommended future layout only; do not create these folders in this task:

```text
ios/Scyra/
  Assets.xcassets/
    AppIcon.appiconset/
    ScyraTurtle.imageset/
    NotificationBrand.imageset/
    RewardIcons/
    CreatureIcons/
    ShellRooms/
  Resources/
    Fonts/
    Audio/
    Localizable.xcstrings
  DesignSystem/
    GeneratedAssetNames.swift
```

| iOS category | What goes there | Android references | Copy/recreate/redraw/localize | Dependency/risk notes |
| ------------ | --------------- | ------------------ | ----------------------------- | --------------------- |
| AppIcon | iOS icon set | `mipmap-*`, launcher drawables, Play Store icon | recreate | Needs iOS sizes/source artwork export. |
| ScyraTurtle | Turtle logo/brand image | `drawable/scyra_turtle.png` | export/copy local iOS asset; owner-created | Do not reference Android path. |
| NotificationBrand | Optional notification/brand mark | `ic_scyra_notification.xml` | recreate | iOS may not need a separate notification icon. |
| RewardIcons | Pearls, badges, movement, reward card art | reward source/drawables/procedural icons | redraw/recreate | Reward reveal is custom UI, not alert. |
| CreatureIcons | Creature sprites/vectors if not Canvas | Shell/Blue catalogs and procedural drawing | redraw or export later | The Blue procedural parity is high risk. |
| ShellRooms | Room backgrounds/illustrations | Shell drawing/source files | redraw/recreate | Preserve hub/room feeling. |
| Fonts | iOS system typography | Android `font/caveatsb.ttf` is not an iOS dependency | no Caveat copy | Use native/system font tokens. |
| Audio | Future soundscapes/TTS assets | No `res/raw` present | verify/future licensed assets | No audio currently to migrate. |
| Localizable.xcstrings | All base/localized copy | `values*/strings.xml`, source copy | localize | Translation review and terminology cleanup required. |
| GeneratedAssetNames.swift | Optional type-safe names | Future iOS asset catalog | generate later | Not needed until iOS project exists. |

## 22. Asset Licensing and Provenance Risks

| Risk | Why it matters | iOS impact | Follow-up task |
| ---- | -------------- | ---------- | -------------- |
| Android Caveat font | Android has `caveatsb.ttf`, but iOS should use system fonts. | No iOS font copy needed. | Define iOS system typography tokens. |
| Turtle image export | `scyra_turtle.png` is product-owner-created brand art. | iOS can use it after local export. | Export proper iOS asset sizes into `ios/` later without referencing Android path. |
| Launcher/store icon provenance | Mipmap icons and Play Store icon may be generated from unknown source. | iOS AppIcon and App Store assets need rights and correct formats. | Rebuild from verified master artwork. |
| Procedural art ownership | Compose Canvas drawings are source-authored but may include design assumptions. | Redrawing in iOS must preserve product style without copying code as dependency. | Document drawing specs and approve native redraws. |
| Third-party audio | No audio now, but soundscapes may be added later. | Audio licensing can block App Store release. | License any future soundscape assets before bundling. |
| Material Icons | Android uses Material Icons in code. | iOS should use SF Symbols/custom icons; Material icon license/visual mismatch must be considered. | Pick iOS icon set per design system. |
| Translation provenance/quality | Localized strings exist but quality/source unknown. | Poor translations or layout failures hurt launch quality. | Native speaker review and screenshot tests. |
| App Store suitability | Android assets may not meet iOS/HIG/store rules. | Rejection or visual inconsistency. | App Store asset checklist task. |

No unsupported licensing claims are made here; unknown sources are marked for verification.

## 23. Asset and Resource Migration Priorities

### Phase 1/Foundation iOS assets/resources

- App name/copy and root navigation labels, because every iOS screen depends on them.
- Base English strings and reviewed String Catalog structure.
- Scyra teal primary color tokens, with `0xFF2F4F6F` only as prior/reference blue if needed.
- Regular/system iOS typography; do not copy Caveat.
- Owner-created turtle logo exported as local iOS asset.
- Root top bar icon equivalents and VoiceOver labels.
- Flow, Story, Pulse, reward reveal, and core Shell copy.
- The Blue, The Chest, and Badges core creature/inventory copy if those features are Foundation/full parity.
- Accessibility strings for icon-only controls and custom-drawn creatures.

### Phase 2

- Full Shell room visuals and room-specific backgrounds.
- Focus Room TTS/player copy and visual states.
- Stillwater vessel/draw visuals and expanded catalog copy.
- Lookout and Voyage Hall visuals/stat copy.
- Advanced localization review for Spanish, Hindi, and Marathi.
- Notification/inlay copy after iOS notification strategy is defined.

### Later / verify

- Soundscapes/audio assets, because no Android raw assets currently exist.
- Aera visual resources/flavor support.
- Legacy objects/trinkets/discoveries/Beam assets and copy.
- Android-to-iOS data import strings/assets.
- App Store marketing images and screenshots.

## 24. Asset/Resource Risks and Open Questions

| Risk/open question | Why it matters | iOS impact | Recommended follow-up task |
| ------------------ | -------------- | ---------- | -------------------------- |
| iOS system typography | iOS intentionally does not use Android Caveat. | Brand feel must be preserved through system font styling, spacing, and color. | Define and review system-font typography tokens. |
| Turtle/logo export | Turtle is owner-created and usable. | Need correct iOS sizes/assets while preserving boundaries. | Export iOS-ready local assets under `ios/`. |
| Android adaptive icon vs iOS icon | Formats and size rules differ. | Direct copy will not satisfy iOS icon requirements. | Create iOS AppIcon from master artwork. |
| Android splash animation vs iOS launch constraints | iOS launch screens cannot mimic arbitrary Android animation. | Need static launch plus post-launch animation. | Design iOS launch/brand transition. |
| The Blue procedural drawing parity | Many visuals are code-drawn and data-driven. | High effort to match with SwiftUI Canvas. | Produce a dedicated The Blue drawing spec/prototype. |
| Creature icon/visual strategy | Creatures appear procedural, not image-backed. | Need Canvas redraws or generated sprites. | Decide Canvas vs asset pipeline. |
| Localized string coverage | Counts match but quality/key equality needs deeper checks. | Missing/poor translations or overflow. | Run key-diff, pseudo-localization, screenshot tests. |
| Hindi/Marathi font fallback | Brand font may not cover Devanagari. | Broken glyphs or poor line height. | Define localized typography fallbacks. |
| Legacy copy cleanup | Beam/object/coral/plants copy remains. | iOS could accidentally port deprecated concepts. | Product terminology cleanup decision. |
| No confirmed soundscape assets | Product mentions soundscapes as future idea. | iOS cannot implement asset-backed soundscapes yet. | Separate soundscape scope/licensing task. |
| Accessibility for custom visuals | Canvas-drawn creatures/icons lack automatic semantics. | VoiceOver gaps on core Shell/Blue UI. | Accessibility audit/prototype for custom drawing. |
| Scyra teal adoption | Teal is now the main Scyra iOS color while Android still has older blue references. | iOS should not choose the prior blue as primary. | Implement teal primary token and document blue as reference/secondary only. |
| App Store asset requirements | Play Store icon is not enough. | Need iOS icon, screenshots, privacy assets. | App Store readiness checklist. |
| iOS notification differences | Android foreground service/channel copy does not map directly. | Active Flow notifications need native strategy. | iOS notification/deep-link design task. |
| Future cloud sync/import copy | Migration/import may need user-facing copy. | Not covered by current resources. | Future sync/import UX spec. |

## 25. Acceptance Criteria for This Document

- This task only creates or changes `docs/07_ASSET_RESOURCE_COPY_AUDIT.md`.
- Android source code is untouched.
- Android resources are untouched.
- No iOS source code or project is created.
- No assets are copied into `ios/`.
- No fonts are copied.
- No strings/translations are edited.
- No Gradle/build files are changed.
- The document is based on actual Android resources/source-backed visuals plus prior docs.
- Resource directories are inventoried.
- Strings/copy domains are audited.
- Localization coverage is audited.
- Fonts are audited without exposing font file contents.
- Drawable/image/icon assets are audited.
- XML resources are audited.
- Audio/raw asset presence or absence is documented.
- Source-backed procedural visuals are audited.
- Creature/catalog visual and copy sources are audited.
- Legacy copy/resource concepts are clearly separated from iOS Scyra parity behavior.
- iOS asset destination recommendations preserve repo boundary rules.
- Unclear behavior or provenance is marked `TODO: verify`.

## 26. Codex Summary

- Docs inspected: `docs/00_REPO_BOUNDARIES.md` through `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- Android resource/source files inspected: `res/values`, localized `values-*`, `res/font`, `res/drawable`, `res/mipmap-*`, `res/xml`, `res/animator`, `ic_launcher-playstore.png`, theme files, top bars, Shell drawing files, The Blue procedural files, reward reveal files, catalog/economy files, Focus Room and Health/notification copy sources.
- Resource directories found: `animator`, `drawable`, `font`, `mipmap-anydpi-v26`, density `mipmap-*`, `values`, `values-es`, `values-hi`, `values-mr`, and `xml`.
- Strings/localizations found: base English plus Spanish, Hindi, and Marathi string files; each inspected file had 1,820 string/plural names.
- Fonts found: Android `caveatsb.ttf`; iOS should not bundle/copy Caveat and should use regular/system typography.
- Drawables/images/icons found: Scyra turtle PNG, splash base/animated drawables, notification vector, launcher foreground/background resources, mipmap launcher PNGs, adaptive icon XML, and Play Store icon.
- XML resources found: backup rules and data extraction rules; both are Android platform resources whose concepts must be reconsidered natively on iOS.
- Audio/raw resources: `res/raw/` was not present; no bundled audio or soundscape assets were found.
- Source-backed procedural visuals found: Shell icons/backgrounds/Pearl icon, The Blue ocean/creature scene, reward reveal card visuals, Stillwater/Lookout/Voyage/Focus Room visuals.
- Creature/catalog visual sources found: regular Flow creatures from Shell content catalog, Stillwater catalog creatures, Beyond Blue catalog examples, and legacy non-creature Shell object/trinket/discovery concepts.
- Legacy resource/copy concerns identified: Beam, internal ShellFind naming, object/trinket/discovery reward concepts, Coral, Plants, and non-creature Chest concepts.
- Asset concerns identified: Android Caveat is skipped for iOS, turtle/logo is owner-created but needs iOS-local export, launcher/store icons need proper iOS sizing, procedural art ownership/specs, translations, Material Icons equivalents, and any future audio.
- Future iOS recommendations: create local iOS asset catalogs, String Catalogs, system typography, SwiftUI/Canvas redraws for procedural visuals, native iOS notification/backup/launch behavior, and no runtime dependency on Android resources.
- Nothing outside `docs/07_ASSET_RESOURCE_COPY_AUDIT.md` should be changed for this task.
- Repo boundary rules remain preserved: docs may reference Android resource paths, but iOS must later receive local copies or native equivalents under `ios/`.
