# Navigation and Screen Map

## 1. Purpose

This document maps the Android app's current navigation and screen structure so the future iOS app can replicate Scyra's:

- top-level app flow
- screen hierarchy
- route arguments
- screen entry points
- modal/dialog/sheet behavior
- Shell room navigation
- Flow/Pulse/Arc launch paths
- reward reveal paths
- back behavior
- top app bar behavior
- iOS `NavigationStack` / sheet / dialog parity

This is documentation only. It does not modify Android source code, create iOS source code, create an Xcode project, move files, change Gradle/build configuration, add dependencies, or refactor anything.

## 2. Source Material Inspected

Prior docs inspected:

- `docs/00_REPO_BOUNDARIES.md`
- `docs/01_ANDROID_ARCHITECTURE.md`
- `docs/02_SCYRA_PRODUCT_SPEC.md`

Android files/directories inspected:

- Manifest/startup: `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/java/com/kingkharnivore/skillz/MainActivity.kt`
- Navigation: `android/app/src/main/java/com/kingkharnivore/skillz/ui/navigation/SkillzDestinations.kt`, `android/app/src/main/java/com/kingkharnivore/skillz/ui/navigation/SkillzNavHost.kt`
- Home/global chrome: `ui/screen/SkillzHomeScreen.kt`, `ui/screen/SkillzTopAppBar.kt`, `ui/screen/HelpScreen.kt`, `ui/screen/NotificationPermissionGate.kt`, `ui/screen/NotepadScreen.kt`
- Flow screens: files under `ui/screen/flow/`, especially `FlowScreen.kt`, `StopwatchSection.kt`, `SurgeMiniControl.kt`, `ArcPill.kt`, `ChronicleField.kt`, `GrandTitleField.kt`, `JourneyLean.kt`
- Flow reward screens: files under `ui/screen/flow/reward/`, especially `RewardRevealDeck.kt`, `RewardRevealMapper.kt`, `SessionRewardContent.kt`, `SoftSessionRewardContent.kt`, `ArcSummaryContent.kt`, `RewardUX.kt`, `RewardRevealText.kt`
- Story/history: files under `ui/screen/story/`, including `StoryScreen.kt`, `StoryBody.kt`, `chronicle/FlowCard.kt`, `chronicle/PulseCard.kt`, `chronicle/ArcGroupCard.kt`, header files including `StoryHeader.kt`, `FlowDetailsSheet.kt`, `PulseEditSheet.kt`, `ScoreDisplay.kt`, `TagFilterRow.kt`, `PeriodAndDateNavigator.kt`
- Pulse: `ui/screen/story/pulse/PulseScreen.kt`
- Paths/plans: files under `ui/screen/paths/`, especially `PathsScreen.kt`, `paths/arc/PlanArcScreen.kt`, `paths/arc/ArcDetailScreen.kt`, `paths/suggested/SuggestedRouteDetailScreen.kt`, `paths/suggested/SuggestedRoutesCatalog.kt`
- Shell: files under `ui/screen/shell/`, especially `ShellRootScreen.kt`, `ShellDestination.kt`, `ShellTopBar.kt`, `TheBlueModels.kt`, `ux/ShellUXHelper.kt`
- Shell rooms: files under `ui/screen/shell/rooms/`, including `blue/`, `focus/`, `ideagrove/`, `lookout/`, `stillwater/`, `voyage/`
- Shell inventory: files under `ui/screen/shell/inventory/`, including `ShellChestScreen.kt`, `BadgesScreen.kt`, `ShellNotificationsScreen.kt`
- Navigation-driving ViewModels: `FlowViewModel.kt`, `StoryViewModel.kt`, `PathsViewModel.kt`, `PlanArcViewModel.kt`, `ArcDetailViewModel.kt`, `SuggestedRouteDetailViewModel.kt`, `IdeaGroveViewModel.kt`, `ShellViewModel.kt`, `LookoutViewModel.kt`, `VoyageHallViewModel.kt`, `HealthSettingsViewModel.kt`
- Resources revealing user-facing navigation labels: `android/app/src/main/res/values/strings.xml`, `android/app/src/main/res/font/caveatsb.ttf`, `android/app/src/main/res/drawable/scyra_turtle.png`

Expected path notes:

- No dedicated app-level `Paths` route is present in `SkillzDestinations.kt`; Paths is an internal page inside `SkillzHomeScreen`. TODO: verify whether iOS should make Paths a top-level route or keep it as a root/dashboard tab/page.
- No dedicated app-level Notepad route is present; Notepad is an internal page inside `SkillzHomeScreen`.
- No soundscape picker implementation was confirmed in inspected navigation/screen files. TODO: verify if soundscapes are future-only or implemented elsewhere.

## 3. Navigation Architecture Summary

### Current Android structure

- Navigation technology: Jetpack Navigation Compose.
- Main NavHost file: `ui/navigation/SkillzNavHost.kt`.
- Route definition file: `ui/navigation/SkillzDestinations.kt`.
- Start destination: `home_screen`.
- Deep link: `skillz://flow` maps to the Flow route (`add_skill?...`) in `SkillzNavHost.kt` and is also declared as an Android `VIEW` intent filter in `AndroidManifest.xml` for `MainActivity`.
- App-level route arguments:
  - Flow route arguments: `prefillJourney`, `prefillTitle`, `prefillSoftMode`, `originPulseId`, `plannedArcTitle`, `plannedArcStepIndex`, `plannedArcTotalSteps`.
  - Plan Arc route argument: `editArcPlanId`.
  - Arc Detail route argument: `arcPlanId`.
  - Suggested Route Detail route argument: `suggestedRouteId`.
- Shell rooms do **not** use app-level NavHost routes. `ShellRootScreen.kt` owns internal room state using `ShellDestination` sealed destinations.
- Dialogs, sheets, popups, reward reveal, and inlays are represented as local Compose state or ViewModel state, not app-level routes.
- ViewModels consume `SavedStateHandle` for app route arguments:
  - `FlowViewModel.kt` reads Flow prefill/origin/Arc launch args.
  - `PlanArcViewModel.kt` reads `editArcPlanId`.
  - `ArcDetailViewModel.kt` reads `arcPlanId`.
  - `SuggestedRouteDetailViewModel.kt` is passed the selected catalog route from NavHost and also uses its own state.
- Active Flow/deep link behavior:
  - `MainActivity` can reinstate the foreground Flow notification when an ongoing Flow exists.
  - `AliveFlowNotificationFactory` builds a PendingIntent to `skillz://flow`.
  - The NavHost deep link opens the Flow route, and `FlowViewModel` restores ongoing session state from persistence.

### iOS parity direction

- Use SwiftUI `NavigationStack` with a typed app route enum.
- Use a root/dashboard container for Home/Story/Paths/Notepad/Help-equivalent pages unless a later navigation spec chooses tabs.
- Use local `sheet`, `fullScreenCover` only where needed, `confirmationDialog`, custom dialog overlays, or in-view card decks for transient UI rather than forcing every Compose sheet into app routes.
- Use an internal `ShellRoute` enum/state inside the Shell feature for rooms, matching Android's internal `ShellDestination` approach.
- Model Flow/Pulse/Arc launch contexts as typed payloads instead of raw query strings.
- Support notification/deep link restoration to active Flow using persisted timestamps/intervals.

## 4. Top-Level Routes

