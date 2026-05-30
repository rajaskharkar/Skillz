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
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.canDisplayInstance
import com.kingkharnivore.skillz.ui.screen.shell.ux.displayedInstanceIds
import com.kingkharnivore.skillz.ui.screen.shell.ux.isUserVisibleShellFind
import com.kingkharnivore.skillz.ui.screen.shell.ux.restingFinds
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellIndicatorColor
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
private fun TheBlueRoomScreen(
    uiState: ShellUiState,
    onDisplayInFocus: (String, String) -> Unit,
    onGrowCreature: (String) -> Unit,
    onReleaseCreature: (String) -> Unit,
    onEncounterBeyondBlue: (String, List<String>) -> Unit,
    onOpenChest: () -> Unit
) {
    val theBlueState = remember(uiState.finds, uiState.focusPlacements) {
        buildTheBlueUiState(uiState.finds, uiState.focusPlacements)
    }
    var selectedAnimal by remember { mutableStateOf<TheBlueAnimalGroupUiModel?>(null) }
    var releaseCandidate by remember { mutableStateOf<TheBlueAnimalGroupUiModel?>(null) }
    var showBeyondBlue by remember { mutableStateOf(false) }
    var beyondBlueInitialZone by remember { mutableStateOf(TheBlueZoneId.SUNLIT_REEF) }
    var entryNewAnimalFindIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var railNavigationJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(theBlueState.newAnimalCount, theBlueState.zones) {
        if (entryNewAnimalFindIds.isEmpty() && theBlueState.newAnimalCount > 0) {
            entryNewAnimalFindIds = theBlueState.zones
                .flatMap { zone -> zone.animals.filter { it.isNew }.map { it.findId } }
                .toSet()
        }
    }
    val pageCount = if (theBlueState.isEmpty) 1 else theBlueState.zones.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    var sceneTimeSeconds by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            sceneTimeSeconds = (withFrameNanos { it } - startNanos) / 1_000_000_000f
        }
    }
    val activeZone by remember(pagerState) {
        derivedStateOf { theBlueZoneForPage(pagerState.currentPage) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(shellBackground())
    ) {
        val pageHeight = maxHeight
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (theBlueState.isEmpty) {
                TheBlueEmptyOceanPage(pageHeight = pageHeight)
            } else {
                val zone = theBlueState.zones[page]
                TheBlueZonePage(
                    zone = zone,
                    state = theBlueState,
                    pageHeight = pageHeight,
                    showRoomHeader = zone.zoneId == TheBlueZoneId.SUNLIT_REEF,
                    entryNewAnimalFindIds = entryNewAnimalFindIds,
                    sceneTimeSeconds = sceneTimeSeconds,
                    onZoneBeyondBlue = {
                        beyondBlueInitialZone = zone.zoneId
                        showBeyondBlue = true
                    },
                    onAnimalClick = { selectedAnimal = it }
                )
            }
        }

        if (!theBlueState.isEmpty) {
            TheBlueDepthRail(
                zones = theBlueState.zones.map { it.zoneId },
                activeZone = activeZone,
                onZoneClick = { target ->
                    railNavigationJob?.cancel()
                    railNavigationJob = scope.launch {
                        for (zone in theBlueSequentialNavigationPath(theBlueZoneForPage(pagerState.currentPage), target)) {
                            pagerState.animateScrollToPage(zone.depthOrder())
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }

    selectedAnimal?.let { animal ->
        TheBlueAnimalDetailSheet(
            animal = animal,
            focusSlotId = remember(uiState.focusPlacements, animal.findId) {
                firstOpenFocusSlotFor(animal.findId, uiState)
            },
            pearlBalance = uiState.pearlBalance,
            onDismiss = { selectedAnimal = null },
            onGrow = { instanceId ->
                onGrowCreature(instanceId)
                selectedAnimal = null
            },
            onRelease = {
                releaseCandidate = animal
                selectedAnimal = null
            },
            onBeyondBlue = {
                beyondBlueInitialZone = animal.zoneId
                showBeyondBlue = true
                selectedAnimal = null
            },
            onDisplayInFocus = { instanceId, slotId ->
                onDisplayInFocus(instanceId, slotId)
                selectedAnimal = null
            },
            onOpenChest = {
                selectedAnimal = null
                onOpenChest()
            },
            firstRestingInstanceId = remember(uiState.finds, uiState.focusPlacements, animal.findId) {
                firstRestingInstanceId(animal.findId, uiState)
            }
        )
    }

    releaseCandidate?.let { animal ->
        ReleaseCreatureConfirmationSheet(
            animal = animal,
            onDismiss = { releaseCandidate = null },
            onConfirm = { instanceId ->
                onReleaseCreature(instanceId)
                releaseCandidate = null
            }
        )
    }

    if (showBeyondBlue) {
        BeyondBlueEncounterSheet(
            pearlBalance = uiState.pearlBalance,
            initialZone = beyondBlueInitialZone,
            activeAnimalInstances = uiState.finds.filter {
                it.creatureStatus == CreatureStatus.ACTIVE && ShellContentCatalog.find(it.findId)?.kind == ShellRewardKind.ANIMAL
            },
          onDismiss = { showBeyondBlue = false },
            onEncounter = { targetCreatureId, selectedIds ->
                onEncounterBeyondBlue(targetCreatureId, selectedIds)
                showBeyondBlue = false
            }
        )
    }
}

@Composable
private fun TheBlueEmptyOceanPage(
    pageHeight: Dp
) {
    val scheme = MaterialTheme.colorScheme
    val headerDescription = stringResource(R.string.the_blue_header_a11y)
    val transition = rememberInfiniteTransition(label = "the-blue-empty-motion")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "empty-water-drift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .semantics { contentDescription = headerDescription }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawTheBlueWaterBackground(TheBlueZoneId.SUNLIT_REEF, scheme, drift)
            drawSunlitReefEnvironment(scheme, drift, animalDensity = 0)
            repeat(12) { index ->
                val x = ((index * 67f + drift * size.width * 0.35f) % (size.width + 60f)) - 30f
                val y = size.height - ((index * 43f + drift * size.height) % size.height)
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.10f + (index % 3) * 0.02f),
                    radius = 3f + (index % 4),
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.shell_room_the_blue_empty_body),
                        color = scheme.onSurface.copy(alpha = 0.78f)
                    )
                    Text(
                        text = stringResource(R.string.the_blue_empty_water_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TheBlueZonePage(
    zone: TheBlueZoneUiModel,
    state: TheBlueUiState,
    pageHeight: Dp,
    showRoomHeader: Boolean,
    entryNewAnimalFindIds: Set<String>,
    sceneTimeSeconds: Float,
    onZoneBeyondBlue: () -> Unit,
    onAnimalClick: (TheBlueAnimalGroupUiModel) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val title = zoneTitle(zone.zoneId)
    val subtitle = zoneSubtitle(zone.zoneId)
    val beyondBlueCtaA11y = stringResource(R.string.beyond_blue_encounter_cta)
    val animalSummary = zoneAnimalSummary(zone)
    val zoneDescription = stringResource(R.string.the_blue_zone_scene_a11y, title, subtitle, animalSummary)
    val zoneHasNewArrival = zone.animals.any { it.isNew || it.findId in entryNewAnimalFindIds }
    val waterPhase = sceneTimeSeconds * (0.028f + zone.zoneId.depthOrder() * 0.006f)
    val drift = sceneTimeSeconds * (0.055f + zone.zoneId.depthOrder() * 0.008f)
    val mantaLoop = (sceneTimeSeconds / 20f) % 1f
    val whaleLoop = (sceneTimeSeconds / 34f) % 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .semantics { contentDescription = zoneDescription }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val placements = renderedCreaturePlacements(zone, size.width, size.height, drift, mantaLoop, whaleLoop)
            drawTheBlueWaterBackground(zone.zoneId, scheme, waterPhase)
            drawZoneEnvironment(zone.zoneId, scheme, drift, zone.animals.sumOf { it.totalCount })
            placements.sortedBy { it.zIndex }.forEach { drawRenderedCreature(it, scheme) }
            if (zoneHasNewArrival) {
                drawRect(scheme.secondary.copy(alpha = 0.045f))
            }
        }

        BoxWithConstraints(Modifier.matchParentSize()) {
            val density = LocalDensity.current
            val placements = with(density) {
                renderedCreaturePlacements(
                    zone = zone,
                    sceneWidth = maxWidth.toPx(),
                    sceneHeight = maxHeight.toPx(),
                    drift = drift,
                    mantaLoop = mantaLoop,
                    whaleLoop = whaleLoop
                )
            }
            placements.filter { it.clickable && it.alpha > 0.12f }.forEach { placement ->
                val animal = placement.animal
                val newLabel = if (animal.isNew || animal.findId in entryNewAnimalFindIds) stringResource(R.string.the_blue_new_arrival) else ""
                val description = stringResource(R.string.the_blue_creature_tile_a11y, findName(animal.findId), animal.totalCount, animal.highestLevel, newLabel)
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { placement.tapBounds.left.toDp() },
                            y = with(density) { placement.tapBounds.top.toDp() }
                        )
                        .size(
                            width = with(density) { placement.tapBounds.width.toDp() },
                            height = with(density) { placement.tapBounds.height.toDp() }
                        )
                        .clip(CircleShape)
                        .clickable(onClick = { onAnimalClick(animal) })
                        .semantics {
                            role = Role.Button
                            contentDescription = description
                        }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 20.dp, end = 76.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (showRoomHeader) {
                        Text(
                            text = stringResource(R.string.shell_room_the_blue_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.shell_room_the_blue_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface.copy(alpha = 0.74f)
                        )
                        Text(
                            text = stringResource(
                                R.string.the_blue_stat_row,
                                state.totalAnimals,
                                state.speciesCount,
                                zoneTitle(state.deepestZoneId ?: TheBlueZoneId.SUNLIT_REEF)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface.copy(alpha = 0.76f)
                    )
                }
            }
            TheBlueOverlaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onZoneBeyondBlue)
                    .semantics {
                        role = Role.Button
                        contentDescription = beyondBlueCtaA11y
                    }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.beyond_blue_encounter_cta),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.beyond_blue_discover_depth_copy),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        }

        TheBlueCreatureTray(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 78.dp, bottom = 24.dp),
            zone = zone,
            entryNewAnimalFindIds = entryNewAnimalFindIds,
            onAnimalClick = onAnimalClick
        )
    }
}

@Composable
private fun zoneAnimalSummary(zone: TheBlueZoneUiModel): String {
    if (zone.animals.isEmpty()) return stringResource(R.string.the_blue_zone_waiting)
    val labels = mutableListOf<String>()
    for (animal in zone.animals) {
        labels += stringResource(R.string.the_blue_animal_count, findName(animal.findId), animal.totalCount)
    }
    return labels.joinToString()
}

@Composable
private fun TheBlueOverlaySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = scheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.14f)),
        modifier = modifier,
        content = { Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { content() } }
    )
}

@Composable
private fun TheBlueCreatureTray(
    modifier: Modifier = Modifier,
    zone: TheBlueZoneUiModel,
    entryNewAnimalFindIds: Set<String>,
    onAnimalClick: (TheBlueAnimalGroupUiModel) -> Unit
) {
    var expanded by remember(zone.zoneId) { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TheBlueOverlaySurface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.the_blue_swimming_here), fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(if (expanded) R.string.shell_hide else R.string.shell_view_all),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
                if (zone.animals.isEmpty()) {
                    Text(stringResource(R.string.the_blue_zone_waiting), style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        zone.animals.forEach { animal ->
                            TheBlueCreatureTile(animal, animal.isNew || animal.findId in entryNewAnimalFindIds) { onAnimalClick(animal) }
                        }
                    }
                }
            }
        }
        if (expanded && zone.animals.isNotEmpty()) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.the_blue_zone_life_title, zoneTitle(zone.zoneId)), fontWeight = FontWeight.SemiBold)
                    zone.animals.forEach { animal ->
                        TheBlueExpandedZoneInventoryRow(animal, animal.isNew || animal.findId in entryNewAnimalFindIds) { onAnimalClick(animal) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TheBlueCreatureTile(animal: TheBlueAnimalGroupUiModel, isNewArrival: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val name = findName(animal.findId)
    val contentDescription = stringResource(R.string.the_blue_creature_tile_a11y, name, animal.totalCount, animal.highestLevel, if (isNewArrival) stringResource(R.string.the_blue_new_arrival) else "")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, if (isNewArrival) scheme.secondary.copy(alpha = 0.70f) else scheme.primary.copy(alpha = 0.18f)),
        modifier = Modifier
            .widthIn(min = 110.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShellObjectIcon(CreatureCatalog.get(animal.findId)?.staticIconKey ?: "animal", Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(999.dp), color = scheme.primary.copy(alpha = 0.15f)) {
                    Text(stringResource(R.string.the_blue_count_badge, animal.totalCount), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                ShellMetricPill(icon = Icons.Outlined.EmojiEvents, text = stringResource(R.string.shell_creature_level_short, animal.highestLevel))
            }
            if (isNewArrival) {
                Surface(shape = RoundedCornerShape(999.dp), color = scheme.secondary.copy(alpha = 0.2f)) {
                    Text(stringResource(R.string.the_blue_new_arrival), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = scheme.secondary)
                }
            }
        }
    }
}
@Composable private fun TheBlueExpandedZoneInventoryRow(animal: TheBlueAnimalGroupUiModel, isNewArrival: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(findName(animal.findId), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.the_blue_highest_level_chip, animal.highestLevel))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            animal.levelCounts.forEach { level ->
                val lv = level.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1
                ShellMetricPill(icon = Icons.Outlined.EmojiEvents, text = stringResource(R.string.shell_creature_level_count_chip, lv, level.count))
            }
        }
    }
}

