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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                    onNavigate = { destination = it }
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
                    bodyRes = R.string.shell_preview_voyage
                )

                ShellDestination.CoralReefPreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_coral_title,
                    bodyRes = R.string.shell_preview_coral
                )

                ShellDestination.IdeaGrovePreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_idea_title,
                    bodyRes = R.string.shell_preview_idea
                )

                ShellDestination.LookoutPreview -> DormantPreviewScreen(
                    titleRes = R.string.shell_room_lookout_title,
                    bodyRes = R.string.shell_preview_lookout
                )
            }
        }
    }
}

@Composable
private fun ShellTopBar(
    destination: ShellDestination,
    pearlBalance: Int,
    onBack: () -> Unit,
    onChest: () -> Unit
) {
    val palette = shellPalette()

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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary
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
                onClick = {},
                label = {
                    Text(stringResource(R.string.shell_pearl_balance, pearlBalance))
                },
                leadingIcon = {
                    ShellPearlMiniIcon(
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = palette.warmShellSurface,
                    labelColor = palette.textOnWarm,
                    leadingIconContentColor = palette.primary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = palette.gold.copy(alpha = 0.45f)
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
    onNavigate: (ShellDestination) -> Unit
) {
    val palette = shellPalette()

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ShellHeroCard(
                uiState = uiState,
                onNavigate = onNavigate
            )
        }

        item {
            Text(
                text = stringResource(R.string.shell_room_entrances),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textOnDark,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            RoomEntranceGrid(onNavigate)
        }

        item {
            ShellPulsePanel(uiState)
        }
    }
}

@Composable
private fun ShellHeroCard(
    uiState: ShellUiState,
    onNavigate: (ShellDestination) -> Unit
) {
    val palette = shellPalette()

    ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.shellDeep
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(shellHeroBrush(palette))
        ) {
            TurtleShellInteriorBackground(
                modifier = Modifier.matchParentSize(),
                palette = palette,
                centerGlow = true
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                palette.shellAbyss.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textOnDark,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.shell_welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textOnDark.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Button(
                    onClick = { onNavigate(ShellDestination.Focus) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.gold,
                        contentColor = palette.textOnGold
                    )
                ) {
                    Text(stringResource(R.string.shell_enter_focus))
                }
            }
        }
    }

    PearlBasin(uiState, onNavigate)
}

@Composable
private fun TurtleShellInteriorBackground(
    modifier: Modifier = Modifier,
    palette: ShellPalette = shellPalette(),
    centerGlow: Boolean = false
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = palette.shellAbyss.copy(alpha = 0.90f),
            topLeft = Offset(-w * 0.12f, h * 0.02f),
            size = Size(w * 1.24f, h * 1.10f)
        )

        drawOval(
            color = palette.shellDeep.copy(alpha = 0.94f),
            topLeft = Offset(w * 0.04f, h * 0.07f),
            size = Size(w * 0.92f, h * 0.86f)
        )

        drawOval(
            color = palette.gold.copy(alpha = 0.18f),
            topLeft = Offset(w * 0.13f, h * 0.12f),
            size = Size(w * 0.74f, h * 0.68f)
        )

        if (centerGlow) {
            drawCircle(
                color = palette.primary.copy(alpha = 0.16f),
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
            color = palette.shellLine.copy(alpha = 0.30f),
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
                color = palette.shellLine.copy(alpha = 0.18f),
                style = Stroke(width = 3f)
            )
        }

        listOf(0.30f, 0.70f).forEach { xFraction ->
            val side = Path().apply {
                moveTo(w * xFraction, h * 0.16f)
                cubicTo(
                    w * (xFraction - if (xFraction < 0.5f) 0.065f else -0.065f),
                    h * 0.34f,
                    w * (xFraction - if (xFraction < 0.5f) 0.055f else -0.055f),
                    h * 0.56f,
                    w * xFraction,
                    h * 0.80f
                )
            }

            drawPath(
                path = side,
                color = palette.shellLine.copy(alpha = 0.15f),
                style = Stroke(width = 2.5f)
            )
        }

        val pearlPoints = listOf(
            Offset(w * 0.23f, h * 0.26f),
            Offset(w * 0.76f, h * 0.31f),
            Offset(w * 0.30f, h * 0.58f),
            Offset(w * 0.68f, h * 0.66f),
            Offset(w * 0.50f, h * 0.43f)
        )

        pearlPoints.forEachIndexed { index, point ->
            drawCircle(
                color = if (index == 4) {
                    palette.primary.copy(alpha = 0.26f)
                } else {
                    palette.pearl.copy(alpha = 0.18f)
                },
                radius = if (index == 4) 7f else 4.5f,
                center = point
            )
        }

        drawCircle(
            color = palette.rareAccent.copy(alpha = 0.08f),
            radius = w * 0.13f,
            center = Offset(w * 0.78f, h * 0.20f)
        )

        drawOval(
            color = Color.Black.copy(alpha = 0.34f),
            topLeft = Offset(-w * 0.08f, h * 0.02f),
            size = Size(w * 1.16f, h * 1.04f),
            style = Stroke(width = w * 0.08f)
        )
    }
}

