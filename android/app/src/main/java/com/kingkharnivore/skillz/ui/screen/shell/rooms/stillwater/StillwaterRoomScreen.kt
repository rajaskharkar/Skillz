package com.kingkharnivore.skillz.ui.screen.shell.rooms.stillwater

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.utils.shell.stillwaterDropsNeeded
import com.kingkharnivore.skillz.utils.shell.stillwaterVesselProgress
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.TurtleShellInteriorBackground
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.viewmodel.shell.isBlueZoneUnlocked
import com.kingkharnivore.skillz.domain.achievement.CollectionProgress
import com.kingkharnivore.skillz.ui.screen.shell.inventory.CollectionDetailsSheet
import com.kingkharnivore.skillz.ui.screen.shell.inventory.collectionDisplayName
import com.kingkharnivore.skillz.ui.screen.shell.inventory.collectionSpeciesDestination
import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.ui.screen.shell.NavigationConsumptionResult
import com.kingkharnivore.skillz.ui.screen.shell.NavigationFailureReason
import com.kingkharnivore.skillz.ui.screen.shell.PendingShellNavigation

internal data class StillwaterDropsCardUiModel(
    @StringRes val primaryStringRes: Int,
    val primaryDrops: Long,
    val secondaryDrops: Long,
    val hasAvailableDraw: Boolean
)

internal data class StillwaterVesselCardUiModel(
    val claimableDrops: Long,
    val canAfford: Boolean,
    val canDraw: Boolean,
    val dropsNeeded: Long,
    val progress: Float
)

internal fun buildStillwaterDropsCardUiModel(uiState: ShellUiState): StillwaterDropsCardUiModel {
    val hasAvailableDraw = StillwaterVessel.entries.any { vessel ->
        uiState.isBlueZoneUnlocked(vessel.zone) &&
            uiState.stillwaterClaimableDrops >= vessel.dropCost
    }
    return StillwaterDropsCardUiModel(
        primaryStringRes = R.string.shell_stillwater_drops_available,
        primaryDrops = uiState.stillwaterClaimableDrops,
        secondaryDrops = uiState.stillwaterLifetimeDrops,
        hasAvailableDraw = hasAvailableDraw
    )
}

internal fun buildStillwaterVesselCardUiModel(
    vessel: StillwaterVessel,
    claimableDrops: Long,
    isUnlocked: Boolean
): StillwaterVesselCardUiModel {
    val canAfford = claimableDrops >= vessel.dropCost
    return StillwaterVesselCardUiModel(
        claimableDrops = claimableDrops,
        canAfford = canAfford,
        canDraw = isUnlocked && canAfford,
        dropsNeeded = stillwaterDropsNeeded(claimableDrops, vessel),
        progress = stillwaterVesselProgress(claimableDrops, vessel)
    )
}

