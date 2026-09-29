package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.shell.*
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.drawLandCreature
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import kotlin.math.ceil
import kotlin.math.sqrt
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land.*

@Composable
fun LandZonePage(
    zone: TheBlueZoneUiModel,
    sceneTimeSeconds: () -> Float,
    onAnimalClick: (TheBlueAnimalGroupUiModel) -> Unit,
    onEncounter: (String?) -> Unit,
    onReturn: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) { drawLandEnvironment(zone.zoneId.creatureZone, time = sceneTimeSeconds()) }
        Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 76.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TheBlueOverlaySurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (zone.zoneId == TheBlueZoneId.GOLDEN_FIELDS) {
                        Text(stringResource(R.string.land_realm_land), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.land_intro), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(zoneTitle(zone.zoneId), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(zoneSubtitle(zone.zoneId), style = MaterialTheme.typography.bodyMedium)
                }
            }
            TheBlueOverlaySurface(Modifier.fillMaxWidth().clickable(role = Role.Button) { onEncounter(null) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.beyond_blue_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.land_discover_zone, zoneTitle(zone.zoneId)),  style = MaterialTheme.typography.bodySmall)
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // A lane includes the animal and some walking space. Actual silhouettes use
                // a common fit below, and the shared Sea plan caps copies of each species.
                val laneCapacity = (maxWidth.value * maxHeight.value / (100f * 80f))
                    .toInt().coerceAtLeast(1)
                val visible = remember(zone.animals, laneCapacity) {
                    landAnimalPresence(zone.animals, laneCapacity)
                }
                val density = LocalDensity.current
                // Fit walking lanes to the viewport, preserving a shared species size scale.
                // Dense collections keep every species represented instead of truncating at six.
                val columns = ceil(sqrt(visible.size * maxWidth.value / maxHeight.value.coerceAtLeast(1f)))
                    .toInt().coerceAtLeast(2).coerceAtMost((maxWidth.value / 48f).toInt().coerceAtLeast(1))
                val rows = ((visible.size + columns - 1) / columns).coerceAtLeast(1)
                val laneWidth = maxWidth / columns
                val laneHeight = maxHeight / rows
                val fit = minOf(1f, laneWidth.value / 170f, laneHeight.value / 150f).coerceAtLeast(.01f)
                visible.forEachIndexed { index, presence ->
                    key(presence.key) {
                        val animal = presence.animal
                        val creature = presence.definition
                        val motionIndex = (animal.findId.hashCode() and 1023) + presence.representativeIndex
                        val baseEdge = LandAnimalScale.edgeDp(creature.creatureId)
                        val growth = 1f + (animal.highestLevel.coerceIn(1, 99) - 1) / 98f * .12f
                        val edge = (baseEdge * growth * fit * presence.scale).dp
                        val target = maxOf(48.dp, edge)
                        val travel = (laneWidth - target).coerceAtLeast(0.dp)
                        val startX = laneWidth * (index % columns)
                        val startY = (laneHeight * (index / columns + 1) - target).coerceAtLeast(0.dp)
                        val animalDescription = stringResource(R.string.land_animal_a11y, findName(animal.findId), animal.totalCount, animal.highestLevel)
                        Canvas(Modifier.offset {
                            val pose = landAnimalPose(sceneTimeSeconds(), motionIndex, baseEdge)
                            with(density) { IntOffset((startX + travel * pose.progress).roundToPx(), startY.roundToPx()) }
                        }.size(target).alpha(if (presence.isCohort) .82f else 1f)
                            .testTag("land-presence:${presence.key}").clickable(role = Role.Button) { onAnimalClick(animal) }
                            .semantics { contentDescription = animalDescription }) {
                            val seconds = sceneTimeSeconds()
                            val pose = landAnimalPose(seconds, motionIndex, baseEdge)
                            val pixels = edge.toPx()
                            val left = (size.width - pixels) / 2f
                            val top = size.height - pixels
                            drawOval(Color(0xFF344A39).copy(alpha=.16f),
                                Offset(left + pixels*.2f, top + pixels*.86f), Size(pixels*.7f,pixels*.055f))
                            withTransform({
                                translate(left, top)
                                scale(pose.facing, 1f, Offset(pixels*.5f, pixels*.5f))
                                // drawLandCreature normalizes against the Canvas size; preserve its
                                // frame while leaving the independent 48dp target large enough to tap.
                                scale(pixels/size.width, pixels/size.height, Offset.Zero)
                            }) { drawLandCreature(creature, seconds, pose.walkPhase, pose.activity) }
                        }
                    }
                }
            }
            TheBlueCreatureTray(zone=zone, entryNewAnimalFindIds=emptySet(), onAnimalClick=onAnimalClick)
        }
    }
}
