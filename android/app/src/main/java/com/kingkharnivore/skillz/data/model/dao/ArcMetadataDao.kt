package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kingkharnivore.skillz.data.model.entity.ArcMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArcMetadataDao {
    @Query("SELECT * FROM arc_metadata ORDER BY arcId")
    fun observeAll(): Flow<List<ArcMetadataEntity>>

    @Query("SELECT * FROM arc_metadata WHERE arcId = :arcId LIMIT 1")
    fun observe(arcId: Long): Flow<ArcMetadataEntity?>

    @Query("SELECT * FROM arc_metadata WHERE arcId = :arcId LIMIT 1")
    suspend fun get(arcId: Long): ArcMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: ArcMetadataEntity)

    @Query("DELETE FROM arc_metadata WHERE arcId = :arcId")
    suspend fun delete(arcId: Long)
}
