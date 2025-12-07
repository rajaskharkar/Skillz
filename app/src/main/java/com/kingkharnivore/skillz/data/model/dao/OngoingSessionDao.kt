package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OngoingSessionDao {
    @Query("SELECT * FROM ongoing_session WHERE id = 1")
    fun getOngoingSession(): Flow<OngoingSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(entity: OngoingSessionEntity)

    @Query("DELETE FROM ongoing_session WHERE id = 1")
    suspend fun clear()
}