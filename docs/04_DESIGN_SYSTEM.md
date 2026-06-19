# Scyra Design System

## 1. Purpose

This document extracts Scyra's current Android visual language and defines iOS parity targets for colors, typography, font usage, app title/brand treatment, spacing, shapes, cards, buttons, chips, stat pills, top bars, dialogs/sheets, reward reveal, Shell rooms, The Blue, The Chest, Badges, Focus Room, empty/loading/error states, icons/assets, and localization/copy style where visually relevant.

This is documentation only. It does not copy assets, create iOS code, create an Xcode project, modify Android source, change Gradle/build files, or add dependencies.

## 2. Source Material Inspected

Prior docs inspected:

- `docs/00_REPO_BOUNDARIES.md`
- `docs/01_ANDROID_ARCHITECTURE.md`
- `docs/02_SCYRA_PRODUCT_SPEC.md`
- `docs/03_NAVIGATION_AND_SCREEN_MAP.md`

Android resources/source inspected:

- Theme/resources: `android/app/src/main/res/values/colors.xml`, `themes.xml`, `strings.xml`, localized folders `values-es`, `values-hi`, `values-mr`, `res/font/caveatsb.ttf`, `res/drawable/`, `res/mipmap-*`, `android/app/src/main/ic_launcher-playstore.png`
- Compose theme: `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`
- Build flavor colors: `android/app/build.gradle.kts`
- App chrome: `ui/screen/SkillzTopAppBar.kt`, `ui/screen/SkillzHomeScreen.kt`, `ui/screen/HelpScreen.kt`, `ui/screen/NotificationPermissionGate.kt`, `ui/screen/NotepadScreen.kt`
- Flow UI: files under `ui/screen/flow/`
- Reward UI: files under `ui/screen/flow/reward/`
- Story/Chronicle UI: files under `ui/screen/story/`
- Paths/Arc UI: files under `ui/screen/paths/`
- Shell root/top bar: `ui/screen/shell/ShellRootScreen.kt`, `ShellTopBar.kt`, `ShellDestination.kt`, `ShellTopBar.kt`, `icons/`, `ux/ShellUXHelper.kt`
- Shell rooms: files under `ui/screen/shell/rooms/`
- The Blue UI/draw: `ui/screen/shell/rooms/blue/`, especially `draw/DrawTheBlue.kt`, creature rendering and scene files
- The Chest/Badges: `ui/screen/shell/inventory/ShellChestScreen.kt`, `BadgesScreen.kt`, `ShellNotificationsScreen.kt`
- Focus Room: `ui/screen/shell/rooms/focus/FocusRoomScreen.kt`, `FocusRoomModels.kt`, `FocusExerciseVoiceGuide.kt`
- Health UI: `ui/health/HealthComponents.kt`
- UI expectation tests: `ShellChestInventoryMapperTest.kt`, `TheBlueUiModelTest.kt`, `RewardRevealMapperTest.kt`, `StillwaterRoomScreenTest.kt`, notification/badge mapper tests

Expected path notes:

- No `android/app/src/main/res/raw/` audio folder was observed. TODO: verify whether any audio/soundscape assets exist outside `res/raw`.
- No confirmed soundscape picker implementation was found in inspected files. TODO: verify if soundscapes are future-only.

## 3. Visual Identity Summary

- Product name: **Scyra**.
- Primary visual tone: focused, reflective, rewarding, mythic/oceanic.
- Primary color direction: treat teal (`0xFF3F8F8B` / current Android teal theme direction) as the main Scyra iOS color; keep `0xFF2F4F6F` as a prior/secondary/reference blue where Android still exposes it.
- Current Compose app theme uses `SlytherinButNiceTeal` (`0xFF3F8F8B`) as `color` in `Theme.kt`; this teal is the current Scyra visual direction and should be the iOS primary color unless future Android source changes.
- Android-only Caveat brand accent: `caveatsb.ttf` and `CaveatSemiBold` are used by Android's top bar, but iOS should use regular/system typography and should not copy Caveat.
- Shell turtle/logo: `scyra_turtle.png` is used as the central Shell top-bar action and contributes to Scyra's creature/oceanic identity.
- Shell/The Blue language: oceanic, mythic, quiet, collection-oriented; The Blue uses zones/depth, creatures, ripples, and procedural drawing.
- Focus Room tone: calm, guided, non-reward-chasing.
- Reward reveal tone: celebratory but gentle; card deck/chips should feel special, not like a generic alert.
- Collection/inventory tone: The Chest should feel like a creature-only, RuneScape-bank-like inventory grid.
- Scyra visual identity should be distinguished from Aera flavor behavior; iOS should prioritize Scyra first unless Aera is explicitly requested.

## 4. Color System

