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
    All("all"), Sea("sea"), Land("land"), Red("red"), ClosestToMastery("closest"), Mastered("mastered"), NotMastered("not_mastered"),
    NeededForTrackedBadges("needed_for_tracked_badges"),
    SunlitReef("sunlit_reef"), DeeperReef("deeper_reef"), OpenBlue("open_blue"), GreatBlue("great_blue"),
    Fishbowl("fishbowl"), Aquarium("aquarium"), Pond("pond"), Lake("lake"),
    GoldenFields("golden_fields"), AncientWoods("ancient_woods"), OpenSands("open_sands"),
    HighPeaks("high_peaks"), GreatWild("great_wild"),
    Pasture("pasture"), Glade("glade"), Oasis("oasis"), Ravine("ravine"), Sanctuary("sanctuary"),
    Triassic("triassic"), Jurassic("jurassic"), Cretaceous("cretaceous");

    val isProgress: Boolean get() = this in listOf(ClosestToMastery, Mastered, NotMastered, NeededForTrackedBadges)

    fun matchesEnvironment(creature: CreatureDefinition): Boolean = when (this) {
        All -> true
        Sea -> creature.realm == CreatureRealm.SEA
        Land -> creature.realm == CreatureRealm.LAND
        Red -> creature.realm == CreatureRealm.RED
        else -> this == environmentOf(creature)
    }
    companion object { fun fromKey(key: String?) = when (key) {
        "tracked_collector", "tracked_mastery", "tracked_completionist" -> NeededForTrackedBadges
        else -> entries.firstOrNull { it.key == key } ?: All
    } }
}

/** Two independent dimensions, including read-through compatibility for the former single filter. */
data class ChestFilters(
    val environment: ChestFilterOption = ChestFilterOption.All,
    val progress: ChestFilterOption = ChestFilterOption.All
) {
    fun withProgress(value: ChestFilterOption): ChestFilters =
        if (value == ChestFilterOption.All || value.isProgress) copy(progress = value)
        else copy(environment = value) // Older callers still select environments through this API.

    fun withEnvironment(value: ChestFilterOption): ChestFilters =
        copy(environment = value.takeUnless { it.isProgress } ?: ChestFilterOption.All)

    companion object {
        fun fromKeys(environmentKey: String?, legacyFilterKey: String?): ChestFilters {
            val legacy = ChestFilterOption.fromKey(legacyFilterKey)
            val environment = if (environmentKey != null) ChestFilterOption.fromKey(environmentKey)
                else legacy.takeUnless { it.isProgress } ?: ChestFilterOption.All
            return ChestFilters(environment.takeUnless { it.isProgress } ?: ChestFilterOption.All,
                legacy.takeIf { it.isProgress } ?: ChestFilterOption.All)
        }
    }
}

/** Exact home environment; Stillwater habitats are distinct from their parent geography. */
fun environmentOf(creature: CreatureDefinition): ChestFilterOption {
    val key = when (creature.sourceType) {
        CreatureSourceType.STILLWATER -> StillwaterCatalog.byId[creature.creatureId]?.vessel?.name
        CreatureSourceType.RESTORATIVE_LAND -> creature.restorativeHabitat?.name
        else -> creature.zone.name
    }
    return ChestFilterOption.fromKey(key?.lowercase(java.util.Locale.ROOT))
}
