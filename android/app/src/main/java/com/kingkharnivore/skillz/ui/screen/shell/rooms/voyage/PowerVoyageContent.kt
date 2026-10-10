package com.kingkharnivore.skillz.ui.screen.shell.rooms.voyage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.utils.shell.voyage.*
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource

@Composable
fun PowerVoyageContent(stats: PowerVoyageStats) {
    val locale = LocalConfiguration.current.locales[0]
    var days by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf<Long?>(null) }
    val range = stats.windows[days] ?: PowerRangeStats()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text(stringResource(R.string.power_voyage_lifetime, stats.lifetime.durationMs / 3_600_000, stats.lifetime.durationMs / 60_000 % 60), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PowerVoyageCalculator.windows.forEach { window -> FilterChip(selected = days == window, onClick = { days = window }, label = { Text(if (window == 0) stringResource(R.string.power_all_time) else pluralStringResource(R.plurals.power_days, window, window)) }) }
        } }
        item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(pluralStringResource(R.plurals.power_voyage_count, range.sessions, range.sessions))
            Text(stringResource(R.string.power_voyage_time, range.durationMs / 3_600_000, range.durationMs / 60_000 % 60))
            Text(stringResource(R.string.power_voyage_share, range.share * 100))
            LinearProgressIndicator(progress = { range.share.toFloat() }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary)
        } } }
        item { Text(stringResource(R.string.power_by_journey), style = MaterialTheme.typography.titleMedium) }
        items(range.journeys, key = { it.journeyId }) { journey ->
            ElevatedCard(onClick = { expanded = if(expanded == journey.journeyId) null else journey.journeyId }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(journey.name ?: stringResource(R.string.power_unnamed_journey), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.power_journey_summary, journey.durationMs / 60_000, journey.share * 100))
                    if (expanded == journey.journeyId) journey.months.forEach { (month, share) ->
                        Text(stringResource(R.string.power_month_share, month.format(DateTimeFormatter.ofPattern("MMM yyyy", locale)), share * 100))
                    }
                }
            }
        }
        item { Text(stringResource(R.string.power_trend_description), style = MaterialTheme.typography.bodySmall) }
    }
}
