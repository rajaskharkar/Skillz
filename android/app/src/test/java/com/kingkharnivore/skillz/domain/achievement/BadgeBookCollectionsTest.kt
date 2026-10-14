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
        assertEquals(collections.size, BadgeBookCollections.newAwards.size)
        assertEquals(collections.size, collections.map { it.completionBadgeId }.distinct().size)
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
        assertEquals(collection.memberIds.size,badge.progress)
        assertEquals(0,badge.remaining)
        assertEquals(1,badge.count)
    }
    @Test fun timestampUsesLastFirstAwardAndDoesNotInventHistoricalDates() {
        val collection=BadgeBookCollections.newAwards.first()
        val earned=collection.memberIds.mapIndexed { i,id -> UserBadgeEntity(id,1,100L+i,500,false) }
        val badges=dashboard(earned).badges.associateBy { it.badgeId }
        assertEquals(103L, BadgeBookCollections.completionEvidence(collection,badges)?.timestamp)
        assertNull(BadgeBookCollections.completionEvidence(collection,badges - collection.memberIds.last()))
    }
    @Test fun sharedBadgesRemainIndependentOfCollectionCompletion() {
        val collection = BadgeBookCollections.byId.getValue("badge_book_flow")
        assertEquals("book_flow_complete", collection.completionBadgeId)
        val badges = dashboard(listOf(UserBadgeEntity("badge_flow_120_min", 1, 100, 100, false))).badges.associateBy { it.badgeId }
        assertTrue(badges.getValue("badge_flow_120_min").earned)
        assertFalse(badges.getValue(collection.completionBadgeId).earned)
        assertTrue(badges.getValue(collection.completionBadgeId).pinnable)
    }
}
