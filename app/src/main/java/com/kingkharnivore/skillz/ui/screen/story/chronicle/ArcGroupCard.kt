package com.kingkharnivore.skillz.ui.screen.story.chronicle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration

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
    onLongPress: (FlowListItemUiModel) -> Unit,
    onClick: (Long) -> Unit
) {
    val wrapperBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    val cardBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val headerAccent = group.headerAccentColor ?: MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = wrapperBg,
                shape = RoundedCornerShape(24.dp)
            )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Arc Length · ${group.totalFlowsCount} flows",
                            style = MaterialTheme.typography.labelLarge,
                            color = headerAccent
                        )

                        Text(
                            text = "Total time · ${formatDuration(group.totalArcDurationMs)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
                        )
                    }

                    if (!isExpandedByFilter) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = if (isExpanded) "Hide flows" else "Show flows",
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
                            label = "Arc Score",
                            value = "${group.totalArcScore}",
                            accent = false
                        )

                        group.peakMultiplier?.let {
                            ArcStatChip(
                                label = "Peak",
                                value = "×${"%.1f".format(it)}",
                                accent = true,
                                accentColor = headerAccent
                            )
                        }
                    }

                    if (group.filteredJourneyDurationMs != null && group.filteredJourneyPercentOfArc != null) {
                        ArcStatChip(
                            label = "Journey",
                            value = "${formatDuration(group.filteredJourneyDurationMs)} • ${group.filteredJourneyPercentOfArc}%",
                            accent = true,
                            accentColor = headerAccent
                        )
                    }
                }
            }
        }

        if (isExpanded) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            group.visibleFlows.forEach { item ->
                FlowCard(
                    session = item.flow,
                    isExpanded = isExpandedFlow(item.flow.sessionId),
                    showScoreUi = showScoreUi,
                    calmMode = calmMode,
                    onToggleExpand = { onToggleFlowExpand(item.flow.sessionId) },
                    onDeleteSession = { onDeleteSession(item.flow.sessionId) },
                    onLongPress = { onLongPress(item.flow) },
                    onClick = { onClick(item.flow.sessionId) },
                    isArcGrouped = true,
                    isFirstInArcGroup = item.isFirstVisibleInArc,
                    isLastInArcGroup = item.isLastVisibleInArc
                )
            }

            if (group.hiddenFlowsCount > 0) {
                Text(
                    text = "${group.hiddenFlowsCount} other flows in arc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
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