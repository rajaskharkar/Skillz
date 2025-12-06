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

    @Query("SELECT * FROM sessions WHERE skillId = :skillId ORDER BY startTime DESC")
    fun getSessionsForSkill(skillId: Long): Flow<List<SessionEntity>>

    @Query("SELECT SUM(durationMs) FROM sessions WHERE skillId = :skillId")
    fun getTotalTimeForSkill(skillId: Long): Flow<Long?>
}