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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.paths.PathsPrimaryTab
import com.kingkharnivore.skillz.model.state.paths.PathsTimeLens
import com.kingkharnivore.skillz.model.state.paths.PathsUiState
import com.kingkharnivore.skillz.model.ui.ArcPlanListItemUiModel
import com.kingkharnivore.skillz.model.ui.ArcPlanStepPreviewUiModel
import com.kingkharnivore.skillz.model.ui.FlowPlanListItemUiModel
import com.kingkharnivore.skillz.ui.screen.paths.suggested.SuggestedRoutesCatalog
import com.kingkharnivore.skillz.ui.theme.color
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
    val suggestedRoutesTitle = stringResource(R.string.paths_suggested_scenes_title)
    val suggestedRoutesSubtitle = stringResource(R.string.paths_suggested_scenes_subtitle)
    val plannedArcsTitle = stringResource(R.string.paths_your_arcs_title)
    val plannedArcsSubtitle = stringResource(R.string.paths_your_arcs_subtitle)
    val suggestionHelperTitle = stringResource(R.string.paths_suggested_sequences_helper_title)
    val suggestionHelperBody = stringResource(R.string.paths_suggested_sequences_helper_body)

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
                                PlannedFlowsHeader(
                                    title = plannedFlowsTitle,
                                    subtitle = plannedFlowsSubtitle,
                                    cta = planFlowText,
                                    onClick = onPlanFlowClick
                                )
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
                    val plannedArcs = uiState.arcPlans
                    if (plannedArcs.isEmpty()) {
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

                        item {
                            SuggestedSequencesHelper(
                                title = suggestionHelperTitle,
                                body = suggestionHelperBody,
                                sectionTitle = suggestedRoutesTitle,
                                sectionSubtitle = suggestedRoutesSubtitle,
                                onOpenSuggestedRoute = onOpenSuggestedRoute
                            )
                        }
                    } else {
                        item {
                            PlannedArcsHeader(
                                title = plannedArcsTitle,
                                subtitle = plannedArcsSubtitle,
                                cta = planArcText,
                                onClick = onPlanArcClick
                            )
                        }

                        items(plannedArcs, key = { it.id }) { arc ->
                            PlannedArcCard(
                                arc = arc,
                                onClick = { onOpenArc(arc.id) },
                                onDelete = { onDeleteArc(arc.id) }
                            )
                        }

                        item {
                            SuggestedSequencesHelper(
                                title = suggestionHelperTitle,
                                body = suggestionHelperBody,
                                sectionTitle = suggestedRoutesTitle,
                                sectionSubtitle = suggestedRoutesSubtitle,
                                onOpenSuggestedRoute = onOpenSuggestedRoute
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedFlowsHeader(
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }

        Button(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(cta)
        }
    }
}

@Composable
private fun PlannedArcsHeader(
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }

        Button(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(cta)
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

    val selectedText = stringResource(R.string.paths_selected)
    val notSelectedText = stringResource(R.string.paths_not_selected)
    val softFlowSwitchA11y = stringResource(R.string.paths_soft_flow_switch_a11y)
    val launchWithSurgeSwitchA11y = stringResource(R.string.paths_launch_with_surge_switch_a11y)
    val onText = stringResource(R.string.paths_on)
    val offText = stringResource(R.string.paths_off)
    val cancelText = stringResource(R.string.paths_cancel)
    val savingText = stringResource(R.string.paths_saving)
    val saveText = stringResource(R.string.paths_save)

    val cleanedTargetMinutes = targetMinutesText.filter(Char::isDigit).take(3)
    if (cleanedTargetMinutes != targetMinutesText) {
        targetMinutesText = cleanedTargetMinutes
    }

    if (isSoftMode && launchWithSurge) {
        launchWithSurge = false
        targetMinutesText = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FlowIntentField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Deep work, gym, reading, writing…"
            )

            MinimalSectionLabel("Journey")

            JourneyInlineField(
                value = tagName,
                onValueChange = { tagName = it },
                tags = tags,
                selectedText = selectedText,
                notSelectedText = notSelectedText
            )

            MinimalSectionLabel("Start")

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InlineToggleRow(
                        title = stringResource(R.string.paths_soft_flow_title),
                        subtitle = stringResource(R.string.paths_soft_flow_body),
                        checked = isSoftMode,
                        onCheckedChange = {
                            isSoftMode = it
                            if (it) {
                                launchWithSurge = false
                                targetMinutesText = ""
                            }
                        },
                        a11yDescription = softFlowSwitchA11y,
                        onText = onText,
                        offText = offText
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    InlineToggleRow(
                        title = "Surge",
                        subtitle = when {
                            isSoftMode -> "Unavailable for Soft flows."
                            launchWithSurge -> "A timed start with a defined target."
                            else -> "Add a target to begin with more intention."
                        },
                        checked = launchWithSurge,
                        onCheckedChange = { enabled ->
                            if (!isSoftMode) {
                                launchWithSurge = enabled
                                if (!enabled) targetMinutesText = ""
                            }
                        },
                        enabled = !isSoftMode,
                        a11yDescription = launchWithSurgeSwitchA11y,
                        onText = onText,
                        offText = offText,
                        trailingContent = {
                            if (launchWithSurge && !isSoftMode) {
                                CompactMinutesField(
                                    value = targetMinutesText,
                                    onValueChange = {
                                        targetMinutesText = it.filter(Char::isDigit).take(3)
                                    }
                                )
                            }
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
                            title.trim(),
                            tagName.trim(),
                            isSoftMode,
                            if (launchWithSurge) targetMinutesText.trim() else "",
                            launchWithSurge
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && title.trim().isNotEmpty(),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (isSaving) savingText else saveText)
                }
            }
        }
    }
}