| Android route string | Destination screen | Source file | Route arguments | Deep links | User-facing purpose | Entry points | Exit/back behavior | iOS equivalent | Priority |
|---|---|---|---|---|---|---|---|---|---|
| `home_screen` | `SkillzHomeScreen` | `SkillzNavHost.kt`, `SkillzHomeScreen.kt` | none | none | Root Home/dashboard containing Story, Paths, Notepad, Help pages and Shell entry | App start; `popToHome()` after Flow/Plan Arc/Suggested Route | Root destination; child page switching is internal state | `AppRoute.home` with root page state | MVP |
| `add_skill?...` / `add_skill` | `FlowScreen` | `SkillzDestinations.kt`, `SkillzNavHost.kt`, `FlowScreen.kt` | `prefillJourney`, `prefillTitle`, `prefillSoftMode`, `originPulseId`, `plannedArcTitle`, `plannedArcStepIndex`, `plannedArcTotalSteps` | `skillz://flow` | Start/continue Flow or Soft Flow, launch planned/Arc/Pulse-origin Flow | Home Story FAB, planned Flow, Arc detail, Suggested Route, Shell journey/pulse launch, active Flow notification/deep link | `onDone` and `onCancel` call `popToHome`; reward reveal can also open Shell or continue Arc | `AppRoute.flow(FlowLaunchContext)` | MVP |
| `add_pulse` | `PulseScreen` | `SkillzNavHost.kt`, `PulseScreen.kt` | none | none | Capture a Pulse | Home/Story add Pulse | `onDone` pops to Home; `onCancel` pops back | `AppRoute.pulse(PulseLaunchContext.create)` | MVP |
| `shell` | `ShellRootScreen` | `SkillzNavHost.kt`, `ShellRootScreen.kt` | none | none | Enter The Shell and its rooms | Home top app bar Shell turtle; Flow reward/open Shell; FlowScreen; active Shell entries | `onBack` pops app NavHost; Shell internal room changes use local state | `AppRoute.shell(initialShellRoute?)` | MVP |
| `plan_arc?editArcPlanId={editArcPlanId}` / `plan_arc` | `PlanArcScreen` | `SkillzDestinations.kt`, `SkillzNavHost.kt`, `PlanArcScreen.kt` | `editArcPlanId` Long default `-1` | none | Create/edit Arc plan | Paths page Plan Arc; Arc Detail edit | `onBack` pops; `onDone` pops to Home | `AppRoute.planArc(editId:)` | MVP/Phase 2 |
| `arc_detail/{arcPlanId}` | `ArcDetailScreen` | `SkillzDestinations.kt`, `SkillzNavHost.kt`, `ArcDetailScreen.kt` | `arcPlanId` string parsed by ViewModel | none | Inspect/start/edit an Arc | Home/Paths `onOpenArc` | Back pops; edit navigates Plan Arc; begin navigates Flow with Arc args | `AppRoute.arcDetail(id:)` | MVP/Phase 2 |
| `suggested_route_detail/{suggestedRouteId}` | `SuggestedRouteDetailScreen` | `SkillzDestinations.kt`, `SkillzNavHost.kt`, `SuggestedRouteDetailScreen.kt` | `suggestedRouteId` | none | Inspect/save/begin suggested route | Home/Paths suggested route card | Back pops; done pops to Home; begin navigates Flow with Arc args | `AppRoute.suggestedRouteDetail(id:)` | Phase 2 |
| `help` | `HelpScreen` | `SkillzNavHost.kt`, `HelpScreen.kt` | none | none | Help/settings screen | App-level route exists, but Home also renders Help as an internal page | No explicit entry found in inspected NavHost except route declaration. TODO: verify if externally navigated. | Prefer root page or `AppRoute.helpSettings` | MVP for settings; route TODO |

Notes:

- `SkillzDestinations.SKILLS_LIST` and `ADD_SKILL` naming are legacy/internal; product-facing naming is Flow.
- Paths, Notepad, Story, and Help are not top-level app routes in current Android Home; they are pages selected by `SkillzTopAppBar` through `LocalSkillzHomeNav`.

## 5. App Startup and Root Flow

Current Android startup:

1. Android launches `MainActivity` declared as the exported launcher activity in `AndroidManifest.xml`.
2. `MainActivity.onCreate` calls `installSplashScreen()` using `Theme.Skillz.Splash`, then `enableEdgeToEdge()`.
3. `MainActivity.maybeReinstateFlowNotification()` checks `AliveFlowRepository.getOngoingSession()`. If an ongoing/running Flow exists and notification permission is allowed, it starts `AliveFlowService` through `AliveFlowServiceController`.
4. `setContent` renders `SkillzTheme`, a full-screen `Surface`, `NotificationPermissionGate`, and `SkillzNavHost`.
5. `SkillzNavHost` starts at `home_screen`, rendering `SkillzHomeScreen`.
6. If the incoming intent is `skillz://flow`, Navigation Compose deep link handling can open the Flow route.
7. On resume, `MainActivity` again reinstates Flow notification and runs foreground Health refresh.

Top-level state affecting navigation:

- `SkillzNavHost` creates a shared `FlowViewModel` and observes `ongoingSession`; this drives `isFlowModeOn` / `hasOngoingFlow` flags passed to Home and Shell.
- A shared `StoryViewModel` is created in NavHost for Story/Pulse/Help surfaces.

### iOS parity expectations

- The future iOS app starts inside `ios/` with its own launch screen/root SwiftUI app target.
- Notification permission should use iOS-native authorization, not Android-style permission gate UI.
- Active Flow restoration must be timestamp-based using persisted session state.
- If iOS supports deep links/notification taps to active Flow, route into `AppRoute.flow(.resumeActive)`.

## 6. Top App Bar and Global Chrome

### Android global Home chrome

- `SkillzTopAppBar.kt` defines `SkillzTopAppBar`.
- Title: hard-coded `Scyra`, using `FontFamily(Font(R.font.caveatsb))`, 30sp, on-primary color.
- Top app bar color: `MaterialTheme.colorScheme.primary` container.
- Home navigation state: `LocalSkillzHomeNav` provides `SkillzHomeNavState(currentPage, onSelectPage)`.
- Icon ordering in `HomeNavIcons` is exactly:
  1. Story icon: `Icons.Outlined.AutoStories`
  2. Paths/Horizon icon: `Icons.Outlined.Explore`
  3. Shell icon: `R.drawable.scyra_turtle` image button
  4. Notepad icon: `Icons.Outlined.EditNote`
  5. Help icon: `Icons.Outlined.HelpOutline`
- Selection styling: selected icons get a rounded capsule background and semantics role `Tab`.
- Shell icon is centered between Paths and Notepad and invokes `onOpenShell` instead of changing Home page.

### Shell chrome

- `ShellTopBar.kt` is separate from the Home top app bar.
- `ShellRootScreen.kt` owns Shell-specific top/header/back behavior and room state. TODO: verify exact Shell top-bar icon ordering before iOS visual implementation.

### Score placement

- Score is not a global top app bar item. It appears in Story header/cards/reward contexts through `ScoreDisplay.kt` and related Story components.

### iOS parity requirements

- Preserve custom Scyra title/brand treatment; a plain native `NavigationBar` title may dilute the design.
- Use a custom SwiftUI header/top bar for the root if needed.
- Preserve Home icon ordering unless product explicitly changes it.
- Keep Shell turtle action visually distinct from root page tabs.
- Keep score display in Story/reward contexts, not global chrome, unless a later design decision changes it.

## 7. Home / Horizon / Dashboard Screen Map

### Current Android naming

- Route: `home_screen`.
- Screen: `SkillzHomeScreen.kt`.
- Current implementation name: Home / `SkillzHomeScreen`.
- Desired product naming: Horizon may be a future/product name because strings mention “Plans on your Horizon,” but current screen is Home. TODO: confirm whether iOS root should be called Home, Horizon, or another name.

