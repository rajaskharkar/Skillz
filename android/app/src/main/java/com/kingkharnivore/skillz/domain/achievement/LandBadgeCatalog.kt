package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.utils.shell.*

enum class LandBadgeMetric { DISCOVERY, REALMS, ZONES, ENCOUNTER, LEVEL, MASTERY, ARC_DEPTH, TIGERS, FLAGSHIPS, ARCS, DRAWS, HABITATS, RESTORATIVE, RARE, MYTHIC, RELEASE, TRADE, MAIN, MASTERED_COLLECTIONS, MASTERED_SPECIES, SEA_DISCOVERY, VESSEL }
enum class LandBadgeMotif { FOOTPRINTS, EARTH, LEAF, LANDSCAPE, ENCOUNTER, GROWTH, CROWN, ROOTS, TIGERS, COMPANIONS, RETURN, DROPLEAF, RARE, MYTHIC, RELEASE, TRADE, SEA }

data class LandBadgeSpec(
    val id: String, val titleRes: Int, val descriptionRes: Int,
    val metric: LandBadgeMetric, val thresholds: List<Int>, val motif: LandBadgeMotif,
    val habitat: LandStillwaterHabitat? = null
) {
    val definition: AchievementBadgeDefinition get() = AchievementBadgeDefinition(
        id, when (metric) {
            LandBadgeMetric.ARC_DEPTH, LandBadgeMetric.TIGERS, LandBadgeMetric.ARCS, LandBadgeMetric.FLAGSHIPS -> BadgeFamily.ARC
            LandBadgeMetric.MASTERY, LandBadgeMetric.MASTERED_SPECIES, LandBadgeMetric.LEVEL -> BadgeFamily.MASTERY
            else -> BadgeFamily.COLLECTION
        }, if (thresholds.size == 1) BadgeCountType.ONE_TIME else BadgeCountType.REPEATABLE,
        BadgeRequirement.EXACT_COUNT, milestones = thresholds
    )
    val category: BadgeUiCategory get() = when (metric) {
        LandBadgeMetric.DRAWS, LandBadgeMetric.HABITATS, LandBadgeMetric.RESTORATIVE, LandBadgeMetric.RARE, LandBadgeMetric.MYTHIC, LandBadgeMetric.VESSEL -> BadgeUiCategory.STILLWATER
        LandBadgeMetric.ARC_DEPTH, LandBadgeMetric.TIGERS, LandBadgeMetric.ARCS, LandBadgeMetric.FLAGSHIPS -> BadgeUiCategory.ARC
        LandBadgeMetric.MASTERY, LandBadgeMetric.MASTERED_SPECIES, LandBadgeMetric.LEVEL -> BadgeUiCategory.MASTERY
        else -> BadgeUiCategory.COLLECTIONS
    }
    val action: BadgeActionDestination get() = when (metric) {
        LandBadgeMetric.ARC_DEPTH, LandBadgeMetric.TIGERS, LandBadgeMetric.ARCS, LandBadgeMetric.FLAGSHIPS -> BadgeActionDestination.Arc
        LandBadgeMetric.VESSEL -> BadgeActionDestination.StillwaterVessel("stillwater_${requireNotNull(habitat).name.lowercase()}")
        LandBadgeMetric.DRAWS, LandBadgeMetric.HABITATS, LandBadgeMetric.RESTORATIVE, LandBadgeMetric.RARE, LandBadgeMetric.MYTHIC -> BadgeActionDestination.CollectionDetails("collection_land_stillwater")
        LandBadgeMetric.ENCOUNTER, LandBadgeMetric.TRADE -> BadgeActionDestination.BeyondBlue("blue_golden_fields", "creature_duck")
        LandBadgeMetric.REALMS -> BadgeActionDestination.CollectionDetails("collection_living_earth")
        LandBadgeMetric.MAIN -> BadgeActionDestination.CollectionDetails("collection_land")
        LandBadgeMetric.SEA_DISCOVERY -> BadgeActionDestination.CollectionDetails("collection_all_waters")
        else -> BadgeActionDestination.CollectionDetails("collection_all_land")
    }
}

