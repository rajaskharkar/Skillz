@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellFindCategory
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.data.model.shell.StillwaterPerspective
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureDefinition
import com.kingkharnivore.skillz.domain.shell.CreaturePlacementBand
import com.kingkharnivore.skillz.domain.shell.CreatureScaleClass
import com.kingkharnivore.skillz.domain.shell.CreatureSceneBehavior
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureRenderFamily
import com.kingkharnivore.skillz.domain.shell.CreatureZone
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgesScreen
import com.kingkharnivore.skillz.ui.screen.shell.inventory.DiscoveryJournalScreen
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellNotificationsScreen
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellChestScreen
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellMetricPill
import com.kingkharnivore.skillz.ui.screen.shell.rooms.focus.FocusRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.rooms.stillwater.StillwaterRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellPearlMiniIcon
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.TurtleShellInteriorBackground
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.TheBlueOverlaySurface
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.TheBlueRoomScreen
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawBranchingCoral
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawSunlitReefEnvironment
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawTheBlueWaterBackground
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.findName
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.zoneRailLabel
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.zoneSubtitle
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.zoneTitle
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.canDisplayInstance
import com.kingkharnivore.skillz.ui.screen.shell.ux.displayedInstanceIds
import com.kingkharnivore.skillz.ui.screen.shell.ux.isUserVisibleShellFind
import com.kingkharnivore.skillz.ui.screen.shell.ux.restingFinds
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellIndicatorColor
import com.kingkharnivore.skillz.utils.shell.shellBackground
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.ShellViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private fun hasRestingPlaceableFinds(uiState: ShellUiState): Boolean = restingFinds(uiState).any { item ->
    val def = ShellContentCatalog.find(item.findId)
    def?.placeable == true
}

private fun hasAffordablePearlShape(uiState: ShellUiState): Boolean =
    uiState.finds.any { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        if (def.kind != ShellRewardKind.OBJECT) return@any false
        val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@any false
        next.pearlCost <= uiState.pearlBalance
    } || ShellContentCatalog.focusPearlObjects.any { (it.pearlCost ?: Int.MAX_VALUE) <= uiState.pearlBalance }

private fun unseenNotificationCount(uiState: ShellUiState): Int =
    uiState.finds.count { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) } +
            uiState.stacks.count { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) } +
            uiState.badges.count { it.isNew } +
            uiState.discoveries.count { it.isNew }

