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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.paths.PathsScreen
import com.kingkharnivore.skillz.ui.screen.story.StoryScreen
import com.kingkharnivore.skillz.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.viewmodel.PathsViewModel
import com.kingkharnivore.skillz.domain.anchor.AnchorMode
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import com.kingkharnivore.skillz.viewmodel.anchor.AnchorSettingsUiState
import kotlinx.coroutines.launch

const val PAGE_STORY = 0
const val PAGE_PATHS = 1
const val PAGE_NOTEPAD = 2
const val PAGE_HELP = 3
const val TOTAL_HOME_PAGES = 4

@Composable
fun SkillzHomeScreen(
    onSessionClick: (Long) -> Unit,
    skillzViewModel: StoryViewModel = hiltViewModel(),
    notepadViewModel: NotepadViewModel = hiltViewModel(),
    pathsViewModel: PathsViewModel = hiltViewModel(),
    onAddSessionClick: () -> Unit,
    onAddPulseClick: () -> Unit,
    onOpenPlannedFlow: (title: String, tagName: String?, isSoftMode: Boolean) -> Unit,
    onPlanArcClick: () -> Unit,
    onOpenArc: (Long) -> Unit,
    onOpenSuggestedRoute: (String) -> Unit,
    onGoToActiveSession: () -> Unit,
    isFlowModeOn: Boolean,
    onOpenShell: () -> Unit,
    anchorState: AnchorSettingsUiState = AnchorSettingsUiState(),
    onToggleAnchor: (Boolean) -> Unit = {},
    onAnchorModeSelected: (AnchorMode) -> Unit = {},
    onManageAnchorApps: () -> Unit = {},
    onEnableAnchorUsageAccess: () -> Unit = {},
    onTestAnchor: () -> Unit = {}
) {
    val notepadText by notepadViewModel.notepadText.collectAsStateWithLifecycle()

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
                if (page in 0 until TOTAL_HOME_PAGES) {
                    scope.launch { pagerState.animateScrollToPage(page) }
                }
            }
        )
    ) {
        Scaffold(
            topBar = { SkillzTopAppBar(onOpenShell = onOpenShell) }
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
                        PAGE_STORY -> StoryScreen(
                            viewModel = skillzViewModel,
                            onAddSessionClick = onAddSessionClick,
                            onAddPulseClick = onAddPulseClick,
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
                            val storyUiState by skillzViewModel.uiState.collectAsStateWithLifecycle()

                            HelpScreen(
                                uiState = storyUiState,
                                selectedLanguageTag = storyUiState.appLanguageTag,
                                onToggleShowScoreUi = skillzViewModel::setShowScoreUi,
                                onToggleCalmMode = skillzViewModel::setCalmMode,
                                onSetAppLanguage = skillzViewModel::setAppLanguage,
                                anchorState = anchorState,
                                onToggleAnchor = onToggleAnchor,
                                onAnchorModeSelected = onAnchorModeSelected,
                                onManageAnchorApps = onManageAnchorApps,
                                onEnableUsageAccess = onEnableAnchorUsageAccess,
                                onTestAnchor = onTestAnchor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}