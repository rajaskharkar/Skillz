@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.kingkharnivore.skillz.ui.screen.shell.rooms.ideagrove

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveFlowUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemType
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveItemUiModel
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveSort
import com.kingkharnivore.skillz.model.state.ideagrove.IdeaGroveUiState
import com.kingkharnivore.skillz.utils.time.formatIdeaGroveDuration
import com.kingkharnivore.skillz.utils.time.formatIdeaGroveDurationForSpeech
import com.kingkharnivore.skillz.viewmodel.IdeaGroveEvent
import com.kingkharnivore.skillz.viewmodel.IdeaGroveViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
fun IdeaGroveRoute(
    onNavigateToFlow: (pulseId: Long, title: String, journeyName: String?) -> Unit,
    onNavigateToCurrentFlow: () -> Unit,
    onSnackbar: suspend (String, String?) -> Unit,
    viewModel: IdeaGroveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is IdeaGroveEvent.NavigateToFlow -> onNavigateToFlow(event.pulseId, event.title, event.journeyName)
                IdeaGroveEvent.NavigateToCurrentFlow -> onNavigateToCurrentFlow()
                is IdeaGroveEvent.ShowSnackbar -> onSnackbar(
                    context.getString(event.messageRes),
                    event.actionLabelRes?.let(context::getString)
                )
            }
        }
    }

    IdeaGroveScreen(
        state = state,
        onPulseClicked = viewModel::onPulseClicked,
        onSortChanged = viewModel::onSortChanged,
        onFlowClicked = viewModel::onFlowClicked,
        onMarkAsInsightClicked = viewModel::onMarkAsInsightClicked,
        onMarkCompletedClicked = viewModel::onMarkCompletedClicked,
        onReviveClicked = viewModel::onReviveClicked
    )
}

@Composable
fun IdeaGroveScreen(
    state: IdeaGroveUiState,
    onPulseClicked: (Long) -> Unit,
    onSortChanged: (IdeaGroveSort) -> Unit,
    onFlowClicked: (Long) -> Unit,
    onMarkAsInsightClicked: (Long) -> Unit,
    onMarkCompletedClicked: (Long) -> Unit,
    onReviveClicked: (Long) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf(stringResource(R.string.idea_grove_tab_alive), stringResource(R.string.idea_grove_tab_completed))

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.idea_grove_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) },
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_tab_a11y, title) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                IdeaGrovePage(
                    items = state.aliveItems,
                    summary = pulseFlowSummary(state.aliveTotalDurationMs, state.aliveFlowCount, false),
                    emptyTitle = if (state.completedItems.isEmpty()) stringResource(R.string.idea_grove_no_ideas_yet) else stringResource(R.string.idea_grove_no_alive_ideas),
                    emptyBody = if (state.completedItems.isEmpty()) stringResource(R.string.idea_grove_empty_create_pulse_hint) else stringResource(R.string.idea_grove_alive_empty_body),
                    sort = state.aliveSort,
                    showSort = true,
                    expandedPulseId = state.expandedPulseId,
                    onPulseClicked = onPulseClicked,
                    onSortChanged = onSortChanged,
                    onFlowClicked = onFlowClicked,
                    onMarkAsInsightClicked = onMarkAsInsightClicked,
                    onMarkCompletedClicked = onMarkCompletedClicked,
                    onReviveClicked = onReviveClicked
                )
            } else {
                IdeaGrovePage(
                    items = state.completedItems,
                    summary = pulseFlowSummary(state.completedTotalDurationMs, state.completedFlowCount, true),
                    emptyTitle = stringResource(R.string.idea_grove_no_completed_ideas_yet),
                    emptyBody = stringResource(R.string.idea_grove_completed_empty_body),
                    sort = state.aliveSort,
                    showSort = false,
                    expandedPulseId = state.expandedPulseId,
                    onPulseClicked = onPulseClicked,
                    onSortChanged = onSortChanged,
                    onFlowClicked = onFlowClicked,
                    onMarkAsInsightClicked = onMarkAsInsightClicked,
                    onMarkCompletedClicked = onMarkCompletedClicked,
                    onReviveClicked = onReviveClicked
                )
            }
        }
    }
}

