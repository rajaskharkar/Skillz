@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.paths

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.kingkharnivore.skillz.model.state.paths.PathsPrimaryTab
import com.kingkharnivore.skillz.model.state.paths.PathsTimeLens
import com.kingkharnivore.skillz.model.state.paths.PathsUiState
import com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.ui.screen.paths.suggested.SuggestedRoutesCatalog
import com.kingkharnivore.skillz.viewmodel.PathsViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@Composable
fun PathsScreen(
    viewModel: PathsViewModel,
    onPlanFlowClick: () -> Unit = {},
    onPlanArcClick: () -> Unit = {},
    onOpenFlowPlan: (title: String, tagName: String?, isSoftMode: Boolean) -> Unit = { _, _, _ -> },
    onOpenSuggestedRoute: (String) -> Unit = {},
    onOpenArc: (Long) -> Unit = {}
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
        onOpenSuggestedRoute = onOpenSuggestedRoute,
        onPinToggle = { id, pinned -> viewModel.setFlowPlanPinned(id, pinned) },
        onDream = viewModel::moveFlowPlanToDreams,
        onRestoreFromDreams = viewModel::restoreFlowPlanFromDreams,
        onDeleteFlow = viewModel::deleteFlowPlan,
        onAddArcToStudio = viewModel::addArcToStudio,
        onRemoveArcFromStudio = viewModel::removeArcFromStudio,
        onOpenArc = onOpenArc,
        onDeleteArc = viewModel::deleteArcPlan
    )

    if (showPlanFlowSheet) {
        PlanFlowSheet(
            tags = uiState.tags,
            isSaving = uiState.isSaving,
            errorMessage = uiState.errorMessage,
            onDismiss = { showPlanFlowSheet = false },
            onSave = { title, tagName, isSoftMode, targetMinutesText, launchWithSurge ->
                viewModel.createFlowPlan(
                    title = title,
                    tagName = tagName,
                    isSoftMode = isSoftMode,
                    targetMinutesText = targetMinutesText,
                    launchWithSurge = launchWithSurge,
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
    onOpenSuggestedRoute: (String) -> Unit,
    onPinToggle: (Long, Boolean) -> Unit,
    onDream: (Long) -> Unit,
    onRestoreFromDreams: (Long) -> Unit,
    onDeleteFlow: (Long) -> Unit,
    onAddArcToStudio: (Long) -> Unit,
    onRemoveArcFromStudio: (Long) -> Unit,
    onOpenArc: (Long) -> Unit,
    onDeleteArc: (Long) -> Unit,
) {
    val loadingDescription = stringResource(R.string.paths_loading)
    val emptyFlowsTitle = stringResource(R.string.paths_empty_flows_title)
    val emptyFlowsBody = stringResource(R.string.paths_empty_flows_body)
    val planFlowText = stringResource(R.string.paths_plan_flow)
    val plannedFlowsTitle = stringResource(R.string.paths_planned_flows_title)
    val plannedFlowsSubtitle = stringResource(R.string.paths_planned_flows_subtitle)
    val emptyArcsTitle = stringResource(R.string.paths_empty_arcs_title)
    val emptyArcsBody = stringResource(R.string.paths_empty_arcs_body)
    val planArcText = stringResource(R.string.paths_plan_arc)
    val studioTitle = stringResource(R.string.paths_studio_title)
    val studioSubtitle = stringResource(R.string.paths_studio_subtitle)
    val suggestedRoutesTitle = stringResource(R.string.paths_suggested_routes_title)
    val suggestedRoutesSubtitle = stringResource(R.string.paths_suggested_routes_subtitle)
    val yourArcsTitle = stringResource(R.string.paths_your_arcs_title)
    val yourArcsSubtitle = stringResource(R.string.paths_your_arcs_subtitle)
    val emptyMoreArcsBody = stringResource(R.string.paths_empty_more_arcs_body)

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
            item { PathsHeader() }

            item {
                PrimaryPathsTabs(
                    selected = uiState.selectedPrimaryTab,
                    onSelected = onPrimaryTabSelected
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
                            CircularProgressIndicator(
                                modifier = Modifier.semantics {
                                    contentDescription = loadingDescription
                                }
                            )
                        }
                    }
                }

                uiState.errorMessage != null &&
                        uiState.flowPlans.isEmpty() &&
                        uiState.dreamFlowPlans.isEmpty() &&
                        uiState.studioArcPlans.isEmpty() &&
                        uiState.arcPlans.isEmpty() -> {
                    item { ErrorPathsState(message = uiState.errorMessage) }
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
                                title = emptyFlowsTitle,
                                body = emptyFlowsBody,
                                cta = planFlowText,
                                onClick = onPlanFlowClick
                            )
                        }
                    } else {
                        if (uiState.flowPlans.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = plannedFlowsTitle,
                                    subtitle = plannedFlowsSubtitle
                                )
                            }

                            item {
                                Button(
                                    onClick = onPlanFlowClick,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(planFlowText)
                                }
                            }

                            items(uiState.flowPlans, key = { it.id }) { plan ->
                                FlowPlanCard(
                                    plan = plan,
                                    onClick = { onOpenFlowPlan(plan) },
                                    onPinToggle = { pinned -> onPinToggle(plan.id, pinned) },
                                    onDream = { onDream(plan.id) },
                                    onDelete = { onDeleteFlow(plan.id) }
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
                                items(uiState.dreamFlowPlans, key = { "dream_${it.id}" }) { plan ->
                                    DreamFlowPlanCard(
                                        plan = plan,
                                        onRestore = { onRestoreFromDreams(plan.id) },
                                        onDelete = { onDeleteFlow(plan.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (uiState.studioArcPlans.isEmpty() && uiState.arcPlans.isEmpty()) {
                        item {
                            EmptyPathsState(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoGraph,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                title = emptyArcsTitle,
                                body = emptyArcsBody,
                                cta = planArcText,
                                onClick = onPlanArcClick
                            )
                        }
                    } else {
                        if (uiState.studioArcPlans.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = studioTitle,
                                    subtitle = studioSubtitle
                                )
                            }

                            items(uiState.studioArcPlans, key = { "studio_${it.id}" }) { arc ->
                                StudioArcCard(
                                    arc = arc,
                                    onClick = { onOpenArc(arc.id) },
                                    onRemoveFromStudio = { onRemoveArcFromStudio(arc.id) }
                                )
                            }
                        }

                        item {
                            SectionHeader(
                                title = suggestedRoutesTitle,
                                subtitle = suggestedRoutesSubtitle
                            )
                        }

                        items(SuggestedRoutesCatalog.routes, key = { "suggested_${it.id}" }) { route ->
                            SuggestedRouteCard(
                                title = route.title,
                                subtitle = route.subtitle,
                                category = route.category,
                                approxMinutes = route.approxMinutes,
                                onClick = { onOpenSuggestedRoute(route.id) }
                            )
                        }

                        item {
                            SectionHeader(
                                title = yourArcsTitle,
                                subtitle = yourArcsSubtitle
                            )
                        }

                        item {
                            Button(
                                onClick = onPlanArcClick,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(planArcText)
                            }
                        }

                        if (uiState.arcPlans.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = emptyMoreArcsBody,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                    )
                                }
                            }
                        } else {
                            items(uiState.arcPlans, key = { it.id }) { arc ->
                                ArcLibraryCard(
                                    arc = arc,
                                    onClick = { onOpenArc(arc.id) },
                                    onAddToStudio = { onAddArcToStudio(arc.id) },
                                    onDelete = { onDeleteArc(arc.id) }
                                )
                            }
                        }
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
    onSave: (
        title: String,
        tagName: String,
        isSoftMode: Boolean,
        targetMinutesText: String,
        launchWithSurge: Boolean
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var tagName by rememberSaveable { mutableStateOf("") }
    var isSoftMode by rememberSaveable { mutableStateOf(false) }
    var targetMinutesText by rememberSaveable { mutableStateOf("") }
    var launchWithSurge by rememberSaveable { mutableStateOf(false) }
    val chipListState = rememberLazyListState()

    val selectedText = stringResource(R.string.paths_selected)
    val notSelectedText = stringResource(R.string.paths_not_selected)
    val softFlowSwitchA11y = stringResource(R.string.paths_soft_flow_switch_a11y)
    val launchWithSurgeSwitchA11y = stringResource(R.string.paths_launch_with_surge_switch_a11y)
    val onText = stringResource(R.string.paths_on)
    val offText = stringResource(R.string.paths_off)
    val planFlowSheetTitle = stringResource(R.string.paths_plan_flow_sheet_title)
    val planFlowSheetSubtitle = stringResource(R.string.paths_plan_flow_sheet_subtitle)
    val titleLabel = stringResource(R.string.paths_title_label)
    val titlePlaceholder = stringResource(R.string.paths_title_placeholder)
    val journeyTagLabel = stringResource(R.string.paths_journey_tag_label)
    val journeyTagPlaceholder = stringResource(R.string.paths_journey_tag_placeholder)
    val recentTagsText = stringResource(R.string.paths_recent_tags)
    val softFlowTitle = stringResource(R.string.paths_soft_flow_title)
    val softFlowBody = stringResource(R.string.paths_soft_flow_body)
    val targetMinutesLabel = stringResource(R.string.paths_target_minutes_label)
    val targetMinutesPlaceholder = stringResource(R.string.paths_target_minutes_placeholder)
    val launchWithSurgeTitle = stringResource(R.string.paths_launch_with_surge_title)
    val launchWithSurgeDisabledSoft = stringResource(R.string.paths_launch_with_surge_disabled_soft)
    val launchWithSurgeDisabledNoTarget = stringResource(R.string.paths_launch_with_surge_disabled_no_target)
    val launchWithSurgeEnabledBody = stringResource(R.string.paths_launch_with_surge_enabled_body)
    val cancelText = stringResource(R.string.paths_cancel)
    val savingText = stringResource(R.string.paths_saving)
    val saveText = stringResource(R.string.paths_save)

    val hasTargetMinutes = targetMinutesText.trim().isNotBlank()
    val surgeEnabled = !isSoftMode && hasTargetMinutes

    if (!surgeEnabled && launchWithSurge) {
        launchWithSurge = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = planFlowSheetTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = planFlowSheetSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(titleLabel) },
                placeholder = { Text(titlePlaceholder) },
                singleLine = true
            )

            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(journeyTagLabel) },
                placeholder = { Text(journeyTagPlaceholder) },
                singleLine = true
            )

            if (tags.isNotEmpty()) {
                Text(
                    text = recentTagsText,
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.take(3).forEach { tag ->
                                val chipA11y = stringResource(
                                    R.string.paths_recent_tag_chip_a11y,
                                    tag.name
                                )
                                val chipState = if (tagName == tag.name) selectedText else notSelectedText

                                FilterChip(
                                    selected = tagName == tag.name,
                                    onClick = { tagName = tag.name },
                                    label = { Text(tag.name) },
                                    modifier = Modifier.semantics {
                                        role = Role.Button
                                        contentDescription = chipA11y
                                        stateDescription = chipState
                                    }
                                )
                            }
                        }
                    }

                    if (tags.size > 3) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tags.drop(3).take(3).forEach { tag ->
                                    val chipA11y = stringResource(
                                        R.string.paths_recent_tag_chip_a11y,
                                        tag.name
                                    )
                                    val chipState = if (tagName == tag.name) selectedText else notSelectedText

                                    FilterChip(
                                        selected = tagName == tag.name,
                                        onClick = { tagName = tag.name },
                                        label = { Text(tag.name) },
                                        modifier = Modifier.semantics {
                                            role = Role.Button
                                            contentDescription = chipA11y
                                            stateDescription = chipState
                                        }
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
                            text = softFlowTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = softFlowBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                    }
                    Switch(
                        checked = isSoftMode,
                        onCheckedChange = {
                            isSoftMode = it
                            if (it) launchWithSurge = false
                        },
                        modifier = Modifier.semantics {
                            contentDescription = softFlowSwitchA11y
                            stateDescription = if (isSoftMode) onText else offText
                        }
                    )
                }
            }

            OutlinedTextField(
                value = targetMinutesText,
                onValueChange = { targetMinutesText = it.filter(Char::isDigit) },
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
                                isSoftMode -> launchWithSurgeDisabledSoft
                                !hasTargetMinutes -> launchWithSurgeDisabledNoTarget
                                else -> launchWithSurgeEnabledBody
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                    }
                    Switch(
                        checked = launchWithSurge,
                        onCheckedChange = { launchWithSurge = it },
                        enabled = surgeEnabled,
                        modifier = Modifier.semantics {
                            contentDescription = launchWithSurgeSwitchA11y
                            stateDescription = if (launchWithSurge) onText else offText
                        }
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
                    Text(cancelText)
                }

                Button(
                    onClick = {
                        onSave(
                            title,
                            tagName,
                            isSoftMode,
                            targetMinutesText,
                            launchWithSurge
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (isSaving) savingText else saveText)
                }
            }
        }
    }
}

