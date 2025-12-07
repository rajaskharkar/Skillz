package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.dao.SessionDao
import com.kingkharnivore.skillz.data.model.entity.SessionEntity
import com.kingkharnivore.skillz.data.model.dao.TagDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val tagDao: TagDao
) {

    fun getAllSessions(): Flow<List<SessionEntity>> =
        sessionDao.getAllSessions()

    fun getSessionsForTag(tagId: Long): Flow<List<SessionEntity>> =
        sessionDao.getSessionsForTag(tagId)

    suspend fun addSession(
        title: String,
        description: String,
        tagId: Long,
        startTime: Long = System.currentTimeMillis(),
        endTime: Long = System.currentTimeMillis(),
        durationMs: Long = 0L          // TODO: wire stopwatch here
    ): Long {
        val session = SessionEntity(
            title = title,
            description = description,
            tagId = tagId,
            startTime = startTime,
            endTime = endTime,
            durationMs = durationMs
        )
        return sessionDao.insertSession(session)
    }

    suspend fun deleteSessionAndCleanupTag(sessionId: Long): Long? {
        val session = sessionDao.getSessionById(sessionId) ?: return null
        val tagId = session.tagId
        sessionDao.deleteSessionById(sessionId)
        val remaining = sessionDao.getSessionCountForTag(tagId)
        return if (remaining == 0) {
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
        sessionDao.deleteSession(sessionId)
    }

    suspend fun insertSession(session: SessionEntity) {
        sessionDao.insertSession(session)
    }
}