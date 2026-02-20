package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kingkharnivore.skillz.ui.atlas.model.JourneyFilter
import com.kingkharnivore.skillz.ui.components.LocalSkillzHomeNav
import com.kingkharnivore.skillz.ui.components.SkillzHomeNavState
import com.kingkharnivore.skillz.ui.components.SkillzTopAppBar
import com.kingkharnivore.skillz.viewmodel.AtlasViewModel
import com.kingkharnivore.skillz.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val notepadText by notepadViewModel.notepadText.collectAsState()
    val atlasState by atlasViewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(
        LocalSkillzHomeNav provides SkillzHomeNavState(
            currentPage = pagerState.currentPage,
            onSelectPage = { page -> scope.launch { pagerState.animateScrollToPage(page) } }
        )
    ) {
        Scaffold(
            topBar = { SkillzTopAppBar() }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true // ✅ swipe between pages again
                ) { page ->
                    when (page) {
                        0 -> AtlasScreen(
                        uiState = atlasState,
                        onFilterAll = { atlasViewModel.setJourneyFilter(JourneyFilter.All) },
                        onFilterJourney = { tagId -> atlasViewModel.setJourneyFilter(JourneyFilter.Only(tagId)) },
                        onStartFlow = onAddSessionClick,
                        onGoToActiveFlow = onGoToActiveSession,

                        // ✅ new
                        onSelectMode = atlasViewModel::setViewMode,
                        onPrevDay = { atlasViewModel.shiftSelectedDay(-1) },
                        onNextDay = { atlasViewModel.shiftSelectedDay(+1) },
                        onToday = { atlasViewModel.goToToday() },
                        onAdvanceDay = { delta -> atlasViewModel.shiftSelectedDay(delta) }
                    )

                        1 -> StoryScreen(
                            viewModel = skillzViewModel,
                            onAddSessionClick = onAddSessionClick,
                            onScheduleBeamClick = onScheduleBeamClick,
                            onSessionClick = onSessionClick,
                            onGoToActiveSession = onGoToActiveSession,
                            isFocusModeOn = isFlowModeOn
                        )

                        2 -> NotepadScreen(
                            text = notepadText,
                            onTextChange = notepadViewModel::onTextChanged,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
