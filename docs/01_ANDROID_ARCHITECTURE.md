# Android Architecture Inventory

## 1. Purpose

This document maps the existing Android implementation so a future SwiftUI iOS app can replicate Scyra accurately without changing Android. It inventories the observed Android product behavior, architecture, persistence, navigation, visual design, platform integrations, reward/economy behavior, and app state handling.

This is documentation only. It does not modify Android source code, Gradle configuration, resources, runtime behavior, or tests.

## 2. Source Files Inspected

Important Android files and directories inspected:

- `android/settings.gradle.kts` — Android Gradle root settings; includes `:app`.
- `android/build.gradle.kts` — top-level Android project plugins.
- `android/gradle/libs.versions.toml` — version catalog for plugins and dependencies.
- `android/app/build.gradle.kts` — Android app module build configuration, flavors, KSP, Room schema export, dependencies, and test setup.
- `android/app/src/main/AndroidManifest.xml` — application class, launcher activity, permissions, services, Health Connect aliases, and deep link.
- `android/app/src/main/java/` — Kotlin source tree for application, data, repositories, models, UI, services, utilities, and ViewModels.
- `android/app/src/main/res/` — Android resources.
- `android/app/src/main/res/values/` — `colors.xml`, `strings.xml`, `themes.xml`, launcher background values.
- `android/app/src/main/res/font/` — contains `caveatsb.ttf`.
- `android/app/src/main/res/drawable/` — launcher foreground/background, Scyra turtle splash/notification drawables, `scyra_turtle.png`.
- `android/app/src/main/res/mipmap-mdpi/`, `mipmap-hdpi/`, `mipmap-xhdpi/`, `mipmap-xxhdpi/`, `mipmap-xxxhdpi/`, `mipmap-anydpi-v26/` — launcher icon assets.
- `android/app/schemas/` — present; contains Room schema JSON for `com.kingkharnivore.skillz.data.model.SkillzDatabase/31.json`.
- `android/app/src/test/` — present; contains unit/domain/UI mapper tests.
- `android/app/src/androidTest/` — present; contains instrumented tests including migration testing.

Expected paths not found:

- No `android/app/src/main/assets/` directory was observed.
- No Android audio resource directory such as `android/app/src/main/res/raw/` was observed.
- No iOS source/project directory was inspected or created.

## 3. Build System and Project Setup

- Gradle root location: `android/`.
- Android app module location: `android/app/`.
- Settings file: `android/settings.gradle.kts`.
- App module build file: `android/app/build.gradle.kts`.
- Namespace: `com.kingkharnivore.skillz`.
- Default `applicationId`: `com.kingkharnivore.skillz`.
- `minSdk`: 26.
- `targetSdk`: 36.
- `compileSdk`: Android API 36 via `compileSdk { version = release(36) }`.
- `versionCode`: 2.
- `versionName`: `1.1`.
- Kotlin/JVM settings: Java source/target compatibility 11 and Kotlin `jvmTarget = "11"`.
- Compose enabled status: `buildFeatures.compose = true`; Kotlin Compose plugin is applied.
- KSP usage: KSP plugin is applied for Hilt and Room compiler processing.
- Room schema export usage: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, `sourceSets.androidTest.assets.srcDir("$projectDir/schemas")`, and `SkillzDatabase(exportSchema = true)`.
- Hilt usage: Hilt Gradle plugin is applied; `hilt { enableAggregatingTask = false }`; app uses `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, and Hilt modules.
- Testing setup: JUnit unit tests; AndroidX JUnit, Espresso, Room testing, Compose UI test JUnit4, Compose debug manifest/tooling.
- Notable `buildConfig` fields:
  - `SHOW_SCORE`: controls whether score UI should be shown by default/flavor.
  - `PRIMARY_COLOR`: used by notification/UI color behavior, including `AliveFlowNotificationFactory`.

Product flavors use flavor dimension `mode`:

### `aera`

- `applicationIdSuffix`: `.aera`.
- `versionNameSuffix`: `-aera`.
- App name: `Aera` via `resValue("string", "app_name", "Aera")`.
- `SHOW_SCORE`: `false`; score UI is hidden by flavor default.
- Primary color: `0xFF3F8F8B` comment-labeled `RavenclawBlue`.
- Other observed behavior: same source set and dependencies as Scyra; flavor differences observed in build config/resource value only.

### `scyra`

- `applicationIdSuffix`: `.scyra`.
- `versionNameSuffix`: `-scyra`.
- App name: `Scyra` via `resValue("string", "app_name", "Scyra")`.
- `SHOW_SCORE`: `true`; score UI is shown by flavor default.
- Primary color: `0xFF2F4F6F` comment-labeled `GryffindorRed`.
- Other observed behavior: same source set and dependencies as Aera; flavor differences observed in build config/resource value only.

## 4. Dependency Inventory

- Jetpack Compose: core UI runtime for nearly all screens under `android/app/src/main/java/com/kingkharnivore/skillz/ui/`; `MainActivity.kt` uses `setContent`, `SkillzTheme`, and `SkillzNavHost`.
- Material 3: primary component library for Compose surfaces, top bars, cards, dialogs, buttons, chips, and theme usage in UI screens/components.
- Navigation Compose: `SkillzNavHost.kt` and `SkillzDestinations.kt` define route strings, arguments, deep link handling, and composable destinations.
- Hilt: app-wide dependency injection through `SkillzApplication.kt`, `MainActivity.kt`, ViewModels, and modules in `data/di/`.
- Room: local database in `SkillzDatabase.kt`; DAOs/entities under `data/model/dao/` and `data/model/entity/`; migrations under `data/model/migration/`; schema exported to `android/app/schemas/`.
- DataStore Preferences: settings and lightweight state in `UserPrefs.kt`, `ArcPrefs.kt`, `NotepadRepository.kt`, and `HealthSettingsRepository.kt`.
- Coroutines/Flow: repositories, ViewModels, services, and DataStore expose/collect `Flow`, `StateFlow`, `MutableStateFlow`, `combine`, and coroutine scopes.
- WorkManager: dependency exists in `android/app/build.gradle.kts`; no concrete `Worker` class was found under `android/app/src/main/java/`. TODO: verify whether dependency is planned or unused.
- Health Connect: steps permission and step aggregation through `HealthConnectClientProvider.kt`, `HealthConnectMovementDataSource.kt`, `HealthPermissionRepository.kt`, `HealthSettingsViewModel.kt`, and related health utilities.
- Lifecycle services/runtime: `lifecycle-service` supports `AliveFlowService`; lifecycle runtime/compose supports lifecycle-aware UI state collection and activity lifecycle work.
- Kotlin datetime: used in date/period calculations, especially Lookout/Voyage/Story time windows. TODO: verify every call site before writing a final date/time porting spec.
- Rich editor library: `com.mohamedrejeb.richeditor:richeditor-compose`; likely used by notepad or rich text editing screens. TODO: verify exact UI usage.
- Splash screen: `androidx.core:core-splashscreen` used by `MainActivity.installSplashScreen()` with `Theme.Skillz.Splash`.
- Vico chart dependencies: `com.patrykandpatrick.vico:compose` and `compose-m3`; likely used for stats/charts. TODO: verify current visible chart call sites.
- Testing dependencies: JUnit, AndroidX test, Espresso, Room testing, Compose UI testing, and Compose debug tooling support unit, instrumented, migration, and UI test coverage.

## 5. Package and Directory Structure

Package root: `com.kingkharnivore.skillz` under `android/app/src/main/java/com/kingkharnivore/skillz/`.

- `MainActivity.kt`, `SkillzApplication.kt`: application and launcher entry points; platform/bootstrap code.
- `data/di/`: Hilt modules (`DatabaseModule`, `HealthModule`, `ShellDatabaseModule`); dependency injection.
- `data/health/`: Health Connect availability/client/step data source/result types; platform integration.
- `data/model/`: Room database, DAOs, entities, migrations, and Shell definitions; persistence/data model.
- `data/repository/`: repository layer for Flow sessions, alive/ongoing sessions, journeys/tags, pulses, plans/arcs, notepad, health, and Shell rooms/economy; data/domain bridge.
- `model/state/`: UI state data classes for Flow, Paths, Plan Arc, Idea Grove, reward reveal, etc.; UI/domain state modeling.
- `model/ui/`: UI list item/summary models such as `FlowListItemUiModel`, `PulseListItemUiModel`, `ChronicleUiModel`; presentation models.
- `ui/health/`: Health UI components; UI/platform integration.
- `ui/model/`: UI runtime state such as `ArcRuntimeState`; UI state support.
- `ui/navigation/`: route definitions and NavHost; navigation.
- `ui/notification/`: `AliveFlowNotificationFactory`; notification construction.
- `ui/screen/`: Compose screens/components for home, help, notepad, Flow, Paths, Story/Pulse, Shell rooms, rewards, and reusable UI; UI.
- `ui/service/`: Android service/controller/runtime/haptics for Alive Flow and Surge; background/platform integration.
- `ui/theme/`: Compose theme, colors, typography; visual design.
- `utils/`: scoring, arc rules/preferences, health reward utilities, localization, Shell economy/reward utilities, time helpers, user preferences; domain utilities and platform-adjacent helpers.
- `viewmodel/`: Hilt ViewModels for Flow, Story, Paths, Plan Arc, Arc Detail, Suggested Route Detail, Notepad, health settings, and Shell rooms; state management.

## 6. Application Entry Points

- Application class: `SkillzApplication.kt` annotated with `@HiltAndroidApp`; injects `UserPrefs` and applies saved language with `AppLocaleManager.applyLanguage` during `onCreate`.
- Main launcher activity: `MainActivity.kt` annotated with `@AndroidEntryPoint`; calls `installSplashScreen()`, `enableEdgeToEdge()`, reinstates flow notification when appropriate, and renders `SkillzApp`.
- Startup flow: Android launches `MainActivity`; Hilt injects repositories/controllers; activity installs splash, checks ongoing flow notification restoration, then sets Compose content using `SkillzTheme`, `NotificationPermissionGate`, and `SkillzNavHost`.
- Splash/theme startup behavior: manifest uses `android:theme="@style/Theme.Skillz.Splash"` for `MainActivity`; regular application theme is `@style/Theme.Skillz`.
- Dependency injection initialization: `SkillzApplication` triggers Hilt app component setup; `MainActivity`, services, and ViewModels receive injected dependencies.
- Notification/service declarations: manifest declares `.ui.service.AliveFlowService` as non-exported foreground service with `foregroundServiceType="dataSync"`.
- Health Connect declarations: manifest declares Health Connect permission rationale and permission usage `activity-alias` entries targeting `MainActivity`.
- Deep links: `MainActivity` handles `skillz://flow`, and `SkillzNavHost` maps it to the Flow route.

