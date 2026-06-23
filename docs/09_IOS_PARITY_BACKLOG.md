# iOS Parity Backlog

## 1. Purpose

This document converts the Task 1 discovery work into a future iOS implementation backlog for Scyra. It turns the Android source-of-truth docs into an ordered plan for iOS project creation, SwiftUI architecture, design system implementation, navigation, persistence, Flow lifecycle, reward engine, HealthKit movement, notifications, Shell, The Blue, The Chest, Badges, Pulse, Story/Chronicle, Paths/Arc, Focus Room, Lookout, Voyage Hall, Stillwater, asset/copy migration, tests, and release readiness.

This is documentation only. It does not create iOS files, create an Xcode project, modify Android source/resources, change Gradle/build configuration, add dependencies, create tests, create migrations, copy assets into `ios/`, or edit strings.

## 2. Source Documents Inspected

### `docs/00_REPO_BOUNDARIES.md`

- Defines `android/`, future `ios/`, and `docs/` as separate concerns.
- Establishes that Android and iOS must be independently openable/buildable from their platform folders.
- Makes `docs/` reference-only and forbids root shared runtime/build dependencies.
- Drives all backlog tasks to keep implementation under `ios/` once iOS work begins.

### `docs/01_ANDROID_ARCHITECTURE.md`

- Inventories Android build, flavors, package structure, Compose/Hilt/Room/DataStore/services/Health Connect setup.
- Identifies source directories, tests, Room schema export, foreground Flow service, notification behavior, and high-risk parity areas.
- Provides the baseline Android implementation map future iOS tasks should cite before implementing features.

### `docs/02_SCYRA_PRODUCT_SPEC.md`

- Defines Scyra product concepts: Flow, Pulse, Arc, Surge, Soft Flow, The Shell, The Blue, The Chest, Pearls, Movement Points, Stillwater, Badges, and product naming.
- Separates current Android behavior from desired product direction and legacy concepts.
- Establishes rules such as Pulse is not a rewarded session, The Chest is creature-only, and legacy objects/trinkets/discoveries need confirmation.

### `docs/03_NAVIGATION_AND_SCREEN_MAP.md`

- Maps Android routes, `SkillzNavHost`, route arguments, `skillz://flow`, top app bar behavior, modals/sheets/dialogs, Shell internal navigation, and end-to-end user flows.
- Recommends SwiftUI `NavigationStack`, typed route enums, local sheet/dialog state, and Shell internal route state.
- Informs the root app shell, route model, Flow/Pulse/Arc launch payloads, and notification/deep-link backlog tasks.

### `docs/04_DESIGN_SYSTEM.md`

- Extracts Scyra visual language: teal as the main iOS color direction, prior/reference Android blue values, system iOS typography direction, top bar order, cards, chips, Shell rooms, reward reveal, The Blue, The Chest, Badges, Focus Room.
- Recommends iOS design tokens/components such as `ScyraColor`, `ScyraTypography`, `ScyraTopBar`, `RewardRevealCard`, and `CreatureInventoryTile`.
- Identifies visual risks around procedural creatures, localization layout, system typography fit, and custom SwiftUI top bar requirements.

### `docs/05_DATA_MODEL_MAP.md`

- Maps Room database version 31, entities, DAOs, repositories, DataStore keys, ledgers, runtime-only state, migrations, transactions, and legacy persistence.
- Recommends repository isolation and now defaults iOS toward SQLite for Room-like control, explicit migrations, ledgers, transaction boundaries, in-memory stores, and iOS domain/persistence model names.
- Drives persistence, reward idempotency, Flow restoration, creature economy, Pearl ledger, Stillwater ledger, badge, and settings backlog tasks.

### `docs/06_REWARD_AND_ECONOMY_SPEC.md`

- Defines Android reward/economy behavior for Scyra Score, timed bonuses, Surge, Arc, Movement Points, delayed movement refresh, Pearls, creature drops/growth/release, Stillwater, Lookout, badges, and reward reveal mapping.
- Documents reward parity: Movement Points are 100 steps per point, Surge should match current Android behavior, Creature Mastery has an idempotency gap, and legacy reward categories should not be ported without confirmation.
- Establishes that reward/economy behavior must be implemented outside UI and backed by tests.

### `docs/07_ASSET_RESOURCE_COPY_AUDIT.md`

- Audits Android resources, strings/localizations, font, drawables, launcher/splash/store assets, XML, absence of `res/raw`, procedural visuals, creature/catalog copy, and legacy terminology.
- Establishes that iOS must get local assets/resources under `ios/`, not references to Android paths.
- Drives AppIcon/LaunchScreen, system typography, owner-created turtle/logo iOS export, String Catalog, localization review, The Blue redraw, and accessibility-label tasks.

### `docs/08_PLATFORM_SERVICES_MAP.md`

- Maps Android platform services: `SkillzApplication`, `MainActivity`, Hilt, Room/DataStore, foreground Flow service, notifications/channels, `skillz://flow`, Health Connect steps, WorkManager absence, AppCompat locales, TextToSpeech, splash, backup/data extraction, edge-to-edge.
- Recommends iOS service abstractions: dependency container, persistence container, Flow lifecycle service, notification service, HealthKit movement service, movement refresh service, speech guide service, locale service, deep link router, launch coordinator.
- Identifies iOS platform risks: no exact foreground-service equivalent, HealthKit differences, notification prompt timing, background refresh limits, backup/privacy decisions.

## 3. Non-Negotiable Repo Boundary Rules

- `android/` must remain self-contained and buildable/openable from Android Studio without depending on `ios/` or root docs at runtime/build time.
- `ios/` must become self-contained and buildable/openable from Xcode without depending on `android/` or root docs at runtime/build time.
- `docs/` is reference-only.
- iOS must not reference Android source, resources, Room schemas, fonts, images, drawables, strings, Gradle files, generated files, or test fixtures at build/runtime.
- Shared root runtime folders are disallowed unless a future architecture decision explicitly approves them.
- Future iOS assets/fonts/strings must be copied, recreated, localized, redrawn, or generated locally under `ios/`.
- Documentation can cite Android paths; implementation cannot depend on them.

Allowed future patterns:

- `ios/Scyra/Assets.xcassets/AppIcon.appiconset` created from verified source artwork.
- `ios/Scyra/Resources/Fonts/` only for future iOS-local fonts if explicitly chosen; do not copy Android Caveat.
- `ios/Scyra/Resources/Localizable.xcstrings` with local iOS strings.
- iOS tests using in-memory repositories and local fixtures under `ios/ScyraTests/`.

Disallowed future patterns:

