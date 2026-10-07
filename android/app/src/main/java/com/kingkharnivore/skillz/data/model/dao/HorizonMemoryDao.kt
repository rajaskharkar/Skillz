package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kingkharnivore.skillz.data.model.entity.HorizonMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HorizonMemoryDao {
    @Query("SELECT * FROM horizon_memories WHERE flowInstanceId = :flowInstanceId")
    suspend fun getByFlowInstanceId(flowInstanceId: String): HorizonMemoryEntity?

    @Query("DELETE FROM horizon_memories WHERE flowInstanceId = :flowInstanceId")
    suspend fun delete(flowInstanceId: String)

    @Insert
    suspend fun insert(memory: HorizonMemoryEntity)

    @Query("UPDATE horizon_memories SET title = :title, tagName = :tagName WHERE sessionId = :sessionId")
    suspend fun updateDetails(sessionId: Long, title: String, tagName: String)

    @Query("SELECT * FROM horizon_memories ORDER BY completedAt DESC, flowInstanceId DESC")
    fun observeAll(): Flow<List<HorizonMemoryEntity>>

    @Query("UPDATE horizon_memories SET arcId = :arcId, arcMultiplier = :multiplier, score = :score WHERE sessionId = :sessionId")
    suspend fun updateArc(sessionId: Long, arcId: Long, multiplier: Double, score: Int)
}
