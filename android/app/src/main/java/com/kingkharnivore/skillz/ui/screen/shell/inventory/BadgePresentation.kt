package com.kingkharnivore.skillz.ui.screen.shell.inventory

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.lookout.ObjectiveBadgeIdentity
import com.kingkharnivore.skillz.domain.lookout.ObjectiveBadgePresentationMetadata

enum class BadgeArtworkKind { FLOW_DURATION, SPECIES_MASTERY, COLLECTOR, CURATOR, COMPLETIONIST, MASTERY, ACTIVITY, OBJECTIVE, SPECIAL }
enum class CollectionArtworkIdentity { SUNLIT_REEF, DEEPER_REEF, OPEN_BLUE, GREAT_BLUE, FISHBOWL, AQUARIUM, POND, LAKE, THE_BLUE, STILLWATER, ALL_WATERS, GOLDEN_FIELDS, ANCIENT_WOODS, OPEN_SANDS, HIGH_PEAKS, GREAT_WILD, PASTURE, GLADE, OASIS, RAVINE, SANCTUARY, LAND, LAND_STILLWATER, ALL_LAND, EARTH }

data class BadgePresentation(
    val badgeId: String,
    val title: String,
    val description: String,
    val artworkKind: BadgeArtworkKind,
    val centerLabel: String? = null,
    val creatureIconKey: String? = null,
    val collectionIdentity: CollectionArtworkIdentity? = null,
    val motif: LandBadgeMotif? = null
)

val LocalObjectiveBadgePresentationMetadata = staticCompositionLocalOf<Map<String, ObjectiveBadgePresentationMetadata>> {
    emptyMap()
}

