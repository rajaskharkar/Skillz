package com.kingkharnivore.skillz.data.model

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
            onDelete = ForeignKey.CASCADE
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
