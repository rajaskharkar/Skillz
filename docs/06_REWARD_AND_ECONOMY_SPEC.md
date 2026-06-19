# Reward and Economy Specification

## 1. Purpose

This document defines Scyra's reward and economy system for future iOS parity. It covers Scyra Score, base Flow score, timed bonuses, Soft Flow behavior, Surge bonuses, Arc multipliers, Movement Points, delayed Health refresh, Pearls, Pearl ledger behavior, reward breakdown persistence, reward reveal mapping, Shell rewards, creature drops, creature growth, creature release, Beyond Blue, Stillwater, Lookout objective rewards, badges, Creature Mastery, duplicate-award prevention, and legacy concepts that should not be ported unless confirmed.

This is documentation only. It does not change Android source, create iOS code, create tests, create migrations, copy schema files, change Gradle/build files, or add dependencies.

## 2. Source Material Inspected

Prior docs inspected:

- `docs/00_REPO_BOUNDARIES.md`
- `docs/01_ANDROID_ARCHITECTURE.md`
- `docs/02_SCYRA_PRODUCT_SPEC.md`
- `docs/03_NAVIGATION_AND_SCREEN_MAP.md`
- `docs/04_DESIGN_SYSTEM.md`
- `docs/05_DATA_MODEL_MAP.md`

Android reward/economy code inspected:

- Core score/reward state: `android/app/src/main/java/com/kingkharnivore/skillz/utils/score/ScoreCalculator.kt`, `ScoreBreakdown.kt`, `model/state/flow/FlowRewardUiModel.kt`, `RewardRevealCardUiModel.kt`, `viewmodel/FlowViewModel.kt`
- Reward reveal: `ui/screen/flow/reward/RewardRevealMapper.kt`, `RewardRevealDeck.kt`, `SessionRewardContent.kt`, `SoftSessionRewardContent.kt`, `ArcSummaryContent.kt`
- Movement/Health: `utils/health/MovementBonusCalculator.kt`, `HealthRefreshUseCase.kt`, `MovementStepAggregator.kt`, `data/health/HealthConnectMovementDataSource.kt`, `data/repository/health/FlowHealthRepository.kt`, `HealthSettingsRepository.kt`, `data/model/entity/health/FlowHealthSnapshotEntity.kt`, `FlowRewardBreakdownEntity.kt`
- Arc/Surge: `utils/arc/ArcPrefs.kt`, `utils/arc/ArcRules.kt`, `data/model/entity/ActiveArcRunEntity.kt`, `data/repository/ActiveArcRunRepository.kt`, `ui/service/SurgeRuntime.kt`, `ui/service/SurgeHapticsManager.kt`, Arc-related sections of `FlowViewModel.kt`
- Shell/Pearls/creatures: `utils/shell/ShellRewardOrchestrator.kt`, `ShellRewardEventRecorder.kt`, `CreatureEconomy.kt`, `Stillwater.kt`, `data/repository/shell/ShellRepository.kt`, `data/model/entity/shell/ShellEntities.kt`, `ShellRewardEventEntity.kt`
- Lookout/objectives: `utils/shell/lookout/ObjectiveProgressCalculator.kt`, `data/repository/shell/LookoutRepository.kt`, `data/model/entity/shell/ObjectiveEntities.kt`
- Persistence/schema context: `docs/05_DATA_MODEL_MAP.md`, Room entity files, `android/app/schemas/com.kingkharnivore.skillz.data.model.SkillzDatabase/31.json`

Tests inspected:

- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/flow/reward/RewardRevealMapperTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/health/MovementBonusCalculatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/health/DelayedMovementRewardPolicyTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/health/FlowActiveIntervalTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/CreatureEconomyTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardEventRecorderTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardEventAggregatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/StillwaterCatalogTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/lookout/ObjectiveProgressCalculatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellChestInventoryMapperTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellNotificationInlayMapperTest.kt`

Expected path notes:

- No dedicated badge/mastery award service was found. Creature mastery currently appears as economy tier logic and test coverage, not a persisted award workflow. TODO: verify if another feature path awards Mastery badges.
- `MovementBonusCalculator.kt` source uses `STEPS_PER_POINT = 100`. Final product/iOS parity decision: 100 steps = 1 Movement Point; older 25-step expectations/tests/notes are stale.

## 3. Reward System Overview

### Regular Flow completion

1. User completes a Flow in `FlowViewModel.saveWithArcBehavior`.
2. `ScoreCalculator.breakdownFromDuration(realDurationMs)` calculates minute-based base/timed score.
3. Soft mode is false, so `baseScyra = breakdown.totalPoints`.
4. Surge points are calculated if a Surge plan exists; Android currently stores/displays Surge points separately but does not add them into `baseScyra` in the inspected `FlowViewModel` completion path. iOS should match this current Android behavior for parity until Android changes.
5. Movement eligibility is checked from Health settings/permission captured at Flow start. If eligible, steps are read across active intervals and Movement Points are calculated.
6. Pre-Arc points are `baseScyra + movementPoints`.
7. If an Arc is active, `ScoreCalculator.arcMath` applies the Arc multiplier to the pre-Arc total.
8. `SessionEntity` is inserted with final `scyraPoints`.
9. `FlowRewardBreakdownEntity` and `FlowHealthSnapshotEntity` are persisted if Health was enabled/eligible.
10. Pulses attached to the live `flowInstanceId` are attached to the saved session; an origin Pulse may receive a `PulseFlowLinkEntity` best-effort.
11. `ShellRewardOrchestrator.onSessionCompleted` writes Pearls, creatures, badges, Stillwater, and Shell reward events depending on regular vs Soft Flow.
12. `FlowRewardUiModel` is built in memory and mapped by `RewardRevealMapper` to reward cards.
13. Ongoing Flow state is cleared and Arc state may continue, complete, or produce an Arc summary.

### Soft Flow completion

1. Score breakdown may still be computed for minutes, but `baseScyra = 0`.
2. Surge is disabled/zeroed.
3. Movement eligibility excludes Soft Flow.
4. Any active Arc is cleared before completion.
5. `SessionEntity` is saved with `isSoftMode = true` and zero Scyra points.
6. `ShellRewardOrchestrator` converts Soft Flow duration seconds to Stillwater drops and writes `stillwater_ledger` if not previously awarded for that session.
7. Reward reveal uses `buildSoftRewardCards` with Stillwater, not Scyra/Pearls.

### Arc Flow completion

- First Flow when the user chooses “continue Arc” can create a new Arc with first session `arcIndex = 1`, `arcMultiplierUsed = 1.0`, and `arcBonusPoints = 0`.
- Subsequent active Arc sessions use `ScoreCalculator.arcMath(beforeArcPoints, chainBase, durationMs)`, persist `arcMultiplierUsed`, `arcBonusPoints`, `arcIndex`, and update `ArcPrefs`.
- Arc summary cards aggregate prior sessions and Shell reward events.

### Movement / delayed Health refresh

- Immediate completion reads Health Connect steps for active intervals if eligible.
- If no data or later data changes, `HealthRefreshUseCase` refreshes snapshots for up to 72 hours, throttled by 30 minutes and max 10 checks.
- Delayed refresh never lowers rewards; it uses max(existing movement points, recalculated movement points), updates session/reward rows, and writes Pearl deltas only when positive and not already written for the same stable reason.

### Shell / Lookout / Voyage

- Shell rewards run from session completion: Pearls, regular Flow creatures, Flow duration badges, Stillwater drops for Soft Flow, and Shell reward event rows.
- Lookout progress is computed from sessions by Journey/window and creates claimable completions; Pearls are inserted only on explicit claim.
- Voyage Hall appears computed from sessions/reward data; no dedicated persisted Voyage reward row was found.

## 4. Regular Flow Score Formula

Current Android score source is `ScoreCalculator.breakdownFromDuration(durationMs)`.

| Rule | Android source | Formula/value | Applies to | iOS parity requirement |
| ---- | -------------- | ------------- | ---------- | ---------------------- |
| Duration minutes | `ScoreCalculator.kt` | `minutes = (durationMs / 60_000L).coerceAtLeast(0).toInt()` | All score breakdowns | Use whole elapsed minutes, floor division. |
| Base points | `ScoreCalculator.kt` | `basePoints = minutes` | Regular Flow | 1 base point per full minute. |
| 10-minute bonus count | `ScoreCalculator.kt` | `tenMinuteBonuses = (minutes / 10) - (minutes / 30)` | Regular Flow | Count 10-minute chunks not also counted as 30-minute chunks. |
| 30-minute bonus count | `ScoreCalculator.kt` | `thirtyMinuteBonuses = (minutes / 30) - (minutes / 60)` | Regular Flow | Count 30-minute chunks not also counted as 60-minute chunks. |
| 60-minute bonus count | `ScoreCalculator.kt` | `sixtyMinuteBonuses = minutes / 60` | Regular Flow | Count every 60-minute chunk. |
| Bonus values | `ScoreCalculator.kt` | `10m * 5`, `30m * 15`, `60m * 50` | Regular Flow | Match exact values. |
| Total score | `ScoreCalculator.kt` | `base + tenCount*5 + thirtyCount*15 + sixtyCount*50` | Regular Flow before Movement/Arc | Persist/display as base Scyra before Movement/Arc. |
| Minimum duration | `FlowViewModel.isZeroDuration` path | Zero duration cannot be saved; exact helper body TODO verify. | Completion save | iOS should prevent saving zero-duration Flow. |
| Soft Flow zero score | `FlowViewModel` | `baseScyra = if (isSoft) 0 else breakdown.totalPoints` | Soft Flow | Soft Flow gets no Scyra Score. |
| Rounding | score base uses integer minutes; Arc/Surge use rounding separately | Base score has no fractional rounding beyond floor minutes. | Regular Flow | Preserve floor minutes. |
| Caps | none observed for base score | No cap found. | Regular Flow | No cap unless later product decision. |
| Stored result | `SessionEntity.scyraPoints`, `FlowRewardBreakdownEntity.finalScyraPoints` | Final after Movement/Arc, not just base. | History/rewards | Store both base/breakdown and final. |

Examples from formula:

- 5 minutes: base 5 + bonuses 0 = **5 Scyra Points**.
- 10 minutes: base 10 + one 10-minute bonus 5 = **15 Scyra Points**.
- 30 minutes: base 30 + two 10-minute bonuses 10 + one 30-minute bonus 15 = **55 Scyra Points**.
- 60 minutes: base 60 + four 10-minute bonuses 20 + one 30-minute bonus 15 + one 60-minute bonus 50 = **145 Scyra Points**.

## 5. Timed Bonuses

Timed bonuses are mutually tiered by chunk, not independently cumulative for the same time segment:

| Threshold/chunk | Count formula | Bonus per counted chunk | Example |
|---|---|---:|---|
| 10-minute chunks | `(minutes / 10) - (minutes / 30)` | `+5` | 30 minutes has 2 counted 10-minute bonuses, because one 30-minute chunk is excluded. |
| 30-minute chunks | `(minutes / 30) - (minutes / 60)` | `+15` | 60 minutes has 1 counted 30-minute bonus, because one 60-minute chunk is excluded. |
| 60-minute chunks | `minutes / 60` | `+50` | 120 minutes has 2 counted 60-minute bonuses. |

Answers:

- Are there 10-, 30-, and 60-minute bonuses? **Yes.**
- Values: **+5**, **+15**, **+50** per counted chunk.
- Are bonuses cumulative or mutually exclusive? They are cumulative across the duration, but the count formulas make 30-minute and 60-minute chunks replace some lower-tier chunk counts.
- Do they apply before multipliers? Yes. `breakdown.totalPoints` is the pre-Movement/pre-Arc base Scyra value.
- Are Soft Flows excluded? Yes, `baseScyra = 0` for Soft Flow even though minutes are still tracked.
- Are they persisted separately? Counts are not persisted in Room except via final points/reward breakdown; they are carried in `FlowRewardUiModel` for reward reveal.

## 6. Soft Flow Reward Rules

| Question | Current Android behavior | Source / notes | iOS parity requirement |
|---|---|---|---|
| Does Soft Flow award Scyra Score? | No. `baseScyra = 0`; saved `scyraPoints` is zero unless TODO bug elsewhere. | `FlowViewModel` | No Score. |
| Does Soft Flow award Pearls? | No regular Pearls. `ShellRewardOrchestrator` does not call `addPearls` for Soft Flow. | `ShellRewardOrchestrator` | No Pearls. |
| Does Soft Flow award regular creature drops? | No. Regular creature path is only non-Soft; `CreatureEconomy.creaturesForRegularFlowMinutes(..., isSoftFlow = true)` returns empty. | `CreatureEconomyTest` | No regular The Blue creature drops. |
| Does Soft Flow progress Arc? | No. `FlowViewModel` clears active Arc if state is Soft Flow. | `FlowViewModel` | Soft Flow should not progress Arc. |
| Does Soft Flow allow Surge? | No. Setting Soft Mode disables Surge and clears `surgePlannedMs`; completion sets `surgePoints = 0`. | `FlowViewModel` | No Surge. |
| Does Soft Flow earn Movement Points? | No. Eligibility excludes `isSoftFlow`. | `MovementBonusEligibilityPolicy` | No Movement Points. |
| Does Soft Flow earn Stillwater drops? | Yes. Drops = duration seconds, written to `stillwater_ledger` once per session/source. | `calculateDropsForSoftFlow`, `ShellRewardOrchestrator`, `ShellRepository.addStillwater` | Preserve if Stillwater in iOS scope. |
| Reward reveal | `buildSoftRewardCards` returns Stillwater card and no Score/Pearl/Blue animal card. | `RewardRevealMapperTest` | Custom Stillwater reward reveal. |

Product direction matches Android here: Soft Flow is calm/rest/leisure-oriented and should not be treated like a normal scored Flow.

## 7. Surge Reward Rules

Surge is a target-duration focus mode with haptic milestones and a separate bonus calculation.

### Current Android formula

`ScoreCalculator.surgePoints(surgePlannedMs, actualDurationMs)`:

1. If `surgePlannedMs == null`, return 0.
2. `plannedMinutes = plannedMs / 60_000.0`, coerced at least 1.0.
3. `actualMinutes = actualDurationMs / 60_000.0`, coerced at least 1.0.
4. `error = abs(actualMinutes - plannedMinutes) / plannedMinutes`.
5. `maxBonus = 0.35`, `sharpness = 5.0`.
6. `multiplier = 1.0 + (0.35 * exp(-5.0 * error))`.
7. `raw = roundToInt(actualMinutes * multiplier)`.
8. If `actualMinutes <= plannedMinutes`, return `raw`; otherwise return `raw.coerceAtMost(plannedMinutes.toInt())`.

### Behavior answers

- Is Surge bonus stored in `SessionEntity.surgePoints`? **Yes**, `FlowViewModel` passes `surgePoints` to `FlowRepository.addSession` and `SessionEntity` stores it.
- Is Surge stored in `FlowRewardBreakdownEntity.surgeBonusPoints`? **No in inspected completion code**: `saveMovementSnapshotAndBreakdown` sets `surgeBonusPoints = 0L`. This conflicts with the model’s field and `MovementRewardRecalculator` support. For iOS parity, match Android and do not add Surge into persisted reward breakdown unless Android changes.
- Does Surge affect final Scyra Points? **Current inspected `FlowViewModel` does not add `surgePoints` into `baseScyra`, `beforeArc`, or `finalScyra`**. Reward reveal can display `reward.surgePoints`, but final score appears not to include it. For iOS parity, treat this as current Android behavior and match it.
- Does Surge affect Pearls? Since Pearls follow `session.scyraPoints`, current inspected behavior suggests **not unless Surge is included in final Scyra elsewhere**. For iOS parity, match current Android behavior.
- Does Surge affect creature drops? No direct evidence; creature drops use Flow duration minutes only.
- Does Surge apply to Soft Flow? No.
- Does Surge interact with Arc multipliers? Intended fields imply it could be pre-multiplier, but current inspected completion passes only `baseScyra + movementPoints` into Arc math. TODO: verify intended Surge/Arc relationship.
- Haptic milestones are runtime-only in `SurgeRuntimeEvaluator`: midpoint, 5/2/1 minutes left, 30/10 seconds left, countdown ticks 5..1, target reached.

## 8. Arc Multiplier Rules

### Current Android source

- Constants in `ArcRules.kt`: `START_MULTIPLIER = 1.3`, `STEP = 0.1`, `PROGRESS_STEP_MS = 10 minutes`, `GRACE_WINDOW_MS = 5 minutes`, pause budgets 2/5/10 minutes depending on Arc session count.
- Math in `ScoreCalculator.arcMath`.
- Runtime state in `ArcPrefs` (`skillz_prefs`) and `ArcRuntimeState`.
- Planned/active run records in Room: `ActiveArcRunEntity`; `FlowViewModel` mainly uses `ArcPrefs` runtime state for reward math.

### Formula

`ScoreCalculator.arcMath(beforeArcPoints, chainBase, durationMs, stepMs = 10m, step = 0.1)`:

1. `tierExtra = arcTierExtra(durationMs)`:
   - `<10m`: `0.0`
   - `10m..<20m`: `0.0`
   - `20m..<40m`: `0.1`
   - `40m..<60m`: `0.2`
   - `60m..<90m`: `0.3`
   - `>=90m`: `0.4`
2. `arcMultiplierUsed = chainBase + tierExtra`.
3. `boosted = roundToInt(beforeArcPoints * arcMultiplierUsed)`.
4. `arcBonusPoints = max(boosted - beforeArcPoints, 0)`.
5. `finalPoints = beforeArcPoints + arcBonusPoints`.
6. `didLevelUp = durationMs >= 10m`.
7. `nextChainBase = if (didLevelUp) chainBase + 0.1 else chainBase`.

### Behavior answers

- First session that starts an Arc is saved with `arcMultiplierUsed = 1.0`, `arcBonusPoints = 0`, `finalScyraPoints = beforeArc`; then `ArcPrefs` stores `multiplier = 1.3` for the next Flow.
- Existing Arc sessions use the saved `ArcPrefs.multiplier` as `chainBase`.
- Multiplier applies to `beforeArc = baseScyra + movementPoints`; it therefore applies to Movement Points.
- Because Surge is not currently included in `beforeArc`, current inspected behavior says Arc does not multiply Surge points. iOS should match this behavior for parity until Android changes.
- Pearls are based on final `session.scyraPoints`, so Arc multipliers affect Pearls for eligible regular Flows.
- `arcBonusPoints` is persisted in `SessionEntity` and `FlowRewardBreakdownEntity`.
- Room `ActiveArcRunEntity` tracks active planned Arc run progress; `ArcPrefs` appears canonical for current reward multiplier runtime in `FlowViewModel`. TODO: verify whether all Arc launch paths synchronize both.

## 9. Movement Points Rules

### Current Android behavior

- Health Connect source record: `StepsRecord.COUNT_TOTAL` via `HealthConnectMovementDataSource.readStepsBetween`.
- Active intervals are normalized by `FlowActiveIntervalNormalizer` and read by `MovementStepAggregator`.
- Eligibility requires: movement bonus enabled, Health Connect available, read steps permission granted, regular point-eligible Flow, and not Soft Flow.
- Current source formula: `MovementBonusCalculator.calculateMovementPoints(steps) = steps.coerceAtLeast(0) / STEPS_PER_POINT`, with `STEPS_PER_POINT = 100L`.
- Rounding: integer floor division.
- Movement Points are pre-multiplier: `beforeArc = baseScyra + movementPoints`, and delayed recalculation uses `MovementRewardRecalculator` before multipliers.
- Movement Points affect final Scyra Points and Pearls when `pearlEligible` is true.
- Soft Flow is excluded.

### Conflict / product decision

`MovementBonusCalculatorTest.kt` currently has a stale test name/expectation around 25-step behavior, but the inspected production source has `STEPS_PER_POINT = 100L` and product direction is now final. Therefore:

- Authoritative Android/product/iOS parity behavior: **steps / 100**.
- Final decision: **100 steps = 1 Movement Point**.
- Older 25-step expectations/tests/product notes are stale and should be updated in future Android/iOS test cleanup rather than copied into iOS.

### Delayed refresh

- Refresh window: 72 hours.
- Throttle: 30 minutes between checks.
- Max checks: 10.
- Refreshable statuses: `PENDING`, `NO_REWARD`, `CAPTURED`, `ERROR_RETRYABLE`.
- `DelayedMovementRewardPolicy` uses `max(existingMovementPoints, newlyCalculatedPoints)` to avoid lowering rewards.
- Positive Pearl deltas use stable reason `movement_bonus_delta_session_${sessionId}_movement_${movementPoints}_final_${finalScyraPoints}` and `PearlLedgerDao.sourceRewardCount("session", sourceId, reason)` to prevent duplicate Pearl deltas.

## 10. Reward Breakdown Persistence

`FlowRewardBreakdownEntity` fields:

- `sessionId`
- `nonMovementPreMultiplierPoints`
- `pulseBonusPoints`
- `surgeBonusPoints`
- `otherPreMultiplierBonusPoints`
- `movementPoints`
- `preMultiplierTotal`
- `arcMultiplier`
- `streakMultiplier`
- `otherMultiplier`
- `arcBonusPoints`
- `finalScyraPoints`
- `pearlsEarned`
- `pearlEligible`
- `roundingMode`

Current write path in `FlowViewModel.saveMovementSnapshotAndBreakdown`:

- Inserts/updates alongside `FlowHealthSnapshotEntity` via `FlowHealthDao.upsertCompletionSnapshotAndBreakdown`.
- `nonMovementPreMultiplierPoints = baseScyra`.
- `pulseBonusPoints = 0L`.
- `surgeBonusPoints = 0L` despite computed `surgePoints` elsewhere. For iOS parity, match current Android behavior.
- `movementPoints = movementRead.movementPoints`.
- `preMultiplierTotal = baseScyra + movementPoints`.
- `arcMultiplier = arcMultiplierUsed ?: 1.0`.
- `finalScyraPoints = finalScyra`.
- `pearlsEarned = finalScyra` for non-Soft Flow; else 0.
- `pearlEligible = !state.isSoftMode`.

Pulse answers:

- `pulseBonusPoints` is a persisted field and calculator parameter, but inspected completion code always writes `0L`.
- No inspected code shows Pulse creation directly awarding Score or Pearls.
- Treat `pulseBonusPoints` as reserved/legacy/unclear until Task 1.7 follow-up or product decision confirms otherwise.
- iOS should not award Pulse creation and should initially store `pulseBonusPoints = 0` if maintaining breakdown parity.

## 11. Pearl Economy

### How Pearls are earned

- Regular Flow completion: `ShellRewardOrchestrator` calls `shellRepository.addPearls(session.scyraPoints, "flow_reward", "session", session.id)`.
- Delayed Movement update: positive delta Pearl ledger row with stable movement reason.
- Lookout objective claim: `LookoutRepository.claimObjectivePearls` inserts positive Pearls with reason `objective_completion_claim`.
- Creature release: `ShellRepository.releaseActiveCreatures` inserts positive Pearls with reason `release_creature`.
- Beyond Blue overpay return: positive Pearls with reason `beyond_blue_overpay_return`.

### How Pearls are spent

- Creature growth: negative Pearls with reason `grow_creature`.
- Legacy object shaping: negative Pearls with reason `shape_find`.
- Beyond Blue encounter: negative Pearls with reason `beyond_blue_encounter`.

### Behavior answers

- Are Pearls equal to final Scyra Points for eligible regular Flows? **Yes in current Android**: Flow reward writes `session.scyraPoints`; reward breakdown writes `pearlsEarned = finalScyraPoints` when `pearlEligible`.
- Are Pearls awarded for Soft Flows? **No**.
- Are Pearls affected by Movement Points? **Yes**, Movement Points can increase final Scyra Points and delayed Pearl deltas.
- Are Pearls affected by Arc multipliers? **Yes**, Pearls follow final Scyra Points.
- Are Pearls affected by Surge bonuses? **Current inspected source suggests no**, because Surge is not added to final Scyra. TODO: verify intended behavior.
- Are balances derived from ledger only? **Yes**, `PearlLedgerDao.observeBalance/getBalance` uses `SUM(delta)`.
- Duplicate prevention: `sourceRewardCount(sourceType, sourceId, reason)` for Flow/Stillwater/delayed movement and claim flags for Lookout.

## 12. Shell Reward Orchestration

`ShellRewardOrchestrator.onSessionCompleted(session)` runs after `SessionEntity` insertion.

Regular Flow path:

- `sourceId = session.id.toString()`.
- `minutes = session.durationMs / 60_000L`.
- Calls `shellRepository.addPearls(session.scyraPoints, "flow_reward", "session", sourceId)`.
- If Pearls were already awarded for this session/reason, returns empty reward result and does not grant duplicate creatures/badges.
- Grants creature copies from `CreatureEconomy.creaturesForRegularFlowMinutes(minutes)`.
- Awards duration badges at `>=10`, `>=30`, `>=60`, `>=120` minutes: `badge_flow_10_min`, `badge_flow_30_min`, `badge_flow_60_min`, `badge_flow_120_min`.
- Records Shell reward events through `ShellRewardEventRecorder`.

Soft Flow path:

- Converts duration seconds to Stillwater drops using `calculateDropsForSoftFlow(durationSeconds)`.
- Calls `shellRepository.addStillwater(drops, "session", sourceId)`.
- If already added or zero, returns empty reward.
- Records `STILLWATER_ADDED` event.

Legacy references:

- `RewardRevealCardType` and `ShellRewardEventTypes` still include object, trinket, and discovery types.
- Reward reveal tests assert legacy non-creature rewards are hidden from user-facing cards while badges are grouped.
- iOS should not port trinkets/objects/discoveries unless product confirms or data compatibility requires it.

## 13. Creature Drop Rules

Creature drops are deterministic and duration-based for regular Flows:

`CreatureEconomy.creaturesForRegularFlowMinutes(minutes, isSoftFlow = false)`:

1. If Soft Flow or minutes < 10: no regular creature drops.
2. Greedy conversion:
   - 120-minute chunks -> `ShellContentCatalog.FOCUS_WHALE`
   - 60-minute chunks -> `ShellContentCatalog.FOCUS_MANTA`
   - 30-minute chunks -> `ShellContentCatalog.FOCUS_SEAHORSE`
   - 10-minute chunks -> `ShellContentCatalog.FOCUS_MINNOW`
3. Additional endurance whales: `minutes / 150` whales added.

Examples from `CreatureEconomyTest`:

- 9m: none.
- 10m: 1 Minnow.
- 29m: 2 Minnows.
- 30m: 1 Seahorse.
- 60m: 1 Manta.
- 80m: 1 Manta + 2 Minnows.
- 120m: 1 Whale.
- 150m: 2 Whales + 1 Seahorse.

Answers:

- Regular Flow creatures are Minnow, Seahorse, Manta, Whale via Shell content constants.
- Drops are deterministic by duration, not random.
- Stillwater creatures are separate and drawn randomly from Stillwater vessels.
- Beyond Blue creatures are separate catalog entries with requirements and hybrid creature/Pearl payment.
- Movement/Arc/Surge do not affect regular creature drops; duration minutes do.

## 14. Creature Growth / Leveling Economy

- Max level: `CreatureEconomy.MAX_CREATURE_LEVEL = 99`.
- Starting level: `UserShellFindInstanceEntity.animalLevel = 1` when granted.
- Growth allowed only for active animals.
- If current level >= 99, Android throws/rejects with “Mastered at Level 99.”
- Growth spends Pearls with reason `grow_creature` and updates `animalLevel` to current + 1.
- `ShellFindUpgradeEntity` is used for legacy object upgrades; animal growth currently does not insert a `ShellFindUpgradeEntity` in `growCreature`/`growCreatureByLevel`.

Growth cost formula:

- Base cost:
  - Minnow: 25
  - Seahorse: 75
  - Manta: 200
  - Whale: 600
  - Else: max(25, requirementMinutes or flowTimeValueMinutes or 25)
- For current level `L` coerced to 1..98:
  - `multiplier = 1.0 + (L * 0.12) + (L^1.45 * 0.018) + (ln(L) * 0.35)`
  - `cost = floor(base * multiplier)`, coerced to Int range and at least 1.
- `growthCostPearls(creature, 99)` is coerced to the level 98 cost because 99 is max.

Mastery badge answer:

- No inspected `growCreature` path increments a species Mastery badge when a creature reaches level 99.
- No inspected entity stores `masteryAwardedAt` per creature instance.
- iOS must add idempotency before implementing desired Mastery awards.

## 15. Creature Release Economy

Release behavior:

- Eligible only for active animals.
- Single release uses `releaseCreature(instanceId)`.
- Bulk release uses `releaseCreaturesByLevel(findId, selectionsByLevel)` and is level-aware.
- Release does **not** delete the creature row; it sets `creatureStatus = RELEASED` and `isArchivedInChest = 1` through DAO update.
- It removes placement with `placementDao.removeByInstance`.
- It writes positive Pearl ledger rows with reason `release_creature`, source type `shell_reward`, source ID = instance ID.

Release value formula:

- `base = canonicalPearlValue(creatureId) = flowTimeValueMinutes(creatureId) * PEARLS_PER_REQUIRED_FLOW_MINUTE`.
- `PEARLS_PER_REQUIRED_FLOW_MINUTE = 2`.
- `upgradeInvestment = cumulativeGrowthCostPearls(creatureId, level)`.
- `salvageRate`:
  - level >=99: 0.35
  - >=75: 0.30
  - >=50: 0.25
  - >=25: 0.20
  - >=10: 0.15
  - else: 0.10
- `normalValue = base + floor(upgradeInvestment * salvageRate)`.
- If source type is Stillwater, adjusted value = `round(normalValue * 0.25)`, at least 1.
- Return Int-coerced adjusted value.

Product requirement:

- Releasing a mastered creature must not remove Mastery. Android currently lacks explicit Mastery award persistence, so iOS should design a lifetime mastery ledger/flag.

## 16. Beyond Blue Economy

Beyond Blue is implemented in `ShellRepository.encounterBeyondBlue` and `CreatureEconomy.quoteBeyondBluePayment`.

- Target must have `CreatureSourceType.BEYOND_BLUE`.
- User may select active animal creature instances as payment.
- Selected creature contribution uses `beyondBlueTradeContributionMinutes`, currently level-independent and equal to flow time value/requirement minutes.
- Required remaining minutes are converted to Pearl cost: `remainingMinutes * PEARLS_PER_REQUIRED_FLOW_MINUTE` (2 Pearls/minute).
- Overpay returns Pearls: `overpayMinutes * PEARLS_PER_EXTRA_FLOW_MINUTE` (1 Pearl/minute).
- If insufficient Pearls for remaining requirement, encounter is rejected.
- Consumed creatures are marked `USED_BEYOND_BLUE` and removed from placements.
- New target creature is granted via `grantFindCopy(targetCreatureId, "beyond_blue", targetCreatureId)`.

Answers:

- Beyond Blue can use creatures, Pearls, or both.
- Consumed creatures are not deleted but are no longer active; they should not appear in The Chest active inventory.
- Beyond Blue creatures are part of CreatureCatalog/The Blue ecosystem but have `CreatureSourceType.BEYOND_BLUE`.

## 17. Stillwater Economy

Stillwater supports Soft Flow drops and random vessel draws.

Soft Flow drops:

- Formula: `calculateDropsForSoftFlow(durationSeconds) = durationSeconds.coerceAtLeast(0)`.
- Therefore 1 second = 1 Drop; 10 minutes = 600 Drops; 22 minutes = 1320 Drops.
- Note: an existing reward reveal test uses 220 drops for 22 minutes as a manually supplied UI model; source formula is seconds. Use source formula as current Android truth.

Ledger/draws:

- `StillwaterLedgerEntity` stores positive/negative units; total is sum of ledger units.
- Vessel costs:
  - `FISHBOWL = 15_000`
  - `AQUARIUM = 25_000`
  - `POND = 45_000`
  - `LAKE = 75_000`
- Draw validation requires unlocked zone and enough drops.
- Draw randomness: rarity roll `0..59 COMMON`, `60..89 UNCOMMON`, `90..97 RARE`, `98..99 MYTHIC`, then random creature from matching vessel+rarity pool.
- Draw consumes units transactionally by inserting negative ledger row and grants a Stillwater creature instance.
- Stillwater creatures are cataloged separately with IDs prefixed `stillwater_` and `CreatureSourceType.STILLWATER`.
- Stillwater does not award Scyra Score or Pearls directly.

## 18. Lookout Objective Rewards

Lookout objective rewards are calculated by `ObjectiveProgressCalculator` and persisted/claimed through `LookoutRepository`.

Rules:

- Objective periods: daily, weekly, monthly.
- Objective kinds: one-time, recurring.
- Progress uses non-Soft Flow sessions in the Journey and objective window.
- Base reward Pearls = `floor(achievedMs / 60_000.0).toInt().coerceAtLeast(1)`.
- Recurring streak multiplier = `1.0 + streakBefore * 0.1`; one-time multiplier = `1.0`.
- Final reward Pearls = `floor(base * multiplier)`.
- Badge key = `objective_badge_${journeyId}_${period}`; label snapshot = `"{Journey} {Period} Objective"`.
- Completion grant inserts `ObjectiveCompletionEntity` with `pearlsGranted=false`, `pearlsClaimed=false`, increments badge, and updates recurring stats in a transaction.
- Pearls are awarded only on explicit claim via `claimObjectivePearls`; duplicate claim is prevented by `pearlsClaimed` check and conditional update.
- Lookout rewards do not modify Scyra Score in inspected code.

## 19. Badges

Badge persistence:

- `UserBadgeEntity`: `badgeId`, `count`, `firstEarnedAt`, `lastEarnedAt`, `isNew`, `viewedAt`.
- `ShellRepository.incrementBadge` increments count/upserts and marks new/unviewed.
- `LookoutRepository.incrementBadgeInTransaction` increments objective badge counts.
- Badges are marked viewed through `UserBadgeDao.markViewed/markAllViewed` and Shell notification viewed flows.

Flow duration badges awarded by Shell reward orchestration:

- `badge_flow_10_min` at minutes >= 10.
- `badge_flow_30_min` at minutes >= 30.
- `badge_flow_60_min` at minutes >= 60.
- `badge_flow_120_min` at minutes >= 120.

Answers:

- Badges are countable (`count`).
- Objective badges are countable.
- Reward reveal groups badge cards; tests assert legacy non-creature rewards are hidden while badges are grouped.
- TODO: verify full badge catalog names and any non-Flow/non-objective badge awards.

## 20. Creature Mastery

Desired product rules:

- Creature max level is 99.
- A creature reaching Level 99 awards a species Mastery badge, e.g. “Minnow Mastery.”
- Mastery badges are countable per individual creature reaching Level 99.
- Two Level 99 Minnows = Minnow Mastery count 2.
- Mastery persists after release/trade.
- Count represents lifetime individual creatures that reached Level 99.

Current Android evidence:

- `CreatureEconomy.MAX_CREATURE_LEVEL = 99`.
- `CreatureEconomy.creatureMasteryTier(99) = MASTERED`.
- `CreatureEconomyTest.maxLevelAndMasteryTitlesUseLevel99Cap` verifies max level and tiers.
- `ShellRepository.growCreature` and `growCreatureByLevel` prevent growth past 99 but do not award a species Mastery badge in inspected code.
- `UserShellFindInstanceEntity` has no `masteryAwardedAt` field.
- `ShellFindUpgradeEntity` records object upgrade events but animal growth paths do not insert it.

Answers:

- Does Android currently award countable species-specific Mastery badges at Level 99? **Not in inspected code.**
- Gap: need idempotent per-creature Mastery award logic and persistence.
- iOS should add either `masteryAwardedAt` on `CreatureInstanceRecord` or a separate `CreatureMasteryLedger(speciesId, instanceId, awardedAt)` with unique instance ID.
- Tests needed: first Level 99 awards count 1, same creature cannot double-award, second Level 99 same species increments to 2, release/trade does not decrement count.

## 21. Reward Reveal Mapping

`RewardRevealMapper` maps `FlowRewardUiModel` and Arc summaries to `RewardRevealCardUiModel` decks.

Regular Flow cards:

- First card is score/time depending on calm mode.
- Score body includes base Flow, total time bonuses, Surge if >0, Arc bonus if >0, Arc multiplier if present, and swipe hint.
- Subtitle says Pearls carried if `shellPearlsEarned > 0`.
- Shell reward cards include animals, badges, and Shell bridge/placeholder behavior.
- Tests assert Pearls are unified with Scyra Points rather than duplicated as a separate Pearl amount card.

Soft Flow cards:

- `buildSoftRewardCards` emits a Stillwater card: title, amount, Stillwater destination, and explanation.
- Tests assert no Scyra/Pearl/The Blue animal cards for Soft Flow.

Arc summary cards:

- `buildArcSummaryRewardCards` emits Arc score summary, grouped animals, grouped badges, Stillwater, and placeholders/bridge cards.
- Object/trinket/discovery aggregate cards are currently intentionally hidden by returning no cards for those legacy categories in tests.

Delayed Health:

- Initial reward reveal uses in-memory `FlowRewardUiModel` from completion.
- Later delayed Health refresh updates persisted session/reward/Pearl ledger; current immediate reward reveal will not retroactively animate unless UI observes updated data. iOS should persist breakdowns and show history updates clearly.

## 22. Duplicate-Award and Idempotency Rules

| Reward operation | Android idempotency mechanism | Tables/fields involved | iOS requirement | Gap/TODO |
| ---------------- | ----------------------------- | ---------------------- | --------------- | -------- |
| Session completion | UI saving flag and ongoing clear; not a single transaction for all side effects | sessions, ongoing_session, reward tables, Shell tables | Use completion transaction or idempotent command ID. | Android TODO notes transaction-safe use case needed. |
| Flow Pearls | `sourceRewardCount(sourceType, sourceId, reason)` before ledger insert | `pearl_ledger` | Unique source+reason idempotency. | DB lacks unique index for this exact triple. |
| Shell reward events | deterministic `eventId(sessionId,type,rewardId)` + DAO `INSERT IGNORE` | `shell_reward_event` | Deterministic event IDs. | Good pattern. |
| Delayed movement Pearl delta | stable reason includes session/movement/final; sourceRewardCount check | `pearl_ledger`, health tables, sessions | Stable id/reason for each positive delta. | Consider unique constraint. |
| Delayed movement lowering | `max(existingMovementPoints, newMovementPoints)` | reward breakdown/snapshot | Never reduce historical rewards from delayed sync. | Preserve. |
| Lookout completion grant | completion unique cycle check and insert ignore | `objective_completions` | Unique objective+period completion. | Preserve. |
| Lookout claim | `pearlsClaimed` check + conditional update | `objective_completions`, `pearl_ledger` | Atomic claim. | Preserve. |
| Stillwater add | `sourceCount(sourceType, sourceId)` | `stillwater_ledger` | One drop grant per session. | Preserve. |
| Stillwater draw | transaction balance check then negative ledger + creature grant | `stillwater_ledger`, creature instance | Transaction. | Preserve. |
| Beyond Blue | transaction validates active selected creatures and balance, marks status | creature instances, pearl ledger | Transaction and selected instance uniqueness. | Preserve. |
| Creature release | requires active status, then status update + ledger | creature instance, pearl ledger | Prevent release if not active. | Preserve. |
| Badge increments | Upsert count | `user_badge` | Atomic increment. | Consider conflict-safe increment. |
| Creature Mastery | None found for per-instance award | user_badge, creature instance | Add mastery idempotency. | Major gap. |

## 23. Legacy Reward Concepts / Do Not Port Without Confirmation

| Concept | Android evidence | Current behavior | Why questionable | iOS port decision | TODOs |
|---|---|---|---|---|---|
| Pulse rewards | `FlowRewardBreakdownEntity.pulseBonusPoints`; calculator accepts pulse bonus | Completion writes 0; no Pulse creation award found | Product says Pulses are thoughts/ideas, not rewarded sessions | Do not award Pulses | Verify no hidden code path. |
| Discovery Journal / discoveries | `UserDiscoveryEntity`, `DISCOVERY_RECORDED`, reveal mapper hooks | Can record discoveries; cards for discoveries are hidden/legacy in tests | Product marks discoveries legacy/questionable | Do not port unless confirmed/data import needed | Verify current UI. |
| Trinkets | `TRINKET_GRANTED`, `RewardRevealCardType.TRINKET`, catalog constants | Tests hide trinket reward cards | The Chest should be creature-only | Do not port first-pass | Verify catalog grants. |
| Room objects | `OBJECT_GRANTED`, `invitePearlObject`, placements | Object shaping/placement exists | May conflict with creature-only Chest | Defer/confirm | Product decision. |
| ShellFind naming | `UserShellFindInstanceEntity`, `ShellFind*Dao` | Umbrella model covers animals/objects/trinkets | Product should say Creature for iOS | Map to Creature models | Keep compatibility note. |
| Object placement | `ShellPlacementEntity`, `placeInstance` | Placement in rooms/slots | May be legacy or Phase 2 | Defer | Verify Focus display need. |
| Coral | Not confirmed in inspected reward files | TODO | Mentioned as legacy risk | Do not port unless found/confirmed | Search catalog in dedicated cleanup. |
| Plants | Not confirmed in inspected reward files | TODO | Mentioned as legacy risk | Do not port unless found/confirmed | Search catalog in dedicated cleanup. |
| Beam | No reward behavior found | None | Requested legacy concern | Nothing to port | TODO broader search if needed. |

## 24. Current Android Behavior vs Desired Product Decisions

| Area | Current Android behavior | Desired/current product direction | iOS recommendation | Needs decision? |
| ---- | ------------------------ | --------------------------------- | ------------------ | --------------- |
| Movement Points ratio | Production source uses `steps / 100`; older 25-step expectations are stale | Final product direction is 100 steps = 1 Movement Point | Implement Android production parity exactly: 100 steps per point | No |
| Aera/Scyra score | Flavor/default settings can hide score for Aera | iOS should start Scyra-first unless Aera requested | Implement Scyra score-visible first | Yes for Aera |
| Pulse bonus field | `pulseBonusPoints` exists but completion writes 0 | Pulses should not be rewarded sessions | Treat as reserved/legacy and keep 0 | Verify |
| Surge | Surge formula exists and UI displays points, but inspected final score/Pearl path does not add it | Product direction is Android parity during iOS port | Match current Android exactly; do not fix/reinterpret until Android changes | No |
| Creature Mastery | Economy tier at 99 exists; no award/idempotency found | Countable species Mastery per Level 99 creature | Add explicit design later | Yes |
| ShellFind/object/discovery | Android includes umbrella/legacy concepts | Chest creature-only; avoid legacy | Map only creatures for iOS Scyra parity unless product confirms legacy data compatibility | Yes |
| Stillwater drops | Source formula is 1 drop per second; some UI tests use manually supplied values | Soft Flow should grant Stillwater if in scope | Use source formula unless product changes | Maybe |

## 25. Worked Examples

| Example | Inputs | Intermediate values | Final Scyra / Pearls / Movement | Creature/badge/Stillwater outcome | Persisted rows/UI |
|---|---|---|---|---|---|
| 5-minute regular Flow | duration 5m, no Arc/Movement/Surge | base=5, bonuses=0 | final=5, Pearls=5 | no creature, no duration badge | session, Pearl ledger, reward card score/shell bridge |
| 10-minute regular Flow | duration 10m | base=10, 10m bonus=5 | final=15, Pearls=15 | 1 Minnow, `badge_flow_10_min` | session, creature instance, badge, events, animal/badge cards |
| 30-minute regular Flow | duration 30m | base=30, 2×10m=10, 1×30m=15 | final=55, Pearls=55 | 1 Seahorse, badges 10/30 | session, ledger, creature, badges |
| 60-minute regular Flow | duration 60m | base=60, 4×10m=20, 1×30m=15, 1×60m=50 | final=145, Pearls=145 | 1 Manta, badges 10/30/60 | reward deck with score/animal/badges |
| Regular Flow with Surge | planned 25m, actual 25m | surge formula returns round(25*1.35)=34 | current final score excludes Surge in inspected path; iOS should match | no creature effect beyond duration | add parity test to prevent accidental divergence |
| Regular Flow with Arc multiplier | active Arc chainBase 1.3, 30m Flow | base=55, tierExtra=0.1, used=1.4, final=round(55*1.4)=77, bonus=22 | Pearls=77 | creature by duration, Arc reward lines | session Arc fields + reward breakdown |
| Regular Flow with Movement | 30m, 1000 steps, source ratio 100 | base=55, movement=10, preArc=65 | final=65 no Arc, Pearls=65 | creature by duration | health snapshot/breakdown persisted |
| Delayed movement update | old movement 0, later steps 342 | source ratio 100 => 3 points; test conflict would expect 13 | reward can only increase; stable Pearl delta reason | Pearl delta if final increases | health snapshot/breakdown/session/ledger updated |
| Soft Flow | 10m Soft | base=0, movement excluded, Surge off | final=0, Pearls=0 | Stillwater drops=600 | Stillwater ledger + soft reward card |
| Creature release | level 1 Minnow | base pearl value = 10m*2=20; no investment | +20 Pearls; status RELEASED | hidden from active Chest/Blue | creature status + Pearl ledger |
| Creature level-up | Minnow level 1 | base cost 25; multiplier=1.12; floor 28 | -28 Pearls; level becomes 2 | no Mastery until 99; no badge found | creature level update + ledger |
| Lookout claim | achieved 30m one-time | basePearls=30, multiplier=1 | +30 Pearls on claim only | objective badge incremented on grant | completion + badge + ledger on claim |
| Level 99 mastery | creature reaches level 99 | economy tier MASTERED | Android award not found | desired species Mastery count +1 | TODO add mastery ledger/flag/tests |

## 26. iOS Reward Engine Recommendations

Recommended future iOS services/components only; do not create code now:

- `RewardCalculator`: pure base Flow score and timed bonus calculator.
- `MovementRewardCalculator`: steps-to-points, eligibility, delayed recalculation.
- `ArcRewardCalculator`: Arc multiplier/tier math and next multiplier.
- `SurgeRewardCalculator`: Surge formula; match current Android final-score/Pearl behavior exactly.
- `PearlLedgerService`: append-only Pearl entries, balance derivation, idempotency keys.
- `ShellRewardOrchestrator`: one entry point after Flow completion for Pearls, creatures, badges, Stillwater, events.
- `CreatureEconomy`: drops, growth costs, release values, Beyond Blue quotes.
- `StillwaterService`: Soft Flow drops, vessel validation, draw randomness.
- `LookoutRewardService`: objective progress, completion grant, claim transaction.
- `BadgeAwardService`: countable badge upserts and Mastery awards.
- `RewardRevealMapper`: persisted/computed rewards to card deck models.
- Repository transaction boundary: `FlowCompletionTransaction` or unit-of-work should save session, reward breakdown, health snapshot, Pearl ledger entries, creature grants, badge updates, Shell events, and ongoing Flow clear idempotently.

Reward calculators should be pure/testable where possible. Repositories should own persistence/transactions. UI should not directly calculate rewards. Use in-memory test repositories for parity tests. Persist reward breakdown data to avoid historical reward drift when formulas change.

## 27. iOS Test Plan Recommendations

| Test group | Android source/test reference | Expected iOS behavior | Priority |
|---|---|---|---|
| Base score | `ScoreCalculator.kt` | 1 point/minute + timed bonus formula | Critical |
| Timed bonuses | `ScoreCalculator.kt`; add tests | 10/30/60 chunk counts/values exactly | Critical |
| Surge | `ScoreCalculator.surgePoints`; add integration tests | Exact formula plus current Android non-contribution to final Scyra/Pearls | High |
| Arc multiplier | `ScoreCalculator.arcMath`, `ArcRules` | Start 1.3, tier extras, +0.1 next base | Critical for Arc parity phase |
| Movement Points | `MovementBonusCalculatorTest` plus source conflict | Chosen ratio; floor division; eligibility | Critical |
| Delayed movement | `DelayedMovementRewardPolicyTest` | Never lowers; Pearl delta idempotent | Critical |
| Pearl ledger | `ShellRepository`, `FlowHealthRepository`, Lookout | Balance from ledger; duplicate prevention | Critical |
| Duplicate award | Shell event tests | Re-running completion cannot duplicate economy | Critical |
| Creature drops | `CreatureEconomyTest` | Greedy duration conversion | Critical for The Blue |
| Creature release | `CreatureEconomyTest`, `ShellRepository` | Status change + payout formula | High |
| Creature growth | `CreatureEconomyTest` | Cost curve and max 99 | High |
| Creature Mastery | missing Android test | Countable per Level 99 instance | Critical before feature |
| Stillwater | `StillwaterCatalogTest` | Drops formula, costs, rarity draw | Medium/High |
| Lookout claim | `ObjectiveProgressCalculatorTest`, `LookoutRepository` | Completion then explicit claim | Medium |
| Reward reveal mapper | `RewardRevealMapperTest` | Unified score/Pearl, soft cards, hide legacy | High |
| Pulse no-reward | Product docs + no award code | Pulse creation no Score/Pearls | Critical |
| Soft Flow no-score | FlowViewModel/Shell orchestrator/reward tests | no Score/Pearls/Movement; Stillwater only | Critical |

## 28. Reward and Economy Risks / Open Questions

| Risk/open question | Why it matters | iOS impact | Recommended follow-up |
|---|---|---|---|
| Exact reward math mismatch | Movement is settled at 100 steps/point; Surge must match current Android non-contribution behavior | iOS could accidentally reintroduce stale assumptions | Parity tests for movement and Surge. |
| Movement ratio implementation | 100 vs 25 changes economy dramatically | Score/Pearl inflation risk | Decide before implementation. |
| `pulseBonusPoints` ambiguity | Field exists but appears zero | Pulse reward product violation risk | Confirm/remove/reserve in reward spec. |
| Arc Room vs DataStore source | Runtime multiplier appears in DataStore while planned runs use Room | Restoration bugs | Arc architecture cleanup/spec. |
| Surge parity coverage | Formula exists but is not applied to final score in inspected path | iOS could accidentally include it and inflate rewards/Pearls | Add Android/iOS parity tests documenting current behavior. |
| Delayed Health refresh on iOS | HealthKit background differs | Late rewards may be missed | HealthKit refresh design. |
| Pearl duplicate-award risk | Ledgers need idempotency | Economy inflation | Unique constraints/idempotency service. |
| Creature Mastery idempotency | No per-instance award flag | Double/missed badges | Mastery persistence design. |
| Creature drop/economy balance | Deterministic duration drops affect collection pace | iOS economy parity | Creature economy test suite. |
| Stillwater randomness/parity | Random draws may differ across platforms | Different collections | Decide deterministic seed vs platform random. |
| Lookout claim idempotency | Claim writes Pearls | Duplicate claim risk | Claim transaction tests. |
| Legacy reward concepts | Objects/trinkets/discoveries remain in code | Scope creep/Chest pollution | Product cleanup decision. |
| Reward reveal historical drift | In-memory reveal may differ from persisted later updates | History/replay mismatch | Persist complete reward snapshots or rebuild rules. |
| Aera support | Score hidden behavior affects reward UI | Scope expansion | Defer Aera unless requested. |
| Cloud sync economy conflicts | Local ledgers may need merge rules | Duplicate rewards across devices | Future sync spec. |

## 29. Acceptance Criteria for This Document

- This task only creates or changes `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- Android source code is untouched.
- No iOS source code or project is created.
- No tests are created.
- No database migrations are created.
- No schema files are copied.
- No Gradle/build files are changed.
- The document is based on actual Android reward/economy code plus prior docs.
- Exact Android formulas are documented where code makes them clear.
- Unclear formulas are marked `TODO: verify`.
- Current Android behavior is separated from desired product direction where needed.
- Pulse creation is not treated as rewarded unless code proves otherwise and product decision confirms it.
- Movement ratio conflict is explicitly documented.
- Creature Mastery behavior and idempotency gap are explicitly addressed.
- Pearl ledger/idempotency behavior is documented.
- Soft Flow and Stillwater behavior are documented.
- Legacy reward concepts are clearly separated from iOS Scyra parity behavior.
- iOS recommendations preserve repo boundary rules.