Permissions and likely purpose:

- `android.permission.FOREGROUND_SERVICE`: required for `AliveFlowService` persistent Flow foreground notification.
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`: required by the service type declared for `AliveFlowService`.
- `android.permission.POST_NOTIFICATIONS`: required on Android 13+ for Flow and hourly reminder notifications.
- `android.permission.VIBRATE`: used by Surge haptic milestone/countdown behavior in `SurgeHapticsManager`/service flow.
- `android.permission.health.READ_STEPS`: used by Health Connect movement bonus step reading.

## 7. Navigation Architecture

- Main navigation host: `ui/navigation/SkillzNavHost.kt`.
- Route definitions/helper builders: `ui/navigation/SkillzDestinations.kt`.
- Start destination: `home_screen`, rendered by `SkillzHomeScreen`.
- Top-level routes observed:
  - `home_screen` → `SkillzHomeScreen`.
  - `add_skill` with query arguments → `FlowScreen`.
  - `add_pulse` → `PulseScreen`.
  - `shell` → `ShellRootScreen`.
  - `plan_arc` with optional `editArcPlanId` → `PlanArcScreen`.
  - `arc_detail/{arcPlanId}` → `ArcDetailScreen`.
  - `suggested_route_detail/{suggestedRouteId}` → `SuggestedRouteDetailScreen`.
  - `help` → `HelpScreen`.
- Flow route arguments: prefilled journey/title/soft mode, origin pulse ID, planned arc title/step/total; these are read by `FlowViewModel` through `SavedStateHandle`.
- Deep link: `skillz://flow` opens the Flow route.
- Shell room navigation: appears internal to `ShellRootScreen.kt` and `ShellDestination.kt`, not as separate NavHost destinations. Shell includes rooms such as The Blue, Focus, Idea Grove, Lookout, Stillwater, and Voyage Hall.
- Flow/Pulse navigation: Flow uses `ADD_SKILL_ROUTE`; Pulse uses `ADD_PULSE_ROUTE`; Shell can launch Flow for a journey or from a pulse by navigating with prefilled arguments.
- Dialogs/popups: represented primarily as Compose state/sheets inside screen files (for example Story pulse edit/details sheets, Blue detail/release sheets, Lookout dialogs, reward reveal deck) rather than separate NavHost routes.

## 8. UI Architecture

The UI is Compose-based, mostly organized by feature under `ui/screen/`.

Major screens/components:

