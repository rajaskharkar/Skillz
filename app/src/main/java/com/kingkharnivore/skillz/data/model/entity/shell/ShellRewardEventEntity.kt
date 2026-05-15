package com.kingkharnivore.skillz.data.model.entity.shell

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object ShellRewardEventTypes {
    const val PEARLS_CARRIED = "PEARLS_CARRIED"
    const val STILLWATER_ADDED = "STILLWATER_ADDED"
    const val ANIMAL_GRANTED = "ANIMAL_GRANTED"
    const val OBJECT_GRANTED = "OBJECT_GRANTED"
    const val TRINKET_GRANTED = "TRINKET_GRANTED"
    const val DISCOVERY_RECORDED = "DISCOVERY_RECORDED"
    const val BADGE_UPDATED = "BADGE_UPDATED"
}

@Entity(
    tableName = "shell_reward_event",
    indices = [
        Index("sourceSessionId"),
        Index("arcId"),
        Index("rewardType"),
        Index(value = ["sourceSessionId", "rewardType", "rewardId"], unique = true)
    ]
)
data class ShellRewardEventEntity(
    @PrimaryKey val id: String,
    val sourceSessionId: Long,
    val arcId: Long?,
    val rewardType: String,
    val rewardId: String?,
    val quantity: Long,
    val occurredAt: Long
)
