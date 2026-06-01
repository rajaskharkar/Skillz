@file:OptIn(
    ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)

package com.kingkharnivore.skillz.ui.screen.shell.rooms.lookout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.domain.lookout.ObjectiveKind
import com.kingkharnivore.skillz.domain.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellPearlMiniIcon
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.viewmodel.shell.CompletedObjectiveHistoryGroupUiState
import com.kingkharnivore.skillz.viewmodel.shell.LookoutJourneyUiState
import com.kingkharnivore.skillz.viewmodel.shell.LookoutMode
import com.kingkharnivore.skillz.viewmodel.shell.LookoutUiState
import com.kingkharnivore.skillz.viewmodel.shell.LookoutViewModel
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveCardUiState
import com.kingkharnivore.skillz.viewmodel.shell.ObjectivePeriodUiState
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveRemoveDialogState
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveRewardDialogState
import com.kingkharnivore.skillz.viewmodel.shell.SetObjectiveDialogState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
fun LookoutRoomScreen(
    modifier: Modifier = Modifier,
    viewModel: LookoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { ObjectivePeriod.entries.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.selectedPeriod) {
        val page = ObjectivePeriod.entries.indexOf(uiState.selectedPeriod)
        if (pagerState.currentPage != page) pagerState.scrollToPage(page)
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectPeriod(ObjectivePeriod.entries[pagerState.currentPage])
    }

    Box(modifier = modifier.fillMaxSize().background(shellChamberBrush())) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
            LookoutHeader(
                mode = uiState.mode,
                onSetObjective = { viewModel.openSetObjective(uiState.selectedPeriod) },
                onCompleted = viewModel::showCompletedHistory,
                onBackToObjectives = viewModel::showObjectives
            )
            Spacer(Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.mode == LookoutMode.CompletedHistory) {
                CompletedHistoryView(uiState.completedHistory)
            } else {
                PeriodTabs(
                    selected = uiState.selectedPeriod,
                    onSelected = { period ->
                        viewModel.selectPeriod(period)
                        scope.launch { pagerState.scrollToPage(ObjectivePeriod.entries.indexOf(period)) }
                    }
                )
                Spacer(Modifier.height(10.dp))
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val period = ObjectivePeriod.entries[page]
                    val periodState = when (period) {
                        ObjectivePeriod.Daily -> uiState.daily
                        ObjectivePeriod.Weekly -> uiState.weekly
                        ObjectivePeriod.Monthly -> uiState.monthly
                    }
                    ObjectivePeriodPage(
                        state = periodState,
                        onCompletedClick = viewModel::showReward,
                        onRemoveClick = viewModel::requestRemove
                    )
                }
            }
        }
    }

    uiState.setObjectiveDialog?.let { dialog ->
        SetObjectiveDialog(
            dialog = dialog,
            journeys = uiState.journeys,
            onChange = viewModel::updateDialog,
            onStartDate = viewModel::setDialogStartDate,
            onDismiss = viewModel::dismissSetObjective,
            onSave = viewModel::saveObjective
        )
    }
    uiState.rewardDialog?.let { RewardDialog(it, onDismiss = viewModel::dismissReward) }
    uiState.removeDialog?.let {
        RemoveObjectiveDialog(
            state = it,
            onDismiss = viewModel::dismissRemove,
            onDeleteOneTime = viewModel::deleteOneTimeObjective,
            onSkip = viewModel::skipRecurringCycle,
            onStop = viewModel::stopRecurringObjective
        )
    }
}

@Composable
private fun LookoutHeader(
    mode: LookoutMode,
    onSetObjective: () -> Unit,
    onCompleted: () -> Unit,
    onBackToObjectives: () -> Unit
) {
    val title = stringResource(R.string.lookout_title)
    val setDescription = stringResource(R.string.lookout_set_objective)
    val completedDescription = stringResource(R.string.lookout_completed_history_title)
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), modifier = Modifier.size(54.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (mode == LookoutMode.CompletedHistory) Icons.Outlined.ArrowBack else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    if (mode == LookoutMode.CompletedHistory) stringResource(R.string.lookout_completed_history_title) else title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (mode == LookoutMode.CompletedHistory) stringResource(R.string.lookout_completed_history_body) else stringResource(R.string.lookout_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (mode == LookoutMode.CompletedHistory) {
                OutlinedButton(onClick = onBackToObjectives) { Text(stringResource(R.string.common_back)) }
            } else {
                OutlinedButton(
                    onClick = onCompleted,
                    modifier = Modifier.semantics { contentDescription = completedDescription; role = Role.Button }
                ) { Text(stringResource(R.string.lookout_completed)) }
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = onSetObjective,
                    modifier = Modifier.semantics { contentDescription = setDescription; role = Role.Button }
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Text(stringResource(R.string.lookout_set))
                }
            }
        }
    }
}