@Composable
fun resolveBadgePresentation(
    badgeId: String,
    objectiveMetadata: ObjectiveBadgePresentationMetadata? = null
): BadgePresentation {
    ObjectiveBadgeIdentity.fromBadgeId(badgeId)?.let { identity ->
        val period = when (identity.periodType) {
            "daily" -> stringResource(R.string.lookout_period_daily)
            "weekly" -> stringResource(R.string.lookout_period_weekly)
            else -> stringResource(R.string.lookout_period_monthly)
        }
        val metadata = objectiveMetadata ?: LocalObjectiveBadgePresentationMetadata.current[badgeId]
        val journey = metadata?.takeIf { it.badgeKey == badgeId }
            ?.journeyNameSnapshot?.takeIf { it.isNotBlank() }
        return BadgePresentation(
            badgeId = badgeId,
            title = if (journey != null) stringResource(R.string.badge_objective_title, journey, period)
                else stringResource(R.string.badge_objective_fallback_title, period),
            description = if (journey != null) stringResource(R.string.badge_objective_description, period, journey)
                else stringResource(R.string.badge_objective_fallback_description, period),
            artworkKind = BadgeArtworkKind.OBJECTIVE,
            centerLabel = period.take(1)
        )
    }
    BadgeBookCollections.byAward[badgeId]?.let { collection ->
        val title = stringResource(collection.titleRes)
        return BadgePresentation(badgeId, stringResource(R.string.book_collection_award_title, title),
            stringResource(R.string.book_collection_award_description, title), BadgeArtworkKind.SPECIAL, motif = LandBadgeMotif.CROWN)
    }
    RedBadgeCatalog.byId[badgeId]?.let { spec ->
        return BadgePresentation(badgeId, stringResource(spec.titleRes), stringResource(spec.descriptionRes), BadgeArtworkKind.SPECIAL, motif = LandBadgeMotif.FOOTPRINTS)
    }
    LandBadgeCatalog.byId[badgeId]?.let { spec ->
        return BadgePresentation(badgeId, stringResource(spec.titleRes), stringResource(spec.descriptionRes),
            BadgeArtworkKind.SPECIAL, centerLabel = when (spec.metric) {
                LandBadgeMetric.ARC_DEPTH -> spec.thresholds.first().toString()
                LandBadgeMetric.TIGERS -> "2"
                LandBadgeMetric.FLAGSHIPS, LandBadgeMetric.HABITATS -> "5"
                else -> null
            }, collectionIdentity = spec.habitat?.let {
                CollectionArtworkIdentity.valueOf(it.name)
            }, motif = spec.motif)
    }
    ShellContentCatalog.badge(badgeId)?.let { legacy ->
        val duration = when (badgeId) {
            "badge_flow_10_min" -> "10"; "badge_flow_30_min" -> "30"
            "badge_flow_60_min" -> "60"; "badge_flow_120_min" -> "120"
            else -> null
        }
        return BadgePresentation(badgeId, stringResource(legacy.titleRes), stringResource(legacy.descriptionRes),
            if (duration != null) BadgeArtworkKind.FLOW_DURATION else BadgeArtworkKind.ACTIVITY, duration)
    }
    val definition = BadgeDefinitionResolver.resolve(badgeId)
    definition?.speciesId?.let { speciesId ->
        val creature = CreatureCatalog.get(speciesId)
        val name = creature?.titleRes?.takeIf { it != 0 }?.let { stringResource(it) }
            ?: stringResource(R.string.badge_creature_fallback)
        return BadgePresentation(badgeId, stringResource(R.string.badge_species_mastery_title, name),
            stringResource(R.string.badge_species_mastery_description, name), BadgeArtworkKind.SPECIES_MASTERY,
            centerLabel = "99", creatureIconKey = creature?.staticIconKey)
    }
    definition?.collectionId?.let { collectionId ->
        val collectionName = collectionDisplayName(collectionId)
        val requirement = definition.requirement
        val title = when (requirement) {
            BadgeRequirement.COLLECTOR -> stringResource(R.string.badge_collector_title, collectionName)
            BadgeRequirement.CURATOR -> stringResource(R.string.badge_curator_title, collectionName)
            BadgeRequirement.COMPLETIONIST -> stringResource(R.string.badge_completionist_title, collectionName)
            BadgeRequirement.EXACT_COUNT -> when (badgeId) {
                "stillwater_first_catch" -> stringResource(R.string.badge_stillwater_first_catch)
                "stillwater_variety" -> stringResource(R.string.badge_stillwater_variety)
                "stillwater_mastery" -> stringResource(R.string.badge_stillwater_mastery)
                else -> stringResource(R.string.badge_collection_progress_title)
            }
        }
        val description = when (requirement) {
            BadgeRequirement.COLLECTOR -> stringResource(R.string.badge_collector_description, collectionName)
            BadgeRequirement.CURATOR -> stringResource(R.string.badge_curator_description, collectionName)
            BadgeRequirement.COMPLETIONIST -> stringResource(R.string.badge_completionist_description, collectionName)
            BadgeRequirement.EXACT_COUNT -> stringResource(R.string.badge_stillwater_progress_description)
        }
        val artwork = when (requirement) {
            BadgeRequirement.COLLECTOR -> BadgeArtworkKind.COLLECTOR
            BadgeRequirement.CURATOR -> BadgeArtworkKind.CURATOR
            BadgeRequirement.COMPLETIONIST -> BadgeArtworkKind.COMPLETIONIST
            BadgeRequirement.EXACT_COUNT -> BadgeArtworkKind.MASTERY
        }
        val identity = when (collectionId) {
            "blue_sunlit_reef" -> CollectionArtworkIdentity.SUNLIT_REEF
            "blue_deeper_reef" -> CollectionArtworkIdentity.DEEPER_REEF
            "blue_open_blue" -> CollectionArtworkIdentity.OPEN_BLUE
            "blue_great_blue" -> CollectionArtworkIdentity.GREAT_BLUE
            "stillwater_fishbowl" -> CollectionArtworkIdentity.FISHBOWL
            "stillwater_aquarium" -> CollectionArtworkIdentity.AQUARIUM
            "stillwater_pond" -> CollectionArtworkIdentity.POND
            "stillwater_lake" -> CollectionArtworkIdentity.LAKE
            "blue_golden_fields" -> CollectionArtworkIdentity.GOLDEN_FIELDS
            "blue_ancient_woods" -> CollectionArtworkIdentity.ANCIENT_WOODS
            "blue_open_sands" -> CollectionArtworkIdentity.OPEN_SANDS
            "blue_high_peaks" -> CollectionArtworkIdentity.HIGH_PEAKS
            "blue_great_wild" -> CollectionArtworkIdentity.GREAT_WILD
            "stillwater_pasture" -> CollectionArtworkIdentity.PASTURE
            "stillwater_glade" -> CollectionArtworkIdentity.GLADE
            "stillwater_oasis" -> CollectionArtworkIdentity.OASIS
            "stillwater_ravine" -> CollectionArtworkIdentity.RAVINE
            "stillwater_sanctuary" -> CollectionArtworkIdentity.SANCTUARY
            "collection_land" -> CollectionArtworkIdentity.LAND
            "collection_land_stillwater" -> CollectionArtworkIdentity.LAND_STILLWATER
            "collection_sea_stillwater" -> CollectionArtworkIdentity.STILLWATER
            "collection_all_land" -> CollectionArtworkIdentity.ALL_LAND
            "collection_living_earth" -> CollectionArtworkIdentity.EARTH
            "collection_the_blue" -> CollectionArtworkIdentity.THE_BLUE
            "collection_stillwater" -> CollectionArtworkIdentity.STILLWATER
            "collection_all_waters" -> CollectionArtworkIdentity.ALL_WATERS
            else -> null
        }
        return BadgePresentation(badgeId, title, description, artwork, collectionIdentity = identity)
    }
    val known = when (badgeId) {
        "mastery_first" -> R.string.badge_first_mastery to R.string.badge_first_mastery_description
        "mastery_circle" -> R.string.badge_mastery_circle to R.string.badge_mastery_circle_description
        "mastery_variety" -> R.string.badge_mastery_variety to R.string.badge_mastery_variety_description
        "variety_collector" -> R.string.badge_variety_collector to R.string.badge_variety_collector_description
        "across_the_depths" -> R.string.badge_across_depths to R.string.badge_across_depths_description
        "one_from_every_water" -> R.string.badge_every_water to R.string.badge_every_water_description
        "keeper_of_the_blue" -> R.string.badge_keeper_blue to R.string.badge_keeper_blue_description
        else -> null
    }
    if (known != null) return BadgePresentation(badgeId, stringResource(known.first), stringResource(known.second), BadgeArtworkKind.SPECIAL, motif = when (badgeId) {
        "across_the_depths", "keeper_of_the_blue", "one_from_every_water" -> LandBadgeMotif.SEA
        "mastery_first", "mastery_circle", "mastery_variety" -> LandBadgeMotif.CROWN
        "variety_collector" -> LandBadgeMotif.EARTH
        else -> null
    })
    Log.w("BadgePresentation", "Unresolved persisted badge id: $badgeId")
    return BadgePresentation(badgeId, stringResource(R.string.badge_unavailable_record_title),
        stringResource(R.string.badge_unavailable_record_description), BadgeArtworkKind.SPECIAL)
}

