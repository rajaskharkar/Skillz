package com.kingkharnivore.skillz.ui.screen.story.chronicle

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration
import java.util.Locale

@Composable
fun FlowCard(
    session: FlowListItemUiModel,
    isExpanded: Boolean,
    showScoreUi: Boolean,
    calmMode: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteSession: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    childPulses: List<PulseListItemUiModel> = emptyList(),
    onEditPulse: (PulseListItemUiModel) -> Unit = {},
    onDeletePulse: (Long) -> Unit = {},
    isArcGrouped: Boolean = false,
    isFirstInArcGroup: Boolean = false,
    isLastInArcGroup: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expandedChildPulseIds by rememberSaveable(session.sessionId) { mutableStateOf(setOf<Long>()) }

    val isSoft = session.isSoftMode
    val showSurgeStat = !isSoft && session.isSurge && session.surgePoints > 0
    val isBeamed = !isSoft && session.beamBonusPoints > 0

    val journeyTint = lerp(
        MaterialTheme.colorScheme.surface,
        session.journeyColor,
        if (isArcGrouped) 0.10f else 0.14f
    )

    val baseContainer = when {
        isSoft -> MaterialTheme.colorScheme.secondary
        session.isSurge -> lerp(journeyTint, MaterialTheme.colorScheme.surfaceVariant, 0.22f)
        else -> journeyTint
    }

    val contentColor = if (isSoft) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val cardShape = if (isArcGrouped) {
        RoundedCornerShape(
            topStart = if (isFirstInArcGroup) 18.dp else 10.dp,
            topEnd = if (isFirstInArcGroup) 18.dp else 10.dp,
            bottomStart = if (isLastInArcGroup) 18.dp else 10.dp,
            bottomEnd = if (isLastInArcGroup) 18.dp else 10.dp
        )
    } else {
        RoundedCornerShape(20.dp)
    }

    val labelColor = when {
        isSoft -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.82f)
        else -> lerp(
            MaterialTheme.colorScheme.onSurface,
            session.journeyColor,
            if (isSystemInDarkTheme()) 0.70f else 0.55f
        )
    }

    val flowTypeLabel = stringResource(
        if (isSoft) R.string.flow_card_type_soft else R.string.flow_card_type_flow
    )
    val expandedStateLabel = stringResource(R.string.a11y_expanded)
    val collapsedStateLabel = stringResource(R.string.a11y_collapsed)
    val editFlowLabel = stringResource(R.string.flow_card_edit_flow)
    val expandFlowLabel = stringResource(R.string.flow_card_expand)
    val collapseFlowLabel = stringResource(R.string.flow_card_collapse)
    val openDetailsLabel = stringResource(R.string.flow_card_open_details)
    val deleteFlowLabel = stringResource(R.string.flow_card_delete_flow)

    val durationLabel = stringResource(
        R.string.flow_card_duration_value,
        formatDuration(session.durationMs)
    )

    val multiplierLabel = session.arcMultiplierUsed?.let {
        stringResource(
            R.string.flow_card_multiplier_used_value,
            String.format(Locale.getDefault(), "%.1f", it)
        )
    }

    val scoreLabel = stringResource(R.string.flow_card_scyra_score_value, session.score)
    val surgeA11yLabel = stringResource(R.string.flow_card_surge_points_value, session.surgePoints)
    val surgeCompactLabel = stringResource(R.string.flow_card_surge_points_compact, session.surgePoints)

    val momentsSummary = pluralStringResource(
        R.plurals.flow_card_moments_count,
        childPulses.size,
        childPulses.size
    )

    val stateLabel = if (isExpanded) expandedStateLabel else collapsedStateLabel
    val toggleLabel = if (isExpanded) collapseFlowLabel else expandFlowLabel

    val cardContentDescription = buildString {
        append(session.title)
        append(", ")
        append(flowTypeLabel)

        if (session.tagName.isNotBlank()) {
            append(", ")
            append(session.tagName)
        }

        append(", ")
        append(durationLabel)

        if (showSurgeStat) {
            append(", ")
            append(surgeA11yLabel)
        }

        if (!isSoft && showScoreUi && !calmMode) {
            append(", ")
            append(scoreLabel)
        }

        if (childPulses.isNotEmpty()) {
            append(", ")
            append(momentsSummary)
        }
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .semantics {
            role = Role.Button
            contentDescription = cardContentDescription
            stateDescription = stateLabel
            onLongClick(label = editFlowLabel) {
                onLongPress()
                true
            }
            customActions = listOf(
                CustomAccessibilityAction(label = toggleLabel) {
                    onToggleExpand()
                    true
                },
                CustomAccessibilityAction(label = openDetailsLabel) {
                    onClick()
                    true
                }
            )
        }
        .combinedClickable(
            onClick = {
                onToggleExpand()
                onClick()
            },
            onLongClick = onLongPress
        )

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = baseContainer,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSoft) {
                                Icons.Outlined.Spa
                            } else {
                                Icons.Outlined.AutoAwesome
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = labelColor
                        )

                        if (session.tagName.isNotBlank()) {
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = session.tagName,
                                style = MaterialTheme.typography.labelMedium,
                                color = labelColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!calmMode && isBeamed) {
                        BeamBonusChip(bonusPoints = session.beamBonusPoints)
                        Spacer(Modifier.height(6.dp))
                    }

                    if (!calmMode && showSurgeStat) {
                        val dark = isSystemInDarkTheme()

                        val surgeTint = lerp(
                            MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.secondary,
                            if (dark) 0.75f else 0.45f
                        )

                        Text(
                            text = surgeCompactLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = surgeTint
                        )

                        Spacer(Modifier.height(6.dp))
                    }

                    if (isExpanded) {
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = deleteFlowLabel,
                                tint = contentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.10f))
            Spacer(modifier = Modifier.height(10.dp))

            if (session.description.isNotBlank()) {
                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = durationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )

            if (!isSoft && !calmMode && session.arcMultiplierUsed != null && multiplierLabel != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = multiplierLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor
                )
            }

            if (!isSoft && showScoreUi && !calmMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = scoreLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }

            if (childPulses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PsychologyAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = contentColor.copy(alpha = 0.82f)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = momentsSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.82f)
                    )
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        childPulses.forEach { pulse ->
                            val pulseExpanded = expandedChildPulseIds.contains(pulse.pulseId)

                            PulseCard(
                                pulse = pulse,
                                isExpanded = pulseExpanded,
                                onToggleExpand = {
                                    expandedChildPulseIds =
                                        if (expandedChildPulseIds.contains(pulse.pulseId)) {
                                            expandedChildPulseIds - pulse.pulseId
                                        } else {
                                            expandedChildPulseIds + pulse.pulseId
                                        }
                                },
                                onLongPress = { onEditPulse(pulse) },
                                onDeletePulse = { onDeletePulse(pulse.pulseId) },
                                nested = true
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.flow_card_delete_dialog_title)) },
            text = { Text(stringResource(R.string.flow_card_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSession()
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}