@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.kingkharnivore.skillz.ui.screen.shell.rooms.ideagrove

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        onReviveClicked = viewModel::onReviveClicked,
        onDeletePulseClicked = viewModel::onDeletePulseClicked,
        onConfirmDeletePulse = viewModel::onConfirmDeletePulse,
        onDismissDeletePulse = viewModel::onDismissDeletePulse
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
    onReviveClicked: (Long) -> Unit,
    onDeletePulseClicked: (Long) -> Unit,
    onConfirmDeletePulse: () -> Unit,
    onDismissDeletePulse: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.idea_grove_tab_alive),
        stringResource(R.string.idea_grove_tab_completed)
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.idea_grove_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))
        IdeaGroveSegmentedControl(
            tabs = tabs,
            selectedIndex = pagerState.currentPage,
            onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )
        Spacer(Modifier.height(12.dp))
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                IdeaGrovePage(
                    items = state.aliveItems,
                    summary = pulseFlowSummary(state.totalPulseFlowDurationMs, state.totalPulseFlowCount, false),
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
                    onReviveClicked = onReviveClicked,
                    onDeletePulseClicked = onDeletePulseClicked
                )
            } else {
                IdeaGrovePage(
                    items = state.completedItems,
                    summary = pulseFlowSummary(state.completedPulseFlowDurationMs, state.completedPulseFlowCount, true),
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
                    onReviveClicked = onReviveClicked,
                    onDeletePulseClicked = onDeletePulseClicked
                )
            }
        }
    }

    if (state.pendingDeletePulseId != null) {
        IdeaGroveDeleteDialog(
            onConfirm = onConfirmDeletePulse,
            onDismiss = onDismissDeletePulse
        )
    }
}

@Composable
private fun IdeaGroveSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedIndex == index
                val tabA11y = stringResource(R.string.idea_grove_tab_a11y, title)
                val background by animateColorAsState(
                    targetValue = if (selected) scheme.surface else scheme.primary.copy(alpha = 0.00f),
                    label = "idea_grove_segment_background"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    label = "idea_grove_segment_text"
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = background,
                    shadowElevation = if (selected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(index) }
                        .semantics {
                            contentDescription = tabA11y
                            role = Role.Tab
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
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
    onReviveClicked: (Long) -> Unit,
    onDeletePulseClicked: (Long) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item { IdeaGroveSummaryCard(summary) }
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
                    onRevive = { onReviveClicked(item.pulseId) },
                    onDelete = { onDeletePulseClicked(item.pulseId) }
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
    val label = sortLabel(sort)
    val sortA11y = stringResource(R.string.idea_grove_sort_a11y, label)
    AssistChip(
        onClick = { expanded = true },
        label = { Text(stringResource(R.string.idea_grove_sort_prefix, label)) },
        modifier = Modifier.semantics { contentDescription = sortA11y }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        IdeaGroveSort.entries.forEach { option ->
            val optionLabel = sortLabel(option)
            DropdownMenuItem(
                text = { Text(optionLabel) },
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
    onRevive: () -> Unit,
    onDelete: () -> Unit
) {
    val cardDescription = cardA11y(item, expanded)
    val startFlowA11y = stringResource(R.string.idea_grove_start_flow_from_a11y, item.title)
    val markInsightA11y = stringResource(R.string.idea_grove_mark_as_insight_a11y, item.title)
    val markCompletedA11y = stringResource(R.string.idea_grove_mark_completed_a11y, item.title)
    val reviveA11y = stringResource(R.string.idea_grove_revive_a11y, item.title)
    val deleteA11y = stringResource(R.string.idea_grove_delete_pulse_a11y, item.title)

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = cardDescription
            role = Role.Button
        }
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
                            Button(onClick = onFlow, modifier = Modifier.semantics { contentDescription = startFlowA11y }) {
                                Text(stringResource(R.string.idea_grove_action_flow))
                            }
                            OutlinedButton(onClick = onMarkAsInsight, modifier = Modifier.semantics { contentDescription = markInsightA11y }) {
                                Text(stringResource(R.string.idea_grove_action_mark_as_insight))
                            }
                        }
                        IdeaGroveItemType.IDEA -> {
                            Button(onClick = onFlow, modifier = Modifier.semantics { contentDescription = startFlowA11y }) {
                                Text(stringResource(R.string.idea_grove_action_flow))
                            }
                            OutlinedButton(onClick = onMarkCompleted, modifier = Modifier.semantics { contentDescription = markCompletedA11y }) {
                                Text(stringResource(R.string.idea_grove_action_mark_completed))
                            }
                        }
                        IdeaGroveItemType.INSIGHT,
                        IdeaGroveItemType.COMPLETED_IDEA -> {
                            Button(onClick = onRevive, modifier = Modifier.semantics { contentDescription = reviveA11y }) {
                                Text(stringResource(R.string.idea_grove_action_revive))
                            }
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics { contentDescription = deleteA11y }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(flow.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(flow.journeyName, formatIdeaGroveDuration(flow.durationMs), shortDate(flow.endTime ?: flow.startTime)).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (flow.description.isNotBlank()) {
            Text(
                text = flow.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IdeaGroveDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.idea_grove_delete_pulse_title)) },
        text = { Text(stringResource(R.string.idea_grove_delete_pulse_body)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.idea_grove_delete_pulse_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.idea_grove_delete_pulse_cancel))
            }
        }
    )
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
private fun sortLabel(sort: IdeaGroveSort): String = when (sort) {
    IdeaGroveSort.Recents -> stringResource(R.string.idea_grove_sort_recents)
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
