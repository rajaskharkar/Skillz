package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.model.BeamStatus
import com.kingkharnivore.skillz.utils.time.formatRange

@Composable
fun BeamDetailsSheetContent(
    b: BeamBlockUi,
    onClose: () -> Unit
) {
    val onJourney = Color.White
    val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)

    val timeText = stringResource(
        R.string.beam_details_time_and_minutes,
        formatRange(b.startMs, b.endMs),
        mins
    )

    val statusText = when (b.status) {
        BeamStatus.UPCOMING -> stringResource(R.string.beam_status_upcoming)
        BeamStatus.ACTIVE -> stringResource(R.string.beam_status_active)
        BeamStatus.COMPLETED_SUCCESS -> stringResource(R.string.beam_status_completed_success)
        BeamStatus.COMPLETED_PARTIAL -> stringResource(R.string.beam_status_completed_partial)
        BeamStatus.MISSED -> stringResource(R.string.beam_status_missed)
    }

    val statusAndReadinessText = stringResource(
        R.string.beam_details_status_and_readiness,
        statusText,
        b.readiness.displayLabel
    )

    val sheetA11y = stringResource(
        R.string.beam_details_sheet_a11y,
        b.tagName,
        timeText,
        statusAndReadinessText
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics {
                contentDescription = sheetA11y
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = b.tagName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onJourney
        )

        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = onJourney.copy(alpha = 0.85f)
        )

        Text(
            text = statusAndReadinessText,
            style = MaterialTheme.typography.labelMedium,
            color = onJourney.copy(alpha = 0.72f)
        )

        if (b.clippedTop) {
            Text(
                text = stringResource(R.string.beam_details_clipped_top_note),
                style = MaterialTheme.typography.bodySmall,
                color = onJourney.copy(alpha = 0.72f)
            )
        }

        if (b.clippedBottom) {
            Text(
                text = stringResource(R.string.beam_details_clipped_bottom_note),
                style = MaterialTheme.typography.bodySmall,
                color = onJourney.copy(alpha = 0.72f)
            )
        }

        Spacer(Modifier.height(6.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = onJourney.copy(alpha = 0.16f),
                contentColor = onJourney
            )
        ) {
            Text(stringResource(R.string.beam_details_close))
        }

        Spacer(Modifier.height(6.dp))
    }
}