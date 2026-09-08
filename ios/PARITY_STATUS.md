# iOS / Android Parity Status

Android is the canonical product specification. This handoff was re-audited against the current Android and iOS code on 2026-09-07.

Status meanings:

- **COMPLETE** — the named, bounded slice is implemented and covered by relevant automated tests.
- **PARTIAL** — meaningful production behavior exists, but one or more Android contracts are absent or unverified.
- **MISSING** — only a route/placeholder or unused model groundwork exists; there is no coherent user-facing implementation.
- **NEEDS VERIFICATION** — implementation exists, but simulator/unit coverage cannot establish device or visual parity.

No whole product area should be inferred complete from a completed domain slice.

## Completed

- **Flow scoring domain:** completed-minute base score; 10/30/60-minute bonuses; Surge accuracy curve and overtime cap; Arc multiplier tiers and progression; Movement-before-Arc ordering.
- **Runtime Arc continuation domain:** inclusive five-minute continuation window, eligible active/recent selection, future timestamp rejection, early/normal/ultra pause budgets, pending-first-Flow behavior, Soft Flow multiplier reset, Continue Arc, Complete Arc, and Android's between-Flow grace-expiry/mid-Flow pause-budget conclusion. Arc-only conclusions aggregate persisted Flow and Shell reward history, reveal without committing a throwaway Flow, preserve paused draft state, clear runtime state transactionally, and remain retryable after persistence failure.
- **Core Flow persistence/lifecycle:** required title/Journey validation, Regular and Soft modes, timestamp-derived pause/resume/restoration, normalized active intervals, active draft restoration, atomic session/Chronicle/Pulse/Movement commit, terminal/continuation reward exit behavior, and Android's reward-time “Enter The Shell” transition without losing or fabricating the next Arc Flow.
- **Flow completion integrity:** completion freezes editable state while an asynchronous Health read is pending; post-commit summary/Journey refresh is best-effort and cannot report a committed Flow as failed; duplicate `flowInstanceID` commits cannot add another session, advance Arc state again, or create Movement rows under a retry UUID.
- **Story calendar/grouping domain:** Day/Week/Month half-open windows, Monday-based weeks, calendar/time-zone/DST-safe navigation, Journey filtering, totals, session-only Saga ranking, whole-Arc grouping, and out-of-window member counts.
- **Chronicle persistence/file domain:** explicit owner types, durable drafts, ordered text/media/voice moments, edit/delete/reorder, finalized-owner write rejection, Flow and Pulse promotion, app-owned files, traversal-safe paths, partial-success media import, cleanup, and reconciliation.
- **Pulse capture domain:** durable draft metadata and Chronicle, idempotent creation receipts, optional live-Flow attachment, promotion to completed session, standalone/nested Story presentation, detail/edit/delete, and Journey/date filtering.
- **Movement domain:** action-triggered HealthKit request, start-time eligibility freeze, active-interval reads, 100 steps per Movement Point, atomic reward snapshot/breakdown persistence, bounded 72-hour delayed refresh, monotonic reward updates, and Story/detail display.
- **Idea Grove vertical slice:** SwiftData and in-memory repository contracts; ALIVE/INSIGHT/COMPLETED transition guards; invalid-state repair; Android Recents/Newest/Oldest/Most Time/Least Time ordering; linked-Flow duration totals and newest-first history; Pulse deletion that preserves completed Flows; Pulse-origin launch context; abandoned-origin cleanup and meaningful-active-Flow conflicts; persisted origin restoration; atomic Flow/session/link commit; idempotent retry protection; INSIGHT auto-revival; Arc continuation choice; production UI/actions/empty/error/delete states; and a real Shell-to-Grove route.
- **Shell reward foundation:** idempotent per-session reward events; Pearl and Stillwater ledgers; regular-versus-Soft reward policy; greedy 120/60/30/10-minute creature milestones plus the 150-minute endurance Whale; cumulative Flow badges; basic durable find/badge grants; session/Arc aggregation; atomic Flow/Arc reward commit; monotonic delayed-Movement Pearl deltas; retry protection; Android completion-reveal mapping and multi-reward ordering; separate current-Flow and completed-Arc reveal stages with canonical headers/Next/Done; a functional reward-time “Enter The Shell” action; Android-shaped paged cards/indicators/Material icons/dialog actions; and a live Pearl balance on the Shell root.
- **Shell Chest/placement and Flow-achievement slice:** V4 per-copy finds, true quantity stacks, placements, upgrade audit, badge pins/count floors, and versioned backfill receipts; dual-write Flow reward ingestion; retry-safe historical grant/event reconciliation without false “new” state; Android core animal/object/trinket definitions and Focus nook compatibility; Android Chest ordering plus mastery/depth/Stillwater-vessel filters; correct resting-copy visibility and Focus displacement/return; atomic Pearl-object invitation, placement displacement/return, and object shaping; exact-source Shell notifications with Android's post-consumption acknowledgment rule, failed-target retry behavior, and typed collection/species child-route coordination; three-pin replacement; exact milestone counts; canonical creature-only Chest empty/grid/detail presentation; four-page Showcase/Badge Book/Within Reach/Progress hub; deduplicated Android-ordered collection cards; species roster/status details; owned/acquisition actions into Chest, Blue/Beyond Blue, and Stillwater; direct Flow and Arc-editor actions; real Shell routes; V3-to-V4 migration, transaction, mapper, coordinator, view-model, and simulator navigation tests.
- **Creature, Stillwater, and The Blue functional slice:** Android's complete 71-species Flow/Beyond Blue/Stillwater catalog; exact zones, sources, rarity weights, vessel costs/unlocks, preference/perspective, spendable and lifetime Drop accounting; atomic weighted draws into V5 per-copy inventory; immutable discovery/mastery/collection-completion evidence; inferred historical backfill without false new-state; collector/curator/completionist and species mastery achievements; tracked achievement/species filters; Level-99 mastery and celebration lifecycle; creature growth/release economy; atomic level-aware multi-copy release; Beyond Blue trade/Pearl quotes and atomic encounters; production Stillwater locked/confirmation/reveal/error states; Android-style four-depth vertical Blue pager and depth rail; owned-creature detail/growth/release/Focus/Chest actions; browseable unowned Beyond Blue targets with creature/Pearl mixed payment; and real Shell routes. Domain, migration, failure-rollback, repository, mapper/view-model, and simulator route tests cover the bounded slice.
- **Lookout objective slice:** V6 one-time and daily/weekly/monthly recurring Journey objectives; Android validation and duplicate rules; calendar/DST-safe 1/7/30-day windows; regular-Flow-only progress with Soft exclusion; first-crossing completion evidence; overshoot rewards; uncapped ten-percent and streak multipliers; skipped-cycle and streak-break behavior; durable processed-session idempotency; individual/badge/all claims; dynamic objective badges; transactionally coupled Flow-completion processing; create/archive/skip/claim/history UI; active-Flow conflict routing; and domain/repository/migration/UI tests.
- **Voyage Hall statistics slice:** completed regular-Flow streaks and day/week/month records; Monday week and calendar month boundaries; best points/duration/count records; Arc duration/count/multiplier/volume records that include Arc-linked Soft Flows; deterministic latest/stable-ID tie-breaks; chronological record drill-down details; empty/error states; and a real Shell route. Domain and simulator UI tests cover the record contracts.
- **Focus Room guided-exercise slice:** Android's exact Original Five catalog, order, copy, steps, and durations; native device speech with text-only fallback and persistent in-player toggle state; timestamp-derived phase advancement; pause/resume/restart/end; automatic background pause; breath countdowns; progress/remaining time; motion-aware breathing and stillness visuals; completion/replay/return states; no-points/no-rewards boundary; semantic labels; and a real Shell route. Catalog/player tests and a simulator route/player smoke test cover the slice.
- **Shell Heart Room composition:** scalable code-native turtle-shell chamber bands/glow, Android orbit positioning, Pearl center, active room buttons, new-creature indicator, whisper routing, Chest/Badge shortcuts, and semantic room identities replace the prior generic list of room cards. Exact screenshot, localization, and physical VoiceOver comparison remain verification work.
- **Completed-Flow Story CRUD slice:** Android's completed-Flow “Edit flow” contract is implemented as a real details surface rather than post-completion score mutation; its content order now follows Android's Flow summary → non-empty Chronicle → Pulses hierarchy instead of burying Pulse actions below iOS-only statistics panels; historical Pulses inherit the Flow's Arc and persist their Chronicle atomically; nested Flow detail → Pulse detail → Pulse edit navigation unwinds back through its presenting context; deleting a Flow preserves and detaches child Pulses, removes session Chronicle/Movement/Grove-link dependents, removes only unused Journeys, and clears Arc metadata only after the final Arc Flow; Pulse update/delete also cleans unused Journeys.
- **Arc metadata and Story exploration:** normalized Arc title/summary/outcome/highlight/next-step persistence, character limits, dirty/discard handling, metadata/reflection rendering, Android seven-day top-Journey ranking projection, current-window Saga ordering, Journey totals, Flow list/drill-down, and full-detail routing.
- **Preferences and Flow platform foundation:** persisted Android-compatible show-score, calm-mode, and language keys/defaults; Story/card/Arc/Surge visibility propagation; calm time-first session/Arc reward reveals; canonical Help toggles and five-topic carousel; `en`/`es`/`hi`/`mr` Help catalogs; action-triggered notification authorization; no-catch-up active-hour reminder scheduling; notification and external `skillz://flow` routing; cold active-Flow reminder restoration without a launch permission prompt; and native one-shot Surge milestone/completion haptics.
- **Horizon planned-Flow and Dreams slice:** durable `FlowPlan` models and repository operations; Android validation/normalization; pinned/updated/title ordering; Journey suggestions; Regular, Soft, target-minute, and launch-with-Surge presets; launch count and last-launched state; Dreams archive/restore/delete; canonical card/menu/empty/error/delete surfaces; active-Flow conflict protection; idle-draft replacement; relaunch-safe Flow prefill; and a real top-level Horizon route. Android declares Day/Week/Month lens state but does not render it or apply filtering, so iOS retains the same explicit state without inventing a filtering rule.
- **Horizon planned-Arc functional slice:** V3 `ArcPlan`, ordered step-snapshot, and singleton active-run persistence; V2-to-V3 lightweight migration; Android title/two-Flow/Soft/custom-day validation; recurrence; create/edit/detail/delete; Studio flags; launch counts; resume/restart; all seven canonical Suggested Scenes; active-Flow conflict protection; pre-created base-multiplier Arc runtime; title/Journey/Surge Flow prefill; durable next-step advancement in the same SwiftData transaction as Flow commit; final/malformed run cleanup; blank-Arc continuation after the final planned step; relaunch recovery; source-Flow deletion detachment with snapshot preservation; Flow planned-Arc status UI; reliable detail-dismiss-to-editor presentation; repository/domain/view-model tests; and simulator Suggested Scene and detail-to-editor tests.
- **Canonical top-level UX foundation:** Android's adaptive light/dark Material color roles, 48-point targets, spacing/radius hierarchy, Caveat SemiBold branding, teal application and Shell top bars, horizontally paged Story/Horizon/SkratchPad/Help navigation, Shell turtle entry, exact Android Material vector icons, launcher artwork, and Android-owned turtle artwork are now packaged natively in iOS. Story, Horizon, Flow, Pulse, Help, SkratchPad, Shell Heart, Idea Grove, Chest, Badge Book, Stillwater, Lookout, Voyage Hall, Focus Room, and the clean-profile Blue state have simulator capture coverage.
- **SkratchPad / Notepad slice:** Android's first-run welcome document, durable rich-text archive, Caveat/monospace/default font modes, bold/italic/underline/strike/super/subscript, undo/redo, search, keyboard-aware formatting bars, explicit empty-document persistence, and real top-level route are implemented with UIKit text storage hosted in SwiftUI. Copy and attribute tests plus simulator capture cover the bounded slice.
- **Story presentation slice:** score is visible on first launch, the Android Saga Pulse section is separate from Flow/Arc Saga cards, canonical period and Journey controls are present, and card/FAB/empty-state hierarchy uses the shared Android-derived design system.

