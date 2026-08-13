package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMediaItemEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChronicleDao {
    @Query("SELECT * FROM chronicles WHERE ownerType = :ownerType AND ownerKey = :ownerKey LIMIT 1")
    fun observeChronicle(ownerType: String, ownerKey: String): Flow<ChronicleEntity?>

    @Query("SELECT * FROM chronicle_moments WHERE chronicleId = :chronicleId ORDER BY position")
    fun observeMoments(chronicleId: String): Flow<List<ChronicleMomentEntity>>

    @Query("SELECT * FROM chronicle_media_items WHERE momentId = :momentId ORDER BY position")
    fun observeMedia(momentId: String): Flow<List<ChronicleMediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChronicle(value: ChronicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMoment(value: ChronicleMomentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedia(values: List<ChronicleMediaItemEntity>)

    @Query("UPDATE chronicles SET draft = :draft, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDraft(id: String, draft: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chronicle_moments WHERE id = :id")
    suspend fun deleteMoment(id: String)

    @Query("UPDATE chronicle_moments SET position = -position - 1 WHERE chronicleId = :chronicleId")
    suspend fun parkPositions(chronicleId: String)

    @Query("UPDATE chronicle_moments SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    @Transaction
    suspend fun reorder(chronicleId: String, orderedIds: List<String>) {
        parkPositions(chronicleId)
        orderedIds.forEachIndexed { index, id -> setPosition(id, index) }
    }
}