private fun DrawScope.drawTheBlueWaterBackground(
    zoneId: TheBlueZoneId,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    val depth = zoneId.depthOrder()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                scheme.primary.copy(alpha = 0.24f - depth * 0.025f),
                scheme.background.copy(alpha = 0.18f + depth * 0.10f),
                scheme.onSurface.copy(alpha = 0.04f + depth * 0.045f)
            ),
            startY = 0f,
            endY = size.height
        )
    )
    repeat(3) { ray ->
        val offset = sin((drift * 0.55f + ray * 0.73f).toDouble()).toFloat() * size.width * 0.06f
        val path = Path().apply {
            moveTo(size.width * (0.12f + ray * 0.22f) + offset, 0f)
            lineTo(size.width * (0.20f + ray * 0.20f) + offset, size.height)
            lineTo(size.width * (0.30f + ray * 0.20f) + offset, size.height)
            lineTo(size.width * (0.20f + ray * 0.22f) + offset, 0f)
            close()
        }
        drawPath(path, scheme.secondary.copy(alpha = (0.07f - depth * 0.012f).coerceAtLeast(0.018f)))
    }
    repeat(18 - depth * 3) { index ->
        val x = ((index * 83f + drift * size.width * (0.10f + depth * 0.03f)) % (size.width + 70f)) - 35f
        val y = size.height - ((index * 47f + drift * size.height * (0.70f - depth * 0.10f)) % size.height)
        drawCircle(
            color = scheme.primary.copy(alpha = 0.055f + (index % 3) * 0.014f),
            radius = 1.8f + (index % 4),
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawZoneEnvironment(
    zoneId: TheBlueZoneId,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    when (zoneId) {
        TheBlueZoneId.SUNLIT_REEF -> drawSunlitReefEnvironment(scheme, drift, animalDensity)
        TheBlueZoneId.DEEPER_REEF -> drawDeeperReefEnvironment(scheme, drift, animalDensity)
        TheBlueZoneId.OPEN_BLUE -> drawOpenBlueEnvironment(scheme, drift)
        TheBlueZoneId.GREAT_BLUE -> drawGreatBlueEnvironment(scheme, drift)
    }
}

private fun DrawScope.drawSunlitReefEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    val baseY = size.height * 0.82f
    drawOval(scheme.secondary.copy(alpha = 0.11f), Offset(-size.width * 0.10f, baseY), Size(size.width * 1.20f, size.height * 0.34f))
    repeat(6) { i ->
        val rootX = size.width * (0.08f + i * 0.16f)
        val sway = sin((drift * 6.28f + i).toDouble()).toFloat() * 10f
        val height = size.height * (0.12f + (i % 3) * 0.035f)
        drawLine(scheme.primary.copy(alpha = 0.34f), Offset(rootX, size.height), Offset(rootX + sway, size.height - height), strokeWidth = 5f)
        drawCircle(scheme.secondary.copy(alpha = 0.28f), 8f + i, Offset(rootX + sway, size.height - height))
    }
    repeat(5 + min(animalDensity / 8, 4)) { i ->
        val x = size.width * (0.05f + i * 0.20f)
        val y = size.height * (0.78f + (i % 2) * 0.07f)
        drawBranchingCoral(x, y, 36f + (i % 3) * 12f, scheme.secondary.copy(alpha = 0.32f), drift + i * 0.1f)
    }
    repeat(5) { i ->
        drawOval(
            scheme.onSurface.copy(alpha = 0.08f),
            Offset(size.width * (0.12f + i * 0.18f), size.height * (0.88f + (i % 2) * 0.03f)),
            Size(36f + i * 7f, 18f + i * 2f)
        )
    }
}

private fun DrawScope.drawDeeperReefEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    repeat(4) { i ->
        val x = if (i % 2 == 0) size.width * (0.08f + i * 0.08f) else size.width * (0.78f - i * 0.05f)
        val top = size.height * (0.28f + (i % 2) * 0.08f)
        drawRoundRockColumn(x, top, size.height * 0.70f, 42f + i * 8f, scheme.onSurface.copy(alpha = 0.12f))
        drawBranchingCoral(x + 12f, top + 80f, 46f, scheme.primary.copy(alpha = 0.26f), drift + i)
    }
    val caveX = size.width * 0.62f
    val caveY = size.height * 0.70f
    drawOval(scheme.onSurface.copy(alpha = 0.22f), Offset(caveX, caveY), Size(size.width * 0.26f, size.height * 0.16f))
    drawOval(scheme.background.copy(alpha = 0.35f), Offset(caveX + 16f, caveY + 12f), Size(size.width * 0.18f, size.height * 0.10f))
    repeat(5 + min(animalDensity / 6, 4)) { i ->
        val x = size.width * (0.18f + i * 0.15f)
        val sway = sin((drift * 6.28f + i).toDouble()).toFloat() * 8f
        drawLine(scheme.primary.copy(alpha = 0.18f), Offset(x, 0f), Offset(x + sway, size.height * (0.16f + (i % 3) * 0.04f)), strokeWidth = 4f)
    }
    repeat(7) { i ->
        drawCircle(scheme.secondary.copy(alpha = 0.10f), 2.5f + (i % 2), Offset(size.width * (0.15f + i * 0.11f), size.height * (0.42f + (i % 3) * 0.08f)))
    }
}

private fun DrawScope.drawOpenBlueEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    repeat(7) { i ->
        val y = size.height * (0.18f + i * 0.10f)
        val xOffset = sin((drift * 6.28f + i).toDouble()).toFloat() * 28f
        drawLine(
            scheme.primary.copy(alpha = 0.11f),
            Offset(-40f + xOffset, y),
            Offset(size.width + 40f + xOffset, y + 24f),
            strokeWidth = 2.5f
        )
    }
    drawOval(scheme.onSurface.copy(alpha = 0.055f), Offset(size.width * 0.62f, size.height * 0.78f), Size(size.width * 0.45f, size.height * 0.16f))
}

private fun DrawScope.drawGreatBlueEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    repeat(5) { i ->
        val y = size.height * (0.18f + i * 0.14f)
        drawLine(scheme.onSurface.copy(alpha = 0.045f), Offset(0f, y), Offset(size.width, y + sin((drift * 6.28f + i).toDouble()).toFloat() * 10f), strokeWidth = 10f)
    }
    repeat(10) { i ->
        val x = ((i * 97f + drift * size.width * 0.04f) % size.width)
        val y = ((i * 61f + drift * size.height * 0.12f) % size.height)
        drawCircle(scheme.secondary.copy(alpha = 0.035f), 1.5f + (i % 2), Offset(x, y))
    }
}



private data class TheBlueSceneSafeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)

    fun clampCenter(center: Offset, halfWidth: Float, halfHeight: Float): Offset {
        val minX = left + halfWidth
        val maxX = right - halfWidth
        val minY = top + halfHeight
        val maxY = bottom - halfHeight
        return Offset(
            x = if (minX <= maxX) center.x.coerceIn(minX, maxX) else (left + right) / 2f,
            y = if (minY <= maxY) center.y.coerceIn(minY, maxY) else (top + bottom) / 2f
        )
    }
}

private data class TheBlueRenderedCreature(
    val animal: TheBlueAnimalGroupUiModel,
    val definition: CreatureDefinition,
    val center: Offset,
    val visualBounds: Rect,
    val tapBounds: Rect,
    val scale: Float,
    val alpha: Float,
    val zIndex: Float,
    val sceneBehavior: CreatureSceneBehavior,
    val placementBand: CreaturePlacementBand,
    val driftSeed: Float,
    val glowing: Boolean,
    val rendererKey: String,
    val facingRight: Boolean,
    val clickable: Boolean = true
)

private data class LifePresencePlan(
    val directIndividuals: List<LifeAgent>,
    val cohorts: List<LifeCohort>,
    val habitatMarks: List<HabitatPresence>,
    val overflowCount: Int
)

private data class LifeAgent(
    val key: String,
    val findId: String,
    val representativeIndex: Int,
    val level: Int,
    val sourceType: String?,
    val laneId: String,
    val motionMode: LifeMotionMode,
    val clickable: Boolean = true
)

private data class LifeCohort(
    val key: String,
    val findId: String,
    val count: Int,
    val laneId: String,
    val motionMode: LifeMotionMode,
    val clickable: Boolean = true
)

private data class HabitatPresence(
    val key: String,
    val findId: String,
    val countRepresented: Int,
    val kind: HabitatPresenceKind,
    val placementBand: CreaturePlacementBand,
    val alpha: Float,
    val clickable: Boolean = false
)

private data class PresenceAccounting(
    val owned: Int,
    var representedDirect: Int = 0,
    var representedCohort: Int = 0,
    var representedHabitat: Int = 0
) {
    val representedTotal: Int get() = representedDirect + representedCohort + representedHabitat
    val remaining: Int get() = (owned - representedTotal).coerceAtLeast(0)
}

private enum class LifeMotionMode { VISIBLE_LANE, ANCHORED, DRIFT_BOUNDED, PASS_THROUGH, AMBIENT }

private enum class HabitatPresenceKind { SCHOOL_SHIMMER, POD_SHADOW, BLOOM_GLOW, REEF_CLUSTER, DISTANT_SILHOUETTE, CURRENT_TRAIL, BUBBLE_CLUSTER }

private fun theBlueSceneSafeBounds(sceneWidth: Float, sceneHeight: Float): TheBlueSceneSafeBounds {
    val horizontalInset = max(24f, sceneWidth * 0.055f)
    return TheBlueSceneSafeBounds(
        left = horizontalInset,
        top = max(132f, sceneHeight * 0.24f),
        right = (sceneWidth - max(92f, sceneWidth * 0.18f)).coerceAtLeast(horizontalInset + 1f),
        bottom = (sceneHeight - max(170f, sceneHeight * 0.24f)).coerceAtLeast(max(132f, sceneHeight * 0.24f) + 1f)
    )
}

private fun isUniqueLegendaryCreature(definition: CreatureDefinition): Boolean {
    val id = definition.creatureId.lowercase()
    return id.contains("leviathan") || id.contains("kraken") || id.contains("megalodon")
}

private fun lifePresencePlan(
    animal: TheBlueAnimalGroupUiModel,
    definition: CreatureDefinition
): LifePresencePlan {
    val owned = animal.totalCount.coerceAtLeast(0)
    if (owned == 0) {
        return LifePresencePlan(emptyList(), emptyList(), emptyList(), overflowCount = 0)
    }
    val uniqueLegendary = isUniqueLegendaryCreature(definition)
    val directLimit = when {
        uniqueLegendary -> 1
        definition.renderFamily == CreatureRenderFamily.WHALE -> 3
        definition.renderFamily == CreatureRenderFamily.RAY -> 3
        definition.renderFamily == CreatureRenderFamily.JELLYFISH -> 5
        definition.sceneBehavior == CreatureSceneBehavior.BOTTOM_DWELL -> 5
        definition.scaleClass == CreatureScaleClass.TINY -> 8
        definition.scaleClass == CreatureScaleClass.SMALL -> 6
        definition.scaleClass == CreatureScaleClass.MEDIUM -> 5
        definition.scaleClass == CreatureScaleClass.LARGE -> 4
        definition.scaleClass == CreatureScaleClass.GIANT -> 3
        definition.scaleClass == CreatureScaleClass.LEGENDARY -> 2
        else -> 3
    }
    val directCount = owned.coerceAtMost(directLimit)
    val directMode = when (definition.sceneBehavior) {
        CreatureSceneBehavior.BOTTOM_DWELL -> LifeMotionMode.ANCHORED
        CreatureSceneBehavior.DRIFT -> LifeMotionMode.DRIFT_BOUNDED
        else -> LifeMotionMode.VISIBLE_LANE
    }
    val directIndividuals = (0 until directCount).map { index ->
        LifeAgent(
            key = "${animal.findId}:direct:$index",
            findId = animal.findId,
            representativeIndex = index,
            level = animal.highestLevel,
            sourceType = definition.sourceType.name,
            laneId = "${definition.placementBand.name.lowercase()}:$index",
            motionMode = directMode
        )
    }
    val overflow = (owned - directCount).coerceAtLeast(0)
    val cohortMode = when (definition.sceneBehavior) {
        CreatureSceneBehavior.BOTTOM_DWELL -> LifeMotionMode.ANCHORED
        CreatureSceneBehavior.DRIFT -> LifeMotionMode.DRIFT_BOUNDED
        else -> LifeMotionMode.VISIBLE_LANE
    }
    val cohorts = if (overflow > 0) {
        listOf(
            LifeCohort(
                key = "${animal.findId}:cohort",
                findId = animal.findId,
                count = overflow,
                laneId = "${definition.placementBand.name.lowercase()}:cohort",
                motionMode = cohortMode
            )
        )
    } else {
        emptyList()
    }
    val habitatKind = when {
        uniqueLegendary -> HabitatPresenceKind.CURRENT_TRAIL
        definition.renderFamily == CreatureRenderFamily.WHALE -> HabitatPresenceKind.POD_SHADOW
        definition.renderFamily == CreatureRenderFamily.RAY -> HabitatPresenceKind.DISTANT_SILHOUETTE
        definition.renderFamily == CreatureRenderFamily.JELLYFISH -> HabitatPresenceKind.BLOOM_GLOW
        definition.sceneBehavior == CreatureSceneBehavior.BOTTOM_DWELL -> HabitatPresenceKind.REEF_CLUSTER
        definition.scaleClass <= CreatureScaleClass.SMALL -> HabitatPresenceKind.SCHOOL_SHIMMER
        else -> HabitatPresenceKind.BUBBLE_CLUSTER
    }
    val habitatMarks = if (overflow > 0) {
        listOf(
            HabitatPresence(
                key = "${animal.findId}:habitat",
                findId = animal.findId,
                countRepresented = overflow,
                kind = habitatKind,
                placementBand = definition.placementBand,
                alpha = if (uniqueLegendary) 0.26f else 0.18f
            )
        )
    } else {
        emptyList()
    }
    return LifePresencePlan(
        directIndividuals = directIndividuals,
        cohorts = cohorts,
        habitatMarks = habitatMarks,
        overflowCount = overflow
    )
}

private fun movementLaneCount(definition: CreatureDefinition, plan: LifePresencePlan): Int = when {
    isUniqueLegendaryCreature(definition) -> 1
    definition.renderFamily == CreatureRenderFamily.WHALE -> max(3, plan.directIndividuals.size)
    definition.renderFamily == CreatureRenderFamily.RAY -> max(3, plan.directIndividuals.size)
    definition.scaleClass == CreatureScaleClass.GIANT -> max(3, plan.directIndividuals.size)
    definition.scaleClass == CreatureScaleClass.LARGE -> max(3, plan.directIndividuals.size)
    definition.scaleClass == CreatureScaleClass.MEDIUM -> max(4, plan.directIndividuals.size)
    else -> max(5, plan.directIndividuals.size)
}

private fun offscreenMarginFor(visualWidth: Float, definition: CreatureDefinition): Float {
    val multiplier = when (definition.scaleClass) {
        CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 0.70f
        CreatureScaleClass.LARGE -> 0.55f
        else -> 0.45f
    }
    return (visualWidth * multiplier).coerceIn(48f, 180f)
}


