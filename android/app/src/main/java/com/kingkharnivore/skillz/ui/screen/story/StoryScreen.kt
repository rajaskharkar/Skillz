@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.story.saga.journeys.ViewJourneysBottomSheet
import com.kingkharnivore.skillz.viewmodel.StoryViewModel

class ExpandedSessionIdsState(
    private val ids: MutableState<Set<Long>>
) {
    fun isExpanded(id: Long): Boolean = ids.value.contains(id)

    fun toggle(id: Long) {
        ids.value = if (ids.value.contains(id)) ids.value - id else ids.value + id
    }
}

class ExpandedArcIdsState(
    private val ids: MutableState<Set<Long>>
) {
    fun isExpanded(id: Long): Boolean = ids.value.contains(id)

    fun toggle(id: Long) {
        ids.value = if (ids.value.contains(id)) ids.value - id else ids.value + id
    }
}

class SessionEditState(
    val editingSession: MutableState<FlowListItemUiModel?>
) {
    fun startEditing(session: FlowListItemUiModel) {
        editingSession.value = session
    }

    fun stopEditing() {
        editingSession.value = null
    }
}

class PulseEditState(
    val editingPulse: MutableState<PulseListItemUiModel?>
) {
    fun startEditing(pulse: PulseListItemUiModel) {
        editingPulse.value = pulse
    }

    fun stopEditing() {
        editingPulse.value = null
    }
}

@Composable
fun rememberPulseEditState(): PulseEditState {
    val editingPulse = remember { mutableStateOf<PulseListItemUiModel?>(null) }
    return remember { PulseEditState(editingPulse) }
}

@Composable
fun StoryScreen(
    viewModel: StoryViewModel,
    onAddSessionClick: () -> Unit,
    onAddPulseClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onGoToActiveSession: () -> Unit,
    isFlowStateActive: Boolean,
    onLaunchPulseFlow: (Long, String, String?) -> Unit = { _, _, _ -> },
    onLaunchObjectiveFlow: (String, com.kingkharnivore.skillz.model.FlowMode) -> Unit = { _, _ -> },
    lookoutViewModel: com.kingkharnivore.skillz.viewmodel.shell.LookoutViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
    groveViewModel: com.kingkharnivore.skillz.viewmodel.IdeaGroveViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val arcEditorState by viewModel.arcEditorState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val objectives by lookoutViewModel.uiState.collectAsStateWithLifecycle()
    val grove by groveViewModel.uiState.collectAsStateWithLifecycle()
    val shownPulse = remember { mutableStateOf<Long?>(null) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val resources = androidx.compose.ui.platform.LocalResources.current
    LaunchedEffect(lookoutViewModel) { lookoutViewModel.events.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(objectives.errorMessage) { objectives.errorMessage?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(groveViewModel, resources, onLaunchPulseFlow) {
        groveViewModel.events.collect { event ->
            when (event) {
                is com.kingkharnivore.skillz.viewmodel.IdeaGroveEvent.NavigateToFlow -> onLaunchPulseFlow(event.pulseId, event.title, event.journeyName)
                com.kingkharnivore.skillz.viewmodel.IdeaGroveEvent.NavigateToCurrentFlow -> onGoToActiveSession()
                is com.kingkharnivore.skillz.viewmodel.IdeaGroveEvent.ShowSnackbar -> snackbar.showSnackbar(resources.getString(event.messageRes))
            }
        }
    }
    shownPulse.value?.let { id ->
        val pulse = (grove.aliveItems + grove.completedItems).firstOrNull { it.pulseId == id }
        SagaPulseHistorySheet(pulse, onDismiss = { shownPulse.value = null })
    }
    objectives.rewardDialog?.let {
        com.kingkharnivore.skillz.ui.screen.shell.rooms.lookout.RewardDialog(it, lookoutViewModel::dismissReward)
    }


    LaunchedEffect(uiState.chronicleItems.size) {
        if (uiState.chronicleItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.kingkharnivore.skillz.ui.screen.chronicle.LocalChronicleReaderFactory provides remember(viewModel) { viewModel::createHistoricalChronicle }
    ) {
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        floatingActionButton = {
            Row(
                modifier = Modifier.semantics { isTraversalGroup = true },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                PulseFab(onClick = onAddPulseClick)
                FlowFab(onClick = onAddSessionClick)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StoryBody(
                uiState = uiState,
                listState = listState,
                isFlowStateActive = isFlowStateActive,
                onTagToggled = viewModel::onTagToggled,
                onClearAllTags = viewModel::onClearAllTags,
                onPeriodSelected = viewModel::onPeriodSelected,
                onPrev = viewModel::goPrev,
                onNext = viewModel::goNext,
                onToday = viewModel::goToday,
                onGoToActiveSession = onGoToActiveSession,
                onAddSessionClick = onAddSessionClick,
                onSessionClick = onSessionClick,
                onDeleteSession = viewModel::deleteSession,
                onDeletePulse = viewModel::deletePulse,
                onCreatePulseForSession = viewModel::createPulseForSession,
                onOpenViewJourneys = viewModel::openViewJourneys,
                onEditArc = viewModel::openArcEditor,
                createChronicleEditor = viewModel::createChronicleEditor,
                onSaveEntry = viewModel::saveEntryDetails,
                objectives = objectives.sagaObjectives,
                onObjectiveFlow = { journey, mode -> if (!grove.isLoading && !grove.isFlowRunning) onLaunchObjectiveFlow(journey, mode) },
                onObjectiveClaim = lookoutViewModel::claimReward,
                onPulseFlow = groveViewModel::onFlowClicked,
                onPulseShow = { shownPulse.value = it },
                sagaFlowEnabled = !grove.isLoading && !grove.isFlowRunning,
                claimEnabled = !objectives.claimInProgress
            )

            ArcDetailsSheet(arcEditorState, viewModel::updateArcEditor, viewModel::requestCloseArcEditor,
                viewModel::saveArcDetails, viewModel::retryArcEditorLoad,
                viewModel::keepEditingArc, viewModel::discardArcChanges)

            ViewJourneysBottomSheet(
                uiState = uiState,
                onClose = viewModel::closeViewJourneys,
                onSessionClick = onSessionClick
            )
        }
    }
}
}

@Composable
fun rememberExpandedSessionIdsState(): ExpandedSessionIdsState {
    val ids = remember { mutableStateOf(setOf<Long>()) }
    return remember { ExpandedSessionIdsState(ids) }
}

@Composable
fun rememberSessionEditState(): SessionEditState {
    val editingSession = remember { mutableStateOf<FlowListItemUiModel?>(null) }
    return remember { SessionEditState(editingSession) }
}

@Composable
fun rememberExpandedArcIdsState(): ExpandedArcIdsState {
    val ids = remember { mutableStateOf(setOf<Long>()) }
    return remember { ExpandedArcIdsState(ids) }
}

@Composable
private fun PulseFab(onClick: () -> Unit) {
    val contentDesc = stringResource(R.string.story_fab_record_pulse)

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = contentDesc
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = {
            Icon(
                imageVector = Icons.Outlined.PsychologyAlt,
                contentDescription = null
            )
        },
        text = {
            Text(text = "Pulse")
        }
    )
}

@Composable
private fun FlowFab(onClick: () -> Unit) {
    val contentDesc = stringResource(R.string.story_fab_start_flow)

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = contentDesc
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null
            )
        },
        text = {
            Text(text = "Flow")
        }
    )
}
