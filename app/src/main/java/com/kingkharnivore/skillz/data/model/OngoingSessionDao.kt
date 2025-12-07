package com.kingkharnivore.skillz.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OngoingSessionDao {
    @Query("SELECT * FROM ongoing_session WHERE id = 1")
    fun getOngoingSession(): Flow<OngoingSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OngoingSessionEntity)

    @Query("DELETE FROM ongoing_session WHERE id = 1")
    suspend fun clear()
}