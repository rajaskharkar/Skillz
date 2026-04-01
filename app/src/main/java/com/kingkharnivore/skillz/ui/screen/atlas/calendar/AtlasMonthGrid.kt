package com.kingkharnivore.skillz.ui.screen.atlas.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.model.AtlasMonthCellUi
import com.kingkharnivore.skillz.ui.model.AtlasMonthUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MONTH_DAY_FMT = DateTimeFormatter.ofPattern("d")
private val MONTH_A11Y_DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
fun AtlasMonthGrid(
    month: AtlasMonthUi,
    selectedDayStartMs: Long,
    nowMs: Long,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthDowLabels = remember {
        listOf(
            R.string.atlas_month_dow_s,
            R.string.atlas_month_dow_m,
            R.string.atlas_month_dow_t,
            R.string.atlas_month_dow_w,
            R.string.atlas_month_dow_t,
            R.string.atlas_month_dow_f,
            R.string.atlas_month_dow_s
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            monthDowLabels.forEach { labelRes ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                }
            }
        }

        month.cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { cell ->
                    AtlasMonthCell(
                        cell = cell,
                        selectedDayStartMs = selectedDayStartMs,
                        nowMs = nowMs,
                        onClick = { onDayClick(cell.dayStartMs) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AtlasMonthCell(
    cell: AtlasMonthCellUi,
    selectedDayStartMs: Long,
    nowMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val zone = remember { ZoneId.systemDefault() }
    val todayStart = remember(nowMs, zone) {
        Instant.ofEpochMilli(nowMs)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
    val isToday = cell.dayStartMs == todayStart
    val isSelected = cell.dayStartMs == selectedDayStartMs

    val date = remember(cell.dayStartMs, zone) {
        Instant.ofEpochMilli(cell.dayStartMs).atZone(zone)
    }
    val accent = cell.previewColors.firstOrNull()?.let(::Color) ?: cs.primary

    val baseColor = when {
        isSelected -> cs.primary.copy(alpha = 0.20f)
        isToday -> cs.secondaryContainer.copy(alpha = 0.62f)
        cell.beamsCount > 0 -> accent.copy(alpha = 0.12f + (cell.beamsCount.coerceAtMost(5) * 0.02f))
        cell.isInCurrentMonth -> cs.surfaceVariant.copy(alpha = 0.55f)
        else -> cs.surfaceContainerLowest.copy(alpha = 0.55f)
    }

    val borderColor = when {
        isSelected -> cs.primary
        isToday -> cs.secondary.copy(alpha = 0.70f)
        cell.beamsCount > 0 -> accent.copy(alpha = 0.24f)
        else -> Color.Transparent
    }

    val beamCountText = if (cell.beamsCount > 0) {
        pluralStringResource(
            R.plurals.atlas_month_beam_count,
            cell.beamsCount,
            cell.beamsCount
        )
    } else {
        ""
    }

    val a11yDate = remember(date) { date.format(MONTH_A11Y_DATE_FMT) }
    val contentDesc = if (cell.beamsCount > 0) {
        stringResource(R.string.atlas_month_cell_a11y_with_beams, a11yDate, beamCountText)
    } else {
        stringResource(R.string.atlas_month_cell_a11y_free, a11yDate)
    }

    val stateParts = buildList {
        if (isToday) add(stringResource(R.string.atlas_month_cell_state_today))
        if (isSelected) add(stringResource(R.string.atlas_month_cell_state_selected))
        if (!cell.isInCurrentMonth) add(stringResource(R.string.atlas_month_cell_state_outside_month))
    }.joinToString(", ")

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(if (cell.isInCurrentMonth) 1f else 0.50f)
            .background(baseColor, RoundedCornerShape(18.dp))
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(
                        width = 1.2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
                if (stateParts.isNotBlank()) {
                    stateDescription = stateParts
                }
            }
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date.format(MONTH_DAY_FMT),
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (cell.previewColors.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cell.previewColors.take(3).forEach { argb ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        Color(argb),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                if (cell.beamsCount > 0) {
                    Text(
                        text = beamCountText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                } else if (cell.isInCurrentMonth) {
                    Text(
                        text = stringResource(R.string.atlas_month_free),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurface.copy(alpha = 0.42f)
                    )
                }
            }
        }
    }
}