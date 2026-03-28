package com.kingkharnivore.skillz.ui.screen

import androidx.activity.compose.BackHandler
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
import com.kingkharnivore.skillz.ui.screen.atlas.AtlasScreen
import com.kingkharnivore.skillz.ui.screen.help.HelpScreen
import com.kingkharnivore.skillz.ui.screen.story.StoryScreen
import com.kingkharnivore.skillz.viewmodel.AtlasViewModel
import com.kingkharnivore.skillz.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

private const val PAGE_ATLAS = 0
private const val PAGE_STORY = 1
private const val PAGE_NOTEPAD = 2
private const val PAGE_HELP = 3

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SkillzHomeScreen(
    onSessionClick: (Long) -> Unit,
    skillzViewModel: StoryViewModel = hiltViewModel(),
    notepadViewModel: NotepadViewModel = hiltViewModel(),
    atlasViewModel: AtlasViewModel = hiltViewModel(),
    onAddSessionClick: () -> Unit,
    onScheduleBeamClick: () -> Unit,
    onStartFlowFromActiveBeam: (String) -> Unit,
    onGoToActiveSession: () -> Unit,
    isFlowModeOn: Boolean
) {
    val notepadText by notepadViewModel.notepadText.collectAsState()
    val docFont by notepadViewModel.notepadDocFont.collectAsState()
    val atlasState by atlasViewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = PAGE_STORY,
        pageCount = { 4 }
    )
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage != PAGE_STORY) {
        scope.launch {
            pagerState.animateScrollToPage(PAGE_STORY)
        }
    }

    CompositionLocalProvider(
        LocalSkillzHomeNav provides SkillzHomeNavState(
            currentPage = pagerState.currentPage,
            onSelectPage = { page ->
                scope.launch { pagerState.animateScrollToPage(page) }
            }
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
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        PAGE_ATLAS -> AtlasScreen(
                            uiState = atlasState,
                            onStartFlow = onStartFlowFromActiveBeam,
                            onSelectMode = atlasViewModel::setViewMode,
                            onPrevDay = { atlasViewModel.shiftSelectedDay(-1) },
                            onNextDay = { atlasViewModel.shiftSelectedDay(+1) },
                            onToday = { atlasViewModel.goToToday() },
                            onAdvanceDay = { delta -> atlasViewModel.shiftSelectedDay(delta) },
                            onScheduleBeamClick = onScheduleBeamClick,
                        )

                        PAGE_STORY -> StoryScreen(
                            viewModel = skillzViewModel,
                            onAddSessionClick = onAddSessionClick,
                            onScheduleBeamClick = onScheduleBeamClick,
                            onSessionClick = onSessionClick,
                            onGoToActiveSession = onGoToActiveSession,
                            isFocusModeOn = isFlowModeOn
                        )

                        PAGE_NOTEPAD -> NotepadScreen(
                            text = notepadText,
                            onTextChange = notepadViewModel::onTextChanged,
                            modifier = Modifier.fillMaxSize()
                        )

                        PAGE_HELP -> {
                            val storyUiState by skillzViewModel.uiState.collectAsState()
                            HelpScreen(
                                uiState = storyUiState,
                                onToggleShowScoreUi = skillzViewModel::setShowScoreUi,
                                onToggleCalmMode = skillzViewModel::setCalmMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}