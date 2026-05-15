@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellFindCategory
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.data.model.shell.ShellSlotDefinition
import com.kingkharnivore.skillz.data.model.shell.StillwaterPerspective
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.ShellViewModel
import kotlin.math.pow

sealed class ShellDestination {
    data object Heart : ShellDestination()
    data object Focus : ShellDestination()
    data object Stillwater : ShellDestination()
    data object ShellChest : ShellDestination()
    data object Badges : ShellDestination()
    data object DiscoveryJournal : ShellDestination()
    data object Notifications : ShellDestination()
    data object VoyagePreview : ShellDestination()
    data object CoralReefPreview : ShellDestination()
    data object IdeaGrovePreview : ShellDestination()
    data object LookoutPreview : ShellDestination()
}

private fun displayedInstanceIds(uiState: ShellUiState): Set<String> =
    uiState.focusPlacements.map { it.instanceId }.toSet()

private fun restingFinds(uiState: ShellUiState): List<UserShellFindInstanceEntity> {
    val displayed = displayedInstanceIds(uiState)
    return uiState.finds.filter { it.instanceId !in displayed }
}

private fun hasRestingPlaceableFinds(uiState: ShellUiState): Boolean = restingFinds(uiState).any { item ->
    val def = ShellContentCatalog.find(item.findId)
    def?.placeable == true
}

private fun hasAffordablePearlShape(uiState: ShellUiState): Boolean =
    uiState.finds.any { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        val next = ShellContentCatalog.nextUpgrade(def.findId, item.currentUpgradeStageId) ?: return@any false
        next.pearlCost <= uiState.pearlBalance
    } || ShellContentCatalog.focusPearlObjects.any { (it.pearlCost ?: Int.MAX_VALUE) <= uiState.pearlBalance }

private fun currentFormOrder(instance: UserShellFindInstanceEntity): Int =
    ShellContentCatalog.upgradesFor(instance.findId)
        .firstOrNull { it.upgradeStageId == instance.currentUpgradeStageId }
        ?.orderIndex ?: 0

private fun unseenNotificationCount(uiState: ShellUiState): Int =
    uiState.finds.count { it.isNew } +
            uiState.stacks.count { it.isNew } +
            uiState.badges.count { it.isNew } +
            uiState.discoveries.count { it.isNew }

internal fun hasAffordableFocusPearlAction(uiState: ShellUiState): Boolean {
    val displayed = displayedInstanceIds(uiState)

    val hasAffordableDisplayedUpgrade = uiState.finds.any { item ->
        if (item.instanceId !in displayed) return@any false

        val def = ShellContentCatalog.find(item.findId) ?: return@any false
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

private fun hasEmptyNookForNewRestingObject(uiState: ShellUiState): Boolean {
    val occupied = uiState.focusPlacements.map { it.slotId }.toSet()
    val emptySlots = ShellContentCatalog.focusSlots.filter { it.slotId !in occupied }
    if (emptySlots.isEmpty()) return false
    return restingFinds(uiState).any { item ->
        val def = ShellContentCatalog.find(item.findId) ?: return@any false
        item.isNew && def.placeable && emptySlots.any { slot -> ShellContentCatalog.isCompatibleWithSlot(slot, def) }
    }
}

@Composable
private fun kindLabel(kind: ShellRewardKind): String = when (kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_kind_animal)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_kind_object)
    ShellRewardKind.TRINKET -> stringResource(R.string.shell_kind_trinket)
    ShellRewardKind.DISCOVERY -> stringResource(R.string.shell_kind_discovery)
}

@Composable
private fun depthLabel(depth: ShellDepthTier?): String? = when (depth) {
    ShellDepthTier.REEF -> stringResource(R.string.shell_depth_reef)
    ShellDepthTier.DEEPER_REEF -> stringResource(R.string.shell_depth_deeper_reef)
    ShellDepthTier.OPEN_BLUE -> stringResource(R.string.shell_depth_open_blue)
    ShellDepthTier.DEEP_OCEAN -> stringResource(R.string.shell_depth_deep_ocean)
    null -> null
}

@Composable
private fun sourceReasonFor(def: ShellFindDefinition): String = when (def.findId) {
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
private fun notificationTitleFor(def: ShellFindDefinition): String {
    val title = stringResource(def.titleRes)
    return when (def.kind) {
        ShellRewardKind.ANIMAL -> stringResource(R.string.shell_notification_title_encountered, title)
        ShellRewardKind.OBJECT -> if (def.isPearlObject) stringResource(R.string.shell_notification_title_invited, title) else stringResource(R.string.shell_notification_title_found, title)
        ShellRewardKind.TRINKET -> title
        ShellRewardKind.DISCOVERY -> title
    }
}

@Composable
private fun notificationBodyFor(def: ShellFindDefinition): String {
    val depth = depthLabel(def.depthTier)
    val reason = sourceReasonFor(def)
    return if (depth != null) stringResource(R.string.shell_notification_depth_body, depth, reason) else reason
}

@Composable
private fun returnToChestLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_let_rest_in_chest)
    else -> stringResource(R.string.shell_return_to_chest)
}

@Composable
private fun placeInFocusLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_display_in_focus)
    else -> stringResource(R.string.shell_place_in_focus)
}

@Composable
private fun restingCurrentFormLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_animal_no_more_forms)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_object_no_more_forms)
    else -> stringResource(R.string.shell_reward_no_more_forms)
}

