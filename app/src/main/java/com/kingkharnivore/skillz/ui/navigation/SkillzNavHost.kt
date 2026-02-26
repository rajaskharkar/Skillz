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
import com.kingkharnivore.skillz.ui.screen.ScheduleBeamScreen
import com.kingkharnivore.skillz.ui.screen.flow.FlowScreen
import com.kingkharnivore.skillz.ui.screen.SkillzHomeScreen
import com.kingkharnivore.skillz.viewmodel.FlowViewModel
import com.kingkharnivore.skillz.viewmodel.ScheduleBeamViewModel

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
        composable(SkillzDestinations.HOME_SCREEN) {
            SkillzHomeScreen(
                onSessionClick = { /* later */ },
                onAddSessionClick = { navController.navigate(SkillzDestinations.ADD_SKILL) },
                onScheduleBeamClick = { navController.navigate(SkillzDestinations.SCHEDULE_BEAM) },
                onGoToActiveSession = { navController.navigate(SkillzDestinations.ADD_SKILL) },
                isFlowModeOn = isFocusModeOn
            )
        }

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

        composable(SkillzDestinations.SCHEDULE_BEAM) {
            val vm: ScheduleBeamViewModel = hiltViewModel()
            ScheduleBeamScreen(
                vm = vm,
                onDone = { popToHome(navController) },
                onCancel = { navController.popBackStack() }
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

