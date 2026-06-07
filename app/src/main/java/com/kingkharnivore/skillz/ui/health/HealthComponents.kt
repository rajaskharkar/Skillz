package com.kingkharnivore.skillz.ui.health

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.health.HealthConnectAvailability
import com.kingkharnivore.skillz.viewmodel.health.HealthSettingsUiState
import com.kingkharnivore.skillz.viewmodel.health.HealthUserMessage

private data class HealthCardRenderState(
    @StringRes val headlineRes: Int,
    @StringRes val bodyRes: Int,
    @StringRes val actionLabelRes: Int? = null,
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
    val renderState = when {
        state.healthConnectAvailability == HealthConnectAvailability.UNAVAILABLE -> HealthCardRenderState(
            headlineRes = R.string.health_connect_unavailable_headline,
            bodyRes = R.string.health_connect_unavailable_body
        )
        state.providerUpdateRequired -> HealthCardRenderState(
            headlineRes = R.string.health_connect_update_required_headline,
            bodyRes = R.string.health_connect_update_required_body,
            actionLabelRes = R.string.health_connect_action_install_update,
            action = onInstallOrUpdateHealthConnect
        )
        state.healthConnectAvailable && !state.readStepsPermissionGranted -> HealthCardRenderState(
            headlineRes = R.string.health_connect_enable_headline,
            bodyRes = R.string.health_connect_enable_body,
            actionLabelRes = R.string.health_connect_action_connect,
            action = onConnectHealth
        )
        state.healthConnectAvailable && state.localMovementBonusEnabled -> HealthCardRenderState(
            headlineRes = R.string.movement_bonus_active_headline,
            bodyRes = R.string.movement_bonus_active_body,
            showSwitch = true
        )
        else -> HealthCardRenderState(
            headlineRes = R.string.movement_bonus_off_headline,
            bodyRes = R.string.movement_bonus_off_body,
            showSwitch = true
        )
    }

    val title = stringResource(R.string.health_settings_title)
    val headline = stringResource(renderState.headlineRes)
    val body = stringResource(renderState.bodyRes)
    val privacy = stringResource(R.string.movement_bonus_privacy_body)
    val userMessage = state.userMessage?.let { stringResource(it.messageRes()) }
    val toggleLabel = stringResource(R.string.movement_bonus_toggle_content_description)
    val toggleState = stringResource(
        if (state.toggleChecked) R.string.movement_bonus_toggle_state_on else R.string.movement_bonus_toggle_state_off
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        headline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
                    )
                }
                if (renderState.showSwitch) {
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = state.toggleChecked,
                        enabled = !state.isBusy,
                        onCheckedChange = onToggleMovementBonus,
                        modifier = Modifier.semantics {
                            contentDescription = toggleLabel
                            stateDescription = toggleState
                        }
                    )
                }
            }

            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
            Text(
                privacy,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
            )
            userMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (renderState.actionLabelRes != null && renderState.action != null) {
                Button(
                    onClick = renderState.action,
                    enabled = !state.isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(stringResource(renderState.actionLabelRes))
                }
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
        title = { Text(stringResource(R.string.health_disable_title)) },
        text = { Text(stringResource(R.string.health_disable_body)) },
        confirmButton = {
            TextButton(onClick = onDisableAnyway) { Text(stringResource(R.string.health_disable_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onKeepHealthOn) { Text(stringResource(R.string.health_disable_keep_on)) }
        }
    )
}

@Composable
fun MovementBonusActivePill(
    steps: Long,
    movementPoints: Long,
    isInFlow: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = stringResource(
        if (isInFlow) {
            R.string.movement_bonus_tracking_active
        } else {
            R.string.movement_bonus_tracking_paused
        }
    )
    val stepsText = stringResource(R.string.movement_bonus_steps_value, steps)
    val pointsText = stringResource(R.string.movement_bonus_points_value, movementPoints)
    val a11yText = stringResource(
        R.string.movement_bonus_active_pill_a11y,
        steps,
        movementPoints
    )

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = a11yText },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Outlined.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    stringResource(R.string.movement_bonus_active_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    stepsText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    pointsText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MovementBonusRewardBlock(steps: Long, movementPoints: Long, modifier: Modifier = Modifier) {
    if (movementPoints <= 0L || steps <= 0L) return
    val stepsText = pluralStringResource(R.plurals.movement_bonus_steps_count, steps.pluralQuantity(), steps)
    val pointsText = pluralStringResource(R.plurals.movement_bonus_points_count, movementPoints.pluralQuantity(), movementPoints)
    val a11yText = stringResource(R.string.movement_bonus_reward_a11y, stepsText, pointsText)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = a11yText },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.movement_bonus_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                stringResource(R.string.movement_bonus_reward_steps, stepsText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                stringResource(R.string.movement_bonus_reward_points, pointsText),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun FlowCardMovementLine(steps: Long?, movementPoints: Long, modifier: Modifier = Modifier) {
    if (movementPoints <= 0L || steps == null || steps <= 0L) return
    val stepsText = pluralStringResource(R.plurals.movement_bonus_steps_count, steps.pluralQuantity(), steps)
    val pointsText = pluralStringResource(R.plurals.movement_bonus_points_count, movementPoints.pluralQuantity(), movementPoints)
    val a11yText = stringResource(R.string.movement_bonus_flow_card_line_a11y, stepsText, pointsText)
    Text(
        text = stringResource(R.string.movement_bonus_flow_card_line, stepsText, pointsText),
        modifier = modifier.clearAndSetSemantics {
            contentDescription = a11yText
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary
    )
}

@StringRes
private fun HealthUserMessage.messageRes(): Int = when (this) {
    HealthUserMessage.HealthPermissionNotGranted -> R.string.health_user_message_permission_not_granted
    HealthUserMessage.CouldNotOpenHealthConnectPermissions -> R.string.health_user_message_permissions_unavailable
    HealthUserMessage.CouldNotOpenHealthConnectInPlayStore -> R.string.health_user_message_play_store_unavailable
}

private fun Long.pluralQuantity(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
