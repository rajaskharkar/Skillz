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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
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

    val zone = remember { ZoneId.systemDefault() }
    val date = remember(day.dayStartMs, zone) {
        Instant.ofEpochMilli(day.dayStartMs).atZone(zone)
    }
    val todayStart = remember(nowMs, zone) {
        Instant.ofEpochMilli(nowMs)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

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

    val dateText = remember(date) { date.format(WEEK_DAY_FMT) }
    val durationText = if (day.totalDurationMs > 0L) {
        formatDuration(day.totalDurationMs)
    } else {
        stringResource(R.string.atlas_week_day_duration_none)
    }
    val beamCountText = stringResource(
        R.string.atlas_week_beam_count_one,
        day.beamsCount,
        day.beamsCount
    )
    val cardContentDescription = stringResource(
        R.string.atlas_week_day_card_a11y,
        dateText,
        durationText,
        beamCountText
    )
    val cardStateDescription = buildList {
        if (isToday) add(stringResource(R.string.atlas_week_day_card_state_today))
        if (isSelected) add(stringResource(R.string.atlas_week_day_card_state_selected))
    }.joinToString(", ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = cardContentDescription
                if (cardStateDescription.isNotBlank()) {
                    stateDescription = cardStateDescription
                }
            },
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
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface
                    )

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant
                    )
                }

                Spacer(Modifier.weight(1f))

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(text = beamCountText)
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
                        Text(stringResource(R.string.atlas_week_open_day))
                    }

                    if (day.beams.size > 4) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(
                                R.string.atlas_week_more_count,
                                day.beams.size - 4
                            ),
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

    val titleText = stringResource(R.string.atlas_week_no_beams_planned)
    val bodyText = stringResource(R.string.atlas_week_empty_day_body)
    val panelContentDescription = stringResource(
        R.string.atlas_week_empty_panel_a11y,
        titleText,
        bodyText
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = panelContentDescription
            },
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
                text = titleText,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface
            )
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onOpenDay,
                border = BorderStroke(1.dp, cs.primary.copy(alpha = if (isDark) 0.55f else 0.35f))
            ) {
                Text(stringResource(R.string.atlas_week_open_day))
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

    val rangeText = formatRange(beam.startMs, beam.endMs)
    val beamRowContentDescription = stringResource(
        R.string.atlas_week_beam_row_a11y,
        beam.tagName,
        rangeText
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = beamRowContentDescription
            },
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
                    text = rangeText,
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