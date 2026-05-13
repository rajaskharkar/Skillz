@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.*
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
    var destination by remember { mutableStateOf<ShellDestination>(ShellDestination.Heart) }
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
        topBar = {
            ShellTopBar(
                destination = destination,
                pearlBalance = uiState.pearlBalance,
                onBack = { if (destination == ShellDestination.Heart) onBack() else destination = ShellDestination.Heart },
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
                ShellDestination.Heart -> HeartRoomScreen(uiState, onNavigate = { destination = it })
                ShellDestination.Focus -> FocusRoomScreen(uiState, viewModel::place, viewModel::returnToChest, viewModel::upgrade)
                ShellDestination.Stillwater -> StillwaterRoomScreen(uiState, viewModel::setPerspective)
                ShellDestination.ShellChest -> ShellChestScreen(uiState, onPlaceInFocus = { destination = ShellDestination.Focus })
                ShellDestination.Badges -> BadgesScreen(uiState)
                ShellDestination.DiscoveryJournal -> DiscoveryJournalScreen(uiState)
                ShellDestination.VoyagePreview -> DormantPreviewScreen(R.string.shell_room_voyage_title, R.string.shell_preview_voyage)
                ShellDestination.CoralReefPreview -> DormantPreviewScreen(R.string.shell_room_coral_title, R.string.shell_preview_coral)
                ShellDestination.IdeaGrovePreview -> DormantPreviewScreen(R.string.shell_room_idea_title, R.string.shell_preview_idea)
                ShellDestination.LookoutPreview -> DormantPreviewScreen(R.string.shell_room_lookout_title, R.string.shell_preview_lookout)
            }
        }
    }
}

@Composable
private fun ShellTopBar(destination: ShellDestination, pearlBalance: Int, onBack: () -> Unit, onChest: () -> Unit) {
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
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.shell_back_a11y)) } },
        actions = {
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.shell_pearl_balance, pearlBalance)) },
                leadingIcon = { Icon(Icons.Outlined.Spa, contentDescription = null) },
                modifier = Modifier.semantics { contentDescription = stringResource(R.string.shell_pearl_balance_a11y, pearlBalance) }
            )
            IconButton(onClick = onChest) { Icon(Icons.Outlined.Inventory2, contentDescription = stringResource(R.string.shell_chest_a11y)) }
        }
    )
}

@Composable
private fun HeartRoomScreen(uiState: ShellUiState, onNavigate: (ShellDestination) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ShellHeroCard(uiState, onNavigate)
        }
        item {
            Text(stringResource(R.string.shell_room_entrances), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
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
private fun ShellHeroCard(uiState: ShellUiState, onNavigate: (ShellDestination) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp).background(shellChamberBrush())) {
            AmbientBubbles(Modifier.matchParentSize())
            Canvas(Modifier.align(Alignment.Center).size(170.dp)) {
                drawOval(Color(0xFF74D6C9).copy(.9f))
                drawCircle(Color(0xFF224B5B), radius = size.minDimension * .20f, center = Offset(size.width * .38f, size.height * .42f))
                drawCircle(Color(0xFFE8FFF9), radius = size.minDimension * .08f, center = Offset(size.width * .46f, size.height * .38f))
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.shell_welcome), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.shell_welcome_body), color = Color.White.copy(.9f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onNavigate(ShellDestination.Focus) }) { Text(stringResource(R.string.shell_enter_focus)) }
            }
        }
    }
    PearlBasin(uiState, onNavigate)
}

@Composable
private fun PearlBasin(uiState: ShellUiState, onNavigate: (ShellDestination) -> Unit) {
    ElevatedCard(
        onClick = { onNavigate(ShellDestination.Focus) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .semantics { contentDescription = stringResource(R.string.shell_pearl_basin_a11y) }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Spa, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.shell_pearl_basin_title), fontWeight = FontWeight.SemiBold)
                Text(if (uiState.pearlBalance >= 80) stringResource(R.string.shell_pearl_basin_suggestion) else stringResource(R.string.shell_pearl_basin_copy))
            }
        }
    }
}

