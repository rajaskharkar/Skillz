package com.kingkharnivore.skillz.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.health.HealthConnectAvailability
import com.kingkharnivore.skillz.viewmodel.health.HealthSettingsUiState

private data class HealthCardRenderState(
    val headline: String,
    val body: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    val showSwitch: Boolean = false
)

@Composable
fun HealthConnectSettingsCard(
    state: HealthSettingsUiState,
    onToggleMovementBonus: (Boolean) -> Unit,
    onConnectHealth: () -> Unit,
    onInstallOrUpdateHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = "Health"
    val (headline, body, actionLabel, action, showSwitch) = when {
        state.healthConnectAvailability == HealthConnectAvailability.UNAVAILABLE -> HealthCardRenderState(
            headline = "Health Connect is not available on this device.",
            body = "Movement Bonus needs Health Connect to read steps during your Flows."
        )
        state.providerUpdateRequired -> HealthCardRenderState(
            headline = "Health Connect needs to be installed or updated.",
            body = "Install or update Health Connect to use Movement Bonus.",
            actionLabel = "Install / Update Health Connect",
            action = onInstallOrUpdateHealthConnect
        )
        state.healthConnectAvailable && !state.readStepsPermissionGranted -> HealthCardRenderState(
            headline = "Enable Health to earn Movement Points during your Flows.",
            body = "Scyra reads your step count through Health Connect and gives you +1 Movement Point for every 25 steps during eligible Flows. Movement Points are added to your total Scyra Points.",
            actionLabel = "Connect Health",
            action = onConnectHealth
        )
        state.healthConnectAvailable && state.localMovementBonusEnabled -> HealthCardRenderState(
            headline = "Movement Bonus is active.",
            body = "Steps taken during eligible Flows can earn Movement Points. Movement Points are added to your Scyra Points and can also generate Pearls.",
            showSwitch = true
        )
        else -> HealthCardRenderState(
            headline = "Movement Bonus is off.",
            body = "Turn it on to earn Movement Points from steps during eligible Flows.",
            showSwitch = true
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.86f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Scyra reads your step count from Health Connect only to calculate Movement Points during eligible Flows.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                )
                state.userMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (actionLabel != null && action != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = action,
                        enabled = !state.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(actionLabel)
                    }
                }
            }
            if (showSwitch) {
                Switch(
                    checked = state.toggleChecked,
                    enabled = !state.isBusy,
                    onCheckedChange = onToggleMovementBonus
                )
            }
        }
    }
}


@Composable
fun DisableHealthPendingFlowsDialog(
    onKeepHealthOn: () -> Unit,
    onDisableAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepHealthOn,
        title = { Text("Disable Health?") },
        text = {
            Text("Some recent Flows may still be waiting for step data or improved Health Connect sync.\n\nIf you disable Health now, Scyra will stop checking those Flows and any pending Movement Points may not be awarded.\n\nYou can turn Health back on later.")
        },
        confirmButton = { TextButton(onClick = onDisableAnyway) { Text("Disable Anyway") } },
        dismissButton = { TextButton(onClick = onKeepHealthOn) { Text("Keep Health On") } }
    )
}

@Composable
fun MovementBonusActivePill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Movement Bonus active · Every 25 steps earns +1 point",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun MovementBonusRewardBlock(steps: Long, movementPoints: Long, modifier: Modifier = Modifier) {
    if (movementPoints <= 0L || steps <= 0L) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Movement Bonus", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("$steps steps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("+$movementPoints Movement Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun FlowCardMovementLine(steps: Long?, movementPoints: Long, modifier: Modifier = Modifier) {
    if (movementPoints <= 0L || steps == null || steps <= 0L) return
    Text(
        text = "$steps steps · $movementPoints Movement Points",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary
    )
}
