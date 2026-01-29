package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kingkharnivore.skillz.ui.atlas.AtlasScreen
import com.kingkharnivore.skillz.ui.atlas.model.JourneyFilter
import com.kingkharnivore.skillz.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import com.kingkharnivore.skillz.viewmodel.atlas.AtlasViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkillzHomeScreen(
    onSessionClick: (Long) -> Unit,
    skillzViewModel: StoryViewModel = hiltViewModel(),
    notepadViewModel: NotepadViewModel = hiltViewModel(),
    atlasViewModel: AtlasViewModel = hiltViewModel(),
    onAddSessionClick: () -> Unit,
    onScheduleBeamClick: () -> Unit,
    onGoToActiveSession: () -> Unit,
    isFlowModeOn: Boolean
) {
    val storyUiState by skillzViewModel.uiState.collectAsState()
    val notepadText by notepadViewModel.notepadText.collectAsState()
    val atlasState by atlasViewModel.uiState.collectAsState()

    // Pages:
    // 0 = Atlas
    // 1 = Story (landing)
    // 2 = Notepad
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 3 }
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                AtlasScreen(
                    uiState = atlasState,
                    onFilterAll = { atlasViewModel.setJourneyFilter(JourneyFilter.All) },
                    onFilterJourney = { tagId -> atlasViewModel.setJourneyFilter(JourneyFilter.Only(tagId)) },
                    onStartFlow = onAddSessionClick, // or a dedicated start flow action
                    onGoToActiveFlow = onGoToActiveSession,
                    onZoomHours = { h -> atlasViewModel.setHorizonHours(h) },
                    onShiftHours = { delta -> atlasViewModel.shiftHorizonByHours(delta) },
                    onResetToNow = { atlasViewModel.resetHorizonToNow() }
                )
            }

            1 -> {
                StoryScreen(
                    viewModel = skillzViewModel,
                    onAddSessionClick = onAddSessionClick,
                    onScheduleBeamClick = onScheduleBeamClick,
                    onSessionClick = onSessionClick,
                    onGoToActiveSession = onGoToActiveSession,
                    isFocusModeOn = isFlowModeOn
                )
            }

            2 -> {
                NotepadScreen(
                    text = notepadText,
                    onTextChange = notepadViewModel::onTextChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
