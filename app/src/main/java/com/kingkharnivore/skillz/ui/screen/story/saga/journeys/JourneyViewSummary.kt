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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun JourneyViewSummary(
    flowsCount: Int,
    totalDurationMs: Long,
    totalBaseScore: Int,
    totalBeamBonus: Int,
    totalScyraScore: Int,
    totalSurge: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            // Row 1: counts + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$flowsCount flow${if (flowsCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "⏱ ${formatDuration(totalDurationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Row 2: Scoring breakdown (no background pills)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreBreakdownRow(label = "Base", value = totalBaseScore.toString())
                if (totalBeamBonus > 0) ScoreBreakdownRow(label = "Beam bonus", value = "+$totalBeamBonus")
                ScoreBreakdownRow(label = "Scyra Score", value = "🔥 $totalScyraScore", strong = true)

                if (totalSurge > 0) {
                    ScoreBreakdownRow(label = "Surge", value = "+$totalSurge", strong = false)
                }
            }
        }
    }
}