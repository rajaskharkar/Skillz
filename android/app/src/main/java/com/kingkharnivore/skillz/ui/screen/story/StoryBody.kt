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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.ui.screen.story.header.StoryHeaderScrollableWithStickyTabs
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun StoryBody(
    uiState: FlowListUiState,
    listState: LazyListState,
    isFlowStateActive: Boolean,
    onTagToggled: (Long) -> Unit,
    onClearAllTags: () -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onGoToActiveSession: () -> Unit,
    onAddSessionClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onDeletePulse: (Long) -> Unit,
    onUpdatePulse: (Long, String, String, String) -> Unit,
    onUpdateSessionDescription: (Long, String) -> Unit,
    onCreatePulseForSession: (Long, String, String, String) -> Unit,
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
                    onTagToggled = onTagToggled,
                    onClearAllTags = onClearAllTags,
                    onPeriodSelected = onPeriodSelected,
                    onPrev = onPrev,
                    onNext = onNext,
                    onToday = onToday,
                    onOpenViewJourneys = onOpenViewJourneys,
                    onSessionClick = onSessionClick,
                    onDeleteSession = onDeleteSession,
                    onDeletePulse = onDeletePulse,
                    onUpdateSessionDescription = onUpdateSessionDescription,
                    onUpdatePulse = onUpdatePulse,
                    onCreatePulseForSession = onCreatePulseForSession,
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
    val loadingLabel = stringResource(R.string.story_loading)

    CircularProgressIndicator(
        modifier = modifier.semantics {
            contentDescription = loadingLabel
            liveRegion = LiveRegionMode.Polite
        }
    )
}

@Composable
private fun ErrorState(message: String?, modifier: Modifier = Modifier) {
    val fallbackError = stringResource(R.string.story_error_generic)
    val resolvedMessage = message ?: fallbackError

    Text(
        text = resolvedMessage,
        modifier = modifier.semantics {
            contentDescription = resolvedMessage
            liveRegion = LiveRegionMode.Polite
        },
        color = MaterialTheme.colorScheme.error
    )
}