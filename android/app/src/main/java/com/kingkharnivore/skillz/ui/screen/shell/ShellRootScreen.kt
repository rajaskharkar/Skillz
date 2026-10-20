@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import android.util.Log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.TurtleShellInteriorBackground
import com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesScreen
import com.kingkharnivore.skillz.ui.screen.shell.inventory.MasteryCelebrationScreen
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellChestScreen
import com.kingkharnivore.skillz.data.repository.shell.SHELL_BADGES_ROUTE
import com.kingkharnivore.skillz.data.repository.shell.SHELL_CHEST_ROUTE
import com.kingkharnivore.skillz.ui.screen.shell.inventory.NotificationInlayOverlay
import com.kingkharnivore.skillz.ui.screen.shell.inventory.unviewedShellNotifications
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.TheBlueRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.rooms.focus.FocusRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.rooms.ideagrove.IdeaGroveRoute
import com.kingkharnivore.skillz.ui.screen.shell.rooms.lookout.LookoutRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.rooms.voyage.VoyageHallScreen
import com.kingkharnivore.skillz.ui.screen.shell.ux.activeChestCreatureCount
import com.kingkharnivore.skillz.ui.screen.shell.ux.activeChestCreatures
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellIndicatorColor
import com.kingkharnivore.skillz.utils.shell.shellBackground
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.ShellViewModel

private fun hasChestCreatures(uiState: ShellUiState): Boolean = activeChestCreatureCount(uiState) > 0

private fun hasAffordablePearlShape(uiState: ShellUiState): Boolean = false

private fun unseenNotificationCount(uiState: ShellUiState): Int =
    unviewedShellNotifications(uiState).size

@Composable
fun sourceReasonFor(def: ShellFindDefinition): String = when (def.findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.shell_find_minnow_description)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.shell_find_seahorse_description)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.shell_find_manta_description)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.shell_find_whale_description)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.shell_find_octopus_description)
    ShellContentCatalog.FOCUS_PEBBLE -> stringResource(R.string.shell_find_pebble_description)
    ShellContentCatalog.FOCUS_LAMP -> stringResource(R.string.shell_source_invited_with_pearls, def.pearlCost ?: 0)
    ShellContentCatalog.FOCUS_PERCH -> stringResource(R.string.shell_source_invited_with_pearls, def.pearlCost ?: 0)
    ShellContentCatalog.FOCUS_PEBBLES -> stringResource(R.string.shell_source_invited_with_pearls, def.pearlCost ?: 0)
    ShellContentCatalog.FOCUS_CURTAIN -> stringResource(R.string.shell_source_invited_with_pearls, def.pearlCost ?: 0)
    ShellContentCatalog.FOCUS_BUBBLES -> stringResource(R.string.shell_source_invited_with_pearls, def.pearlCost ?: 0)
    else -> stringResource(def.descriptionRes)
}

