package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.model.AtlasWeekDayUi
import com.kingkharnivore.skillz.ui.model.AtlasWeekUi
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.utils.time.formatDuration
import com.kingkharnivore.skillz.utils.time.formatRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val WEEK_DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")

/**
 * Explicit Atlas card palette.
 * Avoids Material gray / default Android card feel.
 */
private val AtlasCardBase = Color(0xFFF7F1E8)
private val AtlasCardWithBeams = Color(0xFFF3ECE3)
private val AtlasCardEmpty = Color(0xFFEEE6DB)
private val AtlasCardToday = Color(0xFFE4F1EE)
private val AtlasCardSelected = Color(0xFFD7EBE8)

private val AtlasPanelBase = Color(0xFFFFFAF4)
private val AtlasPanelToday = Color(0xFFF2FBF8)
private val AtlasPanelSelected = Color(0xFFEAF7F5)

@Composable
fun AtlasWeekBoard(
    week: AtlasWeekUi,
    selectedDayStartMs: Long,
    nowMs: Long,
    onOpenDay: (Long) -> Unit,
    onBeamClick: (BeamBlockUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        week.days.forEach { day ->
            AtlasWeekDayCard(
                day = day,
                selectedDayStartMs = selectedDayStartMs,
                nowMs = nowMs,
                onOpenDay = onOpenDay,
                onBeamClick = onBeamClick
            )
        }
    }
}

@Composable
private fun AtlasWeekDayCard(
    day: AtlasWeekDayUi,
    selectedDayStartMs: Long,
    nowMs: Long,
    onOpenDay: (Long) -> Unit,
    onBeamClick: (BeamBlockUi) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(day.dayStartMs).atZone(zone)
    val todayStart = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    val isToday = day.dayStartMs == todayStart
    val isSelected = day.dayStartMs == selectedDayStartMs

    val containerColor = when {
        isSelected -> AtlasCardSelected
        isToday -> AtlasCardToday
        day.beamsCount > 0 -> AtlasCardWithBeams
        else -> AtlasCardEmpty
    }

    val borderColor = when {
        isSelected -> cs.primary
        isToday -> cs.primary.copy(alpha = 0.55f)
        day.beamsCount > 0 -> cs.primary.copy(alpha = 0.18f)
        else -> cs.outline.copy(alpha = 0.14f)
    }

    val chipContainer = when {
        isSelected -> cs.primary.copy(alpha = 0.12f)
        isToday -> cs.primary.copy(alpha = 0.10f)
        else -> Color.White.copy(alpha = 0.82f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = date.format(WEEK_DAY_FMT),
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface
                    )

                    when {
                        day.totalDurationMs > 0L -> {
                            Text(
                                text = formatDuration(day.totalDurationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurface.copy(alpha = 0.74f)
                            )
                        }
                        else -> {
                            Text(
                                text = "No beams planned",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurface.copy(alpha = 0.64f)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            if (day.beamsCount == 1) "1 beam" else "${day.beamsCount} beams"
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = chipContainer,
                        disabledLabelColor = cs.onSurfaceVariant
                    )
                )
            }

            if (day.beams.isEmpty()) {
                EmptyDayPanel(
                    isToday = isToday,
                    isSelected = isSelected,
                    onOpenDay = { onOpenDay(day.dayStartMs) }
                )
            } else {
                day.beams.take(4).forEach { beam ->
                    WeekBeamRow(
                        beam = beam,
                        onClick = { onBeamClick(beam) }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onOpenDay(day.dayStartMs) },
                        border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f))
                    ) {
                        Text("Open Day")
                    }

                    if (day.beams.size > 4) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "+${day.beams.size - 4} more",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayPanel(
    isToday: Boolean,
    isSelected: Boolean,
    onOpenDay: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = when {
        isSelected -> AtlasPanelSelected
        isToday -> AtlasPanelToday
        else -> AtlasPanelBase
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No beams planned",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface
            )
            Text(
                text = "A clear day to rest, improvise, or chart something new.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurface.copy(alpha = 0.70f)
            )
            OutlinedButton(
                onClick = onOpenDay,
                border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f))
            ) {
                Text("Open Day")
            }
        }
    }
}

@Composable
private fun WeekBeamRow(
    beam: BeamBlockUi,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accent = Color(beam.journeyColorArgb)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .background(accent, RoundedCornerShape(999.dp))
                .padding(vertical = 14.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = beam.tagName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurface
            )
            Text(
                text = formatRange(beam.startMs, beam.endMs),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurface.copy(alpha = 0.70f)
            )
        }
    }
}