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
import androidx.compose.material3.AssistChip
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
import com.kingkharnivore.skillz.viewmodel.health.HealthSettingsUiState

@Composable
fun HealthConnectSettingsCard(
    state: HealthSettingsUiState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = "Health"
    val (headline, body) = when {
        !state.healthConnectAvailable -> "Health Connect is not available on this device." to
            "Movement Bonus needs Health Connect to read steps during your Flows."
        state.localMovementBonusEnabled && !state.readStepsPermissionGranted -> "Health permission was removed." to
            "Reconnect Health to earn Movement Points during eligible Flows."
        state.toggleChecked -> "Movement Bonus is active." to
            "Steps taken during eligible Flows can earn Movement Points. Movement Points are added to your Scyra Points and can also generate Pearls."
        else -> "Enable Health to earn Movement Points during your Flows." to
            "Scyra reads your step count through Health Connect and gives you +1 Movement Point for every 25 steps during eligible Flows. Movement Points are added to your total Scyra Points."
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
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(headline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.86f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Scyra reads your step count from Health Connect only to calculate Movement Points during eligible Flows.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                )
            }
            Switch(
                checked = state.toggleChecked,
                enabled = state.toggleEnabled,
                onCheckedChange = onToggle
            )
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
            Text("Some recent Flows may still be waiting for step data from Health Connect.\n\nIf you disable Health now, Scyra will stop checking those Flows and any pending Movement Points may not be awarded.\n\nYou can turn Health back on later.")
        },
        confirmButton = { TextButton(onClick = onDisableAnyway) { Text("Disable Anyway") } },
        dismissButton = { TextButton(onClick = onKeepHealthOn) { Text("Keep Health On") } }
    )
}

@Composable
fun MovementBonusActivePill(modifier: Modifier = Modifier) {
    AssistChip(
        modifier = modifier,
        onClick = {},
        label = { Text("Movement Bonus active · Every 25 steps earns +1 point") },
        leadingIcon = {
            Icon(Icons.Outlined.DirectionsWalk, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
    )
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
