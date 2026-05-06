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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.utils.time.formatDuration

@Composable
fun JourneySessionDetail(
    session: FlowListItemUiModel,
    onOpenFull: (() -> Unit)? = null
) {
    val noDescriptionText = stringResource(R.string.journey_session_detail_no_description)
    val durationLabel = stringResource(R.string.journey_session_detail_duration)
    val baseScoreLabel = stringResource(R.string.journey_session_detail_base_score)
    val scyraScoreLabel = stringResource(R.string.journey_session_detail_scyra_score)
    val surgeLabel = stringResource(R.string.journey_session_detail_surge)
    val openFullFlowText = stringResource(R.string.journey_session_detail_open_full_flow)

    val durationText = formatDuration(session.durationMs)
    val baseScoreValue = session.score.toString()

    val scyraScoreValue = stringResource(
        R.string.journey_session_detail_scyra_score_value,
        session.score
    )
    val surgeValue = stringResource(
        R.string.journey_session_detail_surge_value,
        session.surgePoints
    )

    val descriptionText = if (session.description.isNotBlank()) {
        session.description
    } else {
        noDescriptionText
    }

    val cardA11y = if (session.description.isNotBlank()) {
        stringResource(
            R.string.journey_session_detail_a11y_with_description,
            session.title,
            durationText,
            session.description
        )
    } else {
        stringResource(
            R.string.journey_session_detail_a11y_without_description,
            session.title,
            durationText,
            noDescriptionText
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = cardA11y
            },
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
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clearAndSetSemantics {
                    heading()
                    contentDescription = session.title
                }
            )

            if (session.tagName.isNotBlank()) {
                Text(
                    text = session.tagName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (session.description.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))

            DetailStatRow(
                label = durationLabel,
                value = durationText
            )

            DetailStatRow(
                label = baseScoreLabel,
                value = baseScoreValue
            )

            DetailStatRow(
                label = scyraScoreLabel,
                value = scyraScoreValue,
                strong = true
            )

            if (session.isSurge && session.surgePoints > 0) {
                DetailStatRow(
                    label = surgeLabel,
                    value = surgeValue
                )
            }

            if (onOpenFull != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onOpenFull,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(openFullFlowText)
                }
            }
        }
    }
}