package com.kingkharnivore.skillz.model.state.flow


data class ArcSummaryUiModel(
    val totalSessions: Int,
    val totalDurationMs: Long,
    val totalFinalPoints: Int,
    val totalArcBonusPoints: Int,
    val peakMultiplier: Double
)