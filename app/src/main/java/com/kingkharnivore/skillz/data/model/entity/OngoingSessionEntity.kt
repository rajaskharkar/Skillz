package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ongoing_session")
data class OngoingSessionEntity(
    @PrimaryKey val id: Int = 1, // always a single row
    val title: String,
    val description: String,
    val tagName: String,
    val isInFlowMode: Boolean,
    val isRunning: Boolean,
    val baseStartTimeMs: Long?,          // last start/resume timestamp
    val accumulatedBeforeStartMs: Long,  // elapsed before baseStartTimeMs

    val isSurgeOn: Boolean = false,
    val surgePlannedMs: Long? = null,

    // ✅ persisted runtime state for service-driven Surge
    val surgeMilestonesFiredCsv: String = "",
    val surgeTargetReached: Boolean = false,
    val surgeTargetReachedAtMs: Long? = null,
    val surgeFinalCountdownStarted: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val arcId: Long? = null,
    val arcChainBase: Double? = null,
    val arcSessionCountInArc: Int? = null,
    val arcLastSessionEndTimeMs: Long? = null,
)