- Xcode build phase copies `../android/app/src/main/res/font/caveatsb.ttf`.
- Swift references `../android/app/schemas/.../31.json` at runtime.
- iOS asset catalog references `../android/app/src/main/res/drawable/scyra_turtle.png`.
- A root `shared/assets/` or `shared/schemas/` folder becomes required by both platform builds without explicit architecture approval.

## 4. iOS Parity Principles

- Scyra full Android parity is the product target. Aera support is deferred unless explicitly scoped.
- Native Swift + SwiftUI first; use platform-native HealthKit, UserNotifications, AVSpeechSynthesizer, String Catalogs, launch screen, and persistence abstractions.
- Repository-first architecture. UI talks to view models/services/repositories, not persistence directly.
- UI must not calculate rewards directly.
- Flow timers must be timestamp-based and interval-based, not background-thread based.
- Reward/economy behavior must be test-backed before broad UI integration.
- Pulses are thoughts/ideas, not rewarded sessions.
- Soft Flow should not award Scyra Score, Pearls, Movement Points, or regular The Blue creatures.
- The Chest is creature-only and should not show released creatures or legacy non-creature objects.
- Legacy objects/trinkets/discoveries/Discovery Journal/Coral/Plants/Beam should not be ported to iOS Phase 1 without product confirmation.
- Android UI guides parity, but iOS should use clean SwiftUI components rather than mechanically copying Material/Compose internals.
- iOS must persist enough reward breakdown and ledger data to avoid historical drift and duplicate awards.
- Unclear Android behavior should be marked `TODO: verify`, not guessed.

## 5. Full Parity Scope and Phased Implementation Order

The iOS product goal is **full Android app parity for Scyra**. The phases below are an engineering safety/order plan, not a reduced product scope. Later-phase items are still part of the parity target unless explicitly marked as legacy/do-not-port.

### Phase 0: iOS Project Scaffold

- Create the iOS project scaffold under `ios/` so Xcode opens/builds independently from `ios/`.
- Establish Scyra app identity, bundle target, local resource structure, tests, and repo-boundary-safe project settings.

### Phase 1: Foundation

- Scyra design system basics: SlytherinButNiceTeal primary color, regular/system iOS typography, card/button/chip styles, empty/loading/error states, custom Scyra top bar.
- Root SwiftUI app shell, dependency container, typed navigation route model, settings repository, local persistence foundation, and repository/service protocols.
- SQLite persistence direction behind repositories unless a future implementation spike proves another approach better.

### Phase 2: Core Flow / Story / Pulse / Rewards

- Flow start/pause/resume/complete with timestamp-based active Flow restoration.
- Journey selection/basic creation using product term “Journey.”
- Story/Chronicle basic history for completed Flows and Pulses.
- Pulse capture/edit/history with no reward behavior.
- Base Scyra Score calculation, timed bonus parity, Surge matching current Android behavior, reward breakdown persistence, Pearl ledger, and custom reward reveal deck.
- Notification/deep-link return to active Flow using native iOS patterns if scoped in this phase.

### Phase 3: Shell / Chest / Badges / The Blue / Economy

- Shell root/hub, creature rewards, The Chest creature-only inventory, Badges, creature growth/release, Creature Mastery parity/gap handling, and The Blue visual room.
- HealthKit Movement Points using 100 steps = 1 Movement Point, delayed movement refresh, and idempotent Pearl deltas.

### Phase 4: Remaining Shell Rooms and Platform Polish

- Arc/Paths, Stillwater, Idea Grove, Lookout, Voyage Hall, Focus Room with AVSpeechSynthesizer, full notification polish, localization review, and platform permission education.

### Phase 5: Release Readiness

- Owner-created turtle logo exported into local iOS assets, AppIcon/LaunchScreen, accessibility pass, Spanish/Hindi/Marathi review, App Store privacy/permission checklist, performance pass, integration/UI tests, and release documentation.

### Defer / Product Confirmation Required Despite Full Parity Target

- Aera flavor/support unless explicitly scoped.
- Beam, legacy objects/trinkets/discoveries, Discovery Journal, room object placement, Coral/Plants, and non-creature inventory concepts unless product confirms they belong in current Scyra.
- Soundscapes/audio assets until assets/licensing and product scope exist.
- Android-to-iOS data import, cloud sync, Watch app, widgets, Live Activities, full App Store marketing assets, and advanced Health metrics beyond steps.

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

## 6. Open Product / Engineering Decisions Before Implementation

| Decision | Why it matters | Current Android evidence | Recommended owner/follow-up | Blocks |
| -------- | -------------- | ------------------------ | --------------------------- | ------ |
| SQLite persistence direction | Determines schema control, migrations, transaction guarantees, test stores. | Android Room v31 with complex ledgers/entities. | Use SQLite behind repository/service layers by default; spike only if implementation proves another option better. | Persistence foundation, rewards, ledgers. |
| Scyra-only vs Aera support | Affects colors, score visibility, app names, bundle targets. | Android flavors include Scyra and Aera BuildConfig values. | Product decision. | App identity, design tokens, settings. |
| Movement ratio | Final decision is 100 steps = 1 Movement Point. | Android production uses `steps / 100`; 25-step notes are stale. | Implement 100-step parity and add tests. | HealthKit Movement, reward tests. |
| Surge parity behavior | Affects score, Pearls, reward reveal, economy inflation. | Android currently stores/displays Surge separately and may not add it into final Scyra/Pearls. | Match current Android exactly; do not fix during iOS port. | Reward engine, Pearl ledger. |
| Creature Mastery idempotency design | Prevents duplicate countable Mastery awards. | Android gap around per-instance Level 99 Mastery persistence. | Product + persistence/rewards owner. | Badges/Mastery, creature growth. |
| iOS typography | Brand title treatment must match product direction. | Android has Caveat, but iOS should not use/copy it. | Use regular/system iOS font tokens. | Typography, brand title. |
| Turtle/logo iOS export | Core app icon/top bar/Shell brand asset. | Turtle logo is owner-created and usable. | Export/copy proper local iOS assets later; never reference Android path. | AppIcon, Shell icon, launch branding. |
| Scyra teal primary | iOS primary palette must be intentional. | SlytherinButNiceTeal `#3F8F8B` is the canonical Scyra primary; `0xFF2F4F6F` is legacy/deprecated where still present. | Implement SlytherinButNiceTeal as primary token. | Design system foundation. |
| Notification prompt timing | Bad timing hurts opt-in; Android auto-prompts in gate. | Android `NotificationPermissionGate` launches runtime prompt on composition. | Product/UX decision. | Notification permission PR. |
| iCloud backup/exclusion policy | Economy/Health/notepad data sensitivity. | Android `allowBackup=true` with template XML. | Engineering + privacy/product. | Release readiness, persistence policy. |
| In-app language override on iOS | iOS override differs from AppCompat locales. | Android stores `app_language_tag` and applies AppCompat locale. | Product/engineering decision. | LocaleService, localization UX. |
| The Blue drawing strategy | High-complexity procedural visuals. | Android uses Compose/procedural drawing in `rooms/blue/`. | Design/engineering prototype. | The Blue, creature icons. |
| Soundscape scope/licensing | No current Android assets; could add audio complexity. | `res/raw/` not present. | Product/audio licensing decision. | Soundscape/audio backlog only. |
| Arc/Paths Phase 1 vs Phase 2 | Arc affects Flow launch and reward multipliers. | Android implements Paths/Arc routes and persistence. | Product sequencing decision. | Arc UI, reward engine integration. |
| Shell rooms Phase 1 vs Phase 2 | Shell scope can expand Phase 1 substantially. | Android has Shell root, rooms, Chest, Blue, Badges, Stillwater, Grove, Lookout, Voyage, Focus. | Product sequencing decision. | Shell PR sequence. |
| Lookout/Voyage/Focus/Stillwater Phase 1 vs later | Each adds persistence/UI/reward complexity. | Android implements them; docs mark high-risk areas. | Product sequencing decision. | Phase 2 planning. |

