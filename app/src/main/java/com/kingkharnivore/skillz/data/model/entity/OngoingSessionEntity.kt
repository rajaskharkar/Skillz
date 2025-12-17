package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ongoing_session")
data class OngoingSessionEntity(
    @PrimaryKey val id: Int = 1, // always a single row
    val title: String,
    val description: String,
    val tagName: String,
    val isInFlowMode: Boolean,
    val isRunning: Boolean,
    val baseStartTimeMs: Long?,          // last start/resume timestamp
    val accumulatedBeforeStartMs: Long,  // elapsed before baseStartTimeMs
    val createdAt: Long = System.currentTimeMillis()
)