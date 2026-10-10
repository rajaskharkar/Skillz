package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.domain.green.*
import com.kingkharnivore.skillz.viewmodel.green.*
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.delay

@Composable
fun GreenRoute(calmMode: Boolean = false, viewModel: GreenViewModel = hiltViewModel(),
    onOpenChest: () -> Unit = {}, onOpenBadges: () -> Unit = {},
    initialEnvironment: GreenEnvironment = GreenEnvironment.GARDEN, initialSeedId: String? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalGreenCalmMode provides calmMode) {
        GreenScreen(state, viewModel::plant, viewModel::water, { viewModel.dismissFeedback(state.feedbackId) }, viewModel::view,
            initialEnvironment = initialEnvironment, initialSeedId = initialSeedId,
            initialSection = if (initialSeedId == null) GreenSection.WORLD else GreenSection.CATALOGUE,
            onOpenChest = onOpenChest, onOpenBadges = onOpenBadges)
    }
}

enum class GreenSection(val titleRes: Int) {
    WORLD(R.string.green_world), CATALOGUE(R.string.green_catalogue)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GreenScreen(
    state: GreenUiState,
    onPlant: (String, String) -> Unit = { _, _ -> },
    onWater: (String, Int, String) -> Unit = { _, _, _ -> },
    onDismissFeedback: () -> Unit = {},
    onView: (String, GreenEnvironment?, String?) -> Unit = { _, _, _ -> },
    initialSection: GreenSection = GreenSection.WORLD,
    initialEnvironment: GreenEnvironment = GreenEnvironment.GARDEN,
    initialSpecimenId: String? = null,
    initialSeedId: String? = null,
    onOpenChest: () -> Unit = {}, onOpenBadges: () -> Unit = {}
) {
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var environment by rememberSaveable { mutableStateOf(initialEnvironment) }
    var seedId by rememberSaveable { mutableStateOf(initialSeedId) }
    var specimenId by rememberSaveable { mutableStateOf(initialSpecimenId) }
    var seedToken by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var help by rememberSaveable { mutableStateOf(false) }
    val plants = state.specimens.filter { GreenCatalogue.byId[it.speciesId]?.environment == environment }
    var controlsHeight by remember { mutableIntStateOf(0) }
    val controlsHeightDp = with(LocalDensity.current) { controlsHeight.toDp() }
    val snackbar = remember { SnackbarHostState() }
    val latestOnView by rememberUpdatedState(onView)
    LaunchedEffect(Unit) { latestOnView("green_opened", null, null) }
    LaunchedEffect(environment) { latestOnView("green_environment_opened", environment, null) }
    LaunchedEffect(section, environment) { if (section == GreenSection.CATALOGUE) latestOnView("green_catalogue_opened", environment, null) }
    LaunchedEffect(seedId) {
        seedId?.let(GreenCatalogue.byId::get)?.let { latestOnView("plant_seed_viewed", it.environment, it.id) }
    }
    val mastery = state.action?.takeIf { it.newLevel == 99 }?.let { action ->
        state.specimens.firstOrNull { it.id == action.specimenId }
    }
    val feedback = greenFeedbackText(state)
    val modalVisible = seedId != null || specimenId != null || help || mastery != null
    LaunchedEffect(state.action?.id, state.specimens.any { it.id == state.action?.specimenId }) {
        val action = state.action ?: return@LaunchedEffect
        if (action.kind == "plant_seed_planted" && state.specimens.any { it.id == action.specimenId }) {
            seedId = null; specimenId = action.specimenId
        }
    }
    LaunchedEffect(state.feedbackId, feedback, modalVisible) {
        if (!modalVisible && feedback != null) { snackbar.showSnackbar(feedback); onDismissFeedback() }
    }
    Box(Modifier.fillMaxSize()) {
        if (section == GreenSection.WORLD) {
            GreenWorld(environment, state.specimens, { environment = it }, { specimenId = it },
                { section = GreenSection.CATALOGUE }, onOpenChest, onOpenBadges, controlsHeightDp)
        }
        Column(Modifier.fillMaxSize()) {
            GreenSceneOverlay(Modifier.fillMaxWidth().onSizeChanged { controlsHeight = it.height }.padding(12.dp).testTag("green-controls")) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.green_title), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(environment.nameRes), style = MaterialTheme.typography.titleLarge)
                        }
                        Text(stringResource(R.string.green_drop_balance, state.drops), style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { help = true }) { Icon(Icons.Outlined.HelpOutline, stringResource(R.string.green_help_title)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GreenSection.entries.forEach { tab -> FilterChip(section == tab, { section = tab },
                            { Text(stringResource(tab.titleRes)) }, colors = greenPrimarySelectionColors()) }
                    }
                    if (section == GreenSection.CATALOGUE) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GreenEnvironment.entries.forEach { env -> FilterChip(environment == env, { environment = env },
                            { Text(stringResource(env.nameRes)) }, colors = greenSecondarySelectionColors()) }
                    }
                }
            }
            if (section == GreenSection.CATALOGUE) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(GreenCatalogue.inEnvironment(environment), key = { it.id }) { species ->
                        val copies = plants.filter { it.speciesId == species.id }
                        Card(onClick = { seedId = species.id; seedToken = UUID.randomUUID().toString() }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlantArtwork(species, 90, stringResource(species.nameRes), Modifier.size(80.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(species.nameRes), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.green_seed_price, GreenEconomy.seedCost(species.tier)))
                                    Text(if (copies.isEmpty()) stringResource(R.string.green_not_planted) else
                                        stringResource(R.string.green_owned, copies.size, copies.maxOf { it.level }))
                                    Text(stringResource(R.string.green_targets), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    seedId?.let { id -> GreenCatalogue.byId[id]?.let { species ->
        val cost = GreenEconomy.seedCost(species.tier)
        AlertDialog(onDismissRequest = { if (!state.busy) { seedId = null; onDismissFeedback() } }, title = { Text(stringResource(species.nameRes)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GreenFeedbackMessage(state)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlantArtwork(species, 90, stringResource(species.nameRes), Modifier.fillMaxWidth().height(180.dp))
                    Text(stringResource(species.environment.nameRes)); Text(stringResource(R.string.green_seed_price, cost))
                    Text(stringResource(R.string.green_targets)); Text(stringResource(R.string.green_duplicates))
                    if (state.drops < cost) Text(stringResource(R.string.green_more_drops, cost - state.drops))
                }
            }
        }, confirmButton = {
            Button(enabled = !state.busy && state.drops >= cost, onClick = { onPlant(id, seedToken) }) { Text(stringResource(R.string.green_plant_seed)) }
        }, dismissButton = { TextButton(onClick = { seedId = null; onDismissFeedback() }, enabled = !state.busy) { Text(stringResource(R.string.green_close)) } })
    } }
    if (mastery == null) specimenId?.let { id -> state.specimens.firstOrNull { it.id == id }?.let { plant ->
        SpecimenDialog(plant, state, onWater, { specimenId = null; onDismissFeedback() })
    } }
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text(stringResource(R.string.green_help_title)) }, text = {
        Text(stringResource(R.string.green_help_body), Modifier.verticalScroll(rememberScrollState()))
    }, confirmButton = { TextButton(onClick = { help = false }) { Text(stringResource(R.string.green_close)) } })
    mastery?.let { plant ->
        MasteryDialog(plant, state, {
            specimenId = null; seedId = null; section = GreenSection.WORLD
            environment = GreenCatalogue.byId.getValue(plant.speciesId).environment
            onDismissFeedback()
        })
    }
}

