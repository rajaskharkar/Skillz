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
import androidx.compose.ui.semantics.contentDescription
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
import com.kingkharnivore.skillz.data.model.shell.ShellFindCategory
import com.kingkharnivore.skillz.data.model.shell.ShellRoomId
import com.kingkharnivore.skillz.data.model.shell.ShellSlotDefinition
import com.kingkharnivore.skillz.data.model.shell.StillwaterPerspective
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.ShellViewModel

sealed class ShellDestination {
    data object Heart : ShellDestination()
    data object Focus : ShellDestination()
    data object Stillwater : ShellDestination()
    data object ShellChest : ShellDestination()
    data object Badges : ShellDestination()
    data object DiscoveryJournal : ShellDestination()
    data object VoyagePreview : ShellDestination()
    data object CoralReefPreview : ShellDestination()
    data object IdeaGrovePreview : ShellDestination()
    data object LookoutPreview : ShellDestination()
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
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShellTopBar(
                destination = destination,
                pearlBalance = uiState.pearlBalance,
                onBack = {
                    if (destination == ShellDestination.Heart) {
                        onBack()
                    } else {
                        destination = ShellDestination.Heart
                    }
                },
                onPearls = { showPearlBasin = true },
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
                    onOpenFocus = { destination = ShellDestination.Focus }
                )

                ShellDestination.Badges -> BadgesScreen(uiState)
                ShellDestination.DiscoveryJournal -> DiscoveryJournalScreen(uiState)

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
                }
            )
        }
    }
}

@Composable
private fun ShellTopBar(
    destination: ShellDestination,
    pearlBalance: Int,
    onBack: () -> Unit,
    onPearls: () -> Unit,
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
        ShellDestination.VoyagePreview -> stringResource(R.string.shell_room_voyage_title)
        ShellDestination.CoralReefPreview -> stringResource(R.string.shell_room_coral_title)
        ShellDestination.IdeaGrovePreview -> stringResource(R.string.shell_room_idea_title)
        ShellDestination.LookoutPreview -> stringResource(R.string.shell_room_lookout_title)
    }

    val pearlBalanceDescription = stringResource(R.string.shell_pearl_balance_a11y, pearlBalance)

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
                    color = scheme.secondary.copy(alpha = 0.45f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = pearlBalanceDescription
                }
            )

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
                    val totalFinds = uiState.finds.size + uiState.stacks.sumOf { it.quantity }
                    if (uiState.discoveries.isNotEmpty()) {
                        onNavigate(ShellDestination.DiscoveryJournal)
                    } else if (totalFinds > 0) {
                        onNavigate(ShellDestination.ShellChest)
                    } else {
                        onNavigate(ShellDestination.Focus)
                    }
                }
            )

            HeartShortcutDock(
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
    nodeWidth: Dp,
    nodeHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(labelRes)

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
            .semantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TurtleShellCardPattern(Modifier.matchParentSize())

            if (dormant) {
                Surface(
                    shape = CircleShape,
                    color = scheme.secondary.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(7.dp),
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
    val heartDescription = stringResource(R.string.shell_title)
    val pearlBalanceDescription = stringResource(R.string.shell_pearl_balance_a11y, uiState.pearlBalance)

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = scheme.surface
        ),
        modifier = modifier
            .width(214.dp)
            .semantics { contentDescription = heartDescription }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
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
                textAlign = TextAlign.Center
            )

            AssistChip(
                onClick = onPearlClick,
                label = {
                    Text(stringResource(R.string.shell_pearl_balance, uiState.pearlBalance))
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
    val totalFinds = uiState.finds.size + uiState.stacks.sumOf { it.quantity }

    val text = when {
        uiState.discoveries.isNotEmpty() -> stringResource(R.string.shell_pulse_mystery)
        totalFinds > 0 -> stringResource(R.string.shell_pulse_recent, totalFinds)
        else -> stringResource(R.string.shell_pulse_mystery)
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.35f)),
        modifier = modifier.clickable(onClick = onClick)
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
                onClick = onChest
            )

            HeartShortcut(
                icon = Icons.Outlined.MilitaryTech,
                labelRes = R.string.shell_badges_title,
                onClick = onBadges
            )

            HeartShortcut(
                icon = Icons.Outlined.AutoStories,
                labelRes = R.string.shell_journal_title,
                onClick = onJournal
            )
        }
    }
}

