package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.shell.*

data class RedBadgeSpec(val id: String, val titleRes: Int, val descriptionRes: Int, val species: Set<String> = emptySet(), val mastery: Boolean = false, val target: Int = species.size) {
    val definition get() = AchievementBadgeDefinition(id, if(id.startsWith("power_")) BadgeFamily.FLOW else if(mastery) BadgeFamily.MASTERY else BadgeFamily.COLLECTION, BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT, milestones = listOf(target))
}

object AdaptationPolicy {
    const val MINIMUM_HISTORICAL_ELIGIBLE_SESSIONS = 10
    const val MINIMUM_HISTORICAL_POWER_SHARE = 0.60
    const val RECENT_WINDOW_SIZE = 8
    const val MAXIMUM_RECENT_POWER_SHARE = 0.25
    fun qualifies(sessions: List<SessionEntity>): Boolean = sessions.filter { !it.isSoftMode && it.durationMs > 0 }.groupBy { it.tagId }.values.any { journey ->
        val ordered = journey.sortedWith(compareBy<SessionEntity> { it.endTime }.thenBy { it.id })
        val history = ordered.dropLast(RECENT_WINDOW_SIZE)
        val recent = ordered.takeLast(RECENT_WINDOW_SIZE)
        history.size >= MINIMUM_HISTORICAL_ELIGIBLE_SESSIONS && recent.size == RECENT_WINDOW_SIZE &&
            history.count { it.mode == FlowMode.POWER }.toDouble() / history.size >= MINIMUM_HISTORICAL_POWER_SHARE &&
            recent.count { it.mode == FlowMode.POWER }.toDouble() / recent.size <= MAXIMUM_RECENT_POWER_SHARE
    }
}

object RedBadgeCatalog {
    fun isRedCollection(id: String) = id in setOf("collection_red", "red_triassic", "red_jurassic", "red_cretaceous")
    // Intermediate builds generated these in addition to the specified named badges.
    // Keep stored rows intact, but stop generating or presenting duplicate objectives.
    fun isRedundantCollectionBadge(id: String): Boolean =
        listOf("collector", "curator", "completionist").any { suffix ->
            id.endsWith("_$suffix") && isRedCollection(id.removeSuffix("_$suffix"))
        }

