package com.kingkharnivore.skillz.domain.power

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class RedBadgeIntegrationTest {
    private val red = RedBadgeCatalog.specs.filter { it.id.startsWith("red_") }
    private fun discovery(id: String, time: Long = 100) = CreatureDiscoveryEntity(id,time,"red_purchase",id,time)
    private fun mastery(id: String, time: Long = 200) = CreatureMasteryEventEntity("master-$id",id,id,time,"growth-$id")
    private fun dashboard(discoveries: List<CreatureDiscoveryEntity> = emptyList(), masteries: List<CreatureMasteryEventEntity> = emptyList(),
        instances: List<UserShellFindInstanceEntity> = emptyList(), earned: List<UserBadgeEntity> = emptyList()) =
        BadgeDashboardCalculator.calculate(earned,instances,discoveries,masteries,emptyList(),emptyList(),emptyList())

    @Test fun onlySpecifiedNamedRedBadgesAndSharedSpeciesMasteriesAreRegistered() {
        val expected = setOf("first_footprint","dawn","ascendant","giants","dominion","last_age","extinction","deep_time","the_red",
            "apex","colossus","armored","horned","raptor","crested","feathered","titans","clawed","kings").map { "red_$it" }.toSet()
        assertEquals(expected,red.map { it.id }.toSet())
        val definitions = AchievementBadgeCatalog.definitions
        assertEquals(definitions.size,definitions.distinctBy { it.badgeId }.size)
        assertEquals(expected,definitions.filter { it.badgeId.startsWith("red_") }.map { it.badgeId }.toSet())
        assertEquals(69,definitions.count { it.speciesId in RedCreatureCatalog.byId })
        assertTrue(definitions.none { RedBadgeCatalog.isRedundantCollectionBadge(it.badgeId) })
        val historical = listOf(UserBadgeEntity("red_triassic_collector",1,1,1,false),UserBadgeEntity("collection_red_completionist",1,1,1,false))
        assertTrue(dashboard(earned=historical).badges.none { it.badgeId in historical.map { b -> b.badgeId } })
    }

    @Test fun everyNamedRedBadgeHasCorrectCategoryAndTrackableUnlockedNextStep() {
        val badges = dashboard().badges.associateBy { it.badgeId }
        red.forEach { spec ->
            val badge = badges.getValue(spec.id)
            assertEquals(spec.id,if(spec.mastery) BadgeUiCategory.MASTERY else BadgeUiCategory.COLLECTIONS,badge.category)
            assertEquals(spec.id,spec.target,badge.target)
            assertFalse(spec.id,badge.earned)
            assertTrue(spec.id,badge.canTrack && badge.canNavigate && badge.canProgressNow)
            assertNull(spec.id,badge.disabledReason)
            assertTrue(spec.id,(badge.action as BadgeActionDestination.ChestSpecies).speciesId in spec.species)
        }
    }

    @Test fun eachRosterRequiresEveryDistinctSpeciesAndRetainsLifetimeEvidenceAfterRelease() {
        red.forEach { spec ->
            val partial = spec.species.take((spec.target-1).coerceAtLeast(0))
            val partialDiscoveries = if(spec.mastery) spec.species.map { discovery(it) } else partial.map { discovery(it) }
            val before = dashboard(partialDiscoveries,partial.map { mastery(it) }).badges.first { it.badgeId==spec.id }
            assertFalse(spec.id,before.earned)
            assertEquals(spec.id,spec.target-1,before.progress)
            val full = spec.species.map { discovery(it) }
            val masteries = spec.species.map { mastery(it) }
            val after = dashboard(full,masteries).badges.first { it.badgeId==spec.id }
            assertTrue(spec.id,after.earned && after.terminal)
            assertEquals(1,after.count)
            assertFalse(after.canTrack)
            // No active instances: discovery and mastery events survive release/trade.
            val duplicate = dashboard(full,masteries+masteries.map { it.copy(eventId="repeat-${it.eventId}",creatureInstanceId="repeat-${it.creatureInstanceId}") })
                .badges.first { it.badgeId==spec.id }
            assertEquals(after.progress,duplicate.progress)
            assertEquals(1,duplicate.count)
        }
    }

    @Test fun masteryNextStepPrefersClosestOwnedMissingSpeciesAndNeverRoutesToBlue() {
        val apex=RedBadgeCatalog.byId.getValue("red_apex")
        val high=UserShellFindInstanceEntity("rex","cretaceous_tyrannosaurus",1,"red_purchase",null,null,null,false,true,animalLevel=98)
        val low=high.copy(instanceId="allo",findId="jurassic_allosaurus",animalLevel=20)
        val badges=dashboard(instances=listOf(high,low)).badges.associateBy { it.badgeId }
        assertEquals(BadgeActionDestination.ChestSpecies(high.findId),badges.getValue(apex.id).action)
        val unowned=badges.getValue("mastery_species_triassic_saturnalia")
        assertEquals(BadgeActionDestination.ChestSpecies("triassic_saturnalia"),unowned.action)
        assertTrue(unowned.canTrack)
    }

    @Test fun earnedDatesComeFromFirstCompleteRosterAndUnknownEvidenceStaysUnknown() {
        val dawn=RedBadgeCatalog.byId.getValue("red_dawn")
        val discoveries=dawn.species.mapIndexed { i,id -> discovery(id,100L+i) }
        assertEquals(113L,RedBadgeCatalog.earnedEvidence(dawn,discoveries,emptyList(),emptyList())?.timestamp)
        assertEquals(100L,RedBadgeCatalog.earnedEvidence(RedBadgeCatalog.byId.getValue("red_first_footprint"),discoveries,emptyList(),emptyList())?.timestamp)
        val apex=RedBadgeCatalog.byId.getValue("red_apex")
        val mastered=apex.species.mapIndexed { i,id -> mastery(id,200L+i) }
        assertEquals(202L,RedBadgeCatalog.earnedEvidence(apex,emptyList(),mastered,emptyList())?.timestamp)
        assertNull(RedBadgeCatalog.earnedEvidence(apex,emptyList(),mastered.drop(1),emptyList()))
        val estimated=mastered.map { it.copy(levelUpTransactionId="backfill_${it.speciesId}") }
        assertEquals(AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION,RedBadgeCatalog.earnedEvidence(apex,emptyList(),estimated,emptyList())?.confidence)
    }
    @Test fun trackedRedMasteryFiltersChestToOnlyOutstandingFamilyMembers() {
        val apex=RedBadgeCatalog.byId.getValue("red_apex")
        val mastered=apex.species.take(2)
        val badges=dashboard(masteries=mastered.map { mastery(it) }).badges.map { it.copy(tracked=it.badgeId==apex.id) }
        assertEquals(apex.species-mastered.toSet(),com.kingkharnivore.skillz.ui.screen.shell.inventory.speciesNeededForTrackedBadges(badges))
        val completed=dashboard(masteries=apex.species.map { mastery(it) }).badges.map { it.copy(tracked=it.badgeId==apex.id) }
        assertTrue(com.kingkharnivore.skillz.ui.screen.shell.inventory.speciesNeededForTrackedBadges(completed).isEmpty())
    }

}
