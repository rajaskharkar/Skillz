package com.kingkharnivore.skillz.utils.shell

enum class ChestSortOption(val key: String) {
    Level("level"),
    Recent("recent"),
    NewestArrival("newest_arrival"),
    OldestArrival("oldest_arrival"),
    Alphabetical("alphabetical"),
    Value("value"),
    Count("count"),
    ClosestToMastery("closest_to_mastery"),
    SpeciesMasteryCount("species_mastery_count");

    companion object {
        fun fromKey(key: String?): ChestSortOption = entries.firstOrNull { it.key == key } ?: Level
    }
}

enum class ChestFilterOption(val key: String) {
    All("all"), ClosestToMastery("closest"), Mastered("mastered"), NotMastered("not_mastered"),
    TrackedCollector("tracked_collector"), TrackedCompletionist("tracked_completionist"),
    SunlitReef("sunlit_reef"), DeeperReef("deeper_reef"), OpenBlue("open_blue"), GreatBlue("great_blue"),
    Fishbowl("fishbowl"), Aquarium("aquarium"), Pond("pond"), Lake("lake");
    companion object { fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: All }
}