## 7. Recommended iOS Architecture

Future iOS should use a native SwiftUI app root, a small dependency container, feature folders, domain models, repository protocols, persistence models, platform services, design system components, tests, and local assets/resources.

Recommended future layout only; do not create it in this task:

```text
ios/
  Scyra.xcodeproj
  Scyra/
    App/
    DesignSystem/
    Domain/
    Persistence/
    Services/
    Features/
    Resources/
    Support/
  ScyraTests/
  ScyraUITests/
```

Architecture notes:

- `App/` owns `@main`, app launch coordinator, dependency container composition, root route state, and app lifecycle hooks.
- `DesignSystem/` owns colors, typography, spacing, top bars, cards, chips, reward reveal components, and creature/inventory visual primitives.
- `Domain/` owns product models and pure calculators for Flow, rewards, movement, Pearls, creatures, badges, arcs, objectives.
- `Persistence/` owns SQLite models, migrations, repositories, transactions/unit-of-work, and in-memory test stores.
- `Services/` owns HealthKit, notifications, deep links, speech/TTS, locale, settings, backup policy notes.
- `Features/` owns SwiftUI screens/view models for Flow, Story, Pulse, Shell, Chest, Blue, Badges, etc.
- `Resources/` owns local iOS assets, fonts, String Catalogs, and audio if future scope adds it.
- Swift source belongs in Compile Sources, not Copy Bundle Resources.
- Assets belong in asset catalogs/resources.
- Fonts require proper Info.plist registration later.
- The iOS app must build independently from `ios/`.

## 8. Future iOS Module / Feature Map

| Feature/module | Android reference docs/source | Future iOS folders | Phase 1 priority | Dependencies | Major risks |
| -------------- | ----------------------------- | ------------------ | ------------ | ------------ | ----------- |
| App shell/startup | Docs 03, 08; `MainActivity`, `SkillzApplication` | `App/` | Phase 1 | Project scaffold, DI | Launch/route restoration. |
| Design system | Doc 04; theme/resources/top bars | `DesignSystem/` | Phase 1 | teal/system typography decisions | teal primary adoption, system typography. |
| Navigation/root routes | Doc 03; `SkillzNavHost` | `App/Navigation`, `Features/*` | Phase 1 | App shell | Route payload complexity. |
| Flow | Docs 03, 05, 06; `FlowViewModel`, Flow UI | `Features/Flow`, `Domain/Flow` | Phase 1 | Persistence, reward basics | Timer/restoration correctness. |
| Ongoing Flow restoration | Docs 05, 08; `OngoingSessionEntity`, service | `Services/FlowLifecycle` | Phase 1 | Persistence | iOS background limits. |
| Journey | Docs 02, 05; `TagEntity`, repositories | `Domain/Journey`, `Features/Journey` | Phase 1 | Persistence | Android internal Tag naming. |
| Story/Chronicle | Docs 03, 04 | `Features/Story` | Phase 1 | Flow/Pulse persistence | Naming Story/Chronicle/Horizon. |
| Pulse | Docs 02, 03, 05 | `Features/Pulse`, `Domain/Pulse` | Phase 1 | Journey, persistence | Avoid reward behavior. |
| Reward engine | Doc 06 | `Domain/Rewards` | Phase 1 | Domain models | Formula/idempotency decisions. |
| Reward reveal | Docs 03, 04, 06 | `Features/Rewards`, `DesignSystem` | Phase 1 | Reward engine | Historical drift; custom UI. |
| Persistence | Doc 05 | `Persistence/` | Phase 1 | SQLite decision | Transactions/migrations. |
| Settings | Docs 05, 08 | `Services/Settings`, `Features/Settings` | Phase 1 | Persistence/settings store | Flavor/language choices. |
| Notifications/deep links | Docs 03, 08 | `Services/Notifications`, `Services/DeepLinks` | Phase 1/soon | Active Flow restoration | No foreground-service equivalent. |
| HealthKit Movement | Docs 06, 08 | `Services/Health`, `Domain/Movement` | Soon after Phase 1 unless scoped | Reward engine, permissions | HealthKit timing/ratio decision. |
| Shell root | Docs 03, 04 | `Features/Shell` | Phase 1 if rewards need Shell; otherwise Phase 2 | Design system | Scope creep. |
| The Chest | Docs 02, 03, 05, 06 | `Features/Shell/Chest` | Phase 1 if creature rewards included | Creature persistence | Creature-only direction vs legacy. |
| Badges | Docs 05, 06 | `Features/Shell/Badges` | Phase 1 if badge rewards included | Badge persistence/service | Mastery idempotency. |
| The Blue | Docs 03, 04, 07 | `Features/Shell/TheBlue` | Phase 2 | Creature persistence, drawing strategy | Procedural rendering. |
| Stillwater | Docs 05, 06 | `Features/Shell/Stillwater` | Phase 2 | Soft Flow, ledger | Economy/draw rules. |
| Idea Grove | Docs 03, 05 | `Features/Shell/IdeaGrove` | Phase 2 | Pulse persistence | Pulse/Flow relationship. |
| Lookout | Docs 05, 06 | `Features/Shell/Lookout` | Phase 2 | Objectives, Pearl ledger | Claim idempotency. |
| Voyage Hall | Docs 03, 05 | `Features/Shell/Voyage` | Phase 2 | Sessions/stats | Derived stats vs persistence. |
| Focus Room | Docs 03, 04, 08 | `Features/Shell/FocusRoom`, `Services/Speech` | Phase 2 | TTS service, design | VoiceOver/TTS. |
| Notepad | Docs 03, 05 | `Features/Notepad` | Later/Phase 1 if root nav requires | Settings/editor decision | Rich editor parity. |
| Localization | Docs 07, 08 | `Resources/Localizable.xcstrings`, `Services/Locale` | Phase 1 base; locales later | String Catalog setup | Layout/translation quality. |
| Assets/resources | Doc 07 | `Resources/`, `Assets.xcassets` | Phase 1 basics | Provenance decisions | License/source gaps. |
| Tests | Docs 01, 05, 06, 08 | `ScyraTests`, `ScyraUITests` | Phase 1 | Architecture in place | Coverage discipline. |
| Release readiness | Docs 07, 08 | `Support/Release` | Later | Assets/privacy/tests | App Store requirements. |

