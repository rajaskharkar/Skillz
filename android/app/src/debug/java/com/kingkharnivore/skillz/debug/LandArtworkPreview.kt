package com.kingkharnivore.skillz.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.drawLandCreature
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land.LandAnimalScale
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land.landAnimalPose
import com.kingkharnivore.skillz.utils.shell.*

/** Read-only atlas, intentionally debug-source-set only. */
@Composable
fun LandArtworkPreview(zone: CreatureZone, restorative: Boolean, animate: Boolean) {
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if(animate) {
            val start=withFrameNanos { it }
            while(true) withFrameNanos { seconds=(it-start)/1_000_000_000f }
        }
    }
    val creatures=remember(zone,restorative) {
        (if(restorative) LandCreatureCatalog.restorative else LandCreatureCatalog.main).filter { it.zone==zone }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("${zone.name.replace('_',' ')} · ${if(restorative) "Stillwater" else "Land"}",style=MaterialTheme.typography.titleMedium) }
        creatures.chunked(3).forEach { row ->
            item {
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { creature ->
                        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                            Canvas(Modifier.size(94.dp)) {
                                val pose=landAnimalPose(seconds,0,LandAnimalScale.edgeDp(creature.creatureId))
                                drawLandCreature(creature,seconds,pose.walkPhase,pose.activity)
                            }
                            Text(creature.displayName,style=MaterialTheme.typography.labelMedium)
                        }
                    }
                    repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
