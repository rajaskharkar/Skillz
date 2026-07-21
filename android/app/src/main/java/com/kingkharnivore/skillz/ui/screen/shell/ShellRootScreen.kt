@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.shell

import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellPearlMiniIcon
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
import com.kingkharnivore.skillz.ui.screen.shell.rooms.stillwater.StillwaterRoomScreen
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

internal fun hasAffordableFocusPearlAction(uiState: ShellUiState): Boolean = false


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
    onLaunchFlowFromPulse: (Long, String, String?) -> Unit = { _, _, _ -> },
    onOpenActiveFlow: () -> Unit = {},
    onPlanArc: () -> Unit = {},
    onMovementInfo: () -> Unit = {},
    viewModel: ShellViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val activeFlowMessage = stringResource(R.string.lookout_flow_already_active)
    var destination by remember { mutableStateOf<ShellDestination>(ShellDestination.Heart) }
    var chestFocusSpecies by remember { mutableStateOf<String?>(null) }
    var blueFocusCollection by remember { mutableStateOf<String?>(null) }
    var stillwaterFocusCollection by remember { mutableStateOf<String?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    val notificationCount = unseenNotificationCount(uiState)

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(destination) {
        val room = when (destination) {
            ShellDestination.Heart -> ShellRoomId.HEART
            ShellDestination.Focus -> ShellRoomId.FOCUS
            ShellDestination.Stillwater -> ShellRoomId.STILLWATER
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

    BackHandler(enabled = showNotifications || destination != ShellDestination.Heart) {
        when {
            showNotifications -> showNotifications = false
            else -> destination = ShellDestination.Heart
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShellTopBar(
                destination = destination,
                pearlBalance = uiState.pearlBalance,
                pearlBasinHasIndicator = hasAffordablePearlShape(uiState),
                notificationCount = notificationCount,
                onBack = {
                    if (destination == ShellDestination.Heart) {
                        onBack()
                    } else {
                        destination = ShellDestination.Heart
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
                ShellDestination.Heart -> HeartRoomScreen(
                    uiState = uiState,
                    onNavigate = { destination = it }
                )

                ShellDestination.Focus -> FocusRoomScreen()

                ShellDestination.Stillwater -> StillwaterRoomScreen(
                    uiState = uiState,
                    onDrawFromStillwater = viewModel::onDrawFromStillwater,
                    onConfirmStillwaterDraw = viewModel::onConfirmStillwaterDraw,
                    onDismissStillwaterReveal = viewModel::onDismissStillwaterReveal,
                    onDismissStillwaterDrawConfirmation = viewModel::onDismissStillwaterDrawConfirmation,
                    focusedCollectionId = stillwaterFocusCollection,
                    onFocusConsumed = { stillwaterFocusCollection = null }
                )

                ShellDestination.ShellChest -> ShellChestScreen(
                    uiState = uiState,
                    onReleaseCreaturesByLevel = viewModel::releaseCreaturesByLevel,
                    onLevelUpCreatureByLevel = { id, level -> viewModel.growCreatureByLevel(id, level, "CHEST") },
                    onOpenBlue = { destination = ShellDestination.TheBluePreview },
                    onSortOptionSelected = viewModel::setChestSortOption,
                    onFilterSelected = viewModel::setChestFilter,
                    focusSpeciesId = chestFocusSpecies,
                    onFocusConsumed = { chestFocusSpecies = null }
                )

                ShellDestination.Badges -> BadgesScreen(
                    uiState = uiState,
                    onPin = viewModel::pinBadge,
                    onUnpin = viewModel::unpinBadge,
                    onTrack = viewModel::trackBadge,
                    onUntrack = viewModel::untrackBadge,
                    onCategory = viewModel::setBadgeCategory,
                    onSort = viewModel::setBadgeSort,
                    onBadgeViewed = { viewModel.markBadgeViewed(it) },
                    onAcknowledgeBackfill = viewModel::acknowledgeBackfill,
                    onNavigate = { request ->
                        when (request) {
                            is BadgeActionDestination.Chest -> { chestFocusSpecies = request.speciesId; destination = ShellDestination.ShellChest }
                            is BadgeActionDestination.Blue -> { blueFocusCollection = request.collectionId; destination = ShellDestination.TheBluePreview }
                            is BadgeActionDestination.Stillwater -> { stillwaterFocusCollection = request.collectionId; destination = ShellDestination.Stillwater }
                            BadgeActionDestination.BlueCollection, BadgeActionDestination.StillwaterCollection,
                            BadgeActionDestination.AllWatersCollection -> destination = ShellDestination.Badges
                            BadgeActionDestination.MovementInfo -> onMovementInfo()
                            BadgeActionDestination.BadgeDetails, BadgeActionDestination.Flow, BadgeActionDestination.Arc -> Unit
                        }
                    },
                    onOpenFlow = { if (isFlowActive) onOpenActiveFlow() else onLaunchFlowForJourney("") },
                    onOpenArc = onPlanArc
                )

                ShellDestination.VoyagePreview -> VoyageHallScreen()

                ShellDestination.TheBluePreview -> TheBlueRoomScreen(
                    uiState = uiState,
                    onDisplayInFocus = viewModel::place,
                    onGrowCreature = { id -> viewModel.growCreature(id, "BLUE") },
                    onReleaseCreaturesByLevel = viewModel::releaseCreaturesByLevel,
                    onEncounterBeyondBlue = viewModel::encounterBeyondBlue,
                    onOpenChest = { destination = ShellDestination.ShellChest },
                    focusedCollectionId = blueFocusCollection,
                    onFocusConsumed = { blueFocusCollection = null }
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
                    onLaunchFlowForJourney = { journeyName ->
                        if (isFlowActive) {
                            scope.launch { snackbarHostState.showSnackbar(activeFlowMessage) }
                        } else {
                            onLaunchFlowForJourney(journeyName)
                        }
                    }
                )
            }
        }

        if (showNotifications) {
            NotificationInlayOverlay(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onDismiss = { showNotifications = false },
                onMarkNotificationViewed = viewModel::markNotificationViewed,
                onMarkAllViewed = viewModel::markAllNotificationsViewed,
                onDeepLinkRoute = { route ->
                    destination = when (route) {
                        SHELL_CHEST_ROUTE -> ShellDestination.ShellChest
                        SHELL_BADGES_ROUTE -> ShellDestination.Badges
                        else -> destination
                    }
                }
            )
        }

        uiState.masteryCelebration?.let { celebration ->
            MasteryCelebrationScreen(
                event = celebration,
                uiState = uiState,
                onBegin = { viewModel.beginCelebration() },
                onAdvance = { reduced -> viewModel.advanceCelebration(reduced) },
                onSkip = { viewModel.skipCelebration() },
                onComplete = { origin ->
                    viewModel.completeCelebration {
                        destination = when (origin) {
                            "BLUE" -> ShellDestination.TheBluePreview
                            "STILLWATER" -> ShellDestination.Stillwater
                            else -> ShellDestination.ShellChest
                        }
                    }
                },
                onPin = { badgeId, replacementId -> viewModel.pinBadge(badgeId, replacementId) },
                onUnpin = viewModel::unpinBadge,
                onTrack = viewModel::trackBadge,
                onUntrack = viewModel::untrackBadge,
                onNavigate = { request ->
                    when (request) {
                        is BadgeActionDestination.Chest -> { chestFocusSpecies = request.speciesId; destination = ShellDestination.ShellChest }
                        is BadgeActionDestination.Blue -> { blueFocusCollection = request.collectionId; destination = ShellDestination.TheBluePreview }
                        is BadgeActionDestination.Stillwater -> { stillwaterFocusCollection = request.collectionId; destination = ShellDestination.Stillwater }
                        BadgeActionDestination.BlueCollection, BadgeActionDestination.StillwaterCollection,
                        BadgeActionDestination.AllWatersCollection -> destination = ShellDestination.Badges
                        BadgeActionDestination.Flow -> if (isFlowActive) onOpenActiveFlow() else onLaunchFlowForJourney("")
                        BadgeActionDestination.Arc -> onPlanArc()
                        BadgeActionDestination.MovementInfo -> onMovementInfo()
                        BadgeActionDestination.BadgeDetails -> Unit
                    }
                }
            )
        }

    }
}

@Composable
private fun HeartRoomScreen(
    uiState: ShellUiState,
    onNavigate: (ShellDestination) -> Unit
) {
    val chestHasIndicator = activeChestCreatures(uiState).any { it.isNew }
    val focusChanged = false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeartShellBackground(
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val nodeWidth = if (maxWidth < 360.dp) 96.dp else 108.dp
                val nodeHeight = if (maxHeight < 440.dp) 76.dp else 84.dp

                RoomOrbitNode(
                    labelRes = R.string.shell_room_lookout_title,
                    icon = Icons.Outlined.Visibility,
                    dormant = true,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 4.dp),
                    onClick = { onNavigate(ShellDestination.LookoutPreview) }
                )

                RoomOrbitPair(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.17f),
                    nodeWidth = nodeWidth,
                    left = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_voyage_title,
                            icon = Icons.Outlined.Route,
                            dormant = false,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.VoyagePreview) }
                        )
                    },
                    right = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_idea_title,
                            icon = Icons.Outlined.PsychologyAlt,
                            dormant = true,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.IdeaGrovePreview) }
                        )
                    }
                )

                HeartCenter(
                    uiState = uiState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.36f)
                )

                RoomOrbitPair(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.61f),
                    nodeWidth = nodeWidth,
                    left = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_focus_title,
                            icon = Icons.Outlined.CenterFocusStrong,
                            dormant = false,
                            hasIndicator = focusChanged,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.Focus) }
                        )
                    },
                    right = {
                        RoomOrbitNode(
                            labelRes = R.string.shell_room_stillwater_title,
                            icon = Icons.Outlined.WaterDrop,
                            dormant = false,
                            nodeWidth = nodeWidth,
                            nodeHeight = nodeHeight,
                            onClick = { onNavigate(ShellDestination.Stillwater) }
                        )
                    }
                )

                RoomOrbitNode(
                    labelRes = R.string.shell_room_the_blue_title,
                    icon = Icons.Outlined.FilterVintage,
                    dormant = false,
                    hasIndicator = buildTheBlueUiState(uiState.finds, uiState.focusPlacements).newAnimalCount > 0,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.80f),
                    onClick = { onNavigate(ShellDestination.TheBluePreview) }
                )
            }

            ShellWhisperDock(
                uiState = uiState,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val hasEmptyNook = uiState.focusPlacements.size < ShellContentCatalog.focusSlots.size
                    when {
                        hasAffordablePearlShape(uiState) -> onNavigate(ShellDestination.Focus)
                        hasChestCreatures(uiState) -> onNavigate(ShellDestination.ShellChest)
                        hasEmptyNook -> onNavigate(ShellDestination.Focus)
                        else -> onNavigate(ShellDestination.Focus)
                    }
                }
            )

            HeartShortcutDock(
                chestHasIndicator = chestHasIndicator,
                onChest = { onNavigate(ShellDestination.ShellChest) },
                onBadges = { onNavigate(ShellDestination.Badges) },
                modifier = Modifier.fillMaxWidth()
            )
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
private fun RoomOrbitPair(
    modifier: Modifier = Modifier,
    nodeWidth: Dp,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(nodeWidth)) {
            left()
        }

        Box(Modifier.width(nodeWidth)) {
            right()
        }
    }
}

