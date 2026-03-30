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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    val sessions = uiState.viewJourneysSessions
    val selected = remember(selectedSessionId, sessions) {
        selectedSessionId?.let { id -> sessions.firstOrNull { it.sessionId == id } }
    }

    val windowTitle = remember(uiState.anchorDayStartMs, uiState.period) {
        formatPeriodTitle(uiState.period, uiState.anchorDayStartMs)
    }
    val windowSubtitle = remember(uiState.anchorDayStartMs, uiState.period) {
        formatPeriodSubtitle(uiState.period, uiState.anchorDayStartMs)
    }

    val totalDuration = remember(sessions) { sessions.sumOf { it.durationMs } }
    val totalScyraScore = remember(sessions) { sessions.sumOf { it.score } }
    val totalBeamBonus = remember(sessions) { sessions.sumOf { it.beamBonusPoints } }
    val totalBaseScore = remember(totalScyraScore, totalBeamBonus) { totalScyraScore - totalBeamBonus }
    val totalSurge = remember(sessions) { sessions.sumOf { it.surgePoints } }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected != null) {
                    TextButton(onClick = { selectedSessionId = null }) { Text("Back") }
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.viewJourneysTitle.ifBlank { "Journey" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$windowTitle • $windowSubtitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onClose) { Text("Close") }
            }

            if (selected == null) {
                JourneyViewSummary(
                    flowsCount = sessions.size,
                    totalDurationMs = totalDuration,
                    totalBaseScore = totalBaseScore,
                    totalBeamBonus = totalBeamBonus,
                    totalScyraScore = totalScyraScore,
                    totalSurge = totalSurge
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))

                if (sessions.isEmpty()) {
                    Text(
                        text = "No flows for this journey in this view.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 10.dp)
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