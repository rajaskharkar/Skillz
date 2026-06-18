package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.theme.CaveatSemiBold
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun ScoreDisplay(
    score: Int,
    surgeScore: Int,
    period: StoryPeriod,
    modifier: Modifier = Modifier,
    calmMode: Boolean = false
) {
    val periodLabel = when (period) {
        StoryPeriod.DAY -> stringResource(R.string.score_display_label_day)
        StoryPeriod.WEEK -> stringResource(R.string.score_display_label_week)
        StoryPeriod.MONTH -> stringResource(R.string.score_display_label_month)
    }

    val surgeLabel = stringResource(R.string.score_display_surge_bonus, surgeScore)

    val a11yLabel = if (!calmMode && surgeScore > 0) {
        stringResource(
            R.string.score_display_a11y_with_surge,
            score,
            surgeLabel,
            periodLabel
        )
    } else {
        stringResource(
            R.string.score_display_a11y_without_surge,
            score,
            periodLabel
        )
    }

    Box(
        modifier = modifier
            .padding(80.dp)
            .clearAndSetSemantics {
                contentDescription = a11yLabel
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 70.sp)
            )

            if (!calmMode && surgeScore > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = surgeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = periodLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = CaveatSemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}