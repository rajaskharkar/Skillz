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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.theme.CaveatSemiBold
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun ScoreDisplay(
    score: Int,
    surgeScore: Int,
    period: StoryPeriod,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 70.sp)
            )

            if (surgeScore > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "+$surgeScore Surge",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = when (period) {
                    StoryPeriod.DAY -> "Scyra Score"
                    StoryPeriod.WEEK -> "This week"
                    StoryPeriod.MONTH -> "This month"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = CaveatSemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}