internal fun hasAffordableFocusPearlAction(uiState: ShellUiState): Boolean {
    val displayed = displayedInstanceIds(uiState)

    val hasAffordableDisplayedUpgrade = uiState.finds.any { item ->
        if (item.instanceId !in displayed) return@any false

        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        if (def.kind != ShellRewardKind.OBJECT) return@any false
        val next = ShellContentCatalog.nextUpgrade(
            findId = def.findId,
            currentStageId = item.currentUpgradeStageId
        ) ?: return@any false

        next.pearlCost <= uiState.pearlBalance
    }

    if (hasAffordableDisplayedUpgrade) return true

    val occupiedSlots = uiState.focusPlacements.map { it.slotId }.toSet()
    val emptySlots = ShellContentCatalog.focusSlots.filter { slot ->
        slot.slotId !in occupiedSlots
    }

    if (emptySlots.isEmpty()) return false

    return ShellContentCatalog.focusPearlObjects.any { def ->
        val cost = def.pearlCost ?: return@any false
        cost <= uiState.pearlBalance &&
                emptySlots.any { slot -> ShellContentCatalog.isCompatibleWithSlot(slot, def) }
    }
}

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
    viewModel: ShellViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var destination by remember { mutableStateOf<ShellDestination>(ShellDestination.Heart) }
    var showPearlBasin by remember { mutableStateOf(false) }
    var shouldMarkNotificationsSeenOnExit by remember { mutableStateOf(false) }
    val notificationCount = if (destination == ShellDestination.Notifications) 0 else unseenNotificationCount(uiState)

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
        if (destination == ShellDestination.Notifications) {
            shouldMarkNotificationsSeenOnExit = true
        } else if (shouldMarkNotificationsSeenOnExit) {
            viewModel.markNotificationsSeen()
            shouldMarkNotificationsSeenOnExit = false
        }
    }

    BackHandler(enabled = showPearlBasin || destination != ShellDestination.Heart) {
        if (showPearlBasin) {
            showPearlBasin = false
        } else {
            destination = ShellDestination.Heart
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
                onPearls = { showPearlBasin = true },
                onNotifications = { destination = ShellDestination.Notifications },
                onChest = { destination = ShellDestination.ShellChest }
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
                    onNavigate = { destination = it },
                    onOpenPearlBasin = { showPearlBasin = true }
                )

                ShellDestination.Focus -> FocusRoomScreen(
                    uiState = uiState,
                    onPlace = viewModel::place,
                    onInvite = viewModel::invitePearlObject,
                    onReturn = viewModel::returnToChest,
                    onUpgrade = viewModel::upgrade
                )

                ShellDestination.Stillwater -> StillwaterRoomScreen(
                    uiState = uiState,
                    onPerspective = viewModel::setPerspective
                )

                ShellDestination.ShellChest -> ShellChestScreen(
                    uiState = uiState,
                    onPlace = viewModel::place,
                    onReturn = viewModel::returnToChest,
                    onUpgrade = viewModel::upgrade,
                    onOpenFocus = { destination = ShellDestination.Focus }
                )

                ShellDestination.Badges -> BadgesScreen(uiState)
                ShellDestination.DiscoveryJournal -> DiscoveryJournalScreen(uiState)
                ShellDestination.Notifications -> ShellNotificationsScreen(uiState)

                ShellDestination.VoyagePreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_voyage_title,
                    bodyRes = R.string.shell_preview_voyage,
                    icon = Icons.Outlined.Route
                )

                ShellDestination.TheBluePreview -> TheBlueRoomScreen(
                    uiState = uiState,
                    onDisplayInFocus = viewModel::place,
                    onGrowCreature = viewModel::growCreature,
                    onReleaseCreature = viewModel::releaseCreature,
                    onEncounterBeyondBlue = viewModel::encounterBeyondBlue,
                    onOpenChest = { destination = ShellDestination.ShellChest }
                )

                ShellDestination.IdeaGrovePreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_idea_title,
                    bodyRes = R.string.shell_preview_idea,
                    icon = Icons.Outlined.PsychologyAlt
                )

                ShellDestination.LookoutPreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_lookout_title,
                    bodyRes = R.string.shell_preview_lookout,
                    icon = Icons.Outlined.Visibility
                )
            }
        }

        if (showPearlBasin) {
            PearlBasinSheet(
                uiState = uiState,
                onDismiss = { showPearlBasin = false },
                onOpenFocus = {
                    showPearlBasin = false
                    destination = ShellDestination.Focus
                },
                onOpenChest = {
                    showPearlBasin = false
                    destination = ShellDestination.ShellChest
                },
                onOpenObject = {
                    showPearlBasin = false
                    destination = ShellDestination.Focus
                },
                onInviteObject = { findId ->
                    viewModel.invitePearlObjectToChest(findId)
                    showPearlBasin = false
                    destination = ShellDestination.ShellChest
                }
            )
        }
    }
}

