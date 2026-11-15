package com.kingkharnivore.skillz.data.model.dao.shell

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.PulseFlowLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaGroveDao {
    @Query("SELECT pulses.* FROM pulses INNER JOIN pulse_flow_links ON pulses.id = pulse_flow_links.pulseId WHERE pulse_flow_links.sessionId = :sessionId")
    suspend fun getIdeasForSession(sessionId: Long): List<PulseEntity>

    @Query("SELECT * FROM pulses WHERE tagId = :tagId")
    suspend fun getCompletionCandidates(tagId: Long): List<PulseEntity>

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

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertPulseFlowLink(entity: PulseFlowLinkEntity)

    @Query("SELECT * FROM pulse_flow_links WHERE pulseId = :pulseId")
    suspend fun getLinksForPulse(pulseId: Long): List<PulseFlowLinkEntity>

    @Query("SELECT COUNT(*) FROM pulse_flow_links WHERE pulseId = :pulseId")
    suspend fun getLinkedFlowCount(pulseId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM pulses WHERE id = :pulseId)")
    suspend fun pulseExists(pulseId: Long): Boolean

    @Query("SELECT * FROM pulses WHERE id = :pulseId LIMIT 1")
    suspend fun getPulse(pulseId: Long): PulseEntity?

    @Query(
        """
        UPDATE pulses
        SET groveStatus = :aliveStatus,
            groveStatusChangedAt = :changedAt,
            updatedAt = :changedAt
        WHERE groveStatus = :completedStatus
          AND NOT EXISTS (
              SELECT 1 FROM pulse_flow_links
              WHERE pulse_flow_links.pulseId = pulses.id
          )
        """
    )
    suspend fun repairCompletedPulsesWithoutFlows(
        completedStatus: String,
        aliveStatus: String,
        changedAt: Long
    )
}