@Composable
private fun upgradeA11yLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_upgrade_animal_a11y)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_upgrade_object_a11y)
    else -> stringResource(R.string.shell_upgrade_reward_a11y)
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
            ShellDestination.CoralReefPreview -> ShellRoomId.CORAL_REEF
            ShellDestination.IdeaGrovePreview -> ShellRoomId.IDEA_GROVE
            ShellDestination.LookoutPreview -> ShellRoomId.LOOKOUT
            else -> null
        }

        room?.let(viewModel::markRoomOpened)
        if (destination == ShellDestination.Notifications) {
            shouldMarkNotificationsSeenOnExit = true
        } else if (shouldMarkNotificationsSeenOnExit) {
            viewModel.markNotificationsSeen()
            shouldMarkNotificationsSeenOnExit = false
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

                ShellDestination.CoralReefPreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_coral_title,
                    bodyRes = R.string.shell_preview_coral,
                    icon = Icons.Outlined.FilterVintage
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
private fun ShellTopBar(
    destination: ShellDestination,
    pearlBalance: Int,
    pearlBasinHasIndicator: Boolean,
    notificationCount: Int,
    onBack: () -> Unit,
    onPearls: () -> Unit,
    onNotifications: () -> Unit,
    onChest: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val title = when (destination) {
        ShellDestination.Heart -> stringResource(R.string.shell_title)
        ShellDestination.Focus -> stringResource(R.string.shell_room_focus_title)
        ShellDestination.Stillwater -> stringResource(R.string.shell_room_stillwater_title)
        ShellDestination.ShellChest -> stringResource(R.string.shell_chest_title)
        ShellDestination.Badges -> stringResource(R.string.shell_badges_title)
        ShellDestination.DiscoveryJournal -> stringResource(R.string.shell_journal_title)
        ShellDestination.Notifications -> stringResource(R.string.shell_notifications_title)
        ShellDestination.VoyagePreview -> stringResource(R.string.shell_room_voyage_title)
        ShellDestination.CoralReefPreview -> stringResource(R.string.shell_room_coral_title)
        ShellDestination.IdeaGrovePreview -> stringResource(R.string.shell_room_idea_title)
        ShellDestination.LookoutPreview -> stringResource(R.string.shell_room_lookout_title)
    }

    val pearlBalanceDescription = stringResource(R.string.shell_pearl_basin_chip_a11y, pearlBalance)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            navigationIconContentColor = scheme.primary,
            actionIconContentColor = scheme.primary
        ),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.shell_back_a11y)
                )
            }
        },
        actions = {
            AssistChip(
                onClick = onPearls,
                label = {
                    Text(stringResource(R.string.shell_pearl_balance, pearlBalance))
                },
                leadingIcon = {
                    ShellPearlMiniIcon(Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = scheme.surface,
                    labelColor = scheme.onSurface,
                    leadingIconContentColor = scheme.primary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (pearlBasinHasIndicator) shellIndicatorColor() else scheme.secondary.copy(alpha = 0.45f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = pearlBalanceDescription
                    role = Role.Button
                }
            )

            IconButton(onClick = onNotifications) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.shell_notifications_a11y, notificationCount)
                    )
                    if (notificationCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = notificationCount.coerceAtMost(9).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onChest) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = stringResource(R.string.shell_chest_a11y)
                )
            }
        }
    )
}

@Composable
private fun HeartRoomScreen(
    uiState: ShellUiState,
    onNavigate: (ShellDestination) -> Unit,
    onOpenPearlBasin: () -> Unit
) {
    var showHeartDetail by remember { mutableStateOf(false) }
    val chestHasIndicator = restingFinds(uiState).any { it.isNew } || uiState.stacks.any { it.isNew }
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
                    labelRes = R.string.shell_room_coral_title,
                    icon = Icons.Outlined.FilterVintage,
                    dormant = true,
                    nodeWidth = nodeWidth,
                    nodeHeight = nodeHeight,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight * 0.80f),
                    onClick = { onNavigate(ShellDestination.CoralReefPreview) }
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

                if (dormant) {
                    Text(
                        text = stringResource(R.string.shell_quiet_for_now),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.secondary,
                        maxLines = 1
                    )
                }
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
    val totalFinds = uiState.finds.size + uiState.stacks.sumOf { it.quantity }

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
private fun shellIndicatorColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val secondaryContrast = contrastRatio(scheme.secondary, scheme.surface)
    return if (secondaryContrast >= 3f) scheme.secondary else scheme.primary
}

