package com.kingkharnivore.skillz.ui.screen.paths

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.paths.*
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.ui.screen.chronicle.ExpandedChronicle
import com.kingkharnivore.skillz.ui.screen.chronicle.LocalChronicleReaderFactory
import com.kingkharnivore.skillz.utils.time.formatDuration
import com.kingkharnivore.skillz.viewmodel.PathsViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PathsScreen(viewModel: PathsViewModel, onOpenFlowPlan: (FlowPlanListItemUiModel) -> Unit,
    onResumeFlow: () -> Unit, onPlanArc: () -> Unit, onOpenArc: (Long) -> Unit,
    onOpenSuggestedRoute: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val arcs by viewModel.arcPlans.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(HorizonSection.PLANS) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDreams by rememberSaveable { mutableStateOf(false) }
    var showOtherFlow by remember { mutableStateOf(false) }
    val edited = (state.flowPlans + state.dreamFlowPlans).firstOrNull { it.id == editId }
    fun launch(plan: FlowPlanListItemUiModel) {
        val ongoing = state.ongoing
        if (ongoing?.originPlanId == plan.id) { onResumeFlow(); return }
        if (ongoing != null && (ongoing.isRunning || ongoing.isInFlowMode || ongoing.accumulatedBeforeStartMs > 0)) {
            showOtherFlow = true
            return
        }
        viewModel.launched(plan.id)
        onOpenFlowPlan(plan)
    }
    CompositionLocalProvider(LocalChronicleReaderFactory provides remember(viewModel) { viewModel::createHistoricalChronicle }) {
        HorizonContent(state.copy(selectedSection = section, arcPlans = arcs), showDreams, { showDreams = !showDreams }, viewModel::onPrimaryTabSelected,
            onNew = { editId = null; viewModel.clearError(); showEditor = true },
            onEdit = { editId = it.id; viewModel.clearError(); showEditor = true },
            onLaunch = ::launch, onPin = viewModel::pin, onArchive = viewModel::archive,
            onDelete = viewModel::delete, onPlanArc = onPlanArc, onOpenArc = onOpenArc,
            onDeleteArc = viewModel::deleteArcPlan, onOpenSuggestedRoute = onOpenSuggestedRoute,
            onSection = { section = it },
            onRemoveMemory = viewModel::removeMemory,
            onPlanAgain = { viewModel.planAgain(it) {} })
    }
    if (showEditor) ActivityEditor(edited, state.selectedPrimaryTab, state.tags, state.isSaving,
        state.errorMessage, onClose = { showEditor = false }, onSave = { title, tag, kind, mode, minutes, surge ->
            viewModel.saveActivity(editId, title, tag, kind, mode, minutes, surge) { showEditor = false }
        })
    if (showOtherFlow) AlertDialog(onDismissRequest = { showOtherFlow = false },
        title = { Text(stringResource(R.string.horizon_active_flow)) },
        text = { Text(stringResource(R.string.horizon_active_flow_body)) },
        confirmButton = { TextButton(onClick = { showOtherFlow = false; onResumeFlow() }) { Text(stringResource(R.string.horizon_resume)) } },
        dismissButton = { TextButton(onClick = { showOtherFlow = false }) { Text(stringResource(R.string.common_cancel)) } })
}

