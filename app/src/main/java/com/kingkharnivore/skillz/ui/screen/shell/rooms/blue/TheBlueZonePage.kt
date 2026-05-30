package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureDefinition
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureRenderFamily
import com.kingkharnivore.skillz.domain.shell.CreatureScaleClass
import com.kingkharnivore.skillz.domain.shell.CreatureSceneBehavior
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueUiState
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneUiModel
import com.kingkharnivore.skillz.ui.screen.shell.depthOrder
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.HabitatPresence
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.HabitatPresenceKind
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.LifeAgent
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.LifeCohort
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.LifeMotionMode
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.LifePresencePlan
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.PresenceAccounting
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.TheBlueRenderedCreature
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawRenderedCreature
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawTheBlueWaterBackground
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.drawZoneEnvironment
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.loopAlpha
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.movementLaneCount
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.offscreenHorizontalPassX
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.offscreenMarginFor
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.rectsOverlap
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.stableFacingRight
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.stableLane
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw.stablePhase
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun TheBlueZonePage(
    zone: TheBlueZoneUiModel,
    state: TheBlueUiState,
    pageHeight: Dp,
    showRoomHeader: Boolean,
    entryNewAnimalFindIds: Set<String>,
    sceneTimeSeconds: Float,
    onZoneBeyondBlue: () -> Unit,
    onAnimalClick: (TheBlueAnimalGroupUiModel) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val title = zoneTitle(zone.zoneId)
    val subtitle = zoneSubtitle(zone.zoneId)
    val beyondBlueCtaA11y = stringResource(R.string.beyond_blue_encounter_cta)
    val animalSummary = zoneAnimalSummary(zone)
    val zoneDescription = stringResource(R.string.the_blue_zone_scene_a11y, title, subtitle, animalSummary)
    val zoneHasNewArrival = zone.animals.any { it.isNew || it.findId in entryNewAnimalFindIds }
    val waterPhase = sceneTimeSeconds * (0.028f + zone.zoneId.depthOrder() * 0.006f)
    val drift = sceneTimeSeconds * (0.055f + zone.zoneId.depthOrder() * 0.008f)
    val mantaLoop = (sceneTimeSeconds / 20f) % 1f
    val whaleLoop = (sceneTimeSeconds / 34f) % 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .semantics { contentDescription = zoneDescription }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val placements = renderedCreaturePlacements(zone, size.width, size.height, drift, mantaLoop, whaleLoop)
            drawTheBlueWaterBackground(zone.zoneId, scheme, waterPhase)
            drawZoneEnvironment(zone.zoneId, scheme, drift, zone.animals.sumOf { it.totalCount })
            placements.sortedBy { it.zIndex }.forEach { drawRenderedCreature(it, scheme) }
            if (zoneHasNewArrival) {
                drawRect(scheme.secondary.copy(alpha = 0.045f))
            }
        }

        BoxWithConstraints(Modifier.matchParentSize()) {
            val density = LocalDensity.current
            val placements = with(density) {
                renderedCreaturePlacements(
                    zone = zone,
                    sceneWidth = maxWidth.toPx(),
                    sceneHeight = maxHeight.toPx(),
                    drift = drift,
                    mantaLoop = mantaLoop,
                    whaleLoop = whaleLoop
                )
            }
            placements.filter { it.clickable && it.alpha > 0.12f }.forEach { placement ->
                val animal = placement.animal
                val newLabel = if (animal.isNew || animal.findId in entryNewAnimalFindIds) stringResource(R.string.the_blue_new_arrival) else ""
                val description = stringResource(R.string.the_blue_creature_tile_a11y, findName(animal.findId), animal.totalCount, animal.highestLevel, newLabel)
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { placement.tapBounds.left.toDp() },
                            y = with(density) { placement.tapBounds.top.toDp() }
                        )
                        .size(
                            width = with(density) { placement.tapBounds.width.toDp() },
                            height = with(density) { placement.tapBounds.height.toDp() }
                        )
                        .clip(CircleShape)
                        .clickable(onClick = { onAnimalClick(animal) })
                        .semantics {
                            role = Role.Button
                            contentDescription = description
                        }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 20.dp, end = 76.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (showRoomHeader) {
                        Text(
                            text = stringResource(R.string.shell_room_the_blue_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.shell_room_the_blue_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface.copy(alpha = 0.74f)
                        )
                        Text(
                            text = stringResource(
                                R.string.the_blue_stat_row,
                                state.totalAnimals,
                                state.speciesCount,
                                zoneTitle(state.deepestZoneId ?: TheBlueZoneId.SUNLIT_REEF)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface.copy(alpha = 0.76f)
                    )
                }
            }
            TheBlueOverlaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onZoneBeyondBlue)
                    .semantics {
                        role = Role.Button
                        contentDescription = beyondBlueCtaA11y
                    }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.beyond_blue_encounter_cta),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.beyond_blue_discover_depth_copy),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        }

        TheBlueCreatureTray(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 78.dp, bottom = 24.dp),
            zone = zone,
            entryNewAnimalFindIds = entryNewAnimalFindIds,
            onAnimalClick = onAnimalClick
        )
    }
}