@Composable
private fun MinimalSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun JourneyInlineField(
    value: String,
    onValueChange: (String) -> Unit,
    tags: List<TagUiModel>,
    selectedText: String,
    notSelectedText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                text = stringResource(R.string.paths_journey_inline_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        if (tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(tags.take(8), key = { it.id }) { tag ->
                    val chipA11y = stringResource(
                        R.string.paths_recent_tag_chip_a11y,
                        tag.name
                    )
                    val chipState = if (value == tag.name) selectedText else notSelectedText

                    FilterChip(
                        selected = value == tag.name,
                        onClick = { onValueChange(if (value == tag.name) "" else tag.name) },
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

@Composable
private fun InlineToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    a11yDescription: String,
    onText: String,
    offText: String,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = a11yDescription
                    stateDescription = if (checked) onText else offText
                }
            )
        }

        trailingContent?.invoke()
    }
}

@Composable
private fun CompactMinutesField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 96.dp, max = 120.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                text = stringResource(R.string.paths_minutes_field_placeholder),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Text(
                text = stringResource(R.string.common_minutes_short),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
            )
        }
    }
}

@Composable
private fun SheetSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
            )
        }

        content()
    }
}

@Composable
private fun FlowIntentField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.paths_flow_intent_prompt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                fontWeight = FontWeight.Medium
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth()) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun JourneyTagField(
    value: String,
    onValueChange: (String) -> Unit,
    tags: List<TagUiModel>,
    selectedText: String,
    notSelectedText: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.paths_journey_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    decorationBox = { innerTextField ->
                        if (value.isBlank()) {
                            Text(
                                text = stringResource(R.string.paths_journey_optional_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            if (tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(tags.take(8), key = { it.id }) { tag ->
                        val chipA11y = stringResource(
                            R.string.paths_recent_tag_chip_a11y,
                            tag.name
                        )
                        val chipState = if (value == tag.name) selectedText else notSelectedText

                        FilterChip(
                            selected = value == tag.name,
                            onClick = { onValueChange(if (value == tag.name) "" else tag.name) },
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

@Composable
private fun MiniMinutesField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 110.dp, max = 132.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(R.string.paths_minutes_field_placeholder),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                        )
                    }
                    innerTextField()
                }
            )

            Text(
                text = stringResource(R.string.common_minutes_short),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun PathsHeader() {
    val title = stringResource(R.string.horizon_title)
    val body = stringResource(R.string.horizon_header_body)

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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = FontFamily.Monospace
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
private fun PlannedArcCard(
    arc: ArcPlanListItemUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val moreActionsText = stringResource(R.string.paths_more_actions)
    val deleteText = stringResource(R.string.common_delete)
    val cancelText = stringResource(R.string.common_cancel)
    val deleteTitle = stringResource(R.string.paths_delete_arc_title)
    val deleteBody = stringResource(R.string.paths_delete_arc_body)
    val reviewArcText = stringResource(R.string.paths_review_arc)
    val editText = stringResource(R.string.paths_edit_arc)
    val arcLibraryBody = stringResource(R.string.paths_arc_library_body)
    val stepCountText = pluralStringResource(
        R.plurals.paths_arc_flow_count,
        arc.stepCount,
        arc.stepCount
    )
    var expanded by rememberSaveable(arc.id) { mutableStateOf(false) }
    val previewLimit = 4
    val visibleSteps = if (expanded || arc.stepCount <= previewLimit) arc.steps else arc.steps.take(previewLimit)
    val stepPreviewText = visibleSteps.joinToString(" → ") { it.title }
        .ifBlank { arcLibraryBody }
    val expandedText = if (expanded) {
        stringResource(R.string.paths_expanded)
    } else {
        stringResource(R.string.paths_collapsed)
    }
    val launchText = if (arc.launchCount > 0) {
        pluralStringResource(R.plurals.paths_launch_count, arc.launchCount, arc.launchCount)
    } else {
        stringResource(R.string.paths_not_launched_yet)
    }
    val cardA11y = stringResource(
        R.string.paths_arc_library_card_a11y,
        arc.title,
        buildList {
            add(stepCountText)
            arc.totalTargetMinutes?.let { add(stringResource(R.string.paths_approx_minutes, it)) }
            add(expandedText)
        }.joinToString(". "),
        stringResource(R.string.paths_arc_step_preview_a11y, stepPreviewText)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = arc.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniBadge(text = stepCountText)
                        arc.totalTargetMinutes?.let {
                            MiniBadge(text = stringResource(R.string.paths_approx_minutes, it))
                        }
                    }

                    Text(
                        text = launchText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
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

            ArcStepPreview(
                arc = arc,
                visibleSteps = visibleSteps,
                expanded = expanded,
                onExpandedChange = { expanded = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(editText)
                }
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(reviewArcText)
                }
            }
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
private fun ArcStepPreview(
    arc: ArcPlanListItemUiModel,
    visibleSteps: List<ArcPlanStepPreviewUiModel>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val fallbackBody = stringResource(R.string.paths_arc_library_body)
    val collapseText = stringResource(R.string.paths_collapse)
    val expandedA11yText = stringResource(R.string.paths_expanded)
    val collapsedA11yText = stringResource(R.string.paths_collapsed)
    val viewAllText = pluralStringResource(
        R.plurals.paths_view_all_flows,
        arc.stepCount,
        arc.stepCount
    )
    val moreText = pluralStringResource(
        R.plurals.paths_more_flows,
        arc.stepCount - visibleSteps.size,
        arc.stepCount - visibleSteps.size
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (visibleSteps.isEmpty()) {
            Text(
                text = fallbackBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
        }

        visibleSteps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = (index + 1).toString(),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = step.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    step.targetMinutes?.let {
                        MiniBadge(text = stringResource(R.string.paths_minutes_short, it))
                    }
                    if (step.launchWithSurge) MiniBadge(text = stringResource(R.string.paths_surge))
                    if (step.isSoftMode) MiniBadge(text = stringResource(R.string.paths_soft))
                }
            }
        }

        if (arc.stepCount > visibleSteps.size || expanded) {
            TextButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = if (expanded) collapseText else viewAllText
                    stateDescription = if (expanded) expandedA11yText else collapsedA11yText
                }
            ) {
                Text(if (expanded) collapseText else "$viewAllText • $moreText")
            }
        }
    }
}

@Composable
private fun SuggestedSequencesHelper(
    title: String,
    body: String,
    sectionTitle: String,
    sectionSubtitle: String,
    onOpenSuggestedRoute: (String) -> Unit
) {
    var showAllSuggestions by rememberSaveable { mutableStateOf(false) }
    val browseText = stringResource(R.string.paths_browse_suggested_scenes)
    val collapseText = stringResource(R.string.paths_collapse)
    val expandedA11yText = stringResource(R.string.paths_expanded)
    val collapsedA11yText = stringResource(R.string.paths_collapsed)
    val suggestions = if (showAllSuggestions) {
        SuggestedRoutesCatalog.routes
    } else {
        SuggestedRoutesCatalog.routes.take(2)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )

            SectionHeader(
                title = sectionTitle,
                subtitle = sectionSubtitle
            )

            suggestions.forEach { route ->
                SuggestedRouteCard(
                    title = route.title,
                    subtitle = route.subtitle,
                    category = route.category,
                    approxMinutes = route.approxMinutes,
                    onClick = { onOpenSuggestedRoute(route.id) }
                )
            }

            TextButton(
                onClick = { showAllSuggestions = !showAllSuggestions },
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = if (showAllSuggestions) collapseText else browseText
                    stateDescription = if (showAllSuggestions) expandedA11yText else collapsedA11yText
                }
            ) {
                Text(
                    if (showAllSuggestions) collapseText else browseText
                )
            }
        }
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