# Data Model and Persistence Map

## 1. Purpose

This document maps Android's current persistence model so future iOS work can replicate Scyra's Flow sessions, ongoing Flow state, Journey/tag data, Pulse data, Pulse-to-Flow links, planned Flows, Arc plans, active Arc runs, reward breakdowns, Health/movement snapshots, Pearls, creature ownership, creature levels/status, Stillwater, Lookout objectives, Badges, Shell room state, app settings, notepad data, and legacy/removal candidates.

This is documentation only. It does not create iOS code, create SwiftData models, create database migrations, copy Room schema files, modify Android source, change Gradle/build files, move files, or add dependencies.

## 2. Source Material Inspected

Prior docs inspected:

- `docs/00_REPO_BOUNDARIES.md`
- `docs/01_ANDROID_ARCHITECTURE.md`
- `docs/02_SCYRA_PRODUCT_SPEC.md`
- `docs/03_NAVIGATION_AND_SCREEN_MAP.md`
- `docs/04_DESIGN_SYSTEM.md`

Android build/schema files inspected:

- `android/app/build.gradle.kts`
- `android/app/schemas/com.kingkharnivore.skillz.data.model.SkillzDatabase/31.json`
- `android/app/schemas/` exists and currently contains the latest inspected Room schema JSON for version 31.

Android database files inspected:

- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/SkillzDatabase.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/migration/SkillzDatabaseMigrations.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/di/DatabaseModule.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/di/ShellDatabaseModule.kt`

Entity files inspected:

- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/TagEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/SessionEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/PulseEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/PulseFlowLinkEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/OngoingSessionEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/FlowPlanEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ArcPlanEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ArcPlanStepEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ActiveArcRunEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/health/FlowHealthSnapshotEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/health/FlowHealthSyncStatus.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/health/FlowRewardBreakdownEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/shell/ObjectiveEntities.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/shell/ShellEntities.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/shell/ShellRewardEventEntity.kt`

DAO files inspected:

- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/TagDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/SessionDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/PulseDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/OngoingSessionDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/FlowPlanDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/ArcPlanDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/ActiveArcRunDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/health/FlowHealthDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/IdeaGroveDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ObjectiveDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ShellDaoProvider.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ShellDaos.kt`

Repository and DataStore files inspected:

- `FlowRepository`, `AliveFlowRepository`, `JourneyRepository`, `PulseRepository`, `FlowPlanRepository`, `ArcPlanRepository`, `ActiveArcRunRepository`, `NotepadRepository`
- `FlowHealthRepository`, `HealthPermissionRepository`, `HealthSettingsRepository`
- `ShellRepository`, `IdeaGroveRepository`, `LookoutRepository`
- `UserPrefs`, `ArcPrefs`, `DatabaseModule`, `ShellDatabaseModule`

Tests inspected or identified as persistence-relevant:

- `android/app/src/androidTest/java/com/kingkharnivore/skillz/data/model/SkillzMigrationTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/health/DelayedMovementRewardPolicyTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/health/FlowActiveIntervalTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/health/MovementBonusCalculatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/lookout/ObjectiveProgressCalculatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/CreatureEconomyTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardEventRecorderTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardEventAggregatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellChestInventoryMapperTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellNotificationInlayMapperTest.kt`

Expected path notes:

- No iOS persistence files exist yet, as required.
- No Android WorkManager persistence table was observed. TODO: verify if future worker state is purely runtime/scheduled.
- No separate Room entity for Voyage Hall was observed; stats appear computed from sessions/Journeys/health/rewards. TODO: verify in Voyage Hall utilities before iOS implementation.

## 3. Persistence Architecture Summary

Android uses Room plus Preferences DataStore:

- Room database class: `SkillzDatabase`.
- Database name: `skillz_db` in `DatabaseModule`.
- Room version: `31`.
- `exportSchema = true`; schema JSON exists at `android/app/schemas/com.kingkharnivore.skillz.data.model.SkillzDatabase/31.json`.
- Migrations live in `SkillzDatabaseMigrations.kt` and are attached by `DatabaseModule` through `addMigrations(*SkillzDatabaseMigrations.ALL_MIGRATIONS)`.
- Entity packages are split into core entities, `health`, and `shell` subpackages.
- DAO packages mirror the entity split and include Shell DAO interfaces grouped in `ShellDaos.kt` plus Lookout/Idea Grove DAOs.
- Repository layer wraps persistence for Flow, ongoing Flow, Journey, Pulse, plans, Arcs, Health, Shell, Lookout, Idea Grove, and Notepad.
- DataStore usage is split between `skillz_prefs` from `DatabaseModule`, `user_prefs` from `UserPrefs`, and repositories/prefs that write settings keys.
- In-memory/runtime state includes ViewModel UI state, selected destinations, sheets/dialogs, reward reveal UI models, and Focus Room player state; persisted state includes Room entities and DataStore preferences.
- Transactions use `RoomDatabase.withTransaction` in repositories for operations such as delayed movement reward updates, Lookout claims, Shell room opens, Pearl spending/earning, creature growth/release, Beyond Blue encounters, and Idea Grove link repair.
- Ledgers are explicit for Pearls (`pearl_ledger`), Stillwater (`stillwater_ledger`), and Shell reward events (`shell_reward_event`).
- Singleton tables include `ongoing_session`, `active_arc_run`, and `stillwater_preference`.
- Migration test coverage exists in `SkillzMigrationTest.kt`; domain tests cover movement, creature economy, Shell reward events, Lookout progress, and Chest mapping.

Future iOS direction:

- Use SwiftData or SQLite behind repositories; do not let SwiftUI screens depend directly on storage APIs.
- Provide in-memory test stores for Flow, rewards, Health snapshots, Shell economy, and settings.
- Do not expose Android table names such as `tags` or `ShellFind` as product terminology where product language is Journey/Creature.
- iOS persistence files must live under `ios/` later and must not import or depend on Android schema JSON at build/runtime.

## 4. Room Database Inventory

| Item | Android value/source | Notes | iOS implication |
| ---- | -------------------- | ----- | --------------- |
| Database class | `SkillzDatabase.kt` | Extends `RoomDatabase` and `ShellDaoProvider`. | Create an iOS persistence container behind repositories. |
| Database name | `skillz_db` in `DatabaseModule.kt` | Android-only local DB filename. | iOS may choose its own store name, e.g. `Scyra.sqlite`. |
| Version | `31` | Current inspected Room schema version. | iOS starts fresh; versioning strategy still needed. |
| Schema export | `exportSchema = true` | JSON schema exported under `android/app/schemas/.../31.json`. | Reference-only; do not copy/import into iOS build. |
| Entities | 25 Room entities registered | Core, health, Shell, Lookout, Stillwater, reward entities. | Map to clean domain/persistence models. |
| DAOs | Core DAOs plus `FlowHealthDao`, `IdeaGroveDao`, Shell DAOs, Lookout DAOs | `ShellDaoProvider` exposes many Shell DAO interfaces. | iOS repository protocols should cover same feature operations. |
| Migrations | `SkillzDatabaseMigrations.ALL_MIGRATIONS` | Legacy-to-15, Shell, objectives, Idea Grove, movement bonus, notification viewed columns. | iOS migration plan separate; Android migration history informs import mapping only. |
| TypeConverters | None observed in `SkillzDatabase.kt` | Enums such as `FlowHealthSyncStatus` are persisted by Room support/TODO verify exact converter behavior. | Prefer explicit raw-value enums on iOS. |
| Destructive migration | None observed; migrations are attached. | Existing Android data is migrated. | iOS should avoid destructive migration once shipping. |
| Transaction helpers | `RoomDatabase.withTransaction` used in repositories. | Atomicity is repository-level. | Use SQLite transactions/SwiftData contexts explicitly. |
| Latest schema inspected | `31.json` | Contains tables listed in section 5. | Use as human reference only. |

## 5. Entity Inventory

| Android class | File path | Table | PK | Important fields / defaults / nullables | Indexes/FKs | Purpose/lifecycle | Repository/DAO | iOS domain / persistent model | Priority | Notes/TODOs |
|---|---|---|---|---|---|---|---|---|---|---|
| `TagEntity` | `data/model/entity/TagEntity.kt` | `tags` | `id` auto Long | `name`, `createdAt = now` | none observed | Journey/tag created/reused by Flow/Pulse/plans; deleted when unused by cleanup paths. | `TagDao`, `JourneyRepository` | `Journey` / `JourneyRecord` | MVP | User-facing term is Journey, not tag. |
| `SessionEntity` | `entity/SessionEntity.kt` | `sessions` | `id` auto Long | `title`, `description`, `tagId`, `startTime`, `endTime`, `durationMs`, nullable `surgePlannedMs`, `surgePoints`, `scyraPoints`, `isSoftMode=false`, nullable `arcId/arcIndex/arcMultiplierUsed`, `arcBonusPoints`, `createdAt` | FK `tagId -> tags` cascade; indices `tagId`, `arcId` | Completed Flow storage; created on Flow completion; updated for description/reward/Arc; deleted from Story/cleanup. | `SessionDao`, `FlowRepository` | `FlowSession` / `FlowSessionRecord` | MVP | Soft Flow is a subtype but should not award Scyra Score. |
| `PulseEntity` | `entity/PulseEntity.kt` | `pulses` | `id` auto Long | `title`, `description`, nullable `tagId`, nullable `parentSessionId`, nullable `parentFlowInstanceId`, nullable `arcId`, `createdAt`, `updatedAt`, `groveStatus='ALIVE'`, nullable `groveStatusChangedAt` | FK `tagId -> tags` set null, `parentSessionId -> sessions` set null; indices tag/session/flow/arc/created | Thought/idea record; created/edited/deleted from Pulse/Story/Idea Grove; may attach to active/completed Flow. | `PulseDao`, `PulseRepository`, `IdeaGroveDao` | `Pulse` / `PulseRecord` | MVP | Pulses are not rewarded sessions. |
| `PulseFlowLinkEntity` | `entity/PulseFlowLinkEntity.kt` | `pulse_flow_links` | `id` auto Long | `pulseId`, `sessionId`, `linkedAt` | FK pulse cascade, session cascade; index `pulseId`; unique `sessionId` | Links Pulses to completed Flow sessions; one session appears limited to one link by unique session index. | `IdeaGroveDao` | `PulseFlowLink` / `PulseFlowLinkRecord` | MVP | TODO: verify intended cardinality. |
| `OngoingSessionEntity` | `entity/OngoingSessionEntity.kt` | `ongoing_session` | `id=1` | `flowInstanceId`, title/description/tagName snapshot, `isInFlowMode`, `isRunning`, `isSoftMode`, nullable `baseStartTimeMs`, `accumulatedBeforeStartMs`, Surge fields, Arc fields, origin Pulse snapshots, Health eligibility flags, nullable `activeIntervalJson` | singleton table | Active Flow restoration across process death/background; upserted while Flow active; cleared on completion/discard. | `OngoingSessionDao`, `AliveFlowRepository` | `OngoingFlow` / `OngoingFlowRecord` | MVP | Critical for iOS timestamp-based restoration. |
| `FlowPlanEntity` | `entity/FlowPlanEntity.kt` | `flow_plans` | `id` auto Long | title, nullable `tagId`, `isSoftMode`, nullable `targetMinutes`, `launchWithSurge`, `pinned`, `archived`, launch count/time, timestamps | FK `tagId -> tags` set null; index `tagId` | Planned Flow in Paths; created/edited/pinned/archived/launched/deleted. | `FlowPlanDao`, `FlowPlanRepository` | `PlannedFlow` / `PlannedFlowRecord` | Phase 2/MVP if Paths in scope | TODO: decide iOS MVP scope. |
| `ArcPlanEntity` | `entity/ArcPlanEntity.kt` | `arc_plans` | `id` auto Long | `title`, `isInStudio`, `archived`, `launchCount`, nullable `lastLaunchedAt`, `recurrenceType`, `recurrenceDaysCsv`, timestamps | none observed | Arc plan header; created/edited/studio/archive/launched/deleted. | `ArcPlanDao`, `ArcPlanRepository` | `ArcPlan` / `ArcPlanRecord` | Phase 2/MVP if Arcs in scope | Recurrence strings should become typed enums. |
| `ArcPlanStepEntity` | `entity/ArcPlanStepEntity.kt` | `arc_plan_steps` | `id` auto Long | `arcPlanId`, `orderIndex`, nullable `sourceFlowPlanId`, title/tag/soft/target/surge snapshots, `linkState`, timestamps | FK arc cascade, plan/tag set null; indices `arcPlanId`, `sourceFlowPlanId`, `tagIdSnapshot` | Ordered Arc steps; replaced transactionally in DAO. | `ArcPlanDao`, `ArcPlanRepository` | `ArcStep` / `ArcStepRecord` | Phase 2/MVP | Snapshot design should be preserved. |
| `ActiveArcRunEntity` | `entity/ActiveArcRunEntity.kt` | `active_arc_run` | `id=1` | `arcPlanId`, arc/current step snapshots, current tag/soft, started/updated timestamps | singleton | Active Arc progression singleton; updated step, cleared on completion/cancel. | `ActiveArcRunDao`, `ActiveArcRunRepository` | `ActiveArcRun` / `ActiveArcRunRecord` | Phase 2/MVP | Coexists with `ArcPrefs`; clarify source of truth. |
| `FlowHealthSnapshotEntity` | `entity/health/FlowHealthSnapshotEntity.kt` | `flow_health_snapshots` | `sessionId` | start flags, `status`, nullable `steps`, `rawMovementPoints`, final Scyra/Pearl movement contributions, checked/captured/expires timestamps, `checkCount`, Flow bounds, nullable `activeIntervalJson`, `sourceLabel`, `updatedAfterSync=false` | FK session cascade; indices session/status/expires | Health snapshot and delayed movement refresh state per completed Flow. | `FlowHealthDao`, `FlowHealthRepository` | `MovementSnapshot` / `MovementSnapshotRecord` | MVP if Movement Points included | Status enum must be explicit in iOS. |
| `FlowRewardBreakdownEntity` | `entity/health/FlowRewardBreakdownEntity.kt` | `flow_reward_breakdowns` | `sessionId` | non-movement points, `pulseBonusPoints`, `surgeBonusPoints`, other bonus, movement, multipliers, arc bonus, final points, Pearls, eligibility, rounding mode | FK session cascade; index session | Persistent reward explanation per Flow. | `FlowHealthDao`, `FlowHealthRepository` | `RewardBreakdown` / `RewardBreakdownRecord` | MVP | `pulseBonusPoints` needs Task 1.7 verification; do not infer Pulse reward product rule. |
| `PearlLedgerEntity` | `entity/shell/ShellEntities.kt` | `pearl_ledger` | `id` String | `delta`, `reason`, `sourceType`, nullable `sourceId`, `createdAt`, nullable `note` | none observed | Ledger-backed Pearl balance; earnings/spending/deltas. | `PearlLedgerDao`, `ShellRepository`, `FlowHealthRepository`, `LookoutRepository` | `PearlLedgerEntry` / `PearlLedgerRecord` | MVP | iOS must not use balance-only storage. |
| `UserShellFindInstanceEntity` | `ShellEntities.kt` | `user_shell_find_instance` | `instanceId` String | `findId`, `acquiredAt`, source fields, nullable upgrade/custom/viewed/flow time, `isNew`, `isArchivedInChest`, `animalLevel=1`, `creatureStatus='ACTIVE'`, nullable `creatureSource` | indices find/source/sourceId | Individual Shell find/creature instance; created by rewards/draws/trades; status changes on release/use. | `ShellFindInstanceDao`, `ShellRepository` | `CreatureInstance` / `CreatureInstanceRecord` | MVP for The Blue/Chest | Legacy `ShellFind` name; iOS product model should say Creature. |
| `UserShellFindStackEntity` | `ShellEntities.kt` | `user_shell_find_stack` | `findId` String | `quantity`, first/last acquired, `isNew`, nullable `viewedAt` | none observed | Legacy/summary stack by find ID. | `ShellFindStackDao`, `ShellRepository` | `CreatureStackSummary` / maybe derived | Legacy/TODO | Chest should derive level-aware stacks from instances, not this species-only stack. |
| `ShellPlacementEntity` | `ShellEntities.kt` | `shell_placement` | `placementId` String | `roomId`, `slotId`, `instanceId`, `placedAt` | room/slot, unique room+slot, unique instance | Places Shell objects/instances into rooms/slots. | `ShellPlacementDao`, `ShellRepository` | `ShellPlacement` / `ShellPlacementRecord` | Legacy/Phase 2 | Object placement may not be desired for iOS MVP. |
| `ShellFindUpgradeEntity` | `ShellEntities.kt` | `shell_find_upgrade` | `upgradeEventId` String | `instanceId`, nullable `fromStageId`, `toStageId`, `pearlCost`, `upgradedAt` | index instance | Upgrade history for Shell finds/creatures/objects. | `ShellFindUpgradeDao`, `ShellRepository` | `CreatureGrowthEvent` / `CreatureGrowthRecord` | MVP for creatures; Legacy for objects | Product-clean mapping should separate creature growth from object upgrades. |
| `UserBadgeEntity` | `ShellEntities.kt` | `user_badge` | `badgeId` String | `count`, `firstEarnedAt`, `lastEarnedAt`, `isNew`, nullable `viewedAt` | none observed | Countable earned badges and notification state. | `UserBadgeDao`, `ShellRepository`, `LookoutRepository` | `BadgeAward` / `BadgeRecord` | MVP | Supports countable Mastery if award logic exists; gap noted below. |
| `UserDiscoveryEntity` | `ShellEntities.kt` | `user_discovery` | `userDiscoveryId` String | `discoveryId`, `discoveredAt`, source fields, nullable `grantedFindInstanceId`, `isNew`, nullable `viewedAt` | discovery/source indices | Discovery Journal / notification data. | `UserDiscoveryDao`, `ShellRepository` | `DiscoveryRecord` | Legacy | Do not port unless product confirms/data compatibility requires. |
| `StillwaterLedgerEntity` | `ShellEntities.kt` | `stillwater_ledger` | `id` String | `units`, `sourceType`, nullable `sourceId`, `createdAt` | source indices | Stillwater units ledger; Soft Flow/rest reward pool. | `StillwaterLedgerDao`, `ShellRepository` | `StillwaterLedgerEntry` / `StillwaterLedgerRecord` | Phase 2/MVP if Soft Flow included | Ledger semantics should be preserved. |
| `StillwaterPreferenceEntity` | `ShellEntities.kt` | `stillwater_preference` | `id=1` | `perspective`, `updatedAt` | singleton | Stillwater room preference/view choice. | `StillwaterPreferenceDao`, `ShellRepository` | `StillwaterPreference` / `StillwaterPreferenceRecord` | Phase 2 | TODO: verify product priority. |
| `UserShellRoomStateEntity` | `ShellEntities.kt` | `user_shell_room_state` | `roomId` String | nullable first/last opened, `visualMaturityScore`, `ambientLifeScore`, nullable last changed | singleton per room | Room memory/state, opened timestamps and ambient scores. | `UserShellRoomStateDao`, `ShellRepository` | `ShellRoomState` / `ShellRoomStateRecord` | Phase 2 | Preserve if Shell room progression needed. |
| `ShellRewardEventEntity` | `ShellRewardEventEntity.kt` | `shell_reward_event` | `id` String | `sourceSessionId`, nullable `arcId`, `rewardType`, nullable `rewardId`, `quantity`, `occurredAt` | source session, arc, rewardType, unique session+type+id | Reward read model/event log for Shell notifications and Arc/session aggregation. | `ShellRewardEventDao`, reward recorders | `ShellRewardEvent` / `ShellRewardEventRecord` | MVP | Event types include legacy object/trinket/discovery values. |
| `ObjectiveEntity` | `ObjectiveEntities.kt` | `objectives` | `id` auto Long | Journey snapshot, `periodType`, `objectiveType`, target/window fields, weekly day, streaks/completions, archived, timestamps | indices journey/period/archived | Lookout objective definition. | `ObjectiveDao`, `LookoutRepository` | `LookoutObjective` / `LookoutObjectiveRecord` | Phase 2 | Journey FK is not declared; stores IDs/snapshots. |
| `ObjectiveCompletionEntity` | `ObjectiveEntities.kt` | `objective_completions` | `id` auto Long | objective/journey snapshots, period/window, completed/achieved/target, reward Pearls, streak multiplier, badge, claimed flags/time | unique objective+period window; indices journey/period | Lookout completion/claim row; Pearls may be granted/claimed. | `ObjectiveCompletionDao`, `LookoutRepository` | `LookoutCompletion` / `LookoutCompletionRecord` | Phase 2 | Claim transaction writes Pearl ledger. |
| `ObjectiveSkippedCycleEntity` | `ObjectiveEntities.kt` | `objective_skipped_cycles` | `id` auto Long | `objectiveId`, period window, `skippedAt` | unique objective+period window | Lookout skip state. | `ObjectiveSkippedCycleDao`, `LookoutRepository` | `LookoutSkippedCycle` / `LookoutSkippedCycleRecord` | Phase 2 | TODO: verify iOS MVP need. |

## 6. Core Domain Mapping Table

| Product concept | Android persistence | Android repository/DAO | Future iOS domain model | Future iOS persistent model | Notes |
|---|---|---|---|---|---|
| Flow | `SessionEntity` | `FlowRepository`, `SessionDao` | `FlowSession` | `FlowSessionRecord` | Core rewarded focus unit. |
| Ongoing Flow | `OngoingSessionEntity` | `AliveFlowRepository`, `OngoingSessionDao` | `OngoingFlow` | `OngoingFlowRecord` | Restore from timestamps/intervals. |
| Journey | `TagEntity` | `JourneyRepository`, `TagDao` | `Journey` | `JourneyRecord` | Android internal name is tag. |
| Pulse | `PulseEntity` | `PulseRepository`, `PulseDao` | `Pulse` | `PulseRecord` | Thought/idea, not rewarded session. |
| Pulse-to-Flow Link | `PulseFlowLinkEntity` | `IdeaGroveDao/Repository` | `PulseFlowLink` | `PulseFlowLinkRecord` | Unique session link currently. |
| Story/Chronicle | `sessions`, `pulses`, arcs, health/rewards | DAOs/repositories | `ChronicleEntry` | usually derived | Do not create table unless needed. |
| Planned Flow | `FlowPlanEntity` | `FlowPlanRepository` | `PlannedFlow` | `PlannedFlowRecord` | Paths feature. |
| Arc Plan | `ArcPlanEntity` | `ArcPlanRepository` | `ArcPlan` | `ArcPlanRecord` | Structured progression. |
| Arc Step | `ArcPlanStepEntity` | `ArcPlanRepository` | `ArcStep` | `ArcStepRecord` | Snapshot fields matter. |
| Active Arc Run | `ActiveArcRunEntity`, `ArcPrefs` | `ActiveArcRunRepository`, `ArcPrefs` | `ActiveArcRun` | `ActiveArcRunRecord` | Clarify Room vs DataStore state. |
| Reward Breakdown | `FlowRewardBreakdownEntity` | `FlowHealthRepository` | `RewardBreakdown` | `RewardBreakdownRecord` | Needed for reveal/history. |
| Movement Snapshot | `FlowHealthSnapshotEntity` | `FlowHealthRepository` | `MovementSnapshot` | `MovementSnapshotRecord` | HealthKit equivalent. |
| Pearl Ledger | `PearlLedgerEntity` | `ShellRepository`, `LookoutRepository`, `FlowHealthRepository` | `PearlLedgerEntry` | `PearlLedgerRecord` | Ledger-backed balance. |
| Creature Instance | `UserShellFindInstanceEntity` | `ShellRepository` | `CreatureInstance` | `CreatureInstanceRecord` | Product-clean name. |
| Creature Stack | `UserShellFindStackEntity` plus derived instance groups | `ShellRepository`, Chest mapper | `CreatureStack` | Derived or `CreatureStackRecord` only if needed | Level-aware stacks should derive from instances. |
| Creature Release Status | `creatureStatus` | `ShellRepository` | `CreatureStatus` | enum raw value | ACTIVE/RELEASED/USED_BEYOND_BLUE. |
| Creature Level/Growth | `animalLevel`, `ShellFindUpgradeEntity` | `ShellRepository` | `CreatureLevel`, `CreatureGrowthEvent` | `CreatureInstanceRecord`, `CreatureGrowthRecord` | Level cap 99. |
| The Chest | Creature instances and derived stacks | `ShellRepository`, Chest UI mapper | `ChestInventory` | derived from creatures | Creature-only, no released creatures. |
| Badges | `UserBadgeEntity` | `ShellRepository`, `LookoutRepository` | `Badge` | `BadgeRecord` | Countable. |
| Creature Mastery | `UserBadgeEntity` desired; economy tier in code | `ShellRepository` TODO | `CreatureMasteryBadge` | `BadgeRecord` | Award gap needs follow-up. |
| Stillwater Drops | `StillwaterLedgerEntity` | `ShellRepository` | `StillwaterLedgerEntry` | `StillwaterLedgerRecord` | Soft Flow/rest layer. |
| Stillwater Draws/Preferences | Ledger + `StillwaterPreferenceEntity` | `ShellRepository` | `StillwaterDraw`, `StillwaterPreference` | ledger/preference records | Draw state mostly transactional. |
| Idea Grove | `PulseEntity.groveStatus`, links | `IdeaGroveRepository` | `IdeaGroveItem` | `PulseRecord` + links | Pulse-derived. |
| Lookout Objective | `ObjectiveEntity` | `LookoutRepository` | `LookoutObjective` | `LookoutObjectiveRecord` | Period windows. |
| Lookout Completion/Claim | `ObjectiveCompletionEntity` | `LookoutRepository` | `LookoutCompletion` | `LookoutCompletionRecord` | Claim writes Pearls/badges. |
| Voyage Hall stats | computed from sessions/tags/rewards | Voyage utilities/repos | `VoyageStats` | derived/cache optional | Do not persist unless needed. |
| Shell Room State | `UserShellRoomStateEntity` | `ShellRepository` | `ShellRoomState` | `ShellRoomStateRecord` | Room memory. |
| Shell Notifications | `isNew/viewedAt`, `ShellRewardEventEntity` | Shell repo/notification mappers | `ShellNotification` | derived/read model | Mark viewed updates source rows. |
| Notepad | DataStore keys | `NotepadRepository` | `NotepadDocument` | settings/blob record or UserDefaults | Rich editor HTML. |
| User Settings | `UserPrefs`, DataStore | `UserPrefs` | `UserSettings` | UserDefaults/settings repo | Score/calm/language. |
| Language Setting | `app_language_tag` | `UserPrefs` | `AppLanguage` | settings key | Optional string. |
| Movement Bonus Setting | `movement_bonus_enabled` | `HealthSettingsRepository` | `MovementSettings` | settings key | Default false. |

## 7. DAO Inventory

| DAO | File path | Tables touched | Key operations | Flow/observable outputs | iOS repository equivalent | Notes |
|---|---|---|---|---|---|---|
| `TagDao` | `dao/TagDao.kt` | `tags` | insert, get by name, list, delete | `getAllTags()` | `JourneyRepository` | Sorts by name. |
| `SessionDao` | `dao/SessionDao.kt` | `sessions` | insert, list, delete, get by id, update description, update Arc/reward fields, counts | sessions by tag/all | `FlowSessionRepository` | Provides Arc queries and regular session counts. |
| `PulseDao` | `dao/PulseDao.kt` | `pulses` | insert/update/delete, list, session/arc queries, attach/detach live pulses | all pulses, pulses for session | `PulseRepository` | Attachment updates parent fields. |
| `OngoingSessionDao` | `dao/OngoingSessionDao.kt` | `ongoing_session` | observe singleton, upsert, clear | ongoing session | `OngoingFlowRepository` | Key for restoration. |
| `FlowPlanDao` | `dao/FlowPlanDao.kt` | `flow_plans` | insert/update/get/list active/archive, pin/archive, mark launched, delete | active/archived plans | `PlannedFlowRepository` | No transaction beyond single operations. |
| `ArcPlanDao` | `dao/ArcPlanDao.kt` | `arc_plans`, `arc_plan_steps` | plan/step CRUD, active/archive/studio lists, mark launched, replace steps transaction | active/archived/studio plans, steps | `ArcPlanRepository` | `replaceAllSteps` is transaction. |
| `ActiveArcRunDao` | `dao/ActiveArcRunDao.kt` | `active_arc_run` | observe singleton, upsert, clear, update current step | active run | `ActiveArcRunRepository` | Singleton id 1. |
| `FlowHealthDao` | `dao/health/FlowHealthDao.kt` | `flow_health_snapshots`, `flow_reward_breakdowns` | upsert snapshot/breakdown, get, observe, refreshable count/list, disable/expire, transactional completion upsert | snapshots | `MovementSnapshotRepository` | Delayed refresh support. |
| `IdeaGroveDao` | `dao/shell/IdeaGroveDao.kt` | `pulses`, `pulse_flow_links` | observe links, update grove status, insert link, get links, repair completed pulses | pulse-flow links | `IdeaGroveRepository` | Uses existing Pulse records. |
| `PearlLedgerDao` | `dao/shell/ShellDaos.kt` | `pearl_ledger` | observe/get balance, insert, source reward count, recent | balance/recent | `PearlLedgerRepository` | Duplicate prevention by source/reason checks. |
| `ShellFindInstanceDao` | `ShellDaos.kt` | `user_shell_find_instance`, `shell_placement` subquery | insert/update/observe/get/count/unplaced/mark viewed/level/status/query active level | all/unplaced instances | `CreatureRepository` | Supports level-aware Chest queries. |
| `ShellFindStackDao` | `ShellDaos.kt` | `user_shell_find_stack` | upsert/get/observe/mark viewed | stacks | `CreatureStackRepository` or derived | Legacy species-level stack. |
| `ShellPlacementDao` | `ShellDaos.kt` | `shell_placement` | insert/remove/observe room/get slot/instance | placements by room | `ShellPlacementRepository` | Legacy/object placement risk. |
| `ShellFindUpgradeDao` | `ShellDaos.kt` | `shell_find_upgrade` | insert, get for instance | none | `CreatureGrowthRepository` | Growth/upgrade history. |
| `UserBadgeDao` | `ShellDaos.kt` | `user_badge` | upsert/get/observe/mark viewed | earned badges | `BadgeRepository` | Countable badges. |
| `UserDiscoveryDao` | `ShellDaos.kt` | `user_discovery` | insert/observe/get/count/mark viewed | discoveries | legacy `DiscoveryRepository` | Do not port unless confirmed. |
| `StillwaterLedgerDao` | `ShellDaos.kt` | `stillwater_ledger` | insert, observe total/lifetime/recent, source count | totals/recent | `StillwaterRepository` | Ledger-backed. |
| `StillwaterPreferenceDao` | `ShellDaos.kt` | `stillwater_preference` | upsert/get/observe singleton | preference | `StillwaterPreferenceRepository` | Room preference. |
| `UserShellRoomStateDao` | `ShellDaos.kt` | `user_shell_room_state` | upsert/get/observe all | room states | `ShellRoomStateRepository` | Room memory. |
| `ShellRewardEventDao` | `ShellDaos.kt` | `shell_reward_event` | insert all, get events for Arc/session | none | `ShellRewardEventRepository` | Read model/notification events. |
| `ObjectiveDao` | `dao/shell/ObjectiveDao.kt` | `objectives` | observe/list/get/insert/archive/update streak/reset | active objectives | `LookoutObjectiveRepository` | Lookout. |
| `ObjectiveCompletionDao` | `ObjectiveDao.kt` | `objective_completions` | observe/list/get/insert/mark claimed | completions | `LookoutCompletionRepository` | Claim flag atomic with ledger in repo. |
| `ObjectiveSkippedCycleDao` | `ObjectiveDao.kt` | `objective_skipped_cycles` | observe/list/insert | skipped cycles | `LookoutSkipRepository` | Unique period skip. |

## 8. Repository Inventory

| Repository | File path | Dependencies | Responsibilities / APIs | Product feature | iOS equivalent | Test priority |
|---|---|---|---|---|---|---|
| `FlowRepository` | `data/repository/FlowRepository.kt` | `SessionDao`, `TagDao`, `PulseDao` | Add completed sessions, update description/Arc fields, delete and cleanup unused Journey tags, query sessions/Arc sessions. | Flow/Story/Voyage/Lookout | `FlowSessionRepository` | High |
| `AliveFlowRepository` | `data/repository/AliveFlowRepository.kt` | `OngoingSessionDao` | Save/observe/clear active Flow singleton. | Ongoing Flow restoration | `OngoingFlowRepository` | High |
| `JourneyRepository` | `data/repository/JourneyRepository.kt` | `TagDao` | Observe Journeys, get-or-create by trimmed name. | Journey selection/filtering | `JourneyRepository` | High |
| `PulseRepository` | `data/repository/PulseRepository.kt` | `PulseDao`, `SessionDao`, `TagDao` | CRUD Pulses, attach/detach live Pulses, update details, cleanup unused Journey tags. | Pulse/Story/Idea Grove | `PulseRepository` | High |
| `FlowPlanRepository` | `data/repository/FlowPlanRepository.kt` | `FlowPlanDao` | CRUD planned Flows, normalize target/surge, pin/archive/mark launched. | Paths | `PlannedFlowRepository` | Medium |
| `ArcPlanRepository` | `data/repository/ArcPlanRepository.kt` | `ArcPlanDao` | CRUD Arc plans/steps, studio/archive, launch, replace steps. | Paths/Arcs | `ArcPlanRepository` | Medium/High if Arc MVP |
| `ActiveArcRunRepository` | `data/repository/ActiveArcRunRepository.kt` | `ActiveArcRunDao` | Start/update/clear active Arc run singleton. | Arc continuation | `ActiveArcRunRepository` | Medium/High |
| `NotepadRepository` | `data/repository/NotepadRepository.kt` | `DataStore<Preferences>` | Persist HTML text and document font; default welcome HTML. | Notepad | `NotepadRepository` | Medium |
| `HealthSettingsRepository` | `data/repository/health/HealthSettingsRepository.kt` | `DataStore<Preferences>` | Observe/set movement bonus enabled. | Health/Movement | `MovementSettingsRepository` | High if Movement MVP |
| `HealthPermissionRepository` | `data/repository/health/HealthPermissionRepository.kt` | Health Connect provider | Check Health Connect availability and read steps permission. | Health permission | `HealthPermissionRepository`/HealthKit service | High if Movement MVP |
| `FlowHealthRepository` | `data/repository/health/FlowHealthRepository.kt` | DB, `FlowHealthDao`, `SessionDao`, `PearlLedgerDao` | Snapshot/breakdown CRUD, refreshable queries, expire/disable, delayed movement update transaction updating session/reward/pearls. | Movement rewards | `MovementRewardRepository` | Very high |
| `ShellRepository` | `data/repository/shell/ShellRepository.kt` | DB, Session DAO, Shell DAOs, objective completion DAO | Pearl balance/ledger, Stillwater, creatures/finds, stacks, placements, badges, discoveries, room state, drawing Stillwater, growth/release/Beyond Blue, notification viewed state. | Shell/The Blue/Chest/Badges/Stillwater | `ShellRepository` plus split `CreatureRepository`, `PearlLedgerRepository` | Very high |
| `IdeaGroveRepository` | `data/repository/shell/IdeaGroveRepository.kt` | DB, IdeaGrove/Pulse/Session/Tag DAOs/repos | Build Idea Grove items, status changes, delete/revive, repair links, launch contexts, link completed Flow. | Idea Grove | `IdeaGroveRepository` | Medium/High |
| `LookoutRepository` | `data/repository/shell/LookoutRepository.kt` | DB, objective DAOs, Pearl ledger, badge DAO | Observe/insert/archive/skip objectives, apply completion grant, claim Pearls transactionally, update badges/streaks. | The Lookout | `LookoutRepository` | Medium/High |

## 9. Flow Session Persistence

Completed Flows are stored in `SessionEntity` / `sessions` through `FlowRepository.addSession` and `SessionDao.insertSession`. Important persisted fields are title, description, Journey `tagId`, `startTime`, `endTime`, `durationMs`, `surgePlannedMs`, `surgePoints`, `scyraPoints`, `isSoftMode`, Arc fields (`arcId`, `arcIndex`, `arcMultiplierUsed`, `arcBonusPoints`), and `createdAt`.

Regular Flow vs Soft Flow:

- Regular Flow uses `isSoftMode = false` and is eligible for Scyra points, Pearls, Movement Points, and regular creature rewards when the relevant policies allow it.
- Soft Flow uses `isSoftMode = true`; product direction says it should not award Scyra Score, but it may create Stillwater drops where Android supports that.

Lifecycle:

- Created on Flow completion.
- Description can be updated later with `SessionDao.updateSessionDescription`, supporting post-completion details edits.
- Deleted through `FlowRepository.deleteSessionAndCleanupTag` / `deleteSession`; related health snapshots/reward breakdowns and Pulse links cascade where FKs exist.
- Story/Chronicle reads sessions with Pulses, health snapshots, and reward breakdowns.
- Voyage Hall and Lookout stats should derive from sessions rather than duplicate persisted stat rows unless later needed.

Future iOS:

- Domain model: `FlowSession`.
- Persistent model: `FlowSessionRecord` with strongly typed `FlowKind.regular/soft`, optional Arc context, and timestamps.
- Preserve the ability to complete a Flow and fill/edit details later.
- TODO: verify exact deletion behavior for Shell reward events when a session is deleted; `shell_reward_event` has no FK in entity code.

## 10. Ongoing Flow / Active Session Persistence

`OngoingSessionEntity` is a singleton table (`id = 1`) persisted by `AliveFlowRepository`.

Persisted state includes:

- Identity/title: `flowInstanceId`, `title`, `description`, `tagName`.
- Mode: `isInFlowMode`, `isRunning`, `isSoftMode`.
- Timer restoration: nullable `baseStartTimeMs`, `accumulatedBeforeStartMs`, nullable `activeIntervalJson`.
- Surge: `isSurgeOn`, nullable `surgePlannedMs`, `surgeMilestonesFiredCsv`, `surgeTargetReached`, nullable reached time, final-countdown flag.
- Arc launch: nullable `arcId`, `arcChainBase`, `arcSessionCountInArc`, `arcLastSessionEndTimeMs`.
- Pulse origin: nullable `originPulseId`, `originPulseTitleSnapshot`, `originPulseJourneyNameSnapshot`.
- Health at start: `healthEnabledAtStart`, `healthPermissionGrantedAtStart`, `movementBonusEligibleAtStart`.

Lifecycle:

- Upserted while a Flow is active/running/paused.
- Cleared on completion/discard.
- Used with foreground notification/service behavior to restore after process death or notification deep link.

Future iOS:

- iOS must not rely on continuously running background timers.
- Persist active intervals and base timestamps so elapsed active time can be reconstructed after background/kill/relaunch.
- Use explicit interval objects instead of only encoded strings if using SwiftData/SQLite.

## 11. Journey / Tag Persistence

Android persists user-facing Journeys as `TagEntity` in table `tags`. `JourneyRepository.getOrCreateTagId` trims a name, reuses an existing tag by name, or inserts a new row. Sessions, Pulses, Flow plans, and Arc step snapshots reference tag IDs or tag names. Lookout objectives store `journeyId` plus `journeyNameSnapshot` without a declared Room FK.

Journey filters in Story/Voyage use the persisted `tags` table and related session/pulse rows. No persisted Journey color was observed; Journey color memory appears runtime/UI-derived. TODO: verify color assignment in UI mappers.

Future iOS:

- User-facing/domain name: `Journey`.
- Persistent model: `JourneyRecord`.
- Do not expose “tag” unless product direction changes.
- Preserve cleanup behavior only if deleting unused Journeys is still desired; otherwise consider explicit archive/rename UX later.

## 12. Pulse Persistence

`PulseEntity` stores thoughts/ideas in `pulses` with title, description, optional Journey `tagId`, optional `parentSessionId`, optional `parentFlowInstanceId`, optional `arcId`, timestamps, and Idea Grove status (`ALIVE`, `INSIGHT`, `COMPLETED`).

Behavior observed:

- Created with `PulseRepository.addPulse`.
- Edited with `updatePulse`, `updatePulseDetails`, and Idea Grove status updates.
- Deleted with cleanup of unused Journey tags.
- Can attach to active Flow via `parentFlowInstanceId` and later to a completed `SessionEntity`.
- Read by Story/Chronicle and Idea Grove.

Product rule:

- Pulses are thoughts/ideas, not Flows.
- Pulse creation should not directly award Scyra Score or Pearls.
- Reward-like fields such as `FlowRewardBreakdownEntity.pulseBonusPoints` are current implementation fields requiring Task 1.7 verification; do not infer product-rewarded Pulses here.

Future iOS:

- Domain model: `Pulse`.
- Persistent model: `PulseRecord` with optional Journey, Flow origin/link fields, Arc context, and `IdeaGroveStatus` enum.

## 13. Pulse-to-Flow Link Persistence

`PulseFlowLinkEntity` stores `pulseId`, `sessionId`, and `linkedAt` in `pulse_flow_links`. It has cascading FKs to Pulses and Sessions, an index on `pulseId`, and a unique index on `sessionId`.

Implications:

- A Pulse can have multiple links by `pulseId`.
- A completed session appears limited to one Pulse link because `sessionId` is unique.
- `IdeaGroveRepository.linkCompletedFlowToPulse` creates links after completed Flow sessions.
- Story/Flow details use Pulse links to show associated ideas.

Future iOS:

- Domain model: `PulseFlowLink`.
- Confirm cardinality before implementing UI that allows multiple Pulses per Flow. TODO: verify.

## 14. Planned Flow Persistence

`FlowPlanEntity` stores planned Flow cards in Paths. Fields include title, optional Journey `tagId`, `isSoftMode`, nullable `targetMinutes`, `launchWithSurge`, `pinned`, `archived`, `launchCount`, nullable `lastLaunchedAt`, `createdAt`, and `updatedAt`.

Behavior:

- `FlowPlanRepository.createFlowPlan` normalizes target minutes and surge launch options.
- Plans can be pinned, archived, launched, edited, or deleted.
- Arc steps may reference a source Flow plan and snapshot plan fields.

Future iOS:

- Domain model: `PlannedFlow`.
- Persistence model: `PlannedFlowRecord`.
- Preserve launch payload fields for direct Flow route creation.

## 15. Arc Persistence

Arc persistence uses three Room entities plus DataStore `ArcPrefs`:

- `ArcPlanEntity`: title, studio/archive flags, launch count/time, recurrence type/days, timestamps.
- `ArcPlanStepEntity`: ordered step rows with source Flow plan link, title/Journey/soft/target/surge snapshots, and link state.
- `ActiveArcRunEntity`: singleton active run with current step snapshot, step index/count, current Journey and soft mode, timestamps.
- `ArcPrefs`: DataStore keys for active/recent Arc runtime multiplier/progress/count/last end state.

Behavior:

- Arc plans and steps are created/edited in Paths.
- Steps preserve snapshots so plan edits do not necessarily rewrite historical launch context.
- Active run tracks continuation/progress.
- Reward multiplier relationship exists through session/reward fields but exact math belongs in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

Future iOS:

- Domain models: `ArcPlan`, `ArcStep`, `ActiveArcRun`, `ArcRuntimeSnapshot`.
- TODO: choose one canonical source for active/recent Arc runtime state; Android uses both Room and DataStore.

## 16. Reward Breakdown Persistence

`FlowRewardBreakdownEntity` stores reward explanation rows per `sessionId`:

- Base/non-movement fields: `nonMovementPreMultiplierPoints`, `pulseBonusPoints`, `surgeBonusPoints`, `otherPreMultiplierBonusPoints`.
- Movement: `movementPoints`.
- Totals/multipliers: `preMultiplierTotal`, `arcMultiplier`, `streakMultiplier`, `otherMultiplier`, `arcBonusPoints`, `finalScyraPoints`.
- Pearls: `pearlsEarned`, `pearlEligible`.
- Rounding: `roundingMode = KOTLIN_ROUND_TO_INT_COMPAT`.

Relationships:

- Primary key/FK is `sessionId` with cascade from `sessions`.
- `FlowHealthRepository.upsertCompletion` writes snapshot and breakdown transactionally.
- Delayed Health refresh can update reward points and breakdown, then insert a Pearl delta ledger entry.

Important:

- `pulseBonusPoints` is a persisted current implementation field; Task 1.7 must verify whether this remains product-valid.
- Do not infer that creating a Pulse is rewarded.

Future iOS:

- Domain/persistence model: `RewardBreakdown` / `RewardBreakdownRecord`.
- Store enough fields to reproduce history/reward reveal without recalculating differently after rule changes.

## 17. Health / Movement Snapshot Persistence

`FlowHealthSnapshotEntity` stores Health Connect movement state per completed Flow:

- Health flags: `healthEnabledAtStart`, `permissionGrantedAtStart`.
- Status: `FlowHealthSyncStatus` (`NOT_ENABLED`, `NOT_ELIGIBLE`, `PENDING`, `NO_REWARD`, `CAPTURED`, `EXPIRED`, `PERMISSION_REVOKED`, `DISABLED_BEFORE_CAPTURE`, `ERROR_RETRYABLE`, `ERROR_FINAL`).
- Movement data: nullable `steps`, `rawMovementPoints`, final movement Scyra/Pearl contributions.
- Refresh metadata: first/last checked, captured, expires, check count, `updatedAfterSync`.
- Flow interval: start/end timestamps and nullable `activeIntervalJson`.
- Source: `sourceLabel`, default “Health Connect”.

Current Android movement formula:

- `MovementBonusCalculator.calculateMovementPoints(steps)` returns `steps / 100`, using `STEPS_PER_POINT = 100`.
- Eligibility requires movement bonus enabled, Health Connect available, read steps permission granted, regular point-eligible Flow, and not Soft Flow.
- Delayed recalculation uses the max of existing movement points and newly calculated points, then updates session reward fields and Pearl delta if higher.

Future iOS:

- Use HealthKit instead of Health Connect.
- Query steps across persisted active Flow intervals.
- Preserve delayed refresh states and duplicate-award prevention.
- Product decision on movement ratio belongs in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

## 18. Pearl Ledger Persistence

`PearlLedgerEntity` stores every Pearl delta with String `id`, `delta`, `reason`, `sourceType`, optional `sourceId`, `createdAt`, and optional `note`. `PearlLedgerDao.observeBalance()` computes balance as `SUM(delta)`.

Sources:

- Flow completion / reward carrying.
- Delayed movement reward delta.
- Lookout objective claims.
- Creature release rewards.
- Creature growth/Beyond Blue spending.
- Stillwater/Beyond Blue related transactions where applicable.

Consistency:

- Duplicate-award prevention uses source/reason checks (`sourceRewardCount`) and stable movement delta reasons.
- Ledger insertion is inside transactions for spending/claim/delayed reward paths.

Future iOS:

- Pearls must be ledger-backed.
- Balance should be derived, not stored as a mutable single source of truth.

## 19. Creature / The Blue / Chest Persistence

Android creature persistence is currently named through ShellFind concepts:

- `UserShellFindInstanceEntity`: individual creature/find row with `instanceId`, `findId`, acquisition/source fields, upgrade stage, optional custom name, `isNew`, `isArchivedInChest`, `viewedAt`, `animalLevel`, `creatureStatus`, `creatureSource`, and `flowTimeValueMinutes`.
- `UserShellFindStackEntity`: species-level stack summary by `findId`; not level-aware.
- `ShellFindUpgradeEntity`: growth/upgrade event with instance, from/to stage, Pearl cost, timestamp.
- Catalog source: `ShellContentCatalog`, `CreatureCatalog`, `CreatureEconomy`, and Stillwater catalog are code-backed, not Room-backed.
- Status values: `ACTIVE`, `RELEASED`, `USED_BEYOND_BLUE`.

Behavior:

- Creatures are granted from regular Flow rewards, Stillwater draws, and Beyond Blue encounters.
- Growth uses Pearls and `animalLevel`; max level is 99.
- Release changes creature status and writes Pearl ledger payout; released creatures should not show in The Chest.
- Beyond Blue can consume/use creatures and Pearls to encounter new creatures.
- Chest grouping should be by species and level using instance rows, not only `user_shell_find_stack`.

Product rules for iOS:

- The Chest contains creatures only.
- Do not show released creatures.
- Group by species and level: 3 level 3 Minnows are separate from 4 level 1 Minnows.
- Count appears top right and level appears bottom in UI; persistence must support species+level grouping.
- Product-clean iOS names should use `CreatureInstance`, `CreatureStatus`, and `CreatureGrowthEvent`, not ShellFind.

## 20. Badges and Creature Mastery Persistence

`UserBadgeEntity` persists `badgeId`, `count`, `firstEarnedAt`, `lastEarnedAt`, `isNew`, and nullable `viewedAt`. `UserBadgeDao` can upsert/get/observe/mark seen/viewed. `ShellRepository.incrementBadge` and `LookoutRepository` badge paths update badge counts.

Current Android evidence:

- Countable badges are supported by `count`.
- Objective badges are recorded through Lookout completion grant fields and badge DAO updates.
- Creature economy defines max level 99 and `CreatureMasteryTier.MASTERED` at level >= 99.
- Tests verify `CreatureEconomy.MAX_CREATURE_LEVEL == 99` and mastery tier behavior.

Desired Creature Mastery product behavior:

- Creature max level is 99.
- A creature reaching level 99 awards a species Mastery badge.
- Mastery badges are countable per individual creature.
- Two Level 99 Minnows = Minnow Mastery count 2.
- Mastery persists after release/trade.
- Count is lifetime individual creatures that reached Level 99.

Gap:

- A generic badge table can store countable Mastery, but the inspected persistence does not show a dedicated per-creature mastery-awarded flag on `UserShellFindInstanceEntity`. TODO: verify award idempotency and avoid double-awarding the same creature in Task 1.7.

Future iOS:

- Consider a per-creature `masteryAwardedAt` or dedicated mastery ledger to guarantee lifetime/idempotent awards.

## 21. Stillwater Persistence

Stillwater uses:

- `StillwaterLedgerEntity`: ledger of `units`, source type/id, and timestamp.
- `StillwaterPreferenceEntity`: singleton room preference with `perspective` and `updatedAt`.
- `ShellRepository.addStillwater` and `drawFromStillwater` manage units and draw creatures from Stillwater catalog.

Behavior:

- Soft Flow/rest/leisure can add Stillwater units where Android reward policy supports it.
- Draw cost/vessel behavior is catalog-driven (`StillwaterVessel`, `StillwaterCatalog`) and transactionally consumes ledger units by inserting negative units.
- Creature rewards from draws create creature instances.

Future iOS:

- Model Stillwater as ledger-backed.
- Do not award Scyra Score for Soft Flow, but preserve Stillwater drops if included in MVP.

## 22. Idea Grove Persistence

Idea Grove is Pulse-derived:

- `PulseEntity.groveStatus` stores `ALIVE`, `INSIGHT`, or `COMPLETED`.
- `IdeaGroveDao` updates status and manages `pulse_flow_links`.
- `IdeaGroveRepository` maps Pulses, Journeys, sessions, and links into UI items; it can mark Insight/Completed, revive, delete, repair completed Pulses without Flow links, and provide launch context.

State:

- Sorting/expansion appears UI/runtime-derived. TODO: verify no hidden persistence for expanded rows.
- Flow launch from Pulse uses Pulse title/Journey as launch context.

Product rule:

- Idea Grove is Pulse-derived.
- No direct Pulse reward.

Future iOS:

- Store Idea Grove status on `PulseRecord` and links in `PulseFlowLinkRecord`.

## 23. Lookout Persistence

Lookout uses:

- `ObjectiveEntity`: objective definition with Journey ID/name, period type, objective type, target duration, period start, optional weekly boundary day, streaks, completion counts, archive flag, timestamps.
- `ObjectiveCompletionEntity`: period completion with achieved/target duration, base/final reward Pearls, streak multiplier, badge key/label, claim flags/time.
- `ObjectiveSkippedCycleEntity`: skipped objective period windows.

Behavior:

- Progress is calculated from sessions and objective windows.
- Completion grant inserts completion and updates recurring stats/streaks.
- Claiming Pearls marks completion claimed and inserts a Pearl ledger entry transactionally.
- Badges can be granted through `badgeKey` and `badgeLabelSnapshot`.

Future iOS:

- Domain models: `LookoutObjective`, `LookoutCompletion`, `LookoutSkippedCycle`.
- Persist objective definitions/completions/skips; compute progress from Flow sessions at read time.

## 24. Voyage Hall Persistence

No dedicated Voyage Hall Room entity was observed. Voyage Hall stats appear computed from sessions, Journey/tag data, score/duration fields, Arc fields, and likely Health/reward breakdowns where shown.

Future iOS:

- Prefer derived `VoyageStats` from `FlowSessionRecord`, `JourneyRecord`, reward breakdowns, and movement snapshots.
- Do not persist aggregate stats unless performance requires caching.
- TODO: verify local-day boundary/filter state is runtime-only.

## 25. Shell Room State / Notifications Persistence

Shell room state:

- `UserShellRoomStateEntity` stores room ID, first/last opened timestamps, visual maturity score, ambient life score, and last changed timestamp.
- `ShellRepository.markRoomOpened` upserts this state.

Notifications/read state:

- Creature/find instances, stacks, badges, and discoveries include `isNew` and `viewedAt` fields.
- `ShellRewardEventEntity` records reward events by source session/Arc and reward type.
- `ShellRepository.markNotificationViewed`, `markAllNotificationsViewed`, and `markTheBlueAnimalsSeen` update viewed/new flags.

Legacy discovery behavior:

- `UserDiscoveryEntity` and `DISCOVERY_RECORDED` events support Discovery Journal-like notifications.
- Desired iOS direction should not port Discovery Journal unless product confirms or data compatibility requires it.

## 26. Notepad Persistence

`NotepadRepository` stores notepad state in `DataStore<Preferences>`:

- `notepad_text`: String, defaults to `DEFAULT_WELCOME_HTML` if absent.
- `notepad_doc_font`: Int, defaults to `0`; values are clamped to `0..2` where comments indicate `0 default`, `1 cursive`, `2 mono`.
- The text is HTML used by the rich editor/notepad UI.

Future iOS:

- Store a notepad document in settings/UserDefaults only if small enough; otherwise use a file/blob behind `NotepadRepository`.
- Preserve font preference as enum.
- TODO: decide how Android HTML maps to iOS rich text.

## 27. User Settings and DataStore Map

| Key/name | Source file | DataStore name | Default | Type | Product meaning | Affects | iOS storage recommendation | Priority | Notes/TODOs |
|---|---|---|---|---|---|---|---|---|---|
| `show_score_ui` | `UserPrefs.kt` | `user_prefs` | `BuildConfig.SHOW_SCORE` | Boolean | Score visibility / Scyra vs Aera behavior | UI/reward display | `UserSettings.showScore` | MVP | Scyra default true, Aera false by flavor. |
| `calm_mode` | `UserPrefs.kt` | `user_prefs` | `false` | Boolean | Calm mode | UI/behavior | `UserSettings.calmMode` | Phase 2/TODO | Verify exact UI effect. |
| `app_language_tag` | `UserPrefs.kt` | `user_prefs` | absent/null | String? | App language override | language/localization | `UserSettings.languageTag` | Phase 2 | iOS localization strategy needed. |
| `movement_bonus_enabled` | `HealthSettingsRepository.kt` | `skillz_prefs` | `false` | Boolean | Enable Movement Points | movement/rewards/Health | `MovementSettings.isEnabled` | MVP if Movement included | Requires HealthKit permission separately. |
| `notepad_text` | `NotepadRepository.kt` | `skillz_prefs` | `DEFAULT_WELCOME_HTML` | String | Notepad document HTML | Notepad UI | `NotepadDocument.storage` | Phase 2/MVP if Notepad included | HTML mapping risk. |
| `notepad_doc_font` | `NotepadRepository.kt` | `skillz_prefs` | `0` | Int | Notepad font selection | Notepad UI | `NotepadFontPreference` | Phase 2 | Clamp 0..2. |
| `arc_id` | `ArcPrefs.kt` | `skillz_prefs` | absent | Long | Active Arc runtime ID | Arc/rewards/navigation | `ArcRuntimeState.activeArcId` | Phase 2/MVP if Arcs | Consider Room-only unification. |
| `arc_pending` | `ArcPrefs.kt` | `skillz_prefs` | `true` on load | Boolean | Active Arc pending flag | Arc runtime | `ArcRuntimeState.isPending` | Phase 2 | TODO verify semantics. |
| `arc_mult` | `ArcPrefs.kt` | `skillz_prefs` | `ArcRules.START_MULTIPLIER` | Double | Active Arc multiplier | rewards | `ArcRuntimeState.multiplier` | Phase 2 | Reward spec needed. |
| `arc_progress` | `ArcPrefs.kt` | `skillz_prefs` | read but load returns `0L` | Long | Historical/progress placeholder | Arc runtime | maybe omit | TODO | Code notes strict arcs no carry/banking. |
| `arc_last_end` | `ArcPrefs.kt` | `skillz_prefs` | `0L` | Long | Active Arc last session end | Arc grace window | `ArcRuntimeState.lastEndAt` | Phase 2 | TODO verify with Arc rules. |
| `arc_count` | `ArcPrefs.kt` | `skillz_prefs` | `0` | Int | Session count in Arc | Arc progress/reward | `ArcRuntimeState.sessionCount` | Phase 2 |  |
| `recent_arc_id` | `ArcPrefs.kt` | `skillz_prefs` | absent | Long | Recently ended Arc ID | Arc continuation | `RecentArcSnapshot.arcId` | Phase 2 |  |
| `recent_arc_pending` | `ArcPrefs.kt` | `skillz_prefs` | `true` | Boolean | Recent Arc pending | Arc continuation | `RecentArcSnapshot.isPending` | Phase 2 |  |
| `recent_arc_mult` | `ArcPrefs.kt` | `skillz_prefs` | start multiplier | Double | Recent Arc multiplier | reward/continuation | `RecentArcSnapshot.multiplier` | Phase 2 |  |
| `recent_arc_progress` | `ArcPrefs.kt` | `skillz_prefs` | read but load returns `0L` | Long | Recent progress placeholder | Arc runtime | maybe omit | TODO |  |
| `recent_arc_last_end` | `ArcPrefs.kt` | `skillz_prefs` | completed at fallback | Long | Recent Arc last end | Arc grace | `RecentArcSnapshot.lastEndAt` | Phase 2 |  |
| `recent_arc_count` | `ArcPrefs.kt` | `skillz_prefs` | `0` | Int | Recent Arc session count | Arc summary | `RecentArcSnapshot.sessionCount` | Phase 2 |  |
| `recent_arc_completed_at` | `ArcPrefs.kt` | `skillz_prefs` | absent | Long | Recent Arc completion timestamp | Arc restoration | `RecentArcSnapshot.completedAt` | Phase 2 |  |

## 28. Runtime-Only / Derived State

| State | Android source | Persisted or runtime-only | iOS recommendation |
|---|---|---|---|
| ViewModel UI state | ViewModels under `android/app/src/main/java/.../viewmodel` | Runtime | Keep in SwiftUI Observable state; persist only domain state. |
| Dialog/sheet visibility | Compose screen local state | Runtime | Local `@State`/sheet enum. |
| Selected Shell destination | `ShellRootScreen`, `ShellDestination` | Runtime | Shell route enum; optionally scene restoration only. |
| Story filters/date lens | Story UI/ViewModel | Mostly runtime TODO verify | Persist only if product requests. |
| Selected Journey filters | `TagFilterRow`, Story/Voyage state | Runtime unless TODO verified | Runtime filter state. |
| Reward reveal UI model | `RewardRevealMapperTest`, reward UI files | Derived from session/reward/shell events | Recompute from persisted reward records/events. |
| Journey color memory | UI mappers | TODO: likely runtime | Avoid separate table unless product requires stable colors. |
| Voyage stats | Voyage calculators/tests | Derived | Compute from sessions/rewards. |
| The Blue UI models | The Blue mapper/catalog + creature instances | Derived | Derived from creature records/catalog. |
| Chest stacks | `ShellChestInventoryMapperTest`, `ShellFindInstanceDao` | Derived from active instances by species+level | Do not persist level-aware stacks unless performance requires. |
| Badge groups | Badge screen/mapper | Derived from `UserBadgeEntity` and catalog | Derived. |
| Focus Room player state | Focus Room UI/TTS | Runtime | Do not persist as domain data unless resuming exercises later. |
| Surge runtime events | `OngoingSessionEntity` stores key flags; haptic runtime is runtime-only | Mixed | Persist timer milestones needed for restoration; keep haptics runtime. |
| Notification runtime state | Android notification/service + viewed flags | Mixed | Persist source viewed flags; generate notification UI state. |

## 29. Migrations and Schema Evolution

- Current Room version: 31.
- Latest schema file: `android/app/schemas/com.kingkharnivore.skillz.data.model.SkillzDatabase/31.json`.
- Migration file: `SkillzDatabaseMigrations.kt`.
- Migration test file: `SkillzMigrationTest.kt`.

Historical areas visible in migrations:

- Versions 1-12 migrate directly to v15 via a safe rebuild strategy.
- v13-v15 add Shell tables and Shell reward events.
- v16 normalizes The Blue room identifiers.
- v17 adds creature economy fields.
- v18 no-op compatibility.
- v19-v21 add/normalize objectives and claims.
- v22-v23 add Idea Grove and Pulse `ALIVE` default schema metadata.
- v24-v26 reference Anchor-only cleanup/normalization and target branch schema hardening.
- v27-v30 add/normalize Movement Bonus tables.
- v31 adds notification `viewedAt` columns.

High-risk migration areas:

- Legacy pre-Shell schemas.
- ShellFind/creature economy changes.
- Objective claim fields and Pearl claim state.
- Movement Bonus snapshots/reward breakdowns.
- Notification viewed/new flags.

Future iOS:

- iOS does not import Android Room schemas at build/runtime.
- Future Android-to-iOS data import/sync would need a separate mapping/export task.

## 30. Transaction and Consistency Requirements

| Operation | Android files involved | Tables touched | Risks | iOS transaction requirement |
|---|---|---|---|---|
| Completing Flow | Flow ViewModel/use cases, `FlowRepository`, `FlowHealthRepository`, `ShellRepository` | sessions, ongoing_session, reward breakdown, health snapshot, ledgers/events/creatures | Partial completion or duplicate rewards | Single unit of work or idempotent staged transaction. |
| Saving reward breakdown | `FlowHealthRepository`, `FlowHealthDao` | flow_reward_breakdowns | Reward reveal mismatch | Write with session completion. |
| Applying delayed movement reward | `FlowHealthRepository.applyDelayedMovementUpdateTransactionally` | sessions, health snapshots, reward breakdowns, pearl_ledger | Duplicate Pearl deltas; lower recalculation | Transaction + stable id/reason. |
| Awarding Pearls | `ShellRepository.addPearls`, `LookoutRepository`, `FlowHealthRepository` | pearl_ledger | Double award | Ledger idempotency. |
| Writing Pearl ledger entries | Shell/Lookout/Health repos | pearl_ledger | Balance drift | Balance derived from ledger. |
| Granting creatures | `ShellRepository.grantFindCopy/grantFindOnce/drawFromStillwater` | user_shell_find_instance, stacks, events | Missing instance or notification | Transaction with source IDs. |
| Releasing creatures | `ShellRepository.releaseCreature(s)` | user_shell_find_instance, pearl_ledger, placements maybe | Released creature still shown; duplicate payout | Status + payout in one transaction. |
| Leveling creatures | `ShellRepository.growCreature` | instance, upgrade, pearl ledger | Spend without level change; double Mastery | Transaction + mastery idempotency. |
| Awarding badges | `ShellRepository.incrementBadge`, `LookoutRepository` | user_badge | Lost count updates | Transaction/upsert by badge ID. |
| Claiming Lookout rewards | `LookoutRepository.claimObjectivePearls` | objective_completions, pearl_ledger | Double claim | Conditional update + ledger insert. |
| Stillwater draws | `ShellRepository.drawFromStillwater` | stillwater_ledger, creature instance/stack | Negative balance/race | Transaction and balance check. |
| Beyond Blue trades | `ShellRepository.encounterBeyondBlue` | instances, pearl ledger, creature grant | Consuming wrong creatures; overpay | Transaction with selected IDs/status. |
| Deleting sessions/pulses | `FlowRepository`, `PulseRepository` | sessions/pulses/tags/links/cascades | Orphan reward events | Verify cascade/manual cleanup. |
| Linking/unlinking pulses | `IdeaGroveRepository`, `PulseRepository` | pulses, pulse_flow_links | Broken Story links | Transaction where linking after Flow completion. |
| Archiving/deleting plans/arcs | Plan repos/DAOs | flow_plans, arc_plans, arc_plan_steps | Step orphan/history mismatch | Use FK cascade and explicit archive flags. |

## 31. Legacy / Do Not Port Without Product Confirmation

| Persistence concept | Android evidence | Why questionable | iOS decision | TODOs |
|---|---|---|---|---|
| Discovery Journal / discoveries | `UserDiscoveryEntity`, `UserDiscoveryDao`, `DISCOVERY_RECORDED` event | Product direction flags discoveries as legacy/questionable. | Do not port unless confirmed or data compatibility requires. | Verify current UI entry points. |
| Trinkets | `ShellRewardEventTypes.TRINKET_GRANTED` | The Chest should be creatures only. | Do not port to first iOS. | Verify if any catalog still grants trinkets. |
| Room objects | `OBJECT_GRANTED`, placements, `invitePearlObject` | Object placement may conflict with creature-only Chest direction. | Defer/confirm. | Separate from creature model if retained. |
| ShellFind umbrella naming | `UserShellFindInstanceEntity`, `ShellFind*Dao`, `ShellFindUpgradeEntity` | Product terminology should be Creature for iOS. | Map to Creature models; do not expose name. | Keep data compatibility note. |
| Shell placements | `ShellPlacementEntity` | May represent legacy object placement. | Phase 2 only if product confirms. | Verify Focus display needs. |
| Coral | Shell/creature catalog TODO | Legacy economy concept mentioned by product. | Do not port unless present and confirmed. | Search catalog in follow-up. |
| Plants | Shell/creature catalog TODO | Legacy economy concept mentioned by product. | Do not port unless present and confirmed. | Search catalog in follow-up. |
| Old Shell Chest naming | `SHELL_CHEST_ROUTE`, `ShellChestScreen` | Product name should be The Chest. | Use “The Chest” in iOS UI. | Internal route names can differ. |
| Object placement | `placeInstance`, `invitePearlObject` | May not fit desired creature-only inventory. | Defer. | Product decision needed. |
| Pulse reward interpretation fields | `FlowRewardBreakdownEntity.pulseBonusPoints` | Product says Pulse creation is not rewarded. | Do not create Pulse rewards unless Task 1.7 confirms. | Reward spec. |
| Beam-related persistence | none observed | Requested as possible legacy. | None to port. | TODO verify broader search if needed. |
| WorkManager persistence | no Room entity observed | Background work may be runtime-only. | None to port unless worker state appears. | TODO verify WorkManager workers. |

## 32. iOS Persistence Recommendations

- Use a repository layer regardless of SwiftData vs SQLite.
- Consider model grouping: Flow, Pulse, Journey, Planning/Arc, Rewards, Health, Economy, Shell/Creatures, Lookout, Settings, Notepad.
- Provide in-memory repository implementations for reward tests, Flow lifecycle tests, Chest grouping tests, movement tests, and badge mastery tests.
- Define transaction boundaries for Flow completion, reward application, Pearl ledger writes, creature grant/growth/release, Lookout claims, Stillwater draws, and Beyond Blue trades.
- ID strategy: use UUID/String IDs for ledger/events/creatures where Android uses String IDs; preserve Long-like local auto IDs for user-created records only if useful.
- Timestamp strategy: store epoch milliseconds or `Date` consistently; convert at repository boundaries.
- Enum strategy: raw-value enums for status fields (`CreatureStatus`, `FlowHealthSyncStatus`, `GroveStatus`, objective period/type, recurrence type).
- Value objects: active intervals, reward breakdown, creature level distribution, Arc runtime snapshot.
- Ledger strategy: Pearls and Stillwater should be append-only ledgers; derived balances.
- Migration strategy: start simple but version from first release; avoid UI depending on storage backend.
- Settings storage: UserDefaults/AppStorage behind `SettingsRepository`; do not scatter keys through views.
- HealthKit snapshot strategy: persist permission/eligibility at Flow start and active intervals; refresh delayed results idempotently.
- Notepad storage: UserDefaults for small HTML text initially, file/blob if it grows.
- Future cloud sync readiness: keep stable IDs, source IDs, timestamps, and idempotency keys.

SwiftData can be used if sufficient, but repositories should prevent UI from depending directly on SwiftData. SQLite may be preferred if Room-like control, composite constraints, explicit migrations, and transaction control are more important. Do not choose final implementation here; make the final decision in a later `docs/06_IOS_ARCHITECTURE_DECISIONS.md` or equivalent.

## 33. Suggested iOS Folder Mapping

Future recommendation only; do not create these folders or code now.

```text
ios/Scyra/
  Domain/
    Models/
    Rewards/
    Economy/
  Persistence/
    Models/
    Repositories/
    Migrations/
  Services/
    Health/
    Notifications/
    Settings/
  Features/
    Flow/
    Pulse/
    Story/
    Shell/
