@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.paths

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.state.paths.PathsPrimaryTab
import com.kingkharnivore.skillz.model.state.paths.PathsTimeLens
import com.kingkharnivore.skillz.model.state.paths.PathsUiState
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.viewmodel.PathsViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@Composable
fun PathsScreen(
    viewModel: PathsViewModel,
    onPlanFlowClick: () -> Unit = {},
    onPlanArcClick: () -> Unit = {},
    onOpenFlowPlan: (title: String, tagName: String?, isSoftMode: Boolean) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    var showPlanFlowSheet by rememberSaveable { mutableStateOf(false) }
    var showDreams by rememberSaveable { mutableStateOf(false) }

    PathsScreenContent(
        uiState = uiState,
        showDreams = showDreams,
        onToggleDreams = { showDreams = !showDreams },
        onPrimaryTabSelected = viewModel::onPrimaryTabSelected,
        onTimeLensSelected = viewModel::onTimeLensSelected,
        onPlanFlowClick = {
            viewModel.clearError()
            showPlanFlowSheet = true
            onPlanFlowClick()
        },
        onPlanArcClick = onPlanArcClick,
        onOpenFlowPlan = { plan ->
            viewModel.onFlowPlanLaunched(plan.id)
            onOpenFlowPlan(
                plan.title,
                plan.tagName.takeIf { it.isNotBlank() },
                plan.isSoftMode
            )
        },
        onPinToggle = { id, pinned -> viewModel.setFlowPlanPinned(id, pinned) },
        onDream = viewModel::moveFlowPlanToDreams,
        onRestoreFromDreams = viewModel::restoreFlowPlanFromDreams,
        onDelete = viewModel::deleteFlowPlan
    )

    if (showPlanFlowSheet) {
        PlanFlowSheet(
            tags = uiState.tags,
            isSaving = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onDismiss = { showPlanFlowSheet = false },
            onSave = { title, tagName, isSoftMode ->
                viewModel.createFlowPlan(
                    title = title,
                    tagName = tagName,
                    isSoftMode = isSoftMode,
                    onSaved = { showPlanFlowSheet = false }
                )
            }
        )
    }
}

