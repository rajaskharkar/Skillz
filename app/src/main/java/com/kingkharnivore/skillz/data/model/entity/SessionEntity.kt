package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.utils.score.ScoreFilter

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
    val tagId: Long,                        // <- which Skill this session belongs to
    val startTime: Long,                    // will be from stopwatch later
    val endTime: Long,
    val durationMs: Long,
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