private fun renderedCreaturePlacements(
    zone: TheBlueZoneUiModel,
    sceneWidth: Float,
    sceneHeight: Float,
    drift: Float,
    mantaLoop: Float,
    whaleLoop: Float
): List<TheBlueRenderedCreature> {
    val safeBounds = theBlueSceneSafeBounds(sceneWidth, sceneHeight)
    val placements = mutableListOf<TheBlueRenderedCreature>()
    val occupied = mutableListOf<Rect>()
    val sortedAnimals = zone.animals.sortedWith(
        compareByDescending<TheBlueAnimalGroupUiModel> { animal ->
            val definition = CreatureCatalog.get(animal.findId)
            when (definition?.scaleClass) {
                CreatureScaleClass.LEGENDARY -> 6
                CreatureScaleClass.GIANT -> 5
                CreatureScaleClass.LARGE -> 4
                CreatureScaleClass.MEDIUM -> 3
                CreatureScaleClass.SMALL -> 2
                CreatureScaleClass.TINY -> 1
                null -> 0
            }
        }.thenBy { it.findId }
    )

    sortedAnimals.forEach { animal ->
        val definition = CreatureCatalog.get(animal.findId) ?: return@forEach
        val plan = lifePresencePlan(animal, definition)
        val accentCount = animal.levelCounts.filter { (it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1) > 1 }.sumOf { it.count }
        val levelScale = CreatureEconomy.animalVisualScale(animal.findId, animal.highestLevel)
        val tapBase = when (definition.scaleClass) {
            CreatureScaleClass.TINY -> 48f
            CreatureScaleClass.SMALL -> 58f
            CreatureScaleClass.MEDIUM -> 70f
            CreatureScaleClass.LARGE -> 92f
            CreatureScaleClass.GIANT -> 126f
            CreatureScaleClass.LEGENDARY -> 150f
        }
        val visualBase = when (definition.scaleClass) {
            CreatureScaleClass.TINY -> 34f
            CreatureScaleClass.SMALL -> 44f
            CreatureScaleClass.MEDIUM -> 58f
            CreatureScaleClass.LARGE -> 86f
            CreatureScaleClass.GIANT -> 122f
            CreatureScaleClass.LEGENDARY -> 150f
        }
        val zIndex = when (definition.sceneBehavior) {
            CreatureSceneBehavior.BOTTOM_DWELL -> 1f
            CreatureSceneBehavior.DRIFT -> 2f
            CreatureSceneBehavior.SWIM -> 3f
            CreatureSceneBehavior.GLIDE -> 4f
            CreatureSceneBehavior.CRUISE -> 5f
            CreatureSceneBehavior.LEGENDARY -> 6f
        }
        fun rendererFor(findId: String): String = when (findId) {
            ShellContentCatalog.FOCUS_MINNOW -> "minnow"
            ShellContentCatalog.FOCUS_SEAHORSE -> "seahorse"
            ShellContentCatalog.FOCUS_MANTA -> "manta"
            ShellContentCatalog.FOCUS_WHALE -> "base_whale"
            ShellContentCatalog.FOCUS_OCTOPUS -> "octopus"
            else -> definition.creatureId
        }
        fun visibleCenter(center: Offset, visualWidth: Float, visualHeight: Float): Offset =
            safeBounds.clampCenter(center, visualWidth / 2f, visualHeight / 2f)
        fun tryAdd(
            center: Offset,
            scale: Float,
            seed: Float,
            index: Int,
            facingRight: Boolean,
            renderer: String = rendererFor(animal.findId),
            reserveCorridor: Boolean = false,
            alphaMultiplier: Float = 1f,
            clickable: Boolean = true,
            useLoopAlpha: Boolean = false
        ): Boolean {
            val visualWidth = visualBase * scale * when (definition.scaleClass) {
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 1.55f
                CreatureScaleClass.LARGE -> 1.35f
                else -> 1.18f
            }
            val visualHeight = visualBase * scale
            val clamped = if (reserveCorridor) {
                Offset(center.x, center.y.coerceIn(safeBounds.top + visualHeight / 2f, safeBounds.bottom - visualHeight / 2f))
            } else {
                safeBounds.clampCenter(center, visualWidth / 2f, visualHeight / 2f)
            }
            val tapSize = tapBase * scale.coerceIn(0.85f, 1.9f)
            val visualBounds = Rect(clamped.x - visualWidth / 2f, clamped.y - visualHeight / 2f, clamped.x + visualWidth / 2f, clamped.y + visualHeight / 2f)
            val tapBounds = Rect(clamped.x - tapSize / 2f, clamped.y - tapSize / 2f, clamped.x + tapSize / 2f, clamped.y + tapSize / 2f)
            val spacing = when (definition.scaleClass) {
                CreatureScaleClass.TINY, CreatureScaleClass.SMALL -> 18f
                CreatureScaleClass.MEDIUM -> 26f
                CreatureScaleClass.LARGE -> 38f
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 54f
            }
            val collisionBounds = if (reserveCorridor) {
                Rect(safeBounds.left, tapBounds.top - spacing, safeBounds.right, tapBounds.bottom + spacing)
            } else {
                Rect(tapBounds.left - spacing, tapBounds.top - spacing, tapBounds.right + spacing, tapBounds.bottom + spacing)
            }
            if (occupied.any { rectsOverlap(it, collisionBounds) }) return false
            occupied += collisionBounds
            val alpha = if (useLoopAlpha) loopAlpha(clamped.x, visualWidth, safeBounds) else 1f
            placements += TheBlueRenderedCreature(
                animal = animal,
                definition = definition,
                center = clamped,
                visualBounds = visualBounds,
                tapBounds = tapBounds,
                scale = scale,
                alpha = alpha * alphaMultiplier,
                zIndex = zIndex,
                sceneBehavior = definition.sceneBehavior,
                placementBand = definition.placementBand,
                driftSeed = seed,
                glowing = index < accentCount,
                rendererKey = renderer,
                facingRight = facingRight,
                clickable = clickable
            )
            return true
        }
        fun tryCandidates(
            candidates: List<Offset>,
            scale: Float,
            seed: Float,
            index: Int,
            facingRight: Boolean,
            renderer: String = rendererFor(animal.findId),
            reserveCorridor: Boolean = false,
            alphaMultiplier: Float = 1f,
            clickable: Boolean = true,
            useLoopAlpha: Boolean = false
        ): Boolean = candidates.any { tryAdd(it, scale, seed, index, facingRight, renderer, reserveCorridor, alphaMultiplier, clickable, useLoopAlpha) }

        fun directCenter(index: Int, plannedCount: Int, scale: Float, motionMode: LifeMotionMode): Pair<Offset, Boolean> {
            val phase = stablePhase(animal.findId, index)
            val tau = 6.2831855f
            val visualWidth = visualBase * scale * if (definition.scaleClass >= CreatureScaleClass.LARGE) 1.35f else 1.18f
            val visualHeight = visualBase * scale
            return when (motionMode) {
                LifeMotionMode.ANCHORED -> {
                    val laneCount = max(5, plannedCount)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val baseX = safeBounds.left + safeBounds.width * ((lane + 1f) / (laneCount + 1f))
                    val y = safeBounds.bottom - (22f + (index % 2) * 22f) * scale
                    Offset(baseX, y) to stableFacingRight(animal.findId, index)
                }
                LifeMotionMode.DRIFT_BOUNDED -> {
                    val laneCount = max(4, plannedCount)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val baseX = safeBounds.left + safeBounds.width * ((lane + 1f) / (laneCount + 1f))
                    val xMotion = drift * tau + phase * tau
                    val yMotion = drift * tau * 0.62f + phase * tau
                    val x = baseX + sin(xMotion.toDouble()).toFloat() * min(34f, safeBounds.width * 0.055f)
                    val baseY = safeBounds.top + safeBounds.height * (0.20f + (index % 3) * 0.22f)
                    val y = baseY + sin(yMotion.toDouble()).toFloat() * 16f
                    visibleCenter(Offset(x, y), visualWidth, visualHeight) to (cos(xMotion.toDouble()).toFloat() >= 0f)
                }
                LifeMotionMode.VISIBLE_LANE -> {
                    val laneCount = movementLaneCount(definition, plan)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val baseX = safeBounds.left + safeBounds.width * ((index + 1f) / (plannedCount + 1f))
                    val motionClock = when (definition.sceneBehavior) {
                        CreatureSceneBehavior.GLIDE -> mantaLoop
                        CreatureSceneBehavior.CRUISE, CreatureSceneBehavior.LEGENDARY -> whaleLoop
                        else -> drift
                    }
                    val xMotion = motionClock * tau + phase * tau
                    val yMotion = motionClock * tau * 0.57f + phase * tau
                    val x = baseX + sin(xMotion.toDouble()).toFloat() * min(48f, safeBounds.width * 0.065f)
                    val baseY = safeBounds.top + safeBounds.height * ((lane + 1f) / (laneCount + 1f))
                    val y = baseY + sin(yMotion.toDouble()).toFloat() * min(18f, safeBounds.height * 0.035f)
                    visibleCenter(Offset(x, y), visualWidth, visualHeight) to (cos(xMotion.toDouble()).toFloat() >= 0f)
                }
                LifeMotionMode.PASS_THROUGH, LifeMotionMode.AMBIENT -> {
                    val facingRight = stableFacingRight(animal.findId, index)
                    val progress = (drift + phase) % 1f
                    val margin = offscreenMarginFor(visualWidth, definition)
                    val x = offscreenHorizontalPassX(progress, safeBounds.left, safeBounds.right, visualWidth, margin, facingRight)
                    val laneCount = movementLaneCount(definition, plan)
                    val lane = stableLane(animal.findId, index, laneCount)
                    val y = safeBounds.top + safeBounds.height * ((lane + 1f) / (laneCount + 1f))
                    Offset(x, y) to facingRight
                }
            }
        }

        val accounting = PresenceAccounting(owned = animal.totalCount.coerceAtLeast(0))
        val failedDirectAgents = mutableListOf<LifeAgent>()
        plan.directIndividuals.forEach { agent ->
            val i = agent.representativeIndex
            val scale = when (animal.findId) {
                ShellContentCatalog.FOCUS_WHALE -> (1.20f + (i % 3) * 0.06f) * levelScale
                ShellContentCatalog.FOCUS_MANTA -> (1.00f + (i % 3) * 0.08f) * levelScale
                else -> (0.92f + (i % 3) * 0.08f) * levelScale
            }
            val seed = drift + stablePhase(animal.findId, i)
            val (center, facingRight) = directCenter(i, plan.directIndividuals.size.coerceAtLeast(1), scale, agent.motionMode)
            val visualWidth = visualBase * scale * if (definition.scaleClass >= CreatureScaleClass.LARGE) 1.35f else 1.18f
            val visualHeight = visualBase * scale
            val directCandidates = listOf(
                center,
                safeBounds.clampCenter(Offset(center.x, center.y + safeBounds.height * 0.14f), visualWidth / 2f, visualHeight / 2f),
                safeBounds.clampCenter(Offset(center.x, center.y - safeBounds.height * 0.14f), visualWidth / 2f, visualHeight / 2f),
                safeBounds.clampCenter(Offset(center.x + safeBounds.width * 0.10f, center.y), visualWidth / 2f, visualHeight / 2f),
                safeBounds.clampCenter(Offset(center.x - safeBounds.width * 0.10f, center.y), visualWidth / 2f, visualHeight / 2f)
            ).distinct()
            val placed = tryCandidates(
                candidates = directCandidates,
                scale = scale,
                seed = seed,
                index = i,
                facingRight = facingRight,
                clickable = true,
                alphaMultiplier = 1f
            ) || tryCandidates(
                candidates = directCandidates,
                scale = scale * 0.86f,
                seed = seed,
                index = i,
                facingRight = facingRight,
                clickable = true,
                alphaMultiplier = 0.96f
            )
            if (placed) {
                accounting.representedDirect++
            } else {
                failedDirectAgents += agent
            }
        }

        val desiredCohortCount = plan.cohorts.sumOf { it.count } + failedDirectAgents.size
        val cohortCount = min(desiredCohortCount, accounting.remaining)
        if (cohortCount > 0) {
            val cohortIndex = plan.directIndividuals.size
            val cohortScale = when (definition.scaleClass) {
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 0.72f
                CreatureScaleClass.LARGE -> 0.76f
                else -> 0.82f
            } * levelScale
            val cohortMode = plan.cohorts.firstOrNull()?.motionMode ?: when (definition.sceneBehavior) {
                CreatureSceneBehavior.BOTTOM_DWELL -> LifeMotionMode.ANCHORED
                CreatureSceneBehavior.DRIFT -> LifeMotionMode.DRIFT_BOUNDED
                else -> LifeMotionMode.VISIBLE_LANE
            }
            val (cohortCenter, cohortFacingRight) = directCenter(cohortIndex, plan.directIndividuals.size + 1, cohortScale, cohortMode)
            val cohortPlaced = tryCandidates(
                candidates = listOf(
                    cohortCenter,
                    safeBounds.clampCenter(Offset(cohortCenter.x + safeBounds.width * 0.12f, cohortCenter.y + safeBounds.height * 0.10f), visualBase * cohortScale / 2f, visualBase * cohortScale / 2f),
                    safeBounds.clampCenter(Offset(cohortCenter.x - safeBounds.width * 0.12f, cohortCenter.y - safeBounds.height * 0.10f), visualBase * cohortScale / 2f, visualBase * cohortScale / 2f)
                ),
                scale = cohortScale,
                seed = drift + stablePhase(animal.findId, cohortIndex),
                index = cohortIndex,
                facingRight = cohortFacingRight,
                clickable = true,
                alphaMultiplier = 0.82f
            )
            if (cohortPlaced) {
                accounting.representedCohort += cohortCount
            }
        }

        val desiredHabitatCount = max(plan.habitatMarks.sumOf { it.countRepresented }, accounting.remaining)
        val habitatCount = min(desiredHabitatCount, accounting.remaining).coerceAtMost(5)
        repeat(habitatCount) { habitatIndex ->
            val i = plan.directIndividuals.size + 1 + habitatIndex
            val habitat = plan.habitatMarks.firstOrNull()
            val habitatScale = when (definition.scaleClass) {
                CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 0.42f
                CreatureScaleClass.LARGE -> 0.48f
                else -> 0.56f
            } * levelScale
            val phase = stablePhase(animal.findId, i)
            val x = safeBounds.left + safeBounds.width * (0.14f + (phase * 0.72f))
            val bandBase = when (definition.sceneBehavior) {
                CreatureSceneBehavior.BOTTOM_DWELL -> 0.88f
                CreatureSceneBehavior.DRIFT -> 0.34f + (habitatIndex % 3) * 0.18f
                else -> 0.24f + (habitatIndex % 4) * 0.16f
            }
            val y = safeBounds.top + safeBounds.height * bandBase.coerceIn(0.12f, 0.90f)
            val habitatPlaced = tryCandidates(
                candidates = listOf(
                    Offset(x, y),
                    Offset((x + safeBounds.width * 0.16f).coerceAtMost(safeBounds.right), (y + safeBounds.height * 0.10f).coerceAtMost(safeBounds.bottom)),
                    Offset((x - safeBounds.width * 0.16f).coerceAtLeast(safeBounds.left), (y - safeBounds.height * 0.10f).coerceAtLeast(safeBounds.top))
                ),
                scale = habitatScale,
                seed = drift + phase,
                index = i,
                facingRight = stableFacingRight(animal.findId, i),
                alphaMultiplier = habitat?.alpha ?: 0.16f,
                clickable = habitat?.clickable ?: false
            )
            if (habitatPlaced) {
                accounting.representedHabitat++
            }
        }
    }
    return placements
}