@Composable
private fun HeartShortcut(
    icon: ImageVector,
    labelRes: Int,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(22.dp)
        )

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
                text = stringResource(R.string.shell_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.shell_pearl_balance, uiState.pearlBalance),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = stringResource(R.string.shell_pulse_recent, totalFinds),
                style = MaterialTheme.typography.bodyMedium
            )

            if (uiState.discoveries.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.shell_pulse_mystery),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

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
    onOpenChest: () -> Unit
) {
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

            Text(
                text = if (uiState.pearlBalance >= 80) {
                    stringResource(R.string.shell_pearl_basin_suggestion)
                } else {
                    stringResource(R.string.shell_pearl_basin_copy)
                }
            )

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
            Text(
                text = stringResource(R.string.shell_focus_summary, uiState.focusPlacements.size),
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
            }
        )
    }

    if (selectedInstance != null) {
        PlacedFindSheet(
            item = selectedInstance!!,
            pearlBalance = uiState.pearlBalance,
            onDismiss = { selectedInstance = null },
            onReturn = {
                onReturn(it)
                selectedInstance = null
            },
            onUpgrade = {
                onUpgrade(it)
                selectedInstance = null
            }
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

    val label = if (def != null) {
        stringResource(def.titleRes)
    } else {
        stringResource(R.string.shell_empty_slot)
    }

    val isFilled = def != null

    Surface(
        modifier = modifier
            .semantics { contentDescription = label }
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

@Composable
private fun EmptySlotSheet(
    slotId: String,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onPlace: (String) -> Unit
) {
    val slot = ShellContentCatalog.focusSlots.first { it.slotId == slotId }
    val placedIds = uiState.focusPlacements.map { it.instanceId }.toSet()

    val compatible = uiState.finds.filter { instance ->
        val def = ShellContentCatalog.find(instance.findId)

        instance.instanceId !in placedIds &&
                def?.placeable == true &&
                slot.slotType in def.acceptedSlotTypes &&
                def.category in slot.acceptsCategories
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

            Text(stringResource(R.string.shell_place_something_here))

            compatible.forEach { item ->
                val def = ShellContentCatalog.find(item.findId) ?: return@forEach

                ListItem(
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = { Text(stringResource(R.string.shell_place_free)) },
                    modifier = Modifier.clickable { onPlace(item.instanceId) }
                )
            }

            if (compatible.isEmpty()) {
                Text(stringResource(R.string.shell_no_owned_finds_fit))
            }
        }
    }
}

@Composable
private fun PlacedFindSheet(
    item: UserShellFindInstanceEntity,
    pearlBalance: Int,
    onDismiss: () -> Unit,
    onReturn: (String) -> Unit,
    onUpgrade: (String) -> Unit
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

            Text(stringResource(R.string.shell_earned_from_flow))
            Text(stringResource(R.string.shell_current_form, currentTitle))

            if (next != null) {
                val nextTitle = stringResource(next.titleRes)
                val upgradeVerb = stringResource(next.upgradeVerbRes)
                val upgradeDescription = stringResource(R.string.shell_upgrade_a11y)
                val canAfford = pearlBalance >= next.pearlCost

                Text(stringResource(R.string.shell_next_form, nextTitle))

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
                Text(stringResource(R.string.shell_no_more_forms))
            }

            OutlinedButton(onClick = { onReturn(item.instanceId) }) {
                Text(stringResource(R.string.shell_return_to_chest))
            }
        }
    }
}

@Composable
private fun ShellChestScreen(
    uiState: ShellUiState,
    onPlace: (String, String) -> Unit,
    onOpenFocus: () -> Unit
) {
    var category by remember { mutableStateOf<ShellFindCategory?>(null) }
    var selectedInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }

    val items = uiState.finds.filter {
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

        items(items) { instance ->
            val def = ShellContentCatalog.find(instance.findId) ?: return@items
            val title = stringResource(def.titleRes)
            val categoryLabel = stringResource(categoryLabelFor(def.category))
            val status = if (uiState.focusPlacements.any { it.instanceId == instance.instanceId }) {
                stringResource(R.string.shell_status_placed)
            } else {
                stringResource(R.string.shell_status_unplaced)
            }

            ElevatedCard(
                onClick = { selectedInstance = instance },
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
                            imageVector = iconFor(def.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = { Text(title) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.shell_chest_item_status,
                                categoryLabel,
                                status
                            )
                        )
                    }
                )
            }
        }

        items(uiState.stacks) { stack ->
            val def = ShellContentCatalog.find(stack.findId) ?: return@items

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = {
                        Text(stringResource(R.string.shell_stack_quantity, stack.quantity))
                    }
                )
            }
        }
    }

    selectedInstance?.let { instance ->
        ChestPlacementSheet(
            instance = instance,
            uiState = uiState,
            onDismiss = { selectedInstance = null },
            onPlace = { slotId ->
                onPlace(instance.instanceId, slotId)
                selectedInstance = null
                onOpenFocus()
            },
            onOpenFocus = {
                selectedInstance = null
                onOpenFocus()
            }
        )
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
    val occupied = uiState.focusPlacements.map { it.slotId }.toSet()

    val slots = if (def == null) {
        emptyList()
    } else {
        ShellContentCatalog.focusSlots.filter { slot ->
            slot.slotId !in occupied &&
                    slot.slotType in def.acceptedSlotTypes &&
                    def.category in slot.acceptsCategories
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

            Text(stringResource(R.string.shell_compatible_slots))

            slots.forEach { slot ->
                ListItem(
                    headlineContent = {
                        Text(slot.slotId.replace('_', ' ').replaceFirstChar { it.titlecase() })
                    },
                    supportingContent = {
                        Text(stringResource(R.string.shell_place_free))
                    },
                    modifier = Modifier
                        .clickable { onPlace(slot.slotId) }
                        .semantics {
                            contentDescription = slot.slotId
                        }
                )
            }

            if (slots.isEmpty()) {
                Text(stringResource(R.string.shell_no_owned_finds_fit))
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
                    headlineContent = { Text(title) },
                    supportingContent = {
                        Text(stringResource(def.descriptionRes, badge.count))
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