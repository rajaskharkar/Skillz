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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun StillwaterRoomScreen(
    uiState: ShellUiState,
    onDrawFromStillwater: (StillwaterVessel) -> Unit,
    onConfirmStillwaterDraw: (StillwaterVessel) -> Unit,
    onDismissStillwaterReveal: () -> Unit,
    onDismissStillwaterDrawConfirmation: () -> Unit
) {
    val drops = uiState.stillwaterClaimableDrops
    val lifetimeDrops = uiState.stillwaterLifetimeDrops

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoomHeader(
            title = R.string.shell_room_stillwater_title,
            body = R.string.shell_stillwater_body
        )

        StillwaterDropsCard(
            drops = drops,
            lifetimeDrops = lifetimeDrops
        )

        Text(
            text = stringResource(R.string.shell_stillwater_draw_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        StillwaterVessel.entries.forEach { vessel ->
            StillwaterVesselCard(
                vessel = vessel,
                claimableDrops = drops,
                isUnlocked = uiState.isBlueZoneUnlocked(vessel.zone),
                onDraw = onDrawFromStillwater
            )
        }

        StillwaterExplainerCard()
    }

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

@Composable
private fun StillwaterDropsCard(
    drops: Long,
    lifetimeDrops: Long
) {
    val scheme = MaterialTheme.colorScheme
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(shellChamberBrush())
        ) {
            TurtleShellInteriorBackground(
                modifier = Modifier.matchParentSize(),
                centerGlow = true
            )
            Canvas(Modifier.matchParentSize()) {
                repeat(4) { i ->
                    drawCircle(
                        color = scheme.onPrimary.copy(alpha = 0.10f),
                        radius = 52f + i * 34f,
                        center = Offset(size.width / 2, size.height / 2),
                        style = Stroke(3f)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_stillwater_drops_gathered, drops),
                    color = scheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.shell_stillwater_lifetime_drops, lifetimeDrops),
                    color = scheme.onPrimary.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
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
    val canAfford = claimableDrops >= vessel.dropCost
    val canDraw = isUnlocked && canAfford
    val needed = stillwaterDropsNeeded(claimableDrops, vessel)
    val progress = stillwaterVesselProgress(claimableDrops, vessel)

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
        title = { Text(stringResource(R.string.shell_stillwater_confirm_draw_title, vesselName)) },
        text = {
            Text(
                stringResource(
                    R.string.shell_stillwater_confirm_draw_body,
                    vessel.dropCost,
                    stringResource(rewardFor(vessel)).replaceFirstChar { it.lowercase() }
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
        title = { Text(stringResource(R.string.shell_stillwater_creature_found)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = creature.displayName,
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
