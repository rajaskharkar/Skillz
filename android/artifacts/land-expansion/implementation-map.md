# Android Land implementation map

Both September 2026 v1.1 specifications were read in full before implementation.

- `utils/shell/CreatureEconomy.kt`: canonical catalog, Sea Flow decomposition, pricing (2 Pearls/minute), growth and release. IDs are persisted strings; no realm/zone enum is stored on creature instances. Additive definitions need no creature-table rewrite.
- `data/model/shell/ShellDefinitions.kt`: separate resource-backed Shell find registry gates inventory, growth, release, placement and reward labels. Every Land species must exist in both registries.
- `ShellRepository`: `grantFindCopy` creates independent UUID instances and discovery evidence; `encounterBeyondBlue` atomically debits Pearls/trades instances; `growCreature` atomically records per-instance mastery at 99; release/trade retain records and mastery evidence.
- `AchievementEngine` / `BadgeDashboard`: collection rosters derive from catalog, stable completion evidence survives roster changes. Unavailable restorative species must be excluded from completion requirements.
- `FlowViewModel`: actual Flow completion assigns Arc ID/index and increments `sessionCountInArc`; soft Flows count too. Summary/Voyage history uses persisted Arc sessions. No Land-specific duration qualification is permitted.
- `ArcPrefs` / `ArcContinuationLifecycle` / resolver: active and recently-ended Arc snapshots survive restart in DataStore. Explicit completion still allows resuming within five minutes. User confirmed Land rewards must wait until continuation expires. Flow start can reactivate a recently-ended Arc. Expiration also happens on startup, ticking, cancellation and next Flow start.
- `FlowRepository` / `SessionDao`: persisted `arcId` identifies canonical Arc membership. Completion count must be derived from those records, with no duration/source filter.
- `ShellRewardOrchestrator` / recorder: existing Sea/Stillwater session rewards remain unchanged. Arc summary consumes persisted reward events.
- `TheBlueRoomScreen` / `TheBlueModels`: Sea vertical pager, detail/release sheets, depth rail, deep links. Four-zone UI enum is Sea-specific; need explicit Land mapping rather than ordinal fallthrough.
- `TheBlueZonePage` / draw / life-presence: single scene clock, stable seeded positions, density caps, UI-safe bounds, scale by family/level. Static Canvas icons use a separate dispatch. Land must route through its own grounded drawing path, not Sea swimmer fallbacks.
- `BeyondBlueEncounterSheet`: shared source-gated catalog list, affordability, contribution selections and confirmation; use realm-filtered zones.
- `ShellChestScreen`: existing instance stacks, levels, sorting, filters, growth/release and mastery presentation can be reused.
- Room shipped baseline is v40; registered migrations are explicit, with no destructive fallback. Reward lifecycle requires an additive durable journal and v40 migration tests.

Baseline: `assembleScyraDebug` passed before edits. Pixel 9 Pro XL emulator booted, but APK installation failed with insufficient storage. Existing emulator user data was preserved.

Final realm correction: Pasture, Glade, Oasis, Ravine and Sanctuary are `LandStillwaterHabitat` values, not `CreatureZone` values. Their catalog entries carry a separate habitat and a main Land world zone. Restorative creatures are excluded from Encounter and remain unavailable. Land world rendering consumes owned active instances only. Sea retains its existing pages with an Encounter entry restored for the empty state.
