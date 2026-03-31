@file:OptIn(ExperimentalFoundationApi::class)

package com.kingkharnivore.skillz.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.atlas.AtlasScreen
import com.kingkharnivore.skillz.ui.screen.paths.PathsScreen
import com.kingkharnivore.skillz.ui.screen.story.StoryScreen
import com.kingkharnivore.skillz.viewmodel.AtlasViewModel
import com.kingkharnivore.skillz.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.viewmodel.PathsViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

private const val PAGE_ATLAS = 0
private const val PAGE_STORY = 1
private const val PAGE_PATHS = 2
private const val PAGE_NOTEPAD = 3
private const val PAGE_HELP = 4
private const val TOTAL_HOME_PAGES = 5

@Composable
fun SkillzHomeScreen(
    onSessionClick: (Long) -> Unit,
    skillzViewModel: StoryViewModel = hiltViewModel(),
    notepadViewModel: NotepadViewModel = hiltViewModel(),
    atlasViewModel: AtlasViewModel = hiltViewModel(),
    pathsViewModel: PathsViewModel = hiltViewModel(),
    onAddSessionClick: () -> Unit,
    onAddPulseClick: () -> Unit,
    onScheduleBeamClick: () -> Unit,
    onStartFlowFromActiveBeam: (String) -> Unit,
    onOpenPlannedFlow: (title: String, tagName: String?, isSoftMode: Boolean) -> Unit,
    onPlanArcClick: () -> Unit,
    onEditArc: (Long) -> Unit,
    onOpenArc: (Long) -> Unit,
    onOpenSuggestedRoute: (String) -> Unit,
    onGoToActiveSession: () -> Unit,
    isFlowModeOn: Boolean
) {
    val notepadText by notepadViewModel.notepadText.collectAsState()
    val atlasState by atlasViewModel.uiState.collectAsState()

    val atlasPageLabel = stringResource(R.string.home_page_atlas)
    val storyPageLabel = stringResource(R.string.home_page_story)
    val pathsPageLabel = stringResource(R.string.home_page_paths)
    val notepadPageLabel = stringResource(R.string.home_page_notepad)
    val helpPageLabel = stringResource(R.string.home_page_help)
    val pagerA11yLabel = stringResource(R.string.home_pager_a11y)

    val pagerState = rememberPagerState(
        initialPage = PAGE_STORY,
        pageCount = { TOTAL_HOME_PAGES }
    )
    val scope = rememberCoroutineScope()

    val currentPageLabel = when (pagerState.currentPage) {
        PAGE_ATLAS -> atlasPageLabel
        PAGE_STORY -> storyPageLabel
        PAGE_PATHS -> pathsPageLabel
        PAGE_NOTEPAD -> notepadPageLabel
        PAGE_HELP -> helpPageLabel
        else -> storyPageLabel
    }

    val pagerStateLabel = stringResource(
        R.string.home_pager_state,
        currentPageLabel,
        pagerState.currentPage + 1,
        TOTAL_HOME_PAGES
    )

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
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = pagerA11yLabel
                            stateDescription = pagerStateLabel
                            collectionInfo = androidx.compose.ui.semantics.CollectionInfo(
                                rowCount = 1,
                                columnCount = TOTAL_HOME_PAGES
                            )
                            scrollToIndex { index ->
                                if (index in 0 until TOTAL_HOME_PAGES) {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                    true
                                } else {
                                    false
                                }
                            }
                        },
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        PAGE_ATLAS -> AtlasScreen(
                            uiState = atlasState,
                            onStartFlow = onStartFlowFromActiveBeam,
                            onSelectMode = atlasViewModel::setViewMode,
                            onPrevPeriod = { atlasViewModel.shiftSelectedPeriod(-1) },
                            onNextPeriod = { atlasViewModel.shiftSelectedPeriod(+1) },
                            onToday = { atlasViewModel.goToToday() },
                            onSelectDay = atlasViewModel::selectDay,
                            onScheduleBeamClick = onScheduleBeamClick,
                        )

                        PAGE_STORY -> StoryScreen(
                            viewModel = skillzViewModel,
                            onAddSessionClick = onAddSessionClick,
                            onAddPulseClick = onAddPulseClick,
                            onScheduleBeamClick = onScheduleBeamClick,
                            onSessionClick = onSessionClick,
                            onGoToActiveSession = onGoToActiveSession,
                            isFlowStateActive = isFlowModeOn
                        )

                        PAGE_PATHS -> PathsScreen(
                            viewModel = pathsViewModel,
                            onPlanFlowClick = { },
                            onPlanArcClick = onPlanArcClick,
                            onOpenFlowPlan = onOpenPlannedFlow,
                            onOpenSuggestedRoute = onOpenSuggestedRoute,
                            onOpenArc = onOpenArc
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
                                selectedLanguageTag = storyUiState.appLanguageTag,
                                onToggleShowScoreUi = skillzViewModel::setShowScoreUi,
                                onToggleCalmMode = skillzViewModel::setCalmMode,
                                onSetAppLanguage = skillzViewModel::setAppLanguage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}