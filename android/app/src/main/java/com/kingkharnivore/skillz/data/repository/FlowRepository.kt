package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.PulseDao
import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.dao.TagDao
import com.kingkharnivore.skillz.data.model.dao.ArcMetadataDao
import com.kingkharnivore.skillz.data.model.SkillzDatabase
import androidx.room.withTransaction
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.dao.ChronicleDao
import com.kingkharnivore.skillz.data.model.entity.ChronicleOwnerType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FlowRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val tagDao: TagDao,
    private val pulseDao: PulseDao,
    private val arcMetadataDao: ArcMetadataDao,
    private val database: SkillzDatabase,
    private val chronicleDao: ChronicleDao
) {

    fun getAllSessions(): Flow<List<SessionEntity>> =
        sessionDao.getAllSessions()

    fun getSessionsForTag(tagId: Long): Flow<List<SessionEntity>> =
        sessionDao.getSessionsForTag(tagId)

    suspend fun addSession(
        title: String,
        description: String,
        tagId: Long,
        startTime: Long,
        endTime: Long,
        durationMs: Long,
        surgePlannedMs: Long?,
        surgePoints: Int,
        scyraPoints: Int,
        isSoftMode: Boolean = false
    ): Long {
        val session = SessionEntity(
            title = title,
            description = description,
            tagId = tagId,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs,
            surgePlannedMs = surgePlannedMs,
            surgePoints = surgePoints,
            scyraPoints = scyraPoints,
            isSoftMode = isSoftMode
        )
        return sessionDao.insertSession(session)
    }

    suspend fun addSessionAndPromoteChronicle(flowInstanceId: String, session: SessionEntity): Long =
        database.withTransaction {
            val id = sessionDao.insertSession(session)
            chronicleDao.promote(ChronicleOwnerType.ACTIVE_FLOW, flowInstanceId,
                ChronicleOwnerType.SESSION, id.toString(), System.currentTimeMillis())
            id
        }

    suspend fun deleteSessionAndCleanupTag(sessionId: Long): Long? {
        val session = sessionDao.getSessionById(sessionId) ?: return null
        val tagId = session.tagId

        deleteSessionTransactionally(sessionId)

        val remainingSessions = sessionDao.getSessionCountForTag(tagId)
        val remainingPulses = pulseDao.getPulseCountForTag(tagId)

        return if (remainingSessions == 0 && remainingPulses == 0) {
            tagDao.deleteTagById(tagId)
            tagId
        } else {
            null
        }
    }

    suspend fun updateSessionDescription(sessionId: Long, description: String) {
        sessionDao.updateSessionDescription(sessionId, description)
    }

    suspend fun deleteSession(sessionId: Long) {
        deleteSessionTransactionally(sessionId)
    }

    private suspend fun deleteSessionTransactionally(sessionId: Long) {
        database.withTransaction {
            val arcId = sessionDao.getSessionById(sessionId)?.arcId
            pulseDao.detachPulsesFromSession(sessionId)
            sessionDao.deleteSessionById(sessionId)
            if (arcId != null && sessionDao.getSessionCountForArc(arcId) == 0) {
                arcMetadataDao.delete(arcId)
            }
        }
    }

    suspend fun insertSession(session: SessionEntity) {
        sessionDao.insertSession(session)
    }

    suspend fun updateArcFields(
        sessionId: Long,
        arcId: Long,
        arcIndex: Int,
        arcMultiplierUsed: Double,
        arcBonusPoints: Int,
        finalScyraPoints: Int
    ) {
        sessionDao.updateArcFields(
            sessionId = sessionId,
            arcId = arcId,
            arcIndex = arcIndex,
            arcMultiplierUsed = arcMultiplierUsed,
            arcBonusPoints = arcBonusPoints,
            finalScyraPoints = finalScyraPoints
        )
    }

    suspend fun getSessionsForArc(arcId: Long): List<SessionEntity> =
        sessionDao.getSessionsForArc(arcId)

    suspend fun getLastSessionInArc(arcId: Long): SessionEntity? =
        sessionDao.getLastSessionInArc(arcId)

    suspend fun getMaxArcIndex(arcId: Long): Int =
        sessionDao.getMaxArcIndex(arcId) ?: 0

    suspend fun getSessionById(sessionId: Long): SessionEntity? =
        sessionDao.getSessionById(sessionId)
}
