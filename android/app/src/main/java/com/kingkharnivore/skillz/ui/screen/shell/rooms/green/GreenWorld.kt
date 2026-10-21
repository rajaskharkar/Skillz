package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.green.PlantSpecimenEntity
import com.kingkharnivore.skillz.domain.green.*

/** Each environment has one automatically fitted plot; vertical gestures change environments. */
@Composable
internal fun GreenWorld(environment: GreenEnvironment, plants: List<PlantSpecimenEntity>,
    onEnvironment: (GreenEnvironment) -> Unit, onSelect: (String) -> Unit,
    onPlant: () -> Unit, onOpenChest: () -> Unit,
    topOverlayHeight: Dp, compact: Boolean = false, controlsReady: Boolean = true) {
    val pager = rememberPagerState(initialPage = environment.ordinal, pageCount = { GreenEnvironment.entries.size })
    var lastReported by remember { mutableIntStateOf(environment.ordinal) }
    LaunchedEffect(environment) {
        if (environment.ordinal != lastReported) { pager.scrollToPage(environment.ordinal); lastReported = environment.ordinal }
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage != lastReported) { lastReported = pager.settledPage; onEnvironment(GreenEnvironment.entries[pager.settledPage]) }
    }
    VerticalPager(state = pager, modifier = Modifier.fillMaxSize().clipToBounds().testTag("green-environments"), key = { GreenEnvironment.entries[it].id }) { page ->
        val env = GreenEnvironment.entries[page]
        val specimens = remember(plants, env) { plants.filter { GreenCatalogue.byId[it.speciesId]?.environment == env }.sortedBy { it.createdOrder } }
        val density = LocalDensity.current
        val labels = GreenEnvironment.entries.map { stringResource(it.nameRes) }
        val labelStyle = MaterialTheme.typography.labelSmall
        val textMeasurer = rememberTextMeasurer()
        val labelWidth = remember(labels, labelStyle, density) {
            with(density) { labels.maxOf { textMeasurer.measure(it, style = labelStyle, softWrap = false).size.width }.toDp() }
        }
        var footerPixels by remember { mutableIntStateOf(0) }
        val footerHeight = with(density) { footerPixels.toDp() }
        val dark = MaterialTheme.colorScheme.surface.luminance() < .3f
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val groundLine = if (maxHeight > 0.dp) ((topOverlayHeight + 12.dp) / maxHeight).coerceIn(.08f, .34f) else .34f
            Canvas(Modifier.matchParentSize()) { drawGreenLandscape(env, dark, groundLine = groundLine) }
            val railWidth = (labelWidth + 16.dp).coerceAtLeast(86.dp).coerceAtMost(maxWidth * .45f)
            val safeHeight = (maxHeight - topOverlayHeight - footerHeight - 24.dp).coerceAtLeast(0.dp)
            Box(Modifier.fillMaxSize().padding(top = topOverlayHeight + 12.dp, bottom = footerHeight + 12.dp)) {
                Box(Modifier.fillMaxSize().padding(start = 12.dp, end = railWidth + 20.dp)
                    .testTag("green-specimens:${env.id}")) {
                    // Wait for both overlays to be measured: no frame can plant underneath them.
                    if (controlsReady && footerPixels > 0) {
                        GreenEnvironmentScene(env, specimens, onSelect, Modifier.fillMaxSize(), drawBackground = false)
                    }
                }
                GreenEnvironmentRail(env, onEnvironment, railWidth,
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).heightIn(max = safeHeight).testTag("green-rail:${env.id}"))
            }
            GreenSceneOverlay(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { footerPixels = it.height }.padding(12.dp).testTag("green-footer:${env.id}")) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!compact) GreenPopulationSummary(env, specimens)
                    if (compact) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onPlant) { Text(stringResource(R.string.green_choose_seed)) }
                            TextButton(onClick = onOpenChest) { Text(stringResource(R.string.green_open_chest)) }
                        }
                    } else {
                        TextButton(onClick = onPlant) { Text(stringResource(R.string.green_choose_seed)) }
                        TextButton(onClick = onOpenChest, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.green_open_chest), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GreenSceneOverlay(modifier: Modifier=Modifier, content: @Composable () -> Unit) {
    Surface(modifier,shape=MaterialTheme.shapes.large,
        color=MaterialTheme.colorScheme.surface.copy(alpha=.9f),tonalElevation=1.dp) {
        Box(Modifier.padding(12.dp)) {content()}
    }
}

/** Refit all specimens whenever the population, growth footprints, or available view changes. */
@Composable
fun GreenEnvironmentScene(environment: GreenEnvironment, plants: List<PlantSpecimenEntity>, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, drawBackground: Boolean = true) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .3f
    val calm = LocalGreenCalmMode.current
    BoxWithConstraints(modifier.clipToBounds()) {
        if (drawBackground) Canvas(Modifier.matchParentSize()) { drawGreenLandscape(environment, dark) }
        val placements = remember(plants, maxWidth, maxHeight) {
            layoutGreenPlants(plants, maxWidth.value, maxHeight.value)
        }
        // A resize must fit immediately, including while a previous reflow is running.
        key(environment, maxWidth, maxHeight) {
            GreenPlantArrangement(placements, plants, calm, onSelect)
        }
    }
}