## 9. Ordered Implementation Backlog

| Task ID | Title | Goal | Android/docs to read | Future files/folders likely | Dependencies | Acceptance criteria | Risk | Scope | Codex suitability |
| ------- | ----- | ---- | -------------------- | --------------------------- | ------------ | ------------------- | ---- | ----- | ----------------- |
| IOS-00 | Create iOS project scaffold | Manually create Xcode project under `ios/`. | Doc 00 | `ios/Scyra.xcodeproj`, `ios/Scyra/`, tests | Human/Xcode decision | Xcode opens/builds from `ios/`; Android untouched. | Medium | Phase 1 | Manual Xcode step / human decision first. |
| IOS-01 | Add app root and dependency container | Create `@main`, root view, dependency container skeleton. | Docs 03, 08 | `App/`, `Services/`, `Features/Home` | IOS-00 | App boots to placeholder root; container injectable. | Medium | Phase 1 | Good for Codex after project exists. |
| IOS-02 | Add folder conventions and Scyra identity | Establish groups, display name, safe placeholders. | Docs 00, 07, 08 | `Resources/`, `Assets.xcassets` | IOS-00 | iOS remains self-contained; no Android asset refs. | Medium | Phase 1 | Good for Codex with asset decisions. |
| IOS-03 | Design system foundation | Add Scyra color tokens, spacing, card/button/chip primitives. | Docs 04, 07 | `DesignSystem/` | teal primary direction | Components preview/testable; system iOS typography; no Caveat copy. | Medium | Phase 1 | Good for Codex. |
| IOS-04 | Typography/top bar foundation | Add top bar, system typography, and title treatment. | Docs 04, 07 | `DesignSystem/Typography`, `ScyraTopBar` | IOS-03 | Top bar order and labels match docs; no Caveat dependency. | Medium | Phase 1 | Good for Codex. |
| IOS-05 | Domain model foundation | Define Flow, Journey, Pulse, session, reward, ledger domain models. | Docs 02, 05, 06 | `Domain/Models` | Architecture decisions | Models are product-clean and Android legacy names are avoided. | Medium | Phase 1 | Good for Codex. |
| IOS-06 | Persistence backend foundation | Implement SQLite-backed persistence container skeleton behind repositories. | Doc 05 | `Persistence/` | IOS-05 | Persistence container builds; migrations plan exists; SwiftData is not the default. | High | Phase 1 | Good for Codex after project exists. |
| IOS-07 | Repository protocols + in-memory stores | Add repository protocols and test stores. | Docs 05, 06 | `Persistence/Repositories`, tests | IOS-05 | Tests can run without real DB. | Medium | Phase 1 | Good for Codex. |
| IOS-08 | Settings repository | Implement score/calm/language/movement settings wrapper. | Docs 05, 08 | `Services/Settings` | IOS-07 | Settings persist locally and are test-backed. | Low | Phase 1 | Good for Codex. |
| IOS-09 | Flow lifecycle | Implement start/pause/resume/complete domain/service. | Docs 03, 05, 08 | `Domain/Flow`, `Services/FlowLifecycle` | IOS-07 | Timestamp-based elapsed time tests pass. | High | Phase 1 | Good for Codex. |
| IOS-10 | Ongoing Flow restoration | Restore active Flow after background/relaunch. | Docs 05, 08 | Flow lifecycle + persistence | IOS-09, persistence | Relaunch tests reconstruct elapsed time. | High | Phase 1 | Good for Codex. |
| IOS-11 | Journey + Story basics | Implement Journey selection and Story/Chronicle list. | Docs 03, 05 | `Features/Story`, `Domain/Journey` | IOS-07, IOS-09 | Completed Flows appear in history. | Medium | Phase 1 | Good for Codex. |
| IOS-12 | Pulse capture/edit/history | Implement Pulse as idea/thought, no rewards. | Docs 02, 03, 05, 06 | `Features/Pulse` | IOS-07, Journey | Pulse tests prove no Score/Pearls. | Medium | Phase 1 | Good for Codex. |
| IOS-13 | Base reward calculator | Implement Scyra Score/timed bonuses. | Doc 06 | `Domain/Rewards`, tests | IOS-05 | Android parity formula tests pass. | High | Phase 1 | Good for Codex. |
| IOS-14 | Reward breakdown + Pearl ledger | Persist reward breakdown and ledger-backed Pearls. | Docs 05, 06 | `Domain/Economy`, `Persistence` | IOS-06, IOS-13 | Ledger/idempotency tests pass. | High | Phase 1 | Good for Codex. |
| IOS-15 | Reward reveal UI | Build custom reward deck/cards. | Docs 03, 04, 06 | `Features/Rewards`, `DesignSystem` | IOS-13 | No generic alert; mapper tests pass. | Medium | Phase 1 | Good for Codex. |
| IOS-16 | Creature/badge basics | Add regular Flow creature drops and basic badges if Phase 1 includes them. | Docs 05, 06, 07 | `Domain/Creatures`, `Features/Shell/Chest`, `Badges` | Creature scope decision | Chest groups creatures only; no released/legacy objects. | High | Phase 1/Phase 2 | Good after decision. |
| IOS-17 | Notifications and deep links | Add permission education, local notifications, route handling. | Docs 03, 08 | `Services/Notifications`, `DeepLinks` | IOS-10 | Notification tap restores active Flow. | High | Phase 1/soon | Good for Codex; prompt timing decision needed. |
| IOS-18 | HealthKit Movement | Add step authorization/query and movement points. | Docs 06, 08 | `Services/Health`, tests | 100-step Movement parity, IOS-13 | Step aggregation/idempotency tests pass. | High | Soon | Good after decision. |
| IOS-19 | Delayed movement refresh | Refresh pending snapshots and Pearl deltas. | Docs 05, 06, 08 | `MovementRefreshService` | IOS-18, ledger | No duplicate deltas; no reward lowering. | High | Soon | Good for Codex. |
| IOS-20 | Shell root | Build basic Shell hub/entry. | Docs 03, 04 | `Features/Shell` | Design system | Shell feels like hub, not settings page. | Medium | Phase 1/Phase 2 | Good after scope. |
| IOS-21 | The Chest | Creature-only inventory grid. | Docs 02, 03, 04, 05, 07 | `Features/Shell/Chest` | Creature persistence | Level/count grouping works. | Medium | Phase 1/Phase 2 | Good for Codex. |
| IOS-22 | Badges and Mastery | Badge screen and Mastery idempotency. | Docs 05, 06 | `Features/Shell/Badges` | Mastery decision | Countable Mastery cannot double-award. | High | Phase 2/Phase 1 if badges included | Needs decision first. |
| IOS-23 | The Blue prototype | Decide and implement Canvas/asset rendering prototype. | Docs 04, 07 | `Features/Shell/TheBlue` | Drawing strategy | Prototype performs and is accessible. | High | Phase 2 | Needs prototype/human design. |
| IOS-24 | Stillwater | Soft Flow Stillwater ledger/draws. | Docs 05, 06 | `Features/Shell/Stillwater` | Soft Flow + ledger | Soft Flow no-score; draw transaction tests. | High | Phase 2 | Good after reward foundation. |
| IOS-25 | Paths/Arc | Add Paths/Arc planning and multiplier. | Docs 03, 05, 06 | `Features/Paths`, `Domain/Arc` | Arc scope, reward engine | Arc launch/reward tests pass. | High | Phase 2 | Good after decision. |
| IOS-26 | Lookout/Voyage | Objectives and stats. | Docs 05, 06 | `Features/Shell/Lookout`, `Voyage` | Persistence/reward services | Claim idempotency and stats tests. | Medium/high | Phase 2 | Good after Shell. |
| IOS-27 | Focus Room TTS | Add AVSpeechSynthesizer exercise guide. | Docs 03, 04, 08 | `Features/Shell/FocusRoom`, `SpeechGuideService` | Focus scope | TTS states tested; no rewards. | Medium | Phase 2 | Good for Codex. |
| IOS-28 | Localization review | Add/review locales and screenshots. | Docs 07, 08 | String Catalogs, tests | Product/translation review | Layout passes base locales. | Medium | Soon/later | Needs human review. |
| IOS-29 | Accessibility and release pass | Full accessibility, App Store privacy, icons/launch. | Docs 04, 07, 08 | Assets/support/tests | Assets/provenance | Release checklist clean. | High | Release | Human + Codex mix. |

