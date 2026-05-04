package com.kingkharnivore.skillz.model.state.flow

data class FlowRewardUiModel(
    val minutes: Int,
    val baseScyraPoints: Int,
    val tenMinuteBonuses: Int,
    val thirtyMinuteBonuses: Int,
    val sixtyMinuteBonuses: Int,
    val finalScyraPoints: Int,
    val surgePoints: Int,

    // ARC
    val arcIndexInArc: Int? = null,
    val arcMultiplierUsed: Double? = null,
    val arcBonusPoints: Int = 0,
    val arcNextMultiplier: Double? = null,
    val arcProgressTowardNextMs: Long = 0L,
    val arcDidLevelUp: Boolean = false,
    val arcSummary: ArcSummaryUiModel? = null,
    val isArcOnlySummary: Boolean = false
)