@Composable
private fun PathsHeader() {
    val title = stringResource(R.string.paths_title)
    val body = stringResource(R.string.paths_header_body)

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
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = body,
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
    val countText = pluralStringResource(R.plurals.paths_dreams_count, count, count)
    val headerA11y = stringResource(R.string.paths_dreams_header_a11y, countText)
    val expandedText = stringResource(R.string.paths_expanded)
    val collapsedText = stringResource(R.string.paths_collapsed)
    val dreamsTitle = stringResource(R.string.paths_dreams_title)
    val hideText = stringResource(R.string.paths_hide)
    val showText = stringResource(R.string.paths_show)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .semantics {
                role = Role.Button
                contentDescription = headerA11y
                stateDescription = if (expanded) expandedText else collapsedText
            },
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
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = dreamsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                )
            }

            Text(
                text = if (expanded) hideText else showText,
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

    val surgeText = stringResource(R.string.paths_surge)
    val pinnedText = stringResource(R.string.paths_pinned)
    val moreActionsText = stringResource(R.string.paths_more_actions)
    val unpinText = stringResource(R.string.paths_unpin)
    val pinText = stringResource(R.string.paths_pin)
    val moveToDreamsText = stringResource(R.string.paths_move_to_dreams)
    val deleteTitle = stringResource(R.string.paths_delete_planned_flow_title)
    val deleteBody = stringResource(R.string.paths_delete_planned_flow_body)
    val deleteText = stringResource(R.string.common_delete)
    val cancelText = stringResource(R.string.common_cancel)
    val notLaunchedYetText = stringResource(R.string.paths_not_launched_yet)
    val softText = stringResource(R.string.paths_soft)

    val meta = buildList {
        if (plan.tagName.isNotBlank()) add(plan.tagName)
        plan.targetMinutes?.let { add(stringResource(R.string.paths_minutes_compact, it)) }
        if (plan.launchWithSurge) add(surgeText)
    }.joinToString(" • ")

    val launchText = if (plan.launchCount > 0) {
        pluralStringResource(R.plurals.paths_launch_count, plan.launchCount, plan.launchCount)
    } else {
        notLaunchedYetText
    }

    val cardA11y = buildString {
        append(plan.title)
        if (meta.isNotBlank()) append(". $meta")
        append(". $launchText")
        if (plan.pinned) append(". $pinnedText")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = cardA11y
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    imageVector = if (plan.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (plan.isSoftMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
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

                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }

                if (plan.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = pinnedText
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = moreActionsText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(if (plan.pinned) unpinText else pinText)
                            },
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
                            text = { Text(moveToDreamsText) },
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
                            text = { Text(deleteText) },
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.targetMinutes?.let {
                    MiniBadge(text = stringResource(R.string.paths_minutes_short, it))
                }
                if (plan.launchWithSurge) {
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
                if (plan.isSoftMode) {
                    MiniBadge(text = softText)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = launchText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteTitle) },
            text = { Text(deleteBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) { Text(deleteText) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(cancelText)
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

    val surgeText = stringResource(R.string.paths_surge)
    val savedInDreamsText = stringResource(R.string.paths_saved_in_dreams)
    val moreActionsText = stringResource(R.string.paths_more_actions)
    val bringBackText = stringResource(R.string.paths_bring_back)
    val deleteText = stringResource(R.string.common_delete)
    val cancelText = stringResource(R.string.common_cancel)
    val deleteTitle = stringResource(R.string.paths_delete_dream_flow_title)
    val deleteBody = stringResource(R.string.paths_delete_dream_flow_body)
    val softText = stringResource(R.string.paths_soft)

    val meta = buildList {
        if (plan.tagName.isNotBlank()) add(plan.tagName)
        plan.targetMinutes?.let { add(stringResource(R.string.paths_minutes_compact, it)) }
        if (plan.launchWithSurge) add(surgeText)
    }.joinToString(" • ")

    val cardA11y = buildString {
        append(plan.title)
        if (meta.isNotBlank()) append(". $meta")
        append(". $savedInDreamsText")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = cardA11y
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    imageVector = if (plan.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
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

                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = moreActionsText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(bringBackText) },
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
                            text = { Text(deleteText) },
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.targetMinutes?.let {
                    MiniBadge(text = stringResource(R.string.paths_minutes_short, it))
                }
                if (plan.launchWithSurge) {
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
                if (plan.isSoftMode) {
                    MiniBadge(text = softText)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = savedInDreamsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteTitle) },
            text = { Text(deleteBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) { Text(deleteText) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(cancelText)
                }
            }
        )
    }
}

@Composable
private fun StudioArcCard(
    arc: ArcPlanListItemUiModel,
    onClick: () -> Unit,
    onRemoveFromStudio: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val studioTitle = stringResource(R.string.paths_studio_title)
    val moreActionsText = stringResource(R.string.paths_more_actions)
    val removeFromStudioText = stringResource(R.string.paths_remove_from_studio)

    val launchText = if (arc.launchCount > 0) {
        pluralStringResource(R.plurals.paths_launch_count, arc.launchCount, arc.launchCount)
    } else {
        stringResource(R.string.paths_ready_to_return)
    }

    val cardA11y = stringResource(
        R.string.paths_studio_arc_card_a11y,
        arc.title,
        launchText
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = cardA11y
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
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
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = studioTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = moreActionsText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(removeFromStudioText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromStudio()
                            }
                        )
                    }
                }
            }

            Text(
                text = arc.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = launchText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun ArcLibraryCard(
    arc: ArcPlanListItemUiModel,
    onClick: () -> Unit,
    onAddToStudio: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val moreActionsText = stringResource(R.string.paths_more_actions)
    val addToStudioText = stringResource(R.string.paths_add_to_studio)
    val deleteText = stringResource(R.string.common_delete)
    val cancelText = stringResource(R.string.common_cancel)
    val deleteTitle = stringResource(R.string.paths_delete_arc_title)
    val deleteBody = stringResource(R.string.paths_delete_arc_body)
    val arcLibraryBody = stringResource(R.string.paths_arc_library_body)

    val launchText = if (arc.launchCount > 0) {
        pluralStringResource(R.plurals.paths_launch_count, arc.launchCount, arc.launchCount)
    } else {
        stringResource(R.string.paths_not_launched_yet)
    }

    val cardA11y = stringResource(
        R.string.paths_arc_library_card_a11y,
        arc.title,
        launchText
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = cardA11y
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    imageVector = Icons.Outlined.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = arc.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = launchText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = moreActionsText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(addToStudioText) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onAddToStudio()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(deleteText) },
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
                text = arcLibraryBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteTitle) },
            text = { Text(deleteBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(deleteText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(cancelText)
                }
            }
        )
    }
}