| Android source/name | Value | Usage | Semantic role | iOS token recommendation | Notes |
|---|---:|---|---|---|---|
| `BuildConfig.PRIMARY_COLOR` default/scyra | `0xFF2F4F6F` | Notifications and Scyra flavor build config | Prior/reference Scyra blue in Android build config | `ScyraColor.referenceBlue` if needed | Do not make this the iOS primary unless product/source direction changes. |
| `BuildConfig.PRIMARY_COLOR` aera | `0xFF3F8F8B` | Aera flavor notification/branding | Aera calm teal | `ScyraColor.aeraTeal` if Aera is supported | Defer Aera on iOS. |
| `SlytherinButNiceTeal` / `Theme.kt color` | `0xFF3F8F8B` | Current Material `primary` in `SkillzTheme` | Main Scyra teal | `ScyraColor.primaryTeal` | Final iOS direction: use teal as primary. |
| `RavenclawBlue` | `0xFF2F4F6F` | Compose color constant; prior Scyra flavor primary/reference | Royal Manuscript Blue reference / secondary legacy blue | `ScyraColor.referenceBlue` only if needed | Do not use as iOS primary unless Android/product direction changes. |
| `GryffindorOffWhite` | `0xFFF2EBDD` | Light theme background/on-primary text | Parchment background | `ScyraColor.parchment` | Good Scyra surface/background token. |
| `Color(0xFFE4D8BB)` | `0xFFE4D8BB` | Light theme surface/surfaceVariant | Parchment card surface | `ScyraColor.surfaceParchment` | Normalize as card surface. |
| `AntiqueGold` | `0xFFB8A56A` | Light secondary | Pearl/gold accent | `ScyraColor.pearlGold` | Used as secondary. |
| `Bronze` | `0xFFB7893A` | Dark secondary | Bronze/gold accent | `ScyraColor.bronze` | Dark theme secondary. |
| `RoyalAmethyst` | `0xFFE6A8FF` | Tertiary | Magical/reward accent | `ScyraColor.amethyst` | Used in Material tertiary. |
| `SteelGrey` | `0xFF3A4652` | Compose constant | Steel/neutral | `ScyraColor.steel` | TODO: verify active use. |
| `ColdSteel` | `0xFF6B7C8A` | Compose Int constant | Cool neutral | `ScyraColor.coldSteel` | TODO: verify active use. |
| `SageMist` | `0xFF7E9B8B` | Compose constant | Calm green | `ScyraColor.sageMist` | TODO: verify active use. |
| Dark background | `0xFF1A1412` | Dark theme background | Warm dark background | `ScyraColor.darkBackground` | Current dark mode. |
| Dark surface | `0xFF221C19` | Dark theme surface | Warm dark card | `ScyraColor.darkSurface` | Current dark mode. |
| XML `purple_200/500/700`, `teal_200/700` | Various legacy Material template values | XML resources | Legacy/template colors | Do not promote unless used | TODO: verify if unused. |
| Splash background | `#3F8F8B` | `Theme.Skillz.Splash` | Splash teal | `ScyraColor.launchBackground` | Aligns with teal primary direction. |
| Reward deck selected indicator | Material secondary | `RewardRevealDeck.kt` | Reward page indicator | `ScyraColor.rewardIndicator` | Uses theme secondary. |
| Reward icon backgrounds | primary/secondary alpha | `RewardRevealDeck.kt`, `RewardUX.kt`, `RewardChips.kt` | Reward soft glow | `ScyraColor.rewardGlow` variants | Use alpha tokens. |
| Health cards | surfaceVariant/secondaryContainer alpha | `HealthComponents.kt` | Settings/info surfaces | `ScyraColor.infoSurface` | Should be normalized. |
| Shell pearl/icon colors | theme primary/secondary alpha | `ShellPearlMiniIcon.kt`, `ShellTopBar.kt`, Shell drawings | Pearl/Shell accent | `ScyraColor.pearlGold`, `ScyraColor.shellGlow` | Mostly theme-derived. |
| The Blue creatures | many hardcoded sea colors | `DrawTheBlue.kt`, `ShellDrawings.kt` | Creature/body/accent colors | Native drawing palette tokens by species/zone | Many local one-offs; normalize only if reused. |
| Shell notification card | Material surface, elevated card | `ShellNotificationsScreen.kt` | Floating notification inlay | `ScyraColor.inlaySurface` | Uses elevation + surface. |
| Chest badge/card colors | Material scheme + alpha | `ShellChestScreen.kt` | Inventory tile/badges | `ScyraColor.inventoryTile`, `ScyraColor.badgeSurface` | Need visual QA. |

Color guidance:

- Promote global Scyra tokens for primary teal, parchment background, parchment surface, pearl gold, bronze, amethyst, dark background, dark surface, text colors, reward glow, and Shell ocean/depth colors.
- Keep creature-specific hardcoded colors as procedural drawing palette values unless they become repeated design tokens.
- Resolve stale naming/comments before iOS implementation: product names should not expose Hogwarts-style color names.

## 5. Typography System

| Android source | Size/weight/font | Usage | iOS equivalent | Fallback |
|---|---|---|---|---|
| `Type.kt` `Typography.bodyLarge` | Default font, normal, 16sp, lineHeight 24sp, letterSpacing 0.5sp | Material body baseline | `ScyraTypography.body` | San Francisco body if custom font unavailable. |
| `Type.kt` `CaveatSemiBold` | `R.font.caveatsb`, SemiBold | Brand/accent font family definition | `ScyraTypography.brandTitle` | Fall back to rounded/serif/handwritten-safe title style. |
| `SkillzTopAppBar.kt` title | CaveatSB, 30sp, onPrimary | “Scyra” wordmark in top app bar | `ScyraTypography.wordmark` | Use system title with custom letter spacing if font missing. |
| Material `titleLarge/titleMedium/titleSmall` | Material defaults | Card/screen titles across Flow, Shell, Chest, Focus | `ScyraTypography.screenTitle/cardTitle` | SwiftUI `.title3` / `.headline`. |
| Material `bodyLarge/bodyMedium/bodySmall` | Material defaults | Body copy, descriptions, dialogs | `ScyraTypography.body/bodySmall` | SwiftUI body/callout. |
| Material `labelLarge/labelMedium/labelSmall` | Material defaults | Chips, badges, tabs, tile metadata | `ScyraTypography.label` | SwiftUI caption/footnote. |
| Reward number/title styles | Material title/headline variants | Score/Pearl/reward cards | `ScyraTypography.rewardNumber` | Use bold rounded numeric style. TODO exact sizes. |
| Chest count/level badges | labelSmall/labelLarge | Top-right count and bottom level indicators | `ScyraTypography.inventoryBadge` | Caption with minimum legibility. |
| Focus exercise typography | titleMedium/titleLarge/body | Exercise cards/player prompts | `ScyraTypography.focusPrompt` | Body/headline with calm line height. |