@Composable
private fun RoomEntranceGrid(onNavigate: (ShellDestination) -> Unit) {
    val entrances = listOf(
        Triple(R.string.shell_room_focus_title, R.string.shell_room_focus_description, ShellDestination.Focus),
        Triple(R.string.shell_room_stillwater_title, R.string.shell_room_stillwater_description, ShellDestination.Stillwater),
        Triple(R.string.shell_room_voyage_title, R.string.shell_preview_voyage, ShellDestination.VoyagePreview),
        Triple(R.string.shell_room_coral_title, R.string.shell_preview_coral, ShellDestination.CoralReefPreview),
        Triple(R.string.shell_room_idea_title, R.string.shell_preview_idea, ShellDestination.IdeaGrovePreview),
        Triple(R.string.shell_room_lookout_title, R.string.shell_preview_lookout, ShellDestination.LookoutPreview)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entrances.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (title, body, dest) ->
                    EntranceCard(title, body, dest, onNavigate, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EntranceCard(title: Int, body: Int, dest: ShellDestination, onNavigate: (ShellDestination) -> Unit, modifier: Modifier = Modifier) {
    val titleText = stringResource(title)
    ElevatedCard(onClick = { onNavigate(dest) }, modifier = modifier.semantics { contentDescription = titleText }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Waves, contentDescription = null)
            Text(titleText, fontWeight = FontWeight.SemiBold)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ShellPulsePanel(uiState: ShellUiState) {
    ElevatedCard(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.shell_pulse_title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.shell_pulse_recent, uiState.finds.size + uiState.stacks.sumOf { it.quantity }))
            Text(stringResource(R.string.shell_pulse_mystery))
        }
    }
}

@Composable
private fun FocusRoomScreen(uiState: ShellUiState, onPlace: (String, String) -> Unit, onReturn: (String) -> Unit, onUpgrade: (String) -> Unit) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var selectedInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }
    val placementsBySlot = uiState.focusPlacements.associateBy { it.slotId }
    val findsById = uiState.finds.associateBy { it.instanceId }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RoomHeader(R.string.shell_room_focus_title, R.string.shell_focus_body)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(shellChamberBrush())) {
            AmbientBubbles(Modifier.matchParentSize())
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
                    onFind = { if (find != null) selectedInstance = find }
                )
            }
        }
        Text(stringResource(R.string.shell_focus_summary, uiState.focusPlacements.size), color = MaterialTheme.colorScheme.onPrimary)
    }
    if (selectedSlot != null) EmptySlotSheet(selectedSlot!!, uiState, onDismiss = { selectedSlot = null }, onPlace = { id -> onPlace(id, selectedSlot!!); selectedSlot = null })
    if (selectedInstance != null) PlacedFindSheet(selectedInstance!!, onDismiss = { selectedInstance = null }, onReturn = { onReturn(it); selectedInstance = null }, onUpgrade = { onUpgrade(it); selectedInstance = null })
}

@Composable
private fun SlotChip(slot: ShellSlotDefinition, find: UserShellFindInstanceEntity?, modifier: Modifier, onEmpty: () -> Unit, onFind: () -> Unit) {
    val def = find?.let { ShellContentCatalog.find(it.findId) }
    val label = def?.let { stringResource(it.titleRes) } ?: stringResource(R.string.shell_empty_slot)
    Surface(
        modifier = modifier.semantics { contentDescription = label }.clickable { if (find == null) onEmpty() else onFind() },
        shape = RoundedCornerShape(18.dp), color = if (find == null) Color.White.copy(.16f) else Color(0xFFE7FFF8).copy(.92f)
    ) { Box(contentAlignment = Alignment.Center) { Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(6.dp)) } }
}

@Composable
private fun EmptySlotSheet(slotId: String, uiState: ShellUiState, onDismiss: () -> Unit, onPlace: (String) -> Unit) {
    val slot = ShellContentCatalog.focusSlots.first { it.slotId == slotId }
    val placedIds = uiState.focusPlacements.map { it.instanceId }.toSet()
    val compatible = uiState.finds.filter { instance ->
        val def = ShellContentCatalog.find(instance.findId)
        instance.instanceId !in placedIds && def?.placeable == true && slot.slotType in def.acceptedSlotTypes && def.category in slot.acceptsCategories
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.shell_empty_slot_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.shell_place_something_here))
            compatible.forEach { item ->
                val def = ShellContentCatalog.find(item.findId) ?: return@forEach
                ListItem(headlineContent = { Text(stringResource(def.titleRes)) }, supportingContent = { Text(stringResource(R.string.shell_place_free)) }, modifier = Modifier.clickable { onPlace(item.instanceId) })
            }
            if (compatible.isEmpty()) Text(stringResource(R.string.shell_no_owned_finds_fit))
            Text(stringResource(R.string.shell_shape_with_pearls), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.shell_contextual_invites_preview))
        }
    }
}