private fun renderedCreaturePlacements(
    zone: TheBlueZoneUiModel,
    sceneWidth: Float,
    sceneHeight: Float,
    drift: Float,
    mantaLoop: Float,
    whaleLoop: Float
): List<TheBlueRenderedCreature> {
    val safeBounds = theBlueSceneSafeBounds(sceneWidth, sceneHeight)
    val placements = mutableListOf<TheBlueRenderedCreature>()
    val occupied = mutableListOf<Rect>()
    val sortedAnimals = zone.animals.sortedWith(
        compareByDescending<TheBlueAnimalGroupUiModel> { animal ->
            val definition = CreatureCatalog.get(animal.findId)
            when (definition?.scaleClass) {
                CreatureScaleClass.LEGENDARY -> 6
                CreatureScaleClass.GIANT -> 5
                CreatureScaleClass.LARGE -> 4
                CreatureScaleClass.MEDIUM -> 3
                CreatureScaleClass.SMALL -> 2
                CreatureScaleClass.TINY -> 1
                null -> 0
            }
        }.thenBy { it.findId }
    )

    sortedAnimals.forEach { animal ->
        val definition = CreatureCatalog.get(animal.findId) ?: return@forEach
        val plan = lifePresencePlan(animal, definition)
        val accentCount = animal.levelCounts.filter { (it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1) > 1 }.sumOf { it.count }
        val levelScale = CreatureEconomy.animalVisualScale(animal.findId, animal.highestLevel)
        val tapBase = when (definition.scaleClass) {
            CreatureScaleClass.TINY -> 48f
            CreatureScaleClass.SMALL -> 58f
            CreatureScaleClass.MEDIUM -> 70f
            CreatureScaleClass.LARGE -> 92f
            CreatureScaleClass.GIANT -> 126f
            CreatureScaleClass.LEGENDARY -> 150f
        }
        val visualBase = when (definition.scaleClass) {
            CreatureScaleClass.TINY -> 34f
            CreatureScaleClass.SMALL -> 44f
            CreatureScaleClass.MEDIUM -> 58f
            CreatureScaleClass.LARGE -> 86f
            CreatureScaleClass.GIANT -> 122f
            CreatureScaleClass.LEGENDARY -> 150f
        }
        val zIndex = when (definition.sceneBehavior) {
            CreatureSceneBehavior.BOTTOM_DWELL -> 1f
            CreatureSceneBehavior.DRIFT -> 2f
            CreatureSceneBehavior.SWIM -> 3f
            CreatureSceneBehavior.GLIDE -> 4f
            CreatureSceneBehavior.CRUISE -> 5f
            CreatureSceneBehavior.LEGENDARY -> 6f
        }
        fun rendererFor(findId: String): String = when (findId) {
            ShellContentCatalog.FOCUS_MINNOW -> "minnow"
            ShellContentCatalog.FOCUS_SEAHORSE -> "seahorse"
            ShellContentCatalog.FOCUS_MANTA -> "manta"
            ShellContentCatalog.FOCUS_WHALE -> "base_whale"
            ShellContentCatalog.FOCUS_OCTOPUS -> "octopus"
            else -> definition.creatureId
        }
        fun visibleCenter(center: Offset, visualWidth: Float, visualHeight: Float): Offset =
            safeBounds.clampCenter(center, visualWidth / 2f, visualHeight / 2f)
        fun tryAdd(
            center: Offset,
            scale: Float,
            seed: Float,
            index: Int,
            facingRight: Boolean,
            renderer: String = rendererFor(animal.findId),
            reserveCorridor: Boolean = false,
            alphaMultiplier: Float = 1f,
            clickable: Boolean = true,
            useLoopAlpha: Boolean = false
        ): Boolean {
            val visualWidth = visualBase * scale * when (definition.scaleClass) {
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 1.55f
                CreatureScaleClass.LARGE -> 1.35f
                else -> 1.18f
            }
            val visualHeight = visualBase * scale
            val clamped = if (reserveCorridor) {
                Offset(center.x, center.y.coerceIn(safeBounds.top + visualHeight / 2f, safeBounds.bottom - visualHeight / 2f))
            } else {
                safeBounds.clampCenter(center, visualWidth / 2f, visualHeight / 2f)
            }
            val tapSize = tapBase * scale.coerceIn(0.85f, 1.9f)
            val visualBounds = Rect(clamped.x - visualWidth / 2f, clamped.y - visualHeight / 2f, clamped.x + visualWidth / 2f, clamped.y + visualHeight / 2f)
            val tapBounds = Rect(clamped.x - tapSize / 2f, clamped.y - tapSize / 2f, clamped.x + tapSize / 2f, clamped.y + tapSize / 2f)
            val spacing = when (definition.scaleClass) {
                CreatureScaleClass.TINY, CreatureScaleClass.SMALL -> 18f
                CreatureScaleClass.MEDIUM -> 26f
                CreatureScaleClass.LARGE -> 38f
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 54f
            }
            val collisionBounds = if (reserveCorridor) {
                Rect(safeBounds.left, tapBounds.top - spacing, safeBounds.right, tapBounds.bottom + spacing)
            } else {
                Rect(tapBounds.left - spacing, tapBounds.top - spacing, tapBounds.right + spacing, tapBounds.bottom + spacing)
            }
            if (occupied.any { rectsOverlap(it, collisionBounds) }) return false
            occupied += collisionBounds
            val alpha = if (useLoopAlpha) loopAlpha(clamped.x, visualWidth, safeBounds) else 1f
            placements += TheBlueRenderedCreature(
                animal = animal,
                definition = definition,
                center = clamped,
                visualBounds = visualBounds,
                tapBounds = tapBounds,
                scale = scale,
                alpha = alpha * alphaMultiplier,
                zIndex = zIndex,
                sceneBehavior = definition.sceneBehavior,
                placementBand = definition.placementBand,
                driftSeed = seed,
                glowing = index < accentCount,
                rendererKey = renderer,
                facingRight = facingRight,
                clickable = clickable
            )
            return true
        }
        fun tryCandidates(
            candidates: List<Offset>,
            scale: Float,
            seed: Float,
            index: Int,
            facingRight: Boolean,
            renderer: String = rendererFor(animal.findId),
            reserveCorridor: Boolean = false,
            alphaMultiplier: Float = 1f,
            clickable: Boolean = true,
            useLoopAlpha: Boolean = false
        ): Boolean = candidates.any { tryAdd(it, scale, seed, index, facingRight, renderer, reserveCorridor, alphaMultiplier, clickable, useLoopAlpha) }

        fun directCenter(index: Int, plannedCount: Int, scale: Float, motionMode: LifeMotionMode): Pair<Offset, Boolean> {
            val phase = stablePhase(animal.findId, index)
            val tau = 6.2831855f
            val visualWidth = visualBase * scale * if (definition.scaleClass >= CreatureScaleClass.LARGE) 1.35f else 1.18f
            val visualHeight = visualBase * scale
            return when (motionMode) {
                LifeMotionMode.ANCHORED -> {
                    val laneCount = max(5, plannedCount)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val baseX = safeBounds.left + safeBounds.width * ((lane + 1f) / (laneCount + 1f))
                    val y = safeBounds.bottom - (22f + (index % 2) * 22f) * scale
                    Offset(baseX, y) to stableFacingRight(animal.findId, index)
                }
                LifeMotionMode.DRIFT_BOUNDED -> {
                    val laneCount = max(4, plannedCount)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val baseX = safeBounds.left + safeBounds.width * ((lane + 1f) / (laneCount + 1f))
                    val xMotion = drift * tau + phase * tau
                    val yMotion = drift * tau * 0.62f + phase * tau
                    val x = baseX + sin(xMotion.toDouble()).toFloat() * min(34f, safeBounds.width * 0.055f)
                    val baseY = safeBounds.top + safeBounds.height * (0.20f + (index % 3) * 0.22f)
                    val y = baseY + sin(yMotion.toDouble()).toFloat() * 16f
                    visibleCenter(Offset(x, y), visualWidth, visualHeight) to (cos(xMotion.toDouble()).toFloat() >= 0f)
                }
                LifeMotionMode.VISIBLE_LANE -> {
                    val laneCount = movementLaneCount(definition, plan)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val baseX = safeBounds.left + safeBounds.width * ((index + 1f) / (plannedCount + 1f))
                    val motionClock = when (definition.sceneBehavior) {
                        CreatureSceneBehavior.GLIDE -> mantaLoop
                        CreatureSceneBehavior.CRUISE, CreatureSceneBehavior.LEGENDARY -> whaleLoop
                        else -> drift
                    }
                    val xMotion = motionClock * tau + phase * tau
                    val yMotion = motionClock * tau * 0.57f + phase * tau
                    val x = baseX + sin(xMotion.toDouble()).toFloat() * min(48f, safeBounds.width * 0.065f)
                    val baseY = safeBounds.top + safeBounds.height * ((lane + 1f) / (laneCount + 1f))
                    val y = baseY + sin(yMotion.toDouble()).toFloat() * min(18f, safeBounds.height * 0.035f)
                    visibleCenter(Offset(x, y), visualWidth, visualHeight) to (cos(xMotion.toDouble()).toFloat() >= 0f)
                }
                LifeMotionMode.PASS_THROUGH, LifeMotionMode.AMBIENT -> {
                    val facingRight = stableFacingRight(animal.findId, index)
                    val progress = (drift + phase) % 1f
                    val margin = offscreenMarginFor(visualWidth, definition)
                    val x = offscreenHorizontalPassX(progress, safeBounds.left, safeBounds.right, visualWidth, margin, facingRight)
                    val laneCount = movementLaneCount(definition, plan)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val y = safeBounds.top + safeBounds.height * ((lane + 1f) / (laneCount + 1f))
                    Offset(x, y) to facingRight
                }
            }
        }

        val accounting = PresenceAccounting(owned = animal.totalCount.coerceAtLeast(0))
        val failedDirectAgents = mutableListOf<LifeAgent>()
        plan.directIndividuals.forEach { agent ->
            val i = agent.representativeIndex
            val scale = when (animal.findId) {
                ShellContentCatalog.FOCUS_WHALE -> (1.20f + (i % 3) * 0.06f) * levelScale
                ShellContentCatalog.FOCUS_MANTA -> (1.00f + (i % 3) * 0.08f) * levelScale
                else -> (0.92f + (i % 3) * 0.08f) * levelScale
            }
            val seed = drift + stablePhase(animal.findId, i)
            val (center, facingRight) = directCenter(i, plan.directIndividuals.size.coerceAtLeast(1), scale, agent.motionMode)
            val visualWidth = visualBase * scale * if (definition.scaleClass >= CreatureScaleClass.LARGE) 1.35f else 1.18f
            val visualHeight = visualBase * scale
            val directCandidates = listOf(
                center,
                safeBounds.clampCenter(Offset(center.x, center.y + safeBounds.height * 0.14f), visualWidth / 2f, visualHeight / 2f),
                safeBounds.clampCenter(Offset(center.x, center.y - safeBounds.height * 0.14f), visualWidth / 2f, visualHeight / 2f),
                safeBounds.clampCenter(Offset(center.x + safeBounds.width * 0.10f, center.y), visualWidth / 2f, visualHeight / 2f),
                safeBounds.clampCenter(Offset(center.x - safeBounds.width * 0.10f, center.y), visualWidth / 2f, visualHeight / 2f)
            ).distinct()
            val placed = tryCandidates(
                candidates = directCandidates,
                scale = scale,
                seed = seed,
                index = i,
                facingRight = facingRight,
                clickable = true,
                alphaMultiplier = 1f
            ) || tryCandidates(
                candidates = directCandidates,
                scale = scale * 0.86f,
                seed = seed,
                index = i,
                facingRight = facingRight,
                clickable = true,
                alphaMultiplier = 0.96f
            )
            if (placed) {
                accounting.representedDirect++
            } else {
                failedDirectAgents += agent
            }
        }

        val desiredCohortCount = plan.cohorts.sumOf { it.count } + failedDirectAgents.size
        val cohortCount = min(desiredCohortCount, accounting.remaining)
        if (cohortCount > 0) {
            val cohortIndex = plan.directIndividuals.size
            val cohortScale = when (definition.scaleClass) {
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 0.72f
                CreatureScaleClass.LARGE -> 0.76f
                else -> 0.82f
            } * levelScale
            val cohortMode = plan.cohorts.firstOrNull()?.motionMode ?: when (definition.sceneBehavior) {
                CreatureSceneBehavior.BOTTOM_DWELL -> LifeMotionMode.ANCHORED
                CreatureSceneBehavior.DRIFT -> LifeMotionMode.DRIFT_BOUNDED
                else -> LifeMotionMode.VISIBLE_LANE
            }
            val (cohortCenter, cohortFacingRight) = directCenter(cohortIndex, plan.directIndividuals.size + 1, cohortScale, cohortMode)
            val cohortPlaced = tryCandidates(
                candidates = listOf(
                    cohortCenter,
                    safeBounds.clampCenter(Offset(cohortCenter.x + safeBounds.width * 0.12f, cohortCenter.y + safeBounds.height * 0.10f), visualBase * cohortScale / 2f, visualBase * cohortScale / 2f),
                    safeBounds.clampCenter(Offset(cohortCenter.x - safeBounds.width * 0.12f, cohortCenter.y - safeBounds.height * 0.10f), visualBase * cohortScale / 2f, visualBase * cohortScale / 2f)
                ),
                scale = cohortScale,
                seed = drift + stablePhase(animal.findId, cohortIndex),
                index = cohortIndex,
                facingRight = cohortFacingRight,
                clickable = true,
                alphaMultiplier = 0.82f
            )
            if (cohortPlaced) {
                accounting.representedCohort += cohortCount
            }
        }

        val desiredHabitatCount = max(plan.habitatMarks.sumOf { it.countRepresented }, accounting.remaining)
        val habitatCount = min(desiredHabitatCount, accounting.remaining).coerceAtMost(5)
        repeat(habitatCount) { habitatIndex ->
            val i = plan.directIndividuals.size + 1 + habitatIndex
            val habitat = plan.habitatMarks.firstOrNull()
            val habitatScale = when (definition.scaleClass) {
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 0.42f
                CreatureScaleClass.LARGE -> 0.48f
                else -> 0.56f
            } * levelScale
            val phase = stablePhase(animal.findId, i)
            val x = safeBounds.left + safeBounds.width * (0.14f + (phase * 0.72f))
            val bandBase = when (definition.sceneBehavior) {
                CreatureSceneBehavior.BOTTOM_DWELL -> 0.88f
                CreatureSceneBehavior.DRIFT -> 0.34f + (habitatIndex % 3) * 0.18f
                else -> 0.24f + (habitatIndex % 4) * 0.16f
            }
            val y = safeBounds.top + safeBounds.height * bandBase.coerceIn(0.12f, 0.90f)
            val habitatPlaced = tryCandidates(
                candidates = listOf(
                    Offset(x, y),
                    Offset((x + safeBounds.width * 0.16f).coerceAtMost(safeBounds.right), (y + safeBounds.height * 0.10f).coerceAtMost(safeBounds.bottom)),
                    Offset((x - safeBounds.width * 0.16f).coerceAtLeast(safeBounds.left), (y - safeBounds.height * 0.10f).coerceAtLeast(safeBounds.top))
                ),
                scale = habitatScale,
                seed = drift + phase,
                index = i,
                facingRight = stableFacingRight(animal.findId, i),
                alphaMultiplier = habitat?.alpha ?: 0.16f,
                clickable = habitat?.clickable ?: false
            )
            if (habitatPlaced) {
                accounting.representedHabitat++
            }
        }
    }
    return placements
}

