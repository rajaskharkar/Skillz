package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import org.junit.Assert.*
import org.junit.Test

class LifetimePointsBadgesTest {
    private fun dashboard(total: Long, stored: List<UserBadgeEntity> = emptyList()) =
        BadgeDashboardCalculator.calculate(stored, emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), lifetimeScyraPoints = total).badges.associateBy { it.badgeId }

    @Test fun exactRequestedMilestonesAreVisibleOneTimeTrackableGoals() {
        assertEquals(listOf(1_000, 10_000, 50_000, 100_000, 200_000, 500_000, 1_000_000,
            2_000_000, 5_000_000, 10_000_000, 25_000_000, 50_000_000, 100_000_000,
            200_000_000, 500_000_000, 1_000_000_000), LifetimePointsBadges.specs.map { it.target })
        assertEquals(16, LifetimePointsBadges.byId.size)
        val empty = dashboard(0)
        LifetimePointsBadges.specs.forEach {
            assertEquals(it.definition, AchievementBadgeCatalog.byId[it.id])
            val badge = empty.getValue(it.id)
            assertFalse(badge.everEarned)
            assertTrue(badge.canTrack)
            assertEquals(BadgeCountType.ONE_TIME, badge.countType)
            assertEquals(BadgeActionDestination.Flow, badge.action)
            assertEquals(it.target, badge.remaining)
        }
    }

    @Test fun everyThresholdRequiresExactBoundaryAndAwardsOnlyOnce() {
        LifetimePointsBadges.specs.forEach { spec ->
            val below = dashboard(spec.target - 1L).getValue(spec.id)
            assertFalse(spec.id, below.everEarned)
            assertEquals(0, below.lifetimeCount)
            assertEquals(1, below.remaining)
            listOf(spec.target.toLong(), spec.target + 1L, Long.MAX_VALUE).forEach { points ->
                val badge = dashboard(points).getValue(spec.id)
                assertTrue(spec.id, badge.everEarned)
                assertTrue(badge.terminal)
                assertFalse(badge.canTrack)
                assertEquals(1, badge.lifetimeCount)
                assertEquals(spec.target, badge.currentProgress)
                assertEquals(0, badge.remaining)
            }
        }
    }

    @Test fun earnedBadgeStaysCompleteAfterScoreDropsAndRetainsDate() {
        val spec = LifetimePointsBadges.specs.last()
        val stored = UserBadgeEntity(spec.id, 1, 123L, 123L, false, viewedAt = 200L)
        val badge = dashboard(0, listOf(stored)).getValue(spec.id)
        assertTrue(badge.everEarned)
        assertTrue(badge.terminal)
        assertEquals(1, badge.lifetimeCount)
        assertEquals(0, badge.remaining)
        assertEquals(spec.target, badge.currentProgress)
        assertEquals(123L, badge.firstEarnedAt)
    }

    @Test fun negativeTotalsCannotCreateNegativeProgress() {
        dashboard(-1).values.filter { it.badgeId in LifetimePointsBadges.byId }.forEach {
            assertEquals(0, it.currentProgress)
            assertFalse(it.everEarned)
        }
    }
}
