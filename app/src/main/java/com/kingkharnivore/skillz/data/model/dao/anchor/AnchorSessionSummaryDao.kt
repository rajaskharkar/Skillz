package com.kingkharnivore.skillz.data.model.dao.anchor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.anchor.AnchorSessionSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnchorSessionSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AnchorSessionSummaryEntity)

    @Query("SELECT * FROM anchor_session_summary WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: Long): AnchorSessionSummaryEntity?

    @Query("SELECT * FROM anchor_session_summary WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: Long): Flow<AnchorSessionSummaryEntity?>
}