@Composable
private fun GreenPlantArrangement(target: List<GreenPlantPlacement>, plants: List<PlantSpecimenEntity>,
    calm: Boolean, onSelect: (String) -> Unit) {
    var displayed by remember { mutableStateOf(target) }
    val opacity = remember { Animatable(1f) }
    val plantsById = remember(plants) { plants.associateBy { it.id } }
    LaunchedEffect(target, calm) {
        if (calm) {
            displayed = target
            opacity.snapTo(1f)
        } else {
            if (displayed != target) {
                // Fade between compositions; organic foliage overlap is intentional.
                // Rapid updates cancel and adopt the latest fit.
                opacity.animateTo(0f, tween(120))
                displayed = target
            }
            opacity.animateTo(1f, tween(180))
        }
    }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val latestOnSelect by rememberUpdatedState(onSelect)
    val opaque by remember { derivedStateOf { !opacity.isRunning && opacity.value == 1f } }
    val selectable = displayed == target && opaque
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity.value }
        .pointerInput(displayed, selectable, density, direction) {
            detectTapGestures { point ->
                if (selectable) {
                    val x = if (direction == LayoutDirection.Rtl) size.width - point.x else point.x
                    pickGreenPlant(displayed, x / density.density, point.y / density.density)?.let(latestOnSelect)
                }
            }
        }) {
        displayed.forEach { placement ->
            val plant = plantsById[placement.plantId] ?: return@forEach
            key(plant.id) {
                val species = GreenCatalogue.byId.getValue(plant.speciesId)
                val extent = placement.artworkHeight.dp
                val name = stringResource(species.nameRes)
                val inspect = stringResource(R.string.green_inspect, name)
                Box(Modifier.offset(x = placement.x.dp, y = placement.y.dp)
                    .width(placement.width.dp).height(placement.height.dp)
                    .testTag("green-plant:${plant.id}")
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        if (selectable) onClick(inspect) { latestOnSelect(plant.id); true } else disabled()
                    },
                    contentAlignment = Alignment.BottomCenter) {
                    PlantArtwork(species, plant.level, stringResource(R.string.green_plant_description, name, plant.level,
                        stringResource(PlantGrowthStageResolver.resolve(plant.level).nameRes)),
                        Modifier.padding(bottom = extent * .05f).width(extent * .82f).height(extent))
                }
            }
        }
    }
}

@Composable
internal fun GreenPopulationSummary(environment: GreenEnvironment, specimens: List<PlantSpecimenEntity>) {
    if (specimens.isEmpty()) Text(stringResource(R.string.green_empty), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.green_environment_progress,
        specimens.map { it.speciesId }.distinct().size,
        specimens.filter { it.level >= 90 }.map { it.speciesId }.distinct().size,
        specimens.filter { it.level == 99 }.map { it.speciesId }.distinct().size,
        GreenCatalogue.inEnvironment(environment).size, specimens.size), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun GreenEnvironmentRail(selected: GreenEnvironment, onSelect: (GreenEnvironment) -> Unit, width: Dp, modifier: Modifier) {
    Surface(modifier.width(width),shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface.copy(alpha=.78f)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal=4.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            GreenEnvironment.entries.forEach { env ->
                Surface(shape=MaterialTheme.shapes.medium,
                    color=if(selected==env) MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor=if(selected==env) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface) {
                    Box(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected==env,role=Role.Tab) {onSelect(env)}
                        .padding(horizontal=2.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
                        Text(stringResource(env.nameRes),style=MaterialTheme.typography.labelSmall,
                            textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}
