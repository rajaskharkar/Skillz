package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.entity.PulseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PulseRepository @Inject constructor(
    private val pulseDao: PulseDao,
    private val sessionDao: SessionDao,
    private val tagDao: TagDao
) {

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
        return pulseDao.insertPulse(
            PulseEntity(
                title = title,
                description = description,
                tagId = tagId,
                parentSessionId = parentSessionId,
                parentFlowInstanceId = parentFlowInstanceId,
                arcId = arcId,
                createdAt = now,
                updatedAt = now
            )
        )
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

        pulseDao.deletePulseById(pulseId)

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
        description: String,
        tagId: Long?
    ): Long? {
        val existing = pulseDao.getPulseById(pulseId) ?: return null
        val oldTagId = existing.tagId

        pulseDao.updatePulse(
            existing.copy(
                title = title,
                description = description,
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