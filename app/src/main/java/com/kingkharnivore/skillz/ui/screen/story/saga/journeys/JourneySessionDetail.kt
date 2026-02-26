package com.kingkharnivore.skillz.ui.screen.story.saga.journeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.entity.FlowListItemUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun JourneySessionDetail(
    session: FlowListItemUiModel,
    onOpenFull: (() -> Unit)? = null // optional
) {
    val baseScore = remember(session.score, session.beamBonusPoints) {
        (session.score - session.beamBonusPoints).coerceAtLeast(0)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // If you want: show tag name too (optional)
            if (session.tagName.isNotBlank()) {
                Text(
                    text = session.tagName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (session.description.isNotBlank()) {
                Text(text = session.description, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "No description yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))

            // Core stats
            DetailStatRow(label = "Duration", value = formatDuration(session.durationMs))

            // Score breakdown
            DetailStatRow(label = "Base score", value = baseScore.toString())
            if (session.beamBonusPoints > 0) DetailStatRow(label = "Beam bonus", value = "+${session.beamBonusPoints}")
            DetailStatRow(label = "Scyra Score", value = "🔥 ${session.score}", strong = true)

            if (session.isSurge && session.surgePoints > 0) {
                DetailStatRow(label = "Surge", value = "+${session.surgePoints}")
            }

            if (onOpenFull != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onOpenFull,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open full flow")
                }
            }
        }
    }
}