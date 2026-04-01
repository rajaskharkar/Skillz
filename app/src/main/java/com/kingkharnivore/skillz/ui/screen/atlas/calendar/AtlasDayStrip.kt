package com.kingkharnivore.skillz.ui.screen.atlas.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun AtlasDayStrip(
    selectedDayStartMs: Long,
    nowMs: Long,
    minSelectableDayStartMs: Long?,
    beamsByDayStartMs: Map<Long, Int>,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val locale = remember { Locale.getDefault() }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val pattern = stringResource(R.string.atlas_day_strip_day_format_pattern)
    val dayFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(
            pattern,
            locale
        )
    }

    val selectedDate = remember(selectedDayStartMs) {
        Instant.ofEpochMilli(selectedDayStartMs).atZone(zone).toLocalDate()
    }
    val startOfWeek = remember(selectedDate, firstDayOfWeek) {
        selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    }
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    val selectedText = stringResource(R.string.atlas_day_strip_selected)
    val todayText = stringResource(R.string.atlas_day_strip_today)
    val unavailableText = stringResource(R.string.atlas_day_strip_unavailable)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(7) { index ->
            val date = startOfWeek.plusDays(index.toLong())
            val dayStartMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val isSelected = dayStartMs == selectedDayStartMs
            val isToday = date == today
            val canSelect = minSelectableDayStartMs?.let { dayStartMs >= it } ?: true
            val count = beamsByDayStartMs[dayStartMs] ?: 0

            val shortDay = date.format(dayFormatter)
            val beamBadgeText = if (count == 1) {
                stringResource(R.string.atlas_day_strip_beam_count_one)
            } else {
                stringResource(R.string.atlas_day_strip_beam_count_other, count)
            }

            val dayBaseA11y = if (count > 0) {
                stringResource(
                    R.string.atlas_day_strip_day_a11y_with_beams,
                    shortDay,
                    date.dayOfMonth,
                    count
                )
            } else {
                stringResource(
                    R.string.atlas_day_strip_day_a11y,
                    shortDay,
                    date.dayOfMonth
                )
            }

            val stateText = buildList {
                if (isSelected) add(selectedText)
                if (isToday) add(todayText)
                if (!canSelect) add(unavailableText)
            }.joinToString(", ")

            val clickLabel = stringResource(
                R.string.atlas_day_strip_select_day_a11y,
                "$shortDay ${date.dayOfMonth}"
            )

            val container = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
            val content = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (canSelect) 1f else 0.45f)
                    .clip(MaterialTheme.shapes.large)
                    .background(container)
                    .then(
                        if (canSelect) Modifier.clickable { onSelectDay(dayStartMs) } else Modifier
                    )
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        selected = isSelected
                        contentDescription = dayBaseA11y
                        if (stateText.isNotBlank()) {
                            stateDescription = stateText
                        }
                        if (canSelect) {
                            onClick(label = clickLabel) {
                                onSelectDay(dayStartMs)
                                true
                            }
                        } else {
                            disabled()
                        }
                    }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = shortDay,
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = content
                )
                Spacer(Modifier.height(4.dp))
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) content.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = beamBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) content else MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}