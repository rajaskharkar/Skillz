@file:OptIn(ExperimentalFoundationApi::class)

package com.kingkharnivore.skillz.ui.screen.shell.rooms.voyage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.voyage.ArcRecord
import com.kingkharnivore.skillz.domain.voyage.FlowRecord
import com.kingkharnivore.skillz.domain.voyage.MultiplierRecord
import com.kingkharnivore.skillz.domain.voyage.PeriodCountRecord
import com.kingkharnivore.skillz.domain.voyage.PeriodDurationRecord
import com.kingkharnivore.skillz.domain.voyage.PeriodPointsRecord
import com.kingkharnivore.skillz.domain.voyage.StreakRecord
import com.kingkharnivore.skillz.domain.voyage.VoyageHallStats
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.viewmodel.shell.VoyageHallUiState
import com.kingkharnivore.skillz.viewmodel.shell.VoyageHallViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun VoyageHallScreen(
    modifier: Modifier = Modifier,
    viewModel: VoyageHallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VoyageHallContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun VoyageHallContent(
    uiState: VoyageHallUiState,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(shellChamberBrush())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        VoyageHallHeader()
        Spacer(Modifier.height(14.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = scheme.primary)
            }

            uiState.stats?.hasEligibleFlows == false -> VoyageEmptyState(
                title = stringResource(R.string.voyage_hall_no_records_title),
                body = stringResource(R.string.voyage_hall_no_records_body),
                modifier = Modifier.fillMaxSize()
            )

            uiState.stats != null -> VoyageStatsPager(
                stats = uiState.stats,
                modifier = Modifier.fillMaxSize()
            )

            else -> VoyageEmptyState(
                title = stringResource(R.string.voyage_hall_no_records_title),
                body = stringResource(R.string.voyage_hall_no_records_body),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun VoyageHallHeader() {
    val scheme = MaterialTheme.colorScheme
    ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = scheme.surface.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.22f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Route,
                            contentDescription = null,
                            tint = scheme.primary
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.voyage_hall_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = stringResource(R.string.voyage_hall_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface.copy(alpha = 0.78f)
                )
                Text(
                    text = stringResource(R.string.voyage_hall_shell_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
private fun VoyageStatsPager(
    stats: VoyageHallStats,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Column(modifier = modifier) {
        VoyagePagerSelector(
            selectedPage = pagerState.currentPage,
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } }
        )
        Spacer(Modifier.height(12.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                CoreStatsPage(stats)
            } else {
                BonusStatsPage(stats)
            }
        }
    }
}

@Composable
private fun VoyagePagerSelector(selectedPage: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VoyageTab(
            text = stringResource(R.string.voyage_hall_tab_core_stats),
            selected = selectedPage == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        VoyageTab(
            text = stringResource(R.string.voyage_hall_tab_bonus_stats),
            selected = selectedPage == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VoyageTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val a11y = if (selected) stringResource(R.string.voyage_hall_tab_selected_a11y, text) else text
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) scheme.primary else Color.Transparent,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = a11y
                role = Role.Tab
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 11.dp)
        )
    }
}

@Composable
private fun CoreStatsPage(stats: VoyageHallStats) {
    VoyageStatsColumn {
        VoyageSection(R.string.voyage_hall_section_momentum) {
            StreakCard(R.string.voyage_hall_current_daily_streak, stats.currentDailyStreak, R.string.voyage_hall_current_daily_streak_subtitle)
            StreakCard(R.string.voyage_hall_longest_daily_streak, stats.longestDailyStreak, R.string.voyage_hall_longest_daily_streak_subtitle)
        }
        VoyageSection(R.string.voyage_hall_section_arc_records) {
            ArcDurationCard(R.string.voyage_hall_longest_arc_by_time, stats.longestArcByTime, R.string.voyage_hall_longest_arc_by_time_subtitle)
            MultiplierCard(stats.highestArcMultiplier)
            ArcCountCard(stats.mostChainedFlowsInArc)
        }
        VoyageSection(R.string.voyage_hall_section_point_records) {
            PointsPeriodCard(R.string.voyage_hall_best_day_points, stats.bestDayByPoints, R.string.voyage_hall_best_day_points_subtitle)
            PointsPeriodCard(R.string.voyage_hall_best_week_points, stats.bestWeekByPoints, R.string.voyage_hall_best_week_points_subtitle)
            PointsPeriodCard(R.string.voyage_hall_best_month_points, stats.bestMonthByPoints, R.string.voyage_hall_best_month_points_subtitle)
        }
    }
}

@Composable
private fun BonusStatsPage(stats: VoyageHallStats) {
    VoyageStatsColumn {
        VoyageSection(R.string.voyage_hall_section_flow_records) {
            FlowPointsCard(stats.bestFlowByPoints)
            FlowDurationCard(stats.longestFlow)
            CountPeriodCard(R.string.voyage_hall_most_flows_day, stats.mostFlowsInDay, R.string.voyage_hall_flows_suffix)
        }
        VoyageSection(R.string.voyage_hall_section_time_records) {
            DurationPeriodCard(R.string.voyage_hall_most_time_day, stats.mostTimeInDay)
            DurationPeriodCard(R.string.voyage_hall_most_time_week, stats.mostTimeInWeek)
            DurationPeriodCard(R.string.voyage_hall_most_time_month, stats.mostTimeInMonth)
        }
        VoyageSection(R.string.voyage_hall_section_arc_volume_records) {
            CountPeriodCard(R.string.voyage_hall_most_arcs_day, stats.mostArcsInDay, R.string.voyage_hall_arcs_suffix)
            CountPeriodCard(R.string.voyage_hall_most_arcs_week, stats.mostArcsInWeek, R.string.voyage_hall_arcs_suffix)
        }
    }
}

@Composable
private fun VoyageStatsColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

private fun LazyListScope.VoyageSection(titleRes: Int, content: @Composable () -> Unit) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                modifier = Modifier.padding(start = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun StreakCard(titleRes: Int, record: StreakRecord?, subtitleRes: Int) {
    val subtitle = stringResource(subtitleRes)
    StatCard(
        title = stringResource(titleRes),
        value = record?.let { stringResource(R.string.voyage_hall_days_value, it.days) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = subtitle,
        detail = record?.let { formatPeriod(it.startDate, it.endDate) },
        unavailable = record == null
    )
}

@Composable
private fun ArcDurationCard(titleRes: Int, record: ArcRecord?, subtitleRes: Int) {
    StatCard(
        title = stringResource(titleRes),
        value = record?.let { formatDuration(it.totalDurationMs) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = record?.let { stringResource(R.string.voyage_hall_chained_flows_detail, it.flowCount) }
            ?: stringResource(R.string.voyage_hall_no_arc_record_body),
        detail = record?.let { formatMillisDate(it.latestFlowEndMillis) },
        unavailable = record == null
    )
}

@Composable
private fun MultiplierCard(record: MultiplierRecord?) {
    StatCard(
        title = stringResource(R.string.voyage_hall_highest_arc_multiplier),
        value = record?.let { stringResource(R.string.voyage_hall_multiplier_value, it.multiplier) }
            ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = if (record == null) stringResource(R.string.voyage_hall_no_arc_record_body) else stringResource(R.string.voyage_hall_highest_arc_multiplier_subtitle),
        detail = record?.let { formatMillisDate(it.reachedAtMillis) },
        unavailable = record == null
    )
}

@Composable
private fun ArcCountCard(record: ArcRecord?) {
    StatCard(
        title = stringResource(R.string.voyage_hall_most_chained_flows),
        value = record?.let { stringResource(R.string.voyage_hall_flows_value, it.flowCount) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = if (record == null) stringResource(R.string.voyage_hall_no_arc_record_body) else stringResource(R.string.voyage_hall_most_chained_flows_subtitle),
        detail = record?.let { formatMillisDate(it.latestFlowEndMillis) },
        unavailable = record == null
    )
}

@Composable
private fun PointsPeriodCard(titleRes: Int, record: PeriodPointsRecord?, subtitleRes: Int) {
    StatCard(
        title = stringResource(titleRes),
        value = record?.let { formatNumber(it.points) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = stringResource(subtitleRes),
        detail = record?.let { formatPeriod(it.startDate, it.endDate) },
        unavailable = record == null
    )
}

@Composable
private fun FlowPointsCard(record: FlowRecord?) {
    StatCard(
        title = stringResource(R.string.voyage_hall_best_flow_points),
        value = record?.let { formatNumber(it.points) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = record?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.voyage_hall_best_flow_points_subtitle),
        detail = record?.let { formatMillisDate(it.completedAtMillis) },
        unavailable = record == null
    )
}

@Composable
private fun FlowDurationCard(record: FlowRecord?) {
    StatCard(
        title = stringResource(R.string.voyage_hall_longest_flow),
        value = record?.let { formatDuration(it.durationMs) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = record?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.voyage_hall_longest_flow_subtitle),
        detail = record?.let { formatMillisDate(it.completedAtMillis) },
        unavailable = record == null
    )
}

@Composable
private fun CountPeriodCard(titleRes: Int, record: PeriodCountRecord?, suffixRes: Int) {
    val suffix = stringResource(suffixRes)
    StatCard(
        title = stringResource(titleRes),
        value = record?.let { stringResource(R.string.voyage_hall_count_value, formatNumber(it.count), suffix) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = record?.let { formatPeriod(it.startDate, it.endDate) } ?: stringResource(R.string.voyage_hall_no_arc_record_body),
        detail = null,
        unavailable = record == null
    )
}

@Composable
private fun DurationPeriodCard(titleRes: Int, record: PeriodDurationRecord?) {
    StatCard(
        title = stringResource(titleRes),
        value = record?.let { formatDuration(it.durationMs) } ?: stringResource(R.string.voyage_hall_not_yet),
        subtitle = record?.let { formatPeriod(it.startDate, it.endDate) } ?: stringResource(R.string.voyage_hall_not_yet),
        detail = null,
        unavailable = record == null
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    detail: String?,
    unavailable: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val contentDesc = listOfNotNull(title, value, subtitle, detail).joinToString(". ")
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (unavailable) {
                scheme.surface.copy(alpha = 0.66f)
            } else {
                scheme.surface.copy(alpha = 0.92f)
            }
        ),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = if (unavailable) 0.10f else 0.18f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = if (unavailable) 0.04f else 0.11f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface.copy(alpha = 0.72f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (unavailable) scheme.onSurface.copy(alpha = 0.62f) else scheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface.copy(alpha = 0.76f)
                )
                detail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface.copy(alpha = 0.58f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoyageEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(18.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = (durationMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours <= 0L -> "${minutes}m"
        else -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }
}

private fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance().format(value)

private fun formatPeriod(startDate: LocalDate?, endDate: LocalDate?): String? {
    if (startDate == null || endDate == null) return null
    val locale = Locale.getDefault()
    return when {
        startDate == endDate -> startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        startDate.dayOfMonth == 1 && endDate == startDate.withDayOfMonth(startDate.lengthOfMonth()) ->
            startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        startDate.year == endDate.year && startDate.month == endDate.month ->
            "${startDate.format(DateTimeFormatter.ofPattern("MMM d", locale))}–${endDate.format(DateTimeFormatter.ofPattern("d, yyyy", locale))}"
        else ->
            "${startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))}–${endDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))}"
    }
}

private fun formatMillisDate(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
