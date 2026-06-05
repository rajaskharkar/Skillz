package com.kingkharnivore.skillz.model.ui

import androidx.compose.ui.graphics.Color
import com.kingkharnivore.skillz.data.model.entity.health.MovementDataSourceType

data class FlowListItemUiModel(
    val sessionId: Long,
    val title: String,
    val description: String,
    val tagId: Long,
    val tagName: String,
    val journeyColor: Color,
    val durationMs: Long,
    val createdAt: Long,
    val score: Int,
    val isSoftMode: Boolean = false,
    val isSurge: Boolean,
    val surgePoints: Int,
    val arcId: Long? = null,
    val arcIndex: Int? = null,
    val arcMultiplierUsed: Double? = null,
    val arcBonusPoints: Int = 0,
    val movementSteps: Long? = null,
    val movementPoints: Long = 0L,
    val movementBonusUpdatedAfterSync: Boolean = false,
    val movementDataSource: MovementDataSourceType? = null,
    val movementIsPhoneEstimate: Boolean = false
)