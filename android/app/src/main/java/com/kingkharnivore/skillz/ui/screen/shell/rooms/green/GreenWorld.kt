package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.green.PlantSpecimenEntity
import com.kingkharnivore.skillz.domain.green.*

/** Vertical gestures always change environments; extra planting plots use horizontal paging. */
@Composable
internal fun GreenWorld(environment: GreenEnvironment, plants: List<PlantSpecimenEntity>,
    onEnvironment: (GreenEnvironment) -> Unit, onSelect: (String) -> Unit,
    onPlant: () -> Unit, onOpenChest: () -> Unit, onOpenBadges: () -> Unit,
    topOverlayHeight: Dp) {
    val pager = rememberPagerState(initialPage = environment.ordinal, pageCount = { GreenEnvironment.entries.size })
    var lastReported by remember { mutableIntStateOf(environment.ordinal) }
    LaunchedEffect(environment) {
        if (environment.ordinal != lastReported) { pager.scrollToPage(environment.ordinal); lastReported = environment.ordinal }
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage != lastReported) { lastReported = pager.settledPage; onEnvironment(GreenEnvironment.entries[pager.settledPage]) }
    }
    VerticalPager(state = pager, modifier = Modifier.fillMaxSize().testTag("green-environments"), key = { GreenEnvironment.entries[it].id }) { page ->
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
            Canvas(Modifier.matchParentSize()) { drawGreenLandscape(env, dark) }
            val railWidth = (labelWidth + 16.dp).coerceAtLeast(86.dp).coerceAtMost(maxWidth * .45f)
            val safeHeight = (maxHeight - topOverlayHeight - footerHeight - 24.dp).coerceAtLeast(0.dp)
            val rows = (safeHeight / 120.dp).toInt().coerceIn(1, 3)
            val plots = specimens.chunked(rows * 2).ifEmpty { listOf(emptyList()) }
            val plotPager = rememberPagerState(pageCount = { plots.size })
            val scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize().padding(top = topOverlayHeight + 12.dp, bottom = footerHeight + 12.dp)) {
                HorizontalPager(state = plotPager,
                    modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = railWidth + 20.dp).testTag("green-specimens:${env.id}")) { plot ->
                    // Wait for both overlays to be measured: no frame can plant underneath them.
                    if (topOverlayHeight > 0.dp && footerPixels > 0) {
                        GreenEnvironmentScene(env, plots[plot], onSelect, Modifier.fillMaxSize(), rows = rows, drawBackground = false)
                    }
                }
                GreenEnvironmentRail(env, onEnvironment, railWidth,
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).heightIn(max = safeHeight).testTag("green-rail:${env.id}"))
            }
            GreenSceneOverlay(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .onSizeChanged { footerPixels = it.height }.padding(12.dp).testTag("green-footer:${env.id}")) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (specimens.isEmpty()) Text(stringResource(R.string.green_empty), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.green_environment_progress,
                        specimens.map { it.speciesId }.distinct().size,
                        specimens.filter { it.level >= 90 }.map { it.speciesId }.distinct().size,
                        specimens.filter { it.level == 99 }.map { it.speciesId }.distinct().size,
                        GreenCatalogue.inEnvironment(env).size, specimens.size), style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onPlant) { Text(stringResource(R.string.green_choose_seed)) }
                        Spacer(Modifier.weight(1f))
                        if (plots.size > 1) {
                            IconButton(onClick = { scope.launch { plotPager.animateScrollToPage(plotPager.currentPage - 1) } }, enabled = plotPager.currentPage > 0) {
                                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.green_previous_plot))
                            }
                            Text(stringResource(R.string.green_plot_position, plotPager.currentPage + 1, plots.size), style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = { scope.launch { plotPager.animateScrollToPage(plotPager.currentPage + 1) } }, enabled = plotPager.currentPage < plots.lastIndex) {
                                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.green_next_plot))
                            }
                        }
                    }
                    Row {
                        TextButton(onClick = onOpenChest, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.green_open_chest), style = MaterialTheme.typography.labelMedium) }
                        TextButton(onClick = onOpenBadges, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.green_open_badges), style = MaterialTheme.typography.labelMedium) }
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

/** Each complete plant fits in a cell entirely outside the measured text and navigation zones. */
@Composable
fun GreenEnvironmentScene(environment: GreenEnvironment, plants: List<PlantSpecimenEntity>, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, rows: Int = 3, drawBackground: Boolean = true) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .3f
    BoxWithConstraints(modifier) {
        if (drawBackground) Canvas(Modifier.matchParentSize()) { drawGreenLandscape(environment, dark) }
        val cellWidth = maxWidth / 2
        val cellHeight = maxHeight / rows
        plants.forEachIndexed { index, plant ->
            val species = GreenCatalogue.byId[plant.speciesId] ?: return@forEachIndexed
            val naturalExtent = when (species.footprint(plant.level)) {
                PlantFootprint.TINY -> 70.dp; PlantFootprint.SMALL -> 88.dp; PlantFootprint.MEDIUM -> 110.dp
                PlantFootprint.LARGE -> 136.dp; PlantFootprint.XL -> 155.dp; PlantFootprint.LANDMARK -> 180.dp
            }
            val extent = minOf(naturalExtent, cellHeight - 8.dp, cellWidth / .82f).coerceAtLeast(0.dp)
            val name = stringResource(species.nameRes)
            Box(Modifier.offset(x = cellWidth * (index % 2), y = cellHeight * (index / 2))
                .width(cellWidth).height(cellHeight).testTag("green-plant:${plant.id}")
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.green_inspect, name)) { onSelect(plant.id) },
                contentAlignment = Alignment.BottomCenter) {
                PlantArtwork(species, plant.level, stringResource(R.string.green_plant_description, name, plant.level,
                    stringResource(PlantGrowthStageResolver.resolve(plant.level).nameRes)),
                    Modifier.padding(bottom = 4.dp).width(extent * .82f).height(extent))
            }
        }
    }
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
