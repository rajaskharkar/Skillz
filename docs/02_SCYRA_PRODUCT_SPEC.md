# Scyra Product Specification

## 1. Purpose

This document defines Scyra's product concepts, terminology, expected behavior, and intended iOS parity target for the future SwiftUI replication effort.

Core rules for this spec:

- Android is the current implementation source of truth.
- iOS should replicate Scyra product behavior, terminology, reward rules, visual identity, and user-facing flow unless a later product decision says otherwise.
- Legacy Android concepts should not automatically be ported if they conflict with current Scyra direction.
- Unclear behavior is marked `TODO: verify`.
- This document is documentation only; it does not change Android, create iOS code, change Gradle, or add dependencies.

## 2. Source Material Inspected

Prior docs inspected:

- `docs/00_REPO_BOUNDARIES.md` — repository boundary rules; `docs/` is reference-only and platform builds must not depend on sibling/root folders.
- `docs/01_ANDROID_ARCHITECTURE.md` — Android architecture inventory, high-risk parity areas, persistence inventory, and iOS implications.

Android source areas inspected:

- Navigation: `android/app/src/main/java/com/kingkharnivore/skillz/ui/navigation/SkillzDestinations.kt`, `android/app/src/main/java/com/kingkharnivore/skillz/ui/navigation/SkillzNavHost.kt`.
- Build/flavors: `android/app/build.gradle.kts`, `android/app/src/main/res/values/strings.xml`.
- Flow screen/ViewModel/state/service: `ui/screen/flow/FlowScreen.kt`, `viewmodel/FlowViewModel.kt`, `model/state/flow/FlowUiState.kt`, `model/state/flow/StopwatchState.kt`, `ui/service/AliveFlowService.kt`, `ui/service/AliveFlowServiceController.kt`, `ui/notification/AliveFlowNotificationFactory.kt`.
- Flow components/reward UI: `ui/screen/flow/StopwatchSection.kt`, `ui/screen/flow/SurgeMiniControl.kt`, `ui/screen/flow/ArcPill.kt`, `ui/screen/flow/reward/RewardRevealDeck.kt`, `RewardRevealMapper.kt`, `SessionRewardContent.kt`, `SoftSessionRewardContent.kt`, `ArcSummaryContent.kt`.
- Pulse: `ui/screen/story/pulse/PulseScreen.kt`, `data/model/entity/PulseEntity.kt`, `data/model/entity/PulseFlowLinkEntity.kt`, `data/repository/PulseRepository.kt`, `viewmodel/StoryViewModel.kt`, `viewmodel/IdeaGroveViewModel.kt`.
- Story/Home/Horizon-like surfaces: `ui/screen/SkillzHomeScreen.kt`, `ui/screen/story/StoryScreen.kt`, `ui/screen/story/StoryBody.kt`, `ui/screen/story/header/StoryHeader.kt`, `ScoreDisplay.kt`, `TagFilterRow.kt`, `FlowDetailsSheet.kt`, `PulseEditSheet.kt`, `ui/screen/story/chronicle/FlowCard.kt`, `PulseCard.kt`, `ArcGroupCard.kt`.
- Paths/Arc/Horizon planning: `ui/screen/paths/PathsScreen.kt`, `ui/screen/paths/arc/PlanArcScreen.kt`, `ui/screen/paths/arc/ArcDetailScreen.kt`, `viewmodel/PathsViewModel.kt`, `PlanArcViewModel.kt`, `ArcDetailViewModel.kt`.
- Shell root: `ui/screen/shell/ShellRootScreen.kt`, `ShellDestination.kt`, `ShellTopBar.kt`, `TheBlueModels.kt`, `viewmodel/shell/ShellViewModel.kt`.
- Shell rooms: `ui/screen/shell/rooms/blue/`, `ui/screen/shell/rooms/stillwater/StillwaterRoomScreen.kt`, `ui/screen/shell/rooms/ideagrove/IdeaGroveScreen.kt`, `ui/screen/shell/rooms/lookout/LookoutRoomScreen.kt`, `ui/screen/shell/rooms/voyage/VoyageHallScreen.kt`, `ui/screen/shell/rooms/focus/FocusRoomScreen.kt`, `FocusRoomModels.kt`, `FocusExerciseVoiceGuide.kt`.
- The Chest/badges/notifications: `ui/screen/shell/inventory/ShellChestScreen.kt`, `BadgesScreen.kt`, `ShellNotificationsScreen.kt`, `ui/screen/shell/ux/ShellUXHelper.kt`.
- Reward/economy: `utils/score/ScoreCalculator.kt`, `ScoreBreakdown.kt`, `utils/health/MovementBonusCalculator.kt`, `HealthRefreshUseCase.kt`, `utils/shell/ShellRewardOrchestrator.kt`, `ShellRewardEventRecorder.kt`, `CreatureEconomy.kt`, `Stillwater.kt`, `utils/shell/lookout/ObjectiveProgressCalculator.kt`, `utils/shell/voyage/VoyageStatsCalculator.kt`.
- Persistence entities: `data/model/entity/SessionEntity.kt`, `TagEntity.kt`, `PulseEntity.kt`, `PulseFlowLinkEntity.kt`, `OngoingSessionEntity.kt`, `FlowPlanEntity.kt`, `ArcPlanEntity.kt`, `ArcPlanStepEntity.kt`, `ActiveArcRunEntity.kt`, `data/model/entity/health/FlowHealthSnapshotEntity.kt`, `FlowRewardBreakdownEntity.kt`, `data/model/entity/shell/ShellEntities.kt`, `ObjectiveEntities.kt`, `ShellRewardEventEntity.kt`.
- Resources/theme: `android/app/src/main/res/values/strings.xml`, `colors.xml`, `themes.xml`, `android/app/src/main/res/font/caveatsb.ttf`, `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, drawable/mipmap resources.
- Tests used as product evidence: `ShellChestInventoryMapperTest.kt`, `CreatureEconomyTest.kt`, `TheBlueUiModelTest.kt`, `MovementBonusCalculatorTest.kt`, `DelayedMovementRewardPolicyTest.kt`, `ObjectiveProgressCalculatorTest.kt`, `ShellContentCatalogTest.kt`, `RewardRevealMapperTest.kt`, `StillwaterRoomScreenTest.kt`.

## 3. Product Identity

## Scyra Primary Color

The canonical Scyra primary color is SlytherinButNiceTeal `#3F8F8B`.

This is the active primary for:
- Android theme primary
- iOS theme primary
- top app bar title/accent
- selected navigation accents
- primary buttons
- primary containers
- borders/hairlines derived from primary
- general Scyra brand UI accents