## 10. Recommended PR Sequence

| PR | Purpose | Major files/folders | Tests | Risk notes | Decision dependency |
| -- | ------- | ------------------- | ----- | ---------- | ------------------- |
| PR 1: `ios/project-skeleton` | Create clean Xcode project under `ios/`. | `ios/Scyra.xcodeproj`, app/test targets | Build in Xcode | Manual project setup. | Xcode vs XcodeGen. |
| PR 2: `ios/app-root-and-dependency-container` | Add root app, root view, dependency container. | `App/`, initial services | Basic app launch tests if possible | App architecture baseline. | None after scaffold. |
| PR 3: `ios/design-system-foundation` | Add colors, spacing, cards/buttons/chips. | `DesignSystem/` | Snapshot/unit where practical | Teal primary direction is settled. | Use teal as primary. |
| PR 4: `ios/navigation-shell` | Add typed routes and root top bar. | `App/Navigation`, `DesignSystem/ScyraTopBar` | Route tests | Top bar custom UI. | Naming/Horizon decisions. |
| PR 5: `ios/domain-models` | Add core domain models. | `Domain/Models` | Model unit tests | Avoid Android legacy names. | Persistence choice not required. |
| PR 6: `ios/persistence-foundation` | Add SQLite container. | `Persistence/` | Persistence smoke tests | High transaction stakes. | SQLite default; spike only if implementation proves otherwise. |
| PR 7: `ios/settings-repository` | Add settings storage. | `Services/Settings` | Settings persistence tests | Language override may defer. | Locale decision partial. |
| PR 8: `ios/flow-lifecycle` | Add Flow start/pause/resume/complete service. | `Domain/Flow`, `Services/FlowLifecycle` | Timer/interval tests | Core trust feature. | Persistence ready. |
| PR 9: `ios/ongoing-flow-restoration` | Restore active Flow after relaunch. | Flow persistence + app launch | Relaunch/scene tests | iOS background mismatch. | PR 8. |
| PR 10: `ios/journey-and-story` | Add Journey and basic history. | `Features/Story`, `Domain/Journey` | Repository/UI model tests | Naming Story/Chronicle. | PR 8/9. |
| PR 11: `ios/pulse` | Add Pulse capture/edit/history. | `Features/Pulse` | Pulse no-reward tests | Avoid reward leakage. | Journey/story. |
| PR 12: `ios/reward-engine` | Add base score/timed bonus/pure calculators. | `Domain/Rewards` | Formula parity tests | Reward correctness. | Product decisions for ambiguous formulas. |
| PR 13: `ios/reward-reveal` | Add custom reveal deck. | `Features/Rewards` | Mapper tests/snapshots | Must not be generic alert. | PR 12. |
| PR 14: `ios/pearl-ledger` | Add ledger-backed Pearls and breakdown persistence. | `Domain/Economy`, `Persistence` | Ledger/idempotency tests | Duplicate award risk. | PR 6/12. |
| PR 15: `ios/chest-creature-inventory` | Add creature-only Chest. | `Features/Shell/Chest` | Chest grouping tests | Legacy objects leakage. | Creature persistence. |
| PR 16: `ios/badges-and-mastery` | Add badge screen and Mastery design. | `Features/Shell/Badges` | Badge/Mastery tests | Mastery gap. | Mastery decision. |
| PR 17: `ios/notifications-and-deeplinks` | Add notification permission, local reminders, deep link router. | `Services/Notifications`, `DeepLinks` | Route/notification tests | Prompt timing. | Active Flow restoration. |
| PR 18: `ios/healthkit-movement` | Add HealthKit steps and movement reward refresh. | `Services/Health` | Health fake/idempotency tests | Ratio and background limits. | 100-step Movement parity. |
| PR 19: `ios/shell-root` | Add Shell hub. | `Features/Shell` | UI/snapshot tests | Scope creep. | Design system. |
| PR 20: `ios/the-blue` | Add The Blue prototype/room. | `Features/Shell/TheBlue` | Rendering/accessibility tests | Procedural complexity. | Drawing strategy. |
| PR 21+: `ios/stillwater`, `ios/paths-arc`, `ios/lookout-voyage`, `ios/focus-room`, `ios/localization-release` | Add Phase 2 rooms/services/polish. | Feature folders | Domain/UI tests | Each has separate product risk. | Scope decisions. |

