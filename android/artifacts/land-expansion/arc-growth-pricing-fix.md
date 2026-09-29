# Arc flagship growth pricing fix

## Cause

The five Arc-earned species correctly have `arcFlowRequirement` and no `requirementMinutes` or `flowTimeValueMinutes`. However, `CreatureEconomy.baseGrowthCost` only checked those minute fields and a Stillwater habitat before falling back to 25. All five Arc flagships therefore used the minimum growth base.

The existing growth multiplier at Level 1 is 1.138. Integer truncation made the first Tiger upgrade cost 28 Pearls (25 × 1.138). Lion uses a base of 3,000, producing 3,414 Pearls. This was an actual transaction-pricing bug: both the Chest/Blue UI and `ShellRepository.growCreature` call the same calculator.

## Corrected rule

Arc flagships have an explicit `baseGrowthCostPearls` catalog field, separate from acquisition, base release and contribution values. Each flagship uses the premium purchasable growth tier in its zone. Values are explicit so unrelated catalog additions do not silently rebalance existing creatures.

| Arc species | Zone pricing peer | Growth base | Level 1 → 2 |
|---|---|---:|---:|
| Chicken | Horse | 720 | 819 |
| Deer | Black Bear | 1,200 | 1,365 |
| Camel | Monitor Lizard | 1,500 | 1,707 |
| Moose | Polar Bear | 2,400 | 2,731 |
| Tiger | Lion | 3,000 | 3,414 |

All use the established rising curve and Level 99 cap. Tiger matches Lion for every upgrade from Level 1 through Level 98. An Arc definition missing a positive growth base now fails validation in the calculator instead of silently receiving the minimum cost.

## Data and economy compatibility

No DAO, repository, database schema, migration or iOS changes. Existing creature IDs, ownership, levels, Mastery, Pearl ledger entries and balances are preserved. Already-owned creatures use the corrected cost on their next upgrade; there is no retroactive charge or level reset.

Arc thresholds and the continuation grace period are unchanged. Flagships remain unpurchasable. Base release values stay 30/90/180/360/720, and trade contributions remain unchanged. Sea and restorative growth calculations are unchanged.

The existing release formula estimates upgrade value from the current catalog curve rather than historical per-instance spend. Consequently, leveled Arc creatures—including copies upgraded before this fix—have higher release salvage values after this correction. This retains the existing release formula; it does not modify previously posted payouts or account balances.

## Files

- `app/src/main/java/com/kingkharnivore/skillz/utils/shell/CreatureEconomy.kt`: explicit optional catalog growth base and required Arc-specific resolution.
- `app/src/main/java/com/kingkharnivore/skillz/utils/shell/LandCreatureCatalog.kt`: five premium growth bases.
- `app/src/test/java/com/kingkharnivore/skillz/domain/shell/LandGrowthEconomyTest.kt`: five tests for zone tiers, Tiger/Lion parity, rising/cumulative costs, unchanged acquisition/base value, and exclusion from ordinary duration-based Flow rewards.
- `app/src/androidTest/java/com/kingkharnivore/skillz/domain/land/LandPersistenceTest.kt`: actual Arc Tiger transaction test for affordability rollback, exact debit, reload and idempotent replay.

## Verification

- Before fix: new unit tests reproduced both the flagship-tier and Tiger/Lion failures.
- After fix: debug app and Android test APK build successfully; **52 focused unit tests pass**.
- Isolated Pixel 5/API 34 emulator: **10 Land persistence tests pass**, including the new transaction test and existing Mastery/release/trade/reload checks. Tests use temporary databases; no phone or primary emulator data was modified.
- Broader unit run: **70/71 pass**. The failure is `ShellChestInventoryMapperTest.recentSortUsesBestEffortViewedOrAcquiredTimestampDescending`, also present in the previously recorded `validation/full-unit-suite.txt`. It uses Sea instances and is unrelated to the Arc growth calculation. It remains outside this fix.
- `git diff --check`: clean.

Logs: `validation/arc-growth-before.txt`, `validation/arc-growth-focused.txt`, `validation/arc-growth-broader.txt`, `validation/arc-growth-instrumentation.txt`.
