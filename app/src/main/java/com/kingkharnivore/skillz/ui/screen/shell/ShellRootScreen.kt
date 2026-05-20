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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellFindCategory
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.ui.screen.shell.components.canDisplayInstance
import com.kingkharnivore.skillz.ui.screen.shell.components.depthLabel
import com.kingkharnivore.skillz.ui.screen.shell.components.displayedInstanceIds
import com.kingkharnivore.skillz.ui.screen.shell.components.isUserVisibleShellFind
import com.kingkharnivore.skillz.ui.screen.shell.components.kindLabel
import com.kingkharnivore.skillz.ui.screen.shell.components.restingFinds
import com.kingkharnivore.skillz.ui.screen.shell.components.currentFormOrder
import com.kingkharnivore.skillz.ui.screen.shell.components.hasAffordablePearlShape
import com.kingkharnivore.skillz.ui.screen.shell.components.hasEmptyNookForNewRestingObject
import com.kingkharnivore.skillz.ui.screen.shell.components.hasRestingPlaceableFinds
import com.kingkharnivore.skillz.ui.screen.shell.components.hasAffordableFocusPearlAction
import com.kingkharnivore.skillz.ui.screen.shell.components.unseenNotificationCount
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.data.model.shell.ShellSlotDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellSlotType
import com.kingkharnivore.skillz.data.model.shell.StillwaterPerspective
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureZone
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.ShellViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

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
        ShellDestination.TheBluePreview -> stringResource(R.string.shell_room_the_blue_title)
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
                            color = scheme.secondary,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = notificationCount.coerceAtMost(9).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSecondary,
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