Some PRs require human/manual Xcode setup before Codex can safely work. Do not claim Codex should generate a `.xcodeproj` unless a future task explicitly chooses that approach.

## 11. Phase 1 Dependency Graph

| Feature | Depends on | Blocks | Notes |
| ------- | ---------- | ------ | ----- |
| Project scaffold | Human Xcode setup | Everything iOS | Must live under `ios/`. |
| Design system | Scyra canonical primary color decision, safe font fallback | All screens | Use system typography; no Caveat copy. |
| Persistence | SQLite-backed implementation direction | Flow, Story, rewards, settings | Use repository/service layers and explicit migrations. |
| Settings | Persistence/settings store | Score visibility, movement toggle, language | Keep behind service/repository. |
| Flow | Design system, persistence, settings | Story, rewards, notifications, movement | Core Phase 1. |
| Ongoing Flow restoration | Flow persistence | Notifications, Health interval queries | Timestamp-based. |
| Journey | Persistence | Flow, Pulse, Story filters | Android internal Tag maps to Journey. |
| Reward calculator | Domain models | Reward reveal, ledger, Shell rewards | Pure/test-backed. |
| Reward reveal | Reward calculator, persisted session/reward model, design system | User completion experience | Custom deck/surface. |
| Pearl ledger | Persistence, reward calculator | Chest/growth/release/Lookout | Ledger-backed only. |
| Movement | HealthKit service, permission UI, reward engine | Movement Points/Pearl deltas | Ratio is settled: 100 steps = 1 point. |
| Notifications | Active Flow restoration, deep-link router | Return-to-Flow UX | No Android foreground service equivalent. |
| The Chest | Creature persistence, economy, visual tiles | Creature rewards, release/growth | Creature-only. |
| The Blue | Creature persistence, catalog, drawing strategy | Full Shell creature room | High rendering risk. |
| Badges | Badge repository, award service | Reward reveal badges, Shell badge screen | Mastery needs idempotency design. |
| Creature Mastery | Creature growth, badge service, idempotency persistence | Badges/Mastery release behavior | Needs decision before implementation. |
| Localization | String Catalogs, typography fallback | Release polish/localized UI | Review Spanish/Hindi/Marathi. |
| AppIcon/LaunchScreen | Verified source artwork | Release readiness | Do not copy Android paths. |

## 12. Risk Register

| Risk | Severity | Source doc | Why it matters | Mitigation | Before Phase 1? |
| ---- | -------- | ---------- | -------------- | ---------- | ----------- |
| iOS background/foreground service mismatch | High | `docs/08_PLATFORM_SERVICES_MAP.md` | Android active Flow uses foreground service; iOS cannot match exactly. | Timestamp-based restoration, notification strategy, tests. | Yes |
| Active Flow restoration correctness | High | Docs 05, 08 | Incorrect timers break rewards/trust. | Persist intervals; relaunch tests. | Yes |
| Reward/economy duplicate awards | High | Docs 05, 06 | Pearls/creatures/badges can inflate. | Ledger/idempotency transaction tests. | Yes |
| Movement ratio parity | High | Doc 06 | Stale 25-step assumptions could return. | Implement 100 steps per point and test it. | If Movement Phase 1 |
| Surge current-Android parity | Medium/high | Doc 06 | Score/Pearl math can drift if iOS “fixes” Android. | Match current Android behavior and cover with tests. | Before Surge phase |
| Creature Mastery idempotency gap | High | Docs 05, 06 | Countable Level 99 Mastery can double-award. | Add per-instance `masteryAwardedAt` or mastery ledger design. | Before Mastery |
| SQLite implementation direction | High | Doc 05 | Affects migrations, transactions, tests. | Implement SQLite default behind repositories; spike only with contrary evidence. | Yes |
| The Blue procedural rendering complexity | High | Docs 04, 07 | Hard to replicate Compose Canvas visuals. | Prototype SwiftUI Canvas/assets. | No, unless Blue Phase 1 |
| iOS typography drift | Medium | Docs 04, 07 | iOS intentionally uses system font instead of Android Caveat. | Define and review system typography tokens. | Before final branding |
| Turtle/logo export | Medium | Doc 07 | Owner-created logo still needs proper iOS asset export. | Export local iOS assets. | Before release |
| Localization quality/layout | Medium | Docs 07, 08 | Existing strings need review; Devanagari layout risk. | String Catalog, screenshot tests, human review. | Base yes; full locales later |
| HealthKit permission/background limitations | High | Doc 08 | Delayed movement may not refresh exactly. | Foreground refresh + idempotency; BGTask only if proven. | If Movement Phase 1 |
| Notification prompt timing | Medium | Doc 08 | Bad prompt timing reduces opt-in. | Permission education coordinator. | If notifications Phase 1 |
| iCloud backup/privacy | Medium/high | Docs 05, 08 | Health/economy/notepad data backup must be intentional. | Backup/privacy engineering task. | Before release |
| Legacy concept leakage | High | Docs 02, 06, 07 | iOS could port deprecated objects/trinkets/Beam. | Do-not-port table and PR checklist. | Yes |
| SlytherinButNiceTeal primary adoption | Medium | Docs 04, 07 | Visual identity can drift if iOS uses prior blue. | Use SlytherinButNiceTeal `#3F8F8B` primary token; keep blue as legacy/deprecated only. | Yes |
| Reward reveal historical drift | Medium/high | Doc 06 | Future formula changes can alter old history if not persisted. | Persist reward breakdowns and mapper state. | Yes |
| App Store permission/privacy review | Medium/high | Docs 07, 08 | Health/notifications/TTS/App Privacy can block release. | Release checklist. | Before release |

## 13. Legacy / Do Not Port Without Confirmation

