@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.paths.arc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.paths.PlanArcUiState
import com.kingkharnivore.skillz.model.ui.PlanArcFlowPickerItemUiModel
import com.kingkharnivore.skillz.viewmodel.PlanArcViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@Composable
fun PlanArcScreen(
    viewModel: PlanArcViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateFlowSheet by rememberSaveable { mutableStateOf(false) }

    val flowById = uiState.availableFlows.associateBy { it.id }
    val selectedFlows = uiState.selectedFlowIdsInOrder.mapNotNull { flowById[it] }
    val filteredFlows = uiState.availableFlows.filter { flow ->
        uiState.selectedTagId == null || flow.tagId == uiState.selectedTagId
    }
    val timedMinutes = selectedFlows.mapNotNull { flow ->
        uiState.targetMinutesTextByFlowId[flow.id]
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    }
    val totalMinutes = timedMinutes.sum()
    val untimedCount = selectedFlows.count { flow ->
        uiState.targetMinutesTextByFlowId[flow.id].orEmpty().trim().toIntOrNull()?.let { it > 0 } != true
    }
    val surgeCount = selectedFlows.count { flow -> uiState.launchWithSurgeByFlowId[flow.id] == true }
    val softCount = selectedFlows.count { it.isSoftMode }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        bottomBar = {
            PlanArcFooter(
                uiState = uiState,
                onBack = {
                    if (uiState.currentStep == 0) onBack() else viewModel.goBack()
                },
                onContinue = {
                    when (uiState.currentStep) {
                        0 -> viewModel.continueFromIdentity()
                        1 -> viewModel.continueFromPicker()
                        2 -> viewModel.continueFromShape()
                        3 -> viewModel.continueFromTiming()
                        else -> viewModel.saveArcWithSelectedFlows(onSaved = onDone)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RouteStudioHeader(
                    currentStep = uiState.currentStep + 1,
                    totalSteps = uiState.totalSteps
                )
            }

            when (uiState.currentStep) {
                0 -> {
                    item {
                        RouteStudioStageTitle(
                            title = stringResource(R.string.plan_arc_stage_name_title),
                            subtitle = stringResource(R.string.plan_arc_stage_name_subtitle)
                        )
                    }
                    item {
                        ArcIdentityCard(
                            title = uiState.title,
                            errorMessage = uiState.errorMessage,
                            onTitleChange = viewModel::onTitleChange
                        )
                    }
                    item {
                        RouteStudioPreviewCard(
                            text = stringResource(R.string.plan_arc_preview_body)
                        )
                    }
                }

                1 -> {
                    item {
                        RouteStudioStageTitle(
                            title = stringResource(R.string.plan_arc_stage_choose_title),
                            subtitle = stringResource(R.string.plan_arc_stage_choose_subtitle)
                        )
                    }
                    if (uiState.availableTags.isNotEmpty()) {
                        item {
                            TagFilterRow(
                                tags = uiState.availableTags,
                                selectedTagId = uiState.selectedTagId,
                                onTagSelected = viewModel::onTagFilterSelected
                            )
                        }
                    }
                    item {
                        SelectionSummaryCard(
                            label = selectionSummaryLabel(uiState.selectedFlowIdsInOrder.size)
                        )
                    }
                    item {
                        CompactSelectedSequencePreview(
                            selectedFlowIdsInOrder = uiState.selectedFlowIdsInOrder,
                            availableFlows = uiState.availableFlows,
                            onRemove = viewModel::onFlowToggled
                        )
                    }
                    item {
                        val createFlowText = stringResource(R.string.plan_arc_create_flow_cta)
                        Button(
                            onClick = {
                                viewModel.clearError()
                                showCreateFlowSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    role = Role.Button
                                    contentDescription = createFlowText
                                },
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(createFlowText)
                        }
                    }
                    uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                        item { ErrorInlineCard(message = errorMessage) }
                    }
                    if (filteredFlows.isEmpty()) {
                        item { EmptyFlowPickerCard() }
                    } else {
                        items(filteredFlows, key = { it.id }) { flow ->
                            FlowPickerCard(
                                flow = flow,
                                selected = flow.id in uiState.selectedFlowIdsInOrder,
                                selectionOrder = uiState.selectedFlowIdsInOrder.indexOf(flow.id)
                                    .takeIf { it >= 0 }
                                    ?.plus(1),
                                onClick = { viewModel.onFlowToggled(flow.id) }
                            )
                        }
                    }
                }

                2 -> {
                    item {
                        RouteStudioStageTitle(
                            title = stringResource(R.string.plan_arc_stage_shape_title),
                            subtitle = stringResource(R.string.plan_arc_stage_shape_subtitle)
                        )
                    }
                    item {
                        SelectionSummaryCard(
                            label = if (selectedFlows.isEmpty()) {
                                stringResource(R.string.plan_arc_shape_empty_summary)
                            } else {
                                pluralStringResource(
                                    R.plurals.plan_arc_shape_summary_steps,
                                    selectedFlows.size,
                                    selectedFlows.size
                                )
                            }
                        )
                    }
                    uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                        item { ErrorInlineCard(message = errorMessage) }
                    }
                    if (selectedFlows.isEmpty()) {
                        item { EmptyRouteShapeCard() }
                    } else {
                        items(selectedFlows, key = { it.id }) { flow ->
                            val index = uiState.selectedFlowIdsInOrder.indexOf(flow.id)
                            RouteStepCard(
                                stepNumber = index + 1,
                                flow = flow,
                                canMoveUp = index > 0,
                                canMoveDown = index < uiState.selectedFlowIdsInOrder.lastIndex,
                                onMoveUp = { viewModel.moveSelectedFlowUp(flow.id) },
                                onMoveDown = { viewModel.moveSelectedFlowDown(flow.id) },
                                onRemove = { viewModel.removeSelectedFlow(flow.id) }
                            )
                        }
                    }
                }

                3 -> {
                    item {
                        RouteStudioStageTitle(
                            title = stringResource(R.string.plan_arc_stage_timing_title),
                            subtitle = stringResource(R.string.plan_arc_stage_timing_subtitle)
                        )
                    }
                    item {
                        TimingSummaryCard(
                            totalMinutes = totalMinutes,
                            untimedCount = untimedCount,
                            surgeCount = surgeCount,
                            softCount = softCount
                        )
                    }
                    uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                        item { ErrorInlineCard(message = errorMessage) }
                    }
                    items(selectedFlows, key = { it.id }) { flow ->
                        TimingIntentCard(
                            flow = flow,
                            targetMinutesText = uiState.targetMinutesTextByFlowId[flow.id].orEmpty(),
                            launchWithSurge = uiState.launchWithSurgeByFlowId[flow.id] == true,
                            onTargetMinutesChanged = { viewModel.onStepTargetMinutesChanged(flow.id, it) },
                            onLaunchWithSurgeChanged = { viewModel.onStepLaunchWithSurgeChanged(flow.id, it) }
                        )
                    }
                }

                else -> {
                    item {
                        RouteStudioStageTitle(
                            title = stringResource(R.string.plan_arc_stage_review_title),
                            subtitle = stringResource(R.string.plan_arc_stage_review_subtitle)
                        )
                    }
                    item {
                        ReviewSummaryCard(
                            title = uiState.title.trim(),
                            stepCount = selectedFlows.size,
                            totalMinutes = totalMinutes,
                            untimedCount = untimedCount,
                            surgeCount = surgeCount,
                            softCount = softCount
                        )
                    }
                    item {
                        ReuseAndRepeatCard(
                            recurrenceType = uiState.recurrenceType,
                            recurrenceDays = uiState.recurrenceDays,
                            onRecurrenceTypeSelected = viewModel::onRecurrenceTypeSelected,
                            onCustomDayToggled = viewModel::onCustomDayToggled
                        )
                    }
                    uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                        item { ErrorInlineCard(message = errorMessage) }
                    }
                    items(selectedFlows, key = { it.id }) { flow ->
                        val targetText = uiState.targetMinutesTextByFlowId[flow.id].orEmpty().trim()
                        val targetMinutes = targetText.toIntOrNull()?.takeIf { it > 0 }
                        val launchWithSurge = uiState.launchWithSurgeByFlowId[flow.id] == true
                        ReviewStepCard(
                            stepNumber = uiState.selectedFlowIdsInOrder.indexOf(flow.id) + 1,
                            flow = flow,
                            targetMinutes = targetMinutes,
                            launchWithSurge = launchWithSurge
                        )
                    }
                }
            }
        }
    }

    if (showCreateFlowSheet) {
        CreateFlowStepSheet(
            isSaving = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onDismiss = {
                viewModel.clearError()
                showCreateFlowSheet = false
            },
            onSave = { title, tagName, targetMinutesText, launchWithSurge ->
                viewModel.createFlowAndSelect(
                    title = title,
                    tagName = tagName,
                    targetMinutesText = targetMinutesText,
                    launchWithSurge = launchWithSurge,
                    onSaved = { showCreateFlowSheet = false }
                )
            }
        )
    }
}