@Composable
private fun RoomOrbitNode(
    labelRes: Int,
    icon: ImageVector,
    dormant: Boolean,
    hasIndicator: Boolean = false,
    nodeWidth: Dp,
    nodeHeight: Dp,
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
            .width(nodeWidth)
            .height(nodeHeight)
            .semantics {
                contentDescription = nodeDescription
                role = Role.Button
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
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
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
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
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun HeartCenter(
    uiState: ShellUiState,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val pearlBalanceDescription = stringResource(R.string.shell_pearl_basin_chip_a11y, uiState.pearlBalance)

    ElevatedCard(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = scheme.surface
        ),
        modifier = modifier.width(214.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
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

            Surface(
                shape = CircleShape,
                color = scheme.background,
                border = BorderStroke(
                    width = 1.dp,
                    color = scheme.secondary.copy(alpha = 0.55f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = pearlBalanceDescription
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShellPearlMiniIcon(Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.shell_pearl_balance, uiState.pearlBalance),
                        color = scheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
    val hasEmptyNook = uiState.focusPlacements.size < ShellContentCatalog.focusSlots.size

    val text = when {
        restingCount > 0 -> stringResource(R.string.shell_whisper_chest_waiting, restingCount)
        hasEmptyNook -> stringResource(R.string.shell_whisper_empty_focus)
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
                color = scheme.onSurface.copy(alpha = 0.82f),
                maxLines = 1
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