Localization considerations:

- Localized folders exist for Spanish (`values-es`), Hindi (`values-hi`), and Marathi (`values-mr`).
- System iOS typography should provide broad glyph coverage; still verify Hindi/Marathi layout, line-height, and fallback behavior.
- Long localized labels in buttons/dialogs may require flexible layouts, wrapping, and larger hit targets.
- iOS should use regular/system typography for Scyra; if future non-system fonts are deliberately chosen, they must be local under `ios/` and must not reference Android font files.

## 6. Spacing, Layout, and Shape System

| Pattern | Android source/values | Usage | iOS token/component recommendation |
|---|---|---|---|
| Screen padding | Common `16.dp`; Chest `Modifier.padding(16.dp)`; Badges content padding `16.dp` | Standard screen margins | `ScyraSpacing.screenPadding = 16` |
| Card padding | Reward deck `20.dp`; reward metric rows `14x12.dp`; Chest detail `20.dp`; notification inlay `20/16/12.dp` | Card internals | `ScyraSpacing.cardPadding = 16/20` |
| Section spacing | Common `Arrangement.spacedBy(10.dp/12.dp/16.dp)` | Vertical rhythm | `ScyraSpacing.sectionGap = 12/16` |
| Capsule radius | `RoundedCornerShape(999.dp)` | Selected tabs, chips, badges, metric pills | `ScyraRadius.capsule` |
| Card radius | `16.dp`, `18.dp`, `20.dp`, `24.dp`, `28.dp` | Health cards, reward cards, inlays, Shell cards | `ScyraRadius.card = 20`, variants small/large |
| Reward deck height | `360.dp` pager height | Reward reveal card deck | `RewardRevealCard.height` adaptive equivalent |
| Reward chip padding | `12x9.dp`, rounded capsule | Reward chips | `ScyraComponent.RewardChip` |
| Top bar icon sizes | 22dp icons; 34dp turtle; Shell pearl 18dp | Root chrome | `ScyraIconSize.topBar = 22`, turtle 34 |
| Chest grid | adaptive columns `104.dp`, tile `104.dp`, gaps `10.dp` | The Chest inventory grid | `CreatureInventoryTile.size = 104`, adaptive grid |
| Chest icon size | creature icon `54.dp`; detail icon `64.dp` | Inventory tile/detail | `CreatureIcon.medium/large` |
| Health card radius/padding | radius 18/50, padding 14/16 | Health settings panels | `InfoCard`, `InfoPill` |
| Notification inlay | radius 24, elevation 12, width 380 on wide, max height 420 | Shell notification popover | `ScyraInlayCard` |
| Focus player | local large cards, phase pills, player controls | Guided exercise | `FocusPlayerCard`, `PhaseCountdownPill` |

Spacing guidance:

- iOS should use a small set of tokens (`4`, `6`, `8`, `10`, `12`, `14`, `16`, `20`, `24`) to mirror Android rhythm.
- Normalize repeated rounded-corner values into card/capsule/inlay tokens.
- Keep The Chest tile sizing consistent with level/count badge placement.

## 7. App Chrome and Top Bars

### Root `SkillzTopAppBar`

- File: `ui/screen/SkillzTopAppBar.kt`.
- Title: `Scyra`, CaveatSB, 30sp, onPrimary.
- Background: `MaterialTheme.colorScheme.primary`.
- Exact Android top app bar icon ordering:
  1. Story (`Icons.Outlined.AutoStories`)
  2. Paths/Horizon (`Icons.Outlined.Explore`)
  3. Shell turtle (`R.drawable.scyra_turtle`, 34dp)
  4. Notepad (`Icons.Outlined.EditNote`)
  5. Help (`Icons.Outlined.HelpOutline`)
- Selected icon treatment: rounded capsule with onPrimary alpha background; semantics role `Tab`, selected state, and state description.
- Shell turtle is a central image button and opens The Shell rather than switching root page.

### Shell top bar

- File: `ui/screen/shell/ShellTopBar.kt`.
- Uses Shell-specific title/actions, Pearl mini icon, indicator colors, and back behavior.
- TODO: verify exact Shell top-bar ordering and title states during every room before iOS visual implementation.

### iOS parity

- Use custom SwiftUI components rather than a plain native navigation bar if needed:
  - `ScyraTopBar`
  - `ShellTopBar`
  - `TopBarIconButton`
  - `ShellTurtleButton`
- Preserve custom wordmark/title treatment and root icon ordering.

## 8. Core Reusable Components

