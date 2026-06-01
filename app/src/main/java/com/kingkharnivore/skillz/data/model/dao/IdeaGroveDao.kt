package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.PulseFlowLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaGroveDao {
    @Query("SELECT * FROM pulse_flow_links ORDER BY linkedAt DESC")
    fun observePulseFlowLinks(): Flow<List<PulseFlowLinkEntity>>

    @Query(
        """
        UPDATE pulses
        SET groveStatus = :status,
            groveStatusChangedAt = :changedAt,
            updatedAt = :changedAt
        WHERE id = :pulseId
        """
    )
    suspend fun updatePulseGroveStatus(
        pulseId: Long,
        status: String,
        changedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPulseFlowLink(entity: PulseFlowLinkEntity)

    @Query("SELECT * FROM pulse_flow_links WHERE pulseId = :pulseId")
    suspend fun getLinksForPulse(pulseId: Long): List<PulseFlowLinkEntity>

    @Query("SELECT COUNT(*) FROM pulse_flow_links WHERE pulseId = :pulseId")
    suspend fun getLinkedFlowCount(pulseId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM pulses WHERE id = :pulseId)")
    suspend fun pulseExists(pulseId: Long): Boolean

    @Query("SELECT * FROM pulses WHERE id = :pulseId LIMIT 1")
    suspend fun getPulse(pulseId: Long): PulseEntity?
}
