package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.model.ui.ChronicleUiModel
import com.kingkharnivore.skillz.ui.screen.story.chronicle.ArcGroupCard
import com.kingkharnivore.skillz.ui.screen.story.rememberExpandedSessionIdsState
import com.kingkharnivore.skillz.ui.screen.story.rememberSessionEditState
import com.kingkharnivore.skillz.ui.screen.story.chronicle.FlowCard
import com.kingkharnivore.skillz.ui.screen.story.rememberExpandedArcIdsState
import com.kingkharnivore.skillz.ui.screen.story.saga.SagasCard
import com.kingkharnivore.skillz.utils.time.StoryPeriod

private enum class StoryTab { SAGAS, CHRONICLES }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoryHeaderScrollableWithStickyTabs(
    uiState: FlowListUiState,
    listState: LazyListState,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionDescription: (Long, String) -> Unit,
    onAddSessionClick: () -> Unit,
    extraTopContent: (@Composable () -> Unit)? = null
) {
    var tab by rememberSaveable { mutableStateOf(StoryTab.CHRONICLES) }

    val expandedState = rememberExpandedSessionIdsState()
    val expandedArcState = rememberExpandedArcIdsState()
    val editState = rememberSessionEditState()

    FlowEditDialog(
        editState = editState,
        onSave = { id, text -> onUpdateSessionDescription(id, text) }
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Scrollable Header ───────────────────────────────────────
        item {
            StoryHeader(
                uiState = uiState,
                onTagSelected = onTagSelected,
                onPeriodSelected = onPeriodSelected,
                onPrev = onPrev,
                onNext = onNext,
                onToday = onToday,
                onOpenViewJourneys = onOpenViewJourneys,
                extraTopContent = extraTopContent
            )
        }

        // ── Sticky Lean Tabs ────────────────────────────────────────
        stickyHeader {
            // keep sticky area from showing content "through" while scrolling
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
                                .padding(3.dp) // outer pill padding
                                .height(34.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SegmentedIconTab(
                                selected = tab == StoryTab.SAGAS,
                                onClick = { tab = StoryTab.SAGAS },
                                selectedBg = selectedBg,
                                selectedFg = selectedFg,
                                unselectedFg = unselectedFg,
                                icon = Icons.Outlined.MenuBook,
                                contentDescription = "Sagas"
                            )

                            SegmentedIconTab(
                                selected = tab == StoryTab.CHRONICLES,
                                onClick = { tab = StoryTab.CHRONICLES },
                                selectedBg = selectedBg,
                                selectedFg = selectedFg,
                                unselectedFg = unselectedFg,
                                icon = Icons.Outlined.Timeline,
                                contentDescription = "Chronicles",
                            )
                        }
                    }
                }
            }
        }

        // ── Content (same LazyColumn, so it feels fluid) ─────────────
        when (tab) {
            StoryTab.SAGAS -> {
                item {
                    if (uiState.sagasInView.isEmpty()) {
                        Text(
                            text = "No saga data in this view.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
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

            StoryTab.CHRONICLES -> {
                if (uiState.chronicleItems.isEmpty()) {
                    item { FirstTimeUser(onAddSessionClick = onAddSessionClick) }
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
                                    isExpanded = expandedState.isExpanded(session.sessionId),
                                    showScoreUi = uiState.showScoreUi,
                                    calmMode = uiState.calmMode,
                                    onToggleExpand = { expandedState.toggle(session.sessionId) },
                                    onDeleteSession = { onDeleteSession(session.sessionId) },
                                    onLongPress = { editState.startEditing(session) },
                                    onClick = { onSessionClick(session.sessionId) }
                                )
                            }

                            is ChronicleUiModel.ArcGroup -> {
                                ArcGroupCard(
                                    group = item,
                                    showScoreUi = uiState.showScoreUi,
                                    calmMode = uiState.calmMode,
                                    isExpanded = if (uiState.selectedTagId != null) {
                                        true
                                    } else {
                                        expandedArcState.isExpanded(item.arcId)
                                    },
                                    onToggleExpanded = {
                                        if (uiState.selectedTagId == null) {
                                            expandedArcState.toggle(item.arcId)
                                        }
                                    },
                                    isExpandedByFilter = uiState.selectedTagId != null,
                                    isExpandedFlow = { sessionId -> expandedState.isExpanded(sessionId) },
                                    onToggleFlowExpand = { sessionId -> expandedState.toggle(sessionId) },
                                    onDeleteSession = onDeleteSession,
                                    onLongPress = { session -> editState.startEditing(session) },
                                    onClick = onSessionClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}