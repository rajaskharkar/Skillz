import Foundation

enum SuggestedRoutesCatalog {
    static let routes: [SuggestedRoute] = [
        route(
            "deep_work_launch", "Deep Work Launch", "Start serious work without drifting.", "Focus",
            [("Clear the Desk", "Focus", 10, false), ("Define the Target", "Planning", 10, false),
             ("Deep Work Block", "Work", 45, true), ("Capture Next Action", "Planning", 10, false)]
        ),
        route(
            "learn_and_lock_in", "Learn and Lock In", "Turn study into retained knowledge.", "Learning",
            [("Read or Watch Lesson", "Learning", 30, false), ("Practice Actively", "Practice", 30, true),
             ("Write Notes", "Notes", 15, false), ("Recall From Memory", "Learning", 15, false)]
        ),
        route(
            "ship_one_thing", "Ship One Thing", "Finish one tangible piece before momentum fades.", "Build",
            [("Pick the Smallest Shippable Piece", "Planning", 15, false), ("Build the Core", "Build", 60, true),
             ("Polish the Edge", "Craft", 20, false), ("Publish or Commit", "Ship", 10, false)]
        ),
        route(
            "reset_room_reset_mind", "Reset the Room, Reset the Mind",
            "Recover control of your space and attention.", "Reset",
            [("Clear Visible Clutter", "Home", 15, false), ("Reset One Surface", "Home", 10, false),
             ("Prep the Next Task", "Planning", 10, false), ("Soft Reflection", "Reflection", 10, false)]
        ),
        route(
            "evening_closeout", "Evening Closeout", "End the day with fewer open loops.", "Evening",
            [("Review What Moved", "Reflection", 10, false), ("Capture Unfinished Loops", "Notes", 10, false),
             ("Choose Tomorrow’s First Flow", "Planning", 10, false), ("Wind Down", "Wellness", 5, false)]
        ),
        route(
            "creative_forge", "Creative Forge", "Shape raw sparks into something you can return to.", "Creative",
            [("Gather Sparks", "Creative", 15, false), ("Make the Rough Version", "Creative", 45, true),
             ("Refine One Section", "Craft", 20, false), ("Save the Next Thread", "Notes", 10, false)]
        ),
        route(
            "body_before_battle", "Body Before Battle", "Prepare your body before intense focus.", "Prep",
            [("Walk or Warm Up", "Fitness", 15, false), ("Hydrate", "Wellness", 5, false),
             ("Quick Reset", "Focus", 5, false), ("Begin Focus Block", "Work", 10, true)]
        )
    ]

    private static func route(
        _ id: String,
        _ title: String,
        _ subtitle: String,
        _ category: String,
        _ steps: [(String, String, Int, Bool)]
    ) -> SuggestedRoute {
        SuggestedRoute(
            id: id,
            title: title,
            subtitle: subtitle,
            category: category,
            approximateMinutes: steps.reduce(0) { $0 + $1.2 },
            steps: steps.map {
                SuggestedRouteStep(
                    title: $0.0,
                    journeyName: $0.1,
                    targetMinutes: $0.2,
                    launchWithSurge: $0.3
                )
            }
        )
    }
}
