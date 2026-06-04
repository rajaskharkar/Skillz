package com.kingkharnivore.skillz.model.state.flow

data class FlowRewardUiModel(
    val minutes: Int,
    val baseScyraPoints: Int,
    val tenMinuteBonuses: Int,
    val thirtyMinuteBonuses: Int,
    val sixtyMinuteBonuses: Int,
    val finalScyraPoints: Int,
    val surgePoints: Int,
    val movementSteps: Long? = null,
    val movementPoints: Long = 0L,

    // ARC
    val arcIndexInArc: Int? = null,
    val arcMultiplierUsed: Double? = null,
    val arcBonusPoints: Int = 0,
    val arcNextMultiplier: Double? = null,
    val arcProgressTowardNextMs: Long = 0L,
    val arcDidLevelUp: Boolean = false,
    val shellPearlsEarned: Int = 0,
    val shellStillwaterUnits: Long = 0L,
    val shellGrantedFindIds: List<String> = emptyList(),
    val shellDiscoveryIds: List<String> = emptyList(),
    val shellBadgeIds: List<String> = emptyList(),
    val arcSummary: ArcSummaryUiModel? = null,
    val isArcOnlySummary: Boolean = false
)