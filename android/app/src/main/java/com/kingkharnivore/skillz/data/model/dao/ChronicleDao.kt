package com.kingkharnivore.skillz.data.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMediaItemEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChronicleDao {
    @Query("SELECT * FROM chronicles WHERE ownerType=:ownerType AND ownerKey=:ownerKey LIMIT 1")
    abstract fun observe(ownerType: String, ownerKey: String): Flow<ChronicleEntity?>

    @Query("SELECT * FROM chronicles WHERE ownerType=:ownerType AND ownerKey=:ownerKey LIMIT 1")
    abstract suspend fun find(ownerType: String, ownerKey: String): ChronicleEntity?

    @Query("SELECT * FROM chronicle_moments WHERE chronicleId=:chronicleId ORDER BY position")
    abstract fun observeMoments(chronicleId: String): Flow<List<ChronicleMomentEntity>>

    @Query("SELECT * FROM chronicle_moments WHERE chronicleId=:chronicleId ORDER BY position")
    abstract suspend fun moments(chronicleId: String): List<ChronicleMomentEntity>

    @Query("SELECT * FROM chronicle_media_items WHERE momentId=:momentId ORDER BY position")
    abstract suspend fun media(momentId: String): List<ChronicleMediaItemEntity>

    @Insert abstract suspend fun insertChronicle(value: ChronicleEntity)
    @Insert abstract suspend fun insertMoment(value: ChronicleMomentEntity)
    @Insert abstract suspend fun insertMedia(values: List<ChronicleMediaItemEntity>)
    @Update abstract suspend fun updateChronicle(value: ChronicleEntity)
    @Update abstract suspend fun updateMoment(value: ChronicleMomentEntity)
    @Update abstract suspend fun updateMedia(value: ChronicleMediaItemEntity)
    @Delete abstract suspend fun deleteMoment(value: ChronicleMomentEntity)
    @Delete abstract suspend fun deleteMedia(value: ChronicleMediaItemEntity)

    @Query("UPDATE chronicle_moments SET position=:position, updatedAt=:now WHERE id=:id")
    protected abstract suspend fun setMomentPosition(id: String, position: Int, now: Long)
    @Query("UPDATE chronicle_media_items SET position=:position WHERE id=:id")
    protected abstract suspend fun setMediaPosition(id: String, position: Int)

    @Transaction
    open suspend fun reorderMoments(chronicleId: String, orderedIds: List<String>, now: Long) {
        val current = moments(chronicleId)
        require(orderedIds.size == current.size && orderedIds.toSet() == current.map { it.id }.toSet())
        // Park the complete, validated set first to satisfy the unique position index.
        current.forEachIndexed { index, moment -> setMomentPosition(moment.id, -index - 1, now) }
        orderedIds.forEachIndexed { index, id -> setMomentPosition(id, index, now) }
    }

    @Transaction
    open suspend fun reorderMedia(momentId: String, orderedIds: List<String>) {
        val current = media(momentId)
        require(orderedIds.size == current.size && orderedIds.toSet() == current.map { it.id }.toSet())
        current.forEachIndexed { index, item -> setMediaPosition(item.id, -index - 1) }
        orderedIds.forEachIndexed { index, id -> setMediaPosition(id, index) }
    }

    @Transaction
    open suspend fun promote(ownerType: String, ownerKey: String, newType: String, newKey: String, now: Long) {
        val chronicle = find(ownerType, ownerKey) ?: return
        check(find(newType, newKey) == null) { "Chronicle owner already exists" }
        updateChronicle(chronicle.copy(ownerType = newType, ownerKey = newKey, updatedAt = now))
    }
}