## 30. Codex Summary

- Docs and Android reward/economy files inspected: prior docs `00` through `05`, `ScoreCalculator`, `ScoreBreakdown`, Flow reward UI models, `FlowViewModel`, reward reveal mapper/content/deck files, Movement/Health utilities and repositories, Arc/Surge files, Shell reward orchestration/recorder/economy files, Shell repository/entity files, Lookout calculator/repository/entities, and reward/economy tests.
- Exact reward formulas discovered: base Scyra Score is full minutes plus tiered timed bonuses; Surge formula is exponential accuracy bonus; Arc formula multiplies pre-Arc points by chain base plus duration tier extra; Movement source currently uses `steps / 100`; Stillwater drops equal Soft Flow duration seconds; creature growth/release formulas are in `CreatureEconomy`.
- Timed bonus rules discovered: 10-minute counted chunks `+5`, 30-minute counted chunks `+15`, 60-minute chunks `+50`, with lower-tier counts reduced by higher-tier chunks.
- Surge rules discovered: exact formula exists and `surgePoints` is stored/displayed, but inspected final score/reward breakdown path does not add Surge points; iOS should match current Android behavior until Android changes.
- Arc multiplier rules discovered: start multiplier 1.3, +0.1 chain step after qualifying durations, tier extras 0.0/0.1/0.2/0.3/0.4 by duration, Arc applies to base plus Movement Points.
- Movement Point rules discovered: production source uses `STEPS_PER_POINT = 100`; 25-step expectations are stale, and iOS should implement 100 steps = 1 Movement Point.
- Pearl ledger rules discovered: Pearls are ledger-backed; eligible regular Flow Pearls equal final Scyra Points; Movement/Arc affect Pearls; delayed movement deltas use stable reason/source checks.
- Creature economy rules discovered: regular Flow creature drops are deterministic by duration; growth max is level 99 with Pearl cost curve; release changes status and pays Pearl salvage value; Beyond Blue uses creatures and/or Pearls.
- Stillwater rules discovered: Soft Flow drops are duration seconds; vessel costs are 15k/25k/45k/75k Drops; draw consumes ledger units transactionally and grants Stillwater creatures by rarity roll.
- Lookout reward rules discovered: progress uses non-Soft Flow duration in Journey windows; completions grant countable badges; Pearls are only written on explicit claim.
- Badge/Mastery rules discovered: Flow duration and objective badges are countable; Creature Mastery tier exists at Level 99, but no inspected code awards idempotent species Mastery badges.
- Specific concerns answered: base score, timed bonuses, movement ratio, Pulse no-reward rule, `pulseBonusPoints`, Arc math, Surge formula, Pearl equality/effects, delayed movement idempotency, creature drops, Stillwater exclusivity/drops, creature growth/release formulas, Mastery gap, legacy concepts, and future iOS reward services are all documented above.
- Remaining risks/gaps: Pulse bonus field fate, Aera support timing, Creature Mastery per-instance idempotency, legacy objects/trinkets/discoveries, Stillwater randomness parity, reward parity test coverage, and future cloud sync economy strategy.
- Anything outside `docs/06_REWARD_AND_ECONOMY_SPEC.md` changed: no.
- Repo boundary rules preserved: yes. The document is reference-only under `docs/`; no Android code/build files were modified, no tests/migrations/schema copies were created, and no iOS files were created.
