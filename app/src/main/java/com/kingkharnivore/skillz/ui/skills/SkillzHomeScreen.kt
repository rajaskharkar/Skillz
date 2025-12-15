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
import com.kingkharnivore.skillz.ui.viewmodel.NotepadViewModel
import com.kingkharnivore.skillz.ui.viewmodel.SkillListViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkillzHomeScreen(
    onSessionClick: (Long) -> Unit,
    skillzViewModel: SkillListViewModel = hiltViewModel(),
    notepadViewModel: NotepadViewModel = hiltViewModel(),
    onAddSessionClick: () -> Unit,
    onGoToActiveSession: () -> Unit,
    isFocusModeOn: Boolean
) {
    val uiState by skillzViewModel.uiState.collectAsState()
    val notepadText by notepadViewModel.notepadText.collectAsState()
    val listState = rememberLazyListState()

    // 0 = Sessions (default), 1 = Notepad
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            // HOME page → Session list
            0 -> {
                SkillListScreen(
                    viewModel = skillzViewModel,
                    onAddSessionClick = onAddSessionClick,
                    onSessionClick = { sessionId -> println("Clicked session: $sessionId") },
                    onGoToActiveSession = onGoToActiveSession,
                    isFocusModeOn
                )
            }

            // Swipe LEFT → Notepad (Skratchpad)
            1 -> {
                NotepadScreen(
                    text = notepadText,
                    onTextChange = { newText ->
                        notepadViewModel.onTextChanged(newText)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