| Concept | Android evidence | Desired product direction | iOS decision | Follow-up needed |
| ------- | ---------------- | ------------------------- | ------------ | ---------------- |
| Beam | Strings/routes/reward references in docs and Android copy. | Current Scyra backlog emphasizes Flow/Pulse/Arc/Surge. | Do not port Phase 1. | Product decision if Beam returns. |
| Aera flavor | Android flavor values and score-hidden behavior. | Scyra-first. | Defer unless scoped. | Product decision on multi-flavor iOS. |
| Discovery Journal / discoveries | Entity/reward/source references and copy. | Not confirmed for iOS Phase 1. | Do not port Phase 1. | Product/data compatibility decision. |
| Trinkets | Reward event/source references. | The Chest creature-only. | Do not port. | Product confirmation required. |
| Room objects | `shell_object_*`, placement/source entities. | The Chest creature-only; Shell decorations not Phase 1. | Do not port Phase 1. | Shell room decoration decision. |
| ShellFind terminology | Android internal entity/catalog names. | Product-clean creature naming. | Use Creature names in iOS domain/UI. | Data import compatibility naming only if needed. |
| Shell Chest naming | Internal `shell_chest_*` keys. | Product-facing “The Chest.” | Use “The Chest” in UI. | Avoid internal naming leakage. |
| Coral | `shell_category_coral`, Glow Coral copy. | Non-creature/legacy. | Defer/skip Phase 1. | Product confirmation. |
| Plants | `shell_category_plants`. | Non-creature/legacy. | Defer/skip Phase 1. | Product confirmation. |
| Object placement | Placement entities/copy. | Not Phase 1 unless Shell decoration returns. | Skip unless confirmed. | Product decision. |
| Pulse rewards | `pulseBonusPoints` field ambiguity; product says Pulses are not rewarded sessions. | Pulses are thoughts/ideas. | Do not implement Pulse rewards. | Verify if future product changes. |
| Soundscapes | No Android `res/raw`; soundscape picker not confirmed. | Future idea only. | Do not implement until assets/licensing exist. | Audio scope/licensing. |
| Android WorkManager jobs | WorkManager dependency present but no workers found. | No background job parity to copy. | Do not invent iOS BGTask solely for parity. | Add only if needed. |
| Non-step Health metrics | Android reads only `StepsRecord`. | Movement Points use steps for current parity. | Do not request heart/distance/workouts Phase 1. | Product decision for advanced health. |

## 14. Test Strategy for iOS Parity

| Test category | Source docs / Android tests to reference | Priority | Phase 1 required or later |
| ------------- | ---------------------------------------- | -------- | --------------------- |
| Domain unit tests | Docs 05, 06 | High | Phase 1 |
| Reward calculator tests | `ScoreCalculator`, reward spec/tests | High | Phase 1 |
| Movement calculator tests | `MovementBonusCalculatorTest`, `MovementStepAggregator`, Health docs | High | If Movement included |
| Delayed movement idempotency tests | `DelayedMovementRewardPolicyTest`, `HealthRefreshUseCase` | High | If Movement included |
| Flow lifecycle/restoration tests | Docs 05, 08; `OngoingSessionEntity` | High | Phase 1 |
| Persistence repository tests | Docs 05; Room migration/repository tests | High | Phase 1 |
| Pearl ledger tests | Docs 05, 06; Shell economy tests | High | Phase 1 if Pearls included |
| Creature economy tests | `CreatureEconomyTest`, docs 06/07 | Medium/high | If creature rewards included |
| Chest grouping tests | `ShellChestInventoryMapperTest`, docs 04/07 | Medium/high | If Chest included |
| Badge/Mastery tests | Docs 06 | High | Before Badges/Mastery release |
| Pulse no-reward tests | Docs 02, 06 | High | Phase 1 |
| Soft Flow no-score tests | Docs 02, 06 | High | Phase 1 if Soft Flow included |
| Notification/deep-link routing tests | Docs 03, 08 | Medium/high | If notifications Phase 1 |
| HealthKit fake service tests | Docs 06, 08 | High | If Movement included |
| TTS fake service tests | `FocusExerciseVoiceGuide`, doc 08 | Medium | Focus Room phase |
| Localization snapshot/layout tests | Docs 07, 08 | Medium | Base Phase 1; full locales later |
| Accessibility tests | Docs 04, 07 | Medium/high | Phase 1 core flows |
| UI tests for core flows | Navigation/UX docs | Medium | Phase 1 smoke flows |

## 15. Asset / Copy Migration Backlog

- Create iOS AppIcon from verified source artwork; do not reuse Android adaptive icon XML directly.
- Export the owner-created turtle logo as a proper local iOS asset before using it; do not reference Android drawable paths.
- Use regular/system iOS typography; do not copy Android Caveat into `ios/`.
- Create iOS String Catalog (`Localizable.xcstrings`) and migrate base English copy locally.
- Review Spanish/Hindi/Marathi translations and perform layout/screenshot checks before enabling full localized release.
- Create Scyra color tokens with SlytherinButNiceTeal `#3F8F8B` as the primary color; keep RavenclawBlue `0xFF2F4F6F` only as legacy/deprecated context if needed.
- Redraw/prototype The Blue creatures with SwiftUI Canvas or a product-approved asset pipeline.
- Create Pearl, Badge, Creature, and reward icon strategy as native iOS assets/components.
- Decide LaunchScreen static/animated-post-launch approach.
- Add accessibility labels for custom drawings and icon-only controls.
- Treat soundscapes/audio as future-only until assets and licensing exist.

## 16. Platform Services Backlog

- Add `AppLaunchCoordinator` for startup, initial route, settings load, and foreground refresh hooks.
- Add `AppDependencyContainer` and service/repository protocol wiring.
- Add `PersistenceContainer` using SQLite behind repositories by default; document any future deviation with evidence.
- Add notification permission education and native UserNotifications authorization flow.
- Add local notification scheduling for active Flow return/reminders if scoped.
- Add `DeepLinkRouter` for URL and notification response routing.
- Add active Flow restoration from persisted timestamps/intervals.
- Add HealthKit step authorization/query service for `stepCount` only.
- Add delayed movement refresh service and idempotency around Pearl deltas.
- Add `SpeechGuideService` using `AVSpeechSynthesizer` for Focus Room.
- Add `LocaleService` only if in-app language override is scoped.
- Add backup/iCloud policy notes and release checklist.
- Add App Store privacy/permission checklist for HealthKit, notifications, and any future audio.

## 17. Reward / Economy Backlog

- Implement base reward calculator and timed bonus tests from Android parity.
- Implement Surge to match current Android final-score/Pearl behavior exactly; do not reinterpret during iOS port.
- Implement Arc multiplier only after Arc scope and source-of-truth decisions.
- Implement Movement Points as 100 steps = 1 point and treat old 25-step notes as stale.
- Implement ledger-backed Pearls.
- Implement delayed movement delta idempotency.
- Implement regular creature drops if creatures are in Phase 1.
- Implement creature growth and release with Pearl ledger transactions.
- Implement Stillwater drops/draws when Soft Flow/Stillwater scope is active.
- Implement Lookout claims if Lookout is included.
- Implement badges and countable Creature Mastery after idempotency design.
- Implement reward reveal mapper with persisted reward breakdowns.

Remaining implementation gaps / safeguards:

- Creature Mastery persistence/idempotency design.
- Surge Android-parity tests for final-score/Pearl behavior, even though the behavior decision is settled.
- Movement tests confirming 100-step parity and stale 25-step assumptions are not reintroduced.

## 18. Data / Persistence Backlog