private fun rectsOverlap(a: Rect, b: Rect): Boolean = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

private fun stableHash(findId: String, index: Int): Long = (findId.hashCode() * 31 + index * 997).toUInt().toLong()

private fun stablePhase(findId: String, index: Int): Float = (stableHash(findId, index) % 1000L) / 1000f

private fun stableLane(findId: String, index: Int, laneCount: Int): Int = if (laneCount <= 1) 0 else (stableHash(findId, index) % laneCount).toInt()

private fun stableFacingRight(findId: String, index: Int): Boolean = (stableHash(findId, index) and 1L) == 0L

private fun stableCruiseEntryPhase(findId: String, index: Int): Float = 0.42f + ((stableHash(findId, index) % 160L) / 1000f)

private fun loopAlpha(centerX: Float, visualWidth: Float, bounds: TheBlueSceneSafeBounds): Float {
    val fade = (visualWidth * 0.75f).coerceAtLeast(48f)
    return when {
        centerX < bounds.left - visualWidth -> 0f
        centerX < bounds.left + fade -> ((centerX - (bounds.left - visualWidth)) / (visualWidth + fade)).coerceIn(0f, 1f)
        centerX > bounds.right + visualWidth -> 0f
        centerX > bounds.right - fade -> (((bounds.right + visualWidth) - centerX) / (visualWidth + fade)).coerceIn(0f, 1f)
        else -> 1f
    }
}

private fun offscreenHorizontalPassX(
    progress: Float,
    left: Float,
    right: Float,
    animalWidth: Float,
    margin: Float,
    facingRight: Boolean
): Float {
    val start = left - animalWidth - margin
    val end = right + animalWidth + margin
    val x = start + (end - start) * progress
    return if (facingRight) x else end - (x - start)
}

private fun DrawScope.drawRenderedCreature(
    placement: TheBlueRenderedCreature,
    scheme: androidx.compose.material3.ColorScheme
) {
    if (placement.alpha <= 0.05f) return

    fun drawCreatureBody() {
        val id = placement.rendererKey.lowercase()
        when {
            id == "minnow" -> drawMinnow(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme)
            id == "seahorse" -> drawSeahorse(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme)
            id == "manta" -> drawManta(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme)
            id == "base_whale" -> drawWhaleProfile(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme, "base_whale")
            id == "octopus" -> drawOctopus(placement.center, placement.scale, placement.glowing, scheme)
            id.contains("starfish") -> drawStarfishScene(placement.center, placement.scale * 0.95f, scheme)
            id.contains("urchin") -> drawUrchinScene(placement.center, placement.scale * 0.86f, scheme)
            id.contains("octopus") -> drawOctopus(placement.center, placement.scale, placement.glowing, scheme)
            id.contains("stingray") -> drawStingrayScene(placement.center, placement.scale * 0.70f, placement.driftSeed, scheme)
            id.contains("manta") -> drawManta(placement.center, placement.scale * 0.72f, placement.driftSeed, placement.glowing, scheme)
            id.contains("whale") -> drawWhaleProfile(placement.center, placement.scale * 0.65f, placement.driftSeed, placement.glowing, scheme, id)
            else -> drawSpeciesSwimmer(placement.center, placement.scale, placement.driftSeed, scheme, id, placement.definition.renderFamily.key)
        }
    }

    if (placement.facingRight) {
        drawCreatureBody()
    } else {
        withTransform({
            scale(scaleX = -1f, scaleY = 1f, pivot = placement.center)
        }) {
            drawCreatureBody()
        }
    }
}


private fun DrawScope.drawSpeciesSwimmer(origin: Offset, scale: Float, drift: Float, scheme: androidx.compose.material3.ColorScheme, creatureId: String, familyKey: String) {
    val bob = sin((drift * 6.28f).toDouble()).toFloat() * 5f
    val id = creatureId.lowercase()
    fun fish(body: Color, accent: Color, wMul: Float = 1f, hMul: Float = 1f, stripes: Int = 0, beak: Boolean = false) {
        val w = 34f * scale * wMul
        val h = 18f * scale * hMul
        val c = Offset(origin.x, origin.y + bob)
        drawOval(body, Offset(c.x - w * 0.50f, c.y - h * 0.50f), Size(w, h))
        drawPath(Path().apply { moveTo(c.x - w*0.48f,c.y); lineTo(c.x - w*0.82f,c.y - h*0.52f); lineTo(c.x - w*0.82f,c.y + h*0.52f); close() }, accent)
        drawPath(Path().apply { moveTo(c.x,c.y - h*0.48f); lineTo(c.x + w*0.13f,c.y - h*1.05f); lineTo(c.x + w*0.24f,c.y - h*0.35f); close() }, accent.copy(alpha=0.72f))
        if (beak) drawPath(Path().apply { moveTo(c.x+w*0.48f,c.y-h*0.12f); lineTo(c.x+w*0.70f,c.y-h*0.25f); lineTo(c.x+w*0.50f,c.y+h*0.18f); close() }, accent)
        repeat(stripes) { n -> drawLine(scheme.surface.copy(alpha=0.42f), Offset(c.x - w*0.18f + n*w*0.14f, c.y-h*0.42f), Offset(c.x - w*0.12f + n*w*0.14f, c.y+h*0.42f), strokeWidth=2f*scale) }
        drawCircle(scheme.onSurface.copy(alpha=0.55f), 1.8f*scale, Offset(c.x+w*0.28f, c.y-h*0.16f))
    }
    when {
        "clownfish" in id -> fish(Color(0xFFE9782E).copy(alpha=0.74f), scheme.surface.copy(alpha=0.60f), hMul=1.10f, stripes=3)
        "blue_tang" in id -> fish(Color(0xFF2D77C8).copy(alpha=0.70f), Color(0xFFF2D14C).copy(alpha=0.70f), wMul=1.05f, stripes=1)
        "butterflyfish" in id -> fish(Color(0xFFF4D35E).copy(alpha=0.70f), scheme.onSurface.copy(alpha=0.50f), wMul=0.88f, hMul=1.45f, stripes=4)
        "angelfish" in id -> fish(Color(0xFF6C63C7).copy(alpha=0.66f), Color(0xFFEFB8C8).copy(alpha=0.56f), wMul=0.85f, hMul=1.55f, stripes=2)
        "parrotfish" in id -> fish(Color(0xFF13A999).copy(alpha=0.70f), Color(0xFFFF8F3D).copy(alpha=0.64f), wMul=1.18f, hMul=1.12f, stripes=2, beak=true)
        "lionfish" in id -> { fish(Color(0xFFB45A3C).copy(alpha=0.68f), Color(0xFFF3D6A2).copy(alpha=0.60f), hMul=1.2f, stripes=4); repeat(6){n->drawLine(scheme.secondary.copy(alpha=0.40f), Offset(origin.x-10f*scale+n*5f*scale, origin.y-7f*scale+bob), Offset(origin.x-22f*scale+n*8f*scale, origin.y-30f*scale+bob), strokeWidth=1.5f*scale)} }
        "pufferfish" in id -> { drawCircle(scheme.primary.copy(alpha=0.58f), 17f*scale, Offset(origin.x,origin.y+bob)); repeat(10){n->val a=n*6.28318f/10f; drawLine(scheme.secondary.copy(alpha=0.60f), Offset(origin.x,origin.y+bob), Offset(origin.x+kotlin.math.cos(a)*25f*scale, origin.y+bob+kotlin.math.sin(a)*25f*scale), strokeWidth=1.2f*scale)} }
        "jellyfish" in id -> drawJellyfishScene(origin, scale, bob, scheme)
        "seahorse" in id -> drawSeahorse(origin, scale, bob, false, scheme)
        "turtle" in id -> drawTurtleScene(origin, scale, bob, scheme)
        "sea_otter" in id || "otter" in id -> drawSeaOtterScene(origin, scale, bob, scheme)
        "sea_lion" in id -> drawSeaLionScene(origin, scale, bob, scheme)
        "penguin" in id -> drawPenguinScene(origin, scale, bob, scheme)
        "seal" in id -> drawSealScene(origin, scale, bob, scheme)
        "dolphin" in id -> drawDolphinScene(origin, scale, bob, scheme)
        "orca" in id -> drawOrcaScene(origin, scale, bob, scheme)
        "anglerfish" in id -> { fish(scheme.onSurface.copy(alpha=0.50f), scheme.secondary.copy(alpha=0.55f), wMul=1.15f, hMul=1.20f); drawLine(scheme.secondary.copy(alpha=0.65f), Offset(origin.x+10f*scale,origin.y-9f*scale+bob), Offset(origin.x+24f*scale,origin.y-30f*scale+bob), strokeWidth=1.7f*scale); drawCircle(scheme.secondary.copy(alpha=0.85f), 3f*scale, Offset(origin.x+25f*scale,origin.y-31f*scale+bob)) }
        "megalodon" in id -> drawMegalodonScene(origin, scale, bob, scheme)
        "great_white" in id || "shark" in id -> drawGreatWhiteSharkScene(origin, scale, bob, scheme)
        "whale" in id -> drawWhaleProfile(origin, scale * 0.65f, drift, false, scheme, id)
        "kraken" in id -> drawKrakenScene(origin, scale, bob, scheme)
        "giant_squid" in id -> drawSquidScene(origin, scale * 1.30f, bob, scheme, giant = true)
        "squid" in id -> drawSquidScene(origin, scale, bob, scheme, giant = false)
        "leviathan" in id -> drawLeviathanScene(origin, scale, bob, scheme)
        "moray_eel" in id || "eel" in id -> drawMorayEelScene(origin, scale, bob, scheme)
        "sea_snake" in id || "snake" in id -> drawSeaSnakeScene(origin, scale, bob, scheme)
        "sunfish" in id -> fish(scheme.primary.copy(alpha=0.56f), scheme.secondary.copy(alpha=0.36f), wMul=0.95f, hMul=1.8f)
        "swordfish" in id -> { fish(scheme.primary.copy(alpha=0.58f), scheme.secondary.copy(alpha=0.40f), wMul=1.65f, hMul=0.75f); drawLine(scheme.secondary.copy(alpha=0.55f), Offset(origin.x+24f*scale,origin.y+bob), Offset(origin.x+58f*scale,origin.y-4f*scale+bob), strokeWidth=1.7f*scale) }
        "flying_fish" in id -> { fish(scheme.primary.copy(alpha=0.58f), scheme.secondary.copy(alpha=0.40f), wMul=1.25f, hMul=0.75f); drawPath(Path().apply{moveTo(origin.x-4f*scale,origin.y-6f*scale+bob);lineTo(origin.x-28f*scale,origin.y-34f*scale+bob);lineTo(origin.x+18f*scale,origin.y-8f*scale+bob);close()}, scheme.secondary.copy(alpha=0.30f)) }
        "barracuda" in id -> fish(scheme.onSurface.copy(alpha=0.42f), scheme.secondary.copy(alpha=0.32f), wMul=1.75f, hMul=0.70f)
        else -> drawMissingCreatureRenderer(origin, scale, scheme)
    }
}

private fun DrawScope.drawMissingCreatureRenderer(origin: Offset, scale: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.42f)
    val accent = scheme.secondary.copy(alpha = 0.36f)
    drawOval(body, Offset(origin.x - 20f * scale, origin.y - 9f * scale), Size(36f * scale, 18f * scale))
    drawPath(Path().apply {
        moveTo(origin.x - 18f * scale, origin.y)
        lineTo(origin.x - 34f * scale, origin.y - 12f * scale)
        lineTo(origin.x - 34f * scale, origin.y + 12f * scale)
        close()
    }, accent)
    drawPath(Path().apply {
        moveTo(origin.x - 2f * scale, origin.y - 8f * scale)
        lineTo(origin.x + 8f * scale, origin.y - 23f * scale)
        lineTo(origin.x + 12f * scale, origin.y - 7f * scale)
        close()
    }, accent)
    drawCircle(scheme.surface.copy(alpha = 0.52f), 1.8f * scale, Offset(origin.x + 9f * scale, origin.y - 3f * scale))
}