### Structure and sections

- `SkillzHomeScreen` uses a `Scaffold` with `SkillzTopAppBar`.
- It maintains an internal selected page (`PAGE_STORY`, `PAGE_PATHS`, `PAGE_NOTEPAD`, `PAGE_HELP`) through `LocalSkillzHomeNav`.
- Major internal pages:
  - Story: `StoryScreen`
  - Paths: `PathsScreen`
  - Notepad: `NotepadScreen`
  - Help/settings: `HelpScreen`
- Entry points:
  - Flow: `onAddSessionClick`, active Flow CTA, planned Flow launch.
  - Pulse: `onAddPulseClick` from Story/Home page flow.
  - Shell: top bar turtle icon `onOpenShell`.
  - Paths/Arcs/plans: top bar Paths icon and `PathsScreen` callbacks including Plan Arc, Arc detail, Suggested Route detail.
  - Help/settings: top bar Help icon/internal Help page.
- Active Flow return behavior: Home receives `isFlowModeOn` and `onGoToActiveSession`, navigating to Flow route.
- Story integration: Story is the default/root page and contains history/Chronicle content.
- Empty/loading states: primarily handled inside child pages such as Story and Paths. TODO: inventory exact Home-level loading states if any.
- Associated ViewModels/state: `StoryViewModel` is provided by NavHost; `NotepadViewModel` is obtained in Home; `PathsScreen` obtains/uses `PathsViewModel` internally.

### iOS parity requirements

- Root dashboard should support the same user journeys: Story/history, Paths/plans, Notepad, Help/settings, Shell, start Flow, record Pulse, return to active Flow.
- Preserve Scyra visual hierarchy and top bar brand tone.
- Do not rename Home/Horizon/Story concepts without product confirmation.

## 8. Flow Navigation Map

### Flow entry paths

| Entry path | Android source | Route builder / args | ViewModel consumption | Expected UI state | iOS payload |
|---|---|---|---|---|---|
| Direct Start Flow | Story/Home `onAddSessionClick` in `SkillzNavHost` | `SkillzDestinations.addSkillRoute()` | `FlowViewModel` sees no prefill args | Blank/new Flow or restored ongoing Flow | `FlowLaunchContext.newRegular` |
| Return to active Flow from Home | `onGoToActiveSession` | `addSkillRoute()` | Repository ongoing session restoration | Existing active Flow state | `FlowLaunchContext.resumeActive` |
| Prefilled planned Flow | `PathsScreen` → `onOpenPlannedFlow` | `addSkillRoute(prefillJourney, prefillTitle, prefillSoftMode)` | Reads Journey/title/soft mode from `SavedStateHandle` | Flow fields prefilled | `FlowLaunchContext.plannedFlow(title, journey, soft)` |
| Launch from Arc Detail | `ArcDetailScreen` begin callback | `addSkillRoute(prefillJourney, prefillTitle, prefillSoftMode, plannedArcTitle, plannedArcStepIndex, plannedArcTotalSteps)` | Reads planned Arc metadata and prefill fields | Flow starts with Arc pill/progress context | `FlowLaunchContext.arcStep(...)` |
| Launch from Suggested Route | `SuggestedRouteDetailScreen` begin callback | same planned Arc args as Arc detail | Reads planned Arc metadata | Flow starts from suggested route step context | `FlowLaunchContext.suggestedRouteStep(...)` |
| Launch from Pulse / Idea Grove | `ShellRootScreen` `onLaunchFlowFromPulse` | `addSkillRoute(prefillJourney, prefillTitle, originPulseId)` | Reads `originPulseId`, title, Journey | Flow prefilled from Pulse and can link back to Pulse | `FlowLaunchContext.fromPulse(id,title,journey)` |
| Launch for Journey from Shell | `ShellRootScreen` `onLaunchFlowForJourney` | `addSkillRoute(prefillJourney = journeyName)` | Reads Journey prefill | Flow Journey prefilled | `FlowLaunchContext.journey(journey)` |
| Deep link / notification | Manifest + `AliveFlowNotificationFactory` | `skillz://flow` → Flow route | Restores ongoing session | Active Flow visible | `FlowLaunchContext.deepLinkResume` |
| Soft Flow | Flow UI toggle or planned Flow soft mode | `prefillSoftMode=true` or local toggle | `FlowViewModel` `prefillSoftModeOverride` and `setSoftMode` | Soft Flow UI/copy, no score/Arc/Surge | `FlowLaunchContext(..., softMode: true)` |

### Flow lifecycle navigation behavior

- Pause/resume: local Flow UI state and persisted ongoing session; no app route change.
- Complete/save: `FlowViewModel` saves a `SessionEntity`, computes rewards, updates `lastReward`; `FlowScreen` opens reward reveal dialog when `lastReward` becomes non-null.
- Cancel/discard: `FlowScreen.onCancel` passed by NavHost calls `popToHome`; draft discard behavior is in `FlowViewModel`. TODO: verify exact unsaved discard prompts.
- Reward reveal transition: local `AlertDialog` containing reward content/deck, not a route.
- Post-completion destination: `onDone` eventually calls `popToHome`; reward dialog actions can open Shell or trigger Arc continuation dialog before exit.
- Flow details/edit path from Story: `FlowDetailsSheet.kt` is opened from Story/Chronicle card context and supports notes/pulses editing. TODO: verify whether title/Journey edits are exposed.

## 9. Reward Reveal / Flow Completion Navigation

- Location: `FlowScreen.kt` owns reward reveal visibility via `showPointsDialog` and `lastReward` from `FlowViewModel`.
- Type: local `AlertDialog` with custom reward content/card deck, not a full route.
- Files:
  - `FlowScreen.kt`
  - `ui/screen/flow/reward/RewardRevealDeck.kt`
  - `RewardRevealMapper.kt`
  - `RewardRevealText.kt`
  - `SessionRewardContent.kt`
  - `SoftSessionRewardContent.kt`
  - `ArcSummaryContent.kt`
  - `RewardChips.kt`
  - `RewardUX.kt`
- Regular Flow reward UI: shows Scyra Score breakdown, Pearls, animals/creatures, badges, Shell bridge cards, and Arc-related cards where applicable.
- Soft Flow reward UI: uses Stillwater result/ripple style and copy that Soft Flow is part of Story but does not affect score/Surge/Beam/Arc progression.
- Arc summary reward UI: `ArcSummaryContent` and reward mapper include Arc score/animals/badges/Stillwater/story placeholder/Shell bridge cards.
- Movement delayed update behavior: current reward reveal can show current movement contribution if available; delayed Health refresh updates persisted reward breakdown/snapshots later. TODO: verify whether delayed movement updates have user-visible notification/inlay.
- Continue/complete/dismiss:
  - Dismiss/complete can clear reward and return Home.
  - If Arc idea continuation is pending, Flow can show an additional continuation dialog.
  - Reward actions may open Shell.
- iOS parity:
  - Implement a custom reward reveal surface/card deck, not a generic alert.
  - Keep reward reveal as local modal/state unless route restoration requires a dedicated route later.
  - Exact reward math belongs in `docs/06_REWARD_AND_ECONOMY_SPEC.md`; this document specifies screen behavior.

## 10. Pulse Navigation Map

### Android behavior