@Composable
private fun PlacedFindSheet(item: UserShellFindInstanceEntity, onDismiss: () -> Unit, onReturn: (String) -> Unit, onUpgrade: (String) -> Unit) {
    val def = ShellContentCatalog.find(item.findId)
    val current = def?.let { ShellContentCatalog.upgradesFor(it.findId).firstOrNull { stage -> stage.upgradeStageId == item.currentUpgradeStageId } }
    val next = def?.let { ShellContentCatalog.nextUpgrade(it.findId, item.currentUpgradeStageId) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(def?.let { stringResource(it.titleRes) } ?: item.findId, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.shell_earned_from_flow))
            Text(stringResource(R.string.shell_current_form, current?.let { stringResource(it.titleRes) } ?: def?.let { stringResource(it.titleRes) } ?: item.findId))
            if (next != null) {
                Text(stringResource(R.string.shell_next_form, stringResource(next.titleRes)))
                Button(onClick = { onUpgrade(item.instanceId) }, modifier = Modifier.semantics { contentDescription = stringResource(R.string.shell_upgrade_a11y) }) {
                    Text(stringResource(R.string.shell_upgrade_with_pearls, stringResource(next.upgradeVerbRes), next.pearlCost))
                }
            } else {
                Text(stringResource(R.string.shell_no_more_forms))
            }
            OutlinedButton(onClick = { onReturn(item.instanceId) }) { Text(stringResource(R.string.shell_return_to_chest)) }
        }
    }
}

@Composable
private fun ShellChestScreen(uiState: ShellUiState, onPlaceInFocus: () -> Unit) {
    var category by remember { mutableStateOf<ShellFindCategory?>(null) }
    val items = uiState.finds.filter { category == null || ShellContentCatalog.find(it.findId)?.category == category }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { RoomHeader(R.string.shell_chest_title, R.string.shell_chest_body) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text(stringResource(R.string.shell_filter_all)) })
                ShellFindCategory.entries.forEach { cat -> FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(stringResource(categoryLabelFor(cat))) }) }
            }
        }
        items(items) { instance ->
            val def = ShellContentCatalog.find(instance.findId) ?: return@items
            ElevatedCard(onClick = { if (def.primaryRoomId == ShellRoomId.FOCUS) onPlaceInFocus() }, modifier = Modifier.semantics { contentDescription = stringResource(def.titleRes) }) {
                ListItem(
                    leadingContent = { Icon(iconFor(def.category), contentDescription = null) },
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = { Text(stringResource(R.string.shell_chest_item_status, stringResource(categoryLabelFor(def.category)), if (uiState.focusPlacements.any { it.instanceId == instance.instanceId }) stringResource(R.string.shell_status_placed) else stringResource(R.string.shell_status_unplaced))) }
                )
            }
        }
        items(uiState.stacks) { stack ->
            val def = ShellContentCatalog.find(stack.findId) ?: return@items
            ElevatedCard { ListItem(headlineContent = { Text(stringResource(def.titleRes)) }, supportingContent = { Text(stringResource(R.string.shell_stack_quantity, stack.quantity)) }) }
        }
    }
}

@Composable
private fun BadgesScreen(uiState: ShellUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { RoomHeader(R.string.shell_badges_title, R.string.shell_badges_body) }
        items(uiState.badges) { badge ->
            val def = ShellContentCatalog.badge(badge.badgeId) ?: return@items
            ElevatedCard(modifier = Modifier.semantics { contentDescription = stringResource(R.string.shell_badge_a11y, stringResource(def.titleRes), badge.count) }) {
                ListItem(leadingContent = { Icon(Icons.Outlined.MilitaryTech, contentDescription = null) }, headlineContent = { Text(stringResource(def.titleRes)) }, supportingContent = { Text(stringResource(def.descriptionRes, badge.count)) })
            }
        }
    }
}

@Composable
private fun DiscoveryJournalScreen(uiState: ShellUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { RoomHeader(R.string.shell_journal_title, R.string.shell_journal_body) }
        items(uiState.discoveries) { discovery ->
            val def = ShellContentCatalog.discovery(discovery.discoveryId) ?: return@items
            ElevatedCard(modifier = Modifier.semantics { contentDescription = stringResource(def.titleRes) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(def.titleRes), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(def.explanationRes))
                }
            }
        }
    }
}

