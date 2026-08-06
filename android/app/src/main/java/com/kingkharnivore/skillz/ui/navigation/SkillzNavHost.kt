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
import com.kingkharnivore.skillz.ui.screen.HelpScreen
import com.kingkharnivore.skillz.ui.screen.HelpSection
import com.kingkharnivore.skillz.ui.screen.SkillzHomeScreen
import com.kingkharnivore.skillz.ui.screen.flow.FlowScreen
import com.kingkharnivore.skillz.ui.screen.flow.ShellNavigationMode
import com.kingkharnivore.skillz.ui.screen.paths.arc.ArcDetailScreen
import com.kingkharnivore.skillz.ui.screen.shell.ShellRootScreen
import com.kingkharnivore.skillz.ui.screen.paths.arc.PlanArcScreen
import com.kingkharnivore.skillz.ui.screen.paths.suggested.SuggestedRouteDetailScreen
import com.kingkharnivore.skillz.ui.screen.paths.suggested.SuggestedRoutesCatalog
import com.kingkharnivore.skillz.ui.screen.story.pulse.PulseScreen
import com.kingkharnivore.skillz.viewmodel.ArcDetailViewModel
import com.kingkharnivore.skillz.viewmodel.FlowViewModel
import com.kingkharnivore.skillz.viewmodel.health.HealthSettingsViewModel
import com.kingkharnivore.skillz.viewmodel.PlanArcViewModel
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import com.kingkharnivore.skillz.viewmodel.SuggestedRouteDetailViewModel

