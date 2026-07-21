package com.kingkharnivore.skillz.domain.achievement

enum class CelebrationLifecycle { PENDING, PRESENTING, SUMMARY_REACHED, COMPLETED }
enum class CelebrationStage {
    LEVEL_TRANSITION, MASTERY_REVEAL, SPECIES_BADGE_REVEAL,
    COLLECTION_IMPACT, ADDITIONAL_ACHIEVEMENTS, FINAL_SUMMARY, COMPLETED
}

data class CelebrationTransition(
    val lifecycle: CelebrationLifecycle,
    val stage: CelebrationStage
)

/** Pure deterministic state machine. Persistence and rendering are its callers. */
object MasteryCelebrationStateMachine {
    fun begin(current: CelebrationStage): CelebrationTransition =
        if (current == CelebrationStage.FINAL_SUMMARY) {
            CelebrationTransition(CelebrationLifecycle.SUMMARY_REACHED, current)
        } else CelebrationTransition(CelebrationLifecycle.PRESENTING, current)

    fun advance(current: CelebrationStage, reducedMotion: Boolean = false): CelebrationTransition {
        // Reduced motion changes the renderer, never the content sequence.
        val next = when (current) {
            CelebrationStage.LEVEL_TRANSITION -> CelebrationStage.MASTERY_REVEAL
            CelebrationStage.MASTERY_REVEAL -> CelebrationStage.SPECIES_BADGE_REVEAL
            CelebrationStage.SPECIES_BADGE_REVEAL -> CelebrationStage.COLLECTION_IMPACT
            CelebrationStage.COLLECTION_IMPACT -> CelebrationStage.ADDITIONAL_ACHIEVEMENTS
            CelebrationStage.ADDITIONAL_ACHIEVEMENTS -> CelebrationStage.FINAL_SUMMARY
            CelebrationStage.FINAL_SUMMARY -> CelebrationStage.FINAL_SUMMARY
            CelebrationStage.COMPLETED -> CelebrationStage.COMPLETED
        }
        return CelebrationTransition(
            if (next == CelebrationStage.FINAL_SUMMARY) CelebrationLifecycle.SUMMARY_REACHED else CelebrationLifecycle.PRESENTING,
            next
        )
    }

    fun skip() = CelebrationTransition(CelebrationLifecycle.SUMMARY_REACHED, CelebrationStage.FINAL_SUMMARY)
    fun complete() = CelebrationTransition(CelebrationLifecycle.COMPLETED, CelebrationStage.COMPLETED)
}

fun significantAchievementOrder(badgeId: String): Int = when {
    badgeId == "collection_all_waters_completionist" -> 0
    badgeId == "collection_the_blue_completionist" -> 1
    badgeId.startsWith("blue_") && badgeId.endsWith("_completionist") -> 2
    badgeId == "mastery_first" -> 3
    badgeId.startsWith("mastery_species_") -> 4
    badgeId == "mastery_variety" -> 5
    badgeId == "mastery_circle" -> 6
    else -> 7
}
