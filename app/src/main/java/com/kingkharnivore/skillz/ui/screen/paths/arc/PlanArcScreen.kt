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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RouteStudioHeader(
                currentStep = uiState.currentStep + 1,
                totalSteps = uiState.totalSteps
            )

            when (uiState.currentStep) {
                0 -> {
                    RouteStudioStageTitle(
                        title = "Name your arc",
                        subtitle = "Give this route an identity before you shape its steps."
                    )

                    ArcIdentityCard(
                        title = uiState.title,
                        errorMessage = uiState.errorMessage,
                        onTitleChange = viewModel::onTitleChange
                    )

                    RouteStudioPreviewCard(
                        text = "Next, you’ll choose the planned flows that belong in this route."
                    )
                }

                1 -> {
                    RouteStudioStageTitle(
                        title = "Choose your flows",
                        subtitle = "Pull in the planned flows that belong in this route."
                    )

                    FlowPickerSection(
                        availableTags = uiState.availableTags,
                        selectedTagId = uiState.selectedTagId,
                        availableFlows = uiState.availableFlows,
                        selectedFlowIdsInOrder = uiState.selectedFlowIdsInOrder,
                        errorMessage = uiState.errorMessage,
                        onTagSelected = viewModel::onTagFilterSelected,
                        onFlowToggled = viewModel::onFlowToggled
                    )
                }

                2 -> {
                    RouteStudioStageTitle(
                        title = "Shape the route",
                        subtitle = "Arrange the sequence your arc will move through."
                    )

                    RouteShapeSection(
                        selectedFlowIdsInOrder = uiState.selectedFlowIdsInOrder,
                        availableFlows = uiState.availableFlows,
                        errorMessage = uiState.errorMessage,
                        onMoveUp = viewModel::moveSelectedFlowUp,
                        onMoveDown = viewModel::moveSelectedFlowDown,
                        onRemove = viewModel::removeSelectedFlow
                    )
                }

                3 -> {
                    RouteStudioStageTitle(
                        title = "Set timing intent",
                        subtitle = "Refine the length and pressure of each step."
                    )

                    TimingIntentSection(
                        uiState = uiState,
                        onTargetMinutesChanged = viewModel::onStepTargetMinutesChanged,
                        onLaunchWithSurgeChanged = viewModel::onStepLaunchWithSurgeChanged
                    )
                }

                else -> {
                    RouteStudioStageTitle(
                        title = "Review your arc",
                        subtitle = "See the whole route before you commit it."
                    )

                    ReviewArcSection(
                        uiState = uiState,
                        onRecurrenceTypeSelected = viewModel::onRecurrenceTypeSelected,
                        onCustomDayToggled = viewModel::onCustomDayToggled
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteStudioHeader(
    currentStep: Int,
    totalSteps: Int
) {
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
                        text = "Route Studio",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "$currentStep of $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
        }

        LinearProgressIndicator(
            progress = { currentStep / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth()
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
                text = "What is this route called?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Examples: Morning Routine, Writing Chain, Evening Calm Down",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Arc Title") },
                placeholder = { Text("Morning Routine") },
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
private fun FlowPickerSection(
    availableTags: List<TagUiModel>,
    selectedTagId: Long?,
    availableFlows: List<PlanArcFlowPickerItemUiModel>,
    selectedFlowIdsInOrder: List<Long>,
    errorMessage: String?,
    onTagSelected: (Long?) -> Unit,
    onFlowToggled: (Long) -> Unit
) {
    val filteredFlows = availableFlows.filter { flow ->
        selectedTagId == null || flow.tagId == selectedTagId
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (availableTags.isNotEmpty()) {
            TagFilterRow(
                tags = availableTags,
                selectedTagId = selectedTagId,
                onTagSelected = onTagSelected
            )
        }

        SelectionSummaryCard(
            label = if (selectedFlowIdsInOrder.isEmpty()) {
                "Choose the flows that belong in this route."
            } else {
                "${selectedFlowIdsInOrder.size} ${if (selectedFlowIdsInOrder.size == 1) "flow" else "flows"} selected"
            }
        )

        if (!errorMessage.isNullOrBlank()) {
            ErrorInlineCard(message = errorMessage)
        }

        if (filteredFlows.isEmpty()) {
            EmptyFlowPickerCard()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFlows, key = { it.id }) { flow ->
                    FlowPickerCard(
                        flow = flow,
                        selected = flow.id in selectedFlowIdsInOrder,
                        selectionOrder = selectedFlowIdsInOrder.indexOf(flow.id)
                            .takeIf { it >= 0 }
                            ?.plus(1),
                        onClick = { onFlowToggled(flow.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteShapeSection(
    selectedFlowIdsInOrder: List<Long>,
    availableFlows: List<PlanArcFlowPickerItemUiModel>,
    errorMessage: String?,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onRemove: (Long) -> Unit
) {
    val flowById = availableFlows.associateBy { it.id }
    val selectedFlows = selectedFlowIdsInOrder.mapNotNull { flowById[it] }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SelectionSummaryCard(
            label = if (selectedFlows.isEmpty()) {
                "No flows selected yet."
            } else {
                "${selectedFlows.size} ${if (selectedFlows.size == 1) "step" else "steps"} in this route"
            }
        )

        if (!errorMessage.isNullOrBlank()) {
            ErrorInlineCard(message = errorMessage)
        }

        if (selectedFlows.isEmpty()) {
            EmptyRouteShapeCard()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = selectedFlows,
                    key = { it.id }
                ) { flow ->
                    val index = selectedFlowIdsInOrder.indexOf(flow.id)
                    RouteStepCard(
                        stepNumber = index + 1,
                        flow = flow,
                        canMoveUp = index > 0,
                        canMoveDown = index < selectedFlowIdsInOrder.lastIndex,
                        onMoveUp = { onMoveUp(flow.id) },
                        onMoveDown = { onMoveDown(flow.id) },
                        onRemove = { onRemove(flow.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimingIntentSection(
    uiState: PlanArcUiState,
    onTargetMinutesChanged: (Long, String) -> Unit,
    onLaunchWithSurgeChanged: (Long, Boolean) -> Unit
) {
    val flowById = uiState.availableFlows.associateBy { it.id }
    val selectedFlows = uiState.selectedFlowIdsInOrder.mapNotNull { flowById[it] }

    val timedMinutes = selectedFlows.mapNotNull { flow ->
        uiState.targetMinutesTextByFlowId[flow.id]
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    }

    val untimedCount = selectedFlows.count { flow ->
        uiState.targetMinutesTextByFlowId[flow.id].orEmpty().trim().toIntOrNull()?.let { it > 0 } != true
    }

    val totalMinutes = timedMinutes.sum()
    val surgeCount = selectedFlows.count { flow ->
        uiState.launchWithSurgeByFlowId[flow.id] == true
    }
    val softCount = selectedFlows.count { it.isSoftMode }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TimingSummaryCard(
            totalMinutes = totalMinutes,
            untimedCount = untimedCount,
            surgeCount = surgeCount,
            softCount = softCount
        )

        if (!uiState.errorMessage.isNullOrBlank()) {
            ErrorInlineCard(message = uiState.errorMessage)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(selectedFlows, key = { it.id }) { flow ->
                TimingIntentCard(
                    flow = flow,
                    targetMinutesText = uiState.targetMinutesTextByFlowId[flow.id].orEmpty(),
                    launchWithSurge = uiState.launchWithSurgeByFlowId[flow.id] == true,
                    onTargetMinutesChanged = { onTargetMinutesChanged(flow.id, it) },
                    onLaunchWithSurgeChanged = { onLaunchWithSurgeChanged(flow.id, it) }
                )
            }
        }
    }
}

@Composable
private fun ReviewArcSection(
    uiState: PlanArcUiState,
    onRecurrenceTypeSelected: (String) -> Unit,
    onCustomDayToggled: (Int) -> Unit
) {
    val flowById = uiState.availableFlows.associateBy { it.id }
    val selectedFlows = uiState.selectedFlowIdsInOrder.mapNotNull { flowById[it] }

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
    val surgeCount = selectedFlows.count { flow ->
        uiState.launchWithSurgeByFlowId[flow.id] == true
    }
    val softCount = selectedFlows.count { it.isSoftMode }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ReviewSummaryCard(
            title = uiState.title.trim(),
            stepCount = selectedFlows.size,
            totalMinutes = totalMinutes,
            untimedCount = untimedCount,
            surgeCount = surgeCount,
            softCount = softCount
        )

        ReuseAndRepeatCard(
            recurrenceType = uiState.recurrenceType,
            recurrenceDays = uiState.recurrenceDays,
            onRecurrenceTypeSelected = onRecurrenceTypeSelected,
            onCustomDayToggled = onCustomDayToggled
        )

        ReusePlaceholderCard()

        if (!uiState.errorMessage.isNullOrBlank()) {
            ErrorInlineCard(message = uiState.errorMessage)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

@Composable
private fun ReuseAndRepeatCard(
    recurrenceType: String,
    recurrenceDays: Set<Int>,
    onRecurrenceTypeSelected: (String) -> Unit,
    onCustomDayToggled: (Int) -> Unit
) {
    val options = listOf(
        "one_time" to "One time",
        "daily" to "Daily",
        "weekdays" to "Weekdays",
        "weekly" to "Weekly",
        "custom" to "Custom"
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
                text = "Repeat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Set the rhythm for this route.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { (value, label) ->
                            FilterChip(
                                selected = recurrenceType == value,
                                onClick = { onRecurrenceTypeSelected(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            if (recurrenceType == "custom") {
                HorizontalDivider()

                Text(
                    text = "Choose days",
                    style = MaterialTheme.typography.labelLarge
                )

                val days = listOf(
                    1 to "Mon",
                    2 to "Tue",
                    3 to "Wed",
                    4 to "Thu",
                    5 to "Fri",
                    6 to "Sat",
                    7 to "Sun"
                )

                days.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { (value, label) ->
                            FilterChip(
                                selected = value in recurrenceDays,
                                onClick = { onCustomDayToggled(value) },
                                label = { Text(label) }
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
    LazyColumn(
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTagId == null,
                    onClick = { onTagSelected(null) },
                    label = { Text("All") }
                )
                tags.take(3).forEach { tag ->
                    FilterChip(
                        selected = selectedTagId == tag.id,
                        onClick = { onTagSelected(tag.id) },
                        label = { Text(tag.name) }
                    )
                }
            }
        }

        if (tags.size > 3) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.drop(3).take(3).forEach { tag ->
                        FilterChip(
                            selected = selectedTagId == tag.id,
                            onClick = { onTagSelected(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionSummaryCard(
    label: String
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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

                    val meta = buildList {
                        if (flow.tagName.isNotBlank()) add(flow.tagName)
                        flow.targetMinutes?.let { add("${it}m") }
                        if (flow.launchWithSurge) add("Surge")
                    }.joinToString(" • ")

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
                        contentDescription = null,
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
                                text = selectionOrder?.toString() ?: "✓",
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
                    MiniBadge(text = "${it} min")
                }
                if (flow.launchWithSurge) {
                    MiniBadge(
                        text = "Surge",
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
                    MiniBadge(text = "Soft")
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

                    val meta = buildList {
                        if (flow.tagName.isNotBlank()) add(flow.tagName)
                        flow.targetMinutes?.let { add("${it}m") }
                        if (flow.launchWithSurge) add("Surge")
                    }.joinToString(" • ")

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
                    MiniBadge(text = "${it} min")
                }
                if (flow.launchWithSurge) {
                    MiniBadge(
                        text = "Surge",
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
                    MiniBadge(text = "Soft")
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null
                    )
                    Text("Up")
                }

                TextButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Text("Down")
                }

                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null
                    )
                    Text("Remove")
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
                text = if (untimedCount > 0) {
                    "Approx total: ${totalMinutes}+ min"
                } else {
                    "Approx total: $totalMinutes min"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val detail = buildList {
                if (untimedCount > 0) add("$untimedCount untimed")
                if (surgeCount > 0) add("$surgeCount Surge")
                if (softCount > 0) add("$softCount Soft")
            }.joinToString(" • ")

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
                label = { Text("Target Minutes") },
                placeholder = { Text("25") },
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
                            text = "Launch with Surge",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = when {
                                flow.isSoftMode -> "Surge is unavailable for Soft steps."
                                !validTarget -> "Set target minutes to enable Surge."
                                else -> "Open this step with Surge armed by default."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                    }
                    Switch(
                        checked = launchWithSurge,
                        onCheckedChange = onLaunchWithSurgeChanged,
                        enabled = surgeEnabled
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
                text = title.ifBlank { "Untitled Arc" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            val summaryLine = buildList {
                add("$stepCount ${if (stepCount == 1) "step" else "steps"}")
                add(
                    if (untimedCount > 0) {
                        "${totalMinutes}+ min"
                    } else {
                        "$totalMinutes min"
                    }
                )
            }.joinToString(" • ")

            Text(
                text = summaryLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            val detail = buildList {
                if (untimedCount > 0) add("$untimedCount untimed")
                if (surgeCount > 0) add("$surgeCount Surge")
                if (softCount > 0) add("$softCount Soft")
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
                text = "Reuse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Recurring rhythm, Studio placement, and reuse options will live here next.",
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
                    MiniBadge(text = "${it} min")
                }
                if (launchWithSurge) {
                    MiniBadge(
                        text = "Surge",
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
                    MiniBadge(text = "Soft")
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
                text = "What comes next",
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
            text = "No planned flows match this filter yet.",
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
            text = "This route has no steps yet. Go back and choose some flows first.",
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
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
    val primaryLabel = when (uiState.currentStep) {
        0 -> "Continue"
        1 -> "Continue"
        2 -> "Continue"
        3 -> "Continue"
        else -> if (uiState.isEditing) "Update Arc" else "Save Arc"
    }

    val primaryEnabled = when {
        uiState.isSaving -> false
        uiState.currentStep == 0 -> uiState.title.isNotBlank()
        else -> uiState.selectedFlowIdsInOrder.isNotEmpty()
    }

    Surface(shadowElevation = 4.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
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
                    Text(if (uiState.currentStep == 0) "Back" else "Previous")
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    enabled = primaryEnabled,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (uiState.isSaving) "Saving..." else primaryLabel)
                }
            }
        }
    }
}