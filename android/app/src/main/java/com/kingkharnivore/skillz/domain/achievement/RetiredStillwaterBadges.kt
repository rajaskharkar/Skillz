package com.kingkharnivore.skillz.domain.achievement

/** Retired presentation and earning rules. Historical rows are deliberately never deleted. */
object RetiredStillwaterBadges {
    private val landIds = setOf(
        "land_stillwater_first", "land_stillwater_variety", "land_five_habitats",
        "land_rare_first", "land_mythic_first", "land_pasture_first", "land_glade_first",
        "land_oasis_first", "land_ravine_first", "land_sanctuary_first"
    )
    fun isCollection(id: String): Boolean = id.startsWith("stillwater_") ||
        id in setOf("collection_stillwater", "collection_sea_stillwater", "collection_land_stillwater")
    fun isBadge(id: String): Boolean = id in landIds || id.startsWith("stillwater_") ||
        id.startsWith("collection_stillwater_") || id.startsWith("collection_sea_stillwater_") ||
        id.startsWith("collection_land_stillwater_")
}
