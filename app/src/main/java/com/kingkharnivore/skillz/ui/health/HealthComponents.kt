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

private sealed interface HealthCardAction {
    data object None : HealthCardAction
    data object ConnectHealth : HealthCardAction
    data object InstallOrUpdate : HealthCardAction
    data object Toggle : HealthCardAction
}

private data class HealthCardRenderState(
    val headline: String,
    val body: String,
    val action: HealthCardAction
)

@Composable
fun HealthConnectSettingsCard(
    state: HealthSettingsUiState,
    onToggleMovementBonus: (Boolean) -> Unit,
    onConnectHealth: () -> Unit,
    onInstallOrUpdateHealthConnect: () -> Unit,
    onEnablePhoneStepEstimate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val healthReady = state.healthConnectSourceReady
    val phoneReady = state.phoneStepSourceReady
    val renderState = if (!state.localMovementBonusEnabled) {
        HealthCardRenderState(
            headline = "Movement Bonus is off.",
            body = "Turn it on to earn Movement Points from steps during eligible Flows.",
            action = if (state.providerUpdateRequired) HealthCardAction.InstallOrUpdate else HealthCardAction.Toggle
        )
    } else {
        when {
            healthReady && phoneReady -> HealthCardRenderState(
                headline = "Movement Bonus is active.",
                body = "Scyra uses the higher of Health Connect steps and phone-estimated steps during eligible Flows.",
                action = if (state.providerUpdateRequired) HealthCardAction.InstallOrUpdate else HealthCardAction.Toggle
            )
            healthReady -> HealthCardRenderState(
                headline = "Movement Bonus is active.",
                body = "Scyra can use Health Connect steps during eligible Flows. Watch steps may sync after the Flow.",
                action = if (state.providerUpdateRequired) HealthCardAction.InstallOrUpdate else HealthCardAction.Toggle
            )
            phoneReady -> HealthCardRenderState(
                headline = "Movement Bonus is active.",
                body = "Scyra can estimate steps from your phone during active Flows.",
                action = if (state.providerUpdateRequired) HealthCardAction.InstallOrUpdate else HealthCardAction.Toggle
            )
            else -> HealthCardRenderState(
                headline = "Movement Bonus needs setup.",
                body = "Movement Bonus needs Health Connect or phone step access to track steps.",
                action = if (state.providerUpdateRequired) HealthCardAction.InstallOrUpdate else HealthCardAction.Toggle
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f)
        ),
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
                    text = "Health / Movement Bonus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = renderState.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = renderState.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.86f)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Scyra uses step count data only to calculate Movement Points, Scyra Points, Pearls, and related reward displays.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                )



                if (state.localMovementBonusEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when {
                            healthReady && phoneReady -> {
                                Text(
                                    text = "Phone step estimate enabled.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = "Health Connect connected.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                                )
                            }
                            healthReady -> Text(
                                text = "Phone step estimate is off.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                            )
                            phoneReady -> Text(
                                text = "Health Connect is not connected.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                            )
                        }
                        if (!state.phoneStepSensorAvailable) {
                            Text(
                                text = "Phone step estimate is not available on this device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                            )
                        }
                        when (state.healthConnectAvailability) {
                            HealthConnectAvailability.UNAVAILABLE -> Text(
                                text = "Health Connect is not available on this device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                            )
                            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> Text(
                                text = "Health Connect needs to be installed or updated.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                            )
                            HealthConnectAvailability.AVAILABLE -> Unit
                        }
                    }
                }

                if (state.localMovementBonusEnabled && state.phoneStepSensorAvailable && !state.activityRecognitionPermissionGranted) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onEnablePhoneStepEstimate, enabled = !state.isBusy) {
                        Text("Enable phone estimate")
                    }
                }

                if (state.localMovementBonusEnabled && state.healthConnectAvailable && !state.readStepsPermissionGranted) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onConnectHealth, enabled = !state.isBusy) {
                        Text("Connect Health")
                    }
                }

                state.rawHealthConnectSdkStatus?.let { rawStatus ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Debug · availability=${state.healthConnectAvailability}, sdk=$rawStatus, stepsGranted=${state.readStepsPermissionGranted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.58f)
                    )
                }

                state.userMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                when (renderState.action) {
                    HealthCardAction.ConnectHealth -> {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onConnectHealth,
                            enabled = !state.isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text("Connect Health")
                        }
                    }

                    HealthCardAction.InstallOrUpdate -> {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onInstallOrUpdateHealthConnect,
                            enabled = !state.isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text("Install / Update Health Connect")
                        }
                    }

                    HealthCardAction.None,
                    HealthCardAction.Toggle -> Unit
                }
            }

            if (renderState.action == HealthCardAction.Toggle || renderState.action == HealthCardAction.InstallOrUpdate) {
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
            Text(
                "Some recent Flows may still be waiting for step data or improved Health Connect sync.\n\n" +
                        "If you disable Health now, Scyra will stop checking those Flows and any pending Movement Points may not be awarded.\n\n" +
                        "Already-awarded Movement Points will stay.\n\n" +
                        "You can turn Health back on later."
            )
        },
        confirmButton = {
            TextButton(onClick = onDisableAnyway) {
                Text("Disable Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepHealthOn) {
                Text("Keep Health On")
            }
        }
    )
}

@Composable
fun MovementBonusActivePill(
    estimatedPhoneSteps: Long?,
    estimatedMovementPoints: Long,
    phoneEstimateAvailable: Boolean,
    activityRecognitionPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
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

            val message = when {
                phoneEstimateAvailable && (estimatedPhoneSteps ?: 0L) > 0L -> "Movement Bonus active · ~${estimatedPhoneSteps ?: 0L} phone steps · +$estimatedMovementPoints estimated"
                phoneEstimateAvailable -> "Movement Bonus active · estimating phone steps…"
                !activityRecognitionPermissionGranted -> "Movement Bonus active · phone estimate off · watch steps may sync later"
                else -> "Movement Bonus active · final points update after sync"
            }
            Text(
                message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun MovementBonusRewardBlock(
    steps: Long,
    movementPoints: Long,
    movementIsPhoneEstimate: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (movementPoints <= 0L || steps <= 0L) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Movement Bonus",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                "$steps steps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                "+$movementPoints Movement Points",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            if (movementIsPhoneEstimate) {
                Text(
                    "Estimated from phone. Health Connect may update this later.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
fun FlowCardMovementLine(
    steps: Long?,
    movementPoints: Long,
    movementIsPhoneEstimate: Boolean = false,
    updatedAfterSync: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (movementPoints <= 0L || steps == null || steps <= 0L) return

    Column(modifier = modifier) {
        Text(
            text = "$steps steps · $movementPoints Movement Points",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        when {
            updatedAfterSync -> Text(
                text = "Movement added after sync",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
            )
            movementIsPhoneEstimate -> Text(
                text = "Estimated from phone",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
            )
        }
    }
}