@Composable
private fun HeartRoomScreen(
    uiState: ShellUiState,
    onNavigate: (ShellDestination) -> Unit,
    onOpenPearlBasin: () -> Unit
) {
    var showHeartDetail by remember { mutableStateOf(false) }
    val chestHasIndicator = restingFinds(uiState).any { it.isNew } || uiState.stacks.any { it.isNew && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }
    val hasNewDiscovery = uiState.discoveries.any { it.isNew }
    val focusChanged = hasAffordableFocusPearlAction(uiState)

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
                            dormant = true,
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
                        .offset(y = maxHeight * 0.36f),
                    onClick = { showHeartDetail = true },
                    onPearlClick = onOpenPearlBasin
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
                        uiState.discoveries.any { it.isNew } -> onNavigate(ShellDestination.DiscoveryJournal)
                        hasAffordablePearlShape(uiState) -> onNavigate(ShellDestination.Focus)
                        hasRestingPlaceableFinds(uiState) -> onNavigate(ShellDestination.ShellChest)
                        hasEmptyNook -> onNavigate(ShellDestination.Focus)
                        else -> onNavigate(ShellDestination.Focus)
                    }
                }
            )

            HeartShortcutDock(
                chestHasIndicator = chestHasIndicator,
                journalHasIndicator = hasNewDiscovery,
                onChest = { onNavigate(ShellDestination.ShellChest) },
                onBadges = { onNavigate(ShellDestination.Badges) },
                onJournal = { onNavigate(ShellDestination.DiscoveryJournal) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showHeartDetail) {
        HeartDetailSheet(
            uiState = uiState,
            onDismiss = { showHeartDetail = false },
            onOpenPearlBasin = {
                showHeartDetail = false
                onOpenPearlBasin()
            },
            onOpenFocus = {
                showHeartDetail = false
                onNavigate(ShellDestination.Focus)
            },
            onOpenChest = {
                showHeartDetail = false
                onNavigate(ShellDestination.ShellChest)
            },
            onOpenJournal = {
                showHeartDetail = false
                onNavigate(ShellDestination.DiscoveryJournal)
            }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPearlClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val heartDescription = stringResource(R.string.shell_heart_center_a11y)
    val pearlBalanceDescription = stringResource(R.string.shell_pearl_basin_chip_a11y, uiState.pearlBalance)

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = scheme.surface
        ),
        modifier = modifier
            .width(214.dp)
            .semantics {
                contentDescription = heartDescription
                role = Role.Button
            }
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

            AssistChip(
                onClick = onPearlClick,
                label = {
                    Text(
                        text = stringResource(R.string.shell_pearl_balance, uiState.pearlBalance),
                        textAlign = TextAlign.Center
                    )
                },
                leadingIcon = {
                    ShellPearlMiniIcon(Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = scheme.background,
                    labelColor = scheme.onBackground,
                    leadingIconContentColor = scheme.primary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = scheme.secondary.copy(alpha = 0.55f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = pearlBalanceDescription
                    role = Role.Button
                }
            )
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
    val displayedIds = displayedInstanceIds(uiState)
    val affordableUpgrade = uiState.finds.firstOrNull { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@firstOrNull false
        if (def.kind != ShellRewardKind.OBJECT) return@firstOrNull false
        val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@firstOrNull false
        item.instanceId in displayedIds && next.pearlCost <= uiState.pearlBalance
    }
    val restingCount = restingFinds(uiState).count { item -> ShellContentCatalog.find(item.findId)?.placeable == true }
    val hasEmptyNook = uiState.focusPlacements.size < ShellContentCatalog.focusSlots.size

    val text = when {
        uiState.discoveries.any { it.isNew } -> stringResource(R.string.shell_whisper_new_discovery)
        affordableUpgrade != null -> stringResource(R.string.shell_whisper_upgrade_ready, ShellContentCatalog.find(affordableUpgrade.findId)?.let { stringResource(it.titleRes) } ?: stringResource(R.string.shell_empty_slot))
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
    journalHasIndicator: Boolean,
    onChest: () -> Unit,
    onBadges: () -> Unit,
    onJournal: () -> Unit,
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

            HeartShortcut(
                icon = Icons.Outlined.AutoStories,
                labelRes = R.string.shell_journal_title,
                hasIndicator = journalHasIndicator,
                onClick = onJournal
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
private fun HeartDetailSheet(
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onOpenPearlBasin: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenChest: () -> Unit,
    onOpenJournal: () -> Unit
) {
    val totalFinds = uiState.finds.count { isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) } +
        uiState.stacks.filter { isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }.sumOf { it.quantity }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_heart_detail_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.shell_heart_pearls_gathered, uiState.pearlBalance),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = stringResource(R.string.shell_heart_finds_owned, totalFinds),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.shell_heart_discoveries_awakened, uiState.discoveries.size),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(R.string.shell_heart_objects_displayed_resting, uiState.focusPlacements.size, restingFinds(uiState).size),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenPearlBasin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_pearl_basin_title))
                }

                OutlinedButton(
                    onClick = onOpenFocus,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_room_focus_title))
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpenChest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_chest_title))
                }

                OutlinedButton(
                    onClick = onOpenJournal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_journal_title))
                }
            }
        }
    }
}

