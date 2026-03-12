package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val arcId: Long? = null,
    val arcIndex: Int? = null,
    val arcMultiplierUsed: Double? = null,
    val arcBonusPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