@Composable
private fun PearlBasin(
    uiState: ShellUiState,
    onNavigate: (ShellDestination) -> Unit
) {
    val palette = shellPalette()
    val basinDescription = stringResource(R.string.shell_pearl_basin_a11y)

    ElevatedCard(
        onClick = { onNavigate(ShellDestination.Focus) },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.warmShellSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .semantics { contentDescription = basinDescription }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShellPearlBasinIcon(
                modifier = Modifier.size(52.dp),
                palette = palette
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.shell_pearl_basin_title),
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textOnWarm
                )

                Text(
                    text = if (uiState.pearlBalance >= 80) {
                        stringResource(R.string.shell_pearl_basin_suggestion)
                    } else {
                        stringResource(R.string.shell_pearl_basin_copy)
                    },
                    color = palette.textOnWarm.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun ShellPearlBasinIcon(
    modifier: Modifier = Modifier,
    palette: ShellPalette = shellPalette()
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = palette.goldDeep.copy(alpha = 0.92f),
            topLeft = Offset(w * 0.08f, h * 0.32f),
            size = Size(w * 0.84f, h * 0.50f)
        )

        drawOval(
            color = palette.gold.copy(alpha = 0.82f),
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
                color = palette.textOnGold.copy(alpha = 0.28f),
                style = Stroke(width = 2f)
            )
        }

        drawCircle(
            color = palette.pearl,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.36f)
        )

        drawCircle(
            color = palette.primary.copy(alpha = 0.36f),
            radius = w * 0.08f,
            center = Offset(w * 0.56f, h * 0.31f)
        )
    }
}

@Composable
private fun ShellPearlMiniIcon(
    modifier: Modifier = Modifier
) {
    val palette = shellPalette()

    Canvas(modifier) {
        drawCircle(
            color = palette.pearl,
            radius = size.minDimension * 0.36f,
            center = Offset(size.width / 2f, size.height / 2f)
        )

        drawCircle(
            color = palette.primary.copy(alpha = 0.42f),
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.60f, size.height * 0.38f)
        )
    }
}

