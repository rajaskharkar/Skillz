package com.kingkharnivore.skillz.ui.screen.paths.suggested

import com.kingkharnivore.skillz.model.ui.SuggestedRouteStepUiModel
import com.kingkharnivore.skillz.model.ui.SuggestedRouteUiModel

object SuggestedRoutesCatalog {

    val routes: List<SuggestedRouteUiModel> = listOf(
        SuggestedRouteUiModel(
            id = "deep_work_launch",
            title = "Deep Work Launch",
            subtitle = "Start serious work without drifting.",
            category = "Focus",
            approxMinutes = 75,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Clear the Desk",
                    tagName = "Focus",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Define the Target",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Deep Work Block",
                    tagName = "Work",
                    isSoftMode = false,
                    targetMinutes = 45,
                    launchWithSurge = true
                ),
                SuggestedRouteStepUiModel(
                    title = "Capture Next Action",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "learn_and_lock_in",
            title = "Learn and Lock In",
            subtitle = "Turn study into retained knowledge.",
            category = "Learning",
            approxMinutes = 90,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Read or Watch Lesson",
                    tagName = "Learning",
                    isSoftMode = false,
                    targetMinutes = 30,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Practice Actively",
                    tagName = "Practice",
                    isSoftMode = false,
                    targetMinutes = 30,
                    launchWithSurge = true
                ),
                SuggestedRouteStepUiModel(
                    title = "Write Notes",
                    tagName = "Notes",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Recall From Memory",
                    tagName = "Learning",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "ship_one_thing",
            title = "Ship One Thing",
            subtitle = "Finish one tangible piece before momentum fades.",
            category = "Build",
            approxMinutes = 105,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Pick the Smallest Shippable Piece",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Build the Core",
                    tagName = "Build",
                    isSoftMode = false,
                    targetMinutes = 60,
                    launchWithSurge = true
                ),
                SuggestedRouteStepUiModel(
                    title = "Polish the Edge",
                    tagName = "Craft",
                    isSoftMode = false,
                    targetMinutes = 20,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Publish or Commit",
                    tagName = "Ship",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "reset_room_reset_mind",
            title = "Reset the Room, Reset the Mind",
            subtitle = "Recover control of your space and attention.",
            category = "Reset",
            approxMinutes = 45,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Clear Visible Clutter",
                    tagName = "Home",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Reset One Surface",
                    tagName = "Home",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Prep the Next Task",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Soft Reflection",
                    tagName = "Reflection",
                    isSoftMode = true,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "evening_closeout",
            title = "Evening Closeout",
            subtitle = "End the day with fewer open loops.",
            category = "Evening",
            approxMinutes = 35,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Review What Moved",
                    tagName = "Reflection",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Capture Unfinished Loops",
                    tagName = "Notes",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Choose Tomorrow’s First Flow",
                    tagName = "Planning",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Wind Down",
                    tagName = "Wellness",
                    isSoftMode = true,
                    targetMinutes = 5,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "creative_forge",
            title = "Creative Forge",
            subtitle = "Shape raw sparks into something you can return to.",
            category = "Creative",
            approxMinutes = 90,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Gather Sparks",
                    tagName = "Creative",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Make the Rough Version",
                    tagName = "Creative",
                    isSoftMode = false,
                    targetMinutes = 45,
                    launchWithSurge = true
                ),
                SuggestedRouteStepUiModel(
                    title = "Refine One Section",
                    tagName = "Craft",
                    isSoftMode = false,
                    targetMinutes = 20,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Save the Next Thread",
                    tagName = "Notes",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = false
                )
            )
        ),
        SuggestedRouteUiModel(
            id = "body_before_battle",
            title = "Body Before Battle",
            subtitle = "Prepare your body before intense focus.",
            category = "Prep",
            approxMinutes = 35,
            steps = listOf(
                SuggestedRouteStepUiModel(
                    title = "Walk or Warm Up",
                    tagName = "Fitness",
                    isSoftMode = false,
                    targetMinutes = 15,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Hydrate",
                    tagName = "Wellness",
                    isSoftMode = false,
                    targetMinutes = 5,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Quick Reset",
                    tagName = "Focus",
                    isSoftMode = false,
                    targetMinutes = 5,
                    launchWithSurge = false
                ),
                SuggestedRouteStepUiModel(
                    title = "Begin Focus Block",
                    tagName = "Work",
                    isSoftMode = false,
                    targetMinutes = 10,
                    launchWithSurge = true
                )
            )
        )
    )

    fun getById(id: String): SuggestedRouteUiModel? =
        routes.firstOrNull { it.id == id }
}
