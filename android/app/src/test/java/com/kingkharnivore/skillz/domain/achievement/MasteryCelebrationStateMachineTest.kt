package com.kingkharnivore.skillz.domain.achievement

import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryCelebrationStateMachineTest {
    @Test fun normalPathIsDeterministicAndSummaryIsStable() {
        var transition = MasteryCelebrationStateMachine.begin(CelebrationStage.LEVEL_TRANSITION)
        assertEquals(CelebrationLifecycle.PRESENTING, transition.lifecycle)
        val expected = listOf(
            CelebrationStage.MASTERY_REVEAL, CelebrationStage.SPECIES_BADGE_REVEAL,
            CelebrationStage.COLLECTION_IMPACT, CelebrationStage.ADDITIONAL_ACHIEVEMENTS,
            CelebrationStage.FINAL_SUMMARY
        )
        expected.forEach { stage ->
            transition = MasteryCelebrationStateMachine.advance(transition.stage)
            assertEquals(stage, transition.stage)
        }
        assertEquals(CelebrationLifecycle.SUMMARY_REACHED, transition.lifecycle)
        assertEquals(transition, MasteryCelebrationStateMachine.advance(transition.stage))
    }

    @Test fun skipAndReducedMotionReachSummaryWithoutCompleting() {
        assertEquals(CelebrationLifecycle.SUMMARY_REACHED, MasteryCelebrationStateMachine.skip().lifecycle)
        assertEquals(CelebrationStage.FINAL_SUMMARY,
            MasteryCelebrationStateMachine.advance(CelebrationStage.LEVEL_TRANSITION, reducedMotion = true).stage)
    }

    @Test fun deliberateExitIsTheOnlyCompletionTransition() {
        val complete = MasteryCelebrationStateMachine.complete()
        assertEquals(CelebrationLifecycle.COMPLETED, complete.lifecycle)
        assertEquals(CelebrationStage.COMPLETED, complete.stage)
    }

    @Test fun simultaneousAchievementsUsePrestigeOrdering() {
        val ids = listOf("mastery_circle", "mastery_first", "blue_sunlit_reef_completionist",
            "collection_all_waters_completionist", "collection_the_blue_completionist")
        assertEquals(listOf("collection_all_waters_completionist", "collection_the_blue_completionist",
            "blue_sunlit_reef_completionist", "mastery_first", "mastery_circle"),
            ids.sortedBy(::significantAchievementOrder))
    }
}
