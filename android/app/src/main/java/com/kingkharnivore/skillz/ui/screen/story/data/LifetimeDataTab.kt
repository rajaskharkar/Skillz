package com.kingkharnivore.skillz.ui.screen.story.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.analytics.LifetimeStats
import com.kingkharnivore.skillz.model.FlowMode
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Separate lazy items keep long histories and charts off the composition path until visible. */
fun LazyListScope.lifetimeDataItems(stats: LifetimeStats?, onJourney: (Long) -> Unit) {
    if (stats == null) {
        item("data-loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    } else if (stats.flowCount == 0) {
        item("data-empty") {
            DataCard(stringResource(R.string.lifetime_data_title)) {
                Text(stringResource(R.string.lifetime_empty))
            }
        }
    } else {
        item("data-overview") { LifetimeOverview(stats) }
        item("data-averages") { LifetimeAverages(stats) }
        item("data-modes") { LifetimeModes(stats) }
        item("data-journeys") { LifetimeJourneys(stats, onJourney) }
        item("data-length") { LifetimeLength(stats) }
        item("data-rhythm") { LifetimeRhythm(stats) }
    }
}

@Composable
private fun DataCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun number(value: Number, decimals: Int = 0): String {
    val locale = locale()
    return remember(value, decimals, locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = decimals }.format(value)
    }
}

@Composable
private fun hours(durationMs: Double): String =
    stringResource(R.string.lifetime_hours_value, number(durationMs / 3_600_000.0, 2))

@Composable
private fun StatLine(label: String, value: String) {
    // Two lines remain readable at large font scales and for translated labels.
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LifetimeOverview(stats: LifetimeStats) {
    DataCard(stringResource(R.string.lifetime_data_title)) {
        Text(stringResource(R.string.lifetime_scope), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        stats.firstFlowDate?.let {
            Text(stringResource(R.string.lifetime_since,
                it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()))),
                style = MaterialTheme.typography.bodyMedium)
        }
        StatLine(stringResource(R.string.lifetime_total_hours), hours(stats.totalDurationMs.toDouble()))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) { StatLine(stringResource(R.string.lifetime_total_flows), number(stats.flowCount)) }
            Column(Modifier.weight(1f)) { StatLine(stringResource(R.string.lifetime_active_days), number(stats.activeDays)) }
        }
    }
}