private fun DrawScope.drawStarfishScene(origin: Offset, scale: Float, scheme: androidx.compose.material3.ColorScheme) {
    val star = Path()
    repeat(10) { i ->
        val radius = if (i % 2 == 0) 28f * scale else 12f * scale
        val angle = (-90f + i * 36f) * (Math.PI.toFloat() / 180f)
        val x = origin.x + kotlin.math.cos(angle) * radius
        val y = origin.y + kotlin.math.sin(angle) * radius
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(star, scheme.secondary.copy(alpha = 0.74f))
    drawCircle(scheme.primary.copy(alpha = 0.30f), 4f * scale, origin)
    repeat(5) { i ->
        val angle = (-90f + i * 72f) * (Math.PI.toFloat() / 180f)
        drawCircle(
            scheme.surface.copy(alpha = 0.50f),
            2f * scale,
            Offset(origin.x + kotlin.math.cos(angle) * 15f * scale, origin.y + kotlin.math.sin(angle) * 15f * scale)
        )
    }
}

private fun DrawScope.drawUrchinScene(origin: Offset, scale: Float, scheme: androidx.compose.material3.ColorScheme) {
    repeat(18) { i ->
        val angle = i * 6.28318f / 18f
        drawLine(
            scheme.primary.copy(alpha = 0.58f),
            origin,
            Offset(origin.x + kotlin.math.cos(angle) * 25f * scale, origin.y + kotlin.math.sin(angle) * 25f * scale),
            strokeWidth = 1.6f * scale
        )
    }
    drawCircle(scheme.secondary.copy(alpha = 0.62f), 13f * scale, origin)
    drawCircle(scheme.surface.copy(alpha = 0.40f), 2.5f * scale, Offset(origin.x - 4f * scale, origin.y - 4f * scale))
}

private fun DrawScope.drawJellyfishScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val ink = scheme.secondary.copy(alpha = 0.66f)
    val w = 34f * scale
    val h = 18f * scale
    drawCircle(ink, w * 0.42f, Offset(origin.x, origin.y + bob))
    repeat(4) { t -> drawLine(ink, Offset(origin.x - w * 0.30f + t*w*0.20f, origin.y + bob + h*0.30f), Offset(origin.x - w * 0.38f + t*w*0.22f, origin.y + bob + h*1.4f), strokeWidth = 2.4f * scale) }
}

private fun DrawScope.drawTurtleScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    drawOval(scheme.primary.copy(alpha=0.52f), Offset(origin.x-22f*scale, origin.y-12f*scale+bob), Size(44f*scale, 28f*scale))
    drawCircle(scheme.secondary.copy(alpha=0.42f), 7f*scale, Offset(origin.x+26f*scale, origin.y-3f*scale+bob))
    listOf(-1f to -1f, -1f to 1f, 1f to -1f, 1f to 1f).forEach { (sx, sy) ->
        drawOval(scheme.secondary.copy(alpha=0.34f), Offset(origin.x + sx*22f*scale, origin.y + sy*13f*scale + bob), Size(12f*scale, 7f*scale))
    }
}

private fun DrawScope.drawStingrayScene(origin: Offset, scale: Float, drift: Float, scheme: androidx.compose.material3.ColorScheme) {
    val bob = kotlin.math.sin(drift * 6.28318f) * 4f * scale
    val c = Offset(origin.x, origin.y + bob)
    val wing = Path().apply {
        moveTo(c.x, c.y - 16f * scale)
        cubicTo(c.x - 34f * scale, c.y - 10f * scale, c.x - 46f * scale, c.y + 10f * scale, c.x - 42f * scale, c.y + 20f * scale)
        cubicTo(c.x - 17f * scale, c.y + 13f * scale, c.x - 8f * scale, c.y + 14f * scale, c.x, c.y + 27f * scale)
        cubicTo(c.x + 8f * scale, c.y + 14f * scale, c.x + 17f * scale, c.y + 13f * scale, c.x + 42f * scale, c.y + 20f * scale)
        cubicTo(c.x + 46f * scale, c.y + 10f * scale, c.x + 34f * scale, c.y - 10f * scale, c.x, c.y - 16f * scale)
        close()
    }
    drawPath(wing, scheme.primary.copy(alpha = 0.46f))
    drawLine(scheme.secondary.copy(alpha = 0.48f), Offset(c.x, c.y + 20f * scale), Offset(c.x, c.y + 58f * scale), strokeWidth = 2f * scale)
    drawCircle(scheme.surface.copy(alpha = 0.44f), 2.2f * scale, Offset(c.x - 7f * scale, c.y - 2f * scale))
    drawCircle(scheme.surface.copy(alpha = 0.44f), 2.2f * scale, Offset(c.x + 7f * scale, c.y - 2f * scale))
}

private fun DrawScope.drawSeaOtterScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.onSurface.copy(alpha = 0.38f)
    drawOval(body, Offset(origin.x - 28f * scale, origin.y - 3f * scale + bob), Size(54f * scale, 15f * scale))
    drawCircle(body.copy(alpha = 0.78f), 10f * scale, Offset(origin.x + 23f * scale, origin.y - 5f * scale + bob))
    drawCircle(scheme.secondary.copy(alpha = 0.38f), 3.2f * scale, Offset(origin.x - 3f * scale, origin.y + bob))
    drawCircle(scheme.secondary.copy(alpha = 0.38f), 3.2f * scale, Offset(origin.x + 7f * scale, origin.y - 1f * scale + bob))
    drawCircle(scheme.surface.copy(alpha = 0.55f), 1.8f * scale, Offset(origin.x + 27f * scale, origin.y - 8f * scale + bob))
}

private fun DrawScope.drawSealScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.50f)
    drawOval(body, Offset(origin.x - 32f * scale, origin.y - 10f * scale + bob), Size(62f * scale, 21f * scale))
    drawCircle(body.copy(alpha = 0.86f), 9f * scale, Offset(origin.x + 30f * scale, origin.y - 2f * scale + bob))
    drawPath(Path().apply { moveTo(origin.x - 4f * scale, origin.y + 7f * scale + bob); lineTo(origin.x - 22f * scale, origin.y + 24f * scale + bob); lineTo(origin.x + 6f * scale, origin.y + 10f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.35f))
    drawCircle(scheme.surface.copy(alpha = 0.55f), 1.8f * scale, Offset(origin.x + 33f * scale, origin.y - 5f * scale + bob))
}

private fun DrawScope.drawSeaLionScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.56f)
    drawOval(body, Offset(origin.x - 35f * scale, origin.y - 9f * scale + bob), Size(58f * scale, 23f * scale))
    drawOval(body.copy(alpha = 0.90f), Offset(origin.x + 12f * scale, origin.y - 18f * scale + bob), Size(24f * scale, 21f * scale))
    drawPath(Path().apply { moveTo(origin.x - 7f * scale, origin.y + 8f * scale + bob); lineTo(origin.x - 32f * scale, origin.y + 28f * scale + bob); lineTo(origin.x + 5f * scale, origin.y + 13f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.42f))
    drawPath(Path().apply { moveTo(origin.x + 4f * scale, origin.y + 8f * scale + bob); lineTo(origin.x + 31f * scale, origin.y + 25f * scale + bob); lineTo(origin.x + 15f * scale, origin.y + 5f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.32f))
    drawCircle(scheme.surface.copy(alpha = 0.55f), 1.8f * scale, Offset(origin.x + 30f * scale, origin.y - 10f * scale + bob))
}

private fun DrawScope.drawPenguinScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val black = Color(0xFF263238).copy(alpha = 0.72f)
    drawOval(black, Offset(origin.x - 13f * scale, origin.y - 28f * scale + bob), Size(26f * scale, 52f * scale))
    drawOval(scheme.surface.copy(alpha = 0.68f), Offset(origin.x - 7f * scale, origin.y - 14f * scale + bob), Size(14f * scale, 30f * scale))
    drawPath(Path().apply { moveTo(origin.x - 12f * scale, origin.y - 5f * scale + bob); lineTo(origin.x - 32f * scale, origin.y + 8f * scale + bob); lineTo(origin.x - 11f * scale, origin.y + 8f * scale + bob); close() }, scheme.primary.copy(alpha = 0.35f))
    drawPath(Path().apply { moveTo(origin.x + 12f * scale, origin.y - 5f * scale + bob); lineTo(origin.x + 32f * scale, origin.y + 8f * scale + bob); lineTo(origin.x + 11f * scale, origin.y + 8f * scale + bob); close() }, scheme.primary.copy(alpha = 0.35f))
    drawPath(Path().apply { moveTo(origin.x + 8f * scale, origin.y - 22f * scale + bob); lineTo(origin.x + 22f * scale, origin.y - 18f * scale + bob); lineTo(origin.x + 8f * scale, origin.y - 14f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.72f))
}

private fun DrawScope.drawDolphinScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val body = scheme.primary.copy(alpha = 0.64f)
    val rim = scheme.secondary.copy(alpha = 0.24f)
    val silhouette = Path().apply {
        moveTo(c.x - 44f * scale, c.y + 5f * scale)
        cubicTo(c.x - 28f * scale, c.y - 26f * scale, c.x + 18f * scale, c.y - 28f * scale, c.x + 42f * scale, c.y - 6f * scale)
        cubicTo(c.x + 24f * scale, c.y + 14f * scale, c.x - 16f * scale, c.y + 20f * scale, c.x - 44f * scale, c.y + 5f * scale)
        close()
    }
    drawPath(silhouette, rim)
    drawPath(silhouette, body)
    drawPath(Path().apply { moveTo(c.x + 36f * scale, c.y - 8f * scale); lineTo(c.x + 66f * scale, c.y - 13f * scale); lineTo(c.x + 38f * scale, c.y + 1f * scale); close() }, body)
    drawPath(Path().apply { moveTo(c.x - 40f * scale, c.y + 4f * scale); lineTo(c.x - 66f * scale, c.y - 14f * scale); lineTo(c.x - 53f * scale, c.y + 2f * scale); lineTo(c.x - 67f * scale, c.y + 20f * scale); close() }, body.copy(alpha = 0.90f))
    drawPath(Path().apply { moveTo(c.x - 4f * scale, c.y - 19f * scale); lineTo(c.x + 7f * scale, c.y - 43f * scale); lineTo(c.x + 14f * scale, c.y - 17f * scale); close() }, scheme.secondary.copy(alpha = 0.56f))
    drawPath(Path().apply { moveTo(c.x + 0f, c.y + 9f * scale); lineTo(c.x - 24f * scale, c.y + 34f * scale); lineTo(c.x + 12f * scale, c.y + 13f * scale); close() }, scheme.secondary.copy(alpha = 0.34f))
    drawCircle(scheme.onSurface.copy(alpha = 0.58f), 2f * scale, Offset(c.x + 28f * scale, c.y - 10f * scale))
}

private fun DrawScope.drawOrcaScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val black = Color(0xFF18242A).copy(alpha = 0.82f)
    val white = scheme.surface.copy(alpha = 0.86f)
    val outline = scheme.secondary.copy(alpha = 0.22f)
    drawOval(outline, Offset(c.x - 62f * scale, c.y - 22f * scale), Size(118f * scale, 48f * scale))
    drawOval(black, Offset(c.x - 58f * scale, c.y - 18f * scale), Size(108f * scale, 38f * scale))
    drawOval(white, Offset(c.x - 12f * scale, c.y + 4f * scale), Size(42f * scale, 12f * scale))
    drawOval(white.copy(alpha = 0.78f), Offset(c.x + 20f * scale, c.y - 12f * scale), Size(12f * scale, 7f * scale))
    drawPath(Path().apply { moveTo(c.x - 4f * scale, c.y - 18f * scale); lineTo(c.x + 10f * scale, c.y - 55f * scale); lineTo(c.x + 20f * scale, c.y - 16f * scale); close() }, black)
    drawPath(Path().apply { moveTo(c.x - 54f * scale, c.y); lineTo(c.x - 86f * scale, c.y - 22f * scale); lineTo(c.x - 70f * scale, c.y); lineTo(c.x - 88f * scale, c.y + 22f * scale); close() }, black)
    drawCircle(scheme.surface.copy(alpha = 0.72f), 1.8f * scale, Offset(c.x + 34f * scale, c.y - 8f * scale))
}

private fun DrawScope.drawGreatWhiteSharkScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val top = Color(0xFF6F8792).copy(alpha = 0.74f)
    val belly = scheme.surface.copy(alpha = 0.68f)
    val body = Path().apply {
        moveTo(c.x - 62f * scale, c.y + 2f * scale)
        cubicTo(c.x - 34f * scale, c.y - 23f * scale, c.x + 28f * scale, c.y - 22f * scale, c.x + 58f * scale, c.y - 2f * scale)
        cubicTo(c.x + 22f * scale, c.y + 18f * scale, c.x - 38f * scale, c.y + 19f * scale, c.x - 62f * scale, c.y + 2f * scale)
        close()
    }
    drawPath(body, top)
    drawOval(belly, Offset(c.x - 26f * scale, c.y + 4f * scale), Size(58f * scale, 12f * scale))
    drawPath(Path().apply { moveTo(c.x - 4f * scale, c.y - 19f * scale); lineTo(c.x + 8f * scale, c.y - 48f * scale); lineTo(c.x + 18f * scale, c.y - 16f * scale); close() }, top)
    drawPath(Path().apply { moveTo(c.x - 8f * scale, c.y + 10f * scale); lineTo(c.x - 30f * scale, c.y + 34f * scale); lineTo(c.x + 8f * scale, c.y + 13f * scale); close() }, top.copy(alpha = 0.66f))
    drawPath(Path().apply { moveTo(c.x - 58f * scale, c.y + 1f * scale); lineTo(c.x - 91f * scale, c.y - 20f * scale); lineTo(c.x - 75f * scale, c.y + 1f * scale); lineTo(c.x - 92f * scale, c.y + 22f * scale); close() }, top)
    drawCircle(scheme.onSurface.copy(alpha = 0.62f), 1.8f * scale, Offset(c.x + 38f * scale, c.y - 8f * scale))
}

private fun DrawScope.drawMegalodonScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val top = Color(0xFF44515A).copy(alpha = 0.82f)
    val belly = scheme.surface.copy(alpha = 0.54f)
    drawOval(scheme.secondary.copy(alpha = 0.10f), Offset(c.x - 96f * scale, c.y - 38f * scale), Size(178f * scale, 76f * scale))
    val body = Path().apply {
        moveTo(c.x - 82f * scale, c.y + 3f * scale)
        cubicTo(c.x - 46f * scale, c.y - 34f * scale, c.x + 42f * scale, c.y - 31f * scale, c.x + 76f * scale, c.y - 4f * scale)
        cubicTo(c.x + 36f * scale, c.y + 26f * scale, c.x - 48f * scale, c.y + 28f * scale, c.x - 82f * scale, c.y + 3f * scale)
        close()
    }
    drawPath(body, top)
    drawOval(belly, Offset(c.x - 36f * scale, c.y + 7f * scale), Size(78f * scale, 16f * scale))
    drawPath(Path().apply { moveTo(c.x - 8f * scale, c.y - 29f * scale); lineTo(c.x + 9f * scale, c.y - 70f * scale); lineTo(c.x + 25f * scale, c.y - 24f * scale); close() }, top)
    drawPath(Path().apply { moveTo(c.x - 12f * scale, c.y + 13f * scale); lineTo(c.x - 46f * scale, c.y + 48f * scale); lineTo(c.x + 14f * scale, c.y + 18f * scale); close() }, top.copy(alpha = 0.68f))
    drawPath(Path().apply { moveTo(c.x - 80f * scale, c.y + 2f * scale); lineTo(c.x - 124f * scale, c.y - 31f * scale); lineTo(c.x - 101f * scale, c.y + 2f * scale); lineTo(c.x - 125f * scale, c.y + 34f * scale); close() }, top)
    drawCircle(scheme.secondary.copy(alpha = 0.72f), 2.3f * scale, Offset(c.x + 50f * scale, c.y - 12f * scale))
}

