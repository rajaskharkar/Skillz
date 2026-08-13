package com.kingkharnivore.skillz.data.repository

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChronicleRepository @Inject constructor(
    private val database: SkillzDatabase,
    private val dao: ChronicleDao
) {
    private val commitMutex = Mutex()
    data class Snapshot(val chronicle: ChronicleEntity?, val moments: List<ChronicleMomentEntity>)

    fun observe(ownerType: String, ownerKey: String): Flow<Snapshot> =
        dao.observe(ownerType, ownerKey).flatMapLatest { chronicle ->
            if (chronicle == null) flowOf(Snapshot(null, emptyList()))
            else dao.observeMoments(chronicle.id).flatMapLatest { flowOf(Snapshot(chronicle, it)) }
        }

    fun observeTextPreviews() = dao.observeTextPreviews()
    suspend fun getOrCreate(ownerType: String, ownerKey: String): ChronicleEntity =
        database.withTransaction {
            dao.find(ownerType, ownerKey) ?: run {
                val now = System.currentTimeMillis()
                ChronicleEntity(UUID.randomUUID().toString(), ownerType, ownerKey, "", now, now)
                    .also { dao.insertChronicle(it) }
            }
        }

    suspend fun setDraft(ownerType: String, ownerKey: String, text: String) {
        val chronicle = getOrCreate(ownerType, ownerKey)
        dao.updateChronicle(chronicle.copy(draftText = text, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addText(ownerType: String, ownerKey: String, text: String): String? = commitMutex.withLock {
        if (text.isBlank()) return@withLock null
        database.withTransaction {
            val chronicle = getOrCreate(ownerType, ownerKey)
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            dao.insertMoment(
                ChronicleMomentEntity(
                    id = id,
                    chronicleId = chronicle.id,
                    type = ChronicleMomentType.TEXT,
                    position = dao.moments(chronicle.id).size,
                    text = text,
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.compareAndSetDraft(chronicle.id, text, "", now)
            id
        }
    }

    suspend fun updateText(moment: ChronicleMomentEntity, text: String) {
        require(moment.type == ChronicleMomentType.TEXT && text.isNotBlank())
        dao.updateMoment(moment.copy(text = text, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMoment(moment: ChronicleMomentEntity) =
        dao.deleteMomentAndNormalize(moment, System.currentTimeMillis())

    suspend fun reorder(chronicleId: String, ids: List<String>) =
        dao.reorderMoments(chronicleId, ids, System.currentTimeMillis())

    suspend fun discard(ownerType: String, ownerKey: String) = dao.delete(ownerType, ownerKey)

    suspend fun promoteActiveFlow(flowInstanceId: String, sessionId: Long) =
        dao.promote(
            ChronicleOwnerType.ACTIVE_FLOW,
            flowInstanceId,
            ChronicleOwnerType.SESSION,
            sessionId.toString(),
            System.currentTimeMillis()
        )
}
