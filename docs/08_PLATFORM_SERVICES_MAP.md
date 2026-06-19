# Platform Services Map

## 1. Purpose

This document maps Android-specific services and platform integrations so future iOS work can preserve Scyra behavior while using native iOS equivalents. It covers app startup, dependency injection, Room/DataStore setup, notifications, the foreground Flow service, deep links, Health Connect / movement, background refresh, lifecycle restoration, permissions, localization, TTS / Focus Room voice, backup/data extraction, splash/launch behavior, system UI behavior, platform-specific gaps, and iOS recommendations.

This is documentation only. It does not modify Android source/resources, create iOS files, add dependencies, create tests, create migrations, copy assets into `ios/`, or edit strings.

## 2. Source Material Inspected

### Prior docs

- `docs/00_REPO_BOUNDARIES.md`
- `docs/01_ANDROID_ARCHITECTURE.md`
- `docs/02_SCYRA_PRODUCT_SPEC.md`
- `docs/03_NAVIGATION_AND_SCREEN_MAP.md`
- `docs/04_DESIGN_SYSTEM.md`
- `docs/05_DATA_MODEL_MAP.md`
- `docs/06_REWARD_AND_ECONOMY_SPEC.md`
- `docs/07_ASSET_RESOURCE_COPY_AUDIT.md`

### Android platform files inspected

- Manifest/startup: `android/app/src/main/AndroidManifest.xml`, `SkillzApplication.kt`, `MainActivity.kt`, `res/values/themes.xml`.
- DI/setup: `data/di/DatabaseModule.kt`, `data/di/ShellDatabaseModule.kt`, `data/di/HealthModule.kt`, and source search for `@HiltAndroidApp`, `@AndroidEntryPoint`, and `@HiltViewModel`.
- Notifications/services: `ui/service/AliveFlowService.kt`, `ui/service/AliveFlowServiceController.kt`, `ui/notification/AliveFlowNotificationFactory.kt`, `ui/screen/NotificationPermissionGate.kt`, Shell notification/inlay screens and repositories referenced by prior docs.
- Health/movement: `HealthConnectClientProvider.kt`, `HealthConnectMovementDataSource.kt`, `HealthPermissionRepository.kt`, `HealthSettingsRepository.kt`, `HealthRefreshUseCase.kt`, `MovementStepAggregator.kt`, `MovementBonusCalculator.kt`, `FlowHealthRepository.kt`, health entities/DAOs, health ViewModel/UI files, and movement tests documented in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.
- Background/lifecycle/restoration: `AliveFlowRepository`, `OngoingSessionEntity`, `FlowViewModel`, `MainActivity.onResume`, `AliveFlowService`, `ArcPrefs`, `ActiveArcRunRepository`, and source search for WorkManager workers.
- Persistence platform setup: `SkillzDatabase.kt`, `SkillzDatabaseMigrations.kt`, `DatabaseModule.kt`, `ShellDatabaseModule.kt`, `UserPrefs.kt`, `ArcPrefs.kt`, `NotepadRepository.kt`, `HealthSettingsRepository.kt`.
- Permissions: manifest permissions, Health Connect permission code, notification permission gate, foreground service declaration, TTS query declaration.
- Localization: `AppLocaleManager.kt`, `UserPrefs.KEY_APP_LANGUAGE_TAG`, localized `values-*` folders, and AppCompat locale metadata service in the manifest.
- Focus Room/TTS/audio: `FocusExerciseVoiceGuide.kt`, Focus Room UI files, and Task 1.8 confirmation that `res/raw/` is not present.
- Backup/privacy/platform XML: `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`, and manifest backup attributes.
- Build/dependencies: `android/app/build.gradle.kts` for Hilt, Room schema export, Health Connect, DataStore, WorkManager, lifecycle service/runtime, Navigation Compose, Splash Screen, Kotlin datetime, and rich editor dependencies.

## 3. Platform Service Summary Table