RavenclawBlue / manuscript blue `#2F4F6F` is not the active Scyra primary and should not be used for new Scyra UI work.

Legacy note: RavenclawBlue `#2F4F6F` appeared in older design notes but is not the current Scyra primary.


- App/product name: **Scyra**. Base strings and the `scyra` product flavor use Scyra as the app name.
- Aera relationship: Android has an `aera` flavor with `applicationIdSuffix = ".aera"`, app name `Aera`, and `SHOW_SCORE = false`. This makes Aera visible as a score-hidden/calm variant at build-flavor level.
- Scyra relationship: Android has a `scyra` flavor with `applicationIdSuffix = ".scyra"`, app name `Scyra`, and `SHOW_SCORE = true`. Scyra is the score-enabled experience.
- Primary Scyra visual identity: focused, reflective, rewarding, and mythic/oceanic, especially inside The Shell and The Blue.
- Primary color: **SlytherinButNiceTeal `#3F8F8B` is the canonical Scyra primary color for Android, iOS, and current product direction**. Android still contains/reference values such as `0xFF2F4F6F` (legacy RavenclawBlue / prior Scyra blue reference) and stale source comments, but those are not active Scyra primary guidance.
- Typography: Android includes `android/app/src/main/res/font/caveatsb.ttf`, but **iOS should not use or copy Caveat**. iOS should use regular/system iOS typography for Scyra while preserving the calm brand tone with layout, color, and spacing.
- Design tone: deliberate focus sessions, reflective story/history, reward reveal satisfaction, and an oceanic/mythic Shell meta-layer with creatures, Pearls, The Blue, Stillwater, and room progression.

### iOS parity note

- iOS target is **full Android app parity for Scyra**, implemented in safe phases rather than as a reduced product scope.
- Aera support remains deferred unless explicitly requested later.
- If Aera is later requested, implement it as a deliberate product target, not as an accidental byproduct of Scyra settings.

## 4. Core Product Model

| Concept | Definition | User action | Android evidence | iOS parity expectation | Android vs desired direction |
|---|---|---|---|---|---|
| Flow | Core focused session and primary rewarded unit. | Start, pause/resume, complete, save details, earn score/rewards. | `FlowScreen.kt`, `FlowViewModel.kt`, `SessionEntity.kt`, `OngoingSessionEntity.kt`. | Native SwiftUI Flow timer/session screen with timestamp restoration. | Flow remains the core rewarded unit. |
| Journey | Tag/category for organizing Flows and Pulses. | Assign Journey, filter Story/stats, plan by Journey. | `TagEntity.kt`, `JourneyRepository.kt`, Story/Paths strings and filters. | Local Journey model and filters. | Android stores Journey as `TagEntity`; desired product term is Journey. |
| Pulse | Quick thought/idea/moment record. | Create/edit/delete; optionally attach to active Flow or Journey. | `PulseEntity.kt`, `PulseScreen.kt`, `PulseCard.kt`, `IdeaGroveScreen.kt`. | iOS Pulse capture/edit/history and Idea Grove integration. | Pulses are not rewarded Flow sessions. |
| Story / Chronicle | History of completed Flows and Pulses. | Review cards, filter by time/Journey, edit details/pulses. | `StoryScreen.kt`, `StoryBody.kt`, `FlowCard.kt`, `PulseCard.kt`. | iOS history/chronicle timeline. | Naming varies; desired terminology should be confirmed. |
| Horizon / Home | Main dashboard/planning surface. | Start Flow/Pulse, open Paths/Shell/Help/active Flow. | `SkillzHomeScreen.kt`, `PathsScreen.kt`, Lookout strings referencing Horizon. | iOS root dashboard. | Android uses Home/Paths/Story; Horizon appears in strings. TODO: verify desired naming. |
| Arc | Structured multi-step Flow progression/momentum chain. | Create Arc plan, launch steps, continue chain. | `ArcPlanEntity.kt`, `ArcPlanStepEntity.kt`, `ActiveArcRunEntity.kt`, `PlanArcScreen.kt`. | iOS Arc builder/detail and Flow launch integration. | Math belongs in reward spec. |
| Surge | Timed target/precision challenge inside Flow. | Enable target, receive haptics, see current Android bonus presentation. | `SurgeMiniControl.kt`, `SurgeRuntime.kt`, `AliveFlowService.kt`, `ScoreCalculator.kt`. | iOS foreground haptics/timer with timestamp catch-up. | Match current Android behavior exactly; do not fix/reinterpret Surge during the iOS port. |
| Soft Flow | Gentle unscored session. | Track time without score/Surge/Arc progression. | Flow strings, `isSoftMode`, `SoftSessionRewardContent.kt`, `Stillwater.kt`. | iOS must distinguish regular Flow from Soft Flow. | Soft Flow can feed Stillwater but should not award Scyra Score. |
| Scyra Score | Score representing focused effort. | Earn through regular Flows and modifiers; view in Story/rewards. | `ScoreCalculator.kt`, `SessionEntity.scyraPoints`, `ScoreDisplay.kt`. | iOS must preserve score behavior. | Detailed math deferred to reward spec. |
| Pearls | Spendable Shell/The Blue currency. | Earn from Flow/rewards/objectives; spend/grow/release creatures. | `PearlLedgerEntity`, `ShellRepository.kt`, `CreatureEconomy.kt`. | iOS ledger-backed currency. | Must avoid duplicate awards. |
| Movement Points | Physical effort bonus derived from steps. | Enable Health; earn delayed movement contribution. | `MovementBonusCalculator.kt`, Health repositories/entities. | HealthKit step bonus with same persisted breakdown semantics. | Final product/iOS parity decision: 100 steps = 1 Movement Point. |
| Shell | Gamified reward/meta layer. | Enter Shell, visit rooms, manage rewards/creatures. | `ShellRootScreen.kt`, `ShellViewModel.kt`. | iOS Shell hub with rooms. | Port current desired rooms; legacy concepts require confirmation. |
| Shell rooms | The Shell's feature spaces. | Navigate to The Blue, Stillwater, Idea Grove, Lookout, Voyage Hall, Focus Room, Chest, Badges. | `ShellDestination.kt`, Shell room files. | iOS room navigation. | Full parity target; implementation can be phased for safety. |
| The Blue | Ocean creature collection scene. | View creatures, levels, zones, details, release/encounter. | `rooms/blue/`, `CreatureEconomy.kt`. | iOS ocean/creature collection. | Creature-only direction should be preserved. |
| Stillwater | Soft Flow drop/reward room. | Accumulate Drops, draw vessels for Stillwater creatures. | `Stillwater.kt`, `StillwaterRoomScreen.kt`. | iOS Stillwater in full parity implementation order. | Soft Flow reward layer; no Scyra Score. |
| Idea Grove | Pulse-derived idea room. | Browse/sort/expand/status/delete Pulses. | `IdeaGroveScreen.kt`, `IdeaGroveViewModel.kt`. | iOS pulse idea garden. | No direct Pulse rewards. |
| The Lookout | Objectives/goals room. | Set objectives, track periods, claim rewards, manage streaks/skips. | `LookoutRoomScreen.kt`, `LookoutViewModel.kt`, `ObjectiveEntities.kt`. | iOS objectives if in scope. | Objective reward details need deeper spec. |
| Voyage Hall | Stats/analytics room. | View Flow/Journey/time/score analytics. | `VoyageHallScreen.kt`, `VoyageHallViewModel.kt`. | iOS analytics room. | Full parity target; phase based on engineering order. |
| Focus Room | Guided focus exercise room and creature display context. | Start guided exercises; view displayed creatures. | `FocusRoomScreen.kt`, `FocusRoomModels.kt`, `FocusExerciseVoiceGuide.kt`. | iOS calm guided exercises, likely TTS/audio decision needed. | Exercises explicitly do not affect rewards/stats. |
| The Chest | Creature inventory. | View level-aware creature stacks, level up, release. | `ShellChestScreen.kt`, `ShellChestInventoryMapperTest.kt`. | iOS creature-only inventory. | Desired direction: The Chest contains creatures only. |
| Badges | Lifetime records/achievements. | View countable badges. | `UserBadgeEntity`, `BadgesScreen.kt`, `ShellRewardOrchestrator.kt`. | iOS badge records. | Creature Mastery badge behavior requires implementation/spec verification. |
| Creature Mastery | Lifetime count of individual creatures reaching Level 99. | Level creatures to 99; earn per-creature species Mastery badges. | `CreatureEconomy.MAX_CREATURE_LEVEL`, tests. | iOS must implement countable species Mastery badges. | Current Android has Level 99 tiers but TODO verify badge award on reaching 99. |

