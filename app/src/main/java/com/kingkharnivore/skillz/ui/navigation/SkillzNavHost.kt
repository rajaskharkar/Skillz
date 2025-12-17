package com.kingkharnivore.skillz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.kingkharnivore.skillz.ui.skills.FlowScreen
import com.kingkharnivore.skillz.ui.skills.SkillzHomeScreen
import com.kingkharnivore.skillz.ui.viewmodel.FlowViewModel

@Composable
fun SkillzNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val focusVm: FlowViewModel = hiltViewModel()
    val ongoing by focusVm.ongoingSession.collectAsState()
    val isFocusModeOn = ongoing?.isInFlowMode == true

    NavHost(
        navController = navController,
        startDestination = SkillzDestinations.HOME_SCREEN,
        modifier = modifier
    ) {
        // --- HOME (SkillzHomeScreen: SessionList + Notepad pager) ---
        composable(SkillzDestinations.HOME_SCREEN) {

            SkillzHomeScreen(
                onSessionClick = { /* you can hook this up later if you add details */ },
                onAddSessionClick = {
                    navController.navigate(SkillzDestinations.ADD_SKILL)
                },
                onGoToActiveSession = {
                    navController.navigate(SkillzDestinations.ADD_SKILL)
                },
                isFlowModeOn = isFocusModeOn
            )
        }

        // --- Add Skill Screen ---
        composable(
            SkillzDestinations.ADD_SKILL,
            deepLinks = listOf(navDeepLink { uriPattern = "skillz://flow" })
        ) {
            val addSessionViewModel: FlowViewModel = hiltViewModel()
            FlowScreen(
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