private fun DrawScope.drawMorayEelScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.55f)
    drawArc(body, 165f, 250f, false, Offset(origin.x - 38f * scale, origin.y - 20f * scale + bob), Size(74f * scale, 50f * scale), style = Stroke(width = 9f * scale))
    drawOval(body.copy(alpha = 0.88f), Offset(origin.x + 20f * scale, origin.y - 12f * scale + bob), Size(22f * scale, 16f * scale))
    drawLine(scheme.surface.copy(alpha = 0.55f), Offset(origin.x + 29f * scale, origin.y - 2f * scale + bob), Offset(origin.x + 41f * scale, origin.y + 1f * scale + bob), strokeWidth = 1.3f * scale)
    drawCircle(scheme.surface.copy(alpha = 0.62f), 1.7f * scale, Offset(origin.x + 30f * scale, origin.y - 7f * scale + bob))
    drawOval(scheme.onSurface.copy(alpha = 0.12f), Offset(origin.x - 42f * scale, origin.y + 14f * scale + bob), Size(34f * scale, 14f * scale))
}

private fun DrawScope.drawSeaSnakeScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.secondary.copy(alpha = 0.58f)
    val path = Path().apply {
        moveTo(origin.x - 42f * scale, origin.y + 8f * scale + bob)
        cubicTo(origin.x - 22f * scale, origin.y - 24f * scale + bob, origin.x - 6f * scale, origin.y + 30f * scale + bob, origin.x + 12f * scale, origin.y - 4f * scale + bob)
        cubicTo(origin.x + 25f * scale, origin.y - 26f * scale + bob, origin.x + 38f * scale, origin.y - 4f * scale + bob, origin.x + 48f * scale, origin.y - 12f * scale + bob)
    }
    drawPath(path, body, style = Stroke(width = 5f * scale))
    repeat(6) { i ->
        val x = origin.x - 28f * scale + i * 13f * scale
        drawLine(scheme.surface.copy(alpha = 0.45f), Offset(x, origin.y + bob - 7f * scale), Offset(x + 5f * scale, origin.y + bob + 4f * scale), strokeWidth = 1.3f * scale)
    }
    drawCircle(body.copy(alpha = 0.86f), 5.5f * scale, Offset(origin.x + 48f * scale, origin.y - 12f * scale + bob))
}

private fun DrawScope.drawSquidScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme, giant: Boolean) {
    val color = if (giant) scheme.secondary.copy(alpha = 0.58f) else scheme.primary.copy(alpha = 0.48f)
    val bodyHeight = if (giant) 58f else 42f
    drawPath(Path().apply {
        moveTo(origin.x, origin.y - bodyHeight * 0.60f * scale + bob)
        cubicTo(origin.x - 18f * scale, origin.y - 22f * scale + bob, origin.x - 16f * scale, origin.y + 8f * scale + bob, origin.x, origin.y + 18f * scale + bob)
        cubicTo(origin.x + 16f * scale, origin.y + 8f * scale + bob, origin.x + 18f * scale, origin.y - 22f * scale + bob, origin.x, origin.y - bodyHeight * 0.60f * scale + bob)
        close()
    }, color)
    repeat(if (giant) 8 else 5) { i ->
        val startX = origin.x - (if (giant) 16f else 11f) * scale + i * (if (giant) 4.8f else 5.5f) * scale
        val endX = origin.x - 28f * scale + i * (if (giant) 8f else 11f) * scale
        drawLine(color.copy(alpha = 0.85f), Offset(startX, origin.y + 14f * scale + bob), Offset(endX, origin.y + (if (giant) 54f else 42f) * scale + bob), strokeWidth = if (giant) 3.2f * scale else 2.4f * scale)
    }
}

private fun DrawScope.drawKrakenScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val color = scheme.secondary.copy(alpha = 0.66f)
    drawCircle(color.copy(alpha = 0.16f), 64f * scale, origin)
    drawOval(color, Offset(origin.x - 26f * scale, origin.y - 34f * scale + bob), Size(52f * scale, 46f * scale))
    repeat(10) { i ->
        val angle = -2.8f + i * 0.62f
        val start = Offset(origin.x + kotlin.math.cos(angle) * 12f * scale, origin.y + 6f * scale + bob)
        val end = Offset(origin.x + kotlin.math.cos(angle) * (42f + (i % 3) * 8f) * scale, origin.y + 52f * scale + kotlin.math.sin(angle) * 12f * scale + bob)
        drawLine(color.copy(alpha = 0.78f), start, end, strokeWidth = 4f * scale)
    }
    drawCircle(scheme.onSurface.copy(alpha = 0.60f), 2.4f * scale, Offset(origin.x - 8f * scale, origin.y - 14f * scale + bob))
    drawCircle(scheme.onSurface.copy(alpha = 0.60f), 2.4f * scale, Offset(origin.x + 8f * scale, origin.y - 14f * scale + bob))
}

private fun DrawScope.drawLeviathanScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val body = Color(0xFF172A46).copy(alpha = 0.78f)
    val glow = scheme.secondary.copy(alpha = 0.30f)
    drawOval(glow.copy(alpha = 0.10f), Offset(c.x - 102f * scale, c.y - 48f * scale), Size(194f * scale, 92f * scale))
    val spine = Path().apply {
        moveTo(c.x - 88f * scale, c.y + 18f * scale)
        cubicTo(c.x - 52f * scale, c.y - 50f * scale, c.x + 16f * scale, c.y + 46f * scale, c.x + 70f * scale, c.y - 22f * scale)
    }
    drawPath(spine, body, style = Stroke(width = 15f * scale))
    drawPath(spine, glow, style = Stroke(width = 3f * scale))
    drawCircle(body.copy(alpha = 0.96f), 18f * scale, Offset(c.x + 76f * scale, c.y - 24f * scale))
    drawCircle(scheme.secondary.copy(alpha = 0.82f), 3f * scale, Offset(c.x + 83f * scale, c.y - 29f * scale))
    repeat(7) { i ->
        val x = c.x - 58f * scale + i * 20f * scale
        val y = c.y - 6f * scale + sin((i * 0.9f).toDouble()).toFloat() * 20f * scale
        drawPath(Path().apply { moveTo(x, y); lineTo(x + 7f * scale, y - 22f * scale); lineTo(x + 14f * scale, y + 1f * scale); close() }, glow.copy(alpha = 0.42f))
    }
    drawPath(Path().apply { moveTo(c.x + 88f * scale, c.y - 22f * scale); lineTo(c.x + 118f * scale, c.y - 42f * scale); lineTo(c.x + 98f * scale, c.y - 12f * scale); close() }, body)
}

private fun DrawScope.drawMinnow(origin: Offset, scale: Float, wiggle: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = if (glowing) 0.82f else 0.64f)
    val fin = scheme.secondary.copy(alpha = if (glowing) 0.58f else 0.36f)
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.18f), 22f * scale, origin)
    drawOval(body, Offset(origin.x - 14f * scale, origin.y - 6f * scale), Size(28f * scale, 12f * scale))
    val tail = Path().apply {
        moveTo(origin.x - 13f * scale, origin.y)
        lineTo(origin.x - 26f * scale, origin.y - (8f + wiggle * 3f) * scale)
        lineTo(origin.x - 25f * scale, origin.y + (8f - wiggle * 3f) * scale)
        close()
    }
    drawPath(tail, fin)
    val dorsal = Path().apply {
        moveTo(origin.x - 2f * scale, origin.y - 6f * scale)
        lineTo(origin.x + 5f * scale, origin.y - 13f * scale)
        lineTo(origin.x + 9f * scale, origin.y - 5f * scale)
        close()
    }
    drawPath(dorsal, fin.copy(alpha = fin.alpha * 0.75f))
    drawCircle(scheme.onSurface.copy(alpha = 0.74f), 1.6f * scale, Offset(origin.x + 9f * scale, origin.y - 1.5f * scale))
    drawCircle(scheme.secondary.copy(alpha = 0.32f), 1.7f * scale, Offset(origin.x + 2f * scale, origin.y + 2f * scale))
}

private fun DrawScope.drawSeahorse(origin: Offset, scale: Float, bob: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.16f), 30f * scale, origin)
    val color = scheme.secondary.copy(alpha = 0.58f)
    drawCircle(color, 10f * scale, Offset(origin.x, origin.y - 18f * scale))
    drawCircle(color.copy(alpha = 0.82f), 13f * scale, Offset(origin.x - 2f * scale, origin.y + 2f * scale))
    drawLine(color, Offset(origin.x + 7f * scale, origin.y - 19f * scale), Offset(origin.x + 22f * scale, origin.y - 23f * scale), strokeWidth = 5f * scale)
    val crest = Path().apply {
        moveTo(origin.x - 4f * scale, origin.y - 29f * scale)
        lineTo(origin.x + 2f * scale, origin.y - 38f * scale)
        lineTo(origin.x + 7f * scale, origin.y - 28f * scale)
    }
    drawPath(crest, color, style = Stroke(width = 3f * scale))
    val tail = Path().apply {
        moveTo(origin.x - 4f * scale, origin.y + 14f * scale)
        cubicTo(origin.x - 8f * scale, origin.y + 30f * scale, origin.x + 16f * scale, origin.y + 34f * scale, origin.x + 12f * scale, origin.y + 18f * scale)
    }
    drawPath(tail, color, style = Stroke(width = 4f * scale))
    drawOval(scheme.primary.copy(alpha = 0.24f), Offset(origin.x - 15f * scale, origin.y - (2f + bob) * scale), Size(10f * scale, 16f * scale))
    drawCircle(scheme.onSurface.copy(alpha = 0.72f), 1.7f * scale, Offset(origin.x + 6f * scale, origin.y - 21f * scale))
}

private fun DrawScope.drawManta(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val wingPulse = sin((drift * 6.28f).toDouble()).toFloat()
    val wingLift = wingPulse * 6f * scale
    val bodyColor = scheme.primary.copy(alpha = 0.50f)
    val wingColor = scheme.primary.copy(alpha = 0.42f)
    val accent = scheme.secondary.copy(alpha = if (glowing) 0.36f else 0.20f)

    if (glowing) {
        drawOval(
            color = scheme.secondary.copy(alpha = 0.10f),
            topLeft = Offset(origin.x - 92f * scale, origin.y - 58f * scale),
            size = Size(190f * scale, 116f * scale)
        )
    }

    // Right-facing manta/ray silhouette: cephalic lobes and eyes at the right,
    // trailing tail to the left. drawRenderedCreature mirrors this for leftward motion.
    val manta = Path().apply {
        moveTo(origin.x + 78f * scale, origin.y - 6f * scale)
        cubicTo(origin.x + 44f * scale, origin.y - 48f * scale - wingLift, origin.x - 22f * scale, origin.y - 54f * scale, origin.x - 88f * scale, origin.y - 22f * scale - wingLift)
        cubicTo(origin.x - 48f * scale, origin.y - 8f * scale, origin.x - 28f * scale, origin.y + 28f * scale, origin.x + 8f * scale, origin.y + 42f * scale)
        cubicTo(origin.x + 42f * scale, origin.y + 28f * scale, origin.x + 66f * scale, origin.y + 14f * scale, origin.x + 78f * scale, origin.y - 6f * scale)
        close()
    }
    drawPath(manta, wingColor)

    val body = Path().apply {
        moveTo(origin.x + 62f * scale, origin.y - 2f * scale)
        cubicTo(origin.x + 28f * scale, origin.y - 22f * scale, origin.x - 18f * scale, origin.y - 18f * scale, origin.x - 34f * scale, origin.y)
        cubicTo(origin.x - 16f * scale, origin.y + 18f * scale, origin.x + 30f * scale, origin.y + 20f * scale, origin.x + 62f * scale, origin.y - 2f * scale)
        close()
    }
    drawPath(body, bodyColor)
    drawPath(Path().apply {
        moveTo(origin.x + 66f * scale, origin.y - 15f * scale)
        lineTo(origin.x + 92f * scale, origin.y - 28f * scale)
        lineTo(origin.x + 78f * scale, origin.y - 6f * scale)
        close()
    }, bodyColor.copy(alpha = 0.62f))
    drawPath(Path().apply {
        moveTo(origin.x + 66f * scale, origin.y + 4f * scale)
        lineTo(origin.x + 92f * scale, origin.y + 16f * scale)
        lineTo(origin.x + 78f * scale, origin.y - 6f * scale)
        close()
    }, bodyColor.copy(alpha = 0.56f))

    drawCircle(scheme.onSurface.copy(alpha = 0.40f), 1.8f * scale, Offset(origin.x + 58f * scale, origin.y - 10f * scale))
    drawCircle(scheme.onSurface.copy(alpha = 0.34f), 1.6f * scale, Offset(origin.x + 55f * scale, origin.y + 4f * scale))

    val tailSway = sin((drift * 6.28f - 0.8f).toDouble()).toFloat() * 9f * scale
    val tail = Path().apply {
        moveTo(origin.x - 34f * scale, origin.y)
        cubicTo(origin.x - 70f * scale, origin.y + tailSway * 0.2f, origin.x - 100f * scale, origin.y + tailSway, origin.x - 130f * scale, origin.y + tailSway * 0.65f)
    }
    drawPath(tail, bodyColor.copy(alpha = 0.54f), style = Stroke(width = 2.6f * scale))

    if (glowing) {
        drawLine(accent, Offset(origin.x - 60f * scale, origin.y - 16f * scale - wingLift), Offset(origin.x + 20f * scale, origin.y - 4f * scale), strokeWidth = 2f * scale)
        drawLine(accent, Offset(origin.x - 42f * scale, origin.y + 22f * scale + wingLift), Offset(origin.x + 28f * scale, origin.y + 10f * scale), strokeWidth = 2f * scale)
    }
}


