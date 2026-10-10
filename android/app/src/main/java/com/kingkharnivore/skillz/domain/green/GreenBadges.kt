package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.R

/** These metrics depend only on durable, monotonically increasing plant progress. */
enum class GreenBadgeMetric { SPECIES, SPECIMENS, WATERINGS, ENVIRONMENTS, ARCHETYPES, SAME_SPECIES_COPIES, SPECIES_FAMILIES }
data class PlantProgress(val speciesId: String, val level: Int)
data class GreenBadgeDefinition(
    val id: String, val environment: GreenEnvironment?, val speciesId: String?,
    val requiredLevel: Int, val requiredSpecies: Set<String>,
    val metric: GreenBadgeMetric = GreenBadgeMetric.SPECIES,
    val target: Int = requiredSpecies.size, val copiesPerSpecies: Int = 1,
    val titleRes: Int? = null, val themeKey: String? = null
)
data class GreenPlantTheme(val key: String, val titleRes: Int, val species: Set<String>)

object GreenBadgeEvaluator {
    private val launchIds = GreenCatalogue.species.mapTo(linkedSetOf()) { it.id }
    val themes = listOf(
        GreenPlantTheme("ancient_company", R.string.green_badge_ancient_company, setOf("giant_sequoia", "redwood", "bristlecone_pine", "welwitschia")),
        GreenPlantTheme("living_water", R.string.green_badge_living_water, setOf("waterlily", "lotus", "giant_water_lily", "victoria_lily")),
        GreenPlantTheme("root_architects", R.string.green_badge_root_architects, setOf("banyan", "mangrove", "bald_cypress")),
        GreenPlantTheme("skyward", R.string.green_badge_skyward, setOf("giant_sequoia", "kapok", "redwood", "himalayan_cedar")),
        GreenPlantTheme("quiet_understory", R.string.green_badge_quiet_understory, setOf("fern", "moss", "heather", "bromeliad")),
        GreenPlantTheme("desert_sculptures", R.string.green_badge_desert_sculptures, setOf("saguaro", "agave", "welwitschia", "dragons_blood_tree", "baobab")),
        GreenPlantTheme("bloom_routes", R.string.green_badge_bloom_routes, setOf("peony", "orchid", "wisteria", "edelweiss", "snow_lotus")),
        GreenPlantTheme("rain_collectors", R.string.green_badge_rain_collectors, setOf("monstera", "bromeliad", "pitcher_plant")),
        GreenPlantTheme("forest_pantry", R.string.green_badge_forest_pantry, setOf("cacao", "banana")),
        GreenPlantTheme("extraordinary_blooms", R.string.green_badge_extraordinary_blooms, setOf("corpse_flower", "rafflesia", "victoria_lily")),
        GreenPlantTheme("alpine_company", R.string.green_badge_alpine_company, setOf("edelweiss", "juniper", "snow_lotus", "bristlecone_pine")),
        GreenPlantTheme("canopy_palette", R.string.green_badge_canopy_palette, setOf("maple", "cherry_blossom", "rainbow_eucalyptus", "magnolia"))
    )
    /** Existing v1 IDs remain unchanged. New objectives use a separate namespace. */
    val definitions: List<GreenBadgeDefinition> = buildList {
        GreenCatalogue.species.forEach { add(GreenBadgeDefinition("green_v1_species_${it.id}_mastery", null, it.id, 99, setOf(it.id))) }
        (GreenEnvironment.entries.map { it as GreenEnvironment? } + listOf(null)).forEach { environment ->
            val ids = GreenCatalogue.species.filter { environment == null || it.environment == environment }.mapTo(linkedSetOf()) { it.id }
            listOf("catalogue" to 1, "flourish" to 90, "mastery" to 99).forEach { (name, level) ->
                add(GreenBadgeDefinition("green_v1_${environment?.id ?: "all"}_$name", environment, null, level, ids))
            }
        }
        fun goal(key: String, title: Int, metric: GreenBadgeMetric, target: Int, level: Int = 1, copies: Int = 1,
                 ids: Set<String> = launchIds) {
            add(GreenBadgeDefinition("green_v2_$key", null, null, level, ids, metric, target, copies, title))
        }
        goal("first_roots", R.string.green_badge_first_roots, GreenBadgeMetric.SPECIMENS, 1)
        goal("first_water", R.string.green_badge_first_water, GreenBadgeMetric.WATERINGS, 1)
        goal("young_promise", R.string.green_badge_young_promise, GreenBadgeMetric.SPECIMENS, 1, 30)
        goal("taking_hold", R.string.green_badge_taking_hold, GreenBadgeMetric.SPECIMENS, 1, 45)
        goal("coming_alive", R.string.green_badge_coming_alive, GreenBadgeMetric.SPECIMENS, 1, 60)
        goal("full_form", R.string.green_badge_full_form, GreenBadgeMetric.SPECIMENS, 1, 90)
        goal("first_masterpiece", R.string.green_badge_first_masterpiece, GreenBadgeMetric.SPECIMENS, 1, 99)
        goal("well_tended", R.string.green_badge_well_tended, GreenBadgeMetric.WATERINGS, 100)
        goal("patient_hands", R.string.green_badge_patient_hands, GreenBadgeMetric.WATERINGS, 1_000)
        goal("lifetime_care", R.string.green_badge_lifetime_care, GreenBadgeMetric.WATERINGS, 5_000)
        goal("growing_company", R.string.green_badge_growing_company, GreenBadgeMetric.SPECIMENS, 10)
        goal("living_landscape", R.string.green_badge_living_landscape, GreenBadgeMetric.SPECIMENS, 25, 90)
        goal("master_gardener", R.string.green_badge_master_gardener, GreenBadgeMetric.SPECIMENS, 25, 99)
        goal("six_soils", R.string.green_badge_six_soils, GreenBadgeMetric.ENVIRONMENTS, 6)
        goal("rooted_connections", R.string.green_badge_rooted_connections, GreenBadgeMetric.ENVIRONMENTS, 6, 45)
        goal("canopy_connections", R.string.green_badge_canopy_connections, GreenBadgeMetric.ENVIRONMENTS, 6, 90)
        goal("six_sanctuaries", R.string.green_badge_six_sanctuaries, GreenBadgeMetric.ENVIRONMENTS, 6, 90, 3)
        goal("many_ways_to_grow", R.string.green_badge_many_ways_to_grow, GreenBadgeMetric.ARCHETYPES, 8, 90)
        goal("landmark_atlas", R.string.green_badge_landmark_atlas, GreenBadgeMetric.SPECIES, 6, 90,
            ids = GreenCatalogue.species.filter { it.landmark }.mapTo(linkedSetOf()) { it.id })
        goal("living_monuments", R.string.green_badge_living_monuments, GreenBadgeMetric.SPECIES, 6, 99,
            ids = GreenCatalogue.species.filter { it.landmark }.mapTo(linkedSetOf()) { it.id })
        goal("second_spring", R.string.green_badge_second_spring, GreenBadgeMetric.SAME_SPECIES_COPIES, 2, 90)
        goal("living_grove", R.string.green_badge_living_grove, GreenBadgeMetric.SAME_SPECIES_COPIES, 5, 90)
        goal("family_portrait", R.string.green_badge_family_portrait, GreenBadgeMetric.SAME_SPECIES_COPIES, 3, 99)
        goal("many_generations", R.string.green_badge_many_generations, GreenBadgeMetric.SPECIES_FAMILIES, 5, 90, 2)
        goal("gentle_start", R.string.growth_hello, GreenBadgeMetric.SPECIMENS, 1, 5)
        goal("first_seedling", R.string.growth_companion, GreenBadgeMetric.SPECIMENS, 1, 15)
        goal("mature_promise", R.string.growth_horizon, GreenBadgeMetric.SPECIMENS, 1, 75)
        goal("little_company", R.string.growth_trio, GreenBadgeMetric.SPECIES, 3, 15)
        goal("growing_mosaic", R.string.growth_mosaic, GreenBadgeMetric.SPECIES, 6, 30)
        goal("thriving_ensemble", R.string.growth_ensemble, GreenBadgeMetric.SPECIES, 3, 60)
        goal("side_by_side", R.string.growth_pair, GreenBadgeMetric.SAME_SPECIES_COPIES, 2, 30)
        goal("neighbouring_gardens", R.string.growth_neighbours, GreenBadgeMetric.ENVIRONMENTS, 2, 15)
        goal("shape_study", R.string.growth_forms, GreenBadgeMetric.ARCHETYPES, 4, 30)
        goal("small_ritual", R.string.growth_small_ritual, GreenBadgeMetric.WATERINGS, 10)
        goal("steady_care", R.string.growth_steady_care, GreenBadgeMetric.WATERINGS, 30)
        GreenEnvironment.entries.forEach { environment ->
            val ids = GreenCatalogue.inEnvironment(environment).mapTo(linkedSetOf()) { it.id }
            listOf(15, 45, 60).forEach { level ->
                add(GreenBadgeDefinition("green_v3_${environment.id}_$level", environment, null, level, ids,
                    target = 3, titleRes = environment.nameRes, themeKey = environment.id))
            }
        }
        themes.forEach { theme ->
            listOf("seedling" to 15, "rooted" to 45, "flourish" to 90, "mastery" to 99).forEach { (suffix, level) ->
                add(GreenBadgeDefinition("green_v2_${theme.key}_$suffix", null, null, level, theme.species,
                    titleRes = theme.titleRes, themeKey = theme.key))
            }
        }
    }
    val byId = definitions.associateBy { it.id }
    fun progress(badge: GreenBadgeDefinition, plants: List<PlantProgress>): Int {
        val qualifying = plants.filter { it.speciesId in badge.requiredSpecies && it.level >= badge.requiredLevel }
        val counts = qualifying.groupingBy { it.speciesId }.eachCount()
        return when (badge.metric) {
            GreenBadgeMetric.SPECIES -> counts.size
            GreenBadgeMetric.SPECIMENS -> qualifying.size
            GreenBadgeMetric.WATERINGS -> plants.filter { it.speciesId in badge.requiredSpecies }
                .sumOf { (it.level - 1).coerceAtLeast(0).toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            GreenBadgeMetric.ENVIRONMENTS -> counts.keys.mapNotNull { GreenCatalogue.byId[it] }
                .groupingBy { it.environment }.eachCount().count { it.value >= badge.copiesPerSpecies }
            GreenBadgeMetric.ARCHETYPES -> counts.keys.mapNotNull { GreenCatalogue.byId[it]?.archetype }.distinct().size
            GreenBadgeMetric.SAME_SPECIES_COPIES -> counts.values.maxOrNull() ?: 0
            GreenBadgeMetric.SPECIES_FAMILIES -> counts.count { it.value >= badge.copiesPerSpecies }
        }
    }
    fun eligible(plants: List<PlantProgress>, criteria: List<GreenBadgeDefinition> = definitions): Set<String> =
        criteria.filter { it.requiredSpecies.isNotEmpty() && progress(it, plants) >= it.target }.mapTo(linkedSetOf()) { it.id }
    fun earned(plants: List<PlantProgress>, previousAwards: Set<String>, criteria: List<GreenBadgeDefinition> = definitions): Set<String> =
        previousAwards + eligible(plants, criteria)
}