@Composable
private fun CreateFlowStepSheet(
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        tagName: String,
        targetMinutesText: String,
        launchWithSurge: Boolean
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var tagName by rememberSaveable { mutableStateOf("") }
    var targetMinutesText by rememberSaveable { mutableStateOf("") }
    var launchWithSurge by rememberSaveable { mutableStateOf(false) }

    val cleanedTargetMinutes = targetMinutesText.filter(Char::isDigit).take(3)
    if (cleanedTargetMinutes != targetMinutesText) {
        targetMinutesText = cleanedTargetMinutes
    }

    val validTarget = targetMinutesText.trim().toIntOrNull()?.let { it > 0 } == true
    val surgeEnabled = validTarget

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.plan_arc_create_flow_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_arc_create_flow_field_title)) },
                    placeholder = { Text(stringResource(R.string.plan_arc_create_flow_placeholder)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_arc_create_flow_tag_label)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = targetMinutesText,
                    onValueChange = { targetMinutesText = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.plan_arc_target_minutes_label)) },
                    placeholder = { Text(stringResource(R.string.plan_arc_target_minutes_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            item {
                FlowCreationToggleRow(
                    title = stringResource(R.string.paths_launch_with_surge_title),
                    body = if (!validTarget) {
                        stringResource(R.string.paths_launch_with_surge_disabled_no_target)
                    } else {
                        stringResource(R.string.paths_launch_with_surge_enabled_body)
                    },
                    checked = launchWithSurge && surgeEnabled,
                    onCheckedChange = { launchWithSurge = it && surgeEnabled },
                    enabled = surgeEnabled
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                item { ErrorInlineCard(message = errorMessage) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text(stringResource(R.string.plan_arc_create_flow_cancel))
                    }

                    Button(
                        onClick = {
                            onSave(
                                title.trim(),
                                tagName.trim(),
                                targetMinutesText.trim(),
                                launchWithSurge && surgeEnabled
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving && title.trim().isNotEmpty(),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            if (isSaving) {
                                stringResource(R.string.plan_arc_create_flow_adding)
                            } else {
                                stringResource(R.string.plan_arc_create_flow_add_to_arc)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowCreationToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}


@Composable
private fun RouteStudioHeader(
    currentStep: Int,
    totalSteps: Int
) {
    val routeStudioText = stringResource(R.string.plan_arc_route_studio)
    val stepCounterText = stringResource(R.string.plan_arc_step_counter, currentStep, totalSteps)
    val progressA11y = stringResource(R.string.plan_arc_progress_a11y, currentStep, totalSteps)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = routeStudioText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = stepCounterText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
        }

        LinearProgressIndicator(
            progress = { currentStep / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = progressA11y
                }
        )
    }
}

@Composable
private fun RouteStudioStageTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun ArcIdentityCard(
    title: String,
    errorMessage: String?,
    onTitleChange: (String) -> Unit
) {
    val cardTitle = stringResource(R.string.plan_arc_identity_title)
    val examples = stringResource(R.string.plan_arc_identity_examples)
    val titleLabel = stringResource(R.string.plan_arc_title_label)
    val titlePlaceholder = stringResource(R.string.plan_arc_title_placeholder)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = cardTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = examples,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(titleLabel) },
                placeholder = { Text(titlePlaceholder) },
                singleLine = true,
                isError = !errorMessage.isNullOrBlank()
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun selectionSummaryLabel(selectedCount: Int): String =
    when (selectedCount) {
        0 -> stringResource(R.string.plan_arc_selection_summary_choose)
        1 -> stringResource(R.string.plan_arc_selection_summary_needs_second)
        else -> pluralStringResource(
            R.plurals.plan_arc_selection_summary_selected,
            selectedCount,
            selectedCount
        )
    }


@Composable
private fun CompactSelectedSequencePreview(
    selectedFlowIdsInOrder: List<Long>,
    availableFlows: List<PlanArcFlowPickerItemUiModel>,
    onRemove: (Long) -> Unit
) {
    val flowById = availableFlows.associateBy { it.id }
    val selectedFlows = selectedFlowIdsInOrder.mapNotNull { flowById[it] }
    if (selectedFlows.isEmpty()) return

    var expanded by rememberSaveable(selectedFlowIdsInOrder.joinToString("_")) { mutableStateOf(false) }
    val title = stringResource(R.string.plan_arc_selected_sequence_title)
    val viewSequenceText = stringResource(R.string.plan_arc_view_sequence)
    val hideSequenceText = stringResource(R.string.plan_arc_hide_sequence)
    val countText = pluralStringResource(
        R.plurals.plan_arc_selection_summary_selected,
        selectedFlows.size,
        selectedFlows.size
    )
    val expandedText = stringResource(R.string.paths_expanded)
    val collapsedText = stringResource(R.string.paths_collapsed)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$countText. ${if (expanded) hideSequenceText else viewSequenceText}"
                stateDescription = if (expanded) expandedText else collapsedText
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = countText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = if (expanded) hideSequenceText else viewSequenceText
                        stateDescription = if (expanded) expandedText else collapsedText
                    }
                ) {
                    Text(if (expanded) hideSequenceText else viewSequenceText)
                }
            }

            if (expanded) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )
                selectedFlows.forEachIndexed { index, flow ->
                    val removeFlowA11y = stringResource(
                        R.string.plan_arc_remove_flow_from_arc_a11y,
                        flow.title
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = flow.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { onRemove(flow.id) },
                            modifier = Modifier.semantics {
                                role = Role.Button
                                contentDescription = removeFlowA11y
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ReuseAndRepeatCard(
    recurrenceType: String,
    recurrenceDays: Set<Int>,
    onRecurrenceTypeSelected: (String) -> Unit,
    onCustomDayToggled: (Int) -> Unit
) {
    val repeatTitle = stringResource(R.string.plan_arc_repeat_title)
    val repeatSubtitle = stringResource(R.string.plan_arc_repeat_subtitle)
    val chooseDays = stringResource(R.string.plan_arc_repeat_choose_days)
    val selectedText = stringResource(R.string.plan_arc_selected)
    val notSelectedText = stringResource(R.string.plan_arc_not_selected)

    val options = listOf(
        "one_time" to stringResource(R.string.plan_arc_repeat_one_time),
        "daily" to stringResource(R.string.plan_arc_repeat_daily),
        "weekdays" to stringResource(R.string.plan_arc_repeat_weekdays),
        "weekly" to stringResource(R.string.plan_arc_repeat_weekly),
        "custom" to stringResource(R.string.plan_arc_repeat_custom)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = repeatTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = repeatSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { (value, label) ->
                            val chipA11y = stringResource(R.string.plan_arc_repeat_option_a11y, label)
                            FilterChip(
                                selected = recurrenceType == value,
                                onClick = { onRecurrenceTypeSelected(value) },
                                label = { Text(label) },
                                modifier = Modifier.semantics {
                                    role = Role.Button
                                    contentDescription = chipA11y
                                    stateDescription = if (recurrenceType == value) selectedText else notSelectedText
                                }
                            )
                        }
                    }
                }
            }

            if (recurrenceType == "custom") {
                HorizontalDivider()

                Text(
                    text = chooseDays,
                    style = MaterialTheme.typography.labelLarge
                )

                val days = listOf(
                    1 to stringResource(R.string.plan_arc_repeat_day_mon),
                    2 to stringResource(R.string.plan_arc_repeat_day_tue),
                    3 to stringResource(R.string.plan_arc_repeat_day_wed),
                    4 to stringResource(R.string.plan_arc_repeat_day_thu),
                    5 to stringResource(R.string.plan_arc_repeat_day_fri),
                    6 to stringResource(R.string.plan_arc_repeat_day_sat),
                    7 to stringResource(R.string.plan_arc_repeat_day_sun)
                )

                days.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { (value, label) ->
                            val chipA11y = stringResource(R.string.plan_arc_repeat_day_chip_a11y, label)
                            FilterChip(
                                selected = value in recurrenceDays,
                                onClick = { onCustomDayToggled(value) },
                                label = { Text(label) },
                                modifier = Modifier.semantics {
                                    role = Role.Button
                                    contentDescription = chipA11y
                                    stateDescription = if (value in recurrenceDays) selectedText else notSelectedText
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<TagUiModel>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit
) {
    val allText = stringResource(R.string.plan_arc_filter_all)
    val selectedText = stringResource(R.string.plan_arc_selected)
    val notSelectedText = stringResource(R.string.plan_arc_not_selected)
    val visibleTags = tags.take(6)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibleTags.chunked(3).forEachIndexed { rowIndex, rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rowIndex == 0) {
                    val allA11y = stringResource(R.string.plan_arc_filter_chip_a11y, allText)
                    FilterChip(
                        selected = selectedTagId == null,
                        onClick = { onTagSelected(null) },
                        label = { Text(allText) },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = allA11y
                            stateDescription = if (selectedTagId == null) selectedText else notSelectedText
                        }
                    )
                }

                rowTags.forEach { tag ->
                    val chipA11y = stringResource(R.string.plan_arc_filter_chip_a11y, tag.name)
                    FilterChip(
                        selected = selectedTagId == tag.id,
                        onClick = { onTagSelected(tag.id) },
                        label = { Text(tag.name) },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = chipA11y
                            stateDescription = if (selectedTagId == tag.id) selectedText else notSelectedText
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionSummaryCard(
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
        )
    }
}

@Composable
private fun FlowPickerCard(
    flow: PlanArcFlowPickerItemUiModel,
    selected: Boolean,
    selectionOrder: Int?,
    onClick: () -> Unit
) {
    val surgeText = stringResource(R.string.plan_arc_surge)
    val softText = stringResource(R.string.plan_arc_soft)
    val pinnedText = stringResource(R.string.plan_arc_pinned)

    val meta = buildList {
        if (flow.tagName.isNotBlank()) add(flow.tagName)
        flow.targetMinutes?.let { add(stringResource(R.string.plan_arc_flow_meta_minutes_compact, it)) }
        if (flow.launchWithSurge) add(surgeText)
    }.joinToString(" • ")

    val cardA11y = stringResource(
        R.string.plan_arc_flow_picker_card_a11y,
        flow.title,
        if (meta.isNotBlank()) meta else ""
    )

    val selectedOrderA11y = selectionOrder?.let {
        stringResource(R.string.plan_arc_selected_order_a11y, it)
    }
    val cardContentDescription = buildString {
        append(cardA11y)
        if (flow.pinned) append(". $pinnedText")
        if (selected && selectedOrderA11y != null) {
            append(". ")
            append(selectedOrderA11y)
        }
    }
    val cardStateDescription = if (selected) {
        selectedOrderA11y ?: stringResource(R.string.plan_arc_selected_order_fallback)
    } else {
        stringResource(R.string.plan_arc_not_selected)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = cardContentDescription
                stateDescription = cardStateDescription
            },
        shape = RoundedCornerShape(24.dp),
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (flow.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (flow.isSoftMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = flow.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }

                if (flow.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = pinnedText,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (selected) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectionOrder?.toString()
                                    ?: stringResource(R.string.plan_arc_selected_order_fallback),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                flow.targetMinutes?.let {
                    MiniBadge(text = stringResource(R.string.plan_arc_flow_meta_minutes_short, it))
                }
                if (flow.launchWithSurge) {
                    MiniBadge(
                        text = surgeText,
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                if (flow.isSoftMode) {
                    MiniBadge(text = softText)
                }
            }
        }
    }
}

@Composable
private fun RouteStepCard(
    stepNumber: Int,
    flow: PlanArcFlowPickerItemUiModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val surgeText = stringResource(R.string.plan_arc_surge)
    val softText = stringResource(R.string.plan_arc_soft)
    val meta = buildList {
        if (flow.tagName.isNotBlank()) add(flow.tagName)
        flow.targetMinutes?.let { add(stringResource(R.string.plan_arc_flow_meta_minutes_compact, it)) }
        if (flow.launchWithSurge) add(surgeText)
    }.joinToString(" • ")

    val cardA11y = stringResource(
        R.string.plan_arc_route_step_a11y,
        flow.title,
        stepNumber,
        meta
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = cardA11y
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stepNumber.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = if (flow.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (flow.isSoftMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 10.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = flow.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                flow.targetMinutes?.let {
                    MiniBadge(text = stringResource(R.string.plan_arc_flow_meta_minutes_short, it))
                }
                if (flow.launchWithSurge) {
                    MiniBadge(
                        text = surgeText,
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                if (flow.isSoftMode) {
                    MiniBadge(text = softText)
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moveUpA11y = stringResource(R.string.plan_arc_move_up_a11y)
                val moveDownA11y = stringResource(R.string.plan_arc_move_down_a11y)
                val removeA11y = stringResource(R.string.plan_arc_remove_flow_from_arc_a11y, flow.title)

                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.semantics { contentDescription = moveUpA11y }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.semantics { contentDescription = moveDownA11y }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.semantics { contentDescription = removeA11y }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun TimingSummaryCard(
    totalMinutes: Int,
    untimedCount: Int,
    surgeCount: Int,
    softCount: Int
) {
    val totalText = if (untimedCount > 0) {
        stringResource(R.string.plan_arc_timing_summary_total_flexible, totalMinutes)
    } else {
        stringResource(R.string.plan_arc_timing_summary_total_fixed, totalMinutes)
    }

    val detail = buildList {
        if (untimedCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.plan_arc_timing_summary_untimed,
                    untimedCount,
                    untimedCount
                )
            )
        }
        if (surgeCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.plan_arc_timing_summary_surge,
                    surgeCount,
                    surgeCount
                )
            )
        }
        if (softCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.plan_arc_timing_summary_soft,
                    softCount,
                    softCount
                )
            )
        }
    }.joinToString(" • ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = totalText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
        }
    }
}

@Composable
private fun TimingIntentCard(
    flow: PlanArcFlowPickerItemUiModel,
    targetMinutesText: String,
    launchWithSurge: Boolean,
    onTargetMinutesChanged: (String) -> Unit,
    onLaunchWithSurgeChanged: (Boolean) -> Unit
) {
    val targetMinutesLabel = stringResource(R.string.plan_arc_target_minutes_label)
    val targetMinutesPlaceholder = stringResource(R.string.plan_arc_target_minutes_placeholder)
    val launchWithSurgeTitle = stringResource(R.string.plan_arc_launch_with_surge_title)
    val surgeSwitchA11y = stringResource(R.string.plan_arc_launch_with_surge_switch_a11y)
    val onText = stringResource(R.string.plan_arc_switch_on)
    val offText = stringResource(R.string.plan_arc_switch_off)

    val validTarget = targetMinutesText.trim().toIntOrNull()?.let { it > 0 } == true
    val surgeEnabled = !flow.isSoftMode && validTarget

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (flow.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (flow.isSoftMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = flow.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (flow.tagName.isNotBlank()) {
                        Text(
                            text = flow.tagName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = targetMinutesText,
                onValueChange = onTargetMinutesChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(targetMinutesLabel) },
                placeholder = { Text(targetMinutesPlaceholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = launchWithSurgeTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = when {
                                flow.isSoftMode -> stringResource(R.string.plan_arc_launch_with_surge_soft_disabled)
                                !validTarget -> stringResource(R.string.plan_arc_launch_with_surge_target_needed)
                                else -> stringResource(R.string.plan_arc_launch_with_surge_enabled)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                    }
                    Switch(
                        checked = launchWithSurge,
                        onCheckedChange = onLaunchWithSurgeChanged,
                        enabled = surgeEnabled,
                        modifier = Modifier.semantics {
                            contentDescription = surgeSwitchA11y
                            stateDescription = if (launchWithSurge) onText else offText
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSummaryCard(
    title: String,
    stepCount: Int,
    totalMinutes: Int,
    untimedCount: Int,
    surgeCount: Int,
    softCount: Int
) {
    val actualTitle = title.ifBlank { stringResource(R.string.plan_arc_review_untitled) }
    val stepCountText = pluralStringResource(R.plurals.plan_arc_review_step_count, stepCount, stepCount)
    val totalText = if (untimedCount > 0) {
        stringResource(R.string.plan_arc_review_total_flexible, totalMinutes)
    } else {
        stringResource(R.string.plan_arc_review_total_fixed, totalMinutes)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = actualTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "$stepCountText • $totalText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            val detail = buildList {
                if (untimedCount > 0) {
                    add(
                        pluralStringResource(
                            R.plurals.plan_arc_timing_summary_untimed,
                            untimedCount,
                            untimedCount
                        )
                    )
                }
                if (surgeCount > 0) {
                    add(
                        pluralStringResource(
                            R.plurals.plan_arc_timing_summary_surge,
                            surgeCount,
                            surgeCount
                        )
                    )
                }
                if (softCount > 0) {
                    add(
                        pluralStringResource(
                            R.plurals.plan_arc_timing_summary_soft,
                            softCount,
                            softCount
                        )
                    )
                }
            }.joinToString(" • ")

            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                )
            }
        }
    }
}

@Composable
private fun ReusePlaceholderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.plan_arc_reuse_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.plan_arc_reuse_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun ReviewStepCard(
    stepNumber: Int,
    flow: PlanArcFlowPickerItemUiModel,
    targetMinutes: Int?,
    launchWithSurge: Boolean
) {
    val surgeText = stringResource(R.string.plan_arc_surge)
    val softText = stringResource(R.string.plan_arc_soft)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stepNumber.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = if (flow.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (flow.isSoftMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 10.dp)
                )

                Column(
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Text(
                        text = flow.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (flow.tagName.isNotBlank()) {
                        Text(
                            text = flow.tagName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targetMinutes?.let {
                    MiniBadge(text = stringResource(R.string.plan_arc_flow_meta_minutes_short, it))
                }
                if (launchWithSurge) {
                    MiniBadge(
                        text = surgeText,
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                if (flow.isSoftMode) {
                    MiniBadge(text = softText)
                }
            }
        }
    }
}

@Composable
private fun RouteStudioPreviewCard(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.plan_arc_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider()

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun EmptyFlowPickerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = stringResource(R.string.plan_arc_empty_flow_picker),
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun EmptyRouteShapeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = stringResource(R.string.plan_arc_empty_route_shape),
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun ErrorInlineCard(
    message: String
) {
    val a11y = stringResource(R.string.plan_arc_error_a11y, message)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11y
            }
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MiniBadge(
    text: String,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PlanArcFooter(
    uiState: PlanArcUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val continueText = stringResource(R.string.plan_arc_footer_continue)
    val updateArcText = stringResource(R.string.plan_arc_footer_update_arc)
    val saveArcText = stringResource(R.string.plan_arc_footer_save_arc)
    val backText = stringResource(R.string.plan_arc_footer_back)
    val previousText = stringResource(R.string.plan_arc_footer_previous)
    val savingText = stringResource(R.string.plan_arc_footer_saving)

    val primaryLabel = when (uiState.currentStep) {
        0, 1, 2, 3 -> continueText
        else -> if (uiState.isEditing) updateArcText else saveArcText
    }

    val primaryEnabled = when {
        uiState.isSaving -> false
        uiState.currentStep == 0 -> uiState.title.isNotBlank()
        uiState.currentStep == 1 || uiState.currentStep == 2 -> uiState.selectedFlowIdsInOrder.size >= 2
        else -> uiState.selectedFlowIdsInOrder.size >= 2
    }

    Surface(shadowElevation = 4.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(
                    bottom = WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                ) {
                    Text(if (uiState.currentStep == 0) backText else previousText)
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    enabled = primaryEnabled,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (uiState.isSaving) savingText else primaryLabel)
                }
            }
        }
    }
}