## 5. Flow

Flow is Scyra's core focus session and primary rewarded product unit.

### Current Android behavior

- Purpose: a Flow tracks intentional time spent on meaningful work, practice, study, writing, workouts, or other focused effort.
- User-facing behavior: users enter Flow, provide a title/name, optionally assign a Journey, add notes/details, run a stopwatch, pause/resume, and complete/save.
- Start/active/pause/resume/complete: `FlowViewModel.kt` owns Flow runtime actions and `FlowUiState`; `StopwatchSection.kt` and `FlowScreen.kt` present timer controls; `OngoingSessionEntity` persists running state.
- Save behavior: completed regular Flows become `SessionEntity` rows; Soft Flows are saved as sessions with `isSoftMode = true` and no score/Arc progression according to strings and state.
- Edit behavior after completion: Story details UI includes `FlowDetailsSheet.kt`, notes placeholders, and `StoryViewModel.updateSessionDescription`. User can refine Flow notes/details after completion. TODO: verify whether title/Journey edits after completion are fully supported or only description/pulses.
- Flow title/name: represented by `SessionEntity.title`, `OngoingSessionEntity.title`, Flow title field components, and planned Flow entities.
- Journey/tag association: regular persisted sessions use `SessionEntity.tagId`; active/draft state stores tag name in `OngoingSessionEntity.tagName`.
- Description/notes/details: `SessionEntity.description`, `OngoingSessionEntity.description`, `ChronicleField.kt`, and Story detail sheets.
- Soft Flow distinction: `isSoftMode` appears in session/ongoing/plan entities and Flow UI strings; Soft Flow is recorded without score, Surge, Beam, or Arc progression, and uses soft reward presentation.
- Ongoing Flow persistence: `OngoingSessionEntity` stores singleton current session state including `baseStartTimeMs`, `accumulatedBeforeStartMs`, running flag, Surge fields, Arc fields, Pulse origin fields, and active interval JSON.
- Active interval tracking: health utilities include `FlowActiveInterval`, `FlowActiveIntervalCodec`, and `MovementStepAggregator`; `OngoingSessionEntity.activeIntervalJson` supports movement calculations over active intervals.
- Notification/service behavior: `AliveFlowService.kt` runs a foreground service, `AliveFlowNotificationFactory.kt` builds persistent and hourly reminder notifications, and deep link `skillz://flow` returns to the Flow screen.
- Timer restoration: Android reconstructs elapsed time from persisted timestamps and accumulated duration; iOS must follow this model.
- Reward/reveal behavior: regular Flow completion produces score/reward UI via `FlowRewardUiModel`, `RewardRevealDeck`, `SessionRewardContent`, `RewardRevealMapper`, and Shell reward orchestration.
- Relationship to Arcs: Flow launch can include planned Arc metadata; completion can affect active Arc state/multiplier and reward reveal summary.
- Relationship to Pulses: Pulses may be attached to an active Flow or linked after completion through Flow details. Pulse-origin Flow launch is supported through navigation args.
- Relationship to Movement Points: eligible regular Flows can get movement contribution from Health Connect step reads, including delayed refresh.
- Relationship to Shell rewards: regular Flows can award Pearls, creatures, badges, discoveries/events; Soft Flow can award Stillwater drops.

### Product rules

- Flow is the core rewarded focus unit.
- Pulse is not Flow.
- Soft Flow is a Flow subtype but is intentionally gentler/unscored.
- iOS must not rely on an always-running background timer.
- iOS should reconstruct elapsed time from persisted timestamps and active intervals.
- User should be able to complete a Flow and fill/edit details later where Android supports this pattern.

### iOS parity requirements