private fun contrastRatio(a: Color, b: Color): Float {
    fun channel(v: Float): Float = if (v <= 0.03928f) {
        v / 12.92f
    } else {
        ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    fun luminance(color: Color): Float {
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    val l1 = luminance(a)
    val l2 = luminance(b)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

@Composable
private fun ShellPearlMiniIcon(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        drawCircle(
            color = scheme.onPrimary,
            radius = size.minDimension * 0.36f,
            center = Offset(size.width / 2f, size.height / 2f)
        )

        drawCircle(
            color = scheme.primary.copy(alpha = 0.42f),
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.60f, size.height * 0.38f)
        )
    }
}

private enum class ShellAnimalIcon { MINNOW, SEAHORSE, MANTA, WHALE, OCTOPUS }

@Composable
private fun ShellObjectIcon(
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val animalIcon = when {
        "minnow" in iconKey -> ShellAnimalIcon.MINNOW
        "seahorse" in iconKey -> ShellAnimalIcon.SEAHORSE
        "manta" in iconKey -> ShellAnimalIcon.MANTA
        "whale" in iconKey -> ShellAnimalIcon.WHALE
        "octopus" in iconKey -> ShellAnimalIcon.OCTOPUS
        else -> null
    }
    val vector = when {
        animalIcon != null -> null
        "kelp" in iconKey || "curtain" in iconKey -> Icons.Outlined.Grass
        "bubble" in iconKey || "current" in iconKey -> Icons.Outlined.Waves
        "coral" in iconKey || "perch" in iconKey -> Icons.Outlined.FilterVintage
        else -> Icons.Outlined.Diamond
    }
    Surface(shape = CircleShape, color = scheme.primary.copy(alpha = 0.16f), modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            if (animalIcon != null) {
                ShellAnimalCanvasIcon(animalIcon, Modifier.fillMaxSize().padding(5.dp))
            } else if (vector != null) {
                Icon(imageVector = vector, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ShellAnimalCanvasIcon(
    animalIcon: ShellAnimalIcon,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ink = scheme.primary
        val accent = scheme.primary.copy(alpha = 0.55f)

        when (animalIcon) {
            ShellAnimalIcon.MINNOW -> {
                drawOval(ink, topLeft = Offset(w * 0.22f, h * 0.34f), size = Size(w * 0.44f, h * 0.30f))
                drawPath(Path().apply {
                    moveTo(w * 0.20f, h * 0.50f)
                    lineTo(w * 0.02f, h * 0.34f)
                    lineTo(w * 0.02f, h * 0.66f)
                    close()
                }, ink)
                drawCircle(scheme.surface, radius = w * 0.035f, center = Offset(w * 0.58f, h * 0.44f))
                drawOval(accent, topLeft = Offset(w * 0.66f, h * 0.24f), size = Size(w * 0.18f, h * 0.12f))
                drawOval(accent, topLeft = Offset(w * 0.72f, h * 0.62f), size = Size(w * 0.20f, h * 0.12f))
            }
            ShellAnimalIcon.SEAHORSE -> {
                drawCircle(ink, radius = w * 0.17f, center = Offset(w * 0.56f, h * 0.24f))
                drawLine(ink, Offset(w * 0.64f, h * 0.25f), Offset(w * 0.86f, h * 0.20f), strokeWidth = w * 0.09f)
                drawPath(Path().apply {
                    moveTo(w * 0.54f, h * 0.36f)
                    cubicTo(w * 0.30f, h * 0.44f, w * 0.38f, h * 0.78f, w * 0.58f, h * 0.70f)
                    cubicTo(w * 0.78f, h * 0.62f, w * 0.68f, h * 0.48f, w * 0.54f, h * 0.56f)
                }, ink, style = Stroke(width = w * 0.13f))
                drawCircle(scheme.surface, radius = w * 0.032f, center = Offset(w * 0.61f, h * 0.20f))
                drawLine(accent, Offset(w * 0.37f, h * 0.48f), Offset(w * 0.18f, h * 0.40f), strokeWidth = w * 0.08f)
            }
            ShellAnimalIcon.MANTA -> {
                drawPath(Path().apply {
                    moveTo(w * 0.50f, h * 0.22f)
                    cubicTo(w * 0.20f, h * 0.30f, w * 0.08f, h * 0.58f, w * 0.02f, h * 0.74f)
                    cubicTo(w * 0.28f, h * 0.66f, w * 0.38f, h * 0.62f, w * 0.50f, h * 0.78f)
                    cubicTo(w * 0.62f, h * 0.62f, w * 0.72f, h * 0.66f, w * 0.98f, h * 0.74f)
                    cubicTo(w * 0.92f, h * 0.58f, w * 0.80f, h * 0.30f, w * 0.50f, h * 0.22f)
                    close()
                }, ink)
                drawLine(accent, Offset(w * 0.50f, h * 0.72f), Offset(w * 0.50f, h * 0.96f), strokeWidth = w * 0.05f)
            }
            ShellAnimalIcon.WHALE -> {
                drawOval(ink, topLeft = Offset(w * 0.12f, h * 0.34f), size = Size(w * 0.68f, h * 0.34f))
                drawPath(Path().apply {
                    moveTo(w * 0.78f, h * 0.50f)
                    lineTo(w * 0.98f, h * 0.30f)
                    lineTo(w * 0.92f, h * 0.50f)
                    lineTo(w * 0.98f, h * 0.70f)
                    close()
                }, ink)
                drawCircle(scheme.surface, radius = w * 0.03f, center = Offset(w * 0.24f, h * 0.44f))
                drawLine(accent, Offset(w * 0.36f, h * 0.34f), Offset(w * 0.44f, h * 0.18f), strokeWidth = w * 0.05f)
                drawLine(accent, Offset(w * 0.44f, h * 0.18f), Offset(w * 0.54f, h * 0.34f), strokeWidth = w * 0.05f)
            }
            ShellAnimalIcon.OCTOPUS -> {
                drawOval(ink, topLeft = Offset(w * 0.26f, h * 0.14f), size = Size(w * 0.48f, h * 0.42f))
                listOf(0.20f, 0.36f, 0.52f, 0.68f).forEach { x ->
                    drawLine(ink, Offset(w * (x + 0.06f), h * 0.52f), Offset(w * x, h * 0.86f), strokeWidth = w * 0.08f)
                }
                drawCircle(scheme.surface, radius = w * 0.035f, center = Offset(w * 0.42f, h * 0.32f))
                drawCircle(scheme.surface, radius = w * 0.035f, center = Offset(w * 0.58f, h * 0.32f))
            }
        }
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
private fun FocusRoomScreen(
    uiState: ShellUiState,
    onPlace: (String, String) -> Unit,
    onReturn: (String) -> Unit,
    onInvite: (String, String) -> Unit,
    onUpgrade: (String) -> Unit
) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var selectedInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }
    val placementsBySlot = uiState.focusPlacements.associateBy { it.slotId }
    val findsById = uiState.finds.associateBy { it.instanceId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoomHeader(
            title = R.string.shell_room_focus_title,
            body = R.string.shell_focus_body
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(shellChamberBrush())
        ) {
            TurtleShellInteriorBackground(
                modifier = Modifier.matchParentSize(),
                centerGlow = false
            )

            FocusRoomCarvedShelves(
                modifier = Modifier.matchParentSize()
            )

            ShellContentCatalog.focusSlots.sortedBy { it.zIndex }.forEach { slot ->
                val placement = placementsBySlot[slot.slotId]
                val find = placement?.let { findsById[it.instanceId] }

                SlotChip(
                    slot = slot,
                    find = find,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (maxWidth * slot.anchorX) - ((maxWidth * slot.widthFraction) / 2),
                            y = (maxHeight * slot.anchorY) - ((maxHeight * slot.heightFraction) / 2)
                        )
                        .size(
                            width = maxWidth * slot.widthFraction,
                            height = maxHeight * slot.heightFraction
                        ),
                    onEmpty = { selectedSlot = slot.slotId },
                    onFind = {
                        if (find != null) {
                            selectedInstance = find
                        }
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f))
        ) {
            val restingCount = restingFinds(uiState).size
            val allNooksHolding = uiState.focusPlacements.size >= ShellContentCatalog.focusSlots.size
            Text(
                text = if (allNooksHolding) {
                    stringResource(R.string.shell_all_nooks_holding, restingCount)
                } else {
                    stringResource(R.string.shell_focus_summary, uiState.focusPlacements.size, restingCount)
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }

    if (selectedSlot != null) {
        EmptySlotSheet(
            slotId = selectedSlot!!,
            uiState = uiState,
            onDismiss = { selectedSlot = null },
            onPlace = { id ->
                onPlace(id, selectedSlot!!)
                selectedSlot = null
            },
            onInvite = { findId ->
                onInvite(findId, selectedSlot!!)
                selectedSlot = null
            }
        )
    }

    if (selectedInstance != null) {
        ObjectCopySheet(
            item = selectedInstance!!,
            pearlBalance = uiState.pearlBalance,
            displayed = true,
            onDismiss = { selectedInstance = null },
            onReturn = {
                onReturn(it)
                selectedInstance = null
            },
            onUpgrade = {
                onUpgrade(it)
                selectedInstance = null
            },
            onPlaceInFocus = null
        )
    }
}

@Composable
private fun TurtleShellInteriorBackground(
    modifier: Modifier = Modifier,
    centerGlow: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.primary.copy(alpha = 0.24f),
            topLeft = Offset(-w * 0.12f, h * 0.02f),
            size = Size(w * 1.24f, h * 1.10f)
        )

        drawOval(
            color = scheme.surface.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.04f, h * 0.07f),
            size = Size(w * 0.92f, h * 0.86f)
        )

        drawOval(
            color = scheme.secondary.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.13f, h * 0.12f),
            size = Size(w * 0.74f, h * 0.68f)
        )

        if (centerGlow) {
            drawCircle(
                color = scheme.primary.copy(alpha = 0.18f),
                radius = w * 0.25f,
                center = Offset(w * 0.50f, h * 0.35f)
            )
        }

        val spine = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(
                w * 0.46f,
                h * 0.28f,
                w * 0.54f,
                h * 0.48f,
                w * 0.50f,
                h * 0.80f
            )
        }

        drawPath(
            path = spine,
            color = scheme.secondary.copy(alpha = 0.22f),
            style = Stroke(width = 4.5f)
        )

        val bandYs = listOf(0.20f, 0.34f, 0.49f, 0.64f, 0.78f)
        bandYs.forEachIndexed { index, yFraction ->
            val y = h * yFraction
            val leftInset = w * (0.13f + index * 0.018f)
            val rightInset = w - leftInset

            val band = Path().apply {
                moveTo(leftInset, y)
                cubicTo(
                    w * 0.30f,
                    y - h * 0.060f,
                    w * 0.42f,
                    y + h * 0.035f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.58f,
                    y + h * 0.035f,
                    w * 0.70f,
                    y - h * 0.060f,
                    rightInset,
                    y
                )
            }

            drawPath(
                path = band,
                color = scheme.secondary.copy(alpha = 0.12f),
                style = Stroke(width = 3f)
            )
        }

        drawOval(
            color = Color.Black.copy(alpha = 0.12f),
            topLeft = Offset(-w * 0.08f, h * 0.02f),
            size = Size(w * 1.16f, h * 1.04f),
            style = Stroke(width = w * 0.08f)
        )
    }
}

@Composable
private fun FocusRoomCarvedShelves(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val shelfColor = scheme.secondary.copy(alpha = 0.22f)
        val shelfShadow = Color.Black.copy(alpha = 0.20f)

        listOf(0.30f, 0.52f, 0.74f).forEach { yFraction ->
            val y = h * yFraction

            val shadow = Path().apply {
                moveTo(w * 0.16f, y + 6f)
                cubicTo(
                    w * 0.32f,
                    y + h * 0.03f,
                    w * 0.68f,
                    y + h * 0.03f,
                    w * 0.84f,
                    y + 6f
                )
            }

            drawPath(
                path = shadow,
                color = shelfShadow,
                style = Stroke(width = 8f)
            )

            val shelf = Path().apply {
                moveTo(w * 0.16f, y)
                cubicTo(
                    w * 0.32f,
                    y - h * 0.025f,
                    w * 0.68f,
                    y - h * 0.025f,
                    w * 0.84f,
                    y
                )
            }

            drawPath(
                path = shelf,
                color = shelfColor,
                style = Stroke(width = 4f)
            )
        }
    }
}

@Composable
private fun SlotChip(
    slot: ShellSlotDefinition,
    find: UserShellFindInstanceEntity?,
    modifier: Modifier,
    onEmpty: () -> Unit,
    onFind: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val def = find?.let { ShellContentCatalog.find(it.findId) }

    val slotTitle = stringResource(slot.titleRes)
    val label = if (def != null) {
        stringResource(def.titleRes)
    } else {
        slotTitle
    }
    val placedDescription = if (def != null) {
        stringResource(R.string.shell_placed_object_a11y, label)
    } else {
        stringResource(R.string.shell_empty_slot_a11y, slotTitle)
    }

    val isFilled = def != null

    Surface(
        modifier = modifier
            .semantics { contentDescription = placedDescription }
            .clickable {
                if (find == null) {
                    onEmpty()
                } else {
                    onFind()
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = if (isFilled) {
            scheme.surface.copy(alpha = 0.96f)
        } else {
            scheme.primary.copy(alpha = 0.40f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isFilled) {
                scheme.secondary.copy(alpha = 0.68f)
            } else {
                scheme.secondary.copy(alpha = 0.30f)
            }
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (def != null) {
                    ShellObjectIcon(def.iconKey, Modifier.size(28.dp))
                }
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isFilled) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isFilled) {
                        scheme.onSurface
                    } else {
                        scheme.onPrimary.copy(alpha = 0.82f)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySlotSheet(
    slotId: String,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onPlace: (String) -> Unit,
    onInvite: (String) -> Unit
) {
    val slot = ShellContentCatalog.focusSlots.first { it.slotId == slotId }
    val placedIds = uiState.focusPlacements.map { it.instanceId }.toSet()

    val compatible = uiState.finds.filter { instance ->
        val def = ShellContentCatalog.find(instance.findId)

        def != null &&
                instance.instanceId !in placedIds &&
                def.placeable &&
                ShellContentCatalog.isCompatibleWithSlot(slot, def)
    }

    val invitable = ShellContentCatalog.focusPearlObjects.filter { def ->
        ShellContentCatalog.isCompatibleWithSlot(slot, def)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_empty_slot_title),
                style = MaterialTheme.typography.titleLarge
            )

            Text(stringResource(R.string.shell_slot_named_title, stringResource(slot.titleRes)))
            if (slot.slotType == ShellSlotType.SURGE_CURRENT) {
                Text(stringResource(R.string.shell_surge_current_reserved_body))
            } else {
                Text(stringResource(R.string.shell_empty_nook_choices))
            }

            Text(
                text = stringResource(R.string.shell_place_from_chest),
                fontWeight = FontWeight.SemiBold
            )

            compatible.forEach { item ->
                val def = ShellContentCatalog.find(item.findId) ?: return@forEach

                ListItem(
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = { Text(stringResource(R.string.shell_place_free)) },
                    modifier = Modifier.clickable { onPlace(item.instanceId) }
                )
            }

            if (compatible.isEmpty()) {
                Text(stringResource(R.string.shell_no_resting_finds_fit))
            }

            if (invitable.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.shell_shape_space_with_pearls),
                    fontWeight = FontWeight.SemiBold
                )
            }

            invitable.forEach { def ->
                val cost = def.pearlCost ?: return@forEach
                val canAfford = uiState.pearlBalance >= cost
                ListItem(
                    leadingContent = { ShellObjectIcon(def.iconKey, Modifier.size(30.dp)) },
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = {
                        Text(
                            if (canAfford) stringResource(R.string.shell_invite_with_pearls, cost)
                            else stringResource(R.string.shell_need_more_pearls, cost - uiState.pearlBalance)
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = canAfford) { onInvite(def.findId) }
                        .semantics { role = Role.Button }
                )
            }

            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.shell_leave_empty))
            }
        }
    }
}