| Component | Android file/source | Purpose | Visual treatment/key states | iOS recommendation | Initial iOS design system? |
|---|---|---|---|---|---|
| Scyra top bar | `SkillzTopAppBar.kt` | Root chrome/pages | Primary background, Caveat title, tab icons, Shell turtle | `ScyraTopBar` | Yes |
| Shell top bar | `ShellTopBar.kt` | Shell chrome | Shell-specific title/actions/Pearls | `ShellTopBar` | Yes |
| Cards | Many screens, Material `Card`/`ElevatedCard` | Content grouping | Parchment surfaces, rounded corners, tonal elevation | `ScyraCard` variants | Yes |
| Buttons | Material buttons | Primary/secondary actions | Material colors, rounded shapes | `ScyraButton`, `ScyraSecondaryButton` | Yes |
| Icon buttons | Top bars/cards | Compact actions | Material icons/image buttons with semantics | `TopBarIconButton` | Yes |
| Chips/stat pills | Reward, Shell, Story filters | Compact metadata/actions | Capsule shape, alpha surfaces | `ScyraChip`, `StatPill` | Yes |
| Score display | `ScoreDisplay.kt` | Score summary | Score label/value and period context | `ScoreDisplay` | Yes |
| Journey filters | `TagFilterRow.kt` | Filter history | Chips/row | `JourneyFilterRow` | Yes |
| Reward reveal cards | `RewardRevealDeck.kt`, `RewardChips.kt`, `RewardUX.kt` | Reward presentation | Custom deck, page indicator, glow, chips | `RewardRevealDeck/Card/Chip` | Yes |
| Health rows/cards | `HealthComponents.kt` | Health settings | Info cards, status pills, disable dialog | `HealthStatusCard` | Phase 2/full parity if Movement Points included |
| Notification inlay | `ShellNotificationsScreen.kt` | New rewards/badges | Elevated floating inlay/card | `ShellNotificationInlay` | Phase 2/full parity |
| Shell room nodes/cards | `ShellRootScreen.kt` | Room navigation | Orbit nodes/room cards | `ShellRoomNode` | Yes for Shell parity |
| Pearl display | `ShellPearlMiniIcon.kt`, `ShellTopBar.kt` | Currency | Pearl icon + count | `PearlBalanceView` | Yes |
| Creature tiles | `ShellObjectIcon.kt`, Chest/Blue files | Creatures | Circular/procedural icon, levels/counts | `CreatureTile` | Yes |
| Badge rows | `BadgesScreen.kt` | Countable achievements | List rows, counts | `BadgeRow` | Yes |
| Empty states | Story, Chest, The Blue, notifications | Empty guidance | Title/body/CTA | `ScyraEmptyState` | Yes |
| Dialogs/sheets | Flow/Lookout/Blue/Chest/Health | Confirmation/forms/details | Material AlertDialog/ModalBottomSheet | Custom/SwiftUI sheet/dialog wrappers | Yes |

## 9. Screen-Level Visual Map

| Screen | Android sources | Major visual sections | Special styling / states | iOS parity target |
|---|---|---|---|---|
| Home root | `SkillzHomeScreen.kt`, `SkillzTopAppBar.kt` | Top bar, internal Story/Paths/Notepad/Help pages | Caveat wordmark, central turtle action | Root container with custom top bar |
| Story/Chronicle | `StoryScreen.kt`, `StoryBody.kt`, header/chronicle files | Header, score, time/date filters, Journey filters, cards | Empty/first-time states, Flow/Pulse/Arc cards | Chronicle timeline with sheets |
| Paths | `PathsScreen.kt` | planned Flows, arcs, suggested routes, Plan Flow sheet | Menus, dialogs, tabs/time lens | Planning page/sheets |
| Notepad | `NotepadScreen.kt` | editor, search/font controls | Rich editor dependency; TODO exact design | Native notepad/editor page |
| Help/settings | `HelpScreen.kt`, `HealthComponents.kt` | help cards, settings rows, Health UI | Health disable dialog, language/score/calm settings | Settings/help page |
| Flow | `FlowScreen.kt`, flow components | title, Journey, notes, timer, Surge, Arc, Pulse, controls | Soft Flow, active Flow, dialogs | Custom Flow session screen |
| Reward reveal | `flow/reward/*` | card deck, chips, score, Pearls, creatures, badges, Stillwater | Custom reward deck, animations/styles | Custom reward modal/deck |
| Pulse | `PulseScreen.kt` | capture title/details/Journey/attach | Lightweight capture | Native sheet/route |
| Flow details | `FlowDetailsSheet.kt` | details, notes, Pulses | Sheet | SwiftUI sheet |
| Pulse edit | `PulseEditSheet.kt` | edit fields/Journey | Sheet | SwiftUI sheet |
| Plan Arc / Arc Detail | `PlanArcScreen.kt`, `ArcDetailScreen.kt` | wizard/detail cards/steps | Recurrence/custom days, launch payload | Native route/wizard |
| Shell root/Heart | `ShellRootScreen.kt`, `ShellTopBar.kt` | Heart room, orbit nodes, Pearl basin, shortcuts | Mythic/oceanic hub | Shell hub |
| The Blue | `rooms/blue/*` | ocean scene, depth rail, zones, tray, detail sheets | Procedural creatures, overlays | SwiftUI/Canvas room |
| The Chest | `ShellChestScreen.kt` | creature grid, stack detail, level/release | 104dp tiles, count/level badges | Creature-only inventory grid |
| Badges | `BadgesScreen.kt` | badge groups/list rows | Count display/objective groups | Badge list |
| Stillwater | `StillwaterRoomScreen.kt` | drops, vessels, draw/reveal | Calm soft-flow water tone | Stillwater room if in scope |
| Idea Grove | `IdeaGroveScreen.kt` | pulse idea cards/statuses | Dropdowns/snackbars/delete dialog | Idea garden |
| Lookout | `LookoutRoomScreen.kt` | objectives, period cards, dialogs | Progress bars/reward dialogs | Objectives room |
| Voyage Hall | `VoyageHallScreen.kt` | stats cards/records | Popup details | Stats room |
| Focus Room | `FocusRoomScreen.kt` | exercise list, ready/player/completion | Calm cards, TTS unavailable card, phase timer | Guided focus screen |
| Health UI | `HealthComponents.kt` | status cards/pills/permissions | Rounded info cards and disable dialog | HealthKit settings UI |

## 10. Flow Screen Visual System