- `ui/screen/SkillzHomeScreen.kt`: home shell that likely combines Story/Paths/help/Shell entry. Important actions include add session, add pulse, open planned flow/arc/suggested route, active session, and Shell. iOS equivalent: root tab/dashboard SwiftUI screen.
- `ui/screen/SkillzTopAppBar.kt`: reusable top app bar. iOS equivalent: custom navigation/header component.
- `ui/screen/HelpScreen.kt`: settings/help surface, including score/calm/language and health settings wiring. iOS equivalent: Settings/Help screen.
- `ui/screen/NotificationPermissionGate.kt`: Android notification runtime permission UI gate. iOS equivalent: notification authorization prompt flow.
- `ui/screen/flow/FlowScreen.kt`: primary Flow/Focus session UI. Important states include title/description/tag, soft mode, stopwatch/running state, Surge, arc state, reward reveal, save/cancel. iOS equivalent: Flow session/timer screen with timestamp restoration.
- `ui/screen/flow/StopwatchSection.kt`, `SurgeMiniControl.kt`, `RitualCard.kt`, `RitualFrame.kt`, `ArcPill.kt`, `ChronicleField.kt`, `GrandTitleField.kt`, `JourneyLean.kt`: reusable Flow UI and input pieces. iOS equivalent: timer controls, focus ritual components, arc status pill, journal fields.
- `ui/screen/flow/reward/`: reward UI (`RewardRevealDeck`, `RewardChips`, `RewardRevealMapper`, `SessionRewardContent`, `SoftSessionRewardContent`, `ArcSummaryContent`, `RewardUX`, `RewardRevealText`). iOS equivalent: reward reveal cards/sheets and reward presentation mappers.
- `ui/screen/paths/PathsScreen.kt`: planned flows/arcs/suggested routes organization. iOS equivalent: Paths/Plans screen.
- `ui/screen/paths/arc/PlanArcScreen.kt`, `ArcDetailScreen.kt`: arc creation/edit/detail and launch. iOS equivalent: Arc builder/detail views.
- `ui/screen/paths/suggested/`: suggested route catalog/detail. iOS equivalent: curated route detail/builder.
- `ui/screen/story/StoryScreen.kt` and `StoryBody.kt`: history/story area for sessions and pulses. iOS equivalent: Chronicle/Story timeline.
- `ui/screen/story/header/`: score displays, filters, tabs, date navigation, journey bottom sheets, pulse edit/details sheets. iOS equivalent: Story header/filter/score components.
- `ui/screen/story/chronicle/`: Flow/Pulse/Arc cards. iOS equivalent: timeline cards.
- `ui/screen/story/pulse/PulseScreen.kt`: Pulse capture. Important states include title/description/tag and whether Flow is active. iOS equivalent: Pulse capture sheet/screen.
- `ui/screen/shell/ShellRootScreen.kt`, `ShellTopBar.kt`, `ShellDestination.kt`: Shell root and room selection/navigation. iOS equivalent: Shell hub/container.
- `ui/screen/shell/inventory/`: badges, Shell Chest, notifications. The Chest currently includes creature/inventory mapper tests; iOS equivalent: inventory/chest and badge screens.
- `ui/screen/shell/rooms/blue/`: The Blue room, creature tray/tile/detail/release/encounter sheets, depth rail, scene bounds, overlay. iOS equivalent: The Blue ocean/creature scene and management UI.
- `ui/screen/shell/rooms/blue/creatures/` and `draw/`: procedural creature rendering and presence accounting. iOS equivalent: SwiftUI/Canvas creature rendering.
- `ui/screen/shell/rooms/focus/`: Focus Room screen/models and `FocusExerciseVoiceGuide`. iOS equivalent: Focus Room and voice/audio guidance if retained.
- `ui/screen/shell/rooms/ideagrove/IdeaGroveScreen.kt`: Idea Grove UI for pulses. iOS equivalent: pulse idea garden.
- `ui/screen/shell/rooms/lookout/LookoutRoomScreen.kt`: objectives/lookout UI. iOS equivalent: objective tracker.
- `ui/screen/shell/rooms/stillwater/StillwaterRoomScreen.kt`: Stillwater rewards/draw UI. iOS equivalent: soft-flow reward room.
- `ui/screen/shell/rooms/voyage/VoyageHallScreen.kt`: stats/voyage room. iOS equivalent: stats/voyage view.
- `ui/screen/shell/icons/` and `icons/draw/`: Shell iconography/drawn backgrounds. iOS equivalent: local SwiftUI shapes or copied asset equivalents.
- `ui/health/HealthComponents.kt`: Health Connect/movement settings UI components. iOS equivalent: HealthKit permission/status components.

## 9. State Management and ViewModels

State is primarily ViewModel + Kotlin Flow/StateFlow. Compose screens collect state from Hilt ViewModels; repositories expose database/DataStore flows; events are often functions on ViewModels or `MutableSharedFlow`/channels.

ViewModels observed:

- `viewmodel/FlowViewModel.kt`: serves `FlowScreen`. Depends on journey/session/pulse/Idea Grove/alive flow/arc repositories, service controller, `ArcPrefs`, `UserPrefs`, haptics, Shell reward orchestration/recording, health settings/permission/data source/repository, movement calculator/policy. Exposes `uiState`, `isSaving`, `lastReward`, `error`, `awaitingNextFlowAfterContinue`, `pendingArcIdeaContinuation`, tags, ongoing session, and exit flags. Handles Flow title/description/tag changes, timer start/pause/save, Pulse recording, arc continuation, reward creation, notification/service behavior, and health movement state.
- `viewmodel/StoryViewModel.kt`: serves Story/Help/Pulse flows. Depends on Flow, Pulse, Journey, Alive Flow, Flow Health, Health Refresh, and `UserPrefs`. Exposes story UI state and handles score UI/calm/language settings, period/date/tag filters, session description updates, pulse create/update/delete, session delete, and journey color mapping.
- `viewmodel/PathsViewModel.kt`: serves `PathsScreen`. Depends on FlowPlan, ArcPlan, Journey repositories. Exposes `PathsUiState`; handles tabs/time lens, flow plan CRUD/pin/archive/launch count, arc studio membership, and observed plan lists.
- `viewmodel/PlanArcViewModel.kt`: serves `PlanArcScreen`. Depends on ArcPlan, FlowPlan, Journey repositories. Exposes `PlanArcUiState`; handles wizard steps, selected flows, tag filters, new flow creation, recurrence, custom days, save/edit arc.
- `viewmodel/ArcDetailViewModel.kt`: serves `ArcDetailScreen`. Depends on ArcPlan, Journey, ActiveArcRun repositories and `SavedStateHandle`. Exposes detail state; handles add/remove from studio, begin/restart arc, launch payload creation.
- `viewmodel/SuggestedRouteDetailViewModel.kt`: serves suggested route detail. Depends on ArcPlan, Journey, ActiveArcRun repositories. Handles saving/beginning suggested route-derived arcs.
- `viewmodel/NotepadViewModel.kt`: wraps `NotepadRepository`; exposes notepad text/font StateFlows and update actions.
- `viewmodel/IdeaGroveViewModel.kt`: serves Idea Grove. Depends on `IdeaGroveRepository` and `AliveFlowRepository`; exposes `IdeaGroveUiState`, event channel, sorting/expansion/delete state, and pulse status actions.
- `viewmodel/health/HealthSettingsViewModel.kt`: serves Health settings UI. Depends on settings, permission, Flow Health, and refresh use case. Exposes `HealthSettingsUiState`; handles permission result, enable/disable movement bonus, install/update Health Connect, and foreground refresh.
- `viewmodel/shell/ShellViewModel.kt`: serves Shell root/rooms. Depends on `ShellRepository`; combines economy/ownership/memory flows into `ShellUiState`; handles placement, chest return, invite object, upgrades, creature growth/release, Beyond Blue encounters, Stillwater draws, notification viewed state, and room opened state.
- `viewmodel/shell/LookoutViewModel.kt`: serves Lookout room. Depends on context, Flow/Journey/Lookout repositories, and objective progress calculator. Exposes `LookoutUiState`, events; handles objective creation, reward claiming, removal/skip, period selection, and boundary refresh.
- `viewmodel/shell/VoyageHallViewModel.kt`: serves Voyage Hall. Depends on Flow/Journey repositories and `VoyageStatsCalculator`; exposes voyage stats and schedules local-day boundary refresh.

