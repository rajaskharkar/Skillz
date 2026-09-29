# Android Land expansion implementation

The Blue now has Sea and Land entry. Sea keeps its existing renderer, four zones, Flow rewards, economy and ownership model. The existing Sea/Stillwater catalog block is unchanged byte-for-byte apart from appending Land. No iOS code was modified.

## Implemented

- Realm-aware model and copy; five Land world zones. Owned, active instances alone populate the world. Released, traded and unowned species stay out of scenes.
- 63 main Land definitions: Golden Fields 11, Ancient Woods 13, Open Sands 14, High Peaks 12, Great Wild 13. The five flagships are Arc-earned; the remaining 58 reuse Encounter Beyond the Blue and the specified premium Pearl prices.
- 51 restorative definitions in separate Stillwater habitats: Pasture 11, Glade 13, Oasis 9, Ravine 8, Sanctuary 10. Each maps to its corresponding Land world zone. They are excluded from Encounter and now use the shared Stillwater draw economy at 15,000 / 25,000 / 45,000 / 60,000 / 75,000 Drops. All 51 participate in the existing collection and Mastery systems.
- Shared Chest, growth, per-instance Level 99 Mastery, lifetime mastery records, release and contribution systems. Sea/Land filters are within the existing Chest filter menu.
- Shared creature detail and Encounter sheets, existing affordability/confirmation/persistence flows, realm-aware terminology. Encounter remains accessible in both populated and empty Sea.
- Five distinct Land environments: rolling field rows, layered woodland, dunes and mesas, snowy peaks, and remote acacia wilderness. Ambient grass, mist, dust, snow and fireflies use the existing shared scene clock. Scene creature counts are bounded; static icons and ambient creatures share the drawing pipeline.
- Fourteen reusable render families: small mammal, hoofed, canid, big cat, bear, primate, ground bird, raptor, reptile, snake, tortoise, marsupial, megafauna and scorpion. Species identifiers and catalog entries remain distinct.
- Debug-only synthetic previews for realms, zones, purchase states, mixed inventory, Level 99, Mastery, arbitrary Arc reward packages and Stillwater habitats. Fixtures do not write mock ownership into the user's database.
- Simple `BuildConfig.LAND_ENABLED` switch in `app/build.gradle.kts`.

## Arc rewards and storage

`LandArcRewards` calculates one Tiger for every complete block of 15 qualifying Flows, followed by at most one remainder reward: Chicken at 3, Deer at 6, Camel at 9, Moose at 12. Quantities are compact and not capped. Every granted copy is a normal independent owned instance.

The same persisted Arc membership queried by existing summaries supplies the count. No new duration, room, objective, movement or other eligibility condition was introduced. Soft Flows already counted by the Arc engine are included.

As requested, finalization waits until the existing five-minute continuation window expires. Flow start and reward finalization serialize to avoid paying while an Arc is resuming. Persisted ongoing Flow state also protects resumed Arcs after process restart. The application reconciles pending rewards on startup and every 15 seconds while alive. If killed, it reconciles when reopened; no acquisition depends on screen composition or navigation.

Room migration **40 → 41** only adds the `arc_land_reward` journal table. The journal, independent creature instances and reward-summary events finalize in one transaction. Reopening a summary, reloading the database, repeated events and concurrent retries do not duplicate rewards. Existing columns and stored creature IDs remain intact. No destructive migration or completed-history backfill is used; an ongoing Arc is journaled when its next Flow is persisted, using its full canonical membership.

The completion summary previews the pending package and observes committed finalization to refresh its persisted reward summary.

## Verification

- `assembleScyraDebug` and `assembleScyraDebugAndroidTest`: pass.
- Focused regression suite: **160 passing tests** across Land, creatures, Arc, Stillwater, Blue UI, achievements, rewards and Mastery.
- Full suite: **305 tests, 293 pass, 12 fail**. Remaining failures concern Movement calculations, Voyage statistics tie-breaking, existing slot compatibility, Focus indicators and Chest recent sorting. These areas were not rebalanced to make the suite green. Details are in `validation/full-unit-suite.txt`.
- Catalog fixture checks all 114 IDs, display names, world zones, sources, exact prices, flagship restrictions, habitat mappings and duplicate species/IDs.
- Arc tests cover every requested boundary, additional intermediate values, negative input and `Int.MAX_VALUE` without expanding the calculator's Tiger list.
- UI-model tests check ownership-only scenes, released/traded exclusion, multiple Tiger levels, realm isolation and restorative habitat-to-world placement.
- **6 emulator tests pass**, covering canonical soft/short Flow membership, expiry boundary, duplicate/concurrent finalization, database reopening, independent Tiger copies, purchases, Level 99, retained lifetime Mastery, release/trade, unpurchasable flagships and migration from shipped schema 40.
- `git diff --check`: clean.

See `validation/` for build and test logs, `files-changed.md` for the file inventory, and `screenshots/` for visually inspected emulator captures. Visual fixtures use production Compose components and an isolated debug activity on Pixel 5 API 34.

## Remaining limits and intentionally undefined work

Restorative Land acquisition is now enabled following the user’s subsequent economy decision. See `restorative-economy-update.md` for exact costs and rarity assignments. Sea Stillwater probabilities and duplicate mechanics are reused.

The renderer uses shared procedural families with species details; additional species-specific silhouette and motion polish remains appropriate before calling all 114 illustrations finished. Visual QA covers the listed emulator scenarios, not a full device, font-scale, TalkBack or performance certification matrix. The full unit suite's 12 failures remain unresolved, so this is not an unqualified release-ready sign-off.

Arc flagships remain unpurchasable and use Arc depth requirements. Following the user’s valuation decision, their base release values are Chicken 30, Deer 90, Camel 180, Moose 360 and Tiger 720 Pearls. Economic value is separate from acquisition requirements and feeds the existing trade contribution formula. Existing growth and release upgrade-investment recovery remain unchanged.

## Restorative economy follow-up validation

The follow-up build passes `assembleScyraDebug` and `assembleScyraDebugAndroidTest`, with **196 focused regression tests** and **8 emulator tests passing**. This includes all five habitat costs, exact rarity pool odds, one Rare and one Mythic per vessel, atomic shared Drop spending, no overspending under concurrent draws, database reload, Sea draw compatibility, Arc economics and collection/navigation integration. Logs are in `validation/restorative-build-tests.txt` and `validation/restorative-instrumentation.txt`. The earlier full-suite failures remain a separate outstanding task.
