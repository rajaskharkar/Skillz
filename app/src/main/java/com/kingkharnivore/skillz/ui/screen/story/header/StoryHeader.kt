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
import com.kingkharnivore.skillz.data.model.entity.FlowListUiState
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Composable
fun StoryHeader(
    uiState: FlowListUiState,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    extraTopContent: (@Composable () -> Unit)? = null
) {
    TagFilterRow(
        tags = uiState.tags,
        selectedTagId = uiState.selectedTagId,
        onTagSelected = onTagSelected
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

    if (uiState.selectedTagId != null) {
        TotalTimeHighlight(
            totalDurationMs = uiState.totalDurationMs,
            subtitle = "Time in view"
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ScoreDisplay(
            score = uiState.currentScore,
            surgeScore = uiState.currentSurgeScore,
            period = uiState.period,
            modifier = Modifier.fillMaxWidth()
        )
    }

    HorizontalDivider()

    HorizontalDivider()

    Spacer(Modifier.height(12.dp))
}