object LandBadgeCatalog {
    val specs = listOf(
        LandBadgeSpec("land_first", R.string.badge_land_first_title, R.string.badge_land_first_description,
            LandBadgeMetric.DISCOVERY, listOf(1), LandBadgeMotif.FOOTPRINTS),
        LandBadgeSpec("living_earth_first", R.string.badge_living_earth_first_title, R.string.badge_living_earth_first_description,
            LandBadgeMetric.REALMS, listOf(2), LandBadgeMotif.EARTH),
        LandBadgeSpec("land_variety", R.string.badge_land_variety_title, R.string.badge_land_variety_description,
            LandBadgeMetric.DISCOVERY, listOf(5, 10, 25, 50, 75, 100, 114), LandBadgeMotif.LEAF),
        LandBadgeSpec("across_the_land", R.string.badge_across_the_land_title, R.string.badge_across_the_land_description,
            LandBadgeMetric.ZONES, listOf(5), LandBadgeMotif.LANDSCAPE),
        LandBadgeSpec("land_encounter_first", R.string.badge_land_encounter_first_title, R.string.badge_land_encounter_first_description,
            LandBadgeMetric.ENCOUNTER, listOf(1), LandBadgeMotif.ENCOUNTER),
        LandBadgeSpec("land_growth", R.string.badge_land_growth_title, R.string.badge_land_growth_description,
            LandBadgeMetric.LEVEL, listOf(10, 25, 50, 75), LandBadgeMotif.GROWTH),
        LandBadgeSpec("land_mastery_first", R.string.badge_land_mastery_first_title, R.string.badge_land_mastery_first_description,
            LandBadgeMetric.MASTERY, listOf(1), LandBadgeMotif.CROWN),
        LandBadgeSpec("land_arc_3", R.string.badge_land_arc_3_title, R.string.badge_land_arc_3_description,
            LandBadgeMetric.ARC_DEPTH, listOf(3), LandBadgeMotif.ROOTS),
        LandBadgeSpec("land_arc_6", R.string.badge_land_arc_6_title, R.string.badge_land_arc_6_description,
            LandBadgeMetric.ARC_DEPTH, listOf(6), LandBadgeMotif.ROOTS),
        LandBadgeSpec("land_arc_9", R.string.badge_land_arc_9_title, R.string.badge_land_arc_9_description,
            LandBadgeMetric.ARC_DEPTH, listOf(9), LandBadgeMotif.ROOTS),
        LandBadgeSpec("land_arc_12", R.string.badge_land_arc_12_title, R.string.badge_land_arc_12_description,
            LandBadgeMetric.ARC_DEPTH, listOf(12), LandBadgeMotif.ROOTS),
        LandBadgeSpec("land_arc_15", R.string.badge_land_arc_15_title, R.string.badge_land_arc_15_description,
            LandBadgeMetric.ARC_DEPTH, listOf(15), LandBadgeMotif.ROOTS),
        LandBadgeSpec("land_two_tigers", R.string.badge_land_two_tigers_title, R.string.badge_land_two_tigers_description,
            LandBadgeMetric.TIGERS, listOf(2), LandBadgeMotif.TIGERS),
        LandBadgeSpec("land_five_companions", R.string.badge_land_five_companions_title, R.string.badge_land_five_companions_description,
            LandBadgeMetric.FLAGSHIPS, listOf(5), LandBadgeMotif.COMPANIONS),
        LandBadgeSpec("land_returning", R.string.badge_land_returning_title, R.string.badge_land_returning_description,
            LandBadgeMetric.ARCS, listOf(5, 10, 25, 50, 100), LandBadgeMotif.RETURN),
        LandBadgeSpec("land_stillwater_first", R.string.badge_land_stillwater_first_title, R.string.badge_land_stillwater_first_description,
            LandBadgeMetric.DRAWS, listOf(1), LandBadgeMotif.DROPLEAF),
        LandBadgeSpec("land_five_habitats", R.string.badge_land_five_habitats_title, R.string.badge_land_five_habitats_description,
            LandBadgeMetric.HABITATS, listOf(5), LandBadgeMotif.DROPLEAF),
        LandBadgeSpec("land_stillwater_variety", R.string.badge_land_stillwater_variety_title, R.string.badge_land_stillwater_variety_description,
            LandBadgeMetric.RESTORATIVE, listOf(5, 10, 25, 40, 51), LandBadgeMotif.DROPLEAF),
        LandBadgeSpec("land_rare_first", R.string.badge_land_rare_first_title, R.string.badge_land_rare_first_description,
            LandBadgeMetric.RARE, listOf(1), LandBadgeMotif.RARE),
        LandBadgeSpec("land_mythic_first", R.string.badge_land_mythic_first_title, R.string.badge_land_mythic_first_description,
            LandBadgeMetric.MYTHIC, listOf(1), LandBadgeMotif.MYTHIC),
        LandBadgeSpec("land_release_first", R.string.badge_land_release_first_title, R.string.badge_land_release_first_description,
            LandBadgeMetric.RELEASE, listOf(1), LandBadgeMotif.RELEASE),
        LandBadgeSpec("land_trade_first", R.string.badge_land_trade_first_title, R.string.badge_land_trade_first_description,
            LandBadgeMetric.TRADE, listOf(1), LandBadgeMotif.TRADE),
        LandBadgeSpec("keeper_of_the_land", R.string.badge_keeper_of_the_land_title, R.string.badge_keeper_of_the_land_description,
            LandBadgeMetric.MAIN, listOf(63), LandBadgeMotif.LANDSCAPE),
        LandBadgeSpec("one_from_every_land", R.string.badge_one_from_every_land_title, R.string.badge_one_from_every_land_description,
            LandBadgeMetric.MASTERED_COLLECTIONS, listOf(10), LandBadgeMotif.CROWN),
        LandBadgeSpec("land_mastery_circle", R.string.badge_land_mastery_circle_title, R.string.badge_land_mastery_circle_description,
            LandBadgeMetric.MASTERY, listOf(1, 5, 10, 25, 50, 100, 250, 500, 1000), LandBadgeMotif.CROWN),
        LandBadgeSpec("land_mastery_variety", R.string.badge_land_mastery_variety_title, R.string.badge_land_mastery_variety_description,
            LandBadgeMetric.MASTERED_SPECIES, listOf(1, 5, 10, 25, 50, 100, 114), LandBadgeMotif.CROWN),
        LandBadgeSpec("sea_variety", R.string.badge_sea_variety_title, R.string.badge_sea_variety_description,
            LandBadgeMetric.SEA_DISCOVERY, listOf(1, 5, 10, 25, 50, 71), LandBadgeMotif.SEA),
        LandBadgeSpec("land_pasture_first", R.string.badge_land_pasture_first_title, R.string.badge_land_pasture_first_description,
            LandBadgeMetric.VESSEL, listOf(1), LandBadgeMotif.DROPLEAF, LandStillwaterHabitat.PASTURE),
        LandBadgeSpec("land_glade_first", R.string.badge_land_glade_first_title, R.string.badge_land_glade_first_description,
            LandBadgeMetric.VESSEL, listOf(1), LandBadgeMotif.DROPLEAF, LandStillwaterHabitat.GLADE),
        LandBadgeSpec("land_oasis_first", R.string.badge_land_oasis_first_title, R.string.badge_land_oasis_first_description,
            LandBadgeMetric.VESSEL, listOf(1), LandBadgeMotif.DROPLEAF, LandStillwaterHabitat.OASIS),
        LandBadgeSpec("land_ravine_first", R.string.badge_land_ravine_first_title, R.string.badge_land_ravine_first_description,
            LandBadgeMetric.VESSEL, listOf(1), LandBadgeMotif.DROPLEAF, LandStillwaterHabitat.RAVINE),
        LandBadgeSpec("land_sanctuary_first", R.string.badge_land_sanctuary_first_title, R.string.badge_land_sanctuary_first_description,
            LandBadgeMetric.VESSEL, listOf(1), LandBadgeMotif.DROPLEAF, LandStillwaterHabitat.SANCTUARY),
    )
    val byId = specs.associateBy { it.id }
}