UI state classes include `FlowUiState`, `StopwatchState`, `FlowRewardUiModel`, `RewardRevealCardUiModel`, `PathsUiState`, `PlanArcUiState`, `IdeaGroveModels`, and many Shell/Lookout UI state data classes embedded in ViewModel/screen files.

## 10. Persistence Architecture

Local persistence uses Room plus DataStore. Main database: `data/model/SkillzDatabase.kt`, database name `skillz_db`, version 31, schema export enabled, migrations in `data/model/migration/SkillzDatabaseMigrations.kt`, schema JSON under `android/app/schemas/.../31.json`.

Entity inventory:

| Android class | File path | Table | Purpose | Important fields | Likely iOS model | Notes/TODOs |
|---|---|---|---|---|---|---|
| `TagEntity` | `data/model/entity/TagEntity.kt` | `tags` | Journey/tag labels | `id`, `name`, `createdAt` | `JourneyTag` | Used by sessions/plans/pulses. |
| `SessionEntity` | `data/model/entity/SessionEntity.kt` | `sessions` | Completed Flow sessions | title, description, tagId, start/end/duration, surge/scyra points, soft mode, arc fields | `FlowSession` | Reward math needs deeper spec. |
| `PulseEntity` | `data/model/entity/PulseEntity.kt` | `pulses` | Pulse/idea records | title, description, tagId, parent session/flow IDs, arcId, grove status | `Pulse` | Status values include ALIVE/INSIGHT/COMPLETED. |
| `PulseFlowLinkEntity` | `data/model/entity/PulseFlowLinkEntity.kt` | `pulse_flow_links` | Links pulses to completed sessions | pulseId, sessionId, linkedAt | `PulseFlowLink` | Many-to-many style linkage. |
| `OngoingSessionEntity` | `data/model/entity/OngoingSessionEntity.kt` | `ongoing_session` | Current draft/running Flow | flowInstanceId, title, running/soft flags, base start, accumulated time, surge fields, arc origin, pulse origin, health snapshot flags, active interval JSON | `OngoingFlowSession` | Critical for iOS timestamp restoration. |
| `FlowPlanEntity` | `data/model/entity/FlowPlanEntity.kt` | `flow_plans` | Planned reusable Flow | title, tagId, soft mode, target minutes, surge, pinned/archived, launch count | `FlowPlan` | Paths feature. |
| `ArcPlanEntity` | `data/model/entity/ArcPlanEntity.kt` | `arc_plans` | Multi-step arc plan | title, studio/archive flags, recurrence, launch metadata | `ArcPlan` | Has recurrence constants. |
| `ArcPlanStepEntity` | `data/model/entity/ArcPlanStepEntity.kt` | `arc_plan_steps` | Arc step snapshots | arcPlanId, order, sourceFlowPlanId, title/tag/soft/target/surge snapshots, link state | `ArcPlanStep` | Uses linked/customized/detached states. |
| `ActiveArcRunEntity` | `data/model/entity/ActiveArcRunEntity.kt` | `active_arc_run` | Active arc runtime | singleton id, arc plan/title, current step, tag, soft mode, timestamps | `ActiveArcRun` | Singleton active run table. |
| `FlowHealthSnapshotEntity` | `data/model/entity/health/FlowHealthSnapshotEntity.kt` | `flow_health_snapshots` | Health/movement read status per session | sessionId, enabled/permission flags, status, steps, movement/scyra/pearl contributions, timestamps, active intervals | `FlowHealthSnapshot` | Supports delayed movement refresh. |
| `FlowRewardBreakdownEntity` | `data/model/entity/health/FlowRewardBreakdownEntity.kt` | `flow_reward_breakdowns` | Persisted reward math inputs/output | non-movement points, pulse/surge/other/movement, multipliers, final scyra, pearls, eligibility | `FlowRewardBreakdown` | TODO: expand in `docs/06_REWARD_AND_ECONOMY_SPEC.md`. |
| `PearlLedgerEntity` | `data/model/entity/shell/ShellEntities.kt` | `pearl_ledger` | Pearl balance events | id, delta, reason, source type/id, note | `PearlLedgerEntry` | Economy ledger. |
| `UserShellFindInstanceEntity` | `data/model/entity/shell/ShellEntities.kt` | `user_shell_find_instance` | Individual creature/find instances | instanceId, findId, source, upgrade, customName, status, level, flow time | `CreatureInstance` | Contains legacy `ShellFind` naming. |
| `UserShellFindStackEntity` | `data/model/entity/shell/ShellEntities.kt` | `user_shell_find_stack` | Stacked finds/inventory counts | findId, quantity, acquired/viewed flags | `CreatureStack` | TODO: verify whether still used for creature-only Chest. |
| `ShellPlacementEntity` | `data/model/entity/shell/ShellEntities.kt` | `shell_placement` | Room slot placement | placementId, roomId, slotId, instanceId | `ShellPlacement` | Room object placement may be legacy. |
| `ShellFindUpgradeEntity` | `data/model/entity/shell/ShellEntities.kt` | `shell_find_upgrade` | Upgrade events | upgradeEventId, instanceId, stage IDs, pearl cost | `CreatureUpgradeEvent` | Naming still says ShellFind. |
| `UserBadgeEntity` | `data/model/entity/shell/ShellEntities.kt` | `user_badge` | Badge awards | badgeId, count, first/last earned, viewed | `BadgeAward` | Badge mastery tests exist. |
| `UserDiscoveryEntity` | `data/model/entity/shell/ShellEntities.kt` | `user_discovery` | Discovery records | discoveryId, source, granted instance, viewed | `DiscoveryRecord` | Potential legacy Discovery Journal. |
| `StillwaterLedgerEntity` | `data/model/entity/shell/ShellEntities.kt` | `stillwater_ledger` | Soft-flow Stillwater drops | id, units, source type/id, createdAt | `StillwaterLedgerEntry` | Soft Flow rewards. |
| `StillwaterPreferenceEntity` | `data/model/entity/shell/ShellEntities.kt` | `stillwater_preference` | Stillwater perspective/user choice | singleton id, perspective, updatedAt | `StillwaterPreference` | TODO: verify UI effect. |
| `UserShellRoomStateEntity` | `data/model/entity/shell/ShellEntities.kt` | `user_shell_room_state` | Per-room memory/open state | roomId, first/last opened, maturity/life scores | `ShellRoomState` | Shell progression/memory. |
| `ShellRewardEventEntity` | `data/model/entity/shell/ShellRewardEventEntity.kt` | `shell_reward_event` | Auditable reward events | sourceSessionId, arcId, rewardType/id, quantity | `ShellRewardEvent` | Reward types include object/trinket legacy names. |
| `ObjectiveEntity` | `data/model/entity/shell/ObjectiveEntities.kt` | `objectives` | Lookout objectives | journey, period/type, target, window, streaks, archive | `Objective` | Daily/weekly/monthly objectives. |
| `ObjectiveCompletionEntity` | `data/model/entity/shell/ObjectiveEntities.kt` | `objective_completions` | Completed objective rewards | objective/journey, period, achieved/target duration, pearls, streak, badge | `ObjectiveCompletion` | Claim state persisted. |
| `ObjectiveSkippedCycleEntity` | `data/model/entity/shell/ObjectiveEntities.kt` | `objective_skipped_cycles` | Skipped recurring cycles | objectiveId, period window, skippedAt | `ObjectiveSkippedCycle` | Lookout scheduling. |