@Composable
private fun IdeaGrovePage(
    items: List<IdeaGroveItemUiModel>,
    summary: String,
    emptyTitle: String,
    emptyBody: String,
    sort: IdeaGroveSort,
    showSort: Boolean,
    expandedPulseId: Long?,
    onPulseClicked: (Long) -> Unit,
    onSortChanged: (IdeaGroveSort) -> Unit,
    onFlowClicked: (Long) -> Unit,
    onMarkAsInsightClicked: (Long) -> Unit,
    onMarkCompletedClicked: (Long) -> Unit,
    onReviveClicked: (Long) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            IdeaGroveSummaryCard(summary)
        }
        if (showSort) {
            item { IdeaGroveSortControl(sort, onSortChanged) }
        }
        if (items.isEmpty()) {
            item { IdeaGroveEmptyState(emptyTitle, emptyBody) }
        } else {
            items(items, key = { "pulse_${it.pulseId}" }) { item ->
                IdeaPulseCard(
                    item = item,
                    expanded = expandedPulseId == item.pulseId,
                    onClick = { onPulseClicked(item.pulseId) },
                    onFlow = { onFlowClicked(item.pulseId) },
                    onMarkAsInsight = { onMarkAsInsightClicked(item.pulseId) },
                    onMarkCompleted = { onMarkCompletedClicked(item.pulseId) },
                    onRevive = { onReviveClicked(item.pulseId) }
                )
            }
        }
    }
}

@Composable
private fun IdeaGroveSummaryCard(summary: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IdeaGroveSortControl(sort: IdeaGroveSort, onSortChanged: (IdeaGroveSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = sort.label()
    AssistChip(
        onClick = { expanded = true },
        label = { Text(stringResource(R.string.idea_grove_sort_prefix, label)) },
        modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_sort_a11y, label) }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        IdeaGroveSort.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label()) },
                onClick = {
                    onSortChanged(option)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun IdeaPulseCard(
    item: IdeaGroveItemUiModel,
    expanded: Boolean,
    onClick: () -> Unit,
    onFlow: () -> Unit,
    onMarkAsInsight: () -> Unit,
    onMarkCompleted: () -> Unit,
    onRevive: () -> Unit
) {
    val cardDescription = cardA11y(item, expanded)
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = cardDescription; role = Role.Button }
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            item.journeyName?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (expanded && item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(primarySummary(item), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(dateSummary(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (item.type) {
                        IdeaGroveItemType.RAW_PULSE -> {
                            Button(onClick = onFlow, modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_start_flow_from_a11y, item.title) }) { Text(stringResource(R.string.idea_grove_action_flow)) }
                            OutlinedButton(onClick = onMarkAsInsight, modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_mark_as_insight_a11y, item.title) }) { Text(stringResource(R.string.idea_grove_action_mark_as_insight)) }
                        }
                        IdeaGroveItemType.IDEA -> {
                            Button(onClick = onFlow, modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_start_flow_from_a11y, item.title) }) { Text(stringResource(R.string.idea_grove_action_flow)) }
                            OutlinedButton(onClick = onMarkCompleted, modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_mark_completed_a11y, item.title) }) { Text(stringResource(R.string.idea_grove_action_mark_completed)) }
                        }
                        IdeaGroveItemType.INSIGHT,
                        IdeaGroveItemType.COMPLETED_IDEA -> {
                            Button(onClick = onRevive, modifier = Modifier.semantics { contentDescription = stringResource(R.string.idea_grove_revive_a11y, item.title) }) { Text(stringResource(R.string.idea_grove_action_revive)) }
                        }
                    }
                }
                when {
                    item.type == IdeaGroveItemType.RAW_PULSE -> {
                        Text(stringResource(R.string.idea_grove_no_flows_from_pulse_yet), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.idea_grove_start_flow_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.type == IdeaGroveItemType.INSIGHT -> {
                        Text(stringResource(R.string.idea_grove_insight_body), style = MaterialTheme.typography.bodyMedium)
                    }
                    item.flows.isNotEmpty() -> IdeaFlowHistorySection(item.flows)
                }
            }
        }
    }
}

