package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.domain.achievement.BadgeProgressModel
import com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory
import com.kingkharnivore.skillz.domain.achievement.MilestoneEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class BadgeMedallionCountTest {
    @Test fun exactCountsRemainExactThrough999() {
        assertEquals("1", compactCount(1))
        assertEquals("2", compactCount(2))
        assertEquals("91", compactCount(91))
        assertEquals("999", compactCount(999))
    }

    @Test fun largeCountsUseCompactPresentation() {
        val result = compactCount(1_200)
        assert(result.isNotBlank())
        assert(result != "1200")
    }

    @Test fun earnedRepeatableBadgeAlwaysUsesCompleteUnlockRing() {
        val badge = BadgeProgressModel("badge_flow_10_min", 3, true, BadgeUiCategory.FLOW,
            progress = 3, target = 5, remaining = 2, milestone = MilestoneEngine.evaluate(3))

        assertEquals(BadgeMedallionState.Earned, badgeMedallionState(badge))
    }

    @Test fun onlyLockedBadgeUsesFirstUnlockProgressRing() {
        val badge = BadgeProgressModel("locked", 0, false, BadgeUiCategory.FLOW,
            progress = 3, target = 10, remaining = 7, milestone = MilestoneEngine.evaluate(3))

        assertEquals(BadgeMedallionState.LockedWithProgress(3, 10), badgeMedallionState(badge))
    }
}