- Active Flow hero/timer: `StopwatchSection.kt` uses timer display and reset confirmation; iOS should preserve large readable timer hierarchy.
- Title/name field: `GrandTitleField.kt` and Flow title field accessibility strings; use prominent title entry.
- Journey/tag treatment: `JourneyLean.kt` and Journey suggestions; use chip/autocomplete-like treatment.
- Notes/details field: `ChronicleField.kt` and description fields; reflective journal tone.
- Controls: pause/resume/complete/save/cancel actions in `FlowScreen.kt`; use clear primary/secondary action hierarchy.
- Soft Flow styling: copy and reward visuals distinguish unscored calm sessions; iOS should avoid score pressure.
- Surge controls: `SurgeMiniControl.kt` uses compact rounded control, alpha surfaces, and state-dependent expansion/radius.
- Arc pill/status: `ArcPill.kt` surfaces Arc context/progress.
- Pulse affordance: Flow has a Pulse dialog/action; visually lightweight, not reward-like.
- Notification/deep link visual relationship: notification opens Flow but no special in-app visual treatment beyond active state was observed. TODO: verify if active notification indicator appears in Flow UI.
- Confirmation dialogs: Soft Arc confirm, end confirm, Surge target, Pulse dialog, reward reveal.
- Haptics: Surge haptics are behavioral; visual feedback is state/progress in controls.

## 11. Reward Reveal Visual System

- Treatment: local dialog with custom card deck rather than generic text alert.
- Card deck: `RewardRevealDeck.kt` uses pager height 360dp, horizontal padding 20dp, page spacing 10dp, rounded card radius 28dp, and page indicator dots/capsules.
- Regular Flow reward content: `SessionRewardContent.kt`, `RewardChips.kt`, `RewardUX.kt` show score breakdown, reward metrics, Pearls, creatures, badges, Shell bridge cards.
- Soft Flow reward content: `SoftSessionRewardContent.kt` and mapper use Stillwater result cards/ripple style.
- Arc summary content: `ArcSummaryContent.kt` and mapper show Arc score, animals, badges, Stillwater, Shell bridge/story placeholders.
- Reward chips: `RewardChips.kt` uses capsule chips, tonal elevation, alpha surfaces, compact padding.
- Scyra Score/Pearls/Movement Points: displayed through reward model/chips/rows; exact math deferred.
- Creature drops/badges/Stillwater: card types and animation styles include animal, badge stamp, pearl glow, Stillwater ripple.
- Copy tone: celebratory, gentle, Shell-aware; avoid generic “success” copy.
- Dismissal/actions: continue, complete, open Shell, possibly continue Arc.

### iOS parity

- Do not use a generic alert for reward reveal.
- Build custom `RewardRevealDeck`, `RewardRevealCard`, and `RewardChip` components.
- Exact reward math belongs in `docs/06_REWARD_AND_ECONOMY_SPEC.md`.

## 12. Story / Chronicle Visual System

- Story header: `StoryHeader.kt`, sticky/scrollable header, period/date navigation, filters.
- Score display: `ScoreDisplay.kt`, period labels, surge bonus display, score visibility setting.
- Time filters/date navigation: `PeriodAndDateNavigator.kt`.
- Journey filters: `TagFilterRow.kt` chips/rows.
- Flow cards: `FlowCard.kt` show Flow summary/details.
- Pulse cards: `PulseCard.kt` show thought/idea record.
- Arc group cards: `ArcGroupCard.kt` groups Arc sessions/rewards.
- Saga/Journey detail visuals: `saga/` and `saga/journeys/` files; bottom sheet detail views.
- Empty states: `EmptyChroniclesState.kt`, `EmptySagasState.kt`, `FirstTimeUser.kt`.
- Edit/details sheets: Flow details and Pulse edit sheets use Material sheet/dialog patterns.
- iOS target: timeline/cards with filters, score visibility, and native sheets preserving Scyra copy/tone.

## 13. Paths / Arc Visual System

- Paths layout: `PathsScreen.kt` uses tabs/sections for planned Flows, Arc studio/more arcs, suggested routes, empty states, menus.
- Planned Flow cards: include title/Journey/soft/target/surge metadata and menus.
- Plan Flow sheet: `ModalBottomSheet` with fields, switches, target minutes, Surge options.
- Arc cards/detail: `ArcDetailScreen.kt` shows Arc overview, step rows, badges/metadata, edit/begin actions.
- Plan Arc wizard: `PlanArcScreen.kt` has staged flow identity, picker, route shape, timing/recurrence/custom days.
- Suggested Route detail: `SuggestedRouteDetailScreen.kt` uses route summary/steps/save/begin cards.
- Menus/dialogs: dropdown menus and delete dialogs in Paths.
- iOS target: route/wizard/card system with clear planned Flow and Arc launch payloads.

## 14. Shell Visual System

- Shell root/Heart: `ShellRootScreen.kt` uses a Heart room hub, orbit pairs/nodes, center content, Pearl basin, shortcuts, and contextual invites.
- Room orbit/nodes: room entrances arranged around Heart with selected/active/indicator states.
- Pearl basin/display: Shell top/root surfaces show Pearl balance and basin copy.
- Chest/Badges shortcuts: quick access from Heart.
- Notifications/inlay: Shell notification inlay/screen uses elevated floating surfaces.
- Room preview cards/contextual actions: room descriptions and CTAs preserve Shell story tone.
- Shell top bar: separate from root app bar; uses Shell-specific actions and Pearl mini icon.
- Creature/display state: Focus/The Blue/Chest states influence Shell indicators.
- Room backgrounds: `TurtleShellInteriorBackground.kt` and draw files use layered translucent primary/surface/secondary colors.
- Tone: The Shell should feel like a room/hub, not a generic settings page.

### iOS parity

- Build reusable Shell components: `ShellRoomNode`, `ShellRoomCard`, `PearlBalanceView`, `ShellInlayCard`, `ShellBackground`.
- Preserve mythic/oceanic room feeling.

## 15. The Blue Visual System