@Composable
fun ShellRootScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isFlowActive: Boolean = false,
    onLaunchFlowForJourney: (String) -> Unit = {},
    onLaunchObjectiveFlow: (String, com.kingkharnivore.skillz.model.FlowMode) -> Unit = { _, _ -> },
    onLaunchFlowFromPulse: (Long, String, String?) -> Unit = { _, _, _ -> },
    onOpenActiveFlow: () -> Unit = {},
    onPlanArc: () -> Unit = {},
    onMovementInfo: () -> Unit = {},
    viewModel: ShellViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val greenViewModel: com.kingkharnivore.skillz.viewmodel.green.GreenViewModel = hiltViewModel()
    val greenState by greenViewModel.state.collectAsStateWithLifecycle()
    var greenSeedId by rememberSaveable { mutableStateOf<String?>(null) }
    var greenEnvironment by rememberSaveable { mutableStateOf(com.kingkharnivore.skillz.domain.green.GreenEnvironment.GARDEN) }
    val greenRoomState = rememberSaveableStateHolder()
    var greenVisitId by rememberSaveable { mutableStateOf(0) }
    var greenBadgesRequested by rememberSaveable { mutableStateOf(false) }
    var badgeReturnDestination by rememberSaveable(stateSaver = ShellDestination.StateSaver) { mutableStateOf<ShellDestination>(ShellDestination.Heart) }
    var chestPlants by remember { mutableStateOf(false) }
    var chestPlantFocus by remember { mutableStateOf<String?>(null) }
    var chestSpecimenFocus by remember { mutableStateOf<String?>(null) }
    var showRedTrade by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activeFlowMessage = stringResource(R.string.lookout_flow_already_active)
    var destination by rememberSaveable(stateSaver = ShellDestination.StateSaver) { mutableStateOf<ShellDestination>(ShellDestination.Heart) }
    LaunchedEffect(destination) {
        if (destination != ShellDestination.TheGreen) greenSeedId = null
        if (destination != ShellDestination.Badges) {
            greenBadgesRequested = false
            badgeReturnDestination = ShellDestination.Heart
        }
        if (destination != ShellDestination.ShellChest) { chestPlants = false; chestPlantFocus = null; chestSpecimenFocus = null }
    }
    var redSpecies by remember { mutableStateOf<String?>(null) }
    var blueRealm by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<com.kingkharnivore.skillz.utils.shell.CreatureRealm?>(null) }
    var pendingNavigation by remember { mutableStateOf<PendingShellNavigation?>(null) }
    val handledNavigationRequestIds = remember { mutableSetOf<String>() }
    var showNotifications by remember { mutableStateOf(false) }
    val notificationCount = unseenNotificationCount(uiState)

    fun handleNavigationResult(requestId: String, result: NavigationConsumptionResult) {
        if (pendingNavigation?.requestId != requestId) return
        when (result) {
            NavigationConsumptionResult.Pending -> return
            NavigationConsumptionResult.Consumed -> {
                if (!handledNavigationRequestIds.add(requestId)) return
                if (NotificationAcknowledgementPolicy.shouldMarkViewed(result)) {
                    pendingNavigation?.notificationId?.let(viewModel::markNotificationViewed)
                }
                pendingNavigation = null
                showNotifications = false
            }
            is NavigationConsumptionResult.Failed -> {
                if (!handledNavigationRequestIds.add(requestId)) return
                Log.w("ShellNavigation", "request_failed reason=${result.reason} request=${pendingNavigation?.javaClass?.simpleName}")
                pendingNavigation = null
            }
        }
    }

    fun dispatchBadgeDestination(
        request: BadgeActionDestination,
        notificationId: String? = null
    ): NavigationConsumptionResult {
        if (request is BadgeActionDestination.Green) {
            val species = request.speciesId?.let(com.kingkharnivore.skillz.domain.green.GreenCatalogue.byId::get)
            if (!request.plantSeed && greenState.specimens.any { it.speciesId == request.speciesId }) {
                chestPlants = true; chestPlantFocus = request.speciesId; chestSpecimenFocus = request.specimenId; destination = ShellDestination.ShellChest
            } else {
                // A new explicit destination overrides any previously retained Green view.
                greenRoomState.removeState(greenVisitId)
                greenVisitId++
                greenEnvironment = species?.environment ?: com.kingkharnivore.skillz.domain.green.GreenEnvironment.entries.firstOrNull { it.id == request.environmentId }
                    ?: com.kingkharnivore.skillz.domain.green.GreenEnvironment.GARDEN
                greenSeedId = species?.id; destination = ShellDestination.TheGreen
            }
            return NavigationConsumptionResult.Consumed
        }
        val targetSpecies = when(request) {
            is BadgeActionDestination.BlueRegion -> request.speciesId
            is BadgeActionDestination.ChestSpecies -> request.speciesId
            is BadgeActionDestination.BeyondBlue -> request.speciesId
            else -> null
        }
        if (targetSpecies in com.kingkharnivore.skillz.utils.shell.RedCreatureCatalog.byId) {
            redSpecies = targetSpecies
            destination = ShellDestination.TheRed
            return NavigationConsumptionResult.Consumed
        }
        val dispatch = ShellNavigationCoordinator.dispatch(request, notificationId)
        if (dispatch != null) {
            if (dispatch.destination == ShellDestination.Badges && destination != ShellDestination.Badges) {
                badgeReturnDestination = destination
            }
            pendingNavigation = dispatch.pending
            destination = dispatch.destination
            return NavigationConsumptionResult.Pending
        }
        return when (request) {
            BadgeActionDestination.Flow -> {
                if (isFlowActive) onOpenActiveFlow() else onLaunchFlowForJourney("")
                NavigationConsumptionResult.Consumed
            }
            BadgeActionDestination.Arc -> { onPlanArc(); NavigationConsumptionResult.Consumed }
            BadgeActionDestination.MovementInfo -> { onMovementInfo(); NavigationConsumptionResult.Consumed }
            else -> NavigationConsumptionResult.Failed(NavigationFailureReason.UNSUPPORTED_REQUEST)
        }
    }

    val snackbarResources = androidx.compose.ui.platform.LocalResources.current
    LaunchedEffect(viewModel, snackbarResources) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(when (message) {
                is com.kingkharnivore.skillz.viewmodel.shell.UiText.Resource -> snackbarResources.getString(message.resId, *message.args.toTypedArray())
            })
        }
    }

    LaunchedEffect(destination) {
        val room = when (destination) {
            ShellDestination.TheRed -> ShellRoomId.THE_RED
            ShellDestination.Heart -> ShellRoomId.HEART
            ShellDestination.Focus -> ShellRoomId.FOCUS
            ShellDestination.TheGreen -> ShellRoomId.THE_GREEN
            ShellDestination.VoyagePreview -> ShellRoomId.VOYAGE
            ShellDestination.TheBluePreview -> ShellRoomId.THE_BLUE
            ShellDestination.IdeaGrovePreview -> ShellRoomId.IDEA_GROVE
            ShellDestination.LookoutPreview -> ShellRoomId.LOOKOUT
            else -> null
        }

        room?.let(viewModel::markRoomOpened)
        if (destination == ShellDestination.TheBluePreview) {
            viewModel.markTheBlueAnimalsSeen()
        }
    }

    fun leaveCurrentShellScreen() {
        when {
            showNotifications -> showNotifications = false
            destination == ShellDestination.TheBluePreview && blueRealm != null -> blueRealm = null
            destination == ShellDestination.Badges -> destination = badgeReturnDestination
            destination == ShellDestination.Heart -> onBack()
            else -> destination = ShellDestination.Heart
        }
    }

    BackHandler(enabled = showNotifications || destination != ShellDestination.Heart) {
        leaveCurrentShellScreen()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShellTopBar(
                destination = destination,
                pearlBalance = uiState.pearlBalance,
                pebbleBalance = uiState.pebbleBalance,
                dropBalance = greenState.drops,
                pearlBasinHasIndicator = hasAffordablePearlShape(uiState),
                notificationCount = notificationCount,
                blueRealm = blueRealm,
                onBack = {
                    // Use the same dispatcher as system Back so overlays and nested rooms
                    // get first refusal before the Shell changes its destination.
                    if (backDispatcher != null && (showNotifications || destination != ShellDestination.Heart)) {
                        backDispatcher.onBackPressed()
                    } else {
                        leaveCurrentShellScreen()
                    }
                },
                onNotifications = { showNotifications = !showNotifications }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(shellBackground())
        ) {
            when (destination) {
                ShellDestination.TheRed -> com.kingkharnivore.skillz.ui.screen.shell.rooms.red.RedRoute(
                    onBadges = { badgeReturnDestination = ShellDestination.TheRed; destination = ShellDestination.Badges }, onTrade = { showRedTrade = true }, initialCreature = redSpecies,
                    onCreatureOpened = { redSpecies = null })
                ShellDestination.Heart -> HeartRoomScreen(
                    uiState = uiState,
                    dropBalance = greenState.drops,
                    onNavigate = { destination = it }
                )

                ShellDestination.Focus -> FocusRoomScreen()

                ShellDestination.TheGreen -> greenRoomState.SaveableStateProvider(greenVisitId) {
                    com.kingkharnivore.skillz.ui.screen.shell.rooms.green.GreenRoute(calmMode = uiState.calmMode,
                        viewModel = greenViewModel, initialEnvironment = greenEnvironment, initialSeedId = greenSeedId,
                        onOpenChest = { chestPlants = true; destination = ShellDestination.ShellChest },
                        onOpenBadges = { badgeReturnDestination = ShellDestination.TheGreen; greenBadgesRequested = true; viewModel.setBadgeCategory(com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory.GREEN); destination = ShellDestination.Badges })
                }

                ShellDestination.ShellChest -> ShellChestScreen(
                    uiState = uiState,
                    onReleaseCreaturesByLevel = viewModel::releaseCreaturesByLevel,
                    onLevelUpCreature = viewModel::growCreatureLevels,
                    onOpenBlue = { destination = ShellDestination.TheBluePreview },
                    onSortOptionSelected = viewModel::setChestSortOption,
                    onFilterSelected = viewModel::setChestFilter,
                    onEnvironmentSelected = viewModel::setChestEnvironment,
                    onClearFilters = viewModel::clearChestFilters,
                    onOpenRed = { destination = ShellDestination.TheRed },
                    greenState = greenState, onWaterPlant = greenViewModel::water, onReleasePlant = greenViewModel::release,
                    onDismissGreenFeedback = { greenViewModel.dismissFeedback(greenState.feedbackId) },
                    onOpenGreen = { greenSeedId = null; destination = ShellDestination.TheGreen },
                    showPlantsInitially = chestPlants, focusPlantSpeciesId = chestPlantFocus, focusPlantSpecimenId = chestSpecimenFocus,
                    focusSpeciesId = (pendingNavigation as? PendingShellNavigation.OpenChestSpecies)?.speciesId,
                    focusRequestId = (pendingNavigation as? PendingShellNavigation.OpenChestSpecies)?.requestId,
                    onFocusResult = ::handleNavigationResult
                )

                ShellDestination.Badges -> BadgesScreen(
                    initialBookSection = if (greenBadgesRequested) com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgeBookSection.ALL_BADGES else com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgeBookSection.TRACKED,
                    initialTab = if (greenBadgesRequested)
                        com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesTab.BADGE_BOOK else com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesTab.SHOWCASE,
                    uiState = uiState,
                    onPin = viewModel::pinBadge,
                    onDismissPinReplacement = viewModel::dismissPinReplacement,
                    onUnpin = viewModel::unpinBadge,
                    onTrack = viewModel::trackBadge,
                    onUntrack = viewModel::untrackBadge,
                    onCategory = viewModel::setBadgeCategory,
                    onSort = viewModel::setBadgeSort,
                    onBadgeViewed = { viewModel.markBadgeViewed(it) },
                    onAcknowledgeBackfill = viewModel::acknowledgeBackfill,
                    onNavigate = { dispatchBadgeDestination(it) },
                    onOpenFlow = { if (isFlowActive) onOpenActiveFlow() else onLaunchFlowForJourney("") },
                    onOpenArc = onPlanArc,
                    onRetryInitialization = viewModel::initializeAchievements,
                    pendingNavigation = pendingNavigation,
                    onNavigationResult = ::handleNavigationResult
                )

                ShellDestination.VoyagePreview -> VoyageHallScreen()

                ShellDestination.TheBluePreview -> TheBlueRoomScreen(
                    realm = blueRealm,
                    onSelectRealm = { blueRealm = it },
                    uiState = uiState,
                    onGrowCreature = { id -> viewModel.growCreature(id, "BLUE") },
                    onReleaseCreaturesByLevel = viewModel::releaseCreaturesByLevel,
                    onEncounterBeyondBlue = viewModel::encounterBeyondBlue,
                    onOpenChest = { destination = ShellDestination.ShellChest },
                    focusRequest = pendingNavigation,
                    onFocusResult = ::handleNavigationResult
                )

                ShellDestination.IdeaGrovePreview -> IdeaGroveRoute(
                    onNavigateToFlow = onLaunchFlowFromPulse,
                    onNavigateToCurrentFlow = onOpenActiveFlow,
                    onSnackbar = { message, action ->
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = action
                        )
                        if (action != null && result == SnackbarResult.ActionPerformed) onOpenActiveFlow()
                    }
                )

                ShellDestination.LookoutPreview -> LookoutRoomScreen(
                    onLaunchFlowForJourney = { journeyName, mode ->
                        if (isFlowActive) {
                            scope.launch { snackbarHostState.showSnackbar(activeFlowMessage) }
                        } else {
                            onLaunchObjectiveFlow(journeyName, mode)
                        }
                    }
                )
            }
        }

        if (showRedTrade) {
            com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.BeyondBlueEncounterSheet(
                pearlBalance = uiState.pearlBalance,
                initialZone = TheBlueZoneId.SUNLIT_REEF,
                activeAnimalInstances = uiState.finds.filter {
                    it.creatureStatus == com.kingkharnivore.skillz.utils.shell.CreatureStatus.ACTIVE &&
                        ShellContentCatalog.find(it.findId)?.kind == com.kingkharnivore.skillz.data.model.shell.ShellRewardKind.ANIMAL
                },
                onDismiss = { showRedTrade = false },
                onEncounter = { target, selected ->
                    viewModel.encounterBeyondBlue(target, selected)
                    showRedTrade = false
                }
            )
        }

        if (showNotifications) {
            NotificationInlayOverlay(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onDismiss = { showNotifications = false },
                onMarkNotificationViewed = viewModel::markNotificationViewed,
                onMarkAllViewed = viewModel::markAllNotificationsViewed,
                onFindDestination = { notification ->
                    dispatchBadgeDestination(
                        BadgeActionDestination.ChestSpecies(notification.findId), notification.id
                    )
                },
                onBadgeDestination = { notification ->
                    dispatchBadgeDestination(notification.destination, notification.id)
                }
            )
        }

        uiState.masteryCelebration?.let { celebration ->
            Box(Modifier.padding(padding)) {
                MasteryCelebrationScreen(
                    event = celebration,
                    uiState = uiState,
                    onBegin = { viewModel.beginCelebration() },
                    onAdvance = { reduced -> viewModel.advanceCelebration(reduced) },
                    onPrevious = { viewModel.previousCelebration() },
                    onComplete = { origin ->
                        viewModel.completeCelebration {
                            destination = when (origin) {
                                "RED" -> ShellDestination.TheRed
                                "BLUE" -> ShellDestination.TheBluePreview
                                "STILLWATER" -> ShellDestination.TheBluePreview
                                else -> ShellDestination.ShellChest
                            }
                        }
                    },
                    onPin = { badgeId, replacementId -> viewModel.pinBadge(badgeId, replacementId) },
                    onDismissPinReplacement = viewModel::dismissPinReplacement,
                    onUnpin = viewModel::unpinBadge,
                    onTrack = viewModel::trackBadge,
                    onUntrack = viewModel::untrackBadge,
                    onNavigate = { dispatchBadgeDestination(it) }
                )
            }
        }

    }
}