- Implement Flow as a native SwiftUI session screen with persistent session state.
- Persist active session fields sufficient to restore elapsed time, pause/resume state, notes, Journey, Arc context, Pulse origin, Surge target, and active intervals.
- Use UserNotifications only for user-visible reminders/return-to-flow behavior; never assume iOS background timers run indefinitely.
- Preserve regular-vs-Soft Flow reward differences.
- Preserve Flow reward reveal semantics and defer exact math to `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

## 6. Journey

Journey is the product term for the category/tag that organizes Flows, Pulses, plans, stats, and objectives.

- Current Android storage: `TagEntity` (`tags`) with `id`, `name`, and `createdAt`.
- Repository: `JourneyRepository.kt` manages tags/Journeys.
- Flows attach to Journeys through `SessionEntity.tagId`; planned Flows use `FlowPlanEntity.tagId`; active Flow uses a tag name snapshot.
- Pulses attach to Journeys through `PulseEntity.tagId`.
- Journey filters appear in Story via `TagFilterRow.kt`, Story header state, and `StoryViewModel` selected tag flows.
- Journey stats/details appear in Story/Saga/Journey detail UI and Voyage/Lookout derived models.
- Journey colors: `StoryViewModel` maintains `journeyColorMemory` and palette generation for journey color assignment. TODO: verify desired stable color persistence for iOS.
- Journey creation/editing: Android appears to create/reuse Journey tags from Flow, Pulse, Paths, and Arc planning inputs. TODO: verify if standalone Journey rename/delete exists.

### iOS parity expectations

- Use the user-facing term **Journey**, not “tag,” in UI.
- Internally, iOS may model this as `Journey` or `JourneyTag`, but user-facing copy should stay Journey.
- Preserve Journey filters in Story/history/stats and Journey association on Flows/Pulses/plans.
- Decide later whether Journey colors are deterministic or persisted.

## 7. Pulse

Pulse is a quick thought, idea, feeling, realization, event, or micro-win record.

### Product rules

- Pulses are not Flows.
- Pulses are not rewarded sessions.
- Pulse creation should not directly award Scyra Score.
- Pulse creation should not directly award Pearls.
- Pulses may be linked to Flows or used by Idea Grove if Android supports it.

### Current Android behavior

- Entity: `PulseEntity` stores `title`, `description`, optional `tagId`, optional parent session/flow IDs, optional `arcId`, timestamps, and `groveStatus`.
- Link entity: `PulseFlowLinkEntity` links pulses to completed sessions.
- Creation UI: `PulseScreen.kt` and Story/Flow details strings support recording a Pulse and optionally attaching it to an active Flow.
- Edit/delete behavior: `PulseEditSheet.kt`, `StoryViewModel.updatePulse`, `StoryViewModel.deletePulse`, and `IdeaGroveViewModel` delete/status actions support editing/deleting/status changes.
- Fields: title, description, Journey/tag, parent Flow/session references, Grove status, timestamps.
- Journey relationship: `PulseEntity.tagId` and Pulse edit/create Journey fields.
- Flow linkage: Pulse may be attached to current Flow, appear under active Flow in Chronicles, or be linked to completed Flow details.
- Idea Grove relationship: `IdeaGroveScreen.kt` and `IdeaGroveViewModel.kt` organize pulses with alive/insight/completed states.
- Grove statuses: Android defines `ALIVE`, `INSIGHT`, and `COMPLETED`.
- Reward caveat: `FlowRewardBreakdownEntity` contains `pulseBonusPoints`, but this is a current implementation field requiring Task 1.7 verification. Do not infer from this field that Pulses are product-rewarded.

### iOS parity requirements

- Implement Pulse capture/edit/delete as a lightweight thought/idea flow.
- Allow optional Journey association.
- Allow association with active/completed Flow only where the product explicitly supports it.
- Support Idea Grove statuses if Idea Grove is included.
- Do not award Scyra Score or Pearls directly for Pulse creation unless a later reward spec explicitly changes this rule.

## 8. Story / Chronicle / Horizon / Home

### Current Android naming and behavior

- Home/dashboard: `SkillzHomeScreen.kt` is the Navigation start destination (`home_screen`) and entry surface for starting Flow, recording Pulse, opening planned Flow/Arc/suggested route, returning to active Flow, and opening The Shell.
- Story/history: `StoryScreen.kt`, `StoryBody.kt`, `StoryHeader.kt`, and `StoryViewModel.kt` present the user's recorded Flows/Pulses and period filters.
- Chronicle cards: `FlowCard.kt`, `PulseCard.kt`, and `ArcGroupCard.kt` represent Flow, Pulse, and Arc entries in history.
- Time filters: Story includes day/week/month-style period state and date navigation through `PeriodAndDateNavigator.kt` and `StoryViewModel` period/anchor state.
- Score display: `ScoreDisplay.kt` and strings show Scyra Score when `SHOW_SCORE`/user preference permits.
- Journey filters: `TagFilterRow.kt` and Story state filter history by Journey.
- Flow/Pulse/Arc card behavior: cards show session/pulse/arc summaries; Flow details sheet exposes details and attached pulses; Pulse edit sheet edits Pulse content.
- Entry points: Home/NavHost route to Flow, Pulse, Shell, Paths/Plan Arc/Suggested Route, Help/settings, and active Flow.

### Desired product naming

- “Story” is the broad history/reflection concept.
- “Chronicle” appears to be the card/list treatment inside Story.
- “Home” is the current Android root surface.
- “Horizon” appears in strings around plans/objectives (`Plans on your Horizon`) but is not clearly a distinct implemented screen. TODO: verify whether Horizon should become the iOS dashboard/planning name.

### iOS parity expectations

- Provide a root dashboard that lets users start Flow, record Pulse, return to active Flow, open Shell, open planning/Arc areas, and open settings/help.
- Provide Story/history with Flow/Pulse/Arc cards, time filters, Journey filters, and score visibility behavior.
- Keep naming consistent with product direction once Horizon/Home terminology is confirmed.

## 9. Arc

Arc is structured multi-step Flow progression and momentum across related sessions.

### Current Android behavior

- Arc plan: `ArcPlanEntity` stores title, studio/archive flags, launch metadata, recurrence type/days, and timestamps.
- Arc steps: `ArcPlanStepEntity` stores ordered step snapshots from planned Flows, including title, Journey, soft mode, target minutes, launch-with-Surge, and link state.
- Active arc run: `ActiveArcRunEntity` stores singleton active Arc state, current step, title/Journey snapshots, soft mode, and timestamps.
- Arc planning UI: `PlanArcScreen.kt` and `PlanArcViewModel.kt` create/edit Arcs from planned Flows.
- Arc detail/launch: `ArcDetailScreen.kt`, `ArcDetailViewModel.kt`, `SuggestedRouteDetailScreen.kt`, and navigation launch Flow with planned Arc route args.
- Continuation/progress: `FlowViewModel.kt` reads Arc state/args and updates Arc runtime on completion/continuation.
- Multiplier: product-level behavior is that Arc momentum can increase reward/score multipliers over consecutive qualifying Flows. Android help strings mention a 10-minute threshold and +0.1 multiplier growth, but exact reward math belongs in the reward spec.
- Reward reveal: `ArcSummaryContent.kt`, `ArcSummaryUiModel.kt`, and `ShellRewardEventRecorder` aggregate/show Arc-level reward outcomes.

### iOS parity requirements

- Support Arc plans, ordered steps, launch into Flow, active Arc progress, continuation, and reward reveal summaries in the Arc parity phase.
- Preserve user-facing Arc concept as “structured momentum across Flows.”
- TODO: expand exact scoring/multiplier rules in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

## 10. Surge

Surge exists in Android as a timed target/precision behavior inside Flow.

### Current Android behavior

- Purpose: users can plan a target duration/Surge and receive bonus/haptic feedback tied to progress toward the target.
- UI: `SurgeMiniControl.kt`, `StopwatchSection.kt`, and Flow UI expose controls/status. Planned Flows and Arc steps store `launchWithSurge` and target minutes.
- Runtime: `SurgeRuntime.kt` evaluates events such as midpoint, five/two/one minute left, countdown, and target reached.
- Haptics: `SurgeHapticsManager.kt` plays milestone/countdown haptics; `AliveFlowService.kt` can evaluate Surge while foreground service is active.
- Reward relationship: `ScoreCalculator.surgePoints`, `SessionEntity.surgePoints`, reward chips/score display strings, and reward reveal UI represent Surge bonus.
- Flow relationship: Surge belongs to Flow; Soft Flow copy says Soft Flow is recorded without Surge.

### iOS parity requirements

- Implement Surge to match current Android behavior exactly, including whether Android displays/stores Surge separately from final Scyra/Pearls.
- Do not “fix” or reinterpret Surge during the iOS port; if Android changes later, update iOS parity then.
- Use iOS-native haptics/timers with persisted target state and timestamp catch-up; do not assume iOS can continue haptics/countdown indefinitely in background.

## 11. Scyra Score

Scyra Score is the product measure of focused effort for regular Flows.

### Current Android behavior

- Represents: points from Flow duration and modifiers/bonuses.
- Storage: `SessionEntity.scyraPoints`; reward breakdown stored in `FlowRewardBreakdownEntity.finalScyraPoints`.
- Display: `ScoreDisplay.kt`, Flow/Story cards, Journey/Saga summaries, reward reveal content, and score strings.
- Flavor/user-setting visibility: Scyra flavor defaults `SHOW_SCORE = true`; Aera flavor defaults `SHOW_SCORE = false`; `UserPrefs.showScoreUi` lets users toggle score UI.
- Flow duration relationship: `ScoreCalculator.breakdownFromDuration` calculates base/timed bonus score. TODO: expand exact math in reward spec.
- Arc relationship: `ScoreCalculator.arcMath`, `SessionEntity.arcMultiplierUsed`, and `arcBonusPoints` show Arc multipliers/bonuses.
- Surge relationship: `ScoreCalculator.surgePoints` and `SessionEntity.surgePoints` contribute visible bonus.
- Movement Points relationship: `MovementBonusCalculator` and `FlowRewardBreakdownEntity.movementPoints` can add delayed score contribution.
- Aera relationship: Aera is score-hidden/calm by flavor default, not a separate observed source tree.

### iOS parity requirements

- Preserve Scyra Score behavior and visibility rules for Scyra.
- Start iOS as Scyra score-enabled unless Aera is explicitly requested.
- TODO: expand detailed math in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

## 12. Pearls

Pearls are spendable Shell/The Blue currency and a ledger-backed reward.

### Current Android behavior

- Represents: reward currency brought back by Scyra and used inside The Shell.
- Persistence: `PearlLedgerEntity` stores `delta`, reason, source type/id, created time, and note.
- Earning: regular Flow completion can grant Pearls through `ShellRewardOrchestrator` using session score; objectives can grant Pearls via Lookout; delayed movement refresh can add Pearl deltas if eligible.
- Flow relationship: `ShellRewardResult.pearlsEarned`, reward reveal chips, and Pearl ledger updates are linked to completed sessions.
- Lookout/objective relationship: `ObjectiveCompletionEntity` stores base/final reward Pearls, claimed flags, and badge fields; `LookoutRepository` claims rewards and writes Pearl ledger entries.
- Movement relationship: `HealthRefreshUseCase` can calculate delayed movement score/Pearl delta and `FlowHealthRepository` applies transactional updates.
- Spending/release/growth: `CreatureEconomy.growthCostPearls`, `releaseValuePearls`, Beyond Blue quotes, `ShellRepository.growCreature*`, release/trade actions, and The Chest/Blue UI use Pearls.

### iOS parity requirements

- Implement Pearls as a ledger, not just a balance, to avoid duplicate awards and support auditability.
- Preserve Pearl award/spend/release/growth semantics once reward spec is finalized.
- Transactions must update session/reward/ledger/creature state atomically where Android does.

## 13. Movement Points

Movement Points represent physical effort derived from health step data.

### Current Android behavior

- Source: Health Connect `StepsRecord` through `HealthConnectMovementDataSource.kt`.
- Formula: `MovementBonusCalculator.calculateMovementPoints(steps)` returns `steps.coerceAtLeast(0) / 100`. Current Android behavior is therefore **100 steps per Movement Point**.
- Eligibility: `MovementBonusEligibilityPolicy` requires movement bonus enabled, Health Connect available, read-steps permission granted, regular point-eligible Flow, and not Soft Flow.
- Soft Flow exclusion: Soft Flow is not eligible for Movement Points based on the policy input.
- Delayed refresh: `FlowHealthSnapshotEntity`, `FlowRewardBreakdownEntity`, `HealthRefreshUseCase`, and `FlowHealthRepository` allow movement data to be refreshed after Flow completion, with score/Pearl deltas applied transactionally.
- Failure states: permission missing, unavailable, no data, and error are modeled in `MovementReadResult`.

### Final product direction

- Final product/iOS parity decision: **100 steps = 1 Movement Point**.
- Android production source using `steps / 100` is authoritative.
- Older 25-step expectations/tests/product notes are stale and should not guide iOS implementation.

### iOS parity requirements

- Use HealthKit as the iOS equivalent to Health Connect.
- Query steps across Flow active intervals, not simply the whole wall-clock day.
- Preserve delayed refresh/status behavior and clear denied/unavailable/no-data UI states.
- Keep movement reward math behind a testable service.

## 14. The Shell

The Shell is Scyra's gamified reward/meta layer.

### Current Android behavior

- Purpose: a mythic/oceanic inner world where Flow effort becomes creatures, Pearls, Stillwater drops, badges, objectives, and room state.
- Root behavior: `ShellRootScreen.kt` shows The Shell hub/Heart Room, room entrances, Pearl basin, Chest/Badges shortcuts, notifications, and contextual actions.
- Navigation: `ShellDestination.kt` models Heart, Focus, Stillwater, ShellChest, Badges, VoyagePreview, TheBluePreview, IdeaGrovePreview, and LookoutPreview.
- User progression/memory/state: `UserShellRoomStateEntity` stores per-room open/maturity/life state; `ShellViewModel` combines economy/ownership/memory state.
- Flow relationship: `ShellRewardOrchestrator.onSessionCompleted` converts completed sessions into Pearls, creatures, badges/discoveries/events.
- Pearl relationship: Pearl balance drives creature growth, Beyond Blue actions, and Shell affordances.
- Creature relationship: creatures are owned instances (`UserShellFindInstanceEntity`) with status, level, source, Flow time value, and placement/chest state.

### iOS parity expectations

- Implement The Shell as a native SwiftUI hub with room navigation and local persistence.
- Preserve oceanic/mythic tone and reward arrival feeling.
- Keep Shell state inside `ios/` when the iOS project is created.
- Do not port legacy object/trinket/discovery behavior unless product-confirmed or required for data compatibility.

## 15. Shell Rooms

### The Blue

- Current concept: ocean/creature collection room where animals encountered through regular Flows swim by zone.
- Android files: `ui/screen/shell/rooms/blue/`, `TheBlueRoomScreen.kt`, `TheBlueZonePage.kt`, `TheBlueCreatureTile.kt`, `TheBlueCreatureTray.kt`, `TheBlueAnimalDetailSheet.kt`, `ReleaseCreatureConfirmationSheet.kt`, `BeyondBlueEncounterSheet.kt`, `CreatureEconomy.kt`.
- Creature catalog: `CreatureCatalog` includes flow-earned, Beyond Blue, and Stillwater creatures. Flow-earned creatures include Minnow, Seahorse, Manta, and Whale with minute thresholds.
- Drops: `CreatureEconomy.creaturesForRegularFlowMinutes` awards creatures for regular Flow minutes; Soft Flow returns no regular creatures.
- Growth/levels: creatures have `animalLevel`, max level 99, Pearl growth costs, visual scaling, and mastery tiers.
- Release: releasing active creatures returns Pearls and sets status to released; released creatures should not appear as active inventory.
- Pearls: used for growth, trade/quotes, and received on release.
- Beyond Blue: trade/encounter flow can consume creatures/Pearls for Beyond Blue creatures.
- Rendering: The Blue uses Compose procedural/drawn creature and scene files rather than static creature assets.
- iOS parity target: SwiftUI/Canvas ocean scene, creature zones, level-aware creature display, details, growth, release, and Beyond Blue in the relevant parity phase.

### Stillwater

- Current concept: Soft Flow/rest/leisure reward room.
- Android files: `Stillwater.kt`, `StillwaterRoomScreen.kt`, `ShellRepository.kt`, `StillwaterLedgerEntity`, `StillwaterPreferenceEntity`.
- Soft Flow relationship: `calculateDropsForSoftFlow(durationSeconds)` uses Soft Flow duration seconds as drops. Soft Flow does not award score, but it can gather Stillwater drops.
- Draw/vessel behavior: `StillwaterVessel`, `StillwaterCatalog`, `validateStillwaterDraw`, and `roll` model vessels and Stillwater-exclusive creature draws.
- iOS parity target: implement Stillwater only when Soft Flow reward parity is implemented on iOS; preserve unscored calm tone.

### Idea Grove

- Current concept: Pulse-derived idea room.
- Android files: `IdeaGroveScreen.kt`, `IdeaGroveViewModel.kt`, `IdeaGroveRepository.kt`, `IdeaGroveModels.kt`, `PulseEntity.groveStatus`.
- Statuses: `ALIVE`, `INSIGHT`, `COMPLETED`.
- Behavior: sorting, expansion, mark as insight, mark completed, revive, delete Pulse, launch Flow from Pulse when no meaningful active Flow blocks it.
- Product rule: Pulse/Idea Grove behavior should not directly award Score or Pearls.
- iOS parity target: pulse idea garden with statuses and sorting when Shell parity includes Idea Grove.

### The Lookout

- Current concept: objectives/goals room tied to Journeys and periods.
- Android files: `LookoutRoomScreen.kt`, `LookoutViewModel.kt`, `LookoutRepository.kt`, `ObjectiveEntities.kt`, `ObjectiveProgressCalculator.kt`.
- Periods/windows: daily, weekly, monthly; one-time/recurring objectives; start/end windows; skipped cycles.
- Progress: calculated from Flow sessions by Journey/time window.
- Rewards/claims: objective completions store base/final reward Pearls, streak multipliers, badge key/label, claim flags; claiming writes Pearl ledger and increments badges.
- Streaks/skips: objective entities track current/max streak and skipped cycles.
- iOS parity target: implement as Journey objectives with period windows, progress, claim, streak/skip semantics when Lookout parity is implemented.

### Voyage Hall

- Current concept: stats/analytics room for Journey/Flow history.
- Android files: `VoyageHallScreen.kt`, `VoyageHallViewModel.kt`, `VoyageStatsCalculator.kt`, `VoyageHallStats.kt`.
- Stats: derived from Flow sessions and Journeys; likely includes time, score, streaks/period comparisons. TODO: verify exact visible metrics before iOS implementation.
- Filters/periods: ViewModel schedules local day boundary refresh and recalculates stats from source flows.
- iOS parity target: native stats room with matching metrics after a separate stats spec.

### Focus Room

- Current concept: calm guided focus exercise room plus possible displayed creatures context.
- Android files: `FocusRoomScreen.kt`, `FocusRoomModels.kt`, `FocusExerciseVoiceGuide.kt`.
- Exercises visible in Android: Three-Point Grounding, Box Breathing, Mini Body Scan, 4-7-8 Breathing, Five Senses Reset.
- Voice guide: Android uses TextToSpeech, tries to choose a gentle voice, and falls back to onscreen prompts if unavailable.
- Start behavior: user selects an exercise, starts guided player, can pause/resume/restart/end, and sees step/phase timers for breathing steps.
- Product rule from Android copy: Focus Room exercises do not affect Scyra Points, Pearls, creatures, Stillwater, or stats.
- iOS parity target: calm SwiftUI guided exercise player. TODO: decide voice/audio implementation: iOS TTS, bundled audio, or no voice for first pass.

## 16. The Chest

### Desired product direction

- The Chest should contain creatures only.
- It should not contain Trinkets.
- It should not contain room objects.
- It should not show released creatures.
- It should use level-aware grouping.
- Example: 3 level 3 Minnows should be shown separately from 4 level 1 Minnows.
- Level should appear at the bottom of the icon.
- Count should appear at the top right.
- Visual direction: RuneScape-bank-like icon inventory style.
- Desired iOS naming: **The Chest**.

### Current Android implementation

- Screen: `ShellChestScreen.kt` uses strings `shell_chest_title` = “The Chest” and `shell_chest_body` = “Your creatures from The Blue.”
- Legacy route/name: code still uses `ShellDestination.ShellChest`, `ShellChestScreen`, and route constants with “shell chest” naming. Product-facing copy is already The Chest.
- Filtering: `buildChestInventoryStacks` filters with `isActiveChestCreature`, excluding released/used Beyond Blue creatures and non-creature objects.
- Level grouping: stacks group by `findId` and `animalLevel`, producing separate stacks for the same species at different levels.
- Count badge: `shouldShowChestCountBadge(count)` shows count only when count > 1, positioned as tile badge.
- Level badge: tile shows `Lv <level>`/level label at bottom via Chest UI.
- Release: stack detail lets users release selected copies at a level and preview Pearl return.
- Level up: stack detail lets users level up one creature at the selected level if not max level and Pearls are sufficient.
- Tests: `ShellChestInventoryMapperTest.kt` verifies level-aware grouping, active creature filtering, and sorting behavior.

### iOS parity requirements

- Implement The Chest as creature-only inventory.
- Use level-aware grouping exactly; do not merge level 1 and level 3 copies.
- Do not show released or used/traded creatures.
- Use top-right count and bottom level indicators.
- Keep old “Shell Chest” names out of iOS UI; internal route names may be cleanly `Chest`.

## 17. Badges and Creature Mastery

### Current Android badge implementation

- Persistence: `UserBadgeEntity` stores `badgeId`, `count`, first/last earned timestamps, `isNew`, and viewed state.
- Display: `BadgesScreen.kt` groups objective badges and catalog badges, showing countable rows.
- Flow badges: `ShellRewardOrchestrator.kt` increments badges such as `badge_flow_10_min`, `badge_flow_30_min`, `badge_flow_60_min`, and `badge_flow_120_min` for regular Flow thresholds.
- Objective badges: `ObjectiveProgressCalculator.objectiveBadgeKey` and `LookoutRepository` award countable objective badges on claimed/completed objectives.
- Tests: `CreatureEconomyTest.kt` verifies max level 99 and mastery tiers; `ShellRewardEventAggregatorTest.kt`, `RewardRevealMapperTest.kt`, and notification tests cover badge counts/events/presentation.

### Creature Mastery rules for desired product direction

- Creature max level is 99.
- A creature reaching level 99 awards a Mastery badge for that creature species.
- Badge naming should use “Mastery,” e.g. “Minnow Mastery.”
- Mastery badges are countable per individual creature that reaches Level 99.
- If a user gets two Level 99 Minnows, the user should have Minnow Mastery count 2.
- Mastery persists even if the creature is later released or traded.
- Count represents lifetime individual creatures that reached Level 99.

### Android evidence and gaps

- `CreatureEconomy.MAX_CREATURE_LEVEL = 99`.
- `CreatureEconomy.creatureMasteryTier(99)` returns `CreatureMasteryTier.MASTERED`.
- Android strings currently use tier words such as Seasoned, Proven, Veteran, Ascendant, and Mastered, including “Mastered at Level 99.”
- TODO: verify whether Android currently awards a species-specific `Minnow Mastery` badge exactly when an individual creature reaches Level 99. Evidence found confirms max level/tier behavior but not a complete species Mastery badge award path.

### iOS parity requirements

- Implement countable species Mastery badges when a creature reaches Level 99.
- Treat the badge as lifetime achievement state separate from active inventory, so release/trade does not remove badge count.
- Preserve current Android badge counts where they already exist, but do not let tier labels replace the required “Mastery” badge naming.

## 18. Legacy / Do Not Port Without Product Confirmation

| Item | Current Android evidence | Why it may be legacy/questionable | Porting decision |
|---|---|---|---|
| Discovery Journal | `UserDiscoveryEntity`, `DISCOVERY_RECORDED`, `ShellRewardResult.discoveryIds`, `badge_discovery`, strings saying discovery body. | Product direction emphasizes creatures-only Chest and badges; Discovery Journal may be older Shell memory concept. | Do not port UI unless confirmed; retain only if data compatibility requires. TODO: verify. |
| Discoveries | `UserDiscoveryEntity`, `ShellRepository.recordDiscovery`, Shell notification discovery body. | May overlap with Badges/notifications and not be a desired first iOS concept. | Do not port unless confirmed. |
| Trinkets | `ShellRewardEventTypes.TRINKET_GRANTED`, catalog/test references may exist. | The Chest should not contain Trinkets. | Do not port to iOS inventory unless product explicitly restores Trinkets. |
| Room objects | `OBJECT_GRANTED`, `ShellPlacementEntity`, `ShellObjectIcon.kt`, `shell_contextual_invites_preview` with Glow Coral/Perch/Pebbles. | Current desired Chest is creature-only; object placement may be legacy/deferred Shell decoration. | Do not port to The Chest; port room object systems only if confirmed separately. |
| Shells | `ShellFind` naming and Shell object language. | “ShellFind” appears to be internal legacy naming for finds/creatures/objects. | Use creature/product names in iOS UI; data compatibility mapping only if needed. |
| Coral | Strings mention Glow Coral; potential object decoration. | Conflicts with creature-only Chest unless treated as non-inventory room decoration. | Do not port as inventory item without confirmation. |
| Plants | TODO: verify exact Android catalog references. | Could be old room object/decor concept. | Do not port without confirmation. |
| Old Shell Chest naming | `ShellDestination.ShellChest`, `ShellChestScreen`. | Product-facing desired name is The Chest. | iOS UI should say The Chest; internal names can be clean. |
| ShellFind naming | `UserShellFindInstanceEntity`, `ShellFindUpgradeEntity`, `ShellFindDefinition`. | Internal legacy umbrella term for creatures/objects/finds. | iOS should model creatures explicitly where possible. |
| Object placement | `ShellPlacementEntity`, placement DAOs, placement actions in `ShellRepository`. | May be room-decoration legacy, not part of current creature-only Chest direction. | Do not port unless a room decoration spec confirms it. |
| Pulse reward interpretation | `pulseBonusPoints` exists in `FlowRewardBreakdownEntity`. | Could suggest reward math field, but product rule says Pulses are not rewarded sessions. | Do not award Pulse creation; Task 1.7 must verify field meaning. |
| Beam naming | Strings mention Beam in Soft Flow exclusions and scheduling copy. | Beam does not appear as a primary requested iOS concept and may be stale/planned. | Do not port without product confirmation. TODO: verify. |

## 19. Visual and Experience Principles

- Scyra top app bar/title treatment should feel custom and calm, not generic utility UI.
- Use regular/system iOS typography for Scyra; do not copy or depend on Android Caveat.
- Treat SlytherinButNiceTeal `#3F8F8B` as Scyra’s canonical primary color. `0xFF2F4F6F` remains a legacy/deprecated Android reference value where source still uses it, not primary-color guidance.
- Material-card-inspired Android surfaces should become custom SwiftUI cards/surfaces while preserving hierarchy, softness, rounded shapes, and reward emphasis.
- The Shell should feel mythic/oceanic: The Blue, creatures, Pearls, Stillwater, Shell rooms, and quiet reward arrival.
- Reward reveal should feel satisfying and legible, with score/Pearl/creature/badge/Stillwater outcomes clearly separated.
- Creature inventory should feel collection-oriented and bank-like, especially The Chest's RuneScape-bank-like grid direction.
- Focus Room should feel calm, supportive, and non-gamified; it should not feel noisy or reward-chasing.
- Soundscape selection should use popup/dialog behavior, not bottom sheets, if implemented. TODO: verify soundscapes are in scope and where Android currently implements them, if at all.
- iOS can use native platform controls only where they do not dilute Scyra's visual identity or terminology.

