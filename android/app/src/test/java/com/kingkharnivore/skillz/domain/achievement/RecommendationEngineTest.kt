package com.kingkharnivore.skillz.domain.achievement

import org.junit.Assert.*
import org.junit.Test

class RecommendationEngineTest {
    private fun badge(id: String, category: BadgeUiCategory, remaining: Int, tracked: Boolean = false) =
        BadgeProgressModel(id, 0, false, category, 10 - remaining, 10, remaining,
            MilestoneEngine.evaluate(0), tracked = tracked)

    @Test fun trackedGoalsComeFirstAndDuplicatesAreSuppressed() {
        val tracked = badge("tracked", BadgeUiCategory.COLLECTIONS, 9, true)
        val result = RecommendationEngine.recommend(listOf(
            badge("flow-near", BadgeUiCategory.FLOW, 1), tracked, tracked,
            badge("mastery-near", BadgeUiCategory.MASTERY, 2)
        ))
        assertEquals("tracked", result.first().badgeId)
        assertEquals(result.size, result.map { it.badgeId }.distinct().size)
    }

    @Test fun recommendationsPreferProximityAndCategoryDiversityDeterministically() {
        val input = listOf(
            badge("flow-a", BadgeUiCategory.FLOW, 1),
            badge("flow-b", BadgeUiCategory.FLOW, 2),
            badge("collection", BadgeUiCategory.COLLECTIONS, 3),
            badge("mastery", BadgeUiCategory.MASTERY, 4)
        )
        val first = RecommendationEngine.recommend(input)
        assertEquals(first, RecommendationEngine.recommend(input.reversed()))
        assertEquals(3, first.map { it.category }.distinct().size)
        assertTrue(first.any { it.badgeId == "flow-a" })
    }

    @Test fun completedAndUnavailableTargetsAreExcluded() {
        val complete = badge("complete", BadgeUiCategory.FLOW, 0)
        val invalid = complete.copy(badgeId = "invalid", remaining = 2, target = 0)
        assertTrue(RecommendationEngine.recommend(listOf(complete, invalid)).isEmpty())
    }
}
