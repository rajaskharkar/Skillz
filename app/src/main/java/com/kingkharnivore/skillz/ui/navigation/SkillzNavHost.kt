package com.kingkharnivore.skillz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.kingkharnivore.skillz.ui.skills.AddSessionViewModel
import com.kingkharnivore.skillz.ui.skills.AddSkillScreen
import com.kingkharnivore.skillz.ui.skills.SkillListScreen
import com.kingkharnivore.skillz.ui.skills.SkillListViewModel

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
            val viewModel: SkillListViewModel = hiltViewModel()

            SkillListScreen(
                viewModel = viewModel,
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
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }

    NavHost(
        navController = navController,
        graph = graph,
        modifier = modifier
    )
}