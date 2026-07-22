package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.domain.achievement.BadgeProgressModel
import com.kingkharnivore.skillz.domain.achievement.BadgeGoalType
import com.kingkharnivore.skillz.domain.achievement.BadgeActionDestination
import com.kingkharnivore.skillz.domain.achievement.BadgeCountType
import com.kingkharnivore.skillz.domain.achievement.BadgeUiCategory
import com.kingkharnivore.skillz.domain.achievement.CollectionSpeciesAction
import com.kingkharnivore.skillz.domain.achievement.MilestoneEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class BadgeMedallionCountTest {
    @Test fun exactCountsRemainExactThrough999() {
        assertEquals("1", compactCount(1, Locale.US))
        assertEquals("2", compactCount(2, Locale.US))
        assertEquals("91", compactCount(91, Locale.US))
        assertEquals("999", compactCount(999, Locale.US))
    }

    @Test fun largeCountsUseLocaleAwareGroupingWithoutEnglishSuffixes() {
        assertEquals("1,200", compactCount(1_200, Locale.US))
        assertEquals("1.200", compactCount(1_200, Locale.forLanguageTag("es")))
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
    @Test fun earnedOneTimeBadgeDoesNotShowMilestoneProgress() {
        val badge = BadgeProgressModel("mastery_first", 1, true, BadgeUiCategory.MASTERY,
            progress = 1, target = 1, remaining = 0, milestone = MilestoneEngine.evaluate(1),
            goalType = BadgeGoalType.ONE_TIME, countType = BadgeCountType.ONE_TIME,
            terminal = true, nextTarget = null)

        assertEquals(false, showsMilestoneProgress(badge))
    }

    @Test fun beyondBlueDestinationPreservesCollectionAndSpeciesIdentity() {
        val destination = collectionSpeciesDestination(
            CollectionSpeciesAction.OpenBeyondBlue("creature_anglerfish", "blue_great_blue")
        )

        assertEquals(
            BadgeActionDestination.BeyondBlue("blue_great_blue", "creature_anglerfish"),
            destination
        )
    }

}
