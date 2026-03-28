package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun EmptyChroniclesState(
    period: StoryPeriod,
    isCurrentPeriod: Boolean,
    onTodayClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val title = if (isCurrentPeriod) {
        "No Flows yet"
    } else {
        "No Flows in this view"
    }

    val subtitle = when (period) {
        StoryPeriod.DAY -> {
            if (isCurrentPeriod) {
                "No Flows have been recorded today."
            } else {
                "No Flows were recorded for this day."
            }
        }
        StoryPeriod.WEEK -> {
            if (isCurrentPeriod) {
                "No Flows have been recorded this week."
            } else {
                "No Flows were recorded for this week."
            }
        }
        StoryPeriod.MONTH -> {
            if (isCurrentPeriod) {
                "No Flows have been recorded this month."
            } else {
                "No Flows were recorded for this month."
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Timeline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isCurrentPeriod && onTodayClick != null) {
                TextButton(onClick = onTodayClick) {
                    Text("Go to Today")
                }
            }
        }
    }
}