@Composable
private fun ObjectCopySheet(
    item: UserShellFindInstanceEntity,
    pearlBalance: Int,
    displayed: Boolean,
    onDismiss: () -> Unit,
    onReturn: (String) -> Unit,
    onUpgrade: (String) -> Unit,
    onPlaceInFocus: (() -> Unit)?
) {
    val def = ShellContentCatalog.find(item.findId)
    val current = def?.let {
        ShellContentCatalog.upgradesFor(it.findId)
            .firstOrNull { stage -> stage.upgradeStageId == item.currentUpgradeStageId }
    }
    val next = def?.let {
        ShellContentCatalog.nextUpgrade(it.findId, item.currentUpgradeStageId)
    }

    val findTitle = if (def != null) {
        stringResource(def.titleRes)
    } else {
        item.findId
    }

    val currentTitle = if (current != null) {
        stringResource(current.titleRes)
    } else {
        findTitle
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = findTitle,
                style = MaterialTheme.typography.titleLarge
            )

            def?.let { Text(kindLabel(it.kind)) }
            Text(
                if (displayed) stringResource(R.string.shell_status_displayed_focus)
                else stringResource(R.string.shell_status_resting)
            )
            Text(stringResource(R.string.shell_form_label, currentTitle))
            def?.let { Text(stringResource(R.string.shell_source_label, sourceReasonFor(it))) }

            if (next != null) {
                val nextTitle = stringResource(next.titleRes)
                val upgradeVerb = stringResource(next.upgradeVerbRes)
                val upgradeDescription = upgradeA11yLabel(def)
                val canAfford = pearlBalance >= next.pearlCost

                Text(stringResource(R.string.shell_next_form, nextTitle))
                if (!canAfford) {
                    Text(stringResource(R.string.shell_need_more_pearls, next.pearlCost - pearlBalance))
                }

                Button(
                    onClick = { onUpgrade(item.instanceId) },
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = upgradeDescription
                    }
                ) {
                    Text(
                        stringResource(
                            R.string.shell_upgrade_with_pearls,
                            upgradeVerb,
                            next.pearlCost
                        )
                    )
                }
            } else {
                Text(restingCurrentFormLabel(def))
            }

            if (displayed) {
                OutlinedButton(onClick = { onReturn(item.instanceId) }) {
                    Text(returnToChestLabel(def))
                }
            } else if (onPlaceInFocus != null) {
                OutlinedButton(onClick = onPlaceInFocus) {
                    Text(placeInFocusLabel(def))
                }
            }
        }
    }
}

