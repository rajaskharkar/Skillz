package com.kingkharnivore.skillz.ui.screen.story.chronicle

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArcGroupCard(
    group: ChronicleUiModel.ArcGroup,
    showScoreUi: Boolean,
    calmMode: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    isExpandedByFilter: Boolean,
    isExpandedFlow: (Long) -> Boolean,
    onToggleFlowExpand: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onEditPulse: (PulseListItemUiModel) -> Unit,
    onDeletePulse: (Long) -> Unit,
    onLongPress: (FlowListItemUiModel) -> Unit,
    onClick: (Long) -> Unit,
    onEditDetails: () -> Unit
) {
    val arcTimeRangeLabel = rememberArcTimeRangeLabel(group.visibleFlows.map { it.flow.createdAt })
    val wrapperBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    val cardBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val headerAccent = group.headerAccentColor ?: MaterialTheme.colorScheme.secondary

    val flowCountLabel = pluralStringResource(
        R.plurals.arc_group_flow_count_label,
        group.totalFlowsCount,
        group.totalFlowsCount
    )
    val totalTimeLabel = stringResource(
        R.string.arc_group_total_time,
        formatDuration(group.totalArcDurationMs)
    )
    val showFlowsLabel = stringResource(R.string.arc_group_show_flows)
    val hideFlowsLabel = stringResource(R.string.arc_group_hide_flows)
    val arcScoreLabel = stringResource(R.string.arc_group_stat_arc_score)
    val peakLabel = stringResource(R.string.arc_group_stat_peak)
    val journeyLabel = stringResource(R.string.arc_group_stat_journey)
    val expandedStateLabel = stringResource(R.string.a11y_expanded)
    val collapsedStateLabel = stringResource(R.string.a11y_collapsed)
    val expandArcLabel = stringResource(R.string.arc_group_a11y_expand)
    val collapseArcLabel = stringResource(R.string.arc_group_a11y_collapse)

    val stateLabel = when {
        isExpandedByFilter -> expandedStateLabel
        isExpanded -> expandedStateLabel
        else -> collapsedStateLabel
    }

    val arcSummaryLabel = buildString {
        append(flowCountLabel)
        append(", ")
        append(totalTimeLabel)

        if (!calmMode && showScoreUi) {
            append(", ")
            append(arcScoreLabel)
            append(": ")
            append(group.totalArcScore)

            group.peakMultiplier?.let {
                append(", ")
                append(peakLabel)
                append(": ×")
                append(String.format(Locale.getDefault(), "%.1f", it))
            }
        }

        if (group.filteredJourneyDurationMs != null && group.filteredJourneyPercentOfArc != null) {
            append(", ")
            append(journeyLabel)
            append(": ")
            append(formatDuration(group.filteredJourneyDurationMs))
            append(" • ")
            append(group.filteredJourneyPercentOfArc)
            append("%")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = wrapperBg,
                shape = RoundedCornerShape(24.dp)
            )
            .semantics {
                role = Role.Button
                contentDescription = arcSummaryLabel
                stateDescription = stateLabel
                if (!isExpandedByFilter) {
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = if (isExpanded) collapseArcLabel else expandArcLabel
                        ) {
                            onToggleExpanded()
                            true
                        }
                    )
                }
            }
            .clickable(enabled = !isExpandedByFilter) { onToggleExpanded() }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                group.metadata?.let { metadata ->
                    if (metadata.title != null || metadata.summary != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            metadata.title?.let {
                                Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface, maxLines = if (isExpanded) 2 else 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            metadata.summary?.let { summary ->
                                var overflowed by remember(summary, isExpanded) { mutableStateOf(false) }
                                Text(summary, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isExpanded) 6 else 2, overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { overflowed = it.hasVisualOverflow })
                                if (isExpanded && overflowed) {
                                    TextButton(onClick = onEditDetails) { Text("Read full summary") }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoGraph,
                                contentDescription = null,
                                tint = headerAccent
                            )

                            Text(
                                text = flowCountLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = headerAccent
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = totalTimeLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
                            )

                            arcTimeRangeLabel?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                                )
                            }
                        }
                    }

                    if (!isExpandedByFilter) {
                        Surface(
                            modifier = Modifier.clearAndSetSemantics {},
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = if (isExpanded) hideFlowsLabel else showFlowsLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!calmMode && showScoreUi) {
                        ArcStatChip(
                            label = arcScoreLabel,
                            value = "${group.totalArcScore}",
                            accent = false
                        )

                        group.peakMultiplier?.let {
                            ArcStatChip(
                                label = peakLabel,
                                value = "×${String.format(Locale.getDefault(), "%.1f", it)}",
                                accent = true,
                                accentColor = headerAccent
                            )
                        }
                    }

                    if (group.filteredJourneyDurationMs != null && group.filteredJourneyPercentOfArc != null) {
                        ArcStatChip(
                            label = journeyLabel,
                            value = "${formatDuration(group.filteredJourneyDurationMs)} • ${group.filteredJourneyPercentOfArc}%",
                            accent = true,
                            accentColor = headerAccent
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEditDetails) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Arc details", modifier = Modifier.padding(3.dp))
                    }
                }
            }
        }

        if (isExpanded) {
            group.metadata?.takeIf { it.hasReflection }?.let { metadata ->
                Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Arc reflection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    metadata.outcome?.let { ReflectionField("Outcome", it) }
                    metadata.highlight?.let { ReflectionField("Highlight", it) }
                    metadata.nextStep?.let { ReflectionField("Next step", it) }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            group.visibleFlows.forEach { item ->
                FlowCard(
                    session = item.flow,
                    childPulses = item.childPulses,
                    isExpanded = isExpandedFlow(item.flow.sessionId),
                    showScoreUi = showScoreUi,
                    calmMode = calmMode,
                    onToggleExpand = { onToggleFlowExpand(item.flow.sessionId) },
                    onDeleteSession = { onDeleteSession(item.flow.sessionId) },
                    onLongPress = { onLongPress(item.flow) },
                    onClick = { onClick(item.flow.sessionId) },
                    onEditPulse = onEditPulse,
                    onDeletePulse = onDeletePulse,
                    isArcGrouped = true,
                    isFirstInArcGroup = item.isFirstVisibleInArc,
                    isLastInArcGroup = item.isLastVisibleInArc
                )
            }

            if (group.hiddenFlowsCount > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.arc_group_hidden_flows,
                        group.hiddenFlowsCount,
                        group.hiddenFlowsCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ReflectionField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ArcStatChip(
    label: String,
    value: String,
    accent: Boolean,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val valueColor = if (accent) {
        accentColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    }

    Surface(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$label, $value"
        },
        shape = RoundedCornerShape(999.dp),
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor
            )

            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = valueColor
            )
        }
    }
}

@Composable
private fun rememberArcTimeRangeLabel(times: List<Long>): String? {
    if (times.isEmpty()) return null

    val sorted = times.sorted()
    val start = sorted.first()
    val end = sorted.last()

    return if (start == end) {
        formatChronicleTime(start)
    } else {
        "${formatChronicleTime(start)} --> ${formatChronicleTime(end)}"
    }
}

private fun formatChronicleTime(timeMs: Long): String {
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdhmma")
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMs))
}
