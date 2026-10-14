package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.domain.achievement.BadgeDefinitionResolver
import com.kingkharnivore.skillz.domain.achievement.significantAchievementOrder

internal data class MasteryCountChange(
    val previous: Int,
    val current: Int
) {
    val changed: Boolean get() = previous != current
}

internal data class MasteryCollectionChange(
    val collectionId: String,
    val progress: MasteryCountChange,
    val total: Int
)

/** Immutable rendering state derived only from the persisted post-transaction snapshot. */
internal data class MasteryCelebrationUiState(
    val speciesId: String,
    val artworkKey: String,
    val previousLevel: Int,
    val newLevel: Int,
    val speciesMasteries: MasteryCountChange,
    val totalMasteries: MasteryCountChange,
    val uniqueSpecies: MasteryCountChange,
    val collections: List<MasteryCollectionChange>,
    val newlyEarnedBadgeIds: List<String>,
    val speciesMilestone: Int?
)

internal object MasteryCelebrationUiStateMapper {
    fun map(event: MasteryCelebrationEventEntity): MasteryCelebrationUiState {
        val firstMasteryForSpecies = event.speciesMasteryCount == 1
        val collectionDelta = if (firstMasteryForSpecies) 1 else 0
        val isLand = com.kingkharnivore.skillz.utils.shell.CreatureCatalog.get(event.speciesId)?.realm == com.kingkharnivore.skillz.utils.shell.CreatureRealm.LAND
        val retiredSnapshot = com.kingkharnivore.skillz.domain.achievement.RetiredStillwaterBadges.isCollection(event.regionId)
        val sourceCollection = MasteryCollectionChange(
            "collection_the_blue", countChange(event.blueMastered, collectionDelta), event.blueTotal)
        val collectionChanges = listOfNotNull(
            MasteryCollectionChange(
                event.regionId,
                countChange(event.regionalMastered, collectionDelta),
                event.regionalTotal
            ),
            sourceCollection.takeUnless { isLand || retiredSnapshot },
            MasteryCollectionChange(
                "collection_all_waters",
                countChange(event.allWatersMastered, collectionDelta),
                event.allWatersTotal
            ).takeUnless { isLand || retiredSnapshot }
        ).filterNot { com.kingkharnivore.skillz.domain.achievement.RetiredStillwaterBadges.isCollection(it.collectionId) }.distinctBy { it.collectionId }

        val earned = event.newlyEarnedBadgeIds.csvValues()
            .filter(BadgeDefinitionResolver::isUserVisible)
            .distinct()
            .sortedBy(::significantAchievementOrder)
        val speciesBadgeId = "mastery_species_${event.speciesId}"
        val speciesMilestone = event.milestonesReached.csvValues().mapNotNull { encoded ->
            val separator = encoded.lastIndexOf(':')
            if (separator <= 0 || encoded.substring(0, separator) != speciesBadgeId) return@mapNotNull null
            encoded.substring(separator + 1).toIntOrNull()?.takeIf { it > 1 }
        }.maxOrNull()

        return MasteryCelebrationUiState(
            speciesId = event.speciesId,
            artworkKey = event.artworkKey,
            previousLevel = event.previousLevel,
            newLevel = event.newLevel,
            speciesMasteries = countChange(event.speciesMasteryCount, 1),
            totalMasteries = countChange(event.totalMasteries, 1),
            uniqueSpecies = countChange(event.uniqueMasteredSpecies, collectionDelta),
            collections = collectionChanges,
            newlyEarnedBadgeIds = earned,
            speciesMilestone = speciesMilestone
        )
    }

    private fun countChange(current: Int, eventDelta: Int) = MasteryCountChange(
        previous = (current - eventDelta).coerceAtLeast(0),
        current = current.coerceAtLeast(0)
    )

    private fun String.csvValues(): List<String> = split(',').map(String::trim).filter(String::isNotEmpty)
}
