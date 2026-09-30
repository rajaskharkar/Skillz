# Android Land localization — implementation report

This report covers the localization pass, not release certification of the entire Land expansion.

## Delivered

Hindi (`hi`), Marathi (`mr`) and neutral Spanish (`es`) now have all 165 `land_strings.xml` strings and all 70 `land_badge_strings.xml` strings. This includes distinct names for all 114 Land species, the five main zones, five Stillwater habitats, realm entry, creature ownership language, acquisition requirements and Arc reward explanations.

A further 223 shared strings and three Android plural resources cover collection/mastery badges, Badge Book, Showcase, Your Badges category/sort menus, Chest filters, reward paging, and Encounter terminology. Total checked coverage per language: **458 strings + 3 plural resources**. English remains the default fallback.

Translations use complete messages with numbered typed placeholders, preserve species distinctions, and retain established product and currency terminology. Badge and collection names use the same localized zone/vessel names as navigation. See [terminology and conventions](README.md) and the [shared resource inventory](shared-resource-keys.txt).

## Files and behavior

Resource files:

- `app/src/main/res/values/land_strings.xml`: extracted formerly hardcoded UI text and count-neutral requirements.
- `app/src/main/res/values-{hi,mr,es}/land_strings.xml`: complete Land translations.
- `app/src/main/res/values-{hi,mr,es}/land_badge_strings.xml`: complete Land badge translations.
- `app/src/main/res/values-{hi,mr,es}/strings.xml`: translated/updated shared feature controls, templates, and plurals.

Production Kotlin paths under `app/src/main/java/com/kingkharnivore/skillz/ui/screen/`:

- `shell/rooms/blue/BlueRealmSelector.kt`, `BlueUtils.kt`, `LandZonePage.kt`, `TheBlueDepthRail.kt`, `BeyondBlueEncounterSheet.kt`: resource-based realm, zone, requirement and accessibility text.
- `shell/rooms/blue/TheBlueCreatureTray.kt`: wrapping space for translated headings beside the action.
- `shell/ShellTopBar.kt` and `shell/rooms/stillwater/StillwaterRoomScreen.kt`: localized realm/destination labels.
- `shell/inventory/ShellChestScreen.kt`: resolves owned creature names from resources, refreshes with app language, and sorts with locale-sensitive collation.
- `shell/inventory/BadgesScreen.kt`: app-locale alphabetical ordering, numbers and dates, including Your Badges controls.
- `flow/reward/ArcSummaryContent.kt`, `RewardRevealMapper.kt`, `RewardRevealText.kt`: localized actual/pending Land reward names and reasons; formatting follows the selected app locale.

No DAO, repository, schema, migration, persisted ID, reward/economy or ownership changes were introduced by this localization pass. Existing changes to those files in the workspace belong to earlier feature work. No iOS files were changed.

## Validation

- `:app:assembleScyraDebug`: successful.
- Focused `:app:testScyraDebugUnitTest` run (`*Land*`, `*Badge*`, `*RewardRevealMapperTest`): **68 tests passed, zero failures/errors/skips**.
- Four resource-localization unit tests validate complete coverage, format argument parity, resource uniqueness, 114 distinct species names in each language, and English catalog-name fallbacks in feature UI.
- `LandLocalizationRuntimeTest`: **1 Android instrumentation test passed**, exercising packaged English/Hindi/Marathi/Spanish resources, all Land species and badge labels, and dynamic formatting at 0, 1, 2, 99 and 10,000.
- `git diff --check`: clean.

Tests and fixture:

- `app/src/test/java/com/kingkharnivore/skillz/localization/LandLocalizationTest.kt`
- `app/src/test/resources/land/localization-shared-keys.txt`
- `app/src/androidTest/java/com/kingkharnivore/skillz/domain/land/LandLocalizationRuntimeTest.kt`
- `app/src/test/java/com/kingkharnivore/skillz/ui/screen/flow/reward/RewardRevealMapperTest.kt` (localized text provider fixture)

Logs: [build/unit tests](../validation/localization-build-tests.txt), [Android resource test](../validation/localization-runtime.txt).

## Visual QA

Captures use mock data on the isolated Pixel 5/API 34 emulator with real Android per-app locale settings, including modal sheets. No user phone or primary emulator data was touched.

The [screenshot index](../screenshots/localization/README.md) links all 30 captures in `artifacts/land-expansion/screenshots/localization/`. Each `hi-`, `mr-`, and `es-` set covers the realm selector, populated Great Wild, Land Encounter, Stillwater habitats, 18-Flow Arc summary and Tiger/Chicken reward card, mixed Chest and Level 99 Tiger details, badge details, and Your Badges. Screens were inspected for translated names, Devanagari shaping/Spanish accents, long-label wrapping, spacing, and consistent terms. Scrollable sheets retain the existing collapsed/expanded behavior.

The test emulator's language preference is restored after capture. This pass does not constitute physical-device, every-font-size, every-screen, or independent native-speaker acceptance testing. Legacy app text outside the feature scope is not a complete app-wide translation rewrite.