@Composable
private fun StillwaterRoomScreen(uiState: ShellUiState, onPerspective: (StillwaterPerspective) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoomHeader(R.string.shell_room_stillwater_title, R.string.shell_stillwater_body)
        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
            Box(Modifier.fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color(0xFF0E4E68), Color(0xFF041B2B))))) {
                Canvas(Modifier.matchParentSize()) {
                    repeat(6) { i -> drawCircle(Color(0xFF99F6E4).copy(.18f), radius = 44f + i * 18f, center = Offset(size.width / 2, size.height / 2), style = Stroke(3f)) }
                }
                Text(displayStillwater(uiState.stillwaterTotal, uiState.perspective), color = Color.White, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(24.dp))
            }
        }
        Text(stringResource(R.string.shell_view_as), color = MaterialTheme.colorScheme.onPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).semantics { contentDescription = stringResource(R.string.shell_stillwater_selector_a11y) }) {
            StillwaterPerspective.entries.forEach { perspective ->
                FilterChip(selected = uiState.perspective == perspective, onClick = { onPerspective(perspective) }, label = { Text(stringResource(labelFor(perspective))) })
            }
        }
        Text(stringResource(R.string.shell_soft_flow_copy), color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun DormantPreviewScreen(titleRes: Int, bodyRes: Int) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
        Text(stringResource(bodyRes), color = Color.White.copy(.86f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoomHeader(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        Text(stringResource(body), color = MaterialTheme.colorScheme.onPrimary.copy(.86f))
    }
}

@Composable
private fun AmbientBubbles(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        repeat(16) { i -> drawCircle(Color.White.copy(alpha = .10f + (i % 3) * .03f), radius = 5f + (i % 4) * 3f, center = Offset((size.width * ((i * 37) % 100) / 100f), (size.height * ((i * 53) % 100) / 100f))) }
        val path = Path().apply { moveTo(0f, size.height * .7f); cubicTo(size.width * .25f, size.height * .55f, size.width * .6f, size.height * .85f, size.width, size.height * .68f) }
        drawPath(path, Color(0xFFB8FFF4).copy(.22f), style = Stroke(5f))
    }
}

@Composable private fun shellBackground() = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF062238), Color(0xFF03131F)))
private fun shellChamberBrush() = Brush.radialGradient(listOf(Color(0xFF2DD4BF).copy(.75f), Color(0xFF0F516E), Color(0xFF062238)))
private fun iconFor(category: ShellFindCategory): ImageVector = when (category) { ShellFindCategory.CREATURES -> Icons.Outlined.Pets; ShellFindCategory.SHELLS -> Icons.Outlined.Spa; ShellFindCategory.CORAL -> Icons.Outlined.FilterVintage; ShellFindCategory.PLANTS -> Icons.Outlined.Grass; ShellFindCategory.TROPHIES -> Icons.Outlined.EmojiEvents; ShellFindCategory.TRINKETS -> Icons.Outlined.Diamond; ShellFindCategory.DISCOVERIES -> Icons.Outlined.AutoStories }
private fun categoryLabelFor(category: ShellFindCategory): Int = when (category) { ShellFindCategory.CREATURES -> R.string.shell_category_creatures; ShellFindCategory.SHELLS -> R.string.shell_category_shells; ShellFindCategory.CORAL -> R.string.shell_category_coral; ShellFindCategory.PLANTS -> R.string.shell_category_plants; ShellFindCategory.TROPHIES -> R.string.shell_category_trophies; ShellFindCategory.TRINKETS -> R.string.shell_category_trinkets; ShellFindCategory.DISCOVERIES -> R.string.shell_category_discoveries }
private fun labelFor(p: StillwaterPerspective): Int = when (p) { StillwaterPerspective.CUPS -> R.string.shell_perspective_cups; StillwaterPerspective.BOWLS -> R.string.shell_perspective_bowls; StillwaterPerspective.TANK -> R.string.shell_perspective_tank; StillwaterPerspective.POOL -> R.string.shell_perspective_pool; StillwaterPerspective.LAKE -> R.string.shell_perspective_lake; StillwaterPerspective.LAKE_TAHOE_PERCENT -> R.string.shell_perspective_tahoe; StillwaterPerspective.WORLD_OCEAN_PERCENT -> R.string.shell_perspective_ocean; StillwaterPerspective.STREAM_TIME -> R.string.shell_perspective_stream }

@Composable
private fun displayStillwater(units: Long, p: StillwaterPerspective): String = when (p) {
    StillwaterPerspective.CUPS -> stringResource(R.string.shell_stillwater_cups, units / 2.0)
    StillwaterPerspective.BOWLS -> stringResource(R.string.shell_stillwater_bowls, units / 10.0)
    StillwaterPerspective.TANK -> stringResource(R.string.shell_stillwater_tank, units / 600.0)
    StillwaterPerspective.POOL -> stringResource(R.string.shell_stillwater_pool, units / 20_000.0)
    StillwaterPerspective.LAKE -> stringResource(R.string.shell_stillwater_lake, units / 2_000_000.0)
    StillwaterPerspective.LAKE_TAHOE_PERCENT -> stringResource(R.string.shell_stillwater_tahoe, units / 39_000_000_000.0 * 100.0)
    StillwaterPerspective.WORLD_OCEAN_PERCENT -> stringResource(R.string.shell_stillwater_ocean, units / 1_350_000_000_000_000.0 * 100.0)
    StillwaterPerspective.STREAM_TIME -> stringResource(R.string.shell_stillwater_stream, units / 10L)
}
