package com.kingkharnivore.skillz.data.model.entity

import com.kingkharnivore.skillz.model.FlowMode

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
    indices = [
        Index("tagId"),
        Index("arcId"),
        Index(value = ["tagId", "isSoftMode", "endTime"])
    ]
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
    val scyraPoints: Int = 0,
    @androidx.room.ColumnInfo(name = "isSoftMode")
    val mode: FlowMode = FlowMode.FLOW,
    val arcId: Long? = null,
    val arcIndex: Int? = null,
    val arcMultiplierUsed: Double? = null,
    val arcBonusPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val activeIntervalJson: String? = null
) {
    @get:androidx.room.Ignore
    val isSoftMode: Boolean get() = mode == FlowMode.SOFT
}
