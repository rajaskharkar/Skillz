package com.kingkharnivore.skillz.ui.screen.shell.inventory

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog

enum class BadgeArtworkKind { FLOW_DURATION, SPECIES_MASTERY, COLLECTOR, CURATOR, COMPLETIONIST, MASTERY, ACTIVITY, SPECIAL }

data class BadgePresentation(
    val badgeId: String,
    val title: String,
    val description: String,
    val artworkKind: BadgeArtworkKind,
    val centerLabel: String? = null,
    val creatureIconKey: String? = null
)

@Composable
fun resolveBadgePresentation(badgeId: String): BadgePresentation {
    ShellContentCatalog.badge(badgeId)?.let { legacy ->
        val duration = when (badgeId) {
            "badge_flow_10_min" -> "10"; "badge_flow_30_min" -> "30"
            "badge_flow_60_min" -> "60"; "badge_flow_120_min" -> "120"
            else -> null
        }
        return BadgePresentation(badgeId, stringResource(legacy.titleRes), stringResource(legacy.descriptionRes),
            if (duration != null) BadgeArtworkKind.FLOW_DURATION else BadgeArtworkKind.ACTIVITY, duration)
    }
    val definition = AchievementBadgeCatalog.byId[badgeId]
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
                else -> stringResource(R.string.badge_generic_title)
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
        return BadgePresentation(badgeId, title, description, artwork)
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
    if (known != null) return BadgePresentation(badgeId, stringResource(known.first), stringResource(known.second), BadgeArtworkKind.SPECIAL)
    Log.w("BadgePresentation", "Unresolved persisted badge id: $badgeId")
    return BadgePresentation(badgeId, stringResource(R.string.badge_generic_title),
        stringResource(R.string.badge_generic_description), BadgeArtworkKind.SPECIAL)
}

@Composable
fun collectionDisplayName(collectionId: String): String = stringResource(when (collectionId) {
    "blue_sunlit_reef" -> R.string.collection_sunlit_reef
    "blue_deeper_reef" -> R.string.collection_deeper_reef
    "blue_open_blue" -> R.string.collection_open_blue
    "blue_great_blue" -> R.string.collection_great_blue
    "stillwater_fishbowl" -> R.string.collection_fishbowl
    "stillwater_aquarium" -> R.string.collection_aquarium
    "stillwater_pond" -> R.string.collection_pond
    "stillwater_lake" -> R.string.collection_lake
    "collection_stillwater" -> R.string.collection_stillwater
    "collection_the_blue" -> R.string.collection_the_blue
    "collection_all_waters" -> R.string.collection_all_waters
    else -> R.string.collection_unknown
})
