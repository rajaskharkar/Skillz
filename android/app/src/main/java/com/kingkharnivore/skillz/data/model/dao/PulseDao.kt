package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import com.kingkharnivore.skillz.data.model.entity.PulseCreationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PulseDao {
    @Query("SELECT pulseId FROM pulse_creations WHERE creationKey=:creationKey LIMIT 1")
    suspend fun findCreatedPulse(creationKey: String): Long?

    @Insert
    suspend fun insertCreation(value: PulseCreationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPulse(pulse: PulseEntity): Long

    @Update
    suspend fun updatePulse(pulse: PulseEntity)

    @Query("SELECT * FROM pulses ORDER BY createdAt DESC")
    fun getAllPulses(): Flow<List<PulseEntity>>

    @Query(
        """
        SELECT * FROM pulses
        WHERE parentSessionId = :sessionId
        ORDER BY createdAt DESC
        """
    )
    fun observePulsesForSession(sessionId: Long): Flow<List<PulseEntity>>

    @Query(
        """
        SELECT * FROM pulses
        WHERE parentSessionId = :sessionId
        ORDER BY createdAt DESC
        """
    )
    suspend fun getPulsesForSession(sessionId: Long): List<PulseEntity>

    @Query(
        """
        SELECT * FROM pulses
        WHERE arcId = :arcId
        ORDER BY createdAt DESC
        """
    )
    suspend fun getPulsesForArc(arcId: Long): List<PulseEntity>

    @Query("SELECT * FROM pulses WHERE id = :pulseId LIMIT 1")
    suspend fun getPulseById(pulseId: Long): PulseEntity?

    @Query("DELETE FROM pulses WHERE id = :pulseId")
    suspend fun deletePulseById(pulseId: Long)

    @Query("SELECT COUNT(*) FROM pulses WHERE tagId = :tagId")
    suspend fun getPulseCountForTag(tagId: Long): Int

    @Query(
        """
        UPDATE pulses
        SET parentSessionId = :sessionId,
            parentFlowInstanceId = NULL,
            arcId = CASE WHEN :arcId IS NOT NULL THEN :arcId ELSE arcId END,
            updatedAt = :updatedAt
        WHERE parentFlowInstanceId = :flowInstanceId
        """
    )
    suspend fun attachLivePulsesToSession(
        flowInstanceId: String,
        sessionId: Long,
        arcId: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE pulses
        SET parentSessionId = NULL,
            updatedAt = :updatedAt
        WHERE parentSessionId = :sessionId
        """
    )
    suspend fun detachPulsesFromSession(
        sessionId: Long,
        updatedAt: Long = System.currentTimeMillis()
    )
}
