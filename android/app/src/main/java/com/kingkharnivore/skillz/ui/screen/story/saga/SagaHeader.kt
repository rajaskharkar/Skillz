package com.kingkharnivore.skillz.ui.screen.story.saga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun SagaHeader(
    title: String,
    subtitle: String,
    periodLabel: String,
    totalFlows: Int,
    totalDurationMs: Long,
    totalScore: Int
) {
    val totalSurgeScore = 0

    val flowsLabel = stringResource(R.string.saga_header_flows)
    val durationLabel = stringResource(R.string.saga_header_duration)
    val scoreLabel = stringResource(R.string.saga_header_score)
    val durationText = formatDuration(totalDurationMs)
    val scoreValueText = stringResource(R.string.saga_header_score_value, totalScore)
    val surgeValueText = stringResource(R.string.saga_header_surge_value, totalSurgeScore)

    val topA11y = if (subtitle.isNotBlank()) {
        stringResource(
            R.string.saga_header_top_a11y_with_subtitle,
            title,
            subtitle,
            periodLabel
        )
    } else {
        stringResource(
            R.string.saga_header_top_a11y_no_subtitle,
            title,
            periodLabel
        )
    }

    val statsA11y = if (totalSurgeScore > 0) {
        stringResource(
            R.string.saga_header_stats_a11y_with_surge,
            totalFlows,
            durationText,
            totalScore,
            totalSurgeScore
        )
    } else {
        stringResource(
            R.string.saga_header_stats_a11y,
            totalFlows,
            durationText,
            totalScore
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    heading()
                    contentDescription = topA11y
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📜", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = periodLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = statsA11y
                },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SagaHeaderStat(
                    label = flowsLabel,
                    value = totalFlows.toString()
                )

                SagaHeaderStat(
                    label = durationLabel,
                    value = durationText
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SagaHeaderStat(
                        label = scoreLabel,
                        value = scoreValueText,
                        alignEnd = true
                    )

                    if (totalSurgeScore > 0) {
                        Text(
                            text = surgeValueText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}