# Land multiple-copy rendering fix

LandZonePage iterated `zone.animals.take(6)`. That list contains one **species group** per entry, with copies in `totalCount`. Rendering one Canvas per entry both collapsed all copies into one animal and omitted owned species beyond the first six. Ownership and persisted counts were correct.

Land now calls Sea's existing `lifePresencePlan` and uses its individual limits and overflow cohorts. A Land-specific placement adapter distributes available walking lanes among species/copies, folding individuals that do not fit into an overflow silhouette. Exact counts remain in the tray, accessibility descriptions, and detail selection. Stable keys and motion offsets distinguish representatives. Species size differences and calm Land walking behavior remain intact. Plan generation is remembered; rendering work is bounded independently of total copies owned.

For 13 Tigers with room available: four individual Tigers and one smaller, fainter cohort silhouette representing nine more. Crowded scenes can fold additional copies into that silhouette. Every owned species receives a representative; there is no six-species truncation.

## Files changed for this fix

- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/blue/LandZonePage.kt`
- `app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/blue/land/LandAnimalPresence.kt`
- `app/src/debug/java/com/kingkharnivore/skillz/debug/LandVisualTestActivity.kt` (synthetic copy-count and dense-zone previews)
- `app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/blue/land/LandAnimalPresenceTest.kt`
- `app/src/androidTest/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/blue/LandPresenceRenderingTest.kt`

No changes to Sea rendering, repositories, DAO/schema, persisted creature data, rewards, or economy were needed.

## Validation

- Android debug application and Android test APK builds: passed.
- 28 focused unit tests: passed (7 presence, 4 Land scale/motion, 17 existing Blue UI model).
- Two emulator Compose tests: passed. Verify four direct Tigers plus overflow for 13 owned, exact count 13 when tapping overflow, and one Tiger without invented copies when only one is owned.
- Unit tests cover all 114 species using Sea's plan, 0–4 Tigers, 13 Tigers, constrained lanes, all Great Wild species, mixed-species copy allocation, stable ordering/keys, and Int.MAX_VALUE ownership without unbounded animations.
- Emulator visual checks use isolated synthetic data; no phone installation or user-data reset.
- Logs: `validation/multiple-copies-build.txt`, `validation/multiple-copies-instrumentation.txt`.
- Screenshots: `screenshots/multiple-copies/`.

Visually reviewed all six captures: `tigers-1.png`, `tigers-13.png`, `tigers-10000.png`, `mixed-copies.png`, `great-wild-dense.png`, and `sea-regression.png`. Confirmed multiple independent representatives, restrained overflow silhouettes, unchanged exact counts, distinct species sizes, clear controls/tray, and no animal/UI overlap on the tested Pixel 5 API 34 portrait viewport. Dense collections intentionally use smaller representatives, and crowded scenes may show fewer direct individuals.
