# Land Stillwater economy update

Land acquisition is now enabled using the shared Stillwater transaction, Drop ledger, confirmation and reveal components. No database schema change is needed for this update.

| Vessel | World zone | Drops | Common | Uncommon | Rare (8%) | Mythic (2%) |
|---|---|---:|---|---|---|---|
| Pasture | Golden Fields | 15,000 | Goose, Quail, Partridge, Wombat | Pheasant, Emu, Capybara, Wallaby, Echidna | Kangaroo | Peacock |
| Glade | Ancient Woods | 25,000 | Hedgehog, Sloth, Koala, Chameleon | Kinkajou, Lemur, Tapir, Mandrill, Gibbon, Pangolin, Platypus | Okapi | Panda |
| Oasis | Open Sands | 45,000 | Jerboa, Gazelle, Mongoose, Iguana | Oryx, Caracal, Gerenuk | Serval | Addax |
| Ravine | High Peaks | 60,000 | Pika, Chamois, Tahr, Macaque | Ibex, Vicuna | Wolverine | Takin |
| Sanctuary | Great Wild | 75,000 | Bison, Zebra, Wildebeest, Buffalo | Giraffe, Hippopotamus, Anteater, Lynx | Rhinoceros | Elephant |

Common and Uncommon pool probabilities are 60% and 30%. Selection is uniform within the selected pool; duplicates are allowed, exactly as in Sea Stillwater. Each vessel has exactly one Rare and one Mythic species. The user authorized rarity assignments based on animal grandness. Existing Sea rarity assignments and probabilities are unchanged.

Pasture and Glade draw immediately; Oasis, Ravine and Sanctuary require the same confirmation used by Pond and Lake. Drops come from the existing shared balance and retain existing generation rules. The same zone-access validation applies; Land zones are already accessible in the current Land experience. A draw grants one Level 1 owned instance in the corresponding world zone. Land restorative animals remain excluded from Encounter Beyond the Blue.

Restorative release, growth and contribution follow their Sea vessel counterparts. The 60,000-Drop Ravine interpolates through the same existing formulas: base release 500 Pearls. The other base release values are 125 / 208 / 375 / 625 Pearls, respectively.

## Arc flagship valuation

| Sea reference | Sea base release | Land counterpart | Land base release |
|---|---:|---|---:|
| Minnow | 20 | Chicken | 30 |
| Seahorse | 60 | Deer | 90 |
| Manta | 120 | Camel | 180 |
| Whale | 240 | Moose | 360 |
| — | — | Tiger | 720 |

Tiger continues the Deer → Camel → Moose doubling progression. The economic value is stored separately from acquisition requirements. All five still require completed Arc depth and have no purchase price. The shared economy also uses that canonical base value for trade contribution; existing upgrade-investment recovery remains additive on release. Arc thresholds, Sea values and growth rules are unchanged.

## Validation

- `assembleScyraDebug` and `assembleScyraDebugAndroidTest`: pass.
- 196 focused unit tests: pass, including collection/badge/navigation regressions.
- 8 emulator integration tests: pass, including atomic shared Drop spending, concurrent overspend protection, database reload, Arc rewards, Mastery/release and schema migration.
- `git diff --check`: clean.
- Nine visual captures in `screenshots/stillwater-*.png`: all five habitats, confirmation, reveal, post-draw affordability and shared Drops. Inspected for card spacing, costs, destination names, clipping and enabled/disabled states. Synthetic visual draws do not spend the user's real Drops. Repository tests separately exercise actual Room transactions. The inherited Drops-card text contrast remains part of the broader accessibility polish; this follow-up preserves its existing Sea styling.
- Updated debug build installed on the user's Pixel 9 emulator with data retained.

The broader expansion still has the previously reported creature-art/motion polish and wider release QA work. The earlier full-suite 12 failures were not part of this follow-up; no full-suite green claim is made.
