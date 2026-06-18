package com.kingkharnivore.skillz.model.state.flow


data class ArcSummaryUiModel(
    val totalSessions: Int,
    val totalDurationMs: Long,
    val totalFinalPoints: Int,
    val totalArcBonusPoints: Int,
    val peakMultiplier: Double,
    val shellSummary: ArcShellRewardSummaryUiModel = ArcShellRewardSummaryUiModel()
)

data class ArcShellRewardSummaryUiModel(
    val animals: List<ArcShellRewardCountUiModel> = emptyList(),
    val objects: List<ArcShellRewardCountUiModel> = emptyList(),
    val trinkets: List<ArcShellRewardCountUiModel> = emptyList(),
    val badges: List<ArcShellRewardCountUiModel> = emptyList(),
    val discoveries: List<ArcShellRewardCountUiModel> = emptyList(),
    val unknownRewards: List<ArcShellRewardCountUiModel> = emptyList(),
    val pearlsCarried: Int = 0,
    val stillwaterAdded: Long = 0L
) {
    val hasVisibleShellRewards: Boolean
        get() = animals.isNotEmpty() || objects.isNotEmpty() || trinkets.isNotEmpty() ||
            badges.isNotEmpty() || discoveries.isNotEmpty() || unknownRewards.isNotEmpty() ||
            stillwaterAdded > 0L
}

data class ArcShellRewardCountUiModel(
    val id: String,
    val count: Int
)
