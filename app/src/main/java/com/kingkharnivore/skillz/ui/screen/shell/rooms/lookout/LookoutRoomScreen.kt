@file:OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.domain.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.domain.lookout.ObjectiveKind
import com.kingkharnivore.skillz.domain.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellPearlMiniIcon
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.viewmodel.shell.LookoutJourneyUiState
import com.kingkharnivore.skillz.viewmodel.shell.LookoutUiState
import com.kingkharnivore.skillz.viewmodel.shell.LookoutViewModel
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveCardUiState
import com.kingkharnivore.skillz.viewmodel.shell.ObjectivePeriodUiState
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveRemoveDialogState
import com.kingkharnivore.skillz.viewmodel.shell.ObjectiveRewardDialogState
import com.kingkharnivore.skillz.viewmodel.shell.SetObjectiveDialogState
import java.time.DayOfWeek
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
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectPeriod(ObjectivePeriod.entries[pagerState.currentPage])
    }

    Box(modifier = modifier.fillMaxSize().background(shellChamberBrush())) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
            LookoutHeader(onSetObjective = { viewModel.openSetObjective(uiState.selectedPeriod) })
            Spacer(Modifier.height(12.dp))
            PeriodTabs(
                selected = uiState.selectedPeriod,
                onSelected = { period ->
                    viewModel.selectPeriod(period)
                    scope.launch { pagerState.animateScrollToPage(ObjectivePeriod.entries.indexOf(period)) }
                }
            )
            Spacer(Modifier.height(10.dp))
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val period = ObjectivePeriod.entries[page]
                    val periodState = when (period) {
                        ObjectivePeriod.Daily -> uiState.daily
                        ObjectivePeriod.Weekly -> uiState.weekly
                        ObjectivePeriod.Monthly -> uiState.monthly
                    }
                    ObjectivePeriodPage(
                        state = periodState,
                        onSetObjective = { viewModel.openSetObjective(period) },
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
private fun LookoutHeader(onSetObjective: () -> Unit) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), modifier = Modifier.size(54.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("The Lookout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Set Objectives for the Journeys you want to move forward.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onSetObjective) { Icon(Icons.Outlined.Add, null); Text("Set") }
        }
    }
}

@Composable
private fun PeriodTabs(selected: ObjectivePeriod, onSelected: (ObjectivePeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ObjectivePeriod.entries.forEach { period ->
            val active = selected == period
            val colors = if (active) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            ElevatedCard(onClick = { onSelected(period) }, colors = colors, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                Text(period.label, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = TextAlign.Center, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ObjectivePeriodPage(
    state: ObjectivePeriodUiState,
    onSetObjective: () -> Unit,
    onCompletedClick: (Long) -> Unit,
    onRemoveClick: (Long) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(state.periodTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Text(state.summaryLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        }
        if (state.inProgress.isEmpty() && state.completed.isEmpty() && state.upcoming.isEmpty()) {
            item { EmptyPeriodState(state.period, onSetObjective) }
        } else {
            section("In Progress", state.inProgress, onCompletedClick, onRemoveClick)
            section("Completed", state.completed, onCompletedClick, onRemoveClick)
            section("Upcoming", state.upcoming, onCompletedClick, onRemoveClick, soft = true)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    cards: List<ObjectiveCardUiState>,
    onCompletedClick: (Long) -> Unit,
    onRemoveClick: (Long) -> Unit,
    soft: Boolean = false
) {
    item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
    if (cards.isEmpty()) {
        item { Text("Nothing here right now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (soft) 0.55f else 0.7f)) }
    } else {
        items(cards, key = { it.objectiveId }) { card ->
            ObjectiveCard(card, onCompletedClick, onRemoveClick, soft)
        }
    }
}

@Composable
private fun ObjectiveCard(card: ObjectiveCardUiState, onCompletedClick: (Long) -> Unit, onRemoveClick: (Long) -> Unit, soft: Boolean) {
    val completed = card.state == ObjectiveCardState.Completed
    val brush = if (completed) Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f), MaterialTheme.colorScheme.surface)) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = if (soft) 0.74f else 0.96f), MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (soft) 0.18f else 0.28f)))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(enabled = completed) { onCompletedClick(card.objectiveId) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(Modifier.background(brush).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(card.journeyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (completed) "${card.periodLabel} Objective completed" else "${card.periodLabel} · ${card.typeLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onRemoveClick(card.objectiveId) }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove Objective") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(card.progressLabel, fontWeight = FontWeight.SemiBold)
                    Text("${card.progressPercent}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { card.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)))
                Text(card.timeLeftLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (card.isRecurring) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatChip("Current streak: ${card.currentStreak}")
                        StatChip("Best: ${card.maxStreak}")
                        StatChip("Completed: ${card.totalCompletions}")
                    }
                }
                Text("Reward", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
private fun EmptyPeriodState(period: ObjectivePeriod, onSetObjective: () -> Unit) {
    val body = when (period) {
        ObjectivePeriod.Daily -> "Choose a Journey and set a small target for today."
        ObjectivePeriod.Weekly -> "Choose a Journey you want to move forward this week."
        ObjectivePeriod.Monthly -> "Set a larger target for the next 30 days."
    }
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("No ${period.label} Objectives set.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onSetObjective) { Text("Set ${period.label} Objective") }
        }
    }
}

@Composable
private fun SetObjectiveDialog(
    dialog: SetObjectiveDialogState,
    journeys: List<LookoutJourneyUiState>,
    onChange: ((SetObjectiveDialogState) -> SetObjectiveDialogState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(28.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.widthIn(max = 560.dp)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Set Objective", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Choose what you want to move forward.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Close") }
                }
                JourneyDropdown(dialog, journeys, onChange)
                Text("Start date", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onChange { it.copy(startDate = it.startDate.minusDays(1)) } }) { Text("Previous day") }
                    FilledTonalButton(onClick = {}) { Text(dialog.startDate.toString()) }
                    OutlinedButton(onClick = { onChange { it.copy(startDate = it.startDate.plusDays(1)) } }) { Text("Next day") }
                }
                Text("Objective period", fontWeight = FontWeight.SemiBold)
                ChoiceRow(ObjectivePeriod.entries, dialog.period, { it.label }) { onChange { d -> d.copy(period = it, weeklyBoundaryDay = d.startDate.dayOfWeek) } }
                Text("Objective type", fontWeight = FontWeight.SemiBold)
                ChoiceRow(ObjectiveKind.entries, dialog.kind, { it.label }) { onChange { d -> d.copy(kind = it) } }
                OutlinedTextField(value = dialog.targetMinutesText, onValueChange = { text -> onChange { it.copy(targetMinutesText = text.filter(Char::isDigit).take(5)) } }, label = { Text("Target time") }, suffix = { Text("minutes") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (dialog.period == ObjectivePeriod.Weekly) {
                    Text(if (dialog.kind == ObjectiveKind.OneTime) "Deadline day" else "Resets every", fontWeight = FontWeight.SemiBold)
                    ChoiceRow(DayOfWeek.entries, dialog.weeklyBoundaryDay, { it.name.lowercase().replaceFirstChar { c -> c.titlecase() }.take(3) }) { day -> onChange { it.copy(weeklyBoundaryDay = day) } }
                }
                if (dialog.period == ObjectivePeriod.Monthly) Text("Monthly Objectives run for 30 days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PreviewBlock(dialog)
                dialog.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = onSave) { Text("Save Objective") }
                }
            }
        }
    }
}

