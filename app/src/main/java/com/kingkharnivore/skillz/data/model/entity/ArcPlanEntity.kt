package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arc_plans")
data class ArcPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isInStudio: Boolean = false,
    val archived: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchedAt: Long? = null,
    val recurrenceType: String = RECURRENCE_ONE_TIME,
    val recurrenceDaysCsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val RECURRENCE_ONE_TIME = "one_time"
        const val RECURRENCE_DAILY = "daily"
        const val RECURRENCE_WEEKDAYS = "weekdays"
        const val RECURRENCE_WEEKLY = "weekly"
        const val RECURRENCE_CUSTOM = "custom"
    }
}