| Android platform service | Android source | Product purpose | Current Android behavior | iOS equivalent | iOS risk/notes |
| ------------------------ | -------------- | --------------- | ------------------------ | -------------- | -------------- |
| Application class | `SkillzApplication.kt` | Process-wide initialization | `@HiltAndroidApp`; reads saved language tag with `runBlocking` and applies AppCompat locales. | `@main App` plus optional app delegate / launch coordinator. | Avoid blocking launch longer than needed; apply locale before root UI. |
| Main activity | `MainActivity.kt` | Launcher/root Compose host | `@AndroidEntryPoint`; installs splash, enables edge-to-edge, reinstates Flow notification, mounts `SkillzTheme`, `NotificationPermissionGate`, and `SkillzNavHost`. | `AppRootView` in SwiftUI scene. | Recreate initial route and active Flow restoration natively. |
| Splash screen | `themes.xml`, splash drawables/animator | Branded launch | Android SplashScreen API uses teal background, animated splash icon, 1400 ms duration. | iOS LaunchScreen plus optional post-launch animation. | iOS launch screens are mostly static. |
| Hilt | `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `data/di/*` | DI for DB, repositories, ViewModels, services | Singleton component provides Room, DAOs, DataStore, Health calculator/policy. | Lightweight dependency container + protocols/environment injection. | Do not mirror Hilt mechanically. |
| Room | `SkillzDatabase.kt`, `DatabaseModule.kt` | Local persistence | Database `skillz_db`, version 31, `exportSchema = true`, migrations attached. | SwiftData or SQLite behind repositories. | Reward/economy transactions need Room-like guarantees. |
| DataStore | `DatabaseModule.kt`, `UserPrefs.kt`, `ArcPrefs.kt`, `NotepadRepository.kt`, `HealthSettingsRepository.kt` | Preferences/settings/runtime state | Uses `skillz_prefs` and `user_prefs` DataStores for settings, Arc, notepad, movement toggle. | UserDefaults/AppStorage wrapped by repositories. | Keep storage isolated from SwiftUI views. |
| Foreground Flow service | `AliveFlowService.kt` | Keep active Flow visible and ticking | Foreground service starts notification, observes ongoing session, ticks every second, updates notification, handles Surge haptics/hourly reminders. | Timestamp-based service plus notifications; optional Live Activity later. | No exact iOS equivalent to persistent foreground service. |
| Notification channels | `AliveFlowNotificationFactory.kt` | Active Flow and reminder notifications | Channels `flow_alive_channel` / `flow_hourly_reminder_channel`; low/default importance. | UserNotifications categories if needed. | iOS users manage notification settings differently. |
| Notification permission gate | `NotificationPermissionGate.kt` | Request Android 13+ permission | Automatically launches `POST_NOTIFICATIONS` request on composition; calls `onGranted`. | iOS notification permission education + request. | iOS should not auto-prompt without product-approved timing. |
| Flow deep link | Manifest and `SkillzNavHost.kt` | Reopen active Flow | `skillz://flow` VIEW intent maps to Flow route; notification pending intents use it. | `onOpenURL` / notification response routing. | Restore route from persisted active Flow, not only URL args. |
| Health Connect | Health provider/repository/source files | Movement Points | Checks SDK status, requests/read permission for `StepsRecord`, aggregates step count over active intervals. | HealthKit `HKQuantityTypeIdentifier.stepCount`. | HealthKit authorization and background delivery differ. |
| Movement delayed refresh | `HealthRefreshUseCase.kt`, `FlowHealthRepository` | Update post-completion Movement Points/Pearls | Runs on foreground resume when enabled/permitted; expires old snapshots and transactionally updates snapshots/breakdowns/pearl deltas. | Opportunistic foreground refresh; BGTask/HealthKit background delivery if feasible. | Cannot promise exact timing on iOS. |
| WorkManager | Build dependency; source search found no worker files | Potential background work | `androidx.work:work-runtime-ktx` is included, but no Worker/WorkManager usage was found in source. | BGTaskScheduler only if a real need exists. | Do not invent scheduled jobs for iOS. |
| App locale manager | `AppLocaleManager.kt`, `UserPrefs.kt`, manifest AppCompat locale service | In-app language | Applies `LocaleListCompat` using saved `app_language_tag`; AppCompat auto-store locales service is declared. | String Catalogs; optional in-app language override. | iOS language override is more constrained than Android AppCompat. |
| TextToSpeech | `FocusExerciseVoiceGuide.kt`, manifest TTS query | Focus Room voice guide | Uses Android `TextToSpeech`, English language, gentlest available voice, speech rate/pitch tweaks, error fallback. | `AVSpeechSynthesizer`. | Voice quality/locales and VoiceOver interaction need testing. |
| Backup/data extraction | Manifest attributes, `backup_rules.xml`, `data_extraction_rules.xml` | Backup/transfer policy | `allowBackup=true`; XML files are sample/template-like with comments and no active include/exclude rules. | iCloud backup/exclusion policy. | Health/economy data backup needs product/privacy decisions. |
| Android resources/assets | `res/` and Task 1.8 | Platform assets/copy | Android-local resources only. | iOS-local assets/String Catalogs. | iOS must not reference Android resource paths. |
| System UI / edge-to-edge | `MainActivity.enableEdgeToEdge()`, Compose theme | Immersive modern UI | Edge-to-edge is enabled; Material theme wraps root. | SwiftUI safe areas/status bar styling. | Need custom top bars/insets parity. |

## 4. App Startup and Root Lifecycle

Android launch starts at `MainActivity`, which is exported and has the launcher intent filter. The manifest sets `android:name=".SkillzApplication"`, app label/icon resources, `allowBackup=true`, backup/data extraction XML, AppCompat locale metadata service, `AliveFlowService`, Health Connect permission-rationale aliases, and the `skillz://flow` VIEW intent filter.

`SkillzApplication` is annotated with `@HiltAndroidApp`. In `onCreate`, it injects `UserPrefs`, reads `appLanguageTag.first()` with `runBlocking`, and calls `AppLocaleManager.applyLanguage(savedTag)` before UI startup.

`MainActivity` is `@AndroidEntryPoint`. In `onCreate`, it calls `installSplashScreen()`, `enableEdgeToEdge()`, attempts `maybeReinstateFlowNotification()`, and mounts Compose with `SkillzTheme`, a full-screen `Surface`, `NotificationPermissionGate`, and `SkillzNavHost`. In `onResume`, it again reinstates the Flow notification and runs `healthRefreshUseCase.refreshForeground()`.

Active Flow notification restoration checks `aliveFlowRepository.getOngoingSession().firstOrNull()`, requires an entity that is in Flow mode or running, checks `POST_NOTIFICATIONS` on Android 13+, then starts `AliveFlowServiceController.start()`. The root NavHost starts at the home route and has a `skillz://flow` deep link on the Flow route.

Future iOS should recreate this with an `@main App`, a root dependency container, `AppRootView`, a LaunchScreen, scene phase handling, initial route restoration, app language setup before root UI where possible, and notification registration at a product-approved time. iOS must use persisted active Flow timestamps/intervals to restore state after background/kill instead of assuming a continuously running process.

## 5. Dependency Injection / Service Container

| Android DI item | Source | Provides/injects | Lifetime | iOS equivalent |
| --------------- | ------ | ---------------- | -------- | -------------- |
| `@HiltAndroidApp` | `SkillzApplication.kt` | Hilt application graph | Process singleton graph | `AppDependencyContainer` built during app launch. |
| `@AndroidEntryPoint` activity | `MainActivity.kt` | `AliveFlowRepository`, `AliveFlowServiceController`, `HealthRefreshUseCase` | Activity-injected | Root view model/container dependencies. |
| `@AndroidEntryPoint` service | `AliveFlowService.kt` | `AliveFlowRepository`, `SurgeHapticsManager` | Service-injected | `FlowLifecycleService` / notification coordinator. |
| `@HiltViewModel` | Multiple ViewModels | Feature repositories/use cases from Hilt | ViewModel-scoped | SwiftUI observable models with injected protocol dependencies. |
| `DatabaseModule.provideDatabase` | `DatabaseModule.kt` | Room `SkillzDatabase` named `skillz_db` with migrations | Singleton | `PersistenceContainer` using SwiftData or SQLite. |
| DAO providers | `DatabaseModule.kt`, `ShellDatabaseModule.kt` | Core, health, and Shell DAOs | Database-backed | Repository internals; do not expose DAOs to UI. |
| DataStore provider | `DatabaseModule.kt` | `DataStore<Preferences>` named `skillz_prefs` | Singleton | `SettingsStore` over UserDefaults or file-backed preferences. |
| `UserPrefs` DataStore | `UserPrefs.kt` | Separate `user_prefs` DataStore for score/calm/language | Singleton repository | `UserSettingsRepository`. |
| `ArcPrefs` | `DatabaseModule.kt`, `ArcPrefs.kt` | DataStore-backed Arc runtime/recent state | Singleton wrapper | `ArcRuntimeStore` behind repository. |
| Health module | `HealthModule.kt` | `MovementBonusCalculator`, `MovementBonusEligibilityPolicy` | Provided objects | Pure Swift calculators/services. |
| Health providers/repositories | Health source/repository files | Health Connect availability/steps/settings | Injected services | `HealthKitMovementService`, `HealthSettingsRepository`. |
| Notification/service controller | `AliveFlowServiceController.kt` | Starts/stops foreground service | Singleton | `NotificationService` + `FlowLifecycleService`; no foreground service. |
| WorkManager integration | Build dependency; no worker source found | Not present in source behavior | N/A | Do not create BGTask until needed. |
| TTS/service injection | `FocusExerciseVoiceGuide.kt` | Created from Android `Context`, not Hilt-injected in inspected file | UI/helper lifecycle | `SpeechGuideService` with explicit lifecycle. |

Future iOS should use repository protocols, dependency injection via initializers/environment, and in-memory test implementations. Avoid global singletons where reward, persistence, HealthKit, notification, and TTS behavior must be testable.

## 6. Persistence Platform Services

Android creates Room via `Room.databaseBuilder(context, SkillzDatabase::class.java, "skillz_db")`, attaches `SkillzDatabaseMigrations.ALL_MIGRATIONS`, and builds a singleton database. `SkillzDatabase` is version 31, `exportSchema = true`, and the Gradle build config exports schemas under `android/app/schemas`. `DatabaseModule` and `ShellDatabaseModule` provide DAOs to repositories. `SkillzDatabase` includes Flow, Pulse, Arc, health snapshot/reward breakdown, Pearl ledger, creature/find, Stillwater, badge, objective, Shell room state, and reward event entities.

DataStore is used in two ways: `DatabaseModule` provides a `skillz_prefs` `DataStore<Preferences>` used by `ArcPrefs`, `NotepadRepository`, and `HealthSettingsRepository`; `UserPrefs` creates a separate `user_prefs` DataStore for `show_score_ui`, `calm_mode`, and `app_language_tag`. These values affect UI, Arc runtime, Health movement toggle, notepad HTML/font, and localization.

Android backup is currently enabled at the manifest level, but backup/data extraction XML files are template-like. Future iOS must decide iCloud backup/exclusion for the database, ledgers, settings, and Health-derived snapshots. iOS should use SwiftData or SQLite behind repositories, define transaction boundaries from day one, use UserDefaults/AppStorage only behind settings repositories, version migrations from the first iOS release, and never depend on Android Room schema files at build/runtime.

## 7. Notifications and Foreground Flow Service

`AliveFlowService` is an Android foreground service declared with `foregroundServiceType="dataSync"`. On create, it ensures notification channels, calls `startForeground()` with a boot notification, observes the ongoing Flow entity, starts a one-second ticker, evaluates Surge haptic milestones, evaluates hourly reminders, publishes an ongoing notification, and stops itself when the ongoing session is missing or no longer in Flow mode. The service uses `NotificationManagerCompat` and respects Android 13+ notification permission checks.

`AliveFlowNotificationFactory` defines `CHANNEL_ID = "flow_alive_channel"`, `CHANNEL_NAME = "Flow State"`, `NOTIFICATION_ID = 1001`, `REMINDER_CHANNEL_ID = "flow_hourly_reminder_channel"`, `REMINDER_CHANNEL_NAME = "Flow reminders"`, and `REMINDER_NOTIFICATION_ID = 1002`. It creates an ongoing, non-auto-cancel, low-priority service notification with `skillz://flow` pending intent, chronometer behavior while running, status text, optional Surge line, and BuildConfig primary color. Hourly reminders use `R.drawable.ic_scyra_notification`, default priority, auto-cancel, and the same Flow deep link.

`NotificationPermissionGate` requests `POST_NOTIFICATIONS` on Android 13+ at composition time and calls the provided callback when granted. `MainActivity` only starts/reinstates the service when active Flow exists and notification permission is granted where required.

Android behavior that cannot be reproduced exactly on iOS: a persistent, non-dismissible foreground-service notification with a continuously ticking service process. iOS should instead persist active Flow timestamps/intervals, restore elapsed time on foreground/resume, optionally schedule local reminders, and consider Live Activities only as a later optional product decision. User-visible parity should preserve: users can return to an active Flow from a notification/deep link, elapsed time remains correct after background/relaunch, paused/running state is clear, and long Flow reminders remain gentle/idempotent.

## 8. Deep Links and Navigation Entry Points

The manifest declares a browsable VIEW intent for scheme `skillz`, host `flow`. `SkillzNavHost` also declares `navDeepLink { uriPattern = "skillz://flow" }` on the Flow route. `AliveFlowNotificationFactory` builds pending intents with `Intent.ACTION_VIEW`, `Uri.parse("skillz://flow")`, `MainActivity::class.java`, and `FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_CLEAR_TOP`.

The Flow route accepts multiple route arguments for prefilled Journey/title/Soft Flow and Arc/Pulse launch contexts, but the deep link itself is a simple resume/open Flow entry. Active Flow state comes from persistence through `FlowViewModel`/`AliveFlowRepository`, not from a large URL payload.

Future iOS should support either a custom URL scheme or Universal Links later, handle URLs in `onOpenURL`, handle notification responses through `UNUserNotificationCenterDelegate`, map route payloads to typed route models, and restore active Flow from persisted state. Risks include Android route argument complexity, iOS notification taps not matching Android pending-intent flags, and missing active Flow state after process kill if persistence is incomplete.

## 9. Health Connect / Movement Platform Map

Android Health Connect integration is step-count-only in the inspected source. `HealthPermissionRepository.readStepsPermission` is `HealthPermission.getReadPermission(StepsRecord::class)`, the manifest declares `android.permission.health.READ_STEPS`, and `HealthConnectMovementDataSource` reads `StepsRecord.COUNT_TOTAL` with `HealthConnectClient.aggregate(AggregateRequest(... TimeRangeFilter.between(start, end)))`. It checks granted permissions before reading and returns `HealthConnectUnavailable`, `PermissionMissing`, `NoData`, `Error`, or `Success(steps)`.

`HealthConnectClientProvider` maps SDK status to `AVAILABLE`, `PROVIDER_UPDATE_REQUIRED`, or `UNAVAILABLE`. `HealthSettingsRepository` stores `movement_bonus_enabled` in DataStore. `MovementStepAggregator` normalizes/merges active intervals and sums successful reads across intervals. `MovementBonusCalculator` currently converts steps to Movement Points with `steps / 100`. `HealthRefreshUseCase.refreshForeground()` runs on `MainActivity.onResume`, exits if the setting is disabled, expires old snapshots, checks Health Connect availability and read permission, then refreshes eligible snapshots. Successful delayed refresh transactionally updates health snapshot, reward breakdown, final session points, Arc bonus points, and Pearl delta using a stable movement Pearl reason.

Explicit answers:

- Required Health Connect permission: read permission for `StepsRecord`, exposed as `android.permission.health.READ_STEPS` and `HealthPermission.getReadPermission(StepsRecord::class)`.
- Records read: `StepsRecord` only, aggregated as `StepsRecord.COUNT_TOTAL`.
- Heart rate, distance, exercise sessions: not read in inspected Android source; treat as future-only.
- iOS parity permission: HealthKit read authorization for step count (`HKQuantityTypeIdentifier.stepCount`). No heart-rate/distance/workout permissions should be requested for MVP unless product scope changes.
- Future-only: Health metrics beyond steps, background delivery guarantees, and any exercise-session integration.

## 10. Background Work and Delayed Refresh

The Gradle file includes `androidx.work:work-runtime-ktx`, but source search found no `Worker`, `CoroutineWorker`, `WorkManager`, `PeriodicWorkRequest`, or enqueue-based scheduled job implementation. Health delayed refresh currently appears foreground/opportunistic: `MainActivity.onResume` calls `healthRefreshUseCase.refreshForeground()`, and the use case processes refreshable snapshots if movement bonus is enabled and permissions are available.

The active Flow foreground service provides second-by-second notification updates while the service is alive, but ongoing Flow correctness depends on persisted timestamps/accumulated time, not only on the ticker. Arc runtime persistence is split between Room active run state and DataStore `ArcPrefs` / recently-ended Arc snapshot.

Future iOS should prefer app foreground refresh on launch/resume, persist refreshable movement snapshots, use BGTaskScheduler only if it adds real value, consider HealthKit background delivery only after feasibility testing, and avoid promising exact background refresh timing. If no Android worker exists for a feature, iOS should not invent a background job solely for symmetry.

## 11. Active Flow Lifecycle and Restoration

Android persists ongoing Flow in `OngoingSessionEntity` through `AliveFlowRepository` and uses it across `FlowViewModel`, `MainActivity`, and `AliveFlowService`. Runtime state includes Flow mode, running/paused state, base start time, accumulated time, active intervals, title/Journey/description snapshot, Soft Flow flag, Surge fields, Arc launch fields, Pulse origin fields, and movement snapshot fields as documented in `docs/05_DATA_MODEL_MAP.md` and `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

Pause/resume changes are persisted so elapsed time can be reconstructed. The foreground service computes elapsed as `accumulatedBeforeStartMs + (System.currentTimeMillis() - baseStartTimeMs)` when running, otherwise accumulated time. On process relaunch/resume, `MainActivity` checks ongoing session and may restart the foreground service. Completing/canceling Flow clears/stops Flow mode; service collection then stops itself.

Future iOS must not assume continuous background execution. It should persist an `OngoingFlow` model with timestamp-based running/paused state and active intervals, reconstruct elapsed time from timestamps, react to scene phase changes, optionally schedule local reminders, and include tests for background/relaunch, pause/resume intervals, active Arc/Surge relationship, and Health interval aggregation.

## 12. Permissions Map

| Permission | Android source | Why needed | Runtime/install-time | User-facing flow | iOS equivalent permission/capability | iOS copy/UX differences | MVP priority | Notes/TODO |
| ---------- | -------------- | ---------- | -------------------- | ---------------- | ------------------------------------ | ----------------------- | ------------ | ---------- |
| `android.permission.FOREGROUND_SERVICE` | Manifest | Allows foreground service for active Flow notification. | Install-time/platform requirement. | No direct user runtime prompt. | No direct equivalent; iOS background modes/Live Activities only if scoped. | Explain active Flow reminders, not foreground service. | MVP concept | iOS cannot reproduce persistent service. |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | Manifest | Service type for `AliveFlowService`. | Install-time/platform requirement. | No direct prompt. | No direct equivalent. | N/A | Android-only | Do not port. |
| `android.permission.POST_NOTIFICATIONS` | Manifest, `NotificationPermissionGate.kt`, `MainActivity.kt` | Shows Flow/reminder notifications. | Runtime on Android 13+; earlier platform behavior differs. | Gate launches request and calls on granted. | UserNotifications authorization. | iOS should use education screen/timing before system prompt. | MVP if notifications enabled | Do not auto-prompt without UX decision. |
| `android.permission.VIBRATE` | Manifest, Surge haptics service code | Surge haptic milestones/countdowns. | Install-time normal permission. | No runtime prompt. | Core Haptics / UIFeedbackGenerator; no equivalent permission. | Respect iOS haptic settings. | MVP if Surge haptics included | Verify iOS haptic fallback. |
| `android.permission.health.READ_STEPS` | Manifest, Health permission repository | Reads Health Connect step counts for Movement Points. | Health Connect runtime permission. | Health settings card / permission launcher. | HealthKit step-count read authorization. | HealthKit privacy copy must be native. | MVP if Movement Points included | Steps only; do not request extra metrics. |
| `android.permission.START_VIEW_PERMISSION_USAGE` | Manifest activity aliases | Allows Health Connect permission-rationale/usage entry points. | Permission on exported aliases. | Health Connect platform flows. | No direct equivalent; HealthKit Settings/privacy pages. | iOS flow differs. | MVP concept | Android-only alias behavior. |
| `android.intent.action.TTS_SERVICE` query | Manifest `<queries>` | Discover/use installed TTS services. | Package visibility query, not permission. | No user prompt. | `AVSpeechSynthesizer` voices. | Voice availability differs. | Phase 2/MVP if Focus Room included | iOS does not need manifest query. |
| Internet/network | Manifest | Not present. | N/A | N/A | Not needed unless future features. | N/A | Later | Do not invent network dependency. |
| Wake lock | Manifest | Not present. | N/A | N/A | N/A | N/A | Later | No wakelock behavior found. |
| Exact alarm | Manifest | Not present. | N/A | N/A | N/A | N/A | Later | No alarm behavior found. |
| Boot completed | Manifest | Not present. | N/A | N/A | N/A | N/A | Later | Active Flow is restored on app open/resume, not boot. |
| Activity recognition/sensors | Manifest | Not present. | N/A | N/A | HealthKit steps only. | N/A | Later | Steps come from Health Connect, not sensor permission. |

## 13. App Locale / Language Service

Android localized resources exist for base English plus `values-es`, `values-hi`, and `values-mr`. The manifest declares `androidx.appcompat.app.AppLocalesMetadataHolderService` with `autoStoreLocales=true`. `UserPrefs.KEY_APP_LANGUAGE_TAG` persists a nullable language tag in `user_prefs`. `SkillzApplication` reads that value on startup and calls `AppLocaleManager.applyLanguage(tag)`, which sets AppCompat application locales with `LocaleListCompat.getEmptyLocaleList()` for null/blank or `LocaleListCompat.forLanguageTags(tag)` otherwise. `StoryViewModel` also applies language changes when the setting changes.

Future iOS should default to system language through String Catalogs, then decide separately whether to support an in-app language override. If in-app override is supported, iOS needs an app-level locale service, UI reload/recomposition strategy, typography fallback for Hindi/Marathi, screenshot tests, and product-approved behavior for Scyra/Aera flavor labels.

## 14. Focus Room TTS / Voice Guide Service

`FocusExerciseVoiceGuide` uses Android `TextToSpeech`. It initializes with application context, implements `TextToSpeech.OnInitListener`, sets language to `Locale.US`, selects a preferred UK English voice when available, falls back to any English voice, sets speech rate `0.68f` and pitch `0.94f`, supports `speak(text, flush)` with `QUEUE_FLUSH` or `QUEUE_ADD`, softens text for speech, posts ready/error callbacks on the main thread, supports `retry()`, `stop()`, and `shutdown()`, and reports text-only fallback messages when TTS is unavailable or language data is missing.

Explicit answers:

- Android does not use bundled audio files for Focus Room in the inspected source.
- Android does use platform TTS through `TextToSpeech`.
- iOS must recreate voice guidance natively with `AVSpeechSynthesizer`, including readiness/error state, voice selection/fallback, queue/cancel behavior, lifecycle cleanup, pause/resume/end interaction, silent-mode/audio-session decisions, and VoiceOver interaction testing.

## 15. Audio / Soundscape Platform Map

Task 1.8 confirmed `android/app/src/main/res/raw/` is not present. No bundled soundscape assets, Focus Room audio files, water ambience, or audio playback service were found in the resource/source audit. Focus Room uses TTS instead of audio files. Soundscapes remain a future product idea/open question rather than current Android platform behavior.

Future iOS should only use `AVAudioPlayer`/`AVAudioEngine` if licensed audio assets are added later under `ios/`. That later scope must decide background audio, audio session categories, interruption handling, user controls, asset provenance, localization of soundscape names, and App Store privacy implications.

## 16. Backup, Data Extraction, and Platform Privacy

The manifest sets `android:allowBackup="true"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, and `android:fullBackupContent="@xml/backup_rules"`. The inspected XML files are sample/template-like: `backup_rules.xml` has no active include/exclude entries, and `data_extraction_rules.xml` has TODO comments for cloud-backup/device-transfer rules.

Because Room and DataStore hold Flow history, reward/economy ledgers, settings, notepad HTML, and Health-derived snapshots, future iOS must explicitly decide what is included in iCloud backup, what is excluded, and how Health-derived data is described. This document is not a legal/privacy policy, but future App Privacy labels, permission copy, and backup behavior should be reviewed before iOS release.

## 17. Splash / Launch / Branding Platform Behavior

Android uses `Theme.Skillz.Splash` as `MainActivity` theme. It sets `windowSplashScreenBackground` to `#3F8F8B`, `windowSplashScreenAnimatedIcon` to `@drawable/ic_scyra_splash_animated`, animation duration to `1400`, and post-splash theme to `Theme.Skillz`. Launcher icons are under `mipmap-*` and app label is `@string/app_name`, with Gradle flavors providing Scyra/Aera BuildConfig values and app names.

Future iOS needs a static LaunchScreen, AppIcon asset set, display name and bundle identifier strategy, optional post-launch SwiftUI animation if the turtle drift is desired, and a Scyra-first recommendation unless Aera support is explicitly in scope. iOS cannot directly use Android adaptive icon XML or Android splash animation XML.

## 18. System UI, Theme, and Window Behavior

`MainActivity` calls `enableEdgeToEdge()`, uses `Theme.AppCompat.DayNight.NoActionBar` under `Theme.Skillz`, and wraps root Compose content in `SkillzTheme` plus a Material `Surface` using `MaterialTheme.colorScheme.background`. No manifest orientation lock was observed. Compose screens and custom top bars handle most UI chrome. Keyboard/inset handling appears feature-specific and should be rechecked when implementing forms/notepad/Flow on iOS.

Future iOS should respect safe areas, custom Scyra top bars, status/navigation bar styling, Dynamic Type, color scheme decisions, keyboard avoidance, and iPad/layout behavior. Dark mode parity is TODO: verify from Android theme/colors before deciding whether iOS supports full dark mode or fixed Scyra palette initially.

## 19. External Dependencies and Platform Libraries

| Android dependency | Source/build file | Feature using it | iOS native equivalent or third-party need | Should iOS avoid third-party? | Notes |
| ------------------ | ----------------- | ---------------- | ---------------------------------------- | ----------------------------- | ----- |
| Health Connect client | `libs.androidx.health.connect.client` | Movement Points / steps | HealthKit | Yes, native HealthKit | Request only step count for parity. |
| Hilt | Hilt plugin/deps/KSP | DI, ViewModels, services | Custom container/protocol injection | Yes initially | Swift does not need Hilt-like framework. |
| Room | Room runtime/KTX/compiler | Local database | SwiftData or SQLite | Maybe | SQLite may be better for Room-like control. |
| DataStore Preferences | `androidx.datastore.preferences` | Settings/notepad/Arc/Health toggle | UserDefaults/AppStorage behind repositories | Yes | Wrap to keep testability. |
| WorkManager | `androidx.work.runtime.ktx` | Dependency present; no worker found | BGTaskScheduler if needed | Yes until needed | Do not invent background jobs. |
| Lifecycle service/runtime | `lifecycle-service`, runtime compose | Service/lifecycle/root Compose | SwiftUI scene phase, app lifecycle | Native | Active Flow restoration needs tests. |
| Navigation Compose | `androidx.navigation.compose` | App routes/deep link | SwiftUI `NavigationStack` and typed routes | Native | See navigation map. |
| Splash Screen | `androidx.core:core-splashscreen` | Android splash | LaunchScreen + SwiftUI transition | Native | Animation constraints differ. |
| Kotlin datetime / Java time | Gradle and source time APIs | Dates/stats/Lookout/Health intervals | Foundation `Date`, `Calendar`, `DateComponents` | Native | Locale/time-zone tests needed. |
| Rich editor | `com.mohamedrejeb.richeditor:richeditor-compose` | Notepad HTML editor | Native text editor/WebView/custom rich text | Avoid until scoped | Notepad parity may require separate decision. |
| Android TTS platform API | `android.speech.tts.TextToSpeech` | Focus Room voice | `AVSpeechSynthesizer` | Native | No bundled audio needed. |
| Material/Compose icons/assets | Compose Material deps and resources | UI icons/chrome | SF Symbols/custom assets | Prefer native/custom | Avoid Android Material icon dependency. |

## 20. Platform Service Risks and Open Questions

| Risk/open question | Why it matters | iOS impact | Recommended follow-up task |
| ------------------ | -------------- | ---------- | -------------------------- |
| Android foreground service vs iOS background limits | Active Flow currently has persistent service notification/ticker. | iOS cannot keep equivalent process alive indefinitely. | Design timestamp-based Flow lifecycle and notification strategy. |
| Notification parity | Channels, pending intents, ongoing flags differ. | User return-to-Flow behavior can drift. | iOS notification UX/deep-link spec. |
| Health Connect vs HealthKit differences | Permission and step query APIs differ. | Movement Points may mismatch. | HealthKit spike with interval aggregation tests. |
| Delayed movement refresh reliability | Android refreshes opportunistically on resume. | iOS background timing may be less reliable. | Define refresh windows and idempotency tests. |
| Movement ratio decision | Task 1.7 flags Android `steps / 100` vs possible product direction. | iOS may ship wrong formula if decision unresolved. | Product decision before implementation. |
| WorkManager/BGTask strategy | WorkManager dependency exists but no workers found. | iOS should not over-engineer BGTask usage. | Background refresh architecture decision. |
| Active Flow restoration | Timers/rewards depend on accurate persisted intervals. | Incorrect elapsed time breaks trust/rewards. | Restoration test suite. |
| TTS voice quality/locales | Android picks English voices and text-only fallback. | iOS voice availability differs by locale/device. | SpeechGuideService prototype. |
| VoiceOver vs TTS | Focus Room TTS may conflict with accessibility speech. | Accessibility problems. | VoiceOver/TTS interaction testing. |
| App language override | Android AppCompat locales are app-level. | iOS override is more constrained. | LocaleService feasibility/spec. |
| Backup/iCloud decisions | Economy/Health/notepad data may be sensitive. | Privacy/backup behavior must be explicit. | Backup/privacy engineering task. |
| Splash animation parity | Android has animated splash icon. | iOS launch constraints prevent exact match. | iOS launch/branding design task. |
| Aera flavor support | Android has Scyra/Aera flavor values. | iOS target/scheme complexity. | Decide whether Aera is MVP. |
| App Store permissions/privacy | HealthKit/notifications/TTS copy must satisfy platform expectations. | App review/privacy risk. | App Store privacy/permission checklist. |
| Source-backed drawing performance | The Blue/Shell procedural visuals may be expensive. | SwiftUI Canvas performance risk. | Rendering prototype/performance test. |
| Future cloud sync implications | Local persistence and backup choices affect sync. | Later migration conflict risk. | Cloud sync architecture decision later. |

## 21. iOS Platform Service Recommendations

| Future iOS service/protocol | Purpose | Android source behavior it replaces | MVP or later | Testability recommendation |
| --------------------------- | ------- | ---------------------------------- | ------------ | -------------------------- |
| `AppDependencyContainer` | Compose app services/repositories at launch. | Hilt app graph/modules. | MVP | Build with protocol dependencies and in-memory variants. |
| `PersistenceContainer` | Own SwiftData/SQLite stack and migrations. | Room `SkillzDatabase`, `DatabaseModule`. | MVP | Use temporary/in-memory stores in tests. |
| `SettingsRepository` | Store score/calm/language/movement/notepad settings. | DataStore wrappers (`UserPrefs`, `HealthSettingsRepository`, `NotepadRepository`). | MVP | Mock UserDefaults/store. |
| `FlowLifecycleService` | Manage active Flow timestamps, pause/resume, restoration. | `AliveFlowRepository`, `OngoingSessionEntity`, `AliveFlowService`. | MVP | Unit-test elapsed reconstruction. |
| `NotificationService` | Request permission, schedule local reminders, handle taps. | Notification gate/factory/channels/pending intents. | MVP if notifications included | Mock notification center. |
| `DeepLinkRouter` | Convert URLs/notification responses into typed routes. | Manifest VIEW intent and NavHost deep link. | MVP | Pure route parsing tests. |
| `HealthKitMovementService` | Read step count over active intervals. | Health Connect provider/data source. | MVP if Movement Points included | Abstract `HKHealthStore`; fake query results. |
| `MovementRefreshService` | Refresh pending snapshots idempotently. | `HealthRefreshUseCase`. | MVP if Movement Points included | Transaction/idempotency tests. |
| `SpeechGuideService` | Focus Room voice guide. | `FocusExerciseVoiceGuide`. | Phase 2/MVP if Focus Room included | Fake synthesizer states. |
| `LocaleService` | Apply/select app language if in-app override exists. | `AppLocaleManager`, `UserPrefs.KEY_APP_LANGUAGE_TAG`. | Later unless required | Snapshot tests per locale. |
| `AppLaunchCoordinator` | Startup route restoration, permissions timing, health refresh. | `SkillzApplication`, `MainActivity`. | MVP | Launch scenario tests. |
| `PermissionEducationCoordinator` | Product-approved permission education screens. | Notification/Health permission UI. | MVP for Health/notifications | State-machine tests. |
| `BackupPolicyNotes` | Engineering record for backup exclusions/inclusions. | Manifest backup XML. | Later before release | Checklist/review artifact. |

These are recommendations only and do not create iOS code or folders.

## 22. iOS Test Plan Recommendations

| Test group | Android source/test reference | Expected iOS behavior | Priority |
| ---------- | ----------------------------- | --------------------- | -------- |
| Active Flow restoration after background/relaunch | `OngoingSessionEntity`, `AliveFlowRepository`, `MainActivity`, `AliveFlowService` | Reconstruct elapsed time from persisted timestamps after relaunch. | MVP |
| Pause/resume active intervals | Flow ViewModel and movement interval codec/calculator tests | Persist intervals accurately and exclude paused time. | MVP |
| Notification tap route restoration | `AliveFlowNotificationFactory`, manifest, `SkillzNavHost` | Tapping notification opens/restores Flow route. | MVP if notifications included |
| HealthKit authorization states | `HealthPermissionRepository`, `HealthSettingsViewModel` | Granted/denied/unavailable/update-required equivalents produce correct UI. | MVP if Movement Points included |
| Step aggregation by interval | `MovementStepAggregator` tests | Sum normalized active intervals; handle no-data/errors. | MVP |
| Delayed movement refresh idempotency | `HealthRefreshUseCase`, `FlowHealthRepository`, reward tests | Later refresh increases only when appropriate and does not duplicate Pearl deltas. | MVP if Movement Points included |
| Settings persistence | `UserPrefs`, `HealthSettingsRepository`, `NotepadRepository`, `ArcPrefs` | Settings survive app restart and are isolated behind repositories. | MVP |
| Language setting | `AppLocaleManager`, localized resources | App uses system/default locale or selected override consistently. | Phase 2 unless language override MVP |
| TTS states | `FocusExerciseVoiceGuide` | Ready/error/unavailable/start/stop/retry states work with fake synthesizer. | Phase 2/MVP if Focus Room included |
| Permission education | `NotificationPermissionGate`, Health settings UI | Education states precede native prompts and handle denial. | MVP for permissioned features |
| Database transaction service tests | Room repositories and reward/economy specs | Reward/ledger/creature/health updates are atomic. | MVP |
| Deep link routing | Manifest/NavHost deep link | URL and notification response map to typed routes with fallback restoration. | MVP |

## 23. Acceptance Criteria for This Document

- This task only creates or changes `docs/08_PLATFORM_SERVICES_MAP.md`.
- Android source code is untouched.
- Android resources are untouched.
- No iOS source code or project is created.
- No assets are copied into `ios/`.
- No Gradle/build files are changed.
- No dependencies are added.
- The document is based on actual Android platform-service code plus prior docs.
- App startup, DI, persistence setup, notifications, foreground service behavior, deep links, Health Connect, background work, lifecycle restoration, permissions, localization, TTS/audio, backup/data extraction, splash/launch, and system UI are documented or marked `TODO: verify`.
- iOS recommendations preserve repo boundary rules.
- Unclear behavior is marked `TODO: verify`.

## 24. Codex Summary

- Docs inspected: `docs/00_REPO_BOUNDARIES.md` through `docs/07_ASSET_RESOURCE_COPY_AUDIT.md`.
- Android platform-service files inspected: manifest, `SkillzApplication.kt`, `MainActivity.kt`, DI modules, Room/DataStore setup, notification/service files, Health Connect/movement files, localization files, Focus Room TTS files, backup/data extraction XML, and build dependencies.
- Android platform services discovered: Hilt app graph, Compose root activity, Room, DataStore, foreground Flow service, notification channels/gate, Health Connect steps, locale manager, TextToSpeech, splash screen, edge-to-edge UI, and backup/data extraction declarations.
- Notification/foreground service behavior discovered: `AliveFlowService` runs as a foreground service, maintains ongoing Flow notification, uses `skillz://flow` pending intents, evaluates Surge haptics and hourly reminders, and cannot be reproduced exactly on iOS.
- Health Connect/movement behavior discovered: Android reads only `StepsRecord.COUNT_TOTAL`, requires read-steps permission, aggregates active intervals, uses `steps / 100`, and refreshes pending movement snapshots on foreground resume.
- Background work behavior: WorkManager dependency is present, but no Worker/WorkManager source usage was found; delayed movement refresh is foreground/opportunistic.
- Permissions discovered: foreground service, foreground service data sync, post notifications, vibrate, Health Connect read steps, Health permission usage aliases, and TTS service query; no internet, boot, exact alarm, wake lock, activity recognition, or sensor permission was observed.
- Localization/app language behavior discovered: AppCompat locale service plus `UserPrefs.KEY_APP_LANGUAGE_TAG` and `AppLocaleManager.applyLanguage`.
- TTS/audio behavior discovered: Focus Room uses Android `TextToSpeech`, English voice selection/fallback, and no bundled raw/audio assets.
- Backup/data extraction behavior discovered: manifest backup enabled with sample/template backup and data extraction XML.
- iOS equivalents recommended: SwiftUI `@main App`, dependency container, persistence container, UserNotifications, HealthKit step queries, timestamp-based Flow lifecycle service, AVSpeechSynthesizer, String Catalogs, native launch assets, and iCloud backup policy decisions.
- Highest-risk iOS gaps: Android foreground service parity, notification behavior, HealthKit delayed refresh reliability, active Flow restoration, Movement ratio decision, TTS/VoiceOver interaction, app language override, backup/privacy policy, splash animation parity, and procedural visual performance.
- Nothing outside `docs/08_PLATFORM_SERVICES_MAP.md` should be changed for this task.
- Repo boundary rules remain preserved: docs reference Android source paths only for planning, and future iOS platform services/assets must live under `ios/` without Android build/runtime dependencies.
