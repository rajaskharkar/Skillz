package com.kingkharnivore.skillz.ui.screen.atlas.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.model.AtlasWeekDayUi
import com.kingkharnivore.skillz.ui.model.AtlasWeekUi
import com.kingkharnivore.skillz.ui.model.BeamBlockUi
import com.kingkharnivore.skillz.utils.time.formatDuration
import com.kingkharnivore.skillz.utils.time.formatRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val WEEK_DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")

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
    val isDark = isSystemInDarkTheme()

    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(day.dayStartMs).atZone(zone)
    val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    val isToday = day.dayStartMs == todayStart
    val isSelected = day.dayStartMs == selectedDayStartMs
    val hasBeams = day.beamsCount > 0

    val containerColor = remember(isDark, isSelected, isToday, hasBeams, cs) {
        when {
            isSelected -> if (isDark) {
                cs.primary.copy(alpha = 0.20f).compositeOver(cs.surfaceContainerHigh)
            } else {
                cs.primary.copy(alpha = 0.10f).compositeOver(cs.surfaceContainerHigh)
            }

            isToday -> if (isDark) {
                cs.tertiary.copy(alpha = 0.16f).compositeOver(cs.surfaceContainer)
            } else {
                cs.tertiary.copy(alpha = 0.08f).compositeOver(cs.surfaceContainer)
            }

            hasBeams -> if (isDark) {
                cs.surfaceContainerHigh
            } else {
                cs.surfaceContainer
            }

            else -> if (isDark) {
                cs.surfaceContainerLow
            } else {
                cs.surfaceContainerLowest
            }
        }
    }

    val borderColor = when {
        isSelected -> cs.primary.copy(alpha = if (isDark) 0.85f else 0.55f)
        isToday -> cs.primary.copy(alpha = if (isDark) 0.50f else 0.32f)
        hasBeams -> cs.outline.copy(alpha = if (isDark) 0.34f else 0.18f)
        else -> cs.outline.copy(alpha = if (isDark) 0.24f else 0.12f)
    }

    val chipContainer = when {
        isSelected -> cs.primary.copy(alpha = if (isDark) 0.22f else 0.12f)
        isToday -> cs.primary.copy(alpha = if (isDark) 0.16f else 0.10f)
        else -> if (isDark) cs.surfaceContainerHighest else cs.surface
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

                    Text(
                        text = if (day.totalDurationMs > 0L) {
                            formatDuration(day.totalDurationMs)
                        } else {
                            "No beams planned"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant
                    )
                }

                Spacer(Modifier.weight(1f))

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            text = if (day.beamsCount == 1) "1 beam" else "${day.beamsCount} beams"
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = chipContainer,
                        disabledLabelColor = cs.onSurfaceVariant
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = false,
                        borderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
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
                        border = BorderStroke(1.dp, cs.primary.copy(alpha = if (isDark) 0.55f else 0.35f))
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
    val isDark = isSystemInDarkTheme()

    val bg = when {
        isSelected -> cs.primary.copy(alpha = if (isDark) 0.16f else 0.08f)
            .compositeOver(cs.surfaceContainer)

        isToday -> cs.tertiary.copy(alpha = if (isDark) 0.12f else 0.06f)
            .compositeOver(cs.surfaceContainerLow)

        else -> cs.surfaceContainerLow
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(
            1.dp,
            cs.outline.copy(alpha = if (isDark) 0.22f else 0.10f)
        )
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
                color = cs.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenDay,
                border = BorderStroke(1.dp, cs.primary.copy(alpha = if (isDark) 0.55f else 0.35f))
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
    val isDark = isSystemInDarkTheme()
    val accent = remember(beam.journeyColorArgb) { Color(beam.journeyColorArgb) }

    val rowBackground = if (isDark) {
        accent.copy(alpha = 0.22f).compositeOver(cs.surfaceContainerHigh)
    } else {
        accent.copy(alpha = 0.14f).compositeOver(cs.surface)
    }

    val strokeColor = if (isDark) {
        accent.copy(alpha = 0.40f)
    } else {
        accent.copy(alpha = 0.22f)
    }

    val contentColor = if (rowBackground.luminance() < 0.22f) {
        Color.White.copy(alpha = 0.96f)
    } else {
        cs.onSurface
    }

    val secondaryColor = if (rowBackground.luminance() < 0.22f) {
        Color.White.copy(alpha = 0.72f)
    } else {
        cs.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = rowBackground,
        border = BorderStroke(1.dp, strokeColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(40.dp)
                    .background(accent, RoundedCornerShape(999.dp))
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = beam.tagName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor
                )
                Text(
                    text = formatRange(beam.startMs, beam.endMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
            }
        }
    }
}

private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    val inverse = 1f - alpha
    return Color(
        red = (this.red * alpha) + (background.red * inverse),
        green = (this.green * alpha) + (background.green * inverse),
        blue = (this.blue * alpha) + (background.blue * inverse),
        alpha = 1f
    )
}