package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pulse_flow_links",
    foreignKeys = [
        ForeignKey(
            entity = PulseEntity::class,
            parentColumns = ["id"],
            childColumns = ["pulseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pulseId"]),
        Index(value = ["sessionId"], unique = true)
    ]
)
data class PulseFlowLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pulseId: Long,
    val sessionId: Long,
    val linkedAt: Long
)
