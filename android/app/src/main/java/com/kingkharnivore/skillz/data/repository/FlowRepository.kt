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
import com.kingkharnivore.skillz.data.model.entity.SessionCreationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FlowRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val tagDao: TagDao,
    private val pulseDao: PulseDao,
    private val arcMetadataDao: ArcMetadataDao,
    private val database: SkillzDatabase,
    private val chronicleDao: ChronicleDao,
    private val chronicleRepository: ChronicleRepository
) {
    suspend fun findCreatedSession(flowInstanceId: String): Long? =
        sessionDao.findCreatedSession(flowInstanceId)

    fun getAllSessions(): Flow<List<SessionEntity>> =
        sessionDao.getAllSessions()

    fun getSessionsForTag(tagId: Long): Flow<List<SessionEntity>> =
        sessionDao.getSessionsForTag(tagId)

    suspend fun addSessionAndPromoteChronicle(flowInstanceId: String, session: SessionEntity): Long =
        chronicleRepository.finalizeOwner(ChronicleOwnerType.ACTIVE_FLOW, flowInstanceId) {
          database.withTransaction {
            sessionDao.findCreatedSession(flowInstanceId)?.let { return@withTransaction it }
            val id = sessionDao.insertSession(session)
            database.powerRewardDao().enqueue(com.kingkharnivore.skillz.data.model.entity.shell.SessionShellRewardEntity(sessionId = id, queuedAt = System.currentTimeMillis()))
            session.arcId?.let { com.kingkharnivore.skillz.utils.arc.ArcLandRewardJournal.record(database, it) }
            pulseDao.attachLivePulsesToSession(flowInstanceId, id, session.arcId)
            chronicleDao.promote(ChronicleOwnerType.ACTIVE_FLOW, flowInstanceId,
                ChronicleOwnerType.SESSION, id.toString(), System.currentTimeMillis())
            session.originPlanId?.let { sourceId ->
                val plan = database.flowPlanDao().getFlowPlanById(sourceId)
                // A timer must have accumulated time. Opening / zero-time Continue Arc is not completion.
                // Untagged templates allow choosing a journey at launch. Tagged templates require the same journey.
                if (plan != null && (plan.tagId == null || plan.tagId == session.tagId) && session.durationMs > 0 &&
                    (plan.kind == com.kingkharnivore.skillz.data.model.entity.HorizonKind.HABIT || plan.completedAt == null)) {
                    val tagName = database.tagDao().getAllTagsSnapshot().firstOrNull { it.id == session.tagId }?.name.orEmpty()
                    database.horizonMemoryDao().insert(com.kingkharnivore.skillz.data.model.entity.HorizonMemoryEntity(
                        flowInstanceId = flowInstanceId, sourcePlanId = sourceId, kind = plan.kind,
                        activityTitle = plan.title, sessionId = id, title = session.title, tagName = tagName,
                        mode = session.mode, durationMs = session.durationMs, score = session.scyraPoints,
                        surgePoints = session.surgePoints, surgePlannedMs = session.surgePlannedMs,
                        arcId = session.arcId, arcMultiplier = session.arcMultiplierUsed, completedAt = session.endTime
                    ))
                    database.flowPlanDao().markCompleted(sourceId, session.endTime)
                }
            }
            sessionDao.insertCreation(SessionCreationEntity(flowInstanceId, id, System.currentTimeMillis()))
            id
          }
        }

    suspend fun updateSessionDetails(sessionId: Long, title: String, tagId: Long): Long? =
        database.withTransaction {
            val existing = checkNotNull(sessionDao.getSessionById(sessionId))
            sessionDao.updateDetails(sessionId, title, tagId)
            val tagName = tagDao.getAllTagsSnapshot().firstOrNull { it.id == tagId }?.name.orEmpty()
            database.horizonMemoryDao().updateDetails(sessionId, title, tagName)
            val oldTagId = existing.tagId
            if (oldTagId != tagId && sessionDao.getSessionCountForTag(oldTagId) == 0 &&
                pulseDao.getPulseCountForTag(oldTagId) == 0 && database.flowPlanDao().countForTag(oldTagId) == 0) {
                tagDao.deleteTagById(oldTagId)
                oldTagId
            } else null
        }

    suspend fun deleteSessionAndCleanupTag(sessionId: Long): Long? {
        val session = sessionDao.getSessionById(sessionId) ?: return null
        val tagId = session.tagId

        deleteSessionTransactionally(sessionId)

        val remainingSessions = sessionDao.getSessionCountForTag(tagId)
        val remainingPulses = pulseDao.getPulseCountForTag(tagId)

        return if (remainingSessions == 0 && remainingPulses == 0 && database.flowPlanDao().countForTag(tagId) == 0) {
            tagDao.deleteTagById(tagId)
            tagId
        } else {
            null
        }
    }

    suspend fun deleteSession(sessionId: Long) {
        deleteSessionTransactionally(sessionId)
    }

    suspend fun deleteMemory(flowInstanceId: String) {
        val chronicleId = database.withTransaction {
            val memory = database.horizonMemoryDao().getByFlowInstanceId(flowInstanceId)
                ?: return@withTransaction null
            database.horizonMemoryDao().delete(flowInstanceId)
            memory.sessionId?.let { sessionId ->
                val tagId = sessionDao.getSessionById(sessionId)?.tagId
                val deletedChronicleId = deleteSessionRecords(sessionId)
                if (tagId != null && sessionDao.getSessionCountForTag(tagId) == 0 &&
                    pulseDao.getPulseCountForTag(tagId) == 0 && database.flowPlanDao().countForTag(tagId) == 0) {
                    tagDao.deleteTagById(tagId)
                }
                deletedChronicleId
            }
        }
        if (chronicleId != null) chronicleRepository.cleanupDeletedChronicle(chronicleId)
    }

    private suspend fun deleteSessionTransactionally(sessionId: Long) {
        val chronicleId = database.withTransaction { deleteSessionRecords(sessionId) }
        if (chronicleId != null) chronicleRepository.cleanupDeletedChronicle(chronicleId)
    }

    // Call inside a transaction; clean up media only after that transaction commits.
    private suspend fun deleteSessionRecords(sessionId: Long): String? {
        val chronicleId = chronicleDao.find(ChronicleOwnerType.SESSION, sessionId.toString())?.id
        val arcId = sessionDao.getSessionById(sessionId)?.arcId
        chronicleDao.delete(ChronicleOwnerType.SESSION, sessionId.toString())
        pulseDao.detachPulsesFromSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
        if (arcId != null && sessionDao.getSessionCountForArc(arcId) == 0) {
            arcMetadataDao.delete(arcId)
        }
        return chronicleId
    }

    suspend fun updateArcFields(
        sessionId: Long,
        arcId: Long,
        arcIndex: Int,
        arcMultiplierUsed: Double,
        arcBonusPoints: Int,
        finalScyraPoints: Int
    ) = database.withTransaction {
        sessionDao.updateArcFields(
            sessionId = sessionId,
            arcId = arcId,
            arcIndex = arcIndex,
            arcMultiplierUsed = arcMultiplierUsed,
            arcBonusPoints = arcBonusPoints,
            finalScyraPoints = finalScyraPoints
        )
        database.horizonMemoryDao().updateArc(sessionId, arcId, arcMultiplierUsed, finalScyraPoints)
        com.kingkharnivore.skillz.utils.arc.ArcLandRewardJournal.record(database, arcId)
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