- App-level route: `add_pulse` → `PulseScreen`.
- Create entry from Home/Story: `onAddPulseClick` navigates to `ADD_PULSE_ROUTE`.
- Create while Flow active: `PulseScreen` receives `isFlowStateActive`; strings support “Attach to current Flow.” `FlowScreen` also has an in-Flow Pulse dialog (`showPulseDialog`) for recording a Pulse during Flow.
- Attach Pulse to active Flow: `FlowViewModel.recordPulse` can attach to current Flow when requested.
- Edit Pulse from Story: `PulseEditSheet.kt` and `StoryViewModel.updatePulse`.
- Delete Pulse: `StoryViewModel.deletePulse`; Idea Grove also supports delete with `IdeaGroveDeleteDialog`.
- Pulse details/edit sheets: Story uses local sheets/dialog state, not NavHost routes.
- Pulse-to-Flow launch from Idea Grove: `ShellRootScreen` passes `onLaunchFlowFromPulse` to Idea Grove, which navigates to Flow with `originPulseId`, title, and Journey.
- Journey relationship: Pulse has optional `tagId` and Journey fields in create/edit UI.
- Idea Grove relationship: Pulse `groveStatus` drives Alive/Insight/Completed lists/actions.

Important product rules:

- Pulse is not Flow.
- Pulse creation does not directly award Scyra Score or Pearls.

### iOS parity

- Pulse capture should be lightweight.
- Pulse edit/details can be native sheets/dialogs where appropriate.
- Do not add reward navigation for Pulse creation.
- Provide typed `PulseLaunchContext` for create, attach-to-active-flow, edit, and from-Idea-Grove cases.

## 11. Story / Chronicle Navigation Map

- Main files: `StoryScreen.kt`, `StoryBody.kt`, `StoryViewModel.kt`.
- Cards:
  - Flow cards: `chronicle/FlowCard.kt`
  - Pulse cards: `chronicle/PulseCard.kt`
  - Arc groups: `chronicle/ArcGroupCard.kt`
- Time filters/date navigation: `PeriodAndDateNavigator.kt`, Story period/anchor state in `StoryViewModel`.
- Journey filters: `TagFilterRow.kt`, `StoryViewModel.selectedTagIds`.
- Score display: `ScoreDisplay.kt`, score visibility controlled by `UserPrefs.showScoreUi` / flavor default.
- Flow details sheet: `header/FlowDetailsSheet.kt`; used to inspect Flow details, notes, and Pulses.
- Pulse edit/detail sheet: `header/PulseEditSheet.kt`; used to edit Pulse title/description/Journey.
- Tab behavior: Story header uses segmented/tabs components such as `SegmentedIconTab.kt`; exact tab labels/behavior should be verified before iOS implementation.
- Saga/Journey detail behavior: `saga/` and `saga/journeys/` files show journey summary/detail and `ViewJourneysBottomSheet.kt`.
- Editing/deleting: `StoryViewModel` supports session description update, Pulse create/update/delete, session delete.
- Empty states: `EmptyChroniclesState.kt`, `EmptySagasState.kt`, `FirstTimeUser.kt`.

### iOS parity

- History/Chronicle should support the same inspection/edit paths.
- Use local sheets for Flow details and Pulse edit unless a deep-linkable detail route is required.
- Mark naming conflict as open: Story vs Chronicle vs Home vs Horizon. TODO: product naming decision.

## 12. Paths / Plans / Arc Navigation Map

### Current Android behavior

- `PathsScreen.kt` is not an app-level route; it is an internal page inside `SkillzHomeScreen` selected by the Paths top-bar icon.
- Planned Flows:
  - `PathsScreen` has `PlanFlowSheet` as a `ModalBottomSheet` for creating planned Flows.
  - Planned Flow cards can launch Flow through `onOpenFlowPlan(title, tagName, isSoftMode)`.
  - Menus/dialogs support pin/archive/delete/dreams actions. TODO: verify exact labels.
- Arc planning:
  - `plan_arc?editArcPlanId={editArcPlanId}` app route opens `PlanArcScreen`.
  - `PlanArcViewModel` reads `editArcPlanId` and drives wizard state.
  - Plan Arc supports identity, choosing flows, route shape, timing/recurrence/custom days, save/edit.
- Arc detail:
  - `arc_detail/{arcPlanId}` opens `ArcDetailScreen`.
  - `ArcDetailViewModel` reads `arcPlanId` and can add/remove from Studio, begin/restart Arc, or edit Arc.
- Suggested Route detail:
  - `suggested_route_detail/{suggestedRouteId}` opens `SuggestedRouteDetailScreen` using `SuggestedRoutesCatalog.getById(routeId)`.
  - Can save route as Arc or begin route by launching Flow with planned Arc args.
- Recurrence/custom days: stored in Arc plan entities and handled by Plan Arc UI/ViewModel.

### iOS parity

- Use typed routes for plan list/root page, Arc create/edit, Arc detail, and Suggested Route detail.
- Use a typed `FlowLaunchContext` for planned Flow and Arc contexts.
- Decide whether Paths is a root tab/page or standalone route in iOS.

## 13. Shell Navigation Map

### Current Android behavior

- App route from NavHost: `shell` → `ShellRootScreen`.
- Shell internal navigation file: `ShellDestination.kt`.
- Shell destinations:
  - `Heart`
  - `Focus`
  - `Stillwater`
  - `ShellChest`
  - `Badges`
  - `VoyagePreview`
  - `TheBluePreview`
  - `IdeaGrovePreview`
  - `LookoutPreview`
- Shell rooms are internal state destinations, not app-level NavHost routes.
- `ShellRootScreen.kt` owns room selection, Pearl display, Heart/root room, room previews/full rooms, Chest/Badges shortcuts, notification inlay/screen hooks, and contextual actions.
- Back behavior:
  - From Shell route: `onBack` pops app NavHost.
  - Within Shell: changing `destination` local state returns/moves between rooms. TODO: verify exact per-room back-to-heart behavior and top-bar button behavior.
- Entry points from Shell back into Flow:
  - `onLaunchFlowForJourney` navigates Flow with prefilled Journey.
  - `onLaunchFlowFromPulse` navigates Flow with Pulse title/Journey/origin ID.
  - `onOpenActiveFlow` navigates Flow without args.
- Modals/sheets inside Shell are local state or ViewModel state, not app routes.

### iOS parity

- Use `AppRoute.shell` for entering The Shell.
- Use internal `ShellRoute` enum/state for Shell rooms.
- Do not force every room into app-level `NavigationStack` unless later needed for deep links.
- Preserve The Shell as a hub/room experience.

## 14. Shell Room Screen Map