- Ocean scene: `TheBlueRoomScreen.kt`, `TheBlueZonePage.kt`, `TheBlueSceneSafeBounds.kt`, `TheBlueOverlaySurface.kt`.
- Zone/depth pages: Sunlit Reef, Deeper Reef, Open Blue, Great Blue; depth rail in `TheBlueDepthRail.kt`.
- Creature tray/tile: `TheBlueCreatureTray.kt`, `TheBlueCreatureTile.kt`.
- Creature drawing/procedural visuals: `TheBlueRenderedCreature.kt`, `creatures/Life.kt`, `PresenceAccounting.kt`, and `draw/DrawTheBlue.kt` draw creatures procedurally with Canvas-like Compose drawing.
- Animal detail sheet: `TheBlueAnimalDetailSheet.kt`, level counts, highest level, Focus/Chest counts, growth/release actions.
- Release confirmation: `ReleaseCreatureConfirmationSheet.kt`.
- Beyond Blue: `BeyondBlueEncounterSheet.kt` with bottom sheet and confirmation dialog.
- Empty/no-creature states: `TheBlueEmptyOceanPage.kt` and strings for no animals/quiet water.
- Pearl/level/growth indicators: metric pills, level count chips, growth cost text.
- Scene overlays: safe bounds/overlay surfaces and procedural ocean composition.

### iOS parity

- Recreate creatures with SwiftUI `Canvas`/custom drawing or native assets copied into `ios/` later; do not reference Android drawables directly.
- Preserve depth zones, tray/detail, level-aware interactions, and release/Beyond Blue confirmation flows.

## 16. The Chest Visual System

- Screen: `ShellChestScreen.kt`.
- Product-facing name: The Chest, not Shell Chest.
- Inventory grid: adaptive `104.dp` creature tiles, 10dp spacing, 16dp screen padding.
- Creature icon tile: `ShellObjectIcon` inside a 104dp tile, 54dp centered icon.
- Top-right count badge: shown when stack count > 1; capsule shape with tonal/shadow elevation.
- Bottom level badge: level indicator on tile; stack detail shows level and mastery tier.
- Stack detail visual: 20dp padding, 64dp icon, title, level, owned count, source, level-up status, release controls.
- Release confirmation: dialog with reward preview.
- Level-up confirmation: dialog with cost and confirmation.
- Empty state: empty title/body and CTA to The Blue.
- Product rules: no trinkets/objects/released creatures; group by species and level.

### iOS parity

- `CreatureInventoryTile` with top-right count and bottom level badge.
- RuneScape-bank-like icon grid direction.
- Do not copy Android legacy “Shell Chest” naming into UI.

## 17. Badges Visual System

- Screen: `BadgesScreen.kt`.
- Layout: lazy list with content padding 16dp and 10dp vertical spacing.
- Badge groups: objective badge groups and catalog badges.
- Count display: badge row title includes title × count.
- Viewed/new state: stored in `UserBadgeEntity`; notification inlay uses viewed state; Badge screen visually shows counts.
- Objective badges: grouped by Journey/objective completions.
- Creature Mastery expectations: product requires species Mastery badges with count per Level 99 individual; Android currently confirms Level 99 tiers but exact visual badge path is TODO.
- Empty state: TODO: verify Badges empty state visuals.

### iOS parity

- Countable badge rows/tiles with clear title, count, description.
- Mastery badges should be visually distinct and lifetime/countable.

## 18. Focus Room Visual System

- Exercise list: `FocusRoomScreen.kt` cards from `FocusRoomOriginalExercises`.
- Ready card: selected exercise description and Start guided exercise button.
- Player content: player header, guided prompt card, visual animation, timer, controls.
- Completion content: completion state after exercise.
- Voice toggle: voice control card; `FocusExerciseVoiceGuide.kt` TextToSpeech lifecycle.
- Phase timers: `PhaseCountdownPill` for breathing phase steps.
- Progress indication: current step/remaining time and visual animations.
- Calm treatment: rounded cards, supportive copy, no score/reward language.
- TTS unavailable card: inline card with retry/fallback copy.
- Controls: pause/resume/restart/end.

Product rules:

- Exercise should not auto-start; Android requires user Start.
- Focus Room should feel calm and non-reward-chasing.
- No rewards/stats from Focus Room exercises.

## 19. Dialogs, Sheets, Popups, and Inlays Visual Style

| Category | Android examples | Visual behavior | iOS recommendation |
|---|---|---|---|
| AlertDialogs | Flow end/Surge/Pulse/soft Arc, Chest confirmations, Health disable | Material dialog with title/body/buttons | `ScyraDialog` / SwiftUI confirmation dialog with custom content where needed |
| ModalBottomSheets | Plan Flow, Blue animal detail, Beyond Blue | Rounded sheet with form/detail content | SwiftUI sheet with Scyra card styling |
| Custom card deck dialogs | Flow reward reveal | AlertDialog containing custom pager/card deck | Custom reward modal, not generic alert |
| Confirmation dialogs | Chest release/level-up, Lookout remove | Explicit confirm/cancel | Confirmation dialog/sheet |
| Permission gates | NotificationPermissionGate, Health UI | Inline/root permission guidance | Native permission flow + explanatory card |
| Notification inlays | ShellNotificationsScreen | Elevated floating inlay/card, max width/height | Popover/inlay card |
| Snackbars | IdeaGrove events | Short feedback | SwiftUI toast/banner/snackbar |
| Dropdown menus | Paths menus, Idea Grove item menu | Overflow menu | SwiftUI Menu/context menu |
| Date pickers | Lookout date picker | DatePickerDialog | SwiftUI DatePicker sheet/dialog |
| Reward reveal | Reward deck/cards/chips | Satisfying reward presentation | Custom Scyra reward surface |
| Soundscape picker | TODO: verify | Not confirmed | If implemented, popup/dialog, not bottom sheet |

## 20. Icon and Asset Inventory