@Composable
private fun ShellChestScreen(
    uiState: ShellUiState,
    onPlace: (String, String) -> Unit,
    onReturn: (String) -> Unit,
    onUpgrade: (String) -> Unit,
    onOpenFocus: () -> Unit
) {
    var category by remember { mutableStateOf<ShellFindCategory?>(null) }
    var selectedGroupFindId by remember { mutableStateOf<String?>(null) }
    var selectedInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }
    var placingInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }

    val displayedIds = displayedInstanceIds(uiState)
    val groupedItems = uiState.finds
        .filter { category == null || ShellContentCatalog.find(it.findId)?.category == category }
        .groupBy { it.findId }
        .toList()
        .sortedBy { (findId, _) -> ShellContentCatalog.find(findId)?.titleRes ?: 0 }
    val stackItems = uiState.stacks.filter {
        category == null || ShellContentCatalog.find(it.findId)?.category == category
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_chest_title,
                body = R.string.shell_chest_body
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text(stringResource(R.string.shell_filter_all)) }
                )

                ShellFindCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(stringResource(categoryLabelFor(cat))) }
                    )
                }
            }
        }

        items(groupedItems) { (findId, copies) ->
            val def = ShellContentCatalog.find(findId) ?: return@items
            val title = stringResource(def.titleRes)
            val categoryLabel = kindLabel(def.kind) + (depthLabel(def.depthTier)?.let { " · $it" } ?: "")
            val displayedCount = copies.count { it.instanceId in displayedIds }
            val restingCount = copies.size - displayedCount
            val bestCopy = copies.maxByOrNull { currentFormOrder(it) }
            val bestFormTitle = bestCopy?.let { copy ->
                ShellContentCatalog.upgradesFor(copy.findId)
                    .firstOrNull { it.upgradeStageId == copy.currentUpgradeStageId }
                    ?.let { stringResource(it.titleRes) }
            } ?: title
            val rowDescription = stringResource(
                R.string.shell_chest_group_a11y,
                title,
                copies.size,
                displayedCount,
                restingCount
            )

            ElevatedCard(
                onClick = { selectedGroupFindId = findId },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    contentDescription = rowDescription
                    role = Role.Button
                }
            ) {
                ListItem(
                    leadingContent = {
                        ShellObjectIcon(def.iconKey, Modifier.size(36.dp))
                    },
                    headlineContent = {
                        Text(stringResource(R.string.shell_chest_group_title, title, copies.size))
                    },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.shell_chest_group_status,
                                bestFormTitle,
                                displayedCount,
                                restingCount,
                                categoryLabel
                            ) + "\n" + sourceReasonFor(def)
                        )
                    }
                )
            }
        }

        items(stackItems) { stack ->
            val def = ShellContentCatalog.find(stack.findId) ?: return@items
            val title = stringResource(def.titleRes)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                ListItem(
                    leadingContent = { ShellObjectIcon(def.iconKey, Modifier.size(36.dp)) },
                    headlineContent = { Text(stringResource(R.string.shell_chest_group_title, title, stack.quantity)) },
                    supportingContent = {
                        Text("${kindLabel(def.kind)} · ${stringResource(R.string.shell_stack_quantity, stack.quantity)}\n${sourceReasonFor(def)}")
                    }
                )
            }
        }
    }

    selectedGroupFindId?.let { findId ->
        CopyGroupSheet(
            findId = findId,
            uiState = uiState,
            onDismiss = { selectedGroupFindId = null },
            onSelectCopy = { instance ->
                selectedGroupFindId = null
                selectedInstance = instance
            }
        )
    }

    selectedInstance?.let { instance ->
        val isDisplayed = uiState.focusPlacements.any { it.instanceId == instance.instanceId }
        ObjectCopySheet(
            item = instance,
            pearlBalance = uiState.pearlBalance,
            displayed = isDisplayed,
            onDismiss = { selectedInstance = null },
            onReturn = {
                onReturn(it)
                selectedInstance = null
            },
            onUpgrade = {
                onUpgrade(it)
                selectedInstance = null
            },
            onPlaceInFocus = if (isDisplayed) {
                null
            } else {
                {
                    placingInstance = instance
                    selectedInstance = null
                }
            }
        )
    }

    placingInstance?.let { instance ->
        ChestPlacementSheet(
            instance = instance,
            uiState = uiState,
            onDismiss = { placingInstance = null },
            onPlace = { slotId ->
                onPlace(instance.instanceId, slotId)
                placingInstance = null
                onOpenFocus()
            },
            onOpenFocus = {
                placingInstance = null
                onOpenFocus()
            }
        )
    }
}