```

Additional possible folders:

- `Persistence/Stores/InMemory/` for tests.
- `Persistence/Transactions/` for explicit unit-of-work helpers if SQLite is used.
- `Domain/Lookout/`, `Domain/Arc/`, `Domain/Stillwater/`, and `Domain/Creatures/` if the domain grows.

## 34. Persistence Risks and Open Questions

| Risk/open question | Why it matters | iOS impact | Recommended follow-up |
|---|---|---|---|
| Room version 31 complexity | Many historical migrations and features are encoded. | iOS model could miss edge cases. | Dedicated schema/import audit. |
| SwiftData vs SQLite | Composite constraints, ledgers, and transactions may need precise control. | Wrong backend could complicate parity. | iOS architecture decision doc. |
| Transaction parity | Rewards/economy operations touch many tables. | Duplicate/lost rewards. | Transaction test plan. |
| Reward duplicate-award risk | Pearl/creature/badge events need idempotency. | Economy inflation or user loss. | Reward/economy spec and tests. |
| Delayed movement reward updates | Health data may arrive after Flow completion. | UI/history/rewards can change later. | HealthKit delayed reward design. |
| Active Flow restoration | Android has foreground service; iOS background differs. | Timer drift/lost sessions. | Timestamp restoration prototype. |
| Android movement ratio vs desired ratio | Android uses 100 steps per point; product may want another ratio. | Reward mismatch. | `docs/06_REWARD_AND_ECONOMY_SPEC.md`. |
| Legacy ShellFind/object/discovery data | Persistence mixes creatures, objects, discoveries. | iOS may port unwanted features. | Legacy data compatibility decision. |
| Chest creature-only direction vs legacy persistence | Android stack/object tables can include non-creature concepts. | Chest UI pollution. | Chest data-filter spec/tests. |
| Creature Mastery badge award gap | Badge table can count, but per-creature award idempotency is unclear. | Double/missed Mastery awards. | Mastery persistence design. |
| Pulse reward field interpretation | `pulseBonusPoints` exists in breakdown. | Product violation if Pulses rewarded incorrectly. | Task 1.7 reward verification. |
| Aera/Scyra settings | Score visibility defaults differ by flavor. | iOS scope creep. | Product decision: Scyra-only first. |
| Localization/settings persistence | Android stores language override. | iOS localization strategy differs. | Localization settings spec. |
| Future cloud sync schema | Local-only assumptions may not sync well. | Migration/import challenges. | Cloud sync readiness review. |
| Android-to-iOS import | Users may want Android data migrated later. | Requires mapping and export/import. | Separate import/export task, not now. |

## 35. Acceptance Criteria for This Document

- This task only creates or changes `docs/05_DATA_MODEL_MAP.md`.
- Android source code is untouched.
- No iOS source code or project is created.
- No database migrations are created.
- No schema files are copied.
- No Gradle/build files are changed.
- The document is based on actual Android database/entity/DAO/repository/DataStore files plus prior docs.
- Every Room entity is documented or marked `TODO: verify`.
- Every DataStore/preference key discovered is documented or marked `TODO: verify`.
- Core product concepts are mapped to future iOS domain and persistence models.
- Runtime-only state is separated from persisted state.
- Legacy persistence concepts are clearly separated from desired iOS behavior.
- Transaction/consistency requirements are documented.
- iOS recommendations preserve repo boundary rules.
- Unclear behavior is marked `TODO: verify`.

## 36. Codex Summary

- Docs and Android persistence files inspected: prior docs `00` through `04`, `android/app/build.gradle.kts`, Room schema `31.json`, `SkillzDatabase.kt`, `SkillzDatabaseMigrations.kt`, entity packages, DAO packages, repositories, DataStore wrappers, DI modules, and persistence/economy tests.
- Room database/version/schema findings: Android uses `SkillzDatabase`, database name `skillz_db`, Room version 31, `exportSchema = true`, schema JSON under `android/app/schemas`, and migrations attached through `SkillzDatabaseMigrations.ALL_MIGRATIONS`.
- Entities mapped: 25 Room entities covering Journeys/tags, sessions, Pulses, Pulse links, ongoing Flow, plans, Arcs, Health snapshots, reward breakdowns, Pearl ledger, Shell finds/creatures/stacks/placements/upgrades, badges, discoveries, Stillwater, Shell rooms/events, objectives, completions, and skipped cycles.
- DAOs and repositories mapped: core Flow/Journey/Pulse/planning/Arc DAOs and repositories, Health DAOs/repositories, Shell DAOs/repository, Idea Grove, Lookout, and Notepad.
- DataStore/preferences mapped: `user_prefs` keys for score/calm/language, `skillz_prefs` keys for movement bonus, notepad text/font, and Arc active/recent runtime state.
- High-risk persistence areas: transaction parity, delayed movement rewards, active Flow restoration, reward duplicate prevention, legacy ShellFind/object/discovery data, The Chest creature-only grouping, Creature Mastery idempotency, and Pulse reward field interpretation.
- Legacy persistence concepts identified: Discovery Journal/discoveries, Trinkets/object reward event types, room object placement, ShellFind naming, species-only stacks, old Shell Chest naming, Pulse reward fields, and possible legacy Coral/Plants concepts pending verification.
- Future iOS persistence recommendations: repository-first architecture over SwiftData/SQLite, in-memory test stores, explicit transactions, ledger-backed economy, HealthKit snapshot records, typed enums/value objects, settings repository, and no dependency on Android schemas at build/runtime.
- Anything outside `docs/05_DATA_MODEL_MAP.md` changed: no.
- Repo boundary rules preserved: yes. The document is reference-only under `docs/`; no Android source/build files were modified, no schema files were copied, and no iOS files or folders were created.
