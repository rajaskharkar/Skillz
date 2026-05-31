@file:OptIn(ExperimentalFoundationApi::class)

package com.kingkharnivore.skillz.ui.screen.shell.rooms.voyage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
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
import com.kingkharnivore.skillz.domain.voyage.MultiplierRecord
import com.kingkharnivore.skillz.domain.voyage.PeriodCountRecord
import com.kingkharnivore.skillz.domain.voyage.PeriodDurationRecord
import com.kingkharnivore.skillz.domain.voyage.PeriodPointsRecord
import com.kingkharnivore.skillz.domain.voyage.StreakRecord
import com.kingkharnivore.skillz.domain.voyage.VoyageArcSummary
import com.kingkharnivore.skillz.domain.voyage.VoyageFlowSummary
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
    var selectedRecordDetail by remember { mutableStateOf<VoyageRecordDetail?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        VoyageHallContent(
            uiState = uiState,
            onRecordClick = { selectedRecordDetail = it },
            modifier = Modifier.fillMaxSize()
        )
        selectedRecordDetail?.let { detail ->
            VoyageRecordPopup(
                detail = detail,
                onDismiss = { selectedRecordDetail = null }
            )
        }
    }
}

@Composable
private fun VoyageHallContent(
    uiState: VoyageHallUiState,
    onRecordClick: (VoyageRecordDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(shellChamberBrush())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        VoyageHallHeader()
        Spacer(Modifier.height(14.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = scheme.primary)
            }

            uiState.errorMessage != null -> VoyageEmptyState(
                title = stringResource(R.string.voyage_hall_error_title),
                body = stringResource(R.string.voyage_hall_error_body),
                modifier = Modifier.fillMaxSize()
            )

            uiState.stats != null -> VoyageStatsPager(
                stats = uiState.stats,
                onRecordClick = onRecordClick,
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
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.surface.copy(alpha = 0.88f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.radialGradient(listOf(scheme.primary.copy(alpha = 0.18f), Color.Transparent)))
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
                        Icon(Icons.Outlined.Route, contentDescription = null, tint = scheme.primary)
                    }
                }
                Text(stringResource(R.string.voyage_hall_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.voyage_hall_subtitle), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface.copy(alpha = 0.78f))
                Text(stringResource(R.string.voyage_hall_shell_subtitle), style = MaterialTheme.typography.bodyMedium, color = scheme.primary.copy(alpha = 0.92f))
            }
        }
    }
}

@Composable
private fun VoyageStatsPager(
    stats: VoyageHallStats,
    onRecordClick: (VoyageRecordDetail) -> Unit,
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
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) CoreStatsPage(stats, onRecordClick) else BonusStatsPage(stats, onRecordClick)
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
        VoyageTab(stringResource(R.string.voyage_hall_tab_core_stats), selectedPage == 0, { onSelect(0) }, Modifier.weight(1f))
        VoyageTab(stringResource(R.string.voyage_hall_tab_bonus_stats), selectedPage == 1, { onSelect(1) }, Modifier.weight(1f))
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
            .semantics { contentDescription = a11y; role = Role.Tab }
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 11.dp))
    }
}

@Composable
private fun CoreStatsPage(stats: VoyageHallStats, onRecordClick: (VoyageRecordDetail) -> Unit) {
    VoyageStatsColumn {
        item { StreakHeroCard(stats.currentDailyStreak, stats.longestDailyStreak, stats.hasEligibleFlows) }
        VoyageSection(R.string.voyage_hall_section_arc_records) {
            ArcDurationCard(R.string.voyage_hall_longest_arc_by_time, stats.longestArcByTime, onRecordClick)
            MultiplierCard(stats.highestArcMultiplier, onRecordClick)
            ArcCountCard(stats.mostChainedFlowsInArc, onRecordClick)
        }
        VoyageSection(R.string.voyage_hall_section_point_records) {
            PointsPeriodCard(R.string.voyage_hall_best_day_points, stats.bestDayByPoints, false, onRecordClick)
            PointsPeriodCard(R.string.voyage_hall_best_week_points, stats.bestWeekByPoints, true, onRecordClick)
            PointsPeriodCard(R.string.voyage_hall_best_month_points, stats.bestMonthByPoints, true, onRecordClick)
        }
    }
}

