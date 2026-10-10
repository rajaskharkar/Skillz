package com.kingkharnivore.skillz.data.model.entity.green

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "green_specimen", indices = [Index("speciesId"), Index(value = ["createdOrder"], unique = true)])
data class PlantSpecimenEntity(
    @PrimaryKey val id: String,
    val speciesId: String,
    val level: Int,
    val plantedAt: Long,
    val fullyGrownAt: Long? = null,
    val masteredAt: Long? = null,
    val lastWateredAt: Long? = null,
    val positionKey: String,
    val createdOrder: Long,
    val investedDrops: Long
) {
    init {
        require(level in 1..99)
        require(fullyGrownAt == null || fullyGrownAt >= plantedAt)
        require(masteredAt == null || masteredAt >= (fullyGrownAt ?: plantedAt))
    }
}

@Entity(tableName = "green_badge_award")
data class GreenBadgeAwardEntity(@PrimaryKey val id: String, val awardedAt: Long, val catalogueVersion: Int)

/** Idempotent commands survive retries and process recreation. */
@Entity(tableName = "green_action")
data class GreenActionEntity(
    @PrimaryKey val id: String, val kind: String, val targetId: String,
    val specimenId: String, val oldLevel: Int, val newLevel: Int,
    val cost: Long, val balanceAfter: Long, val occurredAt: Long
)

@Entity(tableName = "green_event", indices = [Index("name"), Index("occurredAt")])
data class GreenEventEntity(
    @PrimaryKey val id: String, val name: String, val occurredAt: Long,
    val speciesId: String? = null, val environmentId: String? = null,
    val tier: Int? = null, val oldLevel: Int? = null, val newLevel: Int? = null,
    val growthStage: String? = null, val dropCost: Long? = null, val balanceAfter: Long? = null
)