| Room/destination | Android files | `ShellDestination` | User-facing name | Purpose | Entry points | Major UI/actions | Transient UI | Data dependencies | iOS route | Priority |
|---|---|---|---|---|---|---|---|---|---|---|
| Heart/root | `ShellRootScreen.kt` | `Heart` | Heart Room / The Shell | Shell hub/resting chamber | Enter `shell`; back from rooms | Room orbit nodes, Pearl basin, Chest/Badges shortcuts, Shell welcome | Heart detail sheet, Pearl basin sheet | `ShellUiState`, Pearl balance, room state, active creatures | `ShellRoute.home` | MVP |
| The Blue | `rooms/blue/*`, `TheBlueModels.kt` | `TheBluePreview` | The Blue | Ocean creature collection | Heart room node, Chest empty CTA, reward Shell bridge | Zone/depth pages, creature tray/tile/detail, release, Beyond Blue | Animal detail sheet, release confirmation, Beyond Blue sheet/dialog | Shell ownership, creature catalog, Pearls | `ShellRoute.theBlue` | MVP |
| The Chest | `inventory/ShellChestScreen.kt` | `ShellChest` | The Chest | Creature-only inventory | Heart shortcuts, The Blue detail, empty CTA | Level-aware grid, stack detail, level up, release | Stack detail sheet-like content, level-up confirmation, release confirmation | Active chest creatures, Pearls | `ShellRoute.chest` | MVP |
| Badges | `inventory/BadgesScreen.kt` | `Badges` | Badges | Countable achievement records | Heart shortcuts | Badge groups/counts/objective badges | None obvious; TODO verify empty state | `UserBadgeEntity`, objective completions | `ShellRoute.badges` | MVP |
| Stillwater | `rooms/stillwater/StillwaterRoomScreen.kt` | `Stillwater` | Stillwater | Soft Flow drops and vessel draws | Heart room node | Drop progress, draw vessels, reveal creatures | Draw confirmation/reveal state via `ShellViewModel` | Stillwater ledger/preference, Shell ownership | `ShellRoute.stillwater` | Phase 2/MVP if Soft Flow rewards included |
| Idea Grove | `rooms/ideagrove/IdeaGroveScreen.kt` | `IdeaGrovePreview` | Idea Grove | Pulse idea room | Heart node | Sort, expand, mark insight/completed/revive, delete, launch Flow | Delete dialog, dropdown menus, snackbars | Pulses, ongoing Flow state | `ShellRoute.ideaGrove` | MVP/Phase 2 |
| The Lookout | `rooms/lookout/LookoutRoomScreen.kt` | `LookoutPreview` | The Lookout | Journey objectives/goals | Heart node | Set objectives, period tabs, progress, claim reward, remove/skip | Set objective dialog, date picker, reward dialog, remove dialog | Objectives, sessions, Journeys, Pearl/badge repos | `ShellRoute.lookout` | Phase 2 |
| Voyage Hall | `rooms/voyage/VoyageHallScreen.kt` | `VoyagePreview` | Voyage Hall | Stats/analytics | Heart node | Stats cards/records | Voyage record popup | Flow sessions/Journeys/stats calculator | `ShellRoute.voyageHall` | Later/Phase 2 |
| Focus Room | `rooms/focus/FocusRoomScreen.kt`, `FocusRoomModels.kt`, `FocusExerciseVoiceGuide.kt` | `Focus` | Focus Room | Guided focus exercises | Heart node | Exercise list, ready/player/completion, voice controls | Player/completion states in-screen; voice unavailable card | Local exercise models/TTS | `ShellRoute.focusRoom` | Phase 2 |
| Shell notifications | `inventory/ShellNotificationsScreen.kt`, notification inlay helpers | TODO: internal, not listed in `ShellDestination` | Shell Notifications | New rewards/badges notifications | Heart/root inlay/shortcuts | Mark viewed/all viewed | Inlay/screen | Unviewed finds/badges/discoveries | `ShellRoute.notifications` if needed | MVP/Phase 2 |

## 15. The Blue Navigation and Modal Map

- Zone/depth navigation: The Blue uses depth/zone UI (`TheBlueDepthRail.kt`, `TheBlueZonePage.kt`) for Sunlit Reef, Deeper Reef, Open Blue, Great Blue.
- Creature tray/tile/detail: `TheBlueCreatureTray.kt`, `TheBlueCreatureTile.kt`, `TheBlueAnimalDetailSheet.kt`.
- Animal detail sheet: `TheBlueAnimalDetailSheet` is a `ModalBottomSheet` opened from selected creature state.
- Release confirmation: `ReleaseCreatureConfirmationSheet.kt` handles creature release confirmation/reward preview from The Blue context.
- Beyond Blue encounter: `BeyondBlueEncounterSheet.kt` is a `ModalBottomSheet` with selection/trade state and internal `AlertDialog` confirmation.
- Growth/level-up: The Blue detail sheet exposes level-up if eligible; actions go through `ShellViewModel.growCreatureByLevel` / repository.
- Creature placement/chest return: Shell/Blue supports display in Focus and resting in Chest counts/actions; `ShellViewModel.place`, `returnToChest`, and `markTheBlueAnimalsSeen` exist. TODO: verify exact UI wording for placement/chest return.
- Pearl spend/receive: growth and Beyond Blue spend Pearls; release receives Pearls.
- Empty states: strings include “No animals yet” and CTA to start Flow; The Blue empty water caption shown when no creatures/zones.
- Scene overlay behavior: `TheBlueOverlaySurface.kt`, `TheBlueSceneSafeBounds.kt`, procedural draw files and rendered creature components.

### iOS parity

- SwiftUI room with creature scene and local sheets/dialogs.
- Release confirmation should preserve product copy and reward preview.
- Level-aware creature interactions should align with The Chest.
- Keep creature scene local to iOS assets/code; do not reference Android resources.

## 16. The Chest Navigation and Modal Map

- Screen file: `ui/screen/shell/inventory/ShellChestScreen.kt`.
- Inventory grid: uses `LazyVerticalGrid`-style stack tiles. TODO: verify exact grid columns/responsiveness before iOS UI spec.
- Level-aware stacks: `buildChestInventoryStacks` groups active creatures by `(findId, animalLevel)`.
- Stack detail: selected stack local state opens detail content with source, owned count, level, cost, release preview.
- Release selection/confirmation: stack detail uses selected count and `ChestReleaseConfirmationDialog`.
- Level-up action: `ChestLevelUpConfirmationDialog` confirms cost and levels one creature in the selected level stack.
- Count badge: shown only when count > 1, top-right style.
- Level badge: level appears on tile, bottom/overlay style.
- Empty state: prompts user to go to The Blue.
- Filters/sorting: stacks sorted by creature name and level; no user filter controls observed. TODO: verify if sorting should be user-configurable.
- Navigation back to Shell: internal Shell destination state/back affordance.
- Relationship to The Blue: Chest contains resting active creatures from The Blue; The Blue details can show resting counts and view in Chest.

### iOS parity

- UI name must be “The Chest”.
- Creature-only inventory: no trinkets/objects, no released creatures.
- RuneScape-bank-like icon grid direction.
- Level at bottom, count top-right.
- Use level-aware grouping exactly.

## 17. Badges Navigation Map

- Screen file: `ui/screen/shell/inventory/BadgesScreen.kt`.
- Entry point: Shell Heart shortcut / `ShellDestination.Badges`.
- Badge groups:
  - Objective badges grouped by Journey/objective completions.
  - Catalog badges excluding objective badge IDs.
- Count display: row title shows badge title and count.
- Viewed/new state: persisted in `UserBadgeEntity`; Shell notifications/inlay use viewed state. `BadgesScreen` itself primarily displays earned badges/counts.
- Mastery badge expectations: product spec requires countable species Mastery badges for each individual Level 99 creature; Android evidence currently confirms max level/tier but needs verification of species-specific badge award path.
- Empty state: TODO: verify if BadgesScreen has a no-badges empty state.

### iOS parity

- Badges screen should be part of Shell internal navigation.
- Preserve countable badges.
- Preserve Creature Mastery counts independent of active creature inventory.

## 18. Focus Room Navigation and Player Flow

