package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kingkharnivore.skillz.ui.model.AtlasUiState
import com.kingkharnivore.skillz.ui.model.AtlasViewMode
import com.kingkharnivore.skillz.ui.model.BeamBlockUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasScreen(
    uiState: AtlasUiState,
    onStartFlow: () -> Unit,
    onSelectMode: (AtlasViewMode) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAdvanceDay: (Long) -> Unit,
    onScheduleBeamClick: () -> Unit,
) {
    var selectedBeam by remember { mutableStateOf<BeamBlockUi?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (selectedBeam != null) {
        val b = selectedBeam!!
        val journeyColor = Color(b.journeyColorArgb)
        val sheetBase = journeyColor.copy(alpha = 0.88f)
        val onJourney = Color.White

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { selectedBeam = null },
            containerColor = sheetBase,
            contentColor = onJourney,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = onJourney.copy(alpha = 0.45f)
                )
            }
        ) {
            BeamDetailsSheetContent(
                b = b,
                onClose = { selectedBeam = null }
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Keep NowZone if you want it — but slim later.
        // For now we can keep it; it's not scroll-nested anymore.
        NowZone(
            now = uiState.now,
            onStartFlow = onStartFlow,
            onScheduleBeamClick = onScheduleBeamClick
        )

        val beamsCountLabel = when (uiState.viewMode) {
            AtlasViewMode.DAY -> {
                val n = uiState.dayPlan.beamsCount
                if (n == 1) "1 beam ⭐" else "$n beams ⭐"
            }
            AtlasViewMode.WEEK -> "Week"
            AtlasViewMode.MONTH -> "Month"
        }
        val canGoPrev = uiState.minSelectableDayStartMs?.let { uiState.selectedDayStartMs > it } ?: true

        AtlasHeader(
            mode = uiState.viewMode,
            dayStartMs = uiState.selectedDayStartMs,
            beamsCountLabel = beamsCountLabel,
            canGoPrev = canGoPrev,
            onSelectMode = onSelectMode,
            onPrev = onPrevDay,
            onNext = onNextDay,
            onToday = onToday
        )

        // Content
        // Content
        when (uiState.viewMode) {
            AtlasViewMode.DAY -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)           // ✅ prevents overlap with NowZone
                ) {
                    DayAgendaTimeline(
                        dayPlan = uiState.dayPlan,
                        onAdvanceDay = { delta -> onAdvanceDay(delta) }, // whatever you already do
                        onBeamClick = { beam -> selectedBeam = beam }    // ✅ add this
                    )
                }
            }
            AtlasViewMode.WEEK -> { /* ... */ }
            AtlasViewMode.MONTH -> { /* ... */ }
        }
    }
}
