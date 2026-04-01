package com.kingkharnivore.skillz.ui.screen.story.saga.journeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.ui.screen.helpers.journeySessionMeta

@Composable
fun JourneySessionRow(
    session: FlowListItemUiModel,
    onExpand: () -> Unit,
    onScry: () -> Unit
) {
    val metaText = journeySessionMeta(session)
    val scoreText = stringResource(R.string.journey_session_row_score_value, session.score)
    val scryText = stringResource(R.string.journey_session_row_scry)
    val scryA11y = stringResource(R.string.journey_session_row_scry_a11y)
    val scoreA11y = stringResource(R.string.journey_session_row_score_a11y, session.score)
    val rowA11y = stringResource(
        R.string.journey_session_row_a11y,
        session.title,
        metaText,
        session.score
    )

    Surface(
        onClick = onExpand,
        modifier = Modifier.clearAndSetSemantics {
            role = Role.Button
            contentDescription = rowA11y
        },
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics {
                        heading()
                        contentDescription = session.title
                    }
                )

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = scoreA11y
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔥", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onScry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = scryA11y
                    }
                ) {
                    Text(
                        text = scryText,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}