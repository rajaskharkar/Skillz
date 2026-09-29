package com.kingkharnivore.skillz.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Prospective journal: written atomically with canonical Arc membership, never backfilled. */
@Entity(tableName = "arc_land_reward")
data class ArcLandRewardEntity(
    @PrimaryKey val arcId: Long,
    val flowCount: Int,
    val lastFlowEndTime: Long,
    val finalSessionId: Long,
    val finalizedAt: Long? = null
)