@Composable
private fun CopyGroupSheet(
    findId: String,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onSelectCopy: (UserShellFindInstanceEntity) -> Unit
) {
    val def = ShellContentCatalog.find(findId)
    val copies = uiState.finds.filter { it.findId == findId }.sortedWith(
        compareByDescending<UserShellFindInstanceEntity> { currentFormOrder(it) }.thenByDescending { it.acquiredAt }
    )
    val displayedIds = displayedInstanceIds(uiState)
    val title = def?.let { stringResource(it.titleRes) } ?: findId

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_chest_group_title, title, copies.size),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            copies.forEach { copy ->
                val formTitle = ShellContentCatalog.upgradesFor(copy.findId)
                    .firstOrNull { it.upgradeStageId == copy.currentUpgradeStageId }
                    ?.let { stringResource(it.titleRes) } ?: title
                val status = if (copy.instanceId in displayedIds) {
                    stringResource(R.string.shell_status_displayed_focus)
                } else {
                    stringResource(R.string.shell_status_resting)
                }

                ListItem(
                    leadingContent = { ShellObjectIcon(def?.iconKey ?: "shell", Modifier.size(30.dp)) },
                    headlineContent = { Text(formTitle) },
                    supportingContent = { Text(status) },
                    modifier = Modifier
                        .clickable { onSelectCopy(copy) }
                        .semantics { role = Role.Button }
                )
            }
        }
    }
}

