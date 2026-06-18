package com.kingkharnivore.skillz.data.model.entity.health

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kingkharnivore.skillz.data.model.entity.SessionEntity

@Entity(
    tableName = "flow_reward_breakdowns",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class FlowRewardBreakdownEntity(
    @PrimaryKey val sessionId: Long,
    val nonMovementPreMultiplierPoints: Long,
    val pulseBonusPoints: Long,
    val surgeBonusPoints: Long,
    val otherPreMultiplierBonusPoints: Long,
    val movementPoints: Long,
    val preMultiplierTotal: Long,
    val arcMultiplier: Double,
    val streakMultiplier: Double,
    val otherMultiplier: Double,
    val arcBonusPoints: Long,
    val finalScyraPoints: Long,
    val pearlsEarned: Long,
    val pearlEligible: Boolean,
    val roundingMode: String = "KOTLIN_ROUND_TO_INT_COMPAT"
)