@Composable
private fun BonusStatsPage(stats: VoyageHallStats, onRecordClick: (VoyageRecordDetail) -> Unit) {
    VoyageStatsColumn {
        VoyageSection(R.string.voyage_hall_section_flow_records) {
            FlowPointsCard(stats.bestFlowByPoints, onRecordClick)
            FlowDurationCard(stats.longestFlow, onRecordClick)
            CountPeriodCard(R.string.voyage_hall_most_flows_day, stats.mostFlowsInDay, CountKind.Flows, false, onRecordClick)
        }
        VoyageSection(R.string.voyage_hall_section_time_records) {
            DurationPeriodCard(R.string.voyage_hall_most_time_day, stats.mostTimeInDay, false, onRecordClick)
            DurationPeriodCard(R.string.voyage_hall_most_time_week, stats.mostTimeInWeek, true, onRecordClick)
            DurationPeriodCard(R.string.voyage_hall_most_time_month, stats.mostTimeInMonth, true, onRecordClick)
        }
        VoyageSection(R.string.voyage_hall_section_arc_volume_records) {
            CountPeriodCard(R.string.voyage_hall_most_arcs_day, stats.mostArcsInDay, CountKind.Arcs, false, onRecordClick)
            CountPeriodCard(R.string.voyage_hall_most_arcs_week, stats.mostArcsInWeek, CountKind.Arcs, true, onRecordClick)
        }
    }
}

@Composable
private fun VoyageStatsColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

private fun LazyListScope.VoyageSection(titleRes: Int, content: @Composable () -> Unit) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f), modifier = Modifier.padding(start = 4.dp))
            content()
        }
    }
}

@Composable
private fun StreakHeroCard(current: StreakRecord?, longest: StreakRecord?, hasEligibleFlows: Boolean) {
    val title = stringResource(R.string.voyage_hall_current_daily_streak)
    val value = when {
        current != null -> daysText(current.days)
        hasEligibleFlows -> stringResource(R.string.voyage_hall_not_active)
        else -> stringResource(R.string.voyage_hall_not_yet)
    }
    val subtitle = when {
        current != null -> stringResource(R.string.voyage_hall_current_daily_streak_subtitle)
        hasEligibleFlows -> stringResource(R.string.voyage_hall_return_today)
        else -> stringResource(R.string.voyage_hall_no_records_body)
    }
    val best = longest?.let { stringResource(R.string.voyage_hall_best_value, daysText(it.days)) } ?: stringResource(R.string.voyage_hall_best_empty)
    StatCard(title, value, subtitle, best, unavailable = !hasEligibleFlows, prominent = true)
}

