package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.model.FlowMode

/** A receipt for a real saved Flow. Snapshots survive deletion of its template or Flow. */
@Entity(tableName = "horizon_memories", foreignKeys = [ForeignKey(
    entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"],
    onDelete = ForeignKey.SET_NULL
)], indices = [Index("sessionId", unique = true), Index("sourcePlanId"), Index("completedAt")])
data class HorizonMemoryEntity(
    @PrimaryKey val flowInstanceId: String,
    val sourcePlanId: Long,
    val kind: String,
    val activityTitle: String,
    val sessionId: Long?,
    val title: String,
    val tagName: String,
    val mode: FlowMode,
    val durationMs: Long,
    val score: Int,
    val surgePoints: Int,
    val surgePlannedMs: Long?,
    val arcId: Long?,
    val arcMultiplier: Double?,
    val completedAt: Long
)

object HorizonKind {
    const val HABIT = "HABIT"
    const val PLAN = "PLAN"
}