@Composable
private fun RoomEntranceGrid(onNavigate: (ShellDestination) -> Unit) {
    val entrances = listOf(
        Triple(
            R.string.shell_room_focus_title,
            R.string.shell_room_focus_description,
            ShellDestination.Focus
        ),
        Triple(
            R.string.shell_room_stillwater_title,
            R.string.shell_room_stillwater_description,
            ShellDestination.Stillwater
        ),
        Triple(
            R.string.shell_room_voyage_title,
            R.string.shell_preview_voyage,
            ShellDestination.VoyagePreview
        ),
        Triple(
            R.string.shell_room_coral_title,
            R.string.shell_preview_coral,
            ShellDestination.CoralReefPreview
        ),
        Triple(
            R.string.shell_room_idea_title,
            R.string.shell_preview_idea,
            ShellDestination.IdeaGrovePreview
        ),
        Triple(
            R.string.shell_room_lookout_title,
            R.string.shell_preview_lookout,
            ShellDestination.LookoutPreview
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entrances.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (title, body, dest) ->
                    EntranceCard(
                        title = title,
                        body = body,
                        dest = dest,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EntranceCard(
    title: Int,
    body: Int,
    dest: ShellDestination,
    onNavigate: (ShellDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = shellPalette()
    val titleText = stringResource(title)

    ElevatedCard(
        onClick = { onNavigate(dest) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.warmShellSurface
        ),
        modifier = modifier.semantics {
            contentDescription = titleText
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
        ) {
            TurtleShellCardPattern(
                modifier = Modifier.matchParentSize(),
                palette = palette
            )

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShellRoomIcon(dest)

                Text(
                    text = titleText,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textOnWarm
                )

                Text(
                    text = stringResource(body),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textOnWarm.copy(alpha = 0.74f)
                )
            }
        }
    }
}

@Composable
private fun TurtleShellCardPattern(
    modifier: Modifier = Modifier,
    palette: ShellPalette = shellPalette()
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = palette.gold.copy(alpha = 0.10f),
            topLeft = Offset(w * 0.08f, -h * 0.30f),
            size = Size(w * 0.84f, h * 1.30f)
        )

        val seamColor = palette.textOnWarm.copy(alpha = 0.13f)

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
private fun ShellRoomIcon(dest: ShellDestination) {
    val palette = shellPalette()

    val icon = when (dest) {
        ShellDestination.Focus -> Icons.Outlined.CenterFocusStrong
        ShellDestination.Stillwater -> Icons.Outlined.WaterDrop
        ShellDestination.VoyagePreview -> Icons.Outlined.Route
        ShellDestination.CoralReefPreview -> Icons.Outlined.FilterVintage
        ShellDestination.IdeaGrovePreview -> Icons.Outlined.PsychologyAlt
        ShellDestination.LookoutPreview -> Icons.Outlined.Visibility
        else -> Icons.Outlined.Waves
    }

    Surface(
        shape = CircleShape,
        color = palette.primaryDeep.copy(alpha = 0.94f),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.gold,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun ShellPulsePanel(uiState: ShellUiState) {
    val palette = shellPalette()

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.warmShellSurface
        )
    ) {
        Box(Modifier.fillMaxWidth()) {
            TurtleShellCardPattern(
                modifier = Modifier.matchParentSize(),
                palette = palette
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_pulse_title),
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textOnWarm
                )

                Text(
                    text = stringResource(
                        R.string.shell_pulse_recent,
                        uiState.finds.size + uiState.stacks.sumOf { it.quantity }
                    ),
                    color = palette.textOnWarm.copy(alpha = 0.78f)
                )

                Text(
                    text = stringResource(R.string.shell_pulse_mystery),
                    color = palette.textOnWarm.copy(alpha = 0.78f)
                )
            }
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
    val palette = shellPalette()
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
                .background(shellChamberBrush(palette))
        ) {
            TurtleShellInteriorBackground(
                modifier = Modifier.matchParentSize(),
                palette = palette,
                centerGlow = false
            )

            FocusRoomCarvedShelves(
                modifier = Modifier.matchParentSize(),
                palette = palette
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

        Text(
            text = stringResource(R.string.shell_focus_summary, uiState.focusPlacements.size),
            color = palette.textOnDark
        )
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
private fun FocusRoomCarvedShelves(
    modifier: Modifier = Modifier,
    palette: ShellPalette = shellPalette()
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val shelfColor = palette.gold.copy(alpha = 0.17f)
        val shelfShadow = Color.Black.copy(alpha = 0.22f)

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
    val palette = shellPalette()
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
            palette.warmShellSurface.copy(alpha = 0.96f)
        } else {
            palette.shellAbyss.copy(alpha = 0.58f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isFilled) {
                palette.gold.copy(alpha = 0.68f)
            } else {
                palette.gold.copy(alpha = 0.24f)
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
                    palette.textOnWarm
                } else {
                    palette.textOnDark.copy(alpha = 0.72f)
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

                Text(stringResource(R.string.shell_next_form, nextTitle))

                Button(
                    onClick = { onUpgrade(item.instanceId) },
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
                modifier = Modifier.semantics {
                    contentDescription = title
                }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = iconFor(def.category),
                            contentDescription = null
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

            ElevatedCard {
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
                Text(stringResource(R.string.shell_enter_focus))
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
                modifier = Modifier.semantics {
                    contentDescription = badgeDescription
                }
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = null
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
    val palette = shellPalette()

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
                containerColor = palette.shellDeep
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(shellChamberBrush(palette))
            ) {
                TurtleShellInteriorBackground(
                    modifier = Modifier.matchParentSize(),
                    palette = palette,
                    centerGlow = true
                )

                Canvas(Modifier.matchParentSize()) {
                    repeat(6) { i ->
                        drawCircle(
                            color = palette.primary.copy(alpha = 0.13f),
                            radius = 44f + i * 18f,
                            center = Offset(size.width / 2, size.height / 2),
                            style = Stroke(3f)
                        )
                    }
                }

                Text(
                    text = displayStillwater(uiState.stillwaterTotal, uiState.perspective),
                    color = palette.textOnDark,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.shell_view_as),
            color = palette.textOnDark
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
            color = palette.textOnDark.copy(alpha = 0.84f)
        )
    }
}

@Composable
private fun DormantPreviewScreen(
    titleRes: Int,
    bodyRes: Int
) {
    val palette = shellPalette()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(shellChamberBrush(palette))
    ) {
        TurtleShellInteriorBackground(
            modifier = Modifier.matchParentSize(),
            palette = palette,
            centerGlow = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = palette.primaryDeep.copy(alpha = 0.84f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Waves,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = palette.gold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textOnDark,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(bodyRes),
                color = palette.textOnDark.copy(alpha = 0.86f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoomHeader(
    title: Int,
    body: Int
) {
    val palette = shellPalette()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textOnDark
        )

        Text(
            text = stringResource(body),
            color = palette.textOnDark.copy(alpha = 0.86f)
        )
    }
}

@Composable
private fun AmbientBubbles(
    modifier: Modifier = Modifier
) {
    val palette = shellPalette()

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        repeat(10) { i ->
            val x = w * ((i * 23 + 11) % 100) / 100f
            val y = h * ((i * 31 + 17) % 100) / 100f
            val radius = 2.5f + (i % 3) * 1.5f

            drawCircle(
                color = if (i % 3 == 0) {
                    palette.primary.copy(alpha = 0.10f)
                } else {
                    palette.gold.copy(alpha = 0.10f)
                },
                radius = radius,
                center = Offset(x, y)
            )
        }

        val path = Path().apply {
            moveTo(w * 0.08f, h * 0.68f)
            cubicTo(
                w * 0.25f,
                h * 0.58f,
                w * 0.62f,
                h * 0.80f,
                w * 0.92f,
                h * 0.64f
            )
        }

        drawPath(
            path = path,
            color = palette.gold.copy(alpha = 0.12f),
            style = Stroke(width = 4f)
        )
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
    val palette = shellPalette()

    return Brush.verticalGradient(
        colors = listOf(
            palette.shellDeep,
            palette.primaryDeep,
            palette.shellAbyss
        )
    )
}

private fun shellHeroBrush(palette: ShellPalette): Brush {
    return Brush.radialGradient(
        colors = listOf(
            palette.gold.copy(alpha = 0.62f),
            palette.primary.copy(alpha = 0.72f),
            palette.primaryDeep,
            palette.shellAbyss
        )
    )
}

private fun shellChamberBrush(palette: ShellPalette): Brush {
    return Brush.radialGradient(
        colors = listOf(
            palette.gold.copy(alpha = 0.28f),
            palette.shellDeep,
            palette.primaryDeep,
            palette.shellAbyss
        )
    )
}

@Composable
private fun shellPalette(): ShellPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.45f

    val primaryDeep = if (isDark) {
        blend(scheme.primary, scheme.background, 0.58f)
    } else {
        blend(scheme.primary, Color.Black, 0.48f)
    }

    val shellDeep = if (isDark) {
        blend(scheme.surface, scheme.primary, 0.10f)
    } else {
        blend(scheme.primary, Color.Black, 0.60f)
    }

    val shellAbyss = if (isDark) {
        blend(scheme.background, Color.Black, 0.28f)
    } else {
        blend(scheme.primary, Color.Black, 0.78f)
    }

    val warmShellSurface = if (isDark) {
        blend(scheme.surface, scheme.secondary, 0.08f)
    } else {
        scheme.surface
    }

    val goldDeep = blend(scheme.secondary, Color.Black, if (isDark) 0.22f else 0.30f)

    val shellLine = if (isDark) {
        blend(scheme.secondary, scheme.onSurface, 0.16f)
    } else {
        blend(scheme.secondary, Color.Black, 0.18f)
    }

    val pearl = if (isDark) {
        blend(Color.White, scheme.primary, 0.10f)
    } else {
        blend(Color.White, scheme.primary, 0.06f)
    }

    return ShellPalette(
        primary = scheme.primary,
        primaryDeep = primaryDeep,
        shellDeep = shellDeep,
        shellAbyss = shellAbyss,
        gold = scheme.secondary,
        goldDeep = goldDeep,
        shellLine = shellLine,
        warmShellSurface = warmShellSurface,
        pearl = pearl,
        textOnDark = scheme.onPrimary,
        textOnWarm = scheme.onSurface,
        textOnGold = scheme.onSecondary,
        rareAccent = scheme.tertiary
    )
}

private data class ShellPalette(
    val primary: Color,
    val primaryDeep: Color,
    val shellDeep: Color,
    val shellAbyss: Color,
    val gold: Color,
    val goldDeep: Color,
    val shellLine: Color,
    val warmShellSurface: Color,
    val pearl: Color,
    val textOnDark: Color,
    val textOnWarm: Color,
    val textOnGold: Color,
    val rareAccent: Color
)

private fun blend(
    start: Color,
    end: Color,
    fraction: Float
): Color {
    val clamped = fraction.coerceIn(0f, 1f)

    return Color(
        red = start.red + (end.red - start.red) * clamped,
        green = start.green + (end.green - start.green) * clamped,
        blue = start.blue + (end.blue - start.blue) * clamped,
        alpha = start.alpha + (end.alpha - start.alpha) * clamped
    )
}