@Composable
private fun ArcDurationCard(titleRes: Int, record: ArcRecord?, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(titleRes)
    val value = record?.let { formatDuration(it.totalDurationMs) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(
        title = title,
        value = value,
        subtitle = record?.let { chainedFlowsText(it.flowCount) } ?: stringResource(R.string.voyage_hall_no_arc_record_body),
        detail = record?.let { formatMillisDate(it.latestFlowEndMillis) },
        unavailable = record == null,
        onClick = record?.let { { onRecordClick(ArcRecordDetail(title, value, it)) } }
    )
}

@Composable
private fun MultiplierCard(record: MultiplierRecord?, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(R.string.voyage_hall_highest_arc_multiplier)
    val value = record?.let { multiplierText(it.multiplier) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(
        title = title,
        value = value,
        subtitle = if (record == null) stringResource(R.string.voyage_hall_no_arc_record_body) else stringResource(R.string.voyage_hall_highest_arc_multiplier_subtitle),
        detail = record?.let { formatMillisDate(it.reachedAtMillis) },
        unavailable = record == null,
        onClick = record?.let { { onRecordClick(MultiplierRecordDetail(title, value, it)) } }
    )
}

@Composable
private fun ArcCountCard(record: ArcRecord?, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(R.string.voyage_hall_most_chained_flows)
    val value = record?.let { flowsText(it.flowCount) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(
        title = title,
        value = value,
        subtitle = if (record == null) stringResource(R.string.voyage_hall_no_arc_record_body) else stringResource(R.string.voyage_hall_most_chained_flows_subtitle),
        detail = record?.let { formatMillisDate(it.latestFlowEndMillis) },
        unavailable = record == null,
        onClick = record?.let { { onRecordClick(ArcRecordDetail(title, value, it)) } }
    )
}

@Composable
private fun PointsPeriodCard(titleRes: Int, record: PeriodPointsRecord?, groupByDate: Boolean, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(titleRes)
    val value = record?.let { formatNumber(it.points) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(
        title = title,
        value = value,
        subtitle = record?.let { formatPeriod(it.startDate, it.endDate) } ?: stringResource(R.string.voyage_hall_not_yet),
        detail = record?.let { flowsText(it.flowCount) },
        unavailable = record == null,
        onClick = record?.let { { onRecordClick(PeriodFlowRecordDetail(title, value, it.startDate, it.endDate, it.totalDurationMs, it.points, it.flows, groupByDate)) } }
    )
}

@Composable
private fun FlowPointsCard(record: VoyageFlowSummary?, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(R.string.voyage_hall_best_flow_points)
    val value = record?.let { formatNumber(it.points) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(title, value, record?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.voyage_hall_best_flow_points_subtitle), record?.let { formatMillisDate(it.completedAtMillis) }, record == null, record?.let { { onRecordClick(SingleFlowRecordDetail(title, value, it)) } })
}

@Composable
private fun FlowDurationCard(record: VoyageFlowSummary?, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(R.string.voyage_hall_longest_flow)
    val value = record?.let { formatDuration(it.durationMs) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(title, value, record?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.voyage_hall_longest_flow_subtitle), record?.let { formatMillisDate(it.completedAtMillis) }, record == null, record?.let { { onRecordClick(SingleFlowRecordDetail(title, value, it)) } })
}

@Composable
private fun CountPeriodCard(titleRes: Int, record: PeriodCountRecord?, kind: CountKind, groupByDate: Boolean, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(titleRes)
    val value = record?.let { if (kind == CountKind.Arcs) arcsText(it.count) else flowsText(it.count) } ?: stringResource(R.string.voyage_hall_not_yet)
    val click = record?.let {
        if (kind == CountKind.Arcs) {
            { onRecordClick(PeriodArcRecordDetail(title, value, it.startDate, it.endDate, it.arcs)) }
        } else {
            { onRecordClick(PeriodFlowRecordDetail(title, value, it.startDate, it.endDate, it.totalDurationMs, it.points, it.flows, groupByDate)) }
        }
    }
    StatCard(title, value, record?.let { formatPeriod(it.startDate, it.endDate) } ?: stringResource(R.string.voyage_hall_not_yet), null, record == null, click)
}

@Composable
private fun DurationPeriodCard(titleRes: Int, record: PeriodDurationRecord?, groupByDate: Boolean, onRecordClick: (VoyageRecordDetail) -> Unit) {
    val title = stringResource(titleRes)
    val value = record?.let { formatDuration(it.durationMs) } ?: stringResource(R.string.voyage_hall_not_yet)
    StatCard(
        title = title,
        value = value,
        subtitle = record?.let { formatPeriod(it.startDate, it.endDate) } ?: stringResource(R.string.voyage_hall_not_yet),
        detail = record?.let { stringResource(R.string.voyage_hall_points_and_flows_detail, formatNumber(it.points), flowsText(it.flowCount)) },
        unavailable = record == null,
        onClick = record?.let { { onRecordClick(PeriodFlowRecordDetail(title, value, it.startDate, it.endDate, it.durationMs, it.points, it.flows, groupByDate)) } }
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    detail: String?,
    unavailable: Boolean,
    onClick: (() -> Unit)? = null,
    prominent: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val clickable = onClick != null && !unavailable
    val tapText = stringResource(R.string.voyage_hall_tap_for_details)
    val contentDesc = listOfNotNull(title, value, subtitle, detail, if (clickable) tapText else null).joinToString(". ")
    ElevatedCard(
        shape = RoundedCornerShape(if (prominent) 34.dp else 28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = scheme.surface.copy(alpha = if (unavailable) 0.66f else 0.92f)),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = if (prominent) 0.32f else if (unavailable) 0.10f else 0.18f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (prominent) 6.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (prominent) 34.dp else 28.dp))
            .then(if (clickable) Modifier.clickable(onClick = onClick!!) else Modifier)
            .semantics { contentDescription = contentDesc }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.radialGradient(listOf(scheme.primary.copy(alpha = if (prominent) 0.24f else 0.11f), Color.Transparent)))
                .padding(if (prominent) 24.dp else 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (prominent) 12.dp else 7.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface.copy(alpha = 0.72f))
                Text(value, style = if (prominent) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (unavailable) scheme.onSurface.copy(alpha = 0.62f) else scheme.primary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface.copy(alpha = 0.76f))
                detail?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = scheme.onSurface.copy(alpha = 0.60f)) }
                if (clickable) Text(tapText, style = MaterialTheme.typography.labelSmall, color = scheme.primary.copy(alpha = 0.80f))
            }
        }
    }
}

