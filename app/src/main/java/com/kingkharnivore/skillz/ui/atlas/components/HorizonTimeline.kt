package com.kingkharnivore.skillz.ui.atlas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.atlas.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun HorizonTimeline(
    horizon: HorizonState,
    ticks: List<HorizonTickUi>,
    blocks: List<BeamBlockUi>,
    height: Dp = 520.dp,
    canvasHeight: Dp = 1000.dp,
    onBlockClick: (BeamBlockUi) -> Unit = {}
) {
    val rangeMinutes = max(1, horizon.rangeMinutes)
    val railWidth = 64.dp

    // ✅ Inner scroll is a LazyColumn with a bounded height => no infinite constraint crash.
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                ticks.forEach { t ->
                    val topFrac = t.minuteFromStart.coerceIn(0, rangeMinutes) / rangeMinutes.toFloat()
                    val y = canvasHeight * topFrac

                    Row(
                        modifier = Modifier
                            .offset(y = y)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(railWidth)
                                .padding(end = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (t.isMajor && t.label.isNotBlank()) {
                                Text(
                                    text = t.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .alpha(if (t.isMajor) 0.16f else 0.08f)
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }

                // ── NOW line ───────────────────────────────────────────────────
                if (horizon.nowMs in horizon.startMs..horizon.endMs) {
                    val nowMin = ((horizon.nowMs - horizon.startMs) / 60_000L).toInt()
                    val nowFrac = nowMin.coerceIn(0, rangeMinutes) / rangeMinutes.toFloat()
                    val y = canvasHeight * nowFrac

                    Row(
                        modifier = Modifier
                            .offset(y = y)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(railWidth)) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .alpha(0.55f)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }

                blocks.forEach { b ->
                    val topFrac = b.startMin.coerceIn(0, rangeMinutes) / rangeMinutes.toFloat()
                    val endMin = max(b.endMin, b.startMin + 1)
                    val heightFrac = (endMin - b.startMin).coerceAtLeast(1) / rangeMinutes.toFloat()

                    val top = canvasHeight * topFrac
                    val h = canvasHeight * heightFrac

                    val journeyColor = Color(b.journeyColorArgb)
                    val bg = journeyColor.copy(alpha = 0.16f)          // subtle fill
                    val accent = journeyColor.copy(alpha = 0.90f)      // strong rail

                    val minHit = 48.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = railWidth)
                            .offset(y = top)
                            .height(maxOf(h, minHit)) // ✅ tap target
                            .clickable { onBlockClick(b) } // ✅ always clickable
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(h.coerceAtLeast(8.dp)), // keep visuals, avoid 0dp weirdness
                            colors = CardDefaults.cardColors(
                                containerColor = bg,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(accent)
                                )

                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        text = b.tagName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )

                                    val clipPrefix = if (b.clippedTop) "↥ " else ""
                                    val clipSuffix = if (b.clippedBottom) " ↧" else ""
                                    val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)

                                    Text(
                                        text = clipPrefix + "${formatRange(b.startMs, b.endMs)} • ${mins}m" + clipSuffix,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val HORIZON_TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a")

private fun formatTime(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    return Instant.ofEpochMilli(ms).atZone(zone).format(HORIZON_TIME_FMT)
}

fun formatRange(startMs: Long, endMs: Long): String {
    val zone = ZoneId.systemDefault()
    val s = formatTime(startMs, zone)
    val e = formatTime(endMs, zone)
    return "$s — $e"
}

private data class BeamPalette(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color
)

private fun formatBeamTimeRange(startMs: Long, endMs: Long): String {
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("h:mm a")

    val s = Instant.ofEpochMilli(startMs).atZone(zone).format(fmt)
    val e = Instant.ofEpochMilli(endMs).atZone(zone).format(fmt)

    val durMin = ((endMs - startMs).coerceAtLeast(0L)) / 60_000L
    val durLabel = when {
        durMin < 60 -> "${durMin}m"
        else -> {
            val h = durMin / 60
            val m = durMin % 60
            if (m == 0L) "${h}h" else "${h}h ${m}m"
        }
    }

    // Cleaner than an arrow; reads like a schedule
    return "$s – $e • $durLabel"
}

