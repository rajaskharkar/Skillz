package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.ui.model.BeamStatus
import com.kingkharnivore.skillz.utils.time.formatRange

private enum class BeamVisualDensity { MICRO, COMPACT, FULL }

private fun densityFor(height: Dp): BeamVisualDensity = when {
    height < 22.dp -> BeamVisualDensity.MICRO
    height < 56.dp -> BeamVisualDensity.COMPACT
    else -> BeamVisualDensity.FULL
}

@Composable
fun BeamCard(
    b: BeamBlockUi,
    h: Dp,
    onBeamClick: (BeamBlockUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val journeyColor = Color(b.journeyColorArgb)
    val bg = when (b.status) {
        BeamStatus.ACTIVE -> journeyColor.copy(alpha = 0.22f)
        else -> journeyColor.copy(alpha = 0.16f)
    }
    val accent = journeyColor.copy(alpha = 0.92f)

    val visualHeight = h.coerceAtLeast(4.dp)
    val density = densityFor(visualHeight)

    val shape = RoundedCornerShape(
        topStart = if (b.clippedTop) 6.dp else 18.dp,
        topEnd = if (b.clippedTop) 6.dp else 18.dp,
        bottomStart = if (b.clippedBottom) 6.dp else 18.dp,
        bottomEnd = if (b.clippedBottom) 6.dp else 18.dp
    )

    val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)
    val title = buildString {
        if (b.clippedTop) append("↑ ")
        append(b.tagName)
        if (b.clippedBottom) append(" ↓")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(visualHeight),
        onClick = { onBeamClick(b) },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        when (density) {
            BeamVisualDensity.MICRO -> {
                Row(Modifier.fillMaxWidth().height(visualHeight)) {
                    Box(
                        Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(accent)
                    )
                }
            }

            BeamVisualDensity.COMPACT -> {
                Row(Modifier.fillMaxWidth().height(visualHeight)) {
                    Box(
                        Modifier
                            .width(7.dp)
                            .fillMaxHeight()
                            .background(accent)
                    )

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            BeamVisualDensity.FULL -> {
                Row(Modifier.fillMaxWidth().height(visualHeight)) {
                    Box(
                        Modifier
                            .width(7.dp)
                            .fillMaxHeight()
                            .background(accent)
                    )

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            lineHeight = 18.sp,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${formatRange(b.startMs, b.endMs)} • ${mins}m",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        when {
                            b.clippedTop && b.clippedBottom -> {
                                Text(
                                    text = "Continues across this day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                    maxLines = 1
                                )
                            }
                            b.clippedTop -> {
                                Text(
                                    text = "Continues from earlier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                    maxLines = 1
                                )
                            }
                            b.clippedBottom -> {
                                Text(
                                    text = "Continues later",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}