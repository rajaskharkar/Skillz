package com.kingkharnivore.skillz.ui.screen.flow.reward

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.state.flow.ArcSummaryUiModel

@Composable
fun ArcSummaryContent(
    arc: ArcSummaryUiModel,
    isAera: Boolean,
    calmMode: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Arc completed.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Totals across this Arc.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )

        RewardCard(
            title = "Arc totals",
            subtitle = if (calmMode || isAera) "Your Story in Time" else null
        ) {
            MetricLine("Flows", "${arc.totalSessions}", MetricTone.Neutral)
            MetricLine("Total duration", formatDuration(arc.totalDurationMs), MetricTone.Neutral)

            if (!isAera && !calmMode) {
                DividerSoft()
                MetricLine("Peak multiplier", "×${"%.1f".format(arc.peakMultiplier)}", MetricTone.Glow)
                HighlightMetric("Arc bonus points", "+${arc.totalArcBonusPoints}", glow = true)
                HighlightMetric("Total Scyra Score", "+${arc.totalFinalPoints}", glow = true)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}