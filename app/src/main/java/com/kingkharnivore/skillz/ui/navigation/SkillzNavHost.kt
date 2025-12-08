package com.kingkharnivore.skillz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.createGraph
import com.kingkharnivore.skillz.ui.viewmodel.AddSessionViewModel
import com.kingkharnivore.skillz.ui.skills.AddSkillScreen
import com.kingkharnivore.skillz.ui.viewmodel.FocusSessionViewModel
import com.kingkharnivore.skillz.ui.skills.SkillListScreen
import com.kingkharnivore.skillz.ui.viewmodel.SkillListViewModel

@Composable
fun SkillzNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val graph = navController.createGraph(
        startDestination = SkillzDestinations.SKILLS_LIST
    ) {
        // --- Skills List Screen ---
        composable(route = SkillzDestinations.SKILLS_LIST) {
            val skillListVm: SkillListViewModel = hiltViewModel()
            val focusVm: FocusSessionViewModel = hiltViewModel()
            val ongoing by focusVm.ongoingSession.collectAsState()

            // Track current route
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            // 🔑 Single effect that owns "go to ADD_SKILL if focus mode is active"
            LaunchedEffect(ongoing?.isInFocusMode, currentRoute) {
                if (
                    ongoing?.isInFocusMode == true               // must be in focus mode
                ) {
                    navController.navigate(SkillzDestinations.ADD_SKILL) {
                        restoreState = true
                    }
                }
                // If not in focus mode: do nothing; user can be wherever.
            }

            SkillListScreen(
                viewModel = skillListVm,
                onAddSessionClick = {
                    navController.navigate(SkillzDestinations.ADD_SKILL)
                },
                onSessionClick = { sessionId ->
                    println("Clicked session: $sessionId")

                    // Later you might do:
                    // navController.navigate(SkillzDestinations.sessionDetailRoute(sessionId))
                }
            )
        }

        // --- Add Skill Screen ---
        composable(route = SkillzDestinations.ADD_SKILL) {
            val addSessionViewModel: AddSessionViewModel = hiltViewModel()
            AddSkillScreen(
                viewModel = addSessionViewModel,
                // Pop everything above SKILLS_LIST (all ADD_SKILL screens), land on SKILLS_LIST
                onDone = { popToSkillsList(navController) },
                onCancel = { popToSkillsList(navController) }
            )
        }
    }

    NavHost(
        navController = navController,
        graph = graph,
        modifier = modifier
    )
}

private fun popToSkillsList(navController: NavHostController) {
    navController.popBackStack(
        route = SkillzDestinations.SKILLS_LIST,
        inclusive = false
    )
}