- Files: `FocusRoomScreen.kt`, `FocusRoomModels.kt`, `FocusExerciseVoiceGuide.kt`.
- Exercise selection: list of original exercises: Three-Point Grounding, Box Breathing, Mini Body Scan, 4-7-8 Breathing, Five Senses Reset.
- Ready/start state: selecting an exercise shows a ready card; user taps Start guided exercise.
- Player state: current exercise, current step index, elapsed/remaining time, visual state, and voice controls.
- Pause/resume/restart/end: `FocusRoomScreen` local functions handle exercise player state.
- Completion state: completion content is shown after exercise ends.
- Voice toggle/guide: Android TextToSpeech guide with unavailable fallback card; gentle voice selection attempts in `FocusExerciseVoiceGuide.kt`.
- Phase timers: breathing exercises show phase countdown pills for breath phase steps.
- Dialog/sheet behavior: no app-level route; player/completion are in-screen state. TODO: verify any confirmation on End/back.
- Back behavior: likely Shell top/back returns to Shell room/hub; player state may reset. TODO: verify exact back behavior during active exercise.

### iOS parity

- Focus Room should not auto-start; Android requires a Start action.
- Voice/audio implementation can be TODO: product decision for AVSpeechSynthesizer vs bundled audio vs no voice in MVP.
- Focus Room exercises do not award rewards/stats.

## 19. Help / Settings / Permissions Navigation

- Help screen: `HelpScreen.kt`, accessible as Home internal Help page and app-level `help` route declaration.
- Notification permission gate: `NotificationPermissionGate.kt` rendered at app root inside `SkillzApp`; invokes `onPermissionGranted` to reinstate Flow notification.
- Health settings UI: `ui/health/HealthComponents.kt`, `HealthSettingsViewModel.kt`, embedded in Help.
- Score/calm/language settings: `HelpScreen` with `StoryViewModel.setShowScoreUi`, `setCalmMode`, `setAppLanguage`; persistence via `UserPrefs`.
- Health Connect install/update/permission flows: `HealthSettingsViewModel` can launch Health Connect install/update and handles permission results; manifest includes Health permission aliases.
- Movement bonus enable/disable: Health settings repository and ViewModel; disabling may show `DisableHealthPendingFlowsDialog`.
- Settings dialogs: Health disable confirmation; health permission UI; TODO verify any language confirmation dialog.
- App language behavior: `SkillzApplication` applies saved language on startup through `AppLocaleManager`.

### iOS parity

- Notification permission uses iOS authorization APIs and native copy.
- HealthKit permission has separate iOS flow and cannot reuse Health Connect assumptions.
- Score visibility setting applies to Scyra/Aera product decisions.
- Language settings require an iOS localization strategy later.

## 20. Dialogs, Sheets, Popups, and Inlays Inventory

| Name | File path | Owning screen | Type | Trigger | User actions | Dismissal | Data affected | iOS equivalent |
|---|---|---|---|---|---|---|---|---|
| Notification permission gate | `NotificationPermissionGate.kt` | Root `SkillzApp` | permission gate | App start / missing POST_NOTIFICATIONS | grant/continue | permission result | Android notification permission | iOS notification permission prompt/gate |
| Flow soft Arc confirm | `FlowScreen.kt` | Flow | dialog | switching to Soft Flow while in Arc context | confirm/stay | button/dismiss | Flow mode/Arc continuation | confirmation dialog |
| Flow in-session Pulse dialog | `FlowScreen.kt` | Flow | dialog | add/record Pulse while in Flow | save/cancel | button/dismiss | Pulse repository/Flow link | lightweight Pulse sheet/dialog |
| Flow end confirm | `FlowScreen.kt` | Flow | dialog | exit/complete Flow | complete/cancel | button/dismiss | session save/reward | confirmation dialog |
| Flow reward reveal deck | `FlowScreen.kt`, `reward/*` | Flow | card deck inside dialog | `lastReward` set after save | continue/open Shell/finish | buttons | reward presentation; maybe clears reward state | custom reward modal/card deck |
| Arc idea continuation dialog | `FlowScreen.kt` | Flow | dialog | pending Arc/Pulse continuation after reward | continue/abandon | button | Arc/Pulse continuation state | confirmation dialog |
| Surge target dialog | `FlowScreen.kt`, `SurgeMiniControl.kt` | Flow | dialog | long press/edit Surge | set/clear/cancel | button/dismiss | Surge planned target | custom dialog |
| Stopwatch reset confirm | `StopwatchSection.kt` | Flow | dialog | reset stopwatch | reset/cancel | button/dismiss | timer state | confirmation dialog |
| Flow details sheet | `FlowDetailsSheet.kt` | Story | sheet/bottom sheet-like | tap Flow card/details | edit notes/add Pulse/close | close/dismiss | session description/pulses | SwiftUI sheet |
| Pulse edit sheet | `PulseEditSheet.kt` | Story | sheet | edit Pulse | save/delete/cancel | close/dismiss | Pulse fields | SwiftUI sheet |
| Journey details sheet | `ViewJourneysBottomSheet.kt` | Story/Saga | sheet | view Journey details | inspect/close | dismiss | none or filters | SwiftUI sheet |
| Plan Flow sheet | `PathsScreen.kt` | Paths | modal bottom sheet | Plan Flow | save/cancel fields | dismiss/save | FlowPlanEntity | SwiftUI sheet |
| Planned Flow menu/delete | `PathsScreen.kt` | Paths | dropdown/dialog | card menu delete/archive/pin | menu actions/confirm | dismiss | FlowPlanEntity | menu + confirmation dialog |
| Plan Arc wizard screens | `PlanArcScreen.kt` | Plan Arc route | routed screen with local steps | create/edit Arc | next/back/save | route back/done | Arc plan/steps | route with internal wizard state |
| Idea Grove delete dialog | `IdeaGroveScreen.kt` | Idea Grove | dialog | delete Pulse | confirm/cancel | button/dismiss | Pulse deletion | confirmation dialog |
| Idea Grove item menu | `IdeaGroveScreen.kt` | Idea Grove | dropdown/snackbar | item overflow/status actions | mark insight/completed/revive/delete | menu dismiss/snackbar | Pulse grove status | menu + snackbar |
| Blue animal detail | `TheBlueAnimalDetailSheet.kt` | The Blue | modal bottom sheet | tap creature | grow/display/view/release | dismiss | creature/placement maybe | SwiftUI sheet |
| Blue release confirmation | `ReleaseCreatureConfirmationSheet.kt` | The Blue | sheet/dialog | release action | confirm/cancel | dismiss | creature status/Pearls | confirmation sheet/dialog |
| Beyond Blue encounter | `BeyondBlueEncounterSheet.kt` | The Blue | modal bottom sheet + dialog | Beyond Blue action | select creatures/pay Pearls/confirm | dismiss | creature status/Pearls/new creature | sheet + confirmation dialog |
| Chest stack detail | `ShellChestScreen.kt` | The Chest | local detail sheet/content | tap stack | level up/release/close | close | selected stack | SwiftUI sheet/detail |
| Chest level-up confirmation | `ShellChestScreen.kt` | The Chest | dialog | level up | confirm/cancel | button/dismiss | creature level/Pearls | confirmation dialog |
| Chest release confirmation | `ShellChestScreen.kt` | The Chest | dialog | release selected count | confirm/cancel | button/dismiss | creature status/Pearls | confirmation dialog |
| Shell notification inlay/screen | `ShellNotificationsScreen.kt`, `ShellRootScreen.kt` | Shell | inlay/screen | unseen rewards/badges | mark viewed/open | action/back | viewed flags | inlay or route/sheet |
| Lookout set objective | `LookoutRoomScreen.kt` | Lookout | dialog | set objective | save/cancel/date | dismiss | ObjectiveEntity | form dialog/sheet |
| Lookout date picker | `LookoutRoomScreen.kt` | Lookout | date picker dialog | start date select | choose/cancel | dismiss | dialog state | SwiftUI date picker sheet/dialog |
| Lookout reward dialog | `LookoutRoomScreen.kt` | Lookout | dialog | claim/complete objective reward | dismiss | dismiss | Pearl/badge claim display | reward dialog |
| Lookout remove dialog | `LookoutRoomScreen.kt` | Lookout | dialog | remove/skip objective | delete/stop/skip/cancel | dismiss | Objective/archive/skips | confirmation dialog |
| Voyage record popup | `VoyageHallScreen.kt` | Voyage Hall | popup | tap stats record | inspect/dismiss | dismiss | none | popover/sheet |
| Focus player/completion | `FocusRoomScreen.kt` | Focus Room | in-screen modal-like state | start exercise | pause/resume/restart/end | end/back | local exercise state | in-screen player state |
| Voice unavailable card | `FocusRoomScreen.kt` | Focus Room | inlay/card | TTS unavailable | retry/follow on screen | retry/state | TTS state | inline card |
| Health disable pending flows | `HealthComponents.kt` | Help/Health | dialog | disable movement with pending flows | disable/keep on | button/dismiss | movement setting | confirmation dialog |
| Soundscape picker | TODO: verify | TODO | TODO popup/dialog | TODO | TODO | TODO | TODO | If implemented, use popup/dialog, not bottom sheet |