## Partial

| Subsystem | Status | Verified on iOS | Remaining Android parity |
| --- | --- | --- | --- |
| App foundation and navigation | PARTIAL | Dependency container, typed routes, Android horizontal root pager and destination order, Story/Flow/Pulse navigation, reusable canonical top/action headers, functional Horizon and SkratchPad surfaces, Android-shaped Shell Heart Room, live typed Shell-root and room routes with no production placeholder fallback, context-preserving Story child back stack, exact-source Shell notification child routing, registered `skillz://flow` URL route, and notification-response routing | general navigation restoration and non-Flow deep links if Android adds any; exhaustive gesture/back-stack comparison on device |
| Flow product | PARTIAL | Timer, Regular/Soft, Surge, runtime and planned Arc actions, Chronicle, live and historical Pulse association, Pulse/FlowPlan/ArcPlan prefills, active-Flow conflict routing, Movement, completed-Flow details/delete, atomic planned-step and Shell-reward commit, show-score/calm behavior, reminders, deep-link restoration, Surge haptics, and Android's reward-to-Shell action | physical-device notification/haptic validation, an intentional iOS long-running/background strategy beyond scheduled reminders, and full Android visual/motion comparison |
| Runtime Arc product | PARTIAL | Core continuation/summary math, planned-Arc launch/association/advance/relaunch/final-step behavior, persisted Arc metadata/reflections, persisted reward-event aggregation, separate Flow-then-Arc completion reveal stages, and the automatic Arc-only conclusion/reveal triggered by grace expiry or pause-budget exhaustion | Android animation-level and broader populated-state visual comparison |
| Story | PARTIAL | Launch-visible score header (including zero-history state), separate Saga Pulse section, persisted show-score and calm propagation, Flow/Pulse history, filters, date windows, totals, Android session-only Saga/top-five ranking, Journey drill-down, Chronicle media, Movement, nested/historical Pulses, Arc grouping/metadata/reflections, Android-ordered completed-Flow details/Pulse composer, context-preserving Flow/Pulse detail/edit navigation, completed-Flow delete, Pulse edit/delete, and transactional cleanup | remaining populated-history card/menu/dialog states, reactive loading transitions, and full Dynamic Type/VoiceOver/device screenshot comparison |
| Chronicle text | PARTIAL | Durable exact payloads, draft prompts, order/edit/delete, promotion, empty/error rendering | screen-by-screen Android layout, copy, haptics, selection-aware dictation insertion, and full accessibility audit |
| Chronicle photo/video | NEEDS VERIFICATION | Native picker/camera review, grouped media moments, metadata/orientation, thumbnails, fullscreen playback, add-only capture publication, and cancellation tokens that prevent a delayed camera-permission response from presenting capture after backgrounding | physical-device camera and library behavior, denial/revocation, captured-video audio, hardware interruption validation, large-file stress, Android crop/layout/motion details |
| Chronicle audio/speech | NEEDS VERIFICATION | AAC recording, level/duration feedback, playback/seek, live dictation replacement, stored-audio transcription, editable transcript persistence, pending-permission cancellation, and safe teardown on background/audio interruption/media-service reset | physical-device microphone/speech behavior, denial/revocation, route changes, hardware interruption validation, locale selection, native text-selection insertion, and Android animation/copy parity |
| Pulse product | PARTIAL | Core capture, Chronicle, optional live-Flow attach, historical-Flow creation with inherited Arc, edit/detail/delete, Story nesting, complete Idea Grove status/history/sort/action behavior, and Pulse-origin Flow lifecycle | final screen-by-screen Android visual/accessibility comparison |
| Movement | NEEDS VERIFICATION | Complete app-side calculation, persistence, delayed refresh, transactionally credited positive Pearl deltas, settings card, and automated domain/repository coverage | real Apple Health samples, provisioning, denial/revocation semantics, delayed delivery after termination, and physical-device lifecycle |
| Shell collection and badges | PARTIAL | Full Android 71-creature catalog, sources/zones/rarities, Flow/Stillwater/Beyond acquisition, growth/atomic multi-release/trade economy, per-copy inventory, Android sort/tie-break and tracked/category/depth/mastery/Stillwater-vessel filters, placements/object upgrades, discovery/mastery/completion evidence, collection badges, exact Flow badges, pins/count floors/new state/backfill, notifications, canonical creature-only Chest with correct resting-copy visibility, four-page badge hub, deduplicated Android-ordered collection progress, species status/detail rows, typed exact-target routing to Chest/Blue/Beyond Blue/Stillwater/Flow/Arc Studio, and mastery celebration lifecycle | exact collection historical-evidence dates/checklists and recommendation disabled reasons, per-creature bespoke Canvas artwork, exact ocean placement animation, full room-open/maturity ambience, translated catalogs, and broader populated-screen visual comparison |
| Shell rooms | PARTIAL | Coherent production routes and canonical chamber-card presentation for Idea Grove, Chest, Badges, Stillwater, Lookout, Voyage Hall, and Focus Room; Stillwater vessel ordering/status/cost hierarchy; clean-profile Blue animated-ocean state; four-depth vertical populated Blue pager/depth rail; creature growth, level-aware bulk release, Focus placement, Chest handoff, and fully browseable Beyond Blue encounters; Android-shaped code-native Heart Room orbit and Shell coordination | exact bespoke object/creature/achievement artwork and species-specific ocean motion, all room strings in `es`/`hi`/`mr`, device speech/VoiceOver validation, and populated/error/dialog screenshot matrices |
| Persistence | PARTIAL | One SwiftData repository for Flow/runtime-Arc/ArcPlan/Chronicle/Pulse/Movement/Idea Grove/Shell rewards/FlowPlan/Chest/creatures/Stillwater/Lookout concepts; explicit V1–V6 schemas; tested lightweight upgrades through V6; transactional Flow/Pulse/planned-step/reward/objective cleanup and advancement; discovery/mastery/completion/action-receipt evidence; persisted origin/active-run/restoration; in-memory test/failure fallback | settings remain intentionally in UserDefaults; backup/export expectations are undefined; future persisted product areas must advance the schema and add upgrade tests |
| Design system and branding | PARTIAL | Canonical adaptive light/dark palette roles, Material card/button/chip/empty-state hierarchy, shared spacing/radius/tap-target components, byte-identical Caveat SemiBold and turtle assets, Android launcher artwork in a no-alpha AppIcon, imported Android Material vectors, and scalable Heart Room drawing | bespoke creature/object/achievement Canvas art is still incomplete; direct strings remain outside Help; physical-device dark/high-contrast/Dynamic Type verification and an exhaustive populated-state screenshot matrix remain |
| Help and settings | PARTIAL | Android's Keep Score Visible and Calm Mode rows, deferred-commit app-language picker, Flow/Soft Flow/Pulse/Arc/Surge carousel copy, Movement settings/permission explanation, and live preference propagation into Story/Flow/rewards | full device visual/accessibility comparison, notification-denial guidance if product requirements add it, and settings for future Shell/room features |
| Localization and accessibility | PARTIAL | App-language catalog/selection for system, `en`, `es`, `hi`, and `mr`; canonical localized Help/settings keys in all four Android languages; selected locale injection; semantic preference/help controls; and existing major-feature labels | catalogs and explicit lookup are not yet ported across the rest of Story/Flow/Pulse/Shell; many direct SwiftUI strings remain; Dynamic Type, VoiceOver order/actions, contrast, reduce-motion, and RTL need systematic audit |
| Platform lifecycle/services | NEEDS VERIFICATION | timestamp restoration, app-root foreground Movement/Flow refresh, action-triggered camera/mic/speech/Health/notification permissions, required usage descriptions, a renewed 48-hour horizon of active-hour local reminders on every foreground with no retroactive catch-up, cancellation on Flow exit/completion, authorized cold restoration, registered/handled `skillz://flow`, notification-response routing, and canonical Surge milestone haptic state | physical-device notification delivery/authorization/denial/revocation and haptic feel; termination tests; the platform-limited case where one Flow remains backgrounded beyond all 48 scheduled hours; iOS strategy for Android's ongoing foreground notification; media permission revocation; and device capability fallbacks |