@Composable
private fun PeriodTabs(selected: ObjectivePeriod, onSelected: (ObjectivePeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ObjectivePeriod.entries.forEach { period ->
            val active = selected == period
            val container = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            val content = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            val label = periodLabel(period)
            ElevatedCard(
                onClick = { onSelected(period) },
                colors = CardDefaults.elevatedCardColors(containerColor = container),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = label; role = Role.Button }
            ) {
                Text(
                    label,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = content,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ObjectivePeriodPage(
    state: ObjectivePeriodUiState,
    onCompletedClick: (Long) -> Unit,
    onRemoveClick: (Long) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(state.periodTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Text(state.summaryLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        }
        if (state.inProgress.isEmpty() && state.completed.isEmpty() && state.upcoming.isEmpty()) {
            item { EmptyPeriodState(state.period) }
        } else {
            if (state.inProgress.isNotEmpty()) section(stringResourceId = R.string.lookout_section_in_progress, state.inProgress, onCompletedClick, onRemoveClick)
            if (state.completed.isNotEmpty()) section(stringResourceId = R.string.lookout_completed, state.completed, onCompletedClick, onRemoveClick)
            if (state.upcoming.isNotEmpty()) section(stringResourceId = R.string.lookout_section_upcoming, state.upcoming, onCompletedClick, onRemoveClick, soft = true)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    stringResourceId: Int,
    cards: List<ObjectiveCardUiState>,
    onCompletedClick: (Long) -> Unit,
    onRemoveClick: (Long) -> Unit,
    soft: Boolean = false
) {
    item { Text(stringResource(stringResourceId), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
    items(cards, key = { it.objectiveId }) { card ->
        ObjectiveCard(card, onCompletedClick, onRemoveClick, soft)
    }
}

@Composable
private fun ObjectiveCard(card: ObjectiveCardUiState, onCompletedClick: (Long) -> Unit, onRemoveClick: (Long) -> Unit, soft: Boolean) {
    val completed = card.state == ObjectiveCardState.Completed
    val removeDescription = stringResource(R.string.lookout_remove_objective)
    val brush = if (completed) {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f), MaterialTheme.colorScheme.surface))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = if (soft) 0.74f else 0.96f), MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (soft) 0.18f else 0.28f)))
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = completed) { onCompletedClick(card.objectiveId) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(Modifier.background(brush).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(card.journeyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (completed) stringResource(R.string.lookout_period_objective_completed, card.periodLabel) else stringResource(R.string.lookout_card_period_type, card.periodLabel, card.typeLabel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRemoveClick(card.objectiveId) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = removeDescription)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(card.progressLabel, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.lookout_percent, card.progressPercent), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { card.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)))
                Text(card.timeLeftLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (card.isRecurring) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatChip(stringResource(R.string.lookout_current_streak_value, card.currentStreak ?: 0))
                        StatChip(stringResource(R.string.lookout_best_streak_value, card.maxStreak ?: 0))
                        StatChip(stringResource(R.string.lookout_total_completed_value, card.totalCompletions ?: 0))
                    }
                }
                Text(stringResource(R.string.lookout_reward), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, leadingIcon = { ShellPearlMiniIcon(Modifier.size(16.dp)) }, label = { Text(card.estimatedRewardLabel) })
                    AssistChip(onClick = {}, leadingIcon = { Icon(Icons.Outlined.MilitaryTech, null, Modifier.size(16.dp)) }, label = { Text(card.badgeLabel) })
                    card.streakBonusLabel?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun EmptyPeriodState(period: ObjectivePeriod) {
    val body = when (period) {
        ObjectivePeriod.Daily -> stringResource(R.string.lookout_empty_daily_body)
        ObjectivePeriod.Weekly -> stringResource(R.string.lookout_empty_weekly_body)
        ObjectivePeriod.Monthly -> stringResource(R.string.lookout_empty_monthly_body)
    }
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.lookout_no_period_objectives, periodLabel(period)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompletedHistoryView(groups: List<CompletedObjectiveHistoryGroupUiState>) {
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (groups.isEmpty()) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.lookout_history_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.lookout_history_empty_body), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(groups, key = { it.journeyName }) { group ->
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.journeyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        group.rows.forEach { row ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(row.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(row.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(row.lastCompletedLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetObjectiveDialog(
    dialog: SetObjectiveDialogState,
    journeys: List<LookoutJourneyUiState>,
    onChange: ((SetObjectiveDialogState) -> SetObjectiveDialogState) -> Unit,
    onStartDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(28.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.widthIn(max = 560.dp)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.lookout_set_objective), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.lookout_set_objective_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close)) }
                }
                JourneyComboField(dialog, journeys, onChange)
                Text(stringResource(R.string.lookout_start_date), fontWeight = FontWeight.SemiBold)
                StartDateSelector(dialog = dialog, onStartDate = onStartDate)
                Text(stringResource(R.string.lookout_objective_period), fontWeight = FontWeight.SemiBold)
                ChoiceRow(ObjectivePeriod.entries, dialog.period, { periodLabel(it) }) { period -> onChange { d -> d.copy(period = period, weeklyBoundaryDay = if (d.weeklyBoundaryWasChanged) d.weeklyBoundaryDay else d.startDate.dayOfWeek) } }
                Text(stringResource(R.string.lookout_objective_type), fontWeight = FontWeight.SemiBold)
                ChoiceRow(ObjectiveKind.entries, dialog.kind, { kindLabel(it) }) { onChange { d -> d.copy(kind = it) } }
                TargetTimeField(dialog, onChange)
                if (dialog.period == ObjectivePeriod.Weekly) {
                    Text(if (dialog.kind == ObjectiveKind.OneTime) stringResource(R.string.lookout_deadline_day) else stringResource(R.string.lookout_resets_every), fontWeight = FontWeight.SemiBold)
                    ChoiceRow(DayOfWeek.entries, dialog.weeklyBoundaryDay, { dayLabel(it) }) { day -> onChange { it.copy(weeklyBoundaryDay = day, weeklyBoundaryWasChanged = true) } }
                }
                if (dialog.period == ObjectivePeriod.Monthly) Text(stringResource(R.string.lookout_monthly_helper), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dialog.kind == ObjectiveKind.Recurring) Text(stringResource(R.string.lookout_recurring_helper), color = MaterialTheme.colorScheme.onSurfaceVariant)
                dialog.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = onSave) { Text(stringResource(R.string.lookout_save_objective)) }
                }
            }
        }
    }
}