## 21. End-to-End User Flow Maps

### First launch to Home

- Start point: Android launcher.
- Screens/routes: `MainActivity` → splash theme → `SkillzApp` → `NotificationPermissionGate` + `SkillzNavHost` → `home_screen` / `SkillzHomeScreen`.
- Args/state: none; `ongoingSession` observed.
- Modals: notification permission gate may appear.
- End state: Home/Story page visible.
- iOS parity: launch screen → root SwiftUI app → optional notification prompt → Home.

### Start regular Flow from Home

- Start: Home/Story page.
- Routes: `home_screen` → `add_skill`.
- Args: no prefill args.
- Modals: Flow may show Surge/Pulse/end dialogs during use.
- End: active Flow screen.
- iOS parity: `AppRoute.flow(.newRegular)`.

### Pause/resume/complete Flow

- Start: `FlowScreen`.
- Routes: no route changes while pausing/resuming.
- State: `OngoingSessionEntity` and `FlowUiState` update.
- Complete: end confirmation → save → reward reveal dialog.
- End: reward shown or Home after dismiss/done.
- iOS parity: local state + persisted timestamps; no background timer dependency.

### Complete Flow and view rewards

- Start: active Flow.
- Routes: still `add_skill`.
- Modals: end confirm → reward reveal deck dialog → optional Arc continuation dialog or Shell open.
- End state: Home or Shell or next Flow context.
- iOS parity: custom reward reveal modal/card deck.

### Create Pulse from Home

- Start: Home/Story.
- Routes: `add_pulse`.
- Args: none.
- Modals: Pulse screen itself; no reward reveal.
- End: save pops to Home.
- iOS parity: lightweight Pulse creation route/sheet.

### Create Pulse while Flow is active if supported

- Start: Flow active.
- Routes: no app route change if using in-Flow Pulse dialog; `add_pulse` route also receives `isFlowStateActive` from NavHost if opened.
- State: attach-to-current-flow supported.
- End: Pulse saved and may appear under Flow/Chronicle.
- iOS parity: in-Flow Pulse sheet/dialog with attach option.

### Launch Flow from Pulse / Idea Grove if supported

- Start: Shell → Idea Grove.
- Routes: `shell` internal `IdeaGrovePreview` → app route `add_skill`.
- Args: `originPulseId`, `prefillTitle`, `prefillJourney`.
- End: Flow prefilled from Pulse.
- iOS parity: `FlowLaunchContext.fromPulse`.

### Create planned Flow / Arc and launch it

- Start: Home → Paths page.
- Routes: Paths internal page; Plan Flow uses modal sheet; Plan Arc uses `plan_arc` route; Arc detail uses `arc_detail/{arcPlanId}`.
- Args: `editArcPlanId` for edit; `arcPlanId` for detail.
- Modals: Plan Flow sheet; Arc wizard screen.
- End: saved plan/Arc or Flow launched with prefill.
- iOS parity: plan page + `AppRoute.planArc(editId:)` + Flow launch payload.

### Complete Arc step and continue Arc

- Start: Arc Detail or Suggested Route Detail → Flow.
- Routes: `arc_detail/{id}` or `suggested_route_detail/{id}` → `add_skill` with planned Arc args.
- Args: planned Arc title/step index/total plus Flow title/Journey/soft mode.
- Modals: reward reveal; optional continuation dialog.
- End: next Flow or Home/Arc state updated.
- iOS parity: persist active Arc run and route payload.

### Enter The Shell

- Start: Home top app bar turtle or Flow reward/open Shell.
- Routes: `shell`.
- State: Shell internal `Heart` default.
- Modals: Heart/Pearl/notification inlays as applicable.
- End: Shell hub visible.
- iOS parity: `AppRoute.shell(initial: .home)`.

### Open The Blue and view/release creature

- Start: Shell Heart.
- Routes: internal `ShellDestination.TheBluePreview`.
- Modals: animal detail sheet → release confirmation sheet/dialog.
- State: creature status/Pearl ledger updates on release.
- End: The Blue updated or creature removed/released.
- iOS parity: `ShellRoute.theBlue` + local sheets.

### Open The Chest and manage creature stack

- Start: Shell Heart or The Blue.
- Routes: internal `ShellDestination.ShellChest`.
- Modals: stack detail → level-up confirmation or release confirmation.
- State: creature level/status/Pearls.
- End: stack updated; remains in Chest.
- iOS parity: `ShellRoute.chest` creature-only grid.

### Open Badges and view countable badge

- Start: Shell Heart shortcut.
- Routes: internal `ShellDestination.Badges`.
- Modals: none obvious.
- State: displayed badge counts; viewed state may be changed elsewhere.
- End: Badges screen visible.
- iOS parity: `ShellRoute.badges`.

### Use Soft Flow and view Stillwater result

- Start: Flow screen with Soft Flow enabled.
- Routes: `add_skill`.
- Modals: Flow end confirm → reward reveal with Soft/Stillwater content.
- State: Soft session saved, Stillwater drops ledger updated.
- End: reward dismissed to Home or Shell.
- iOS parity: Soft Flow reward reveal and Stillwater ledger.

### Open Lookout and claim objective reward

- Start: Shell Heart.
- Routes: internal `LookoutPreview`.
- Modals: set objective dialog, date picker, reward dialog, remove dialog.
- State: objective completion/claim, Pearl ledger, badge count.
- End: Lookout updated.
- iOS parity: phase based on MVP scope; preserve claim dialog flow.

### Open Voyage Hall stats

- Start: Shell Heart.
- Routes: internal `VoyagePreview`.
- Modals: Voyage record popup on record tap.
- State: read-only stats derived from sessions/Journeys.
- End: Voyage stats visible.
- iOS parity: stats room when in scope.

### Use Focus Room exercise

- Start: Shell Heart.
- Routes: internal `Focus`.
- Modals/states: exercise selection → ready → player → completion; voice unavailable card if needed.
- State: local exercise state only; no rewards/stats.
- End: completion or back to Focus Room list.
- iOS parity: calm in-screen player; no rewards.

### Enable Movement Points / Health integration

- Start: Home Help page.
- Routes: internal Help page; Health permission flow may leave app to Health Connect/system permission.
- Modals: Health permission UI; disable pending flows dialog if turning off.
- State: movement bonus setting, permissions, pending snapshot refresh.
- End: movement bonus enabled/disabled.
- iOS parity: HealthKit authorization/settings flow.