## Missing

The following Android product areas do not yet have a coherent iOS implementation:

- **App-wide localization remainder:** Android `en`, `es`, `hi`, and `mr` catalogs outside the implemented Help/settings surface, including plural/format conversion and removal of direct user-facing Swift literals.
- **Canonical bespoke artwork remainder:** individual creature drawings/placement motion and the remaining Android code-drawn room/object/achievement scenes. The usable populated ocean pager, Android Material icons, launcher artwork, Caveat font, and turtle artwork are already implemented or packaged.

Shell rooms, the full creature catalog/economy, creature evidence/achievements, and mastery celebrations are no longer missing. Their remaining artwork, localization, device behavior, and exact presentation differences are tracked as **PARTIAL** or **NEEDS VERIFICATION** above rather than being misclassified as absent.

## Important Android References

### Flow, Arc, Story, Chronicle, Pulse, Movement

- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/FlowViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/flow/FlowScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/service/AliveFlowService.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/service/SurgeHapticsManager.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/score/ScoreCalculator.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/arc/ArcContinuationResolver.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/arc/ArcRules.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/StoryViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/story/StoryScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ArcMetadataEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/ChronicleRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/chronicle/ChroniclePage.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/PulseRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/story/pulse/PulseScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/health/FlowHealthRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/health/HealthRefreshUseCase.kt`

### Idea Grove and Pulse-origin Flow

- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/IdeaGroveRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/IdeaGroveDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/IdeaGroveViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/model/state/ideagrove/IdeaGroveModels.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/ideagrove/IdeaGroveScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/PulseFlowLinkEntity.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/viewmodel/IdeaGroveActiveFlowStateTest.kt`

