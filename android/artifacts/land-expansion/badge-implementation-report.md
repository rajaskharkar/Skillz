# Land and Sea Badge Book expansion

Implemented on Android, 17 September 2026.

## Coverage

- 32 new named milestone badges: Land discovery, collection variety, growth, first Mastery, Arc depth at 3/6/9/12/15, two Tigers from one Arc, all five Arc companions, rewarding Arc counts, restorative draws, five vessels, Rare/Mythic draws, release, contribution, Land Mastery counts and variety, and Sea variety.
- Collector, Curator and Completionist badges cover every zone and vessel: four Sea zones, five Land zones, four Sea Stillwater vessels and five Land Stillwater vessels.
- Aggregate collections cover main Sea (39), main Land (63), Sea Stillwater (32), Land Stillwater (51), shared Stillwater (83), Sea and Its Waters (71), Land and Its Habitats (114), and The Living Earth (185). All eight aggregates have all three collection badge types. There are 26 collection rosters and 78 collection badges in total.
- Species Mastery covers all 185 creatures, including all 114 Land creatures. Level 99 awards Mastery per individual copy; repeated copies increase the existing lifetime count.
- Existing ocean-specific badge display names now say Sea. Existing badge/collection IDs remain unchanged. Existing zone names and Encounter Beyond the Blue retain their names.

## Compatibility and evidence

No DAO, repository, entity, database, schema or migration files were changed during this badge pass. Earlier Land expansion persistence work remains in the working tree. A hash comparison with the start of this pass is recorded in `validation/badges-persistence-audit.json`.

The badge dashboard derives new milestone progress from existing persisted creature instances, discovery records and Mastery evidence. Released and contributed instances remain evidence. Existing pinning and tracking operations work with the derived badge dashboard and persist through the existing tables.

Arc badges use only actual Arc reward instances grouped by their persisted Arc source ID. These exist after authoritative finalization and the continuation grace period. Summed typed reward requirements provide the depth floor needed for the badge thresholds; no second Flow qualification rule or exact Flow count is invented. Two Tigers must come from the same Arc. Historical Arcs without Land grants are not retroactively inferred.

Existing Sea collection IDs keep their Sea-only rosters and earned history. Land collection badges reuse the existing generic collection achievement pipeline. Land Mastery previews and celebrations avoid displaying Sea aggregate counts as Land progress.

## Artwork and UX

Shared code-native Canvas artwork is used in the Badge Book, badge detail sheets and Mastery celebrations. Motifs include footprints, roots with Arc depth labels, a globe, landscape, growth, crowns, paired Tiger paws, a five-companion ring, return, restorative drops, rarity gems and release/contribution arrows. Each zone and vessel has an environment crest. Every species Mastery badge uses that creature’s own icon with a 99 label. Collection badge types retain distinct Collector/Curator/Completionist symbols.

Locked badges retain muted artwork and a small lock so users can see what they are pursuing. The existing search, categories, cards, sheets, accessibility labels, pinning and tracking remain in use. Artwork is only composed for visible badge items. Debug galleries and synthetic fixture screens are restricted to the debug source set.

## Files in this badge pass

Domain:
- `app/src/main/java/com/kingkharnivore/skillz/domain/achievement/LandBadgeCatalog.kt`
- `app/src/main/java/com/kingkharnivore/skillz/domain/achievement/AchievementEngine.kt`
- `app/src/main/java/com/kingkharnivore/skillz/domain/achievement/BadgeDashboard.kt`

UI and strings:
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/BadgePresentation.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/BadgeSymbolArtwork.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/BadgesScreen.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/MasteryCelebrationScreen.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/MasteryCelebrationUiState.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellChestScreen.kt`
- `app/src/main/res/values/land_badge_strings.xml`
- `app/src/main/res/values/strings.xml`

Tests/debug:
- `app/src/test/java/com/kingkharnivore/skillz/domain/achievement/LandBadgeTest.kt`
- `app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/MasteryCelebrationUiStateMapperTest.kt`
- `app/src/androidTest/java/com/kingkharnivore/skillz/domain/land/LandPersistenceTest.kt`
- `app/src/debug/java/com/kingkharnivore/skillz/debug/LandVisualTestActivity.kt`
- `app/src/debug/java/com/kingkharnivore/skillz/debug/BadgeArtworkPreview.kt`

## Validation

- Scyra debug APK builds successfully.
- Nine emulator instrumentation tests passed, including existing 40-to-41 migration preservation, Land rewards, ownership, purchase/trade/release/Mastery, Stillwater concurrency, and new badge pinning/tracking/reload coverage. See `validation/badges-instrumentation.txt`.
- The focused unit suite covers badge thresholds, all Land collection rosters, all species Mastery definitions, Sea scope preservation, historical earnings, Arc grant evidence, Stillwater rarity, release/contribution history, and Land Mastery celebration mapping. Result: 207 tests passed, 0 failures and 0 errors. See `validation/badges-build-unit-tests.txt`.
- All 56 persistence-related files in the pre-pass audit are unchanged.
- No iOS files were edited.

Thirteen screenshots were captured and visually reviewed on the isolated Pixel 5 / API 34 emulator in light mode, using debug-only synthetic data:

- `screenshots/badges/badge-showcase.png`
- `screenshots/badges/badge-book-tab.png`
- `screenshots/badges/land-icons-1.png` through `land-icons-4.png`
- `screenshots/badges/habitat-discovery-icons.png`
- `screenshots/badges/collection-icons-1.png` through `collection-icons-3.png`
- `screenshots/badges/tiger-mastery.png`
- `screenshots/badges/tiger-mastery-expanded.png`
- `screenshots/badges/arc-badge-expanded.png`

The review covered all 32 milestone badges and all 26 collection crests, plus the production Badge Book and Tiger/Arc detail sheets. It caught and corrected clipped Arc/99 labels and lock placement near circular edges. Expanded sheets were checked for readable text and reachable pin/track/actions. One emulator ANR occurred with the main thread waiting in Android window-manager Binder code; after restarting only the app process, these screens were recaptured successfully. This is not a physical-device performance certification.

## Deliberate limits

The new computed milestone badges do not add a separate persistent badge event or notification ledger. They can be earned, pinned and tracked, and survive reload using existing durable evidence. An exact historical unlock timestamp is unavailable when no prior badge event exists; the UI uses its existing unknown-date treatment rather than inventing a date. Existing species and collection badges continue through their existing persisted achievement pipeline.

This pass validates badge functionality and compatibility; it does not certify the entire Land release for a physical phone. Earlier full-suite testing had 12 failures outside this focused passing suite; these have not been resolved or proven against a clean baseline in this badge pass. Wider Land animation polish and physical-device release QA remain separate work.