@Composable
private fun JourneyDropdown(dialog: SetObjectiveDialogState, journeys: List<LookoutJourneyUiState>, onChange: ((SetObjectiveDialogState) -> SetObjectiveDialogState) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(value = dialog.selectedJourneyName, onValueChange = {}, readOnly = true, label = { Text("Journey") }, trailingIcon = { Icon(Icons.Outlined.ExpandMore, null) }, modifier = Modifier.fillMaxWidth().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            journeys.forEach { journey ->
                DropdownMenuItem(text = { Text(journey.name) }, onClick = {
                    expanded = false
                    onChange { it.copy(selectedJourneyId = journey.id, selectedJourneyName = journey.name) }
                })
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            if (item == selected) Button(onClick = { onSelect(item) }) { Text(label(item)) }
            else OutlinedButton(onClick = { onSelect(item) }) { Text(label(item)) }
        }
    }
}

@Composable
private fun PreviewBlock(dialog: SetObjectiveDialogState) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Preview", fontWeight = FontWeight.Bold)
            Text("Starts: ${dialog.previewStarts}")
            Text("Ends: ${dialog.previewEnds}")
            Text(dialog.previewDuration)
            Text("Reward", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Text("Earn Pearls equal to the minutes spent completing this Objective.")
            Text("${dialog.selectedJourneyName} ${dialog.period.label} Objective badge +1")
            if (dialog.kind == ObjectiveKind.Recurring) Text("Each streak completion adds +10% to future Pearl rewards.")
        }
    }
}

@Composable
private fun RewardDialog(state: ObjectiveRewardDialogState, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(shape = RoundedCornerShape(30.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp)) { Box(contentAlignment = Alignment.Center) { ShellPearlMiniIcon(Modifier.size(38.dp)) } }
                Text(state.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.pearls, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(state.badge)
                state.streakBonus?.let { Text(it, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
                state.currentStreak?.let { Text(it) }
                Button(onClick = onDismiss) { Text("Done") }
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
                Text(if (state.isRecurring) "Remove Objective" else "Delete Objective?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.isRecurring) {
                    Text("Do you want to remove this Objective for the current period only, or stop it completely?")
                    val skip = when (state.period) { ObjectivePeriod.Daily -> "Skip today"; ObjectivePeriod.Weekly -> "Skip this week"; ObjectivePeriod.Monthly -> "Skip this cycle" }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onDismiss) { Text("Cancel") }; FilledTonalButton(onClick = onSkip) { Text(skip) }; Button(onClick = onStop) { Text("Stop completely") } }
                } else {
                    Text("This removes the Objective from future display.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onDismiss) { Text("Cancel") }; Button(onClick = onDeleteOneTime) { Text("Delete") } }
                }
            }
        }
    }
}