@Composable
internal fun HorizonContent(state: PathsUiState, showDreams: Boolean, onToggleDreams: () -> Unit,
    onTab: (PathsPrimaryTab) -> Unit, onNew: () -> Unit, onEdit: (FlowPlanListItemUiModel) -> Unit,
    onLaunch: (FlowPlanListItemUiModel) -> Unit, onPin: (Long, Boolean) -> Unit,
    onArchive: (Long, Boolean) -> Unit, onDelete: (Long) -> Unit,
    onPlanArc: () -> Unit, onOpenArc: (Long) -> Unit, onDeleteArc: (Long) -> Unit,
    onOpenSuggestedRoute: (String) -> Unit, onSection: (HorizonSection) -> Unit,
    onRemoveMemory: (MemoryFlowUi) -> Unit = {},
    onPlanAgain: (MemoryGroupUi) -> Unit) {
    val kind = if (state.selectedPrimaryTab == PathsPrimaryTab.PLANS) HorizonKind.PLAN else HorizonKind.HABIT
    val activities = state.flowPlans.filter { it.kind == kind }
    val dreams = state.dreamFlowPlans.filter { it.kind == kind }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PathsHeader() }
        item {
            SegmentedSurface {
                Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SegmentedChoice(Modifier.weight(1f), stringResource(R.string.horizon_plans),
                        state.selectedSection == HorizonSection.PLANS, { onSection(HorizonSection.PLANS) })
                    SegmentedChoice(Modifier.weight(1f), stringResource(R.string.horizon_planned_arcs),
                        state.selectedSection == HorizonSection.PLANNED_ARCS, { onSection(HorizonSection.PLANNED_ARCS) })
                }
            }
        }
        if (state.selectedSection == HorizonSection.PLANS) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.horizon_plans), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onNew, shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.horizon_new))
                    }
                }
            }
            item {
                SegmentedSurface {
                    Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PathsPrimaryTab.entries.forEach { tab ->
                            SegmentedChoice(Modifier.weight(1f), stringResource(when (tab) {
                                PathsPrimaryTab.HABITS -> R.string.horizon_habits
                                PathsPrimaryTab.PLANS -> R.string.horizon_plans
                                PathsPrimaryTab.MEMORIES -> R.string.horizon_memories
                            }), state.selectedPrimaryTab == tab, { onTab(tab) })
                        }
                    }
                }
            }
        }
        state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        when {
            state.isLoading -> item { CircularProgressIndicator() }
            state.selectedSection == HorizonSection.PLANNED_ARCS -> {
                item {
                    PlannedArcsHeader(stringResource(R.string.horizon_planned_arcs),
                        stringResource(R.string.paths_your_arcs_subtitle), stringResource(R.string.paths_plan_arc), onPlanArc)
                }
                if (state.arcPlans.isEmpty()) item { HorizonEmpty(R.string.paths_empty_arcs_title, R.string.paths_empty_arcs_body) }
                items(state.arcPlans, key = { "arc:${it.id}" }) { arc ->
                    PlannedArcCard(arc, onClick = { onOpenArc(arc.id) }, onDelete = { onDeleteArc(arc.id) })
                }
                item {
                    SuggestedSequencesHelper(stringResource(R.string.paths_suggested_sequences_helper_title),
                        stringResource(R.string.paths_suggested_sequences_helper_body),
                        stringResource(R.string.paths_suggested_scenes_title), stringResource(R.string.paths_suggested_scenes_subtitle),
                        onOpenSuggestedRoute)
                }
            }
            state.selectedPrimaryTab == PathsPrimaryTab.MEMORIES -> {
                item { Text(stringResource(R.string.horizon_memories_hint), style = MaterialTheme.typography.bodyMedium) }
                if (state.memories.isEmpty()) item { HorizonEmpty(R.string.horizon_empty_memories, R.string.horizon_empty_memories_body) }
                items(state.memories, key = { it.key }) { memory -> MemoryCard(memory, onRemoveMemory, onPlanAgain) }
            }
            else -> {
                item { Text(stringResource(if (kind == HorizonKind.HABIT) R.string.horizon_habits_hint else R.string.horizon_plans_hint),
                    style = MaterialTheme.typography.bodyMedium) }
                if (activities.isEmpty()) item {
                    HorizonEmpty(if (kind == HorizonKind.HABIT) R.string.horizon_empty_habits else R.string.horizon_empty_plans,
                        if (kind == HorizonKind.HABIT) R.string.horizon_empty_habits_body else R.string.horizon_empty_plans_body)
                }
                items(activities, key = { "active:${it.id}" }) { activity ->
                    ActivityCard(activity, state.ongoing?.originPlanId == activity.id, false,
                        { onLaunch(activity) }, { onEdit(activity) }, { onPin(activity.id, !activity.pinned) },
                        { onArchive(activity.id, true) }, { onDelete(activity.id) })
                }
                if (dreams.isNotEmpty()) {
                    item {
                        TextButton(onClick = onToggleDreams, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.horizon_dreams_count, dreams.size))
                            Icon(if (showDreams) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                        }
                    }
                    if (showDreams) items(dreams, key = { "dream:${it.id}" }) { activity ->
                        ActivityCard(activity, false, true, { onArchive(activity.id, false) }, { onEdit(activity) },
                            { onPin(activity.id, !activity.pinned) }, { onArchive(activity.id, false) }, { onDelete(activity.id) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun HorizonEmpty(title: Int, body: Int) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ActivityCard(activity: FlowPlanListItemUiModel, inProgress: Boolean, archived: Boolean,
    onLaunch: () -> Unit, onEdit: () -> Unit, onPin: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().clickable(enabled = !inProgress, onClick = onEdit),
        shape = RoundedCornerShape(22.dp),
        colors = flowCardColors(activity.mode)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (activity.kind == HorizonKind.HABIT) Icons.Outlined.Repeat else Icons.Outlined.Checklist, null)
                Text(activity.title, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.titleMedium)
                if (activity.pinned) Icon(Icons.Outlined.PushPin, stringResource(R.string.paths_pinned), Modifier.size(18.dp))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.paths_more_actions)) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.horizon_edit)) }, enabled = !inProgress, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text(stringResource(if (activity.pinned) R.string.paths_unpin else R.string.paths_pin)) }, onClick = { menu = false; onPin() })
                        DropdownMenuItem(text = { Text(stringResource(if (archived) R.string.horizon_restore else R.string.paths_move_to_dreams)) }, enabled = !inProgress,
                            onClick = { menu = false; onArchive() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.common_delete)) }, enabled = !inProgress,
                            onClick = { menu = false; deleting = true })
                    }
                }
            }
            if (activity.tagName.isNotBlank()) Text(activity.tagName, color = flowCardAccent(activity.mode), style = MaterialTheme.typography.labelLarge)
            Text(buildList {
                add(stringResource(flowModeLabel(activity.mode)))
                if (activity.launchWithSurge) add(stringResource(R.string.horizon_surge_minutes, activity.targetMinutes ?: 0))
            }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            if (activity.kind == HorizonKind.HABIT && activity.completionCount > 0) {
                Text(pluralStringResource(R.plurals.horizon_completion_count, activity.completionCount, activity.completionCount), style = MaterialTheme.typography.labelLarge)
                activity.lastCompletedAt?.let { Text(stringResource(R.string.horizon_last_completed, dateLabel(it)), style = MaterialTheme.typography.bodySmall) }
            }
            if (inProgress) Text(stringResource(R.string.horizon_in_progress), color = flowCardAccent(activity.mode))
            Button(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (archived) R.string.horizon_restore else if (inProgress) R.string.horizon_resume else R.string.horizon_start))
            }
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.paths_delete_planned_flow_title)) },
        text = { Text(stringResource(R.string.horizon_delete_body)) },
        confirmButton = { TextButton(onClick = { deleting = false; onDelete() }) { Text(stringResource(R.string.common_delete)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.common_cancel)) } })
}

