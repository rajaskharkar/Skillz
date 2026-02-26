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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.utils.time.formatRange

@Composable
fun BeamDetailsSheetContent(
    b: BeamBlockUi,
    onClose: () -> Unit
) {
    val onJourney = Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = b.tagName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onJourney
        )

        val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)

        Text(
            text = "${formatRange(b.startMs, b.endMs)} • ${mins}m",
            style = MaterialTheme.typography.bodyMedium,
            color = onJourney.copy(alpha = 0.85f)
        )

        Text(
            text = "${b.status} • ${b.readiness.displayLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = onJourney.copy(alpha = 0.72f)
        )

        if (b.clippedTop || b.clippedBottom) {
            val note = buildString {
                if (b.clippedTop) append("Starts earlier (outside day).")
                if (b.clippedBottom) append("Ends later (outside day).")
            }.trim()

            Text(
                text = note,
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
            Text("Close")
        }

        Spacer(Modifier.height(6.dp))
    }
}