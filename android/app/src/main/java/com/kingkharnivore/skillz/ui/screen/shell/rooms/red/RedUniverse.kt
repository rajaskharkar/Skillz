package com.kingkharnivore.skillz.ui.screen.shell.rooms.red

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.TheBlueOverlaySurface
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.RedUiState

@Composable
internal fun rememberRedSceneClock(simulatedTime: Float?, enabled: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(simulatedTime ?: 0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle,simulatedTime,enabled) {
        if(simulatedTime != null) time.floatValue = simulatedTime
        else if(enabled) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val start = withFrameNanos { it }
            val previous = time.floatValue
            while(true) withFrameNanos { time.floatValue = previous + (it-start)/1_000_000_000f }
        }
    }
    return time
}

internal val RedEra.ageRes: Int get() = when(this) {
    RedEra.TRIASSIC -> R.string.red_triassic_age
    RedEra.JURASSIC -> R.string.red_jurassic_age
    RedEra.CRETACEOUS -> R.string.red_cretaceous_age
}
internal val RedEra.worldRes: Int get() = when(this) {
    RedEra.TRIASSIC -> R.string.red_triassic_world
    RedEra.JURASSIC -> R.string.red_jurassic_world
    RedEra.CRETACEOUS -> R.string.red_cretaceous_world
}

@Composable
internal fun RedEraPage(era: RedEra,state: RedUiState,time: ()->Float,onCatalog: ()->Unit,onCreature: (String)->Unit,onBadges: ()->Unit) {
    val owned = remember(era,state.creatures) { state.creatures.filter { it.creatureStatus==CreatureStatus.ACTIVE && RedCreatureCatalog.byId[it.findId]?.era==era } }
    val population = remember(era,state.creatures) { RedScenePopulation.populate(era,state.creatures) }
    val highestLevels = remember(owned) { owned.groupBy { it.findId }.mapValues { (_,copies) -> copies.maxOf { it.animalLevel } } }
    val entries = RedCreatureCatalog.entries.filter { it.era==era }
    val description = pluralStringResource(R.plurals.red_scene_description,population.size,stringResource(era.titleRes),population.size)
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TheBlueOverlaySurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(era.titleRes),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                        Text(stringResource(era.ageRes),style=MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick=onBadges) { Icon(Icons.Outlined.MilitaryTech,stringResource(R.string.red_badges)) }
                }
                Text(stringResource(era.worldRes),style=MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.red_progress,owned.map { it.findId }.distinct().size,entries.size),
                    style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
            }
        }
        TheBlueOverlaySurface(Modifier.fillMaxWidth().clickable(onClick=onCatalog).testTag("red-open-catalog")) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.red_catalog),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.tertiary)
                    Text(stringResource(R.string.red_catalog_subtitle),style=MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment=Alignment.End) {
                    PebbleIcon(Modifier.size(22.dp))
                    Text(pluralStringResource(R.plurals.red_balance,state.pebbles,state.pebbles),style=MaterialTheme.typography.labelMedium)
                }
            }
        }
        // This measured space is below all header text and above the owned-creature tray.
        // The drawing clips to it, so no body, tail or walking path can cross a text overlay.
        RedCreatureLanes(population,highestLevels,time,
            Modifier.weight(1f).fillMaxWidth().graphicsLayer().testTag("red-animal-lanes").semantics { contentDescription=description })
        if(owned.isEmpty()) {
            TheBlueOverlaySurface(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.red_empty_world),style=MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyRow(Modifier.fillMaxWidth().testTag("red-owned-creatures"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(owned.distinctBy { it.findId },key={it.findId}) { creature ->
                    val entry=RedCreatureCatalog.byId.getValue(creature.findId)
                    Surface(onClick={onCreature(entry.id)},shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface.copy(alpha=.88f)) {
                        Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            RedDinosaurIcon(entry.id,Modifier.size(44.dp))
                            Text(stringResource(entry.nameRes),style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
