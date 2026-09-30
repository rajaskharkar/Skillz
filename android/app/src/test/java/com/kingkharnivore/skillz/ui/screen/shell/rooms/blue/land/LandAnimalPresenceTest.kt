package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land

import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.lifePresencePlan
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.LandCreatureCatalog
import org.junit.Assert.*
import org.junit.Test

class LandAnimalPresenceTest {
    @Test fun thirteenTigersUseSeaIndividualLimitAndOneOverflowCohort() {
        val animal = group("creature_tiger", 13)
        val visible = landAnimalPresence(listOf(animal), 6)
        assertEquals(4, visible.count { !it.isCohort })
        assertEquals(9, visible.single { it.isCohort }.countRepresented)
        assertEquals(13, visible.sumOf { it.countRepresented })
        assertEquals(.76f, visible.single { it.isCohort }.scale, 0f)
        assertEquals(visible.size, visible.map { it.key }.distinct().size)
        assertEquals(visible.size, visible.map { it.representativeIndex }.distinct().size)
    }

    @Test fun zeroOneAndSeveralCopiesAreNotConfusedWithSpeciesCount() {
        for (count in 0..4) {
            val visible = landAnimalPresence(listOf(group("creature_tiger", count)), 6)
            assertEquals(count, visible.size)
            assertTrue(visible.none { it.isCohort })
            assertEquals(count, visible.sumOf { it.countRepresented })
        }
    }

    @Test fun everyLandSpeciesUsesTheSamePlanAsSeaWhenThereIsSpace() {
        LandCreatureCatalog.all.forEach { definition ->
            val animal = group(definition.creatureId, 100)
            val seaPlan = lifePresencePlan(animal, definition)
            val visible = landAnimalPresence(listOf(animal), 20)
            assertEquals(definition.creatureId, seaPlan.directIndividuals.map { it.key },
                visible.filterNot { it.isCohort }.map { it.key })
            assertEquals(seaPlan.cohorts.sumOf { it.count }, visible.filter { it.isCohort }.sumOf { it.countRepresented })
            assertEquals(100, visible.sumOf { it.countRepresented })
        }
    }

    @Test fun crampedScenesFoldUnplacedIndividualsIntoCohortWithoutDoubleCounting() {
        for (capacity in 0..6) {
            val visible = landAnimalPresence(listOf(group("creature_tiger", 13)), capacity)
            assertTrue(visible.size <= capacity.coerceAtLeast(1))
            assertEquals(13, visible.sumOf { it.countRepresented })
            assertEquals(1, visible.count { it.isCohort })
        }
    }

    @Test fun denseScenesPreserveEverySpeciesInsteadOfTruncatingAtSix() {
        val groups = LandCreatureCatalog.all.filter { it.zone.name == "GREAT_WILD" }
            .map { group(it.creatureId, 13) }
        assertTrue(groups.size > 6)
        val visible = landAnimalPresence(groups, 6)
        assertEquals(groups.size, visible.size)
        groups.forEach { animal ->
            assertEquals(13, visible.filter { it.animal.findId == animal.findId }.sumOf { it.countRepresented })
        }
        assertEquals(visible, landAnimalPresence(groups.reversed(), 6))
    }

    @Test fun spareLanesAreSharedByCopiesAcrossSpecies() {
        val groups = listOf("gorilla", "baboon", "tiger", "lion", "crocodile", "grizzly_bear")
            .map { group("creature_$it", 13) }
        val visible = landAnimalPresence(groups, 12)
        assertEquals(12, visible.size)
        groups.forEach { animal ->
            val species = visible.filter { it.animal.findId == animal.findId }
            assertEquals(2, species.size)
            assertEquals(1, species.count { !it.isCohort })
            assertEquals(13, species.sumOf { it.countRepresented })
        }
    }

    @Test fun hugeOwnershipDoesNotAllocateAnAnimationForEachCopy() {
        val groups = listOf(group("creature_tiger", Int.MAX_VALUE), group("creature_lion", Int.MAX_VALUE))
        val visible = landAnimalPresence(groups, 6)
        assertEquals(6, visible.size)
        groups.forEach { animal ->
            assertEquals(Int.MAX_VALUE, visible.filter { it.animal.findId == animal.findId }.sumOf { it.countRepresented })
        }
        assertEquals(visible.size, visible.map { it.key }.distinct().size)
    }

    private fun group(id: String, count: Int) = TheBlueAnimalGroupUiModel(
        findId = id, zoneId = TheBlueZoneId.valueOf(CreatureCatalog.require(id).zone.name),
        totalCount = count, displayedInFocusCount = 0, restingCount = count,
        bestFormStageId = null, formCounts = emptyList(), iconKey = null, isNew = false
    )
}