@Composable
private fun PearlBasinSheet(
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenChest: () -> Unit,
    onOpenObject: () -> Unit,
    onInviteObject: (String) -> Unit
) {
    var inviteConfirmation by remember { mutableStateOf<ShellFindDefinition?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShellPearlBasinIcon(Modifier.size(52.dp))

                Column {
                    Text(
                        text = stringResource(R.string.shell_pearl_basin_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.shell_pearl_balance, uiState.pearlBalance),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(stringResource(R.string.shell_pearl_basin_copy))

            val displayedIds = displayedInstanceIds(uiState)
            val upgradeSuggestions = uiState.finds.mapNotNull { item ->
                val def = ShellContentCatalog.find(item.findId) ?: return@mapNotNull null
                if (def.kind != ShellRewardKind.OBJECT) return@mapNotNull null
                val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@mapNotNull null
                Triple(item, def, next)
            }.sortedBy { it.third.pearlCost }
            val displayedUpgradeSuggestions = upgradeSuggestions.filter { it.first.instanceId in displayedIds }
            val objectSuggestions = ShellContentCatalog.focusPearlObjects
                .sortedBy { it.pearlCost ?: Int.MAX_VALUE }
            val allCosts = displayedUpgradeSuggestions.map { it.third.pearlCost } + objectSuggestions.mapNotNull { it.pearlCost }
            val hasAvailable = allCosts.any { it <= uiState.pearlBalance }

            Text(
                text = stringResource(R.string.shell_available_now),
                fontWeight = FontWeight.SemiBold
            )
            if (hasAvailable) {
                displayedUpgradeSuggestions.filter { it.third.pearlCost <= uiState.pearlBalance }.take(2).forEach { (item, def, next) ->
                    SuggestionRow(
                        title = stringResource(R.string.shell_basin_brighten_suggestion, stringResource(def.titleRes)),
                        cost = next.pearlCost,
                        onClick = onOpenObject
                    )
                }
                objectSuggestions.filter { (it.pearlCost ?: 0) <= uiState.pearlBalance }.take(2).forEach { def ->
                    SuggestionRow(
                        title = stringResource(R.string.shell_basin_invite_suggestion, stringResource(def.titleRes)),
                        cost = def.pearlCost ?: 0,
                        onClick = { inviteConfirmation = def }
                    )
                }
            } else {
                Text(stringResource(R.string.shell_no_available_shapes))
            }

            Text(
                text = stringResource(R.string.shell_affordable_soon),
                fontWeight = FontWeight.SemiBold
            )
            (displayedUpgradeSuggestions.filter { it.third.pearlCost > uiState.pearlBalance }.map { (_, def, next) ->
                stringResource(R.string.shell_basin_soon_upgrade, stringResource(def.titleRes), stringResource(next.titleRes)) to next.pearlCost
            } + objectSuggestions.filter { (it.pearlCost ?: 0) > uiState.pearlBalance }.map { def ->
                stringResource(R.string.shell_basin_invite_suggestion, stringResource(def.titleRes)) to (def.pearlCost ?: 0)
            })
                .sortedBy { it.second }
                .take(3)
                .forEach { (title, cost) ->
                    SuggestionRow(
                        title = title,
                        cost = cost,
                        enabled = false,
                        supportingText = stringResource(R.string.shell_need_more_pearls, cost - uiState.pearlBalance),
                        onClick = {}
                    )
                }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenFocus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_room_focus_title))
                }

                OutlinedButton(
                    onClick = onOpenChest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shell_chest_title))
                }
            }
        }
    }

    inviteConfirmation?.let { def ->
        InvitePearlObjectConfirmationSheet(
            definition = def,
            onDismiss = { inviteConfirmation = null },
            onConfirm = {
                inviteConfirmation = null
                onInviteObject(def.findId)
            }
        )
    }
}

@Composable
private fun InvitePearlObjectConfirmationSheet(
    definition: ShellFindDefinition,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = stringResource(definition.titleRes)
    val cost = definition.pearlCost ?: 0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_invite_confirm_title, title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.shell_invite_confirm_body, title))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(stringResource(R.string.shell_invite_confirm_cta, cost))
            }
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.shell_cancel))
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    cost: Int,
    enabled: Boolean = true,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText ?: stringResource(R.string.shell_pearl_cost, cost)) },
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
    )
}