### Horizon / planning

- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/PathsViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/paths/PathsScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/FlowPlanEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/FlowPlanDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/FlowPlanRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ArcPlanEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ArcPlanStepEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/ArcPlanRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/ActiveArcRunEntity.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/ActiveArcRunRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/PlanArcViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/ArcDetailViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/SuggestedRouteDetailViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/paths/arc/PlanArcScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/paths/arc/ArcDetailScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/paths/suggested/SuggestedRoutesCatalog.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/viewmodel/PlannedArcAdvanceTest.kt`

### Shell, rooms, achievements

- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/shell/ShellEntities.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/shell/AchievementEntities.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/ShellDaos.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/dao/shell/AchievementDao.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/shell/ShellDefinitions.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/model/entity/shell/ObjectiveEntities.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/ShellRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/AchievementBackfillWorker.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/shell/ShellRewardOrchestrator.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/shell/ShellRewardEventRecorder.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/flow/reward/RewardRevealMapper.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/viewmodel/shell/ShellViewModel.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/ShellRootScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/ShellNavigationCoordinator.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/shell/Stillwater.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/shell/CreatureEconomy.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/stillwater/StillwaterRoomScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/shell/LookoutRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/shell/lookout/ObjectiveProgressCalculator.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/lookout/LookoutRoomScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/shell/voyage/VoyageStatsCalculator.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/voyage/VoyageHallScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/focus/FocusRoomModels.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/focus/FocusRoomScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/focus/FocusExerciseVoiceGuide.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/rooms/blue/TheBlueRoomScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellChestScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/BadgesScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellNotificationsScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/domain/achievement/AchievementEngine.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/domain/lookout/ObjectiveCompletionProcessor.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardPolicyTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardEventRecorderTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/ShellRewardEventAggregatorTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/flow/reward/RewardRevealMapperTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/shell/CreatureEconomyTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/domain/achievement/AchievementEngineTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/data/model/shell/ShellContentCatalogTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/data/repository/shell/AchievementBackfillWorkerTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/inventory/ShellChestInventoryMapperTest.kt`
- `android/app/src/test/java/com/kingkharnivore/skillz/ui/screen/shell/ShellNavigationCoordinatorTest.kt`

