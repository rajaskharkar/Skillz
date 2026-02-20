package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.viewmodel.TagUiModel
import com.kingkharnivore.skillz.utils.score.ScoreFilter
import com.kingkharnivore.skillz.utils.time.StoryPeriod

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val tagId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val surgePlannedMs: Long? = null,
    val surgePoints: Int = 0,
    val beamId: Long? = null,
    val beamEligibleMs: Long = 0L,
    val beamBonusPoints: Int = 0,
    val beamMultiplier: Double? = null,
    val scyraPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun SessionEntity.isInScoreWindow(
    nowMs: Long,
    filter: ScoreFilter
): Boolean {
    // All-time: include everything
    if (filter == ScoreFilter.ALL_TIME) return true

    // For non-all-time filters, durationMs is non-null by design
    val windowLengthMs = filter.durationMs
        ?: return true // defensive fallback, should never hit

    val windowStart = nowMs - windowLengthMs

    // We use endTimestamp so only *finished* sessions are counted
    return createdAt >= windowStart
}

data class Journey7dStatUiModel(
    val tagId: Long,
    val tagName: String,
    val totalScore: Int,
    val totalDurationMs: Long,
    val sessionsCount: Int
)

data class FlowListUiState(
    val isLoading: Boolean = true,
    val sessions: List<FlowListItemUiModel> = emptyList(),
    val tags: List<TagUiModel> = emptyList(),
    val selectedTagId: Long? = null,
    val totalDurationMs: Long = 0L,
    val errorMessage: String? = null,
    val period: StoryPeriod = StoryPeriod.DAY,
    val anchorDayStartMs: Long = 0L,
    val currentScore: Int = 0,
    val currentSurgeScore: Int = 0,
    val topJourneysLast7d: List<Journey7dStatUiModel> = emptyList(),
    val firstSessionStartMs: Long? = null,
    val isCurrentPeriod: Boolean = true,
    val sagasInView: List<Journey7dStatUiModel> = emptyList(), // reuse your stat model
    val isViewJourneysOpen: Boolean = false,
    val viewJourneysTitle: String = "",
    val viewJourneysSessions: List<FlowListItemUiModel> = emptyList(),
)

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
    val beamBonusPoints: Int
)