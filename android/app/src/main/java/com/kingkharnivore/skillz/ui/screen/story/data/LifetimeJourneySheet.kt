package com.kingkharnivore.skillz.ui.screen.story.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.story.chronicle.FlowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifetimeJourneySheet(
    uiState: FlowListUiState,
    onClose: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onEditSession: (FlowListItemUiModel) -> Unit,
    onEditPulse: (PulseListItemUiModel) -> Unit,
    onDeletePulse: (Long) -> Unit
) {
    var expanded by rememberSaveable(uiState.viewJourneysTitle) { mutableStateOf(emptyList<Long>()) }
    val title = uiState.viewJourneysTitle
    ModalBottomSheet(onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.9f).semantics { paneTitle = title },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.lifetime_all_history), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = onClose) { Text(stringResource(R.string.common_close)) }
                }
            }
            if (uiState.viewJourneysSessions.isEmpty()) {
                item { Text(stringResource(R.string.view_journeys_empty)) }
            }
            items(uiState.viewJourneysSessions, key = { it.sessionId }) { session ->
                FlowCard(
                    session = session, isExpanded = session.sessionId in expanded,
                    showScoreUi = uiState.showScoreUi, calmMode = uiState.calmMode,
                    onToggleExpand = { expanded = if (session.sessionId in expanded) expanded - session.sessionId else expanded + session.sessionId },
                    onDeleteSession = { onDeleteSession(session.sessionId) },
                    onLongPress = { onEditSession(session) },
                    onClick = { onSessionClick(session.sessionId) },
                    childPulses = uiState.pulsesBySessionId[session.sessionId].orEmpty(),
                    onEditPulse = onEditPulse, onDeletePulse = onDeletePulse
                )
            }
        }
    }
}
