package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class CreatureGrowthBadgesTest {
    private fun instance(id: String, species: String, level: Int) = UserShellFindInstanceEntity(id, species, 1, "fixture", null, null, null, false, true, animalLevel = level)
    private fun model(spec: CreatureGrowthBadge, instances: List<UserShellFindInstanceEntity>, stored: List<UserBadgeEntity> = emptyList()) =
        BadgeDashboardCalculator.calculate(stored, instances, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()).badges.first { it.badgeId == spec.id }

    @Test fun everyRealmHasEarlyMiddleAndLateGrowthAndSmallCollections() {
        assertEquals(80, CreatureGrowthBadges.specs.size)
        assertEquals(CreatureGrowthBadges.specs.size, CreatureGrowthBadges.byId.size)
        CreatureRealm.entries.forEach { realm ->
            val goals = CreatureGrowthBadges.specs.filter { it.realm == realm }
            assertTrue(goals.map { it.level }.containsAll(listOf(5,15,30,45,60,75)))
            assertTrue(goals.any { it.level == 15 && it.target == 3 })
            assertTrue(goals.any { it.metric == CreatureGrowthMetric.SAME_SPECIES_COPIES })
            assertTrue(goals.any { it.metric == CreatureGrowthMetric.HABITATS })
        }
        CreatureGrowthBadges.specs.forEach { spec ->
            assertTrue(spec.species.isNotEmpty())
            assertTrue(spec.species.all { CreatureCatalog.require(it).realm == spec.realm })
            assertEquals(spec.definition, AchievementBadgeCatalog.byId[spec.id])
        }
    }

    @Test fun everyGoalRequiresItsExactBoundaryAndKeepsPartialCountsUnawarded() {
        CreatureGrowthBadges.specs.forEach { spec ->
            val roster = spec.species.mapIndexed { index, id -> instance("$index", id, spec.level) }.let {
                if (spec.metric == CreatureGrowthMetric.SAME_SPECIES_COPIES) List(spec.target) { i -> instance("copy-$i", spec.species.first(), spec.level) } else it
            }
            assertEquals(spec.id,0,CreatureGrowthBadges.progress(spec,roster.map { it.copy(animalLevel=spec.level-1) }))
            assertTrue(spec.id,CreatureGrowthBadges.progress(spec,roster)>=spec.target)
            assertTrue(spec.id,model(spec,roster).terminal)
            if (spec.target>1) {
                val partial=model(spec,roster.take(1))
                assertFalse(spec.id,partial.earned);assertEquals(spec.id,0,partial.count)
                assertTrue(spec.id,partial.canTrack)
            }
        }
    }

    @Test fun duplicateRowsCannotPretendToBeCopiesSpeciesOrHabitats() {
        val goals=CreatureGrowthBadges.specs.filter { it.realm==CreatureRealm.SEA && it.collectionId==null }
        val row=instance("one",goals.first().species.first(),60)
        goals.filter { it.target>1 }.forEach { assertEquals(it.id,1,CreatureGrowthBadges.progress(it,List(20) { row })) }
        val copies=List(3) { row.copy(instanceId="copy-$it") }
        assertEquals(3,CreatureGrowthBadges.progress(goals.first { it.metric==CreatureGrowthMetric.SAME_SPECIES_COPIES },copies))
        assertEquals(1,CreatureGrowthBadges.progress(goals.first { it.id.endsWith("trio") },copies))
    }

    @Test fun releasedTradedAndMasteryOnlyEvidenceIsRetainedWithoutCountingTwice() {
        val spec=CreatureGrowthBadges.byId.getValue("growth_v1_sea_pair")
        val species=spec.species.first()
        val rows=listOf(instance("one",species,99).copy(creatureStatus=CreatureStatus.RELEASED),instance("two",species,30).copy(creatureStatus=CreatureStatus.USED_BEYOND_BLUE))
        val event=CreatureMasteryEventEntity("mastery","one",species,100,"growth")
        assertEquals(2,CreatureGrowthBadges.progress(spec,CreatureGrowthBadges.evidence(rows,listOf(event))))
        assertEquals(2,CreatureGrowthBadges.progress(spec,CreatureGrowthBadges.evidence(rows.drop(1),listOf(event))))
        val old=UserBadgeEntity(spec.id,1,123,123,false,viewedAt=150)
        val kept=model(spec,emptyList(),listOf(old))
        assertTrue(kept.terminal);assertEquals(123L,kept.firstEarnedAt);assertEquals(1,kept.count)
    }

    @Test fun legacySpeciesMasteryFloorsProveGrowthWithoutUnboundedSyntheticCopies() {
        val spec=CreatureGrowthBadges.byId.getValue("growth_v1_land_pair")
        val species=spec.species.first()
        val evidence=CreatureGrowthBadges.evidence(emptyList(),emptyList(),mapOf(species to Int.MAX_VALUE))
        assertEquals(2,evidence.size)
        assertEquals(2,CreatureGrowthBadges.progress(spec,evidence))
        val retained=listOf(instance("one",species,99))
        assertEquals(2,CreatureGrowthBadges.evidence(retained,emptyList(),mapOf(species to 2)).size)
    }

    @Test fun nextStepPrefersClosestActiveUnfinishedSpeciesAndIncludesLegacyBlue() {
        val spec=CreatureGrowthBadges.byId.getValue("growth_v1_sea_trio")
        val ids=spec.species.take(3)
        val rows=listOf(instance("done",ids[0],15),instance("near",ids[1],14),instance("young",ids[2],5))
        assertEquals(ids[1],CreatureGrowthBadges.nextSpecies(spec,rows))
        assertEquals(BadgeActionDestination.ChestSpecies(ids[1]),model(spec,rows).action)
        CreatureCatalog.all.filter { it.isHeritageSpecies && it.realm!=CreatureRealm.RED }.forEach { legacy ->
            assertTrue(legacy.creatureId,CreatureGrowthBadges.specs.any { it.realm==legacy.realm && legacy.creatureId in it.species })
        }
    }
}
