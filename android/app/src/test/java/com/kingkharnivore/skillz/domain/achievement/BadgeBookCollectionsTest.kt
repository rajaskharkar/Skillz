package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import kotlin.test.*

class BadgeBookCollectionsTest {
    private fun dashboard(earned: List<UserBadgeEntity> = emptyList()) = BadgeDashboardCalculator.calculate(earned,emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList())
    @Test fun rostersAreValidNonRecursiveAndOrderedWithOverlap() {
        val collections = BadgeBookCollections.collections
        assertEquals(collections.size, collections.map { it.id }.distinct().size)
        assertEquals(collections.map { it.difficulty }.sorted(), collections.map { it.difficulty })
        collections.forEach { collection ->
            assertTrue(collection.memberIds.isNotEmpty())
            assertEquals(collection.memberIds.size, collection.memberIds.distinct().size)
            collection.memberIds.forEach { id ->
                assertNotNull(AchievementBadgeCatalog.byId[id],id)
                assertFalse(id in BadgeBookCollections.byAward,id)
                assertTrue(BadgeDefinitionResolver.isUserVisible(id),id)
            }
            assertNotNull(AchievementBadgeCatalog.byId[collection.completionBadgeId])
        }
        assertTrue(collections.count { "power_spark" in it.memberIds } > 1)
        assertTrue(collections.count { "red_first_footprint" in it.memberIds } > 1)
        assertEquals(12, BadgeBookCollections.newAwards.size)
    }
    @Test fun awardsRequireEveryMemberOnceAndDoNotMultiplyWithRepeatCounts() {
        BadgeBookCollections.newAwards.forEach { collection ->
            val rows = collection.memberIds.mapIndexed { i,id -> UserBadgeEntity(id,10,100L+i,200L+i,false) }
            val partial = dashboard(rows.dropLast(1)).badges.first { it.badgeId == collection.completionBadgeId }
            assertFalse(partial.earned,collection.id)
            assertTrue(partial.canTrack)
            assertEquals(collection.memberIds.size-1,partial.progress)
            val complete = dashboard(rows).badges.first { it.badgeId == collection.completionBadgeId }
            assertTrue(complete.terminal,collection.id)
            assertEquals(1,complete.count)
            assertEquals(0,complete.remaining)
            assertFalse(complete.canTrack)
            assertEquals(BadgeActionDestination.CollectionDetails(collection.id),complete.action)
        }
    }
    @Test fun persistedAwardsRemainEarnedWithMissingCurrentEvidence() {
        val collection=BadgeBookCollections.newAwards.first()
        val badge=dashboard(listOf(UserBadgeEntity(collection.completionBadgeId,1,100,100,false))).badges.first { it.badgeId == collection.completionBadgeId }
        assertTrue(badge.earned)
        assertEquals(100L,badge.firstEarnedAt)
        assertEquals(1,badge.count)
    }
    @Test fun timestampUsesLastFirstAwardAndDoesNotInventHistoricalDates() {
        val collection=BadgeBookCollections.newAwards.first()
        val earned=collection.memberIds.mapIndexed { i,id -> UserBadgeEntity(id,1,100L+i,500,false) }
        val badges=dashboard(earned).badges.associateBy { it.badgeId }
        assertEquals(103L, BadgeBookCollections.completionEvidence(collection,badges)?.timestamp)
        assertNull(BadgeBookCollections.completionEvidence(collection,badges - collection.memberIds.last()))
    }
    @Test fun discoveryAndMasteryRewardsReuseTheEquivalentCreatureRosters() {
        BadgeBookCollections.collections.filter { !it.createsAward && it.id != "badge_book_flow" && !it.id.startsWith("badge_book_green_") }.forEach { group ->
            val reward=BadgeDefinitionResolver.resolve(group.completionBadgeId)
            val red=RedBadgeCatalog.byId[group.completionBadgeId]
            val expected=red?.species ?: CollectionCatalog.byId.getValue(requireNotNull(reward.collectionId)).eligibleRoster(reward.requirement)
            val actual=group.memberIds.flatMap { id ->
                val spec=RedBadgeCatalog.byId[id]
                if(spec != null) spec.species else {
                    val member=BadgeDefinitionResolver.resolve(id)
                    CollectionCatalog.byId.getValue(requireNotNull(member.collectionId)).eligibleRoster(member.requirement)
                }
            }.toSet()
            assertEquals(expected.toSet(),actual,group.id)
        }
    }
}
