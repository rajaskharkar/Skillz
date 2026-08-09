package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.domain.achievement.BadgeCountType
import com.kingkharnivore.skillz.domain.achievement.BadgeDefinitionResolver
import com.kingkharnivore.skillz.domain.achievement.BadgeFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity

class ObjectiveBadgeIdentityTest {
    @Test fun validIdentityRoundTripsForEveryPeriod() {
        listOf("daily", "weekly", "monthly").forEach { period ->
            val id = ObjectiveBadgeIdentity.badgeId(42, period)
            assertEquals(ObjectiveBadgeIdentity(42, period), ObjectiveBadgeIdentity.fromBadgeId(id))
        }
    }

    @Test fun malformedIdentityRemainsUnknown() {
        assertNull(ObjectiveBadgeIdentity.fromBadgeId("objective_badge_42_yearly"))
        assertNull(ObjectiveBadgeIdentity.fromBadgeId("objective_badge_bad_daily"))
        assertNull(ObjectiveBadgeIdentity.fromBadgeId("corrupt"))
    }

    @Test fun objectiveDefinitionIsVisibleRepeatableAndPinnable() {
        val definition = BadgeDefinitionResolver.resolve("objective_badge_42_daily")
        assertEquals(BadgeFamily.OBJECTIVE, definition.family)
        assertEquals(BadgeCountType.REPEATABLE, definition.countType)
        assertTrue(definition.pinnable)
        assertTrue(BadgeDefinitionResolver.isUserVisible(definition.badgeId))
    }

    @Test fun presentationMetadataUsesEarliestHistoricalSnapshot() {
        val later = completion(2, 200, "Renamed")
        val earlier = completion(1, 100, "Original")
        val metadata = objectiveBadgePresentationMetadata(listOf(later, earlier)).getValue(earlier.badgeKey)
        assertEquals("Original", metadata.journeyNameSnapshot)
        assertEquals(42, metadata.journeyId)
        assertEquals("Original", objectiveJourneyPresentationNames(listOf(later, earlier))[42])
    }

    private fun completion(id: Long, completedAt: Long, name: String) = ObjectiveCompletionEntity(
        id = id, objectiveId = id, journeyId = 42, journeyNameSnapshot = name,
        periodType = "daily", objectiveType = "recurring", periodStartMs = id,
        periodEndMs = id + 1, completedAt = completedAt, achievedDurationMs = 60_000,
        targetDurationMs = 60_000, baseRewardPearls = 1, streakBeforeCompletion = 0,
        streakMultiplier = 1.0, finalRewardPearls = 1,
        badgeKey = "objective_badge_42_daily", badgeLabelSnapshot = "ignored"
    )
}