- Choose SQLite default; spike only if implementation proves otherwise.
- Create product-clean domain models.
- Create persistent models independent from Android table names where Android names are legacy.
- Create repository protocols and concrete stores.
- Create transaction service/unit-of-work for Flow completion, rewards, ledgers, creatures, badges, Health refresh, and Lookout claims.
- Plan migrations from the first iOS release.
- Add in-memory test stores.
- Add settings storage.
- Add ongoing Flow persistence.
- Add reward breakdown persistence.
- Add creature instance persistence.
- Add Pearl and Stillwater ledgers.
- Add badge persistence.
- Add objective persistence if Lookout is included.
- Add notepad storage if Notepad is included.
- Keep future cloud sync readiness in mind without adding sync prematurely.

## 19. UX / Screen Backlog

- Root app shell and custom Scyra top bar.
- Flow screen with timer, Journey, notes/details, pause/resume/complete, Soft Flow and Surge controls as scoped.
- Reward reveal modal/deck.
- Story/Chronicle list, filters, detail/edit sheets.
- Pulse capture/edit/history.
- Paths/Arc list/detail/plan flows if Phase 2.
- Shell root/hub.
- The Chest creature-only grid.
- Badges screen.
- The Blue scene/room.
- Stillwater room.
- Idea Grove.
- Lookout objectives.
- Voyage Hall stats.
- Focus Room and TTS player.
- Help/settings including permission education.
- Shell notification inlay.
- Notification/deep-link permission education screens.

## 20. Definition of Done for Future iOS PRs

- Only touches intended `ios/` files unless explicitly a docs update.
- Android untouched unless the task is Android-specific.
- No iOS build/runtime dependency on Android paths.
- Builds from Xcode with project opened from `ios/`.
- Tests added/updated for logic changes.
- Accessibility labels added for new interactive UI.
- Localization keys added for user-facing strings.
- Feature uses repository/service layer.
- No reward/economy calculations in SwiftUI views.
- No duplicate-award paths introduced.
- No legacy concepts ported without confirmation.
- Clear TODOs for incomplete parity.
- PR summary lists Android files/docs referenced.
- Any asset/font copied into iOS has provenance/license noted.
- Any Health/notification/TTS permission behavior has native iOS copy/UX.

## 21. Future Codex Prompt Template

```text
Task name: <IOS-XX: Feature title>

Goal:
<One paragraph describing the iOS feature or service to implement.>

Read these docs first:
- docs/00_REPO_BOUNDARIES.md
- docs/<relevant Task 1 docs>

Android source references:
- <Android file paths/classes/functions used as source of truth>

Future iOS files to create/edit:
- ios/Scyra/<paths>
- ios/ScyraTests/<paths>

Rules:
- Do not modify Android files unless explicitly requested.
- Do not reference Android files from iOS build/runtime.
- Keep iOS self-contained under ios/.
- Use repository/service layers.
- Add tests where logic changes.
- Preserve Scyra product terminology.
- Mark unclear Android behavior as TODO instead of inventing behavior.
- Do not port legacy concepts unless this task explicitly confirms them.

Acceptance criteria:
- <Build behavior>
- <Feature behavior>
- <Accessibility/localization requirements>
- <No repo-boundary violations>

Tests required:
- <Unit tests>
- <UI/snapshot tests if applicable>
- <Manual Xcode checks if needed>

Report summary:
- Files changed
- Android docs/source referenced
- Tests run
- Known TODOs/risks
```

## 22. Recommended Next Step After Task 1

After Task 1 docs are complete, keep/merge the docs PR as the reference baseline. The first implementation step should be to manually create a clean iOS Xcode project under `ios/` unless an XcodeGen/project-generation approach is explicitly chosen in a future architecture decision. Commit the clean scaffold by itself, verify Xcode opens and builds from `ios/`, then begin implementation PRs from this backlog.

Do not start high-risk reward, HealthKit, Shell, The Blue, or persistence-heavy work before the iOS foundation is stable. Suggested first implementation prompt title:

- `IOS-00: Create iOS project scaffold`

If a project already exists by then, the next prompt should be:

- `IOS-01: Add app root and dependency container`

## 23. Acceptance Criteria for This Document

- This task only creates or changes `docs/09_IOS_PARITY_BACKLOG.md`.
- Android source code is untouched.
- Android resources are untouched.
- No iOS source code or project is created.
- No assets are copied into `ios/`.
- No Gradle/build files are changed.
- No dependencies are added.
- The document synthesizes Tasks 1.1 through 1.9.
- Phase 1 scope is defined.
- Deferred scope is defined.
- Open decisions are listed.
- Ordered implementation backlog is included.
- Recommended PR sequence is included.
- Risk register is included.
- Test strategy is included.
- Asset/copy, platform, reward, data, and UX backlogs are included.
- Future Codex prompt template is included.
- iOS recommendations preserve repo boundary rules.
- Unclear behavior is marked `TODO: verify`.

## 24. Codex Summary

- Docs inspected: `docs/00_REPO_BOUNDARIES.md` through `docs/08_PLATFORM_SERVICES_MAP.md`.
- Phase 1 scope recommended: iOS scaffold, Scyra identity, design system basics, root navigation, Flow lifecycle/restoration, Journey, Story/Chronicle, Pulse, base reward calculator, reward reveal, local persistence, settings, optional notifications/deep links, and Shell/Chest/Badges only where needed by reward scope.
- Later phases / product-confirmation scope recommended: HealthKit Movement if not Phase 1, The Blue, full Shell hub, Arc/Paths, Lookout, Voyage Hall, Focus Room TTS, Stillwater, localization review, Aera, Beam, legacy objects/trinkets/discoveries, soundscapes, data import, cloud sync, Watch app, Live Activities, advanced Health metrics.
- Biggest remaining decisions/gaps: Scyra-only vs Aera timing, Creature Mastery idempotency, notification timing, backup/iCloud policy, iOS language override, The Blue drawing strategy, soundscape scope, and detailed room sequencing.
- Highest-risk iOS parity areas: active Flow restoration without Android foreground service, reward/economy idempotency, HealthKit delayed refresh, creature Mastery, The Blue procedural rendering, asset export/procedural visual provenance, localization layout, App Store permissions/privacy.
- Recommended first iOS implementation tasks: `IOS-00: Create iOS project scaffold`, then `IOS-01: Add app root and dependency container`, followed by design system foundation, domain models, persistence foundation, Flow lifecycle, and reward engine.
- Nothing outside `docs/09_IOS_PARITY_BACKLOG.md` should be changed for this task.
- Repo boundary rules remain preserved: this backlog is reference-only, and future iOS implementation must be self-contained under `ios/` with no Android build/runtime dependencies.
