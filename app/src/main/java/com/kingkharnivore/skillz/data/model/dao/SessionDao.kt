package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query(
        """
        SELECT * FROM sessions
        WHERE tagId = :tagId
        ORDER BY createdAt DESC
        """
    )
    fun getSessionsForTag(tagId: Long): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT * FROM sessions
        ORDER BY createdAt DESC
        """
    )
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions WHERE tagId = :tagId")
    suspend fun getSessionCountForTag(tagId: Long): Int

    @Query("UPDATE sessions SET description = :description WHERE id = :id")
    suspend fun updateSessionDescription(id: Long, description: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query(
        """
        UPDATE sessions
        SET arcId = :arcId,
            arcIndex = :arcIndex,
            arcMultiplierUsed = :arcMultiplierUsed,
            arcBonusPoints = :arcBonusPoints,
            scyraPoints = :finalScyraPoints
        WHERE id = :sessionId
        """
    )
    suspend fun updateArcFields(
        sessionId: Long,
        arcId: Long,
        arcIndex: Int,
        arcMultiplierUsed: Double,
        arcBonusPoints: Int,
        finalScyraPoints: Int
    )

    @Query(
        """
        SELECT * FROM sessions
        WHERE arcId = :arcId
        ORDER BY arcIndex ASC
        """
    )
    suspend fun getSessionsForArc(arcId: Long): List<SessionEntity>

    @Query(
        """
        SELECT * FROM sessions
        WHERE arcId = :arcId
        ORDER BY arcIndex DESC
        LIMIT 1
        """
    )
    suspend fun getLastSessionInArc(arcId: Long): SessionEntity?

    @Query("SELECT MAX(arcIndex) FROM sessions WHERE arcId = :arcId")
    suspend fun getMaxArcIndex(arcId: Long): Int?
}