private fun theBlueSceneSafeBounds(sceneWidth: Float, sceneHeight: Float): TheBlueSceneSafeBounds {
    val horizontalInset = max(24f, sceneWidth * 0.055f)
    return TheBlueSceneSafeBounds(
        left = horizontalInset,
        top = max(132f, sceneHeight * 0.24f),
        right = (sceneWidth - max(92f, sceneWidth * 0.18f)).coerceAtLeast(horizontalInset + 1f),
        bottom = (sceneHeight - max(170f, sceneHeight * 0.24f)).coerceAtLeast(max(132f, sceneHeight * 0.24f) + 1f)
    )
}

private fun lifePresencePlan(
    animal: TheBlueAnimalGroupUiModel,
    definition: CreatureDefinition
): LifePresencePlan {
    val owned = animal.totalCount.coerceAtLeast(0)
    if (owned == 0) {
        return LifePresencePlan(emptyList(), emptyList(), emptyList(), overflowCount = 0)
    }
    val uniqueLegendary = isUniqueLegendaryCreature(definition)
    val directLimit = when {
        uniqueLegendary -> 1
        definition.renderFamily == CreatureRenderFamily.WHALE -> 3
        definition.renderFamily == CreatureRenderFamily.RAY -> 3
        definition.renderFamily == CreatureRenderFamily.JELLYFISH -> 5
        definition.sceneBehavior == CreatureSceneBehavior.BOTTOM_DWELL -> 5
        definition.scaleClass == CreatureScaleClass.TINY -> 8
        definition.scaleClass == CreatureScaleClass.SMALL -> 6
        definition.scaleClass == CreatureScaleClass.MEDIUM -> 5
        definition.scaleClass == CreatureScaleClass.LARGE -> 4
        definition.scaleClass == CreatureScaleClass.GIANT -> 3
        definition.scaleClass == CreatureScaleClass.LEGENDARY -> 2
        else -> 3
    }
    val directCount = owned.coerceAtMost(directLimit)
    val directMode = when (definition.sceneBehavior) {
        CreatureSceneBehavior.BOTTOM_DWELL -> LifeMotionMode.ANCHORED
        CreatureSceneBehavior.DRIFT -> LifeMotionMode.DRIFT_BOUNDED
        else -> LifeMotionMode.VISIBLE_LANE
    }
    val directIndividuals = (0 until directCount).map { index ->
        LifeAgent(
            key = "${animal.findId}:direct:$index",
            findId = animal.findId,
            representativeIndex = index,
            level = animal.highestLevel,
            sourceType = definition.sourceType.name,
            laneId = "${definition.placementBand.name.lowercase()}:$index",
            motionMode = directMode
        )
    }
    val overflow = (owned - directCount).coerceAtLeast(0)
    val cohortMode = when (definition.sceneBehavior) {
        CreatureSceneBehavior.BOTTOM_DWELL -> LifeMotionMode.ANCHORED
        CreatureSceneBehavior.DRIFT -> LifeMotionMode.DRIFT_BOUNDED
        else -> LifeMotionMode.VISIBLE_LANE
    }
    val cohorts = if (overflow > 0) {
        listOf(
            LifeCohort(
                key = "${animal.findId}:cohort",
                findId = animal.findId,
                count = overflow,
                laneId = "${definition.placementBand.name.lowercase()}:cohort",
                motionMode = cohortMode
            )
        )
    } else {
        emptyList()
    }
    val habitatKind = when {
        uniqueLegendary -> HabitatPresenceKind.CURRENT_TRAIL
        definition.renderFamily == CreatureRenderFamily.WHALE -> HabitatPresenceKind.POD_SHADOW
        definition.renderFamily == CreatureRenderFamily.RAY -> HabitatPresenceKind.DISTANT_SILHOUETTE
        definition.renderFamily == CreatureRenderFamily.JELLYFISH -> HabitatPresenceKind.BLOOM_GLOW
        definition.sceneBehavior == CreatureSceneBehavior.BOTTOM_DWELL -> HabitatPresenceKind.REEF_CLUSTER
        definition.scaleClass <= CreatureScaleClass.SMALL -> HabitatPresenceKind.SCHOOL_SHIMMER
        else -> HabitatPresenceKind.BUBBLE_CLUSTER
    }
    val habitatMarks = if (overflow > 0) {
        listOf(
            HabitatPresence(
                key = "${animal.findId}:habitat",
                findId = animal.findId,
                countRepresented = overflow,
                kind = habitatKind,
                placementBand = definition.placementBand,
                alpha = if (uniqueLegendary) 0.26f else 0.18f
            )
        )
    } else {
        emptyList()
    }
    return LifePresencePlan(
        directIndividuals = directIndividuals,
        cohorts = cohorts,
        habitatMarks = habitatMarks,
        overflowCount = overflow
    )
}