### Return to active Flow from notification/deep link

- Start: Android notification or deep link `skillz://flow`.
- Routes: `MainActivity` → NavHost deep link → `add_skill`.
- Args: deep link route without prefill; active session loaded from repository.
- Modals: none by default.
- End: active Flow visible.
- iOS parity: notification/deep link opens `AppRoute.flow(.resumeActive)` and reconstructs elapsed time.

## 22. iOS Route Model Recommendations

Recommendation only; do not create iOS code.

### App route enum suggestions

- `AppRoute.home`
- `AppRoute.flow(FlowLaunchContext)`
- `AppRoute.pulse(PulseLaunchContext)`
- `AppRoute.shell(initial: ShellRoute? = nil)`
- `AppRoute.planArc(editId: ArcPlanID?)`
- `AppRoute.arcDetail(id: ArcPlanID)`
- `AppRoute.suggestedRouteDetail(id: SuggestedRouteID)`
- Optional: `AppRoute.helpSettings` only if Help becomes separate from root page.

### Root page suggestions

- `RootPage.story`
- `RootPage.paths`
- `RootPage.notepad`
- `RootPage.help`

### Shell internal route enum suggestions

- `ShellRoute.home`
- `ShellRoute.theBlue`
- `ShellRoute.chest`
- `ShellRoute.badges`
- `ShellRoute.stillwater`
- `ShellRoute.ideaGrove`
- `ShellRoute.lookout`
- `ShellRoute.voyageHall`
- `ShellRoute.focusRoom`
- `ShellRoute.notifications` if Shell notifications become a full screen.

### Payload models

- `FlowLaunchContext`
  - `newRegular`
  - `resumeActive`
  - `plannedFlow(title:journey:softMode:)`
  - `fromPulse(id:title:journey:)`
  - `arcStep(title:journey:softMode:arcTitle:stepIndex:totalSteps:)`
  - `deepLinkResume`
- `PulseLaunchContext`
  - `create`
  - `createAttachedToActiveFlow`
  - `edit(id:)`
- `ArcLaunchPayload`
  - `arcPlanId`
  - `stepIndex`
  - `totalSteps`
  - `title/Journey/softMode` snapshot

### Modal/sheet model suggestions

- Use route-independent modal state for reward reveal, Flow details, Pulse edit, Blue detail, Chest detail, Lookout dialogs, Health confirmations.
- Use app routes only for screens that must be deep-linkable or have independent navigation history.
- Persist active Flow and active Arc restoration state independently from visible route.

## 23. Navigation Risks and Open Questions

| Risk/open question | Why it matters | iOS impact | Recommended follow-up |
|---|---|---|---|
| Home/Horizon/Story/Chronicle naming | Android uses Home/Story/Chronicle/Paths and strings mention Horizon. | Wrong naming can confuse product parity. | Product naming decision before iOS UI copy. |
| Shell internal navigation vs app-level navigation | Android rooms are internal state, but iOS may want deep links. | Over-routing can lose Shell hub feel; under-routing may hurt restoration. | Decide Shell route/deep-link scope. |
| Exact iOS equivalent of Android chrome | Android has custom top bar with page icons and Shell turtle. | Native NavigationBar may dilute Scyra identity. | Design-system/top-bar spec. |
| Android foreground service/deep link vs iOS notifications | Android can keep foreground service; iOS cannot. | Active Flow return must be timestamp-restored. | Flow lifecycle/notification spec. |
| Flow route argument complexity | Flow launch contexts combine planned Flow, Arc, Pulse, Soft Flow. | Raw string args would be brittle in SwiftUI. | Define typed `FlowLaunchContext`. |
| Reward reveal is local state, not route | Android uses dialog/card deck. | iOS restoration/back behavior must be chosen. | Reward reveal UX spec with restoration decisions. |
| Compose vs SwiftUI modal parity | Android has bottom sheets/dialogs/popups/inlays. | Some should become SwiftUI sheets, others custom overlays. | Modal inventory review during iOS design. |
| Legacy Shell destinations/concepts | Shell has ShellFind/object/discovery naming. | Porting legacy concepts can bloat MVP. | Legacy product/data compatibility audit. |
| All Shell rooms first-pass scope | Rooms vary greatly in complexity. | MVP risk if all are included. | iOS MVP/phasing decision. |
| Soundscapes unknown | Product direction says popup/dialog if implemented; source not found. | Could introduce audio assets/UI scope. | Verify soundscape implementation/scope. |
| Focus Room voice/player MVP | Android TTS/player is substantial. | TTS/audio and timers require separate effort. | Focus Room MVP/audio decision. |
| App-level Help route exists but internal Help page is primary | NavHost has `composable("help")` but top bar uses internal page. | Duplicating settings routes could confuse navigation. | Decide if iOS Help is root page or route. |

## 24. Acceptance Criteria for This Document

- This task only creates or changes `docs/03_NAVIGATION_AND_SCREEN_MAP.md`.
- Android source code is untouched.
- No iOS source code or project is created.
- No Gradle/build files are changed.
- The document is based on actual Android navigation/screen files plus prior docs.
- Every top-level Android route is documented or marked `TODO: verify`.
- Every major user-visible screen is mapped or marked `TODO: verify`.
- Shell internal navigation is documented.
- Flow/Pulse/Arc launch paths are documented.
- Dialogs/sheets/popups/inlays are inventoried.
- iOS parity recommendations preserve repo boundary rules.
- Unclear behavior is marked `TODO: verify`.

## 25. Codex Summary

- Docs and Android files/directories inspected: `docs/00_REPO_BOUNDARIES.md`, `docs/01_ANDROID_ARCHITECTURE.md`, `docs/02_SCYRA_PRODUCT_SPEC.md`, manifest/startup files, Navigation Compose files, Home/top bar/help/permission files, Flow/reward files, Story/Pulse files, Paths/Arc files, Shell/room/inventory files, navigation-related ViewModels, and navigation-label resources.
- Top-level routes discovered: `home_screen`, `add_skill?...` / `add_skill`, `add_pulse`, `shell`, `plan_arc?editArcPlanId={editArcPlanId}`, `arc_detail/{arcPlanId}`, `suggested_route_detail/{suggestedRouteId}`, and `help`.
- Major screens mapped: Home/Story/Paths/Notepad/Help, Flow, Pulse, Arc plan/detail/suggested route, The Shell, Heart, The Blue, The Chest, Badges, Stillwater, Idea Grove, The Lookout, Voyage Hall, Focus Room, Shell notifications, Help/Health settings.
- Major dialogs/sheets/popups/inlays discovered: notification permission gate, Flow end/reward/Surge/Pulse/Arc dialogs, Flow details sheet, Pulse edit sheet, Plan Flow sheet, planned Flow menus/delete dialog, Blue detail/release/Beyond Blue sheets, Chest stack/level-up/release dialogs, Lookout dialogs/date picker, Voyage popup, Idea Grove delete/menu/snackbars, Focus player/inlays, Health disable dialog, Shell notification inlay/screen.
- Highest-risk navigation areas for iOS replication: Home/Horizon naming, Shell internal routing, custom Scyra chrome, Flow launch payload complexity, Android service/deep link behavior vs iOS notifications, reward reveal as local state, modal parity, legacy Shell concepts, Shell room MVP scope, soundscape scope, and Focus Room voice/player scope.
- Anything outside `docs/03_NAVIGATION_AND_SCREEN_MAP.md` changed: no.
- Repo boundary rules preserved: yes. The document is reference-only under `docs/`; no Android code/build files were modified and no iOS project/files were created.