@Composable
internal fun MemoryCard(memory: MemoryGroupUi, onRemoveMemory: (MemoryFlowUi) -> Unit = {},
    onPlanAgain: (MemoryGroupUi) -> Unit) {
    var showJourney by rememberSaveable(memory.key) { mutableStateOf(false) }
    var expandedFlows by rememberSaveable(memory.key) { mutableStateOf(listOf<String>()) }
    fun toggle(flow: MemoryFlowUi) {
        val id = flow.receipt.flowInstanceId
        expandedFlows = if (id in expandedFlows) expandedFlows - id else expandedFlows + id
    }
    Card(colors = flowCardColors(memory.latest.session?.mode ?: memory.latest.receipt.mode),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth().clickable { toggle(memory.latest) }) {
                Text(memory.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(if (memory.kind == HorizonKind.HABIT) R.string.horizon_habit else R.string.horizon_plan),
                    style = MaterialTheme.typography.labelMedium)
            }
            if (memory.kind == HorizonKind.HABIT) Text(pluralStringResource(R.plurals.horizon_completion_count, memory.flows.size, memory.flows.size),
                color = flowCardAccent(memory.latest.session?.mode ?: memory.latest.receipt.mode), style = MaterialTheme.typography.labelLarge)
            val visibleFlows = if (showJourney) memory.flows else listOf(memory.latest)
            visibleFlows.forEachIndexed { index, flow ->
                key(flow.receipt.flowInstanceId) {
                    if (index > 0) HorizontalDivider()
                    MemoryFlowDetails(flow, expanded = flow.receipt.flowInstanceId in expandedFlows,
                        onToggle = { toggle(flow) }, onDelete = { onRemoveMemory(flow) })
                }
            }
            if (memory.kind == HorizonKind.HABIT && memory.flows.size > 1) TextButton(onClick = { showJourney = !showJourney }) {
                Text(stringResource(if (showJourney) R.string.horizon_hide_flows else R.string.horizon_show_flows))
            }
            if (memory.kind == HorizonKind.PLAN) OutlinedButton(onClick = { onPlanAgain(memory) }) {
                Text(stringResource(R.string.horizon_plan_again))
            }
        }
    }
}

