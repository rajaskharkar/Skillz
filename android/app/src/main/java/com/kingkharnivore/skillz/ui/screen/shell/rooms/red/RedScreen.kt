package com.kingkharnivore.skillz.ui.screen.shell.rooms.red

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet
import kotlinx.coroutines.launch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.RedUiState
import com.kingkharnivore.skillz.viewmodel.shell.RedViewModel
import java.util.UUID

@Composable
fun RedRoute(onBadges: () -> Unit, onTrade: () -> Unit, initialCreature: String? = null,
    onCreatureOpened: () -> Unit = {}, viewModel: RedViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastEra by viewModel.lastEra.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val messageText = message?.let { stringResource(it) }
    LaunchedEffect(messageText) { messageText?.let { snack.showSnackbar(it); viewModel.clearMessage() } }
    Box(Modifier.fillMaxSize()) {
        RedScreen(state, busy, onBadges, onTrade, viewModel::purchase, viewModel::grow, viewModel::release,
            initialEra = initialCreature?.let { RedCreatureCatalog.byId[it]?.era } ?: RedEra.entries.firstOrNull { it.name == lastEra },
            initialCreature = initialCreature, onEraChanged = viewModel::selectEra, onInitialCreatureOpened = onCreatureOpened)
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedScreen(
    state: RedUiState, busy: Boolean = false, onBadges: () -> Unit = {}, onTrade: () -> Unit = {},
    onPurchase: (String, String) -> Unit = { _, _ -> }, onGrow: (String, Int) -> Unit = { _, _ -> },
    onRelease: (String) -> Unit = {}, initialEra: RedEra? = null, initialCatalog: Boolean = false,
    initialCreature: String? = null, simulatedSceneTime: Float? = null, initialPageOffsetFraction: Float = 0f,
    onEraChanged: (RedEra) -> Unit = {}, onInitialCreatureOpened: () -> Unit = {}
) {
    val pager = rememberPagerState(initialPage = (initialEra ?: RedEra.TRIASSIC).ordinal,initialPageOffsetFraction=initialPageOffsetFraction) { RedEra.entries.size }
    val scope = rememberCoroutineScope()
    val eraChanged by rememberUpdatedState(onEraChanged)
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { eraChanged(RedEra.entries[it]) } }
    val totalOwned = remember(state.creatures) { state.creatures.filter { it.creatureStatus == CreatureStatus.ACTIVE && it.findId in RedCreatureCatalog.byId }.map { it.findId }.distinct().size }
    var catalog by rememberSaveable { mutableStateOf(initialCatalog) }
    var selected by rememberSaveable { mutableStateOf(initialCreature) }
    var returnToCatalog by rememberSaveable { mutableStateOf(false) }
    val creatureOpened by rememberUpdatedState(onInitialCreatureOpened)
    LaunchedEffect(initialCreature) {
        initialCreature?.let { id ->
            RedCreatureCatalog.byId[id]?.let { entry ->
                catalog = false
                returnToCatalog = false
                selected = id
                pager.scrollToPage(entry.era.ordinal)
            }
            creatureOpened()
        }
    }
    val time = rememberRedSceneClock(simulatedSceneTime, !catalog && selected == null)
    Box(Modifier.fillMaxSize()) {
        // Each of the three static regions keeps its own bounded texture. Swiping only
        // translates these layers; it does not redraw the geological paths or seam masks.
        RedEra.entries.forEachIndexed { index, era ->
            RedTerrain(era,Modifier.matchParentSize().graphicsLayer {
                translationX = (index - pager.currentPage - pager.currentPageOffsetFraction) * size.width
                clip = true
            })
        }
        RedAtmosphere({ time.value },Modifier.matchParentSize().graphicsLayer())
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth().testTag("red-era-pager"), pageSpacing = 0.dp) { page ->
                RedEraPage(RedEra.entries[page],state,{ time.value },
                    onCatalog = { catalog = true },
                    onCreature = { selected = it; returnToCatalog = false },onBadges = onBadges)
            }
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .92f)) {
                Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        RedEra.entries.forEachIndexed { index,era ->
                            val chosen=pager.currentPage==index
                            Surface(onClick={scope.launch { pager.animateScrollToPage(index) }},
                                modifier=Modifier.weight(1f).fillMaxHeight().testTag("red-era-${era.name}").semantics { this.selected=chosen; role=Role.Tab },
                                shape=MaterialTheme.shapes.medium,
                                color=if(chosen) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                                contentColor=if(chosen) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface) {
                                Text(stringResource(era.titleRes),Modifier.padding(horizontal=4.dp,vertical=10.dp),
                                    style=MaterialTheme.typography.labelMedium,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    Text(stringResource(R.string.red_total_progress,totalOwned,RedCreatureCatalog.entries.size),Modifier.align(Alignment.CenterHorizontally).testTag("red-total-progress"),style=MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.red_swipe_eras),Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if(catalog) BeyondRedSheet(state,RedEra.entries[pager.currentPage],
        onDismiss={catalog=false},onSelect={selected=it;catalog=false;returnToCatalog=true})
    selected?.let { id -> RedCreatureCatalog.byId[id]?.let { creature ->
        RedCreatureDetail(creature,state,busy,{selected=null;if(returnToCatalog) catalog=true},onPurchase,onGrow,onRelease,
            onTrade={catalog=false;returnToCatalog=false;selected=null;onTrade()})
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeyondRedSheet(state: RedUiState, initialEra: RedEra, onDismiss: ()->Unit, onSelect: (String)->Unit) {
    var era by remember { mutableStateOf(initialEra) }
    val list = rememberLazyListState()
    LaunchedEffect(era) { list.scrollToItem(0) }
    ScyraParchmentSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        LazyColumn(state=list,modifier=Modifier.fillMaxWidth().testTag("red-catalog-sheet"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.red_catalog),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Text(stringResource(R.string.red_catalog_subtitle))
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        PebbleIcon(Modifier.size(24.dp));Text(pluralStringResource(R.plurals.red_balance,state.pebbles,state.pebbles))
                    }
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        RedEra.entries.forEach { option -> FilterChip(selected=era==option,onClick={era=option},label={Text(stringResource(option.titleRes))}) }
                    }
                }
            }
            items(RedCreatureCatalog.entries.filter { it.era==era },key={it.id}) { creature ->
                val copies=state.creatures.filter { it.findId==creature.id && it.creatureStatus==CreatureStatus.ACTIVE }
                ElevatedCard(onClick={onSelect(creature.id)},colors=CardDefaults.elevatedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        RedDinosaurIcon(creature.id,Modifier.size(76.dp))
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(creature.nameRes),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            Text(pluralStringResource(R.plurals.red_price,creature.pebbleCost,creature.pebbleCost))
                            Text(when {
                                (state.masteries[creature.id] ?: 0)>0 || copies.any { it.animalLevel>=99 } -> stringResource(R.string.red_mastered)
                                copies.isNotEmpty() -> stringResource(R.string.red_owned,copies.size,copies.maxOf { it.animalLevel })
                                state.pebbles>=creature.pebbleCost -> stringResource(R.string.red_affordable)
                                else -> stringResource(R.string.red_insufficient)
                            },style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedCreatureDetail(creature: RedCatalogEntry, state: RedUiState, busy: Boolean, onDismiss: () -> Unit,
    onPurchase: (String, String) -> Unit, onGrow: (String, Int) -> Unit, onRelease: (String) -> Unit, onTrade: () -> Unit) {
    val copies = state.creatures.filter { it.findId == creature.id }
    val active = copies.filter { it.creatureStatus == CreatureStatus.ACTIVE }
    val growth = active.filter { it.animalLevel < 99 }.maxByOrNull { it.animalLevel }
    val masteryCount = maxOf(state.masteries[creature.id] ?: 0, copies.count { it.animalLevel >= 99 })
    var action by remember { mutableStateOf<String?>(null) }
    var purchaseId by rememberSaveable(creature.id) { mutableStateOf(UUID.randomUUID().toString()) }
    LaunchedEffect(copies.size) { purchaseId = UUID.randomUUID().toString() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RedDinosaurIcon(creature.id,Modifier.align(Alignment.CenterHorizontally).size(150.dp))
            Text(stringResource(creature.nameRes), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(creature.era.titleRes), color = MaterialTheme.colorScheme.tertiary)
            Text(pluralStringResource(R.plurals.red_price, creature.pebbleCost, creature.pebbleCost))
            if (active.isNotEmpty()) Text(stringResource(R.string.red_owned, active.size, active.maxOf { it.animalLevel }))
            else if (copies.isNotEmpty()) Text(stringResource(R.string.collection_species_discovered_not_owned))
            if (copies.isNotEmpty()) Text(pluralStringResource(R.plurals.red_lifetime, copies.size, copies.size))
            if (masteryCount > 0) Text(stringResource(R.string.red_mastered), color = MaterialTheme.colorScheme.tertiary)
            Text(stringResource(R.string.red_progression_help))
            Button(enabled = !busy && state.pebbles >= creature.pebbleCost, onClick = { action = "purchase" }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.pebbles < creature.pebbleCost) R.string.red_insufficient else if (copies.isEmpty()) R.string.red_purchase else R.string.red_purchase_copy))
            }
            growth?.let { instance ->
                val cost = CreatureEconomy.growthCostPearls(creature.id, instance.animalLevel)
                OutlinedButton(enabled = !busy && state.pearls >= cost, onClick = { action = "grow" }, modifier = Modifier.fillMaxWidth()) { Text(pluralStringResource(R.plurals.red_grow, cost, cost)) }
            }
            active.firstOrNull()?.let { instance ->
                OutlinedButton(enabled = !busy, onClick = { action = "release" }, modifier = Modifier.fillMaxWidth()) { Text(pluralStringResource(R.plurals.red_release, CreatureEconomy.releaseValuePearls(creature.id, instance.animalLevel), CreatureEconomy.releaseValuePearls(creature.id, instance.animalLevel))) }
                TextButton(onClick = { onDismiss(); onTrade() }) { Text(stringResource(R.string.red_trade)) }
            }
        }
    }
    action?.let { pending ->
        val amount = when(pending) { "grow" -> growth?.let { CreatureEconomy.growthCostPearls(creature.id, it.animalLevel) } ?: 0; "release" -> active.firstOrNull()?.let { CreatureEconomy.releaseValuePearls(creature.id, it.animalLevel) } ?: 0; else -> creature.pebbleCost }
        AlertDialog(onDismissRequest = { action = null }, title = { Text(stringResource(creature.nameRes)) },
            text = { Text(pluralStringResource(when(pending) { "grow" -> R.plurals.red_grow; "release" -> R.plurals.red_release; else -> R.plurals.red_price }, amount, amount)) },
            confirmButton = { TextButton(onClick = {
                when(pending) { "purchase" -> { onPurchase(creature.id, purchaseId) }; "grow" -> growth?.let { onGrow(it.instanceId, it.animalLevel) }; "release" -> active.firstOrNull()?.let { onRelease(it.instanceId) } }
                action = null
            }) { Text(stringResource(R.string.red_confirm)) } },
            dismissButton = { TextButton(onClick = { action = null }) { Text(stringResource(R.string.common_cancel)) } })
    }
}

@Composable
fun PebbleIcon(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val stone = Path().apply { moveTo(size.width * .08f,size.height*.65f); lineTo(size.width*.24f,size.height*.2f); lineTo(size.width*.68f,size.height*.12f); lineTo(size.width*.94f,size.height*.52f); lineTo(size.width*.8f,size.height*.83f); lineTo(size.width*.3f,size.height*.92f); close() }
        drawPath(stone, ink)
        drawLine(Color.White.copy(alpha=.45f),Offset(size.width*.28f,size.height*.4f),Offset(size.width*.6f,size.height*.3f),size.width*.06f)
    }
}
