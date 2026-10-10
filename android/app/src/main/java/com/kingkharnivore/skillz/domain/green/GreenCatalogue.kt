package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.R

/** Stable IDs and explicit tiers: ordering is presentation only. */
enum class GreenEnvironment(val id: String, val nameRes: Int) {
    GARDEN("garden", R.string.green_environment_garden),
    WOODLANDS("woodlands", R.string.green_environment_woodlands),
    RAINFOREST("rainforest", R.string.green_environment_rainforest),
    WETLANDS("wetlands", R.string.green_environment_wetlands),
    DRYLANDS("drylands", R.string.green_environment_drylands),
    HIGHLANDS("highlands", R.string.green_environment_highlands)
}

enum class GrowthArchetype { TREE, FLOWER, SHRUB, VINE, CACTUS_SUCCULENT, AQUATIC, GROUNDCOVER, SPECIAL }
enum class PlantFootprint { TINY, SMALL, MEDIUM, LARGE, XL, LANDMARK }

data class PlantSpecies(
    val id: String,
    val nameRes: Int,
    val environment: GreenEnvironment,
    val tier: Int,
    val archetype: GrowthArchetype,
    val landmark: Boolean = false
) {
    init { require(tier in 1..10) }
    val stageAssetKeys: List<String> get() = (1..7).map { "plant_${id}_stage_${it.toString().padStart(2, '0')}" }
    fun assetKey(level: Int): String = stageAssetKeys[PlantGrowthStageResolver.resolve(level).assetIndex]
    fun footprint(level: Int): PlantFootprint = when (PlantGrowthStageResolver.resolve(level).assetIndex) {
        0 -> PlantFootprint.TINY
        1 -> PlantFootprint.SMALL
        2, 3 -> PlantFootprint.MEDIUM
        4, 5 -> PlantFootprint.LARGE
        else -> if (landmark) PlantFootprint.LANDMARK else PlantFootprint.XL
    }
    // Stable art-direction key lets final artwork specialize beyond its archetype.
    val artDirectionKey: String get() = id
}

