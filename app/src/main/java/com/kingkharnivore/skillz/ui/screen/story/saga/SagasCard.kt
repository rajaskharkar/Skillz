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
import androidx.compose.ui.unit.dp
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
        mutableStateOf(true) // dedicated view => expanded feels right
    }

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
            // ── Header row + expand toggle ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SagaHeader(
                        title = "Your Saga",
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
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = cs.surfaceVariant,
                        contentColor = cs.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // ── Body ───────────────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (stats.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cs.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = "No saga data in this view.",
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