# Badge UX update

Implemented the requested Showcase and Badge Book layout.

- Showcase accepts any number of earned badges, preserving pin order and existing pins.
- Your Badges sits directly below Showcase and includes every earned badge, including pinned ones, in alphabetical order. Tiles show artwork, the exact count on the icon, and the badge name. Tapping opens the existing detail sheet for pinning/unpinning.
- Tracked Badges is the first section in Badge Book. Links to tracked badges now open that tab.
- Locked badges display only a lock within their progress ring; the artwork and count plate stay hidden until earned, including in detail sheets.
- Both galleries render rows lazily and adapt their column count to available width and font scaling. Badge names can wrap instead of being truncated.

## Data compatibility

The repository contained the three-pin business-rule limit. This update removes only the two limit guards from `ShellRepository.pinBadge`; the existing transaction, eligibility checks, duplicate handling, pin order, unpin behavior and explicit replacement compatibility remain. No DAO, entity, schema or migration changes were needed. This narrowly scoped repository change implements the newer request for unlimited pins.

## Files

- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/BadgesScreen.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/BadgeGallery.kt`
- `app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/ShellRepository.kt` (two pin-limit guards removed)
- `app/src/main/res/values/strings.xml`
- `app/src/debug/java/com/kingkharnivore/skillz/debug/LandVisualTestActivity.kt` (interactive preview pins/tracking)
- `app/src/androidTest/java/com/kingkharnivore/skillz/domain/land/LandPersistenceTest.kt`

## Verification

- Debug app and instrumentation APKs build successfully.
- 83 focused badge, achievement, collection, Mastery and Land unit tests passed.
- 10 emulator persistence/migration tests passed. New coverage pins five badges, verifies duplicate pinning is harmless, reloads the database, unpins and re-pins while checking order, and rejects an unearned badge.
- Logs: `validation/badge-ux-build-tests.txt`, `validation/badge-ux-instrumentation.txt`.
- Captured and visually inspected on the isolated Pixel 5 / API 34 emulator: `screenshots/badge-ux/showcase-five-pins.png`, `your-badges-grid.png`, `tracked-first.png`, `locked-detail.png`.
- QA uses synthetic debug fixtures; no phone data was modified. No iOS changes.