private fun DrawScope.drawWhaleProfile(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme, profileKey: String) {
    val key = profileKey.lowercase()
    val bob = sin((drift * 6.28f).toDouble()).toFloat() * 5f * scale
    // Whale silhouettes were originally authored facing left; normalize them to the
    // scene renderer convention (right-facing by default) so the shared
    // facingRight mirror in drawRenderedCreature matches movement direction.
    withTransform({ scale(scaleX = -1f, scaleY = 1f, pivot = origin) }) {
    when {
        "blue_whale" in key -> {
            val color = scheme.onSurface.copy(alpha = 0.44f)
            val rim = if (glowing) scheme.secondary.copy(alpha = 0.24f) else scheme.primary.copy(alpha = 0.18f)
            drawOval(rim, Offset(origin.x - 146f * scale, origin.y - 36f * scale + bob), Size(280f * scale, 70f * scale))
            drawOval(color, Offset(origin.x - 136f * scale, origin.y - 22f * scale + bob), Size(248f * scale, 42f * scale))
            drawOval(scheme.surface.copy(alpha = 0.30f), Offset(origin.x - 70f * scale, origin.y + 1f * scale + bob), Size(116f * scale, 16f * scale))
            drawPath(Path().apply { moveTo(origin.x + 102f * scale, origin.y + bob); lineTo(origin.x + 146f * scale, origin.y - 20f * scale + bob); lineTo(origin.x + 132f * scale, origin.y + bob); lineTo(origin.x + 148f * scale, origin.y + 20f * scale + bob); close() }, color.copy(alpha = 0.86f))
            drawPath(Path().apply { moveTo(origin.x + 14f * scale, origin.y - 20f * scale + bob); lineTo(origin.x + 28f * scale, origin.y - 38f * scale + bob); lineTo(origin.x + 34f * scale, origin.y - 18f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.36f))
        }
        "humpback" in key -> {
            val color = scheme.onSurface.copy(alpha = 0.38f)
            val rim = if (glowing) scheme.secondary.copy(alpha = 0.24f) else scheme.primary.copy(alpha = 0.16f)
            val back = Path().apply {
                moveTo(origin.x - 104f * scale, origin.y + 6f * scale + bob)
                cubicTo(origin.x - 70f * scale, origin.y - 56f * scale + bob, origin.x + 36f * scale, origin.y - 42f * scale + bob, origin.x + 90f * scale, origin.y - 2f * scale + bob)
                cubicTo(origin.x + 42f * scale, origin.y + 36f * scale + bob, origin.x - 56f * scale, origin.y + 42f * scale + bob, origin.x - 104f * scale, origin.y + 6f * scale + bob)
                close()
            }
            drawPath(back, rim)
            drawPath(back, color)
            drawLine(scheme.secondary.copy(alpha = 0.32f), Offset(origin.x - 18f * scale, origin.y + 18f * scale + bob), Offset(origin.x - 80f * scale, origin.y + 76f * scale + bob), strokeWidth = 6f * scale)
            drawPath(Path().apply { moveTo(origin.x + 82f * scale, origin.y + bob); lineTo(origin.x + 124f * scale, origin.y - 24f * scale + bob); lineTo(origin.x + 110f * scale, origin.y + bob); lineTo(origin.x + 126f * scale, origin.y + 24f * scale + bob); close() }, color.copy(alpha = 0.82f))
            repeat(4) { i -> drawCircle(scheme.secondary.copy(alpha = 0.24f), 2f * scale, Offset(origin.x - 82f * scale + i * 10f * scale, origin.y - 6f * scale + bob)) }
        }
        else -> drawWhale(origin, scale, drift, glowing, scheme)
    }
    }
}

private fun DrawScope.drawWhale(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val color = scheme.onSurface.copy(alpha = 0.36f)
    val rim = if (glowing) scheme.secondary.copy(alpha = 0.26f) else scheme.primary.copy(alpha = 0.18f)
    drawOval(rim, Offset(origin.x - 118f * scale, origin.y - 38f * scale), Size(220f * scale, 78f * scale))
    drawOval(color, Offset(origin.x - 108f * scale, origin.y - 28f * scale), Size(190f * scale, 56f * scale))
    drawOval(color.copy(alpha = 0.16f), Offset(origin.x - 54f * scale, origin.y + 2f * scale), Size(94f * scale, 22f * scale))
    val tailWave = sin((drift * 6.28f).toDouble()).toFloat() * 8f * scale
    val tail = Path().apply {
        moveTo(origin.x + 78f * scale, origin.y)
        lineTo(origin.x + 126f * scale, origin.y - 25f * scale + tailWave)
        lineTo(origin.x + 114f * scale, origin.y)
        lineTo(origin.x + 128f * scale, origin.y + 25f * scale + tailWave)
        close()
    }
    drawPath(tail, color.copy(alpha = 0.26f))
    drawCircle(scheme.background.copy(alpha = 0.45f), 2.4f * scale, Offset(origin.x - 74f * scale, origin.y - 8f * scale))
}

private fun DrawScope.drawOctopus(origin: Offset, pulse: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.15f), 46f * pulse, origin)
    val color = scheme.secondary.copy(alpha = 0.46f)
    drawOval(color, Offset(origin.x - 22f * pulse, origin.y - 30f * pulse), Size(44f * pulse, 38f * pulse))
    repeat(6) { i ->
        val startX = origin.x - 18f + i * 7f
        val curl = sin((pulse * 4f + i).toDouble()).toFloat() * 8f
        val tentacle = Path().apply {
            moveTo(startX, origin.y + 2f)
            cubicTo(startX - 10f, origin.y + 22f, startX + curl, origin.y + 32f, startX - 4f, origin.y + 44f)
        }
        drawPath(tentacle, color, style = Stroke(width = 4f))
    }
    drawCircle(scheme.onSurface.copy(alpha = 0.75f), 2.4f * pulse, Offset(origin.x + 8f * pulse, origin.y - 14f * pulse))
}

private fun DrawScope.drawBranchingCoral(x: Float, y: Float, height: Float, color: Color, drift: Float) {
    val sway = sin((drift * 6.28f).toDouble()).toFloat() * 5f
    drawLine(color, Offset(x, y), Offset(x + sway, y - height), strokeWidth = 5f)
    drawLine(color, Offset(x + sway * 0.6f, y - height * 0.55f), Offset(x - 16f + sway, y - height * 0.86f), strokeWidth = 4f)
    drawLine(color, Offset(x + sway * 0.7f, y - height * 0.45f), Offset(x + 17f + sway, y - height * 0.78f), strokeWidth = 4f)
}

private fun DrawScope.drawRoundRockColumn(x: Float, top: Float, bottom: Float, width: Float, color: Color) {
    drawOval(color, Offset(x - width / 2f, top), Size(width, bottom - top))
    drawOval(color.copy(alpha = color.alpha * 0.7f), Offset(x - width * 0.65f, top + 60f), Size(width * 1.3f, width * 0.75f))
}


private fun formatMinutesCompact(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun TheBlueZoneId.toCreatureZone(): CreatureZone = when (this) {
    TheBlueZoneId.SUNLIT_REEF -> CreatureZone.SUNLIT_REEF
    TheBlueZoneId.DEEPER_REEF -> CreatureZone.DEEPER_REEF
    TheBlueZoneId.OPEN_BLUE -> CreatureZone.OPEN_BLUE
    TheBlueZoneId.GREAT_BLUE -> CreatureZone.GREAT_BLUE
}

private fun theBlueZoneFor(zone: CreatureZone): TheBlueZoneId = when (zone) {
    CreatureZone.SUNLIT_REEF -> TheBlueZoneId.SUNLIT_REEF
    CreatureZone.DEEPER_REEF -> TheBlueZoneId.DEEPER_REEF
    CreatureZone.OPEN_BLUE -> TheBlueZoneId.OPEN_BLUE
    CreatureZone.GREAT_BLUE -> TheBlueZoneId.GREAT_BLUE
}

@Composable
private fun TheBlueAnimalDetailSheet(
    animal: TheBlueAnimalGroupUiModel,
    focusSlotId: String?,
    firstRestingInstanceId: String?,
    pearlBalance: Int,
    onDismiss: () -> Unit,
    onGrow: (String) -> Unit,
    onRelease: () -> Unit,
    onBeyondBlue: () -> Unit,
    onDisplayInFocus: (String, String) -> Unit,
    onOpenChest: () -> Unit
) {
    val name = findName(animal.findId)
    val zone = zoneTitle(animal.zoneId)
    val title = stringResource(R.string.the_blue_animal_count, name, animal.totalCount)
    val source = theBlueEncounteredReason(animal.findId)
    val detailDescription = stringResource(R.string.the_blue_detail_a11y, title, zone, source)
    val growthInstanceId = animal.highestLevelActiveInstanceId ?: animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val releaseInstanceId = animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val growthCost = CreatureEconomy.growthCostPearls(animal.findId, animal.highestLevel.coerceAtLeast(1))
    val canGrow = growthInstanceId != null && pearlBalance >= growthCost
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .semantics { contentDescription = detailDescription },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ShellObjectIcon(CreatureCatalog.get(animal.findId)?.staticIconKey ?: "animal", Modifier.size(56.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(text = zone, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(source)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ShellMetricPill(Icons.Outlined.Waves, stringResource(R.string.the_blue_swimming_chip, animal.totalCount))
                ShellMetricPill(Icons.Outlined.AutoStories, stringResource(R.string.the_blue_lifetime_chip, animal.lifetimeEncounteredCount))
                ShellMetricPill(Icons.Outlined.EmojiEvents, stringResource(R.string.the_blue_highest_level_chip, animal.highestLevel))
                if (animal.releasedCount > 0) ShellMetricPill(Icons.Outlined.Route, stringResource(R.string.the_blue_released_chip, animal.releasedCount))
                if (animal.usedBeyondBlueCount > 0) ShellMetricPill(Icons.Outlined.WaterDrop, stringResource(R.string.the_blue_beyond_blue_chip, animal.usedBeyondBlueCount))
            }
            animal.flowTimeValueMinutes?.let {
                ElevatedCard {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Route, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.the_blue_created_by_flow_title)) },
                        supportingContent = {
                            Text(stringResource(R.string.the_blue_created_by_flow_value, formatMinutesCompact(it)))
                        }
                    )
                }
            }
            animal.releaseValuePearls?.let {
                ElevatedCard {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Diamond, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.the_blue_pearl_value_title)) },
                        supportingContent = { Text(stringResource(R.string.shell_creature_pearl_value_each, it)) }
                    )
                }
                ElevatedCard {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Diamond, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.the_blue_release_return_title)) },
                        supportingContent = { Text(stringResource(R.string.shell_creature_release_value_each, it)) }
                    )
                }
            }

            Text(stringResource(R.string.shell_creature_levels_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (animal.levelCounts.isEmpty()) {
                Text(stringResource(R.string.the_blue_forms_unavailable))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    animal.levelCounts.sortedBy { it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 0 }
                        .forEach { level ->
                            val lv = level.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1
                            ShellMetricPill(Icons.Outlined.EmojiEvents, stringResource(R.string.shell_creature_level_count_chip, lv, level.count))
                        }
                }
            }
            Text(stringResource(R.string.the_blue_growth_support_copy))

            Text(stringResource(R.string.the_blue_displayed_in_focus_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(animal.displayedInFocusCount.toString())
            Text(stringResource(R.string.the_blue_resting_in_chest_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(animal.restingCount.toString())

            Text(stringResource(R.string.the_blue_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { growthInstanceId?.let(onGrow) },
                enabled = canGrow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.shell_creature_grow_with_pearls_cost, growthCost))
            }
            if (!canGrow) {
                val missing = (growthCost - pearlBalance).coerceAtLeast(0)
                Text(
                    text = if (growthInstanceId == null) stringResource(R.string.shell_creature_no_active_to_grow) else stringResource(R.string.shell_creature_need_more_pearls_to_grow, missing),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            OutlinedButton(onClick = onBeyondBlue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Waves, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.beyond_blue_encounter_cta))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (firstRestingInstanceId != null && focusSlotId != null) {
                            onDisplayInFocus(firstRestingInstanceId, focusSlotId)
                        }
                    },
                    enabled = firstRestingInstanceId != null && focusSlotId != null,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.the_blue_display_one_in_focus)) }
                OutlinedButton(onClick = onOpenChest, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.the_blue_view_in_chest))
                }
            }
            OutlinedButton(
                onClick = onRelease,
                enabled = releaseInstanceId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.shell_creature_release_for_pearls))
            }
            when (theBlueDisplayDisabledReason(focusSlotId, firstRestingInstanceId)) {
                TheBlueDisplayDisabledReason.NO_FOCUS_SLOT -> Text(
                    stringResource(R.string.the_blue_no_focus_slot, name),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                TheBlueDisplayDisabledReason.NO_RESTING_COPY -> Text(
                    stringResource(R.string.the_blue_no_resting_copy),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                null -> Unit
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ReleaseCreatureConfirmationSheet(
    animal: TheBlueAnimalGroupUiModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val name = findName(animal.findId)
    val instanceId = animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val releaseValue = animal.releaseValuePearls ?: CreatureEconomy.releaseValuePearls(animal.findId, animal.highestLevel)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.shell_creature_release_confirm_title, name, releaseValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.shell_creature_release_confirm_body))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.shell_creature_keep_swimming)) }
                Button(
                    onClick = { instanceId?.let(onConfirm) },
                    enabled = instanceId != null,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.shell_creature_release_for_pearls)) }
            }
        }
    }
}

