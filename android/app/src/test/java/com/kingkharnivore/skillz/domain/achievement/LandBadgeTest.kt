package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class LandBadgeTest {
    private fun copy(id: String, source: String="beyond_blue", origin: String?="test", level:Int=1, status:String=CreatureStatus.ACTIVE, suffix:String="0") = UserShellFindInstanceEntity(
        "$id:$origin:$suffix",id,100,source,origin,null,null,false,true,animalLevel=level,creatureStatus=status)
    private fun dashboard(copies:List<UserShellFindInstanceEntity> = emptyList(), earned:List<UserBadgeEntity> = emptyList(), masteries:List<CreatureMasteryEventEntity> = emptyList(), pins:List<BadgePinEntity> = emptyList()) = BadgeDashboardCalculator.calculate(
        earned,copies,copies.distinctBy { it.findId }.map { CreatureDiscoveryEntity(it.findId,100,it.sourceType,it.instanceId,100) },masteries,emptyList(),pins,emptyList())
    private fun BadgeDashboard.badge(id:String) = badges.single { it.badgeId==id }
    private fun arc(depth:Int, id:String="arc1") = LandArcRewards.forFlowCount(depth).flatMap { reward ->
        (0 until reward.quantity).map { copy(reward.creatureId,"arc",id,suffix=it.toString()) }
    }

    @Test fun everySpeciesAndEveryRegionHasAchievableCollectionAndMasteryBadges() {
        assertEquals(185, CreatureCatalog.all.count { it.realm != com.kingkharnivore.skillz.utils.shell.CreatureRealm.RED })
        assertEquals(69, CreatureCatalog.all.count { it.realm == com.kingkharnivore.skillz.utils.shell.CreatureRealm.RED })
        assertEquals(254, CreatureCatalog.all.size)
        val definitions = AchievementBadgeCatalog.definitions
        assertEquals(definitions.size, definitions.map { it.badgeId }.distinct().size)
        CreatureCatalog.all.forEach { c ->
            assertEquals(c.creatureId, BadgeDefinitionResolver.resolve("mastery_species_${c.creatureId}").speciesId)
        }
        assertEquals(114, LandCreatureCatalog.all.count { c -> definitions.any { it.speciesId==c.creatureId } })
        CollectionCatalog.collections.forEach { collection ->
            assertTrue(collection.species.isNotEmpty())
            if (RedBadgeCatalog.isRedCollection(collection.collectionId)) {
                val roster = collection.species.map { it.creatureId }.toSet()
                val named = RedBadgeCatalog.specs.filter { it.species == roster && it.target == roster.size }
                assertEquals("Red uses its specified acquisition/mastery pair", 2, named.size)
                assertEquals(setOf(false,true), named.map { it.mastery }.toSet())
                named.forEach { assertNotNull(AchievementBadgeCatalog.byId[it.id]) }
                listOf("collector","curator","completionist").forEach { kind ->
                    assertNull(AchievementBadgeCatalog.byId["${collection.collectionId}_$kind"])
                }
            } else {
                listOf("collector","curator","completionist").forEach { kind ->
                    assertNotNull(AchievementBadgeCatalog.byId["${collection.collectionId}_$kind"])
                }
            }
        }
        val initial=dashboard()
        assertTrue(initial.badges.filter { LandBadgeCatalog.byId.containsKey(it.badgeId) }.all { !it.earned && it.canTrack })
    }

    @Test fun collectingAndMasteringLandCompletesEveryLandRegionWithoutCompletingSea() {
        val owned = LandCreatureCatalog.all.map { copy(it.creatureId, level = 99) }
        val masteries = owned.map { CreatureMasteryEventEntity("m:${it.instanceId}", it.instanceId, it.findId, 100, "grow:${it.instanceId}") }
        val result = dashboard(owned, masteries = masteries)
        val landCollections = CollectionCatalog.collections.filter { collection ->
            collection.species.all { it.realm == CreatureRealm.LAND }
        }
        assertEquals(7, landCollections.size) // Five regions and two Land aggregates.
        landCollections.forEach { collection ->
            listOf("collector", "curator", "completionist").forEach { kind ->
                assertTrue("${collection.collectionId}_$kind", result.badge("${collection.collectionId}_$kind").earned)
            }
        }
        owned.forEach { assertEquals(1, result.badge("mastery_species_${it.findId}").count) }
        assertTrue(result.badge("one_from_every_land").earned)
        assertTrue(result.badge("keeper_of_the_land").earned)
        assertFalse(result.badge("collection_the_blue_collector").earned)
        assertFalse(result.badge("collection_living_earth_collector").earned)
    }

    @Test fun mainLandDiscoveryDoesNotCompleteRestorativeCollections() {
        val result = dashboard(LandCreatureCatalog.main.map { copy(it.creatureId) })
        assertFalse(result.badge("collection_land_collector").earned)
        assertTrue(result.badges.none { RetiredStillwaterBadges.isBadge(it.badgeId) })
        assertFalse(result.badge("collection_all_land_collector").earned)
        assertEquals(63, result.badge("collection_all_land_collector").progress)
        assertEquals(114, result.badge("collection_all_land_collector").target)
    }

    @Test fun blueRostersIncludeHeritageWhileHistoricalIdsRemain() {
        assertEquals(71,CollectionCatalog.byId.getValue("collection_the_blue").species.size)
        assertEquals(71,CollectionCatalog.byId.getValue("collection_all_waters").species.size)
        assertEquals(114,CollectionCatalog.byId.getValue("collection_land").species.size)
        assertEquals(114,CollectionCatalog.byId.getValue("collection_all_land").species.size)
        assertEquals(185,CollectionCatalog.byId.getValue("collection_living_earth").species.size)
        val sea=CreatureCatalog.all.filter { it.realm==CreatureRealm.SEA }.map { copy(it.creatureId) }
        val result=dashboard(sea)
        for(id in listOf("across_the_depths","keeper_of_the_blue","collection_the_blue_collector","collection_all_waters_collector")) assertTrue(id,result.badge(id).earned)
        assertFalse(result.badge("across_the_land").earned)
        assertFalse(result.badge("collection_living_earth_collector").earned)
    }

    @Test fun thresholdBadgesDoNotUnlockAtTheFirstProgressPoint() {
        val one=dashboard(listOf(copy("creature_duck")))
        assertTrue(one.badge("land_first").earned)
        assertFalse(one.badge("land_variety").earned)
        assertEquals(1,one.badge("land_variety").progress)
        assertEquals(5,one.badge("land_variety").target)
        assertFalse(one.badge("living_earth_first").earned)
        val five=dashboard(LandCreatureCatalog.main.take(5).map { copy(it.creatureId) })
        assertTrue(five.badge("land_variety").earned)
        assertEquals(10,five.badge("land_variety").nextTarget)
        val growing=dashboard(listOf(copy("creature_duck",level=9)))
        assertFalse(growing.badge("land_growth").earned)
        assertTrue(dashboard(listOf(copy("creature_duck",level=10))).badge("land_growth").earned)
    }

    @Test fun arcDepthBadgesUseFinalizedAcquisitionEvidenceAndKeepPackagesDistinct() {
        val pending=dashboard()
        assertFalse(pending.badge("land_arc_3").earned)
        for(depth in listOf(3,6,9,12,15,18,30,33,63)) {
            val result=dashboard(arc(depth))
            for(threshold in listOf(3,6,9,12,15)) assertEquals("depth=$depth threshold=$threshold",depth>=threshold,result.badge("land_arc_$threshold").earned)
            assertEquals(depth>=30,result.badge("land_two_tigers").earned)
            assertFalse(result.badge("land_five_companions").earned)
        }
        assertFalse(dashboard(arc(15,"a")+arc(15,"b")).badge("land_two_tigers").earned)
        val five=(1..5).flatMap { arc(it*3,"arc$it") }
        assertTrue(dashboard(five).badge("land_five_companions").earned)
        assertTrue(dashboard(five).badge("land_returning").earned)
        assertFalse(dashboard(five).badge("land_two_tigers").earned)
        // A debug grant of a flagship is not an Arc completion.
        assertFalse(dashboard(listOf(copy("creature_tiger","debug"))).badge("land_arc_15").earned)
    }

    @Test fun releaseContributionAndReloadDoNotEraseEarnedProgressOrMastery() {
        val active=arc(30)
        val changed=active.mapIndexed { i,c -> c.copy(creatureStatus=if(i==0) CreatureStatus.RELEASED else CreatureStatus.USED_BEYOND_BLUE,animalLevel=99) }
        val masteries=changed.mapIndexed { i,c -> CreatureMasteryEventEntity("m$i",c.instanceId,c.findId,100,"grow$i") }
        val a=dashboard(changed,masteries=masteries)
        val b=dashboard(changed.map { it.copy() },masteries=masteries.map { it.copy() })
        for(id in listOf("land_two_tigers","land_arc_15","land_first","land_release_first","land_trade_first","land_growth","land_mastery_first")) assertTrue(id,a.badge(id).earned)
        assertEquals(2,a.badge("mastery_species_creature_tiger").count)
        assertEquals(2,a.badge("land_mastery_circle").count)
        assertEquals(a.badges.map { it.badgeId to it.count },b.badges.map { it.badgeId to it.count })
    }

    @Test fun legacyCreaturesUnlockRegularRegionsWithoutRetiredBadges() {
        val creatures=listOf("creature_peacock","creature_panda","creature_addax","creature_takin","creature_elephant")
        for(source in listOf("stillwater","beyond_blue")) {
            val copies=creatures.map { copy(it,source,level=99) }
            val events=copies.map { CreatureMasteryEventEntity("m:${it.instanceId}",it.instanceId,it.findId,100,"grow:${it.instanceId}") }
            val result=dashboard(copies,masteries=events)
            assertTrue(result.badge("across_the_land").earned)
            assertTrue(result.badge("one_from_every_land").earned)
            assertTrue(result.badges.none { RetiredStillwaterBadges.isBadge(it.badgeId) })
        }
        val stored=UserBadgeEntity("stillwater_mastery",7,10,20,false)
        val result=dashboard(earned=listOf(stored),pins=listOf(BadgePinEntity(stored.badgeId,0,10)))
        assertTrue(result.badges.none { RetiredStillwaterBadges.isBadge(it.badgeId) })
        assertTrue(result.collections.none { RetiredStillwaterBadges.isCollection(it.collectionId) })
        assertTrue(AchievementBadgeCatalog.definitions.none { RetiredStillwaterBadges.isBadge(it.badgeId) })
        assertTrue(BadgeBookCollections.collections.none { it.memberIds.any(RetiredStillwaterBadges::isBadge) })
        assertFalse(BadgeDefinitionResolver.isObsolete("mastery_species_stillwater_shrimp"))
    }

    @Test fun keeperOfTheLandNeedsTheExpandedRosterButKeepsOldAwards() {
        val oldRoster=LandCreatureCatalog.main.map {copy(it.creatureId)}
        assertFalse(dashboard(oldRoster).badge("keeper_of_the_land").earned)
        assertTrue(dashboard(LandCreatureCatalog.all.map {copy(it.creatureId)}).badge("keeper_of_the_land").earned)
        val oldAward=UserBadgeEntity("keeper_of_the_land",1,10,10,false)
        assertTrue(dashboard(oldRoster,earned=listOf(oldAward)).badge("keeper_of_the_land").earned)
    }

    @Test fun oldEarnedBadgesAndPinsSurviveEvenIfCurrentRosterIsIncomplete() {
        val earned=UserBadgeEntity("collection_the_blue_collector",1,10,10,false)
        val result=dashboard(earned=listOf(earned),pins=listOf(BadgePinEntity(earned.badgeId,0,10)))
        val badge=result.badge(earned.badgeId)
        assertTrue(badge.earned)
        assertEquals(0,badge.pinnedOrder)
        assertEquals(10L,badge.firstEarnedAt)
        assertFalse(badge.currentRosterComplete!!)
    }
}