@Composable
private fun SuggestedRouteCard(
    title: String,
    subtitle: String,
    category: String,
    approxMinutes: Int?,
    onClick: () -> Unit
) {
    val tapToPreviewText = stringResource(R.string.paths_tap_to_preview)

    val meta = buildList {
        approxMinutes?.let { add(stringResource(R.string.paths_approx_minutes, it)) }
        add(tapToPreviewText)
    }.joinToString(" • ")

    val cardA11y = stringResource(
        R.string.paths_suggested_route_card_a11y,
        category,
        title,
        subtitle,
        meta
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )

            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
            )
        }
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
                text = stringResource(R.string.paths_tab_flows),
                selected = selected == PathsPrimaryTab.FLOWS,
                onClick = { onSelected(PathsPrimaryTab.FLOWS) }
            )
            SegmentedChoice(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.paths_tab_arcs),
                selected = selected == PathsPrimaryTab.ARCS,
                onClick = { onSelected(PathsPrimaryTab.ARCS) }
            )
        }
    }
}

@Composable
private fun SegmentedSurface(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) { content() }
}

@Composable
private fun SegmentedChoice(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedText = stringResource(R.string.paths_selected)
    val notSelectedText = stringResource(R.string.paths_not_selected)

    Surface(
        modifier = modifier.semantics {
            role = Role.Tab
            contentDescription = text
            stateDescription = if (selected) selectedText else notSelectedText
        },
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
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
                ) { icon() }
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
private fun ErrorPathsState(message: String?) {
    val fallback = stringResource(R.string.paths_error_generic)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = message ?: fallback
            }
    ) {
        Text(
            text = message ?: fallback,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}