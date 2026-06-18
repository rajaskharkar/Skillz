package com.kingkharnivore.skillz.ui.screen.story.saga.journeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun JourneyViewSummary(
    flowsCount: Int,
    totalDurationMs: Long,
    totalBaseScore: Int,
    totalScyraScore: Int,
    totalSurge: Int
) {
    val flowsText = pluralStringResource(
        R.plurals.journey_view_summary_flows_count,
        flowsCount,
        flowsCount
    )
    val durationText = formatDuration(totalDurationMs)
    val durationDisplayText = stringResource(
        R.string.journey_view_summary_duration_value,
        durationText
    )

    val baseLabel = stringResource(R.string.journey_view_summary_base)
    val scyraScoreLabel = stringResource(R.string.journey_view_summary_scyra_score)
    val surgeLabel = stringResource(R.string.journey_view_summary_surge)

    val scyraScoreValue = stringResource(
        R.string.journey_view_summary_score_value,
        totalScyraScore
    )
    val surgeValue = stringResource(
        R.string.journey_view_summary_plus_value,
        totalSurge
    )

    val a11yText = if (totalSurge > 0) {
        stringResource(
            R.string.journey_view_summary_a11y_with_surge,
            flowsText,
            durationText,
            totalBaseScore,
            totalScyraScore,
            totalSurge
        )
    } else {
        stringResource(
            R.string.journey_view_summary_a11y_no_surge,
            flowsText,
            durationText,
            totalBaseScore,
            totalScyraScore
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = a11yText
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = flowsText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = durationDisplayText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreBreakdownRow(
                    label = baseLabel,
                    value = totalBaseScore.toString()
                )

                ScoreBreakdownRow(
                    label = scyraScoreLabel,
                    value = scyraScoreValue,
                    strong = true
                )

                if (totalSurge > 0) {
                    ScoreBreakdownRow(
                        label = surgeLabel,
                        value = surgeValue,
                        strong = false
                    )
                }
            }
        }
    }
}