@Composable
internal fun HeartRoomScreen(
    uiState: ShellUiState,
    dropBalance: Long = 0,
    onNavigate: (ShellDestination) -> Unit
) {
    val chestHasIndicator = activeChestCreatures(uiState).any { it.isNew }
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        HeartShellBackground(Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoomOrbitNode(R.string.shell_room_lookout_title, Icons.Outlined.Visibility, true,
                    modifier = Modifier.weight(1f), onClick = { onNavigate(ShellDestination.LookoutPreview) })
                RoomOrbitNode(R.string.shell_room_focus_title, Icons.Outlined.CenterFocusStrong, false,
                    modifier = Modifier.weight(1f), onClick = { onNavigate(ShellDestination.Focus) })
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoomOrbitNode(R.string.shell_room_voyage_title, Icons.Outlined.Route, false,
                    modifier = Modifier.weight(1f), onClick = { onNavigate(ShellDestination.VoyagePreview) })
                RoomOrbitNode(R.string.shell_room_idea_title, Icons.Outlined.PsychologyAlt, true,
                    modifier = Modifier.weight(1f), onClick = { onNavigate(ShellDestination.IdeaGrovePreview) })
            }
            HeartCenter(uiState, dropBalance, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomOrbitNode(
                    R.string.red_title, Icons.Outlined.Terrain, false,
                    hasIndicator = uiState.finds.any { it.isNew && it.findId in com.kingkharnivore.skillz.utils.shell.RedCreatureCatalog.byId },
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ShellDestination.TheRed) }
                )
                RoomOrbitNode(
                    R.string.shell_room_the_blue_title, Icons.Outlined.FilterVintage, false,
                    hasIndicator = buildTheBlueUiState(uiState.finds).newAnimalCount > 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ShellDestination.TheBluePreview) }
                )
                RoomOrbitNode(
                    R.string.shell_room_stillwater_title, Icons.Outlined.Spa, false,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ShellDestination.TheGreen) }
                )
            }
            ShellWhisperDock(uiState, Modifier.fillMaxWidth()) {
                onNavigate(if (hasChestCreatures(uiState)) ShellDestination.ShellChest else ShellDestination.Focus)
            }
            HeartShortcutDock(chestHasIndicator, { onNavigate(ShellDestination.ShellChest) },
                { onNavigate(ShellDestination.Badges) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HeartShellBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(shellChamberBrush())
    ) {
        TurtleShellInteriorBackground(
            modifier = Modifier.matchParentSize(),
            centerGlow = true
        )
    }
}