@Composable
fun SkillzNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val focusVm: FlowViewModel = hiltViewModel()
    val storyViewModel: StoryViewModel = hiltViewModel()
    val ongoing by focusVm.ongoingSession.collectAsState()
    val isFocusModeOn = ongoing?.isInFlowMode == true
    val hasOngoingFlow = ongoing != null

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
                onOpenPlannedFlow = { title, tagName, isSoftMode ->
                    navController.navigate(
                        SkillzDestinations.addSkillRoute(
                            prefillJourney = tagName,
                            prefillTitle = title,
                            prefillSoftMode = isSoftMode
                        )
                    )
                },
                onPlanArcClick = {
                    navController.navigate(SkillzDestinations.planArcRoute())
                },
                onOpenArc = { arcPlanId ->
                    navController.navigate(
                        SkillzDestinations.arcDetailRoute(arcPlanId)
                    )
                },
                onOpenSuggestedRoute = { routeId ->
                    navController.navigate(
                        SkillzDestinations.suggestedRouteDetailRoute(routeId)
                    )
                },
                onGoToActiveSession = {
                    navController.navigate(SkillzDestinations.addSkillRoute())
                },
                isFlowModeOn = isFocusModeOn,
                onOpenShell = { navController.navigate(SkillzDestinations.SHELL) }
            )
        }

        composable(
            route = SkillzDestinations.ADD_SKILL_ROUTE,
            arguments = listOf(
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PREFILL_JOURNEY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PREFILL_TITLE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PREFILL_SOFT_MODE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(SkillzDestinations.ADD_SKILL_ARG_ORIGIN_PULSE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PLANNED_ARC_TITLE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PLANNED_ARC_STEP_INDEX) {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument(SkillzDestinations.ADD_SKILL_ARG_PLANNED_ARC_TOTAL_STEPS) {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "skillz://flow" }
            )
        ) {
            val addSessionViewModel: FlowViewModel = hiltViewModel()

            FlowScreen(
                viewModel = addSessionViewModel,
                onDone = { popToHome(navController) },
                onCancel = { popToHome(navController) },
                onOpenShell = { mode ->
                    when (mode) {
                        ShellNavigationMode.PreservePreparedFlow -> {
                            navController.navigate(SkillzDestinations.SHELL) { launchSingleTop = true }
                        }
                        ShellNavigationMode.RemoveCompletedFlow -> {
                            popToHome(navController)
                            navController.navigate(SkillzDestinations.SHELL) { launchSingleTop = true }
                        }
                    }
                }
            )
        }

        composable(
            route = SkillzDestinations.ARC_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(SkillzDestinations.ARC_DETAIL_ARG_ID) {
                    type = NavType.StringType
                }
            )
        ) {
            val vm: ArcDetailViewModel = hiltViewModel()
            ArcDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEditArc = { arcPlanId ->
                    navController.navigate(
                        SkillzDestinations.planArcRoute(editArcPlanId = arcPlanId)
                    )
                },
                onBeginArc = { payload ->
                    navController.navigate(
                        SkillzDestinations.addSkillRoute(
                            prefillJourney = payload.tagName,
                            prefillTitle = payload.title,
                            prefillSoftMode = payload.isSoftMode,
                            plannedArcTitle = payload.plannedArcTitle,
                            plannedArcStepIndex = payload.plannedArcStepIndex,
                            plannedArcTotalSteps = payload.plannedArcTotalSteps
                        )
                    )
                }
            )
        }

        composable(
            route = SkillzDestinations.PLAN_ARC_ROUTE,
            arguments = listOf(
                navArgument(SkillzDestinations.PLAN_ARC_ARG_EDIT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            val vm: PlanArcViewModel = hiltViewModel()
            PlanArcScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onDone = { popToHome(navController) }
            )
        }

        composable(
            route = SkillzDestinations.SUGGESTED_ROUTE_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(SkillzDestinations.SUGGESTED_ROUTE_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments
                ?.getString(SkillzDestinations.SUGGESTED_ROUTE_ID_ARG)
                .orEmpty()

            val route = SuggestedRoutesCatalog.getById(routeId)
            val vm: SuggestedRouteDetailViewModel = hiltViewModel()

            if (route != null) {
                SuggestedRouteDetailScreen(
                    route = route,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onDone = { popToHome(navController) },
                    onBeginArc = { payload ->
                        navController.navigate(
                            SkillzDestinations.addSkillRoute(
                                prefillJourney = payload.tagName,
                                prefillTitle = payload.title,
                                prefillSoftMode = payload.isSoftMode,
                                plannedArcTitle = payload.plannedArcTitle,
                                plannedArcStepIndex = payload.plannedArcStepIndex,
                                plannedArcTotalSteps = payload.plannedArcTotalSteps
                            )
                        )
                    }
                )
            }
        }

        composable(SkillzDestinations.SHELL) {
            ShellRootScreen(
                onBack = { navController.popBackStack() },
                isFlowActive = hasOngoingFlow,
                onLaunchFlowForJourney = { journeyName ->
                    navController.navigate(SkillzDestinations.addSkillRoute(prefillJourney = journeyName))
                },
                onLaunchFlowFromPulse = { pulseId, title, journeyName ->
                    navController.navigate(
                        SkillzDestinations.addSkillRoute(
                            prefillJourney = journeyName,
                            prefillTitle = title,
                            originPulseId = pulseId
                        )
                    )
                },
                onOpenActiveFlow = {
                    navController.navigate(SkillzDestinations.addSkillRoute())
                },
                onPlanArc = { navController.navigate(SkillzDestinations.planArcRoute()) },
                onMovementInfo = { navController.navigate("help?section=${HelpSection.MOVEMENT_BONUS.name}") }
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

        composable("help?section={section}") { entry ->
            val uiState by storyViewModel.uiState.collectAsState()
            val healthViewModel: HealthSettingsViewModel = hiltViewModel()

            HelpScreen(
                uiState = uiState,
                selectedLanguageTag = uiState.appLanguageTag,
                healthViewModel = healthViewModel,
                onToggleShowScoreUi = storyViewModel::setShowScoreUi,
                onToggleCalmMode = storyViewModel::setCalmMode,
                onSetAppLanguage = storyViewModel::setAppLanguage,
                modifier = Modifier.fillMaxSize(),
                initialSection = entry.arguments?.getString("section")
                    ?.let { runCatching { HelpSection.valueOf(it) }.getOrNull() }
                    ?: HelpSection.OVERVIEW
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