## 20. iOS Parity Principles

- iOS should be native SwiftUI but behaviorally faithful.
- iOS should not directly copy Android architecture where platform differences require a better native equivalent.
- iOS should preserve Scyra terminology and reward behavior.
- iOS should keep its own local assets/resources under `ios/`.
- iOS should not reference `android/` resources.
- iOS should start with Scyra only unless Aera is explicitly requested.
- iOS should avoid porting legacy features unless confirmed.
- iOS should prioritize Flow, reward engine, persistence, Pulse, Shell, Chest, Badges, and The Blue before lower-priority polish.
- iOS should reconstruct timers from timestamps/intervals rather than assuming continuous background execution.
- iOS should keep HealthKit, notification, audio/TTS, and background behavior behind platform-native abstractions.

## 21. Product Risks and Open Questions

- Movement Points ratio is resolved.
  - Final decision: 100 steps = 1 Movement Point.
  - iOS impact: implement Android production parity and treat old 25-step notes/tests as stale.
- Whether Aera should exist on iOS.
  - Why it matters: Android supports score-hidden Aera via flavor, but iOS should prioritize Scyra first.
  - iOS impact: multiple targets/settings add scope and QA burden.
  - Follow-up: explicit iOS target/flavor decision.
- Whether legacy Shell economy concepts should be retained for data compatibility.
  - Why it matters: discoveries, trinkets, room objects, ShellFind naming, Coral/Plants may be legacy but may exist in persisted Android data.
  - iOS impact: porting them adds non-current concepts to full parity work; ignoring them may affect migration/import later.
  - Follow-up: legacy/data compatibility audit.