@Composable
private fun VoyageRecordPopup(detail: VoyageRecordDetail, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val scheme = MaterialTheme.colorScheme
    val recordDetailsDescription = stringResource(R.string.voyage_hall_record_details)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(onClick = onDismiss)
        )
        ElevatedCard(
            shape = RoundedCornerShape(34.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = scheme.surface.copy(alpha = 0.97f)),
            border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.28f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.84f)
                .semantics { contentDescription = recordDetailsDescription }
        ) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { PopupHeader(detail.title, detail.value) }
                    when (detail) {
                        is ArcRecordDetail -> arcDetailItems(detail.arc, detail.highlightSessionId)
                        is MultiplierRecordDetail -> multiplierDetailItems(detail.record)
                        is SingleFlowRecordDetail -> singleFlowItems(detail.flow)
                        is PeriodFlowRecordDetail -> periodFlowItems(detail)
                        is PeriodArcRecordDetail -> periodArcItems(detail)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.voyage_hall_close_record_details))
                }
            }
        }
    }
}

@Composable
private fun PopupHeader(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(end = 42.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

private fun LazyListScope.arcDetailItems(arc: ArcRecord, highlightSessionId: Long?) {
    item { ArcSummaryText(arc.flowCount, arc.totalDurationMs, arc.totalPoints, arc.peakMultiplier) }
    item { DetailSectionTitle(R.string.voyage_hall_flows_in_arc) }
    arc.flows.forEachIndexed { index, flow -> item { VoyageFlowRow(index + 1, flow, highlightSessionId == flow.sessionId) } }
}

private fun LazyListScope.multiplierDetailItems(record: MultiplierRecord) {
    item { Text(stringResource(R.string.voyage_hall_reached_during, record.flows.firstOrNull { it.sessionId == record.reachedInSessionId }?.title.orEmpty()), style = MaterialTheme.typography.bodyMedium) }
    item { DetailSectionTitle(R.string.voyage_hall_arc_total) }
    item { ArcSummaryText(record.flowCount, record.totalDurationMs, record.totalPoints, record.multiplier) }
    item { DetailSectionTitle(R.string.voyage_hall_flows_in_arc) }
    record.flows.forEachIndexed { index, flow -> item { VoyageFlowRow(index + 1, flow, record.reachedInSessionId == flow.sessionId) } }
}

private fun LazyListScope.singleFlowItems(flow: VoyageFlowSummary) {
    item { SingleFlowBlock(flow) }
}

private fun LazyListScope.periodFlowItems(detail: PeriodFlowRecordDetail) {
    item { Text(formatPeriod(detail.startDate, detail.endDate).orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
    item { Text(stringResource(R.string.voyage_hall_period_flow_summary, flowsText(detail.flows.size), formatDuration(detail.totalDurationMs), formatNumber(detail.totalPoints)), style = MaterialTheme.typography.bodyMedium) }
    item { DetailSectionTitle(R.string.voyage_hall_flows_included) }
    if (detail.groupByDate) {
        detail.flows.groupBy { localDate(it.completedAtMillis) }.toSortedMap().forEach { (date, flows) ->
            item { Text(formatMonthDay(date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            flows.forEachIndexed { index, flow -> item { VoyageFlowRow(index + 1, flow, false, compactDate = true) } }
        }
    } else {
        detail.flows.forEachIndexed { index, flow -> item { VoyageFlowRow(index + 1, flow, false, compactDate = true) } }
    }
}

private fun LazyListScope.periodArcItems(detail: PeriodArcRecordDetail) {
    item { Text(formatPeriod(detail.startDate, detail.endDate).orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
    item { Text(arcsText(detail.arcs.size), style = MaterialTheme.typography.bodyMedium) }
    item { DetailSectionTitle(R.string.voyage_hall_arcs_included) }
    detail.arcs.forEachIndexed { index, arc ->
        item { ArcBlock(index + 1, arc) }
    }
}

@Composable
private fun DetailSectionTitle(titleRes: Int) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ArcSummaryText(flowCount: Int, durationMs: Long, points: Int, peak: Double?) {
    val text = if (peak != null) {
        stringResource(R.string.voyage_hall_arc_summary_with_peak, flowsText(flowCount), formatDuration(durationMs), formatNumber(points), multiplierText(peak))
    } else {
        stringResource(R.string.voyage_hall_arc_summary_without_peak, flowsText(flowCount), formatDuration(durationMs), formatNumber(points))
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
}

@Composable
private fun SingleFlowBlock(flow: VoyageFlowSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(flow.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(flow.tagName.orUntagged(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        Text(stringResource(R.string.voyage_hall_points_value, formatNumber(flow.points)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(formatDuration(flow.durationMs), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.voyage_hall_completed_on, formatMillisDateTime(flow.completedAtMillis)), style = MaterialTheme.typography.bodyMedium)
        flow.arcId?.let { Text(stringResource(R.string.voyage_hall_part_of_arc), style = MaterialTheme.typography.bodyMedium) }
        flow.arcMultiplierUsed?.let { Text(multiplierText(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ArcBlock(index: Int, arc: VoyageArcSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.voyage_hall_arc_number, index), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ArcSummaryText(arc.flowCount, arc.totalDurationMs, arc.totalPoints, arc.peakMultiplier)
        arc.flows.forEachIndexed { flowIndex, flow -> VoyageFlowRow(flowIndex + 1, flow, false, compactDate = true) }
    }
}

@Composable
private fun VoyageFlowRow(index: Int, flow: VoyageFlowSummary, highlighted: Boolean, compactDate: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val details = listOfNotNull(formatDuration(flow.durationMs), stringResource(R.string.voyage_hall_points_abbrev, formatNumber(flow.points)), flow.arcMultiplierUsed?.let { multiplierText(it) }).joinToString(" · ")
    val meta = stringResource(R.string.voyage_hall_flow_meta, flow.tagName.orUntagged(), if (compactDate) formatTime(flow.completedAtMillis) else formatMillisDateTime(flow.completedAtMillis))
    val desc = listOf(flow.title, meta, details, if (highlighted) stringResource(R.string.voyage_hall_peak_reached_here) else null).filterNotNull().joinToString(". ")
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) scheme.primary.copy(alpha = 0.12f) else scheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = if (highlighted) 0.28f else 0.08f)),
        modifier = Modifier.semantics { contentDescription = desc }
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.voyage_hall_numbered_title, index, flow.title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = 0.62f))
            Text(details, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface.copy(alpha = 0.74f))
            if (highlighted) Text(stringResource(R.string.voyage_hall_peak_reached_here), style = MaterialTheme.typography.labelMedium, color = scheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VoyageEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(18.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(shape = RoundedCornerShape(32.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(24.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), textAlign = TextAlign.Center)
            }
        }
    }
}

private sealed interface VoyageRecordDetail { val title: String; val value: String }
private data class ArcRecordDetail(override val title: String, override val value: String, val arc: ArcRecord, val highlightSessionId: Long? = null) : VoyageRecordDetail
private data class MultiplierRecordDetail(override val title: String, override val value: String, val record: MultiplierRecord) : VoyageRecordDetail
private data class SingleFlowRecordDetail(override val title: String, override val value: String, val flow: VoyageFlowSummary) : VoyageRecordDetail
private data class PeriodFlowRecordDetail(override val title: String, override val value: String, val startDate: LocalDate, val endDate: LocalDate, val totalDurationMs: Long, val totalPoints: Int, val flows: List<VoyageFlowSummary>, val groupByDate: Boolean) : VoyageRecordDetail
private data class PeriodArcRecordDetail(override val title: String, override val value: String, val startDate: LocalDate, val endDate: LocalDate, val arcs: List<VoyageArcSummary>) : VoyageRecordDetail
private enum class CountKind { Flows, Arcs }

@Composable private fun daysText(count: Int) = pluralStringResource(R.plurals.voyage_hall_days_count, count, count)
@Composable private fun flowsText(count: Int) = pluralStringResource(R.plurals.voyage_hall_flows_count, count, count)
@Composable private fun chainedFlowsText(count: Int) = pluralStringResource(R.plurals.voyage_hall_chained_flows_count, count, count)
@Composable private fun arcsText(count: Int) = pluralStringResource(R.plurals.voyage_hall_arcs_count, count, count)
@Composable private fun multiplierText(value: Double) = stringResource(R.string.voyage_hall_multiplier_value, value)
@Composable private fun String?.orUntagged(): String = this?.takeIf { it.isNotBlank() } ?: stringResource(R.string.voyage_hall_untagged)

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = (durationMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours <= 0L) "${minutes}m" else "${hours}h ${minutes.toString().padStart(2, '0')}m"
}

private fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance().format(value)
private fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun formatPeriod(startDate: LocalDate?, endDate: LocalDate?): String? {
    if (startDate == null || endDate == null) return null
    val locale = Locale.getDefault()
    return when {
        startDate == endDate -> startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        startDate.dayOfMonth == 1 && endDate == startDate.withDayOfMonth(startDate.lengthOfMonth()) -> startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        startDate.year == endDate.year && startDate.month == endDate.month -> "${startDate.format(DateTimeFormatter.ofPattern("MMM d", locale))}–${endDate.format(DateTimeFormatter.ofPattern("d, yyyy", locale))}"
        else -> "${startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))}–${endDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))}"
    }
}

private fun formatMillisDate(millis: Long): String = localDate(millis).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
private fun formatMillisDateTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault()))
private fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
private fun formatMonthDay(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
private fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