@Composable
fun collectionDisplayName(collectionId: String): String = stringResource(when (collectionId) {
    "collection_red" -> R.string.red_title
    "red_triassic" -> R.string.red_triassic
    "red_jurassic" -> R.string.red_jurassic
    "red_cretaceous" -> R.string.red_cretaceous
    "blue_sunlit_reef" -> R.string.collection_sunlit_reef
    "blue_deeper_reef" -> R.string.collection_deeper_reef
    "blue_open_blue" -> R.string.collection_open_blue
    "blue_great_blue" -> R.string.collection_great_blue
    "blue_golden_fields" -> R.string.land_zone_golden_fields
    "blue_ancient_woods" -> R.string.land_zone_ancient_woods
    "blue_open_sands" -> R.string.land_zone_open_sands
    "blue_high_peaks" -> R.string.land_zone_high_peaks
    "blue_great_wild" -> R.string.land_zone_great_wild
    "stillwater_pasture" -> R.string.land_zone_pasture
    "stillwater_glade" -> R.string.land_zone_glade
    "stillwater_oasis" -> R.string.land_zone_oasis
    "stillwater_ravine" -> R.string.land_zone_ravine
    "stillwater_sanctuary" -> R.string.land_zone_sanctuary
    "stillwater_fishbowl" -> R.string.collection_fishbowl
    "stillwater_aquarium" -> R.string.collection_aquarium
    "stillwater_pond" -> R.string.collection_pond
    "stillwater_lake" -> R.string.collection_lake
    "collection_stillwater" -> R.string.collection_stillwater
    "collection_land" -> R.string.collection_land
    "collection_sea_stillwater" -> R.string.collection_sea_stillwater
    "collection_land_stillwater" -> R.string.collection_land_stillwater
    "collection_all_land" -> R.string.collection_all_land
    "collection_living_earth" -> R.string.collection_living_earth
    "collection_the_blue" -> R.string.collection_the_blue
    "collection_all_waters" -> R.string.collection_all_waters
    else -> R.string.collection_unknown
})
