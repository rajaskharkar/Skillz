package com.kingkharnivore.skillz.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}