@Composable
fun LifetimeAverages(stats: LifetimeStats) {
    DataCard(stringResource(R.string.lifetime_averages)) {
        StatLine(stringResource(R.string.lifetime_daily), hours(stats.averageDailyMs))
        StatLine(stringResource(R.string.lifetime_weekly), hours(stats.averageWeeklyMs))
        StatLine(stringResource(R.string.lifetime_monthly), hours(stats.averageMonthlyMs))
        Text(stringResource(R.string.lifetime_average_definition), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.lifetime_period_counts, number(stats.calendarDays),
            number(stats.calendarWeeks), number(stats.calendarMonths)), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun modeLabel(mode: FlowMode): String = stringResource(when (mode) {
    FlowMode.FLOW -> R.string.flow_card_type_flow
    FlowMode.SOFT -> R.string.flow_card_type_soft
    FlowMode.POWER -> R.string.power_title
})

@Composable
fun LifetimeModes(stats: LifetimeStats) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    DataCard(stringResource(R.string.lifetime_modes)) {
        val total = hours(stats.totalDurationMs.toDouble())
        Box(Modifier.align(Alignment.CenterHorizontally).size(180.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(12.dp).clearAndSetSemantics {}) {
                val stroke = Stroke(24.dp.toPx())
                drawArc(track, 0f, 360f, false, style = stroke)
                var start = -90f
                if (stats.totalDurationMs > 0) stats.modes.forEachIndexed { index, mode ->
                    val sweep = (mode.durationMs.toDouble() / stats.totalDurationMs * 360).toFloat()
                    if (sweep > 0f) drawArc(colors[index], start, sweep, false, style = stroke)
                    start += sweep
                }
            }
            Text(total, modifier = Modifier.widthIn(max = 125.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        stats.modes.forEachIndexed { index, mode ->
            val label = modeLabel(mode.mode)
            val share = if (stats.totalDurationMs > 0) mode.durationMs.toDouble() / stats.totalDurationMs else 0.0
            val percent = NumberFormat.getPercentInstance(locale()).apply { maximumFractionDigits = 1 }.format(share)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(colors[index]))
                Column {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.lifetime_mode_summary, number(mode.count), hours(mode.durationMs.toDouble()), percent),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun LifetimeJourneys(stats: LifetimeStats, onJourney: (Long) -> Unit) {
    val barWidth = (96 * LocalConfiguration.current.fontScale.coerceAtLeast(1f)).dp
    val max = (stats.journeys.maxOfOrNull { it.durationMs } ?: 0).coerceAtLeast(1)
    DataCard(stringResource(R.string.lifetime_journeys)) {
        Text(stringResource(R.string.lifetime_journeys_hint), style = MaterialTheme.typography.bodySmall)
        LazyRow(Modifier.fillMaxWidth().testTag("lifetime-journey-chart"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stats.journeys, key = { it.tagId }) { journey ->
                val time = hours(journey.durationMs.toDouble())
                val description = stringResource(R.string.lifetime_journey_bar, journey.name, time)
                Column(Modifier.width(barWidth).clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.lifetime_open_journey)) { onJourney(journey.tagId) }
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .testTag("lifetime-journey-${journey.tagId}").padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(time, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Box(Modifier.height(160.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.width(44.dp).height((160 * journey.durationMs.toDouble() / max).toFloat().dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(MaterialTheme.colorScheme.primary))
                    }
                    HorizontalDivider()
                    Text(journey.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun LifetimeLength(stats: LifetimeStats) {
    DataCard(stringResource(R.string.lifetime_length)) {
        StatLine(stringResource(R.string.lifetime_average_length),
            stringResource(R.string.lifetime_minutes_value, number(stats.averageFlowMs / 60_000, 1)))
        StatLine(stringResource(R.string.lifetime_typical_length),
            stringResource(R.string.lifetime_minutes_value, number(stats.medianFlowMs / 60_000, 1)))
        Text(stringResource(R.string.lifetime_length_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LifetimeRhythm(stats: LifetimeStats) {
    var showStarts by rememberSaveable { mutableStateOf(true) }
    val locale = locale()
    val values = remember(stats.rhythm, showStarts) {
        List(42) { index ->
            stats.rhythm.filter { it.weekday == index / 6 && it.hour / 4 == index % 6 }
                .sumOf { if (showStarts) it.starts.toLong() else it.durationMs }
        }
    }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    DataCard(stringResource(R.string.lifetime_rhythm)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(showStarts, { showStarts = true }, label = { Text(stringResource(R.string.lifetime_starts)) })
            FilterChip(!showStarts, { showStarts = false }, label = { Text(stringResource(R.string.lifetime_active_time)) })
        }
        Text(stringResource(R.string.lifetime_rhythm_hint, stats.zoneId), style = MaterialTheme.typography.bodySmall)
        Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.width(48.dp))
                repeat(6) { band ->
                    Text("${number(band * 4)}–${number((band + 1) * 4)}", modifier = Modifier.width(56.dp),
                        style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
            repeat(7) { day ->
                val dayName = DayOfWeek.of(day + 1).getDisplayName(TextStyle.FULL, locale)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(DayOfWeek.of(day + 1).getDisplayName(TextStyle.SHORT, locale), Modifier.width(48.dp),
                        style = MaterialTheme.typography.labelSmall)
                    repeat(6) { band ->
                        val value = values[day * 6 + band]
                        val valueLabel = if (showStarts) number(value) else hours(value.toDouble())
                        val description = stringResource(R.string.lifetime_rhythm_cell, dayName, band * 4, (band + 1) * 4,
                            if (showStarts) stringResource(R.string.lifetime_starts_value, valueLabel) else valueLabel)
                        val background = lerp(MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.primaryContainer, value.toFloat() / max)
                        Box(Modifier.width(56.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(6.dp)).background(background)
                            .clearAndSetSemantics { contentDescription = description }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(valueLabel, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        if (stats.estimatedTimingCount > 0) {
            Text(stringResource(R.string.lifetime_estimated, number(stats.estimatedTimingCount)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
