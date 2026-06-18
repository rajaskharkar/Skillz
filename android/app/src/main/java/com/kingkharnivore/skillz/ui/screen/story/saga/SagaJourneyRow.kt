package com.kingkharnivore.skillz.ui.screen.story.saga

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.Journey7dStatUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun SagaJourneyRow(
    rank: Int,
    stat: Journey7dStatUiModel,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val accentAlpha = when (rank) {
        1 -> 0.28f
        2 -> 0.22f
        3 -> 0.18f
        else -> 0.14f
    }

    val flowCountText = pluralStringResource(
        R.plurals.saga_journey_row_flows_count,
        stat.sessionsCount,
        stat.sessionsCount
    )
    val durationText = formatDuration(stat.totalDurationMs)
    val rankText = stringResource(R.string.saga_journey_row_rank, rank)
    val metaText = stringResource(R.string.saga_journey_row_meta, flowCountText, durationText)
    val scoreText = stringResource(R.string.saga_journey_row_score_value, stat.totalScore)
    val scoreA11y = stringResource(R.string.saga_journey_row_score_a11y, stat.totalScore)
    val rowA11y = stringResource(
        R.string.saga_journey_row_a11y,
        stat.tagName,
        rank,
        metaText,
        stat.totalScore
    )
    val openDetailsLabel = stringResource(R.string.saga_journey_row_open_details)

    Surface(
        onClick = onClick,
        modifier = Modifier.clearAndSetSemantics {
            role = Role.Button
            contentDescription = rowA11y
        },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = cs.surfaceVariant,
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(44.dp)
                    .background(
                        color = cs.secondary.copy(alpha = accentAlpha),
                        shape = RoundedCornerShape(999.dp)
                    )
            )

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cs.secondary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, cs.secondary.copy(alpha = 0.22f))
            ) {
                Text(
                    text = rankText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.secondary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stat.tagName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = cs.onSurface
                )

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = scoreA11y
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = cs.secondary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, cs.secondary.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = scoreText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = openDetailsLabel,
                    modifier = Modifier.size(14.dp),
                    tint = cs.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}