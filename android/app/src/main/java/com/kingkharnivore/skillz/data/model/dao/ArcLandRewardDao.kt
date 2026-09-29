package com.kingkharnivore.skillz.data.model.dao

import androidx.room.*
import com.kingkharnivore.skillz.data.model.entity.ArcLandRewardEntity

@Dao
interface ArcLandRewardDao {
    @Query("SELECT * FROM arc_land_reward WHERE arcId = :arcId")
    suspend fun get(arcId: Long): ArcLandRewardEntity?
    @Query("SELECT * FROM arc_land_reward WHERE finalizedAt IS NULL AND lastFlowEndTime < :expiredBefore")
    suspend fun pending(expiredBefore: Long): List<ArcLandRewardEntity>
    @Query("SELECT * FROM arc_land_reward WHERE arcId = :arcId")
    fun observe(arcId: Long): kotlinx.coroutines.flow.Flow<ArcLandRewardEntity?>
    @Upsert suspend fun save(row: ArcLandRewardEntity)
}