@Composable
private fun HeartShellBackground(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.primary.copy(alpha = 0.20f),
            topLeft = Offset(-w * 0.16f, h * 0.02f),
            size = Size(w * 1.32f, h * 0.96f)
        )

        drawOval(
            color = scheme.secondary.copy(alpha = 0.10f),
            topLeft = Offset(w * 0.08f, h * 0.09f),
            size = Size(w * 0.84f, h * 0.76f)
        )

        val centerSeam = Path().apply {
            moveTo(w * 0.50f, h * 0.07f)
            cubicTo(
                w * 0.46f,
                h * 0.26f,
                w * 0.54f,
                h * 0.48f,
                w * 0.50f,
                h * 0.86f
            )
        }

        drawPath(
            path = centerSeam,
            color = scheme.secondary.copy(alpha = 0.18f),
            style = Stroke(width = 4f)
        )

        listOf(0.20f, 0.35f, 0.50f, 0.66f, 0.80f).forEach { yFraction ->
            val y = h * yFraction

            val band = Path().apply {
                moveTo(w * 0.12f, y)
                cubicTo(
                    w * 0.30f,
                    y - h * 0.05f,
                    w * 0.43f,
                    y + h * 0.025f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.57f,
                    y + h * 0.025f,
                    w * 0.70f,
                    y - h * 0.05f,
                    w * 0.88f,
                    y
                )
            }

            drawPath(
                path = band,
                color = scheme.secondary.copy(alpha = 0.10f),
                style = Stroke(width = 2.5f)
            )
        }

        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(-w * 0.10f, h * 0.02f),
            size = Size(w * 1.20f, h * 0.96f),
            style = Stroke(width = w * 0.10f)
        )
    }
}

@Composable
private fun ShellPearlBasinIcon(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.secondary.copy(alpha = 0.90f),
            topLeft = Offset(w * 0.08f, h * 0.32f),
            size = Size(w * 0.84f, h * 0.50f)
        )

        drawOval(
            color = scheme.surface.copy(alpha = 0.86f),
            topLeft = Offset(w * 0.16f, h * 0.36f),
            size = Size(w * 0.68f, h * 0.34f)
        )

        repeat(4) { index ->
            val x = w * (0.30f + index * 0.13f)
            val groove = Path().apply {
                moveTo(x, h * 0.42f)
                cubicTo(
                    x - w * 0.04f,
                    h * 0.52f,
                    x - w * 0.02f,
                    h * 0.62f,
                    x,
                    h * 0.70f
                )
            }

            drawPath(
                path = groove,
                color = scheme.onSecondary.copy(alpha = 0.26f),
                style = Stroke(width = 2f)
            )
        }

        drawCircle(
            color = scheme.onPrimary,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.36f)
        )

        drawCircle(
            color = scheme.primary.copy(alpha = 0.40f),
            radius = w * 0.08f,
            center = Offset(w * 0.56f, h * 0.31f)
        )
    }
}

@Composable
private fun TurtleShellCardPattern(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.secondary.copy(alpha = 0.08f),
            topLeft = Offset(w * 0.08f, -h * 0.30f),
            size = Size(w * 0.84f, h * 1.30f)
        )

        val seamColor = scheme.onSurface.copy(alpha = 0.10f)

        val center = Path().apply {
            moveTo(w * 0.50f, 0f)
            cubicTo(
                w * 0.46f,
                h * 0.35f,
                w * 0.54f,
                h * 0.60f,
                w * 0.50f,
                h
            )
        }

        drawPath(
            path = center,
            color = seamColor,
            style = Stroke(width = 2f)
        )

        repeat(3) { index ->
            val y = h * (0.28f + index * 0.22f)

            val band = Path().apply {
                moveTo(w * 0.10f, y)
                cubicTo(
                    w * 0.32f,
                    y - h * 0.08f,
                    w * 0.44f,
                    y + h * 0.05f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.56f,
                    y + h * 0.05f,
                    w * 0.68f,
                    y - h * 0.08f,
                    w * 0.90f,
                    y
                )
            }

            drawPath(
                path = band,
                color = seamColor,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun DormantPreviewScreen(
    titleRes: Int,
    bodyRes: Int,
    icon: ImageVector
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(shellChamberBrush())
    ) {
        TurtleShellInteriorBackground(
            modifier = Modifier.matchParentSize(),
            centerGlow = true
        )

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = scheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.24f)),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = scheme.onPrimary
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(bodyRes),
                    color = scheme.onSurface.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
