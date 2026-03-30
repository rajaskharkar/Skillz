package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pulses",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentSessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("tagId"),
        Index("parentSessionId"),
        Index("parentFlowInstanceId"),
        Index("arcId"),
        Index("createdAt")
    ]
)
data class PulseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val tagId: Long? = null,
    val parentSessionId: Long? = null,
    val parentFlowInstanceId: String? = null,
    val arcId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
