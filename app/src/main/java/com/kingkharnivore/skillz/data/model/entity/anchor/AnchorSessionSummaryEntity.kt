package com.kingkharnivore.skillz.data.model.entity.anchor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anchor_session_summary")
data class AnchorSessionSummaryEntity(
    @PrimaryKey val sessionId: Long,
    val anchorEnabled: Boolean,
    val distractionAttemptCount: Int,
    val anchorPausedCount: Int,
    val disabledForFlow: Boolean,
    val breakCount: Int,
    val totalBreakDurationMs: Long,
    val phoneDownModeEnabled: Boolean,
    val phoneDownDurationMs: Long
)
