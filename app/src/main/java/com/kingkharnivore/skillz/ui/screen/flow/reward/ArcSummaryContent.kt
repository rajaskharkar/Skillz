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
    val duration = formatDuration(arc.totalDurationMs)
    val cards = buildArcSummaryRewardCards(
        arc = arc,
        isAera = isAera,
        calmMode = calmMode,
        text = rememberRewardRevealTextProvider(),
        durationText = duration
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.arc_summary_completed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.arc_summary_totals_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )
        RewardRevealDeck(cards = cards)
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
