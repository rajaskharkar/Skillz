package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import com.kingkharnivore.skillz.data.model.entity.ChronicleEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentType
import com.kingkharnivore.skillz.data.model.entity.PulseCreationEntity
import java.util.UUID

@Singleton
class PulseRepository @Inject constructor(
    private val pulseDao: PulseDao,
    private val sessionDao: SessionDao,
    private val tagDao: TagDao,
    private val database: SkillzDatabase,
    private val chronicleDao: ChronicleDao,
    private val chronicleRepository: ChronicleRepository
) {
    suspend fun findCreatedPulse(creationKey: String): Long? =
        pulseDao.findCreatedPulse(creationKey)

    fun getAllPulses(): Flow<List<PulseEntity>> = pulseDao.getAllPulses()

    fun observePulsesForSession(sessionId: Long): Flow<List<PulseEntity>> =
        pulseDao.observePulsesForSession(sessionId)

    suspend fun getPulsesForSession(sessionId: Long): List<PulseEntity> =
        pulseDao.getPulsesForSession(sessionId)

    suspend fun getPulsesForArc(arcId: Long): List<PulseEntity> =
        pulseDao.getPulsesForArc(arcId)

    suspend fun addPulse(
        title: String,
        description: String,
        tagId: Long?,
        parentSessionId: Long?,
        parentFlowInstanceId: String?,
        arcId: Long?
    ): Long {
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val id = pulseDao.insertPulse(PulseEntity(
                title = title,
                description = "",
                tagId = tagId,
                parentSessionId = parentSessionId,
                parentFlowInstanceId = parentFlowInstanceId,
                arcId = arcId,
                createdAt = now,
                updatedAt = now
            ))
            if (description.isNotBlank()) {
                val chronicleId = UUID.randomUUID().toString()
                chronicleDao.insertChronicle(ChronicleEntity(chronicleId, ChronicleOwnerType.PULSE,
                    id.toString(), "", now, now))
                chronicleDao.insertMoment(ChronicleMomentEntity(UUID.randomUUID().toString(), chronicleId,
                    ChronicleMomentType.TEXT, 0, text=description, createdAt=now, updatedAt=now))
            }
            id
        }
    }

    suspend fun addPulseAndPromoteDraft(draftId: String, pulse: PulseEntity): Long =
        chronicleRepository.finalizeOwner(ChronicleOwnerType.PULSE_DRAFT, draftId) {
          database.withTransaction {
            pulseDao.findCreatedPulse(draftId)?.let { return@withTransaction it }
            val id = pulseDao.insertPulse(pulse)
            chronicleDao.promote(ChronicleOwnerType.PULSE_DRAFT, draftId,
                ChronicleOwnerType.PULSE, id.toString(), System.currentTimeMillis())
            pulseDao.insertCreation(PulseCreationEntity(draftId, id, System.currentTimeMillis()))
            id
          }
        }

    suspend fun updatePulse(pulse: PulseEntity) {
        pulseDao.updatePulse(
            pulse.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun attachLivePulsesToSession(
        flowInstanceId: String,
        sessionId: Long,
        arcId: Long?
    ) {
        pulseDao.attachLivePulsesToSession(
            flowInstanceId = flowInstanceId,
            sessionId = sessionId,
            arcId = arcId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun detachPulsesFromSession(sessionId: Long) {
        pulseDao.detachPulsesFromSession(
            sessionId = sessionId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deletePulseAndCleanupTag(pulseId: Long): Long? {
        val pulse = pulseDao.getPulseById(pulseId) ?: return null
        val tagId = pulse.tagId
        val chronicleId = chronicleDao.find(ChronicleOwnerType.PULSE, pulseId.toString())?.id

        database.withTransaction {
            chronicleDao.delete(ChronicleOwnerType.PULSE, pulseId.toString())
            pulseDao.deletePulseById(pulseId)
        }
        if (chronicleId != null) chronicleRepository.cleanupDeletedChronicle(chronicleId)

        if (tagId == null) return null

        val remainingSessions = sessionDao.getSessionCountForTag(tagId)
        val remainingPulses = pulseDao.getPulseCountForTag(tagId)

        return if (remainingSessions == 0 && remainingPulses == 0) {
            tagDao.deleteTagById(tagId)
            tagId
        } else {
            null
        }
    }

    suspend fun getPulseById(pulseId: Long): PulseEntity? =
        pulseDao.getPulseById(pulseId)

    suspend fun updatePulseDetails(
        pulseId: Long,
        title: String,
        tagId: Long?
    ): Long? {
        val existing = pulseDao.getPulseById(pulseId) ?: return null
        val oldTagId = existing.tagId

        pulseDao.updatePulse(
            existing.copy(
                title = title,
                description = existing.description,
                tagId = tagId,
                updatedAt = System.currentTimeMillis()
            )
        )

        return if (oldTagId != null && oldTagId != tagId) {
            cleanupTagIfUnused(oldTagId)
        } else {
            null
        }
    }

    private suspend fun cleanupTagIfUnused(tagId: Long): Long? {
        val remainingSessions = sessionDao.getSessionCountForTag(tagId)
        val remainingPulses = pulseDao.getPulseCountForTag(tagId)

        return if (remainingSessions == 0 && remainingPulses == 0) {
            tagDao.deleteTagById(tagId)
            tagId
        } else {
            null
        }
    }
}
