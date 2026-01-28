package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kingkharnivore.skillz.ui.beams.DailyBeamDataScreen
import com.kingkharnivore.skillz.viewmodel.DailyBeamViewModel
import com.kingkharnivore.skillz.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkillzHomeScreen(
    onSessionClick: (Long) -> Unit,
    skillzViewModel: StoryViewModel = hiltViewModel(),
    notepadViewModel: NotepadViewModel = hiltViewModel(),
    dailyBeamViewModel: DailyBeamViewModel = hiltViewModel(),
    onAddSessionClick: () -> Unit,
    onScheduleBeamClick: () -> Unit,
    onGoToActiveSession: () -> Unit,
    isFlowModeOn: Boolean
) {
    val uiState by skillzViewModel.uiState.collectAsState()
    val notepadText by notepadViewModel.notepadText.collectAsState()
    val beams by dailyBeamViewModel.beams.collectAsState()

    // Pages:
    // 0 = DailyBeamData
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
                DailyBeamDataScreen(beams = beams)
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


