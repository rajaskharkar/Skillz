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

data class ChronicleSummary(val ownerType: String, val ownerKey: String, val momentCount: Int, val excerpt: String?)

@Dao
abstract class ChronicleDao {
    @Query("SELECT c.ownerType, c.ownerKey, COUNT(m.id) AS momentCount, substr((SELECT t.text FROM chronicle_moments t WHERE t.chronicleId=c.id AND t.type='TEXT' AND length(trim(t.text)) > 0 ORDER BY t.position LIMIT 1),1,240) AS excerpt FROM chronicles c LEFT JOIN chronicle_moments m ON m.chronicleId=c.id GROUP BY c.id")
    abstract fun observeSummaries(): Flow<List<ChronicleSummary>>
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

    @Query("SELECT COUNT(*) FROM chronicle_media_items WHERE localPath=:path OR thumbnailPath=:path")
    abstract suspend fun mediaPathReferenceCount(path: String): Int

    @Query("SELECT COUNT(*) FROM chronicle_moments WHERE audioPath=:path")
    abstract suspend fun audioPathReferenceCount(path: String): Int

    @Query("SELECT i.* FROM chronicle_media_items i INNER JOIN chronicle_moments m ON m.id=i.momentId WHERE m.chronicleId=:chronicleId ORDER BY m.position, i.position")
    abstract fun observeMedia(chronicleId: String): Flow<List<ChronicleMediaItemEntity>>

    @Query("DELETE FROM chronicles WHERE ownerType=:ownerType AND ownerKey=:ownerKey")
    abstract suspend fun delete(ownerType: String, ownerKey: String)

    @Insert abstract suspend fun insertChronicle(value: ChronicleEntity)
    @Insert abstract suspend fun insertMoment(value: ChronicleMomentEntity)
    @Insert abstract suspend fun insertMedia(values: List<ChronicleMediaItemEntity>)
    @Update abstract suspend fun updateChronicle(value: ChronicleEntity)
    @Update abstract suspend fun updateMoment(value: ChronicleMomentEntity)
    @Update abstract suspend fun updateMedia(value: ChronicleMediaItemEntity)
    @Delete abstract suspend fun deleteMoment(value: ChronicleMomentEntity)
    @Delete abstract suspend fun deleteMedia(value: ChronicleMediaItemEntity)
    @Query("DELETE FROM chronicle_media_items WHERE momentId=:momentId")
    protected abstract suspend fun deleteMediaForMoment(momentId: String)

    @Query("UPDATE chronicles SET draftText=:replacement, updatedAt=:now WHERE id=:id AND draftText=:expected")
    abstract suspend fun compareAndSetDraft(id: String, expected: String, replacement: String, now: Long): Int

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
    open suspend fun replaceMedia(momentId: String, values: List<ChronicleMediaItemEntity>) {
        require(values.isNotEmpty() && values.all { it.momentId == momentId })
        check(momentsForId(momentId)?.type == "MEDIA")
        deleteMediaForMoment(momentId)
        insertMedia(values.mapIndexed { index, item -> item.copy(position = index) })
    }

    @Query("SELECT * FROM chronicle_moments WHERE id=:id LIMIT 1")
    protected abstract suspend fun momentsForId(id: String): ChronicleMomentEntity?

    @Transaction
    open suspend fun promote(ownerType: String, ownerKey: String, newType: String, newKey: String, now: Long) {
        val chronicle = find(ownerType, ownerKey) ?: return
        check(find(newType, newKey) == null) { "Chronicle owner already exists" }
        updateChronicle(chronicle.copy(ownerType = newType, ownerKey = newKey, updatedAt = now))
    }

    @Transaction
    open suspend fun deleteMomentAndNormalize(moment: ChronicleMomentEntity, now: Long) {
        deleteMoment(moment)
        moments(moment.chronicleId).forEachIndexed { index, item -> setMomentPosition(item.id, index, now) }
    }
}