- Exact reward math needs a separate spec.
  - Why it matters: score, Surge, Arc, Movement Points, Pearls, badges, creatures, and Stillwater span multiple files.
  - iOS impact: high risk of mismatch/duplicate awards.
  - Follow-up: `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- Exact visual parity needs a separate design-system spec.
  - Why it matters: Android has Compose theme/resources and many custom Shell drawings.
  - iOS impact: native SwiftUI may drift visually without tokens/components.
  - Follow-up: design-system/resource spec.
- Exact navigation parity needs a separate navigation/screen-map spec.
  - Why it matters: Android routes, Shell internal destinations, sheets, and dialogs are distributed.
  - iOS impact: NavigationStack paths/sheets need a clean map.
  - Follow-up: navigation and screen map doc.
- Shell room implementation order.
  - Why it matters: the target is full Android parity, but The Blue, Chest, Stillwater, Idea Grove, Lookout, Voyage Hall, and Focus Room vary in complexity.
  - iOS impact: engineering order and dependency management.
  - Follow-up: phase planning, not product-scope reduction.
- Whether Focus Room voice/audio should use TTS, bundled audio, or another approach.
  - Why it matters: Android uses TTS and voice availability fallback.
  - iOS impact: AVSpeechSynthesizer/bundled audio have different UX and asset implications.
  - Follow-up: Focus Room audio implementation decision.
- Whether soundscapes are in scope for first iOS pass.
  - Why it matters: requested UX principle mentions popup/dialog soundscape selection, but no clear Android soundscape implementation was confirmed in inspected files.
  - iOS impact: audio assets and UI scope may expand.
  - Follow-up: soundscape scope/design task. TODO: verify Android source references.
- Creature Mastery badge award path.
  - Why it matters: desired product requires countable species Mastery badges at Level 99.
  - iOS impact: must avoid implementing only visual mastery tiers without persistent badges.
  - Follow-up: verify Android badge award behavior and add reward spec/tests.

## 22. Acceptance Criteria for This Document

- This task only creates or changes `docs/02_SCYRA_PRODUCT_SPEC.md`.
- Android source code is untouched.
- No iOS source code or project is created.
- No Gradle/build files are changed.
- The document is based on actual Android files plus prior docs.
- The document distinguishes current Android behavior from desired product direction where needed.
- Unclear behavior is marked `TODO: verify`.
- Legacy concepts are clearly separated from desired iOS behavior.
- Pulses are documented as thoughts/ideas, not rewarded Flow sessions.
- The Chest is documented as creature-only.
- Creature Mastery is documented as countable per individual Level 99 creature.
- Repo boundary rules from `docs/00_REPO_BOUNDARIES.md` remain intact.

## 23. Codex Summary

- Docs and Android files/directories inspected: `docs/00_REPO_BOUNDARIES.md`, `docs/01_ANDROID_ARCHITECTURE.md`, Android navigation, Flow, Pulse, Story/Home, Paths/Arc, Shell root/rooms, reward/economy, persistence entity, resource/theme, and test files listed in Section 2.
- Most important product concepts captured: Flow, Journey, Pulse, Story/Chronicle/Home/Horizon, Arc, Surge, Soft Flow, Scyra Score, Pearls, Movement Points, The Shell, Shell rooms, The Blue, Stillwater, Idea Grove, The Lookout, Voyage Hall, Focus Room, The Chest, Badges, and Creature Mastery.
- Legacy concepts identified: Discovery Journal/discoveries, Trinkets, room objects, Shells/ShellFind naming, Coral, Plants, old Shell Chest naming, object placement, Beam naming, and any Pulse reward interpretation.
- Resolved decisions applied: full Android parity target, SlytherinButNiceTeal `#3F8F8B` as the canonical Scyra primary color, 100 steps per Movement Point, system iOS typography instead of Caveat, owner-created turtle logo usable after iOS-local export, and Surge should match current Android behavior. Remaining gaps include Aera scope, legacy data compatibility, detailed reward parity tests, full Shell implementation order, Focus Room voice/audio details, soundscape scope, and exact Creature Mastery badge idempotency path.
- Anything outside `docs/02_SCYRA_PRODUCT_SPEC.md` changed: no.
- Repo boundary rules preserved: yes. The document is reference-only under `docs/`; no Android code/build files were modified and no iOS project/files were created.
