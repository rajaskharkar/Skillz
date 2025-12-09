package com.kingkharnivore.skillz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kingkharnivore.skillz.ui.skills.FocusOnScreen
import com.kingkharnivore.skillz.ui.skills.SkillzHomeScreen
import com.kingkharnivore.skillz.ui.viewmodel.FocusOnViewModel

@Composable
fun SkillzNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = SkillzDestinations.HOME_SCREEN,
        modifier = modifier
    ) {

        // --- HOME (SkillzHomeScreen: SessionList + Notepad pager) ---
        composable(SkillzDestinations.HOME_SCREEN) {
            val focusVm: FocusOnViewModel = hiltViewModel()
            val ongoing by focusVm.ongoingSession.collectAsState()

            val sessionId = ongoing?.id
            var hasNavigatedForSession by rememberSaveable(sessionId) {
                mutableStateOf(false)
            }

            LaunchedEffect(sessionId, ongoing?.isInFocusMode) {
                val inFocus = ongoing?.isInFocusMode == true
                if (!inFocus) {
                    // Not in focus mode (or no session): reset so next focus session can navigate again
                    hasNavigatedForSession = false
                    return@LaunchedEffect
                }

                // Only navigate ONCE per session
                if (!hasNavigatedForSession) {
                    hasNavigatedForSession = true
                    navController.navigate(SkillzDestinations.ADD_SKILL) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            SkillzHomeScreen(
                onSessionClick = { /* you can hook this up later if you add details */ },
                onAddSessionClick = {
                    navController.navigate(SkillzDestinations.ADD_SKILL)
                }
            )
        }

        // --- Add Skill Screen ---
        composable(SkillzDestinations.ADD_SKILL) {
            val addSessionViewModel: FocusOnViewModel = hiltViewModel()
            FocusOnScreen(
                viewModel = addSessionViewModel,
                onDone = { popToHome(navController) },
                onCancel = { popToHome(navController) }
            )
        }
    }
}

private fun popToHome(navController: NavHostController) {
    navController.popBackStack(
        route = SkillzDestinations.HOME_SCREEN,
        inclusive = false
    )
}

