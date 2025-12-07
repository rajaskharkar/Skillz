package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.data.model.SessionDao
import com.kingkharnivore.skillz.data.model.SessionEntity
import com.kingkharnivore.skillz.data.model.TagDao
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
}