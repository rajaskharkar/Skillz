package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.state.FlowListUiState
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun StoryHeader(
    uiState: FlowListUiState,
    onTagToggled: (Long) -> Unit,
    onClearAllTags: () -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    extraTopContent: (@Composable () -> Unit)? = null
) {
    TagFilterRow(
        tags = uiState.tags,
        selectedTagIds = uiState.selectedTagIds,
        onTagToggled = onTagToggled,
        onClearAll = onClearAllTags
    )

    Spacer(modifier = Modifier.height(12.dp))

    PeriodAndDateNavigator(
        period = uiState.period,
        anchorDayStartMs = uiState.anchorDayStartMs,
        firstSessionStartMs = uiState.firstSessionStartMs,
        onPeriodSelected = onPeriodSelected,
        onPrev = onPrev,
        onNext = onNext,
        onToday = onToday
    )

    Spacer(modifier = Modifier.height(12.dp))

    extraTopContent?.invoke()

    when {
        uiState.selectedTagIds.isNotEmpty() -> {
            TotalTimeHighlight(
                totalDurationMs = uiState.totalDurationMs,
                subtitle = "Time in selected journeys"
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        uiState.totalDurationMs > 0L -> {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SubtleTimeSummary(totalDurationMs = uiState.totalDurationMs)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (uiState.showScoreUi) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScoreDisplay(
                score = uiState.currentScore,
                surgeScore = uiState.currentSurgeScore,
                period = uiState.period,
                modifier = Modifier.fillMaxWidth(),
                calmMode = uiState.calmMode
            )
        }
    }

    HorizontalDivider()
    Spacer(Modifier.height(12.dp))
}