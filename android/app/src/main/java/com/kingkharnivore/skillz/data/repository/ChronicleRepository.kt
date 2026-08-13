package com.kingkharnivore.skillz.data.repository

import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChronicleRepository @Inject constructor(
    private val database: SkillzDatabase,
    private val dao: ChronicleDao
) {
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

    suspend fun addText(ownerType: String, ownerKey: String, text: String): String? {
        val normalized = text.trim()
        if (normalized.isEmpty()) return null
        return database.withTransaction {
            val chronicle = getOrCreate(ownerType, ownerKey)
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            dao.insertMoment(
                ChronicleMomentEntity(
                    id = id,
                    chronicleId = chronicle.id,
                    type = ChronicleMomentType.TEXT,
                    position = dao.moments(chronicle.id).size,
                    text = normalized,
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.updateChronicle(chronicle.copy(draftText = "", updatedAt = now))
            id
        }
    }

    suspend fun updateText(moment: ChronicleMomentEntity, text: String) {
        require(moment.type == ChronicleMomentType.TEXT && text.isNotBlank())
        dao.updateMoment(moment.copy(text = text.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun promoteActiveFlow(flowInstanceId: String, sessionId: Long) =
        dao.promote(
            ChronicleOwnerType.ACTIVE_FLOW,
            flowInstanceId,
            ChronicleOwnerType.SESSION,
            sessionId.toString(),
            System.currentTimeMillis()
        )
}
