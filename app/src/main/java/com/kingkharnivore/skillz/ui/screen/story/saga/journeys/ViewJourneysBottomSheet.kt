package com.kingkharnivore.skillz.ui.screen.story.saga.journeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.ui.screen.helpers.formatPeriodSubtitle
import com.kingkharnivore.skillz.ui.screen.helpers.formatPeriodTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewJourneysBottomSheet(
    uiState: FlowListUiState,
    onClose: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    if (!uiState.isViewJourneysOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedSessionId by remember(uiState.isViewJourneysOpen, uiState.viewJourneysTitle) {
        mutableStateOf<Long?>(null)
    }

    val backText = stringResource(R.string.common_back)
    val closeText = stringResource(R.string.common_close)
    val fallbackTitle = stringResource(R.string.view_journeys_fallback_title)
    val emptyText = stringResource(R.string.view_journeys_empty)
    val paneTitleText = stringResource(R.string.view_journeys_sheet_pane_title)

    val sessions = uiState.viewJourneysSessions
    val selected = remember(selectedSessionId, sessions) {
        selectedSessionId?.let { id -> sessions.firstOrNull { it.sessionId == id } }
    }

    val windowTitle = formatPeriodTitle(
        period = uiState.period,
        anchorDayStartMs = uiState.anchorDayStartMs
    )
    val windowSubtitle = formatPeriodSubtitle(
        period = uiState.period,
        anchorDayStartMs = uiState.anchorDayStartMs
    )
    val windowLabel = stringResource(
        R.string.view_journeys_window_label,
        windowTitle,
        windowSubtitle
    )

    val totalDuration = remember(sessions) { sessions.sumOf { it.durationMs } }
    val totalScyraScore = remember(sessions) { sessions.sumOf { it.score } }
    val totalBaseScore = totalScyraScore
    val totalSurge = remember(sessions) { sessions.sumOf { it.surgePoints } }

    val sheetTitle = uiState.viewJourneysTitle.ifBlank { fallbackTitle }
    val headerA11y = stringResource(
        R.string.view_journeys_header_a11y,
        sheetTitle,
        windowLabel
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { paneTitle = paneTitleText }
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected != null) {
                    TextButton(onClick = { selectedSessionId = null }) {
                        Text(backText)
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {
                            heading()
                            contentDescription = headerA11y
                        }
                ) {
                    Text(
                        text = sheetTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = windowLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onClose) {
                    Text(closeText)
                }
            }

            if (selected == null) {
                JourneyViewSummary(
                    flowsCount = sessions.size,
                    totalDurationMs = totalDuration,
                    totalBaseScore = totalBaseScore,
                    totalScyraScore = totalScyraScore,
                    totalSurge = totalSurge
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))

                if (sessions.isEmpty()) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .clearAndSetSemantics {
                                contentDescription = emptyText
                            }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        items(
                            items = sessions,
                            key = { it.sessionId }
                        ) { s ->
                            JourneySessionRow(
                                session = s,
                                onExpand = { selectedSessionId = s.sessionId },
                                onScry = {
                                    selectedSessionId = s.sessionId
                                    onSessionClick(s.sessionId)
                                }
                            )
                        }
                    }
                }
            } else {
                JourneySessionDetail(
                    session = selected,
                    onOpenFull = { onSessionClick(selected.sessionId) }
                )
            }
        }
    }
}