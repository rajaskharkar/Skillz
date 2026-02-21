package com.kingkharnivore.skillz.ui.screen.components.atlas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.atlas.model.BeamBlockUi
import com.kingkharnivore.skillz.utils.time.formatRange

@Composable
fun BeamCard(
    b: BeamBlockUi,
    h: Dp,
    onBeamClick: (BeamBlockUi) -> Unit
) {
    val journeyColor = Color(b.journeyColorArgb)
    val bg = journeyColor.copy(alpha = 0.16f)
    val accent = journeyColor.copy(alpha = 0.9f)

    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(h),
        colors = CardDefaults.cardColors(
            containerColor = bg,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { onBeamClick(b) }
    ) {
        Row(Modifier.Companion.fillMaxSize()) {
            Box(
                Modifier.Companion
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            Column(
                modifier = Modifier.Companion
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = b.tagName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)
                Text(
                    text = "${formatRange(b.startMs, b.endMs)} • ${mins}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }
        }
    }
}