@Composable
internal fun EnvironmentSummary(environment: GreenEnvironment, plants: List<PlantSpecimenEntity>) {
    Text(stringResource(environment.nameRes), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.green_environment_progress,
        plants.map { it.speciesId }.distinct().size,
        plants.filter { it.level >= 90 }.map { it.speciesId }.distinct().size,
        plants.filter { it.level == 99 }.map { it.speciesId }.distinct().size,
        GreenCatalogue.inEnvironment(environment).size, plants.size))
}

@Composable
internal fun SpecimenCard(plant: PlantSpecimenEntity, onClick: () -> Unit) {
    val species = GreenCatalogue.byId[plant.speciesId] ?: return
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlantArtwork(species, plant.level, stringResource(species.nameRes), Modifier.size(72.dp))
            Column {
                Text(stringResource(species.nameRes), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.green_level_stage, plant.level, stringResource(PlantGrowthStageResolver.resolve(plant.level).nameRes)))
                Text(stringResource(R.string.green_planted_date, date(plant.plantedAt)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SpecimenDialog(plant: PlantSpecimenEntity, state: GreenUiState, onWater: (String, Int, String) -> Unit, onClose: () -> Unit) {
    val species = GreenCatalogue.byId[plant.speciesId] ?: return
    val stage = PlantGrowthStageResolver.resolve(plant.level)
    val token = remember(plant.id, plant.level) { UUID.randomUUID().toString() }
    val calmMode = LocalGreenCalmMode.current
    var wet by remember { mutableStateOf(false) }
    LaunchedEffect(plant.lastWateredAt) { if (plant.lastWateredAt != null && !calmMode) { wet = true; delay(650); wet = false } }
    val soil by animateColorAsState(if(wet) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, label = "water")
    AlertDialog(onDismissRequest = onClose, title = { Text(stringResource(species.nameRes)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GreenFeedbackMessage(state, plant.id)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlantArtwork(species, plant.level, stringResource(R.string.green_plant_description, stringResource(species.nameRes), plant.level, stringResource(stage.nameRes)),
                    Modifier.fillMaxWidth().height(if(species.landmark && plant.level >= 90) 230.dp else 180.dp).clip(RoundedCornerShape(20.dp)).background(soil))
                Text(stringResource(R.string.green_level_stage, plant.level, stringResource(stage.nameRes)), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleMedium)
                if (plant.level == 99) Text(stringResource(R.string.green_stage_fully_grown))
                PlantGrowthStageResolver.nextVisibleGrowth(plant.level)?.let { Text(stringResource(R.string.green_next_growth, it)) }
                LinearProgressIndicator(progress = { plant.level / 99f }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.green_mastery_progress, plant.level))
                PlantDates(plant)
                val copies = state.specimens.filter { it.speciesId == plant.speciesId }
                Text(stringResource(R.string.green_species_stats, copies.size, copies.count { it.level >= 90 },
                    copies.count { it.level == 99 }, copies.maxOfOrNull { it.level } ?: plant.level))
                if(plant.level < 99) {
                    val cost = GreenEconomy.waterCost(species.tier, plant.level)
                    Text(stringResource(R.string.green_water_price, cost))
                    if(state.drops < cost) Text(stringResource(R.string.green_more_drops, cost-state.drops))
                }
            }
        }
    }, confirmButton = {
        if(plant.level < 99) Button(enabled = !state.busy && state.drops >= GreenEconomy.waterCost(species.tier, plant.level),
            onClick = { onWater(plant.id, plant.level, token) }) { Text(stringResource(R.string.green_water)) }
    }, dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.green_close)) } })
}

