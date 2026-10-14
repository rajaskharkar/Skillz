package com.kingkharnivore.skillz.ui.screen.story.saga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveCardUiState

@Composable
fun SagaObjectivesCard(
    objectives: List<ObjectiveCardUiState>,
    onLaunchFlow: (String, FlowMode) -> Unit,
    onClaim: (Long) -> Unit,
    claimEnabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    val sections = remember(objectives) {
        val byPeriod = objectives.groupBy { it.period }
        listOf(ObjectivePeriod.Daily, ObjectivePeriod.Weekly, ObjectivePeriod.Monthly)
            .mapNotNull { period -> byPeriod[period]?.let { period to it } }
    }
    val expansionState = stringResource(
        if (expanded) R.string.sagas_card_expanded else R.string.sagas_card_collapsed
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.07f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics { heading() }
                        ) {
                            Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(20.dp), tint = cs.secondary)
                            Text(stringResource(R.string.saga_objectives_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            stringResource(R.string.saga_objectives_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = cs.surfaceVariant, tonalElevation = 1.dp) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SagaHeaderStat(stringResource(R.string.saga_objectives_total), objectives.size.toString())
                            SagaHeaderStat(stringResource(R.string.saga_objectives_active), objectives.count { it.state == ObjectiveCardState.InProgress }.toString())
                            SagaHeaderStat(stringResource(R.string.saga_objectives_completed), objectives.count { it.state == ObjectiveCardState.Completed }.toString(), alignEnd = true)
                        }
                    }
                }
                FilledTonalIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(40.dp).semantics { stateDescription = expansionState },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = cs.surfaceVariant, contentColor = cs.onSurfaceVariant)
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(if (expanded) R.string.saga_objectives_collapse else R.string.saga_objectives_expand)
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sections.forEach { (period, periodObjectives) ->
                        key(period) {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(when (period) {
                                            ObjectivePeriod.Daily -> R.string.lookout_period_daily
                                            ObjectivePeriod.Weekly -> R.string.lookout_period_weekly
                                            ObjectivePeriod.Monthly -> R.string.lookout_period_monthly
                                        }),
                                        modifier = Modifier.semantics { heading() },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cs.secondary
                                    )
                                    HorizontalDivider(Modifier.weight(1f), color = cs.onSurface.copy(alpha = 0.07f))
                                }
                                periodObjectives.forEachIndexed { index, objective ->
                                    key(objective.objectiveId, objective.completionId) {
                                        if (index > 0) HorizontalDivider(color = cs.onSurface.copy(alpha = 0.07f))
                                        SagaObjectiveRow(objective, onLaunchFlow, onClaim, claimEnabled)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SagaObjectiveRow(
    objective: ObjectiveCardUiState,
    onLaunchFlow: (String, FlowMode) -> Unit,
    onClaim: (Long) -> Unit,
    claimEnabled: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val flowLabel = stringResource(when (objective.flowMode) {
        FlowMode.FLOW -> R.string.lookout_flow_regular
        FlowMode.SOFT -> R.string.lookout_flow_soft
        FlowMode.POWER -> R.string.lookout_flow_power
    })
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 4.dp).width(6.dp).height(44.dp).background(cs.secondary.copy(alpha = 0.22f), RoundedCornerShape(999.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(objective.journeyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    Text(flowLabel, style = MaterialTheme.typography.labelSmall, color = cs.secondary)
                }
                // Flow is a journey shortcut in every objective state; claiming is independent.
                IconButton(onClick = { onLaunchFlow(objective.journeyName, objective.flowMode) }) {
                    Icon(Icons.Outlined.PlayArrow, stringResource(R.string.lookout_start_flow), tint = cs.secondary)
                }
                if (objective.state == ObjectiveCardState.Completed && !objective.pearlsClaimed) {
                    IconButton(
                        onClick = { objective.completionId?.let(onClaim) },
                        enabled = claimEnabled && objective.completionId != null
                    ) {
                        Icon(Icons.Outlined.Redeem, stringResource(R.string.lookout_claim))
                    }
                }
            }
            Text(objective.timeLeftLabel, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant.copy(alpha = 0.78f))
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(objective.progressLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(stringResource(R.string.lookout_percent, objective.progressPercent), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.secondary)
            }
            LinearProgressIndicator(
                progress = { objective.progressPercent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                color = cs.secondary,
                trackColor = cs.secondary.copy(alpha = 0.12f)
            )
        }
    }
}