@Composable
private fun PathsScreenContent(
    uiState: PathsUiState,
    showDreams: Boolean,
    onToggleDreams: () -> Unit,
    onPrimaryTabSelected: (PathsPrimaryTab) -> Unit,
    onTimeLensSelected: (PathsTimeLens) -> Unit,
    onPlanFlowClick: () -> Unit,
    onPlanArcClick: () -> Unit,
    onOpenFlowPlan: (FlowPlanListItemUiModel) -> Unit,
    onPinToggle: (Long, Boolean) -> Unit,
    onDream: (Long) -> Unit,
    onRestoreFromDreams: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PathsHeader()
            }

            item {
                PrimaryPathsTabs(
                    selected = uiState.selectedPrimaryTab,
                    onSelected = onPrimaryTabSelected
                )
            }

            item {
                TimeLensTabs(
                    selected = uiState.selectedTimeLens,
                    onSelected = onTimeLensSelected
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                uiState.errorMessage != null &&
                        uiState.flowPlans.isEmpty() &&
                        uiState.dreamFlowPlans.isEmpty() -> {
                    item {
                        ErrorPathsState(message = uiState.errorMessage)
                    }
                }

                uiState.selectedPrimaryTab == PathsPrimaryTab.FLOWS -> {
                    if (uiState.flowPlans.isEmpty() && uiState.dreamFlowPlans.isEmpty()) {
                        item {
                            EmptyPathsState(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                title = "No planned flows yet",
                                body = "Save activities so they’re always ready to begin.",
                                cta = "Plan Flow",
                                onClick = onPlanFlowClick
                            )
                        }
                    } else {
                        if (uiState.flowPlans.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Planned Flows",
                                    subtitle = "Ready to shape into action"
                                )
                            }

                            item {
                                Button(
                                    onClick = onPlanFlowClick,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text("Plan Flow")
                                }
                            }

                            items(
                                items = uiState.flowPlans,
                                key = { it.id }
                            ) { plan ->
                                FlowPlanCard(
                                    plan = plan,
                                    onClick = { onOpenFlowPlan(plan) },
                                    onPinToggle = { pinned -> onPinToggle(plan.id, pinned) },
                                    onDream = { onDream(plan.id) },
                                    onDelete = { onDelete(plan.id) }
                                )
                            }
                        }

                        if (uiState.dreamFlowPlans.isNotEmpty()) {
                            item {
                                DreamsSectionHeader(
                                    count = uiState.dreamFlowPlans.size,
                                    expanded = showDreams,
                                    onToggle = onToggleDreams
                                )
                            }

                            if (showDreams) {
                                items(
                                    items = uiState.dreamFlowPlans,
                                    key = { "dream_${it.id}" }
                                ) { plan ->
                                    DreamFlowPlanCard(
                                        plan = plan,
                                        onRestore = { onRestoreFromDreams(plan.id) },
                                        onDelete = { onDelete(plan.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.selectedPrimaryTab == PathsPrimaryTab.ARCS -> {
                    item {
                        EmptyPathsState(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Timeline,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            title = "No planned arcs yet",
                            body = "Build a guided sequence of planned flows so the next step is always ready.",
                            cta = "Plan Arc",
                            onClick = onPlanArcClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanFlowSheet(
    tags: List<TagUiModel>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, tagName: String, isSoftMode: Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var tagName by rememberSaveable { mutableStateOf("") }
    var isSoftMode by rememberSaveable { mutableStateOf(false) }
    val chipListState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Plan Flow",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Save an activity so it’s always ready to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                placeholder = { Text("Deep Work, Guitar Practice, Journal…") },
                singleLine = true
            )

            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Journey Tag") },
                placeholder = { Text("Writing, Music, Fitness…") },
                singleLine = true
            )

            if (tags.isNotEmpty()) {
                Text(
                    text = "Recent Tags",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = chipListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.take(3).forEach { tag ->
                                FilterChip(
                                    selected = tagName == tag.name,
                                    onClick = { tagName = tag.name },
                                    label = { Text(tag.name) }
                                )
                            }
                        }
                    }

                    if (tags.size > 3) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.drop(3).take(3).forEach { tag ->
                                    FilterChip(
                                        selected = tagName == tag.name,
                                        onClick = { tagName = tag.name },
                                        label = { Text(tag.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                            text = "Soft Flow",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Save this as a gentler, low-pressure activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                    }
                    Switch(
                        checked = isSoftMode,
                        onCheckedChange = { isSoftMode = it }
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                ErrorPathsState(message = errorMessage)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onSave(title, tagName, isSoftMode) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}

@Composable
private fun PathsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Paths",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Prepare what you want ready to live next.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun DreamsSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudQueue,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = "Dreams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count ${if (count == 1) "flow" else "flows"} for someday",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                )
            }

            Text(
                text = if (expanded) "Hide" else "Show",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FlowPlanCard(
    plan: FlowPlanListItemUiModel,
    onClick: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onDream: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (plan.isSoftMode) {
                        Icons.Outlined.Spa
                    } else {
                        Icons.Outlined.AutoAwesome
                    },
                    contentDescription = null,
                    tint = if (plan.isSoftMode) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (plan.tagName.isNotBlank()) {
                        Text(
                            text = plan.tagName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }

                if (plan.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More actions"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (plan.pinned) "Unpin" else "Pin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onPinToggle(!plan.pinned)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Move to Dreams") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CloudQueue,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDream()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = if (plan.launchCount > 0) {
                    "Launched ${plan.launchCount} ${if (plan.launchCount == 1) "time" else "times"}"
                } else {
                    "Not launched yet"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete planned flow?") },
            text = { Text("This will permanently remove this planned flow.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DreamFlowPlanCard(
    plan: FlowPlanListItemUiModel,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    imageVector = if (plan.isSoftMode) {
                        Icons.Outlined.Spa
                    } else {
                        Icons.Outlined.AutoAwesome
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (plan.tagName.isNotBlank()) {
                        Text(
                            text = plan.tagName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More actions"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bring Back") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Unarchive,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRestore()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = "Saved in Dreams",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete dream flow?") },
            text = { Text("This will permanently remove this flow from Dreams.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrimaryPathsTabs(
    selected: PathsPrimaryTab,
    onSelected: (PathsPrimaryTab) -> Unit
) {
    SegmentedSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SegmentedChoice(
                modifier = Modifier.weight(1f),
                text = "Flows",
                selected = selected == PathsPrimaryTab.FLOWS,
                onClick = { onSelected(PathsPrimaryTab.FLOWS) }
            )
            SegmentedChoice(
                modifier = Modifier.weight(1f),
                text = "Arcs",
                selected = selected == PathsPrimaryTab.ARCS,
                onClick = { onSelected(PathsPrimaryTab.ARCS) }
            )
        }
    }
}

@Composable
private fun TimeLensTabs(
    selected: PathsTimeLens,
    onSelected: (PathsTimeLens) -> Unit
) {
    SegmentedSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SegmentedChoice(
                modifier = Modifier.weight(1f),
                text = "Day",
                selected = selected == PathsTimeLens.DAY,
                onClick = { onSelected(PathsTimeLens.DAY) }
            )
            SegmentedChoice(
                modifier = Modifier.weight(1f),
                text = "Week",
                selected = selected == PathsTimeLens.WEEK,
                onClick = { onSelected(PathsTimeLens.WEEK) }
            )
            SegmentedChoice(
                modifier = Modifier.weight(1f),
                text = "Month",
                selected = selected == PathsTimeLens.MONTH,
                onClick = { onSelected(PathsTimeLens.MONTH) }
            )
        }
    }
}

@Composable
private fun SegmentedSurface(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        content()
    }
}

@Composable
private fun SegmentedChoice(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyPathsState(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    cta: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(cta)
            }
        }
    }
}

@Composable
private fun ErrorPathsState(
    message: String?
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message ?: "Something went wrong",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}