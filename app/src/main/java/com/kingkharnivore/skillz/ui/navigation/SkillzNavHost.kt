package com.kingkharnivore.skillz.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.kingkharnivore.skillz.ui.screen.ScheduleBeamScreen
import com.kingkharnivore.skillz.ui.screen.SkillzHomeScreen
import com.kingkharnivore.skillz.ui.screen.flow.FlowScreen
import com.kingkharnivore.skillz.ui.screen.help.HelpScreen
import com.kingkharnivore.skillz.ui.screen.story.pulse.PulseScreen
import com.kingkharnivore.skillz.viewmodel.FlowViewModel
import com.kingkharnivore.skillz.viewmodel.ScheduleBeamViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel

@Composable
fun SkillzNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val focusVm: FlowViewModel = hiltViewModel()
    val storyViewModel: StoryViewModel = hiltViewModel()
    val ongoing by focusVm.ongoingSession.collectAsState()
    val isFocusModeOn = ongoing?.isInFlowMode == true

    NavHost(
        navController = navController,
        startDestination = SkillzDestinations.HOME_SCREEN,
        modifier = modifier
    ) {
        composable(SkillzDestinations.HOME_SCREEN) {
            SkillzHomeScreen(
                onSessionClick = { /* later */ },
                onAddSessionClick = {
                    navController.navigate(SkillzDestinations.addSkillRoute())
                },
                onAddPulseClick = {
                    navController.navigate(SkillzDestinations.ADD_PULSE_ROUTE)
                },
                onScheduleBeamClick = {
                    navController.navigate(SkillzDestinations.SCHEDULE_BEAM)
                },
                onStartFlowFromActiveBeam = { journeyName ->
                    navController.navigate(
                        SkillzDestinations.addSkillRoute(prefillJourney = journeyName)
                    )
                },
                onGoToActiveSession = {
                    navController.navigate(SkillzDestinations.addSkillRoute())
                },
                isFlowModeOn = isFocusModeOn
            )
        }

        composable(
            route = SkillzDestinations.ADD_SKILL_ROUTE,
            arguments = listOf(
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PREFILL_JOURNEY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "skillz://flow" }
            )
        ) {
            val addSessionViewModel: FlowViewModel = hiltViewModel()

            FlowScreen(
                viewModel = addSessionViewModel,
                onDone = { popToHome(navController) },
                onCancel = { popToHome(navController) }
            )
        }

        composable(SkillzDestinations.ADD_PULSE_ROUTE) {
            PulseScreen(
                viewModel = storyViewModel,
                isFlowStateActive = isFocusModeOn,
                onDone = { popToHome(navController) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(SkillzDestinations.SCHEDULE_BEAM) {
            val vm: ScheduleBeamViewModel = hiltViewModel()
            ScheduleBeamScreen(
                vm = vm,
                onDone = { popToHome(navController) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("help") {
            val uiState by storyViewModel.uiState.collectAsState()

            HelpScreen(
                uiState = uiState,
                onToggleShowScoreUi = storyViewModel::setShowScoreUi,
                onToggleCalmMode = storyViewModel::setCalmMode,
                modifier = Modifier.fillMaxSize()
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