    private fun era(era: RedEra) = RedCreatureCatalog.entries.filter { it.era == era }.map { it.id }.toSet()
    private val all = RedCreatureCatalog.byId.keys
    val specs = listOf(
        RedBadgeSpec("red_first_footprint", R.string.red_first_footprint_title, R.string.red_first_footprint_description, all, target = 1),
        RedBadgeSpec("red_dawn", R.string.red_dawn_title, R.string.red_dawn_description, era(RedEra.TRIASSIC)),
        RedBadgeSpec("red_ascendant", R.string.red_ascendant_title, R.string.red_ascendant_description, era(RedEra.TRIASSIC), mastery = true),
        RedBadgeSpec("red_giants", R.string.red_giants_title, R.string.red_giants_description, era(RedEra.JURASSIC)),
        RedBadgeSpec("red_dominion", R.string.red_dominion_title, R.string.red_dominion_description, era(RedEra.JURASSIC), mastery = true),
        RedBadgeSpec("red_last_age", R.string.red_last_age_title, R.string.red_last_age_description, era(RedEra.CRETACEOUS)),
        RedBadgeSpec("red_extinction", R.string.red_extinction_title, R.string.red_extinction_description, era(RedEra.CRETACEOUS), mastery = true),
        RedBadgeSpec("red_deep_time", R.string.red_deep_time_title, R.string.red_deep_time_description, all),
        RedBadgeSpec("red_the_red", R.string.red_the_red_title, R.string.red_the_red_description, all, mastery = true),
        RedBadgeSpec("red_apex", R.string.red_apex_title, R.string.red_apex_description, setOf("triassic_herrerasaurus", "jurassic_allosaurus", "cretaceous_tyrannosaurus"), mastery = true),
        RedBadgeSpec("red_colossus", R.string.red_colossus_title, R.string.red_colossus_description, setOf("jurassic_camarasaurus", "jurassic_mamenchisaurus", "jurassic_apatosaurus", "jurassic_diplodocus", "jurassic_giraffatitan", "jurassic_brachiosaurus", "cretaceous_patagotitan", "cretaceous_argentinosaurus"), mastery = true),
        RedBadgeSpec("red_armored", R.string.red_armored_title, R.string.red_armored_description, setOf("jurassic_kentrosaurus", "jurassic_stegosaurus", "cretaceous_ankylosaurus"), mastery = true),
        RedBadgeSpec("red_horned", R.string.red_horned_title, R.string.red_horned_description, setOf("cretaceous_protoceratops", "cretaceous_styracosaurus", "cretaceous_pachyrhinosaurus", "cretaceous_triceratops"), mastery = true),
        RedBadgeSpec("red_raptor", R.string.red_raptor_title, R.string.red_raptor_description, setOf("cretaceous_microraptor", "cretaceous_deinonychus", "cretaceous_utahraptor", "cretaceous_velociraptor"), mastery = true),
        RedBadgeSpec("red_crested", R.string.red_crested_title, R.string.red_crested_description, setOf("jurassic_dilophosaurus", "jurassic_cryolophosaurus", "cretaceous_corythosaurus", "cretaceous_parasaurolophus"), mastery = true),
        RedBadgeSpec("red_feathered", R.string.red_feathered_title, R.string.red_feathered_description, setOf("jurassic_archaeopteryx", "cretaceous_microraptor", "cretaceous_yutyrannus"), mastery = true),
        RedBadgeSpec("red_titans", R.string.red_titans_title, R.string.red_titans_description, setOf("jurassic_brachiosaurus", "cretaceous_patagotitan", "cretaceous_argentinosaurus"), mastery = true),
        RedBadgeSpec("red_clawed", R.string.red_clawed_title, R.string.red_clawed_description, setOf("cretaceous_deinonychus", "cretaceous_utahraptor", "cretaceous_baryonyx", "cretaceous_therizinosaurus"), mastery = true),
        RedBadgeSpec("red_kings", R.string.red_kings_title, R.string.red_kings_description, setOf("jurassic_allosaurus", "cretaceous_carcharodontosaurus", "cretaceous_giganotosaurus", "cretaceous_spinosaurus", "cretaceous_tyrannosaurus"), mastery = true),
        RedBadgeSpec("power_spark", R.string.power_spark_title, R.string.power_spark_description, target = 1),
        RedBadgeSpec("power_resolve", R.string.power_resolve_title, R.string.power_resolve_description, target = 10),
        RedBadgeSpec("power_pressure", R.string.power_pressure_title, R.string.power_pressure_description, target = 600),
        RedBadgeSpec("power_bedrock", R.string.power_bedrock_title, R.string.power_bedrock_description, target = 3000),
        RedBadgeSpec("power_unyielding", R.string.power_unyielding_title, R.string.power_unyielding_description, target = 6000),
        RedBadgeSpec("power_strata", R.string.power_strata_title, R.string.power_strata_description, target = 10),
        RedBadgeSpec("power_adaptation", R.string.power_adaptation_title, R.string.power_adaptation_description, target = 1),
    )
    val byId = specs.associateBy { it.id }
    /** Reconstruct the first qualifying evidence, never the time a reconciliation happened. */
    fun earnedEvidence(
        spec: RedBadgeSpec,
        discoveries: List<com.kingkharnivore.skillz.data.model.entity.shell.CreatureDiscoveryEntity>,
        masteries: List<com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity>,
        sessions: List<SessionEntity>
    ): EvidenceTimestamp? {
        if (spec.species.isNotEmpty()) {
            return if (spec.mastery) {
                AchievementTimestampCalculator.completionTimestamp(spec.species.map { species ->
                    AchievementTimestampCalculator.firstMasteryTimestamp(masteries.filter { it.speciesId == species })
                })
            } else AchievementTimestampCalculator.discoveryVarietyThresholdTimestamp(discoveries, spec.target, spec.species)
        }
        if (spec.id == "power_adaptation") {
            val first = sessions.filter { !it.isSoftMode && it.durationMs > 0 }.groupBy { it.tagId }.values.mapNotNull { journey ->
                val ordered = journey.sortedWith(compareBy<SessionEntity> { it.endTime }.thenBy { it.id })
                var totalPower = 0
                var historicalPower = 0
                ordered.indices.firstOrNull { index ->
                    if (ordered[index].mode == FlowMode.POWER) totalPower++
                    val historicalSize = index + 1 - AdaptationPolicy.RECENT_WINDOW_SIZE
                    if (historicalSize > 0 && ordered[historicalSize - 1].mode == FlowMode.POWER) historicalPower++
                    historicalSize >= AdaptationPolicy.MINIMUM_HISTORICAL_ELIGIBLE_SESSIONS &&
                        historicalPower.toDouble() / historicalSize >= AdaptationPolicy.MINIMUM_HISTORICAL_POWER_SHARE &&
                        (totalPower - historicalPower).toDouble() / AdaptationPolicy.RECENT_WINDOW_SIZE <= AdaptationPolicy.MAXIMUM_RECENT_POWER_SHARE
                }?.let { ordered[it].endTime }
            }.minOrNull()
            return first?.takeIf { it > 0 }?.let { EvidenceTimestamp(it, AchievementTimestampConfidence.EXACT) }
        }
        val eligible = sessions.filter { !it.isSoftMode && it.durationMs > 0 }
            .sortedWith(compareBy<SessionEntity> { it.endTime }.thenBy { it.id })
        var count = 0
        var duration = 0L
        val journeys = mutableSetOf<Long>()
        for (session in eligible) {
            if (session.mode == FlowMode.POWER) { count++; duration += session.durationMs; journeys += session.tagId }
            val reached = when (spec.id) {
                "power_spark", "power_resolve" -> count >= spec.target
                "power_pressure", "power_bedrock", "power_unyielding" -> duration / 60_000L >= spec.target
                "power_strata" -> journeys.size >= spec.target
                else -> false
            }
            if (reached) return session.endTime.takeIf { it > 0 }?.let { EvidenceTimestamp(it, AchievementTimestampConfidence.EXACT) }
        }
        return null
    }

    fun progress(spec: RedBadgeSpec, discovered: Set<String>, mastered: Set<String>, sessions: List<SessionEntity>): Int {
        if (spec.species.isNotEmpty()) return spec.species.intersect(if(spec.mastery) mastered else discovered).size
        val power = sessions.filter { it.mode == FlowMode.POWER && it.durationMs > 0 }
        return when(spec.id) {
            "power_spark", "power_resolve" -> power.size
            "power_pressure", "power_bedrock", "power_unyielding" -> (power.sumOf { it.durationMs } / 60_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            "power_strata" -> power.map { it.tagId }.distinct().size
            "power_adaptation" -> if(AdaptationPolicy.qualifies(sessions)) 1 else 0
            else -> 0
        }
    }
}
