package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.ui.screen.story.rememberExpandedArcIdsState
import com.kingkharnivore.skillz.ui.screen.story.rememberExpandedSessionIdsState
import com.kingkharnivore.skillz.ui.screen.story.rememberPulseEditState
import com.kingkharnivore.skillz.ui.screen.story.rememberSessionEditState
import com.kingkharnivore.skillz.ui.screen.story.chronicle.ArcGroupCard
import com.kingkharnivore.skillz.ui.screen.story.chronicle.FlowCard
import com.kingkharnivore.skillz.ui.screen.story.chronicle.PulseCard
import com.kingkharnivore.skillz.ui.screen.story.saga.SagaPulseSection
import com.kingkharnivore.skillz.ui.screen.story.saga.SagasCard
import com.kingkharnivore.skillz.utils.time.StoryPeriod

private enum class StoryTab { SAGAS, CHRONICLES }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryHeaderScrollableWithStickyTabs(
    uiState: FlowListUiState,
    listState: LazyListState,
    onTagToggled: (Long) -> Unit,
    onClearAllTags: () -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onDeletePulse: (Long) -> Unit,
    onUpdatePulse: (Long, String, String) -> Unit,
    onCreatePulseForSession: (Long, String, String, String) -> Unit,
    onAddSessionClick: () -> Unit,
    extraTopContent: (@Composable () -> Unit)? = null,
    onEditArc: (Long) -> Unit,
    createHistoricalChronicle: (String, String) -> com.kingkharnivore.skillz.ui.screen.chronicle.ChronicleReadState
) {
    var tab by rememberSaveable { mutableStateOf(StoryTab.CHRONICLES) }

    val sagasLabel = stringResource(R.string.story_tab_sagas)
    val chroniclesLabel = stringResource(R.string.story_tab_chronicles)

    val expandedState = rememberExpandedSessionIdsState()
    val expandedArcState = rememberExpandedArcIdsState()
    val editState = rememberSessionEditState()
    val pulseEditState = rememberPulseEditState()
    var expandedPulseIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    val editingFlow = editState.editingSession.value
    val historicalHolder = editingFlow?.let { flow ->
        remember(flow.sessionId) { createHistoricalChronicle("SESSION", flow.sessionId.toString()) }
    }
    DisposableEffect(historicalHolder) {
        onDispose { historicalHolder?.close() }
    }
    val historicalMoments = historicalHolder?.moments?.collectAsState()?.value.orEmpty()
    val editingPulse = pulseEditState.editingPulse.value
    val pulseHistoricalHolder = editingPulse?.let { pulse ->
        remember(pulse.pulseId) { createHistoricalChronicle("PULSE", pulse.pulseId.toString()) }
    }
    DisposableEffect(pulseHistoricalHolder) { onDispose { pulseHistoricalHolder?.close() } }
    val pulseHistoricalMoments = pulseHistoricalHolder?.moments?.collectAsState()?.value.orEmpty()
    FlowDetailsSheet(
        editState = editState,
        tags = uiState.tags,
        childPulses = editingFlow?.let { uiState.pulsesBySessionId[it.sessionId].orEmpty() }.orEmpty(),
        onCreatePulse = onCreatePulseForSession,
        onDeletePulse = onDeletePulse,
        onEditPulse = { pulse -> pulseEditState.startEditing(pulse) },
        chronicleMoments = historicalMoments
    )

    PulseEditSheet(
        editState = pulseEditState,
        tags = uiState.tags,
        onSave = onUpdatePulse,
        chronicleMoments = pulseHistoricalMoments
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StoryHeader(
                uiState = uiState,
                onTagToggled = onTagToggled,
                onClearAllTags = onClearAllTags,
                onPeriodSelected = onPeriodSelected,
                onPrev = onPrev,
                onNext = onNext,
                onToday = onToday,
                onOpenViewJourneys = onOpenViewJourneys,
                extraTopContent = extraTopContent,
            )
        }

        stickyHeader {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val container = MaterialTheme.colorScheme.surfaceVariant
                    val selectedBg = MaterialTheme.colorScheme.surface
                    val selectedFg = MaterialTheme.colorScheme.secondary
                    val unselectedFg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = container,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(3.dp)
                                .height(34.dp)
                                .semantics { isTraversalGroup = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SegmentedIconTab(
                                selected = tab == StoryTab.SAGAS,
                                onClick = { tab = StoryTab.SAGAS },
                                selectedBg = selectedBg,
                                selectedFg = selectedFg,
                                unselectedFg = unselectedFg,
                                icon = Icons.Outlined.MenuBook,
                                contentDescription = sagasLabel
                            )

                            SegmentedIconTab(
                                selected = tab == StoryTab.CHRONICLES,
                                onClick = { tab = StoryTab.CHRONICLES },
                                selectedBg = selectedBg,
                                selectedFg = selectedFg,
                                unselectedFg = unselectedFg,
                                icon = Icons.Outlined.Timeline,
                                contentDescription = chroniclesLabel,
                            )
                        }
                    }
                }
            }
        }

        when (tab) {
            StoryTab.SAGAS -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (uiState.sagaPulsesInView.isNotEmpty()) {
                            SagaPulseSection(pulses = uiState.sagaPulsesInView)
                        }

                        if (uiState.sagasInView.isEmpty()) {
                            if (uiState.sagaPulsesInView.isEmpty()) {
                                EmptySagasState()
                            }
                        } else {
                            SagasCard(
                                period = uiState.period,
                                anchorDayStartMs = uiState.anchorDayStartMs,
                                stats = uiState.sagasInView,
                                onOpenViewJourneys = onOpenViewJourneys
                            )
                        }
                    }
                }
            }

            StoryTab.CHRONICLES -> {
                if (uiState.chronicleItems.isEmpty()) {
                    item {
                        val shouldShowFirstTimeUser =
                            !uiState.hasAnyRecordedArtifacts && uiState.isCurrentPeriod

                        if (shouldShowFirstTimeUser) {
                            FirstTimeUser(onAddSessionClick = onAddSessionClick)
                        } else {
                            EmptyChroniclesState(
                                period = uiState.period,
                                isCurrentPeriod = uiState.isCurrentPeriod,
                                onTodayClick = if (uiState.isCurrentPeriod) null else onToday
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.chronicleItems,
                        key = { it.key }
                    ) { item ->
                        when (item) {
                            is ChronicleUiModel.StandaloneFlow -> {
                                val session = item.flow
                                FlowCard(
                                    session = session,
                                    childPulses = item.childPulses,
                                    isExpanded = expandedState.isExpanded(session.sessionId),
                                    showScoreUi = uiState.showScoreUi,
                                    calmMode = uiState.calmMode,
                                    onToggleExpand = { expandedState.toggle(session.sessionId) },
                                    onDeleteSession = { onDeleteSession(session.sessionId) },
                                    onLongPress = { editState.startEditing(session) },
                                    onClick = { onSessionClick(session.sessionId) },
                                    onEditPulse = { pulse -> pulseEditState.startEditing(pulse) },
                                    onDeletePulse = onDeletePulse
                                )
                            }

                            is ChronicleUiModel.StandalonePulse -> {
                                val pulse = item.pulse
                                val isExpanded = expandedPulseIds.contains(pulse.pulseId)

                                PulseCard(
                                    pulse = pulse,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        expandedPulseIds =
                                            if (expandedPulseIds.contains(pulse.pulseId)) {
                                                expandedPulseIds - pulse.pulseId
                                            } else {
                                                expandedPulseIds + pulse.pulseId
                                            }
                                    },
                                    onLongPress = { pulseEditState.startEditing(pulse) },
                                    onDeletePulse = { onDeletePulse(pulse.pulseId) }
                                )
                            }

                            is ChronicleUiModel.ArcGroup -> {
                                val isFilteredByTags = uiState.selectedTagIds.isNotEmpty()

                                ArcGroupCard(
                                    group = item,
                                    showScoreUi = uiState.showScoreUi,
                                    calmMode = uiState.calmMode,
                                    isExpanded = if (isFilteredByTags) {
                                        true
                                    } else {
                                        expandedArcState.isExpanded(item.arcId)
                                    },
                                    onToggleExpanded = {
                                        if (!isFilteredByTags) {
                                            expandedArcState.toggle(item.arcId)
                                        }
                                    },
                                    isExpandedByFilter = isFilteredByTags,
                                    isExpandedFlow = { sessionId -> expandedState.isExpanded(sessionId) },
                                    onToggleFlowExpand = { sessionId -> expandedState.toggle(sessionId) },
                                    onDeleteSession = onDeleteSession,
                                    onEditPulse = { pulse -> pulseEditState.startEditing(pulse) },
                                    onDeletePulse = onDeletePulse,
                                    onLongPress = { session -> editState.startEditing(session) },
                                    onClick = onSessionClick,
                                    onEditDetails = { onEditArc(item.arcId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