| Asset/category | Android location | Usage | iOS destination recommendation | Copy/recreate/redraw | Notes |
|---|---|---|---|---|---|
| App launcher icons | `res/mipmap-*`, `mipmap-anydpi-v26` | Android launcher | iOS asset catalog under `ios/` later | Recreate/copy into iOS assets later | Do not reference Android. |
| Play Store icon | `android/app/src/main/ic_launcher-playstore.png` | Store listing | App Store marketing asset if needed | Recreate/copy later | TODO licensing/source. |
| Turtle logo | `res/drawable/scyra_turtle.png` | Shell top-bar turtle, branding | iOS asset catalog | Copy/re-export later into `ios/` | Needs local iOS copy. |
| Splash turtle drawables | `ic_scyra_splash_base.xml`, `ic_scyra_splash_animated.xml`, animator | Android splash | iOS launch screen/native animation | Recreate/adapt | iOS launch constraints differ. |
| Notification icon | `ic_scyra_notification.xml` | Android notifications | iOS notification icon not equivalent | Recreate if needed | Platform-specific. |
| Launcher foreground/background | drawable + mipmap XML | Android app icon | iOS AppIcon set | Recreate | App icon rules differ. |
| Android Caveat font | `res/font/caveatsb.ttf` | Android title/accent only | Do not copy to iOS | Skip for iOS | iOS uses regular/system typography. |
| Shell procedural icons | `ui/screen/shell/icons/`, `icons/draw/` | Shell objects/creatures/backgrounds | SwiftUI/Canvas drawing files | Redraw natively or asset-export | Prefer native drawing where feasible. |
| The Blue creatures | `rooms/blue/creatures/`, `draw/DrawTheBlue.kt` | Procedural ocean creatures | SwiftUI Canvas or local assets | Redraw natively | High parity risk. |
| Localized strings | `values-*` | Text layout/copy | String Catalog/Localizable.strings | Recreate translations | Requires layout QA. |
| Audio/raw assets | none observed | N/A | TODO | TODO | Focus uses TTS; soundscapes unknown. |

Repo-boundary rule: iOS must eventually receive its own local copy or native equivalent inside `ios/`; iOS must not reference assets from `android/`.

## 21. Localization and Copy Presentation

- Base strings: `android/app/src/main/res/values/strings.xml` contains extensive UI labels, dialog copy, reward copy, Shell room copy, and accessibility strings.
- Localized folders: `values-es`, `values-hi`, `values-mr` exist.
- Visual layout impact:
  - Long reward/Help/Shell/Health strings require flexible wrapping.
  - Top bar labels are mostly icon-only with accessibility labels, reducing visible width risk.
  - Devanagari localizations may require fallback fonts and larger line heights.
  - Buttons/dialogs should support multiline labels.
- Language setting behavior: `UserPrefs.appLanguageTag`, `SkillzApplication`, and `AppLocaleManager` apply language on Android.
- iOS recommendation: use String Catalogs or `Localizable.strings`; design flexible card/sheet layouts for Spanish/Hindi/Marathi.

## 22. Accessibility and Semantics

Observed behavior:

- `SkillzTopAppBar.kt` sets content descriptions, semantic role `Tab`, selected state, and state descriptions for root page icons.
- Shell turtle icon has content description from `shell_icon_a11y`.
- Story/Flow/Pulse strings include accessibility labels for cards, fields, and filters.
- The Blue strings include accessibility descriptions for depth rail, zones, scenes, animal cards, and details.
- The Chest includes stack accessibility strings with species, level, and owned count.
- Notification permission and Health components include explanatory copy.
- Many decorative icons pass `contentDescription = null`, which is correct when surrounding text/semantics carry meaning.

TODOs:

- Verify all custom-drawn Shell/The Blue interactive creatures have accessible labels at point of interaction.
- Verify Focus Room player controls and TTS unavailable states have complete labels.
- Verify Dialog/Sheet focus order in Compose and iOS equivalent.

### iOS parity

- Provide VoiceOver labels for icon-only actions.
- Preserve semantic grouping for top bar tabs/cards.
- Custom-drawn interactive creatures/icons must expose species, level, count, zone, and action labels.

## 23. iOS Design Token Recommendations

Recommended future names only; do not create iOS code.

### Colors

- `ScyraColor.primaryTeal`
- `ScyraColor.referenceBlue` (optional prior/secondary reference)
- `ScyraColor.aeraTeal`
- `ScyraColor.parchment`
- `ScyraColor.surfaceParchment`
- `ScyraColor.darkBackground`
- `ScyraColor.darkSurface`
- `ScyraColor.pearlGold`
- `ScyraColor.bronze`
- `ScyraColor.amethyst`
- `ScyraColor.rewardGlow`
- `ScyraColor.stillwaterRipple`
- `ScyraColor.shellGlow`
- `ScyraColor.theBlueDepthSunlit/Deeper/Open/Great`

### Typography

- `ScyraTypography.wordmark`
- `ScyraTypography.brandTitle`
- `ScyraTypography.screenTitle`
- `ScyraTypography.cardTitle`
- `ScyraTypography.body`
- `ScyraTypography.label`
- `ScyraTypography.rewardNumber`
- `ScyraTypography.inventoryBadge`
- `ScyraTypography.focusPrompt`

### Spacing/radius/icon sizes

- `ScyraSpacing.screenPadding`
- `ScyraSpacing.cardPadding`
- `ScyraSpacing.sectionGap`
- `ScyraSpacing.inlineGap`
- `ScyraRadius.cardSmall/card/large/capsule`
- `ScyraIconSize.topBar`
- `ScyraIconSize.shellTurtle`
- `CreatureInventoryTile.size`

### Components