@Composable
private fun IdeaFlowHistorySection(flows: List<IdeaGroveFlowUiModel>) {
    var showAll by remember { mutableStateOf(false) }
    val visible = if (showAll || flows.size <= 5) flows else flows.take(5)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.idea_grove_flow_history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        visible.forEach { IdeaFlowHistoryRow(it) }
        if (flows.size > 5) {
            OutlinedButton(onClick = { showAll = !showAll }) {
                Text(if (showAll) stringResource(R.string.idea_grove_show_less) else stringResource(R.string.idea_grove_show_more))
            }
        }
    }
}

@Composable
private fun IdeaFlowHistoryRow(flow: IdeaGroveFlowUiModel) {
    Column(Modifier.fillMaxWidth()) {
        Text(flow.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(flow.journeyName, formatIdeaGroveDuration(flow.durationMs), shortDate(flow.endTime ?: flow.startTime)).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IdeaGroveEmptyState(title: String, body: String) {
    ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun pulseFlowSummary(durationMs: Long, flowCount: Int, completed: Boolean): String = when {
    flowCount <= 0 && completed -> stringResource(R.string.idea_grove_no_completed_pulse_flows_yet)
    flowCount <= 0 -> stringResource(R.string.idea_grove_no_pulse_flows_yet)
    else -> stringResource(R.string.idea_grove_summary_spent, formatIdeaGroveDuration(durationMs), flowCount)
}

@Composable
private fun IdeaGroveSort.label(): String = when (this) {
    IdeaGroveSort.Newest -> stringResource(R.string.idea_grove_sort_newest)
    IdeaGroveSort.Oldest -> stringResource(R.string.idea_grove_sort_oldest)
    IdeaGroveSort.MostTime -> stringResource(R.string.idea_grove_sort_most_time)
    IdeaGroveSort.LeastTime -> stringResource(R.string.idea_grove_sort_least_time)
}

@Composable
private fun primarySummary(item: IdeaGroveItemUiModel): String = when (item.type) {
    IdeaGroveItemType.RAW_PULSE -> stringResource(R.string.idea_grove_no_flows_yet)
    IdeaGroveItemType.INSIGHT -> stringResource(R.string.idea_grove_insight)
    IdeaGroveItemType.IDEA,
    IdeaGroveItemType.COMPLETED_IDEA -> stringResource(
        R.string.idea_grove_flow_summary,
        item.flowCount,
        if (item.flowCount == 1) stringResource(R.string.idea_grove_flow_singular) else stringResource(R.string.idea_grove_flow_plural),
        formatIdeaGroveDuration(item.totalFlowDurationMs)
    )
}

@Composable
private fun dateSummary(item: IdeaGroveItemUiModel): String = when (item.type) {
    IdeaGroveItemType.RAW_PULSE -> stringResource(R.string.idea_grove_created_date, shortDate(item.createdAt))
    IdeaGroveItemType.IDEA -> stringResource(R.string.idea_grove_last_worked_date, shortDate(item.lastWorkedAt ?: item.updatedAt))
    IdeaGroveItemType.INSIGHT,
    IdeaGroveItemType.COMPLETED_IDEA -> stringResource(R.string.idea_grove_completed_date, shortDate(item.groveStatusChangedAt ?: item.updatedAt))
}

@Composable
private fun cardA11y(item: IdeaGroveItemUiModel, expanded: Boolean): String {
    val journey = item.journeyName?.let { stringResource(R.string.idea_grove_card_journey_a11y, it) }.orEmpty()
    val flowLabel = if (item.flowCount == 1) stringResource(R.string.idea_grove_flow_singular) else stringResource(R.string.idea_grove_flow_plural)
    val status = when (item.type) {
        IdeaGroveItemType.RAW_PULSE -> stringResource(R.string.idea_grove_card_raw_a11y)
        IdeaGroveItemType.INSIGHT -> stringResource(R.string.idea_grove_card_insight_a11y)
        else -> stringResource(
            R.string.idea_grove_card_flows_a11y,
            item.flowCount,
            flowLabel,
            formatIdeaGroveDurationForSpeech(item.totalFlowDurationMs)
        )
    }
    return stringResource(
        R.string.idea_grove_card_a11y,
        item.title,
        journey,
        status,
        dateSummary(item),
        if (expanded) stringResource(R.string.idea_grove_collapse_a11y) else stringResource(R.string.idea_grove_expand_a11y)
    )
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private fun shortDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DATE_FMT)
