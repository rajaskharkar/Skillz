package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.utils.shell.*

/** Lifetime growth evidence includes released/traded copies. No spending or calendar requirements. */
enum class CreatureGrowthMetric { SPECIES, SAME_SPECIES_COPIES, HABITATS }
data class CreatureGrowthBadge(
    val id: String, val realm: CreatureRealm, val titleRes: Int,
    val level: Int, val target: Int, val species: Set<String>,
    val metric: CreatureGrowthMetric = CreatureGrowthMetric.SPECIES,
    val collectionId: String? = null, val themeTitleRes: Int? = null
) {
    val definition get() = AchievementBadgeDefinition(id, BadgeFamily.COLLECTION,
        BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT, milestones = listOf(target))
}

object CreatureGrowthBadges {
    val specs = buildList {
        CreatureRealm.entries.forEach { realm ->
            val roster = CreatureCatalog.all.filter { it.isAvailable && it.realm == realm }.mapTo(linkedSetOf()) { it.creatureId }
            fun goal(key: String, title: Int, level: Int, target: Int = 1,
                     metric: CreatureGrowthMetric = CreatureGrowthMetric.SPECIES) {
                add(CreatureGrowthBadge("growth_v1_${realm.name.lowercase()}_$key", realm, title, level, target, roster, metric))
            }
            goal("hello", R.string.growth_hello, 5)
            goal("companion", R.string.growth_companion, 15)
            goal("stride", R.string.growth_stride, 30)
            goal("bond", R.string.growth_bond, 45)
            goal("flourishing", R.string.growth_flourishing, 60)
            goal("horizon", R.string.growth_horizon, 75)
            goal("trio", R.string.growth_trio, 15, 3)
            goal("mosaic", R.string.growth_mosaic, 30, 6)
            goal("ensemble", R.string.growth_ensemble, 60, 3)
            goal("pair", R.string.growth_pair, 30, 2, CreatureGrowthMetric.SAME_SPECIES_COPIES)
            goal("neighbours", R.string.growth_neighbours, 15, 2, CreatureGrowthMetric.HABITATS)
            goal("atlas", R.string.growth_atlas, 45, CreatureZone.entries.count { it.realm == realm }, CreatureGrowthMetric.HABITATS)
            CreatureZone.entries.filter { it.realm == realm }.forEach { zone ->
                val ids = roster.filterTo(linkedSetOf()) { CreatureCatalog.require(it).zone == zone }
                val collection = "${if (realm == CreatureRealm.RED) "red" else "blue"}_${zone.name.lowercase()}"
                listOf(15 to R.string.growth_small_world, 45 to R.string.growth_established_world).forEach { (level, title) ->
                    add(CreatureGrowthBadge("growth_v1_${zone.name.lowercase()}_$level", realm, title, level, 3, ids, collectionId = collection))
                }
            }
        }
        // The existing specialist families get an attainable discovery-to-mastery journey.
        RedBadgeCatalog.specs.filter { it.mastery && it.id !in setOf("red_ascendant", "red_dominion", "red_extinction", "red_the_red") }.forEach { theme ->
            listOf(15 to R.string.growth_theme_beginning, 45 to R.string.growth_theme_developing).forEach { (level, title) ->
                add(CreatureGrowthBadge("growth_v1_${theme.id}_$level", CreatureRealm.RED, title, level,
                    theme.species.size, theme.species, themeTitleRes = theme.titleRes))
            }
        }
    }
    val byId = specs.associateBy { it.id }

    fun evidence(instances: List<UserShellFindInstanceEntity>, masteries: List<CreatureMasteryEventEntity>,
                 lifetimeMasteryCounts: Map<String, Int> = emptyMap()): List<UserShellFindInstanceEntity> {
        val byInstance = instances.associateBy { it.instanceId }.toMutableMap()
        masteries.forEach { event ->
            val old = byInstance[event.creatureInstanceId]
            byInstance[event.creatureInstanceId] = old?.copy(animalLevel = maxOf(old.animalLevel, 99))
                ?: UserShellFindInstanceEntity(event.creatureInstanceId, event.speciesId, event.achievedAt,
                    "mastery_evidence", null, null, null, false, true, animalLevel = 99, creatureStatus = CreatureStatus.RELEASED)
        }
        // Legacy per-species floors are also proof of growth. Only materialize the
        // bounded number needed by these finite goals, never an unbounded old count.
        val copyTarget = specs.filter { it.metric == CreatureGrowthMetric.SAME_SPECIES_COPIES }.maxOf { it.target }
        lifetimeMasteryCounts.forEach { (species, count) ->
            if (CreatureCatalog.get(species) != null) {
                val retained = byInstance.values.count { it.findId == species && it.animalLevel >= 99 }
                repeat((count.coerceAtMost(copyTarget) - retained).coerceAtLeast(0)) { index ->
                    val id = "growth-floor:$species:$index"
                    byInstance[id] = UserShellFindInstanceEntity(id, species, 0, "mastery_floor", null, null, null, false, true,
                        animalLevel = 99, creatureStatus = CreatureStatus.RELEASED)
                }
            }
        }
        return byInstance.values.toList()
    }

    fun progress(spec: CreatureGrowthBadge, evidence: List<UserShellFindInstanceEntity>): Int {
        val qualified = evidence.distinctBy { it.instanceId }.filter { it.findId in spec.species && it.animalLevel >= spec.level }
        return when (spec.metric) {
            CreatureGrowthMetric.SPECIES -> qualified.map { it.findId }.distinct().size
            CreatureGrowthMetric.SAME_SPECIES_COPIES -> qualified.groupingBy { it.findId }.eachCount().values.maxOrNull() ?: 0
            CreatureGrowthMetric.HABITATS -> qualified.mapNotNull { CreatureCatalog.get(it.findId)?.zone }.distinct().size
        }
    }

    fun nextSpecies(spec: CreatureGrowthBadge, evidence: List<UserShellFindInstanceEntity>): String {
        val qualified = evidence.filter { it.findId in spec.species && it.animalLevel >= spec.level }
        val complete = qualified.map { it.findId }.toSet()
        val habitats = complete.mapNotNull { CreatureCatalog.get(it)?.zone }.toSet()
        val candidates = when (spec.metric) {
            CreatureGrowthMetric.SPECIES -> spec.species - complete
            CreatureGrowthMetric.HABITATS -> spec.species.filter { CreatureCatalog.get(it)?.zone !in habitats }.toSet()
            CreatureGrowthMetric.SAME_SPECIES_COPIES -> setOf(qualified.groupingBy { it.findId }.eachCount().maxByOrNull { it.value }?.key
                ?: evidence.filter { it.findId in spec.species }.maxByOrNull { it.animalLevel }?.findId ?: spec.species.first())
        }
        return evidence.filter { it.creatureStatus == CreatureStatus.ACTIVE && it.findId in candidates && it.animalLevel < spec.level }
            .maxByOrNull { it.animalLevel }?.findId ?: candidates.firstOrNull() ?: spec.species.first()
    }
}