@Composable
private fun MemoryFlowDetails(flow: MemoryFlowUi, expanded: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    val receipt = flow.receipt
    val session = flow.session
    val mode = session?.mode ?: receipt.mode
    var deleting by rememberSaveable(receipt.flowInstanceId) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(session?.title ?: receipt.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    stringResource(if (expanded) R.string.flow_card_collapse else R.string.flow_card_expand))
            }
            if (flow.tagName.isNotBlank()) Text(flow.tagName, style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current)
            Text(dateLabel(receipt.completedAt), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(flowModeLabel(mode)) +
                " · " + formatDuration(session?.durationMs ?: receipt.durationMs), style = MaterialTheme.typography.bodyMedium)
            if (mode != FlowMode.SOFT) Text(stringResource(R.string.flow_card_scyra_score_value, session?.scyraPoints ?: receipt.score), style = MaterialTheme.typography.bodySmall)
            val surge = session?.surgePoints ?: receipt.surgePoints
            if (surge > 0) Text(stringResource(R.string.flow_card_surge_points_value, surge), style = MaterialTheme.typography.bodySmall)
            val arcId = session?.arcId ?: receipt.arcId
            if (arcId != null) {
                Text(stringResource(R.string.horizon_part_of_arc), style = MaterialTheme.typography.labelLarge)
                (session?.arcMultiplierUsed ?: receipt.arcMultiplier)?.let { multiplier ->
                    Text(stringResource(R.string.flow_card_multiplier_used_value, String.format(Locale.getDefault(), "%.1f", multiplier)))
                }
            }
        }
        if (expanded) {
            if (session != null) ExpandedChronicle("SESSION", session.id, session.description)
            else Text(stringResource(R.string.horizon_flow_removed), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = { deleting = true }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.horizon_remove_memory))
            }
        }
    }
    if (deleting) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.horizon_remove_memory_title)) },
        text = { Text(stringResource(R.string.horizon_remove_memory_body)) },
        confirmButton = { TextButton(onClick = { deleting = false; onDelete() }) { Text(stringResource(R.string.horizon_remove_memory)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.common_cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ActivityEditor(activity: FlowPlanListItemUiModel?, tab: PathsPrimaryTab, tags: List<TagUiModel>, saving: Boolean,
    error: String?, onClose: () -> Unit, onSave: (String, String, String, FlowMode, Int?, Boolean) -> Unit) {
    var title by rememberSaveable { mutableStateOf(activity?.title.orEmpty()) }
    var tag by rememberSaveable { mutableStateOf(activity?.tagName.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(activity?.kind ?: if (tab == PathsPrimaryTab.HABITS) HorizonKind.HABIT else HorizonKind.PLAN) }
    var mode by rememberSaveable { mutableStateOf(activity?.mode ?: FlowMode.FLOW) }
    var surge by rememberSaveable { mutableStateOf(activity?.launchWithSurge ?: false) }
    var minutes by rememberSaveable { mutableStateOf(activity?.targetMinutes?.toString().orEmpty()) }
    ModalBottomSheet(onDismissRequest = { if (!saving) onClose() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(if (activity == null) R.string.horizon_new_activity else R.string.horizon_edit), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(kind == HorizonKind.HABIT, onClick = { kind = HorizonKind.HABIT }, enabled = !saving, label = { Text(stringResource(R.string.horizon_habit)) })
                FilterChip(kind == HorizonKind.PLAN, onClick = { kind = HorizonKind.PLAN }, enabled = !saving, label = { Text(stringResource(R.string.horizon_plan)) })
            }
            Text(stringResource(if (kind == HorizonKind.HABIT) R.string.horizon_habits_hint else R.string.horizon_plans_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(title, { title = it.take(100) }, label = { Text(stringResource(R.string.paths_title_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
            OutlinedTextField(tag, { tag = it }, label = { Text(stringResource(R.string.pulse_edit_journey_field_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
            if (tags.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it.id }) { item -> SuggestionChip(onClick = { tag = item.name }, enabled = !saving, label = { Text(item.name) }) }
            }
            Text(stringResource(R.string.flow_screen_mode_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowMode.entries.forEach { option ->
                    FilterChip(selected = mode == option, onClick = {
                        mode = option
                        if (option == FlowMode.SOFT) surge = false
                    }, enabled = !saving, label = { Text(stringResource(flowModeLabel(option))) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.paths_surge), Modifier.weight(1f))
                Switch(surge, onCheckedChange = { surge = it }, enabled = mode != FlowMode.SOFT && !saving)
            }
            if (surge) OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.horizon_target_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !saving)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onClose, enabled = !saving, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = { onSave(title.trim(), tag.trim(), kind, mode, minutes.toIntOrNull(), surge) },
                    enabled = !saving && title.isNotBlank() && (!surge || (minutes.toIntOrNull() ?: 0) > 0), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

private fun dateLabel(time: Long): String = SimpleDateFormat(
    DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM d yyyy hmma"), Locale.getDefault()
).format(Date(time))

internal fun flowModeLabel(mode: FlowMode): Int = when (mode) {
    FlowMode.FLOW -> R.string.flow_card_type_flow
    FlowMode.SOFT -> R.string.flow_card_type_soft
    FlowMode.POWER -> R.string.horizon_power_flow
}

@Composable
private fun flowCardColors(mode: FlowMode): CardColors {
    val colors = MaterialTheme.colorScheme
    return CardDefaults.cardColors(
        containerColor = when (mode) {
            FlowMode.FLOW -> colors.surface
            FlowMode.SOFT -> colors.secondary
            FlowMode.POWER -> colors.tertiary
        },
        contentColor = when (mode) {
            FlowMode.FLOW -> colors.onSurface
            FlowMode.SOFT -> colors.onSecondary
            FlowMode.POWER -> colors.onTertiary
        }
    )
}

@Composable
private fun flowCardAccent(mode: FlowMode) =
    if (mode == FlowMode.FLOW) MaterialTheme.colorScheme.primary else LocalContentColor.current
