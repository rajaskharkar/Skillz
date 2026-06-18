package com.kingkharnivore.skillz.ui.screen.story.saga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.Journey7dStatUiModel
import com.kingkharnivore.skillz.ui.screen.helpers.sagaSubtitle
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun SagasCard(
    period: StoryPeriod,
    anchorDayStartMs: Long,
    stats: List<Journey7dStatUiModel>,
    onOpenViewJourneys: (tagId: Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val totalFlows = remember(stats) { stats.sumOf { it.sessionsCount } }
    val totalDuration = remember(stats) { stats.sumOf { it.totalDurationMs } }
    val totalScore = remember(stats) { stats.sumOf { it.totalScore } }

    var expanded by rememberSaveable(period, anchorDayStartMs) {
        mutableStateOf(true)
    }

    val titleText = stringResource(R.string.sagas_card_title)
    val expandText = stringResource(R.string.sagas_card_expand)
    val collapseText = stringResource(R.string.sagas_card_collapse)
    val emptyText = stringResource(R.string.sagas_card_empty)
    val expandedText = stringResource(R.string.sagas_card_expanded)
    val collapsedText = stringResource(R.string.sagas_card_collapsed)
    val toggleStateText = stringResource(
        R.string.sagas_card_toggle_state,
        if (expanded) collapseText else expandText,
        if (expanded) expandedText else collapsedText
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                ) {
                    SagaHeader(
                        title = titleText,
                        subtitle = sagaSubtitle(period, anchorDayStartMs),
                        periodLabel = period.label,
                        totalFlows = totalFlows,
                        totalDurationMs = totalDuration,
                        totalScore = totalScore
                    )
                }

                Spacer(Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (expanded) collapseText else expandText
                            stateDescription = toggleStateText
                        },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = cs.surfaceVariant,
                        contentColor = cs.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (stats.isEmpty()) {
                        Surface(
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = emptyText
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = cs.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = emptyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        stats.forEachIndexed { index, stat ->
                            SagaJourneyRow(
                                rank = index + 1,
                                stat = stat,
                                onClick = { onOpenViewJourneys(stat.tagId) }
                            )
                        }
                    }
                }
            }
        }
    }
}