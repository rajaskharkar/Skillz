package com.kingkharnivore.skillz.data.model.entity.shell

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object ObjectivePeriodTypes {
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
}

object ObjectiveTypes {
    const val ONE_TIME = "one_time"
    const val RECURRING = "recurring"
}

@Entity(
    tableName = "objectives",
    indices = [
        Index("journeyId"),
        Index("periodType"),
        Index(value = ["journeyId", "periodType", "isArchived"])
    ]
)
data class ObjectiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val journeyId: Long,
    val journeyNameSnapshot: String,
    val periodType: String,
    val objectiveType: String,
    val targetDurationMs: Long,
    val startAtMs: Long,
    val weeklyBoundaryDay: Int? = null,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalCompletions: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "objective_completions",
    indices = [
        Index(value = ["objectiveId", "periodStartMs", "periodEndMs"], unique = true),
        Index("journeyId"),
        Index("periodType")
    ]
)
data class ObjectiveCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val objectiveId: Long,
    val journeyId: Long,
    val journeyNameSnapshot: String,
    val periodType: String,
    val objectiveType: String,
    val periodStartMs: Long,
    val periodEndMs: Long,
    val completedAt: Long,
    val achievedDurationMs: Long,
    val targetDurationMs: Long,
    val baseRewardPearls: Int,
    val streakBeforeCompletion: Int,
    val streakMultiplier: Double,
    val finalRewardPearls: Int,
    val badgeKey: String,
    val badgeLabelSnapshot: String,
    val pearlsGranted: Boolean = true,
    val badgeGranted: Boolean = true
)

@Entity(
    tableName = "objective_skipped_cycles",
    indices = [Index(value = ["objectiveId", "periodStartMs", "periodEndMs"], unique = true)]
)
data class ObjectiveSkippedCycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val objectiveId: Long,
    val periodStartMs: Long,
    val periodEndMs: Long,
    val skippedAt: Long
)