object GreenCatalogue {
    const val VERSION = 1
    val species: List<PlantSpecies> = listOf(
        PlantSpecies("daisy", R.string.green_species_daisy, GreenEnvironment.GARDEN, 1, GrowthArchetype.FLOWER, false),
        PlantSpecies("tulip", R.string.green_species_tulip, GreenEnvironment.GARDEN, 2, GrowthArchetype.FLOWER, false),
        PlantSpecies("lavender", R.string.green_species_lavender, GreenEnvironment.GARDEN, 3, GrowthArchetype.SHRUB, false),
        PlantSpecies("rose", R.string.green_species_rose, GreenEnvironment.GARDEN, 4, GrowthArchetype.SHRUB, false),
        PlantSpecies("hydrangea", R.string.green_species_hydrangea, GreenEnvironment.GARDEN, 5, GrowthArchetype.SHRUB, false),
        PlantSpecies("sunflower", R.string.green_species_sunflower, GreenEnvironment.GARDEN, 6, GrowthArchetype.FLOWER, false),
        PlantSpecies("wisteria", R.string.green_species_wisteria, GreenEnvironment.GARDEN, 7, GrowthArchetype.VINE, false),
        PlantSpecies("peony", R.string.green_species_peony, GreenEnvironment.GARDEN, 8, GrowthArchetype.FLOWER, false),
        PlantSpecies("cherry_blossom", R.string.green_species_cherry_blossom, GreenEnvironment.GARDEN, 9, GrowthArchetype.TREE, false),
        PlantSpecies("magnolia", R.string.green_species_magnolia, GreenEnvironment.GARDEN, 10, GrowthArchetype.TREE, true),
        PlantSpecies("fern", R.string.green_species_fern, GreenEnvironment.WOODLANDS, 1, GrowthArchetype.GROUNDCOVER, false),
        PlantSpecies("birch", R.string.green_species_birch, GreenEnvironment.WOODLANDS, 2, GrowthArchetype.TREE, false),
        PlantSpecies("aspen", R.string.green_species_aspen, GreenEnvironment.WOODLANDS, 3, GrowthArchetype.TREE, false),
        PlantSpecies("maple", R.string.green_species_maple, GreenEnvironment.WOODLANDS, 4, GrowthArchetype.TREE, false),
        PlantSpecies("willow", R.string.green_species_willow, GreenEnvironment.WOODLANDS, 5, GrowthArchetype.TREE, false),
        PlantSpecies("oak", R.string.green_species_oak, GreenEnvironment.WOODLANDS, 6, GrowthArchetype.TREE, false),
        PlantSpecies("banyan", R.string.green_species_banyan, GreenEnvironment.WOODLANDS, 7, GrowthArchetype.TREE, false),
        PlantSpecies("rainbow_eucalyptus", R.string.green_species_rainbow_eucalyptus, GreenEnvironment.WOODLANDS, 8, GrowthArchetype.TREE, false),
        PlantSpecies("redwood", R.string.green_species_redwood, GreenEnvironment.WOODLANDS, 9, GrowthArchetype.TREE, false),
        PlantSpecies("giant_sequoia", R.string.green_species_giant_sequoia, GreenEnvironment.WOODLANDS, 10, GrowthArchetype.TREE, true),
        PlantSpecies("monstera", R.string.green_species_monstera, GreenEnvironment.RAINFOREST, 1, GrowthArchetype.SHRUB, false),
        PlantSpecies("bromeliad", R.string.green_species_bromeliad, GreenEnvironment.RAINFOREST, 2, GrowthArchetype.FLOWER, false),
        PlantSpecies("orchid", R.string.green_species_orchid, GreenEnvironment.RAINFOREST, 3, GrowthArchetype.FLOWER, false),
        PlantSpecies("heliconia", R.string.green_species_heliconia, GreenEnvironment.RAINFOREST, 4, GrowthArchetype.FLOWER, false),
        PlantSpecies("cacao", R.string.green_species_cacao, GreenEnvironment.RAINFOREST, 5, GrowthArchetype.TREE, false),
        PlantSpecies("banana", R.string.green_species_banana, GreenEnvironment.RAINFOREST, 6, GrowthArchetype.SPECIAL, false),
        PlantSpecies("pitcher_plant", R.string.green_species_pitcher_plant, GreenEnvironment.RAINFOREST, 7, GrowthArchetype.SPECIAL, false),
        PlantSpecies("corpse_flower", R.string.green_species_corpse_flower, GreenEnvironment.RAINFOREST, 8, GrowthArchetype.SPECIAL, false),
        PlantSpecies("rafflesia", R.string.green_species_rafflesia, GreenEnvironment.RAINFOREST, 9, GrowthArchetype.SPECIAL, false),
        PlantSpecies("kapok", R.string.green_species_kapok, GreenEnvironment.RAINFOREST, 10, GrowthArchetype.TREE, true),
        PlantSpecies("reed", R.string.green_species_reed, GreenEnvironment.WETLANDS, 1, GrowthArchetype.GROUNDCOVER, false),
        PlantSpecies("cattail", R.string.green_species_cattail, GreenEnvironment.WETLANDS, 2, GrowthArchetype.GROUNDCOVER, false),
        PlantSpecies("papyrus", R.string.green_species_papyrus, GreenEnvironment.WETLANDS, 3, GrowthArchetype.GROUNDCOVER, false),
        PlantSpecies("waterlily", R.string.green_species_waterlily, GreenEnvironment.WETLANDS, 4, GrowthArchetype.AQUATIC, false),
        PlantSpecies("lotus", R.string.green_species_lotus, GreenEnvironment.WETLANDS, 5, GrowthArchetype.AQUATIC, false),
        PlantSpecies("mangrove", R.string.green_species_mangrove, GreenEnvironment.WETLANDS, 6, GrowthArchetype.TREE, false),
        PlantSpecies("bald_cypress", R.string.green_species_bald_cypress, GreenEnvironment.WETLANDS, 7, GrowthArchetype.TREE, false),
        PlantSpecies("pickerelweed", R.string.green_species_pickerelweed, GreenEnvironment.WETLANDS, 8, GrowthArchetype.AQUATIC, false),
        PlantSpecies("giant_water_lily", R.string.green_species_giant_water_lily, GreenEnvironment.WETLANDS, 9, GrowthArchetype.AQUATIC, false),
        PlantSpecies("victoria_lily", R.string.green_species_victoria_lily, GreenEnvironment.WETLANDS, 10, GrowthArchetype.AQUATIC, true),
        PlantSpecies("aloe", R.string.green_species_aloe, GreenEnvironment.DRYLANDS, 1, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("jade", R.string.green_species_jade, GreenEnvironment.DRYLANDS, 2, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("yucca", R.string.green_species_yucca, GreenEnvironment.DRYLANDS, 3, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("agave", R.string.green_species_agave, GreenEnvironment.DRYLANDS, 4, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("prickly_pear", R.string.green_species_prickly_pear, GreenEnvironment.DRYLANDS, 5, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("barrel_cactus", R.string.green_species_barrel_cactus, GreenEnvironment.DRYLANDS, 6, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("saguaro", R.string.green_species_saguaro, GreenEnvironment.DRYLANDS, 7, GrowthArchetype.CACTUS_SUCCULENT, false),
        PlantSpecies("welwitschia", R.string.green_species_welwitschia, GreenEnvironment.DRYLANDS, 8, GrowthArchetype.SPECIAL, false),
        PlantSpecies("dragons_blood_tree", R.string.green_species_dragons_blood_tree, GreenEnvironment.DRYLANDS, 9, GrowthArchetype.TREE, false),
        PlantSpecies("baobab", R.string.green_species_baobab, GreenEnvironment.DRYLANDS, 10, GrowthArchetype.TREE, true),
        PlantSpecies("moss", R.string.green_species_moss, GreenEnvironment.HIGHLANDS, 1, GrowthArchetype.GROUNDCOVER, false),
        PlantSpecies("heather", R.string.green_species_heather, GreenEnvironment.HIGHLANDS, 2, GrowthArchetype.SHRUB, false),
        PlantSpecies("edelweiss", R.string.green_species_edelweiss, GreenEnvironment.HIGHLANDS, 3, GrowthArchetype.FLOWER, false),
        PlantSpecies("rhododendron", R.string.green_species_rhododendron, GreenEnvironment.HIGHLANDS, 4, GrowthArchetype.SHRUB, false),
        PlantSpecies("juniper", R.string.green_species_juniper, GreenEnvironment.HIGHLANDS, 5, GrowthArchetype.SHRUB, false),
        PlantSpecies("larch", R.string.green_species_larch, GreenEnvironment.HIGHLANDS, 6, GrowthArchetype.TREE, false),
        PlantSpecies("fir", R.string.green_species_fir, GreenEnvironment.HIGHLANDS, 7, GrowthArchetype.TREE, false),
        PlantSpecies("snow_lotus", R.string.green_species_snow_lotus, GreenEnvironment.HIGHLANDS, 8, GrowthArchetype.FLOWER, false),
        PlantSpecies("himalayan_cedar", R.string.green_species_himalayan_cedar, GreenEnvironment.HIGHLANDS, 9, GrowthArchetype.TREE, false),
        PlantSpecies("bristlecone_pine", R.string.green_species_bristlecone_pine, GreenEnvironment.HIGHLANDS, 10, GrowthArchetype.TREE, true),
    )
    val byId = species.associateBy { it.id }
    fun inEnvironment(environment: GreenEnvironment) = species.filter { it.environment == environment }
}