@Composable
private fun ChestPlacementSheet(
    instance: UserShellFindInstanceEntity,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onPlace: (String) -> Unit,
    onOpenFocus: () -> Unit
) {
    val def = ShellContentCatalog.find(instance.findId)
    val placementsBySlot = uiState.focusPlacements.associateBy { it.slotId }
    val findsById = uiState.finds.associateBy { it.instanceId }

    val slots = if (def == null) {
        emptyList()
    } else {
        ShellContentCatalog.focusSlots.filter { slot ->
            ShellContentCatalog.isCompatibleWithSlot(slot, def)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = def?.let { stringResource(it.titleRes) } ?: instance.findId,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = if (slots.any { placementsBySlot[it.slotId] != null }) {
                    stringResource(R.string.shell_choose_something_to_swap)
                } else {
                    stringResource(R.string.shell_compatible_slots)
                }
            )

            slots.forEach { slot ->
                val slotTitle = stringResource(slot.titleRes)
                val displayedCopy = placementsBySlot[slot.slotId]?.let { placement -> findsById[placement.instanceId] }
                val displayedDef = displayedCopy?.let { ShellContentCatalog.find(it.findId) }
                ListItem(
                    headlineContent = { Text(slotTitle) },
                    supportingContent = {
                        Text(
                            displayedDef?.let { stringResource(R.string.shell_swap_with, stringResource(it.titleRes)) }
                                ?: stringResource(R.string.shell_place_free)
                        )
                    },
                    modifier = Modifier
                        .clickable { onPlace(slot.slotId) }
                        .semantics {
                            contentDescription = slotTitle
                            role = Role.Button
                        }
                )
            }

            if (slots.isEmpty()) {
                Text(stringResource(R.string.shell_no_compatible_nooks))
            }

            OutlinedButton(onClick = onOpenFocus) {
                Text(stringResource(R.string.shell_room_focus_title))
            }
        }
    }
}

@Composable
private fun BadgesScreen(uiState: ShellUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_badges_title,
                body = R.string.shell_badges_body
            )
        }

        items(uiState.badges) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            val title = stringResource(def.titleRes)
            val badgeDescription = stringResource(R.string.shell_badge_a11y, title, badge.count)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    contentDescription = badgeDescription
                }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.shell_badge_row_title, title, badge.count)) },
                    supportingContent = {
                        Text(stringResource(def.descriptionRes))
                    }
                )
            }
        }
    }
}

@Composable
private fun DiscoveryJournalScreen(uiState: ShellUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_journal_title,
                body = R.string.shell_journal_body
            )
        }

        items(uiState.discoveries) { discovery ->
            val def = ShellContentCatalog.discovery(discovery.discoveryId) ?: return@items
            val title = stringResource(def.titleRes)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    contentDescription = title
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(stringResource(def.explanationRes))
                }
            }
        }
    }
}

@Composable
private fun ShellNotificationsScreen(uiState: ShellUiState) {
    val newFinds = uiState.finds.filter { it.isNew }
    val newStacks = uiState.stacks.filter { it.isNew }
    val newBadges = uiState.badges.filter { it.isNew }
    val newDiscoveries = uiState.discoveries.filter { it.isNew }
    val hasNotifications = newFinds.isNotEmpty() || newStacks.isNotEmpty() || newBadges.isNotEmpty() || newDiscoveries.isNotEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_notifications_title,
                body = R.string.shell_notifications_body
            )
        }

        if (!hasNotifications) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.shell_notifications_empty_title)) },
                        supportingContent = { Text(stringResource(R.string.shell_notifications_empty_body)) }
                    )
                }
            }
        }

        items(newFinds, key = { it.instanceId }) { find ->
            val def = ShellContentCatalog.find(find.findId) ?: return@items
            ShellNotificationCard(
                icon = iconFor(def.category),
                title = notificationTitleFor(def),
                body = notificationBodyFor(def)
            )
        }

        items(newStacks, key = { it.findId }) { stack ->
            val def = ShellContentCatalog.find(stack.findId) ?: return@items
            val title = stringResource(def.titleRes)
            ShellNotificationCard(
                icon = iconFor(def.category),
                title = stringResource(R.string.shell_chest_group_title, title, stack.quantity),
                body = stringResource(R.string.shell_notification_stack_body, stack.quantity)
            )
        }

        items(newBadges, key = { it.badgeId }) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            val title = stringResource(def.titleRes)
            ShellNotificationCard(
                icon = Icons.Outlined.MilitaryTech,
                title = stringResource(R.string.shell_badge_notification_title, title),
                body = stringResource(R.string.shell_badge_notification_body, badge.count, stringResource(def.descriptionRes))
            )
        }

        items(newDiscoveries, key = { it.userDiscoveryId }) { discovery ->
            val def = ShellContentCatalog.discovery(discovery.discoveryId) ?: return@items
            val title = stringResource(def.titleRes)
            ShellNotificationCard(
                icon = Icons.Outlined.AutoStories,
                title = stringResource(R.string.shell_discovery_notification_title, title),
                body = stringResource(def.explanationRes)
            )
        }
    }
}

