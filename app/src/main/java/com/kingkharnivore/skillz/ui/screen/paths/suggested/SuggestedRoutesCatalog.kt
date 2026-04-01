package com.kingkharnivore.skillz.ui.screen.paths.suggested

import com.kingkharnivore.skillz.model.ui.SuggestedRouteStepUiModel
import com.kingkharnivore.skillz.model.ui.SuggestedRouteUiModel

object SuggestedRoutesCatalog {

    val routes: List<SuggestedRouteUiModel> = listOf(
        SuggestedRouteUiModel(
            id = "morning_reset",
            title = "Morning Reset",
            subtitle = "Ease into the day with clarity and movement.",
            category = "Morning",
            approxMinutes = 25,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Hydrate and Wake Up",
                    tagName = "Wellness",
                    isSoftMode = false,
                    targetMinutes = 5,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Gentle Movement",
                    tagName = "Fitness",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Set Day Intention",
                    tagName = "Reflection",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "deep_work_launch",
            title = "Deep Work Launch",
            subtitle = "Clear friction and enter focused work cleanly.",
            category = "Focus",
            approxMinutes = 45,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Clear Desk",
                    tagName = "Focus",
                    isSoftMode = false,
                    targetMinutes = 5,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Review Priorities",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "First Focus Block",
                    tagName = "Work",
                    isSoftMode = false,
                    targetMinutes = 30,
                    launchWithSurge = true
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "evening_wind_down",
            title = "Evening Wind Down",
            subtitle = "Slow the nervous system before sleep.",
            category = "Evening",
            approxMinutes = 30,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Tidy Reset",
                    tagName = "Home",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Quiet Reflection",
                    tagName = "Reflection",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Sleep Prep",
                    tagName = "Wellness",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "creative_warmup",
            title = "Creative Warmup",
            subtitle = "Open the door before the real session begins.",
            category = "Creative",
            approxMinutes = 35,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Free Write or Improv",
                    tagName = "Creative",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Skill Drill",
                    tagName = "Practice",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = true
                ),
                SuggestedRouteStepUiModel(
                    title = "Main Creative Session",
                    tagName = "Creative",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = true
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "walk_and_reflect",
            title = "Walk and Reflect",
            subtitle = "Move the body and let thoughts settle into place.",
            category = "Recovery",
            approxMinutes = 30,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Go for a Walk",
                    tagName = "Wellness",
                    isSoftMode = false,
                    targetMinutes = 20,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Capture a Thought",
                    tagName = "Reflection",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "sunday_recovery",
            title = "Sunday Recovery",
            subtitle = "Reset the mind and space before the week begins.",
            category = "Weekend",
            approxMinutes = 40,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Reset One Space",
                    tagName = "Home",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Reflect on the Week",
                    tagName = "Reflection",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Light Plan for Tomorrow",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                )
            )
        )
    )

    fun getById(id: String): SuggestedRouteUiModel? =
        routes.firstOrNull { it.id == id }
}