- `ScyraComponent.ScyraCard`
- `ScyraComponent.ScyraTopBar`
- `ScyraComponent.ShellTopBar`
- `ScyraComponent.TopBarIconButton`
- `ScyraComponent.RewardRevealDeck`
- `ScyraComponent.RewardRevealCard`
- `ScyraComponent.RewardChip`
- `ScyraComponent.CreatureInventoryTile`
- `ScyraComponent.PearlBalanceView`
- `ScyraComponent.ShellRoomNode`
- `ScyraComponent.BadgeRow`
- `ScyraComponent.HealthStatusCard`

## 24. Design Risks and Open Questions

| Risk/open question | Why it matters | iOS impact | Recommended follow-up |
|---|---|---|---|
| Exact Android color token mapping | Theme constants, XML colors, flavor colors, and hardcoded colors are not fully normalized. | iOS could choose wrong primary/surface palette. | Color-token audit with screenshots. |
| Stale color comments/names | Product names differ from Android names like Gryffindor/Ravenclaw. | Bad naming leaks into iOS design system. | Rename semantics in docs/tokens, not necessarily Android code yet. |
| Material 3 vs Scyra-native SwiftUI | Android uses Material, but Scyra identity is custom. | Overly native/generic iOS UI may dilute identity. | SwiftUI component/design-system spec. |
| Android Caveat font divergence | Android uses CaveatSB but iOS product direction uses regular/system typography. | Visual title treatment will differ intentionally. | Define system-font title style in iOS design tokens. |
| Creature procedural drawing parity | The Blue creatures are custom Compose drawings. | Hard to match in SwiftUI/Canvas. | Creature drawing parity prototype/spec. |
| The Blue scene complexity | Zones, tray, overlays, movement/presence accounting are visual-heavy. | High implementation risk. | Dedicated The Blue visual/interaction spec. |
| Shell room visual phasing | Many rooms have distinct visuals. | phasing risk. | iOS visual phasing decision. |
| Focus Room TTS/audio states | Android uses TTS with fallback cards. | iOS audio/TTS UI may differ. | Focus Room audio/UI decision. |
| Soundscape picker scope | No confirmed implementation found. | Risk of inventing UI/assets. | Verify scope; if implemented, use popup/dialog. |
| Animation parity | Splash, reward reveal, The Blue/Focus visuals use animation concepts. | Static iOS UI may feel flat. | Animation inventory/prototype. |
| Localization layout risk | Spanish/Hindi/Marathi strings may expand/change font needs. | Layout clipping and font fallback issues. | Localization QA/design rules. |
| Accessibility gaps | Custom drawings and icon-only actions need labels. | VoiceOver parity risk. | Accessibility audit before iOS build. |
| Aera visual theme | Android has Aera flavor; iOS should start Scyra-first. | Extra theme support increases scope. | Product decision on Aera iOS. |

## 25. Acceptance Criteria for This Document

- This task only creates or changes `docs/04_DESIGN_SYSTEM.md`.
- Android source code is untouched.
- No iOS source code or project is created.
- No assets are copied into `ios/`.
- No Gradle/build files are changed.
- The document is based on actual Android theme/resource/UI files plus prior docs.
- Colors, typography, spacing, shapes, app chrome, core components, major screen visuals, Shell visuals, The Blue visuals, The Chest visuals, reward reveal visuals, and asset inventory are documented or marked `TODO: verify`.
- iOS parity recommendations preserve repo boundary rules.
- Unclear behavior is marked `TODO: verify`.

## 26. Codex Summary

- Docs and Android files/resources inspected: `docs/00_REPO_BOUNDARIES.md`, `docs/01_ANDROID_ARCHITECTURE.md`, `docs/02_SCYRA_PRODUCT_SPEC.md`, `docs/03_NAVIGATION_AND_SCREEN_MAP.md`, Android colors/themes/strings/localizations/font/drawable/mipmap resources, Compose theme files, app top bar, Flow/reward, Story, Paths/Arc, Shell, The Blue, The Chest/Badges, Focus Room, Health UI, and UI mapper tests.
- Major color/typography/design findings: teal (`0xFF3F8F8B` / Android teal direction) is the main Scyra iOS color; `0xFF2F4F6F` remains an Android/prior-reference blue where source still uses it; parchment surfaces and Pearl/gold accents are core; Android uses CaveatSB for the wordmark but iOS should use regular/system typography.
- Major reusable components identified: Scyra top bar, Shell top bar, cards, buttons, icon buttons, chips/stat pills, score display, Journey filters, reward reveal deck/cards/chips, Health cards, Shell inlays, room nodes, Pearl display, creature tiles, badge rows, empty states, dialogs/sheets.
- Major screen visual systems mapped: Home/Story/Paths/Notepad/Help, Flow, reward reveal, Pulse, Story/Chronicle, Paths/Arc, Shell root/Heart, The Blue, The Chest, Badges, Stillwater, Idea Grove, Lookout, Voyage Hall, Focus Room, Health settings.
- Asset categories discovered: launcher/app icons, Play Store icon, owner-created turtle logo, splash drawables/animator, notification icon, Android-only Caveat font, Shell procedural icons/drawings, The Blue procedural creature drawing code, localized strings; no `res/raw` audio assets observed.
- Highest-risk visual parity areas for iOS replication: teal token normalization, stale Android color naming, SwiftUI-vs-Material component strategy, intentional system-font title treatment, The Blue procedural drawing/scene parity, Shell room phasing, Focus Room TTS/audio states, soundscape scope, animation parity, localization layout, accessibility, and Aera theme support.
- Anything outside `docs/04_DESIGN_SYSTEM.md` changed: no.
- Repo boundary rules preserved: yes. The document is reference-only under `docs/`; no Android code/build files were modified, no assets were copied, and no iOS project/files were created.