DAO packages:

- `data/model/dao/`: base session/pulse/tag/plan/arc/ongoing DAOs.
- `data/model/dao/health/FlowHealthDao.kt`: health snapshot and reward breakdown persistence.
- `data/model/dao/shell/`: Shell, objective, Pearl, badge, discovery, Stillwater, and room-state DAOs plus `ShellDaoProvider`/`ShellDaos`.

Repositories:

- `FlowRepository`, `AliveFlowRepository`, `PulseRepository`, `JourneyRepository`, `FlowPlanRepository`, `ArcPlanRepository`, `ActiveArcRunRepository`, `NotepadRepository`.
- `health/FlowHealthRepository`, `HealthPermissionRepository`, `HealthSettingsRepository`.
- `shell/ShellRepository`, `IdeaGroveRepository`, `LookoutRepository`.

Long-term persisted user data includes sessions, pulses, journeys/tags, planned flows/arcs, active arc run, Shell creatures/economy/badges/discoveries/objectives/room state, health snapshots/reward breakdowns, notepad content, app settings, and arc preference snapshots.

Temporary/ongoing state includes `OngoingSessionEntity`, active intervals, service notification runtime, Surge runtime, pending arc continuation, and UI-only dialog/sheet state.

## 11. DataStore and Settings

DataStore usage observed:

- `DatabaseModule.kt` provides a singleton `DataStore<Preferences>` named `skillz_prefs`.
- `utils/user/UserPrefs.kt` defines a separate context extension `preferencesDataStore(name = "user_prefs")` and injects `Context` directly.
- `utils/arc/ArcPrefs.kt` wraps injected `skillz_prefs` for arc runtime/recent arc state.
- `data/repository/NotepadRepository.kt` wraps injected `skillz_prefs` for notepad text/font.
- `data/repository/health/HealthSettingsRepository.kt` wraps injected `skillz_prefs` for movement bonus setting.

Settings inventory:

| Android key/name | Location | Purpose | Default | Affects | Likely iOS storage |
|---|---|---|---|---|---|
| `show_score_ui` | `UserPrefs.kt` | User-facing score visibility toggle | `BuildConfig.SHOW_SCORE` | UI/score display; flavor-sensitive | `UserDefaults`/settings repository |
| `calm_mode` | `UserPrefs.kt` | Calm display mode | `false` | UI behavior | `UserDefaults`/settings repository |
| `app_language_tag` | `UserPrefs.kt` | Saved app locale | `null` | localization/app behavior | `UserDefaults` plus iOS localization manager |
| `arc_id` | `ArcPrefs.kt` | active arc ID snapshot | TODO: verify default semantics | Arc reward/runtime | settings repository or persisted active arc model |
| `arc_pending` | `ArcPrefs.kt` | active arc pending flag | TODO: verify | Arc runtime | settings repository/model |
| `arc_mult` | `ArcPrefs.kt` | active arc multiplier | TODO: verify | rewards | settings repository/model |
| `arc_progress` | `ArcPrefs.kt` | active arc progress | TODO: verify | arc runtime | settings repository/model |
| `arc_last_end` | `ArcPrefs.kt` | last arc end timestamp | TODO: verify | arc runtime/reward grace | settings repository/model |
| `arc_count` | `ArcPrefs.kt` | arc session count | TODO: verify | arc runtime/rewards | settings repository/model |
| `recent_arc_*` keys | `ArcPrefs.kt` | recently completed arc snapshot | TODO: verify | reward reveal/continuation | settings repository/model |
| `notepad_text` | `NotepadRepository.kt` | Notepad content | empty string | app behavior/content | local persistence/UserDefaults or SQLite |
| `notepad_doc_font` | `NotepadRepository.kt` | Notepad font choice, 0 default/1 cursive/2 mono | `0` | UI | UserDefaults |
| `movement_bonus_enabled` | `HealthSettingsRepository.kt` | Enables Health Connect movement bonus | `false` | movement/rewards/Health UI | UserDefaults/settings repository plus HealthKit authorization state |

Settings screens/toggles:

- `HelpScreen.kt` and `HealthComponents.kt` expose score/calm/language and movement bonus/permission UI.
- `StoryViewModel.kt` writes `show_score_ui`, `calm_mode`, and language.
- `HealthSettingsViewModel.kt` writes movement bonus setting after permission/availability checks.
- `NotepadScreen.kt`/`NotepadViewModel.kt` use notepad settings/content. TODO: verify current navigation route to Notepad.

## 12. Dependency Injection

Hilt setup:

- `SkillzApplication.kt`: `@HiltAndroidApp` application root.
- `MainActivity.kt`: `@AndroidEntryPoint` activity injection.
- `AliveFlowService.kt`: `@AndroidEntryPoint` service injection.
- ViewModels use `@HiltViewModel` with constructor injection.