@Composable
fun StillwaterRoomScreen(
    uiState: ShellUiState,
    onDrawFromStillwater: (StillwaterVessel) -> Unit,
    onConfirmStillwaterDraw: (StillwaterVessel) -> Unit,
    onDismissStillwaterReveal: () -> Unit,
    onDismissStillwaterDrawConfirmation: () -> Unit,
    onNavigate: (BadgeActionDestination) -> Unit,
    focusRequest: PendingShellNavigation.OpenStillwaterSpecies? = null,
    onFocusResult: (NavigationConsumptionResult) -> Unit = {}
) {
    val dropsCard = buildStillwaterDropsCardUiModel(uiState)
    val drops = dropsCard.primaryDrops

    val listState = rememberLazyListState()
    var collectionDetails by remember { mutableStateOf<CollectionProgress?>(null) }
    var highlightedCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(focusRequest) {
        val focusedCollectionId = focusRequest?.collectionId ?: return@LaunchedEffect
        val collection = uiState.badgeDashboard?.collections?.firstOrNull { it.collectionId == focusedCollectionId }
        if (collection == null) {
            onFocusResult(NavigationConsumptionResult.Failed(NavigationFailureReason.COLLECTION_NOT_FOUND))
            return@LaunchedEffect
        }
        val speciesId = focusRequest.speciesId
        if (speciesId != null && collection.speciesStates.none { it.speciesId == speciesId }) {
            onFocusResult(NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND))
            return@LaunchedEffect
        }
        highlightedCollectionId = focusedCollectionId
        val index = if (focusedCollectionId == "collection_stillwater") 2 else {
            val vesselName = focusedCollectionId.removePrefix("stillwater_")
            4 + StillwaterVessel.entries.indexOfFirst { it.name.lowercase() == vesselName }.coerceAtLeast(0)
        }
        listState.animateScrollToItem(index)
        onFocusResult(NavigationConsumptionResult.Consumed)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item("header") { RoomHeader(
            title = R.string.shell_room_stillwater_title,
            body = R.string.shell_stillwater_body
        ) }

        item("drops") { StillwaterDropsCard(dropsCard = dropsCard) }

        uiState.badgeDashboard?.collections?.firstOrNull { it.collectionId == "collection_stillwater" }?.let { progress ->
            item("overall-progress") { StillwaterProgressCard(progress, highlightedCollectionId == progress.collectionId) { highlightedCollectionId = progress.collectionId; collectionDetails = progress } }
        }

        item("prompt") { Text(
            text = stringResource(R.string.shell_stillwater_draw_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ) }

        StillwaterVessel.entries.forEach { vessel ->
            val collectionId = "stillwater_${vessel.name.lowercase()}"
            item(collectionId) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { StillwaterVesselCard(
                vessel = vessel,
                claimableDrops = drops,
                isUnlocked = uiState.isBlueZoneUnlocked(vessel.zone),
                onDraw = onDrawFromStillwater
            ); uiState.badgeDashboard?.collections?.firstOrNull { it.collectionId == collectionId }?.let { progress ->
                StillwaterProgressCard(progress, highlightedCollectionId == collectionId) { highlightedCollectionId = collectionId; collectionDetails = progress }
            } } }
        }

        item("explainer") { StillwaterExplainerCard() }
    }

    collectionDetails?.let { progress -> CollectionDetailsSheet(progress, { collectionDetails = null }) { action ->
        collectionSpeciesDestination(action)?.let(onNavigate)
    } }

    uiState.stillwaterRevealCreature?.let { creature ->
        StillwaterCreatureRevealDialog(
            creature = creature,
            onDismiss = onDismissStillwaterReveal
        )
    }

    uiState.pendingStillwaterDrawVessel?.let { vessel ->
        StillwaterDrawConfirmDialog(
            vessel = vessel,
            onConfirm = { onConfirmStillwaterDraw(vessel) },
            onDismiss = onDismissStillwaterDrawConfirmation
        )
    }
}

@Composable private fun StillwaterProgressCard(progress: CollectionProgress, focused: Boolean, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.stillwater_named_collection_progress, collectionDisplayName(progress.collectionId)), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.collection_discovered_progress, progress.discoveredSpeciesCount, progress.totalParticipatingSpecies))
            Text(stringResource(R.string.collection_owned_progress, progress.currentlyOwnedSpeciesCount, progress.totalParticipatingSpecies))
            Text(stringResource(R.string.collection_mastered_progress, progress.masteredSpeciesCount, progress.totalCompletionistSpecies))
            Text("${stringResource(R.string.badge_state_collector)}: ${stringResource(if (progress.collectorEarned) R.string.badge_earned else R.string.badge_locked)}")
            Text("${stringResource(R.string.badge_state_curator)}: ${stringResource(if (progress.curatorEarned) R.string.badge_earned else R.string.badge_locked)}")
            Text("${stringResource(R.string.badge_state_completionist)}: ${stringResource(if (progress.completionistEarned) R.string.badge_earned else R.string.badge_locked)}")
        }
    }
}

