package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.ui.screen.story.header.StoryHeaderScrollableWithStickyTabs
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun StoryBody(
    uiState: FlowListUiState,
    listState: LazyListState,
    isFlowStateActive: Boolean,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onGoToActiveSession: () -> Unit,
    onAddSessionClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionDescription: (Long, String) -> Unit,
    onOpenViewJourneys: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.align(Alignment.Center))

            uiState.errorMessage != null -> ErrorState(
                message = uiState.errorMessage,
                modifier = Modifier.align(Alignment.Center)
            )

            else -> {
                StoryHeaderScrollableWithStickyTabs(
                    uiState = uiState,
                    listState = listState,
                    onTagSelected = onTagSelected,
                    onPeriodSelected = onPeriodSelected,
                    onPrev = onPrev,
                    onNext = onNext,
                    onToday = onToday,
                    onOpenViewJourneys = onOpenViewJourneys,
                    onSessionClick = onSessionClick,
                    onDeleteSession = onDeleteSession,
                    onUpdateSessionDescription = onUpdateSessionDescription,
                    onAddSessionClick = onAddSessionClick,
                    extraTopContent = if (isFlowStateActive) {
                        { FlowModeHeroCard(onGoToActiveSession = onGoToActiveSession) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
private fun ErrorState(message: String?, modifier: Modifier = Modifier) {
    Text(
        text = message ?: "Error",
        modifier = modifier,
        color = MaterialTheme.colorScheme.error
    )
}
