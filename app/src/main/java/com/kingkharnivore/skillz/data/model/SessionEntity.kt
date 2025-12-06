package com.kingkharnivore.skillz.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val notes: String? = null
)