@Composable
private fun BeyondBlueEncounterSheet(
    pearlBalance: Int,
    initialZone: TheBlueZoneId,
    activeAnimalInstances: List<UserShellFindInstanceEntity>,
    onDismiss: () -> Unit,
    onEncounter: (String, List<String>) -> Unit
) {
    data class TradeStack(
        val key: String,
        val findId: String,
        val level: Int,
        val instances: List<UserShellFindInstanceEntity>,
        val perMinutes: Int
    )

    var confirmTargetId by remember { mutableStateOf<String?>(null) }
    var selectedZone by remember(initialZone) { mutableStateOf(initialZone) }
    var selectedCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var tradeExpanded by remember { mutableStateOf(false) }
    val confirmTarget = confirmTargetId?.let { CreatureCatalog.get(it) }

    val tradeStacks = remember(activeAnimalInstances) {
        activeAnimalInstances
            .filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy { "${it.findId}:${it.animalLevel.coerceAtLeast(1)}" }
            .map { (key, group) ->
                val first = group.first()
                val level = first.animalLevel.coerceAtLeast(1)
                TradeStack(
                    key = key,
                    findId = first.findId,
                    level = level,
                    instances = group.sortedBy { it.acquiredAt },
                    perMinutes = CreatureEconomy.beyondBlueTradeContributionMinutes(first.findId, level)
                )
            }
            .sortedBy { it.findId + ":" + it.level }
    }

    val selectedInstanceIds = remember(selectedCounts, tradeStacks) {
        buildList {
            tradeStacks.forEach { stack ->
                val selected = (selectedCounts[stack.key] ?: 0).coerceIn(0, stack.instances.size)
                stack.instances.take(selected).forEach { add(it.instanceId) }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.beyond_blue_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.beyond_blue_zone_encounters, zoneTitle(selectedZone)))
            Text(zoneSubtitle(selectedZone))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TheBlueZoneId.entries.forEach { zone ->
                    FilterChip(selected = selectedZone == zone, onClick = { selectedZone = zone }, label = { Text(zoneRailLabel(zone)) })
                }
            }

            if (confirmTarget == null) {
                val selectedCreatureZone = selectedZone.toCreatureZone()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CreatureCatalog.beyondBlue.filter { it.zone == selectedCreatureZone }.forEach { target ->
                        val requirement = target.requirementMinutes ?: 0
                        val price = CreatureEconomy.pearlPriceForRequirement(requirement)
                        val canAfford = pearlBalance >= price
                        ElevatedCard(onClick = {
                            confirmTargetId = target.creatureId
                            selectedCounts = emptyMap()
                            tradeExpanded = false
                        }) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ShellObjectIcon(target.staticIconKey, Modifier.size(46.dp))
                                Text(target.displayName, fontWeight = FontWeight.Bold)
                                Text(zoneTitle(theBlueZoneFor(target.zone)), style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ShellMetricPill(Icons.Outlined.Diamond, stringResource(R.string.beyond_blue_or_pearls, price))
                                    ShellMetricPill(Icons.Outlined.Route, formatMinutesCompact(requirement))
                                    ShellMetricPill(Icons.Outlined.WaterDrop, if (canAfford) stringResource(R.string.beyond_blue_ready_to_buy) else stringResource(R.string.beyond_blue_need_more_pearls, (price - pearlBalance).coerceAtLeast(0)))
                                }
                            }
                        }
                    }
                }
            } else {
                val target = confirmTarget
                val requirement = target.requirementMinutes ?: 0
                val pearlOnlyPrice = CreatureEconomy.pearlPriceForRequirement(requirement)
                val selectedMinutes = tradeStacks.sumOf { (selectedCounts[it.key] ?: 0) * it.perMinutes }
                val quote = CreatureEconomy.quoteBeyondBluePayment(target.creatureId, selectedMinutes, pearlBalance)
                val progress = if (requirement == 0) 1f else (selectedMinutes.toFloat() / requirement.toFloat()).coerceIn(0f, 1f)
                var showConfirm by remember(target.creatureId, selectedCounts, quote) { mutableStateOf(false) }

                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShellObjectIcon(target.staticIconKey, Modifier.size(56.dp))
                        Text(target.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(zoneTitle(theBlueZoneFor(target.zone)), color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.beyond_blue_life_waiting_depth))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ShellMetricPill(Icons.Outlined.Route, formatMinutesCompact(requirement))
                            ShellMetricPill(Icons.Outlined.Diamond, stringResource(R.string.beyond_blue_or_pearls, pearlOnlyPrice))
                        }
                    }
                }

                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.beyond_blue_buy_with_pearls), fontWeight = FontWeight.SemiBold)
                        ShellMetricPill(Icons.Outlined.Diamond, stringResource(R.string.beyond_blue_use_pearls_only_amount, pearlOnlyPrice))
                        Text(stringResource(R.string.beyond_blue_balance_pearls, pearlBalance))
                        Text(stringResource(R.string.beyond_blue_confirm_no_creatures_leave))
                        if (pearlBalance < pearlOnlyPrice) {
                            Text(stringResource(R.string.beyond_blue_need_more_pearls, pearlOnlyPrice - pearlBalance), color = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.beyond_blue_trade_or_return_after_flow), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                        }
                        Button(
                            onClick = { showConfirm = true },
                            enabled = if (selectedInstanceIds.isEmpty()) pearlBalance >= pearlOnlyPrice else quote.canEncounter,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(if (selectedInstanceIds.isEmpty()) R.string.beyond_blue_buy_with_pearls else R.string.beyond_blue_trade_and_buy))
                        }
                    }
                }

                if (tradeStacks.isNotEmpty()) {
                    Text(stringResource(R.string.beyond_blue_optional), fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { tradeExpanded = !tradeExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.beyond_blue_trade_reduce_optional))
                    }
                }

                if (tradeExpanded && tradeStacks.isNotEmpty()) {
                    Text(stringResource(R.string.beyond_blue_trade_from_blue), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.beyond_blue_calling_life_in), fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.beyond_blue_contribution_value, formatMinutesCompact(selectedMinutes), formatMinutesCompact(requirement)))
                    Column(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tradeStacks.forEach { stack ->
                            val selected = selectedCounts[stack.key] ?: 0
                            ElevatedCard {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ShellObjectIcon(CreatureCatalog.get(stack.findId)?.staticIconKey ?: "animal", Modifier.size(34.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(findName(stack.findId), fontWeight = FontWeight.Bold)
                                            Text(stringResource(R.string.shell_creature_level_short, stack.level))
                                        }
                                        ShellMetricPill(Icons.Outlined.Inventory2, stringResource(R.string.beyond_blue_owned_count, stack.instances.size))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                        ShellMetricPill(Icons.Outlined.Route, stringResource(R.string.beyond_blue_each_value, formatMinutesCompact(stack.perMinutes)))
                                        ShellMetricPill(Icons.Outlined.WaterDrop, stringResource(R.string.beyond_blue_selected_count, selected))
                                        ShellMetricPill(Icons.Outlined.Route, stringResource(R.string.beyond_blue_contributes_value, formatMinutesCompact(selected * stack.perMinutes)))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(onClick = { selectedCounts = selectedCounts + (stack.key to (selected - 1).coerceAtLeast(0)) }, enabled = selected > 0) { Text(stringResource(R.string.beyond_blue_remove_one, findName(stack.findId))) }
                                        OutlinedButton(onClick = { selectedCounts = selectedCounts + (stack.key to (selected + 1).coerceAtMost(stack.instances.size)) }, enabled = selected < stack.instances.size) { Text(stringResource(R.string.beyond_blue_add_one, findName(stack.findId))) }
                                    }
                                }
                            }
                        }
                    }
                }

                if (tradeExpanded || selectedInstanceIds.isNotEmpty()) {
                    ElevatedCard {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.beyond_blue_adjusted_cost), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.beyond_blue_life_selected, formatMinutesCompact(quote.selectedCreatureMinutes)))
                            Text(stringResource(R.string.beyond_blue_pearls_used, quote.pearlCostForRemaining))
                            if (quote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_payment_returned, quote.pearlReturnForOverpay))
                            if (selectedInstanceIds.isNotEmpty()) {
                                Button(
                                    onClick = { showConfirm = true },
                                    enabled = quote.canEncounter,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.beyond_blue_trade_and_buy))
                                }
                            }
                        }
                    }

                    ElevatedCard {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selectedInstanceIds.isEmpty()) {
                                Text(stringResource(R.string.beyond_blue_confirm_no_creatures_leave))
                                Text(stringResource(R.string.beyond_blue_pearls_call_life_in))
                            } else {
                                Text(stringResource(R.string.beyond_blue_selected_leave))
                                Text(stringResource(R.string.beyond_blue_lifetime_remains))
                                if (quote.pearlCostForRemaining > 0) Text(stringResource(R.string.beyond_blue_pearls_cover_rest))
                                if (quote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_extra_value_returns))
                            }
                        }
                    }

                    if (!quote.canEncounter) {
                        Text(stringResource(R.string.beyond_blue_need_more_pearls, (quote.pearlCostForRemaining - pearlBalance).coerceAtLeast(0)), color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.beyond_blue_trade_or_return_after_flow), color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedButton(onClick = { confirmTargetId = null }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.beyond_blue_back)) }

                if (showConfirm) {
                    AlertDialog(
                        onDismissRequest = { showConfirm = false },
                        confirmButton = {
                            Button(onClick = { onEncounter(target.creatureId, selectedInstanceIds) }) {
                                Text(stringResource(if (selectedInstanceIds.isEmpty()) R.string.beyond_blue_buy else R.string.beyond_blue_trade_and_buy))
                            }
                        },
                        dismissButton = { OutlinedButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.beyond_blue_cancel)) } },
                        title = { Text(stringResource(if (selectedInstanceIds.isEmpty()) R.string.beyond_blue_buy_with_pearls_title else R.string.beyond_blue_trade_and_buy_title, target.displayName)) },
                        text = {
                            Column {
                                if (selectedInstanceIds.isEmpty()) {
                                    Text(stringResource(R.string.beyond_blue_pearls_will_be_used, quote.pearlCostForRemaining))
                                    Text(stringResource(R.string.beyond_blue_confirm_no_creatures_leave))
                                } else {
                                    Text(stringResource(R.string.beyond_blue_selected_leave))
                                    if (quote.pearlCostForRemaining > 0) Text(stringResource(R.string.beyond_blue_confirm_pearls_cover_remaining, quote.pearlCostForRemaining))
                                    if (quote.pearlReturnForOverpay > 0) Text(stringResource(R.string.beyond_blue_confirm_returned, quote.pearlReturnForOverpay))
                                    Text(stringResource(R.string.beyond_blue_lifetime_remains))
                                }
                                Text(stringResource(R.string.beyond_blue_confirm_enters_zone, target.displayName, zoneTitle(theBlueZoneFor(target.zone))))
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TheBlueDepthRail(
    zones: List<TheBlueZoneId>,
    activeZone: TheBlueZoneId,
    onZoneClick: (TheBlueZoneId) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val railDescription = stringResource(R.string.the_blue_depth_rail_a11y)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.18f)),
        modifier = modifier
            .fillMaxHeight(0.48f)
            .width(58.dp)
            .semantics { contentDescription = railDescription }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            zones.forEach { zone ->
                val active = zone == activeZone
                val title = zoneTitle(zone)
                val goToDescription = stringResource(R.string.the_blue_depth_rail_go_to_zone_a11y, title)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button) { onZoneClick(zone) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .semantics {
                            contentDescription = goToDescription
                            role = Role.Button
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (active) scheme.secondary else scheme.primary.copy(alpha = 0.24f),
                        modifier = Modifier.size(if (active) 12.dp else 8.dp),
                        content = {}
                    )
                    Text(
                        text = zoneRailLabel(zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) scheme.secondary else scheme.onSurface.copy(alpha = 0.58f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 36.dp, max = 52.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun zoneTitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_title)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_title)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_title)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_title)
}

@Composable
private fun zoneRailLabel(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_rail)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_rail)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_rail)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_rail)
}

@Composable
private fun zoneSubtitle(zoneId: TheBlueZoneId): String = when (zoneId) {
    TheBlueZoneId.SUNLIT_REEF -> stringResource(R.string.the_blue_zone_sunlit_reef_subtitle)
    TheBlueZoneId.DEEPER_REEF -> stringResource(R.string.the_blue_zone_deeper_reef_subtitle)
    TheBlueZoneId.OPEN_BLUE -> stringResource(R.string.the_blue_zone_open_blue_subtitle)
    TheBlueZoneId.GREAT_BLUE -> stringResource(R.string.the_blue_zone_great_blue_subtitle)
}

@Composable
private fun findName(findId: String): String = ShellContentCatalog.find(findId)?.let { stringResource(it.titleRes) } ?: stringResource(R.string.reward_card_shell_recorded_title)

@Composable
private fun formName(findId: String, stageId: String?): String =
    ShellContentCatalog.upgradesFor(findId).firstOrNull { it.upgradeStageId == stageId }?.let { stringResource(it.titleRes) }
        ?: stringResource(R.string.shell_form_base)

@Composable
private fun theBlueSourceReason(findId: String): String = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.the_blue_source_minnow)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.the_blue_source_seahorse)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.the_blue_source_manta)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.the_blue_source_whale)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.the_blue_source_octopus)
    else -> ShellContentCatalog.find(findId)?.let { stringResource(it.descriptionRes) } ?: stringResource(R.string.reward_card_shell_recorded_body)
}

@Composable
private fun theBlueEncounteredReason(findId: String): String = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.the_blue_encountered_minnow)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.the_blue_encountered_seahorse)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.the_blue_encountered_manta)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.the_blue_encountered_whale)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.the_blue_source_octopus)
    else -> theBlueSourceReason(findId)
}

private fun firstOpenFocusSlotFor(findId: String, uiState: ShellUiState): String? {
    val definition = ShellContentCatalog.find(findId) ?: return null
    val occupied = uiState.focusPlacements.map { it.slotId }.toSet()
    return ShellContentCatalog.focusSlots.firstOrNull { slot ->
        slot.slotId !in occupied && ShellContentCatalog.isCompatibleWithSlot(slot, definition)
    }?.slotId
}

private fun firstRestingInstanceId(findId: String, uiState: ShellUiState): String? {
    val displayed = uiState.focusPlacements.map { it.instanceId }.toSet()
    return uiState.finds.firstOrNull { item ->
        item.findId == findId && item.instanceId !in displayed && canDisplayInstance(item, ShellContentCatalog.find(item.findId))
    }?.instanceId
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

private fun categoryLabelFor(category: ShellFindCategory): Int = when (category) {
    ShellFindCategory.CREATURES -> R.string.shell_category_creatures
    ShellFindCategory.SHELLS -> R.string.shell_category_shells
    ShellFindCategory.CORAL -> R.string.shell_category_coral
    ShellFindCategory.PLANTS -> R.string.shell_category_plants
    ShellFindCategory.TROPHIES -> R.string.shell_category_trophies
    ShellFindCategory.TRINKETS -> R.string.shell_category_trinkets
    ShellFindCategory.DISCOVERIES -> R.string.shell_category_discoveries
}

@Composable
private fun shellBackground(): Brush {
    val scheme = MaterialTheme.colorScheme

    return Brush.verticalGradient(
        colors = listOf(
            scheme.primary,
            scheme.background
        )
    )
}