@Composable
private fun ShellNotificationCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.semantics {
            contentDescription = title
        }
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            headlineContent = { Text(title) },
            supportingContent = { Text(body) }
        )
    }
}

@Composable
private fun StillwaterRoomScreen(
    uiState: ShellUiState,
    onPerspective: (StillwaterPerspective) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoomHeader(
            title = R.string.shell_room_stillwater_title,
            body = R.string.shell_stillwater_body
        )

        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = scheme.primary
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(shellChamberBrush())
            ) {
                TurtleShellInteriorBackground(
                    modifier = Modifier.matchParentSize(),
                    centerGlow = true
                )

                Canvas(Modifier.matchParentSize()) {
                    repeat(6) { i ->
                        drawCircle(
                            color = scheme.primary.copy(alpha = 0.14f),
                            radius = 44f + i * 18f,
                            center = Offset(size.width / 2, size.height / 2),
                            style = Stroke(3f)
                        )
                    }
                }

                Text(
                    text = displayStillwater(uiState.stillwaterTotal, uiState.perspective),
                    color = scheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.24f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_view_as),
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                val selectorDescription = stringResource(R.string.shell_stillwater_selector_a11y)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .semantics {
                            contentDescription = selectorDescription
                        }
                ) {
                    StillwaterPerspective.entries.forEach { perspective ->
                        FilterChip(
                            selected = uiState.perspective == perspective,
                            onClick = { onPerspective(perspective) },
                            label = { Text(stringResource(labelFor(perspective))) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.shell_stillwater_same_water),
                    color = scheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(R.string.shell_soft_flow_copy),
                    color = scheme.onSurface.copy(alpha = 0.76f)
                )
            }
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
                    text = stringResource(R.string.shell_quiet_for_now),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(bodyRes),
                    color = scheme.onSurface.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RoomHeader(
    title: Int,
    body: Int
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )

            Text(
                text = stringResource(body),
                color = scheme.onSurface.copy(alpha = 0.78f)
            )
        }
    }
}

private fun iconFor(category: ShellFindCategory): ImageVector = when (category) {
    ShellFindCategory.CREATURES -> Icons.Outlined.Pets
    ShellFindCategory.SHELLS -> Icons.Outlined.Spa
    ShellFindCategory.CORAL -> Icons.Outlined.FilterVintage
    ShellFindCategory.PLANTS -> Icons.Outlined.Grass
    ShellFindCategory.TROPHIES -> Icons.Outlined.EmojiEvents
    ShellFindCategory.TRINKETS -> Icons.Outlined.Diamond
    ShellFindCategory.DISCOVERIES -> Icons.Outlined.AutoStories
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

private fun labelFor(p: StillwaterPerspective): Int = when (p) {
    StillwaterPerspective.CUPS -> R.string.shell_perspective_cups
    StillwaterPerspective.BOWLS -> R.string.shell_perspective_bowls
    StillwaterPerspective.TANK -> R.string.shell_perspective_tank
    StillwaterPerspective.POOL -> R.string.shell_perspective_pool
    StillwaterPerspective.LAKE -> R.string.shell_perspective_lake
    StillwaterPerspective.LAKE_TAHOE_PERCENT -> R.string.shell_perspective_tahoe
    StillwaterPerspective.WORLD_OCEAN_PERCENT -> R.string.shell_perspective_ocean
    StillwaterPerspective.STREAM_TIME -> R.string.shell_perspective_stream
}

@Composable
private fun displayStillwater(
    units: Long,
    p: StillwaterPerspective
): String = when (p) {
    StillwaterPerspective.CUPS -> stringResource(R.string.shell_stillwater_cups, units / 2.0)
    StillwaterPerspective.BOWLS -> stringResource(R.string.shell_stillwater_bowls, units / 10.0)
    StillwaterPerspective.TANK -> stringResource(R.string.shell_stillwater_tank, units / 600.0)
    StillwaterPerspective.POOL -> stringResource(R.string.shell_stillwater_pool, units / 20_000.0)
    StillwaterPerspective.LAKE -> stringResource(R.string.shell_stillwater_lake, units / 2_000_000.0)
    StillwaterPerspective.LAKE_TAHOE_PERCENT -> stringResource(
        R.string.shell_stillwater_tahoe,
        units / 39_000_000_000.0 * 100.0
    )
    StillwaterPerspective.WORLD_OCEAN_PERCENT -> stringResource(
        R.string.shell_stillwater_ocean,
        units / 1_350_000_000_000_000.0 * 100.0
    )
    StillwaterPerspective.STREAM_TIME -> stringResource(R.string.shell_stillwater_stream, units / 10L)
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

@Composable
private fun shellChamberBrush(): Brush {
    val scheme = MaterialTheme.colorScheme

    return Brush.radialGradient(
        colors = listOf(
            scheme.secondary.copy(alpha = 0.24f),
            scheme.primary.copy(alpha = 0.82f),
            scheme.background
        )
    )
}