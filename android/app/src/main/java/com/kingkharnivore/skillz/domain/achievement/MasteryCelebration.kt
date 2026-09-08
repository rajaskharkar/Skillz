package com.kingkharnivore.skillz.domain.achievement

enum class CelebrationLifecycle { PENDING, PRESENTING, SUMMARY_REACHED, COMPLETED }
enum class CelebrationStage {
    LEVEL_TRANSITION, MASTERY_REVEAL, SPECIES_BADGE_REVEAL,
    COLLECTION_IMPACT, ADDITIONAL_ACHIEVEMENTS, FINAL_SUMMARY, COMPLETED
}

/** The three user-visible pages. Legacy stage values remain readable for persisted celebrations. */
enum class MasteryCelebrationStep(val pageIndex: Int) {
    LEVEL_99(0), MASTERY(1), PROGRESS(2);

    companion object {
        fun from(stage: CelebrationStage): MasteryCelebrationStep = when (stage) {
            CelebrationStage.LEVEL_TRANSITION -> LEVEL_99
            CelebrationStage.MASTERY_REVEAL,
            CelebrationStage.SPECIES_BADGE_REVEAL -> MASTERY
            CelebrationStage.COLLECTION_IMPACT,
            CelebrationStage.ADDITIONAL_ACHIEVEMENTS,
            CelebrationStage.FINAL_SUMMARY,
            CelebrationStage.COMPLETED -> PROGRESS
        }
    }
}

data class CelebrationTransition(
    val lifecycle: CelebrationLifecycle,
    val stage: CelebrationStage
)

/** Pure deterministic state machine. Persistence and rendering are its callers. */
object MasteryCelebrationStateMachine {
    fun begin(current: CelebrationStage): CelebrationTransition =
        if (MasteryCelebrationStep.from(current) == MasteryCelebrationStep.PROGRESS) {
            CelebrationTransition(CelebrationLifecycle.SUMMARY_REACHED, CelebrationStage.FINAL_SUMMARY)
        } else CelebrationTransition(CelebrationLifecycle.PRESENTING, current)

    fun advance(current: CelebrationStage, reducedMotion: Boolean = false): CelebrationTransition {
        // Reduced motion changes the pager animation, never the three-page sequence.
        val next = when (MasteryCelebrationStep.from(current)) {
            MasteryCelebrationStep.LEVEL_99 -> CelebrationStage.MASTERY_REVEAL
            MasteryCelebrationStep.MASTERY -> CelebrationStage.FINAL_SUMMARY
            MasteryCelebrationStep.PROGRESS -> CelebrationStage.FINAL_SUMMARY
        }
        return CelebrationTransition(
            if (next == CelebrationStage.FINAL_SUMMARY) CelebrationLifecycle.SUMMARY_REACHED else CelebrationLifecycle.PRESENTING,
            next
        )
    }

    fun previous(current: CelebrationStage): CelebrationTransition {
        val previous = when (MasteryCelebrationStep.from(current)) {
            MasteryCelebrationStep.LEVEL_99 -> CelebrationStage.LEVEL_TRANSITION
            MasteryCelebrationStep.MASTERY -> CelebrationStage.LEVEL_TRANSITION
            MasteryCelebrationStep.PROGRESS -> CelebrationStage.MASTERY_REVEAL
        }
        return CelebrationTransition(CelebrationLifecycle.PRESENTING, previous)
    }

    fun complete() = CelebrationTransition(CelebrationLifecycle.COMPLETED, CelebrationStage.COMPLETED)
}

fun significantAchievementOrder(badgeId: String): Int = when {
    badgeId == "collection_all_waters_completionist" -> 0
    badgeId == "collection_the_blue_completionist" -> 1
    badgeId == "collection_stillwater_completionist" -> 2
    badgeId.startsWith("stillwater_") && badgeId.endsWith("_completionist") -> 3
    badgeId.startsWith("blue_") && badgeId.endsWith("_completionist") -> 4
    badgeId == "mastery_first" -> 5
    badgeId.startsWith("mastery_species_") -> 6
    badgeId == "mastery_variety" -> 7
    badgeId == "mastery_circle" -> 8
    else -> 9
}
