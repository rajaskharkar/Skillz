package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ongoing_session")
data class OngoingSessionEntity(
    @PrimaryKey val id: Int = 1,
    val flowInstanceId: String,
    val title: String,
    val description: String,
    val tagName: String,
    val isInFlowMode: Boolean,
    val isRunning: Boolean,
    val isSoftMode: Boolean = false,
    val baseStartTimeMs: Long?,
    val accumulatedBeforeStartMs: Long,
    val isSurgeOn: Boolean = false,
    val surgePlannedMs: Long? = null,
    val surgeMilestonesFiredCsv: String = "",
    val surgeTargetReached: Boolean = false,
    val surgeTargetReachedAtMs: Long? = null,
    val surgeFinalCountdownStarted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val arcId: Long? = null,
    val arcChainBase: Double? = null,
    val arcSessionCountInArc: Int? = null,
    val arcLastSessionEndTimeMs: Long? = null,
    val originPulseId: Long? = null,
    val originPulseTitleSnapshot: String? = null,
    val originPulseJourneyNameSnapshot: String? = null,
    val healthEnabledAtStart: Boolean = false,
    val healthPermissionGrantedAtStart: Boolean = false,
    val movementBonusEligibleAtStart: Boolean = false,
    val activeIntervalJson: String? = null
)