Modules:

- `data/di/DatabaseModule.kt`: provides singleton `SkillzDatabase`, DAOs for tags/sessions/pulses/Idea Grove/ongoing session/plans/arcs/health, singleton `ArcPrefs`, and singleton preferences DataStore `skillz_prefs`. iOS equivalent: lightweight dependency container that constructs database, repositories, settings store, and services.
- `data/di/ShellDatabaseModule.kt`: provides Shell/economy/objective DAOs from `SkillzDatabase`, including Pearl, ShellFind, placement, upgrades, badges, discoveries, Stillwater, room state, reward events, and objective DAOs. iOS equivalent: dependency container/repository factory for Shell persistence.
- `data/di/HealthModule.kt`: provides `MovementBonusCalculator` and `MovementBonusEligibilityPolicy`. iOS equivalent: stateless services registered in dependency container/manual injection.

Repository/service classes largely rely on constructor injection without explicit provider modules unless their dependencies are directly injectable.

## 13. Background Work, Services, and Notifications

Services:

- `ui/service/AliveFlowService.kt`: foreground service for active Flow sessions. Triggered by `AliveFlowServiceController` and `MainActivity.maybeReinstateFlowNotification()` when an ongoing session exists and notification permission allows. User-visible behavior: persistent Flow notification with elapsed chronometer, active/paused status, Surge status, and hourly reminder notification. Future iOS equivalent: UserNotifications plus timestamp-based restoration; do not assume indefinite background execution.
- `ui/service/AliveFlowServiceController.kt`: starts/stops Alive Flow service. TODO: verify exact API and Android version branching before iOS parity spec.
- `ui/service/SurgeHapticsManager.kt` and `SurgeRuntime.kt`: haptic milestone/countdown runtime for Surge. Future iOS equivalent: haptics/timers while foreground; timestamp catch-up after background.

Notifications:

- `ui/notification/AliveFlowNotificationFactory.kt`: creates channels `flow_alive_channel` and `flow_hourly_reminder_channel`, boot notification, persistent Flow notification, and hourly reminder notification. Uses `skillz://flow` PendingIntent and `BuildConfig.PRIMARY_COLOR`.
- Main notification ID: `1001`; reminder notification ID: `1002`.

WorkManager:

- Dependency exists, but no `Worker` classes were found in source. TODO: verify if WorkManager is unused or planned.

App lifecycle/timer restoration:

- `MainActivity.onResume()` reinstates notification and runs foreground health refresh.
- `AliveFlowService.computeElapsed()` reconstructs elapsed time from `baseStartTimeMs` and `accumulatedBeforeStartMs` rather than relying only on in-memory ticking.
- `OngoingSessionEntity` persists Flow runtime fields and `activeIntervalJson`, supporting restoration after process/service interruption.
- iOS must follow the timestamp reconstruction model; it should not assume timers can run forever in background.

## 14. Health and Movement Integration

Health Connect integration files:

- `data/health/HealthConnectAvailability.kt`: enum for availability states. Future iOS equivalent: HealthKit availability/status enum.
- `data/health/HealthConnectClientProvider.kt`: checks Health Connect SDK status and provides client. Future iOS equivalent: `HKHealthStore` provider.
- `data/health/HealthConnectMovementDataSource.kt`: reads aggregated `StepsRecord.COUNT_TOTAL` between instants. Future iOS equivalent: HealthKit step quantity query over active intervals.
- `data/health/MovementReadResult.kt`: success/permission/unavailable/no data/error result type. Future iOS equivalent: typed result for HealthKit reads.
- `data/repository/health/HealthPermissionRepository.kt`: exposes `HealthPermission.getReadPermission(StepsRecord::class)` and checks granted read-steps permission. Future iOS equivalent: HealthKit authorization repository.
- `data/repository/health/HealthSettingsRepository.kt`: persists `movement_bonus_enabled`. Future iOS equivalent: UserDefaults/settings repository.
- `data/repository/health/FlowHealthRepository.kt`: persists snapshots/breakdowns and applies delayed movement updates transactionally, including pearl ledger deltas. Future iOS equivalent: repository transaction around flow session, health snapshot, reward breakdown, and pearl ledger.
- `utils/health/HealthRefreshUseCase.kt`: foreground refresh of pending health snapshots; reads steps for active intervals and recalculates delayed movement rewards.
- `utils/health/MovementBonusCalculator.kt`: movement points, eligibility, active interval codec/normalizer, delayed movement reward policy, and pearl delta key.
- `utils/health/MovementStepAggregator.kt`: reads steps across normalized active intervals.
- `viewmodel/health/HealthSettingsViewModel.kt`: permission/settings UI orchestration.
- `ui/health/HealthComponents.kt`: Health UI components.

Records read:

- Observed Health Connect record: `StepsRecord` only. No distance, heart rate, exercise session, or calorie records were observed. TODO: verify before HealthKit parity implementation.

Movement point calculation:

- `MovementBonusCalculator.calculateMovementPoints` converts steps to points using `steps / 100`.
- Eligibility requires movement bonus enabled, Health Connect available, read-steps permission granted, regular point-eligible flow, and not soft flow.
- Delayed movement reward recalculation can update Scyra points and pearls after a Flow completes.

Failure/denied states:

- Result types include permission missing, unavailable, no data, and error.
- Health settings ViewModel handles permission result, permission launch failure, install/update intent, disable confirmation, and foreground refresh.

## 15. Reward, Score, and Economy Architecture

Files/classes for deeper Task 1.7 inspection:

- `utils/score/ScoreCalculator.kt`: Scyra Score base duration breakdown, Surge points, and Arc math. TODO: expand in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- `utils/score/ScoreBreakdown.kt`: score breakdown model.
- `viewmodel/FlowViewModel.kt`: orchestrates Flow completion, session persistence, health snapshot/reward breakdown, arc continuation, Shell rewards, and UI reward model.
- `model/state/flow/FlowRewardUiModel.kt`, `RewardRevealCardUiModel.kt`, `ArcSummaryUiModel.kt`: reward presentation state.
- `ui/screen/flow/reward/RewardRevealMapper.kt` and reward UI files: mapping reward data into reveal cards/chips/text.
- `data/model/entity/health/FlowRewardBreakdownEntity.kt`: persisted reward breakdown including non-movement points, movement points, multipliers, final Scyra points, pearls, and eligibility.
- `utils/health/MovementBonusCalculator.kt` and `HealthRefreshUseCase.kt`: movement points and delayed movement updates.
- `utils/shell/ShellRewardOrchestrator.kt`: grants Shell rewards on session completion, handles regular vs soft Flow behavior, pearls/Stillwater/finds/badges/discoveries.
- `utils/shell/ShellRewardEventRecorder.kt`: records auditable reward events and aggregates arc reward summary.
- `utils/shell/CreatureEconomy.kt`: creature catalog, flow-earned creatures, Beyond Blue creatures, Stillwater creatures, pearl pricing, release value, growth costs, mastery tiers, visual scale, and counts.
- `utils/shell/Stillwater.kt`: soft-flow Stillwater drop units, vessel progress, validation, catalog, and roll logic.
- `data/repository/shell/ShellRepository.kt`: economy persistence operations for pearls/finds/placements/upgrades/badges/discoveries/Stillwater/room state.
- `data/repository/shell/LookoutRepository.kt` and `utils/shell/lookout/ObjectiveProgressCalculator.kt`: objectives, completion rewards, pearls, badges, and streak behavior.
- `utils/shell/voyage/VoyageStatsCalculator.kt` and `VoyageHallStats.kt`: Voyage stats derived from Flow/Journey history.