@Composable
private fun RoomOrbitNode(
    labelRes: Int,
    icon: ImageVector,
    dormant: Boolean,
    hasIndicator: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(labelRes)
    val nodeDescription = if (dormant) {
        stringResource(R.string.shell_room_preview_a11y, label)
    } else {
        stringResource(R.string.shell_room_active_a11y, label)
    }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (dormant) {
                scheme.surface.copy(alpha = 0.82f)
            } else {
                scheme.surface
            }
        ),
        modifier = modifier
            .heightIn(min = 84.dp)
            .fillMaxHeight()
            .semantics {
                contentDescription = nodeDescription
                role = Role.Button
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            TurtleShellCardPattern(Modifier.matchParentSize())

            if (hasIndicator) {
                Surface(
                    shape = CircleShape,
                    color = shellIndicatorColor(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(10.dp),
                    content = {}
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (dormant) {
                        scheme.primary.copy(alpha = 0.64f)
                    } else {
                        scheme.primary
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dormant) {
                        scheme.onSurface.copy(alpha = 0.76f)
                    } else {
                        scheme.onSurface
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HeartCenter(
    uiState: ShellUiState,
    dropBalance: Long,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    ElevatedCard(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = scheme.surface
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.shell_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            ShellBalances(uiState.pearlBalance, uiState.pebbleBalance, dropBalance)
        }
    }
}

@Composable
private fun ShellWhisperDock(
    uiState: ShellUiState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val restingCount = activeChestCreatureCount(uiState)

    val text = when {
        restingCount > 0 -> stringResource(R.string.shell_whisper_chest_waiting, restingCount)
        else -> stringResource(R.string.shell_pulse_mystery)
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.35f)),
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun HeartShortcutDock(
    chestHasIndicator: Boolean,
    onChest: () -> Unit,
    onBadges: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartShortcut(
                icon = Icons.Outlined.Inventory2,
                labelRes = R.string.shell_chest_title,
                hasIndicator = chestHasIndicator,
                onClick = onChest
            )

            HeartShortcut(
                icon = Icons.Outlined.MilitaryTech,
                labelRes = R.string.shell_badges_title,
                hasIndicator = false,
                onClick = onBadges
            )
        }
    }
}

@Composable
private fun HeartShortcut(
    icon: ImageVector,
    labelRes: Int,
    hasIndicator: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(labelRes)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp)
            )
            if (hasIndicator) {
                Surface(
                    shape = CircleShape,
                    color = shellIndicatorColor(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp),
                    content = {}
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TurtleShellCardPattern(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        drawOval(
            color = scheme.secondary.copy(alpha = 0.08f),
            topLeft = Offset(size.width * 0.08f, -size.height * 0.25f),
            size = Size(size.width * 0.84f, size.height * 1.25f)
        )
        drawLine(
            color = scheme.onSurface.copy(alpha = 0.10f),
            start = Offset(size.width * 0.50f, 0f),
            end = Offset(size.width * 0.50f, size.height),
            strokeWidth = 2f
        )
    }
}