@Composable
private fun PlantDates(plant: PlantSpecimenEntity) {
    Text(stringResource(R.string.green_planted_date, date(plant.plantedAt)))
    plant.fullyGrownAt?.let { Text(stringResource(R.string.green_full_date, date(it))) }
    plant.masteredAt?.let { Text(stringResource(R.string.green_mastered_date, date(it))) }
    Text(stringResource(R.string.green_invested, plant.investedDrops))
}
private fun date(time: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))

@Composable
internal fun MasteryDialog(plant: PlantSpecimenEntity, state: GreenUiState, onClose: () -> Unit, returnLabel: Int = R.string.green_return) {
    val species = GreenCatalogue.byId.getValue(plant.speciesId)
    var page by rememberSaveable(plant.id) { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = onClose, title = { Text(stringResource(if(page == 2) R.string.green_mastery_badge else R.string.green_stage_mastered)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if(page == 0) PlantArtwork(species, 99, stringResource(species.nameRes), Modifier.fillMaxWidth().height(240.dp))
            Text(stringResource(species.nameRes), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.green_mastered_full))
            if(page == 1) PlantDates(plant)
            if(page == 2) {
                val definition = GreenBadgeEvaluator.byId.getValue("green_v1_species_${species.id}_mastery")
                com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgeMedallion(
                    definition.dashboardModel(state.specimens, state.awards.firstOrNull { it.id == definition.id }, null, null, false),
                    com.kingkharnivore.skillz.ui.screen.shell.inventory.BadgeMedallionSize.Large)
                Text(stringResource(R.string.green_species_mastery_count, stringResource(species.nameRes), state.specimens.count { it.speciesId == species.id && it.level == 99 }))
            }
        }
    }, confirmButton = { Button(onClick = { if(page < 2) page++ else onClose() }) { Text(stringResource(if(page < 2) R.string.green_continue else returnLabel)) } })
}

@Composable
fun greenPrimarySelectionColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary)

@Composable
fun greenSecondarySelectionColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.secondary,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary)