### Settings, platform, localization, Notepad

- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/HelpScreen.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/utils/user/UserPrefs.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/NotificationPermissionGate.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/notification/AliveFlowNotificationFactory.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/data/repository/NotepadRepository.kt`
- `android/app/src/main/java/com/kingkharnivore/skillz/ui/screen/NotepadScreen.kt`
- `android/app/src/main/res/values/strings.xml`
- `android/app/src/main/res/values-es/strings.xml`
- `android/app/src/main/res/values-hi/strings.xml`
- `android/app/src/main/res/values-mr/strings.xml`

Android unit/instrumentation tests under `android/app/src/test` and `android/app/src/androidTest` remain executable specifications, especially for Arc lifecycle, planned-Arc advancement, Movement, reward reveal, Shell economy, achievements, Lookout objectives, Stillwater, Voyage statistics, and database migrations.

## iOS Architecture Notes

- `AppDependencyContainer` injects one `ScyraRepository` boundary into implemented features. SwiftData is the production store; the in-memory repository is used by tests and as an explicitly surfaced startup fallback.
- SwiftData now uses `ScyraSchemaV1` through `ScyraSchemaV6` with a lightweight migration plan. V1 mirrors the previously shipped unversioned 1.0 schema; V2 adds `FlowPlanModel`; V3 adds planned Arcs/steps/active runs; V4 adds per-copy/stacked finds, placements, upgrade audit, badge pins/count floors, and Shell backfill receipts; V5 adds creature lifecycle/discovery/mastery/collection completion/tracking/celebrations/action receipts and Stillwater perspective; V6 adds Lookout objectives/completions/skipped cycles/processed-session receipts. On-disk tests cover every introduced upgrade boundary and preserve prior data. Every future persisted product area must advance the schema and add an upgrade test.
- Timers derive elapsed time from persisted timestamps. Health reads use normalized active intervals rather than wall-clock duration.
- Flow commit is the success boundary. Session, Arc state, planned-Arc step transition, Chronicle/Pulse promotion, Movement snapshot, Idea Grove linkage, Pearl/Stillwater ledgers, find/badge grants, materialized Shell inventory, and reward events share one SwiftData save boundary. UI-only summary/Journey refreshes occur afterward and are best-effort. Duplicate completion keys preserve the first session and all of its side effects.
- Chronicle ownership is explicit (`ACTIVE_FLOW`, `SESSION`, `PULSE_DRAFT`, `PULSE`). File metadata and owner promotion are committed before binary cleanup; `ChronicleFileStore` owns app-relative media paths.
- `IdeaGroveRepository` owns Grove projections and transition rules. A Pulse-origin Flow persists its origin metadata in `ActiveFlowModel`; successful completion inserts the `PulseFlowLinkModel`, promotes Chronicle/Movement/Arc state, and revives INSIGHT within the same SwiftData save boundary. Duplicate completion receipts cannot attach the Flow to another Pulse.
- The Shell exposes functional destinations for Idea Grove, Chest, Badges, Stillwater, Lookout, Voyage Hall, Focus Room, and The Blue. `ShellCollectionRepository` owns inventory/placement/upgrade/badge/backfill transactions; `CreatureShellRepository` owns creature/Stillwater/Beyond Blue evidence and economy; `LookoutRepository` owns objectives and claims. `AchievementActionPolicy` reproduces Android's owned-versus-acquisition destination choice, while `ShellNavigationCoordinator` retains collection, species, instance, and notification identity until the child room reports successful consumption. Failed or superseded focus requests clear transient routing without marking their exact notification viewed, so the notification remains retryable. The Heart Room is a scalable SwiftUI drawing of Android's orbit rather than a static bitmap.
- Focus Room deliberately has no repository or reward dependency. Its player derives phase position from wall-clock timestamps, pauses on background, and uses an injected speech boundary so guidance failure never affects exercise progress or create Shell rewards.
- Movement refresh may only increase persisted rewards. Each resulting positive Pearl delta uses a stable final-state reason and is inserted in the same transaction as the snapshot, reward breakdown, and session score update.
- `RewardRevealMapper` is a platform-neutral mapping layer. SwiftUI renders its canonical cards as swipeable per-stage decks and receives calm mode explicitly so session/Arc reveals become time-first without altering persisted score or Shell rewards. Completed Arcs cannot be swiped into early: the current Flow deck is shown first, Android's explicit Next action resets paging into the Arc deck, and “Enter The Shell” remains available from either stage when the current session earned a Shell reward.
- Chronicle camera, recording, and speech controllers use operation identities across asynchronous authorization/staging work. Backgrounding or interruption invalidates pending work, so late permission callbacks cannot reopen capture, start recording/dictation, or publish a staging file that lifecycle cleanup already discarded.
- `ArcConclusionPolicy` reads all persisted Flow/Shell evidence before clearing runtime state. An Arc-only expiry reveal never inserts a Flow and dismisses back to the current Flow screen, preserving a paused draft exactly as Android does. Android does not currently add a confirmation or reflection editor to this automatic path; Arc metadata remains editable from Story.
- `AppPreferencesModel` is the single app-scoped preference state. It uses the same stable keys and defaults as Android (`show_score_ui = true`, `calm_mode = false`, optional `app_language_tag`) and is backed by `UserDefaults`, not the evolving SwiftData product schema.
- FlowPlan launch is orchestrated at `AppRootView`: `FlowViewModel.prepareFromPlan` replaces only an idle draft, clears abandoned Pulse origin, durably applies title/Journey/Soft and the stored Surge target, and refuses to overwrite a meaningful active Flow. `HorizonViewModel` records launch count/time only after preparation succeeds. Android's current route forwards title/Journey/Soft while its entity and UI copy explicitly promise a launch-with-Surge preset; iOS carries that stored target through so the advertised contract is functional.
- `PathsTimeLens` is present in Android state but has no rendered selector, query, transformation, or test usage. iOS retains `HorizonTimeLens` and selection state for parity, but intentionally does not expose an inert control or invent date filtering.
- Planned Arcs retain immutable Flow title/Journey/Soft/timing/Surge snapshots and an optional source-Flow ID. Deleting a source FlowPlan nulls that ID and marks the step detached without destroying the executable snapshot. A planned launch durably creates a base-multiplier runtime Arc plus one singleton active run; Continue Arc advances that run inside the Flow commit transaction before reward dismissal, and reward exit hydrates the already-durable next step. Final or malformed runs clear and fall back to Android's blank Arc continuation.
- Flow platform effects are injected boundaries. Production uses `UserNotificationFlowReminderScheduler` and `SystemSurgeHaptics`; tests use inert/recording implementations. Restoration never asks for notification permission at app launch. iOS schedules future active-hour reminders rather than attempting to emulate Android's unsupported ongoing foreground-service notification.
- `Supporting/Info.plist` is intentional: it preserves the existing privacy usage descriptions and registers the canonical `skillz://flow` scheme. It is excluded from synchronized resource copying through the Xcode project exception set.
- Localization is deliberately honest: only the implemented Help/settings surface has Android-derived `en`, `es`, `hi`, and `mr` `.strings` coverage. `AppLocalization` resolves the selected bundle explicitly while the selected locale is also injected into SwiftUI for future catalog-backed screens.
- Story intentionally uses whole-Arc totals while showing which Arc members fall outside the selected window, matching Android.
- Story child routes use an in-memory presentation stack: opening a completed Flow, then a nested Pulse, then its editor returns through Pulse details and Flow details one level at a time. Selecting any other top-level product area clears that stack; it is presentation state, not persisted navigation restoration.
- Android labels the completed-Flow long-press action “Edit flow,” but its current implementation opens read-only Flow/Chronicle details plus the historical-Pulse composer; it has no completed-session title/Journey/score update API. iOS follows that product contract and does not invent post-completion score editing.
- Flow deletion mirrors Android foreign-key/transaction effects explicitly in SwiftData: child Pulses are detached, Pulse/Flow links and Movement rows are removed, the Flow Chronicle is deleted before owned-file cleanup, Shell reward history remains durable, unused Journeys are removed, and Arc metadata survives until the last member is deleted.
- `ArcMetadata` normalization trims only leading/trailing whitespace and preserves internal content. Empty normalized forms clear the row; title/summary/reflection limits are 60/500/250 characters.
- `topJourneysLast7Days` mirrors Android's current computed top-five projection (score, duration, count) but is not rendered as a separate card because the canonical Android UI also does not currently consume that state. The visible Saga card and Journey drill-down use the selected Story window.
- `Resources/ASSET_COPY_MAP.md` records missing binary asset work. Do not reference Android runtime paths from iOS code.
- Canonical Android vector icons are copied into `Assets.xcassets` as template-rendered image sets and selected through `ScyraCanonicalIcon`/`ScyraCanonicalLabel`; feature views should not introduce new SF Symbol approximations when Android already uses a Material icon.
- The Blue must not render the undiscovered catalog as owned content. With no active/owned creature instances, Android's canonical state is an animated empty ocean; catalog progress belongs to Chest/collection surfaces until a creature is encountered.

## Next Recommended Work

1. **Run an average-user device reliability pass before visual polish.** Exercise notification authorization/delivery, HealthKit, Chronicle camera/video/audio/speech, background/foreground/termination restoration, permission denial/revocation, and interruption recovery on hardware; fix every crash, blocked action, or state-loss path found.
2. **Run a populated-state UX matrix for Story/Flow/Pulse/Horizon and Shell.** Use representative active/completed/error/locked/new/reveal data, exercise every card/menu/dialog/sheet action, and close broken or misleading interaction gaps before spacing-only work.
3. **Finish The Blue's bespoke scene layer.** Replace generic species glyphs with Android's per-species code-drawn creatures, level/age scale, placement families and motion, exact depth environments, room-open/maturity ambience, and reduce-motion behavior.
4. **Defer app-wide localization/accessibility until the usable beta path is stable**, then port the remaining Android catalogs and validate Dynamic Type, VoiceOver, contrast, RTL, and localized layout.
