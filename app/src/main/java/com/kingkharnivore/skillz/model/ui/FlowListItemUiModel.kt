package com.kingkharnivore.skillz.model.ui

data class FlowListItemUiModel(
    val sessionId: Long,
    val title: String,
    val description: String,
    val tagName: String,
    val durationMs: Long,
    val createdAt: Long,
    val score: Int,
    val isSurge: Boolean,
    val surgePoints: Int,
    val beamBonusPoints: Int,
    val arcId: Long? = null,
    val arcIndex: Int? = null,
    val arcMultiplierUsed: Double? = null,
    val arcBonusPoints: Int = 0
)