Observed concepts:

- Scyra Score: stored in `SessionEntity.scyraPoints`, shown conditionally by flavor/user setting, calculated via `ScoreCalculator` and adjusted by movement/arc/surge logic.
- Flow scoring: duration-based base score with timed bonuses; detailed math must be expanded later.
- Timed bonuses/Surge: `surgePlannedMs`, `surgePoints`, service haptics, and reward UI.
- Arc multipliers: `arcMultiplierUsed`, `arcBonusPoints`, `ArcPrefs`, `ActiveArcRunEntity`, `ScoreCalculator.arcMath`.
- Movement Points: Health Connect steps become movement points and delayed score/pearl contribution.
- Pearls: ledger in `PearlLedgerEntity`, reward breakdown `pearlsEarned`, Shell economy payments/releases/objectives.
- The Blue: creature inventory/scene and Beyond Blue trade/encounter flows under Shell blue room.
- Creature drops/inventory/release/mastery: `CreatureEconomy`, `ShellRepository`, `ShellViewModel`, Chest/Blue UI.
- Badge awards: `UserBadgeEntity`, objective/badge tests, Shell reward orchestrator/Lookout.
- Stillwater rewards: soft-flow drops and draw vessels in `Stillwater.kt` and Stillwater room UI.
- Pulses: persisted and linked to flows, but detailed reward treatment must be verified; avoid assuming Pulses are rewarded sessions. TODO: expand in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

## 16. Resource and Asset Architecture

Resource categories:

- Themes: `android/app/src/main/res/values/themes.xml`; splash and app themes used by manifest. Future iOS destination: SwiftUI app theme/design system and launch screen assets under `ios/`.
- Colors: XML colors in `res/values/colors.xml` plus Compose colors in `ui/theme/Color.kt`. Future iOS destination: color assets or SwiftUI design tokens under `ios/`.
- Typography: Compose typography in `ui/theme/Type.kt`; custom font resource `res/font/caveatsb.ttf`. Future iOS destination: bundled font under `ios/` and registered in iOS app metadata.
- App icons: launcher icons in `res/mipmap-*` and play store icon `android/app/src/main/ic_launcher-playstore.png`. Future iOS destination: iOS asset catalog app icons under `ios/`.
- Drawables: `ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_scyra_notification.xml`, `ic_scyra_splash_animated.xml`, `ic_scyra_splash_base.xml`, `scyra_turtle.png`, and `animator/scyra_turtle_drift.xml`. Future iOS destination: local iOS asset catalog/vector/animation equivalents under `ios/`.
- Strings: base `res/values/strings.xml`; localized `values-es`, `values-hi`, `values-mr`. Future iOS destination: `Localizable.strings`/String Catalogs under `ios/`.
- XML backup/data extraction: `res/xml/backup_rules.xml` and `data_extraction_rules.xml`. Future iOS equivalent: iCloud/local backup decisions if needed.
- Audio assets: no `res/raw` or audio assets observed. TODO: verify if `FocusExerciseVoiceGuide` uses TTS only.
- Creature assets: no static creature image assets observed; The Blue creatures appear procedurally drawn via Compose files under `ui/screen/shell/rooms/blue/creatures/` and `draw/`. Future iOS destination: SwiftUI/Canvas procedural drawings or local copied assets if later introduced.
- Shell room assets: many Shell icons/backgrounds appear Compose-drawn under `ui/screen/shell/icons/` and `icons/draw/`; future iOS destination should be local SwiftUI drawing code/assets under `ios/`.

Boundary reminder: iOS must eventually receive its own local copies or native equivalents inside `ios/`; it must not reference assets from `android/`.

## 17. Testing Architecture

Unit tests under `android/app/src/test/` include:

- Basic example test: `ExampleUnitTest.kt`.
- Shell catalog/economy/reward tests: `ShellContentCatalogTest`, `CreatureCatalogTest`, `CreatureEconomyTest`, `ShellRewardPolicyTest`, `ShellRewardEventAggregatorTest`, `ShellRewardEventRecorderTest`, `StillwaterCatalogTest`.
- Health/movement tests: `DelayedMovementRewardPolicyTest`, `FlowActiveIntervalTest`, `MovementBonusCalculatorTest`.
- Lookout/Voyage tests: `ObjectiveProgressCalculatorTest`, `VoyageStatsCalculatorTest`.
- UI mapper/policy tests: `RewardRevealMapperTest`, `TheBlueUiModelTest`, `ShellChestInventoryMapperTest`, `ShellNotificationInlayMapperTest`, `ShellFocusIndicatorPolicyTest`, `StillwaterRoomScreenTest`.
- ViewModel/time tests: `IdeaGroveActiveFlowStateTest`, `StillwaterUnlocksTest`, `IdeaGroveDurationFormatterTest`.

Instrumented tests under `android/app/src/androidTest/` include:

- `ExampleInstrumentedTest.kt`.
- `data/model/SkillzMigrationTest.kt`, which likely uses exported Room schemas from `android/app/schemas/`.

Testing gaps / likely tests needed before or while porting to iOS:

- Reward calculator tests for full Flow scoring, Arc multiplier edge cases, Surge bonuses, and score/pearl rounding.
- Flow lifecycle tests for start/pause/resume/background/process death/timestamp restoration.
- Persistence tests for all Room migrations and repository transactions.
- Pulse behavior tests for link/status transitions and confirmation that Pulses are not incorrectly treated as rewarded sessions.
- Movement points tests covering denied/unavailable/no-data Health states and delayed Health refresh windows.
- Creature economy tests for release/Beyond Blue/growth/mastery edge cases.
- Badge mastery tests for repeat awards, viewed state, and objective reward claims.
- Compose navigation/UI tests appear limited or absent beyond mapper-style unit tests. TODO: verify if additional UI tests exist outside listed paths.

## 18. Legacy or Removal Candidates

Do not delete anything; findings only:

- Discovery Journal: `UserDiscoveryEntity`, discovery IDs in `ShellRewardResult`, and `DISCOVERY_RECORDED` events suggest discovery-journal style persistence still exists. TODO: verify whether this remains product direction.
- Trinkets: `ShellRewardEventType.TRINKET_GRANTED` and likely Shell catalog kinds reference trinkets. TODO: verify whether trinkets should be excluded from current Scyra.
- Room objects: `OBJECT_GRANTED`, `ShellPlacementEntity`, `ShellFindUpgradeEntity`, and `ShellObjectIcon.kt` indicate object/placement concepts. TODO: verify compatibility with current Shell rooms.
- Shells / ShellFind naming: many persistence classes use `ShellFind`/`UserShellFind*`; may be legacy naming if the current Chest should contain creatures only.
- Coral/Plants: likely present in Shell definitions/catalog/drawing semantics. TODO: verify with product direction before porting.
- Old Shell Chest naming: `ShellChestScreen.kt` and mapper tests may conflict if product language should be “The Chest contains creatures only.” TODO: verify desired naming.
- Pulses as rewarded sessions: Pulse entities and link tables exist, but no clear Pulse reward session entity was observed. Ensure iOS does not reward Pulses unless later reward spec says so. TODO: verify.
- WorkManager dependency without observed worker classes may be unused/planned. TODO: verify.

## 19. iOS Port Implications

Likely iOS equivalents:

- Compose → SwiftUI.
- Material 3 components → custom Scyra SwiftUI design system.
- Navigation Compose/routes → `NavigationStack` with typed route enum/path state.
- Room → SwiftData or SQLite behind repositories; schema/migration design should account for existing Room entities.
- DataStore → `UserDefaults`/`@AppStorage` for simple settings, or a settings repository abstraction for testability.
- Hilt → manual/lightweight dependency container that creates repositories, database, settings, HealthKit provider, reward services, and ViewModels.
- Health Connect → HealthKit (`HKHealthStore`) step-count authorization and queries.
- WorkManager → `BGTaskScheduler` only where needed; do not assume periodic background execution.
- Android foreground/persistent notification → UserNotifications plus timestamp-based restoration; iOS should reconstruct elapsed Flow time from persisted timestamps.
- Android resources → iOS asset catalogs, localized strings/string catalogs, bundled fonts/audio, and SwiftUI drawing equivalents under `ios/`.
- Android services/haptics → foreground SwiftUI timers/haptics with persisted runtime state; background behavior must be conservative.
- Room transactions/repositories → repository methods that preserve reward ledger consistency and avoid double-awards.

## 20. Architecture Risks and Open Questions

- Reward logic spread across UI/repositories/utilities.
  - Why it matters: Flow completion touches score, health, Shell rewards, arc, pearls, and UI reveal models.
  - iOS impact: high risk of mismatched reward math or duplicate rewards.
  - Follow-up: Task 1.7 should produce `docs/06_REWARD_AND_ECONOMY_SPEC.md` from `FlowViewModel`, score, health, and Shell economy files.
- Timer/background behavior differs between Android and iOS.
  - Why it matters: Android uses a foreground service and notification chronometer; iOS has stricter background execution.
  - iOS impact: Flow timers must be persisted and reconstructed from timestamps.
  - Follow-up: write a Flow lifecycle/timer restoration spec and tests before implementation.
- Health Connect vs HealthKit differences.
  - Why it matters: permission UX, availability, step aggregation windows, and delayed data availability differ.
  - iOS impact: HealthKit may return different step samples/authorization states.
  - Follow-up: create a HealthKit movement bonus parity spec.
- Android-only resources need iOS equivalents.
  - Why it matters: icons, turtle splash, font, localized strings, and procedural creatures must be local to iOS.
  - iOS impact: cannot reference `android/` assets due repo boundaries.
  - Follow-up: asset inventory/export task that copies or recreates assets under `ios/` only when iOS project exists.
- Legacy economy code may not be ported.
  - Why it matters: Discovery, trinkets, room objects, ShellFind naming, Coral/Plants may conflict with current Scyra direction.
  - iOS impact: porting legacy concepts could reintroduce removed product behavior.
  - Follow-up: product cleanup/legacy audit before iOS implementation.
- Flavor differences between Aera and Scyra.
  - Why it matters: score visibility and branding differ by flavor.
  - iOS impact: iOS must decide whether it ships Scyra only or supports Aera-style score-hidden mode.
  - Follow-up: product decision on iOS targets/flavors.
- Large screens with embedded logic.
  - Why it matters: Compose screens/ViewModels may mix presentation, mapping, timers, and domain behavior.
  - iOS impact: direct port may create oversized SwiftUI views.
  - Follow-up: define SwiftUI feature modules and ViewModel boundaries before coding.
- Persistence model complexity.
  - Why it matters: Room version 31 has many related entities and ledgers.
  - iOS impact: migrations and transaction parity are non-trivial.
  - Follow-up: persistence schema mapping doc and fixture-based tests.
- Missing tests around full reward/session lifecycle.
  - Why it matters: existing tests cover many utilities but not necessarily end-to-end Flow completion.
  - iOS impact: parity regressions may go unnoticed.
  - Follow-up: add Android characterization tests before implementing equivalent iOS logic.

## 21. Acceptance Criteria for This Document

- This task only creates or changes `docs/01_ANDROID_ARCHITECTURE.md`.
- Android source code is untouched.
- No iOS source code or project is created.
- No Gradle/build files are changed.
- The document is based on actual files under `android/`.
- The document uses file paths for evidence wherever possible.
- Unclear behavior is marked `TODO: verify`.
- The repo boundary rules from `docs/00_REPO_BOUNDARIES.md` remain intact.

## 22. Codex Summary

- Android files/directories inspected: build files (`android/settings.gradle.kts`, `android/build.gradle.kts`, `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`), manifest, Kotlin source under `android/app/src/main/java/`, resources under `android/app/src/main/res/`, Room schema under `android/app/schemas/`, unit tests under `android/app/src/test/`, and instrumented tests under `android/app/src/androidTest/`.
- Most important architecture findings: Android is a Compose/Hilt/Room/DataStore app with Navigation Compose, foreground Flow service/notifications, Health Connect step-based movement bonuses, a substantial Shell creature/economy system, Flow/Arc/Pulse planning/history, and flavor-specific score/branding behavior.
- Highest-risk iOS replication areas: reward/economy parity, Flow timer/background restoration, HealthKit movement equivalence, Room schema/ledger transaction parity, Android-only resource recreation, and legacy Shell/economy concepts that may not match current product direction.
- Anything outside `docs/01_ANDROID_ARCHITECTURE.md` changed: no.
- Repo boundary rules preserved: yes. The document is reference-only under `docs/`; no Android code/build files were modified and no iOS project/files were created.
