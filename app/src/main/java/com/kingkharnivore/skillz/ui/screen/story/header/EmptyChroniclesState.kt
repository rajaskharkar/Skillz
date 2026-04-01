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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun EmptyChroniclesState(
    period: StoryPeriod,
    isCurrentPeriod: Boolean,
    onTodayClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val title = if (isCurrentPeriod) {
        stringResource(R.string.empty_chronicles_title_current)
    } else {
        stringResource(R.string.empty_chronicles_title_past)
    }

    val subtitle = when (period) {
        StoryPeriod.DAY -> {
            if (isCurrentPeriod) {
                stringResource(R.string.empty_chronicles_day_current)
            } else {
                stringResource(R.string.empty_chronicles_day_past)
            }
        }
        StoryPeriod.WEEK -> {
            if (isCurrentPeriod) {
                stringResource(R.string.empty_chronicles_week_current)
            } else {
                stringResource(R.string.empty_chronicles_week_past)
            }
        }
        StoryPeriod.MONTH -> {
            if (isCurrentPeriod) {
                stringResource(R.string.empty_chronicles_month_current)
            } else {
                stringResource(R.string.empty_chronicles_month_past)
            }
        }
    }

    val goToTodayText = stringResource(R.string.empty_chronicles_go_to_today)
    val a11yText = stringResource(R.string.empty_chronicles_a11y, title, subtitle)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = a11yText
            },
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isCurrentPeriod && onTodayClick != null) {
                TextButton(onClick = onTodayClick) {
                    Text(goToTodayText)
                }
            }
        }
    }
}