/** Rebuildable from existing durable records, including copies that were released or contributed.
 * Arc evidence is exclusively granted reward instances: those only exist after finalization.
 * Summing their typed requirements yields the depth floor needed for these badge thresholds,
 * without counting Flows again or claiming an exact Flow count from the reward package.
 */
class LandBadgeEvidence(
    instances: List<UserShellFindInstanceEntity>,
    discoveredIds: Set<String>,
    mastery: Map<String, SpeciesMasteryEvidence>
) {
    private val copies = instances.distinctBy { it.instanceId }
    private val discovered = discoveredIds + copies.map { it.findId }
    private val land = copies.filter { CreatureCatalog.get(it.findId)?.realm == CreatureRealm.LAND }
    private val landIds = LandCreatureCatalog.all.filter { it.isAvailable }.map { it.creatureId }.toSet()
    private val mainIds = LandCreatureCatalog.main.map { it.creatureId }.toSet()
    private val restorativeIds = LandCreatureCatalog.restorative.map { it.creatureId }.toSet()
    private val draws = land.filter { it.sourceType == "stillwater" && it.findId in restorativeIds }
    private val arcs = land.filter { it.sourceType == "arc" && it.sourceId != null && CreatureCatalog.get(it.findId)?.sourceType == CreatureSourceType.ARC_EARNED }.groupBy { it.sourceId }
    private val mastered = mastery.filterValues { it.hasEverBeenMastered }.keys
    private val masteryCount = mastery.filterKeys { it in landIds }.values.sumOf { it.effectiveLifetimeCount }

    fun count(spec: LandBadgeSpec): Int = when (spec.metric) {
        LandBadgeMetric.DISCOVERY -> discovered.intersect(landIds).size
        LandBadgeMetric.SEA_DISCOVERY -> discovered.count { CreatureCatalog.get(it)?.realm == CreatureRealm.SEA }
        LandBadgeMetric.REALMS -> discovered.mapNotNull { CreatureCatalog.get(it)?.realm }.distinct().size
        LandBadgeMetric.ZONES -> discovered.filter { it in landIds }.map { CreatureCatalog.require(it).zone }.distinct().size
        LandBadgeMetric.ENCOUNTER -> land.count { it.sourceType == "beyond_blue" }
        LandBadgeMetric.LEVEL -> maxOf(land.maxOfOrNull { it.animalLevel } ?: 0, if (masteryCount > 0) 99 else 0)
        LandBadgeMetric.MASTERY -> masteryCount
        LandBadgeMetric.MASTERED_SPECIES -> mastered.intersect(landIds).size
        LandBadgeMetric.ARC_DEPTH -> arcs.values.maxOfOrNull { group -> group.sumOf { CreatureCatalog.require(it.findId).arcFlowRequirement ?: 0 } } ?: 0
        LandBadgeMetric.TIGERS -> arcs.values.maxOfOrNull { group -> group.count { it.findId == "creature_tiger" } } ?: 0
        LandBadgeMetric.FLAGSHIPS -> arcs.values.flatten().map { it.findId }.distinct().size
        LandBadgeMetric.ARCS -> arcs.size
        LandBadgeMetric.DRAWS -> draws.size
        LandBadgeMetric.HABITATS -> draws.mapNotNull { CreatureCatalog.get(it.findId)?.restorativeHabitat }.distinct().size
        LandBadgeMetric.VESSEL -> draws.count { CreatureCatalog.get(it.findId)?.restorativeHabitat == spec.habitat }
        LandBadgeMetric.RESTORATIVE -> discovered.intersect(restorativeIds).size
        LandBadgeMetric.RARE -> draws.count { LandStillwaterCatalog.rarityById[it.findId] == StillwaterRarity.RARE }
        LandBadgeMetric.MYTHIC -> draws.count { LandStillwaterCatalog.rarityById[it.findId] == StillwaterRarity.MYTHIC }
        LandBadgeMetric.RELEASE -> land.count { it.creatureStatus == CreatureStatus.RELEASED }
        LandBadgeMetric.TRADE -> land.count { it.creatureStatus == CreatureStatus.USED_BEYOND_BLUE }
        LandBadgeMetric.MAIN -> discovered.intersect(mainIds).size
        LandBadgeMetric.MASTERED_COLLECTIONS -> CollectionCatalog.collections.count { collection ->
            (collection.collectionId in LandCreatureCatalog.mainZones.map { "blue_${it.name.lowercase()}" } ||
                collection.collectionId in LandStillwaterHabitat.entries.map { "stillwater_${it.name.lowercase()}" }) &&
                collection.species.any { it.creatureId in mastered }
        }
    }
}