@Composable
private fun StillwaterDropsCard(
    dropsCard: StillwaterDropsCardUiModel
) {
    val scheme = MaterialTheme.colorScheme
    ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(shellChamberBrush())
        ) {
            TurtleShellInteriorBackground(
                modifier = Modifier.matchParentSize(),
                centerGlow = true
            )
            Canvas(Modifier.matchParentSize()) {
                repeat(5) { index ->
                    drawCircle(
                        color = scheme.onPrimary.copy(alpha = 0.07f - index * 0.008f),
                        radius = 56f + index * 38f,
                        center = Offset(size.width * 0.72f, size.height * 0.35f),
                        style = Stroke(width = 3f)
                    )
                }
                repeat(4) { index ->
                    drawCircle(
                        color = scheme.onPrimary.copy(alpha = 0.05f - index * 0.007f),
                        radius = 36f + index * 28f,
                        center = Offset(size.width * 0.22f, size.height * 0.78f),
                        style = Stroke(width = 2f)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.shell_room_stillwater_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onPrimary.copy(alpha = 0.82f),
                        fontWeight = FontWeight.SemiBold
                    )

                    if (dropsCard.hasAvailableDraw) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = scheme.onPrimary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = stringResource(R.string.shell_stillwater_ready_to_draw),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(dropsCard.primaryStringRes, dropsCard.primaryDrops),
                        color = scheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.shell_stillwater_lifetime_drops, dropsCard.secondaryDrops),
                        color = scheme.onPrimary.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = stringResource(R.string.shell_stillwater_soft_flows_gather_drops),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimary.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun StillwaterVesselCard(
    vessel: StillwaterVessel,
    claimableDrops: Long,
    isUnlocked: Boolean,
    onDraw: (StillwaterVessel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val cardState = buildStillwaterVesselCardUiModel(vessel, claimableDrops, isUnlocked)
    val canAfford = cardState.canAfford
    val canDraw = cardState.canDraw
    val needed = cardState.dropsNeeded
    val progress = cardState.progress

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(titleFor(vessel)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.shell_stillwater_vessel_cost, vessel.dropCost),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = when {
                        !isUnlocked -> scheme.surfaceVariant
                        canDraw -> scheme.secondary
                        else -> scheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            !isUnlocked -> stringResource(R.string.shell_stillwater_locked)
                            canDraw -> stringResource(R.string.shell_stillwater_ready)
                            else -> stringResource(R.string.shell_stillwater_filling)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = stringResource(rewardFor(vessel)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(categoryFor(vessel)),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface.copy(alpha = 0.72f)
            )

            if (!isUnlocked) {
                Text(
                    text = stringResource(R.string.shell_stillwater_reach_depth_first),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (!canAfford) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.shell_stillwater_progress, claimableDrops, vessel.dropCost),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.shell_stillwater_more_needed, needed),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onDraw(vessel) },
                    enabled = canDraw
                ) {
                    Text(
                        text = if (canDraw) {
                            stringResource(R.string.shell_stillwater_draw)
                        } else {
                            stringResource(R.string.shell_stillwater_keep_filling)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StillwaterExplainerCard() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
    ) {
        Text(
            text = stringResource(R.string.shell_stillwater_explainer),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun StillwaterDrawConfirmDialog(
    vessel: StillwaterVessel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val vesselName = stringResource(titleFor(vessel))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.shell_stillwater_confirm_draw_title, vesselName)) },
        text = {
            Text(
                stringResource(
                    R.string.shell_stillwater_confirm_draw_body,
                    vessel.dropCost
                )
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.shell_stillwater_draw)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun StillwaterCreatureRevealDialog(
    creature: CreatureDefinition,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.shell_stillwater_creature_found)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = creature.titleRes.takeIf { it != 0 }?.let { stringResource(it) }
                        ?: stringResource(R.string.badge_creature_fallback),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = categoryForZone(creature.zone))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.shell_stillwater_exclusive),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.shell_stillwater_added_to_chest))
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.shell_stillwater_done)) }
        }
    )
}

@StringRes
private fun titleFor(vessel: StillwaterVessel): Int = when (vessel) {
    StillwaterVessel.FISHBOWL -> R.string.shell_stillwater_fishbowl_title
    StillwaterVessel.AQUARIUM -> R.string.shell_stillwater_aquarium_title
    StillwaterVessel.POND -> R.string.shell_stillwater_pond_title
    StillwaterVessel.LAKE -> R.string.shell_stillwater_lake_title
}

@StringRes
private fun rewardFor(vessel: StillwaterVessel): Int = when (vessel) {
    StillwaterVessel.FISHBOWL -> R.string.shell_stillwater_fishbowl_reward
    StillwaterVessel.AQUARIUM -> R.string.shell_stillwater_aquarium_reward
    StillwaterVessel.POND -> R.string.shell_stillwater_pond_reward
    StillwaterVessel.LAKE -> R.string.shell_stillwater_lake_reward
}

@StringRes
private fun categoryFor(vessel: StillwaterVessel): Int = when (vessel) {
    StillwaterVessel.FISHBOWL -> R.string.shell_stillwater_fishbowl_category
    StillwaterVessel.AQUARIUM -> R.string.shell_stillwater_aquarium_category
    StillwaterVessel.POND -> R.string.shell_stillwater_pond_category
    StillwaterVessel.LAKE -> R.string.shell_stillwater_lake_category
}

@Composable
private fun categoryForZone(zone: CreatureZone): String = when (zone) {
    CreatureZone.SUNLIT_REEF -> stringResource(R.string.shell_stillwater_fishbowl_category)
    CreatureZone.DEEPER_REEF -> stringResource(R.string.shell_stillwater_aquarium_category)
    CreatureZone.OPEN_BLUE -> stringResource(R.string.shell_stillwater_pond_category)
    CreatureZone.GREAT_BLUE -> stringResource(R.string.shell_stillwater_lake_category)
}
