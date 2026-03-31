package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel

@Composable
fun ArcSummaryContent(
    arc: ArcSummaryUiModel,
    isAera: Boolean,
    calmMode: Boolean
) {
    val completedText = stringResource(R.string.arc_summary_completed)
    val totalsSubtitle = stringResource(R.string.arc_summary_totals_subtitle)
    val cardTitle = stringResource(R.string.arc_summary_card_title)
    val cardSubtitle = if (calmMode || isAera) {
        stringResource(R.string.arc_summary_card_subtitle_time)
    } else {
        null
    }
    val flowsLabel = stringResource(R.string.arc_summary_flows)
    val flowsValue = stringResource(R.string.arc_summary_flows_value, arc.totalSessions)
    val durationLabel = stringResource(R.string.arc_summary_total_duration)
    val durationValue = formatDuration(arc.totalDurationMs)
    val peakMultiplierLabel = stringResource(R.string.arc_summary_peak_multiplier)
    val peakMultiplierValue = stringResource(
        R.string.arc_summary_multiplier_value,
        arc.peakMultiplier
    )
    val bonusLabel = stringResource(R.string.arc_summary_bonus_points)
    val bonusValue = stringResource(
        R.string.arc_summary_points_value,
        arc.totalArcBonusPoints
    )
    val totalScoreLabel = stringResource(R.string.arc_summary_total_score)
    val totalScoreValue = stringResource(
        R.string.arc_summary_points_value,
        arc.totalFinalPoints
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = completedText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = totalsSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )

        RewardCard(
            title = cardTitle,
            subtitle = cardSubtitle
        ) {
            MetricLine(flowsLabel, flowsValue, MetricTone.Neutral)
            MetricLine(durationLabel, durationValue, MetricTone.Neutral)

            if (!isAera && !calmMode) {
                DividerSoft()
                MetricLine(peakMultiplierLabel, peakMultiplierValue, MetricTone.Glow)
                HighlightMetric(bonusLabel, bonusValue, glow = true)
                HighlightMetric(totalScoreLabel, totalScoreValue, glow = true)
            }
        }
    }
}

@Composable
private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        stringResource(R.string.arc_summary_duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.arc_summary_duration_minutes, minutes)
    }
}