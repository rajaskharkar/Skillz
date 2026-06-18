package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_arc_run")
data class ActiveArcRunEntity(
    @PrimaryKey val id: Int = 1,
    val arcPlanId: Long,
    val arcTitle: String,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val currentStepTitle: String,
    val currentTagName: String,
    val currentIsSoftMode: Boolean,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)