@Composable
private fun JourneyComboField(
    dialog: SetObjectiveDialogState,
    journeys: List<LookoutJourneyUiState>,
    onChange: ((SetObjectiveDialogState) -> SetObjectiveDialogState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val trimmed = dialog.journeyText.trim()
    val suggestions = journeys.filter { journey ->
        trimmed.isBlank() || journey.name.contains(trimmed, ignoreCase = true)
    }
    val exactMatch = journeys.any { it.name.equals(trimmed, ignoreCase = true) }
    val createLabel = stringResource(R.string.lookout_create_journey, trimmed)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = dialog.journeyText,
            onValueChange = { text ->
                expanded = true
                val matched = journeys.firstOrNull { it.name.equals(text.trim(), ignoreCase = true) }
                onChange { it.copy(journeyText = text, selectedJourneyId = matched?.id) }
            },
            label = { Text(stringResource(R.string.lookout_journey)) },
            placeholder = { Text(stringResource(R.string.lookout_choose_journey)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            suggestions.forEach { journey ->
                DropdownMenuItem(
                    text = { Text(journey.name) },
                    onClick = {
                        expanded = false
                        onChange { it.copy(selectedJourneyId = journey.id, journeyText = journey.name) }
                    }
                )
            }
            if (trimmed.isNotBlank() && !exactMatch) {
                DropdownMenuItem(
                    text = { Text(createLabel) },
                    onClick = {
                        expanded = false
                        onChange { it.copy(selectedJourneyId = null, journeyText = trimmed) }
                    }
                )
            }
        }
    }
}

@Composable
private fun TargetTimeField(
    dialog: SetObjectiveDialogState,
    onChange: ((SetObjectiveDialogState) -> SetObjectiveDialogState) -> Unit
) {
    val targetPearls = dialog.targetMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 0
    OutlinedTextField(
        value = dialog.targetMinutesText,
        onValueChange = { text -> onChange { it.copy(targetMinutesText = text.filter(Char::isDigit).take(5)) } },
        label = { Text(stringResource(R.string.lookout_target_time)) },
        suffix = { Text(stringResource(R.string.lookout_minutes)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (targetPearls > 0) {
        Text(stringResource(R.string.lookout_at_least_pearls_when_completed, targetPearls), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StartDateSelector(dialog: SetObjectiveDialogState, onStartDate: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceButton(selected = dialog.startDate == today, label = stringResource(R.string.lookout_today)) { onStartDate(today) }
        ChoiceButton(selected = dialog.startDate == tomorrow, label = stringResource(R.string.lookout_tomorrow)) { onStartDate(tomorrow) }
        OutlinedButton(onClick = { showDatePicker = true }) {
            Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.lookout_choose_date))
        }
    }
    if (dialog.startDate != today && dialog.startDate != tomorrow) {
        Text(stringResource(R.string.lookout_selected_date, formatter.format(dialog.startDate)), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showDatePicker) {
        LookoutDatePickerDialog(
            initialDate = dialog.startDate,
            onDateSelected = { date ->
                onStartDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun LookoutDatePickerDialog(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() >= today
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val selected = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: initialDate
                    if (!selected.isBefore(today)) onDateSelected(selected)
                }
            ) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    ) {
        DatePicker(state = state, title = { Text(stringResource(R.string.lookout_choose_date), modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) })
    }
}

@Composable
private fun <T> ChoiceRow(items: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item -> ChoiceButton(selected = item == selected, label = label(item)) { onSelect(item) } }
    }
}

@Composable
private fun ChoiceButton(selected: Boolean, label: String, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) } else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun RewardDialog(state: ObjectiveRewardDialogState, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(30.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { ShellPearlMiniIcon(Modifier.size(38.dp)) }
                }
                Text(state.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.pearls, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(state.badge)
                state.streakBonus?.let { Text(it, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
                state.currentStreak?.let { Text(it) }
                Button(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            }
        }
    }
}

@Composable
private fun RemoveObjectiveDialog(
    state: ObjectiveRemoveDialogState,
    onDismiss: () -> Unit,
    onDeleteOneTime: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(26.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (state.isRecurring) stringResource(R.string.lookout_remove_objective) else stringResource(R.string.lookout_delete_objective), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.isRecurring) {
                    Text(stringResource(R.string.lookout_remove_objective_body))
                    val skip = when (state.period) {
                        ObjectivePeriod.Daily -> stringResource(R.string.lookout_skip_today)
                        ObjectivePeriod.Weekly -> stringResource(R.string.lookout_skip_this_week)
                        ObjectivePeriod.Monthly -> stringResource(R.string.lookout_skip_this_cycle)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                        FilledTonalButton(onClick = onSkip) { Text(skip) }
                        Button(onClick = onStop) { Text(stringResource(R.string.lookout_stop_completely)) }
                    }
                } else {
                    Text(stringResource(R.string.lookout_delete_objective_body))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                        Button(onClick = onDeleteOneTime) { Text(stringResource(R.string.common_delete)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun periodLabel(period: ObjectivePeriod): String = when (period) {
    ObjectivePeriod.Daily -> stringResource(R.string.lookout_period_daily)
    ObjectivePeriod.Weekly -> stringResource(R.string.lookout_period_weekly)
    ObjectivePeriod.Monthly -> stringResource(R.string.lookout_period_monthly)
}

@Composable
private fun kindLabel(kind: ObjectiveKind): String = when (kind) {
    ObjectiveKind.OneTime -> stringResource(R.string.lookout_type_one_time)
    ObjectiveKind.Recurring -> stringResource(R.string.lookout_type_recurring)
}

@Composable
private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.lookout_monday_short)
    DayOfWeek.TUESDAY -> stringResource(R.string.lookout_tuesday_short)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.lookout_wednesday_short)
    DayOfWeek.THURSDAY -> stringResource(R.string.lookout_thursday_short)
    DayOfWeek.FRIDAY -> stringResource(R.string.lookout_friday_short)
    DayOfWeek.SATURDAY -> stringResource(R.string.lookout_saturday_short)
    DayOfWeek.SUNDAY -> stringResource(R.string.lookout_sunday_short)
}
